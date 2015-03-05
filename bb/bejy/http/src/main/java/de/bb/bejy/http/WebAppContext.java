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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import de.bb.bejy.Config;
import de.bb.bejy.http.FilterRegistration.Dynamic;
import de.bb.bejy.http.FilterRegistration.MappingData;
import de.bb.bejy.http.jsp.JspServlet;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.XmlFile;
import de.bb.util.ZipClassLoader;

public class WebAppContext extends HttpContext {
    // private final static boolean DEBUG = true;
    private final static String PROPERTIES[][] = {
            { "path", "the local path to a directory or an WAR file" },
            {
                    "urlPath",
                    "the URL context path. resulting URL consists from urlPath and WAR file name or alias" },
            { "group", "optional name of a user group to restrict access" },
            { "realm", "a descriptive name for this realm" },
            { "alias",
                    "an alias for which is used instead of the WAR file name" },
            { "workDir", "the temp directory" }, };

    private ClassLoader parentClassLoader;

    private HashSet<Servlet> servlets;

    HashMap<String, ServletHandler> name2servletHandler;

    WebAppContext() {
        this(null);
    }

    public WebAppContext(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
        if (parentClassLoader instanceof Injector)
            injector = (Injector) parentClassLoader;
        init("WAR", PROPERTIES);
    }

