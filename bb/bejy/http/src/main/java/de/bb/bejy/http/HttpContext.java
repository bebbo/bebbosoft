/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpContext.java,v $
 * $Revision: 1.53 $
 * $Date: 2014/10/21 20:37:22 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Context implementation
 *
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.http;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.LogManager;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.bejy.UserGroupDbi;
import de.bb.bejy.http.FilterRegistration.MappingData;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.SessionManager;
import de.bb.util.Trie;
import de.bb.util.ZipClassLoader;

/**
 * 
 * @author bebbo
 */
public class HttpContext extends Configurable implements
        javax.servlet.ServletContext, javax.servlet.http.HttpSessionContext {
    private final static boolean DEBUG = HttpProtocol.DEBUG;

    private final static String PROPERTIES[][] = {
            { "path", "the local path" },
            { "urlPath", "the URL context path" },
            { "group", "optional name of a user group to restrict access" },
            { "realm", "a descriptive name for this realm" }, };

    private static final ArrayList<String> EMPTYLIST = new ArrayList<String>();

    // the context parameters
    HashMap<String, String> parameter = new HashMap<String, String>();

    // configurated values
    Host hServer;

    String sContext, sPath;

    int scLen;

    String aGroup, aRealm;

    UserGroupDbi verify = null; // interface to verify users;

    // private StringTree handlerByPath = new StringTree();
    private Trie handlerByPath = new Trie();

    private HashMap<String, HttpHandler> handlerByName = new HashMap<String, HttpHandler>();

    private HashMap<String, HttpHandler> handlerByExtension = new HashMap<String, HttpHandler>();

    private HashMap<String, HttpHandler> handlerExact = new HashMap<String, HttpHandler>();

    HashMap<String, Object> attributes = new HashMap<String, Object>(); // attributes
                                                                        // for
                                                                        // this
                                                                        // context

    // listeners
    ArrayList<ServletContextListener> sclv = new ArrayList<ServletContextListener>(); // for
                                                                                      // this
                                                                                      // context

    ArrayList<ServletContextAttributeListener> scalv = new ArrayList<ServletContextAttributeListener>(); // for
                                                                                                         // this
                                                                                                         // context
                                                                                                         // attributes

    ArrayList<HttpSessionListener> hslv = new ArrayList<HttpSessionListener>(); // for
                                                                                // this
                                                                                // context's
                                                                                // sessions

    ArrayList<javax.servlet.http.HttpSessionAttributeListener> hsalv = new ArrayList<HttpSessionAttributeListener>(); // for
                                                                                                                      // this
                                                                                                                      // context's
                                                                                                                      // session
                                                                                                                      // attributes

    ArrayList<javax.servlet.ServletRequestListener> srlv; // for the request's
                                                          // listeners

    ArrayList<javax.servlet.ServletRequestAttributeListener> sralv; // for the
                                                                    // request's
                                                                    // attributes
                                                                    // listeners

    ArrayList<MappingData> requestFilterVector = new ArrayList<MappingData>();

    ArrayList<MappingData>includeFilterVector = new ArrayList<MappingData>();

    ArrayList<MappingData>forwardFilterVector = new ArrayList<MappingData>();

    ArrayList<MappingData> errorFilterVector = new ArrayList<MappingData>();

    HashMap<String, String> exceptionMap = new HashMap<String, String>();

    HashMap<String, String> statusMap = new HashMap<String, String>();

    HashMap<String, String> mimeTypes = new HashMap<String, String>();

    MultiMap<String, SecurityConstraint> constraints = new MultiMap<String, SecurityConstraint>(
            SLC.instance);

    SessionManager<String, HttpSession> sessionManager = new SessionManager<String, HttpSession>(
            15 * 60 * 1000L);

    long loadTime;

    /** own classloader to load WAR data */
    ZipClassLoader zcl;

    boolean initialized;

    protected Injector injector;

    HashMap<String, FilterRegistration.Dynamic> filterRegistrations = new HashMap<String, FilterRegistration.Dynamic>();
    HashMap<String, ServletRegistration.Dynamic> servletRegistrations = new HashMap<String, ServletRegistration.Dynamic>();

    ArrayList<MappingData> frontQueue = new ArrayList<FilterRegistration.MappingData>();
    ArrayList<MappingData> midQueue = new ArrayList<FilterRegistration.MappingData>();
    ArrayList<MappingData> afterQueue = new ArrayList<FilterRegistration.MappingData>();

    String createSession(HttpSession hs) {
        String r = SessionManager.newKey();
        sessionManager.put(r, hs);
        hs.sessionId = r;
        if (hslv != null) {
            javax.servlet.http.HttpSessionEvent hse = new javax.servlet.http.HttpSessionEvent(
                    hs);
            for (Iterator<javax.servlet.http.HttpSessionListener> e = hslv
                    .iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionListener hsl = e.next();
                hsl.sessionCreated(hse);
            }
        }
        return r;
    }

    void removeSession(HttpSession hs) {
        sessionManager.remove(hs.sessionId);
        if (hslv != null) {
            javax.servlet.http.HttpSessionEvent hse = new javax.servlet.http.HttpSessionEvent(
                    hs);
            for (Iterator<javax.servlet.http.HttpSessionListener> e = hslv
                    .iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionListener hsl = e.next();
                hsl.sessionDestroyed(hse);
            }
        }
    }

    HttpServletRequest checkAccess(String method, String localPath,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (verify == null) {
            return request;
        }
        if (constraints.isEmpty())
            return request;
        // find longest pattern which also matches the used method
        SecurityConstraint sc = lookupConstraint(method, localPath);
        // System.out.println(method + " " + localPath + "=" + sc);
        if (sc == null)
            return request;

        final HttpRequest r = RequestDispatcher.dereference(request);
        Collection<String> roles = null;
        if (verify instanceof FormVerification) {
            final HttpSession session = (HttpSession) request.getSession(true);
            String remoteUser = (String) session.getAttribute("j_username");
            if (remoteUser == null) {
                final FormVerification fv = (FormVerification) verify;
                if (localPath.equals(fv.loginPage)
                        || localPath.equals(fv.loginErrorPage)
                        || sc.isAllowAll())
                    return request;

                if (!localPath.endsWith("/j_security_check")) {
                    session.setAttribute("j_redirect", request.getRequestURI());
                    response.sendRedirect(this.sContext + fv.loginPage);
                    return null;
                }
                try {
                    fv.handler.service(request, response);
                    remoteUser = (String) session.getAttribute("j_username");
                } catch (ServletException e) {
                    response.setStatus(500, e.getMessage());
                    return null;
                }
            }
            roles = (Collection<String>) session.getAttribute("j_user_roles");
            r.remoteUser = remoteUser;
        } else {
            if (r.remoteUser == null || r.remotePass == null) {
                response.addHeader("WWW-Authenticate", "Basic realm=\""
                        + aRealm + "\"");
                response.setStatus(401);
                return null;
            }

            roles = verify.verifyUserGroup(r.remoteUser, r.remotePass);
        }
        if (roles != null) {
            final HashSet<String> roleLookup = new HashSet<String>();
            roleLookup.addAll(roles);
            final StringTokenizer st = sc.roles();
            for (; st.hasMoreElements();) {
                String role = st.nextToken();
                // System.out.println("usergroup: " + vfy);
                if ("*".equals(role) || roleLookup.contains(role)) {
                    final HttpSession session = (HttpSession) request
                            .getSession(true);
                    session.setAttribute("j_user_roles", roleLookup);
                    if (verify instanceof FormVerification) {
                        final String previousUri = (String) session
                                .getAttribute("j_redirect");
                        if (previousUri != null) {
                            session.removeAttribute("j_redirect");
                            response.sendRedirect(previousUri);
                            return null;
                        }
                    }
                    return request;
                }
            }
        }

        if (verify instanceof FormVerification) {
            final FormVerification fv = (FormVerification) verify;
            if (fv.loginErrorPage != null) {
                response.sendRedirect(this.sContext + fv.loginErrorPage);
                return null;
            }
        }
        response.setStatus(403);
        return null;
    }

    /**
     * @param method
     * @param localPath
     * @return
     */
    private SecurityConstraint lookupConstraint(String method, String localPath) {
        // start search with longest pattern, till we find a match
        SecurityConstraint longestScConstraint = null;
        int longestPl = 0;
        for (Iterator<String> i = constraints.keySet().iterator(); i.hasNext();) {
            String pattern = i.next();

            // System.out.println(pattern);

            int pl = pattern.length() - 1;
            if (localPath.equals(pattern) || (pattern.charAt(pl) == '*')
                    && localPath.startsWith(pattern.substring(0, pl))) {
                // found a matching pattern, no check all configured SCs for
                // method
                SecurityConstraint sc = constraints.get(pattern);
                if (sc.containsMethod(method)) {
                    if (longestScConstraint == null || pl > longestPl) {
                        longestScConstraint = sc;
                        longestPl = pl;
                    }
                }
            }
        }
        return longestScConstraint;
    }

    /**
     * CT.
     */
    protected HttpContext() {
        init("context", PROPERTIES);
    }

    public String getName() {
        String name = sContext == null || sContext.length() == 0 ? "/"
                : sContext;
        return name;
    }

    public void activate(LogFile logFile) throws Exception {
        activate1(logFile);
        activate2(logFile);
        initialize(logFile);
    }

    protected void activate1(LogFile logFile) throws Exception {
        if (hServer == null) {
            for (Configurable p = getParent(); p != null; p = p.getParent()) {
                if (p instanceof Host) {
                    hServer = (Host) p;
                    break;
                }
            }
        }
        if (hServer == null) {
            throw new Exception("no vhost for this context:" + this.getName());
        }

        sContext = getProperty("urlPath", "/");
        sPath = getProperty("path", "");
        aGroup = getProperty("group", "");
        aRealm = getProperty("realm", sContext);
        if (aRealm.length() == 0) {
            aRealm = null;
        }

        for (int idx = sPath.indexOf('\\'); idx >= 0; idx = sPath.indexOf('\\',
                idx)) {
            sPath = sPath.substring(0, idx) + '/' + sPath.substring(idx + 1);
        }

        if (aGroup != null) {
            final UserGroupDbi vfy = Config.getGroup(aGroup);

            final String loginJsp = getRealPath("loginPage.jsp");
            if (new File(loginJsp).exists()) {
                verify = new FormVerification("loginPage.jsp",
                        "loginErrorPage.jsp", new ServletHandler() {
                            @Override
                            public void service(ServletRequest in,
                                    ServletResponse out) throws IOException,
                                    ServletException {
                                final String userName = in
                                        .getParameter("j_username");
                                final String password = in
                                        .getParameter("j_password");
                                final Collection<String> permissions = vfy
                                        .verifyUserGroup(userName, password);
                                final HttpServletRequest hsr = (HttpServletRequest) in;
                                final javax.servlet.http.HttpSession session = hsr
                                        .getSession(true);
                                if (permissions != null) {
                                    session.setAttribute("j_username", userName);
                                    session.setAttribute("j_user_roles",
                                            permissions);
                                }
                            }
                        });
            } else {
                verify = vfy;
            }

            SecurityConstraint sc = new SecurityConstraint(aGroup);
            sc.addMethod("*");
            sc.addRole("DEFAULT");
            constraints.put("/*", sc);
        }

        if (!sPath.endsWith("/")) {
            sPath += '/';
        }

        if (sContext.endsWith("/")) {
            sContext = sContext.substring(0, sContext.length() - 1);
        }

        scLen = sContext.length();
    }

    void activate2(LogFile logFile) throws Exception {
        parameter.clear();
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable o = i.next();
            if (o instanceof ParameterCfg) {
                String name = o.getProperty("name");
                String value = o.getProperty("value");
                if (name != null && value != null) {
                    parameter.put(name, value);
                }
                continue;
            }
            if (o instanceof HttpHandler) {
                HttpHandler h = (HttpHandler) o;
                h.activate(logFile);
                String mask = o.getProperty("mask", "*");
                addHandler(mask, h);
                logFile.writeDate("added:     handler: " + mask + ": "
                        + h.getName());
            }
        }
    }

    public javax.servlet.RequestDispatcher getRequestDispatcher(
            java.lang.String path) // from
                                   // javax.servlet.ServletContext
    {
        if (DEBUG) {
            System.out.println("getReqDisp:" + path);
        }

        String query = null;
        String pathInfo = null;

        int q = path.indexOf('?');
        if (q > 0) {
            query = path.substring(q + 1);
            path = path.substring(0, q);
        }

        // remove all "../" parts from path
        for (int ppp = path.indexOf("/../"); ppp > 0; ppp = path
                .indexOf("/../")) {
            int ldir = path.lastIndexOf("/", ppp - 1);
            if (ldir < 0) {
                ldir = path.lastIndexOf("\\", ppp - 1);
            }
            path = path.substring(0, ldir) + path.substring(ppp + 3);
        }

        HttpHandler handler = handlerExact.get(path);

        if (handler == null) {
            // StringTree node = handlerByPath.getNode(path);
            Trie node = handlerByPath.searchLast(path, 1);
            if (node != null) {
                int len = node.length();
                pathInfo = path.substring(len);
                path = path.substring(0, len);
                handler = (HttpHandler) node.getObject();
                if (pathInfo.length() == 0) {
                    pathInfo = null;
                }
            }
            if (handler == null) {
                // search handlers by extension
                int dot = path.lastIndexOf('.');
                if (dot >= 0) {
                    String extension = path.substring(dot + 1);
                    int semi = extension.indexOf(';');
                    if (semi >= 0) {
                        extension = extension.substring(0, semi);
                    }
                    handler = handlerByExtension.get(extension);
                    if (handler != null && semi >= 0) {
                        int len = dot + 1 + semi;
                        pathInfo = path.substring(len);
                        path = path.substring(0, len);
                    }
                }

                // add the default handler, which might be the file handler!?
                if (handler == null) {
                    handler = handlerByName.get("default");
                }
            }
        }

        RequestDispatcher rd = new RequestDispatcher(this, path, pathInfo,
                query, handler);

        if (DEBUG) {
            System.out.println(rd);
        }
        return rd;

    }

    String localPath(String path) {
        int sl = sContext.length();
        if (path.length() < sl) {
            return "";
        }
        return path.substring(sl);
    }

    boolean matchPath(String path) {
        int pl = path.length();
        int sl = sContext.length();
        if (pl < sl) {
            return false;
        }

        return path.substring(0, sl).equals(sContext.substring(0, sl));
    }

    // attribute stuff
    /**
     * Returns the value of the named attribute as an Object, or null if no
     * attribute of the given name exists. Attributes can be set two ways. The
     * servlet container may set attributes to make available custom
     * informa-tion about a request. For example, for requests made using HTTPS,
     * the attribute javax.servlet.request.X509Certificate can be used to
     * retrieve information on the certificate of the client. Attributes can
     * also be set programatically using setAttribute(String, Object) . This
     * allows information to be embedded into a request before a
     * RequestDispatcher call. Attribute names should follow the same
     * conventions as package names. This specification reserves names matching
     * java.*, javax.*, and sun.*.
     * 
     * @param name
     *            - a String specifying the name of the attribute
     * @return an Object containing the value of the attribute, or null if the
     *         attribute does not exist
     */

    public java.lang.Object getAttribute(java.lang.String name) {
        return attributes.get(name);
    }

    /**
     * Returns an Enumeration containing the names of the attributes available
     * to this request. This method returns an empty Enumeration if the request
     * has no attributes available to it.
     * 
     * @return an Enumeration of strings containing the names of the request???s
     *         attributes
     */

    public java.util.Enumeration<String> getAttributeNames() {
        return new IterEnum<String>(attributes.keySet().iterator());
    }

    /**
     * Removes an attribute from this request. This method is not generally
     * needed as attributes only persist as long as the request is being
     * handled. Attribute names should follow the same conventions as package
     * names. Names beginning with java.*, javax.*, and com.sun.*, are reserved
     * for use by Sun Microsystems.
     * 
     * @param name
     *            - a String specifying the name of the attribute to remove
     */

    public void removeAttribute(java.lang.String name) {
        Object o = attributes.remove(name);
        if (null != o && scalv.size() > 0) {
            javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(
                    this, name, o);
            for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv
                    .iterator(); e.hasNext();) {
                javax.servlet.ServletContextAttributeListener scal = e.next();
                scal.attributeRemoved(scae);
            }
        }
    }

    /**
     * Stores an attribute in this request. Attributes are reset between
     * requests. This method is most often used in conjunction with
     * RequestDispatcher . Attribute names should follow the same conventions as
     * package names. Names beginning with java.*, javax.*, and com.sun.*, are
     * reserved for use by Sun Microsystems.
     * 
     * @param name
     *            - a String specifying the name of the attribute
     * @param o
     *            - the Object to be stored
     */

    public void setAttribute(java.lang.String name, java.lang.Object o) {
        Object old = attributes.put(name, o);
        if (scalv.size() > 0) {
            if (old == null) {
                javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(
                        this, name, o);
                for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv
                        .iterator(); e.hasNext();) {
                    javax.servlet.ServletContextAttributeListener scal = e
                            .next();
                    scal.attributeAdded(scae);
                }
            } else {
                javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(
                        this, name, old);
                for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv
                        .iterator(); e.hasNext();) {
                    javax.servlet.ServletContextAttributeListener scal = e
                            .next();
                    scal.attributeReplaced(scae);
                }
            }
        }
    }

    void initialize(LogFile log) {
        initialized = true;
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 3;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void log(Exception e, String s) {
        de.bb.bejy.Config.getLogFile().writeDate(e.getMessage() + "\r\n" + s);
    }

    public void log(String s) {
        de.bb.bejy.Config.getLogFile().writeDate(s);
    }

    public void log(String s, Throwable t) {
        de.bb.bejy.Config.getLogFile().writeDate(s + "\r\n" + t.getMessage());
    }

    public javax.servlet.ServletContext getContext(java.lang.String s) {
        return hServer.getContext(s);
    }

    public java.lang.String getInitParameter(java.lang.String p) // from
                                                                 // javax.servlet.ServletContext
    {
        return parameter.get(p);
    }

    public java.util.Enumeration<String> getInitParameterNames()
    // from javax.servlet.ServletContext
    {
        return new IterEnum<String>(parameter.keySet().iterator());
    }

    public java.lang.String getMimeType(java.lang.String mo) // from
                                                             // javax.servlet.ServletContext
    {
        int dot = mo.lastIndexOf('.');
        if (dot > 0) {
            mo = mo.substring(dot + 1);
        }

        String mt = mimeTypes.get(mo);
        if (mt != null) {
            return mt;
        }
        return MimeTypesCfg.getMimeType(mo);
    }

    /**
     * Converts the given path relative to this context into a real path
     */

    public java.lang.String getRealPath(java.lang.String path) // from
                                                               // javax.servlet.ServletContext
    {
        if (path == null) {
            return null;
        }
        int l = path.length() > 0 && path.charAt(0) == '/' ? 1 : 0;
        // return sPath + path.substring(l);
        File f = new File(sPath, path.substring(l));
        if (f.isDirectory()) {
            return f.getAbsolutePath() + File.separatorChar;
        }
        return f.getAbsolutePath();
    }

    /**
     * Converts the given path relative to this context into a real path always
     * using '/' as separator
     */
    java.lang.String getRealPath2(java.lang.String path) // from
                                                         // javax.servlet.ServletContext
    {
        if (path == null) {
            return null;
        }
        int l = path.length() > 0 && path.charAt(0) == '/' ? 1 : 0;
        return sPath + path.substring(l);
    }

    public java.lang.String getServerInfo() // from javax.servlet.ServletContext
    {
        return HttpProtocol.getVersion();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public javax.servlet.Servlet getServlet(java.lang.String s) // from
                                                                // javax.servlet.ServletContext
    {
        return handlerByName.get(s);
    }

    // get a dispatcher for the supplied servlet name

    public javax.servlet.RequestDispatcher getNamedDispatcher(
            java.lang.String dispatcherName) // from
                                             // javax.servlet.ServletContext
    {
        HttpHandler h = handlerByName.get(dispatcherName);
        RequestDispatcher rd = new RequestDispatcher(this, null, null, null, h);
        return rd;
    }

    public java.lang.String getServletContextName()
    // from javax.servlet.ServletContext
    {
        String name = getName();
        if (hServer != null) {
            String server = hServer.getProperty("name");
            if (server.indexOf('*') >= 0) {
                server = "__default__";
            }
            Configurable p = hServer.getParent().getParent();
            name = p.getName() + "-" + p.getProperty("port") + "/" + server
                    + name;
        }
        return name;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public java.util.Enumeration<String> getServletNames()
    // from javax.servlet.ServletContext
    {
        return new IterEnum<String>(handlerByName.keySet().iterator());
    }

    /**
     * @deprecated
     */
    @Deprecated
    public java.util.Enumeration getServlets()
    // from javax.servlet.ServletContext
    {
        return new IterEnum<HttpHandler>(handlerByName.values().iterator());
    }

    // open

    public java.net.URL getResource(java.lang.String s) // from
                                                        // javax.servlet.ServletContext
    {
        try {
            String rp = getRealPath(s);
            if (new java.io.File(rp).exists()) {
                return new java.net.URL("file:" + rp);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    public java.io.InputStream getResourceAsStream(java.lang.String s) // from
                                                                       // javax.servlet.ServletContext
    {
        try {
            return new java.io.FileInputStream(getRealPath(s));
        } catch (Exception e) {
        }
        return null;
    }

    public java.util.Set<?> getResourcePaths() // from
                                               // javax.servlet.ServletContext
    {
        return null;
    }

    public java.util.Set getResourcePaths(String s) // from
                                                    // javax.servlet.ServletContext
    {
        return null;
    }

    // deprecated dummies
    /**
     * @deprecated
     */
    @Deprecated
    public java.util.Enumeration<String> getIds() {
        return new IterEnum<String>(EMPTYLIST.iterator());
    }

    /**
     * @deprecated
     */
    @Deprecated
    public javax.servlet.http.HttpSession getSession(java.lang.String sessionId) {
        return null;
    }

    /**
     * to release once allocated resources.
     */
    protected void destroy() {
        // report session destroyed and drop the session.
        for (final Iterator<String> i = sessionManager.keys(); i.hasNext();) {
            final HttpSession hs = (HttpSession) sessionManager.get(i.next());
            if (hs.context == this) {
                final HttpSessionEvent paramHttpSessionEvent = new HttpSessionEvent(
                        hs);
                for (final HttpSessionListener hsl : hslv) {
                    try {
                        hsl.sessionDestroyed(paramHttpSessionEvent);
                    } catch (Throwable t) {
                        log(t.getMessage(), t);
                    }
                }
                i.remove();
            }
        }

        // report context destroyed
        final ServletContextEvent sce = new ServletContextEvent(this);
        for (final ServletContextListener scl : sclv) {
            try {
                scl.contextDestroyed(sce);
            } catch (Throwable t) {
                log(t.getMessage(), t);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */

    public void deactivate(LogFile logFile) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            destroy();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        parameter.clear();
        handlerByExtension.clear();
        handlerByName.clear();
        // handlerByPath.clear();
        handlerByPath = new Trie();
        attributes.clear();

        // listeners
        sclv.clear();
        scalv.clear();
        hslv.clear();
        hsalv.clear();
        requestFilterVector = null;
        includeFilterVector = null;
        forwardFilterVector = null;

        exceptionMap.clear();
        mimeTypes.clear();

        killThreads();
        killDrivers();
        LogManager.getLogManager().reset();

        super.deactivate(logFile);
    }

    // private void killLoggers() {
    // final LogManager lm = LogManager.getLogManager();
    // final ArrayList<String> names = new ArrayList<String>();
    // for (final Enumeration<String> e = lm.getLoggerNames();
    // e.hasMoreElements();) {
    // names.add(e.nextElement());
    // }
    // for (final String name : names) {
    // final Logger l = lm.getLogger(name);
    // if (refersToMe(l.getClass().getClassLoader())) {
    // lm.
    // }
    // }
    // }

    private void killDrivers() {
        try {
            List<?> drivers = (List<?>) getMember(null, DriverManager.class,
                    "registeredDrivers");
            for (Object driverInfo : new ArrayList<Object>(drivers)) {
                final Driver driver = (Driver) getMember(driverInfo, "driver");
                if (refersToMe(driver.getClass().getClassLoader())) {
                    try {
                        log("ERROR JDBC driver not deregistered: " + driver
                                + " trying to deregister");
                        drivers.remove(driverInfo);
                    } catch (Exception e) {
                        log("ERROR while deregistering: " + driver + ": "
                                + e.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            log("ERROR while checking JDBC drivers: " + ex.getMessage());
        }
    }

    private boolean refersToMe(ClassLoader classLoader) {
        while (classLoader != null) {
            if (classLoader == zcl)
                return true;
            classLoader = classLoader.getParent();
        }
        return false;
    }

    private void killThreads() {
        final Thread iam = Thread.currentThread();

        // can't kill ourselves
        if (zcl == iam.getContextClassLoader())
            return;

        boolean neededKill = false;
        for (final Thread t : getThreads()) {
            if (t.getContextClassLoader() == zcl) {
                if (t == iam)
                    continue;
                neededKill = true;
                log("WARN Thread " + t + " still running");
                t.interrupt();
            }
        }
        if (!neededKill)
            return;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        for (final Thread t : getThreads()) {
            if (t.getContextClassLoader() == zcl) {
                if (t == iam)
                    continue;
                log("ERROR Thread " + t + " still running - trying to kill");
                t.interrupt();
                try {
                    ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) getMember(
                            t, "target.this$0");
                    stpe.shutdown();
                } catch (Throwable tw) {
                    try {
                        t.stop();
                    } catch (Throwable t2) {
                    }
                }
                t.setContextClassLoader(null);
            }
        }
    }

    /**
     * Get all threads. (really? There are threads not inside thread groups!!!)
     * 
     * @return an array with all threads.
     */
    private static Thread[] getThreads() {
        for (int size = 200;; size += size) {
            final Thread[] threads = new Thread[size];
            // start searching from the topmost thread group to search all
            // threads
            ThreadGroup root = Thread.currentThread().getThreadGroup();
            while (root.getParent() != null) {
                root = root.getParent();
            }
            int n = root.enumerate(threads);
            if (n < size) {
                final Thread[] result = new Thread[n];
                System.arraycopy(threads, 0, result, 0, n);
                return result;
            }
        }
    }

    public void addHandler(String mask, HttpHandler h) {
        h.hContext = this;
        String name = h.getName();
        handlerByName.put(name, h);
        if ("/".equals(mask) || "*".equals(mask)) {
            handlerByName.put("default", h);
            return;
        }

        if (mask.startsWith("/") && mask.endsWith("/*")) {
            mask = mask.substring(0, mask.length() - 2);
            // handlerByPath.put(mask, h);
            Trie t = handlerByPath.insert(mask, 1);
            t.setObject(h);
            return;
        }

        if (mask.startsWith("*.")) {
            handlerByExtension.put(mask.substring(2), h);
            if ("*.jsp".equals(mask)) {
                handlerByName.put("jsp", h);
            }
            return;
        }

        handlerExact.put(mask, h);
    }

    static class SLC implements Comparator<String> {

        static SLC instance = new SLC();

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */

        public int compare(String s1, String s2) {
            return s1.length() - s2.length();
        }
    }

    public String getContextPath() {
        return sContext;
    }

    private static Object getMember(Object reference, String memberName) {
        try {
            for (StringTokenizer st = new StringTokenizer(memberName, "."); st
                    .hasMoreTokens();) {
                reference = getMember(reference, reference.getClass(),
                        st.nextToken());
            }
            return reference;
        } catch (Exception ex) {
        }
        return null;
    }

    private static Object getMember(Object reference, Class<?> clazz,
            String memberName) throws NoSuchFieldException,
            IllegalAccessException {
        try {
            final Field memberField = clazz.getDeclaredField(memberName);
            memberField.setAccessible(true);
            final Object object = memberField.get(reference);
            return object;
        } catch (NoSuchFieldException nsfe) {
            if (clazz.getSuperclass() == null)
                throw nsfe;
            return getMember(reference, clazz.getSuperclass(), memberName);
        }
    }

    // since 3.0 / 3.1

    public javax.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, String filterClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Filter> clazz = (Class<Filter>) zcl
                    .loadClass(filterClassName);
            return addFilter(filterName, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, Class<? extends Filter> filterClass) {
        try {
            final Filter filter = createFilter(filterClass);
            return addFilter(filterName, filter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {
        if (initialized)
            throw new IllegalStateException("context already initialized");
        if (filterName == null)
            throw new IllegalArgumentException("filterName must not be null");

        FilterRegistration.Dynamic filterRegistration = filterRegistrations
                .get(filterName);
        if (filterRegistration == null) {
            filterRegistration = new de.bb.bejy.http.FilterRegistration.Dynamic(
                    this, filterName, filter);
            filterRegistrations.put(filterName, filterRegistration);
        }
        return filterRegistration;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, String servletClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Servlet> clazz = (Class<Servlet>) zcl
                    .loadClass(servletClassName);
            return addServlet(servletName, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        if (initialized)
            throw new IllegalStateException("context already initialized");
        if (servletName == null)
            throw new IllegalArgumentException("filterName must not be null");

        ServletRegistration.Dynamic servletRegistration = servletRegistrations
                .get(servletName);
        if (servletRegistration == null) {
            servletRegistration = new de.bb.bejy.http.ServletRegistration.Dynamic(
                    this, servletName, servlet);
            servletRegistrations.put(servletName, servletRegistration);
        }
        return servletRegistration;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, Class<? extends Servlet> servletClass) {
        try {
            return addServlet(servletName, createServlet(servletClass));
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Filter> T createFilter(Class<T> filterClass)
            throws ServletException {
        try {
            T filter = filterClass.newInstance();
            if (injector != null)
                injector.inject(null, filter);
            return filter;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public <T extends EventListener> T createListener(Class<T> listenerClass)
            throws ServletException {
        try {
            T listener = listenerClass.newInstance();
            if (injector != null)
                injector.inject(null, listener);
            return listener;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public <T extends Servlet> T createServlet(Class<T> servletClass)
            throws ServletException {
        try {
            T servlet = servletClass.newInstance();
            if (injector != null)
                injector.inject(null, servlet);
            return servlet;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public ClassLoader getClassLoader() {
        return zcl;
    }

    public void addListener(String arg0) {
        // TODO Auto-generated method stub

    }

    public <T extends EventListener> void addListener(T arg0) {
        // TODO Auto-generated method stub

    }

    public void addListener(Class<? extends EventListener> arg0) {
        // TODO Auto-generated method stub

    }

    public void declareRoles(String... arg0) {
        // TODO Auto-generated method stub

    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getEffectiveMajorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getEffectiveMinorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    public FilterRegistration getFilterRegistration(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    public javax.servlet.ServletRegistration getServletRegistration(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, ? extends javax.servlet.ServletRegistration> getServletRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean setInitParameter(String arg0, String arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
        // TODO Auto-generated method stub

    }

    public Set<String> addSecurity4Mappings(
            ServletSecurityElement servletSecurityElement, Set<String> keySet) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Collection<String> getServletNameMappings(Filter filter) {
        final ArrayList<String> al = new ArrayList<String>();
        for (final MappingData md : frontQueue) {
            if (md.filter == filter && md.servletName != null)
                al.add(md.servletName);
        }
        for (final MappingData md : midQueue) {
            if (md.filter == filter && md.servletName != null)
                al.add(md.servletName);
        }
        for (final MappingData md : afterQueue) {
            if (md.filter == filter && md.servletName != null)
                al.add(md.servletName);
        }
        return al;
    }

    public Collection<String> getUrlPatternMappings(Filter filter) {
        final ArrayList<String> al = new ArrayList<String>();
        for (final MappingData md : frontQueue) {
            if (md.filter == filter && md.urlPattern != null)
                al.add(md.urlPattern);
        }
        for (final MappingData md : midQueue) {
            if (md.filter == filter && md.urlPattern != null)
                al.add(md.urlPattern);
        }
        for (final MappingData md : afterQueue) {
            if (md.filter == filter && md.urlPattern != null)
                al.add(md.urlPattern);
        }
        return al;
    }

}

/******************************************************************************
 * $Log: HttpContext.java,v $ Revision 1.53 2014/10/21 20:37:22 bebbo
 * 
 * @R removed typed types if interface method without typed types is implemented
 *
 *    Revision 1.52 2014/09/19 19:56:55 bebbo
 * @B if a group is defined the role DEFAULT is required for access
 *
 *    Revision 1.51 2014/06/24 09:17:27 bebbo
 * @B fixed BASIC authentication
 * @I killThreads no longer tries to kill the System threads
 *
 *    Revision 1.50 2014/06/23 15:38:46 bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles Revision 1.49 2013/11/28
 *    10:27:58 bebbo
 * 
 * @B resource a security context now protects only the configured resources
 *    (and not all) Revision 1.48 2012/12/15 19:38:54 bebbo
 * 
 * @I refactoring Revision 1.47 2012/08/11 16:58:40 bebbo
 * 
 * @I explicit sessionId creation Revision 1.46 2012/07/18 06:44:48 bebbo
 * 
 * @I typified Revision 1.45 2011/01/07 13:48:18 bebbo
 * 
 * @R added servlet 2.5 compatibility Revision 1.44 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.43 2010/04/11 12:41:02 bebbo
 * @B fixed pathinfo: added leading slash
 * 
 *    Revision 1.42 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.41 2008/06/04 06:45:27 bebbo
 * @B fixed class loading issues on reload of web applications
 * 
 *    Revision 1.40 2008/03/13 20:43:38 bebbo
 * @O performance optimizations
 * 
 *    Revision 1.39 2008/01/17 17:31:36 bebbo
 * @I modified the context matching algorithm. Now a Trie is used.
 * 
 *    Revision 1.38 2007/05/01 19:05:27 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.37 2007/01/18 21:46:11 bebbo
 * @N added support for servlet 2.4 listeners
 * 
 *    Revision 1.36 2006/10/12 05:54:37 bebbo
 * @B reuse initial HostCfg in update()
 * 
 *    Revision 1.35 2006/05/09 12:13:26 bebbo
 * @R changes to comply to servlet2_4
 * 
 *    Revision 1.34 2006/02/06 09:15:18 bebbo
 * @I cleanup
 * 
 *    Revision 1.33 2005/11/18 14:46:03 bebbo
 * @B modified getServletContextName() to return the configured name instead of
 *    realm name
 * 
 *    Revision 1.32 2004/12/13 15:31:30 bebbo
 * @B fixed real path handling
 * 
 *    Revision 1.31 2004/04/20 13:22:18 bebbo
 * @R added support for SecurityConstraint
 * 
 *    Revision 1.30 2004/04/16 13:47:23 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group,
 *    Cfg, Factory
 * 
 *    Revision 1.29 2004/03/23 18:58:59 bebbo
 * @D added DEBUG output
 * 
 *    Revision 1.28 2004/03/23 11:13:37 bebbo
 * @B query strings in forward/include are now applied to the request
 * 
 *    Revision 1.27 2004/01/01 14:14:08 bebbo
 * @B fixed getURL() and changed file:// into file:
 * 
 *    Revision 1.26 2003/11/26 09:57:37 bebbo
 * @B fixed NPEs
 * 
 *    Revision 1.25 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.24 2003/06/18 13:36:04 bebbo
 * @R almost complete on the fly update.
 * 
 *    Revision 1.23 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.22 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.21 2003/01/07 18:32:20 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.20 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.19 2002/09/06 16:08:27 bebbo
 * @B fixed due to changes in de.bb.util.SessionManager
 * 
 *    Revision 1.18 2002/08/21 09:13:01 bebbo
 * @I added destroy() function
 * 
 *    Revision 1.17 2002/05/19 12:57:42 bebbo
 * @B fixed H401 and H302 handling
 * 
 *    Revision 1.16 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.15 2002/03/30 15:47:08 franke
 * @N added support for Filters
 * @B many fixes in incomplete Servlet 2.3 functions
 * 
 *    Revision 1.14 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some
 *    config function calls. Also added the use of a special ClassLoader.
 * 
 *    Revision 1.13 2002/01/26 15:26:25 bebbo
 * @B fixed the recognition of a default document
 * 
 *    Revision 1.12 2001/12/28 11:47:57 franke
 * @B implemented getResourceAsStream()
 * 
 *    Revision 1.11 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions.
 *    Caused some changes, since members from HttpProtocol moved.
 * 
 *    Revision 1.10 2001/11/26 16:52:36 bebbo
 * @B fixed matching algorithm for context selection - failed on short
 *    entries...
 * 
 *    Revision 1.9 2001/11/26 16:01:20 bebbo
 * @B normalize the path in get RequestDispatcher - remove "/../" and previous
 *    dir
 * 
 *    Revision 1.8 2001/11/20 17:36:42 bebbo
 * @B fixed RequestDispatcher stuff
 * 
 *    Revision 1.7 2001/09/15 08:46:39 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.6 2001/04/16 20:03:56 bebbo
 * @B fixes in 302 redirect
 * 
 *    Revision 1.5 2001/04/16 16:23:17 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.4 2001/04/16 13:43:54 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.3 2001/04/11 13:15:53 bebbo
 * @R if requested name matches a directory, a redirect is replied (302)
 * 
 *    Revision 1.2 2001/03/30 17:28:04 bebbo
 * @N added user authentication
 * 
 *    Revision 1.1 2001/03/29 07:08:44 bebbo
 * @N implements javax.servlet.ServletContext
 * 
 *****************************************************************************/
