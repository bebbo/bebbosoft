/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpResponse.java,v $
 * $Revision: 1.33 $
 * $Date: 2014/10/21 20:34:45 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * HTTP protocol
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

 (c) 1994-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;

import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.DateFormat;

class HttpResponse implements javax.servlet.http.HttpServletResponse {
    // debug switch
    private final static boolean DEBUG = HttpProtocol.DEBUG;

    final static ByteRef HTTP = new ByteRef("HTTP/");

    final static ByteRef HTTP10 = new ByteRef("HTTP/1.0");

    final static ByteRef COMMENT = new ByteRef("COMMENT");

    final static ByteRef DOMAIN = new ByteRef("DOMAIN");

    final static ByteRef SECURE = new ByteRef("SECURE");

    final static ByteRef MAXAGE = new ByteRef("MAX-AGE");

    final static ByteRef VERSION = new ByteRef("VERSION");

    final static ByteRef PATH = new ByteRef("PATH");

    final static ByteRef EXPIRES = new ByteRef("EXPIRES");

    final static ByteRef HEAD = new ByteRef("HEAD");

    final static byte[] CRLF = {0xd, 0xa};
    final static byte[] CRLFCRLF = {0xd, 0xa, 0xd, 0xa};

    final static byte[] SERVER = "\r\nServer: ".getBytes();
    final static byte[] DATE = "\r\nDate: ".getBytes();
    final static byte[] CONTENTLENGTH = "\r\nContent-Length: ".getBytes();
    final static byte[] SETCOOKIE_JSESSION = "\r\nSet-Cookie: JSESSIONID=\"".getBytes();
    final static byte[] COOKIEPATH = "\"; path=\"".getBytes();
    final static byte[] COOKIESECURE = "\"; secure".getBytes();
    final static byte[] COOKIEEND = "\"; httponly".getBytes();
    final static byte[] SETCOOKIE = "\r\nSet-Cookie: ".getBytes();
    final static byte[] COLONSPACE = ": ".getBytes();

    // attached HttpRequest object
    HttpRequest request;

    // attached OutputStream
    OutputStream os;

    // used character encoding for writing
    String charEncoding = "ISO-8859-1";

    // used Locale to write
    Locale outLocale;

    // all cookies which are sent
    HashMap<String, Cookie> outCookies;

    // all headers to send
    HashMap<String, String> outHeaders = new HashMap<String, String>();

    // used to disable set/add/remove functions when in include mode
    int includeCount;

    // current outBufferSize
    int bufferSize;

    // our output stream
    SOStream sos;

    // an eventually used printwriter
    PrintWriter pw;

    // status of this response
    int status;

    // attached status text
    String statusText;

    // out content length
    long contentLength = -1;

    // mark whether header is already written
    boolean headerWritten;

    HttpResponse(HttpRequest request) {
        this.request = request;
        bufferSize = 0x2000;
        if ("HEAD".equals(request.method))
            contentLength = 0;
    }

    void flushRequest() throws java.io.IOException {
        if (pw != null)
            pw.close();
        if (sos == null)
            getOutputStream();
        if (status != 200 && sos.written == 0 && contentLength < 0)
            ByteUtil.writeString("<html><body><h1>" + statusText + "</h1>" + status + "</body></html>", sos);
        sos.close();
    }

