/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.SortedMap;

import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Plugin;
import de.bb.tools.bnm.model.Repository;
import de.bb.tools.bnm.plugin.PluginParams;
import de.bb.tools.bnm.setting.Mirror;
import de.bb.util.LRUCache;
import de.bb.util.XmlFile;

public class Loader {

	static int DLID = 0;

	Hashtable<String, String> BLACKLISTS = new Hashtable<String, String>();
	Hashtable<File, File> NOTAVAILABLE = new Hashtable<File, File>();

	private static LRUCache<String, PluginParams> pluginParams = new LRUCache<String, PluginParams>();

	private static File repoPath;

	public static File getRepoPath() {
		return repoPath;
	}

	protected static ArrayList<Repository> repos;
	private static ArrayList<Repository> pluginRepos;
	// yet unused...
	private static SortedMap<String, Mirror> mirrors;

	ArrayList<Repository> pomRepositories = new ArrayList<Repository>();
	ArrayList<Repository> pomPluginRepositories = new ArrayList<Repository>();

	private HashSet<String> localPoms = new HashSet<String>();

	private boolean downloadSnapshots;

	public Loader() {
		this(null, null);
	}

	public Loader(ArrayList<Repository> repositories, ArrayList<Repository> pluginRepositories) {
		if (repositories != null)
			pomRepositories.addAll(repositories);

		if (pluginRepos != null)
			pomPluginRepositories.addAll(pluginRepos);

		// add the bebbosoft repo as default
		if (pomPluginRepositories.isEmpty()) {
			Repository repo = new Repository();
			repo.id = "bebbosoft";
			repo.url = "http://www.bebbosoft.de/repo";
			pomPluginRepositories.add(repo);
		}
	}

	public Loader(ArrayList<Repository> repositories, ArrayList<Repository> pluginRepositories,
			Loader parentLoader) {
		this(repositories, pluginRepositories);
		pomRepositories.addAll(parentLoader.pomRepositories);
		pomPluginRepositories.addAll(parentLoader.pomPluginRepositories);

		localPoms = parentLoader.localPoms;
		BLACKLISTS = parentLoader.BLACKLISTS;
		NOTAVAILABLE = parentLoader.NOTAVAILABLE;
	}

	public File makeRepoPath(Id id, String classifier, String ext) {
		File file = new File(id.groupId.replace('.', '/'), id.artifactId + "/" + id.version);
		String base = id.artifactId + "-" + id.version;
		if (classifier != null)
			base += "-" + classifier;
		file = new File(file, base + "." + ext);
		File repoFile = new File(repoPath, file.toString());
		return repoFile;
	}

	public URL findURL(Id id, String ext) throws Exception {
		return findURL(id, null, ext, null, true);
	}

	public URL findURL(Id id, String ext, String inside) throws Exception {
		return findURL(id, null, ext, inside, true);
	}

	/**
	 * Searches the URL for a given module, extension and also inside JAR.
	 *
	 * @param id
	 * @param ext
	 * @param inside
	 * @param mustExist force the existence of that file.
	 * @return
	 * @throws Exception
	 */
	public URL findURL(Id id, String classifier, String ext, String inside,
			boolean mustExist) throws Exception {
		if (id.version == null)
			throw new Exception("no version in Id: " + id.getId());
		if (id.version.charAt(0) == '[') {
			File metafile = new File(id.toPath(), id.artifactId + "/maven-metadata.xml");
			File repoMetafile = new File(repoPath, metafile.toString());
			if (!repoMetafile.exists()) {
				updateFile(metafile, repoMetafile);
				if (!repoMetafile.exists()) {
					throw new Exception("no maven-metadata.xml: version ranges are not supported: " + id);
				}
			}

			XmlFile xmlFile = new XmlFile();
			xmlFile.readFile(repoMetafile.getAbsolutePath());
			String version = xmlFile.getContent("/metadata/version");
			if (version == null)
				version = xmlFile.getContent("/metadata/versioning/release");
			if (version == null) {
				throw new Exception("can't detect a usable version for " + id);
			}
			Log.getLog().warn("fixed version range with maven info: " + id.getId() + " -> " + version);
			id.version = version;
		}

		// check local repository
		String folderPart = id.toPath();
		File file = new File(folderPart, id.artifactId + "/" + id.version);
		file = new File(file,
				id.artifactId + "-" + id.version + (classifier != null ? "-" + classifier : "") + "." + ext);
		File repoFile = new File(repoPath, file.toString());

		if (mustExist) {
			synchronized (localPoms) {
				if (!repoFile.exists() || (downloadSnapshots && id.version != null && id.version.endsWith("SNAPSHOT")
						&& !localPoms.contains(id.getGA()))) {
					updateFile(file, repoFile);
				}
			}
			if (!repoFile.exists())
				Log.getLog().error("can't load: " + repoFile);
		}
		if (inside == null)
			return new URL("file:///" + repoFile.getAbsolutePath());
		return new URL("jar:file:///" + repoFile.getAbsolutePath() + "!/" + inside);
	}

	public InputStream findInputStream(Id id, String ext, String inside) throws Exception {
		try {
			URL url = findURL(id, ext, inside);
			return url.openStream();
		} catch (Exception ex) {
			throw new IOException("can't load: " + id + "." + ext + ":" + inside, ex);
		}
	}

	private void updateFile(File file, File repoFile) {
		if (NOTAVAILABLE.get(file) != null)
			return;
		Log.getLog().info("getting update for: " + file);
		try {
			Group group = new Group();
			group.add(file, repoFile, this, pomRepositories, pomPluginRepositories);
			group.join();
		} catch (Exception ex) {
		}
	}

