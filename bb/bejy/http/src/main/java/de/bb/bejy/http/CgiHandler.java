/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/CgiHandler.java,v $
 * $Revision: 1.50 $
 * $Date: 2014/06/23 15:36:42 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * CGI handler for bejy
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import de.bb.bejy.Version;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;

public class CgiHandler extends HttpHandler {
    private boolean DEBUG = HttpProtocol.DEBUG;

    private final static String PROPERTIES[][] = {
            { "command", "the CGI command line" },
            //      {"chdir", "command line switch to set the current directory"},
            { "timeout", "maximum timeout im ms to wait for completion", "180000" },
            { "debug", "enable debug output", "false" }, { "h404", "handler for file not found", "" },
            { "env", "additional environment vars: name1=value1|name2=value2|...", "" },
            { "needsFile", "url must reference an existing file", "true" } };

    private final static ByteRef STATUS = new ByteRef("STATUS");

    private final static ByteRef COOKIE = new ByteRef("SET-COOKIE");

    private final static ByteRef LOCATION = new ByteRef("LOCATION");

    private final static String SERVER_SOFTWARE = "SERVER_SOFTWARE=BEJY/" + Version.getVersion() + " http/"
            + HttpProtocol.getVersion();

    private static ArrayList<String> globalEnv;

    private ArrayList<String> myEnv = new ArrayList<String>();

    private String sCommand;

    //String sChDir;
    long timeout;

    private String h404;

    private boolean needsFile;

