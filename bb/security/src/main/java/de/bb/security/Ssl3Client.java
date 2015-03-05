/******************************************************************************
 * $Revision: 1.15 $
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Ssl3Client.java,v $
 * $Date: 2014/09/22 09:16:24 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2003. All rights reserved
 *
 * a SSL3 client implementation
 *
 * Based on http://home.netscape.com/eng/ssl3/draft302.txt 
 *****************************************************************************/

package de.bb.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import de.bb.util.Misc;

/**
 * Implements the client side handshake for SSL3. Contains all client specific function which are needed to establish an
 * SSL3 connection.
 */
public class Ssl3Client extends Ssl3 // implements Connector
{
    /** ASN.1 path to the public key inside a x.509 certificate. */
    private final static int PK_PATH[] = { 0x90, 0x90, 0x10, 0x10, 0x10, 0x10, 0x90, 0x83 };

    /** ASN.1 path to the public modulo inside the public key. */
    private final static int MODULO_PATH[] = { 0x90, 0x82 };

    /** ASN.1 path to the public exponent inside the public key. */
    private final static int EXPONENT_PATH[] = { 0x90, 0x02, 0x82 };

    /** a random value received from server, used to create the master secret. */
    private byte preMasterSecret[];

    private boolean supportSessionTicket;

    private byte[] dhp;
    private byte[] dhg;

    /**
     * Creates a new Ssl3Client object. You need to use the connect method to connect this object to a socket.
     * 
     * @throws java.io.IOException
     *             throws an IOException if any non recoverable error occurs.
     */
    public Ssl3Client() throws IOException {
        super(CIPHERSUITES);
    }

    /**
     * Creates a new Ssl3Client object. You need to use the connect method to connect this object to a socket.
     * 
     * @param ciphersuites
     *            a distinct set of ciphersuites
     * @throws java.io.IOException
     *             throws an IOException if any non recoverable error occurs.
     */
    public Ssl3Client(byte[][] ciphersuites) throws IOException {
        super(ciphersuites);
    }

    /**
     * Performs a SSL3 handshake. Uses the given InputStream for reading. and the given OutputStream for writing.
     * 
     * @param _is
     *            the InputStream to read from
     * @param _os
     *            the OutputStream to write to
     * @throws IOException
     */
    // public void connect(InputStream _is, OutputStream _os, byte[] clnRand,
    // byte b0, byte[] clientHello)
    public void connect(InputStream _is, OutputStream _os, String serverName) throws IOException {
        try {
            setStreams(_is, _os);

            rhashBuffer = null;
            rpos = readBuffer.length;

            collect = true;

            int len = newClientHello(serverName);
            os.write(writeBuffer, 0, len);
            os.flush();

            readnum = 0;
            writenum = 0;

            // receive the server hello
            byte[] b = hs_read(2);
            if (DEBUG.ON)
                Misc.dump("serverHello", System.out, b);

            byte[] oldSessionId = sessionId;
            cipherIndex = parseServerHello3(b);
            if (cipherIndex < 0)
                throw new IOException("no common cipher suite");

            if (DEBUG.ON)
                System.out.println("using cipher: " + getCipherSuite());

            boolean exchange = sessionId.length == 0 || !equals(oldSessionId, 0, sessionId, 0, sessionId.length);

            if (DEBUG.ON) {
                System.out.println("exchanging a new key: " + exchange);
            }

            if (exchange) {
                // receive the certificate
                b = hs_read(11);
                if (DEBUG.ON)
                    Misc.dump("serverCerts", System.out, b);
                certs = parseCertificate3(b);
                // writeln("got " + certs.size() + " certificates");

                // receive server done

                int rlen = rawread(head, head.length, 22);
                if (head.length != rlen)
                    throw new IOException("handshake");

                // DHE_RSA
                boolean useDHE = ciphersuites[cipherIndex][5] == 1;
                if (useDHE) {
                    if (head[0] != 12)
                        throw new IOException("handshake");

                    b = innerRead();
                    readDheServerKey(b);

                    rlen = rawread(head, head.length, 22);
                    if (head.length != rlen)
                        throw new IOException("handshake");
                }

                // check for additional client certificate request
                boolean sendCerts = false;
                if (head[0] == 13) {
                    b = innerRead();

                    sendCerts = true;

                    rlen = rawread(head, head.length, 22);
                    if (head.length != rlen)
                        throw new IOException("corrupt header received");
                }

                if (head[0] != 14)
                    throw new IOException("expected server done");

                b = innerRead();
                // writeln("got server done");

                if (sendCerts) {
                    // send a zero client certificate list
                    len = newEmptyClientCerts();
                    os.write(writeBuffer, 0, len);
                }

                // now use the certificates
                if (useDHE) {
                    len = newClientKeyExchange3DHE();
                } else {
                    len = newClientKeyExchange3RSA(certs);
                }
                os.write(writeBuffer, 0, len);
                // writeln("key exchange sent");

                if (DEBUG.HANDSHAKEHASH) {
                    Misc.dump("SHA256", System.out, ((MessageDigest) hsSha.clone()).digest());
                }

                // calulate the master secret
                if (DEBUG.ON) {
                    Misc.dump("preMasterSecret", System.out, preMasterSecret);
                    Misc.dump("clientRandom", System.out, clientRandom);
                    Misc.dump("serverRandom", System.out, serverRandom);
                }
                if (versionMinor != 0)
                    masterSecret = PRF(48, preMasterSecret, "master secret", clientRandom, serverRandom);
                else
                    masterSecret = makeHashBytes(preMasterSecret, 48, clientRandom, serverRandom);
            }
            if (DEBUG.ON) {
                Misc.dump("masterSecret", System.out, masterSecret);
            }

            // calculate the keys for client(=>false)
            createKeys(false);

            os.flush();

            if (exchange) {
                clientFinished();
                serverFinished();
            } else {
                serverFinished();
                clientFinished();
            }

            collect = false; // disable handshake logging
            // writeln("got server finished");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("client handshake", e);
        }
    }