    void activate2(LogFile logFile) throws Exception {
        String path = sPath.substring(0, sPath.length() - 1);

        // if WAR archive, check, whether to unpack it
        if (path.toUpperCase().endsWith(".WAR")) {
            File file = new File(path);
            File dir = new File(getProperty("folder"));
            logFile.writeDate("unpacking " + file.getPath());
            unpack(file, dir);
            path = dir.toString();
        }

        // zcl.addPath(path);
        sPath = path + "/";

        zcl = new ZipClassLoader(parentClassLoader);
        ClassLoader lastCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(zcl);

            // Properties props = System.getProperties();
            // zcl.addPath(System.getProperty("java.class.path"));
            zcl.addPath(path + "/WEB-INF/classes");
            zcl.addPath(path + "/WEB-INF");

            // add libs to classpath
            {
                File dir = new File(path, "WEB-INF/lib");
                String list[] = dir.list();
                if (list != null) {
                    for (int i = 0; i < list.length; ++i) {
                        zcl.addPath(path + "/WEB-INF/lib/" + list[i]);
                    }
                }
            }

            // zcl2.addPath(sPath);
            InputStream is = zcl.getResourceAsStream("web.xml");
            XmlFile xf = new XmlFile();
            if (is != null)
                xf.read(is);

            int idx = path.lastIndexOf('/');
            String alias;
            if (idx >= 0) {
                alias = path.substring(idx);
            } else {
                alias = path;
            }

            alias = getProperty("alias", alias);

            if (!alias.startsWith("/")) {
                alias = "/" + alias;
            }

            sContext += alias;
            while (sContext.startsWith("//")) {
                sContext = sContext.substring(1);
            }

            if (sContext.endsWith("/")) {
                sContext = sContext.substring(0, sContext.length() - 1);
            }

            if (sContext.startsWith(aRealm)) {
                aRealm = sContext;
            }
            scLen = sContext.length();

            addFileHandler(logFile, xf);

            readContextParams(xf);

            readSessionTimeout(xf);

            readListeners(logFile, xf);

            name2servletHandler = readServlets(logFile, xf);

            readServletMappings(logFile, xf);

            readFilters(logFile, xf);

            readFilterMappings(logFile, xf);

            servlets = new HashSet<Servlet>(name2servletHandler.values());

            // read error page definitions
            for (Enumeration<String> e = xf.getSections("/web-app/error-page")
                    .elements(); e.hasMoreElements();) {
                String s = e.nextElement();
                String ex = xf.getContent(s + "exception-type");
                String err = xf.getContent(s + "error-code");
                String loc = xf.getContent(s + "location");
                if (loc == null) {
                    continue;
                }
                if (ex != null) {
                    exceptionMap.put(ex, loc);
                }
                if (err != null) {
                    statusMap.put(err, loc);
                }
            }

            // read login configuration
            {
                String realm = xf
                        .getContent("/web-app/login-config/realm-name");
                if (realm != null && realm.length() > 0) {
                    aRealm = realm;
                }
                if (aGroup == null)
                    aGroup = aRealm;
                String am = xf.getContent("/web-app/login-config/auth-method");
                if (am == null) {
                    am = "";
                } else {
                    am = am.trim();
                }
                if ("BASIC".equalsIgnoreCase(am)) {
                    verify = Config.getGroup(aGroup);
                } else if ("FORM".equalsIgnoreCase(am)) {
                    String login = xf
                            .getContent("/web-app/login-config/form-login-config/form-login-page");
                    String loginError = xf
                            .getContent("/web-app/login-config/form-login-config/form-error-page");
                    if (login != null) {
                        String servletClassName = getInitParameter("loginClass");
                        ServletRegistration.Dynamic servletRegistration = (de.bb.bejy.http.ServletRegistration.Dynamic) addServlet(
                                "j_security_check", servletClassName);

                        ServletHandler h = new ServletHandler();
                        h.setClassLoader(zcl);
                        h.setContext(this);
                        h.setName("j_security_check");
                        h.servletRegistration = servletRegistration;

                        name2servletHandler.put("j_security_check", h);

                        verify = new FormVerification(login.trim(), loginError,
                                h);
                    }
                } else if (am.length() > 0) {
                    logFile.writeDate("WARNING: auth-method " + am
                            + " is not supported, using BASIC");
                    verify = Config.getGroup(aGroup);
                }
            }

            // DENY /WEB-INF/
            {
                SecurityConstraint sc = new SecurityConstraint("WEB-INF");
                constraints.clear();
                sc.addMethod("*");
                constraints.put("/WEB-INF/*", sc);
            }

            // read security-constraints
            for (Enumeration<String> e = xf.getSections(
                    "/web-app/security-constraint").elements(); e
                    .hasMoreElements();) {
                String s = e.nextElement();
                for (Enumeration<String> ee = xf.getSections(
                        s + "web-resource-collection").elements(); ee
                        .hasMoreElements();) {
                    String wrc = ee.nextElement();
                    String wrcname = xf.getContent(wrc + "web-resource-name");
                    if (wrcname == null) {
                        wrcname = "unnamed";
                    }
                    SecurityConstraint sc = new SecurityConstraint(wrcname);
                    for (Enumeration<String> f = xf.getSections(
                            wrc + "http-method").elements(); f
                            .hasMoreElements();) {
                        String method = f.nextElement();
                        sc.addMethod(xf.getContent(method));
                    }
                    for (Enumeration<String> f = xf.getSections(
                            s + "auth-constraint/role-name").elements(); f
                            .hasMoreElements();) {
                        String role = f.nextElement();
                        sc.addRole(xf.getContent(role));
                    }
                    String transport = xf.getContent(s
                            + "user-data-constraint/transport-guarantee");
                    sc.setTransport(transport);
                    for (Enumeration<String> f = xf.getSections(
                            wrc + "url-pattern").elements(); f
                            .hasMoreElements();) {
                        String pattern = f.nextElement();
                        constraints.put(xf.getContent(pattern), sc);
                    }
                }
            }

            // add mime types
            for (Enumeration<String> e = xf
                    .getSections("/web-app/mime-mapping").elements(); e
                    .hasMoreElements();) {
                String s = e.nextElement();
                String ex = xf.getContent(s + "extension");
                String mi = xf.getContent(s + "mime-type");
                if (ex != null && mi != null) {
                    mimeTypes.put(ex, mi);
                }
            }

            if (getProperty("workDir") != null) {
                parameter.put(ServletContext.TEMPDIR, getProperty("workDir"));
            }
            addJspHandler();

        } finally {
            Thread.currentThread().setContextClassLoader(lastCl);
        }
    }

    private void readServletMappings(LogFile logFile, XmlFile xf) {
        // get all <servlet-mappings>
        for (Enumeration<String> e = xf.getSections("/web-app/servlet-mapping")
                .elements(); e.hasMoreElements();) {
            String section = e.nextElement();
            String servletName = xf.getContent(section + "servlet-name");
            if (servletName == null) {
                logFile.writeDate("missing servlet-name");
                continue;
            }

            ServletHandler servletHandler = name2servletHandler
                    .get(servletName);
            if (servletHandler == null) {
                logFile.writeDate("no servlet with name:" + servletName);
                continue;
            }

            for (Enumeration<String> f = xf
                    .getSections(section + "url-pattern").elements(); f
                    .hasMoreElements();) {
                String urlSection = f.nextElement();
                String urlPattern = xf.getContent(urlSection);
                if (urlPattern == null) {
                    logFile.writeDate("invalid urlPattern in servlet-mapping for "
                            + servletName);
                    continue;
                }
                urlPattern = urlPattern.trim();

                if (urlPattern.length() == 0
                        || "*/".indexOf(urlPattern.charAt(0)) < 0) {
                    urlPattern = "/" + urlPattern;
                }
                // if (url.endsWith("/*")) url = url.substring(0, url.length() -
                // 1);

                servletHandler.servletRegistration.addMapping(urlPattern);
            }
        }
    }

    private HashMap<String, ServletHandler> readServlets(LogFile logFile,
            XmlFile xf) {
        // read all <servlet>
        HashMap<String, ServletHandler> name2servletHandler = new HashMap<String, ServletHandler>();
        for (Enumeration<String> e = xf.getSections("/web-app/servlet")
                .elements(); e.hasMoreElements();) {
            String servletName = "";
            try {
                String section = e.nextElement();

                servletName = xf.getContent(section + "servlet-name");
                if (servletName == null) {
                    logFile.writeDate("error: missing servlet-name");
                    continue;
                }
                servletName = servletName.trim();

                // String displayName = xf.getContent(s + "display-name");
                // if (displayName != null)
                // displayName = displayName.trim();

                ServletRegistration.Dynamic servletRegistration;

                final String servletClassName = xf.getContent(section
                        + "servlet-class");
                final String jspFile = xf.getContent(section + "jsp-file");
                if (servletClassName != null) {
                    servletRegistration = (de.bb.bejy.http.ServletRegistration.Dynamic) this
                            .addServlet(servletName, servletClassName.trim());
                } else if (jspFile != null) {
                    JspServlet jspServlet = new de.bb.bejy.http.jsp.StaticJspServlet();
                    jspServlet.setJspFileName(jspFile);
                    jspServlet.setZipClassLoader(zcl);
                    servletRegistration = (de.bb.bejy.http.ServletRegistration.Dynamic) this
                            .addServlet(servletName, jspServlet);
                } else {
                    logFile.writeDate("servlet " + servletName
                            + " needs either servlet-class or jsp-file");
                    continue;
                }

                String loadOnStartup = xf.getContent(section
                        + "load-on-startup");
                if (loadOnStartup != null) {
                    try {
                        int paramInt = Integer.parseInt(loadOnStartup.trim());
                        servletRegistration.setLoadOnStartup(paramInt);
                    } catch (RuntimeException rex) {
                        logFile.writeDate("invalid value load-on-startup = "
                                + loadOnStartup + " for servlet: "
                                + servletName);
                    }
                }

                ServletHandler h = new ServletHandler();
                h.setClassLoader(zcl);
                h.setContext(this);
                h.setName(servletName);
                h.servletRegistration = servletRegistration;

                h.activate(logFile);

                name2servletHandler.put(servletName, h);
            } catch (Throwable ex) {
                logFile.writeDate("cannot load servlet " + servletName + ": "
                        + ex.getMessage());
            }
        }
        return name2servletHandler;
    }

    private void addJspHandler() throws ServletException {
        // add a handler for JSP pages
        {
            ServletHandler h = new ServletHandler();
            h.setClassLoader(zcl);
            h.setName("jsp");
            h.setContext(this);
            de.bb.bejy.http.jsp.JspServlet sjs = new de.bb.bejy.http.jsp.JspServlet();
            sjs.setZipClassLoader(zcl);
            h.setServlet(sjs);
            addHandler("*.jsp", h);
            addHandler("*.jspx", h);

            String workDir = getProperty("workDir");
            if (workDir != null) {
                sjs.setWorkDir(workDir);
            }
        }
    }

    private void readContextParams(XmlFile xf) {
        // add context init para,s
        for (Enumeration<String> f = xf.getSections("/web-app/context-param")
                .elements(); f.hasMoreElements();) {
            String in = f.nextElement();
            String pn = xf.getContent(in + "param-name");
            String pv = xf.getContent(in + "param-value");
            if (pn != null && pv != null && pn.length() > 0) {
                parameter.put(pn, pv);
            }
        }
    }

    private void addFileHandler(LogFile logFile, XmlFile xf) throws Exception {
        // add a handler for files
        StringBuilder wf = new StringBuilder();
        boolean hit = false;
        for (Enumeration<String> e = xf.getSections(
                "/web-app/welcome-file-list/welcome-file").elements(); e
                .hasMoreElements();) {
            if (hit) {
                wf.append(" ");
            }
            hit = true;
            String sec = e.nextElement();
            wf.append(xf.getContent(sec).trim());
        }
        if (!hit) {
            wf.append("index.jsp index.html");
        }
        FileHandler h = new FileHandler();
        h.setParent(this);
        h.activate(logFile);
        h.setName("default");
        h.setContext(this);
        h.setWelcomeFile(wf.toString());
        addHandler("/", h);
    }

    private void readSessionTimeout(XmlFile xf) {
        // get session config
        String sTimeOut = xf
                .getContent("/web-app/session-config/session-timeout");
        if (sTimeOut != null) {
            try {
                int sto = Integer.parseInt(sTimeOut);
                sto *= 60 * 1000; // from mins into milliseconds
                sessionManager.setTimeout(sto);
            } catch (Exception ex) {
            }
        }
    }

    private void readListeners(LogFile logFile, XmlFile xf) {
        // get all listeners
        for (Enumeration<String> e = xf.getSections("/web-app/listener")
                .elements(); e.hasMoreElements();) {
            String s = e.nextElement();
            String cn = xf.getContent(s + "listener-class");
            // if (DEBUG) logFile.writeDate(s + " = " + cn);
            try {
                Class<?> clazz = zcl.loadClass(cn);
                Object o = clazz.newInstance();
                if (injector != null)
                    injector.inject(logFile, o);

                if (o instanceof javax.servlet.ServletContextListener) {
                    sclv.add((ServletContextListener) o);
                } else if (o instanceof javax.servlet.ServletContextAttributeListener) {
                    scalv.add((ServletContextAttributeListener) o);
                } else if (o instanceof javax.servlet.http.HttpSessionListener) {
                    hslv.add((HttpSessionListener) o);
                } else if (o instanceof javax.servlet.http.HttpSessionAttributeListener) {
                    hsalv.add((HttpSessionAttributeListener) o);
                } else if (o instanceof javax.servlet.ServletRequestListener) {
                    if (srlv == null) {
                        srlv = new ArrayList<ServletRequestListener>();
                    }
                    srlv.add((ServletRequestListener) o);
                } else if (o instanceof javax.servlet.ServletRequestAttributeListener) {
                    if (sralv == null) {
                        sralv = new ArrayList<ServletRequestAttributeListener>();
                    }
                    sralv.add((ServletRequestAttributeListener) o);
                } else {
                    throw new Exception("no interface implemented");
                }
            } catch (Exception ex) {
                logFile.writeln("invalid listener: " + cn + " - " + ex);
            }
        }

        // init the contexts
        javax.servlet.ServletContextEvent sce = new javax.servlet.ServletContextEvent(
                this);
        for (Iterator<javax.servlet.ServletContextListener> e = sclv.iterator(); e
                .hasNext();) {
            javax.servlet.ServletContextListener scl = e.next();
            scl.contextInitialized(sce);
        }
    }

    private void readFilters(LogFile logFile, XmlFile xf) {
        // load Servlet Filter into HashMap
        for (Enumeration<String> e = xf.getSections("/web-app/filter")
                .elements(); e.hasMoreElements();) {
            String s = e.nextElement();
            String filterName = xf.getContent(s + "filter-name");
            String filterClass = xf.getContent(s + "filter-class");
            try {

                FilterRegistration.Dynamic filterRegistration = (Dynamic) addFilter(
                        filterName, filterClass);
                filterRegistrations.put(filterName, filterRegistration);

                // load init-params
                for (Enumeration<String> f = xf.getSections(s + "init-param")
                        .elements(); f.hasMoreElements();) {
                    String in = f.nextElement();
                    String pn = xf.getContent(in + "param-name");
                    String pv = xf.getContent(in + "param-value");
                    if (pn != null && pv != null) {
                        filterRegistration.setInitParameter(pn, pv);
                    }
                }

                filterRegistration.filter.init(filterRegistration);

            } catch (Exception ex) {
                logFile.writeDate(ex.getMessage());
            }
        }
    }

    private void readFilterMappings(LogFile logFile, XmlFile xf) {
        /*
         * The order in which the container builds the chain of filters to be
         * applied for a particular request URI is 1) The URL pattern matching
         * filter-mappings in the same as the order that those elements appear
         * in the deployment descriptor, and then 2) The servlet-name matching
         * filter-mappings in the same as the order that those elements appear
         * in the deployment descriptor
         */
        for (Enumeration<String> e = xf.getSections("/web-app/filter-mapping")
                .elements(); e.hasMoreElements();) {
            String section = e.nextElement();
            String filterName = xf.getContent(section + "filter-name");

            if (filterName == null) {
                logFile.writeDate("filter-mapping: filter-name is missing");
                continue;
            }
            filterName = filterName.trim();

            FilterRegistration.Dynamic filterRegistration = (Dynamic) filterRegistrations
                    .get(filterName);
            if (filterRegistration == null || filterRegistration.filter == null) {
                logFile.writeDate("filter-mapping: filter " + filterName
                        + " does not exist");
                continue;
            }
            javax.servlet.Filter f = filterRegistration.filter;

            EnumSet<DispatcherType> dispatcherTypes = null;
            for (Iterator<String> di = xf.sections(section + "dispatcher"); di
                    .hasNext();) {
                String dk = di.next();
                String disp = xf.getContent(dk).trim().toUpperCase();
                DispatcherType dispatcherType = DispatcherType.valueOf(disp);
                if (dispatcherTypes == null)
                    dispatcherTypes = EnumSet.of(dispatcherType);
                else
                    dispatcherTypes.add(dispatcherType);
            }

            for (final Iterator<String> i = xf.sections(section); i.hasNext();) {
                final String child = i.next();
                final String tag = XmlFile.getLastSegment(child);
                if ("url-pattern".equals(tag)) {
                    final String urlPattern = xf.getContent(child).trim();
                    filterRegistration.addMappingForUrlPatterns(
                            dispatcherTypes, true, urlPattern);
                } else if ("servlet-name".equals(tag)) {
                    final String servletName = xf.getContent(child).trim();
                    filterRegistration.addMappingForServletNames(
                            dispatcherTypes, true, servletName);
                }
            }
        }

        midQueue = frontQueue;
        midQueue.addAll(afterQueue);

        frontQueue = new ArrayList<FilterRegistration.MappingData>();
        afterQueue = new ArrayList<FilterRegistration.MappingData>();
    }

    @Override
    protected void destroy() {
        for (Servlet s : servlets) {
            try {
                s.destroy();
            } catch (Exception e) {
            }
        }
        for (FilterRegistration.Dynamic filterRegistration : (Collection<FilterRegistration.Dynamic>) (Collection) filterRegistrations
                .values()) {
            try {
                filterRegistration.filter.destroy();
            } catch (Exception e) {
            }
        }
        super.destroy();
    }

    public static void unpack(File file, File dir) throws ZipException,
            IOException {
        // unpack archive if necessary
        if (!dir.exists() || dir.lastModified() < file.lastModified()) {
            java.util.zip.ZipFile zf = new java.util.zip.ZipFile(file);
            deleteRecursive(dir);
            dir.delete();
            dir.mkdirs();
            String lPath = "";
            for (@SuppressWarnings("unchecked")
            Enumeration<java.util.zip.ZipEntry> e = (Enumeration<ZipEntry>) zf
                    .entries(); e.hasMoreElements();) {
                java.util.zip.ZipEntry ze = e.nextElement();
                String name = ze.getName();
                File f = new File(dir, name);
                if (ze.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                int idx = name.lastIndexOf('/');
                if (idx >= 0) {
                    String nPath = name.substring(0, idx);
                    if (!nPath.equals(lPath)) {
                        lPath = nPath;
                        new File(dir, nPath).mkdirs();
                    }
                }

                OutputStream os = null;
                InputStream is = null;
                try {
                    os = new FileOutputStream(f);
                    is = zf.getInputStream(ze);
                    byte b[] = new byte[8192];
                    while (is.available() > 0) {
                        int l = is.read(b);
                        if (l <= 0) {
                            break;
                        }
                        os.write(b, 0, l);
                    }
                    os.flush();
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (Exception ioe) {
                        }
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception ioe) {
                        }
                    }
                }
            }
            zf.close();
        }
    }

    private static void deleteRecursive(File dir) {
        final File[] files = dir.listFiles();
        if (files != null)
            for (final File f : files) {
                if (f.isDirectory())
                    deleteRecursive(f);
                f.delete();
            }
    }

    void initialize(LogFile log) {
        Thread t = Thread.currentThread();
        java.lang.ClassLoader cl = t.getContextClassLoader();
        try {
            t.setContextClassLoader(zcl);

            super.initialize(log);

            // apply servletRegistration
            // sort by init
            MultiMap<Integer, ServletHandler> sorted = new MultiMap<Integer, ServletHandler>();
            for (ServletHandler servletHandler : name2servletHandler.values()) {
                de.bb.bejy.http.ServletRegistration.Dynamic servletRegistration = servletHandler.servletRegistration;
                for (String mapping : servletRegistration.getMappings()) {
                    addHandler(mapping, servletHandler);
                }
                servletHandler.init(servletRegistration);
                if (servletHandler.servletRegistration.loadOnStartup >= 0)
                    sorted.put(
                            servletHandler.servletRegistration.loadOnStartup,
                            servletHandler);
            }

            // apply
            for (ServletHandler sh : sorted.values()) {
                de.bb.bejy.http.ServletRegistration.Dynamic servletRegistration = sh.servletRegistration;
                try {
                    sh.init(servletRegistration);
                    sh.loadServlet(log, injector);
                } catch (Throwable ttt) {
                    log.writeDate("exception during loadServlet for: "
                            + servletRegistration.name + ": "
                            + ttt.getMessage());
                }
            }

            // convert filter mappings
            final ArrayList<MappingData> mdl = new ArrayList<FilterRegistration.MappingData>();
            mdl.addAll(frontQueue);
            mdl.addAll(midQueue);
            mdl.addAll(afterQueue);
            for (final MappingData mdata : mdl) {
                ArrayList<MappingData> filterQueue;
                if (mdata.dispatcherType == DispatcherType.REQUEST)
                    filterQueue = requestFilterVector;
                else if (mdata.dispatcherType == DispatcherType.FORWARD)
                    filterQueue = forwardFilterVector;
                else if (mdata.dispatcherType == DispatcherType.INCLUDE)
                    filterQueue = includeFilterVector;
                else if (mdata.dispatcherType == DispatcherType.ERROR)
                    filterQueue = errorFilterVector;
                else
                    continue;

                filterQueue.add(mdata);
            }

            for (FilterRegistration filterRegistration : filterRegistrations.values()) {
                try {
                    filterRegistration.filter.init(filterRegistration);
                } catch (ServletException e) {
                    log.writeDate("filter init failed:" + e.getMessage());
                }
            }

        } finally {
            t.setContextClassLoader(cl);
        }
    }

    /**
     * @param e
     * @return
     */
    public String getErrorPage(Exception e) {
        return exceptionMap.get(e.getClass().getName());
    }

    /**
     * Return all instantiated objects: servlets, filters
     * 
     * @return
     */
    public ArrayList<Object> getInstances() {
        ArrayList<Object> r = new ArrayList<Object>();

        return r;
    }

    public ZipClassLoader getZcl() {
        return zcl;
    }

}
