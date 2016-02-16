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

        if (queryString.length() > 0)
            v.add("REQUEST_URI=" + urlEscape(hp0.getRequestURI() + "?" + queryString));
        else
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

        sr.setContentLength(br.length());
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
