package de.bb.tools.bnm.plugin.dependency;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.model.Id;

public class UnpackPlugin extends AbstractPlugin {

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
            File outFolder = new File(ai.outputDirectory);
            if (outFolder.getAbsolutePath().length() != ai.outputDirectory.length())
                outFolder = new File(project.getPath(), ai.outputDirectory);
            
            final ZipFile zf = new ZipFile(inFile);
            
            try {
                byte buffer[] = new byte[0x8000];
                
                for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                    ZipEntry ze = e.nextElement();
                    if (ze.isDirectory())
                        continue;
                    File file = new File(outFolder, ze.getName());
                    file.getParentFile().mkdirs();
                    InputStream jis = zf.getInputStream(ze);
                    FileOutputStream fos = new FileOutputStream(file);
                    long sz = ze.getSize();
                    while (sz > 0) {
                        int canRead = buffer.length < sz ? buffer.length : (int)sz;
                        int read = jis.read(buffer, 0, canRead);
                        sz -= read;
                        fos.write(buffer, 0, read);
                    }
                    jis.close();
                    fos.close();
                }
            } finally {
                zf.close();
            }
            
            // CopyDepsPlugin.copyFile(log, inFile, outFile);
        }
    }
}