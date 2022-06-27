package de.bb.tools.bnm.plugin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import de.bb.io.IOUtils;
import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.eclipse.versioning.dumb.ManifestInfo;
import de.bb.tools.bnm.model.Id;
import de.bb.util.FileBrowser;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;

public class ImportPluginsPlugin extends AbstractPlugin {

	private static boolean done;

	MultiMap<String, ManifestInfo> name2manifest = new MultiMap<>();
	Map<File, String> file2name = new SingleMap<>();
	Map<String, File> base2file = new SingleMap<>();
	
	public void execute() throws Exception {

		// run only once.
		if (done)
			return;
		done = true;

		Log log = getLog();
		String eclipseDir = System.getProperty("eclipse.dir");
		log.debug("eclipse.dir=" + eclipseDir);

		File plugins = new File(eclipseDir, "plugins");
		if (!plugins.exists())
			throw new Exception(
					"not in a valid Eclipse directory given via -Declipse.dir=... - plugins folder does not exists. " + plugins.getAbsolutePath());

		loadDeps(plugins);
		importPlugins(plugins);
	}
	
	private void importPlugins(File dir) throws IOException {
        Loader loader = project.getLoader();
        
        for (File source : base2file.values()) {
        	String name = file2name.get(source);
//        	if (!name.startsWith("org.eclipse"))
//        		continue;
        	
        	ManifestInfo mi = name2manifest.get(name);
        	String version = mi.getVersion();
        	
        	getLog().info("importing " + name);
        	int lastUnder = name.lastIndexOf('_');

        	if (lastUnder < 0)
        		continue;
        	
            String base = name.substring(0, lastUnder);
			Id id = new Id("org.eclipse.platform:" + base + ":" + name.substring(lastUnder + 1));
            
    		File pomFile = loader.makeRepoPath(id, null, "pom");
    		pomFile.getParentFile().mkdirs();
    		try (FileWriter fos = new FileWriter(pomFile)) {
    			fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    					+ "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\" xsi:noNamespaceSchemaLocation=\"http://maven.apache.org/POM/4.0.0\">\n"
    					+ "<modelVersion>4.0.0</modelVersion>\n"
    					+ "  <groupId>org.eclipse.platform</groupId>\n"
    					+ "  <artifactId>" + base + "</artifactId>\n"
    					+ "  <version>" + version + "</version>\n"
						+ "  <dependencies>\n"
    					);
    			
    			for (String ref : mi.getReferences()) {
    				if (ref.equals(base))
    					continue;
    				
    				File fn = base2file.get(ref);
    				if (fn == null)
    					continue;
    				ManifestInfo refMi = name2manifest.get(file2name.get(fn));
    				String refVersion = refMi.getVersion();
    				fos.write("    <dependency>\n"
    						+ "      <groupId>org.eclipse.platform</groupId>\n"
    						+ "      <artifactId>" + ref + "</artifactId>\n"
    						+ "      <version>" + refVersion + "</version>\n"
    						+ "    </dependency>\n");
    			}
    			fos.write("  </dependencies>\n"
    					+ "</project>\n");
    		}
    		File jarFile = loader.makeRepoPath(id, null, "jar");
    		if (source.isFile()) {
    			copyFile(getLog(), source, jarFile);
    		} else {
    			List<String> files = new ArrayList<>();
    			FileBrowser fb = new FileBrowser() {
					@Override
					protected void handleFile(String path, String file) {
						files.add(path + "/" + file);
					}
				};
				fb.scan(source.getAbsolutePath(), true);
				
				try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))){
					for (String fileName : files) {
	                    JarEntry jarAdd = new JarEntry(fileName);
	                    File jf = new File(source, fileName);
	                    jarAdd.setTime(jf.lastModified());
	                    jos.putNextEntry(jarAdd);
	
	                    // Write file to archive
	                    InputStream fis = new FileInputStream(jf);
	                    IOUtils.copy(fis, jos, jf.length());
	                    fis.close();
					}
				}
    		}
        }
		
	}

	private void loadDeps(File dir) throws IOException {
		for (File p : dir.listFiles()) {
			String name = p.getName();
			if (p.isDirectory()) {
				File mani = new File(p, "META-INF/MANIFEST.MF");
				if (mani.exists()) {
					getLog().info("loading dir " + p);
					name2manifest.put(name, new ManifestInfo(mani));
				}
			} else {
				getLog().info("loading jar " + p);
				try (JarFile jf = new JarFile(p)) {
					ZipEntry ze = jf.getEntry("META-INF/MANIFEST.MF");
					InputStream is = jf.getInputStream(ze);
					byte[] data = new byte[is.available()];
					IOUtils.readFully(is, data);
					is.close();
					name = name.substring(0, name.length() - 4);
					name2manifest.put(name, new ManifestInfo(new String(data)));
				} catch (IOException ioe) {
					getLog().error(ioe.toString());
				}
			}
			file2name.put(p, name);
			int under = name.lastIndexOf('_');
			String base = under > 0 ? name.substring(0, under) : name;
			File exist = base2file.get(base);
			if (exist == null || exist.getName().compareTo(name) < 0)
				base2file.put(base, p);
		}
	}

}
