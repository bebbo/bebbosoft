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

import java.util.HashMap;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import de.bb.util.ByteRef;

class ForwardRequest extends HttpServletRequestWrapper {

    private RequestDispatcher rd;
    private HttpServletRequest hsr;
    private HashMap<String, String[]> parameters = new HashMap<String, String[]>();

    ForwardRequest(HttpServletRequest request, RequestDispatcher rd) {
        super(request);
        this.rd = rd;
        hsr = request;
        
        // read post data only if no external program is called
        if (!(rd.sHandler instanceof CgiHandler || rd.sHandler instanceof FastCgiHandler))
                parameters.putAll(request.getParameterMap());
        if (rd.query != null)
            HttpRequest.extractParameters(parameters, new ByteRef(rd.query), null);
    }

    public String getPathInfo() {
        return rd.pathInfo;
    }

    public String getPathTranslated() {
        return rd.getRealPath();
    }

    public String getQueryString() {
        return rd.query;
    }

    public String getServletPath() {
        String sp = rd.servletPath;
        if (sp != null)
            return sp;
        return hsr.getServletPath();
    }

    public String getRequestURI() {
        String cp = getContextPath();
        String sp = getServletPath();
        String pi = getPathInfo();
        if (pi != null) {
            sp += pi;
        }
        if (cp.endsWith("/") && sp.startsWith("/")) {
            sp = sp.substring(1);
        }
        return cp + sp;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        String urlPath = getRequestURI();
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443) {
            url.append(':');
            url.append(port);
        }
        url.append(urlPath);
        return url;
    }

    public java.util.Map getParameterMap() {
        return parameters;
    }

    public java.util.Enumeration getParameterNames() {
        return new IterEnum(parameters.keySet().iterator());
    }

    public java.lang.String[] getParameterValues(java.lang.String name) {
        Object ret = parameters.get(name);
        if (ret == null)
            return null;
        return (ret instanceof String[]) ? (String[]) ret : new String[]{ret.toString()};
    }

    public java.lang.String getParameter(java.lang.String name) {
        Object o = parameters.get(name);
        if (o == null)
            return null;
        if (o instanceof String)
            return (String) o;
        try {
            String sa[] = (String[]) o;
            return sa[0];
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.FORWARD;
    }

    public String toString() {
        return "ForwardRequest: " + getServletPath() + parameters;
    }
}
