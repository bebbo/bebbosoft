/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.install-plugin.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.install-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.jar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.util.FileBrowser;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;

/**
 * Base class for creating a jar from project classes.
 */
public abstract class AbstractJarPlugin extends AbstractPlugin {

    @Property("${project.build.directory}")
    protected File outputDirectory;

    @Config("outputDirectory")
    private File cfgOutputDirectory;

    @Config("classifier")
    protected String classifier;

    // @Config("useDefaultManifestFile")
    // private boolean useDefaultManifestFile;

    @Config("forceCreation")
    protected boolean forceCreation;

    @Config("finalName")
    protected String finalName;

    long maxDate;

    private File jarFile;

    protected Map<String, File> files = new SingleMap<String, File>();

    /**
     * Return the specific output directory to serve as the root for the
     * archive.
     * 
     * @throws Exception
     */
    protected abstract File getContentDirectory() throws Exception;

    protected abstract String getType();

    public void execute() throws Exception {
        maxDate = 0;
        makeName();

        jarFile = new File(cfgOutputDirectory != null ? cfgOutputDirectory
                : outputDirectory, finalName);

        collectFiles();
        createJar();
    }

    protected void createJar() throws IOException {
        if (files.size() == 0) {
            getLog().warn(
                    "JAR will be empty - no content was marked for inclusion!");
        }
        if (forceCreation || maxDate > jarFile.lastModified()) {
            byte buffer[] = new byte[0x10000];
            Log log = getLog();
            log.debug("JAR: creating JAR file: " + jarFile.getAbsolutePath());
            File path = jarFile.getParentFile();
            if (!path.exists())
                path.mkdirs();
            FileOutputStream fos = new FileOutputStream(jarFile);
            JarOutputStream jos = new JarOutputStream(fos);

            boolean createManifest = true;
            if (files.size() == 0) {
                JarEntry jarAdd = new JarEntry(".empty");
                jarAdd.setTime(System.currentTimeMillis());
                jos.putNextEntry(jarAdd);
            } else {
                for (Map.Entry<String, File> file : files.entrySet()) {
                    log.debug("JAR: adding " + file);
                    final String fileName = file.getKey();
                    JarEntry jarAdd = new JarEntry(fileName);
                    File jf = file.getValue();
                    jarAdd.setTime(jf.lastModified());
                    jos.putNextEntry(jarAdd);

                    // Write file to archive
                    InputStream fis = new FileInputStream(jf);
                    if ("META-INF/MANIFEST.MF".equals(fileName)) {
                        fis = patchManifest(fis);
                        createManifest = false;
                    }
                    copyData(buffer, jos, fis);
                    fis.close();
                }
            }

            if (createManifest) {
                InputStream fis = patchManifest(null);
                if (fis != null) {
                    JarEntry jarAdd = new JarEntry("META-INF/MANIFEST.MF");
                    jos.putNextEntry(jarAdd);
                    copyData(buffer, jos, fis);
                    fis.close();
                }
            }

            jos.close();
            fos.close();

            project.attachFile(jarFile, classifier);
            jarFile.getParentFile().setLastModified(System.currentTimeMillis());

        } else {
            getLog().info("file is up to date: " + jarFile.getAbsolutePath());
        }
    }

    private InputStream patchManifest(InputStream fis) throws IOException {
        final String archive = configuration.get("archive");
        if (archive == null || true)
            return fis;

        final XmlFile xml = new XmlFile();
        xml.readString("<x>" + archive + "</x>");
        final Vector<String> me = xml.getSections("/x/manifestEntries");
        if (me.isEmpty())
            return fis;

        if (fis != null) {
            // read existing manifest
            fis.close();
        }
        // create new file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        return new ByteArrayInputStream(bos.toByteArray());
    }

    private void copyData(byte[] buffer, OutputStream jos, InputStream fis)
            throws IOException {
        for (;;) {
            int nRead = fis.read(buffer, 0, buffer.length);
            if (nRead <= 0)
                break;
            jos.write(buffer, 0, nRead);
        }
    }

    protected void collectFiles() throws Exception {
        addFiles(getContentDirectory(), "", configuration.get("includes"),
                configuration.get("excludes"));

    }

    protected void addFiles(final File contentDirectory, final String folder, final String includes,
            final String excludes) throws IOException {
        FileBrowser fb = new FileBrowser() {
            protected void handleFile(String path, String name) {
                String fp = path + "/" + name;
                fp = fp.substring(1);
                files.put(folder + fp, new File(contentDirectory, fp));
                long t = new File(getBaseDir(), fp).lastModified();
                if (t > maxDate)
                    maxDate = t;
            }
        };
        if (contentDirectory.exists()) {
            if (includes != null) {
                final XmlFile xmlFile = new XmlFile();
                xmlFile.readString("<x>" + includes + "</x>");
                for (final String sec : xmlFile.getSections("/x/include")) {
                    String pattern = xmlFile.getContent(sec);
                    if (pattern.length() > 0 && pattern.charAt(0) != '/')
                        pattern = "/" + pattern;
                    fb.addInclude(pattern);
                }
            }

            fb.addExclude("**/package.html");
            fb.addExclude("**/.svn**");

            if (excludes != null) {
                final XmlFile xmlFile = new XmlFile();
                xmlFile.readString("<x>" + excludes + "</x>");
                for (final String sec : xmlFile.getSections("/x/exclude")) {
                    String pattern = xmlFile.getContent(sec);
                    if (pattern.length() > 0 && pattern.charAt(0) != '/')
                        pattern = "/" + pattern;
                    fb.addExclude(pattern);
                }
            }
            fb.scan(contentDirectory.getAbsolutePath(), true);
        }

    }

    protected abstract void makeName() throws Exception;

}
