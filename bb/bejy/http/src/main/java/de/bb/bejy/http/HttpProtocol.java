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
        version = headerVersion + " (c) 2000-2015 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
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
                    if (request.method.equals("POST") && request.sis != null) {
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
