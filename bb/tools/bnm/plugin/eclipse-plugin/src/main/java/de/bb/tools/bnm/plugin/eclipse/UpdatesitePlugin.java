package de.bb.tools.bnm.plugin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.eclipse.versioning.dumb.ManifestInfo;
import de.bb.tools.bnm.eclipse.versioning.dumb.NameVersion;
import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.model.Resource;
import de.bb.tools.bnm.plugin.jar.JarPlugin;
import de.bb.util.FileBrowser;
import de.bb.util.XmlFile;

public class UpdatesitePlugin extends JarPlugin {

	private final static String CONTENT_TEMPLATE = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?metadataRepository version='1.1.0'?>"
			+ "<repository name='' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>"
			+ "<properties size='2'>" + "<property name='p2.timestamp' value=''/>"
			+ "<property name='p2.compressed' value='false'/>" + "</properties>" + "</repository>";

	private final static String ARTIFACT_TEMPLATE = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?artifactRepository version='1.1.0'?><repository name='' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>"
			+ "<properties size='2'>" + "<property name='p2.timestamp' value=''/>"
			+ "<property name='p2.compressed' value='false'/>" + "</properties>" + "<mappings size='3'>"
			+ "<rule filter='(&amp; (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}-${version}.jar'/>"
			+ "<rule filter='(&amp; (classifier=binary))' output='${repoUrl}/binary/${id}-${version}'/>"
			+ "<rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='${repoUrl}/features/${id}-${version}.jar'/>"
			+ "</mappings>" + "</repository>";

	private final static String SITE_TEMPLATE = "<?xml version='1.0' encoding='UTF-8'?><site/>";

	private long maxDate;

	@Property("description")
	private String myDescription;

	@Config("label")
	private String myLabel;

	@Config("organization")
	private String organization;

	private File pluginsDir;

	private Log log;

	private HashSet<String> added;

	private Loader loader;

	private File featureDir;

