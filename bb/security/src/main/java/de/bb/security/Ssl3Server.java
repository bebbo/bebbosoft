package de.bb.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.bb.util.Misc;

/**
 * This class is designed to ...
 * 
 * @author bebbo
 */
public class Ssl3Server extends Ssl3 {
    
    private byte[] preMasterSecret;

    private byte[] firstChunk;

    private String hostName;

    private boolean secureRenegotiation;

    private boolean sessionTicket;

    private Ssl3Config config;

    private boolean useDHE;

    /**
     * Create a SSL3Server with the default cipher suites.
     * 
     * @param certificate
     *            a byte array with the certificate(s)
     * @param pkData
     *            8 byte arrays with the private key data (n, e, d, p, q, dp1, dq1, iqmp)
     * @throws IOException
     */
    public Ssl3Server(byte[][] certificates, byte[][] pkData) throws IOException {
        this(certificates, pkData, CIPHERSUITES);
    }

    /**
     * Create a SSL3Server with the specified cipher suites.
     * 
     * @param certificate
     *            a byte array with the certificate(s)
     * @param pkData
     *            8 byte arrays with the private key data (n, e, d, p, q, dp1, dq1, iqmp)
     * @param ciphersuites
     *            a distinct set of ciphersuites
     * @throws IOException
     */
    public Ssl3Server(byte[][] certificates, byte[][] pkData, byte[][] ciphersuites) throws IOException {
        this(new Ssl3Config(certificates, pkData, ciphersuites));
    }

    public Ssl3Server(Ssl3Config config) throws IOException {
        super(config.getCiphersuites());
        this.config = config;
    }

    /**
     * Waits for a clientHello does some pre checks and returns.
     */
    void listen() throws IOException {
        try {
            collect = true;
            firstChunk = r5;
            // ======================================
            // start of handshake - determine method
            byte b[] = hs_read(1);
            if (b == null)
                throw new SslException(clientSessionId, "EOS detected");

            if (DEBUG.ON)
                Misc.dump("client hello", System.out, b);
            int ct[] = parseClientHello3(b); // set the client-hello-random

            // clear it since the data is processed now.
            firstChunk = null;

            cipherIndex = -1;
            for (int j = 0; j < ciphersuites.length; ++j) {
                int val = ciphersuites[j][0] << 8 | (ciphersuites[j][1] & 0xff);
                for (int i = 0; i < ct.length; ++i) {
                    if (ct[i] == val) {
                        cipherIndex = j;
                        break;
                    }
                }
                if (cipherIndex >= 0)
                    break;
            }
            if (cipherIndex < 0)
                throw new SslException(clientSessionId, "no common cipher suite");

            // dump(clientRandom);
            if (DEBUG.ON)
                System.out.println("using: " + getCipherSuite());

            if (DEBUG.ON)
                Misc.dump("hashing handshake messages", System.out, pendingHandshake);

            if (versionMinor == 3) {
                hsSha = new SHA256();
                hsSha.update(pendingHandshake, 0, pendingHandshake.length);
            } else {
                hsMd5 = new MD5();
                hsSha = new SHA();
                hsMd5.update(pendingHandshake, 0, pendingHandshake.length);
                hsSha.update(pendingHandshake, 0, pendingHandshake.length);
            }
            pendingHandshake = NULLBYTES;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (DEBUG.ON)
                e.printStackTrace();
            throw new SslException(clientSessionId, "server handshake: " + e.toString());
        }
    }

    /**
     * Method Performs a shortened handshake, coz it is reused.
     * 
     * @param is
     * @param os
     * @param other
     *            the Ssl3ServerImpl which listened and received the ClientHello
     */
    void resume(byte[] oldSecret) throws IOException {
        try {
            sessionId = clientSessionId.data;
            masterSecret = oldSecret;

            collect = true;

            // disable encryption
            rhashBuffer = null;
            readnum = writenum = 0;

            // send server hello
            int len = newServerHello3(cipherIndex); // set the server-hello-random
            os.write(writeBuffer, 0, len);

            // enable write encryption
            createKeys(true);

            // send change cipherspec
            os.write(css);

            byte[] b = serverFinishedHash();
            b[0] = 20;
            b[3] = (byte) (b.length - 4);
            rawwrite(b, 22);
            os.flush();

            updateHandshakeHashes(b, 0, b.length);

            // get change cipherspec
            rawread(onebyte, 1, 20);
            if (onebyte[0] != 1)
                throw new Exception("got no change cipherspec");
            // enable read encryption
            rhashBuffer = new byte[hashLen];

            byte h[] = clientFinishedHash();

            // get handshake finish masterSecret
            int hsLen = versionMinor > 0 ? 16 : 40;
            b = new byte[hsLen];
            rawread(b, hsLen, 22);
            // dump(b);

            if (!equals(b, 4, h, 4, h.length - 4))
                throw new SslException(clientSessionId, "Client Finished failed");

            collect = false;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SslException(clientSessionId, "server resume: " + e.toString());
        }
    }

