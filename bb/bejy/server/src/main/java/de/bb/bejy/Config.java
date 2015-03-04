/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Config.java,v $
 * $Revision: 1.48 $
 * $Date: 2014/09/22 09:24:39 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * config handling of bejy
 *
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 1994-2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import de.bb.unix.GUID;
import de.bb.util.LRUCache;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;
import de.bb.util.ZipClassLoader;

/**
 * Config of BEJY. Used to query common stuff.
 */
public class Config extends Configurable implements Configurator {
    private static String configFileName;
    /** the config singleton. */
    private static Config instance;
    // default log file, migth be changed by configuration
    private static LogFile defaultLog = new LogFile("*");
    // our configuration
    private XmlFile xml;
    // the configurator modules
    private static SingleMap<String, Configurator> configurators = new SingleMap<String, Configurator>();
    // keys are the extension point id, values the instance of the extended object
    private static MultiMap<String, Configurator> dependencies = new MultiMap<String, Configurator>();
    // contains all which are successful configured
    private static SingleMap<String, Configurator> configured = new SingleMap<String, Configurator>();
    // the configured modules
    private static LinkedList<Configurable> modules = new LinkedList<Configurable>();
    // unique global elements
    private static SingleMap<String, String> unique = new SingleMap<String, String>();
    // keys are the extension point id, values the class names which can be applied here
    private static MultiMap<String, String> loadables = new MultiMap<String, String>();

    private static HashMap<Integer, ServerSocket> serverSockets = new HashMap<Integer, ServerSocket>();

    // properties to set uid and gid
    private final static String PROPERTIES[][] = {{"uid", "the unix uid for the process - used after loading is done"},
            {"gid", "the unix gid for the process - used after loading is done"}};

    protected Config() {
        init("BEJY config", PROPERTIES);
    }

    /**
     * Get the Config instance.
     * 
     * @return the Config instance.
     */
    public synchronized static Config getInstance() {
        if (instance == null)
            instance = new Config();
        return instance;
    }

    /**
     * Creates the singleton and loads the Config.
     */
    static void loadConfig(XmlFile xml) {
        instance = new Config();

        configurators.clear();
        dependencies.clear();
        configured.clear();
        modules.clear();
        loadables.clear();

        addGlobalUnique("global");
        addGlobalUnique("dns");
        instance.load(xml);
        try {
            int uid = instance.getIntProperty("uid", 0);
            int gid = instance.getIntProperty("gid", 0);
            if (uid > 0 || gid > 0) {
                boolean ret = GUID.setGUID(uid, gid);
                if (ret) {
                    defaultLog.writeDate("SUCCESS: uid=" + uid + ", gid=" + gid);
                } else {
                    defaultLog.writeDate("FAILED setting uid to " + uid + "and/or gid to " + gid);
                }
                defaultLog.writeDate("current dir = " + new File(".").getAbsolutePath());
            }
        } catch (Exception ex) {
            defaultLog.writeDate("FAILED setting uid and/or gid. Can't load shared library: " + ex.getMessage());
        }
    }

    /**
     * Creates the singleton and loads the Config.
     */
    static void loadConfig(String cfgName) {
        configFileName = cfgName;
        XmlFile xml = new XmlFile();
        xml.readFile(cfgName);
        loadConfig(xml);
    }

