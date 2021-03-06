/******************************************************************************
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2022. All rights reserved
 *
 * Base class for SSL3
 *
 * Based on http://home.netscape.com/eng/ssl3/draft302.txt
 * and teh various SSL/TLS RFCs
 *****************************************************************************/

package de.bb.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import de.bb.security.Ssl3Server.SID;
import de.bb.util.Misc;

/**
 * Base class for Ssl3Client and Ssl3Server. Contains all common functions,
 * which are needed to establish an SSL3 connection.
 */
public abstract class Ssl3 {
	// 0-1: cipher id
	// 2: key bytes;
	// 3: crypter: 0=RC4, 1=AES, 2=DES3, 3=DES, 4=AES with GCM;
	// 4: hash: 1=MD5, 2=SHA, 4=SHA256, 5=SHA384;
	// 5: key exchange: 0=DHE_DSS, 1=DHE_RSA, 2=DH_ANON 4=RSA, 5=DH_DSS, 6=DH_RSA, 7=ECDHE_RSA
	
	public final static byte[] TLS_AES_256_GCM_SHA384       = {0x13, 0x02, 32, 4, 5, 7};
	public final static byte[] TLS_AES_128_GCM_SHA256       = {0x13, 0x01, 16, 4, 4, 7};
//	public final static byte[] TLS_CHACHA20_POLY1305_SHA256 = {0x13, 0x03};

	public final static byte[] TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 = {(byte)0xc0, (byte)0x30, 32, 4, 5, 7};

	public final static byte[] TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 = {(byte)0xc0, (byte)0x2f, 16, 4, 4, 7};

	public final static byte[] TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 = {0x00, (byte)0x9f, 32, 4, 5, 1};

	public final static byte[] TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 = {0x00, (byte)0x9e, 16, 4, 4, 1};
	
	public final static byte[] TLS_RSA_WITH_AES_256_GCM_SHA384 = {0x00, (byte)0x9d, 32, 1, 5, 4};
	
	public final static byte[] TLS_RSA_WITH_AES_128_GCM_SHA256 = {0x00, (byte)0x9c, 16, 1, 4, 4};
	
	public final static byte[] TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 = { 0, 0x6b, 32, 1, 4, 1};

	public final static byte[] TLS_DHE_RSA_WITH_AES_256_CBC_SHA = { 0, 0x39, 32, 1, 2, 1};

	public final static byte[] TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 = { (byte)0xc0, 0x67, 16, 1, 4, 7};

	public final static byte[] TLS_DHE_RSA_WITH_AES_128_CBC_SHA256 = { 0, 0x67, 16, 1, 4, 1};

	public final static byte[] TLS_DHE_RSA_WITH_AES_128_CBC_SHA = { 0, 0x33, 16, 1, 2, 1};

	public final static byte[] TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA = {(byte)0xc0, 0x13, 16, 1, 2, 7};

	public final static byte[] TLS_RSA_WITH_AES_256_CBC_SHA256 = { 0, 0x3D, 32, 1, 4, 4};

	public final static byte[] TLS_RSA_WITH_AES_128_CBC_SHA256 = { 0, 0x3c, 16, 1, 4, 4};

	public final static byte[] TLS_RSA_WITH_AES_256_CBC_SHA = { 0, 0x35, 32, 1, 2, 4};

	public final static byte[] TLS_RSA_WITH_AES_128_CBC_SHA = { 0, 0x2f, 16, 1, 2, 4};

//	public final static byte[] TLS_RSA_WITH_3DES_EDE_CBC_SHA = { 0, 0x0a, 24, 2, 2, 4};
//
//	public final static byte[] TLS_RSA_WITH_DES_CBC_SHA = { 0, 0x09, 8, 3, 2, 4};
//
//	public final static byte[] TLS_RSA_WITH_RC4_128_SHA = { 0, 0x05, 16, 0, 2, 4};
//
//	public final static byte[] TLS_RSA_WITH_RC4_128_MD5 = { 0, 0x04, 16, 0, 1, 4};
//
//	public final static byte[] TLS_RSA_EXPORT_WITH_RC4_40_MD5 = { 0, 0x03, 5, 0, 1, 4};

	
	public final static byte[] CIPHERSUITES[] = { 
			TLS_AES_256_GCM_SHA384, TLS_AES_128_GCM_SHA256,       
			TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
			TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
			TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
			TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TLS_DHE_RSA_WITH_AES_128_CBC_SHA256, 
			TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
			TLS_RSA_WITH_AES_256_CBC_SHA256, TLS_RSA_WITH_AES_256_CBC_SHA,
			TLS_RSA_WITH_AES_128_CBC_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA,
//			TLS_RSA_WITH_3DES_EDE_CBC_SHA,
			/*
			 * disabled for security reasons TLS_RSA_WITH_DES_CBC_SHA,
			 * TLS_RSA_WITH_RC4_128_MD5, TLS_RSA_EXPORT_WITH_RC4_40_MD5
			 */
	};
	 
	public final static String CIPHERSUITENAMES[] = { 
			"TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256",
			"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
			"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", 
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
			"TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_256_CBC_SHA",
			"TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA",
//			"TLS_RSA_WITH_3DES_EDE_CBC_SHA", 
			};

	/** a constant used in key creation. */
	static final byte SERVER[] = { (byte) 0x53, (byte) 0x52, (byte) 0x56, (byte) 0x52 };

	/** a constant used in key creation. */
	static final byte CLIENT[] = { (byte) 0x43, (byte) 0x4c, (byte) 0x4e, (byte) 0x54 };

	/** an array with length 0. */
	static final byte NULLBYTES[] = {};

	static final byte SPACE32[] = "                                ".getBytes();
	
	private static final HashMap<String, byte[]> CIPHERLOOKUP = new HashMap<String, byte[]>();

	private static final byte[] ALERTCLOSE = { 1, 0 };

	/** an array for a full change cipher spec message. */
	final byte[] css = { 20, 3, 0, 0, 1, 1 };

	/** the secure random instance used in this package. */
	static SecureRandom secureRnd = SecureRandom.getInstance();

	/** own InputStream to facade internal functions. */
	private InputStream myIn;

	/** own OutputStream to facade internal functions. */
	private OutputStream myOut;

	/** the used CIPHERSUITES. */
	byte[][] ciphersuites;

	/** vector for certificates. */
	Vector<byte[]> certs;

	/** supplied InputStream. */
	InputStream is;

	/** supplied OutputStream. */
	OutputStream os;

	/** member to point to the ciphertype if >= 0. */
	int cipherIndex = -1;

	/** for calculation in send / receive mode. */
	MessageDigest md5 = new MD5();
	MessageDigest sha = new SHA();
	MessageDigest prfMd = new SHA256();

	/**
	 * SSL2: for the 2byte/3byte msg header. SSL3: for the 4byte msg header.
	 */
	byte head[] = new byte[4];

	/** buffer to read a message or a single byte. */
	byte readBuffer[] = NULLBYTES, onebyte[] = new byte[1];

	int readBufferLength;

	/** read position in current message. */
	int rpos;

	/** the master secret. */
	byte masterSecret[];

	/** the client random. */
	byte clientRandom[];

	/** the server random. */
	byte serverRandom[];

	/** the used sessionId, to reuse session. */
	byte sessionId[];

