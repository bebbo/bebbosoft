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
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

import de.bb.bejy.Config;
import de.bb.bejy.IOPipeThread;
import de.bb.bejy.Protocol;
import de.bb.bejy.UserGroupDbi;
import de.bb.bejy.Version;
import de.bb.io.FastBufferedInputStream;
import de.bb.io.FastBufferedOutputStream;
import de.bb.io.FastByteArrayInputStream;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.Misc;
import de.bb.util.MultiMap;

/**
 * A request response redirector. Incoming requests are modified and forwarded
 * to the configured destination. The response is also modified according to the
 * configuration.
 * 
 * @author bebbo
 *
 */
class RRProtocol extends Protocol {

    private final static ByteRef ACCEPT = new ByteRef("ACCEPT");
    private final static ByteRef AUTHORIZATION = new ByteRef("AUTHORIZATION");
    private final static ByteRef CHUNKED = new ByteRef("CHUNKED");
    private final static ByteRef CONNECTION = new ByteRef("CONNECTION");
    private final static ByteRef CLOSE = new ByteRef("close");
    private final static ByteRef CONTENT_ENCODING = new ByteRef("CONTENT-ENCODING");
    private final static ByteRef CONTENT_LENGTH = new ByteRef("CONTENT-LENGTH");
    private final static ByteRef CONTENT_TYPE = new ByteRef("CONTENT-TYPE");
    private final static ByteRef COOKIE_PATH = new ByteRef("PATH=");
    private final static ByteRef HOST = new ByteRef("HOST");
    private final static ByteRef KEEPALIVE = new ByteRef("KEEP-ALIVE");
    private final static ByteRef LOCATION = new ByteRef("LOCATION");
    private final static ByteRef SET_COOKIE = new ByteRef("SET-COOKIE");
    private final static ByteRef TRANSFER_ENCODING = new ByteRef("TRANSFER-ENCODING");

    private final static ByteRef HTTP = new ByteRef("http://");
    private final static ByteRef HTTPS = new ByteRef("https://");
    private final static ByteRef SHTTP = new ByteRef("/http:/");
    private final static ByteRef SHTTPS = new ByteRef("/https:/");
    private final static ByteRef HTTP10 = new ByteRef("HTTP/1.0");

    private final static ByteRef GZIP = new ByteRef("GZIP");

    private final static ByteRef ROOT = new ByteRef("/");
    private static final ByteRef SPACE = new ByteRef(" ");

    private final static byte[] CRLF = { 0xd, 0xa };
    private final static byte[] COLON_SPACE = { ':', ' ' };

    private final static ByteRef XML = new ByteRef("<?xml");
    private final static ByteRef STREAM = new ByteRef("<stream:");

    private boolean DEBUG;

    private RRFactory factory;

    private LogFile logFile;

    private ByteRef requestBuffer;

    private String currentDest;

    private int currentDestPort;

    private String forwardHost;

    private ByteRef orgPath;

    private ByteRef mappedPath;

    private ByteRef requestLine;

    private ByteRef responseBuffer;

    private long outContentLength;

    private boolean keepAlive;

    private boolean chunked;

    ByteRef contentType;

    ByteRef contentEncoding;

    private long inContentLength;

    private ByteRef host;

    RREntry lastDestination;
    java.net.Socket forwardSocket;
    InputStream is2Forward;
    OutputStream os2Forward;
    private ByteRef lookupPath;
    private String forwardUrl;

    RRProtocol(RRFactory hf, LogFile _logFile) {
        super(hf);
        factory = hf;
        DEBUG = hf.getBooleanProperty("verbose", false);
        logFile = hf.getLogFile();
    }

