package de.bb.bejy.j2ee;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;

import javax.naming.NamingException;

import de.bb.util.LogFile;
import de.bb.util.SessionManager;

/**
 * The registry is used to lookup stuff by name.
 * 
 * There are global registered items like database connections.
 * And there are items registered via an EAR.
 * 
 * @author bebbo
 */
public class Registry {
    private final static Class<?>[] NOCLASS = {};

    private final static Object[] NOOBJECT = {};

    // free client after 30 mins without working TCP/IP connection
    static SessionManager clientBeans = new SessionManager(1000 * 30 * 60L);

    static LogFile logFile;

    private static int no = 0;

    private static WeakHashMap<Thread, Principal> helperCache = new WeakHashMap<Thread, Principal>();

    private static HashMap<String, String> classNameMap = new HashMap<String, String>();

    /** hold global registered objects. */
    private static HashMap<String, Object> globalMap = new HashMap<String, Object>();
    
    /** old loaded ears. */
    private static HashSet<Ear> earSet = new HashSet<Ear>();
    
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
        return helperCache.get(Thread.currentThread());
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

        String mapped = classNameMap.get(className);
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
        Class<?> clazz = cl.loadClass(className);
        Constructor<?> ct = clazz.getConstructor(NOCLASS);
        return ct.newInstance(NOOBJECT);
    }

    public static Object lookupInstance(Principal principal, String name) throws NamingException {
        logFile.writeDate("lookup for: " + name + " with " + principal);
        if (name.startsWith("java:comp/env/"))
            name = name.substring(14);
        
        Object o = globalMap.get(name);
        if (o != null)
            return o;
        
        for (Ear ear : earSet) {
            o = ear.lookup(principal, name);
            if (o != null)
                return o;
        }
        return null;
    }
    
    static {
        globalMap.put("jdbc/taxords", new DS());
        globalMap.put("java:comp/UserTransaction", new UT());
    }
}