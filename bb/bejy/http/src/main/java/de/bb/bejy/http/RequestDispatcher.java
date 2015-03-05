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