	public void execute() throws Exception {
		added = new HashSet<String>();

		maxDate = 0;

		final FileBrowser fb = new FileBrowser() {
			protected void handleFile(final String path, final String name) {
				final String f = getBaseDir() + path + "/" + name;
				final long mod = new File(f).lastModified();
				if (mod > maxDate)
					maxDate = mod;
			}
		};

		final Project epom = project.getEffectivePom();
		final ArrayList<? extends Resource> resources = epom.build.resources;
		if (resources != null) {
			for (final Resource r : resources) {
				if (r.directory == null)
					continue;
				final File dir = new File(currentDir, r.directory);
				fb.scan(dir.getAbsolutePath(), true);
			}
		}
		final ArrayList<Id> artifacts = this.project.getRuntimeDependencies();
		loader = project.getLoader();

		final File soutfile = new File(getContentDirectory(), "site.xml");
		final File outfile = new File(getContentDirectory(), "content.xml");
		final File aoutfile = new File(getContentDirectory(), "artifacts.xml");
		boolean uptodate = outfile.exists() && aoutfile.exists()
				&& soutfile.lastModified() > new File(currentDir, "pom.xml").lastModified()
				&& outfile.lastModified() > new File(currentDir, "pom.xml").lastModified()
				&& aoutfile.lastModified() > new File(currentDir, "pom.xml").lastModified();
		if (uptodate) {
			for (final Id artifact : artifacts) {
				// skip self
				if (artifact.getGA().equals(epom.getGA()))
					continue;
				final File jar = loader.findFile(artifact, "jar");
				if (jar.lastModified() > outfile.lastModified()) {
					uptodate = false;
					break;
				}
			}
		}
		// still uptodate
		if (uptodate) {
			getLog().info("file is uptodate: " + outfile.getAbsolutePath());
			getLog().info("file is uptodate: " + aoutfile.getAbsolutePath());
			return;
		}

		// create new content.xml
		final XmlFile xml = new XmlFile();
		xml.readString(CONTENT_TEMPLATE);

		final XmlFile axml = new XmlFile();
		axml.readString(ARTIFACT_TEMPLATE);

		final XmlFile sxml = new XmlFile();
		sxml.readString(SITE_TEMPLATE);

		final long now = System.currentTimeMillis();

		xml.setString("/repository", "name", epom.groupId + "." + epom.artifactId);
		xml.setString("/repository/properties/\\property\\p2.timestamp", "value", Long.toString(now));

		axml.setString("/repository", "name", epom.groupId + "." + epom.artifactId);
		axml.setString("/repository/properties/\\property\\p2.timestamp", "value", Long.toString(now));

		sxml.setString("/site/description", "name", myLabel);
		// sxml.setString("/site/description", "url", myUrl);
		sxml.setContent("/site/description", myDescription);

		// add own unit
		final String mainKey = xml.createSection("/repository/units/unit");
		xml.setString(mainKey, "id", epom.groupId + "." + epom.artifactId);
		setVersion(xml, mainKey, epom.version);

		addProperty(xml, mainKey, "org.eclipse.equinox.p2.name", myLabel);
		addProperty(xml, mainKey, "org.eclipse.equinox.p2.description", myDescription);
		addProperty(xml, mainKey, "org.eclipse.equinox.p2.provider", organization);
		addPropertySize(xml, mainKey);

		addProvides(xml, mainKey, "org.eclipse.equinox.p2.iu", epom.groupId + "." + epom.artifactId, epom.version);
		addProvidesSize(xml, mainKey);

		xml.setString(mainKey + "touchpoint", "id", "null");
		xml.setString(mainKey + "touchpoint", "version", "0.0.0");

		final Bnm bnm = project.getBnm();
		log = getLog();
		featureDir = new File(getContentDirectory(), "features");
		featureDir.mkdirs();

		pluginsDir = new File(getContentDirectory(), "plugins");
		pluginsDir.mkdirs();

		String lastKey = null;
		// add two units per feature
		for (final Id artifact : artifacts) {
			// skip self
			if (artifact.getGA().equals(epom.getGA()))
				continue;

			added.clear();
			addArtifact(axml, artifact, true);

			final String ga = artifact.groupId + "." + artifact.artifactId;

			final XmlFile feature = new XmlFile();
			final InputStream is = loader.findInputStream(artifact, "jar", "feature.xml");
			feature.read(is);
			is.close();
			final String providerName = feature.getString("/feature", "provider-name", null);
			final String label = feature.getString("/feature", "label", ga);
			final String descriptionUrl = feature.getString("/feature/description", "url", null);
			final String description = feature.getContent("/feature/description").trim();
			final String copyright = feature.getContent("/feature/copyright").trim();
			final String copyrightUrl = feature.getString("/feature/copyright", "url", null);
			final String license = feature.getContent("/feature/license").trim();
			final String licenseUrl = feature.getString("/feature/license", "url", null);

			final String cat = sxml.createSection("/site/category-def");
			sxml.setString(cat, "name", label);
			sxml.setString(cat, "label", label);
			sxml.setContent(cat + "description", description);
			setVersion(sxml, cat, artifact.version);

			addRequired(xml, mainKey, ga + ".group", artifact.version, artifact.version);

			String key = xml.createSection("/repository/units/unit");
			xml.setString(key, "id", ga + ".feature.jar");
			setVersion(xml, key, artifact.version);

			addProperty(xml, key, "org.eclipse.equinox.p2.name", label);
			addProperty(xml, key, "org.eclipse.equinox.p2.provider", providerName);
			addProperty(xml, key, "org.eclipse.equinox.p2.description", description);
			addProperty(xml, key, "org.eclipse.equinox.p2.description.url", descriptionUrl);
			addPropertySize(xml, key);

			addProvides(xml, key, "org.eclipse.equinox.p2.eclipse.type", "feature", "1.0.0");
			addProvides(xml, key, "org.eclipse.equinox.p2.iu", ga + ".feature.jar", artifact.version);
			addProvides(xml, key, "org.eclipse.update.feature", ga, artifact.version);
			addProvidesSize(xml, key);

			xml.setContent(key + "filter", "(org.eclipse.update.install.features=true)");

			xml.setString(key + "artifacts", "size", "1");
			xml.setString(key + "artifacts/artifact", "classifier", "org.eclipse.update.feature");
			xml.setString(key + "artifacts/artifact", "id", ga);
			setVersion(xml, key + "artifacts/artifact", artifact.version);

			xml.setString(key + "touchpoint", "id", "org.eclipse.equinox.p2.osgi");
			xml.setString(key + "touchpoint", "version", "1.0.0");

			xml.setString(key + "touchpointData", "size", "1");
			xml.setString(key + "touchpointData/instructions", "size", "1");
			xml.setString(key + "touchpointData/instructions/instruction", "key", "zipped");
			xml.setContent(key + "touchpointData/instructions/instruction", "true");

			xml.setString(key + "licenses", "size", "1");
			xml.setContent(key + "licenses/license", license);
			if (licenseUrl != null) {
				xml.setString(key + "licenses/license", "url", licenseUrl);
				xml.setString(key + "licenses/license", "uri", licenseUrl);
			}
			xml.setContent(key + "copyright", copyright);
			if (copyrightUrl != null) {
				xml.setString(key + "copyright", "url", copyrightUrl);
				xml.setString(key + "copyright", "uri", copyrightUrl);
			}

			// add group
			key = xml.createSection("/repository/units/unit");
			xml.setString(key, "id", ga + ".feature.group");
			setVersion(xml, key, artifact.version);
			xml.setString(key, "singleton", "false");

			xml.setString(key + "update", "id", ga + ".feature.group");
			xml.setString(key + "update", "severity", "0");
			xml.setString(key + "update", "range", "[0.0.0," + artifact.version + "]");

			addProperty(xml, key, "org.eclipse.equinox.p2.name", label);
			addProperty(xml, key, "org.eclipse.equinox.p2.provider", providerName);
			addProperty(xml, key, "org.eclipse.equinox.p2.description", description);
			addProperty(xml, key, "org.eclipse.equinox.p2.description.url", descriptionUrl);
			addProperty(xml, key, "org.eclipse.equinox.p2.type.group", "true");
			addPropertySize(xml, key);

			addProvides(xml, key, "org.eclipse.equinox.p2.iu", ga + ".feature.group", artifact.version);
			addProvidesSize(xml, key);

			final String subkey = addRequired(xml, key, ga + ".feature.jar", artifact.version, artifact.version);
			xml.setContent(subkey + "filter", "(org.eclipse.update.install.features=true)");
			Pom artifactPom = bnm.getPom(artifact.getId());
			if (artifactPom == null) {
				artifactPom = bnm.loadPom(loader, null, artifact);
			}

			for (final Id id : artifactPom.getRuntimeDependencies()) {
				if (id.getGA().equals(artifact.getGA()))
					continue;

				addRequired(xml, key, id.groupId + "." + id.artifactId, id.version, id.version);

				if (addArtifact(axml, id, false)) {

					// create an entry for the artifact
					final String unitKey = xml.createSection("/repository/units/unit");

					final InputStream mis = loader.findInputStream(id, "jar", "META-INF/MANIFEST.MF");
					final int len = mis.available();
					final byte data[] = new byte[len];
					mis.read(data);
					mis.close();
					final String content = new String(data, "utf-8");
					final ManifestInfo mi = new ManifestInfo(content);

					xml.setString(unitKey, "id", id.groupId + "." + id.artifactId);
					setVersion(xml, unitKey, id.version);

					if (!mi.getFullSymbolicName().endsWith("singleton:=true"))
						xml.setString(unitKey, "singleton", "false");

					xml.setString(unitKey + "update", "id", id.groupId + "." + id.artifactId);
					xml.setString(unitKey + "update", "range", "[0.0.0," + id.version + ")");
					xml.setString(unitKey + "update", "severity", "0");

					addProperty(xml, unitKey, "org.eclipse.equinox.p2.name", mi.getBundleName());
					addProperty(xml, unitKey, "org.eclipse.equinox.p2.provider", mi.getBundleVendor());
					addProvidesSize(xml, unitKey);

					addProvides(xml, unitKey, "org.eclipse.equinox.p2.iu", id.groupId + "." + id.artifactId,
							id.version);
					addProvides(xml, unitKey, "osgi.bundle", id.groupId + "." + id.artifactId, id.version);
					addProvides(xml, unitKey, "org.eclipse.equinox.p2.eclipse.type", "bundle", "1.0.0");
					addProvidesSize(xml, unitKey);

					for (final NameVersion nv : mi.getBundleMap().values()) {
						if (nv.getName().equals(id.groupId + "." + id.artifactId))
							continue;
						addRequired(xml, unitKey, nv.getName(), nv.getVersion(), "9" + nv.getVersion());
					}
					addRequiredSize(xml, unitKey);

					xml.setString(unitKey + "artifacts", "size", "1");
					xml.setString(unitKey + "artifacts/artifact", "classifier", "osgi.bundle");
					xml.setString(unitKey + "artifacts/artifact", "id", id.groupId + "." + id.artifactId);
					setVersion(xml, unitKey + "artifacts/artifact", id.version);

					xml.setString(unitKey + "touchpoint", "id", "org.eclipse.equinox.p2.osgi");
					xml.setString(unitKey + "touchpoint", "version", "1.0.0");

					xml.setString(unitKey + "touchpointData", "size", "1");
					xml.setString(unitKey + "touchpointData/instructions", "size", "1");
					xml.setString(unitKey + "touchpointData/instructions/instruction", "key", "manifest");
					xml.setContent(unitKey + "touchpointData/instructions/instruction", "Bundle-SymbolicName: "
							+ mi.getFullSymbolicName() + "\r\nBundle-Version: " + id.version + "\r\n");
				}
			}
			addRequiredSize(xml, key);

			xml.setString(key + "touchpoint", "id", "null");
			xml.setString(key + "touchpoint", "version", "0.0.0");

			xml.setString(key + "licenses", "size", "1");
			xml.setContent(key + "licenses/license", license);
			if (licenseUrl != null) {
				xml.setString(key + "licenses/license", "url", licenseUrl);
				xml.setString(key + "licenses/license", "uri", licenseUrl);
			}
			xml.setContent(key + "copyright", copyright);
			if (copyrightUrl != null) {
				xml.setString(key + "copyright", "url", copyrightUrl);
				xml.setString(key + "copyright", "uri", copyrightUrl);
			}

			lastKey = key;

			// add category to feature.xml
			key = sxml.createSection("/site/feature");
			sxml.setString(key, "url", "features/" + ga + "-" + artifact.version + ".jar");
			sxml.setString(key, "id", ga);
			setVersion(sxml, key, artifact.version);
			sxml.setString(key + "category", "id", label);

			// add unit for the category
			final String catUnitKey = xml.createSection("/repository/units/unit");
			xml.setString(catUnitKey, "id", label);
			setVersion(xml, catUnitKey, artifact.version);

			addProperty(xml, catUnitKey, "org.eclipse.equinox.p2.name", label);
			addProperty(xml, catUnitKey, "org.eclipse.equinox.p2.type.category", "true");
			addPropertySize(xml, catUnitKey);

			addProvides(xml, catUnitKey, "org.eclipse.equinox.p2.iu", label, artifact.version);
			addProvidesSize(xml, catUnitKey);

			addRequired(xml, catUnitKey, ga, artifact.version, artifact.version);
			addRequiredSize(xml, catUnitKey);

			xml.setString(catUnitKey + "touchpoint", "id", "null");
			xml.setString(catUnitKey + "touchpoint", "version", "0.0.0");
		}

		final int unitCount = xml.getSections("/repository/units/unit").size();
		xml.setString("/repository/units", "size", Integer.toString(unitCount));

		if (lastKey != null)
			xml.moveBehind(mainKey, lastKey);

		// System.out.println(xml.toString());
		// System.out.println(axml.toString());

		final FileOutputStream fos = new FileOutputStream(outfile);
		xml.write(fos);
		fos.close();

		final FileOutputStream afos = new FileOutputStream(aoutfile);
		axml.write(afos);
		afos.close();

		final FileOutputStream sfos = new FileOutputStream(soutfile);
		sxml.write(sfos);
		sfos.close();

		super.execute();
	}

