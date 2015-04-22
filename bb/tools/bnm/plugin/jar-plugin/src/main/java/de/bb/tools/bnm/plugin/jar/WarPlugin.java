package de.bb.tools.bnm.plugin.jar;

import java.io.File;

import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Project;
import de.bb.util.XmlFile;

public class WarPlugin extends JarPlugin {

    @Property("${project.build.directory}")
    private String directory;

    protected String getType() {
        return "war";
    }

    protected File getContentDirectory() throws Exception {
        final Project epom = project.getEffectivePom();
        final File dir = new File(directory, epom.groupId + "."
                + epom.artifactId);
        dir.mkdirs();
        return dir;
    }

    protected void collectFiles() throws Exception {
        addFiles(new File(project.getPath(), "src/main/webapp"), "",
                configuration.get("includes"), configuration.get("excludes"));

        addFiles(new File(outputDirectory, "classes/"), "WEB-INF/classes/", null, null);

        final String webResources = configuration.get("webResources");
        final XmlFile xml = new XmlFile();
        xml.readString("<x>" + webResources + "</x>");
        for (final String section : xml.getSections("/x/resource")) {
            final String src = xml.getContent(section + "directory");
            addFiles(new File(project.getPath(), src), "",
                    xml.getContent(section + "includes"),
                    xml.getContent(section + "excludes"));
        }
    }
}
