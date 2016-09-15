/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
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
public class HttpContext extends Configurable implements javax.servlet.ServletContext, javax.servlet.http.HttpSessionContext {
    private final static boolean DEBUG = HttpProtocol.DEBUG;

    private final static String PROPERTIES[][] = { { "path", "the local path" }, { "urlPath", "the URL context path" },
            { "group", "optional name of a user group to restrict access" }, { "realm", "a descriptive name for this realm" }, };

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

    ArrayList<MappingData> includeFilterVector = new ArrayList<MappingData>();

    ArrayList<MappingData> forwardFilterVector = new ArrayList<MappingData>();

    ArrayList<MappingData> errorFilterVector = new ArrayList<MappingData>();

    HashMap<String, String> exceptionMap = new HashMap<String, String>();

    HashMap<String, String> statusMap = new HashMap<String, String>();

    HashMap<String, String> mimeTypes = new HashMap<String, String>();

    MultiMap<String, SecurityConstraint> constraints = new MultiMap<String, SecurityConstraint>(SLC.instance);

    SessionManager<String, HttpSession> sessionManager = new SessionManager<String, HttpSession>(15 * 60 * 1000L);

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
            javax.servlet.http.HttpSessionEvent hse = new javax.servlet.http.HttpSessionEvent(hs);
            for (Iterator<javax.servlet.http.HttpSessionListener> e = hslv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionListener hsl = e.next();
                hsl.sessionCreated(hse);
            }
        }
        return r;
    }

    void removeSession(HttpSession hs) {
        sessionManager.remove(hs.sessionId);
        if (hslv != null) {
            javax.servlet.http.HttpSessionEvent hse = new javax.servlet.http.HttpSessionEvent(hs);
            for (Iterator<javax.servlet.http.HttpSessionListener> e = hslv.iterator(); e.hasNext();) {
                javax.servlet.http.HttpSessionListener hsl = e.next();
                hsl.sessionDestroyed(hse);
            }
        }
    }

    HttpServletRequest checkAccess(String method, String localPath, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (verify == null) {
            return request;
        }
        if (constraints.isEmpty())
            return request;
        // find longest pattern which also matches the used method
        final HashSet<SecurityConstraint> scs = lookupConstraint(method, localPath);
        // System.out.println(method + " " + localPath + "=" + sc);
        if (scs.isEmpty())
            return request;

        // search if the resource is global allowed
        for (final SecurityConstraint sc : scs) {
            StringTokenizer r = sc.roles();
            if (r.hasMoreElements()) {
                if ("*".equals(r.nextElement())) {
                    return request;
                }
            }
        }

        final HttpRequest r = RequestDispatcher.dereference(request);
        Collection<String> roles = null;
        if (verify instanceof FormVerification) {
            final HttpSession session = (HttpSession) request.getSession(true);
            String remoteUser = (String) session.getAttribute("j_username");
            if (remoteUser == null) {
                final FormVerification fv = (FormVerification) verify;
                if (localPath.equals("/" + fv.loginPage) || localPath.equals("/" + fv.loginErrorPage))
                    return request;

                if (!localPath.endsWith("/j_security_check")) {
                    if (session.getAttribute("j_redirect") == null)
                        session.setAttribute("j_redirect", request.getRequestURI());
                    response.sendRedirect(this.sContext + "/" + fv.loginPage);
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
                response.addHeader("WWW-Authenticate", "Basic realm=\"" + aRealm + "\"");
                response.setStatus(401);
                return null;
            }

            roles = verify.verifyUserGroup(r.remoteUser, r.remotePass);
        }
        if (roles != null) {
            final HashSet<String> roleLookup = new HashSet<String>();
            roleLookup.addAll(roles);
            for (SecurityConstraint sc : scs) {
                final StringTokenizer st = sc.roles();
                for (; st.hasMoreElements();) {
                    String role = st.nextToken();
                    // System.out.println("usergroup: " + vfy);
                    if (roleLookup.contains(role)) {
                        final HttpSession session = (HttpSession) request.getSession(true);
                        session.setAttribute("j_user_roles", roleLookup);
                        if (verify instanceof FormVerification) {
                            String previousUri = (String) session.getAttribute("j_redirect");
                            if (previousUri != null) {
                                session.removeAttribute("j_redirect");
                                if (previousUri.endsWith("/j_security_check"))
                                    previousUri = previousUri.substring(0, previousUri.length() - 16);
                                response.sendRedirect(previousUri);
                                return null;
                            }
                        }
                        return request;
                    }
                }
            }
        }

        if (verify instanceof FormVerification) {
            final FormVerification fv = (FormVerification) verify;
            if (fv.loginErrorPage != null) {
                response.sendRedirect(this.sContext + "/" + fv.loginErrorPage);
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
    private HashSet<SecurityConstraint> lookupConstraint(String method, String localPath) {
        HashSet<SecurityConstraint> result = new HashSet<SecurityConstraint>();

        // start search with longest pattern, till we find a match
        for (Iterator<String> i = constraints.keySet().iterator(); i.hasNext();) {
            String pattern = i.next();

            if (pattern.charAt(0) == '*') {
                if (localPath.endsWith(pattern.substring(1))) {
                    SecurityConstraint sc = constraints.get(pattern);
                    if (sc.containsMethod(method)) {
                        result.add(sc);
                    }
                }
                continue;
            }

            int pl = pattern.length() - 1;
            if (localPath.equals(pattern) || ((pattern.charAt(pl) == '*') && localPath.startsWith(pattern.substring(0, pl)))) {
                // found a matching pattern, no check all configured SCs for
                // method
                SecurityConstraint sc = constraints.get(pattern);
                if (sc.containsMethod(method)) {
                    result.add(sc);
                }
            }
        }
        return result;
    }

    /**
     * CT.
     */
    protected HttpContext() {
        init("context", PROPERTIES);
    }

    public String getName() {
        String name = sContext == null || sContext.length() == 0 ? "/" : sContext;
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

        for (int idx = sPath.indexOf('\\'); idx >= 0; idx = sPath.indexOf('\\', idx)) {
            sPath = sPath.substring(0, idx) + '/' + sPath.substring(idx + 1);
        }

        if (aGroup != null) {
            final UserGroupDbi vfy = Config.getGroup(aGroup);

            final String loginJsp = getRealPath("loginPage.jsp");
            if (new File(loginJsp).exists()) {
                verify = new FormVerification("loginPage.jsp", "loginErrorPage.jsp", new ServletHandler() {
                    @Override
                    public void service(ServletRequest in, ServletResponse out) throws IOException, ServletException {
                        if (vfy == null)
                            return;
                        final String userName = in.getParameter("j_username");
                        final String password = in.getParameter("j_password");
                        final Collection<String> permissions = vfy.verifyUserGroup(userName, password);
                        final HttpServletRequest hsr = (HttpServletRequest) in;
                        final javax.servlet.http.HttpSession session = hsr.getSession(true);
                        if (permissions != null) {
                            session.setAttribute("j_username", userName);
                            session.setAttribute("j_user_roles", permissions);
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
                logFile.writeDate("added:     handler: " + mask + ": " + h.getName());
            }
        }
    }

    public javax.servlet.RequestDispatcher getRequestDispatcher(java.lang.String path) // from
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
        for (int ppp = path.indexOf("/../"); ppp > 0; ppp = path.indexOf("/../")) {
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

        RequestDispatcher rd = new RequestDispatcher(this, path, pathInfo, query, handler);

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
            javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(this, name, o);
            for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv.iterator(); e.hasNext();) {
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
                javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(this, name, o);
                for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv.iterator(); e.hasNext();) {
                    javax.servlet.ServletContextAttributeListener scal = e.next();
                    scal.attributeAdded(scae);
                }
            } else {
                javax.servlet.ServletContextAttributeEvent scae = new javax.servlet.ServletContextAttributeEvent(this, name, old);
                for (Iterator<javax.servlet.ServletContextAttributeListener> e = scalv.iterator(); e.hasNext();) {
                    javax.servlet.ServletContextAttributeListener scal = e.next();
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

    public javax.servlet.RequestDispatcher getNamedDispatcher(java.lang.String dispatcherName) // from
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
            name = p.getName() + "-" + p.getProperty("port") + "/" + server + name;
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
                final HttpSessionEvent paramHttpSessionEvent = new HttpSessionEvent(hs);
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
        requestFilterVector.clear();
        includeFilterVector.clear();
        forwardFilterVector.clear();

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
            List<?> drivers = (List<?>) getMember(null, DriverManager.class, "registeredDrivers");
            for (Object driverInfo : new ArrayList<Object>(drivers)) {
                final Driver driver = (Driver) getMember(driverInfo, "driver");
                if (refersToMe(driver.getClass().getClassLoader())) {
                    try {
                        log("ERROR JDBC driver not deregistered: " + driver + " trying to deregister");
                        drivers.remove(driverInfo);
                    } catch (Exception e) {
                        log("ERROR while deregistering: " + driver + ": " + e.getMessage());
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
                    ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) getMember(t, "target.this$0");
                    if (stpe != null)
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
            for (StringTokenizer st = new StringTokenizer(memberName, "."); st.hasMoreTokens();) {
                if (reference == null)
                    return null;
                reference = getMember(reference, reference.getClass(), st.nextToken());
            }
            return reference;
        } catch (Exception ex) {
        }
        return null;
    }

    private static Object getMember(Object reference, Class<?> clazz, String memberName) throws NoSuchFieldException, IllegalAccessException {
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

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String filterClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Filter> clazz = (Class<Filter>) zcl.loadClass(filterClassName);
            return addFilter(filterName, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        try {
            final Filter filter = createFilter(filterClass);
            return addFilter(filterName, filter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if (initialized)
            throw new IllegalStateException("context already initialized");
        if (filterName == null)
            throw new IllegalArgumentException("filterName must not be null");

        FilterRegistration.Dynamic filterRegistration = filterRegistrations.get(filterName);
        if (filterRegistration == null) {
            filterRegistration = new de.bb.bejy.http.FilterRegistration.Dynamic(this, filterName, filter);
            filterRegistrations.put(filterName, filterRegistration);
        }
        return filterRegistration;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, String servletClassName) {
        try {
            ClassLoader cl = zcl == null ? Thread.currentThread().getContextClassLoader() : zcl;
            @SuppressWarnings("unchecked")
            Class<? extends Servlet> clazz = (Class<Servlet>) cl.loadClass(servletClassName);
            return addServlet(servletName, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        if (initialized)
            throw new IllegalStateException("context already initialized");
        if (servletName == null)
            throw new IllegalArgumentException("filterName must not be null");

        ServletRegistration.Dynamic servletRegistration = servletRegistrations.get(servletName);
        if (servletRegistration == null) {
            servletRegistration = new de.bb.bejy.http.ServletRegistration.Dynamic(this, servletName, servlet);
            servletRegistrations.put(servletName, servletRegistration);
        }
        return servletRegistration;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        try {
            return addServlet(servletName, createServlet(servletClass));
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Filter> T createFilter(Class<T> filterClass) throws ServletException {
        try {
            T filter = filterClass.newInstance();
            if (injector != null)
                injector.inject(null, filter);
            return filter;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public <T extends EventListener> T createListener(Class<T> listenerClass) throws ServletException {
        try {
            T listener = listenerClass.newInstance();
            if (injector != null)
                injector.inject(null, listener);
            return listener;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public <T extends Servlet> T createServlet(Class<T> servletClass) throws ServletException {
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

    public Set<String> addSecurity4Mappings(ServletSecurityElement servletSecurityElement, Set<String> keySet) {
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
    
    public String toString() {
    	return super.toString() + " " + sContext + " <" + aRealm + "> [" + aGroup + "]";
    }
}