    protected void writeHeader() throws IOException {
        if (headerWritten)
            return;
        headerWritten = true;

        //        if (contentLength < 0 && request.protocol.startsWith(HTTP))
        //            request.protocol = HTTP10;

        // out buffer fo composed header
        //ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);

        OutputStream bos = this.os;

        request.protocol.writeTo(bos);
        bos.write(' ');
        ByteUtil.writeInt(status, bos);
        bos.write(' ');
        ByteUtil.writeString(statusText, bos);

        bos.write(SERVER);
        ByteUtil.writeString(HttpProtocol.getHeaderVersion(), bos);

        bos.write(DATE);
        //ByteUtil.writeString(DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss_GMT(System.currentTimeMillis()), bos);
        bos.write(DateFormat.ba_EEE__dd_MMM_yyyy_HH_mm_ss_GMT(System.currentTimeMillis()));

        if (contentLength >= 0) {
            bos.write(CONTENTLENGTH);
            ByteUtil.writeLong(contentLength, bos);
        }
        // write content header
        if (outLocale != null) {
            ByteUtil.writeString("\r\nContent-Language: " + outLocale.getLanguage() + "-" + outLocale.getCountry(), bos);
        }

        // session Cookie
        if (request.sid != null) {
            bos.write(SETCOOKIE_JSESSION);
            ByteUtil.writeString(request.sid, bos);
            bos.write(COOKIEPATH);
            String cp = request.getContextPath();
//            if (!cp.endsWith("/"))
//                cp += "/";
            ByteUtil.writeString(cp, bos);
            if (request.isSecure()) {
                bos.write(COOKIESECURE);
            } else {
                bos.write(COOKIEEND);
            }
        }

        // write outCookies
        if (outCookies != null)
            for (Iterator<Cookie> e = outCookies.values().iterator(); e.hasNext();) {
                javax.servlet.http.Cookie c = e.next();

                String con = c.getName();
                if ("JSESSIONID".equals(con))
                    continue;

                StringBuffer sb = new StringBuffer(con);
                sb.append("=");
                sb.append(c.getValue());

                String s;
                s = c.getComment();
                if (s != null)
                    sb.append("; comment=").append(s);
                s = c.getDomain();
                if (s != null)
                    sb.append("; domain=").append(s);
                s = c.getPath();
                if (s != null)
                    sb.append("; path=").append(s);
                long ma = c.getMaxAge();
                if (ma > 0) {
                    //        sb.append("; Max-Age=").append(ma);
                    sb.append("; expires=").append(
                            DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss_GMT(System.currentTimeMillis() + ma * 1000L));
                }
                int v = c.getVersion();
                if (v > 0)
                    sb.append("; version=").append(v);
                if (c.getSecure())
                    sb.append("; secure");

                //      bos.write(CRLF);
                bos.write(SETCOOKIE);
                ByteUtil.writeString(sb.toString(), bos);
            }

        // write outHeaders
        if (outHeaders != null)
            for (Iterator<Entry<String, String>> i = outHeaders.entrySet().iterator(); i.hasNext();) {
                Entry<String, String> e = i.next();
                bos.write(CRLF);
                ByteUtil.writeString(e.getKey(), bos);
                bos.write(COLONSPACE);
                ByteUtil.writeString(e.getValue(), bos);
            }

        bos.write(CRLFCRLF);

        //    if (DEBUG)
        //      System.out.print(bos.toString());

        // bos.writeTo(os);

        //    if (hp.method.equals(HEAD)) os.close();
    }

    public void reset() // from javax.servlet.ServletResponse
    {
        if (includeCount > 0)
            return;

        if (sos != null)
            sos.reset();

        setStatus(200);
        outCookies = null;
        outHeaders.clear();

        contentLength = -1;
        if ("HEAD".equals(request.method))
            contentLength = 0;

        charEncoding = "ISO-8859-1";
        outLocale = null;
    }

    /**
     * Returns the name of the character encoding used in the body of this request. This method returns null if the
     * request does not specify a character encoding
     * 
     * @return a String containing the name of the chararacter encoding, or null if the request does not specify a
     *         character encoding
     */
    public java.lang.String getCharacterEncoding() {
        return charEncoding;
    }

    public void sendError(int err) throws IOException// from javax.servlet.http.HttpServletResponse
    {
        if (headerWritten) {
            throw new IllegalStateException();
        }
        //    if (err < 400)
        //      err = 410;
        setStatus(err);
        if (includeCount == 0)
            writeHeader();
    }

    public void sendError(int err, java.lang.String txt) throws IOException // from javax.servlet.http.HttpServletResponse
    {
        if (headerWritten)
            throw new IllegalStateException();
        //    if (err < 400)
        //      err = 410;
        setStatus(err, txt);
        if (includeCount == 0)
            writeHeader();
    }