	/** server read secret. */
	byte readSecret[];

	/** server write secret. */
	byte writeSecret[];

	/**
	 * hashes for checksum of messages.
	 */
	MessageDigest readHash, writeHash;

	/** length of read / write hash. */
	int hashLen;

	/** buffer to hold read hash. */
	byte rhashBuffer[];

	protected BlockCipher cryptRead;

	protected BlockCipher cryptWrite;

	protected byte[] readIV;

	protected byte[] writeIV;

	protected int blockSize;

	/** read and write buffer for 5 bytes. */
	protected byte r5[] = new byte[5];

	byte writeBuffer[] = NULLBYTES;

	/** true when handshake messages are collected in handshake hash. */
	boolean collect;

	/** the handshake hashes. collect handshake messages. */
	MessageDigest hsMd5, hsSha;

	/** counter for received and sent messages. */
	long readnum, writenum;

	protected byte[] pendingHandshake = {};

	protected int maxFragmentLength = (1 << 14);

	protected SID clientSessionId;

	protected byte maxVersion = 4;

	protected byte minVersion = 3;

	protected byte versionMinor;

	protected byte[] writeAad;
	protected byte[] readAad;
	byte[] writeNonce;
	byte[] readNonce;
	byte[] t16 = new byte[16];
	protected byte[] eekPriv;

	final static byte ALERT86[] = { 21, 3, 3, 0, 2, 2, 86 };
	protected byte[] serverSecret;
	protected byte[] clientSecret;
	private byte[] handshakeSecret;


	/**
	 * Creates a new Ssl3 object, which uses the given InputStream for reading
	 * and the given OutputStream for writing.
	 * 
	 * @exception java.io.IOException
	 *                throws an IOException if any non recoverable error occurs.
	 */
	Ssl3(byte[][] ciphersuites) throws IOException {
		this.ciphersuites = ciphersuites;
	}

	/**
	 * Reads one byte from input stream with blocking.
	 * 
	 * @return
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	protected final int read() throws IOException {
		if (1 != rawread(onebyte, 1, 23))
			return -1;
		return onebyte[0] & 0xff;
	}

	/**
	 * Reads into the given byte array from input stream with blocking. This
	 * functions returns with an partial read too! Check the count of read
	 * bytes.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @return count of bytes read
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	protected final int read(byte b[]) throws IOException {
		int len = available();
		// System.err.println("avail " + len);
		while (len <= 0) {
			readahead();
			len = available();
		}
		if (len > b.length)
			len = b.length;
		// System.err.println("reading " + len);
		return rawread(b, len, 23);
	}

	/**
	 * Writes the given byte array to the output stream. If necessary the block
	 * is split into smaller packets.
	 * 
	 * @param b
	 *            the buffer which is written.
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	protected final void write(byte b[]) throws IOException {
		if (b.length <= maxFragmentLength) {
			rawwrite(b, 23);
			return;
		}
		byte t[] = new byte[maxFragmentLength];
		int off = 0;
		while (off < b.length) {
			int add = t.length;
			if (off + add > b.length) {
				add = b.length - off;
				t = new byte[add];
			}
			System.arraycopy(b, off, t, 0, add);
			rawwrite(t, 23);
			off += add;
		}
	}

	/**
	 * Reads data into the byte array until the required length is read.
	 * 
	 * @param b
	 *            the byte array
	 * @param length
	 *            the k
	 * @return boolean true when b was completely filled
	 * @throws IOException
	 */
	final boolean readFully(byte b[], int length) throws IOException {
		if (length == 0)
			return true;

		int l = is.read(b, 0, length);
		while (l < length) {
			int b0 = is.read();
			if (b0 < 0) {
				throw new SslException(clientSessionId, "EOS");
			}
			b[l++] = (byte) b0;
			while (l < length) {
				int n = is.read(b, l, length - l);
				if (n == 0)
					break;
				if (n < 0) {
					r5[0] = 0;
					return false;
				}
				l += n;
			}
		}
		return true;
	}

	/**
	 * Reads the next SSL3 block into internal buffer. This function blocks
	 * until the complete block is read. An hack allows to read also an SSL2
	 * hello message header!
	 * 
	 * @return
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	protected final int readahead() throws IOException {
		for (;;) {
			if (!readFully(r5, 5))
				return -1;
			if (DEBUG.ON)
				Misc.dump("read header", System.out, r5);
			int len = ((r5[3] & 0xff) << 8) | (r5[4] & 0xff);
			rpos = 0;
			if (r5[1] != 3 || len <= 0) {
				// ==========================================
				// try to read a SSL2 client hello message
				if (r5[2] != 1 || r5[3] != 3)
					throw new SslException(clientSessionId, "no SSL3 packet");
				// emulate the 4 bytes since we just try to read the handshake
				// message header
				readBuffer = new byte[4];
				readBuffer[0] = 1;
				readBuffer[1] = 0;
				readBuffer[2] = (byte) (r5[0] & 0x3f);
				readBuffer[3] = r5[1];
				readBufferLength = 4;

				r5[0] = 22; // emulate handshake type
				r5[1] = 2; // set version to 2
				return 4;
				// ==========================================
			}
			if (rhashBuffer != null) {
				if (readBuffer.length < len)
					readBuffer = new byte[len];

				if (!readFully(readBuffer, len))
					return 0;

				if (cryptRead instanceof GCM) {
					readBufferLength = gcmDecrypt(r5[0], len);
					if (eekPriv != null)
						r5[0] = readBuffer[--readBufferLength]; // patch type
				} else {
					if (versionMinor > 1) {
						rpos = readIV.length;
					}
	
					cryptRead.decryptCBC(readIV, readBuffer, 0, readBuffer, 0, len);
	
					if (blockSize > 1) {
						--len;
						
						int pad = readBuffer[len];
						
						readBufferLength = len - pad - hashLen;
	
						if (readBufferLength < rpos || readBufferLength >= len)
							readBufferLength = len - hashLen; // no exception here -
																// let the hash
																// check throw it
						
						if (versionMinor > 0) {
							// check padding
							for (int i = 1; i <= pad; ++i) {
								if (readBuffer[len - i] != pad) {
									++readBuffer[rpos]; // hash check will fail
									break;
								}
							}
						}
					} else {
						readBufferLength = len - hashLen;
					}
	
					byte hb[] = calcMessageHash(readHash, hashLen, readSecret, readnum++, r5[0], readBuffer, rpos,
							readBufferLength - rpos);
					if (!equals(readBuffer, readBufferLength, hb, 0, hashLen))
						throw new SslException(clientSessionId, "MAC error");
				}
				
				len = readBufferLength - rpos;
				// handle alerts
				if (r5[0] == 21) {
					readBufferLength = rpos;
					if (readBuffer[rpos] != 1)
						throw new SslException(clientSessionId, "SSL/TLS alert: " + (0xff & readBuffer[rpos + 1]));
					if (readBuffer[rpos + 1] == 0) {
						is.close();
						return 0;
					}
					continue;
				}
				
			} else {
				readBufferLength = len;
				readBuffer = new byte[len];
				if (!readFully(readBuffer, len))
					return 0;
			}
			return len;
		}
	}

	/**
	 * Reads into the given byte array from input stream with blocking.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param typ
	 *            the type of the read packet
	 * @return count of read bytes
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs, an alert is
	 *                received or a type mismatch is detected.
	 */
	final int rawread(byte b[], int blen, int typ) throws IOException {
		for (int i = 0; i < blen;) {
			if (rpos < readBufferLength) {
				int rbytes = blen;
				if (rbytes > readBufferLength - rpos)
					rbytes = readBufferLength - rpos;
				System.arraycopy(readBuffer, rpos, b, i, rbytes);
				i += rbytes;
				rpos += rbytes;
			}
			if (i < blen) {
				final int read = readahead();
				if (read == 0)
					continue;
				if (read < 0) {
					if (i > 0) {
						blen = i;
						break;
					}
					return -1;
				}
				if (r5[0] != typ) {
					if (r5[0] == 0) {
						return 0;
					}
					if (r5[0] == 21) // alert!
					{
						// close of connection received?
						if (readBuffer[0] == 1 && readBuffer[1] == 0) {
							close();
							return 0;
						}
						throw new SslException(clientSessionId, "alert: " + readBuffer[0] + ":" + readBuffer[1]);
					}
					if (r5[0] == 22) { // some handshake message!?
						int len = r5[3] & 0xff;
						len = (len << 8) | (r5[4] & 0xff);
						updateHandshakeHashes(readBuffer, rpos, len);
						handleHandshakeMessage(readBuffer, rpos, len);
						rpos += len;
					} else
						throw new SslException(clientSessionId, "wrong packet type: " + r5[0]);
				}
			}
		}
		// test r5[0] since TLS1.3 will patch this
		if (collect && r5[0] == 22) {
			// cumulate handshake messages
			updateHandshakeHashes(b, 0, blen);
		}
		return blen;
	}