    /**
     * Performs a full handshake.
     */
    void handshake(InputStream _is, OutputStream _os) throws IOException {
        setStreams(_is, _os);
        try {

            // mark if DHE is used
            if (ciphersuites[cipherIndex][5] == 1) {
                useDHE = true;
            }

            collect = true;
            // ======================================
            // send server hello + certificate + server done
            byte[][] pkData = config.getKeyData(hostName);
            int len = newServerHello3(cipherIndex, config.getCertificates(hostName), pkData); // set the server-hello-random
            os.write(writeBuffer, 0, len);
            os.flush();
            if (DEBUG.ON)
                Misc.dump("hello + cert + (key) + done", System.out, writeBuffer, 0, len);

            // get key exchange message -> pmasterSecret
            byte[] b = hs_read(16);
            if (b == null)
                throw new SslException(clientSessionId, "no key exchange");

            // use RSA
            if (useDHE) {
                // DHE
                byte x[] = new byte[b.length - 2];
                System.arraycopy(b, 2, x, 0, x.length);
                preMasterSecret = Pkcs6.doRSA(x, pkData[8], preMasterSecret);
            } else {

                if (versionMinor != 0) {
                    int nl = ((0xff & b[0]) << 8) | (0xff & b[1]);
                    byte t[] = new byte[nl];
                    System.arraycopy(b, 2, t, 0, t.length);
                    b = t;
                }

                b = Pkcs6.doRSA(b, pkData);
                preMasterSecret = new byte[48];

                int i = 2;
                for (; i < b.length; ++i) {
                    if (b[i] == 0)
                        break;
                }
                
                boolean wrong = (b[0] != 0 || b[1] != 2 || b[i] != 0);

                ++i;
                if (versionMinor != 0) {
                    wrong |= (b[i] != 3 || b[i + 1] != versionMinor);
                }
                
                // copy only if it's ok - handshake check will fail, no exception here
                if (!wrong)
                	System.arraycopy(b, i, preMasterSecret, 0, preMasterSecret.length);
            }

            // get change cipherspec
            rawread(b, 1, 20);

            // enable read encryption
            rhashBuffer = new byte[hashLen];
            readnum = 0;
            // dump(x);

            if (DEBUG.ON) {
                Misc.dump("preMasterSecret", System.out, preMasterSecret);
                Misc.dump("clientRandom", System.out, clientRandom);
                Misc.dump("serverRandom", System.out, serverRandom);
            }

            // calulate the master secret
            if (versionMinor != 0)
                masterSecret = PRF(48, preMasterSecret, "master secret", clientRandom, serverRandom);
            else
                masterSecret = makeHashBytes(preMasterSecret, 48, clientRandom, serverRandom);

            if (DEBUG.ON) {
                Misc.dump("masterSecret", System.out, masterSecret);
            }

            // calculate the keys
            createKeys(true);

            byte h[] = clientFinishedHash();

            // get handshake finish masterSecret
            int hsLen = versionMinor > 0 ? 16 : 40;
            b = new byte[hsLen];
            rawread(b, hsLen, 22);
            if (DEBUG.ON) {
                Misc.dump("client finished hash", System.out, b);
            }

            if (!equals(b, 4, h, 4, h.length - 4))
                throw new SslException(clientSessionId, "Client Finished failed");

            if (sessionTicket) {
                len = newSessionTicket();
                os.write(writeBuffer, 0, len);
            }

            // send change cipherspec
            os.write(css);
            // enable write encryption
            writenum = 0;

            b = serverFinishedHash();
            b[0] = 20;
            b[3] = (byte) (b.length - 4);
            rawwrite(b, 22);
            collect = false; // disable handshake logging
            os.flush();
            // System.out.println("SSL connected");
        } catch (IOException e) {
            if (DEBUG.ON)
                System.out.println(e.getMessage());
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SslException(clientSessionId, "server handshake: " + e.toString());
        }
    }