    public void sendRedirect(java.lang.String rdr) throws IOException // from javax.servlet.http.HttpServletResponse
    {
        if (headerWritten)
            throw new IllegalStateException();

        if (rdr.indexOf("://") == -1)
            rdr = encodeRedirectURL(rdr);
        outHeaders.put("Location", rdr);

        status = 302;
        setContentLength(0);
        if (includeCount == 0) {
            statusText = "redirecting to: " + rdr;
            writeHeader();
        }
    }

    public void setBufferSize(int sz) // from javax.servlet.ServletResponse
    {
        if (sos != null)
            sos.setBufferSize(sz);
        bufferSize = sz;
    }

    public void flushBuffer() throws IOException // from javax.servlet.ServletResponse
    {
        if (sos != null)
            sos.flush();
    }

    public int getBufferSize() // from javax.servlet.ServletResponse
    {
        return bufferSize;
    }

    public javax.servlet.ServletOutputStream getOutputStream() // from javax.servlet.ServletResponse
    {
        if (pw != null)
            throw new IllegalStateException();
        if (sos != null)
            return sos;
        return sos = new SOStream(this, os, bufferSize, request.httpProto.outBuffer, request.protocol.equals(HttpProtocol.HTTP11));
    }

    public java.io.PrintWriter getWriter() throws UnsupportedEncodingException // from javax.servlet.ServletResponse
    {
        if (pw != null)
            return pw;
        if (sos != null)
            throw new IllegalStateException();

        return pw = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
    }

    public boolean isCommitted() // from javax.servlet.ServletResponse
    {
        if (sos == null)
            return false;
        return sos.isCommitted();
    }

    public void resetBuffer() // from javax.servlet.ServletResponse
    {
        if (sos != null)
            sos.reset();
    }

    public void setContentLength(int len) // from javax.servlet.ServletResponse
    {
        if (includeCount > 0)
            return;
        contentLength = len;
    }

    public void setContentLength(long len)
    {
        if (includeCount > 0)
            return;
        contentLength = len;
    }

    /**
     * set the content-type
     */
    public void setContentType(java.lang.String type) // from javax.servlet.ServletResponse
    {
        if (includeCount > 0)
            return;
        outHeaders.remove("Content-Type");
        int cs = type.toLowerCase().indexOf("charset=");
        if (cs > 0)
            charEncoding = type.substring(cs + 8);
        addHeader("Content-Type", type.trim());
    }

    // locale stuff
    public void setLocale(java.util.Locale loc) // from javax.servlet.ServletResponse
    {
        if (includeCount > 0)
            return;
        outLocale = loc;
    }

    /**
     * Returns the preferred Locale that the client will accept content in, based on the Accept-Language header. If the
     * client request doesn???t provide an Accept-Language header, this method returns the default locale for the
     * server.
     * 
     * @return the preferred Locale for the client
     */
    public java.util.Locale getLocale() {
        if (request.locales.size() > 0)
            return request.locales.get(0);
        return java.util.Locale.US;
    }

    /**
     * add a cookie to the response
     */
    public void addCookie(javax.servlet.http.Cookie ck) // from javax.servlet.http.HttpServletResponse
    {
        if (includeCount > 0)
            return;
        if (outCookies == null)
            outCookies = new HashMap<String, Cookie>();
        if (ck.getMaxAge() == 0) {
            outCookies.remove(ck.getName());
        } else {
            outCookies.put(ck.getName(), ck);
        }
    }