    /**
     * handle incoming data first byte is already read read the rest
     */
    protected boolean doit() throws Exception {
        responseBuffer = new ByteRef();
        forwardSocket = null;
        try {

            // get request line
            requestBuffer = readFirst();

            // check for an incoming SOCKS5 start packet
            if (factory.socks5Group != null && requestBuffer.charAt(0) == 5) {
                socks5Connect(requestBuffer);
                return false;
            }

            // check for an incoming JABBER packet
            if (factory.jabberPort != 0 && (requestBuffer.startsWith(XML) || requestBuffer.startsWith(STREAM))) {
                jabberConnect(requestBuffer);
                return false;
            }

            long lastRequestAt = 0;
            for (;;) {

                host = null;
                lookupPath = null;
                forwardUrl = null;

                // read the http request
                requestLine = readLine(requestBuffer);
                if (requestLine == null)
                    return false;
                if (DEBUG)
                    System.out.println("<" + requestLine + ">");

                // check for proxy CONNECT
                if (requestLine.startsWith("CONNECT")) {
                    return proxyConnect(requestLine, requestBuffer);
                }

                long now = System.currentTimeMillis();
                if (now - 3141 > lastRequestAt) {
                    closeForwardSocket();
                }
                lastRequestAt = now;

                // read the header
                final MultiMap<ByteRef, ByteRef> requestHeaders = new MultiMap<ByteRef, ByteRef>();
                if (!readRequestHeaders(requestBuffer, requestHeaders))
                    break;

                final RREntry destination = lookupDestination();
                if (destination == null) {
                    send404();
                    return false;
                }

                if (!checkAccess(destination, requestHeaders)) {
                    send401(destination);
                    continue;
                }

                checkForDestinationSwitch(destination);

                // get the URI to forward to
                forwardUrl = factory.getUri(remoteAddress, destination);
                if (forwardUrl == null) {
                    send404();
                    return false;
                }

                setupPaths(destination, forwardUrl);

                connect();

                if (!forwardRequest(requestHeaders)) {
                    send408();
                    return false;
                }

                final ByteRef responseLine = copyResponseLine();
                if (responseLine == null) {
                    send502();
                    return false;
                }

                // no data to handle on 304
                responseLine.nextWord();
                ByteRef rc = responseLine.nextWord();
                int rcode = rc.toInteger();

                logFile.writeDate(rcode + "\t" + getRemoteAddress() + "\t" + host + "\t" + lookupPath + "\t" + forwardUrl);

                final MultiMap<ByteRef, ByteRef> responseHeaders = readResponseHeaders(responseBuffer, is2Forward, os);

                if (contentType == null) {
                    final ByteRef accept = requestHeaders.get(ACCEPT);
                    if (accept != null)
                        contentType = accept.nextWord(',');
                }

                final String[] attrs = getPatchedAttributes(destination);
                final boolean copyData = attrs == null;

                // if there is nothing to patch, write the headers now.
                if (copyData || rcode == 304 || chunked)
                    writeHeaders(os, responseHeaders);

                if (rcode != 304) {
                    ByteRef output;
                    if (chunked) {
                        output = copyOrReadChunkedData(copyData);
                    } else if (outContentLength < 0) {
                        output = copyOrReadUntilEOF(copyData);
                    } else {
                        output = copyOrReadBySize(copyData);
                    }

                    // data to patch was collected in output - now patch it
                    if (!copyData) {

                        if (contentEncoding != null) {
                            // decompress
                            final FastByteArrayInputStream bis = new FastByteArrayInputStream(output.toByteArray());
                            final FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
                            FileHandler.decompress(GZIP.equalsIgnoreCase(contentEncoding), output.length(), bis, bos);
                            output = new ByteRef(bos.toByteArray());
                        }

                        for (final String attr : attrs) {
                            output = doPatchXML(output, attr + "=");
                        }

                        if (contentEncoding != null) {
                            // compress again
                            final FastByteArrayInputStream bis = new FastByteArrayInputStream(output.toByteArray());
                            final FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
                            FileHandler.compress(GZIP.equalsIgnoreCase(contentEncoding), output.length(), bis, bos);
                            output = new ByteRef(bos.toByteArray());
                        }

                        responseHeaders.remove(CONTENT_LENGTH);
                        if (chunked) {
                            writeOutputChunked(output);
                        } else {
                            responseHeaders.put(CONTENT_LENGTH, new ByteRef(Integer.toString(output.length())));
                            writeHeaders(os, responseHeaders);
                            output.writeTo(os);
                        }
                    }
                }

                if (DEBUG)
                    System.out.println("done");

                os.flush();

                // exit loop if necessary
                if (!keepAlive) {
                    closeForwardSocket();
                    break;
                }
            }
        } catch (IOException ioe) {
            if (DEBUG)
                ioe.printStackTrace();
            logFile.writeDate("500\t" + getRemoteAddress() + "\t" + host + "\t" + lookupPath + "\t" + ioe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeForwardSocket();
        }
        return false;
    }

    private void writeOutputChunked(ByteRef output) throws IOException {
        // write the patched data chunked
        for (;;) {
            int len = output.length() > 8192 ? 8192 : output.length();
            String hex = Integer.toHexString(len);
            while (hex.length() < 4) {
                hex = "0" + hex;
            }
            ByteUtil.writeString(hex, os);
            os.write(CRLF);
            if (len == 0)
                break;

            ByteRef chunk = output.splitLeft(len);
            chunk.writeTo(os);
            os.write(CRLF);
        }
        os.write(CRLF);
    }

    private ByteRef copyOrReadBySize(boolean copy) throws IOException {
        ByteRef output = new ByteRef();
        while (outContentLength > 0) {
            if (DEBUG)
                System.out.println("needed: " + outContentLength);

            while (responseBuffer != null && responseBuffer.length() == 0) {
                responseBuffer = responseBuffer.update(is2Forward);
            }

            if (responseBuffer == null)
                throw new IOException("not enough data");

            int sz = outContentLength > responseBuffer.length() ? responseBuffer.length() : (int) outContentLength;

            if (DEBUG)
                System.out.println("copy: " + sz);

            if (copy) {
                responseBuffer.splitLeft(sz).writeTo(os);
            } else {
                output = output.append(responseBuffer.splitLeft(sz));
            }
            outContentLength -= sz;
        }
        return output;
    }

    private ByteRef copyOrReadUntilEOF(boolean copy) throws IOException {
        ByteRef output = new ByteRef();
        if (DEBUG)
            System.out.println("copy to EOF: ");
        while (responseBuffer != null) {
            if (responseBuffer.length() > 0) {
                if (copy) {
                    responseBuffer.writeTo(os);
                } else {
                    output = output.append(responseBuffer);
                }
                responseBuffer = new ByteRef();
            }
            responseBuffer = responseBuffer.update(is2Forward);
        }
        return output;
    }

    private ByteRef copyOrReadChunkedData(boolean copy) throws IOException {
        ByteRef output = new ByteRef();
        for (;;) {
            ByteRef sizeLine;
            for (;;) {
                sizeLine = responseBuffer.nextLineCRLF();
                if (sizeLine == null)
                    responseBuffer.update(is2Forward);
                if (sizeLine == null)
                    continue;
                if (sizeLine.length() > 0)
                    break;
            }

            if (copy) {
                sizeLine.writeTo(os);
                os.write(CRLF);
            }

            int len = Integer.parseInt(sizeLine.toString(), 16) + 2;

            while (responseBuffer.length() < len) {
                responseBuffer.update(is2Forward);
            }
            ByteRef chunk = responseBuffer.splitLeft(len);
            if (copy) {
                chunk.writeTo(os);
            } else {
                output = output.append(chunk.substring(0, chunk.length() - 2));
            }
            if (len == 2)
                break;
        }
        return output;
    }

    private String[] getPatchedAttributes(RREntry destination) {
        String[] attrs = null;
        if (!destination.extension2Attrs.isEmpty()) {
            // read the response and check if patch is needed
            int lspace = requestLine.lastIndexOf(' ');
            ByteRef ending = requestLine.substring(0, lspace);

            // search extensions
            for (Entry<String, String[]> e : destination.extension2Attrs.entrySet()) {
                if (ending.endsWith(e.getKey())) {
                    attrs = e.getValue();
                    break;
                }
            }
        }
        if (attrs == null && contentType != null && !destination.type2Attrs.isEmpty()) {
            contentType = contentType.nextWord(';').trim();
            attrs = destination.type2Attrs.get(contentType.toString());
        }
        return attrs;
    }

    private boolean forwardRequest(Map<ByteRef, ByteRef> requestHeader) throws IOException {

        // change the url
        ByteRef rewriteRequestLine = rewriteRequestLine(requestLine);
        if (DEBUG) {
            System.out.println(requestLine + " => " + rewriteRequestLine);
        }

        rewriteRequestLine.writeTo(os2Forward);

        // add information to suggest the hidden instance the port
        // and
        // remote host.
        os2Forward.write(CRLF);
        os2Forward.write(("B-REMOTEHOST: " + remoteAddress).getBytes());
        os2Forward.write(CRLF);
        os2Forward.write(("B-SERVERPORT: " + server.getPort()).getBytes());
        os2Forward.write(CRLF);
        if (server.usesSsl()) {
            os2Forward.write("B-SECURE: true".getBytes());
            os2Forward.write(CRLF);
        }

        writeHeaders(os2Forward, requestHeader);

        // send content Data, if any
        if (inContentLength > 0) {
            if (DEBUG)
                System.out.println("request content: " + inContentLength);
            while (inContentLength > 0) {
                while (requestBuffer.length() == 0) {
                    requestBuffer = requestBuffer.update(is);
                    if (requestBuffer == null)
                        return false;
                }
                if (inContentLength > 0) {
                    int sz = inContentLength > requestBuffer.length() ? requestBuffer.length() : (int) inContentLength;
                    ByteRef dat = requestBuffer.splitLeft(sz);
                    dat.writeTo(os2Forward);
                    inContentLength -= sz;
                }
            }
        }

        os2Forward.flush();
        return true;
    }

    private void connect() throws IOException {
        int dp = forwardHost.indexOf(':');
        // open new connection if necessary
        if (forwardSocket != null && forwardSocket.isClosed()) {
            closeForwardSocket();
        }
        if (forwardSocket == null) {
            if (DEBUG)
                System.out.println("new Socket -> " + forwardHost);
            currentDest = forwardHost.substring(0, dp);
            currentDestPort = Integer.parseInt(forwardHost.substring(dp + 1));
            forwardSocket = new java.net.Socket(currentDest, currentDestPort);
            int timeout = server.getTimeout();
            forwardSocket.setSoTimeout(timeout);
            // get other streams
            is2Forward = new FastBufferedInputStream(forwardSocket.getInputStream(), 1412);
            // is2Forward = forwardSocket.getInputStream();
            os2Forward = new FastBufferedOutputStream(forwardSocket.getOutputStream(), 1412);
            // os2Forward = forwardSocket.getOutputStream();
        }
    }

    private void setupPaths(final RREntry destination, final String forwardUrl) {
        int slash = forwardUrl.indexOf('/');
        if (slash == -1)
            slash = forwardUrl.length();

        forwardHost = forwardUrl.substring(0, slash);

        mappedPath = new ByteRef(forwardUrl);
        mappedPath = mappedPath.substring(slash);
        if (mappedPath.endsWith(ROOT))
            mappedPath = mappedPath.substring(0, mappedPath.length() - 1);

        if (DEBUG)
            System.out.println("redirect to:" + forwardHost);

        orgPath = destination.path;
        if (orgPath.endsWith(ROOT))
            orgPath = orgPath.substring(orgPath.length() - 1);
    }

    private void checkForDestinationSwitch(RREntry destination) {
        // did the destination change?
        if (lastDestination != destination) {
            closeForwardSocket();
        }
        lastDestination = destination;
    }

    private void closeForwardSocket() {
        // close old
        try {
            if (forwardSocket != null) {
                if (DEBUG)
                    System.out.println("closing forward socket");
                forwardSocket.close();
            }
        } catch (IOException ex) {
        }
        os2Forward = null;
        is2Forward = null;
        forwardSocket = null;
    }

    private boolean checkAccess(final RREntry destination, final Map<ByteRef, ByteRef> requestHeader) throws IOException {
        UserGroupDbi group = Config.getGroup(destination.group);
        if (group != null) {

            String user = null;
            String pass = null;

            ByteRef auth = (ByteRef) requestHeader.remove(AUTHORIZATION);
            if (auth != null) {
                ByteRef basic = auth.nextWord();
                if (basic.equalsIgnoreCase("basic")) {
                    // parse the auth and use the group to verify
                    byte b[] = Mime.decode(auth.toByteArray(), 0, auth.length());
                    if (b != null) {
                        ByteRef line = new ByteRef(b);
                        user = line.nextWord(':').toString();
                        pass = line.toString();
                    }
                }
            }

            if (user == null || pass == null || group.verifyUserGroup(user, pass) != null) {
                return false;
            }

            if (destination.userHeader != null && destination.userHeader.length() > 0)
                requestHeader.put(new ByteRef(destination.userHeader), new ByteRef(user));
        }

        return true;
    }

    private void send401(final RREntry destination) throws IOException {
        ByteUtil.writeString("HTTP/1.0 401 Unauthorized\r\n", os);
        ByteUtil.writeString("WWW-Authenticate: Basic realm=\"" + destination.group + "\"\r\n", os);
        ByteUtil.writeString("Content-Length: 0\r\n", os);
        ByteUtil.writeString("Connection: close, close\r\n\r\n", os);
        os.flush();
    }

    private void send404() throws IOException {
        logFile.writeDate("404\t" + getRemoteAddress() + "\t" + host + "\t" + lookupPath + "\t" + forwardUrl);
        ByteUtil.writeString("HTTP/1.0 404 Not Found\r\n", os);
        ByteUtil.writeString("Content-Length: 0\r\n", os);
        ByteUtil.writeString("Connection: close, close\r\n\r\n", os);
        os.flush();
    }

    private void send408() throws IOException {
        logFile.writeDate("408\t" + getRemoteAddress() + "\t" + host + "\t" + lookupPath + "\t" + forwardUrl);
        ByteUtil.writeString("HTTP/1.0 408 Request Timeout\r\n", os);
        ByteUtil.writeString("Content-Length: 0\r\n", os);
        ByteUtil.writeString("Connection: close, close\r\n\r\n", os);
        os.flush();
    }

    private void send502() throws IOException {
        logFile.writeDate("502\t" + getRemoteAddress() + "\t" + host + "\t" + lookupPath + "\t" + forwardUrl);
        ByteUtil.writeString("HTTP/1.0 502 Bad Gateway\r\n", os);
        ByteUtil.writeString("Content-Length: 0\r\n", os);
        ByteUtil.writeString("Connection: close, close\r\n\r\n", os);
        os.flush();
    }

    private RREntry lookupDestination() {
        lookupPath = extractPath();
        if (DEBUG)
            System.out.println("lookup for PATH=" + lookupPath);

        if (host == null) {
            // get the host name from the path
            int sp = lookupPath.indexOf('/');
            if (sp > 0) {
                sp += 2;
                int ep = lookupPath.indexOf('/', sp);
                host = lookupPath.substring(sp, ep);
                lookupPath = lookupPath.substring(ep);
            }
            if (host == null)
                return null;
        }

        // lookup with host only - no port
        final int colon = host.indexOf(':');
        if (colon > 0)
            host = host.substring(0, colon);

        if (DEBUG)
            System.out.println("lookup for HOST=" + host);

        if (host == null)
            return null; // no host -> error

        // get the forward entry
        final RREntry destination = factory.getRREntry(host, lookupPath);
        if (DEBUG)
            System.out.println("got destination: " + destination);

        return destination;
    }

    /**
     * Write the requestHeader to the output stream.
     * 
     * @param bos
     *            the OutputStream.
     * @param headers
     *            the map containing the name value pairs.
     * @throws IOException
     */
    private void writeHeaders(OutputStream bos, Map<ByteRef, ByteRef> headers) throws IOException {
        for (Entry<ByteRef, ByteRef> e : headers.entrySet()) {
            e.getKey().writeTo(bos);
            bos.write(COLON_SPACE);
            e.getValue().writeTo(bos);
            bos.write(CRLF);
        }
        bos.write(CRLF);
    }

    private ByteRef rewriteRequestLine(ByteRef requestLine) {
        int space = requestLine.indexOf(' ');
        if (space > 0) {
            ByteRef url = requestLine.substring(space + 1);
            if (url.startsWith(orgPath)) {
                url = url.substring(orgPath.length());
                if ((mappedPath.length() == 0 || url.charAt(0) > 32) && url.charAt(0) != '/')
                    url = ROOT.append(url);
                requestLine = requestLine.substring(0, space + 1).append(mappedPath).append(url);
            }
        }
        return requestLine;
    }

    /**
     * act like a SOCKS 5 server
     * 
     * @param requestBuffer
     * @throws IOException
     */
    private void socks5Connect(ByteRef requestBuffer) throws Exception {
        requestBuffer = readAtLeast(requestBuffer, 2);
        int numMethods = requestBuffer.charAt(1);

        requestBuffer = requestBuffer.substring(2);
        requestBuffer = readAtLeast(requestBuffer, numMethods);
        // we only support method 2 = user/password
        int i = 0;
        for (; i < numMethods; ++i) {
            if (requestBuffer.charAt(i) == 2)
                break;
        }
        if (i == numMethods) {
            // NO ACCEPTABLE METHOD
            os.write(5);
            os.write(0xff);
            os.flush();
            return;
        }

        os.write(5);
        os.write(0x2);
        os.flush();

        requestBuffer = requestBuffer.substring(numMethods);

        requestBuffer = readAtLeast(requestBuffer, 2);

        // user name
        if (requestBuffer.charAt(0) != 1)
            return;

        int nameLen = requestBuffer.charAt(1);
        requestBuffer = requestBuffer.substring(2);

        requestBuffer = readAtLeast(requestBuffer, nameLen);
        String userName = requestBuffer.splitLeft(nameLen).toString();

        requestBuffer = readAtLeast(requestBuffer, 1);
        nameLen = requestBuffer.charAt(0);
        requestBuffer = requestBuffer.substring(1);

        requestBuffer = readAtLeast(requestBuffer, nameLen);
        String password = requestBuffer.splitLeft(nameLen).toString();

        UserGroupDbi userDbi = Config.getGroup(factory.socks5Group);
        if (userDbi.verifyUserGroup(userName, password) == null) {
            // invalid user
            os.write(5);
            os.write(0xff);
            os.flush();
            return;
        }

        os.write(5);
        os.write(0x0);
        os.flush();

        requestBuffer = readAtLeast(requestBuffer, 4);

        byte[] reply = requestBuffer.toByteArray();
        if (requestBuffer.charAt(0) != 5 || requestBuffer.charAt(1) != 1) {
            // support only tcp ip4v for now
            reply[1] = 2;
            os.write(reply);
            os.flush();
            return;
        }

        int type = requestBuffer.charAt(3);
        requestBuffer = requestBuffer.substring(4);
        String dest = null;
        switch (type) {
        case 1:
            requestBuffer = readAtLeast(requestBuffer, 4);
            dest = requestBuffer.charAt(0) + "." + requestBuffer.charAt(1) + "." + requestBuffer.charAt(2) + "." + requestBuffer.charAt(3);
            requestBuffer = requestBuffer.substring(4);
            break;
        case 3:
            requestBuffer = readAtLeast(requestBuffer, 1);
            nameLen = requestBuffer.charAt(0);
            requestBuffer = requestBuffer.substring(1);
            requestBuffer = readAtLeast(requestBuffer, nameLen);
            dest = requestBuffer.splitLeft(nameLen).toString();
            break;
        case 4:
            requestBuffer = readAtLeast(requestBuffer, 16);
            dest = hb(requestBuffer.charAt(0), requestBuffer.charAt(1)) + ":" + hb(requestBuffer.charAt(2), requestBuffer.charAt(3)) + ":"
                    + hb(requestBuffer.charAt(4), requestBuffer.charAt(5)) + ":" + hb(requestBuffer.charAt(6), requestBuffer.charAt(7)) + ":"
                    + hb(requestBuffer.charAt(8), requestBuffer.charAt(9)) + ":" + hb(requestBuffer.charAt(10), requestBuffer.charAt(11)) + ":"
                    + hb(requestBuffer.charAt(12), requestBuffer.charAt(13)) + ":" + hb(requestBuffer.charAt(14), requestBuffer.charAt(15));
            requestBuffer = requestBuffer.substring(16);
            break;
        }

        if (dest == null) {
            // invalid address type
            reply[1] = 8;
            os.write(reply);
            os.flush();
            return;
        }

        requestBuffer = readAtLeast(requestBuffer, 2);
        int port = requestBuffer.charAt(0) << 8 | requestBuffer.charAt(1);
        requestBuffer = requestBuffer.substring(2);

        final Socket forwardSocket = new Socket(dest, port);

        reply[1] = 0;
        os.write(reply);
        os.flush();

        InputStream fbis = forwardSocket.getInputStream();
        OutputStream fbos = forwardSocket.getOutputStream();
        // create the pipe threads and start them
        IOPipeThread client2Server = new IOPipeThread(is, fbos);
        IOPipeThread server2Client = new IOPipeThread(fbis, getOs());
        // cross connect
        server2Client.setSlave(client2Server);
        client2Server.setSlave(server2Client);

        // start
        server2Client.start();
        client2Server.start();

        client2Server.join();
        server2Client.join();
        forwardSocket.close();

    }

    private ByteRef readAtLeast(ByteRef requestBuffer, int length) throws IOException {
        while (requestBuffer.length() < length) {
            final ByteRef last = requestBuffer;
            requestBuffer = requestBuffer.update(is);
            if (requestBuffer == null) {
                if (DEBUG)
                    Misc.dump(System.out, last.toByteArray());
                throw new IOException("EOS: wanted " + length + " had " + last.length());
            }
        }
        if (DEBUG)
            Misc.dump(System.out, requestBuffer.toByteArray());

        return requestBuffer;
    }

    private static String hb(int hi, int lo) {
        return Integer.toHexString((hi << 8) | lo);
    }

    private void jabberConnect(ByteRef requestBuffer) throws Exception {
        final Socket fallBackSocket = new Socket(factory.jabberHost, factory.jabberPort);
        InputStream fbis = fallBackSocket.getInputStream();
        OutputStream fbos = fallBackSocket.getOutputStream();
        // create the pipe threads and start them
        IOPipeThread client2Server = new IOPipeThread(is, fbos);
        IOPipeThread server2Client = new IOPipeThread(fbis, getOs());
        // cross connect
        server2Client.setSlave(client2Server);
        client2Server.setSlave(server2Client);

        // start
        server2Client.start();
        requestBuffer.writeTo(fbos);
        client2Server.start();

        client2Server.join();
        server2Client.join();

        fallBackSocket.close();
    }

    private boolean proxyConnect(ByteRef requestLine, ByteRef br) throws IOException, InterruptedException {

        HttpRequestBase request = new HttpRequestBase(this, 0);
        br = request.parseHeader(br);
        if (br == null)
            return false;
        request.parseRequestLine(requestLine);

        LogFile logFile = factory.getLogFile();
        if (factory.proxyGroup != null) {
            UserGroupDbi userDbi = Config.getGroup(factory.proxyGroup);
            if (userDbi == null) {
                logFile.writeDate("cannot find configured group: " + factory.proxyGroup);
                os.write((request.protocol + " 500 proxy is not configure\r\n\r\n").getBytes());
                return false;
            }

            String auth = request.getHeader("PROXY-AUTHORIZATION");
            byte b[] = Mime.decode(auth.getBytes(), 6, auth.length());
            // ByteRef line = new ByteRef(b);
            int i = 0;
            while (i < b.length && b[i] != ':') {
                ++i;
            } // search :
            if (i > b.length) {
                logFile.writeDate("validation failed - no user:password found");
                os.write((request.protocol + "  401 missing credentials\r\n\r\n").getBytes());
                return false;
            }
            String remoteUser = new String(b, 0, 0, i);
            String remotePass = new String(b, 0, i + 1, b.length - i - 1);

            if (userDbi.verifyUserGroup(remoteUser, remotePass) == null) {
                logFile.writeDate("validation failed for: " + remoteUser);
                os.write((request.protocol + " 401 wrong credentials\r\n\r\n").getBytes());
                return false;
            }
        }

        int colon = request.fullhost.indexOf(':');
        String host, sport;
        if (colon < 0) {
            host = request.fullhost;
            sport = "80";
        } else {
            host = request.fullhost.substring(0, colon);
            sport = request.fullhost.substring(colon + 1);
        }

        Socket socket = new Socket(host, Integer.parseInt(sport));
        String msg = request.protocol + " 200 Connection established\r\nProxy-Agent: " + Version.getShort();
        if (request.protocol.equals("HTTP/1.1"))
            msg += "\r\nProxy-Connection: keep-alive";
        msg += "\r\n\r\n";
        msg = "HTTP/1.0 200 Connection established\r\n\r\n";
        os.write((msg).getBytes());

        InputStream sis = socket.getInputStream();
        OutputStream sos = socket.getOutputStream();

        os.flush();
        br.writeTo(sos);
        sos.flush();

        IOPipeThread in = new IOPipeThread(Thread.currentThread().getName() + "-in", is, sos);
        IOPipeThread out = new IOPipeThread(Thread.currentThread().getName() + "-out", sis, os);
        in.setSlave(out);
        out.setSlave(in);

        in.start();
        out.start();

        try {
            in.join();
            out.join();
        } finally {
        }

        socket.close();
        return false;
    }

    /**
     * Parse request line and retrieve the request path stripped from optional
     * request parameters.
     * 
     * @param requestLine
     * @return the path
     */
    private ByteRef extractPath() {
        int sp = requestLine.indexOf(' ');
        ByteRef path = ROOT;
        if (sp > 0) {
            sp++;
            int ep = requestLine.indexOf(' ', sp);
            if (ep > 0) {
                path = requestLine.substring(sp, ep);

                // remove parameters from path
                ep = path.indexOf('?');
                if (ep > 0) {
                    path = path.substring(0, ep);
                }

                int skp = path.indexOf(';');
                if (skp > 0) {
                    path = path.substring(0, skp);
                }

                if (path.startsWith(HTTP)) {
                    path = fixRequestLine(path, HTTP.length());
                } else if (path.startsWith(HTTPS)) {
                    path = fixRequestLine(path, HTTPS.length());
                } else if (path.startsWith(SHTTP)) {
                    path = fixRequestLine(path, SHTTP.length());
                } else if (path.startsWith(SHTTPS)) {
                    path = fixRequestLine(path, SHTTPS.length());
                }
            }
        }
        return path;
    }

    private ByteRef fixRequestLine(ByteRef path, int length) {
        // fix path
        path = path.substring(length);
        host = path.nextWord('/');
        path = ROOT.append(path);

        // fix request line
        ByteRef cmd = requestLine.nextWord();
        requestLine = requestLine.substring(length);
        requestLine.nextWord('/');
        requestLine = cmd.append(SPACE).append(ROOT).append(requestLine);

        if (DEBUG)
            System.out.println("==>> " + requestLine);

        return path;
    }

    /**
     * read the request header and get the HOST: if specified.
     * 
     * @param br
     *            input buffer
     * @param requestHeader
     *            a HashMap to keep the values
     * @return true on success
     */
    private boolean readRequestHeaders(ByteRef br, Map<ByteRef, ByteRef> requestHeader) throws IOException {
        host = null;
        inContentLength = -1;
        keepAlive = false;
        // connection = null;
        for (;;) {
            ByteRef line = readLine(br);
            if (line == null)
                return false;
            if (line.length() == 0)
                break;

            // extract the key word and make it uppercase
            int dp = line.indexOf(':');

            // handle it
            if (dp > 0) {
                ByteRef key = line.substring(0, dp).toUpperCase();
                ByteRef val = line.substring(dp + 2); // skip ': '
                if (DEBUG)
                    System.out.println(key + ": " + val);
                if (key.equals(HOST)) {
                    host = val;
                } else if (key.equals(CONTENT_LENGTH)) {
                    inContentLength = val.toLong();
                } else if (key.equals(CONNECTION)) {
                    // connection = val;
                    keepAlive = KEEPALIVE.equalsIgnoreCase(val);
                }
                requestHeader.put(key, val);
            }
        }
        return true;
    }

    /**
     * copy the response header and get the HOST: if specified.
     * 
     * @param br
     *            input buffer
     * @param mappedPath
     * @param orgPath
     * @param logBuffer
     *            output buffer
     * @return true on success
     */
    private MultiMap<ByteRef, ByteRef> readResponseHeaders(ByteRef br, InputStream in, OutputStream out) throws IOException {
        outContentLength = -1;
        chunked = false;
        contentType = null;
        contentEncoding = null;
        final MultiMap<ByteRef, ByteRef> headers = new MultiMap<ByteRef, ByteRef>();

        for (;;) {
            final ByteRef line = ByteRef.readLine(br, in);
            if (line == null)
                return null;
            if (line.length() == 0)
                break;

            // extract the key word and make it uppercase
            int dp = line.indexOf(':');

            // handle it
            if (dp > 0) {
                // it's ok to uppercase all the headers
                ByteRef key = line.substring(0, dp).toUpperCase();
                ByteRef val = line.substring(dp + 2); // skip ': '
                if (DEBUG)
                    System.out.println(key + ": " + val);
                if (key.equals(CONTENT_LENGTH)) {
                    outContentLength = val.toLong();
                } else if (key.equals(CONNECTION)) {
                    keepAlive &= !CLOSE.equalsIgnoreCase(val);
                } else if (key.equals(TRANSFER_ENCODING)) {
                    chunked = CHUNKED.equalsIgnoreCase(val);
                } else if (key.equals(CONTENT_TYPE)) {
                    contentType = val;
                } else if (key.equals(CONTENT_ENCODING)) {
                    contentEncoding = val;
                } else if (key.equals(LOCATION)) {
                    // try to translate the location
                    ByteRef location = translateLocation(val, orgPath, mappedPath);
                    if (location != null)
                        val = location;
                } else if (key.equals(SET_COOKIE)) {
                    ByteRef v2 = new ByteRef(val.toByteArray()).toUpperCase();
                    int pathPos = v2.indexOf(COOKIE_PATH);
                    while (pathPos > 0 && !v2.substring(0, pathPos).trim().endsWith(";")) {
                        pathPos = v2.indexOf(COOKIE_PATH, pathPos + 5);
                    }
                    // either patch the path
                    if (pathPos > 0) {
                        pathPos += COOKIE_PATH.length();
                        int end = val.indexOf(';', pathPos);
                        if (end < 0)
                            end = val.length();
                        ByteRef path = val.substring(pathPos, end);
                        if (path.startsWith(mappedPath)) {
                            // replace mapped path
                            val = val.substring(0, pathPos).append(orgPath).append(path.substring(mappedPath.length())).append(val.substring(end));
                        } else {
                            // prepend mapped path
                            val = val.substring(0, pathPos).append(orgPath).append(path).append(val.substring(end));
                        }
                    } else {
                        // or add a path
                        val = val.append(";Path=").append(orgPath);
                    }
                }
                headers.put(key, val);
            }
        }

        if (!keepAlive)
            headers.put(CONNECTION, CLOSE);

        return headers;
    }

    private ByteRef translateLocation(ByteRef location, ByteRef orgPath, ByteRef mappedPath) {
        boolean useHttps = false;
        if (location.startsWith(HTTP)) {
            location = location.substring(HTTP.length());
        } else if (location.startsWith(HTTPS)) {
            location = location.substring(HTTPS.length());
            useHttps = true;
        } else {
            // location without host
            if (!location.startsWith(mappedPath))
                return null;

            return orgPath.append(location.substring(mappedPath.length()));
        }

        if (DEBUG) {
            System.out.println("location:" + location);
            System.out.println("currentDest:" + currentDest);
            System.out.println("host:" + host);
        }

        if (!location.startsWith(currentDest) && !location.startsWith(host))
            return null;

        // remove host part
        location.nextWord('/');

        // translate port back
        int port = server.getPort();
        useHttps = server.usesSsl();

        location = ROOT.append(location);
        if (location.startsWith(mappedPath))
            location = orgPath.append(location.substring(mappedPath.length()));
        if (location.charAt(0) == '/')
            location = location.substring(1);

        if (DEBUG)
            System.out.println(host + " " + useHttps + " -> " + location + " " + this.server.usesSsl() + " " + port + "=" + currentDestPort);

        ByteRef translated = useHttps ? HTTPS : HTTP;

        translated = translated.append(host);
        if ((!useHttps && port != 80) || (useHttps && port != 443))
            translated = translated.append(":" + port);
        translated = translated.append(ROOT).append(location);

        if (DEBUG)
            System.out.println("location: " + translated);

        return translated;
    }

    private ByteRef copyResponseLine() throws IOException {
        ByteRef response = ByteRef.readLine(responseBuffer, is2Forward);
        if (response == null || response.length() == 0)
            return null;

        if (DEBUG)
            System.out.println(response);
        if (response.startsWith(HTTP10))
            keepAlive = false;
        response.writeTo(os);
        os.write(CRLF);
        return response;
    }

    /**
     * Translate the locations for all keywords.
     * 
     * @param data
     *            the data to patch
     * @param sKeyWord
     *            the keyword - an xml parameter inlcuding =, e.g. "location="
     * @param mappedPath
     * @param orgPath
     * @return the patched data
     */
    private ByteRef doPatchXML(ByteRef data, String sKeyWord) {
        ByteRef keyWord = new ByteRef(sKeyWord);

        for (int pos = data.indexOf(keyWord); pos > 0; pos = data.indexOf(keyWord, pos)) {
            pos += keyWord.length();
            int quote = data.charAt(pos++); // get " or '
            int end = data.indexOf(quote, pos);
            ByteRef location = data.substring(pos, end);
            location = translateLocation(location, orgPath, mappedPath);
            if (location != null) {
                data = data.substring(0, pos).append(location).append(data.substring(end));
                pos += location.length();
            } else {
                pos = end;
            }
        }
        return data;
    }
}