    private int newSessionTicket() {
        int offset = 9;
        // lifetime hint
        writeBuffer[offset++] = -1;
        writeBuffer[offset++] = -1;
        writeBuffer[offset++] = -1;
        writeBuffer[offset++] = -1;

        writeBuffer[offset++] = 0;
        writeBuffer[offset++] = (byte) sessionId.length;
        System.arraycopy(sessionId, 0, writeBuffer, offset, sessionId.length);
        offset += sessionId.length;

        // header for server hello - needs the data to update hashes
        addHandshakeHeader(4, writeBuffer, 5, offset - 9);
        addMessageHeader(22, writeBuffer, 0, offset - 5);

        return offset;
    }

    // ===========================================================================
    // S E R V E R specific
    // ===========================================================================
    /**
     * parse a client hello -> get ciphertypes
     * 
     * @param b
     *            the client hello message
     * @return an array of supported cipher types
     */
    private final int[] parseClientHello3(byte b[]) throws IOException {
        int off = 0;
        int vh = b[off++];
        byte vl = b[off++];
        if (vh != 3) {
//            versionMinor = vl;
//            if (vh != 2 || versionMinor > 2)
                throw new SslException(clientSessionId, "unsupported protocol version: " + vh + ", " + versionMinor);

//            // read an SSL 2 Client hello
//            int clen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
//            off += 2;
//            int sidlen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
//            off += 2;
//            int rlen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
//            off += 2;
//            clen /= 3;
//            int ct[] = new int[clen];
//            int i = 0;
//            while (clen > 0) {
//                ct[i] = ((b[off] & 0xff) << 16) | ((b[off + 1] & 0xff) << 8) | (b[off + 2] & 0xff);
//                off += 3;
//                --clen;
//                ++i;
//            }
//            clientSessionId = new SID(versionMinor, b, off, sidlen);
//            off += sidlen;
//            if (rlen <= 32)
//                System.arraycopy(b, off, clientRandom, 32 - rlen, rlen);
//            else
//                System.arraycopy(b, off + rlen - 32, clientRandom, 0, 32);
//
//            return ct;
        }
        // is V3
        this.versionMinor = vl;
        //System.err.println(vl);
        if (versionMinor < minVersion || versionMinor > maxVersion)
            throw new SslException(clientSessionId, "unsupported protocol version: " + vh + ", " + versionMinor);

        css[2] = vl;
        // random
        System.arraycopy(b, off, clientRandom, 0, 32);
        off += 32;
        int sidlen = 0xff & b[off++];
        clientSessionId = new SID(versionMinor, b, off, sidlen);
        off += sidlen; // session id - we'll ignore the session id
        int clen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
        off += 2;
        clen >>>= 1;
        int i = 0;
        final int ct[] = new int[clen];
        while (clen > 0) {
            ct[i] = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
            if (ct[i] == 0xff) {
                secureRenegotiation = true;
            } else
            if (ct[i] == 0x5600) {
                if (versionMinor < maxVersion) {
                    os.write(ALERT86);
                    os.flush();
                    throw new IOException("inappropriate fallback detected.");
                }
            }
            off += 2;
            --clen;
            ++i;
        }
        int compLen = 0xff & b[off++];
        // ignore compression - not yet supported
        off += compLen;

        if (off < b.length) {
            // read client hello extensions
            final int extensionsLen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
            off += 2;

            if (off + extensionsLen != b.length)
                throw new SslException(clientSessionId, "client hello length mismatch");

            final int extensionsEnd = off + extensionsLen;
            while (off < extensionsEnd) {
                final int extType = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
                off += 2;
                final int extDataLen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
                off += 2;

                switch (extType) {
                case 0: // server_name
                    readServerNames(b, off);
                    break;
                case 1: // max_fragment_length
                    readMaxFragmentLength(b, off);
                    break;
                case 2: // client_certificat_url
                    readClientCertUrl(b, off);
                    break;
                case 3: // trusted_ca_keys
                    readTrustedCaKeys(b, off);
                    break;
                case 4: // truncated_hmac
                    readTruncatedHmac(b, off);
                    break;
                case 5: // status request
                    readStatusRequest(b, off);
                    break;
                case 13: // signature_algorithms
                    readSignatureAlgorithms(b, off);
                    break;
                case 0x23: // session ticket
                    // sessionTicket = true; // ignore until fully supported
                    break;
                case 0xff01:
                    secureRenegotiation = true;
                    break;
                }
                off += extDataLen;
            }
        }
        return ct;
    }