    public CgiHandler() {
        init("CGI Handler", PROPERTIES);
        DEBUG |= getBooleanProperty("debug", false);
    }

    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);

        sCommand = getProperty("command", "echo not configured for");
        //    sChDir = getProperty("chdir");
        timeout = getIntProperty("timeout", 45000);
        DEBUG |= getBooleanProperty("debug", false);

        h404 = getProperty("h404", "");

        needsFile = "true".equals(getProperty("needsFile", "true"));

        final String senv = getProperty("env", "");
        for (final StringTokenizer st = new StringTokenizer(senv, "|"); st.hasMoreElements();) {
            final String t = st.nextToken();
            myEnv.add(t);
        }
    }

    public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
            throws IOException {
        HttpServletRequest hreq = (HttpServletRequest) request;
        HttpRequest hp0 = RequestDispatcher.dereference(request);
        HttpResponse sr = RequestDispatcher.dereference(response);

        if (hp0.method.equals("OPTIONS")) {
            sendOptions(sr);
            return;
        }

        HttpContext context = hp0.context;

        String queryString = hreq.getQueryString();
        String servletPath = hreq.getServletPath();
        String pathInfo = hreq.getPathInfo();

        String redirectUrl = null;
        if (needsFile) {
            String path = hp0.context.getRealPath2(servletPath);
            if (DEBUG)
                System.out.println("CGI handler: " + path);

            File f = new File(path);
            if (!f.exists()) {
                // apply configure redirect
                if (h404 != null && h404.length() > 0 && !h404.startsWith(servletPath)) {
                    redirectUrl = urlEscape(servletPath);
                    servletPath = h404 + redirectUrl;
                    int qm = servletPath.indexOf('?');
                    if (qm > 0) {
                        if (queryString != null) {
                            queryString += "&" + servletPath.substring(qm + 1);
                        } else {
                            queryString = servletPath.substring(qm + 1);
                        }
                        servletPath = servletPath.substring(0, qm);
                    }
                    path = hp0.context.getRealPath2(servletPath);
                    if (DEBUG)
                        System.out.println("CGI redirect: " + path);
                    f = new File(path);
                }
            }
            if (!f.exists()) {
                sr.setStatus(404);
                return;
            }
            // send redirect if trailing slash is missing
            if (f.isDirectory()) {
                if (!servletPath.endsWith("/")) {
                    new HttpHandler.H302Handler(hContext).service(request, response);
                    return;
                }
            }
        }

        if (queryString == null)
            queryString = "";

        InputStream is = hreq.getInputStream();
        OutputStream os = response.getOutputStream();

        ArrayList<String> v = (ArrayList<String>) globalEnv.clone();
        v.addAll(myEnv);

        int cl = hreq.getContentLength();
        if (cl >= 0) {
            v.add("CONTENT_LENGTH=" + cl);
        } else {
            cl = is.available();
        }

        String s = hreq.getContentType();
        if (s != null) {
            v.add("CONTENT_TYPE=" + s);
            v.add("HTTP_CONTENT_TYPE=" + s);
        }

        // v.add("DOCUMENT_ROOT=" + hp.getContextPath());
        v.add("DOCUMENT_ROOT=" + context.getRealPath("/"));
        v.add("REDIRECT_STATUS=200");
        if (redirectUrl != null)
            v.add("REDIRECT_URL=" + redirectUrl); // ??

        v.add("GATEWAY_INTERFACE=CGI/1.1"); // ??

        if (pathInfo != null)
            v.add("PATH_INFO=" + pathInfo);
        v.add("PATH_TRANSLATED=" + context.getRealPath(servletPath));
        v.add("QUERY_STRING=" + queryString);
        v.add("REMOTE_ADDR=" + hreq.getRemoteAddr());
        v.add("REMOTE_HOST=" + hreq.getRemoteHost());
        //    v.add("REMOTE_IDENT=");  // what's that?
        s = hreq.getRemoteUser();
        if (s != null)
            v.add("REMOTE_USER=" + hreq.getRemoteUser());
        v.add("REQUEST_METHOD=" + hreq.getMethod());

        s = hreq.getContextPath();
        {
            String s2 = servletPath;
            if (s.endsWith("/") && s2.startsWith("/"))
                s += s2.substring(1);
            else
                s += s2;
        }
        v.add("SCRIPT_NAME=" + s);

        v.add("REQUEST_URI=" + urlEscape(hp0.getRequestURI()));
        v.add("SERVER_NAME=" + hreq.getServerName());
        v.add("SERVER_PORT=" + hreq.getServerPort());
        v.add("SERVER_PROTOCOL=" + hreq.getProtocol());

        v.add(SERVER_SOFTWARE);
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cookies.length; ++i) {
                javax.servlet.http.Cookie c = cookies[i];
                if (sb.length() > 0)
                    sb.append("; ");
                sb.append(c.getName());
                sb.append('=');
                sb.append(c.getValue());
            }
            v.add("HTTP_COOKIE=" + sb);
        }

        for (Enumeration<String> e = hreq.getHeaderNames(); e.hasMoreElements();) {
            String key = e.nextElement();
            String val = hreq.getHeader(key);
            key = key.toUpperCase();
            int idx;
            while ((idx = key.indexOf('-')) >= 0) {
                key = key.substring(0, idx) + "_" + (key.substring(idx + 1));
            }
            v.add("HTTP_" + key + "=" + val);
        }
        //    v.add("HTTP_ACCEPT=" + hp.getHeader("accept"));
        String referer = hreq.getHeader("referer");
        if (referer != null)
            v.add("HTTP_REFERER=" + referer);

        v.add("HTTP_USER_AGENT=" + hreq.getHeader("user-agent"));

        //  
        String env[] = new String[v.size()];
        v.toArray(env);

        if (DEBUG) {
            for (int i = 0; i < env.length; ++i)
                System.out.println(env[i]);
        }

        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();

        String cmd = sCommand + ' ';
        String realPath = hreq.getRealPath(servletPath);

        File dir = null;
        if (needsFile) {
            int dp = realPath.lastIndexOf('/') + 1;
            if (dp > 0) {
                dir = new File(realPath.substring(0, dp));
                realPath = realPath.substring(dp);
            }
        }
        cmd += realPath;

        if (DEBUG)
            System.out.println("executing: " + cmd);

        int ret;
        if (DEBUG) {
            byte b[];
            if (cl >= 0) {
                b = new byte[cl]; // b[cl-2] = 0xd; // b[cl-1] = 0xa;

                if (cl > 0) {
                    for (int pos = 0; pos < cl;) {
                        int len = is.read(b, pos, cl - pos);
                        if (len == 0)
                            break;
                        pos += len;
                    }
                }
            } else {
                ByteRef pd = new ByteRef();
                System.out.println("PD: " + pd);
                while (pd.update(is) != null) {
                }
                b = pd.toByteArray();
            }

            if (cl > 0) {
                System.out.println("POST DATA: " + new String(b));
                System.out.println("POST DATA LEN: " + b.length);
            }
            ret = de.bb.util.Process.execute(cmd, new ByteArrayInputStream(b), bos, env, timeout, dir);
        } else {
            ret = de.bb.util.Process.execute(cmd, is, bos, env, timeout, dir);
        }
        if (DEBUG) {
            System.out.println("returned: " + ret);
            System.out.println("output: " + bos.toString());
        }

        sr.setStatus(500);

        if (bos.size() == 0 && ret < 0)
            return;

        ByteRef br = new ByteRef(bos.toByteArray());
        int status = -1;
        String statusMsg = null;
        for (;;) {
            ByteRef l = br.nextLine();
            if (l == null || l.length() == 0) {
                if (status < 0)
                    status = 200;
                if (status >= 400) {
                    if (statusMsg != null)
                        sr.sendError(status, statusMsg);
                    else
                        sr.sendError(status);
                } else {
                    sr.setStatus(status);
                    sr.setContentLength(br.length());
                    if (DEBUG)
                        System.out.println("content-length: " + br.length());
                }
                break;
            }
            int p = l.indexOf(':');
            if (p > 0) {
                ByteRef key = l.substring(0, p).toUpperCase();
                ByteRef val = l.substring(p + 1).trim();
                if (key.equals(STATUS)) {
                    p = val.indexOf(' ');
                    try {
                        status = Integer.parseInt(val.substring(0, p).toString());
                        statusMsg = val.substring(p + 1).toString();
                    } catch (Exception e) {
                    }
                } else if (key.equals(COOKIE))
                    sr.addCookie(HttpResponse.createCookie(val));
                else if (key.equals(LOCATION)) {
                    sr.sendRedirect(val.toString());
                    status = 302;
                } else
                    sr.addHeader(key.toString(), val.toString());
            }
            if (DEBUG)
                System.out.println(l);
        }

        br.writeTo(os);
    }

    static {
        globalEnv = new ArrayList<String>();
        // autodetect some settings...
        try {
            String os = System.getProperty("os.name").toUpperCase();
            //      if (DEBUG) 
            System.out.println(os);

            if (os.length() >= 7 && os.substring(0, 7).equals("WINDOWS")) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                if (os.endsWith("NT") || os.endsWith("2000") || os.endsWith("XP") || os.endsWith("VISTA")
                        || os.startsWith("WINDOWS"))
                    de.bb.util.Process.execute("cmd /c set", null, bos, null);
                else
                    de.bb.util.Process.execute("command /c set", null, bos, null);

                String br = bos.toString().toUpperCase();
                //System.out.println(br);

                String sroot, windir;
                sroot = windir = null;
                int idx = br.indexOf("SYSTEMROOT=");
                if (idx > 0) {
                    int end = br.indexOf('\r', idx);
                    sroot = br.substring(idx, end);
                }
                idx = br.indexOf("WINDIR=");
                if (idx > 0) {
                    int end = br.indexOf('\r', idx);
                    windir = br.substring(idx, end);
                } else {
                    if (sroot != null)
                        windir = "WINDIR=" + sroot.substring(11);
                }
                //        System.out.println(sroot);
                //        System.out.println(windir);
                if (sroot != null)
                    globalEnv.add(sroot);
                if (windir != null)
                    globalEnv.add(windir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert illegal characters into % quoted representation. "foo bar" -> "foo%20bar" No support for UTF yet.
     * 
     * @param url
     *            the url
     * @return the escaped url
     */
    public static String urlEscape(String url) {
        url = url.trim();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < url.length(); ++i) {
            char ch = url.charAt(i);
            if (ch <= 32 || ch > 127 || ILLEGAL.indexOf(ch) >= 0) {
                sb.append("%").append(Integer.toHexString(ch >> 4)).append(Integer.toHexString(ch & 0xf));
            } else
                sb.append(ch);
        }
        return sb.toString();
    }

    private final static String ILLEGAL = "<>'\"+&";

}

/******************************************************************************
 * $Log: CgiHandler.java,v $
 * Revision 1.50  2014/06/23 15:36:42  bebbo
 * @N added needsFile="false" - set this to true if no file existence check must be done
 * Revision 1.49 2013/12/29 21:32:49 bebbo
 * 
 * @N added property "env" to configure additional environment entries
 * 
 *    Revision 1.48 2013/07/23 06:40:10 bebbo
 * @B DEBUG on POST did not change the directory.
 * 
 *    Revision 1.47 2013/06/07 16:23:44 bebbo
 * @B forwarding to a CGI handler no longer reads the post data. Revision 1.46 2013/05/17 10:33:42 bebbo
 * 
 * @R added support for OPTIONS Revision 1.45 2012/12/21 08:14:23 bebbo
 * 
 * @B fixed REQUEST_URI with 404 redirection: keep the original URI Revision 1.44 2012/11/14 15:11:44 bebbo
 * 
 * @B h404 directly invokes the CgiHandler
 * @B CgiHandler no longer uses a ForwardRequest, CGI params are set directly
 * @B cgi handler with h404 is working for Drupal Revision 1.43 2012/11/08 12:10:56 bebbo
 * 
 * @N h404 handler is able to use URL rewriting Revision 1.42 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.41 2010/07/08 18:16:25 bebbo
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 *    Revision 1.40 2010/04/11 10:16:15 bebbo
 * @N new configuration option "h404" to add a 404 handler to CGI (e.g. PHP) based applications to enable stuff like
 *    wordpress permalinks.
 * 
 *    Revision 1.39 2010/04/10 12:10:07 bebbo
 * @I reformatted
 * 
 *    Revision 1.38 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.37 2009/06/15 08:36:26 bebbo
 * @B B-REMOTEHOST is used correctly
 * @I using BufferedStreams where convenient
 * 
 *    Revision 1.36 2008/01/17 17:26:21 bebbo
 * @B fixed to better handle cookies if Request is wrapped
 * 
 *    Revision 1.35 2004/04/07 16:27:24 bebbo
 * @O optimizations - removed unused variables/methods
 * 
 *    Revision 1.34 2004/03/23 14:44:14 bebbo
 * @B the original request is now properly resovled from request wrappers
 * 
 *    Revision 1.33 2003/11/26 09:58:48 bebbo
 * @B fixed COOKIE handling
 * 
 *    Revision 1.32 2003/11/16 09:03:47 bebbo
 * @B fixed redirect if status occured to soon
 * 
 *    Revision 1.31 2003/09/05 10:57:57 bebbo
 * @B checking whether resource exists and send 404 if not
 * 
 *    Revision 1.30 2003/09/04 09:18:28 bebbo
 * @B REQUEST_URI used RequestURL... fixed to RequestURI
 * 
 *    Revision 1.29 2003/09/03 15:00:20 bebbo
 * @B fixed SCRIPT_NAME
 * @N added REQUEST_URI
 * 
 *    Revision 1.28 2003/07/09 18:29:49 bebbo
 * @N added default values.
 * 
 *    Revision 1.27 2003/06/17 10:18:43 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.26 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.25 2003/03/21 10:14:08 bebbo
 * @I cleanup
 * 
 *    Revision 1.24 2003/03/20 12:11:43 bebbo
 * @I updated server message
 * 
 *    Revision 1.23 2003/02/06 13:41:21 bebbo
 * @I inputstream is now unbuffered
 * 
 *    Revision 1.22 2003/02/05 08:11:44 bebbo
 * @R now all headers are set as HTTP_xxx env vars
 * 
 *    Revision 1.21 2003/02/02 15:41:23 bebbo
 * @B headers without space were corrupted
 * 
 *    Revision 1.20 2003/01/27 19:33:08 bebbo
 * @N status code is now set on redirects, if ommitted
 * @N added some PHP environment variables
 * 
 *    Revision 1.19 2003/01/27 15:10:40 bebbo
 * @N a Location Header leads now to rediredt Status Code
 * 
 *    Revision 1.18 2003/01/27 14:58:31 bebbo
 * @I removed usage of some obsolete functions
 * 
 *    Revision 1.17 2003/01/07 18:32:20 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.16 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.15 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.14 2002/07/16 10:53:50 bebbo
 * @B fixed PATH_TRANSLATED
 * @B removed REMOTE_USER amd PATH_INFO if null
 * 
 *    Revision 1.13 2002/04/02 13:02:34 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.12 2002/03/10 20:11:06 bebbo
 * @I changes due to redesigned buffer handling
 * 
 *    Revision 1.11 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions. Caused some changes, since members from
 *    HttpProtocol moved.
 * 
 *    Revision 1.10 2001/09/15 08:46:25 bebbo
 * @I using XmlFile instead of ConfigFile
 * 
 *    Revision 1.9 2001/08/24 08:24:26 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.8 2001/04/22 20:27:32 bebbo
 * @R moved the execute function to de.bb.util.Process
 * 
 *    Revision 1.7 2001/04/16 13:43:54 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.6 2001/04/11 13:15:11 bebbo
 * @R added check for return codes - missing CGIs generates now 500
 * 
 *    Revision 1.5 2001/04/06 05:54:05 bebbo
 * @B fix for HTTP/1.0
 * @B fix for cookies
 * 
 *    Revision 1.4 2001/03/29 07:08:18 bebbo
 * @R HttpHandler now implements javax.servlet.Servlet and javax.servlet.ServletConfig
 * 
 *    Revision 1.3 2001/03/28 09:15:05 bebbo
 * @D debug off
 * 
 *    Revision 1.2 2001/03/27 19:49:39 bebbo
 * @I removed clone
 * @I all member vars are readonly now
 * 
 *    Revision 1.1 2001/03/20 18:34:08 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 * 
 *    Revision 1.1 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *****************************************************************************/