	// override me
	protected void handleHandshakeMessage(byte[] buffer, int offset, int len) {
	}

	/**
	 * Reads an array for a handshake message from input stream with blocking.
	 * 
	 * @return a new buffer into which the data was read.
	 * @param msgType
	 *            the handshake message type of the read packet
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	final byte[] hs_read(int msgType) throws IOException {
		int rlen = rawread(head, head.length, 22);
		if (DEBUG.ON)
			Misc.dump("inner header", System.out, head);
		if (head.length != rlen)
			throw new SslException(clientSessionId, "handshake");

		if (msgType != head[0])
			throw new SslException(clientSessionId, "handshake");

		return innerRead();
	}

	final byte[] innerRead() throws IOException {
		int len = ((head[1] & 0xff) << 16) | ((head[2] & 0xff) << 8) | (head[3] & 0xff);

		if (r5[1] == 2) { // got an emulated version 2 packet?
							// ==========================================
							// try to read a SSL2 client hello message
			--len;
			byte b[] = new byte[len]; // reassemble the hello packet
			b[0] = r5[3];
			b[1] = r5[4];
			for (int l = 2; l < len;)
				l += is.read(b, l, len - l); // read the rest

			pendingHandshake = NULLBYTES;
			if (hsSha != null) {
				if (hsMd5 != null) {
					hsMd5.reset();
				}
				hsSha.reset();
			}

			updateHandshakeHashes(r5, 2, 1);
			updateHandshakeHashes(b, 0, b.length);

			// Trace.dump(System.out, r5);
			// Trace.dump(System.out, b);

			b[0] = 2; // it's a version 2 handshake!
			return b;
			// ==========================================
		}
		if (len == 0)
			return NULLBYTES;
		byte b[] = new byte[len];
		if (len != rawread(b, len, 22))
			return null;

		return b;
	}

	/**
	 * Write the array to the output stream using the given packet type.
	 * 
	 * @param b
	 *            the bytes to sent
	 * @param typ
	 *            the packet type of the sent packet
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs.
	 */
	final void rawwrite(byte b[], int typ) throws IOException {
		int len = b.length;
		
		if (cryptWrite instanceof GCM) {
			int outLen = gcmEncrypt(b, typ, len);
			os.write(writeBuffer, 0, outLen);
			return;
		}
		
		int offset = 0;
		if (versionMinor > 1 && writeIV.length > 0) {
			// add a random explicit writeIV - since TLS 1.1
			offset = writeIV.length;
			byte tmp[] = new byte[len + offset];
			secureRnd.nextBytes(tmp, 0, offset);
			if (DEBUG.USE_TEST_DATA)
				for (int ii = 0; ii < offset; ++ii)
					tmp[ii] = (byte) ii;
			System.arraycopy(b, 0, tmp, offset, len);

			b = tmp;
			len = b.length;
		}

		byte hb[] = calcMessageHash(writeHash, hashLen, writeSecret, writenum++, typ, b, offset, b.length - offset);

		offset += 5;
		// without padding
		if (blockSize == 1) {
			final int useLen = len + hashLen;
			int outLen = useLen + 5;
			if (writeBuffer.length < outLen)
				writeBuffer = new byte[outLen];

			byte w[] = writeBuffer;
			w[0] = (byte) typ;
			w[1] = (byte) 3; // version
			w[2] = versionMinor;
			w[3] = (byte) ((useLen) >>> 8);
			w[4] = (byte) (useLen);

			cryptWrite.encryptCBC(writeIV, b, 0, w, offset, len);
			cryptWrite.encryptCBC(writeIV, hb, 0, w, offset + len, hashLen);
			os.write(w, 0, outLen);
			return;
		}

		// add the padding to the length
		int pad = 0;
		len += hashLen;
		int rem = (len + 1) % blockSize;
		pad = blockSize - rem;
		if (pad == blockSize)
			pad = 0;
		len += pad + 1;

		int outLen = len + 5;

		if (writeBuffer.length < outLen)
			writeBuffer = new byte[outLen];

		byte w[] = writeBuffer;
		w[0] = (byte) typ;
		w[1] = (byte) 3; // version
		w[2] = versionMinor;
		w[3] = (byte) (len >>> 8);
		w[4] = (byte) (len);

		System.arraycopy(b, 0, w, 5, b.length);
		System.arraycopy(hb, 0, w, 5 + b.length, hashLen);
		for (int i = 1; i <= pad; ++i) {
			w[len - i + 4] = (byte) pad; // secureRnd.next(8);
		}
		writeBuffer[len + 4] = (byte) pad;
		cryptWrite.encryptCBC(writeIV, writeBuffer, 5, writeBuffer, 5, len);
		
		os.write(writeBuffer, 0, outLen);
	}

