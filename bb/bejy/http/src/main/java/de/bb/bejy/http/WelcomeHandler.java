/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/WelcomeHandler.java,v $
 * $Revision: 1.9 $
 * $Date: 2014/06/23 15:38:46 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * file handler for bejy
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

import de.bb.util.*;

import java.io.*;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WelcomeHandler extends HttpHandler {
    private final static boolean DEBUG = HttpProtocol.DEBUG;
    private final static String PROPERTIES[][] = {{"default", "the default welcome files"},
            {"h404", "404 error handler page"}};

    private String defIndex;
    private String s404;

    public WelcomeHandler() {
        init("WelcomeHandler", PROPERTIES);
    }

    public void activate(LogFile logFile) throws Exception {
        defIndex = getProperty("default", "index.html");
        s404 = getProperty("h404");
        super.activate(logFile);
    }

    public void service(javax.servlet.ServletRequest in, javax.servlet.ServletResponse out) {
        HttpServletRequest hp = (HttpServletRequest) in;
        HttpRequestBase hp0 = RequestDispatcher.dereference(in);
        HttpResponse sr = RequestDispatcher.dereference(out);

        if (hp0.method.equals("OPTIONS")) {
            sendOptions(sr);
            return;
        }

        String path = hp.getServletPath();
        //    String pathInfo = hp.getPathInfo();
        //    if (pathInfo != null)
        //      path += pathInfo;
        String realPath = hp0.context.getRealPath2(path);
        if (DEBUG)
            System.out.println("welcomehandler: " + realPath);

        try {
            File f = new File(realPath);
            if (!f.exists() && s404 != null) {
                String newPath = s404 + path;
                RequestDispatcher rd = (RequestDispatcher) hp0.context.getRequestDispatcher(newPath);
                if (rd.sHandler != this) {
                    ForwardRequest fr = new ForwardRequest(hp, rd);
                    rd.handle(fr, (HttpServletResponse) out);
                    return;
                }
            }
            if (f.isDirectory()) {
                String servletPath = hp.getServletPath();
                if (!servletPath.endsWith("/")) {
                    new HttpHandler.H302Handler(hContext).service(in, out);
                    return;
                }
                if (defIndex == null) {
                    sr.setStatus(403);
                    return;
                }

                String root = hp.getServletPath();

                for (StringTokenizer st = new StringTokenizer(defIndex, " ,\r\n\t\f"); st.hasMoreTokens();) {
                    String newPath = root + st.nextToken();
                    RequestDispatcher rd = (RequestDispatcher) hContext.getRequestDispatcher(newPath);
                    if (rd != null) {
                        rd.forward(in, out);
                        if (sr.status < 400)
                            return;
                    }
                }
            }
            sr.setStatus(404);

        } catch (Exception e) {
            if (DEBUG)
                e.printStackTrace();
        }
    }

    /**
     * @param wf
     */
    void setWelcomeFile(String wf) {
        defIndex = wf;
    }

}

/******************************************************************************
 * $Log: WelcomeHandler.java,v $
 * Revision 1.9  2014/06/23 15:38:46  bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles
 *
 * Revision 1.8  2013/05/17 10:30:57  bebbo
 * @R added support for OPTIONS
 * Revision 1.7 2010/07/08 18:16:25 bebbo
 * 
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 *    Revision 1.6 2010/04/11 10:16:14 bebbo
 * @N new configuration option "h404" to add a 404 handler to CGI (e.g. PHP) based applications to enable stuff like
 *    wordpress permalinks.
 * 
 *    Revision 1.5 2009/11/25 08:29:13 bebbo
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.4 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.3 2004/12/16 16:00:17 bebbo
 * @I changes due to getRealPath() changes (\ instead of /)
 * 
 *    Revision 1.2 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.1 2003/09/30 12:42:27 bebbo
 * @N added welcome handler
 * 
 *****************************************************************************/
