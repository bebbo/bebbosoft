package de.bb.tools.bnm.plugin.antrun;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;

public class AntrunPlugin extends AbstractPlugin {

    @Config("target")
    private String target;

    @Override
    public void execute() throws Exception {
        if (target == null)
            throw new Exception("plugin has no target in configuration");

        Loader loader = project.getLoader();

        String cp = "";

        if (dependencies != null)
            for (Dependency dep : dependencies) {
                if ("pom".equals(dep.type))
                    continue;

                Pom depPom = project.getBnm().loadPom(loader, null, dep);

                for (Id id : depPom.getRuntimeDependencies()) {
                    File cpElement = loader.findFile(id, "jar");
                    if (cp.length() > 0)
                        cp += File.pathSeparator;
                    cp += cpElement.getAbsolutePath();
                }
            }

        File tempDir = new File(currentDir, "target");
        tempDir.mkdirs();
        File tempFile = File.createTempFile("bnm", ".xml", tempDir);
        try {

            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(("<project basedir=\"" + currentDir.getAbsolutePath() + "\"><target name=\"bnm\">\r\n")
                    .getBytes());
            fos.write(target.getBytes());
            fos.write("\r\n</target></project>".getBytes());
            fos.close();

            String cmd[] = { "java", "-cp", cp, "org.apache.tools.ant.Main",
                    "-f", tempFile.getAbsolutePath(), "bnm" };

            Log log = getLog();

            Process proc = Runtime.getRuntime().exec(cmd, null, currentDir);
            InputStream is = proc.getInputStream();
            BufferedReader ir = new BufferedReader(new InputStreamReader(is));
            InputStream es = proc.getErrorStream();
            BufferedReader er = new BufferedReader(new InputStreamReader(es));
            int ret = -1;
            for (;;) {
                String line = null;
                if (is.available() > 0) {
                    line = ir.readLine();
                    if (line != null)
                        log.info(line);
                }
                String eline = null;
                if (es.available() > 0) {
                    eline = er.readLine();
                    if (eline != null)
                        log.error(eline);
                }

                if (line == null && eline == null) {
                    try {
                        ret = proc.exitValue();
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {

                    }
                }
            }
            for(String line = ir.readLine(); line != null; line = ir.readLine()) {
                log.info(line);
            }
            
            for(String line = er.readLine(); line != null; line = er.readLine()) {
                log.info(line);
            }
            
            if (ret != 0) {
                log.error("ant returned: " + ret);
            }
            proc.destroy();

        } finally {
            tempFile.delete();
        }
    }

}
