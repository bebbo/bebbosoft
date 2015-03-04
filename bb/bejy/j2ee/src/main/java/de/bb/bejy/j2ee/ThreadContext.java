package de.bb.bejy.j2ee;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;

import de.bb.bejy.ServerThread;
import de.bb.bejy.http.HttpProtocol;
import de.bb.bejy.http.HttpRequest;
import de.bb.util.Pair;
import de.bb.util.Pool;
import de.bb.util.XmlFile;

/**
 * The context data for each working thrad.
 * 
 * @author stefan franke
 * 
 */
class ThreadContext {
    private static HashMap<String, EntityManagerFactory> emfMap = new HashMap<String, EntityManagerFactory>(3);
    private HashMap<Pair<ClassLoader, String>, EntityManager> entityManagers = new HashMap<Pair<ClassLoader, String>, EntityManager>(
            3);

    SC sessionContext;
    Connection connection;
    Pool pool;

    EntityManager getEntityManager(ClassLoader cl, PersistenceContext pc) {
        Pair<ClassLoader, String> p = Pair.makePair(cl, pc.unitName());
        EntityManager em = entityManagers.get(p);
        if (em == null) {
            em = createEntityManager(pc, p);
        }
        return em;
    }

    private EntityManager createEntityManager(PersistenceContext pc, Pair<ClassLoader, String> p) {
        synchronized (emfMap) {
            EntityManager em = entityManagers.get(p);
            if (em == null) {
                Map<String, String> props = new HashMap<String, String>();
                props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                props.put("hibernate.transaction.factory_class", "org.hibernate.transaction.JTATransactionFactory");
                for (PersistenceProperty pp : pc.properties()) {
                    props.put(pp.name(), pp.value());
                }
                EntityManagerFactory emf = emfMap.get(pc.unitName());
                if (emf == null) {
                    emf = Persistence.createEntityManagerFactory(pc.unitName(), props);
                    emfMap.put(pc.unitName(), emf);
                }
                em = emf.createEntityManager(props);
            }
            entityManagers.put(p, em);
            return em;
        }
    }

    /**
     * Release no longer used stuff.
     */
    public void release() {
        if (pool != null && connection != null) {
            pool.release(Thread.currentThread(), connection);
            connection = null;
        }
        pool = null;
    }

    public static ThreadContext currentThreadContext() {
        final Thread t = Thread.currentThread();
        ThreadContext tc;
        if (t instanceof ServerThread) {
            tc = (ThreadContext) ((ServerThread) t).getContext();
        } else {
            ClassLoader cl = t.getContextClassLoader();
            while (cl != null && !(cl instanceof EarClassLoader)) {
                cl = cl.getParent();
            }
            if (cl == null)
                return null;
            tc = ((EarClassLoader) cl).getDefaultContext();
        }
        return tc;
    }

    public static Principal currentUserPrincipal() {
        final Thread t = Thread.currentThread();
        if (t instanceof ServerThread) {
            final ServerThread st = (ServerThread) t;
            final HttpRequest request = ((HttpProtocol) st.getProtocol()).getRequest();
            return request.getUserPrincipal();
        }
        return null;
    }

    public static boolean isCallerInRole(String role) {
        final Thread t = Thread.currentThread();
        if (t instanceof ServerThread) {
            final ServerThread st = (ServerThread) t;
            final HttpRequest request = ((HttpProtocol) st.getProtocol()).getRequest();
            return request.isUserInRole(role);
        }
        return false;
    }

    public static boolean isCallerInOneRoleOf(HashSet<String> roles) {
        final Thread t = Thread.currentThread();
        if (t instanceof ServerThread) {
            final ServerThread st = (ServerThread) t;
            final HttpRequest request = ((HttpProtocol) st.getProtocol()).getRequest();
            for (final String role : roles) {
                if (request.isUserInRole(role))
                    return true;
            }
        }
        return false;
    }
}