    private int newClientKeyExchange3DHE() {
        byte y[] = new byte[dhp.length];
        secureRnd.nextBytes(y);

        // update our preMasterSecret
        preMasterSecret = Pkcs6.doRSA(preMasterSecret, dhp, y);

        // create the data for the server
        y = Pkcs6.doRSA(dhg, dhp, y);

        int dlen = y.length + 2;
        if (writeBuffer.length < dlen + 9)
            writeBuffer = new byte[dlen + 9];

        byte b[] = writeBuffer;
        b[9] = (byte) (y.length >> 8);
        b[10] = (byte) y.length;
        System.arraycopy(y, 0, b, 11, y.length);

        addHandshakeHeader(16, b, 5, dlen);
        addMessageHeader(22, b, 0, dlen + 4);

        return dlen + 9;
    }

    private void readDheServerKey(byte[] b) throws IOException {
        int len = ((b[0] & 0xff) << 8) | (b[1] & 0xff);
        dhp = new byte[len];
        System.arraycopy(b, 2, dhp, 0, len);

        int offset = len + 2;

        len = ((b[offset] & 0xff) << 8) | (b[offset + 1] & 0xff);
        dhg = new byte[len];
        System.arraycopy(b, offset + 2, dhg, 0, len);

        offset += len + 2;

        len = ((b[offset] & 0xff) << 8) | (b[offset + 1] & 0xff);
        preMasterSecret = new byte[len];
        System.arraycopy(b, offset + 2, preMasterSecret, 0, len);

        offset += len + 2;

        int hash = 2;

        if (versionMinor >= 3) {
            hash = b[offset++];
            if (hash != 2 && hash != 4)
                throw new IOException("expected SHA or SHA256");

            if (b[offset++] != 1)
                throw new IOException("expected RSA signature");
        }

        len = ((b[offset] & 0xff) << 8) | (b[offset + 1] & 0xff);
        byte[] signed = new byte[len];
        System.arraycopy(b, offset + 2, signed, 0, len);

        byte[] cert = certs.get(0);
        byte[] e = Pkcs6.getX509Exponent(cert);
        byte[] n = Pkcs6.getX509Modulo(cert);

        // get the signed content
        byte[] signedContent = Pkcs6.doRSA(signed, n, e);

        // calculate our signature
        if (versionMinor >= 3)
            offset -= 2;

        byte[] dhData = new byte[clientRandom.length + serverRandom.length + offset];
        System.arraycopy(clientRandom, 0, dhData, 0, clientRandom.length);
        System.arraycopy(serverRandom, 0, dhData, clientRandom.length, serverRandom.length);
        System.arraycopy(b, 0, dhData, clientRandom.length + serverRandom.length, offset);

        int keyLength = n.length - (n[0] == 0 ? 1 : 0);
        byte[] signedVerify;
        if (versionMinor >= 3) {
            signedVerify = Pkcs6.prepareSignedContent(dhData, hash == 2 ? "SHA" : "SHA256", keyLength);
        } else {
            byte[] mdd = new MD5().digest(dhData);
            byte[] shd = new SHA().digest(dhData);
            byte[] t = new byte[mdd.length + shd.length];
            System.arraycopy(mdd, 0, t, 0, mdd.length);
            System.arraycopy(shd, 0, t, mdd.length, shd.length);
            signedVerify = Pkcs6.padSignedContent(t, keyLength);
        }
        if (!Misc.equals(signedContent, signedVerify))
            throw new IOException("dh key params signature mismatch");
    }