    static javax.servlet.http.Cookie createCookie(ByteRef val) {
        //        if (DEBUG)
        //            System.out.println("cookie: " + val);

        int sp = 0;
        int gp = val.indexOf('=');
        if (gp < 0)
            return null;
        int ep = val.indexOf(';');
        if (ep < 0)
            ep = val.length();

        ByteRef name = val.substring(sp, gp);
        ByteRef value = val.substring(gp + 1, ep);

        if (name == null || value == null)
            return null;

        javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(name.toString(), value.toString());
        // parse optional stuff

        for (;;) {
            sp = ep + 1;
            ep = val.indexOf(';', sp);
            if (ep < 0)
                ep = val.length();
            if (ep <= sp)
                break;
            gp = val.indexOf('=', sp);
            if (gp < 0)
                gp = ep;

            name = val.substring(sp, gp).trim().toUpperCase();
            value = val.substring(gp + 1, ep);

            if (DEBUG)
                System.out.println("cookie: " + name + "=" + value);

            if (name.equals(COMMENT))
                c.setComment(value.toString());
            else if (name.equals(DOMAIN))
                c.setDomain(value.toString());
            else if (name.equals(PATH))
                c.setPath(value.toString());
            else if (name.equals(VERSION)) {
                int v = Integer.parseInt(value.toString());
                c.setVersion(v);
            } else if (name.equals(EXPIRES)) {
                long dt =
                        DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(value.toString()) - System.currentTimeMillis();
                dt /= 1000;
                if (dt < 0)
                    dt = 0;
                if (dt > 0x7fffffffL)
                    dt = 0x7fffffffL;
                c.setMaxAge((int) dt);
            } else if (name.equals(MAXAGE)) {
                int age = Integer.parseInt(value.toString());
                c.setMaxAge(age);
            } else if (name.equals(SECURE))
                c.setSecure(true);

        }
        return c;
    }

    // header handling
    public void addDateHeader(java.lang.String name, long time) // from javax.servlet.http.HttpServletResponse
    {
        addHeader(name, DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss_GMT(time));
    }

    public void addIntHeader(java.lang.String name, int value) // from javax.servlet.http.HttpServletResponse
    {
        addHeader(name, Integer.toString(value));
    }

    public void addHeader(java.lang.String name, java.lang.String value) // from javax.servlet.http.HttpServletResponse
    {
        if (includeCount > 0)
            return;
        String s = outHeaders.get(name);
        if (s != null)
            value = s + "," + value;
        outHeaders.put(name, value);
    }

    public void setDateHeader(java.lang.String name, long time) // from javax.servlet.http.HttpServletResponse
    {
        setHeader(name, DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss_GMT(time));
    }

    public void setHeader(java.lang.String name, java.lang.String value) // from javax.servlet.http.HttpServletResponse
    {
        if (includeCount > 0)
            return;
        outHeaders.put(name, value);
    }

    public void setIntHeader(java.lang.String name, int val) // from javax.servlet.http.HttpServletResponse
    {
        if (includeCount > 0)
            return;
        outHeaders.put(name, Integer.toString(val));
    }

    public boolean containsHeader(java.lang.String name) // from javax.servlet.http.HttpServletResponse
    {
        return outHeaders.get(name) != null;
    }

    public java.lang.String encodeRedirectURL(java.lang.String url) // from javax.servlet.http.HttpServletResponse
    {
        url = encodeURL(url);
        if (url.startsWith("http:") || url.startsWith("https:"))
            return url;

        boolean secure = request.isSecure();
        StringBuffer start = new StringBuffer();
        if (secure)
            start.append("https://");
        else
            start.append("http://");

        start.append(request.host);

        if (!((request.port == 80 && !secure) || (request.port == 443 && secure)))
            start.append(":" + request.port);

        if (!url.startsWith("/")) {
            String cPath = request.getContextPath();
            String sPath = request.getServletPath();
            start.append(cPath);
            if (!cPath.endsWith("/"))
                start.append("/");
            if (sPath.startsWith("/"))
                start.append(sPath.substring(1));
            else
                start.append(sPath);
            String pi = request.getPathInfo();
            if (pi != null)
                start.append(pi);

            String base = start.toString();
            int slash = base.lastIndexOf('/');
            if (slash < base.length() - 1)
                start = new StringBuffer().append(base.substring(0, slash + 1));
        }

        start.append(url);
        //    System.out.println("Location: " + start);
        return start.toString();
    }

    /**
     * @deprecated
     */
    public java.lang.String encodeRedirectUrl(java.lang.String url) // from javax.servlet.http.HttpServletResponse
    {
        return encodeRedirectURL(url);
    }

    public java.lang.String encodeURL(java.lang.String url) // from javax.servlet.http.HttpServletResponse
    {
        if (request.fromC || request.sid == null)
            return url;

        int kpos = url.indexOf('?');
        if (kpos < 0)
            return url + ";jsessionid=" + request.sid;

        url = url.substring(0, kpos) + ";jsessionid=" + request.sid + url.substring(kpos);
        return url;
    }

