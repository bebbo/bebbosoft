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
    //    private boolean extPI;

    private final static boolean DEBUG = HttpProtocol.DEBUG;

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

    /**
     * compare the file name with the specified pattern. the pattern is held in the global variable maskBytes
     * 
     * @param name
     *            the checked file name
     * @param pos
     *            is filled with: p[0] start if path, p[1] end of filename = position of last '/' p[2] end if mask =
     *            start of path extra info
     * @return true if the file name matches the pattern, false either. / boolean accept(ByteRef name, int pos[]) { for
     *         (Iterator i = masks.iterator(); i.hasNext();) { byte[] maskBytes = (byte[])i.next(); if (accept(name,
     *         pos, maskBytes)) return true; } return false; }
     */
    /**
     * @param name
     * @param pos
     * @param maskBytes
     * @return
     */
    //    private boolean accept(ByteRef name, int[] pos, byte[] maskBytes) {
    //        if (DEBUG) {
    //            System.out.println("" + name + " with " + new String(maskBytes)); // + " def=" + bDefault);
    //        }
    //
    //        byte[] n = name.toByteArray();
    //
    //        boolean star = false;
    //        int lastSlash = 0;
    //        int i = 0, j = 0;
    //        int lastStarPos = -1;
    //        while (i < maskBytes.length && j < n.length) {
    //            // next mask character      
    //            byte mask = maskBytes[i];
    //            if (mask == '*') {
    //                lastStarPos = i;
    //                ++i;
    //                star = true;
    //                continue;
    //            }
    //
    //            // get next character
    //            byte ch = n[j];
    //            // disable wild star
    //            if (star) {
    //                if (ch == mask) {
    //                    star = false;
    //                }
    //                if (ch == '/') {
    //                    lastSlash = j + 1;
    //                }
    //            }
    //            // match against star?
    //            if (!star) {
    //                if (ch != mask) {
    //                    if (lastStarPos >= 0) {
    //                        i = lastStarPos;
    //                        continue;
    //                    }
    //                    break;
    //                }
    //                ++i;
    //            }
    //            ++j;
    //            if (i == maskBytes.length && j < n.length && lastStarPos >= 0 && "/;,?".indexOf(n[j]) < 0) {
    //                i = lastStarPos;
    //            }
    //        }
    //        /*
    //            if (i > 15)
    //            {
    //              i = i + 0;
    //            }
    //        */
    //        // did not fit the mask
    //        if (i < maskBytes.length) {
    //            // check for "/*" in mask
    //            if (i + 2 == maskBytes.length && maskBytes[i] == '/') {
    //                ++i;
    //            }
    //
    //            if (i == 0 || maskBytes[i - 1] != '/') {
    //                return false;
    //            }
    //
    //            if (i + 1 != maskBytes.length || maskBytes[i] != '*') {
    //                return false;
    //            }
    //
    //            // /foo/bar matches also foo/bar/*
    //            ++i;
    //            star = true;
    //        }
    //
    //        if (star) {
    //            if (extPI) {
    //                i -= 2;
    //                pos[2] = i;
    //                while (--i >= 0) {
    //                    if (n[i] == '/') {
    //                        break;
    //                    }
    //                }
    //                pos[1] = i;
    //                return true;
    //            }
    //
    //            j = n.length;
    //            lastSlash = j;
    //            while (--lastSlash >= 0) {
    //                if (n[lastSlash] == '/') {
    //                    ++lastSlash;
    //                    break;
    //                }
    //            }
    //        }
    //
    //        if (j < n.length && "/;,?".indexOf(n[j]) < 0) {
    //            return false;
    //        }
    //
    //        pos[1] = lastSlash;
    //        pos[2] = j;
    //
    //        return true;
    //
    //        /*    
    //            // now compare this with the maskBytes
    //            int i = 0, j = 0;
    //            int end = -1;
    //            boolean star = false;
    //            try {
    //              for(;i < maskBytes.length && j < n.length;)
    //              {
    //                if (maskBytes[i] == '*')
    //                {
    //                  ++i;
    //                  star = true;
    //                  end = j;
    //                  continue;
    //                }
    //                  
    //                if (maskBytes[i] == n[j] || maskBytes[i] == '?')
    //                {
    //                  ++i;
    //                  star = false;
    //                  end = j;
    //                  ++j;
    //                  continue;
    //                }
    //                if (!star)
    //                  break;
    //                ++j;
    //                end = j;
    //              }
    //            } catch (Exception e)
    //            {}
    //            
    //            if (star) {
    //              --j;
    //              --end;
    //            }
    //        //      j = n.length;
    //            
    //            //ignore one trailing star
    //            if (i + 1 == maskBytes.length && maskBytes[i] == '*' && maskBytes.length > 1)
    //            {
    //              ++i;
    //            } else
    //            //ignore one trailing /*
    //            if (i + 2 == maskBytes.length && maskBytes[i] == '/')
    //            {
    //              i += 2;
    //            }
    //            
    //            // mask must match completely
    //            if (i != maskBytes.length)
    //            {
    //              return false;
    //            }
    //            
    //        /*      
    //            if (star)
    //            {
    //              end = j;
    //              j = n.length;
    //            }
    //        * /    
    //            // end   : start of filename itself
    //            // j     : end of servlet path, start of extra info    
    //            pos[1] = end > pos[0] ? end : pos[0];
    //            pos[2] = j > 0 ? j : 0;
    //            
    //            return true;
    //        */
    //    }

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