    private int newEmptyClientCerts() {
        writeBuffer[9] = 0;
        writeBuffer[10] = 0;
        writeBuffer[11] = 0;
        addHandshakeHeader(11, writeBuffer, 5, 3);
        addMessageHeader(22, writeBuffer, 0, 7);
        return 12;
    }

    private void clientFinished() throws IOException {
        os.write(css);

        // calculate the hashes for the finished msg
        byte b[] = clientFinishedHash();

        b[0] = 20;
        b[1] = 0;
        b[2] = 0;
        b[3] = (byte) (b.length - 4);

        updateHandshakeHashes(b, 0, b.length);
        rawwrite(b, 22);
        // writeln("sent client finished");
        os.flush();
    }

    private void serverFinished() throws IOException {

        byte b[] = serverFinishedHash();

        // get changecipherspec
        rawread(onebyte, 1, 20);
        if (onebyte[0] != 1)
            throw new IOException("ChangeCipherSpec");

        // enable read encryption
        readnum = 0;
        rhashBuffer = new byte[hashLen];
        // writeln("got change cipher spec");

        int hsLen = versionMinor != 0 ? 16 : 40;
        byte[] f = new byte[hsLen];
        // get finished
        if (hsLen != rawread(f, f.length, 22))
            throw new IOException();

        if (!equals(f, 4, b, 4, b.length - 4))
            throw new IOException("finished: MAC error");
    }

    /**
     * parse a server hello -> get the cipher type.
     * 
     * @param b
     *            a byte array containing a server hello message
     * @return the selected cipher type
     * @throws Exception
     *             if message is corrupt
     */
    private final int parseServerHello3(byte b[]) throws IOException {
        int off = 0;
        int vh = b[off++];
        versionMinor = b[off++];
        if (DEBUG.ON)
            System.out.println("minor verion " + versionMinor);

        if (vh != 3 || versionMinor > maxVersion || versionMinor < minVersion)
            throw new IOException("unsupported protocol version: " + vh + ", " + versionMinor);

        css[2] = versionMinor;

        System.arraycopy(b, off, serverRandom, 0, 32);
        off += 32; // 11 - 42 := Random

        int sl = b[off++] & 0xff; // session length
        sessionId = new byte[sl];

        System.arraycopy(b, off, sessionId, 0, sl);
        off += sl; // 43 - 43+off := session id

        if (DEBUG.HANDSHAKEHASH) {
            Misc.dump("DigestUpdate", System.out, pendingHandshake, 0, pendingHandshake.length);
        }
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

        byte ct0 = b[off++];
        byte ct1 = b[off++];
        // we require NO compression
        if (b[off] != 0)
            throw new IOException("unsupported compression: " + b[off]);
        // search the ciphertype
        for (int i = 0; i < ciphersuites.length; ++i) {
            if (ciphersuites[i][0] == ct0 && ciphersuites[i][1] == ct1)
                return i;
        }
        return -1;
    }

