package de.bb.tools.bnm.plugin.ant;

import java.io.File;
import java.util.Map;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;

public class AntPlugin extends AbstractPlugin {
    @Property("${project.build.outputDirectory}")
    private File classesDirectory;

    @Config("instructions")
    private Map<String, String> script;

    @Override
    public void execute() throws Exception {
        getLog().info("THE ANT PLUGIN IS NOT YET IMPLEMENTED!");

    }

}
