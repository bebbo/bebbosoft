/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Tiger.java,v $
 * $Revision: 1.22 $
 * $Date: 2014/09/22 09:24:53 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Iterator;

import de.bb.util.LogFile;
import de.bb.util.XmlFile;

/**
 * Main class to start BEJY Tiger, a small web application server.
 * 
 * @author bebbo
 */
public class Tiger {
    private static String xmlfile = "bejy.xml";

    private static boolean shutDown;

    private static String logfileName = "*";

    private static LogFile logFile;

    private static String webapps = "./webapps";

    private static String javac = "javac";

    private static int port = 8080;

    /**
     * Starts or terminates the BEJY tiger.
     * 
     * @param args
     *            command line arguments.
     */
    public static void main(String[] args) {
        System.out.println(Version.getFull());
        System.out.println("BEJY Tiger - power for webapps");
        try {
            args = doOptions(args);
            if (args.length > 0) {
                throw new Exception("too many parameters");
            }

            if (shutDown) {
                shutdown();
                return;
            }
            XmlFile xml = new XmlFile();
            xml.readFile(xmlfile);

            final String bejy = getOrCreate(xml, "/bejy");
            final String global = getOrCreate(xml, bejy + "global");

            // set javac command if nothing is specified
            if (xml.getString(global, "javac", null) == null)
                xml.setString(global, "javac", javac);

            final String mimes = getOrCreate(xml, bejy + "mime-types");
            final String mime_type = xml.createSection(mimes + "mime-type");
            xml.setString(mime_type, "extension", "css");
            xml.setString(mime_type, "type", "text/css");
            
            final String srv = getOrCreate(xml, bejy + "server");

            xml.setString(srv, "name", "tiger");
            xml.setString(srv, "port", "" + port);
//            xml.setString(srv, "maxWait", "1");
//            xml.setString(srv, "maxCount", "999");

            final String proto = getOrCreate(xml, srv + "protocol");
            xml.setString(proto, "class", "de.bb.bejy.http.HttpFactory");

            final String host = getOrCreate(xml, proto + "host");
//            xml.setString(host, "logFile", logfileName);

            String wapps = getOrCreate(xml, host + "webapps");
            xml.setString(wapps, "path", webapps);

            startup(xml);
            // add shutdown thread
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Config.shutdown();
                    } catch (Throwable t) {
                    }
                    shutdown();
                }
            });

        } catch (Exception e) {
            System.out.println(e.getMessage());
            showUsage();
        }
    }

    /**
     * Get the section or create it.
     * 
     * @param xml
     *            the XmlFile
     * @param path
     *            the path to search or create
     * @return the unique path.
     */
    private static String getOrCreate(final XmlFile xml, final String path) {
        final Iterator<String> i = xml.sections(path);
        if (i.hasNext())
            return i.next();
        return xml.createSection(path);
    }

    /**
     * Start the server.
     * 
     * @param xml
     */
    private static void startup(XmlFile xml) throws Exception {
        File started = new File(webapps + "/.bejy_tiger");
        if (started.exists()) {
            try {
                ServerSocket ss = new ServerSocket(port);
                ss.close();
            } catch (Exception ee) {
                System.out.println("BEJY Tiger seems to be running with webapps=" + webapps);
                System.out.println("use -shutdown to terminate that process");
                System.out.println("or remove the .bejy_tiger synch file manually");
                System.exit(1);
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(started);
            fos.write(new Date().toString().getBytes());
        } finally {
            if (fos != null)
                fos.close();
        }

        Config.loadConfig(xml);
        logFile = Config.getLogFile();

        Config cfg = Config.getInstance();
        Configurable c = cfg.getChild("server");
        if (c == null)
            return;
        c = c.getChild("protocol");
        if (c == null)
            return;
        Configurable host = c.getChild("host");
        if (host == null)
            return;
        c = host.getChild("webapps");
        long last = System.currentTimeMillis();
        try {
            if (c == null) {
                // reload the single war
                c = host.getChild("war");
                if (c == null)
                    return;
                File folder = new File(c.getProperty("path"));
                for (;;) {
                    if (!started.exists()) {
                        break;
                    }
                    long check = new File(folder, "WEB-INF/web.xml").lastModified();
                    if (check > last) {
                        logFile.writeDate("reloading " + folder);
                        c.update(logFile);
                        last = check;
                    }
                    Thread.sleep(2000);
                }
                return;
            }

            // reload server on changes
            for (;;) {
                if (!started.exists()) {
                    break;
                }
                Thread.sleep(2000);
            }
        } finally {
            started.delete();
            System.exit(0);
        }
    }

    /**
     * Stop the server.
     */
    static void shutdown() {
        File started = new File(webapps + "/.bejy_tiger");
        if (!started.exists()) {
            System.out.println("... is not running ...");
            return;
        }
        boolean r = started.delete();
        System.out.println("shutdown returned: " + r);
    }

    /**
     * parse the command line for options and return other parameters.
     */
    private static String[] doOptions(String args[]) throws Exception {
        int j = 0;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) != '-') // no argument
            {
                args[j++] = args[i];
                continue;
            }
            String o = args[i];
            // an argument
            if (o.equals("-?")) {
                throw new Exception(""); // show usage only!
            }
            try {
                if (o.equals("-p")) {
                    port = Integer.parseInt(args[++i]);
                    continue;
                }
                if (o.equals("-c")) {
                    javac = args[++i] + " -g";
                    continue;
                }
                if (o.equals("-w")) {
                    webapps = args[++i];
                    continue;
                }
                if (o.equals("-l")) {
                    logfileName = args[++i];
                    continue;
                }
                if (o.equals("-f")) {
                    xmlfile = args[++i];
                    continue;
                }
                if (o.equals("-shutdown")) {
                    shutDown = true;
                    continue;
                }
            } catch (Exception e) {
                throw new Exception("Invalid parameter for '" + args[i - 1] + "'");
            }
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }

    /**
     * Display the usage message on stdout.
     * 
     */
    private static void showUsage() {
        System.out
                .println("\nUsage: java de.bb.bejy.Tiger [-?] [-p <port:8080>] [-c <javac>] [-l <logfile:*>] [-w <webapps>]");
        System.out.println("  -?            display this message");
        System.out.println("  -c <javac>    full path to javac, 'javac' is default");
        System.out.println("  -f <bejy.xml> specify a different name instead of bejy.xml");
        System.out.println("  -l <logfile>  a logfile to log to, '*' is default (stdout)");
        System.out.println("  -p <port>     use this port to listen on, '8080' is default");
        System.out.println("  -shutdown     shutdowns the server running in the same webapps folder");
        System.out.println("  -w <webapps>  path to webapps directory, './webapps' is default");
    }
}

