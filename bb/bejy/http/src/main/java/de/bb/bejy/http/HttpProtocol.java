/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpProtocol.java,v $
 * $Revision: 1.71 $
 * $Date: 2014/06/23 15:38:46 $
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
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletInputStream;

import de.bb.bejy.Protocol;
import de.bb.bejy.Version;
import de.bb.io.FastBufferedInputStream;
import de.bb.io.FastBufferedOutputStream;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.DateFormat;
import de.bb.util.LogFile;

public class HttpProtocol extends Protocol {
    // debug switch
    final static boolean DEBUG = false;

    // many constants
    final static ByteRef GET = new ByteRef("GET");

    final static ByteRef USERAGENT = new ByteRef("USER-AGENT");

    final static ByteRef REFERER = new ByteRef("REFERER");

    final static ByteRef CONNECTION = new ByteRef("CONNECTION");

    final static ByteRef KEEPALIVE = new ByteRef("KEEP-ALIVE");

    final static ByteRef HTTP11 = new ByteRef("HTTP/1.1");

    final static ByteRef RTSP10 = new ByteRef("RTSP/1.0");

    final static byte[] SDS = { ' ', '-', ' ' };

    final static byte[] ILLEGAL_REQUEST = "illegal request".getBytes();

    // used http-logfile
    //private LogFile logFile;
    // our factory
    HttpFactory factory;

    // version stuff
    private final static String no;

    private final static String headerVersion;

    private final static String version;
    static {
        String s = "$Revision: 1.71 $";
        no = "1.3." + s.substring(11, s.length() - 1);
        headerVersion = Version.getShort() + " HTTP " + no;
        version = headerVersion + " (c) 2000-2012 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    public static String getVersion() {
        return no;
    }

    public static String getHeaderVersion() {
        return headerVersion;
    }

    public static String getFullVersion() {
        return version;
    }

    protected ByteRef br; // current read buffer

    ByteRef requestLine; // complete request line

    boolean keep; // keep connection alive

    private FastBufferedInputStream fis = new FastBufferedInputStream(is, 32000);

    // out buffer for log
    FastByteArrayOutputStream logBuffer = new FastByteArrayOutputStream(8192);

    FastByteArrayOutputStream headerBuffer = new FastByteArrayOutputStream(4096);

    FastByteArrayOutputStream outBuffer = new FastByteArrayOutputStream(32768);

    private HttpRequest currentRequest;

    protected HttpProtocol(HttpFactory hf, LogFile _logFile) {
        super(hf);
        factory = hf;
    }

    protected void setStreams(InputStream _is, OutputStream _os, String remote) {
        super.setStreams(_is, new FastBufferedOutputStream(_os, 14120), remote);
        //    super.setStreams(_is, _os, remote);
    }

    /**
     * handle incoming data first byte is already read read the rest
     */
    protected boolean doit() throws Exception {
        final HttpRequest request = new HttpRequest(this, server.getPort());
        this.currentRequest = request;
        try {
            //    request.is = is;
            //    request.isSsl = server.usesSsl();
            //    request.remoteAddress = remoteAddress;
            //    request.port = server.getPort();

            // parse incoming data and assemble a HttpRequestObject
            if (br == null || br.length() == 0)
                br = readFirst();

            // get request line
            do {
                requestLine = readLine(br);
                if (requestLine == null)
                    return false;
            } while (requestLine.length() == 0);

            if (DEBUG) {
                System.out.println("<" + requestLine + ">");
                System.out.println("br:" + br.length());
                System.out.println(br);
            }

            br = request.parseHeader(br);
            if (br == null)
                return false;

            if (DEBUG) {
                System.out.println("got headers:");
                System.out.println(request.inHeaders);
            }

            request.parseRequestLine(requestLine);

            // lookup server from hostname
            Host hs = (Host) factory.hosts.get(request.host);
            if (hs == null) {
                hs = (Host) factory.hosts.get("*");
            }

            if (DEBUG) {
                System.out.println("host = " + hs.getName());
            }

            HttpResponse response = new HttpResponse(request);

            // set OutputStream
            response.os = os;
            if (request.inContentLength > 0) {
                if (DEBUG)
                    System.out.println("BR: " + br);
                request.sis = createSIS(request);
            }

            request.handle(hs, response);

            try {
                response.flushRequest();
            } catch (Throwable t) {
                if (DEBUG) {
                    t.printStackTrace();
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    System.out.println("with class loader: " + cl);
                }
                // ignore errors here      
            }
            if (hs != null) {
                hs.httpLog.writeln(formatLog(request, response));
                //      LogThread.add(hs.httpLog, request, response, requestLine, remoteAddress);
            }

            boolean alive = keepAlive(request, response);
            if (alive) {
                try {
                    // we dont know whether the Servlet has read the POST parameters, so skip all available data
                    if (request.method.equals("POST")) {
                        request.sis.skip(((SIStream) request.sis).avail);
                    }
                } catch (Throwable t) {
                    // ignore errors here      
                    if (DEBUG)
                        t.printStackTrace();
                }
            }
            return alive;
        } finally {
            currentRequest = null;
        }
    }

