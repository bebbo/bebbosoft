package de.bb.tools.bnm.plugin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.model.Resource;
import de.bb.tools.bnm.plugin.jar.JarPlugin;
import de.bb.util.XmlFile;

public class FeaturePlugin extends JarPlugin {
    public void execute() throws Exception {

        // locate feature.xml file
        File feature = null;
        final Project epom = project.getEffectivePom();
        final ArrayList<? extends Resource> resources = epom.build.resources;
        if (resources != null) {
            for (final Resource r : resources) {
                if (r.directory == null)
                    continue;
                final File dir = new File(currentDir, r.directory);
                final File file = new File(dir, "feature.xml");
                if (file.exists()) {
                    feature = file;
                    break;
                }
            }
        }
        if (feature == null)
            throw new Exception("feature.xml not found in resource paths");

        final ArrayList<Id> artifacts = this.project.getRuntimeDependencies();
        final Loader loader = project.getLoader();

        // check if uptodate
        final File outfile = new File(getContentDirectory(), "feature.xml");
        boolean uptodate = outfile.exists() && outfile.lastModified() > feature.lastModified()
                && outfile.lastModified() > new File(currentDir, "pom.xml").lastModified();
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
            return;
        }

        // read the feature.xml template file and fill it
        final XmlFile xml = new XmlFile();
        xml.readFile(feature.getAbsolutePath());

        xml.setString("/feature", "id", epom.groupId + ':' + epom.artifactId);
        xml.setString("/feature", "version", epom.version);

        for (final Id artifact : artifacts) {
            // skip self
            if (artifact.getGA().equals(epom.getGA()))
                continue;
            final File jar = loader.findFile(artifact, "jar");
            if (jar.exists()) {
                long fsize = jar.length();
                final String key = xml.createSection("/feature/plugin");
                xml.setString(key, "id", artifact.groupId + "." + artifact.artifactId);
                xml.setString(key, "download-size", "" + fsize);
                xml.setString(key, "install-size", "" + fsize);
                xml.setString(key, "unpack", "false");
                xml.setString(key, "version", artifact.version);
            }
        }
        final FileOutputStream fos = new FileOutputStream(outfile);
        xml.write(fos);
        fos.close();

        super.execute();
    }

}
