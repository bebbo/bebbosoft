/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/j2ee/src/main/java/de/bb/bejy/j2ee/EntAppsCfg.java,v $
 * $Revision: 1.2 $
 * $Date: 2013/11/28 10:42:01 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003-2011.
 *
 */
package de.bb.bejy.j2ee;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import de.bb.bejy.Configurable;
import de.bb.bejy.http.WebAppsCfg;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class EntAppsCfg extends WebAppsCfg {
    private final static String PROPERTIES[][] = {{"path", "path to entapps folder"}, {"urlPath", "url prefix"},
            {"workDir", "a work folder"}};

    public EntAppsCfg() {
        init("entapps", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "entapps";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "to load all EAR jar files/unpacked EAR files in the specified directory";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "entapps";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.http.entapps";
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
        return new EntAppsCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    public void activate(LogFile logFile) throws Exception {
        logFile.writeDate("loading enterprise apps:");
        Registry.logFile = logFile;
        HashMap<String, Ear> remaining = new HashMap<String, Ear>();
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            if (c instanceof Ear) {
                remaining.put(c.getName(), (Ear) c);
            }
        }
        try {
            String path = this.getProperty("path");
            if (path != null) {
                File dir = new File(path);
                path = dir.getAbsolutePath();
                String[] ff = dir.list();
                if (ff != null)
                    for (int j = 0; j < ff.length; ++j) {
                        String name = ff[j];
                        if (".bejy_tiger".equals(name))
                            continue;
                        if (new File(dir, name + ".link").exists())
                            continue;

                        try {
                            Ear entApp = new Ear();

                            final String[] s3 = resolvePath(new File(path, name));
                            final String sname = s3[0];
                            final String spath = s3[1];
                            final String sfolder = s3[2];
                            
                            long age = new File(spath).lastModified();

                            if (spath.equals(sfolder)) {
                                long ageAppXml = new File(sfolder, "META-INF/application.xml").lastModified();
                                if (ageAppXml > age)
                                    age = ageAppXml;
                            }

                            if (entApp.getProperty("alias") == null)
                                entApp.setProperty("alias", sname);

                            entApp.setProperty("path", spath);
                            entApp.setProperty("folder", sfolder);
                            entApp.setProperty("uriPath", this.getProperty("uriPath"));
                            entApp.setProperty("workDir", this.getProperty("workDir"));

                            remaining.remove(name);

                            Configurable oo = remaining.get(name);

                            // only load new and expired stuff
                            if (oo != null && oo instanceof Ear) {
                                Ear oldEar = (Ear) oo;
                                if (oldEar.loadTime == age) {
                                    addChild(name, oldEar);
                                    continue;
                                }
                                this.remove(oo);
                            }

                            entApp.setProperty("name", name);
                            this.addChild("ear", entApp);
                            entApp.activate(logFile);
                            entApp.loadTime = age;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            logFile.writeDate("cannot load " + ff[j] + " in " + getName() + ": " + ex.getMessage());
                        }
                    }
                 for (Configurable c : remaining.values()) {
                    String name = c.getProperty("name");
                    logFile.writeDate("unloading: " + name);
                    this.remove(c);
                }
            }
            logFile.writeDate("success enterprise applications: " + this.getName());
        } catch (Exception ex) {
            logFile.writeDate("cannot load " + this.getName() + ": " + ex.getMessage());
        }
    }

}

/******************************************************************************
 * $Log: EntAppsCfg.java,v $
 * Revision 1.2  2013/11/28 10:42:01  bebbo
 * @N development snapshot
 *
 * Revision 1.1  2012/07/18 09:06:57  bebbo
 * @N new
 * Revision 1.3 2003/06/20 09:09:38 bebbo
 */
