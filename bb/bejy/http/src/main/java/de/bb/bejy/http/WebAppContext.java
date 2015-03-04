/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/WebAppContext.java,v $
 * $Revision: 1.38 $
 * $Date: 2014/09/22 09:21:50 $
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

 (c) 1994-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

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
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
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
import de.bb.util.Pair;
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

    /*
     * // open public java.net.URL getResource(java.lang.String s) // from
     * javax.servlet.ServletContext { return null; } public java.io.InputStream
     * getResourceAsStream(java.lang.String s) // from
     * javax.servlet.ServletContext { return
     * zcl.getResourceAsStream(getRealPath(s)); } public java.util.Set
     * getResourcePaths() // from javax.servlet.ServletContext { return null; }
     */
}

/******************************************************************************
 * $Log: WebAppContext.java,v $ Revision 1.38 2014/09/22 09:21:50 bebbo
 * 
 * @B if security constraints are provided the DEFAULT constraint is dropped
 *
 *    Revision 1.37 2014/06/23 15:38:46 bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles Revision 1.36 2013/11/28
 *    10:30:58 bebbo
 * 
 * @B resolving a linked *.jar file now results into a local unpacked folder
 *    Revision 1.35 2013/05/17 10:32:04 bebbo
 * 
 * @N workDir can now be set
 * @B logFile is set correctly Revision 1.34 2012/11/11 18:36:42 bebbo
 * 
 * @I import cleanup Revision 1.33 2012/11/08 12:14:16 bebbo
 * 
 * @B fixed proxy with HTTP chunked mode
 * @N added SOCKS5 proxy support
 * @N added a fallback option for XML data -> XMPP server Revision 1.32
 *    2012/07/18 06:44:45 bebbo
 * 
 * @I typified Revision 1.31 2011/03/06 18:19:42 bebbo
 * 
 * @R made a release version Revision 1.30 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.29 2008/06/04 06:45:27 bebbo
 * @B fixed class loading issues on reload of web applications
 * 
 *    Revision 1.28 2008/01/17 17:33:44 bebbo
 * @B fixes for better handling if the request is wrapped
 * 
 *    Revision 1.27 2007/05/01 19:05:26 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.26 2007/01/18 21:50:10 bebbo
 * @N added support for servlet 2.4 listeners
 * 
 *    Revision 1.25 2006/10/12 05:55:06 bebbo
 * @B removed erraneous import
 * 
 *    Revision 1.24 2006/05/09 12:13:26 bebbo
 * @R changes to comply to servlet2_4
 * 
 *    Revision 1.23 2006/02/06 09:16:44 bebbo
 * @I cleanup
 * 
 *    Revision 1.22 2004/12/13 15:37:36 bebbo
 * @D added log message when unpacking a WAR archives
 * 
 *    Revision 1.21 2004/04/20 13:23:40 bebbo
 * @R added support for SecurityConstraint
 * 
 *    Revision 1.20 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group,
 *    Cfg, Factory
 * 
 *    Revision 1.19 2004/03/23 12:29:59 bebbo
 * @B servlet loading order is applied correctly
 * @B welcome file list was still broken
 * 
 *    Revision 1.18 2004/01/09 19:36:05 bebbo
 * @B fixed reading the welcome file list
 * @B fixed specifying a work dir
 * 
 *    Revision 1.17 2003/12/18 10:41:41 bebbo
 * @B war files was not closed
 * 
 *    Revision 1.16 2003/11/26 09:56:42 bebbo
 * @B fixed NPEs
 * 
 *    Revision 1.15 2003/09/03 14:56:31 bebbo
 * @N now supports multiple welcome files
 * 
 *    Revision 1.14 2003/07/01 11:09:09 bebbo
 * @B missing web.xml is treated now as an empty web.xml
 * 
 *    Revision 1.13 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.12 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.11 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.10 2003/01/05 15:48:50 bebbo
 * @B fixed displayed realm for webapps.
 * 
 *    Revision 1.9 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.8 2002/12/02 18:26:36 bebbo
 * @R default is now index.jsp
 * 
 *    Revision 1.7 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.6 2002/07/30 10:28:26 bebbo
 * @B fix for WAR files, for implicit directories (not stored explicit)
 * 
 *    Revision 1.5 2002/07/26 18:32:51 bebbo
 * @N added alias support for war contexts
 * @B fixed Exception on shor relative paths
 * 
 *    Revision 1.4 2002/04/03 15:41:12 franke
 * @N added support for webapps
 * @B exceptions in servlet init() no longer causes to unload the complete
 *    context
 * 
 *    Revision 1.3 2002/04/02 13:02:36 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.2 2002/03/30 15:50:26 franke
 * @N use init params
 * @N use filter and filter-mappings
 * 
 *    Revision 1.1 2002/03/21 14:39:36 franke
 * @N added support for web-apps. Added to config file based configuration some
 *    config function calls. Also added the use of a special ClassLoader.
 * 
 ******************************************************************************/
