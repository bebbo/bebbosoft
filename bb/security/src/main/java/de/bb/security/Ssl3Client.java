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

    private final static byte SERVER_VFY[] = "TLS 1.3, server CertificateVerify\u0000".getBytes();

	private static final byte[] CLIENT_CHANGE_CIPHER_SPEC = {0x14, 0x03, 0x03, 00, 0x01, 0x01};
    
    /** a random value received from server, used to create the master secret. */
    private byte preMasterSecret[];

    private boolean supportSessionTicket;

    private byte[] dhp;
    private byte[] dhg;
    private byte[] dhgy;

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

            int len;
            if (DEBUG.USE_TEST_DATA) {
            	writeBuffer = Misc.hex2Bytes("16 03 01 00 f8 01 00 00 f4 03 03 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f 20 e0 e1 e2 e3 e4 e5 e6 e7 e8 e9 ea eb ec ed ee ef f0 f1 f2 f3 f4 f5 f6 f7 f8 f9 fa fb fc fd fe ff 00 08 13 02 13 03 13 01 00 ff 01 00 00 a3 00 00 00 18 00 16 00 00 13 65 78 61 6d 70 6c 65 2e 75 6c 66 68 65 69 6d 2e 6e 65 74 00 0b 00 04 03 00 01 02 00 0a 00 16 00 14 00 1d 00 17 00 1e 00 19 00 18 01 00 01 01 01 02 01 03 01 04 00 23 00 00 00 16 00 00 00 17 00 00 00 0d 00 1e 00 1c 04 03 05 03 06 03 08 07 08 08 08 09 08 0a 08 0b 08 04 08 05 08 06 04 01 05 01 06 01 00 2b 00 03 02 03 04 00 2d 00 02 01 01 00 33 00 26 00 24 00 1d 00 20 35 80 72 d6 36 58 80 d1 ae ea 32 9a df 91 21 38 38 51 ed 21 a2 8e 3b 75 e9 65 d0 d2 cd 16 62 54 ");
            	updateHandshakeHashes(writeBuffer, 5, writeBuffer.length - 5);
            	len = writeBuffer.length;
            	clientRandom = Misc.hex2Bytes("00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f ");
            	sessionId = Misc.hex2Bytes("e0 e1 e2 e3 e4 e5 e6 e7 e8 e9 ea eb ec ed ee ef f0 f1 f2 f3 f4 f5 f6 f7 f8 f9 fa fb fc fd fe ff");
            	eekPriv = Misc.hex2Bytes("202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f");
            } else {
            	len = newClientHello(serverName);
            }
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

            if (eekPriv != null) {
                tls13Handshake(oldSessionId);
            } else {
	            ssl3Handshake(oldSessionId);
            }

            collect = false; // disable handshake logging
            // writeln("got server finished");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("client handshake", e);
        }
    }

	private void tls13Handshake(byte[] oldSessionId) throws IOException, Exception {
		byte[] b;
		rawread(onebyte, 1, 20);
		if (onebyte[0] != 1)
		    throw new IOException("ChangeCipherSpec");

		createTls13EarlyKeys(false);
		enableEncryption();
		
		// read as application data
		Outer:
		for(;;) {
			// need the hash without current message since the server used that to construct this message
			MessageDigest lastHsHash = hsSha.clone();
			rawread(head, head.length, 22);
			b = innerRead();
			
			switch (head[0]) {
			case 0x8: // server extensions
				break;
			case 0xb:
				certs = parseCertificate3(b, 1);
				break;
			case 0xf:
				parseTls13CertificateVerify(b, 0, lastHsHash);
				break;
			case 0x14:
		        parseTls13ServerHandshakeFinished(b, lastHsHash);
				break Outer;
			}
		}
		MessageDigest lastHsHash = hsSha.clone();
		os.write(CLIENT_CHANGE_CIPHER_SPEC);
		sendTls13ClientFinished();
		createTls13Keys(false, lastHsHash);
	}

    private void sendTls13ClientFinished() throws IOException {

        // calculate the hashes for the finished msg
        byte verify[] = createTls13HandshakeFinished(hsSha.clone(), clientSecret);
        byte b[] = new byte[verify.length + 4];
        b[0] = 20;
        b[1] = 0;
        b[2] = 0;
        b[3] = (byte) (verify.length);
        System.arraycopy(verify, 0, b, 4, verify.length);

        updateHandshakeHashes(b, 0, b.length);
        rawwrite(b, 22);
    }

	
	private void ssl3Handshake(byte[] oldSessionId) throws IOException, Exception {
		int len;
		byte[] b;
		boolean exchange = sessionId.length == 0 || !equals(oldSessionId, 0, sessionId, 0, sessionId.length);

		if (DEBUG.ON) {
		    System.out.println("exchanging a new key: " + exchange);
		}

		if (exchange) {
		    // receive the certificate
		    b = hs_read(11);
		    if (DEBUG.ON)
		        Misc.dump("serverCerts", System.out, b);
		    certs = parseCertificate3(b, 0);
		    // writeln("got " + certs.size() + " certificates");

		    MessageDigest lastHsHash = hsSha.clone();
		    
		    // receive server done
		    int rlen = rawread(head, head.length, 22);
		    if (head.length != rlen)
		        throw new IOException("handshake");

		    // DHE_RSA
		    int useDHE = ciphersuites[cipherIndex][5];
		    int curve = 0;
		    if (useDHE == 1 || useDHE == 7) {
		        if (head[0] != 12)
		            throw new IOException("handshake");

		        b = innerRead();
		        
		        if (DEBUG.ON)
		        	Misc.dump("ServerKeyExchange", System.out, b);
		        
		        if (useDHE == 1)
		        	readDheServerKey(b, lastHsHash);
		        else
		        	curve = readDhECServerKey(b, lastHsHash);

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
		    if (useDHE == 1) {
		        len = newClientKeyExchange3DhE();
		    } else if (useDHE == 7) {
		        len = newClientKeyExchange3DHeC(curve);
		    } else {
		        len = newClientKeyExchange3RSA(certs);
		    }
		    os.write(writeBuffer, 0, len);
		    // writeln("key exchange sent");

			calcMasterSecret();
		}
		if (DEBUG.ON) {
		    Misc.dump("masterSecret", System.out, masterSecret);
		}

		// calculate the keys for client(=>false)
		createKeys(false);
		
		os.flush();

		if (exchange) {
		    sendSslClientFinished();
		    serverFinished();
		} else {
		    serverFinished();
		    sendSslClientFinished();
		}
	}

	private void parseTls13ServerHandshakeFinished(byte[] b, MessageDigest md) throws IOException {
		byte[] verifyData = createTls13HandshakeFinished(md, serverSecret);
		if (!Misc.equals(verifyData, b))
			throw new IOException("server handshake finished");
	}

	private void parseTls13CertificateVerify(byte[] b, int off, MessageDigest hsHash) throws IOException {
		MessageDigest md = null;
		if (b[off++] == 8) {
			if (b[off] == 4)
				md = new SHA256();
			else if (b[off] == 5)
				md = new SHA384();
		}
		if (md == null)
			throw new IOException("not supported: " + b[0] + ", " + b[1]);

		Misc.dump("rsa sig",  System.out, b, off, b.length - off);

		
		byte[] hshash = hsHash.digest();
		
		// length of signature
		int len = (b[++off] & 0xff);
		len = (len << 8) | (b[++off] & 0xff);
        byte[] signed = new byte[len];
        System.arraycopy(b, ++off, signed, 0, len);

		
		// the data use to sign
		byte sigdata[] = new byte[64 + SERVER_VFY.length + hshash.length];
		for (int i = 0; i < 64; ++i)
			sigdata[i] = ' ';
		System.arraycopy(SERVER_VFY, 0, sigdata, 64, SERVER_VFY.length);
		System.arraycopy(hshash, 0, sigdata, 64 + SERVER_VFY.length, hshash.length);
		
        byte[] cert = certs.get(0);
        byte[] e = Pkcs6.getX509Exponent(cert);
        byte[] n = Pkcs6.getX509Modulo(cert);

        int emBits = n.length * 8 - 1;
        byte hi;
        if (n[0] == 0) {
        	emBits -= 8;
        	hi = n[1];
        } else 
        	hi = n[0];
        
        while (hi > 0) {
        	--emBits;
        	hi <<= 1;
        }
        
        byte[] signedContent = Pkcs6.doRSA(signed, n, e);
        boolean ok = md.emsPssVerify(sigdata, emBits, -1, signedContent);
        if (!ok)
        	throw new IOException("certificate verify");
	}

	private void calcMasterSecret() {
		if (DEBUG.HANDSHAKEHASH) {
		    Misc.dump("SHA256", System.out, hsSha.clone().digest());
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

	private int newClientKeyExchange3DhE() {
        byte y[] = random(dhp.length);

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

	private int newClientKeyExchange3DHeC(int curve) {
		// create the premaster secret
		
		byte b[];
		int dlen;
		if (curve == 0x1d) { // X22519
	        byte clientPrivateKey[] = random(32);
	        preMasterSecret = ECMath.x25519(clientPrivateKey, dhg);
	        byte[] pub = ECMath.x25519Pub(clientPrivateKey);
	        
	        b = writeBuffer;
	        b[9] = (byte)32;
	        b[10] = 0;
	        System.arraycopy(pub, 0, b, 10 + 32 - pub.length, pub.length);
			dlen = 33;
		} else 
		if (curve == 0x1e) { // X448
	        byte clientPrivateKey[] = random(56);
	        preMasterSecret = ECMath.x448(clientPrivateKey, dhg);
	        byte[] pub = ECMath.x448Pub(clientPrivateKey);
	        
	        b = writeBuffer;
	        b[9] = (byte)56;
	        b[10] = 0;
	        System.arraycopy(pub, 0, b, 10 + 56 - pub.length, pub.length);
			dlen = 57;
		} else {
			byte clientPrivateKey[] = ECMath.genPrivateKey(curve);
			byte[][] pt = ECMath.mult(curve, this.dhg, this.dhgy, clientPrivateKey);
			preMasterSecret = pt[0]; // x coordinate
			int ylen = ECMath.byteLength(curve);
			if (preMasterSecret.length < ylen) {
				byte[]tmp = new byte [ylen];
				System.arraycopy(preMasterSecret, 0, tmp, 1, preMasterSecret.length);
				clear(preMasterSecret);
				preMasterSecret = tmp;
			}

			// create the pubkey for the server
			byte [][] pub = ECMath.pub(curve, clientPrivateKey);
			if (DEBUG.EC) {
				Misc.dump("pubX", System.out, pub[0], 0, pub[0].length);
			}
		
	        dlen = 2 * ylen + 2;
	        if (writeBuffer.length < dlen + 9)
	            writeBuffer = new byte[dlen + 9];
	
	        b = writeBuffer;
	        b[9] = (byte) (1 + ylen * 2);
	        b[10] = 4; // uncompressed
	        
	        b[11] = 0;
	        b[11 + ylen] = 0;
	        System.arraycopy(pub[0], 0, b, 11 + ylen - pub[0].length, pub[0].length);
	        System.arraycopy(pub[1], 0, b, 11 + ylen + ylen - pub[1].length, pub[1].length);
		}
		
        addHandshakeHeader(16, b, 5, dlen);
        addMessageHeader(22, b, 0, dlen + 4);

        if (DEBUG.ON)
        	Misc.dump("newClientKeyExchange3DHeC", System.out, b, 0, dlen + 9);
        
        return dlen + 9;
    }

	
	private int readDhECServerKey(byte[] b, MessageDigest lastHsHash) throws IOException {
		if (b[0] != 3) throw new IOException("expected named curve");
		int curve = ((b[1] & 0xff) << 8) | (b[2] & 0xff);
		int len = b[3] & 0xff;
		if (curve >= 0x17 && curve <= 0x19) {
			if (b[4] != 4)
				throw new IOException("need uncompressed");
			--len;
			len >>= 1;
			dhg = new byte[len];
			dhgy = new byte[len];
			System.arraycopy(b, 5, dhg, 0, len);
			System.arraycopy(b, 5 + len, dhgy, 0, len);
			
			checkRSASignature(b, 5 + len + len, lastHsHash);			
		} else if (curve == 0x1D || curve == 0x1E) {
			// X25519 or X448 
			dhg = new byte[len];
			System.arraycopy(b, 4, dhg, 0, len);
			checkRSASignature(b, 4 + len, lastHsHash);			
		} else
			throw new IOException("unsupported curve");
		return curve;
	}

	
    private void readDheServerKey(byte[] b, MessageDigest lastHsHash) throws IOException {
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

        checkRSASignature(b, offset, lastHsHash);
    }

	private void checkRSASignature(byte[] b, int offset, MessageDigest lastHsHash) throws IOException {
		int len;
		int hash = 2;

		Misc.dump("rsa sig",  System.out, b, offset, b.length - offset);
		
        if (versionMinor >= 3) {
            hash = b[offset];
            if (hash == 8) {
            	parseTls13CertificateVerify(b, offset, lastHsHash);
            	return;
            }
            if (hash != 2 && hash != 4)
                throw new IOException("expected SHA or SHA256");
            
            ++offset;

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
            signedVerify = Pkcs6.padSignedContent(t, keyLength, 2);
        }
        // fix for some broken servers!?
        signedVerify[1] = signedContent[1];
        if (!Misc.equals(signedContent, signedVerify)) {
        	Misc.dump("content:", System.out, signedContent);
        	Misc.dump("verify:", System.out, signedVerify);
            throw new IOException("dh key params signature mismatch");
        }
	}

    private int newEmptyClientCerts() {
        writeBuffer[9] = 0;
        writeBuffer[10] = 0;
        writeBuffer[11] = 0;
        addHandshakeHeader(11, writeBuffer, 5, 3);
        addMessageHeader(22, writeBuffer, 0, 7);
        return 12;
    }

    private void sendSslClientFinished() throws IOException {
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

        enableEncryption();
        // writeln("got change cipher spec");

        int hsLen = versionMinor != 0 ? 16 : 40;
        byte[] f = new byte[hsLen];
        // get finished
        if (hsLen != rawread(f, f.length, 22))
            throw new IOException();

        if (!equals(f, 4, b, 4, b.length - 4))
            throw new IOException("finished: MAC error");
    }

	private void enableEncryption() {
		// enable read encryption
        readnum = 0;
        rhashBuffer = new byte[hashLen];
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
        serverRandom = new byte[32];
        System.arraycopy(b, off, serverRandom, 0, 32);
        off += 32; // 11 - 42 := Random

        int sl = b[off++] & 0xff; // session length
        sessionId = new byte[sl];

        System.arraycopy(b, off, sessionId, 0, sl);
        off += sl; // 43 - 43+off := session id

        byte ct0 = b[off++];
        byte ct1 = b[off++];
        // we require NO compression
        if (b[off++] != 0)
            throw new IOException("unsupported compression: " + b[off]);
        
        // search the ciphertype
        int selected = 0;
        for (; selected < ciphersuites.length; ++selected) {
            if (ciphersuites[selected][0] == ct0 && ciphersuites[selected][1] == ct1)
                break;
        }
        if (selected == ciphersuites.length)
        	return -1;

        // check for extensions as in TLS1.3
        if (off < b.length) {
        	int totalExtLen = 0xff & b[off++];
        	totalExtLen = totalExtLen << 8 | 0xff & b[off++];
        	int end = off + totalExtLen;
        	while (off < end) {
            	int type = 0xff & b[off++];
            	type = type << 8 | 0xff & b[off++];
            	int extLen = 0xff & b[off++];
            	extLen = extLen << 8 | 0xff & b[off++];
            	switch (type) {
            	case 0x2b: // version extension
            		++off;
            		versionMinor = b[off++];
            		break;
            	case 0x33: // early shared key
                	int ciph = 0xff & b[off++];
                	ciph = ciph << 8 | 0xff & b[off++];
                	int clen = 0xff & b[off++];
                	clen = clen << 8 | 0xff & b[off++];
                	if (ciph != 0x1D || clen != 32)
                		throw new IOException("unsupported shared key type: " + ciph);
                	byte pub[] = new byte[clen];
                	System.arraycopy(b, off, pub, 0, clen);
                	off += clen;
                	masterSecret = ECMath.x25519(eekPriv, pub);
            		break;
            	default:
            		off += extLen;
            	}
        	}
        }
        if (masterSecret == null)
        	eekPriv = null;

        startHandshakeHashes(selected);
                
        return selected;
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
    private static final Vector<byte[]> parseCertificate3(byte b[], int off) throws Exception {
        // certlistlen
        int cllen = ((b[off] & 0xff) << 16) | ((b[off + 1] & 0xff) << 8) | (b[off + 2] & 0xff);
        cllen += 3 - off;
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
        preMasterSecret = random(48);
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
        if (maxVersion == 4)
        	extHashLen += 4;
        	/* add
        		rsa_pkcs1_sha1 (0x201), rsa_pkcs1_sha256 (0x301), rsa_pkcs1_sha384 (0x401)
	           ecdsa_secp256r1_sha256(0x0403),
	           ecdsa_secp384r1_sha384(0x0503),
	        //   ecdsa_secp521r1_sha512(0x0603),

	           // TODO:

          /* RSASSA-PSS algorithms with public key OID rsaEncryption 
          rsa_pss_rsae_sha256(0x0804),
          rsa_pss_rsae_sha384(0x0805),
          rsa_pss_rsae_sha512(0x0806),

          /* EdDSA algorithms 
          ed25519(0x0807),
          ed448(0x0808),
          */ 
        
        if (DEBUG.ALL_HASH_EXTENSIONS)
            extHashLen = 5 * 2 * 3 + 2 + 6;

        int extNameLen = serverName == null ? 0 : serverName.length() + 9;
        int extLen = extHashLen + extNameLen 
	        + 16 // ec extension:  00 0A 00 0C 00 0A 00 1D 00 1E 00 19 00 18 00 17 
	    	// x25519 (29)            -> 00 1D
	    	// x448 (30)              -> 00 1E
	        // support secp521r1 (25) -> 00 19
	    	// support secp384r1 (24) -> 00 18
	    	// support secp256r1 (23) -> 00 17
	    ;

        if (supportSessionTicket)
            extLen += 4;

        if (maxVersion == 4)
        	extLen += 7 + 42; // supported version extension early encryption

        // total length including the 2 headers.
        int len = 48 + sessionIdlength + cipherCount + cipherCount + 2 // supportSecureNegotiation dummy cipher
                + 2 + extLen; // 2 length bytes + extension data
        
        if (writeBuffer.length < len)
            writeBuffer = new byte[len];

        byte b[] = writeBuffer;

        // bytes 0..8 are for headers.
        int i = 9;

        b[i++] = 3; // version = 3, get also an SSL 3 reply!!
        b[i++] = maxVersion == 4 ? 3 : maxVersion; // 1 = TLS1, 2 = TLS1.1, 3 = TLS1.2, TLS1.3: send 3 instead of 4!?

        clientRandom = random(32);

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
            
if (maxVersion == 4) 
{            
            b[i++] = 8;
            b[i++] = 4; // 0x0804 rsa_pss_rsae_sha256
            b[i++] = 8;
            b[i++] = 5; // rsa_pss_rsae_sha384
}
        }

        if (supportSessionTicket) {
            // session ticket
            b[i++] = 0;
            b[i++] = 0x23;
            b[i++] = 0;
            b[i++] = 0;
        }

        // add supported elliptic curves extension
        b[i++] = 0x00; 
        b[i++] = 0x0A;
		b[i++] = 0x00;
		b[i++] = 0x0C;
		b[i++] = 0x00;
		b[i++] = 0x0A;
		b[i++] = 0x00;
		b[i++] = 0x1E; //		x25519 (29)
		b[i++] = 0x00;
		b[i++] = 0x1D; //		x448 (30)
		b[i++] = 0x00;
		b[i++] = 0x19;
		b[i++] = 0x00;
		b[i++] = 0x18;
		b[i++] = 0x00;
		b[i++] = 0x17; 

		if (maxVersion == 4) {
			// add supported version extension
			b[i++] = 0;
			b[i++] = 0x2b;
			b[i++] = 0;
			b[i++] = 3;
			b[i++] = 2;
			b[i++] = 3;	//hi version
			b[i++] = 4; //lo version -> TLS1.3
			
			eekPriv = random(32);
			byte[] eekPub = ECMath.x25519Pub(eekPriv);
			b[i++] = 0;
			b[i++] = 0x33;
			b[i++] = 0;
			b[i++] = 0x26;
			b[i++] = 0;
			b[i++] = 0x24;
			b[i++] = 0;
			b[i++] = 0x1d; // x25519
			b[i++] = 0;
			b[i++] = 0x20;
			System.arraycopy(eekPub, 0, b, i, 32);
			i += 32;
			
			versionMinor = 1;
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
    
    protected void handleHandshakeMessage(byte[] buffer, int offset, int len) {
    	switch (buffer[offset]) {
    	case 0x04: // session ticket - todo
    		break;
    	}
    }
}
