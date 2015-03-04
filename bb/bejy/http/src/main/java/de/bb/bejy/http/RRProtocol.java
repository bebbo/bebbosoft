/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/RRProtocol.java,v $
 * $Revision: 1.35 $
 * $Date: 2014/06/23 15:38:46 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * HTTP redirector protocol
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map.Entry;

import de.bb.bejy.Config;
import de.bb.bejy.IOPipeThread;
import de.bb.bejy.Protocol;
import de.bb.bejy.UserGroupDbi;
import de.bb.bejy.Version;
import de.bb.io.FastBufferedOutputStream;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.Misc;

class RRProtocol extends Protocol {
    private ByteRef responseBuffer;

    private long outContentLength;

    // private ByteRef connection;
    private boolean keepAlive;

    private boolean chunked;

    private long inContentLength;

    private ByteRef host;

    private boolean DEBUG;

    // private LogFile logFile;
    RRFactory factory;

    private final static ByteRef HOST = new ByteRef("HOST");

    private final static ByteRef CONTENTLENGTH = new ByteRef("CONTENT-LENGTH");

    // private final static ByteRef LOCATION = new ByteRef("LOCATION");
    private final static ByteRef CONNECTION = new ByteRef("CONNECTION");

    private final static ByteRef TRANSFER_ENCODING = new ByteRef("TRANSFER-ENCODING");

    private final static ByteRef LOCATION = new ByteRef("LOCATION");
    private final static ByteRef CS = new ByteRef(": ");

    private final static ByteRef HTTP = new ByteRef("http://");
    private final static ByteRef HTTPS = new ByteRef("https://");
    private final static ByteRef SHTTP = new ByteRef("/http:/");
    private final static ByteRef SHTTPS = new ByteRef("/https:/");

    private final static ByteRef KEEPALIVE = new ByteRef("KEEP-ALIVE");

    private final static ByteRef CLOSE = new ByteRef("CLOSE");

    private final static ByteRef CHUNKED = new ByteRef("CHUNKED");

    private final static ByteRef HTTP10 = new ByteRef("HTTP/1.0");

    private final static ByteRef ROOT = new ByteRef("/");

    private final static byte[] CONNECTION_CLOSE = "CONNECTION: close\r\n".getBytes();

    private final static ByteRef XML = new ByteRef("<?xml");

    private final static ByteRef STREAM = new ByteRef("<stream:");

    private final static byte[] CRLF = {0xd, 0xa};

    private static final ByteRef AUTHORIZATION = new ByteRef("AUTHORIZATION");

    private static final ByteRef SPACE = new ByteRef(" ");

    final byte b[] = new byte[8192];

    // java.io.ByteArrayOutputStream bos;

    RRProtocol(RRFactory hf, LogFile _logFile) {
        super(hf);
        factory = hf;
        DEBUG = hf.getBooleanProperty("verbose", false);
        // logFile = _logFile;
        // bos = new java.io.ByteArrayOutputStream();
    }

    InputStream is;

    OutputStream os;

    private String currentDest;

    private int currentDestPort;

    private String forwardHost;

    private ByteRef orgPath;

    private ByteRef mappedPath;

    private ByteRef requestLine;

    public boolean trigger() throws Exception {
        is = getIs();
        os = new FastBufferedOutputStream(getOs(), 1400);
        return super.trigger();
    }

    /**
     * handle incoming data first byte is already read read the rest
     */