    private void readSignatureAlgorithms(byte[] b, int off) {
        int len = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
        off += 2;
        // TODO
    }

    private void readStatusRequest(byte[] b, int off) {
        // TODO Auto-generated method stub

    }

    private void readTruncatedHmac(byte[] b, int off) {
        // no support yet
    }

    private void readTrustedCaKeys(byte[] b, int off) {
        // no support yet
    }

    private void readClientCertUrl(byte[] b, int off) {
        // no support yet
    }

    private void readMaxFragmentLength(byte[] b, int off) throws IOException {
        switch (b[off]) {
        case 1:
            maxFragmentLength = (1 << 9);
            break;
        case 2:
            maxFragmentLength = (1 << 10);
            break;
        case 3:
            maxFragmentLength = (1 << 11);
            break;
        case 4:
            maxFragmentLength = (1 << 12);
            break;
        default:
            throw new SslException(clientSessionId, "invalid maximum fragment length: " + b[off]);
        }

    }

    private void readServerNames(byte[] b, int off) throws IOException {
        // list 2b len of
        int len = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
        off += 2;
        while (len > 0) {
            //   name_type 1b  0 = host_name
            final int nameType = b[off++];
            if (nameType != 0)
                throw new SslException(clientSessionId, "invalid name_type: " + nameType);
            //   hostname 2b len
            final int nameLen = ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
            off += 2;
            //     name
            hostName = new String(b, 0, off, nameLen);
            len -= nameLen + 3;
            off += nameLen;
        }
    }

    /**
     * create a server hello - for a resumed connection.
     * 
     * @param cipherIndex
     *            the used cipherIndex
     * @param pkData
     * @param cs
     * @return a new allocated byte array with server hello message data
     */
    private final int newServerHello3(int cipherIndex) {
        if (writeBuffer.length < 79)
            writeBuffer = new byte[79];

        //
        byte b[] = writeBuffer;

        b[9] = 3;
        b[10] = this.versionMinor; // version
        secureRnd.nextBytes(b, 11, 32);
        if (DEBUG.USE_TEST_DATA)
            for (int ii = 11; ii < 43; ++ii)
                b[ii] = (byte) 0xcc;
        System.arraycopy(b, 11, serverRandom, 0, 32);

        int offset = sessionTicket ? 0 : 32;
        b[43] = (byte) offset; // a session ID with 32 bytes

        if (!sessionTicket)
            System.arraycopy(sessionId, 0, b, 44, 32);

        offset += 44;

        b[offset++] = ciphersuites[cipherIndex][0];
        b[offset++] = ciphersuites[cipherIndex][1];
        b[offset++] = 0; // none

        addHandshakeHeader(2, b, 5, offset - 9);
        addMessageHeader(22, b, 0, offset - 5);
        return offset;
    }

