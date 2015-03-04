/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/ServletHandler.java,v $
 * $Revision: 1.28 $
 * $Date: 2014/06/23 15:38:46 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * servlet handler for bejy
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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import de.bb.util.LogFile;

public class ServletHandler extends HttpHandler implements
        javax.servlet.FilterChain {
    private final static boolean DEBUG = HttpProtocol.DEBUG;

    private final static Object PROPERTIES[][] = { {
            "servlet",
            "class name of the servlet. Specify the class name of your servlet, which must be in classpath. de.bb.jsp.JspServlet is a servlet handler for *.jsp files.",
            Servlet.class }, };

    private String className;

    javax.servlet.Servlet servlet = null;

    private LogFile logFile;

    ServletRegistration.Dynamic servletRegistration;

    public ServletHandler() {
        init("ServletHandler", PROPERTIES);
    }

    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);
        this.logFile = logFile;
        className = getProperty("servlet");
        if (DEBUG)
            System.out.println(className);
    }

    public void setServlet(javax.servlet.Servlet s)
            throws javax.servlet.ServletException {
        servlet = s;
    }

    public void service(javax.servlet.ServletRequest in,
            javax.servlet.ServletResponse out) throws IOException,
            javax.servlet.ServletException {

        doFilter(in, out);
    }

    /**
     * Called by service() to load the Servlet. The function loads a class named
     * by the className parameter given in the constructor.
     * 
     * @param injector
     * 
     * @throws ServletException
     * @throws ClassNotFoundException
     * 
     * @exception java.lang.ClassNotFoundException
     */
    synchronized void loadServlet(LogFile log, Injector injector)
            throws Exception {
        if (servlet != null)
            return;

        servlet = createServlet(log, injector);
    }

    private Servlet createServlet(LogFile log, Injector injector)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, Exception, ServletException {
        Thread thread = Thread.currentThread();
        ClassLoader lastcl = null;
        try {
            if (classLoader != null) {
                lastcl = thread.getContextClassLoader();
                thread.setContextClassLoader(classLoader);
            }
            if (className == null)
                className = servletRegistration.className;
            Class<?> clazz = classLoader == null ? thread
                    .getContextClassLoader().loadClass(className) : classLoader
                    .loadClass(className);
            Object o = clazz.newInstance();
            if (!(o instanceof javax.servlet.Servlet)) {
                throw new ClassNotFoundException("class " + className
                        + " is not a servlet");
            }
            if (injector != null)
                injector.inject(log, o);
            javax.servlet.Servlet s = (javax.servlet.Servlet) o;
            s.init(sConfig);
            return s;
        } finally {
            if (classLoader != null)
                thread.setContextClassLoader(lastcl);
        }
    }

    /**
     * The finalizer calls the destroy() function of the servlet
     */
    protected void finalize() throws Throwable {
        if (servlet != null)
            servlet.destroy();
        servlet = null;
        super.finalize();
    }

    // from FilterChain
    public void doFilter(javax.servlet.ServletRequest request,
            javax.servlet.ServletResponse response) throws IOException,
            javax.servlet.ServletException {
        javax.servlet.ServletResponse r = response;
        while (r instanceof javax.servlet.ServletResponseWrapper)
            r = ((javax.servlet.ServletResponseWrapper) r).getResponse();
        HttpResponse sr = (HttpResponse) r;
        sr.status = 200;

        try {
            if (servlet == null) {
                try {
                    if (DEBUG)
                        System.out.println("loading servlet");
                    loadServlet(logFile, null);
                } catch (Exception ex) {
                    if (DEBUG)
                        ex.printStackTrace();
                    sr.status = 503;
                }
            }

            if (servlet instanceof javax.servlet.SingleThreadModel) {
                synchronized (servlet) {
                    servlet.service(request, response);
                }
            } else {
                servlet.service(request, response);
            }

            if (sr.status != 200) {
                String errorPage = this.hContext.statusMap.get("" + sr.status);
                if (errorPage != null) {
                    javax.servlet.RequestDispatcher rd = request
                            .getRequestDispatcher(errorPage);
                    if (rd != null) {
                        forwardEx(rd, request, sr, null, sr.statusText);
                    }
                }
            }
        } catch (javax.servlet.ServletException sex) {
            sex.printStackTrace();
            throw sex;
        } catch (java.io.IOException iox) {
            throw iox;
        } catch (Exception ex) {
            if (sr.status < 500)
                sr.setStatus(500);

            String errorPage = this.hContext.exceptionMap.get(ex.getClass()
                    .getName());
            if (errorPage == null) {
                response.setContentType("text/html");
                PrintWriter out = null;
                try {
                    out = response.getWriter();
                } catch (Exception e) {
                    out = new PrintWriter(response.getOutputStream());
                }
                // out.println(ex.getMessage());

                out.println("<html><head><title>Servlet's Exception</title></head><body>");
                out.println("<h1>An Exeption occured for " + className
                        + ":</h1><br>");
                String msg = ex.getMessage();
                if (msg == null)
                    msg = "internal error";
                out.println("<pre>");
                out.println(msg);
                out.println("</pre>");
                out.println("<hr>\n");
                out.println("<pre>");
                ex.printStackTrace(out);
                out.println("</pre>");
                // out.write(e.toString());
                out.print("</body></html>");

                out.flush();
                return;
            }
            javax.servlet.RequestDispatcher rd = request
                    .getRequestDispatcher(errorPage);
            if (rd == null)
                throw new javax.servlet.ServletException(ex);
            forwardEx(rd, request, sr, ex, ex.getMessage());
        }
    }

    private void forwardEx(javax.servlet.RequestDispatcher rd,
            javax.servlet.ServletRequest request, HttpResponse sr,
            Exception ex, String msg) throws IOException,
            javax.servlet.ServletException {
        if (request.getAttribute("javax.servlet.error.servlet_name") != null)
            return;

        ServletRequest r = request;
        while (r instanceof javax.servlet.ServletRequestWrapper)
            r = ((javax.servlet.ServletRequestWrapper) r).getRequest();
        HttpRequest hr = (HttpRequest) r;

        request.setAttribute("javax.servlet.error.status_code", new Integer(
                sr.status));
        request.setAttribute("javax.servlet.error.request_uri",
                hr.getRequestURI());
        request.setAttribute("javax.servlet.error.servlet_name", getName());
        request.setAttribute("javax.servlet.error.message", msg);
        if (ex != null) {
            request.setAttribute("javax.servlet.error.exception_type",
                    ex.getClass());
            request.setAttribute("javax.servlet.error.exception", ex);
        }

        sr.setStatus(200);
        rd.forward(request, sr);
    }

}

