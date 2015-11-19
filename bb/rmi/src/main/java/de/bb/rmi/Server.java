package de.bb.rmi;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;

import de.bb.util.LogFile;
import de.bb.util.SessionManager;
import de.bb.util.XmlFile;

/**
 * @author bebbo
 */
public class Server {
    // free client after 30 mins without working TCP/IP connection
    static SessionManager clientBeans = new SessionManager(1000 * 30 * 60L);

    private static int no = 0;

    private static int port = 1111;

    private static WeakHashMap helperCache = new WeakHashMap();

    private static HashMap globalMap = new HashMap();

    private static String logFileName = "*";

    static LogFile logFile;

    static {
        loadEjbJar("ejb-jar.xml");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            doOptions(args);
            logFile = new LogFile(logFileName);
            ServerSocket ss = new ServerSocket(port);
            for (;;) {
                Socket s = ss.accept();
                Handler h = new Handler();
                h.setOutputStream(s.getOutputStream());
                h.setInputStream(s.getInputStream());
                h.start();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            showUsage();
        }
    }

    /**
     * @param string
     */
    private static void loadEjbJar(String ejbJarName) {
        XmlFile xml = new XmlFile(ejbJarName);

        for (Iterator i = xml.getSections("/ejb-jar/enterprise-beans/session").iterator(); i.hasNext();) {
            String key = (String) i.next();
        }
        for (Iterator i = xml.getSections("/ejb-jar/enterprise-beans/entity").iterator(); i.hasNext();) {
            String key = (String) i.next();
            String name = xml.getContent(key + "ejb-name");
            String remote = xml.getContent(key + "remote");

            if (name != null && remote != null)
                globalMap.put(name, remote);
        }
    }

    /**
     * Perform a login.
     * 
     * @param properties
     * @return
     */
    static ClientBean createClient(Principal principal) {
        long newId = newKey();
        ClientBean client = new ClientBean(newId, principal);
        clientBeans.put(new Long(newId), client);

        return client;
    }

    static Object lookupObject(long cid, long oid) {
        ClientBean client = (ClientBean) clientBeans.get(new Long(cid));
        if (client == null)
            return null;
        return client.objects.get(oid);
    }

    static RemoteId register(EasyRemoteObject ro) {
        Thread t = Thread.currentThread();
        if (!(t instanceof Handler))
            return null;

        Handler h = (Handler) t;
        ClientBean client = h.client;

        long oid = client.newKey();
        client.objects.put(oid, ro);
        RemoteId ri = new RemoteId(client.cid, oid, ro.getClass().getName() + "_Ying");

        return ri;
    }

    static synchronized long newKey() {
        return ++no;
    }

    public static Principal getPrincipal() {
        return (Principal) helperCache.get(Thread.currentThread());
    }

    /**
     * @param principal
     * @param helper
     * @param className
     * @return
     * @throws Exception
     */
    static Object makeInstance(Principal principal, /*String helper,
                                                    */String className) throws Exception {
        if (className.startsWith("java:comp/env/"))
            className = className.substring(14);

        String mapped = (String) globalMap.get(className);
        if (mapped == null) {
            if (!className.endsWith("Bean"))
                className += "Bean";
        } else {
            className = mapped;
        }

        //    int dot = className.lastIndexOf('.') + 1;
        //    className = className.substring(0, dot) + "mejb." + className.substring(dot) + "CMP";

        Thread currentThread = Thread.currentThread();
        ClassLoader cl = currentThread.getContextClassLoader();
        helperCache.put(currentThread, principal);

        // create the object
        Class clazz = cl.loadClass(className);
        return clazz.newInstance();
    }

    /**
     * parse the command line for options and return other parameters.
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private static String[] doOptions(String args[]) throws Exception {
        int j = 0;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) != '-') // no argument
            {
                args[j++] = args[i];
                continue;
            }
            String o = args[i++];
            // an argument
            if (o.equals("-?")) {
                throw new Exception(""); // show usage only!
            }
            try {
                if (o.equals("-p")) {
                    port = Integer.parseInt(args[i++]);
                    continue;
                }
                if (o.equals("-l")) {
                    logFileName = args[i++];
                }

            } catch (Exception e) {
                throw new Exception("Invalid parameter for '" + args[i - 2] + "'");
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
        System.out.println("\nUsage: java de.bb.rmi.Server [-?] [-p <port:1111>]");
        System.out.println("  -?            display this message");
        System.out.println("  -p <port>     use this port to listen on, '1111' is default");
        System.out.println("  -l <logfile>  log to <logfile>, * is default");
    }
}