	/**
	 * Read from readBuffer and decrypt into readBuffer
	 * @param len
	 * @return the resulting len
	 * @throws IOException 
	 */
	private int gcmDecrypt(int typ, int len) throws IOException {
		GCM gcm = (GCM) cryptRead;
		
		byte r[] = readBuffer;
		if (DEBUG.ON)
			Misc.dump("GCM packet in: ", System.out, r, 0, len);
		
		int off;
		if (eekPriv != null) {
			long rn = readnum > 0 ? readnum - 1 : 0;
			rn ^= readnum++;
			for (int i = 0; rn != 0 && i < readIV.length; ++i) {
				readIV[readIV.length - i - 1] ^= (byte)rn;
				rn >>>= 8;
			}
			gcm.init(readIV);
			gcm.updateHash(r5, 0, r5.length);
			off = 0;
			len -= 16;
		} else {
			readNonce[4] = r[0];
			readNonce[5] = r[1];
			readNonce[6] = r[2];
			readNonce[7] = r[3];
			readNonce[8] = r[4];
			readNonce[9] = r[5];
			readNonce[10] = r[6];
			readNonce[11] = r[7];
			
			gcm.init(readNonce);
			if (DEBUG.ON)
				Misc.dump("read nonce: ", System.out, readNonce, 0, 12);
	
			
			len -= 24;
			off = 8;
			
			readAad[8] = (byte)typ;
			readAad[11] = (byte)(len >> 8);
			readAad[12] = (byte)len;
			gcm.updateHash(readAad, 0, readAad.length);

			if (DEBUG.ON)
				Misc.dump("read aad: ", System.out, readAad, 0, 13);

			// inc counter
			for (int i = 7; i >= 0; --i)
				if (++readAad[i] != 0)
					break;
		}

		gcm.decrypt(r, off, r, 0, len);

		if (DEBUG.ON)
			Misc.dump("decoded GCM packet: ", System.out, r, 0, len);

		// append hash
		gcm.calcHash(t16, 0);
		if (DEBUG.ON)
			Misc.dump("GCM hash: ", System.out, t16, 0, 16);
		
		if (!Misc.equals(t16, 0, r, len + off, 16))
			throw new IOException("GCM MAC error");
		
		return len;
	}
	
	protected int gcmEncrypt(byte[] b, int typ, int len) {
		GCM gcm = (GCM) cryptWrite;
		int inOff;
		if (eekPriv != null) {
			if (writeBuffer.length < b.length + 32)
				writeBuffer = new byte[b.length + 32];
			System.arraycopy(b, 0, writeBuffer, 31, b.length);
			writeBuffer[31 + b.length] = (byte) typ;
			inOff = 31;
			b = writeBuffer;
			++len;
		} else
			inOff = 0;
		
		int elen;
		if (eekPriv != null)
			elen = len + 16; // + hash + typ is part of b!
		else
			elen = len + 8 + 16; // iv + hash
			
		
		int outLen = elen + 5; // header

		if (writeBuffer.length < outLen)
			writeBuffer = new byte[outLen];
		
		byte w[] = writeBuffer;
		w[1] = (byte) 3; // version
		w[3] = (byte) (elen >>> 8);
		w[4] = (byte) (elen);


		int off;
		if (eekPriv != null) {
			w[0] = 0x17;
			w[2] = 3;
			
			long wn = writenum > 0 ? writenum - 1 : 0;
			wn ^= writenum++;
			for (int i = 0; wn != 0 && i < writeIV.length; ++i) {
				writeIV[writeIV.length - i - 1] ^= (byte)wn;
				wn >>>= 8;
			}
			gcm.init(writeIV);
			gcm.updateHash(w, 0, 5);
			
			off = 5;
		} else {
			w[0] = (byte) typ;
			w[2] = versionMinor;
		

			w[5] = writeNonce[4];
			w[6] = writeNonce[5];
			w[7] = writeNonce[6];
			w[8] = writeNonce[7];
			w[9] = writeNonce[8];
			w[10] = writeNonce[9];
			w[11] = writeNonce[10];
			w[12] = writeNonce[11];

			gcm.init(writeNonce);

			if (DEBUG.ON)
				Misc.dump("write nonce: ", System.out, writeNonce, 0, 12);

			
			for (int i = 11; i >= 4; --i)
				if (++writeNonce[i] != 0)
					break;

//				laufende nummer 8 bytes
//				type   22 (packet type)
//				major
//				minor
//				hi(len)   des Pakets - ohne expl iv
//				lo(len)

			writeAad[8] = (byte)typ;
			writeAad[11] = (byte)(len >> 8);
			writeAad[12] = (byte)len;
			gcm.updateHash(writeAad, 0, writeAad.length);

			if (DEBUG.ON)
				Misc.dump("write aad: ", System.out, writeAad, 0, 13);

			// inc counter
			for (int i = 7; i >= 0; --i)
				if (++writeAad[i] != 0)
					break;

			off = 13;
		}
		
		gcm.encrypt(b, inOff, w, off, len);
		
		// append hash
		gcm.calcHash(w, len + off);
		
		if (DEBUG.ON)
			Misc.dump("GCM packet: ", System.out, writeBuffer, 0, outLen);
		return outLen;
	}

	/**
	 * Add the headers and write the message in writeBuffer.
	 * 
	 * @param msgType
	 *            the message type
	 * @param length
	 *            the length of the message, buffer's size is at least len + 9
	 * @throws IOException
	 *             / final void hsWrite(int msgType, int length, int add) throws
	 *             IOException { byte b[] = writeBuffer; b[5] = (byte)0x
	 *             msgType; b[6] = (byte)0x (length >> 16); b[7] = (byte)0x
	 *             (length >> 8); b[8] = (byte)0x (length);
	 * 
	 *             length += 4; if (DEBUG.ON) Misc.dump("write", System.out, b,
	 *             5, length); hsMd5.update(b, 5, length); hsSha.update(b, 5,
	 *             length ); if (DEBUG.ON) Misc.dump("hsMd5", System.out,
	 *             ((MD5)hsMd5.clone()).digest());
	 * 
	 *             b[0] = (byte)0x 22; b[1] = (byte)0x 3; // version b[2] =
	 *             (byte)0x 0; b[3] = (byte)0x (length >>> 8); b[4] = (byte)0x
	 *             (length); os.write(b, 0, length + 5 + add); }
	 * 
	 *             /** Write the array to the output stream as an handshake
	 *             packet.
	 * @param b
	 *            the bytes to sent
	 * @param msgType
	 *            the handshake message type of the sent packet
	 * @exception java.io.IOException
	 *                throws an IOException if an I/O Error occurs. / final void
	 *                hs_write(byte b[], int msgType) throws IOException { byte
	 *                w0[] = new byte[b.length + 4]; w0[0] = (byte)0x msgType;
	 *                w0[1] = (byte)0x (b.length >> 16); w0[2] = (byte)0x
	 *                (b.length >> 8); w0[3] = (byte)0x (b.length);
	 *                System.arraycopy(b, 0, w0, 4, b.length); rawwrite(w0, 22);
	 *                }
	 * 
	 *                /** calculate a message MAC. hash(secret + pad_2 + hash
	 *                (secret + pad_1 + seq_num + typ + length + content)); this
	 *                function is synchronized since the same MessageDigest
	 *                object is used for reading and writing.
	 * @param md
	 *            the MessageDigest which is used to caclulate the MAC
	 * @param hlen
	 *            length for the given hash
	 * @param secret
	 *            the secret for this MAC (see formula)
	 * @param seqNum
	 *            the sequence Number for this MAC (see formula)
	 * @param typ
	 *            the packet type for this MAC (see formula)
	 * @param b
	 *            the message content for this MAC (see formula)
	 * @return a new allocated byte array containing the MAC
	 */
	final synchronized byte[] calcMessageHash(MessageDigest md, int hlen, byte secret[], long seqNum, int typ, byte b[],
			int boffset, int blength) {

		// since TLS1.0
		if (versionMinor != 0) {
			byte b13[] = new byte[13];
			for (int i = 0; i < 8; i++) {
				b13[i] = (byte) ((seqNum >>> (((7 - i) * 8))) & 0xff);
			}
			b13[8] = (byte) typ;
			b13[9] = 3;
			b13[10] = versionMinor;
			b13[11] = (byte) (blength >>> 8);
			b13[12] = (byte) (blength);
			byte t[] = new byte[blength];
			System.arraycopy(b, boffset, t, 0, blength);
			return md.hmac(secret, b13, t);
		}

		// SSL 3.0
		md.update(secret);
		for (int i = 0; i < 80 - 2 * hlen; ++i)
			md.update((byte) 0x36);
		for (int i = 0; i < 8; i++) {
			md.update((byte) ((seqNum >>> (((7 - i) * 8))) & 0xff));
		}
		md.update((byte) typ);
		md.update((byte) (blength >>> 8));
		md.update((byte) (blength));
		md.update(b, 0, blength);
		byte hs[] = md.digest();
		md.update(secret);
		for (int i = 0; i < 80 - 2 * hlen; ++i)
			md.update((byte) 0x5c); // 5c
		md.update(hs);
		return md.digest();
	}