    ServletInputStream createSIS(HttpRequest request) {
        fis.reassign(is);
        return new SIStream(br, request.getContentLength(), fis);
    }

    protected static boolean keepAlive(HttpRequestBase request, HttpResponse response) {
        if (request.protocol.equals(RTSP10))
            return true;
        if (!request.protocol.equals(HTTP11))
            return false;
        String bAlive = request.inHeaders.get("CONNECTION");
        boolean alive = bAlive != null && bAlive.toUpperCase().equals("KEEP-ALIVE");
        return alive;
    }

    protected byte[] formatLog(HttpRequestBase request, HttpResponse response) throws IOException {
        // get some values
        String referer = request.getHeader("REFERER");
        String userAgent = request.getHeader("USER-AGENT");

        FastByteArrayOutputStream bos = this.logBuffer;
        bos.reset();
        ByteUtil.writeString(remoteAddress, bos);
        bos.write(SDS);
        if (request.remoteUser != null)
            ByteUtil.writeString(request.remoteUser, bos);
        else
            bos.write('-');
        bos.write(' ');
        bos.write('[');
        //ByteUtil.writeString(DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(System.currentTimeMillis()), bos);
        bos.write(DateFormat.ba_dd_MMM_yyyy_HH_mm_ss_zzzz(System.currentTimeMillis()));
        bos.write(']');
        bos.write(' ');
        bos.write('"');
        if (requestLine != null)
            requestLine.writeTo(bos);
        else
            bos.write(ILLEGAL_REQUEST);
        bos.write('"');
        bos.write(' ');
        ByteUtil.writeInt(response.status, bos);
        bos.write(' ');
        ByteUtil.writeLong(response.sos.getWritten(), bos);
        bos.write(' ');
        bos.write('"');
        if (referer != null)
            ByteUtil.writeString(referer, bos);
        else
            bos.write('-');
        bos.write('"');
        bos.write(' ');
        bos.write('"');
        if (userAgent != null)
            ByteUtil.writeString(userAgent, bos);
        else
            bos.write('-');
        bos.write('"');
        return bos.toByteArray();
    }

    /**
     * Returns true if the servers uses a secure connection aka HTTPS.
     * 
     * @return true if the servers uses a secure connection.
     */
    public boolean isSecure() {
        return server.usesSsl();
    }

    public HttpRequest getRequest() {
        return currentRequest;
    }
}

