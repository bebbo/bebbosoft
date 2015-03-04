package de.bb.bejy.j2ee;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

/**
 * The real EntityManager is attached to the class loader - each ejb module needs an own EntityManager! Also the the
 * PersistenceContext is a scope.
 * 
 * @author stefan franke
 * 
 */
class EMWrapper implements EntityManager {

    private ClassLoader cl;
    private PersistenceContext pc;

    EMWrapper(ClassLoader cl, PersistenceContext pc) {
        this.cl = cl;
        this.pc = pc;
    }

    private EntityManager getEntityManager() {
        final ThreadContext tc = ThreadContext.currentThreadContext();
        return tc.getEntityManager(cl, pc);
    }

    public void clear() {
        getEntityManager().clear();
    }

    public void close() {
        getEntityManager().close();
    }

    public boolean contains(Object arg0) {
        return getEntityManager().contains(arg0);
    }

    public Query createNamedQuery(String arg0) {
        return getEntityManager().createNamedQuery(arg0);
    }

    public Query createNativeQuery(String arg0) {
        return getEntityManager().createNativeQuery(arg0);
    }

    public Query createNativeQuery(String arg0, Class arg1) {
        return getEntityManager().createNativeQuery(arg0, arg1);
    }

    public Query createNativeQuery(String arg0, String arg1) {
        return getEntityManager().createNativeQuery(arg0, arg1);
    }

    public Query createQuery(String arg0) {
        return getEntityManager().createQuery(arg0);
    }

    public <T> T find(Class<T> arg0, Object arg1) {
        return getEntityManager().find(arg0, arg1);
    }

    public void flush() {
        getEntityManager().flush();
    }

    public Object getDelegate() {
        return getEntityManager().getDelegate();
    }

    public FlushModeType getFlushMode() {
        return getEntityManager().getFlushMode();
    }

    public <T> T getReference(Class<T> arg0, Object arg1) {
        return getEntityManager().getReference(arg0, arg1);
    }

    public EntityTransaction getTransaction() {
        return getEntityManager().getTransaction();
    }

    public boolean isOpen() {
        return getEntityManager().isOpen();
    }

    public void joinTransaction() {
        getEntityManager().joinTransaction();
    }

    public void lock(Object arg0, LockModeType arg1) {
        getEntityManager().lock(arg0, arg1);
    }

    public <T> T merge(T arg0) {
        return getEntityManager().merge(arg0);
    }

    public void persist(Object arg0) {
        getEntityManager().persist(arg0);
    }

    public void refresh(Object arg0) {
        getEntityManager().refresh(arg0);
    }

    public void remove(Object arg0) {
        getEntityManager().remove(arg0);
    }

    public void setFlushMode(FlushModeType arg0) {
        getEntityManager().setFlushMode(arg0);
    }

    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public void detach(Object arg0) {
        // TODO Auto-generated method stub

    }

    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    public LockModeType getLockMode(Object arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Metamodel getMetamodel() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub

    }

    public void refresh(Object arg0, Map<String, Object> arg1) {
        // TODO Auto-generated method stub

    }

    public void refresh(Object arg0, LockModeType arg1) {
        // TODO Auto-generated method stub

    }

    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        // TODO Auto-generated method stub

    }

    public void setProperty(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    public <T> T unwrap(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
