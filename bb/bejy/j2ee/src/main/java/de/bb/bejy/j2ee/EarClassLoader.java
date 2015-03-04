package de.bb.bejy.j2ee;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerService;
import javax.jws.WebService;
import javax.persistence.PersistenceContext;
import javax.xml.ws.WebServiceContext;

import de.bb.bejy.MiniClass;
import de.bb.bejy.http.WebAppContext;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.ZipClassLoader;

/**
 * The ear's class loader.
 * 
 * @author stefan franke
 * 
 */
public class EarClassLoader extends ZipClassLoader {

    public static final String JAVAX_JWS_WEB_SERVICE = "Ljavax/jws/WebService;";
    public static final String JAVAX_EJB_STATEFUL = "Ljavax/ejb/Stateful;";
    public static final String JAVAX_EJB_STATELESS = "Ljavax/ejb/Stateless;";

    private String path;
    private HashSet<URL> jars = new HashSet<URL>();
    HashSet<URL> beanJars = new HashSet<URL>();
    private HashMap<String, EjbIH> localMap = new HashMap<String, EjbIH>();
    private HashMap<String, EjbIH> remoteMap = new HashMap<String, EjbIH>();
    private HashSet<EjbIH> used = new HashSet<EjbIH>();

    private ThreadContext defaultContext = new ThreadContext();
    private HashMap<WebAppContext, WSC> wac2wsc = new HashMap<WebAppContext, WSC>();

    /**
     * CT for the ear root class loader.
     * 
     * @param path
     *            the path to the unpacked ear.
     */
    public EarClassLoader(String path) {
        this.path = path;
    }

    /**
     * Add a jar to the class path
     * 
     * @param jar
     *            name of a JAR
     * @throws MalformedURLException
     */
    public URL addJar(String jar) throws MalformedURLException {
        URL url = path2URL(path + "/" + jar);
        if (jars.contains(url)) {
            // beans must not live in a multiple used jar
            beanJars.remove(url);
        } else {
            addURL(url);
            beanJars.add(url);
            jars.add(url);
        }
        return url;
    }

    /**
     * Scan all referred classes and search - javax/ejb/Stateless - javax/ejb/Stateful - javax/ejb/Local -
     * javax/ejb/Remote
     * 
     * @param ecl
     * 
     * @param ejbStuff
     * 
     * @param logFile
     * 
     * @throws Exception
     */
    public void initializeBeans(EjbClassLoader ecl, MultiMap<String, String> ejbStuff, LogFile logFile)
            throws Exception {
        for (String stateless : ejbStuff.subMap(JAVAX_EJB_STATELESS, JAVAX_EJB_STATELESS + "\u0000").values()) {
            try {
                Class<?> statelessClass = ecl.loadClass(stateless);
                Stateless statelessAnnotation = statelessClass.getAnnotation(Stateless.class);
                if (statelessAnnotation == null)
                    continue;

                // search the implemented interfaces
                EjbIH h = null;
                for (Class<?> iface : statelessClass.getInterfaces()) {
                    Local local = iface.getAnnotation(Local.class);
                    //                if (local != null)
                    //                    locals.put(statelessClass.getClass().getName(), wrapper);
                    Remote remote = iface.getAnnotation(Remote.class);

                    if (local != null || remote != null) {
                        h = new EjbIH(statelessClass.newInstance());
                        Class<?>[] interfaces = new Class<?>[1];
                        interfaces[0] = iface;
                        Object proxy = Proxy.newProxyInstance(ecl, interfaces, h);
                        h.proxy = proxy;
                        if (local != null)
                            localMap.put(iface.getName(), h);
                        if (remote != null)
                            remoteMap.put(iface.getName(), h);
                        break;
                    }
                }
                if (h != null) {
                    logFile.writeDate("instantiated bean: " + stateless);
                } else {
                    logFile.writeDate("ERROR: instantiated bean: " + stateless + " no interface found!");
                }
            } catch (Throwable ex) {
                logFile.writeDate("ERROR: instantiating bean " + stateless + ": " + ex.getClass().getName() + " "
                        + ex.getMessage());
            }
        }
    }

    static MultiMap<String, String> earscan(ClassLoader cl, String[] classes) throws Exception {
        MultiMap<String, String> ejbMap = new MultiMap<String, String>();
        ejbMap = new MultiMap<String, String>();
        for (String cn : classes) {
            InputStream is = cl.getResourceAsStream(cn);
            if (is == null)
                continue;
            MiniClass mc = new MiniClass(is);
            is.close();
            if (mc.isInterface())
                continue;
            cn = cn.substring(0, cn.length() - 6);
            HashSet<String> strings = mc.getStrings();
            if (strings.contains(JAVAX_EJB_STATELESS))
                ejbMap.put(JAVAX_EJB_STATELESS, cn);
            if (strings.contains(JAVAX_EJB_STATEFUL))
                ejbMap.put(JAVAX_EJB_STATEFUL, cn);
            if (strings.contains(JAVAX_JWS_WEB_SERVICE))
                ejbMap.put(JAVAX_JWS_WEB_SERVICE, cn);
        }
        return ejbMap;
    }