    /**
     * parse a certificate msg.
     * 
     * @param b
     *            a byte array containing a certifcate message
     * @return a new allocated vector with al certificates
     * @throws Exception
     *             if message is corrupt
     */
    // ===========================================================================
    private static final Vector<byte[]> parseCertificate3(byte b[]) throws Exception {
        // certlistlen
        int off = 0;
        int cllen = ((b[off] & 0xff) << 16) | ((b[off + 1] & 0xff) << 8) | (b[off + 2] & 0xff);
        cllen += 3;
        off += 3;
        Vector<byte[]> v = new Vector<byte[]>();
        while (off < cllen) {
            // len of cert
            int clen = ((b[off] & 0xff) << 16) | ((b[off + 1] & 0xff) << 8) | (b[off + 2] & 0xff);
            if (clen == 0) // some server send corrupt arrays with plenty
                           // zeroes...
                break;
            off += 3;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(clen);
            bos.write(b, off, clen);
            v.add(bos.toByteArray());
            off += clen;
        }
        return v;
    }

    /**
     * create the client key exchange msg.
     * 
     * @param v
     *            a vector with certificates - only the first is used
     * @return the length of the message
     * @throws Exception
     *             if message is corrupt
     */
    private final int newClientKeyExchange3RSA(Vector<byte[]> v) throws Exception {
        if (v.size() == 0)
            throw new IOException("no certificate");

        byte cert[] = v.elementAt(0);
        // get the public key from certificate
        byte b[] = getSeq(cert, PK_PATH, 0);
        byte nb[] = getSeq(b, MODULO_PATH, b[0] == 0 ? 1 : 0);
        byte eb[] = getSeq(b, EXPONENT_PATH, b[0] == 0 ? 1 : 0);

        // create the premaster secret
        preMasterSecret = new byte[48];
        secureRnd.nextBytes(preMasterSecret); // init with random
        if (DEBUG.USE_TEST_DATA) {
            for (int ii = 0; ii < preMasterSecret.length; ++ii) {
                preMasterSecret[ii] = (byte) ii;
            }
        }
        preMasterSecret[0] = 3;
        preMasterSecret[1] = versionMinor; // version

        // encrypted the preMasterSecret
        int dlen = nb.length;
        if (nb[0] == 0)
            --dlen;
        byte encodedPMS[] = new byte[dlen];
        unzero(encodedPMS);
        encodedPMS[0] = 0;
        encodedPMS[1] = 2;
        encodedPMS[encodedPMS.length - 49] = 0;
        System.arraycopy(preMasterSecret, 0, encodedPMS, encodedPMS.length - 48, 48);

        int mod[] = FastMath32.byte2Int(nb, 0);
        int z[] = FastMath32.byte2Int(encodedPMS, (mod.length << 1) + 1);

        int r[] = FastMath32.oddModPow(z, eb, mod);
        byte encryptedPMS[] = new byte[dlen];
        FastMath32.int2Byte(r, encryptedPMS);

        if (DEBUG.ON) {
            Misc.dump("encoded PMS", System.out, encodedPMS);
            Misc.dump("public exponent", System.out, eb);
            Misc.dump("public modulo", System.out, nb);
            Misc.dump("encrypted PMS", System.out, encryptedPMS);
        }

        dlen = encodedPMS.length - encryptedPMS.length;

        if (writeBuffer.length < encodedPMS.length + 11)
            writeBuffer = new byte[encodedPMS.length + 11];

        int offset = versionMinor == 0 ? 9 : 11;

        b = writeBuffer;
        if (dlen >= 0) {
            // to short - insert leading zeros
            int j = offset;
            for (int i = 0; i < dlen; ++i, ++j) {
                b[j] = 0;
            }
            System.arraycopy(encryptedPMS, 0, b, j, encryptedPMS.length);
        } else {
            // copy only the fitting part
            System.arraycopy(encryptedPMS, -dlen, b, offset, encodedPMS.length);
        }
        dlen = encodedPMS.length;

        if (versionMinor > 0) {
            b[9] = (byte) (dlen >> 8);
            b[10] = (byte) dlen;
            dlen += 2;
        }

        addHandshakeHeader(16, b, 5, dlen);
        addMessageHeader(22, b, 0, dlen + 4);

        dlen += 9;

        if (DEBUG.ON)
            Misc.dump("clientKeyExchange", System.out, b, 0, dlen);

        return dlen;
    }

