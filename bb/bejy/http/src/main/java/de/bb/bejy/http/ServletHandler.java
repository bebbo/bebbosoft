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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import de.bb.bejy.http.ServletRegistration.Dynamic;
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

    ServletHandler(WebAppContext webAppContext, Dynamic servletRegistration) {
        this.classLoader = webAppContext.zcl;
        this.servletRegistration = servletRegistration;
        servlet = servletRegistration.servlet;
    }

    
    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);
        this.logFile = logFile;
        className = getProperty("servlet");
        if (className == null)
            className = "de.bb.jsp.JspServlet";
        if (DEBUG)
            System.out.println(className);
        
        ServletRegistration.Dynamic sr = (Dynamic) hContext.addServlet(className + ":" + getProperty("mask"), className);
        init(sr);
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
