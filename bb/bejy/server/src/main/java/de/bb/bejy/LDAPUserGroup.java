/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/LDAPUserGroup.java,v $
 * $Revision: 1.12 $
 * $Date: 2014/06/23 19:02:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import de.bb.util.LogFile;
import de.bb.util.SessionManager;

/**
 * This class implements the UserGroupDbi which performs a check against a LDAP
 * server.
 * 
 * @author bebbo
 * @version $Revision: 1.12 $
 */
public class LDAPUserGroup extends Configurable implements UserGroupDbi, Loadable {
    private SessionManager<String, Collection<String>> sessionMan = new SessionManager<String, Collection<String>>(15 * 60 * 1000L);

    private String ldapClass;

    private String ldapUrl;

    private String ldapLoginQuery;

    private String ldapRolesQuery;

    private String ldapBaseDN;

    private String ldapRolesBaseDN;

    private String ldapUser;

    private String ldapPassword;

    private String securityProtocol;

    private final static String[][] PROPERTIES = {
            { "name", "group name" },
            { "ldapBase", "base definition", "ou=people,dc=example,dc=xyz" },
            { "ldapClass", "ldap factory class name", "com.sun.jndi.ldap.LdapCtxFactory" },
            { "ldapQuery", "the LDAP search to yield the user",
                    "(&(objectClass=inetOrgPerson)(uid={u})(ismemberof=cn=user,ou=some,ou=groups,dc=example,dc=xyz))" },
            { "ldapQueryRoles", "the LDAP search to get the users roles", "((objectClass=groupOfUniqueNames)(uniqueMember={U}))" },
            { "ldapRolesBase", "roles base to search for groups", "ou=some,ou=groups,dc=example,dc=xyz" },
            { "ldapUser", "the LDAP user" }, { "ldapPassword", "password for LDAP user" }, { "ldapUrl", "url for LDAP connection" },
            { "ldapSecurity", "use SSL if set", "" }, { "cacheTimeout", "timeout for ldap response cache", "5" } };

    private static final Collection<String> DEFAULT = new ArrayList<String>();
    {
        DEFAULT.add("DEFAULT");
    }

