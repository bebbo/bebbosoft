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
