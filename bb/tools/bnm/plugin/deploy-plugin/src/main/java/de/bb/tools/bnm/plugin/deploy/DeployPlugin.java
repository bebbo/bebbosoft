package de.bb.tools.bnm.plugin.deploy;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.tools.bnm.annotiation.EPom;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.setting.Server;
import de.bb.tools.bnm.setting.Settings;
import de.bb.util.Process;

/**
 * Simple implementation to deploy what' inside the local repository. Since this runs after install, the local
 * repository is accurate.
 * 
 * @author stefan franke
 * 
 */
public class DeployPlugin extends AbstractPlugin {

    @EPom("project.distributionManagement.repository.layout")
    @Default("default")
    protected String layout;

    @EPom("project.distributionManagement.repository.url")
    protected String url;

    @EPom("project.distributionManagement.repository.id")
    protected String id;

    /**
     * Do the deployment.
     */
    @Override
    public void execute() throws Exception {
        if (!"default".equals(layout))
            throw new Exception("only default layout is supported yet");

        if (!url.startsWith("scpexe://"))
            throw new Exception("only scpexe:// is supported yet");

        // get the local and repo paths
        Project pom = project.getProject();
        Loader loader = project.getLoader();
        File fullModule = loader.findFile(pom, "pom");
        File fullModulePath = fullModule.getParentFile();
        int repoLen = Loader.getRepoPath().toString().length();
        File relModulePath = new File(fullModulePath.toString().substring(repoLen));

        String files[] = fullModulePath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                return f.isFile();
            }
        });

        if (files == null || files.length == 0)
            return;

        // extract host and port
        int slash = url.indexOf('/', 9);
        if (slash < 0)
            throw new Exception("can't find host in " + url);
        String host = url.substring(9, slash);
        String path = url.substring(slash);

        int colon = host.indexOf(':');
        String port = null;
        if (colon > 0) {
            port = host.substring(colon + 1);
            host = host.substring(0, colon);
        }

        // setup user, commands and arguments
        String userName = System.getProperty("user.name");
        String scp = "scp";
        String ssh = "ssh";
        String scpArgs = " ";
        String sshArgs = " ";

        // use the server config, if any
        Server serverCfg = null;
        if (id != null) {
            Settings settings = project.getSettings();
            for (Server server : settings.servers) {
                if (server.id.equals(id)) {
                    serverCfg = server;
                    break;
                }
            }
        }

        // apply server config.
        if (serverCfg != null) {
            if (serverCfg.username != null)
                userName = serverCfg.username;
            if (serverCfg.configuration != null) {
                Map<String, String> cfg = serverCfg.configuration;
                String val = cfg.get("sshExecutable");
                if (val != null)
                    ssh = val;
                val = cfg.get("scpExecutable");
                if (val != null)
                    scp = val;
                val = cfg.get("sshArgs");
                if (val != null)
                    sshArgs = val;
                val = cfg.get("scpArgs");
                if (val != null)
                    scpArgs = val;
            }
        }

        String scpPart = "";
        String sshPart = "";
        if (port != null) {
            scpPart = "-P " + port + " ";
            sshPart = "-p " + port + " ";
        }
        scp += " " + scpArgs;
        scpPart = " " + userName + "@" + host + ":" + path;
        ssh += " " + sshArgs + " " + userName + "@" + host;

        HashSet<String> dirsCreated = new HashSet<String>();
        // deploy all files
        for (String s : files) {
            File destFile = new File(relModulePath, s);
            String destDir = new File(path, destFile.toString()).getParent().replace('\\', '/');
            if (!dirsCreated.contains(destDir)) {
                createDir(ssh, destDir);
                dirsCreated.add(destDir);
            }
            String dest = destFile.toString().replace('\\', '/');
            deployFile(scp, scpPart, new File(fullModulePath, s), dest);
        }

    }

    /**
     * Create remote folders recursively.
     * 
     * @param destDir
     * @throws IOException
     */
    protected void createDir(String ssh, String destDir) throws IOException {
        String cmd = ssh + " mkdir -p " + destDir;
        getLog().info("[deploy] " + cmd);
        int result = Process.execute(cmd, null, null, null);
        if (result != 0)
            throw new IOException("createDir returned " + result + " with " + cmd);
    }

    /**
     * deploy a single file. Look at the cmd and see wht else is used.
     * 
     * @param scpPart
     * @param scp
     * @param srcFile
     *            the source file
     * @param dest
     *            the destination path
     * @throws IOException
     */
    protected void deployFile(String scp, String scpPart, File srcFile, String dest) throws IOException {
        String cmd = scp + " " + srcFile + " " + scpPart + dest;
        getLog().info("[deploy] " + cmd);
        int result = Process.execute(cmd, null, null, null);
        if (result != 0)
            throw new IOException("deployFile returned " + result + " with " + cmd);
    }

}
