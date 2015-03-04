/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpHandler.java,v $
 * $Revision: 1.40 $
 * $Date: 2013/05/17 10:33:53 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * HttpHandler base class
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

/******************************************************************************
 * $Log: HttpHandler.java,v $ Revision 1.40 2013/05/17 10:33:53 bebbo
 * 
 * @R added support for OPTIONS Revision 1.39 2012/07/18 06:44:40 bebbo
 * 
 * @I typified Revision 1.38 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.37 2009/11/25 08:29:13 bebbo
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.36 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.35 2008/01/17 17:31:51 bebbo
 * @I modified the context matching algorithm. Now a Trie is used.
 * 
 *    Revision 1.34 2007/05/01 19:05:26 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.33 2007/01/18 21:46:54 bebbo
 * @N added support for OPTIONS
 * @R moved classLoader instance for threadContext classloader
 * 
 *    Revision 1.32 2004/12/13 15:32:31 bebbo
 * @B added support for multiple mappings to the same servlet
 * 
 *    Revision 1.31 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.30 2004/03/23 11:15:08 bebbo
 * @B modified the method to select the correct servlet, might be still not perfect...
 * 
 *    Revision 1.29 2003/09/30 12:42:27 bebbo
 * @N added welcome handler
 * 
 *    Revision 1.28 2003/07/01 11:10:17 bebbo
 * @R H401 and H404 are now public available
 * 
 *    Revision 1.27 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.26 2003/06/18 08:36:52 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.25 2003/06/17 12:09:56 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.24 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.23 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.22 2003/01/27 14:58:31 bebbo
 * @I removed usage of some obsolete functions
 * 
 *    Revision 1.21 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.20 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.19 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.18 2002/05/19 12:54:22 bebbo
 * @B fixed H401 and H302 handling
 * 
 *    Revision 1.17 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.16 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some config function calls. Also added the
 *    use of a special ClassLoader.
 * 
 *    Revision 1.15 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions. Caused some changes, since members from
 *    HttpProtocol moved.
 * 
 *    Revision 1.14 2001/11/20 17:36:42 bebbo
 * @B fixed RequestDispatcher stuff
 * 
 *    Revision 1.13 2001/09/15 08:46:54 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.12 2001/06/11 06:32:43 bebbo
 * @D more DEBUG out
 * 
 *    Revision 1.11 2001/04/16 20:03:56 bebbo
 * @B fixes in 302 redirect
 * 
 *    Revision 1.10 2001/04/16 13:43:54 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.9 2001/04/11 13:16:08 bebbo
 * @R if requested name matches a directory, a redirect is replied (302)
 * @N added H302 Handler
 * 
 *    Revision 1.8 2001/04/06 05:54:05 bebbo
 * @B fix for HTTP/1.0
 * @B fix for cookies
 * 
 *    Revision 1.7 2001/03/30 17:28:04 bebbo
 * @N added user authentication
 * 
 *    Revision 1.6 2001/03/29 18:25:58 bebbo
 * @I completed beta stage
 * 
 *    Revision 1.5 2001/03/29 07:08:18 bebbo
 * @R HttpHandler now implements javax.servlet.Servlet and javax.servlet.ServletConfig
 * 
 *    Revision 1.4 2001/03/28 09:15:04 bebbo
 * @D debug off
 * 
 *    Revision 1.3 2001/03/27 19:48:53 bebbo
 * @I lot's of stuff changed
 * @I changed pattern matching and usage of default index
 * 
 *    Revision 1.2 2001/03/20 18:34:07 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 * 
 *    Revision 1.1 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *****************************************************************************/
