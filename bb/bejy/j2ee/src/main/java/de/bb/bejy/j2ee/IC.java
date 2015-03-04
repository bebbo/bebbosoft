package de.bb.bejy.j2ee;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * This class is ...
 * 
 * @author administrator
 */
class IC implements Context {

    private RemoteClient remoteClient;
    private ClientLocal localClient;

    private boolean local;

    private boolean remote;

    IC(Hashtable<?, ?> properties) throws NamingException {
        String dest = (String) properties.get(Context.PROVIDER_URL);
        if (dest == null)
            throw new NamingException("missing property: " + Context.PROVIDER_URL);
        if (!dest.startsWith("fastrmi://"))
            throw new NamingException("unsupported protocol: " + dest);

        String val = (String) properties.get(Constants.LOCAL_SETTING);
        local = !"never".equalsIgnoreCase(val);
        remote = !"only".equalsIgnoreCase(val);

        String user = (String) properties.get(Context.SECURITY_PRINCIPAL);
        if (user == null)
            user = "anonymous";
        String pwd = (String) properties.get(Context.SECURITY_CREDENTIALS);
        if (pwd == null)
            pwd = "*";

        Principal principal = new Principal(user, pwd);

        if (local)
            localClient = new ClientLocal(principal);

        if (remote) {
            try {
                remoteClient = new RemoteClient(properties, principal);
            } catch (NamingException ne) {
                remote = false;
                if (local) {
                    if (Registry.logFile != null)
                        Registry.logFile
                                .writeDate("WARNING: remote connection is configured as enabled but not available, disabling it.");
                } else {
                    throw ne;
                }
            }
        }

    }

    /**
     * @see javax.naming.Context#lookup(Name)
     */
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    /**
     * @see javax.naming.Context#lookup(String)
     */
    public Object lookup(String name) throws NamingException {
        try {
            if (local) {
                Object o = localClient.lookup(name);
                if (o != null)
                    return o;
            }

            if (remote) {
                return remoteClient.lookup(name);
            }
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            NamingException ne = new NamingException(e.getMessage());
            ne.initCause(e);
            throw ne;
        }
        throw new NamingException("not found: " + name);
    }

    /**
     * @see javax.naming.Context#bind(Name, Object)
     */
    public void bind(Name name, Object obj) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#bind(String, Object)
     */
    public void bind(String name, Object obj) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#rebind(Name, Object)
     */
    public void rebind(Name name, Object obj) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#rebind(String, Object)
     */
    public void rebind(String name, Object obj) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#unbind(Name)
     */
    public void unbind(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#unbind(String)
     */
    public void unbind(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#rename(Name, Name)
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#rename(String, String)
     */
    public void rename(String oldName, String newName) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#list(Name)
     */
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#list(String)
     */
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#listBindings(Name)
     */
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#listBindings(String)
     */
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#destroySubcontext(Name)
     */
    public void destroySubcontext(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#destroySubcontext(String)
     */
    public void destroySubcontext(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#createSubcontext(Name)
     */
    public Context createSubcontext(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#createSubcontext(String)
     */
    public Context createSubcontext(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#lookupLink(Name)
     */
    public Object lookupLink(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#lookupLink(String)
     */
    public Object lookupLink(String name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#getNameParser(Name)
     */
    public NameParser getNameParser(Name name) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#getNameParser(String)
     */
    public NameParser getNameParser(final String name0) throws NamingException {
        return new NameParser() {
            
            public Name parse(String name) throws NamingException {
                return new CompositeName(name);
            }
        };
    }

    /**
     * @see javax.naming.Context#composeName(Name, Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#composeName(String, String)
     */
    public String composeName(String name, String prefix) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#addToEnvironment(String, Object)
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#removeFromEnvironment(String)
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new NamingException("not allowed");
    }

    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException {
    }

    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException {
        throw new NamingException("not allowed");
    }

}
