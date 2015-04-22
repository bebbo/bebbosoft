/******************************************************************************
 * This file is part of de.bb.tools.bnm.launcher.
 *
 *   de.bb.tools.bnm.launcher is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.launcher is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.launcher.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Stack;
import java.util.StringTokenizer;

public class BnmLauncher {

	private final static String ERRORS[] = {
			"no version specified: use --version=<bnm version>",
			"can't find the repository: set environment variable M2_REPO",
			"can't create the repository folder",
			"can't find the pom file for bnm core",
			"can't find the jar file for bnm core",
			"can't determine the version of de.bb.util",
			"can't find the jar file for de.bb.util",
			"failed to setup the class URL loader",
			"exception while invoking the bnm main" };

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int error = 0;
		int argsUsed = 0;
		Fatal: {
			// error = 0
			if (args.length == 0)
				break Fatal;
			String version = args[0];
			if (!version.startsWith("--version="))
				break Fatal;
			version = version.substring(10);
			++argsUsed;
			++error;

			// locate the settings.xml and the local repository
			// error = 1
			String srepo = null;
			String m2repo = System.getProperty("M2_REPO");
			String home = System.getProperty("user.home");
			if (m2repo == null) {
				if (home == null)
					break Fatal;
				m2repo = home + File.separator + ".m2";
				System.out.println("M2_REPO is undefined, using: " + m2repo);
			}
			File settings = new File(m2repo, "settings.xml");
			try {
				if (!settings.exists()) {
					// ask user to create settings.xml
					for (;;) {
						System.out.println(settings.toString()
								+ " does not exist! Create (Y/n)?");
						int ch = System.in.read();
						if (ch == 'n' || ch == 'N')
							break Fatal;
						if (ch < 32 || ch == 'y' || ch == 'Y')
							break;
					}
					while (System.in.available() > 0) {
						System.in.read();
					}
					// ask user for local repo path
					String local = "";
					System.out
							.println("Enter a location for your local repository: ("
									+ home + File.separator + "repo): ");
					for (;;) {
						int ch = System.in.read();
						if (ch < 32)
							break;
						local += (char) ch;
					}
					while (System.in.available() > 0) {
						System.in.read();
					}
					local = local.trim();
					if (local.length() == 0)
						local = home + "/repo";

					// create the settings.xml
					settings.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(settings);
					fos.write(("<settings><localRepository>" + local + "</localRepository></settings>")
							.getBytes());
					fos.close();
					System.out.println("Created: " + local);
				}

				BnmLauncher dxs = new BnmLauncher(settings);
				srepo = dxs.search("/settings/localRepository");
			} catch (IOException e) {
			}
			if (srepo == null)
				break Fatal;
			++error;

			// ensure local repository is valid
			// error = 2
			File repo = new File(srepo);
			if (!repo.exists()) {
				if (!repo.mkdirs()) {
					break Fatal;
				}
			}
			++error;

			// ensure bnm core pom exists
			// error = 3
			File core = new File(repo, "de/bb/tools/bnm/core/" + version);
			File corePom = download(repo, core, "core-" + version + ".pom");
			if (corePom == null || !corePom.exists())
				break Fatal;
			++error;

			// ensure bnm core jar exists
			// error = 4
			File coreJar = download(repo, core, "core-" + version + ".jar");
			if (coreJar == null || !coreJar.exists())
				break Fatal;
			++error;

			// ensure util jar dependency exists
			// error = 5
			String utilVersion = null;
			try {
				BnmLauncher dxs = new BnmLauncher(corePom);
				utilVersion = dxs
						.search("/project/dependencies/dependency/version");
			} catch (IOException e) {
			}
			if (utilVersion == null)
				break Fatal;
			++error;

			// ensure util jar exists
			// error = 6
			File util = new File(repo, "de/bb/util/" + utilVersion);
			File utilJar = download(repo, util, "util-" + utilVersion + ".jar");
			if (utilJar == null || !utilJar.exists())
				break Fatal;
			++error;

			// ensure downloads were successful
			// error = 7
			URL urls[] = new URL[2];
			try {
				urls[0] = coreJar.toURI().toURL();
				urls[1] = utilJar.toURI().toURL();
			} catch (MalformedURLException e) {
				break Fatal;
			}
			++error;

			// instantiate bnm and run
			// error = 8
			String nargs[] = new String[args.length - argsUsed];
			System.arraycopy(args, argsUsed, nargs, 0, nargs.length);

			URLClassLoader ucl = new URLClassLoader(urls);
			try {
				Class clazz = ucl.loadClass("de.bb.tools.bnm.Main");
				Method m = clazz.getMethod("main",
						new Class[] { ERRORS.getClass() });
				m.invoke(null, new Object[] { nargs });
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		error("error #" + error + ": " + ERRORS[error]);
	}

	private static File download(File repo, File dir, String fileName) {
		File file = new File(dir, fileName);
		if (file.exists())
			return file;

		String part = file.toString().substring(repo.toString().length());
		part = part.replace('\\', '/');

		try {
			URL url = new URL("http://www.bebbosoft.de/repo" + part);
			info("trying " + url);
			URLConnection con = url.openConnection();
			con.setReadTimeout(60000);
			try {
				con.connect();
			} catch (Exception ex) {
				error("cannot connect to " + url);
				return null;
			}
			InputStream is = con.getInputStream();
			file.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(file);
			byte b[] = new byte[32768];
			for (;;) {
				int len = is.read(b);
				if (len <= 0)
					break;
				fos.write(b, 0, len);
			}
			is.close();
			fos.close();

			return file;
		} catch (Exception ex) {
			error(ex.getMessage());
		}
		return null;
	}

	private static void info(String msg) {
		System.out.println("[INFO]  " + msg);
	}

	private static void error(String msg) {
		System.out.println("[ERROR] " + msg);
	}

	/** embedded stuff to have only 1 class file. */
	private String content;

	private BnmLauncher(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		int len = fis.available();
		byte d[] = new byte[len];
		fis.read(d);
		fis.close();
		// no encoding support - works mostly...
		this.content = new String(d, 0, 0, len);
	}

	private String search(String path) {
		int slash = path.lastIndexOf('/');
		String key = "<" + path.substring(slash + 1);
		String part = path.substring(0, slash < 1 ? slash + 1 : slash);
		int offset = 0;
		for (; offset >= 0; ++offset) {
			offset = content.indexOf(key, offset);
			if (offset < 0)
				break;
			String tag = new StringTokenizer(content.substring(offset),
					" \r\n\t>").nextToken();
			if (!key.equals(tag))
				continue;

			String val = getValuePos(key.substring(1), offset);
			if (val == null)
				continue;

			if (!hasPartialPath(offset, part))
				continue;

			return val;
		}
		return null;
	}

	private boolean hasPartialPath(int offset, String part) {
		Stack/* <String> */stack = new Stack/* <String> */();
		while (part.length() > 0) {
			int slash = part.lastIndexOf('/');
			String key = part.substring(slash + 1);
			part = part.substring(0, slash < 1 ? slash + 1 : slash);

			// search the key before current position.
			// if a closing element occurs, push the key and make the previous
			// current
			// do this until key is empty
			for (;;) {
				int ket = content.lastIndexOf('>', offset);
				if (ket < 0)
					return part.equals("/");
				// skip comments
				if (ket > 1 && "--".equals(content.substring(ket - 2, ket))) {
					int bra = content.lastIndexOf("<!--", ket);
					if (bra < 0)
						return false;
					offset = bra;
					continue;
				}
				// skip CDATA
				if (ket > 1 && "]]".equals(content.substring(ket - 2, ket))) {
					int bra = content.lastIndexOf("<![CDATA[", ket);
					if (bra < 0)
						return false;
					offset = bra;
					continue;
				}
				int bra = content.lastIndexOf('<', ket);
				if (bra < 0)
					return false;

				// endTag? push current key and search the matching start Tag
				if (content.charAt(bra + 1) == '/') {
					stack.push(key);
					key = content.substring(bra + 2, ket).trim();
					offset = bra;
					continue;
				}

				String tag = new StringTokenizer(
						content.substring(bra + 1, ket), " \r\n\t").nextToken();

				if (tag.startsWith("?")) {
					offset = bra;
					continue;
				}

				if (!tag.equals(key))
					return false;
				offset = bra;
				if (stack.size() == 0)
					break;
				key = (String) stack.pop();
			}
		}
		return true;
	}

	private String getValuePos(String key, int offset) {
		int ket = content.indexOf('>', offset);
		if (ket < 0)
			return null;

		int start = ket + 1;
		Stack/* <String> */stack = new Stack/* <String> */();
		for (;;) {
			int bra = content.indexOf("<", ket);
			if (bra < 0)
				return null;

			// ensure that the closing tag belongs to the key
			// skip comments
			if (content.substring(bra).startsWith("<!--")) {
				ket = content.indexOf("-->", bra + 4);
				if (ket < 0)
					return null;
				continue;
			}

			// skip CDATA
			if (content.substring(bra).startsWith("<![CDATA[")) {
				ket = content.indexOf("]]>", bra);
				if (ket < 0)
					return null;
				continue;
			}

			// check if new tag starts
			String rest = content.substring(bra + 1);
			String tag = new StringTokenizer(rest, " \r\n\t>").nextToken();

			if (!tag.startsWith("/")) {
				ket = content.indexOf('>', bra);
				stack.push(key);
				key = tag;
				continue;
			}

			tag = tag.substring(1);
			if (!tag.equals(key))
				return null;

			if (stack.size() > 0) {
				ket = content.indexOf('>', bra);
				key = (String) stack.pop();
				continue;
			}

			return content.substring(start, bra);

		}
	}
}