    /**
     * scan for config classes and init all by using the specified config file.
     * 
     * @param cfgName
     *            name of the config file.
     * @throws MalformedURLException
     */
    private void load(XmlFile xml) {
        // load the config file
        this.xml = xml;

        load(this, xml, "/bejy/");

        // create a classloader to scan for Configurators
        String classPath = System.getProperty("java.class.path");
        ZipClassLoader zcl;
        try {
            zcl = new ZipClassLoader(classPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            zcl = new ZipClassLoader();
        }
        LRUCache<String, MiniClass> cmap = new LRUCache<String, MiniClass>();

        String[] classes = zcl.list("*Cfg.class");
        for (int i = 0; i < classes.length; ++i) {
            String fn = classes[i];
            String cn = fn.substring(0, fn.length() - 6);
            if (cn.startsWith("/"))
                cn = cn.substring(1);

            try {
                if (isImplementorOf(cn, "de/bb/bejy/Configurator", zcl, cmap)) {
                    Class<?> clazz = zcl.loadClass(cn);
                    if (Configurator.class.isAssignableFrom(clazz)) {
                        Object o = clazz.newInstance();
                        Configurator cg = (Configurator) o;
                        String id = cg.getId();
                        if (id == null)
                            continue;

                        if (configurators.get(id) != null)
                            continue;

                        configurators.put(id, cg);

                        String ext = cg.getExtensionId();
                        if (ext != null)
                            for (StringTokenizer st = new StringTokenizer(ext, " ,\r\n\f\t"); st.hasMoreElements();)
                                dependencies.put(st.nextToken(), cg);

                        defaultLog.writeDate("found cfg:  " + cn + " - " + ext);
                    }
                }
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        }

        preloadLoadables("*Handler.class", zcl, cmap);
        preloadLoadables("*Group.class", zcl, cmap);
        preloadLoadables("*Factory.class", zcl, cmap);

        defaultLog.writeDate(cmap.toString());
        cmap.clear();

        // all known configurators are available now.
        // put all root elements into the list
        LinkedList<Object[]> order = new LinkedList<Object[]>();
        for (Iterator<Configurator> i = dependencies.subMap("de.bb.bejy", "de.bb.bejy\0").values().iterator(); i
                .hasNext();) {
            Configurator cfg = i.next();
            order.addLast(new Object[]{this, cfg, "/bejy/"});
        }

        // try to configure all elements in list
        for (int count = 0; order.size() > count;) {
            Object o3[] = order.removeFirst();
            Configurable current = (Configurable) o3[0];
            Configurator cfg = (Configurator) o3[1];
            String path = (String) o3[2];
            if (configure(order, current, cfg, path)) {
                count = 0;
            } else {
                order.addLast(o3);
                ++count;
            }
        }
        while (order.size() > 0) {
            Object o3[] = order.removeFirst();
            Configurator cfg = (Configurator) o3[1];
            defaultLog.writeDate("FAILURE:    " + cfg.getName() + ": has missing dependencies");
        }

        // activate direct children
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable ca = i.next();
            try {
                defaultLog.writeDate("activating: " + ca.getName());
                ca.activate(defaultLog);
                defaultLog.writeDate("activated:  " + ca.getName());
            } catch (Exception ex) {
                defaultLog.writeDate("FAILURE:    " + ca.getName() + ": " + ex.getMessage());
            }
        }

        for (ServerSocket ssocket : serverSockets.values()) {
            try {
                ssocket.close();
            } catch (Exception e) {
            }
        }
        serverSockets.clear();
    }

    /**
   * 
   */
    private void preloadLoadables(String mask, ZipClassLoader zcl, LRUCache<String, MiniClass> cmap) {
        String classes[] = zcl.list(mask);
        for (int i = 0; i < classes.length; ++i) {
            String fn = classes[i];
            String cn = fn.substring(0, fn.length() - 6);
            if (cn.startsWith("/"))
                cn = cn.substring(1);
            try {

                if (isImplementorOf(cn, "de/bb/bejy/Loadable", zcl, cmap)) {
                    //          System.out.println("check: " + cn);          
                    Class<?> clazz = zcl.loadClass(cn);
                    if (Loadable.class.isAssignableFrom(clazz)) {
                        Object o = clazz.newInstance();
                        Loadable l = (Loadable) o;
                        cn = clazz.getName();

                        String ext = l.getImplementationId();
                        for (StringTokenizer st = new StringTokenizer(ext, " ,\r\n\f\t"); st.hasMoreElements();)
                            loadables.put(st.nextToken(), cn);

                        defaultLog.writeDate("found loadable:  " + cn + " - " + ext);
                    }

                }

            } catch (Throwable e1) {
                //        System.out.println("failed: " + cn);
                //        e1.printStackTrace();
            }
        }
    }

    /**
     * @param mc
     * @param interfaceName
     * @param zcl
     * @param cmap
     * @return
     * @throws Exception
     */
    private boolean isImplementorOf(MiniClass mc, String interfaceName, ZipClassLoader zcl,
            LRUCache<String, MiniClass> cmap) throws Exception {
        String ifaces[] = mc.getInterfaceNames();
        for (int i = 0; i < ifaces.length; ++i) {
            String in = ifaces[i];
            //      System.out.println("i: " + in);
            if (interfaceName.equals(in))
                return true;
            try {
                if (isImplementorOf(in, interfaceName, zcl, cmap))
                    return true;
            } catch (Throwable t) {
            }
        }

        String cn = mc.getSuperClassName();
        if (cn != null) {
            return isImplementorOf(cn, interfaceName, zcl, cmap);
        }
        return false;
    }

    /**
     * @param cn
     * @param interfaceName
     * @param zcl
     * @param cmap
     * @return
     */
    private boolean isImplementorOf(String cn, String interfaceName, ZipClassLoader zcl,
            LRUCache<String, MiniClass> cmap) throws Exception {
        MiniClass mc = cmap.get(cn);
        if (mc == null) {
            InputStream is = zcl.getResourceAsStream(cn + ".class");
            if (is == null)
                return false;
            mc = new MiniClass(is);
            is.close();
            cmap.put(cn, mc);
        }
        return isImplementorOf(mc, interfaceName, zcl, cmap);
    }

    /**
     * Configure the current element.
     * 
     * @param order
     *            the list where all elements to be configured are held.
     * @param current
     *            current Configurable
     * @param cfgt
     *            current Configurator
     * @param currentPath
     *            curren Path
     * @return
     */
    private boolean configure(LinkedList<Object[]> order, Configurable current, Configurator cfgt, String currentPath) {
        //    defaultLog.writeDate("checking:   " + cfgt.getName());

        if (!checkRequired(cfgt.getRequired()))
            return false;

        try {
            String path = cfgt.getPath();
            if (!path.startsWith("/"))
                path = currentPath + path;

            String id = cfgt.getId();

            Enumeration<String> e = xml.getSections(path).elements();
            if (!e.hasMoreElements())
                return true;

            defaultLog.writeDate("configuring: " + cfgt.getPath() + " for " + current.getName());
            for (; e.hasMoreElements();) {
                String section = e.nextElement();

                String eid = id;
                Configurable ca = null;
                String caName = xml.getString(section, "class", null);
                if (cfgt.loadClass() && caName != null) {
                    defaultLog.writeDate("load class: " + caName);
                    Class<?> cz = Class.forName(caName);
                    ca = (Configurable) cz.newInstance();
                    if (ca.getId() != null)
                        eid = ca.getId();
                } else
                    ca = cfgt.create();

                if (ca == null)
                    throw new Exception("no instance");

                // load it            
                ca.load(cfgt, xml, section);

                // append to loaded modules, which are activated later
                modules.addLast(ca);

                defaultLog.writeDate("add child:  " + cfgt.getName() + " : " + ca.getName() + " - " + ca.getStatus());

                current.addChild(cfgt.getPath(), ca);

                for (Iterator<Configurator> i = dependencies.subMap(eid, eid + '\0').values().iterator(); i.hasNext();) {
                    Configurator cfg = i.next();
                    order.addLast(new Object[]{ca, cfg, section});
                }
            }

            configured.put(cfgt.getId(), cfgt);
            defaultLog.writeDate("configured: " + cfgt.getPath() + " for " + current.getName());
            return true;

        } catch (Throwable t) {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.toString();
            defaultLog.writeDate("FAILURE: " + msg + " while configuring: " + cfgt.getPath() + " for "
                    + current.getName());
            t.printStackTrace();
        }
        return false;
    }

    /**
     * Check whether required modules are already loaded.
     * 
     * @param required
     * @return
     */
    private static boolean checkRequired(String required) {
        if (required != null) {
            StringTokenizer st = new StringTokenizer(required, ", \r\n\t\f");
            for (; st.hasMoreTokens();) {
                String depend = st.nextToken();
                if (configured.get(depend) == null)
                    return false;
            }
        }
        return true;
    }

    /**
     * Return the available Configurators.
     * 
     * @return the available Configurators.
     */
    public Iterator<Configurator> configurators() {
        return configurators.values().iterator();
    }

    // our cron
    private static Cron cron = new Cron();

    // static UIFrame uiFrame;

    /**
     * Return the instance of the scheduler.
     * 
     * @return the instance of the scheduler.
     */
    public static Cron getCron() {
        return cron;
    }

    /**
     * Return the default log.
     * 
     * @return the default log.
     */
    public static LogFile getLogFile() {
        return defaultLog;
    }

    /**
     * Retrieve a group by name.
     * 
     * @param name
     *            the group name.
     * @return the group implementation if found.
     */
    public static UserGroupDbi getGroup(String name) {
        if (name == null)
            return null;
        for (Iterator<Configurable> i = getInstance().children(); i.hasNext();) {
            Configurable c = i.next();
            if (c instanceof UserGroupDbi && name.equals(c.getProperty("name")))
                return (UserGroupDbi) c;
        }
        return null;
    }

    public static HashMap<String, SslCfg> getSslConfigs() {
        HashMap<String, SslCfg>  r = new HashMap<String, SslCfg> ();
        for (Iterator<Configurable> i = getInstance().children(); i.hasNext();) {
            Configurable c = i.next();
            if (c instanceof SslCfg )
                r.put(c.getProperty("name"), (SslCfg) c);
        }
        return r;
    }
    
    /* *
     * Get a non public config instance, to perform administrative changes.
     * @param password the password;
     */
    /*
    public static void openUI(String password)
    {
      if ("test1234".equals(password))
      {
        openUI();
      }
    }
    */

    /**
     * Dummy implementation - returns always null.
     * 
     * @return returns always null.
     */
    public Configurable create() {
        return null;
    }

    /**
     * Returns the description of the Configurable object.
     * 
     * @return the description of the Configurable object.
     */
    public String getDescription() {
        return "BEJY - Bebbo's economic Java server";
    }

    /**
     * Return the extension point where this element wants to contribute to - or null.
     * 
     * @return the extension point where this element wants to contribute to, or null.
     */
    public String getExtensionId() {
        return null;
    }

    /**
     * Return the own id or null.
     * 
     * @return the own id or null.
     */
    public String getId() {
        return "de.bb.bejy";
    }

    /**
     * Return the path where the settings are held. This is a relative path applied to the current path.
     * 
     * @return the path where the settings are held.
     */
    public String getPath() {
        return "bejy";
    }

    /**
     * Return null, since nothing is required here.
     * 
     * @return null, since nothing is required here.
     */
    public String getRequired() {
        return null;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#acceptNewChild(de.bb.bejy.Configurator)
     */
    public boolean acceptNewChild(Configurator ct) {
        String path = ct.getPath();
        if (unique.get(path) == null)
            return true;

        return getChild(path) == null;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#mayRemove(de.bb.bejy.Configurable)
     */
    public boolean mayRemove(Configurable configurable) {
        return true; //acceptNewChild(configurable.getConfigurator());
    }

    /**
     * Mark the path as an unique element.
     * 
     * @param path
     *            path of the unique element
     */
    public static void addGlobalUnique(String path) {
        unique.put(path, path);
    }

    /**
     * Get an Iterator over the available implementation class names.
     * 
     * @param ct
     *            the Configurator
     * @return an Iterator for all possible class names
     */
    public static Iterator<String> getClassNames(Configurator ct) {
        String id = ct.getId();
        return loadables.subMap(id, id + "\0").values().iterator();
    }

    /**
     * save current settings.
     */
    public static void save() {
        // rename current file
        int ext = 0;
        File renamed;
        do {
            renamed = new File(configFileName + "." + (++ext));
        } while (renamed.exists());
        File current = new File(configFileName);
        current.renameTo(renamed);

        XmlFile xml = new XmlFile();
        xml.readFile(configFileName);
        getInstance().store("", xml);
        xml.flush();
    }

    /**
     * revert to last save settings.
     */
    public static void revert() {
        try {
            getInstance().deactivate(defaultLog);
        } catch (Exception e) {
        }
        loadConfig(configFileName);
    }

    /**
     * @throws Exception
     * 
     */
    public static void shutdown() throws Exception {
        getInstance().deactivate(getLogFile());
    }

    /**
     * @param c
     *            an interface
     * @param mask
     *            a mask to filter class files
     * @return a list with all implementors.
     */
    public static List<String> getImplementors(Class<?> c, String mask) {
        List<String> ll = new LinkedList<String>();

        String classPath = System.getProperty("java.class.path");
        ZipClassLoader zcl;
        try {
            zcl = new ZipClassLoader(classPath);
        } catch (MalformedURLException e) {
            return ll;
        }

        String[] classes = zcl.list(mask);
        for (int i = 0; i < classes.length; ++i) {
            String cn = classes[i];
            cn = cn.substring(0, cn.length() - 6);
            if (cn.startsWith("/"))
                cn = cn.substring(1);
            try {
                Class<?> clazz = zcl.loadClass(cn);
                if (c.isAssignableFrom(clazz) && clazz.newInstance() != null)
                    ll.add(clazz.getName());
            } catch (Throwable ex) {
            }
        }
        return ll;
    }

    /**
     * Retrieve the configurator for a given element.
     * 
     * @param name
     * @return the configurator for the name or null.
     */
    public Configurator getConfigurator(String name) {
        for (Iterator<Configurator> k = configurators(); k.hasNext();) {
            Configurator c = k.next();
            if (c.getName().equals(name))
                return c;
        }
        return null;
    }

    public ServerSocket getServerSocket(int port) {
        return serverSockets.remove(port);
    }

    public void putServerSocket(int port, ServerSocket ssocket) {
        serverSockets.put(port, ssocket);
    }

}

/******************************************************************************
 * $Log: Config.java,v $
 * Revision 1.48  2014/09/22 09:24:39  bebbo
 * @N added support for SSL host name, to choose certificate and key based on host name
 *
 * Revision 1.47  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 * Revision 1.46  2013/06/18 13:23:46  bebbo
 * @I preparations to use nio sockets
 * @V 1.5.1.68
 * Revision 1.45 2012/11/13 19:27:10 bebbo
 * 
 * @B restored non static member functions Revision 1.44 2012/11/13 06:38:44 bebbo
 * 
 * @I code cleanup Revision 1.43 2012/08/11 17:03:48 bebbo
 * 
 * @I typed collections Revision 1.42 2010/04/10 12:12:25 bebbo
 * 
 * @I use a default Config instance if none exists to support tests
 * 
 *    Revision 1.41 2009/11/18 08:05:44 bebbo
 * @N added support to change UID and GID in linux
 * 
 *    Revision 1.40 2006/10/12 05:50:26 bebbo
 * @B catch possible NPE
 * 
 *    Revision 1.39 2006/02/06 09:11:13 bebbo
 * @C added comments
 * 
 *    Revision 1.38 2005/11/11 18:49:16 bebbo
 * @N added new methods for new admin UI
 * 
 *    Revision 1.37 2004/12/13 15:26:13 bebbo
 * @B fixed class loading for JDK1.4.2_05 and later
 * 
 *    Revision 1.36 2004/04/16 13:41:25 bebbo
 * @R changed start behaviour
 * 
 *    Revision 1.35 2004/04/07 16:32:05 bebbo
 * @R determining classes without using a ClassLoader
 * 
 *    Revision 1.34 2004/03/23 11:06:18 bebbo
 * @I using new ZipClassLoader
 * 
 *    Revision 1.33 2003/11/26 09:56:15 bebbo
 * @B fixed NPEs
 * 
 *    Revision 1.32 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.31 2003/07/30 10:10:38 bebbo
 * @R enahanced information in admin interface
 * 
 *    Revision 1.30 2003/07/14 12:44:25 bebbo
 * @I made Tiger restart on webapp changes
 * 
 *    Revision 1.29 2003/07/09 18:29:45 bebbo
 * @N added default values.
 * 
 *    Revision 1.28 2003/06/30 12:10:35 bebbo
 * @R added new loadConfig(XmlFile) function, to enable custom mains.
 * 
 *    Revision 1.27 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.26 2003/06/18 13:54:46 bebbo
 * @R modified some descriptions
 * @R removed mainDomain from global
 * @B fixed activate of Dns
 * 
 *    Revision 1.25 2003/06/18 13:36:08 bebbo
 * @R almost complete on the fly update.
 * 
 *    Revision 1.24 2003/06/18 08:36:56 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.23 2003/06/17 15:13:36 bebbo
 * @R more changes to enable on the fly config updates
 * 
 *    Revision 1.22 2003/06/17 13:01:26 bebbo
 * @R added creation/deletion restrictions
 * 
 *    Revision 1.21 2003/06/17 12:31:46 bebbo
 * @R added configuration as Configurable root node
 * 
 *    Revision 1.20 2003/06/17 12:10:03 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.19 2003/06/17 10:18:10 bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.18 2003/05/13 15:42:07 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.17 2003/04/10 11:16:57 bebbo
 * @I development snapshot
 * 
 *    Revision 1.16 2003/01/25 15:07:24 bebbo
 * @I lock is now private
 * 
 *    Revision 1.15 2002/12/16 16:32:30 bebbo
 * @N added a role interface to hook custom isUserInRole implementations
 * 
 *    Revision 1.14 2002/08/21 09:14:43 bebbo
 * @R changes for the admin UI
 * 
 *    Revision 1.13 2002/04/03 15:39:44 franke
 * @N added javac to global config
 * 
 *    Revision 1.12 2002/01/20 11:59:56 franke
 * @B fixed logged servernames (was <unnamed>)
 * 
 *    Revision 1.11 2001/10/08 22:05:23 bebbo
 * @L modified logging
 * 
 *    Revision 1.10 2001/09/15 08:45:12 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.9 2001/06/11 06:32:04 bebbo
 * @N added getConfigFile
 * 
 *    Revision 1.8 2001/04/16 16:23:10 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.7 2001/04/16 13:43:41 bebbo
 * @I now using a XML file
 * 
 *    Revision 1.6 2001/04/06 05:52:02 bebbo
 * @I changed ini.getSections())
 * 
 *    Revision 1.5 2001/03/27 19:47:20 bebbo
 * @N added getDefaultLog()
 * 
 *    Revision 1.4 2001/01/01 01:01:44 bebbo
 * @R passing logFile to Server CT
 * 
 *    Revision 1.3 2000/12/30 09:02:54 bebbo
 * @R added Cron
 * 
 *    Revision 1.2 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *    Revision 1.1 2000/11/10 18:13:26 bebbo
 * @N new (uncomplete stuff)
 * 
 *****************************************************************************/