    /**
     * @deprecated
     */
    public java.lang.String encodeUrl(java.lang.String url) // from javax.servlet.http.HttpServletResponse
    {
        return encodeURL(url);
    }

    // state stuff
    public void setStatus(int state) // from javax.servlet.http.HttpServletResponse
    {
        if (headerWritten || includeCount > 0)
            return;
        status = state;
        switch (status) {
            case 100:
                statusText = "Continue";
                break;
            case 101:
                statusText = "Switching Protocols";
                break;
            case 200:
                statusText = "OK";
                break;
            case 201:
                statusText = "Created";
                break;
            case 202:
                statusText = "Accepted";
                break;
            case 203:
                statusText = "Non-Authoritative Information";
                break;
            case 204:
                statusText = "No Content";
                break;
            case 205:
                statusText = "Reset Content";
                break;
            case 206:
                statusText = "Partial Content";
                break;
            case 300:
                statusText = "Multiple Choices";
                break;
            case 301:
                statusText = "Moved Permanently";
                break;
            case 302:
                statusText = "Found";
                break;
            case 303:
                statusText = "See Other";
                break;
            case 304:
                statusText = "Not Modified";
                break;
            case 305:
                statusText = "Use Proxy";
                break;
            case 306:
                statusText = "Unused";
                break;
            case 307:
                statusText = "Temporary Redirect";
                break;
            case 400:
                statusText = "Bad Request";
                break;
            case 401:
                statusText = "Unauthorized";
                break;
            case 402:
                statusText = "Payment Required";
                break;
            case 403:
                statusText = "Forbidden";
                break;
            case 404:
                statusText = "Not Found";
                break;
            case 405:
                statusText = "Method Not Allowed";
                break;
            case 406:
                statusText = "Not Acceptable";
                break;
            case 407:
                statusText = "Proxy Authentication Required";
                break;
            case 408:
                statusText = "Request Timeout";
                break;
            case 409:
                statusText = "Conflict";
                break;
            case 410:
                statusText = "Gone";
                break;
            case 411:
                statusText = "Length Required";
                break;
            case 412:
                statusText = "Precondition Failed";
                break;
            case 413:
                statusText = "Request Entity Too Large";
                break;
            case 414:
                statusText = "Request-URI Too Long";
                break;
            case 415:
                statusText = "Unsupported Media Type";
                break;
            case 416:
                statusText = "Requested Range Not Satisfiable";
                break;
            case 417:
                statusText = "Expectation Failed";
                break;
            case 500:
                statusText = "Internal Server Error";
                break;
            case 501:
                statusText = "Not Implemented";
                break;
            case 502:
                statusText = "Bad Gateway";
                break;
            case 503:
                statusText = "Service Unavailable";
                break;
            case 504:
                statusText = "Gateway Timeout";
                break;
            case 505:
                statusText = "HTTP Version Not Supported";
                break;
            default:
                statusText = ".?.";
                break;
        }
    }

    /**
     * @deprecated
     */
    public void setStatus(int state, java.lang.String txt) // from javax.servlet.http.HttpServletResponse
    {
        if (includeCount > 0)
            return;
        status = state;
        statusText = txt;
    }

    public String getContentType() {
        return outHeaders.get("Content-Type");
    }

    public void setCharacterEncoding(String arg0) {
        if (includeCount > 0)
            return;

        charEncoding = arg0;

        String type = outHeaders.remove("Content-Type");
        if (type == null)
            type = "text/html";
        int cs = type.toLowerCase().indexOf("charset=");
        if (cs > 0)
            type = type.substring(0, cs);
        if (!type.endsWith(";"))
            type += ";";

        type += "charset=" + charEncoding;

        setHeader("Content-Type", type.trim());
    }

    // since 3.0 / 3.1
    
