package de.bb.bejy.j2ee;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.handler.MessageContext;

import de.bb.bejy.ServerThread;
import de.bb.bejy.http.HttpProtocol;
import de.bb.bejy.http.HttpRequest;

public class MC implements MessageContext {

    WSC wsc;

    public MC(WSC wsc) {
        this.wsc = wsc;
    }

    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean containsKey(Object key) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        return false;
    }

    public Object get(Object key) {
        if (MessageContext.SERVLET_REQUEST.equals(key)) {
            final Thread t = Thread.currentThread();
            if (t instanceof ServerThread) {
                final ServerThread st = (ServerThread) t;
                final HttpRequest request = ((HttpProtocol) st.getProtocol()).getRequest();
                return request;
            }
        }
        return null;
    }

    public Object put(String key, Object value) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object remove(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    public void putAll(Map<? extends String, ? extends Object> m) {
        // TODO Auto-generated method stub

    }

    public void clear() {
        // TODO Auto-generated method stub

    }

    public Set<String> keySet() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<Object> values() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setScope(String name, Scope scope) {
        // TODO Auto-generated method stub

    }

    public Scope getScope(String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