/******************************************************************************
 * $Log: ServletHandler.java,v $ Revision 1.28 2014/06/23 15:38:46 bebbo
 * 
 * @N implemented form authentication
 * @R reworked authentication handling to support roles Revision 1.27 2012/07/18
 *    06:44:41 bebbo
 * 
 * @I typified Revision 1.26 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.25 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.24 2008/06/04 06:45:27 bebbo
 * @B fixed class loading issues on reload of web applications
 * 
 *    Revision 1.23 2008/01/17 17:33:17 bebbo
 * @R made some methods more visible
 * 
 *    Revision 1.22 2007/05/01 19:05:27 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.21 2007/01/18 21:49:03 bebbo
 * @I reformatted
 * 
 *    Revision 1.20 2006/02/06 09:16:44 bebbo
 * @I cleanup
 * 
 *    Revision 1.19 2004/12/13 15:35:55 bebbo
 * @B setting thread context class loader
 * 
 *    Revision 1.18 2004/04/07 16:31:18 bebbo
 * @R unavalaible servlets are dumping its stacktrace
 * 
 *    Revision 1.17 2004/03/23 12:29:22 bebbo
 * @D provides more stack trace information
 * 
 *    Revision 1.16 2003/07/30 11:31:05 bebbo
 * @R also sets the Thread context class loader
 * 
 *    Revision 1.15 2003/07/01 10:55:43 bebbo
 * @N added class browsing for Servlets
 * 
 *    Revision 1.14 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.13 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.12 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.11 2003/03/31 16:35:02 bebbo
 * @N added DNS support to verify mail sender
 * 
 *    Revision 1.10 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.9 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.8 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.7 2002/03/30 15:49:17 franke
 * @N support for filter chains
 * 
 *    Revision 1.6 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some
 *    config function calls. Also added the use of a special ClassLoader.
 * 
 *    Revision 1.5 2002/02/27 18:14:14 bebbo
 * @B synchronized init of ServletHandler
 * 
 *    Revision 1.4 2001/09/15 08:48:01 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.3 2001/06/11 06:32:32 bebbo
 * @B fixed servlet init params
 * 
 *    Revision 1.2 2001/04/16 13:43:55 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.1 2001/03/29 18:25:31 bebbo
 * @N new generated
 * 
 *****************************************************************************/