    public void setContentLengthLong(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getHeader(String key) {
        return outHeaders.get(key);
    }

    public Collection<String> getHeaderNames() {
        return outHeaders.keySet();
    }

    public Collection<String> getHeaders(String key) {
        final ArrayList<String> al = new ArrayList<String>();
        final String s = outHeaders.get(key);
        if (s != null)
            al.add(s);
        return al;
    }

    public int getStatus() {
        return status;
    }

}

/******************************************************************************
 * $Log: HttpResponse.java,v $
 * Revision 1.33  2014/10/21 20:34:45  bebbo
 * @R JSESSIONID cookie is now "httponly" and if SSL is used also "secure"
 *
 * Revision 1.32  2013/11/23 10:36:19  bebbo
 * @R contentLength supports long values now
 *
 * Revision 1.31  2013/05/17 10:49:54  bebbo
 * @I smaller buffer size due to chunked support
 * Revision 1.30 2012/12/21 08:14:54 bebbo
 * 
 * @R keep connections if contentLength is 0 Revision 1.29 2012/07/18 06:44:53 bebbo
 * 
 * @I typified Revision 1.28 2011/09/16 16:19:33 bebbo
 * 
 * @D disabled DEBUG Revision 1.27 2011/06/12 17:46:42 bebbo
 * 
 * @D removed some debug stuff Revision 1.26 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.25 2010/04/10 12:11:07 bebbo
 * @B no longer adds duplicate cookies - existing cookies are overridden
 * 
 *    Revision 1.24 2010/03/17 10:15:25 bebbo
 * @R duplicate cookies (with same name) are no longer allowed: last one wins.
 * 
 *    Revision 1.23 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.22 2009/06/15 08:36:26 bebbo
 * @B B-REMOTEHOST is used correctly
 * @I using BufferedStreams where convenient
 * 
 *    Revision 1.21 2008/03/13 17:24:34 bebbo
 * @R added some text message if status code <> 200
 * 
 *    Revision 1.20 2008/01/17 17:32:52 bebbo
 * @O optimizations for better performance
 * 
 *    Revision 1.19 2007/05/01 19:05:27 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.18 2007/04/13 17:56:03 bebbo
 * @B fixed setCharacterEncoding() to set also the charset
 * 
 *    Revision 1.17 2007/01/18 21:48:18 bebbo
 * @I reformatted
 * 
 *    Revision 1.16 2006/05/09 12:13:25 bebbo
 * @R changes to comply to servlet2_4
 * 
 *    Revision 1.15 2006/02/06 09:15:51 bebbo
 * @I cleanup
 * 
 *    Revision 1.14 2005/11/18 14:47:10 bebbo
 * @B fixed encodeRedirectURL() if url starts with http:// or https:// it is unmodified
 * 
 *    Revision 1.13 2004/12/16 15:59:45 bebbo
 * @I changed upper/lowercas of last-modified
 * 
 *    Revision 1.12 2004/12/13 15:33:26 bebbo
 * @B fixed broken redirect URL
 * 
 *    Revision 1.11 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.10 2004/04/07 16:30:17 bebbo
 * @B fixed character encoding
 * 
 *    Revision 1.9 2004/03/24 09:43:08 bebbo
 * @I name changes
 * 
 *    Revision 1.8 2004/03/23 11:17:06 bebbo
 * @B content-type sets also charEncoding
 * 
 *    Revision 1.7 2003/11/26 09:57:09 bebbo
 * @B also sending cookie path for JSESSIONID
 * 
 *    Revision 1.6 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.5 2003/04/07 07:58:44 bebbo
 * @R sendError(err) with err < 400 results to 410 now
 * 
 *    Revision 1.4 2003/03/21 10:13:18 bebbo
 * @B absolute redirects are passed through unmodified
 * 
 *    Revision 1.3 2003/02/02 15:41:52 bebbo
 * @R now ommitting port, if possible
 * 
 *    Revision 1.2 2003/01/07 18:32:20 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.1 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.6 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.5 2002/11/06 18:20:45 bebbo
 * @B fixed double emit of JSESSIONID cookie
 * 
 *    Revision 1.4 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.3 2002/07/26 18:08:45 bebbo
 * @B fixed redirection, if port != 80 port number was ommitted.
 * 
 *    Revision 1.2 2002/05/19 18:12:45 bebbo
 * @B fixed sendRedirect together with https
 * 
 *    Revision 1.1 2002/04/02 13:02:36 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *****************************************************************************/
