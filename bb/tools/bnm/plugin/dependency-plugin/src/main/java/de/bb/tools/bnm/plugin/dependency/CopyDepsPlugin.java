package de.bb.tools.bnm.plugin.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.tools.bnm.model.Dependency;

public class CopyDepsPlugin extends AbstractPlugin {

    private final static String SCOPES[] = { "COMPILE", "PROVIDED", "RUNTIME",
            "TEST", "SYSTEM" };

    @Config("outputDirectory")
    @Default("${project.build.directory}/dependency")
    private File outputDirectory;

    @Config("includeGroupIds")
    private String includeGroupIds;
    
    private Log log;

    private Loader loader;

    private String[] includeGroups;

    @Override
    public void execute() throws Exception {
        log = getLog();
        loader = project.getLoader();

        outputDirectory.mkdirs();

        includeGroups = null;
        if (includeGroupIds != null) includeGroups = includeGroupIds.split(",");
        
        HashSet<String> done = new HashSet<String>();
        for (String scope : SCOPES) {
            ArrayList<Object> deps = project.getDependencyTree(scope);
            recurse(deps, done);
        }

    }

    private void recurse(ArrayList<Object> deps, HashSet<String> done)
            throws Exception {
        for (Object o : deps) {
            if (o instanceof Dependency) {
                Dependency dep = (Dependency) o;
                if (done.contains(dep.getId()))
                    continue;

                done.add(dep.getId());
                copyFile(dep);
                continue;
            }
            if (o instanceof ArrayList) {
                ArrayList<Object> al = (ArrayList<Object>) o;
                recurse(al, done);
                continue;
            }

        }
    }

    private void copyFile(Dependency dep) throws Exception {
        
        incGroup:
        if (includeGroups != null) {
            for (String group : includeGroups) {
                if (dep.groupId.startsWith(group))
                    break incGroup;
            }
            return;
        }
        
        File cpElement = loader.findFile(dep, "jar");
        File outFile = new File(outputDirectory, dep.artifactId + "-"
                + dep.version + ".jar");
        if (cpElement.lastModified() <= outFile.lastModified())
            return;
        
        copyFile(log, cpElement, outFile);
    }
}