	class Group {

		ArrayList<T> ts = new ArrayList<T>();

		public Group() {
		}

		public synchronized void add(File file, File repoFile, Loader loader,
				ArrayList<Repository> repos, ArrayList<Repository> pomPluginRepositories) {
			T t = new T(file, repoFile, repos, pomPluginRepositories, Log.getLog());
			ts.add(t);
			t.start();
		}

		public void join() throws Exception {
			ArrayList<File> fl = new ArrayList<File>();
			for (T t : ts) {
				try {
					t.join();
				} catch (InterruptedException e) {
					t.success = false;
				}
				if (!t.success) {
					fl.add(t.file);
				}
			}
			if (fl.size() > 0)
				throw new Exception("can't load/update " + fl);
		}

		class T extends Thread {
			File file;
			private File repoFile;
			boolean success;
			private ArrayList<Repository> repositories = new ArrayList<Repository>();
			private Log log;
			private long fileDate;
			private HashSet<String> tried = new HashSet<String>();

			public T(File file, File repoFile, ArrayList<Repository> repositories,
					ArrayList<Repository> pomPluginRepositories, Log log) {
				super("downloader-" + ++DLID);
				this.file = file;
				this.repoFile = repoFile;
				this.repositories.addAll(repositories);
				this.repositories.addAll(Loader.repos);
				this.repositories.addAll(pomPluginRepositories);
				this.log = log;
			}

			public void run() {
				fileDate = -1;
				if (repoFile.exists())
					fileDate = repoFile.lastModified();
				log.debug("using repos: " + repositories);
				for (Repository r : repositories) {
					for (Mirror m : Loader.mirrors.subMap(r.id, r.id + "\u0000").values()) {
						success = downloadAt(m.url);
						if (success)
							break;
					}
					if (!success)
						success = downloadAt(r.url);
					if (success)
						return;
				}
				NOTAVAILABLE.put(file, file);
			}

			private boolean downloadAt(String surl) {
				if (!surl.endsWith("/"))
					surl += "/";
				if (BLACKLISTS.get(surl) != null)
					return false;
				if (tried.contains(surl))
					return false;
				tried.add(surl);
				try {
					URL url = new URL(surl + file.toString().replace('\\', '/'));
					log.info("trying " + url);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					while (true) {
						con.setReadTimeout(60000);
						try {
							con.connect();
						} catch (Exception ex) {
							log.info("black listing: " + surl);
							BLACKLISTS.put(surl, surl);
							return false;
						}
						if (con.getDate() <= fileDate) {
							return false;
						}

						int status = con.getResponseCode();
						if (status != HttpURLConnection.HTTP_OK) {
							if (status == HttpURLConnection.HTTP_MOVED_TEMP
									|| status == HttpURLConnection.HTTP_MOVED_PERM
									|| status == HttpURLConnection.HTTP_SEE_OTHER) {
								String newUrl = con.getHeaderField("Location");
								String cookies = con.getHeaderField("Set-Cookie");

								// open the new connnection again
								con = (HttpURLConnection) new URL(newUrl).openConnection();
								con.setRequestProperty("Cookie", cookies);
								log.info("redirect to " + newUrl);
								continue;
							}
						}
						break;
					}

					InputStream is = con.getInputStream();
					repoFile.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(repoFile);
					byte b[] = new byte[32768];
					for (;;) {
						int len = is.read(b);
						Log.toggle();
						if (len <= 0)
							break;
						fos.write(b, 0, len);
					}
					is.close();
					fos.close();
					return true;
				} catch (Exception e) {
				}
				return false;
			}
		}
	}

	public static void setMirrors(SortedMap<String, Mirror> mirrors) {
		Loader.mirrors = mirrors;
	}

	public static void setRepo(File repoPath) {
		Loader.repoPath = repoPath;
	}

	public static void setRepositories(ArrayList<Repository> repos, ArrayList<Repository> pluginRepos) {
		Loader.repos = repos;
		Loader.pluginRepos = pluginRepos;
	}

	public PluginParams getComponent(Plugin p) throws Exception {
		PluginParams pp = pluginParams.get(p.getId());
		if (pp != null)
			return pp;

		InputStream is = findInputStream(p, "jar", "META-INF/maven/plugin.xml");
		XmlFile xml = new XmlFile();
		xml.read(is);
		is.close();

		pp = new PluginParams();
		Bind.bind(xml, "/plugin/", pp);
		pp.fillMap();

		pluginParams.put(p.getId(), pp);
		return pp;
	}

	protected static String stats() {
		return pluginParams.toString();
	}

	public File findFile(Id id, String ext) throws Exception {
		return findFile(id, null, ext, true);
	}

	public File findFile(Id id, String classifier, String ext) throws Exception {
		return findFile(id, classifier, ext, true);
	}

	public File findFile(Id id, String classifier, String ext, boolean mustExist)
			throws Exception {
		URL url = findURL(id, classifier, ext, null, mustExist);
		if (!url.getProtocol().equals("file"))
			throw new Exception("no local file found: " + url);

		return new File(url.toString().substring(6));
	}

	public void markLocal(String ga) {
		synchronized (localPoms) {
			localPoms.add(ga);
		}
	}

	public void setSnapshotBehaviour(boolean downloadSnapshots) {
		this.downloadSnapshots = downloadSnapshots;
	}

}
