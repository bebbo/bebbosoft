
package de.bb.bejy.http;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

class HttpSession implements javax.servlet.http.HttpSession, de.bb.util.SessionManager.Callback {

    Hashtable objectTable;

    long lastUsed;

    long created;

    String sessionId;

    long timeout;

    boolean invalid = false;

    HttpContext context;

    /**
     * Constructor
     * 
     * @param server
     *            The ServerEngine the Session is created for.<br> Every ServerEngine has an own session manager.
     */
    HttpSession(HttpContext ctx) {
        context = ctx;
        created = lastUsed = System.currentTimeMillis();
        objectTable = new Hashtable();
        timeout = context.sessionManager.getTimeout();
        sessionId = context.createSession(this);
    }

    public boolean dontRemove(Object key) {
        invalidate();
        return false;
    }

    void touch() {
        context.sessionManager.touch(sessionId);
        lastUsed = System.currentTimeMillis();
    }

    // ====== HttpSession implementation ======================

    public long getCreationTime() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return created;
    }

    public String getId() {
        return sessionId;
    }

    public long getLastAccessedTime() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return lastUsed;
    }

    public int getMaxInactiveInterval() {
        if (invalid)
            throw new IllegalStateException();
        return (int) (timeout / 1000);
    }

    /**
     * @deprecated
     */
    public Object getValue(String name) throws IllegalStateException {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    public String[] getValueNames() throws IllegalStateException {
        synchronized (objectTable) {
            if (invalid)
                throw new IllegalStateException();
            String[] names = new String[objectTable.size()];
            Enumeration e = objectTable.keys();
            for (int i = 0; e.hasMoreElements(); ++i) {
                names[i] = (String) e.nextElement();
            }
            return names;
        }
    }

    public void invalidate() throws IllegalStateException {
        synchronized (objectTable) {
            if (invalid)
                throw new IllegalStateException();

            for (Enumeration e = objectTable.keys(); e.hasMoreElements();) {
                removeAttribute((String) e.nextElement());
            }
            objectTable.clear();
            invalid = true;
            context.removeSession(this);
        }
    }

    public boolean isNew() throws IllegalStateException {
        if (invalid)
            throw new IllegalStateException();
        return created == lastUsed;
    }

    /**
     * @deprecated
     */
    public void putValue(String name, Object value) throws IllegalStateException {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    public void removeValue(String name) throws IllegalStateException {
        removeAttribute(name);
    }

    public void setMaxInactiveInterval(int val) {
        if (invalid)
            throw new IllegalStateException();
        timeout = val * 1000L;
        context.sessionManager.touch(sessionId, timeout);
    }

    public java.lang.Object getAttribute(java.lang.String name) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        return objectTable.get(name);
    }

    public java.util.Enumeration getAttributeNames() // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        return objectTable.keys();
    }

    public void removeAttribute(java.lang.String name) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        Object old = objectTable.remove(name);
        javax.servlet.http.HttpSessionBindingEvent hsbe = new javax.servlet.http.HttpSessionBindingEvent(this, name,
                old);
        if (old instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) old).valueUnbound(hsbe);
        }
        if (context.hsalv != null) {
            for (Iterator e = context.hsalv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionAttributeListener hsal = (javax.servlet.http.HttpSessionAttributeListener) e
                        .next();
                hsal.attributeRemoved(hsbe);
            }
        }
    }

    public void setAttribute(java.lang.String name, java.lang.Object o) // from javax.servlet.http.HttpSession
    {
        if (invalid)
            throw new IllegalStateException();
        Object old = objectTable.put(name, o);
        javax.servlet.http.HttpSessionBindingEvent hsbe = new javax.servlet.http.HttpSessionBindingEvent(this, name, o);
        javax.servlet.http.HttpSessionBindingEvent hsbeo = new javax.servlet.http.HttpSessionBindingEvent(this, name,
                old);
        if (o instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) o).valueBound(hsbe);
        }
        if (old instanceof javax.servlet.http.HttpSessionBindingListener) {
            ((javax.servlet.http.HttpSessionBindingListener) old).valueUnbound(hsbeo);
        }
        if (context.hsalv != null) {
            for (Iterator e = context.hsalv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionAttributeListener hsal = (javax.servlet.http.HttpSessionAttributeListener) e
                        .next();
                if (old == null)
                    hsal.attributeAdded(hsbe);
                else
                    hsal.attributeReplaced(hsbeo);
            }
        }
    }

    public javax.servlet.ServletContext getServletContext() {
        return context;
    }

    /**
     * @deprecated
     */
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return context;
    }

}