	/**
	 * create some hash bytes md5(x + sha('A' + x + ra + readBuffer)) + md5(x +
	 * sha('BB' + x + ra + readBuffer)) + md5(x + sha('CCC' + x + ra +
	 * readBuffer)) + ...
	 * 
	 * @param n
	 *            the number of needed hash bytes
	 * @param x
	 *            an input parameter(see formula)
	 * @param ra
	 *            an input parameter(see formula)
	 * @param input
	 *            an input parameter(see formula)
	 * @return a new allocated byte array containing the hash bytes
	 */
	final byte[] makeHashBytes(byte x[], int n, byte ra[], byte input[]) {
		byte r[] = new byte[(n + 15) & ~15];
		for (int i = 0; i * 16 < n; ++i) {
			for (int j = 0; j <= i; ++j)
				sha.update((byte) (0x41 + i));
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb1: ", System.out, sha.clone().digest());
			sha.update(x);
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb2: ", System.out, sha.clone().digest());
			sha.update(ra);
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb3: ", System.out, sha.clone().digest());
			sha.update(input);
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb4: ", System.out, sha.clone().digest());
			md5.update(x);
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb5: ", System.out, md5.clone().digest());
			md5.update(sha.digest());
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("mhb6: ", System.out, md5.clone().digest());
			System.arraycopy(md5.digest(), 0, r, i * 16, 16);
		}
		byte z[] = new byte[n];
		System.arraycopy(r, 0, z, 0, n);
		return z;
	}

	/**
	 * calculate a handshake MAC. hash(masterSecret + pad2 + hash(handshake +
	 * sender + mastersecret + pad1));
	 * 
	 * @param md
	 *            the MessageDigest which is used to caclulate the MAC
	 * @param len
	 *            the count of pad_x bytes (see formula)
	 * @param sender
	 *            the sender of this message (see formula)
	 * @return a new allocated byte array containing the MAC
	 */
	final byte[] calc_hs_hash(MessageDigest md, int len, byte sender[]) {
		md.update(sender); // handshake + sender
		md.update(masterSecret); // handshake + sender + mastersecret
		for (int i = 0; i < len; ++i)
			// handshake + sender + mastersecret + pad1
			md.update((byte) 0x36);
		byte b[] = md.digest();
		md.update(masterSecret);
		for (int i = 0; i < len; ++i)
			// masterSecret + pad2
			md.update((byte) 0x5c);
		md.update(b);
		return md.digest();
		// masterSecret + pad2 + md5(handshake + sender + mastersecret + pad1)
	}

	/**
	 * create the key and secret material.
	 * 
	 * @param isServer
	 *            indicates whether they are generated for a server or a client
	 * @param hlen
	 *            length for the new hash
	 */
	final void createKeys(boolean isServer) {
		byte[] cs = ciphersuites[cipherIndex];
		int keymat = cs[2];
		switch (cs[4]) {
		case 1:
			this.readHash = new MD5();
			this.writeHash = new MD5();
			this.hashLen = 16;
			break;
		case 2:
			this.readHash = new SHA();
			this.writeHash = new SHA();
			this.hashLen = 20;
			break;
		case 4:
			this.readHash = new SHA256();
			this.writeHash = new SHA256();
			this.hashLen = 32;
			break;
		case 5:
			this.readHash = new SHA384();
			this.writeHash = new SHA384();
			this.hashLen = 48;
			break;
		}
		switch (cs[3]) {
		case 0:
			this.cryptRead = new RC4();
			this.cryptWrite = new RC4();
			break;
		case 1:
			this.cryptRead = new AES();
			this.cryptWrite = new AES();
			break;
		case 2:
			this.cryptRead = new DES3();
			this.cryptWrite = new DES3();
			break;
		case 3:
			this.cryptRead = new DES();
			this.cryptWrite = new DES();
			break;
		case 4:
			this.cryptRead = new GCM(new AES());
			this.cryptWrite = new GCM(new AES());
			hashLen = 0; // not used
			writeAad = new byte[13];
			writeAad[9] = 3;
			writeAad[10] = versionMinor;
			readAad = new byte[13];
			readAad[9] = 3;
			readAad[10] = versionMinor;
			break;
		}
		this.readSecret = new byte[hashLen];
		this.writeSecret = new byte[hashLen];
		this.blockSize = cryptRead.blockSize;
		if (blockSize == 1) {
			this.readIV = NULLBYTES;
			this.writeIV = NULLBYTES;
		} else {
			this.readIV = new byte[blockSize];
			this.writeIV = new byte[blockSize];
		}

		// calculate the keys
		byte srk[] = new byte[keymat];
		byte swk[] = new byte[keymat];

		int keyMaterialLength = 2 * (hashLen + keymat);
		// add room for implicit IVs
		keyMaterialLength += readIV.length + readIV.length;

		byte b[];
		if (versionMinor != 0) {
			b = PRF(keyMaterialLength, masterSecret, "key expansion", serverRandom, clientRandom);
		} else {
			b = makeHashBytes(masterSecret, keyMaterialLength, serverRandom, clientRandom);
		}
		System.arraycopy(b, 0, isServer ? readSecret : writeSecret, 0, hashLen);
		System.arraycopy(b, hashLen, isServer ? writeSecret : readSecret, 0, hashLen);
		System.arraycopy(b, 2 * hashLen, srk, 0, keymat);
		System.arraycopy(b, 2 * hashLen + keymat, swk, 0, keymat);

		System.arraycopy(b, 2 * (hashLen + keymat), isServer ? readIV : writeIV, 0, readIV.length);
		System.arraycopy(b, 2 * (hashLen + keymat) + readIV.length, isServer ? writeIV : readIV, 0, readIV.length);
		// export version: recalculate the final keys
		if (keymat < 16) {
			md5.update(srk);
			md5.update(clientRandom);
			md5.update(serverRandom);
			srk = md5.digest();
			md5.update(swk);
			md5.update(serverRandom);
			md5.update(clientRandom);
			swk = md5.digest();
		}
		// set cryptkeys
		if (isServer) {
			cryptRead.setKey(srk);
			cryptWrite.setKey(swk);
		} else {
			cryptRead.setKey(swk);
			cryptWrite.setKey(srk);
		}

		if (cryptRead instanceof GCM) {
			writeNonce = new byte[12];
			writeNonce[0] = writeIV[0];
			writeNonce[1] = writeIV[1];
			writeNonce[2] = writeIV[2];
			writeNonce[3] = writeIV[3];
			secureRnd.nextBytes(writeNonce, 4, 8);
			
			readNonce = new byte[12];
			readNonce[0] = readIV[0];
			readNonce[1] = readIV[1];
			readNonce[2] = readIV[2];
			readNonce[3] = readIV[3];
		}
	}