	private boolean addArtifact(final XmlFile axml, final Id id, final boolean isFeature) throws Exception {
		if (added.contains(id.getId()))
			return false;

		added.add(id.getId());

		final File pFile = loader.findFile(id, "jar");
		final File outFile = new File(isFeature ? featureDir : pluginsDir,
				id.groupId + "." + id.artifactId + "-" + fixVersion(id.version) + ".jar");
		copyFile(log, pFile, outFile);

		final String key = axml.createSection("/repository/artifacts/artifact");
		axml.setString(key, "classifier", isFeature ? "org.eclipse.update.feature" : "osgi.bundle");
		axml.setString(key, "id", id.groupId + "." + id.artifactId);
		setVersion(axml, key, id.version);

		addProperty(axml, key, "artifact.size", Long.toString(outFile.length()));
		addProperty(axml, key, "download.size", Long.toString(outFile.length()));

		final String md5 = calc(MessageDigest.getInstance("MD5"), outFile);
		addProperty(axml, key, "download.md5", md5);

		if (isFeature)
			addProperty(axml, key, "download.contentType", "application/zip");

		addPropertySize(axml, key);

		return true;
	}

	private static void addRequiredSize(final XmlFile xml, final String key) {
		final int size = xml.getSections(key + "requires/required").size();
		if (size > 0)
			xml.setString(key + "requires", "size", Integer.toString(size));
	}