/******************************************************************************
 * $Log: Tiger.java,v $
 * Revision 1.22  2014/09/22 09:24:53  bebbo
 * @B startup bug fixes
 * Revision 1.21 2014/06/23 19:02:58 bebbo
 * 
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis Revision 1.20 2013/06/18 13:23:35 bebbo
 * 
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.19 2012/11/13 06:38:49 bebbo
 * 
 * @I code cleanup Revision 1.18 2011/05/20 08:47:40 bebbo
 * 
 * @R reload a webapp only if web.xml changes Revision 1.17 2008/06/04 06:42:14 bebbo
 * 
 * @I cleanup
 * 
 *    Revision 1.16 2007/08/09 16:06:54 bebbo
 * @I integrated new SSL implementation
 * 
 *    Revision 1.15 2007/01/18 21:44:05 bebbo
 * @B fixed bogus command line parsing
 * @B fixed reload of webapps
 * 
 *    Revision 1.14 2006/10/12 05:52:32 bebbo
 * @N added support for single configured war folders
 * 
 *    Revision 1.13 2005/11/11 18:53:12 bebbo
 * @I catching new Exception from Config.shutdown()
 * 
 *    Revision 1.12 2004/12/13 15:28:43 bebbo
 * @N added support for link files
 * 
 *    Revision 1.11 2004/04/20 13:21:05 bebbo
 * @B fixed xml paths
 * 
 *    Revision 1.10 2004/04/16 13:43:41 bebbo
 * @N Tiger now also uses a bejy.xml configuration to load groups/users etc.
 * 
 *    Revision 1.9 2004/04/07 16:32:39 bebbo
 * @V new version message
 * 
 *    Revision 1.8 2004/03/23 11:09:48 bebbo
 * @R improved restart behaviour, if the server is down but the lock file still exists
 * 
 *    Revision 1.7 2004/01/09 19:36:46 bebbo
 * @B improved start/stop handling
 * 
 *    Revision 1.6 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.5 2003/08/04 08:33:36 bebbo
 * @I modified shutdown handling
 * 
 *    Revision 1.3 2003/07/15 08:41:19 bebbo
 * @R modified message, if a .bejy_tiger synch file was found
 * 
 *    Revision 1.2 2003/07/14 12:44:25 bebbo
 * @I made Tiger restart on webapp changes
 * 
 *    Revision 1.1 2003/07/09 18:29:14 bebbo
 * @N webapp runner main
 * 
 */