	/**
	 * create the early key and secret material in TLS1.3
	 * 
	 * @param isServer
	 *            indicates whether they are generated for a server or a client
	 * @param hlen
	 *            length for the new hash
	 */
	void createTls13EarlyKeys(boolean isServer) {
		this.cryptRead = new GCM(new AES());
		this.cryptWrite = new GCM(new AES());

		byte[] cs = ciphersuites[cipherIndex];
		switch (cs[4]) {
		case 4:
			this.readHash = new SHA256();
			this.writeHash = new SHA256();
			this.hashLen = 32;
			break;
		case 5:
			this.readHash = new SHA384();
			this.writeHash = new SHA384();
			this.hashLen = 48;
			break;
		}
		int keyLen = cs[2];

    	byte[] helloHash = hsSha.clone().digest();
		byte[] earlySecret = readHash.hmac(new byte[1], new byte[hashLen]);
		byte[] emptyHash = readHash.digest();
		byte[] derivedSecret = readHash.expandLabel(earlySecret, "tls13 derived", emptyHash, hashLen);
		handshakeSecret = readHash.hmac(derivedSecret, masterSecret);
		clientSecret = readHash.expandLabel(handshakeSecret, "tls13 c hs traffic", helloHash, hashLen);
    	serverSecret = readHash.expandLabel(handshakeSecret, "tls13 s hs traffic", helloHash, hashLen);
    	byte[] clientKey = readHash.expandLabel(clientSecret, "tls13 key", NULLBYTES, keyLen);
    	byte[] serverKey = readHash.expandLabel(serverSecret, "tls13 key", NULLBYTES, keyLen);

    	byte[] clientIv = readHash.expandLabel(clientSecret, "tls13 iv", NULLBYTES, 12);
    	byte[] serverIv = readHash.expandLabel(serverSecret, "tls13 iv", NULLBYTES, 12);
		
    	Misc.dump("helloHash", System.out, helloHash);
    	Misc.dump("derivedSecret", System.out, derivedSecret);
    	Misc.dump("handshakeSecret", System.out, handshakeSecret);
    	Misc.dump("clientSecret", System.out, clientSecret);
    	Misc.dump("serverSecret", System.out, serverSecret);
    	Misc.dump("clientKey", System.out, clientKey);
    	Misc.dump("serverKey", System.out, serverKey);
    	Misc.dump("clientIv", System.out, clientIv);
    	Misc.dump("serverIv", System.out, serverIv);
    	
		// set cryptkeys
		if (isServer) {
			cryptRead.setKey(clientKey);
			cryptWrite.setKey(serverKey);
			readIV = clientIv;
			writeIV = serverIv;
		} else {
			cryptRead.setKey(serverKey);
			cryptWrite.setKey(clientKey);
			readIV = serverIv;
			writeIV = clientIv;
		}
	}

	protected void createTls13Keys(boolean isServer, MessageDigest lastHsHash) {
		this.cryptRead = new GCM(new AES());
		this.cryptWrite = new GCM(new AES());

		byte[] cs = ciphersuites[cipherIndex];
		switch (cs[4]) {
		case 4:
			this.readHash = new SHA256();
			this.writeHash = new SHA256();
			this.hashLen = 32;
			break;
		case 5:
			this.readHash = new SHA384();
			this.writeHash = new SHA384();
			this.hashLen = 48;
			break;
		}
		int keyLen = cs[2];

    	byte[] handshakeHash = lastHsHash.digest();
		byte[] emptyHash = readHash.digest();
		byte[] derivedSecret = readHash.expandLabel(handshakeSecret, "tls13 derived", emptyHash, hashLen);
		masterSecret = readHash.hmac(derivedSecret, new byte[hashLen]);
    	
    	Misc.dump("masterSecret", System.out, masterSecret);
		
		clientSecret = readHash.expandLabel(masterSecret, "tls13 c ap traffic", handshakeHash, hashLen);
    	serverSecret = readHash.expandLabel(masterSecret, "tls13 s ap traffic", handshakeHash, hashLen);
    	byte[] clientKey = readHash.expandLabel(clientSecret, "tls13 key", NULLBYTES, keyLen);
    	byte[] serverKey = readHash.expandLabel(serverSecret, "tls13 key", NULLBYTES, keyLen);

    	byte[] clientIv = readHash.expandLabel(clientSecret, "tls13 iv", NULLBYTES, 12);
    	byte[] serverIv = readHash.expandLabel(serverSecret, "tls13 iv", NULLBYTES, 12);
		
		// set cryptkeys
		if (isServer) {
			cryptRead.setKey(clientKey);
			cryptWrite.setKey(serverKey);
			readIV = clientIv;
			writeIV = serverIv;
		} else {
			cryptRead.setKey(serverKey);
			cryptWrite.setKey(clientKey);
			readIV = serverIv;
			writeIV = clientIv;
		}
		readnum = writenum = 0;
	}

	
	/**
	 * sends a closure notification, prior to close the streams.
	 * 
	 * @see de.bb.minissl.SslBase#close()
	 */
	void close() throws IOException {
		// send close_notify - and close all
		try {
			if (writeIV != null)
				rawwrite(ALERTCLOSE, 21);
		} catch (IOException ioe) {
		}
		is.close();
		os.flush();
		os.close();
	}

	/**
	 * Assign InputStream and OutputStream for reading and writing to the
	 * underlying layer (often socket streams).
	 * 
	 * @param i
	 *            the assigned InputStream
	 * @param o
	 *            the assigned OutputStream
	 */
	protected final void setStreams(InputStream i, OutputStream o) {
		is = i;
		os = o;
	}

	/**
	 * Query the used ciphertype.
	 * 
	 * @return the used ciphertype 3 = SSL_RSA_WITH_RC4_40_MD5 4 =
	 *         SSL_RSA_WITH_RC4_128_MD5 5 = SSL_RSA_WITH_RC4_128_SHA
	 */
	public final int getCipherType() {
		if (cipherIndex < 0 || cipherIndex >= ciphersuites.length)
			return 0;
		return 0xff & ciphersuites[cipherIndex][1];
	}

	public final String getCipherSuite() {
		if (cipherIndex >= 0 && cipherIndex < ciphersuites.length) {
			final byte[] cs = ciphersuites[cipherIndex];
			for (int i = 0; i < CIPHERSUITES.length; ++i) {
				if (CIPHERSUITES[i][0] == cs[0] && CIPHERSUITES[i][1] == cs[1])
					return CIPHERSUITENAMES[i];
			}
		}
		return "<none>";
	}

