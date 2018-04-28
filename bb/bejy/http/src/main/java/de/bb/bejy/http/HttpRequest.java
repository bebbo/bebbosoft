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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.HttpUtils;
import javax.servlet.http.Part;

import de.bb.io.FastByteArrayInputStream;
import de.bb.io.IOUtils;
import de.bb.util.ByteRef;
import de.bb.util.DateFormat;
import de.bb.util.MimeFile;
import de.bb.util.SessionManager;

/**
 * This class is designed to ...
 * 
 * @author bebbo
 */
public class HttpRequest extends HttpRequestBase implements javax.servlet.http.HttpServletRequest {

    private final static Enumeration<String> NADA = new Vector<String>().elements();

    HttpProtocol httpProto;

    private HashMap<String, Part> parts;

    private File partsFile;

    private byte[] partsData;

    HttpRequest(HttpProtocol proto, int port) {
        super(proto, port);
        this.httpProto = proto;
    }

    private final static boolean DEBUG = HttpProtocol.DEBUG;

    //========================================================================
    // interfaced functions
    //========================================================================
    /**
     * Returns the name of the authentication scheme.
     * 
     * @return The name of the authentication scheme.
     */

    public String getAuthType() {
        if (authType == null) {
            return null;
        }
        return authType.toString();
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

    /**
     * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT. Same as the
     * value of the CGI variable REQUEST_METHOD.
     * 
     * @return a <code>String</code> specifying the name of the method with which this request was made
     */
    /**
     * Returns the name and version of the protocol the request uses in the form protocol/majorVersion.minor-Version,
     * for example, HTTP/1.1. For HTTP servlets, the value returned is the same as the value of the CGI variable
     * SERVER_PROTOCOL.
     * 
     * @return a String containing the protocol name and version number
     */

    public java.lang.String getProtocol() {
        return protocol.toString();
    }

    public String getMethod() {
        return method;
    }

    /**
     * Returns the Internet Protocol (IP) address of the client that sent the request. For HTTP servlets, same as the
     * value of the CGI variable REMOTE_ADDR.
     * 
     * @returns a String containing the IP address of the client that sent the request
     */

    public java.lang.String getRemoteAddr() {
        return httpProto.getRemoteAddress();
    }

    /**
     * Returns the fully qualified name of the client that sent the request, or the IP address of the client if the name
     * cannot be determined. For HTTP servlets, same as the value of the CGI variable REMOTE_HOST.
     * 
     * @return a String containing the fully qualified name of the client
     */

    public java.lang.String getRemoteHost() {
        return httpProto.getRemoteAddress();
    }

    /**
     * Returns a RequestDispatcher object that acts as a wrapper for the resource located at the given path. A
     * RequestDispatcher object can be used to forward a request to the resource or to include the resource in a
     * response. The resource can be dynamic or static. The pathname specified may be relative, although it cannot
     * extend outside the current servlet handler. If the path begins with a ???/??? it is interpreted as relative to
     * the current handler root. This method returns null if the servlet container cannot return a RequestDispatcher.
     * The difference between this method and getRequestDispatcher(String) is that this method can take a relative path.
     * 
     * @param pagePath
     *            - a String specifying the pathname to the resource
     * @return a RequestDispatcher object that acts as a wrapper for the resource at the specified path
     * @see RequestDispatcher, getRequestDispatcher(String)
     */

    public javax.servlet.RequestDispatcher getRequestDispatcher(java.lang.String pagePath) {
        if (pagePath.indexOf('/') != 0) {
            String sp = getServletPath();
            int i = sp.lastIndexOf('/');
            if (i >= 0) {
                pagePath = sp.substring(0, i + 1) + pagePath;
            }
        }

        return context.getRequestDispatcher(pagePath);
    }

    /**
     * Returns the name of the scheme used to make this request, for example, http, https,orftp. Different schemes have
     * different rules for constructing URLs, as noted in RFC 1738.
     * 
     * @return a String containing the name of the scheme used to make this request
     */

    public java.lang.String getScheme() {
        return isSecure() ? "https" : "http";
    }

    /**
     * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
     * 
     * @return a boolean indicating if the request was made using a secure channel
     */

    public boolean isSecure() {
        return isSecure || httpProto.isSecure();
    }

    /**
     * 
     * Returns the query string that is contained in the request URL after the path. This method returns
     * <code>null</code> if the URL does not have a query string. Same as the value of the CGI variable QUERY_STRING.
     * 
     * @return a <code>String</code> containing the query string or <code>null</code> if the URL contains no query
     *         string. The value is not decoded by the container.
     * 
     */

    public String getQueryString() {
        return queryString;
    }

    /**
     * 
     * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if
     * the user has not been authenticated. Whether the user name is sent with each subsequent request depends on the
     * browser and type of authentication. Same as the value of the CGI variable REMOTE_USER.
     * 
     * @return a <code>String</code> specifying the login of the user making this request, or <code>null</code if the
     *         user login is not known
     * 
     */

    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified logical "role". Roles
     * and role membership can be defined using deployment descriptors. If the user has not been authenticated, the
     * method returns <code>false</code>.
     * 
     * @param role
     *            a <code>String</code> specifying the name of the role
     * 
     * @return a <code>boolean</code> indicating whether the user making this request belongs to a given role;
     *         <code>false</code> if the user has not been authenticated
     */

    public boolean isUserInRole(final String role) {
        javax.servlet.http.HttpSession session = getSession(false);
        if (session == null)
            return false;

        final HashSet<String> roles = (HashSet<String>) session.getAttribute("j_user_roles");
        if (roles == null)
            return false;
        return roles.contains(role);
    }

    /**
     * Returns a <code>java.security.Principal</code> object containing the name of the current authenticated user. If
     * the user has not been authenticated, the method returns <code>null</code>.
     * 
     * @return a <code>java.security.Principal</code> containing the name of the user making this request;
     *         <code>null</code> if the user has not been authenticated
     */

    public java.security.Principal getUserPrincipal() {
        return new Principal() {
            public String getName() {
                return remoteUser != null ? remoteUser : "<anonymous>";
            }
        };
    }

    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the
     * HTTP request. The web container does not decode this String. For example:
     * 
     * <blockquote> <table> <tr align=left> <th>First line of HTTP request <th> <th>Returned Value <tr> <td>POST
     * /some/path.html HTTP/1.1 <td> <td>/some/path.html <tr> <td>GET http://foo.bar/a.html HTTP/1.0 <td> <td>/a.html
     * <tr> <td>HEAD /xyz?a=b HTTP/1.1 <td> <td>/xyz </table> </blockquote>
     * 
     * <p> To reconstruct an URL with a scheme and host, use {@link HttpUtils#getRequestURL}.
     * 
     * @return a <code>String</code> containing the part of the URL from the protocol name up to the query string
     * 
     * @see HttpUtils#getRequestURL
     */

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

    /**
     * 
     * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
     * number, and server path, but it does not include query string parameters.
     * 
     * <p> Because this method returns a <code>StringBuffer</code>, not a string, you can modify the URL easily, for
     * example, to append query parameters.
     * 
     * <p> This method is useful for creating redirect messages and for reporting errors.
     * 
     * @return a <code>StringBuffer</code> object containing the reconstructed URL
     * 
     */

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

    /**
     * Retrieves the body of the request as character data using a BufferedReader. The reader translates the character
     * data according to the character encoding used on the body. Either this method or getReader() may be called to
     * read the body, not both.
     * 
     * @return a BufferedReader containing the body of the request
     * @exception UnsupportedEncodingException
     *                - if the character set encoding used is not supported and the text cannot be decoded
     * @exception IllegalStateException
     *                - if getInputStream() method has been called on this request
     * @exception IOException
     *                - if an input or output exception occurred
     * @seegetInputStream()
     */

    public java.io.BufferedReader getReader() throws IOException {
        if (ir != null) {
            return ir;
        }
        if (sis != null) {
            throw new IllegalStateException();
        }

        InputStreamReader isr = charEncoding == null ? new InputStreamReader(getInputStream()) : new InputStreamReader(
                getInputStream(), charEncoding);
        return ir = new BufferedReader(isr);
    }

    /**
     * Overrides the name of the character encoding used in the body of this request. This method must be called prior
     * to reading request parameters or reading input using getReader().
     * 
     * @param env
     *            - a String containing the name of the chararacter encoding.
     * @exception java.io.UnsupportedEncodingException
     *                - if this is not a valid encoding
     */

    public void setCharacterEncoding(java.lang.String enc) throws java.io.UnsupportedEncodingException {
        new String(new byte[] { 0 }, enc);
        charEncoding = enc;
    }

    /**
     * Retrieves the body of the request as binary data using a ServletInputStream . Either this method or getReader()
     * may be called to read the body, not both.
     * 
     * @return a ServletInputStream object containing the body of the request
     * @exception IllegalStateException
     *                - if the getReader() method has already been called for this request
     * @exception IOException
     *                - if an input or output exception occurred
     */

    public javax.servlet.ServletInputStream getInputStream() {
        if (ir != null) {
            throw new IllegalStateException();
        }
        if (sis == null) {
            sis = httpProto.createSIS(this);
        }
        return sis;
    }

    /**
     * Returns the value of a request parameter as a String,ornull if the parameter does not exist. Request parameters
     * are extra information sent with the request. For HTTP servlets, parameters are contained in the query string or
     * posted form data. You should only use this method when you are sure the parameter has only one value. If the
     * parameter might have more than one value, use getParameterValues(String) . If you use this method with a
     * multivalued parameter, the value returned is equal to the first value in the array returned by
     * getParameterValues. If the parameter data was sent in the request body, such as occurs with an HTTP POST request,
     * then read-ing the body directly via getInputStream() or getReader() can interfere with the execution of this
     * method.
     * 
     * @param name
     *            - a String specifying the name of the parameter
     * @return a String representing the single value of the parameter
     * @see getParameterValues(String)
     */

    public java.lang.String getParameter(java.lang.String name) {
        checkParams();
        Object o = parameters.get(name);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        try {
            String sa[] = (String[]) o;
            return sa[0];
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Returns a java.util.Map of the parameters of this request. Request parameters are extra information sent with the
     * request. For HTTP servlets, parameters are contained in the query string or posted form data.
     * 
     * @return an immutable java.util.Map containing parameter names as keys and parameter values as map values.
     */

    public java.util.Map getParameterMap() {
        checkParams();
        return parameters;
    }

    /**
     * Returns an Enumeration of String objects containing the names of the parameters contained in this request. If the
     * request has no parameters, the method returns an empty Enumeration.
     * 
     * @return an Enumeration of String objects, each String containing the name of a request parameter; or an empty
     *         Enumeration if the request has no parameters
     */

    public java.util.Enumeration getParameterNames() {
        checkParams();
        return new IterEnum(parameters.keySet().iterator());
    }

    private void checkParams() {
        if (!paramParsed) {
            parameters = parseParams(this);
            paramParsed = true;
        }
    }

    /**
     * Returns an array of String objects containing all of the values the given request parameter has, or null if the
     * parameter does not exist. If the parameter has a single value, the array has a length of 1.
     * 
     * @param name
     *            - a String containing the name of the parameter whose value is requested
     * @return an array of String objects containing the parameter???s values
     * @see getParameter(String)
     */

    public java.lang.String[] getParameterValues(java.lang.String name) {
        checkParams();
        Object ret = parameters.get(name);
        if (ret == null) {
            return null;
        }
        return (ret instanceof String[]) ? (String[]) ret : new String[] { ret.toString() };
    }

    HashMap<String, String[]> parseParams(HttpServletRequest request) {
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        if ("POST".equals(request.getMethod())) {
            String cType = request.getHeader("CONTENT-TYPE");
            if (cType != null) {
                String ce = charEncoding;
                int semi = cType.indexOf(';');
                if (semi > 0) {
                    String rest = cType.substring(semi + 1).trim();
                    cType = cType.substring(0, semi).trim();
                    if (rest.toUpperCase().startsWith("CHARSET=")) {
                        ce = rest.substring(8);
                    }
                }
                if (cType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                    int len = request.getContentLength();

                    byte b[] = new byte[len];
                    try {
                        InputStream is = request.getInputStream();
                        int pos = 0;
                        while (pos < len) {
                            int rlen = is.read(b, pos, len - pos);
                            pos += rlen;
                        }
                    } catch (Exception e) {
                    }
                    extractParameters(parameters, new ByteRef(b), ce);
                } else if (cType.toLowerCase().startsWith("multipart/form-data;")) {
                }
            }
        }

        String queryString = request.getQueryString();
        if (queryString != null) {
            extractParameters(parameters, new ByteRef(queryString), null);
        }

        return parameters;
    }

    static void extractParameters(HashMap<String, String[]> parameters, ByteRef q, String charEncoding) {
        int lp = 0;
        int gp, sp;
        if (DEBUG) {
            System.out.println(q);
        }
        for (;;) {
            sp = lp;
            lp = q.indexOf('&', sp);
            if (lp < 0) {
                break;
            }
            gp = q.indexOf('=', sp);
            ByteRef n = unescape(q.substring(sp, gp));
            ByteRef v = unescape(q.substring(gp + 1, lp));
            ++lp;
            if (DEBUG) {
                System.out.println(n.toString(charEncoding) + " = " + v.toString(charEncoding));
            }
            putParam(parameters, n.toString(charEncoding), v.toString(charEncoding));
        }
        gp = q.indexOf('=', sp);
        ByteRef n = unescape(q.substring(sp, gp));
        ByteRef v = unescape(q.substring(gp + 1));
        if (DEBUG) {
            System.out.println(n.toString(charEncoding) + " = " + v.toString(charEncoding));
        }
        if (n.length() > 0) {
            putParam(parameters, n.toString(charEncoding), v.toString(charEncoding));
        }
    }

    private static void putParam(HashMap<String, String[]> parameters, String sn, String val) {
        String sa[] = parameters.get(sn);
        String san[] = new String[sa == null ? 1 : sa.length + 1];
        if (sa != null) {
            System.arraycopy(sa, 0, san, 0, san.length - 1);
        }
        san[san.length - 1] = val;
        parameters.put(sn, san);
    }

    // could be faster...
    private static ByteRef unescape(ByteRef b) {
        // replace + by space
        for (int i = b.indexOf('+'); i >= 0; i = b.indexOf('+', i + 1)) {
            b = b.substring(0, i).append(" ").append(b.substring(i + 1));
        }

        // decode %xx
        for (int i = b.indexOf('%'); i >= 0; i = b.indexOf('%', i + 1)) {
            if (i + 3 > b.length()) {
                break;
            }
            byte[] c = new byte[] { (byte) Integer.parseInt(b.substring(i + 1, i + 3).toString(), 16) };
            b = b.substring(0, i).append(new ByteRef(c)).append(b.substring(i + 3));
        }

        //    System.out.println(o.toString() + " => " + b.toString());
        return b;
    }

    /**
     * Returns the preferred Locale that the client will accept content in, based on the Accept-Language header. If the
     * client request doesn't provide an Accept-Language header, this method returns the default locale for the server.
     * 
     * @return the preferred Locale for the client
     */

    public java.util.Locale getLocale() {
        if (locales.size() > 0) {
            return locales.get(0);
        }
        return java.util.Locale.getDefault();
    }

    /**
     * Returns an Enumeration of Locale objects indicating, in decreasing order starting with the preferred locale, the
     * locales that are acceptable to the client based on the Accept-Language header. If the client request doesn???t
     * provide an Accept-Language header, this method returns an Enumeration containing one Locale, the default locale
     * for the server.
     * 
     * @return an Enumeration of preferred Locale objects for the client
     */

    public java.util.Enumeration<Locale> getLocales() {
        return new IterEnum<Locale>(locales.iterator());
    }

    /**
     * Returns an array containing all inCookies.
     * 
     * @return An array containing all inCookies.
     */

    public javax.servlet.http.Cookie[] getCookies() {
        if (inCookies.size() == 0) {
            return null;
        }
        Cookie c[] = new Cookie[inCookies.size()];
        c = inCookies.toArray(c);
        return c;
    }

    /**
     * Returns the length, in bytes, of the request body and made available by the input stream, or -1 if the length is
     * not known. For HTTP servlets, same as the value of the CGI variable CONTENT_LENGTH.
     * 
     * @return an integer containing the length of the request body or -1 if the length is not known
     */

    public int getContentLength() {
        return (int) inContentLength;
    }

    /**
     * Returns the MIME type of the body of the request, or null if the type is not known. For HTTP servlets, same as
     * the value of the CGI variable CONTENT_TYPE.
     * 
     * @return a String containing the name of the MIME type of the request, or -1 if the type is not known
     */

    public java.lang.String getContentType() {
        if (contentType == null) {
            String s = getHeader("CONTENT-TYPE");
            if (s == null) {
                return null;
            }
            contentType = new ByteRef(s);
        }

        return contentType.toString();
    }

    /**
     * Returns the value of the specified request header as the long value for the date
     */

    public long getDateHeader(String name) {
        String val = getHeader(name);
        if (val == null) {
            return -1;
        }
        int kp = val.indexOf(';');
        if (kp >= 0) {
            val = val.substring(0, kp);
        }
        long r = DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(val);
        if (r == -1) {
            throw new IllegalArgumentException();
        }
        return r;
    }

    /**
     * 
     * Returns all the values of the specified request header as an <code>Enumeration</code> of <code>String</code>
     * objects.
     * 
     * <p> Some inHeaders, such as <code>Accept-Language</code> can be sent by clients as several inHeaders each with a
     * different value rather than sending the header as a comma separated list.
     * 
     * <p> If the request did not include any inHeaders of the specified name, this method returns an empty
     * <code>Enumeration</code>. The header name is case insensitive. You can use this method with any request header.
     * 
     * @param name
     *            a <code>String</code> specifying the header name
     * 
     * @return an <code>Enumeration</code> containing the values of the requested header. If the request does not have
     *         any inHeaders of that name return an empty enumeration. If the container does not allow access to header
     *         information, return null
     * 
     */

    public Enumeration<String> getHeaders(String name) {
        String s = (String) inHeaders.get(name.toUpperCase());
        if (s == null) {
            return NADA;
        }

        Enumeration st = new StringTokenizer(s, ", ");
        return st;
    }

    /**
     * 
     * Returns an enumeration of all the header names this request contains. If the request has no inHeaders, this
     * method returns an empty enumeration.
     * 
     * <p> Some servlet containers do not allow servlets to access headers using this method, in which case this method
     * returns <code>null</code>
     * 
     * @return an enumeration of all the header names sent with this request; if the request has no inHeaders, an empty
     *         enumeration; if the servlet container does not allow servlets to use this method, <code>null</code>
     */

    public Enumeration<String> getHeaderNames() {
        return new IterEnum<String>(inHeaderNames.iterator());
    }

    /**
     * 
     * Returns the value of the specified request header as an <code>int</code>. If the request does not have a header
     * of the specified name, this method returns -1. If the header cannot be converted to an integer, this method
     * throws a <code>NumberFormatException</code>.
     * 
     * <p> The header name is case insensitive.
     * 
     * @param name
     *            a <code>String</code> specifying the name of a request header
     * 
     * @return an integer expressing the value of the request header or -1 if the request doesn't have a header of this
     *         name
     * 
     * @exception NumberFormatException
     *                If the header value can't be converted to an <code>int</code>
     */

    public int getIntHeader(String name) {
        String val = getHeader(name);
        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    /**
     * Returns the host name of the server that received the request. For HTTP servlets, same as the value of the CGI
     * variable SERVER_NAME.
     * 
     * @return a String containing the name of the server to which the request was sent
     */

    public java.lang.String getServerName() {
        return host.toString();
    }

    /**
     * Returns the port number on which this request was received. For HTTP servlets, same as the value of the CGI
     * variable SERVER_PORT.
     * 
     * @return an integer specifying the port number
     */

    public int getServerPort() {
        return port;
    }

    // path stuff example is
    // a) http://www.test.com/here/there/doit.jsp?bla=fasel
    // b) https://www.test.com:444/here/there/doit.jsp/not/there/bla.dat?bla=fasel

    /**
     * returns the path to the servlet (file) a) /here/there/doit.jsp b) /here/there/doit.jsp
     */

    public String getServletPath() {
        String servletPath = reqDisp.servletPath;
        if (servletPath != null) {
            return servletPath;
        }
        return path.substring(context.sContext.length()).toString("utf-8");
    }

    /**
     * returns extra path info, if any or same as servlet path a) /here/there/doit.jsp b) /not/there/bla.dat
     */

    public String getPathInfo() {
        return reqDisp.pathInfo;
    }

    /**
     * returns the current path, but mapped (if possible)
     */

    public String getPathTranslated() {
        String pi = getPathInfo();
        if (pi == null) {
            return null;
        }

        return context.getRealPath(pi);
    }

    /**
     * maps the path to the real path.
     * 
     * @deprecated
     */
    @Deprecated
    public java.lang.String getRealPath(java.lang.String path) {
        return context.getRealPath(path);
    }

    public String getContextPath() {
        if (context == null) {
            return "";
        }
        String ret = context.sContext;
        return ret;
    }

    // functions passed to handler

    public java.lang.Object getAttribute(java.lang.String name) {
        return attributes.get(name);
    }

    public java.util.Enumeration<String> getAttributeNames() {
        return new IterEnum<String>(attributes.keySet().iterator());
    }

    public void removeAttribute(java.lang.String name) {
        Object old = attributes.remove(name);
        if (context.sralv != null) {
            javax.servlet.ServletRequestAttributeEvent srae = new javax.servlet.ServletRequestAttributeEvent(context,
                    this, name, old);

            for (Iterator<ServletRequestAttributeListener> e = context.sralv.iterator(); e.hasNext();) {
                javax.servlet.ServletRequestAttributeListener sral = e.next();
                sral.attributeRemoved(srae);
            }
        }
    }

    public void setAttribute(java.lang.String name, java.lang.Object o) {
        Object old = attributes.put(name, o);
        if (context.sralv != null) {
            javax.servlet.ServletRequestAttributeEvent srae = new javax.servlet.ServletRequestAttributeEvent(context,
                    this, name, o);

            for (Iterator<ServletRequestAttributeListener> e = context.sralv.iterator(); e.hasNext();) {
                javax.servlet.ServletRequestAttributeListener sral = e.next();
                if (old == null) {
                    sral.attributeAdded(srae);
                } else {
                    sral.attributeReplaced(srae);
                }
            }
        }
    }

    /**
     * Returns the session ID specified by the client. This may not be the same as the ID of the actual session in use.
     * For example, if the request specified an old (expired) session ID and the server has started a new session, this
     * method gets a new session with a new ID. If the request did not specify a session ID, this method returns
     * <code>null</code>.
     * 
     * @return a <code>String</code> specifying the session ID, or <code>null</code> if the request did not specify a
     *         session ID
     * 
     * @see #isRequestedSessionIdValid
     */

    public String getRequestedSessionId() {
        return sid;
    }

    /**
     * 
     * Returns the current <code>HttpSession</code> associated with this request or, if if there is no current session
     * and <code>create</code> is true, returns a new session.
     * 
     * <p> If <code>create</code> is <code>false</code> and the request has no valid <code>HttpSession</code>, this
     * method returns <code>null</code>.
     * 
     * <p> To make sure the session is properly maintained, you must call this method before the response is committed.
     * 
     * @param <code>true</code> to create a new session for this request if necessary; <code>false</code> to return
     *        <code>null</code> if there's no current session
     * 
     * 
     * @return the <code>HttpSession</code> associated with this request or <code>null</code> if <code>create</code> is
     *         <code>false</code> and the request has no valid session
     * 
     * @see #getSession()
     */

    public javax.servlet.http.HttpSession getSession(boolean create) {
        HttpSession hs = (HttpSession) context.sessionManager.get(sid);
        if (hs != null) {
            return hs;
        }

        sid = null;

        if (!create) {
            return null;
        }

        // make new Session
        hs = new HttpSession(context);
        sid = hs.sessionId;
        return hs;
    }

    /**
     * 
     * Returns the current session associated with this request, or if the request does not have a session, creates one.
     * 
     * @return the <code>HttpSession</code> associated with this request
     * 
     * @see #getSession(boolean)
     * 
     */

    public javax.servlet.http.HttpSession getSession() {
        return getSession(true);
    }

    /**
     * 
     * Checks whether the requested session ID is still valid.
     * 
     * @return <code>true</code> if this request has an id for a valid session in the current session handler;
     *         <code>false</code> otherwise
     * 
     * @see #getRequestedSessionId
     * @see #getSession
     * @see HttpSessionContext
     * 
     */

    public boolean isRequestedSessionIdValid() {
        return context.sessionManager.get(sid) != null;
    }

    /**
     * 
     * Checks whether the requested session ID came in as a cookie.
     * 
     * @return <code>true</code> if the session ID came in as a cookie; otherwise, <code>false</code>
     * 
     * 
     * @see #getSession
     * 
     */

    public boolean isRequestedSessionIdFromCookie() {
        return fromC;
    }

    public ServletContext getServletContext() {
        return context;
    }

    /**
     * 
     * Checks whether the requested session ID came in as part of the request URL.
     * 
     * @return <code>true</code> if the session ID came in as part of a URL; otherwise, <code>false</code>
     * 
     * 
     * @see #getSession
     * 
     */

    public boolean isRequestedSessionIdFromURL() {
        return fromU;
    }

    /**
     * 
     * @deprecated As of Version 2.1 of the Java Servlet API, use {@link #isRequestedSessionIdFromURL} instead.
     * 
     */

    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return fromU;
    }

    /**
     * Method handle.
     * 
     * @param response
     */
    public void handle(Host hs, HttpResponse response) {
        String sPath = path.toString();
        if (hs != null) {
            context = hs.getContext(sPath);
        }

        if (DEBUG) {
            System.out.println("context = " + context.getName());
        }

        if (context == null) {
            response.setStatus(404);
            return;
        }

        for (Cookie c : inCookies) {
            if ("JSESSIONID".equals(c.getName()) && context.sessionManager.containsKey(c.getValue())) {
                sid = c.getValue();
                if (DEBUG) {
                    System.out.println("got jsessionid: " + sid);
                }
                fromC = true;
                break;
            }
        }
        
        
        String localPath = context.localPath(sPath);
        if (DEBUG) {
            System.out.println("localPath = " + localPath);
        }
        reqDisp = (RequestDispatcher) context.getRequestDispatcher(localPath);

        //        try {
        //            if (context.verifyUser(getMethod(), localPath, remoteUser, remotePass)) {
        //                reqDisp = (RequestDispatcher) context.getRequestDispatcher(localPath);
        //            } else {
        //                reqDisp = new RequestDispatcher(context, sPath, null, null, new H401Handler(context));
        //            }
        //        } catch (Exception e1) {
        //            if (DEBUG) {
        //                System.out.println("--> 403");
        //            }
        //            response.setStatus(403);
        //            return;
        //        }

        if (reqDisp == null) {
            if (DEBUG) {
                System.out.println("--> 404");
            }
            response.setStatus(404);
            return;
        }

        if (sid != null) {
            HttpSession sess = (HttpSession) getSession(false);
            if (DEBUG) {
                System.out.println("lookup session: " + sess);
            }
            if (sess != null) {
                sess.touch();
            }
        }

        response.setStatus(200);
        try {
            javax.servlet.ServletRequestEvent srae = null;
            Thread curThread = null;
            ClassLoader oldCl = null;
            try {
                if (context.zcl != null) {
                    curThread = Thread.currentThread();
                    oldCl = curThread.getContextClassLoader();
                    curThread.setContextClassLoader(context.zcl);
                }

                if (context.srlv != null) {
                    srae = new javax.servlet.ServletRequestEvent(context, this);
                    for (Iterator<ServletRequestListener> e = context.srlv.iterator(); e.hasNext();) {
                        javax.servlet.ServletRequestListener sral = e.next();
                        sral.requestInitialized(srae);
                    }
                }
                if (DEBUG) {
                    System.out.println("handle: " + reqDisp);
                }
                reqDisp.handle(this, response);

            } finally {
                if (srae != null) {
                    for (Iterator<ServletRequestListener> e = context.srlv.iterator(); e.hasNext();) {
                        javax.servlet.ServletRequestListener sral = e.next();
                        sral.requestDestroyed(srae);
                    }
                }
                if (oldCl != null && curThread != null) {
                    curThread.setContextClassLoader(oldCl);
                }
            }
        } catch (SocketException se) {
            // ignore - socket was closed.
        } catch (Exception e) {
            System.out.println("something went wrong: ");
            e.printStackTrace();
            response.setStatus(500);
        }

    }

    public int getLocalPort() {
        return getServerPort();
    }

    public int getRemotePort() {
        return 0;
    }

    public String getLocalAddr() {
        return "127.0.0.1";
    }

    public String getLocalName() {
        return "localhost";
    }

    public String toString() {
        String s = this.method + " " + this.host + ":" + this.port + this.path;
        if (this.queryString != null) {
            s += "?" + queryString;
        }
        return s;
    }

    // since 3.0 / 3.1

    public String changeSessionId() {
        HttpSession session = (HttpSession) context.sessionManager.remove(sid);
        if (session == null)
            return null;
        final String nid = SessionManager.newKey();
        session.sessionId = nid;
        context.sessionManager.put(nid, session);
        return nid;
    }

    public long getContentLengthLong() {
        return inContentLength;
    }

    /**
     * Read the Post data and parse the parts
     * 
     * @throws IOException
     * @throws ServletException
     */
    private void parseParts() throws IOException, ServletException {
        parts = new HashMap<String, Part>();

        final String cType = getContentType();
        if (cType == null || cType.length() < 20 || !cType.substring(0, 20).equalsIgnoreCase("multipart/form-data;"))
            throw new ServletException("request is not of type multipart/form-data");

        final int boundaryOff = cType.toLowerCase().indexOf("boundary=") + 9;
        String boundary = cType.substring(boundaryOff).trim();
        final int semi = boundary.indexOf(';');
        if (semi > 0)
            boundary = boundary.substring(0, semi);

        // use a file if length to long
        if (inContentLength < 0 || inContentLength > 0x8000) {
            partsFile = File.createTempFile("parts_", "temp");
            final FileOutputStream fos = new FileOutputStream(partsFile);
            IOUtils.copy(getInputStream(), fos, inContentLength);
            fos.close();
        } else {
            partsData = new byte[(int) inContentLength];
            IOUtils.readFully(getInputStream(), partsData);
        }

        final InputStream is = partsFile != null ? new FileInputStream(partsFile) : new FastByteArrayInputStream(
                partsData);
        final ArrayList<MimeFile.Info> mimeInfos = MimeFile.parseMime(is, boundary);
        is.close();

        for (final MimeFile.Info mimeInfo : mimeInfos) {
            final Part p = new MimePart(partsFile, partsData, cType, mimeInfo);
            parts.put(p.getName().toLowerCase(), p);
        }
    }

    public Part getPart(String key) throws IOException, ServletException {
        if (parts == null)
            parseParts();
        return parts.get(key.toLowerCase());
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        if (parts == null)
            parseParts();
        return parts.values();
    }

    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return context.checkAccess(method, getServletPath(), this, response) != null;
    }

    public void login(String user, String pass) throws ServletException {
        if (remoteUser != null || authType != null)
            throw new ServletException("already logged in");
        if (context.verify == null)
            throw new ServletException("can't login without authentication mechanism");
        if (context.verify instanceof FormVerification)
            throw new ServletException("can't login with form based authentication");

        Collection<String> roles = context.verify.verifyUserGroup(user, pass);
        if (roles == null)
            throw new ServletException("login failed for user: " + user);
            
        final HttpSession session = (HttpSession) getSession(true);
        session.setAttribute("j_username", user);
        session.setAttribute("j_user_roles", roles);
        remoteUser = user;
    }

    public void logout() throws ServletException {
        final HttpSession session = (HttpSession) getSession(true);
        session.removeAttribute("j_username");
        session.removeAttribute("j_user_roles");
        remoteUser = null;
    }

    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    public AsyncContext startAsync() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }
}