    /**
     * creates a client hello - support the configured cipersuites. The client hello is placed into the writeBuffer at
     * offset 9. The writeBuffer is resized if necessary.
     * 
     * @return the length of the client hello message
     */
    private final int newClientHello(final String serverName) {
        // consider the sessionId
        int sessionIdlength = 0;
        if (sessionId != null) {
            sessionIdlength = sessionId.length;
            hsMd5 = null;
            hsSha = null;
        }

        // and all configured CIPHERSUITES
        int cipherCount = ciphersuites.length;
        //        final TreeSet<Integer> hashes = new TreeSet<Integer>();
        //        for (int i = 0; i < max; ++i) {
        //            hashes.add(-ciphersuites[i][4]);
        //        }
        int extHashLen = 3 * 2 + 6;
        if (DEBUG.ALL_HASH_EXTENSIONS)
            extHashLen = 5 * 2 * 3 + 2 + 6;

        int extNameLen = serverName == null ? 0 : serverName.length() + 9;
        int extLen = extHashLen + extNameLen;

        if (supportSessionTicket)
            extLen += 4;

        // total length including the 2 headers.
        int len = 48 + sessionIdlength + cipherCount + cipherCount + 2 // supportSecureNegotiation dummy cipher
                + 2 + extLen; // 2 length bytes + extension data

        if (writeBuffer.length < len)
            writeBuffer = new byte[len];

        byte b[] = writeBuffer;

        // bytes 0..8 are for headers.
        int i = 9;

        b[i++] = 3; // version = 3, get also an SSL 3 reply!!
        b[i++] = maxVersion; // 1 = TLS1, 2 = TLS1.1, 3 = TLS1.2

        secureRnd.nextBytes(clientRandom); // init with random

        int time = (int) (System.currentTimeMillis() / 1000);
        clientRandom[0] = (byte) (time >>> 24);
        clientRandom[1] = (byte) (time >>> 16);
        clientRandom[2] = (byte) (time >>> 8);
        clientRandom[3] = (byte) time;

        if (DEBUG.USE_TEST_DATA) {
            for (int ii = 0; ii < clientRandom.length; ++ii)
                clientRandom[ii] = (byte) ii;
        }

        System.arraycopy(clientRandom, 0, b, i, 32); // challenge
        i += 32;

        // add session id
        b[i++] = (byte) sessionIdlength;
        if (sessionIdlength != 0) {
            System.arraycopy(sessionId, 0, b, i, sessionIdlength);
            i += sessionIdlength;
        }

        // add the cipher suites - also insert the DUMMY ff-00 for TLS secure re-negotiation
        // supportSecureNegotiation
        ++cipherCount;
        b[i++] = (byte) (cipherCount >>> 7);
        b[i++] = (byte) (cipherCount << 1); // the configured ones
        // supportSecureNegotiation
        --cipherCount;
        for (int j = 0; j < cipherCount; ++j) {
            b[i++] = ciphersuites[j][0];
            b[i++] = ciphersuites[j][1];
        }
        //supportSecureNegotiation
        b[i++] = 0;
        b[i++] = (byte) 0xff; // add dummy for TLS extensions

        // add compression = no compression
        b[i++] = 1;
        b[i++] = 0;

        // add TLS extensions
        b[i++] = (byte) (extLen >> 8);
        b[i++] = (byte) extLen;

        if (serverName != null) {
            // add TLS extension with server name
            b[i++] = 0;
            b[i++] = 0;

            extNameLen -= 4;
            b[i++] = (byte) (extNameLen >> 8);
            b[i++] = (byte) extNameLen;

            extNameLen -= 2;
            b[i++] = (byte) (extNameLen >> 8);
            b[i++] = (byte) extNameLen;

            b[i++] = 0;

            extNameLen -= 3;
            b[i++] = (byte) (extNameLen >> 8);
            b[i++] = (byte) extNameLen;

            System.arraycopy(serverName.getBytes(), 0, b, i, extNameLen);
            i += extNameLen;
        }

        // add TLS extension with signature algorithms
        // right we support
        b[i++] = 0;
        b[i++] = 0xd;
        extHashLen -= 4;
        b[i++] = (byte) (extHashLen >> 8);
        b[i++] = (byte) extHashLen;
        extHashLen -= 2;
        b[i++] = (byte) (extHashLen >> 8);
        b[i++] = (byte) extHashLen;

        if (DEBUG.ALL_HASH_EXTENSIONS) {
            for (int ii = 6; ii >= 2; --ii) {
                b[i++] = (byte) ii;
                b[i++] = 1;
                b[i++] = (byte) ii;
                b[i++] = 2;
                b[i++] = (byte) ii;
                b[i++] = 3;
            }
            b[i++] = 1;
            b[i++] = 1;
        } else {
            b[i++] = 2;
            b[i++] = 1; // RSA
            b[i++] = 4;
            b[i++] = 1; // RSA
            b[i++] = 5;
            b[i++] = 1; // RSA
        }

        if (supportSessionTicket) {
            // session ticket
            b[i++] = 0;
            b[i++] = 0x23;
            b[i++] = 0;
            b[i++] = 0;
        }

        addHandshakeHeader(1, b, 5, i - 9);
        addMessageHeader(22, b, 0, i - 5);

        if (DEBUG.ON)
            Misc.dump("clienthello", System.out, b);
        return i;
    }

