/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/RequestDispatcher.java,v $
 * $Revision: 1.24 $
 * $Date: 2014/06/23 15:38:46 $
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

import java.util.ArrayList;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.bb.bejy.http.FilterRegistration.MappingData;

class RequestDispatcher implements javax.servlet.RequestDispatcher {

    static boolean DEBUG = HttpProtocol.DEBUG;

    String servletPath;

    String pathInfo;

    String query;

    String documentRoot;

    String realPath;

    // the handler which is invoked
    HttpHandler sHandler;

    private HttpContext context;

    RequestDispatcher(HttpContext context, String servletPath, String pathInfo,
            String query, HttpHandler sh) {
        this.context = context;
        while (servletPath.startsWith("//")) {
            servletPath = servletPath.substring(1);
        }
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.query = query;

        this.sHandler = sh;

        // without filename
        this.documentRoot = sHandler.hContext.sPath;
        /*
         * this.documentRoot = sHandler.hContext.getRealPath2("/"); int ep =
         * documentRoot.lastIndexOf(File.separatorChar); if (ep >= 0 && ep <
         * documentRoot.length() - 1) { documentRoot = documentRoot.substring(0,
         * ep); }
         */
    }

    // RequestDispatcher functions
    public void forward(javax.servlet.ServletRequest request,
            javax.servlet.ServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        HttpResponse origResponse = dereference(response);

        if (request instanceof HttpServletRequest) {
            HttpServletRequest hr = (HttpServletRequest) request;
            ForwardRequest fr;
            String pathInfo = hr.getPathInfo();
            String query = hr.getQueryString();
            if (servletPath == null) {
                String servletPath = hr.getServletPath();
                RequestDispatcher rd = new RequestDispatcher(context,
                        servletPath, pathInfo, query, this.sHandler);
                fr = new ForwardRequest(hr, rd);
            } else {
                RequestDispatcher rd = new RequestDispatcher(context,
                        servletPath, pathInfo, query, this.sHandler);
                fr = new ForwardRequest(hr, rd);
                if (hr.getAttribute("javax.servlet.forward.request_uri") == null) {
                    fr.setAttribute("javax.servlet.forward.request_uri",
                            hr.getRequestURI());
                    fr.setAttribute("javax.servlet.forward.context_path",
                            hr.getContextPath());
                    fr.setAttribute("javax.servlet.forward.servlet_path",
                            hr.getServletPath());
                    fr.setAttribute("javax.servlet.forward.path_info",
                            hr.getPathInfo());
                    fr.setAttribute("javax.servlet.forward.query_string",
                            hr.getQueryString());
                }
            }

            request = fr;
        }
        // reset output if any
        if (origResponse.sos != null)
            origResponse.sos.reset();
        // origResponse.pw = null;

        ArrayList<MappingData> filterVector = context.forwardFilterVector;
        if (filterVector.isEmpty())
            sHandler.service(request, response);
        else
            new AFilterChain(servletPath, filterVector.iterator(), sHandler)
                    .doFilter(request, response);

        if (origResponse.pw != null)
            origResponse.pw.close();
        if (origResponse.sos != null)
            origResponse.sos.close();

    }

    public void include(javax.servlet.ServletRequest request,
            javax.servlet.ServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        HttpRequest origRequest = dereference(request);
        HttpResponse origResponse = dereference(response);

        final String orgEncoding = origRequest.inHeaders
                .remove("ACCEPT-ENCODING");
        Object orgUri, orgCp, orgSp, orgPi, orgQs;
        orgUri = orgCp = orgSp = orgPi = orgQs = null;
        if (request instanceof HttpServletRequest) {
            HttpServletRequest hr = (HttpServletRequest) request;
            ForwardRequest fr;
            if (servletPath == null) {
                String servletPath = hr.getServletPath();
                String pathInfo = hr.getPathInfo();
                String query = hr.getQueryString();
                RequestDispatcher rd = new RequestDispatcher(context,
                        servletPath, pathInfo, query, this.sHandler);
                fr = new IncludeRequest(hr, rd);
            } else {
                fr = new IncludeRequest(hr, this);
                orgUri = fr.getAttribute("javax.servlet.include.request_uri");
                orgCp = fr.getAttribute("javax.servlet.include.context_path");
                orgSp = fr.getAttribute("javax.servlet.include.servlet_path");
                orgPi = fr.getAttribute("javax.servlet.include.path_info");
                orgQs = fr.getAttribute("javax.servlet.include.query_string");
                fr.setAttribute("javax.servlet.include.request_uri",
                        fr.getRequestURI());
                fr.setAttribute("javax.servlet.include.context_path",
                        fr.getContextPath());
                fr.setAttribute("javax.servlet.include.servlet_path",
                        fr.getServletPath());
                fr.setAttribute("javax.servlet.include.path_info",
                        fr.getPathInfo());
                fr.setAttribute("javax.servlet.include.query_string",
                        fr.getQueryString());
            }
            request = fr;
        }

        origRequest.inHeaders.remove("IF-MODIFIED-SINCE");

        try {
            ++origResponse.includeCount;

            ArrayList<MappingData> filterVector = context.includeFilterVector;
            if (filterVector.isEmpty())
                sHandler.service(request, response);
            else
                new AFilterChain(servletPath, filterVector.iterator(), sHandler)
                        .doFilter(request, response);

            if (origResponse.status == 302) {
                origResponse.status = 200;
                String url = (String) origResponse.outHeaders
                        .remove("Location");

                String base = origResponse.encodeRedirectUrl("/")
                        + sHandler.hContext.sContext.substring(1);
                url = url.substring(base.length());

                javax.servlet.RequestDispatcher rd = origRequest
                        .getRequestDispatcher(url);
                rd.include(origRequest, origResponse);
                // javax.servlet.RequestDispatcher rd =
                // request.getRequestDispatcher(url);
                // rd.include(request, response);
            }

        } catch (javax.servlet.ServletException sex) {
            throw sex;
        } catch (java.io.IOException iex) {
            throw iex;
        } finally {
            --origResponse.includeCount;
            origRequest.inHeaders.put("ACCEPT-ENCODING", orgEncoding);
        }

        if (origResponse.sos != null)
            origResponse.sos.open();

        if (orgUri != null && request instanceof HttpServletRequest) {
            HttpServletRequest hr = (HttpServletRequest) request;
            hr.setAttribute("javax.servlet.include.request_uri", orgUri);
            hr.setAttribute("javax.servlet.include.context_path", orgCp);
            hr.setAttribute("javax.servlet.include.servlet_path", orgSp);
            hr.setAttribute("javax.servlet.include.path_info", orgPi);
            hr.setAttribute("javax.servlet.include.query_string", orgQs);
        }

    }

    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        if (DEBUG)
            System.out.println("RequestDispatcher.handle -> " + sHandler);