    /**
     * Creates a user group which is maintained in the config file.
     */
    public LDAPUserGroup() {
        init("DLAP user group", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.LDAPgroup";
    }

    /**
     * Is a user with that password in the specified group?
     * 
     * @param userName
     *            a String containing the user name
     * @param pass
     *            a String containing the password
     * @param group
     *            a String containing the group name
     * @return true if that user/password/group combination exists, false
     *         either.
     */
    public synchronized Collection<String> verifyUserGroup(String userName, String pass) {
        if (userName == null || pass == null)
            return null;

        Collection<String> roles = sessionMan.get(userName);
        if (roles != null)
            return roles;

        DirContext ctx = null;
        try {
            if (pass.length() == 0)
                pass = "$";

            Hashtable<String, String> env = new Hashtable<String, String>(11);

            env.put(Context.INITIAL_CONTEXT_FACTORY, this.ldapClass);
            env.put(Context.PROVIDER_URL, this.ldapUrl);
            env.put(Context.SECURITY_PRINCIPAL, ldapUser);
            env.put(Context.SECURITY_CREDENTIALS, ldapPassword);

            if ("ssl".equals(securityProtocol)) {
                env.put(Context.SECURITY_PROTOCOL, securityProtocol);
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
            }

            try {
                ctx = new InitialDirContext(env);
            } catch (Exception e) {
                env.put(Context.SECURITY_CREDENTIALS, "********");
                throw new Exception("InitialDirContext failed with: " + env, e);
            }

            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, ldapUser);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, ldapPassword);

            String query = ldapLoginQuery.replace("{u}", userName);

            NamingEnumeration<?> ne = null;
            try {
                ne = ldapQuery(ctx, query, this.ldapBaseDN);
            } catch (Exception e) {
                System.out.println(e);
            }
            if (ne == null || !ne.hasMore()) {
                System.out.println("query: " + query);
                System.out.println("baseDn: " + this.ldapBaseDN);
                System.out.println("no ldap user: " + userName);
                return null;
            }

            SearchResult sres = (SearchResult) ne.next();
            String userDn = sres.getNameInNamespace();

            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDn);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, pass);

            ne = ldapQuery(ctx, query, userDn);
            if (ne.hasMore()) {
                roles = new ArrayList<String>();
                roles.addAll(DEFAULT);
                // read roles from LDAP
                if (ldapRolesQuery.length() > 0) {
                    ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, ldapUser);
                    ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, ldapPassword);
                    
                    final String rolesQuery = ldapRolesQuery.replace("{%U}", userDn);

                    try {
                        ne = ldapQuery(ctx, rolesQuery, this.ldapRolesBaseDN);
                        while(ne.hasMore()) {
                            SearchResult rres = (SearchResult) ne.next();
                            roles.add(rres.getName().substring(3).toUpperCase());
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                // add to SessionManager
                sessionMan.put(userName, roles);
                return roles;
            }

            System.out.println("invalid password for ldap user: " + userName);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }
        }
        return null;
    }

    /**
     * Method ldapQuery.
     * 
     * @param bindDn
     * @param password
     * @param query
     * @param baseDn
     * @return NamingEnumeration
     */
    private static NamingEnumeration<?> ldapQuery(DirContext ctx, String query, String baseDn) throws NamingException {

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        return ctx.search(baseDn, query, sc);

    }

    public synchronized void activate(LogFile logFile) throws Exception {
        String sTimeout = getProperty("cacheTimeout", "5400000");
        try {
            int timeout = Integer.parseInt(sTimeout);
            this.sessionMan.setTimeout(timeout);
        } catch (Exception ex) {
        }

        this.ldapUrl = getProperty("ldapUrl");
        this.ldapClass = getProperty("ldapClass");
        this.ldapUser = getProperty("ldapUser");
        this.ldapPassword = getProperty("ldapPassword");
        this.ldapLoginQuery = getProperty("ldapQuery");
        this.ldapRolesQuery = getProperty("ldapQueryRoles");
        this.ldapBaseDN = getProperty("ldapBase");
        this.ldapRolesBaseDN = getProperty("ldapRolesBase");
        this.securityProtocol = getProperty("ldapSecurity");

        super.activate(logFile);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */
    public void deactivate(LogFile logFile) throws Exception {
        sessionMan.clear();
        super.deactivate(logFile);
    }

    /**
     * Returns the id of the Configurator. The config group extends a
     * "de.bb.bejy.group".
     * 
     * @return the id of the Configurator.
     */
    public String getImplementationId() {
        return "de.bb.bejy.group";
    }

    public boolean hashPassword() {
        return false; // we need the password
    }

}
/******************************************************************************
 * $Log: LDAPUserGroup.java,v $ Revision 1.12 2014/06/23 19:02:58 bebbo
 * 
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2
 *    with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 *    Revision 1.11 2012/11/13 06:39:33 bebbo
 * @I code cleanup
 * @R property changed to support better LDAP queries. Revision 1.10 2006/03/17
 *    11:29:09 bebbo
 * 
 * @I activate is now synchronized
 * 
 *    Revision 1.9 2004/05/06 10:42:03 bebbo
 * @R context is no longer a singleton
 * 
 *    Revision 1.8 2004/04/20 13:20:20 bebbo
 * @B fixed possible NPE if password is null
 * 
 *    Revision 1.7 2004/04/16 13:40:44 bebbo
 * @R now usable as Loadable for GroupCfg
 * 
 *    Revision 1.6 2004/03/23 11:09:06 bebbo
 * @I internal method ldapQuery is no longer synchronized
 * 
 *    Revision 1.5 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 */