	/**
	 * Returns an input stream for this Ssl connection.
	 * 
	 * @return a stream for reading from this Ssl connection.
	 */
	public final InputStream getInputStream() {
		if (myIn == null)
			myIn = new SslInputStream(this);
		return myIn;
	}

	/**
	 * Returns an input stream for this Ssl connection.
	 * 
	 * @return a stream for reading from this Ssl connection.
	 */
	public final OutputStream getOutputStream() {
		if (myOut == null)
			myOut = new SslOutputStream(this);
		return myOut;
	}

	/**
	 * return the vector of received certificates.
	 * 
	 * @return a vector with all received certificates
	 */
	public final Vector<byte[]> getCertificates() {
		return certs;
	}

	/**
	 * flush the output stream.
	 * 
	 * @throws java.io.IOException
	 *             throws an IOException if an I/O Error occurs.
	 */
	final void flush() throws IOException {
		os.flush();
	}

	/**
	 * Returns the number of bytes that can be read from this input stream
	 * without blocking.
	 * 
	 * @return the number of bytes that can be read from this input stream
	 *         without blocking.
	 * @throws java.io.IOException
	 *             throws an IOException if an I/O Error occurs.
	 */
	final int available() throws IOException {
		if (rpos < readBufferLength)
			return readBufferLength - rpos;
		if (is.available() < head.length + hashLen)
			return 0;
		readahead();
		return readBufferLength - rpos;
	}

	/**
	 * replace all zeros with a new random value != 0.
	 * 
	 * @param b
	 *            a byte array wherein all zeros are replaced by non zero random
	 *            values
	 */
	final static void unzero(byte b[]) {
		secureRnd.nextBytes(b);
		for (int i = 0; i < b.length; ++i) {
			if (DEBUG.USE_TEST_DATA)
				b[i] = (byte) 0xee;
			while (b[i] == 0) {
				b[i] = (byte) secureRnd.nextInt();
			}
		}
	}

	/**
	 * compare 2 bytearrays.
	 * 
	 * @param a
	 *            first byte array
	 * @param ai
	 *            offset into first byte array
	 * @param b
	 *            second byte array
	 * @param bi
	 *            offset into second byte array
	 * @param len
	 *            the count of bytes to compare
	 * @return true if the compared data are equal. false if not
	 */
	final static boolean equals(byte a[], int ai, byte b[], int bi, int len) {
		if (a == null)
			return b == null;
		if (b == null)
			return false;
		if (a.length < len)
			return false;
		for (int i = 0; i < len; ++i)
			if (a[i + ai] != b[i + bi])
				return false;
		return true;
	}

