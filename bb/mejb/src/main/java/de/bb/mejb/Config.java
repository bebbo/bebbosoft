package de.bb.mejb;

import de.bb.rmi.Principal;
import de.bb.sql.JdbcFactory;
import de.bb.util.*;
import java.io.*;
import java.rmi.RemoteException;
import java.sql.DriverManager;
import java.util.*;

public class Config {

    public Config() {
    }

    private static void init() {
        try {
            int i = Integer.parseInt(a.getString("/mejb", "debugLevel", "500"));
            Logger.setLevel(i);
        } catch (Exception _ex) {
        }
        String s = a.getString("/mejb", "logFile", "*");
        Logger.setLogFile(s);
        s = a.getString("/mejb", "dbLogFile", "*");
        d = new LogFile(s);
        Iterator iterator = a.getSections("/mejb/db").iterator();
        while (iterator.hasNext()) {
            String s1 = (String) iterator.next();
            String s2 = a.getString(s1, "package-prefixes", "");
            String s3 = a.getString(s1, "class", null);
            String s4 = a.getString(s1, "jdbcUrl", null);
            String s5 = a.getString(s1, "user", null);
            String s6 = a.getString(s1, "password", null);
            String s7 = a.getString(s1, "maxConnections", "10");
            String s8 = a.getString(s1, "threshold", "3");
            String s9 = a.getString(s1, "jdbcLog", "");
            String s10 = a.getString(s1, "name", null);
            String s11 = a.getString(s1, "type", s10);
            Logger.info("registering database");
            Logger.info("  prefixes: " + s2 + ", type: " + s11 + ", jdbcUrl: " + s4 + ", driver : " + s3);
            if (s4 == null || s3 == null || s5 == null)
                throw new RuntimeException("missing or invalid database configuration. check mejb.xml!");
            ArrayList arraylist = new ArrayList();
            for (Iterator iterator1 = a.getSections(s1 + "init").iterator(); iterator1.hasNext();) {
                String s12 = (String) iterator1.next();
                String s14 = a.getString(s12, "command", null);
                if (s14 != null)
                    arraylist.add(s14);
            }

            try {
                if ("true".equalsIgnoreCase(s9))
                    DriverManager.setLogWriter(new PrintWriter(System.err));
                try {
                    Logger.info("loading jdbc driver " + s3);
                    Class.forName(s3).newInstance();
                    Logger.info("jdbc driver " + s3 + " loaded successfully");
                } catch (Throwable _ex) {
                    Logger.warn("unable to load jdbc driver " + s3);
                    Logger.warn(" --> skipping database prefixes: " + s2 + ", type: " + s11);
                    continue;
                }
                Pool pool = new Pool(new JdbcFactory(s3, s4, s5, s6, arraylist));
                pool.setMaxCount(1);
                pool.setMaxCount(Integer.parseInt(s7));
                pool.setThreshold(Integer.parseInt(s8));
                String s13 = a.getString("/mejb", "roleChecker", null);
                if (s13 != null)
                    try {
                        e = (RoleChecker) Class.forName(s13).newInstance();
                    } catch (Throwable _ex) {
                        Logger.warn("unable to load role checker " + s13);
                    }
                if (s2.length() == 0) {
                    Code.put("", ((Object) (new Object[] { pool, s11 })));
                } else {
                    for (StringTokenizer stringtokenizer = new StringTokenizer(s2, "\r\n\f\t, "); stringtokenizer
                            .hasMoreElements(); Code.put(stringtokenizer.nextElement(), ((Object) (new Object[] { pool,
                            s11 }))))
                        ;
                }
            } catch (Exception exception) {
                RuntimeException runtimeexception = new RuntimeException(exception.getMessage());
                runtimeexception.initCause(exception);
                throw runtimeexception;
            }
        }
    }

    private static synchronized void checkForModification() {
        long l = b.lastModified();
        if (l == c)
            return;
        if (b.exists()) {
            Logger.info("Reading config file " + b.getAbsolutePath());
            a = new XmlFile(b.getAbsolutePath());
        } else {
            a = new XmlFile();
            InputStream inputstream = null;
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            Logger.info("Reading config via getResourceAsStream(\"mejb.xml\")");
            inputstream = classloader.getResourceAsStream("mejb.xml");
            a.read(inputstream);
            try {
                inputstream.close();
            } catch (Exception _ex) {
            }
        }
        init();
        c = l;
    }

    static void b(File file) {
        if (file == null) {
            return;
        } else {
            b = file;
            return;
        }
    }

    public static String getDBType(String s) throws RemoteException {
        Object aobj[] = getConfigValues(s);
        if (aobj != null)
            return (String) aobj[1];
        else
            throw new RemoteException("no database type defined for package: " + s);
    }

    static Pool c(String s) {
        Logger.debug("Pool request for package " + s);
        Object aobj[] = getConfigValues(s);
        if (aobj != null) {
            Pool pool = (Pool) aobj[0];
            Logger.debug("found  pool for package: " + s);
            return pool;
        } else {
            Logger.warn("no backend found for package " + s);
            return null;
        }
    }

    private static Object[] getConfigValues(String packageName) {
        checkForModification();
        do {
            Object aobj[] = (Object[]) Code.get(packageName);
            if (aobj != null)
                return aobj;
            int i = packageName.lastIndexOf('.');
            if (i >= 0)
                packageName = packageName.substring(0, i);
            else
                return (Object[]) Code.get("");
        } while (true);
    }

    public static int getRMIPort() {
        checkForModification();
        String s = a.getString("/mejb/rmi", "port", "1111");
        return Integer.parseInt(s);
    }

    public static String getRMIHost() {
        checkForModification();
        String s = a.getString("/mejb/rmi", "host", "localhost");
        return s;
    }

    public static void dblog(String s, String s1, String s2, StringBuffer stringbuffer) {
        d.writeDate("[DBLOG] user: " + s + ", table: " + s1 + ", action: " + s2 + ", attributes: " + stringbuffer);
    }

    public static boolean isCallerInRole(Principal principal, String s) {
        if (e == null)
            return false;
        else
            return e.isCallerInRole(principal, s);
    }

    private static HashMap Code = new HashMap();
    private static XmlFile a;
    private static File b = new File("mejb.xml");
    private static long c = -1L;
    private static LogFile d;
    private static RoleChecker e;

}