    /**
     * create a server hello - no compression Date
     * 
     * @param cipherIndex
     *            the used cipherIndex
     * @param certs
     *            an array of byte arrays with certificates
     * @return a new allocated byte array with server hello message data
     */
    private final int newServerHello3(int cipherIndex, byte certs[][], byte[][] pkData) {
        int certsLen = 0;
        for (int i = 0; i < certs.length; ++i) {
            certsLen += certs[i].length + 3;
        }

        int extLen = calcExtLen();
        int dheLen;
        if (useDHE) {
            byte[] n = pkData[0];
            dheLen = 5 + 4 + 2 + pkData[8].length + 2 + pkData[9].length + 2 + pkData[8].length + 2 + n.length
                    - (n[0] == 0 ? 1 : 0);
            if (versionMinor >= 3)
                dheLen += 2;
        } else {
            dheLen = 0;
        }

        if (writeBuffer.length < 110 + certsLen + extLen + dheLen)
            writeBuffer = new byte[110 + certsLen + extLen + dheLen];

        // server hello [0 - 78]
        byte b[] = writeBuffer;
        secureRnd.nextBytes(b, 11, 32);
        if (DEBUG.USE_TEST_DATA)
            for (int ii = 11; ii < 76; ++ii)
                b[ii] = (byte) 0xAA;

        System.arraycopy(b, 11, serverRandom, 0, 32);

        b[9] = 3;
        b[10] = versionMinor; // version

        int offset = sessionTicket ? 0 : 32;
        b[43] = (byte) offset; // a session ID with 32 bytes

        if (sessionId == null)
            sessionId = new byte[32];

        secureRnd.nextBytes(sessionId, 0, 32);
        if (DEBUG.USE_TEST_DATA)
            for (int ii = 0; ii < 32; ++ii)
                sessionId[ii] = (byte) 0xcc;

        if (!sessionTicket)
            System.arraycopy(sessionId, 0, b, 44, 32);

        offset += 44;

        b[offset++] = ciphersuites[cipherIndex][0];
        b[offset++] = ciphersuites[cipherIndex][1];
        b[offset++] = 0; // none

        if (extLen > 0)
            offset = addExtensions(b, offset, extLen);

        // header for server hello - needs the data to update hashes
        addHandshakeHeader(2, b, 5, offset - 9);
        addMessageHeader(22, b, 0, offset - 5);

        // server certificate 
        int certStart = offset;
        offset += 9;
        b[offset++] = (byte) (certsLen >> 16);
        b[offset++] = (byte) (certsLen >> 8);
        b[offset++] = (byte) (certsLen);
        certsLen += 3; // add the 3

        for (int i = 0; i < certs.length; ++i) {
            byte c[] = certs[i];
            int l = c.length;
            b[offset] = (byte) (l >> 16);
            b[offset + 1] = (byte) (l >> 8);
            b[offset + 2] = (byte) (l);
            System.arraycopy(c, 0, b, offset + 3, c.length);
            offset += 3 + c.length;
        }

        // insert the handshake headers.
        addHandshakeHeader(11, b, certStart + 5, certsLen);
        addMessageHeader(22, b, certStart, certsLen + 4);

        // if DH is used add the dh params
        // p, g, y, signature

        if (useDHE) {
            byte[] dhp = pkData[8];
            byte[] dhg = pkData[9];
            preMasterSecret = new byte[dhp.length];
            secureRnd.nextBytes(preMasterSecret);

            byte[] y = Pkcs6.doRSA(dhg, dhp, preMasterSecret);

            int dhStart = offset;
            offset += 9;

            // copy p
            final int dhParamOffset = offset;
            b[offset++] = (byte) (dhp.length >> 8);
            b[offset++] = (byte) dhp.length;
            System.arraycopy(dhp, 0, b, offset, dhp.length);
            offset += dhp.length;

            // copy g
            b[offset++] = (byte) (dhg.length >> 8);
            b[offset++] = (byte) dhg.length;
            System.arraycopy(dhg, 0, b, offset, dhg.length);
            offset += dhg.length;

            // copy y
            b[offset++] = (byte) (y.length >> 8);
            b[offset++] = (byte) (y.length);
            System.arraycopy(y, 0, b, offset, y.length);
            offset += y.length;

            final int dhParamLength = offset - dhParamOffset;
            byte[] dhData = new byte[clientRandom.length + serverRandom.length + dhParamLength];
            System.arraycopy(clientRandom, 0, dhData, 0, clientRandom.length);
            System.arraycopy(serverRandom, 0, dhData, clientRandom.length, serverRandom.length);
            System.arraycopy(b, dhParamOffset, dhData, clientRandom.length + serverRandom.length, dhParamLength);

            if (DEBUG.ON)
                Misc.dump("to sign", System.out, dhData);

            byte n[] = pkData[0];
            int keyLength = n.length - (n[0] == 0 ? 1 : 0);
            byte prepared[];
            if (versionMinor >= 3) {
                prepared = Pkcs6.prepareSignedContent(dhData, "SHA", keyLength);
            } else {
                byte[] mdd = new MD5().digest(dhData);
                byte[] shd = new SHA().digest(dhData);
                byte[] t = new byte[mdd.length + shd.length];
                System.arraycopy(mdd, 0, t, 0, mdd.length);
                System.arraycopy(shd, 0, t, mdd.length, shd.length);
                prepared = Pkcs6.padSignedContent(t, keyLength, 1);
            }
            if (DEBUG.ON)
                Misc.dump("prepared", System.out, prepared);

            byte signed[] = Pkcs6.doRSA(prepared, pkData);
            if (DEBUG.ON)
                Misc.dump("signed", System.out, signed);

            if (versionMinor >= 3) {
                b[offset++] = 2; // SHA1
                b[offset++] = 1; // rsa
            }

            // copy signed
            b[offset++] = (byte) (signed.length >> 8);
            b[offset++] = (byte) (signed.length);
            System.arraycopy(signed, 0, b, offset, signed.length);
            offset += signed.length;

            // add handshake header
            int len = offset - dhParamOffset;
            addHandshakeHeader(12, b, dhStart + 5, len);
            addMessageHeader(22, b, dhStart, len + 4);
        }

        // server done - no data
        addHandshakeHeader(14, b, offset + 5, 0);
        addMessageHeader(22, b, offset, 4);

        return offset + 9;
    }

