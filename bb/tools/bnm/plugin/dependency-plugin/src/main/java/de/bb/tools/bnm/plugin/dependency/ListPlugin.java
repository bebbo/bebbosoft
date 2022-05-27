package de.bb.tools.bnm.plugin.dependency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;

public class ListPlugin extends AbstractPlugin {

    private final static String SCOPES[] = { "COMPILE", "PROVIDED", "RUNTIME",
            "TEST", "SYSTEM" };

    @Config("outputFile")
    private String outputFile;

    private Log log;

    private FileOutputStream fos;

    @Override
    public void execute() throws Exception {
        log = getLog();

        fos = null;

        try {

            if (outputFile != null) {
                File dir = new File(outputFile);
                dir = dir.getParentFile();
                if (!dir.exists())
                    dir.mkdirs();
                fos = new FileOutputStream(outputFile);
            }

            write("The following files have been resolved:");

            HashSet<String> done = new HashSet<String>();
            for (String scope : SCOPES) {
                ArrayList<Object> deps = project.getDependencyTree(scope);
                recurse(deps, done);
            }

        } finally {
            if (fos != null)
                fos.close();
        }
    }

    private void write(String msg) throws IOException {
        if (fos != null) {
            fos.write(msg.getBytes());
            fos.write(0xd);
            fos.write(0xa);
        } else {
            log.info(msg);
        }
    }

    private void recurse(ArrayList<Object> deps, HashSet<String> done)
            throws IOException {
        for (Object o : deps) {
            if (o instanceof Dependency) {
                Dependency dep = (Dependency) o;
                if (done.contains(dep.getId()))
                    continue;
                
                done.add(dep.getId());
                String scope = dep.scope;
                if (scope == null)
                    scope = "compile";
                String msg = "\t" + dep.getId() + ":" + scope;
                write(msg);
                continue;
            }
            if (o instanceof ArrayList) {
                ArrayList<Object> al = (ArrayList<Object>) o;
                recurse(al, done);
                continue;
            }

        }
    }
}
