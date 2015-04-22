package de.bb.tools.bnm.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.model.Id;

public class CopyPlugin extends AbstractPlugin {

    @Config("artifactItems")
    private ArrayList<ArtifactItem> artifactItems;

    private Log log;

    private Loader loader;

    @Override
    public void execute() throws Exception {
        log = getLog();
        
        // lookup for version
        final ArrayList<Id> ids = project.getAllDependencies();
        final HashMap<String, String> id2v = new HashMap<String, String>();
        for (final Id id : ids) {
            if (id.version != null)
                id2v.put(id.getGA(), id.version);
        }
        
        
        loader = project.getLoader();
        for (final ArtifactItem ai : artifactItems) {
            if (ai.version == null)
                ai.version = id2v.get(ai.getGA());
            final File inFile = loader.findFile(ai, ai.type);
            if (inFile == null) {
                log.error("artifact " + ai  + " not found");
                throw new IOException("artifact " + ai  + " not found");
            }
            File outFile = new File(new File(project.getPath(), ai.outputDirectory), ai.destFileName);
            CopyDepsPlugin.copyFile(log, inFile, outFile);
        }
    }
}