	private static String addRequired(final XmlFile xml, final String key, final String name, final String from,
			final String to) {
		final String r = xml.createSection(key + "requires/required");
		xml.setString(r, "namespace", "org.eclipse.equinox.p2.iu");
		xml.setString(r, "name", name);
		String vv;
		if (from == null)
			vv = "0.0.0";
		else
			vv = "[" + fixVersion(from) + "," + fixVersion(to) + "]";

		xml.setString(r, "range", vv);
		return r;
	}

	private static void addProvidesSize(final XmlFile xml, final String key) {
		final int size = xml.getSections(key + "provides/provided").size();
		xml.setString(key + "provides", "size", Integer.toString(size));
	}

	private static void addProvides(final XmlFile xml, final String key, final String namespace, final String name,
			final String version) {
		final String p = xml.createSection(key + "provides/provided");
		xml.setString(p, "namespace", namespace);
		xml.setString(p, "name", name);
		setVersion(xml, p, version);
	}

	private static void addPropertySize(final XmlFile xml, final String key) {
		final int size = xml.getSections(key + "properties/property").size();
		xml.setString(key + "properties", "size", Integer.toString(size));
	}

	private static void addProperty(final XmlFile xml, final String key, final String name, final String value) {
		if (value == null)
			return;

		final String p = xml.createSection(key + "properties/property");
		xml.setString(p, "name", name);
		xml.setString(p, "value", value);
	}

	public static void setVersion(final XmlFile xml, final String key, final String version) {
		xml.setString(key, "version", fixVersion(version));
	}

	private static String fixVersion(final String version) {
		if (version.endsWith("SNAPSHOT"))
			return version.substring(0, version.length() - 9);
		return version;
	}

}