	/**
	 * Get some ASN.1 data from a sequence. The path contains the types of the
	 * elements When 0x80 is or'd to the type, it means, that the given element
	 * is entered some examples path = 0x10: the first sequence is searched and
	 * return with header it IS the complete sequence path = 0x90: the first
	 * sequence is searched and its content is returned it IS the competet
	 * content of the sequence WITHOUT header path = 0x10, 0x90, 0x84: 0x10,
	 * search first sequence, do not enter 0x90, search next sequence, ENTER
	 * that sequence 0x84, search bitstring, return its content!
	 * 
	 * @param b
	 *            the sequence which is searched.
	 * @param s
	 *            the path to the searched element.
	 * @param off
	 *            an offset into b.
	 * @return a new allocated byte array which represent the searched element,
	 *         or null when not found.
	 */
	// ===========================================================================
	static final byte[] getSeq(byte b[], int s[], int off) {
		if (b == null)
			return null;
		int typ, len = 0, end = b.length;
		try {
			for (int i = 0; i < s.length;) {
				int start = off;
				typ = b[off++] & 0x1f;
				len = b[off++];
				if (len < 0) {
					int n = len & 0x7f;
					len = 0;
					while (n-- > 0) {
						len = (len << 8) | (b[off++] & 0xff);
					}
					if (len == 0) // no length given -> use the rest!
						len = end - off;
				}
				if (s[i] == typ) {
					++i;
					if (i == s.length) {
						len += off - start;
						off = start;
					}
				} else if ((0x7f & s[i]) == typ) {
					++i;
					end = off + len;
					continue;
				}
				if (i < s.length)
					off += len;
			}
			byte r[] = new byte[len];
			System.arraycopy(b, off, r, 0, len);
			return r;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Insert 4 bytes of a handshake message header at given offset. Rest of
	 * message follows 4 bytes behind the header and has the given length
	 * 
	 * @param msgType
	 *            the message type
	 * @param b
	 *            a buffer
	 * @param offset
	 *            offset to add the header
	 * @param length
	 *            of the message
	 */
	final void addHandshakeHeader(int msgType, byte b[], int offset, int length) {
		b[offset] = (byte) msgType;
		b[offset + 1] = (byte) (length >> 16);
		b[offset + 2] = (byte) (length >> 8);
		b[offset + 3] = (byte) (length);

		length += 4;

		updateHandshakeHashes(b, offset, length);
	}

	protected void updateHandshakeHashes(byte[] b, int offset, int length) {
		if (hsSha != null) {
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("DigestUpdate", System.out, b, offset, length);
			if (hsMd5 != null)
				hsMd5.update(b, offset, length);
			hsSha.update(b, offset, length);
			if (DEBUG.HANDSHAKEHASH)
				Misc.dump("hsSha", System.out, hsSha.clone().digest());
		} else {
			byte t[] = new byte[length + pendingHandshake.length];
			System.arraycopy(pendingHandshake, 0, t, 0, pendingHandshake.length);
			System.arraycopy(b, offset, t, pendingHandshake.length, length);
			pendingHandshake = t;
		}
	}

	/**
	 * Insert 5 bytes of a packet message header at given offset. Rest of
	 * message follows 5 bytes behind the header and has the given length
	 * 
	 * @param packetType
	 *            the packet type
	 * @param b
	 *            a buffer
	 * @param offset
	 *            offset to add the header
	 * @param length
	 *            of the message
	 */
	final void addMessageHeader(int packetType, byte b[], int offset, int length) {
		b[offset] = (byte) packetType;
		b[offset + 1] = (byte) 3; // version
		b[offset + 2] = versionMinor;
		b[offset + 3] = (byte) (length >>> 8);
		b[offset + 4] = (byte) (length);
	}

	protected byte[] PRF(int length, byte[] secret, String id, byte[] add1, byte[] add2) {

		int idlen = id.length();
		byte data[] = new byte[idlen + add1.length + add2.length];
		id.getBytes(0, idlen, data, 0);
		System.arraycopy(add1, 0, data, idlen, add1.length);
		System.arraycopy(add2, 0, data, idlen + add1.length, add2.length);

		if (DEBUG.ON)
			Misc.dump("PRF IN", System.out, data);

		byte r[];
		if (versionMinor < 3) {
			// SSL3.0 - TLS 1.1
			// get the 2 halves of the preMasterSecret
			int partLen = (secret.length + 1) >>> 1;
			byte shaSecret[] = new byte[partLen];
			byte md5Secret[] = new byte[partLen];
			System.arraycopy(secret, 0, md5Secret, 0, partLen);
			System.arraycopy(secret, secret.length - partLen, shaSecret, 0, partLen);

			r = pHash(length, sha, shaSecret, data);
			byte r1[] = pHash(length, md5, md5Secret, data);
			for (int i = 0; i < r.length; ++i) {
				r[i] ^= r1[i];
			}
		} else {
			// TLS 1.2 is using SHA only /?
			r = pHash(length, prfMd, secret, data);
		}
		if (DEBUG.ON)
			Misc.dump("PRF", System.out, r);
		return r;
	}

	protected byte[] serverFinishedHash() {
		return finishedHash("server finished", SERVER);
	}

	protected byte[] clientFinishedHash() {
		return finishedHash("client finished", CLIENT);
	}

	private byte[] finishedHash(String cs, byte[] csData) {
		// calculate the hashes for the client finished masterSecret
		if (versionMinor != 0) {
			// TLS
			byte b[] = new byte[16];
			byte[] hsNowMd5 = hsMd5 != null ? hsMd5.clone().digest() : NULLBYTES;
			byte[] hsNowSha = hsSha.clone().digest();
			byte h[] = PRF(12, masterSecret, cs, hsNowMd5, hsNowSha);
			System.arraycopy(h, 0, b, 4, 12);
			return b;
		}
		// SSL 3.0
		byte cmd5hash[] = calc_hs_hash(hsMd5.clone(), 48, csData);
		byte cshahash[] = calc_hs_hash(hsSha.clone(), 40, csData);
		byte h[] = new byte[40];
		System.arraycopy(cmd5hash, 0, h, 4, 16);
		System.arraycopy(cshahash, 0, h, 20, 20);
		return h;
	}

	/**
	 * Set the max supported version. Some servers do not support newest stuff.
	 * 
	 * @param v
	 *            0 = SSL3, 1 = TLS1, 2 = TLS1.1, 3 = TLS1.2
	 */
	public void setMaxVersion(int v) {
		if (v >= 1 && v <= 3) {
			maxVersion = (byte) v;
			if (versionMinor > maxVersion)
				versionMinor = maxVersion;
		}
	}

	/**
	 * Set the min supported version. Some servers do not support old stuff.
	 * 
	 * @param v
	 *            0 = SSL3, 1 = TLS1, 2 = TLS1.1, 3 = TLS1.2
	 */
	public void setMinVersion(int v) {
		if (v >= 0 && v <= 3) {
			minVersion = (byte) v;
			if (minVersion > maxVersion)
				minVersion = maxVersion;
		}
	}

	protected byte[] createTls13HandshakeFinished(MessageDigest md, byte[] secret) {
		byte[] finishedHash = md.digest();
		byte[] finishedKey = md.expandLabel(secret, "tls13 finished", NULLBYTES, finishedHash.length);  
		byte[] verifyData = md.hmac(finishedKey, finishedHash);
		return verifyData;
	}

	protected void startHandshakeHashes(int selectedChiperSuite) {
		if (DEBUG.ON)
		    Misc.dump("hashing handshake messages", System.out, pendingHandshake);
	
		if (versionMinor >= 3) {
		    switch(ciphersuites[selectedChiperSuite][4]) {
			case 5:
				prfMd = new SHA384();
		        hsSha = new SHA384();
				break;
			default:
				prfMd = new SHA256();
		        hsSha = new SHA256();
				break;
		    }
		} else {
		    hsMd5 = new MD5();
		    hsSha = new SHA();
		    hsMd5.update(pendingHandshake, 0, pendingHandshake.length);
		}
	    hsSha.update(pendingHandshake, 0, pendingHandshake.length);
		pendingHandshake = NULLBYTES;
	}

	static byte[] pHash(int length, MessageDigest md, byte[] secret, byte[] seed) {

		byte[] ai = seed;
		byte[] r = new byte[length];

		if (DEBUG.ON)
			Misc.dump("secret", System.out, secret);
		if (DEBUG.ON)
			Misc.dump("seed", System.out, seed);

		int pos = 0;
		while (pos < length) {
			ai = md.hmac(secret, ai);
			// Misc.dump("a1", System.out, ai);
			byte d[] = md.hmac(secret, ai, seed);
			int copyLen = d.length;
			if (pos + copyLen > length)
				copyLen = length - pos;
			System.arraycopy(d, 0, r, pos, copyLen);
			pos += copyLen;
		}

		// Misc.dump("result", System.out, r);
		return r;
	}

	/**
	 * Initialize the secure random instance.
	 */
	static {
		secureRnd = SecureRandom.getInstance();
		try {
			// secureRnd = java.security.SecureRandom.getInstance("SHA1PRNG");
		} catch (Throwable e) {
		}
	}

	/*
	 * public static void main(String args[]) {
	 * 
	 * byte secret[] = Misc.hex2Bytes(
	 * "6d eb 86 48 1f f7 68 a1  d5 d7 f0 c6 ee 92 1e 52"+
	 * "21 10 05 5f cd 20 19 77  91 a9 24 f4 bb 35 8e cf" ); byte seed[] =
	 * Misc.hex2Bytes( "6d 61 73 74 65 72 20 73  65 63 72 65 74 48 fb 56"+
	 * "9e fb 8b da 61 d8 21 1c  a6 e5 94 26 b8 34 a5 97"+
	 * "0f c8 1b c9 b3 89 fd d0  3f 81 ea d8 8b 48 fb 56"+
	 * "9e e1 1a 1e 83 6b 09 7f  25 7d fc ed 59 10 a8 e2"+
	 * "ef d1 e1 5e 30 66 33 c1  0b 67 c5 5b 13" );
	 * 
	 * Misc.dump("secret", System.out, secret); Misc.dump("seed", System.out,
	 * seed);
	 * 
	 * byte r[] = pHash(48, new MD5(), secret, seed); Misc.dump("result",
	 * System.out, r);
	 * 
	 * secret = Misc.hex2Bytes(
	 * "b2 8f a0 6c 81 c0 5d be  ee 90 4f 3b 51 c5 7e fc"+
	 * "24 96 d6 8f c7 8c a3 e7  43 f4 d9 fc 81 33 b9 fa" ); r = pHash(48, new
	 * SHA(), secret, seed); Misc.dump("result", System.out, r);
	 * 
	 * 
	 * 
	 * }
	 */

	public synchronized static byte[][] getCipherSuites(String sCiphers) {
		if (CIPHERLOOKUP.isEmpty()) {
			for (int i = 0; i < CIPHERSUITENAMES.length; ++i) {
				CIPHERLOOKUP.put(CIPHERSUITENAMES[i], CIPHERSUITES[i]);
			}
		}

		if (sCiphers == null)
			return CIPHERSUITES;

		final String scs[] = sCiphers.split(",");
		final ArrayList<byte[]> r = new ArrayList<byte[]>();
		for (final String cn : scs) {
			final byte[] c = CIPHERLOOKUP.get(cn.trim());
			if (c != null)
				r.add(c);
		}
		return r.toArray(new byte[r.size()][]);
	}

	public static void clear(byte[] b) {
		for (int i = b.length - 1; i >= 0; --i)
			b[i] = 0;
	}

	protected static byte[] random(int len) {
		byte[] b = new byte[len];
		secureRnd.nextBytes(b, 0, b.length); // init with random
		
        if (DEBUG.USE_TEST_DATA) {
            for (int ii = 0; ii < b.length; ++ii) {
                b[ii] = (byte) ii;
            }
        }
		
		return b;
	}
}