/******************************************************************************
 * $Log: HttpProtocol.java,v $
 * Revision 1.71  2014/06/23 15:38:46  bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles
 * Revision 1.70 2013/11/28 10:28:32 bebbo
 * 
 * @R the HttpProtocol keeps track of the current processed request
 * 
 *    Revision 1.69 2013/11/23 10:36:19 bebbo
 * @R contentLength supports long values now
 * 
 *    Revision 1.68 2013/06/07 16:23:42 bebbo
 * @B forwarding to a CGI handler no longer reads the post data. Revision 1.67 2013/05/17 10:49:03 bebbo
 * 
 * @I remove HTTP/1.0 answer in HTTP/1.1 Revision 1.66 2012/12/15 19:38:55 bebbo
 * 
 * @I refactoring Revision 1.65 2012/07/18 06:44:59 bebbo
 * 
 * @I typified Revision 1.64 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.63 2010/07/08 18:16:25 bebbo
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 *    Revision 1.62 2009/11/25 08:29:13 bebbo
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.61 2009/11/18 08:47:41 bebbo
 * @D Debug stuff
 * 
 *    Revision 1.60 2009/06/15 08:36:26 bebbo
 * @B B-REMOTEHOST is used correctly
 * @I using BufferedStreams where convenient
 * 
 *    Revision 1.59 2008/03/13 20:43:38 bebbo
 * @O performance optimizations
 * 
 *    Revision 1.58 2008/01/17 17:32:14 bebbo
 * @O optimizations for better performance
 * 
 *    Revision 1.57 2007/04/13 17:54:56 bebbo
 * @R made the readBuffer <code>bvisible to derived classes
 * 
 *    Revision 1.56 2007/02/12 20:38:24 bebbo
 * @N added support for derived RtspProtocol
 * 
 *    Revision 1.55 2007/01/18 21:47:16 bebbo
 * @I reformatted
 * 
 *    Revision 1.54 2006/05/09 12:13:26 bebbo
 * @R changes to comply to servlet2_4
 * 
 *    Revision 1.53 2006/03/17 20:06:22 bebbo
 * @B fixed possible NPE
 * 
 *    Revision 1.52 2006/02/06 09:15:33 bebbo
 * @I cleanup
 * 
 *    Revision 1.51 2004/12/13 15:24:10 bebbo
 * @B using an own repsonse instance per request to avoid reuse collisions
 * 
 *    Revision 1.50 2004/04/07 16:28:44 bebbo
 * @V new version message
 * 
 *    Revision 1.49 2004/03/24 09:54:05 bebbo
 * @V new version information
 * 
 *    Revision 1.48 2004/03/24 09:48:38 bebbo
 * @V new version information
 * 
 *    Revision 1.47 2004/03/23 12:27:12 bebbo
 * @V the http protocol now always uses the same version as BEJY
 * 
 *    Revision 1.46 2003/11/16 09:29:38 bebbo
 * @B re-enabled the real remoteAddress if redirected
 * 
 *    Revision 1.45 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.44 2003/02/25 07:01:56 bebbo
 * @R protocols are using now BufferedOutputStreams
 * 
 *    Revision 1.43 2003/01/08 12:57:30 bebbo
 * @B removed connection close on redirects - this must be solved in the RRProtocol!
 * 
 *    Revision 1.42 2002/12/19 14:52:23 bebbo
 * @R renamed ServletResponse into HttpResponse
 * 
 *    Revision 1.41 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.40 2002/11/07 13:39:42 bebbo
 * @D more DEBUG messages
 * 
 *    Revision 1.39 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.38 2002/05/19 12:54:22 bebbo
 * @B fixed H401 and H302 handling
 * 
 *    Revision 1.37 2002/05/16 15:22:52 franke
 * @B HttpSessions are now marked "used" correctly
 * 
 *    Revision 1.36 2002/04/02 13:02:35 franke
 * @I fixed mayn bugs and added too many features to enumerate them here
 * 
 *    Revision 1.35 2002/03/30 15:47:49 franke
 * @B many fixes in incomplete Servlet 2.3 functions
 * 
 *    Revision 1.34 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some config function calls. Also added the
 *    use of a special ClassLoader.
 * 
 *    Revision 1.33 2002/03/10 20:06:53 bebbo
 * @I redesigned buffer handling: on large data now a fallback to HTTP/1.0 is used and no ContentLength is specified.
 * @B fixed setAttribue functions
 * 
 *    Revision 1.32 2002/03/04 20:43:00 franke
 * @B rewrote POST parameter handling and fixed to parse them only when content type is
 *    application/x-www-form-urlencoded.
 * @B remove unread bytes from input stream, when a servlet/CGI does not read the complete POST data.
 * 
 *    Revision 1.31 2002/02/11 12:40:17 franke
 * @B fixed handling of pipelined requests
 * 
 *    Revision 1.30 2002/01/26 15:26:25 bebbo
 * @B fixed the recognition of a default document
 * 
 *    Revision 1.29 2002/01/20 20:44:23 franke
 * @B fixed context/handler selection with user groups
 * 
 *    Revision 1.28 2002/01/20 20:03:24 franke
 * @D disabled DEBUG
 * 
 *    Revision 1.27 2001/12/28 11:47:09 franke
 * @B fixed parseParameters: POST parameters are no longer appended to the query string
 * 
 *    Revision 1.26 2001/12/04 17:40:42 franke
 * @N separated RequestDispatcher to ease the forward and inlude funtions. Caused some changes, since members from
 *    HttpProtocol moved.
 * 
 *    Revision 1.25 2001/11/20 17:36:42 bebbo
 * @B fixed RequestDispatcher stuff
 * 
 *    Revision 1.24 2001/09/15 08:47:33 bebbo
 * @I using JDK 1.2
 * 
 *    Revision 1.23 2001/08/24 08:24:26 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.22 2001/08/14 16:12:04 bebbo
 * @N implemented sendRedirect()
 * 
 *    Revision 1.21 2001/07/13 13:13:03 bebbo
 * @B made parameters work
 * 
 *    Revision 1.20 2001/06/11 06:33:09 bebbo
 * @N implemented param functions
 * 
 *    Revision 1.19 2001/05/14 21:39:21 bebbo
 * @B user is now set correctly
 * 
 *    Revision 1.18 2001/05/07 16:18:03 bebbo
 * @B fixed behaviour for long file donwload
 * 
 *    Revision 1.17 2001/05/06 13:11:25 bebbo
 * @R date format function changed name
 * 
 *    Revision 1.16 2001/04/22 21:39:56 bebbo
 * @B fixed format of http log
 * 
 *    Revision 1.15 2001/04/16 20:03:56 bebbo
 * @B fixes in 302 redirect
 * 
 *    Revision 1.14 2001/04/11 15:46:41 bebbo
 * @N added information for real TCP port
 * 
 *    Revision 1.13 2001/04/11 13:17:07 bebbo
 * @N implemented getRequestURI()
 * 
 *    Revision 1.12 2001/04/06 05:54:05 bebbo
 * @B fix for HTTP/1.0
 * @B fix for cookies
 * 
 *    Revision 1.11 2001/04/02 16:14:15 bebbo
 * @I removed obsolete parameter
 * 
 *    Revision 1.10 2001/03/30 17:28:04 bebbo
 * @N added user authentication
 * 
 *    Revision 1.9 2001/03/29 19:55:06 bebbo
 * @B fixed getServletPath()
 * @I cookies are now partially read
 * @N added session handling
 * 
 *    Revision 1.8 2001/03/29 18:25:58 bebbo
 * @I completed beta stage
 * 
 *    Revision 1.7 2001/03/29 07:09:52 bebbo
 * @R further changes to conform with servlet 2.3
 * 
 *    Revision 1.6 2001/03/28 09:15:04 bebbo
 * @D debug off
 * 
 *    Revision 1.5 2001/03/27 19:48:34 bebbo
 * @I lot's of stuff changed
 * @I more servlet functions implemented
 * 
 *    Revision 1.4 2001/03/27 09:52:19 franke
 * @I continued to implement
 * 
 *    Revision 1.3 2001/03/20 18:34:07 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 * 
 *    Revision 1.2 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *    Revision 1.1 2001/02/26 17:48:54 bebbo
 * @R new home
 * 
 *    Revision 1.2 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *    Revision 1.1 2000/11/10 18:13:26 bebbo
 * @N new (uncomplete stuff)
 * 
 *****************************************************************************/