    public String getVersion() {
        return "3." + versionMinor;
    }

    /**
     * create a client hello - support only 5 ciphertypes.
     * 
     * @return a new allocated byte array containing a client hello message / private final byte[] newClientHello2() {
     * 
     *         int sl = 0; if (sessionId != null) { sl = sessionId.length; }
     * 
     *         byte b[] = new byte[2 + 1 + 8 + 5 * 3 + 16 + sl];
     * 
     *         int i = 0;
     * 
     *         b[i++] = (byte) (0x80); b[i++] = (byte) (1 + 8 + 5 * 3 + 16 + sl); // length of rest
     * 
     *         b[i++] = (byte) 1; // client hello
     * 
     *         b[i++] = 3; // version = 3, get also an SSL 3 reply!! b[i++] = 0;
     * 
     *         b[i++] = 0; b[i++] = 3 * 5; // 5 ciphers
     * 
     *         b[i++] = 0; b[i++] = (byte) sl; // session id length
     * 
     *         b[i++] = 0; b[i++] = 16; // challenge length
     * 
     *         if (sl != 0) { System.arraycopy(sessionId, 0, b, i, sl); i += sl; }
     * 
     *         b[i++] = 0; b[i++] = 0; b[i++] = (byte) (0x05); // SSL3_RSA_WITH_RC4_SHA
     * 
     *         b[i++] = 0; b[i++] = 0; b[i++] = (byte) (0x04); // SSL3_RSA_WITH_RC4_MD5
     * 
     *         b[i++] = 1; b[i++] = 0; b[i++] = (byte) (0x80); // SSL2_RSA_WITH_RC4_128_MD5
     * 
     *         b[i++] = 0; b[i++] = 0; b[i++] = (byte) (0x03); // SSL3_RSA_WITH_RC4_40_MD5_EXPORT
     * 
     *         b[i++] = 2; b[i++] = 0; b[i++] = (byte) (0x80); // SSL2_RSA_WITH_RC4_40_MD5_EXPORT
     * 
     *         SslBase.secureRnd.nextBytes(clientRandom); // init with random System.arraycopy(clientRandom, 0, b, i,
     *         16); // challenge for (i = 0; i < 16; ++i) clientRandom[i] = 0; return b; } /
     **/
}
/*
 * $Log: Ssl3Client.java,v $
 * Revision 1.15  2014/09/22 09:16:24  bebbo
 * @N added a config class Ssl3Config to support key/certificate selection based on host name
 *
 * Revision 1.14  2014/09/18 13:49:19  bebbo
 * @N added support for TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 and TLS_DHE_RSA_WITH_AES_256_CBC_SHA
 *
 * Revision 1.13  2014/04/13 20:17:23  bebbo
 * @N send also TLS extension with server name
 * @N send also TLS extension with signature hashes
 * @N signal secure renegotiation but don't support it
 *
 * Revision 1.12  2013/11/28 12:26:15  bebbo
 * @N aded String getCipherSuite() to get a verbose name for the used cipher suite
 * @N prepared support for session ticket
 * @I client adds current time to client random
 *
 * Revision 1.11  2013/05/17 10:54:43  bebbo
 * @D more DEBUG infos
 * @B errors yield a SslException which are 100ms tick aligned to avoid timing attacks
 *
 * Revision 1.10  2012/08/21 19:34:53  bebbo
 * @I handshake hash instances are now nulled if the client is used again. Session resuming is working now
 *
 * Revision 1.9  2012/08/21 06:36:57  bebbo
 * @N added a method to limit the protocol version, e.g. to SSL3
 *
 * Revision 1.8  2012/08/19 20:10:22  bebbo
 * @N added support for the secure renegotiation extension - even if we don't support renegotiation.
 *
 * Revision 1.7  2012/08/19 15:26:38  bebbo
 * @N added SHA256
 * @R added support for TLS1.2
 *
 * Revision 1.6  2012/08/16 19:35:22  bebbo
 * @R added support for TLS1.1
 *
 * Revision 1.5  2011/01/06 11:00:19  bebbo
 * @N added client support for TLS 1.0
 * @V 1.0.2
 * Revision 1.4 2009/02/05 20:06:28 bebbo
 * 
 * @N added support for TLS 1.0
 * 
 * Revision 1.3 2008/03/13 20:50:32 bebbo
 * 
 * @I working towards TLS
 * 
 * Revision 1.2 2007/05/03 19:37:22 bebbo
 * 
 * @I more stability
 * 
 * Revision 1.1 2007/04/21 11:33:56 bebbo
 * 
 * @N added AES and DES to SSLv3
 * 
 * Revision 1.5 2007/04/20 14:55:42 bebbo
 * 
 * @I cleanups
 * 
 * Revision 1.4 2007/04/20 14:26:22 bebbo
 * 
 * @I more plain writes. further enhancements are otw
 * 
 * Revision 1.3 2007/04/20 05:12:01 bebbo
 * 
 * @R removed SSLv2
 * 
 * @R enabled resumed handshakes
 * 
 * Revision 1.2 2007/04/19 16:56:56 bebbo
 * 
 * @R modified to use SSLv3 directly
 * 
 * @R removed SSLv2
 * 
 * @N added AES 128,192,256
 * 
 * Revision 1.1 2007/04/18 13:07:10 bebbo
 * 
 * @N first checkin
 * 
 * Revision 1.9 2003/03/06 15:25:00 bebbo
 * 
 * @C completed documentation
 * 
 * Revision 1.8 2003/01/04 12:22:35 bebbo
 * 
 * @R removed DEBUG messages
 * 
 * Revision 1.7 2003/01/04 12:10:37 bebbo
 * 
 * @I cleaned up imports and formatted source code
 * 
 * Revision 1.6 2003/01/04 12:07:15 bebbo
 * 
 * @R cleanup
 * 
 * Revision 1.5 2001/09/15 08:53:07 bebbo
 * 
 * @R removed protected attribute
 * 
 * Revision 1.4 2001/02/19 06:42:36 bebbo
 * 
 * @R added method connect(InputStream is, OutputStream os)
 * 
 * Revision 1.3 2000/09/25 13:03:28 bebbo
 * 
 * @R getCertificates now public available
 * 
 * @R format of certificate is now (byte [])
 * 
 * Revision 1.2 2000/09/25 12:49:19 bebbo
 * 
 * @C fixed comments
 * 
 * Revision 1.1 2000/09/25 12:21:10 bebbo
 * 
 * @N repackaged
 */
