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
import java.util.Iterator;

import de.bb.bejy.Configurable;
import de.bb.bejy.Loadable;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;

public abstract class HttpHandler extends Configurable implements javax.servlet.Servlet, Loadable {

    private final static String PROPERTIES[][] = { { "mask", "the match pattern" }, };

    final static ByteRef M_GET = new ByteRef("GET"), M_PUT = new ByteRef("PUT"), M_POST = new ByteRef("POST"),
            M_HEAD = new ByteRef("HEAD"), M_OPTIONS = new ByteRef("OPTIONS");

    HttpContext hContext;

    javax.servlet.ServletConfig sConfig;

    String info;

    // the context parameters
    HashMap<String, String> parameter = new HashMap<String, String>();

    protected ClassLoader classLoader = null;

    /**
     * public CT since this is load by name.
     * 
     */
    public HttpHandler() {
        init("HttpHandler", PROPERTIES);
    }

    /**
     * CT for derived classes
     * 
     * @param hc
     */
    HttpHandler(HttpContext hc) {
        hContext = hc;
    }

    public void setContext(HttpContext hc) {
        hContext = hc;
    }

    public void activate(LogFile logFile) throws Exception {
        info = getProperty("info", null);
        /* TODO    
            if (masks.size() == 0)
            {
              String sMask   = getProperty("mask", "*");
              setMask(sMask);
            }
        */
        // init the context if not already set
        if (hContext == null) {
            hContext = (HttpContext) getParent();
        }

        // read parameters
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable o = i.next();
            if (o instanceof ParameterCfg) {
                String name = o.getProperty("name");
                String value = o.getProperty("value");
                if (name != null && value != null) {
                    parameter.put(name, value);
                }
            }
        }
    }

    public void destroy() // from javax.servlet.Servlet
    {
    }

    public javax.servlet.ServletConfig getServletConfig() // from javax.servlet.Servlet
    {
        return sConfig;
    }

    public java.lang.String getServletInfo() // from javax.servlet.Servlet
    {
        return info;
    }

    public void init(javax.servlet.ServletConfig sc) // from javax.servlet.Servlet
    {
        sConfig = sc;
    }

    /*  
      public void service(javax.servlet.ServletRequest in, javax.servlet.ServletResponse out) // from javax.servlet.Servlet
      {
      }
    */
    void addParameter(String name, String val) {
        parameter.put(name, val);
    }

    public java.lang.String getInitParameter(java.lang.String p) // from javax.servlet.ServletContext
    {
        return parameter.get(p);
    }

    public java.util.Enumeration<String> getInitParameterNames() // from javax.servlet.ServletContext
    {
        return new IterEnum<String>(parameter.keySet().iterator());
    }

    public javax.servlet.ServletContext getServletContext() // from javax.servlet.ServletConfig
    {
        return hContext;
    }

    public java.lang.String getServletName() // from javax.servlet.ServletConfig
    {
        return getName();
    }

    public static class H302Handler extends HttpHandler {
        H302Handler(HttpContext hc) {
            super(hc);
        }

        public void service(javax.servlet.ServletRequest in, javax.servlet.ServletResponse out) {
            HttpRequest hr = (HttpRequest) in;
            HttpResponse sr = (HttpResponse) out;
            HttpRequest hp0 = RequestDispatcher.dereference(in);

            StringBuffer sb = hr.getRequestURL();

            if (sb.charAt(sb.length() - 1) == '/') {
                return;
            }

            sb.append('/');
            String q = hp0.getQueryString();
            if (q != null && q.length() > 0) {
                sb.append("?").append(q);
            }
            sr.addHeader("location", sb.toString());
            sr.setStatus(302);
        }
    }

    //    /**
    //     * Mark the last path element as part of path info!
    //     */
    //    void extendPathInfo() {
    //        extPI = true;
    //    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Loadable#getExtensionId()
     */

    public String getImplementationId() {
        return "de.bb.bejy.http.handler";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */

    public void deactivate(LogFile logFile) throws Exception {
        parameter.clear();
        super.deactivate(logFile);
    }

    protected void setClassLoader(ClassLoader cl) {
        classLoader = cl;
    }

    protected void sendOptions(HttpResponse sr) {
        sr.addHeader("Accept", "OPTIONS, GET, HEAD, POST");
        sr.setStatus(200);
    }
}