    private int addExtensions(byte[] b, int offset, int extLen) {
        final int xLen = extLen - 2;
        // add server name extension
        b[offset++] = (byte) (xLen >> 8);
        b[offset++] = (byte) xLen;

        if (hostName != null) {
            b[offset++] = 0;
            b[offset++] = 0; // 00 00 = hostname
            b[offset++] = 0;
            b[offset++] = 0; // len
        }
        if (secureRenegotiation) {
            b[offset++] = (byte) 0xff;
            b[offset++] = 1; // ff 01 = secureReneg
            b[offset++] = 0;
            b[offset++] = 1; // len
            b[offset++] = 0;
        }
        if (sessionTicket) {
            b[offset++] = 0;
            b[offset++] = 0x23;
            b[offset++] = 0;
            b[offset++] = 0;
        }
        return offset;
    }

    private int calcExtLen() {
        int extLen = 2;
        if (versionMinor > 0 && hostName != null)
            extLen += 4;
        if (secureRenegotiation)
            extLen += 5;
        if (sessionTicket)
            extLen += 4;
        if (extLen == 2)
            extLen = 0;
        return extLen;
    }

    /**
     * Method listen.
     * 
     * @param inputStream
     *            the input stream to read from
     * @param outputStream
     *            the output stream to write to
     * @param certArray
     *            a byte array containing the certificate(s) sent to the client
     */
    public void listen(InputStream is, OutputStream os) throws IOException {
        setStreams(is, os);
        listen();

        if (clientSessionId != null) {
            byte[] oldSecret = config.findSessionData(clientSessionId);
            if (oldSecret != null) {
                // perform a shortened handshake (check docs)
                try {
                    resume(oldSecret);
                } catch (IOException ioe) {
                    // remove the possible corrupted session
                    config.dropSession(clientSessionId);
                    throw ioe;
                }
                return;
            }
        }

        handshake(is, os);
        config.createSession(new SID(versionMinor, sessionId, 0, sessionId.length), masterSecret);
    }

    /**
     * Returns the first read data which was not processed during the handshake. If the data was already processed this
     * method returns null.
     * 
     * @return data or null.
     */
    public byte[] getFirstChunk() {
        return firstChunk;
    }

    static class SID {
        private int minorVersion;
        private byte[] data;

        SID(int minorVersion, byte[] sessionId, int offset, int len) {
            data = new byte[len];
            System.arraycopy(sessionId, offset, data, 0, len);
        }

        public int hashCode() {
            if (data == null)
                return super.hashCode();
            int hc = minorVersion;
            for (int i = 0; i < data.length; ++i) {
                hc = ((hc << 7) | (hc >>> 25)) ^ data[i];
            }
            return hc;
        }

        public boolean equals(Object o) {
            if (!(o instanceof SID))
                return false;
            SID oo = (SID) o;
            if (minorVersion != oo.minorVersion)
                return false;
            if (data == null || oo.data == null)
                return false;
            if (data.length != oo.data.length)
                return false;
            for (int i = 0; i < data.length; ++i) {
                if (data[i] != oo.data[i])
                    return false;
            }
            return true;
        }
    }
}