        request = context.checkAccess(request.getMethod(), servletPath,
                request, response);
        if (request == null)
            return;

        final ArrayList<MappingData> filterVector = context.requestFilterVector;
        if (filterVector.isEmpty())
            sHandler.service(request, response);
        else
            new AFilterChain(servletPath, filterVector.iterator(), sHandler)
                    .doFilter(request, response);
    }

    /**
     * @param response
     * @return
     */
    static HttpResponse dereference(ServletResponse response) {
        while (response instanceof javax.servlet.http.HttpServletResponseWrapper) {
            response = ((javax.servlet.http.HttpServletResponseWrapper) response)
                    .getResponse();
        }
        return (HttpResponse) response;
    }

    /**
     * @param request
     * @return
     */
    static HttpRequest dereference(ServletRequest request) {
        while (request instanceof javax.servlet.http.HttpServletRequestWrapper) {
            request = ((javax.servlet.http.HttpServletRequestWrapper) request)
                    .getRequest();
        }
        return (HttpRequest) request;
    }

    String getRealPath() {
        if (this.realPath == null)
            this.realPath = sHandler.hContext.getRealPath(servletPath);
        return realPath;
    }

    public String toString() {
        return "RequestDispatcher: " + servletPath + " - " + pathInfo + " ? "
                + query;
    }

}
/******************************************************************************
 * $Log: RequestDispatcher.java,v $ Revision 1.24 2014/06/23 15:38:46 bebbo
 * 
 * @N implemented form authentication
 * @R reworked authentication handling to support roles Revision 1.23 2012/07/18
 *    06:44:55 bebbo
 * 
 * @I typified Revision 1.22 2009/11/25 08:29:13 bebbo
 * 
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.21 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.20 2008/01/17 17:33:02 bebbo
 * @B fixes for better handling if the request is wrapped
 * 
 *    Revision 1.19 2007/05/01 19:05:27 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.18 2007/01/18 21:48:47 bebbo
 * @B fixed getServletURI in forward and redirect
 * 
 *    Revision 1.17 2004/12/13 15:34:10 bebbo
 * @N include now also handels redirects (302)
 * 
 *    Revision 1.16 2004/04/20 13:23:28 bebbo
 * @B fixed possible SIooB
 * 
 *    Revision 1.15 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group,
 *    Cfg, Factory
 * 
 *    Revision 1.14 2004/04/07 16:30:37 bebbo
 * @O optimizations - removed unused variables/methods
 * 
 *    Revision 1.13 2004/03/24 09:42:54 bebbo
 * @I name changes
 * 
 *    Revision 1.12 2004/03/23 19:00:57 bebbo
 * @B include and forward using dereferenced response
 * 
 *    Revision 1.11 2004/03/23 14:44:14 bebbo
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.10 2004/03/23 12:27:46 bebbo
 * @B query strings in forward/include are now applied to the request
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.9 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.8 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.7 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.6 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.5 2002/05/19 12:54:22 bebbo
 * @B fixed H401 and H302 handling
 * 
 *    Revision 1.4 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.3 2002/03/30 15:49:05 franke
 * @N support for filter chains
 * @B fixed handling for include and forward
 * 
 *    Revision 1.2 2002/03/10 20:07:55 bebbo
 * @I changes due to redesigned buffer handling
 * 
 *    Revision 1.1 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions.
 *    Caused some changes, since members from HttpProtocol moved.
 * 
 */