    /**
     * 
     * @param logFile
     * @param beans
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    void injectBeans(Collection<Object> beans, LogFile logFile) throws IllegalArgumentException, IllegalAccessException {
        // now inject stuff into the beans
        for (Object bean : beans) {
            injectInstance(null, logFile, bean);
        }
    }

    void injectInstance(WebAppContext wac, LogFile logFile, Object bean) throws IllegalAccessException {
        ArrayList<Field> fieldList = new ArrayList<Field>();
        for (Class<?> dis = bean.getClass(); dis != null; dis = dis.getSuperclass()) {
            Field[] fields = dis.getDeclaredFields();
            for (Field field : fields) {
                if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                    if ((field.getModifiers() & Modifier.FINAL) != Modifier.FINAL)
                        logFile.writeDate("WARNING: " + field + " should be final");
                    continue;
                }
                if (field.getAnnotations().length > 0) {
                    field.setAccessible(true);
                    fieldList.add(field);
                } else {
                    logFile.writeDate("WARNING: non static field: " + field);
                }
            }
        }

        for (Field field : fieldList) {
            EJB ejb = field.getAnnotation(EJB.class);
            if (ejb != null) {
                String name = field.getType().getName();
                EjbIH h = localMap.get(name);
                if (h == null)
                    h = remoteMap.get(name);
                if (h == null) {
                    logFile.writeDate("ERROR: no bean for " + field + " in " + bean.getClass().getName());
                    continue;
                }
                used.add(h);
                field.set(bean, h.proxy);
                continue;
            }

            PersistenceContext pc = field.getAnnotation(PersistenceContext.class);
            if (pc != null) {
                field.set(bean, new EMWrapper(bean.getClass().getClassLoader(), pc));
                continue;
            }

            Resource res = field.getAnnotation(Resource.class);
            if (res != null) {
                if (field.getType() == SessionContext.class) {
                    field.set(bean, SCWrapper.INSTANCE);
                    continue;
                }

                if (field.getType() == TimerService.class) {
                    // TODO use an instance per EAR
                    field.set(bean, new TS());
                    continue;
                }

                if (field.getType() == WebServiceContext.class) {
                    if (wac == null) {
                        logFile.writeDate("ERROR: not in a WAR: " + field + " " + field.getAnnotations()[0]);
                        continue;
                    }
                    
                    WSC wsc = wac2wsc .get(wac);
                    if (wsc == null) {
                        wsc = new WSC(wac);
                        wac2wsc.put(wac, wsc);
                    }
                    field.set(bean, wsc);
                    continue;
                }

                //continue;
            }
            logFile.writeDate("TODO: " + field + " " + field.getAnnotations()[0]);
        }
        logFile.writeDate("injected: " + bean.getClass().getName());
    }

    Set<EjbIH> getBeans() {
        HashSet<EjbIH> beans = new HashSet<EjbIH>();
        beans.addAll(localMap.values());
        beans.addAll(remoteMap.values());
        return beans;
    }

    public void initializeWS(WebAppContext wac, LogFile logFile) throws Exception {
        ZipClassLoader zcl = wac.getZcl();
        Thread.currentThread().setContextClassLoader(zcl);
        MultiMap<String, String> warStuff = earscan(zcl, zcl.list("*.class"));
        for (String wsClassName : warStuff.subMap(JAVAX_JWS_WEB_SERVICE, JAVAX_JWS_WEB_SERVICE + "\u0000").values()) {
            if (wsClassName.startsWith("classes/"))
                continue;
            try {
                Class<?> wsClass = zcl.loadClass(wsClassName);
                if (!wsClass.isAnnotationPresent(WebService.class))
                    continue;

                WebServiceHandler wss = new WebServiceHandler(wsClass);
                injectInstance(wac, logFile, wss.wsImpl);
                wac.addHandler("/" + wss.serviceName, wss);

            } catch (Throwable t) {
                logFile.writeDate(t.getMessage());
            }
        }
    }

    public void check(LogFile logFile) {
        Set<EjbIH> ejbs = getBeans();
        ejbs.removeAll(used);
        for (EjbIH bean : ejbs) {
            logFile.writeDate("WARNING: unused bean: " + bean.ejb.getClass().getName());
        }

    }

    public ThreadContext getDefaultContext() {
        return defaultContext;
    }

}