    protected boolean doit() throws Exception {
        RREntry lastDestination = null;
        java.net.Socket socket = null;
        InputStream is2 = null;
        OutputStream os2 = null;

        responseBuffer = new ByteRef();

        try {

            // get request line
            ByteRef requestBuffer = readFirst();

            if (factory.socks5Group != null && requestBuffer.charAt(0) == 5) {
                socks5Connect(requestBuffer);
                return false;
            }

            if (factory.jabberPort != 0 && (requestBuffer.startsWith(XML) || requestBuffer.startsWith(STREAM))) {
                jabberConnect(requestBuffer);
                return false;
            }

            for (;;) {
                if (DEBUG) {
                    System.out.println("=================" + this);
                }
                requestLine = readLine(requestBuffer);
                while (requestLine == null || requestLine.length() == 0) {
                    requestBuffer = requestBuffer.update(is);
                    if (requestBuffer == null)
                        return false;
                    requestLine = readLine(requestBuffer);
                }

                // check for proy CONNECT
                if (requestLine.startsWith("CONNECT")) {
                    if (!proxyConnect(requestLine, requestBuffer))
                        return false;

                    continue;
                }

                if (DEBUG)
                    System.out.println("<" + requestLine + ">");

                HashMap<ByteRef, ByteRef> requestHeader = new HashMap<ByteRef, ByteRef>();
                if (!copyRequestHeader(requestBuffer, requestHeader))
                    break;

                ByteRef path = extractPath();
                if (DEBUG)
                    System.out.println("lookup for PATH=" + path);

                if (host == null) {
                    // get the host name from the path
                    int sp = path.indexOf('/');
                    if (sp > 0) {
                        sp += 2;
                        int ep = path.indexOf('/', sp);
                        host = path.substring(sp, ep);
                        path = path.substring(ep);
                    }
                    if (host == null)
                        break;
                    sp = host.indexOf(':');
                    if (sp > 0)
                        host = host.substring(0, sp);
                }

                if (DEBUG)
                    System.out.println("lookup for HOST=" + host);

                if (host == null)
                    break; // no host -> error

                // get the forward entry
                RREntry destination = factory.getRREntry(host, path);
                if (destination == null)
                    break;

                UserGroupDbi group = Config.getGroup(destination.group);
                if (group != null) {

                    String user = null;
                    String pass = null;

                    ByteRef auth = (ByteRef) requestHeader.remove(AUTHORIZATION);
                    if (auth != null) {
                        ByteRef basic = auth.nextWord();
                        if (basic.equalsIgnoreCase("basic")) {
                            //parse the auth and use the group to verify
                            byte b[] = Mime.decode(auth.toByteArray(), 0, auth.length());
                            if (b != null) {
                                ByteRef line = new ByteRef(b);
                                user = line.nextWord(':').toString();
                                pass = line.toString();
                            }
                        }
                    }

                    if (user == null || pass == null || group.verifyUserGroup(user, pass) != null) {
                        // send 401
                        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
                        ByteUtil.writeString("HTTP/1.0 401 Unauthorized\r\n", bos);
                        ByteUtil.writeString("WWW-Authenticate: Basic realm=\"" + destination.group + "\"\r\n", bos);
                        ByteUtil.writeString("Content-Length: 0\r\n", bos);
                        ByteUtil.writeString("Connection: close, close\r\n\r\n", bos);
                        os.write(bos.toByteArray());
                        os.flush();
                        break;
                    }

                    if (destination.userHeader != null && destination.userHeader.length() > 0)
                        requestHeader.put(new ByteRef(destination.userHeader), new ByteRef(user));
                }

                // did the destination change?
                if (lastDestination != destination) {
                    if (DEBUG)
                        System.out.println("closing socket");
                    // close old
                    try {
                        if (socket != null)
                            socket.close();
                    } catch (IOException ex) {
                    }
                    os2 = null;
                    is2 = null;
                    socket = null;
                    lastDestination = destination;
                }

                if (DEBUG)
                    System.out.println("got destination: " + destination);

                // get the URI to forward to
                String forwardUrl = factory.getUri(remoteAddress, destination);
                if (forwardUrl == null)
                    break;

                try {
                    int slash = forwardUrl.indexOf('/');
                    if (slash == -1)
                        slash = forwardUrl.length();

                    forwardHost = forwardUrl.substring(0, slash);

                    if (DEBUG)
                        System.out.println("redirect to:" + forwardHost);

                    int dp = forwardHost.indexOf(':');

                    // open new connection if necessary
                    if (socket == null) {
                        currentDest = forwardHost.substring(0, dp);
                        currentDestPort = Integer.parseInt(forwardHost.substring(dp + 1));
                        socket = new java.net.Socket(currentDest, currentDestPort);
                        int timeout = server.getTimeout();
                        socket.setSoTimeout(timeout);
                        // get other streams
                        is2 = socket.getInputStream();
                        os2 = socket.getOutputStream();
                    }

                    orgPath = destination.path;
                    if (orgPath.endsWith(ROOT))
                        orgPath = orgPath.substring(orgPath.length() - 1);

                    // buffer for the response
                    FastByteArrayOutputStream bos = new FastByteArrayOutputStream();

                    //                    // send redirect for the folder if path does not end with /
                    //                    if (path.equals(orgPath)) {
                    //                        ByteUtil.writeString("HTTP/1.0 302 location found\r\nLocation: ", bos);
                    //                        path.writeTo(bos);
                    //                        ByteUtil.writeString("/\r\n\r\n", bos);
                    //                        os.write(bos.toByteArray());
                    //                        break;
                    //                    }

                    mappedPath = new ByteRef(forwardUrl);
                    if (slash < 0) {
                        mappedPath = ROOT;
                    } else {
                        mappedPath = mappedPath.substring(slash);
                        if (mappedPath.endsWith(ROOT))
                            mappedPath = mappedPath.substring(0, mappedPath.length() - 1);
                    }

                    // change the url
                    ByteRef rewriteRequestLine = rewriteRequestLine(requestLine);
                    if (DEBUG) {
                        System.out.println(requestLine + " => " + rewriteRequestLine);
                    }
                    rewriteRequestLine.writeTo(bos);

                    // add information to suggest the hidden instance the port and
                    // remote host.
                    bos.write(CRLF);
                    bos.write(("B-REMOTEHOST: " + remoteAddress).getBytes());
                    bos.write(CRLF);
                    bos.write(("B-SERVERPORT: " + server.getPort()).getBytes());
                    bos.write(CRLF);
                    if (server.usesSsl()) {
                        bos.write("B-SECURE: true".getBytes());
                        bos.write(CRLF);
                    }

                    writeHeader(bos, requestHeader);

                    // send header to forwarded server
                    os2.write(bos.toByteArray());

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
                                int sz = inContentLength > requestBuffer.length() ? requestBuffer.length() : (int)inContentLength;
                                requestBuffer.substring(0, sz).writeTo(os2);
                                requestBuffer = requestBuffer.substring(sz);
                                inContentLength -= sz;
                            }
                        }
                    }

                    os2.flush();

                    // read the response
                    int lspace = requestLine.lastIndexOf(' ');
                    boolean patchWSDL = requestLine.substring(0, lspace).toLowerCase().endsWith("?wsdl");
                    sendResponse(is2, os, patchWSDL);
                    os.flush();

                    // exit loop if necessary
                    if (!keepAlive)
                        break;
                } catch (ConnectException ce) {
                    factory.getLogFile().writeDate("failed to connect to: " + forwardUrl);
                }
            }
        } catch (SocketException se) {
            // ignore
        } catch (IOException ioe) {
            if (DEBUG)
                ioe.printStackTrace();
            factory.getLogFile().writeDate("IO Exception: " + ioe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.flush();
                os.close();
            } catch (Exception e1) {
            }
            try {
                is.close();
            } catch (Exception e1) {
            }
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e1) {
            }
        }
        if (DEBUG)
            System.out.println("closed");

        return false;
    }

    private void writeHeader(FastByteArrayOutputStream bos, HashMap<ByteRef, ByteRef> requestHeader) throws IOException {
        for (Entry<ByteRef, ByteRef> e : requestHeader.entrySet()) {
            //            if (HOST.equals(e.getKey())) {
            //                HOST.writeTo(bos);
            //                bos.write(':');
            //                bos.write(' ');
            //                ByteUtil.writeString(forwardHost, bos);
            //                bos.write(CRLF);
            //                continue;
            //            }
            e.getKey().writeTo(bos);
            bos.write(':');
            bos.write(' ');
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
                dest =
                        requestBuffer.charAt(0) + "." + requestBuffer.charAt(1) + "." + requestBuffer.charAt(2) + "."
                                + requestBuffer.charAt(3);
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
                dest =
                        hb(requestBuffer.charAt(0), requestBuffer.charAt(1)) + ":"
                                + hb(requestBuffer.charAt(2), requestBuffer.charAt(3)) + ":"
                                + hb(requestBuffer.charAt(4), requestBuffer.charAt(5)) + ":"
                                + hb(requestBuffer.charAt(6), requestBuffer.charAt(7)) + ":"
                                + hb(requestBuffer.charAt(8), requestBuffer.charAt(9)) + ":"
                                + hb(requestBuffer.charAt(10), requestBuffer.charAt(11)) + ":"
                                + hb(requestBuffer.charAt(12), requestBuffer.charAt(13)) + ":"
                                + hb(requestBuffer.charAt(14), requestBuffer.charAt(15));
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
     * Parse request line and retrieve the request path stripped from optional request parameters.
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
     * copy the request header and get the HOST: if specified.
     * 
     * @param br
     *            input buffer
     * @param requestHeader
     *            output stream
     * @return true on success
     */
    private boolean copyRequestHeader(ByteRef br, HashMap<ByteRef, ByteRef> requestHeader) throws IOException {
        host = null;
        inContentLength = -1;
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
                } else if (key.equals(CONTENTLENGTH)) {
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
    private boolean copyResponseHeader(ByteRef br, InputStream in, OutputStream out) throws IOException {
        outContentLength = -1;
        chunked = false;
        for (;;) {
            ByteRef line = ByteRef.readLine(br, in);
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
                if (key.equals(CONTENTLENGTH)) {
                    outContentLength = val.toLong();
                } else if (key.equals(CONNECTION)) {
                    // connection = val;
                    keepAlive &= !CLOSE.equalsIgnoreCase(val);
                } else if (key.equals(TRANSFER_ENCODING)) {
                    chunked = CHUNKED.equalsIgnoreCase(val);
                } else if (key.equals(LOCATION)) {
                    // try to translate the location
                    ByteRef location = translateLocation(val, orgPath, mappedPath);
                    if (location != null)
                        line = LOCATION.append(CS).append(location);
                }
            }

            line.writeTo(out);
            out.write(CRLF);

        }
        if (!keepAlive)
            out.write(CONNECTION_CLOSE);

        // this is the emtpy line behind header
        out.write(CRLF);
        return true;
    }

    private ByteRef translateLocation(ByteRef location, ByteRef orgPath, ByteRef mappedPath) {
        boolean useHttps = false;
        if (location.startsWith(HTTP)) {
            location = location.substring(HTTP.length());
        } else if (location.startsWith(HTTPS)) {
            location = location.substring(HTTPS.length());
            useHttps = true;
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
            System.out.println(host + " " + useHttps + " -> " + location + " " + this.server.usesSsl() + " " + port
                    + "=" + currentDestPort);
        if (useHttps) {
            location = HTTPS.append(host).append(ROOT).append(location);
        } else {
            location = HTTP.append(host).append(ROOT).append(location);
        }
        if (DEBUG)
            System.out.println("location:" + location);

        return location;
    }

    /**
     * Deliver the reply.
     * 
     * @param in
     *            the InputStream, reading from the forwarded server
     * @param out
     *            the OutputStream, writing to the client
     * @param orgPath
     *            the path used by the client
     * @param mappedPath
     *            the path used to redirect
     * @param c
     */
    private void sendResponse(InputStream in, OutputStream out, boolean patchWSDL) throws IOException {

        ByteRef response = ByteRef.readLine(responseBuffer, in);
        if (response == null || response.length() == 0)
            throw new IOException("no response in: " + responseBuffer);

        if (DEBUG)
            System.out.println(response);
        if (response.startsWith(HTTP10))
            keepAlive = false;
        response.writeTo(out);
        out.write(CRLF);

        copyResponseHeader(responseBuffer, in, out);

        response.nextWord();
        ByteRef rc = response.nextWord();
        int rcode = rc.toInteger();

        if (rcode != 304) {
            if (chunked) {
                // if WSDL needs a patch, collect the data and don't write em
                ByteRef output = new ByteRef();
                for (;;) {
                    ByteRef sizeLine;
                    for (;;) {
                        sizeLine = responseBuffer.nextLineCRLF();
                        if (sizeLine == null)
                            responseBuffer.update(in);
                        if (sizeLine == null)
                            continue;
                        if (sizeLine.length() > 0)
                            break;
                        out.write(CRLF);
                    }

                    if (!patchWSDL) {
                        sizeLine.writeTo(out);
                        out.write(CRLF);
                    }

                    int len = Integer.parseInt(sizeLine.toString(), 16);
                    if (len == 0) {
                        if (!patchWSDL)
                            responseBuffer.writeTo(out);
                        responseBuffer = new ByteRef();
                        break;
                    }

                    while (responseBuffer.length() < len) {
                        responseBuffer.update(in);
                    }
                    ByteRef chunk = responseBuffer.splitLeft(len + 2);
                    if (patchWSDL) {
                        output = output.append(chunk.substring(0, chunk.length() - 2));
                    } else {
                        chunk.writeTo(out);
                    }
                }

                // data to patch was collected in output - now patch it
                if (patchWSDL) {
                    output = doPatchWSDL(output, "schemaLocation=");
                    output = doPatchWSDL(output, "location=");

                    // write the patched data chunked
                    for (;;) {
                        int len = output.length() > 8192 ? 8192 : output.length();
                        String hex = Integer.toHexString(len);
                        while (hex.length() < 4) {
                            hex = "0" + hex;
                        }
                        ByteUtil.writeString(hex, out);
                        out.write(CRLF);
                        if (len == 0)
                            break;

                        ByteRef chunk = output.splitLeft(len);
                        chunk.writeTo(out);
                        out.write(CRLF);
                    }
                    out.write(CRLF);
                }

            } else if (outContentLength < 0) {
                if (DEBUG)
                    System.out.println("copy to EOF: ");
                while (responseBuffer != null) {
                    if (responseBuffer.length() > 0) {
                        responseBuffer.writeTo(out);
                        responseBuffer = new ByteRef();
                    }
                    responseBuffer = responseBuffer.update(in);
                }
            } else {
                while (outContentLength > 0) {
                    if (DEBUG)
                        System.out.println("needed: " + outContentLength);

                    while (responseBuffer != null && responseBuffer.length() == 0) {
                        responseBuffer = responseBuffer.update(in);
                    }

                    if (responseBuffer == null)
                        throw new IOException("not enough data");

                    int sz = outContentLength > responseBuffer.length() ? responseBuffer.length() : (int)outContentLength;

                    if (DEBUG)
                        System.out.println("copy: " + sz);

                    responseBuffer.substring(0, sz).writeTo(out);
                    responseBuffer = responseBuffer.substring(sz);
                    outContentLength -= sz;
                }
            }
        }
        if (DEBUG)
            System.out.println("done");
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
    private ByteRef doPatchWSDL(ByteRef data, String sKeyWord) {
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

/******************************************************************************
 * $Log: RRProtocol.java,v $
 * Revision 1.35  2014/06/23 15:38:46  bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles
 *
 * Revision 1.34  2013/11/23 10:36:19  bebbo
 * @R contentLength supports long values now
 *
 * Revision 1.33  2013/05/17 10:50:52  bebbo
 * @I more DEBUG logging
 * Revision 1.32 2012/12/10 22:20:46 bebbo
 * 
 * @N added authentication for forwarded paths: configure group and optional user header Revision 1.31 2012/12/09
 *    20:15:42 bebbo
 * 
 * @B various url rewrite enhancements Revision 1.30 2012/11/14 15:09:57 bebbo
 * 
 * @B forward host is no longer patched
 * @D added some debug printlns Revision 1.29 2012/11/13 06:36:14 bebbo
 * 
 * @R improved support for URL rewrite Revision 1.28 2012/11/08 12:14:17 bebbo
 * 
 * @B fixed proxy with HTTP chunked mode
 * @N added SOCKS5 proxy support
 * @N added a fallback option for XML data -> XMPP server Revision 1.27 2012/08/11 16:59:55 bebbo
 * 
 * @B redirect now handles "chunked"
 * @B 301-304 do not return a response -> no longer waiting for it
 * @R using the IOPipeThread from bejy server Revision 1.26 2011/09/16 16:19:32 bebbo
 * 
 * @D disabled DEBUG Revision 1.25 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.24 2010/07/08 18:16:25 bebbo
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 *    Revision 1.23 2006/02/06 09:16:44 bebbo
 * @I cleanup
 * 
 *    Revision 1.22 2005/12/11 20:27:46 bebbo
 * @D removed printStackTrace () on broken connections
 * 
 *    Revision 1.21 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.20 2004/03/23 12:28:28 bebbo
 * @O removed a redundant condition
 * @B fixed possible NPE
 * 
 *    Revision 1.19 2004/01/09 19:36:24 bebbo
 * @B rewrote and now working without reader thread
 * 
 *    Revision 1.18 2003/08/04 08:35:48 bebbo
 * @R modified redirector (removed uri substitution) and rolled back to last working version
 * 
 *    Revision 1.17 2003/07/30 10:09:49 bebbo
 * @R enahanced information in admin interface
 * 
 *    Revision 1.16 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.15 2003/03/26 19:44:05 bebbo
 * @B fixed trigger function of RRProtocol
 * 
 *    Revision 1.14 2003/02/25 07:01:56 bebbo
 * @R protocols are using now BufferedOutputStreams
 * 
 *    Revision 1.13 2002/01/26 15:26:53 bebbo
 * @B fixed connection closing time
 * 
 *    Revision 1.12 2002/01/22 21:31:49 bebbo
 * @I using a pipe thread again.
 * 
 *    Revision 1.11 2002/01/21 17:23:31 franke
 * @B fixed EOS detection
 * 
 *    Revision 1.10 2001/12/20 23:40:46 bebbo
 * @B since a read can not longer be interrupted, a sleep loop is added
 * 
 *    Revision 1.9 2001/10/09 08:00:49 bebbo
 * @B fixed broken forward of header
 * 
 *    Revision 1.8 2001/10/08 20:35:25 bebbo
 * @R removed additional thread to pipe the data
 * 
 *    Revision 1.7 2001/09/21 14:41:19 bebbo
 * @B fixes for InputStream.read() and Thread.interrupt() problem.
 * 
 *    Revision 1.6 2001/09/15 08:48:11 bebbo
 * @I removed dd.bb.io.*
 * 
 *    Revision 1.5 2001/08/24 08:24:26 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.4 2001/04/16 20:04:13 bebbo
 * @B B-params are now send at begin of header
 * 
 *    Revision 1.3 2001/04/11 15:46:40 bebbo
 * @N added information for real TCP port
 * 
 *    Revision 1.2 2001/04/06 05:53:39 bebbo
 * @N working round robin dispatcher
 * 
 *    Revision 1.1 2001/04/02 16:14:35 bebbo
 * @N new
 * 
 *****************************************************************************/
