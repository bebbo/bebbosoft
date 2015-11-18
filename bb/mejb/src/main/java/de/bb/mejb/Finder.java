package de.bb.mejb;

import java.util.HashMap;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

public class Finder {
    private Context context;
    private HashMap<String, Object> homeCache;

    public Finder(Properties properties) throws NamingException {
        homeCache = new HashMap<String, Object>();
        properties.put("de.bb.rmi.PRINCIPAL_HOLDER", "de.bb.mejb.SyntheticHome");
        context = NamingManager.getInitialContext(properties);
    }

    public synchronized Object getHome(String s) throws NamingException {
        if (!s.startsWith("java:comp/env/"))
            s = "java:comp/env/" + s;
        Object shb = homeCache.get(s);
        if (shb != null)
            return shb;
        try {
            shb = context.lookup(s);
        } catch (NamingException namingexception) {
            throw namingexception;
        }
        if (shb != null) {
            homeCache.put(s, shb);
            return shb;
        } else {
            throw new NamingException("not found: " + s);
        }
    }
}
