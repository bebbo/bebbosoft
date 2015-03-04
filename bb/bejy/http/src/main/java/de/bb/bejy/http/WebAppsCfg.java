/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/WebAppsCfg.java,v $
 * $Revision: 1.8 $
 * $Date: 2014/10/21 20:36:11 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class WebAppsCfg extends Configurable implements Configurator, Runnable {
    private final static String PROPERTIES[][] = { { "path", "path to webapps folder" }, { "urlPath", "url prefix" },
            { "workDir", "a work folder" } };

    Host vhost;

    private volatile long nextRun;
    private int checkFrequency = 5000;

    private long last;

    private File webAppsDir;

    public WebAppsCfg() {
        init("webapps", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "webapps";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "to load all WAR files and unpacked WAR files in the specified directory";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "webapps";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.http.webapps";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy.http.host";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getRequired()
     */
    public String getRequired() {
        return null;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#create()
     */
    public de.bb.bejy.Configurable create() {
        return new WebAppsCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    public void activate(LogFile logFile) throws Exception {
        String path = this.getProperty("path");
        if (path != null)
            webAppsDir = new File(path);
        if (nextRun > 0) {
            logFile.writeDate("loading webapps: ");
            HashSet<String> remaining = new HashSet<String>();
            remaining.addAll(this.childNames());
            try {
                path = webAppsDir.getAbsolutePath();
                String[] ff = webAppsDir.list();
                if (ff != null) {
                    final TreeSet<String> sorted = new TreeSet<String>();
                    for (final String f : ff) {
                        sorted.add(f);
                    }

                    for (final String filename : sorted) {
                        if (".bejy_tiger".equals(filename))
                            continue;

                        // skip unpacked folders
                        if (sorted.contains(filename + ".link") || sorted.contains(filename + ".war"))
                            continue;

                        try {
                            String name = filename;
                            HttpContext hc = new WebAppContext();

                            final String[] s3 = resolvePath(new File(path, name));
                            String sname = s3[0];
                            final String spath = s3[1];
                            final String sfolder = s3[2];

                            if (sname.equals("ROOT") || sname.equals("ROOT.link"))
                                sname = "";

                            long age = new File(spath).lastModified();

                            if (spath.equals(sfolder)) {
                                long ageWebXml = new File(sfolder, "WEB-INF/this.xml").lastModified();
                                if (ageWebXml > age)
                                    age = ageWebXml;
                            }


                            hc.setProperty("path", spath);
                            hc.setProperty("folder", sfolder);
                            hc.setProperty("urlPath", this.getProperty("urlPath"));
                            hc.setProperty("workDir", this.getProperty("workDir"));

                            name = "/" + sname;
                            hc.setProperty("alias", name);
                            remaining.remove(name);

                            Configurable oo = this.getChild(name);

                            if (name.endsWith("/"))
                                name = name.substring(0, name.length() - 1);

                            // only load new and expired stuff
                            if (oo instanceof HttpContext) {
                                HttpContext oldContext = (HttpContext) oo;
                                if (oldContext.loadTime == age) {
                                    vhost.contexts.put(name, oldContext);
                                    continue;
                                }
                                this.remove(oo);
                            }

                            hc.sContext = name;
                            vhost.activateContext(logFile, hc);
                            this.addChild(hc.getName(), hc);
                            hc.loadTime = age;
                        } catch (Exception ex) {
                            logFile.writeDate("cannot load " + filename + " in " + getName() + ": " + ex.getMessage());
                        }
                    }
                }
                for (Iterator<String> r = remaining.iterator(); r.hasNext();) {
                    String name = r.next();
                    logFile.writeDate("unloading: " + name);
                    this.remove(this.getChild(name));
                }

                logFile.writeDate("success webapps: " + this.getName());

                last = System.currentTimeMillis();
            } catch (Exception ex) {
                logFile.writeDate("cannot load " + this.getName() + ": " + ex.getMessage());
            }
        }
        if (webAppsDir != null) {
            nextRun = System.currentTimeMillis() + 500;
            Config.getCron().runIn("check " + this.getName(), this, 500);
        }
    }

    @Override
    public synchronized void deactivate(LogFile logFile) throws Exception {
        nextRun = Long.MAX_VALUE;
        super.deactivate(logFile);
    }

    public synchronized void run() {
        last = checkForUpdate(getParent(), webAppsDir, last);

        nextRun = System.currentTimeMillis() + checkFrequency;
        Config.getCron().runIn("check " + this.getName(), this, checkFrequency);
    }

    /**
     * @param folder
     * @return
     */
    private static long checkForUpdate(Configurable c, File folder, long last) {
        long t = last;
        String files[] = folder.list();
        for (int i = 0; i < files.length; ++i) {
            String webAppName = files[i];
            File f = new File(folder, files[i]);
            if (webAppName.endsWith(".link")) {
                FileReader fr = null;
                try {
                    fr = new FileReader(f);
                    BufferedReader br = new BufferedReader(fr);
                    String line = br.readLine();
                    f = new File(line);
                    webAppName = webAppName.substring(0, webAppName.length() - 5);
                } catch (IOException e) {
                }
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e) {
                    }
                }
            }
            File wf = new File(f, "WEB-INF/web.xml");
            if (!wf.exists())
                wf = f;

            long ft = wf.lastModified();
            if (ft > t)
                t = ft;
        }
        try {
            if (t > last)
                c.update(Config.getLogFile());
        } catch (Exception e) {
        }

        return t;
    }

    /**
     * The configure path is one of * a folder which contains the ear/war * an archive which contains the ear/war * a
     * link file which refers to a folder or archive
     * 
     * @param path
     * @return
     * @throws IOException
     */

    public static String[] resolvePath(File path) throws IOException {
        // where the result must reside
        String name = null;
        File parent = path.getParentFile();
        File folder = null;

        // use the .link file content
        if (path.getName().endsWith(".link")) {
            // update the name
            name = path.getName();
            name = name.substring(0, name.length() - 5);
            FileReader fr = new FileReader(path);
            final BufferedReader br = new BufferedReader(fr);
            String referenced = br.readLine();
            br.close();

            path = new File(referenced);
            if (!path.exists()) {
                path = new File(parent, referenced);
                if (!path.exists())
                    throw new IOException("invalid link to: " + referenced);
            }
        }

        // reference is unpacked so update the parent
        if (path.isDirectory()) {
            parent = path.getParentFile();
            folder = path;
            if (name == null)
                name = folder.getName();
        } else {
            if (name == null) {
                name = path.getName();
                name = name.substring(0, name.length() - 4);
            }
            folder = new File(parent, name);
        }

        return new String[] { name, path.toString(), folder.toString() };
    }

}

/******************************************************************************
 * $Log: WebAppsCfg.java,v $ Revision 1.8 2014/10/21 20:36:11 bebbo
 * 
 * @R add support for relativ paths in link files
 *
 *    Revision 1.7 2014/06/23 15:38:46 bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles Revision 1.6 2013/11/28 10:30:58 bebbo
 * 
 * @B resolving a linked *.jar file now results into a local unpacked folder Revision 1.5 2012/12/15 19:38:56 bebbo
 * 
 * @I refactoring Revision 1.4 2012/07/18 06:44:44 bebbo
 * 
 * @I typified Revision 1.3 2003/06/20 09:09:38 bebbo
 * 
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.2 2003/06/17 12:09:56 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.1 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 */
