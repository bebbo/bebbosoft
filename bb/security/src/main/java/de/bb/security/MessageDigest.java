/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/MessageDigest.java,v $
 * $Revision: 1.8 $
 * $Date: 2014/06/23 15:51:15 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Base class for all message digests
 *
 *****************************************************************************/

package de.bb.security;

import de.bb.util.Misc;

/**
 * This class was partially created from Sun's source for java.security.MessageDigest because it was not shipped with
 * Netscape's JVM. Meanwhile there are many changes, so blame me for errors.
 */
public abstract class MessageDigest {
    /* the algorithm */
    private String algorithm;

    long count;
    byte data[];
    int mask;
	int countLen; // size of counter
	int end;      // where counter starts

    /**
     * Creates a message digest with the specified algorithm name.
     * 
     * @param algorithm
     *            the standard name of the digest algorithm. See Appendix A in the <a href=
     *            "../guide/security/CryptoSpec.html#AppA"> Java Cryptography Architecture API Specification &amp;
     *            Reference </a> for information about standard algorithm names.
     * @param sz
     */
    protected MessageDigest(String algorithm, int sz) {
        this.algorithm = algorithm;
        data = new byte[sz];
        mask = sz - 1;
        countLen = sz /8;
        end = sz - countLen;
    }

    /**
     * Performs a final update on the digest using the specified array of bytes, then completes the digest computation.
     * That is, this method first calls <a href = "#update(byte[])">update</a> on the array, then calls <a href =
     * "#digest()">digest()</a>.
     * 
     * @param input
     *            the input to be updated before the digest is completed.
     * 
     * @return the array of bytes for the resulting hash value.
     */
    final public byte[] digest(byte[] input) {
        update(input, 0, input.length);
        return digest();
    }

    /**
     * Returns a string that identifies the algorithm, independent of implementation details. The name should be a
     * standard Java Security name (such as "SHA", "MD5", and so on). See Appendix A in the <a href=
     * "../guide/security/CryptoSpec.html#AppA"> Java Cryptography Architecture API Specification &amp; Reference </a>
     * for information about standard algorithm names.
     * 
     * @return a string that identifies the algorithm.
     */
    public final String getAlgorithm() {
        return algorithm;
    }

    /**
     * <b>SPI</b>: Completes the hash computation by performing final operations such as padding. Once
     * <code>engineDigest</code> has been called, the engine should be reset (see <a href = "#reset">reset</a>).
     * Resetting is the responsibility of the engine implementor.
     * 
     * @return the array of bytes for the resulting hash value.
     */
    /**
     * <b>SPI</b>: Completes the hash computation by performing final operations such as padding. Once
     * <code>engineDigest</code> has been called, the engine should be reset (see <a href = "#reset">reset</a>).
     * Resetting is the responsibility of the engine implementor.
     * 
     * @return the array of bytes for the resulting hash value.
     */
    public byte[] digest() {
        long bitCount = count << 3;

        int i = (int) count & mask;
        data[i++] = (byte) 0x80;
        if (i > end) {
            for (int k = i; k < data.length; ++k)
                data[k] = 0;
            transform();
            i = 0;
        }
        for (; i < mask - (countLen - 1); ++i)
            data[i] = 0;

        addBitCount(bitCount);
        transform();

        byte digestBits[] = __getDigest();
        reset();

        return digestBits;
    }

    protected void addBitCount(long bitCount) {
    }

    protected byte[] __getDigest() {
        return null;
    }

    /**
     * add one byte to the digest. when this is implemented all of the abstract class methods end up calling this method
     * for types other than bytes.
     * 
     * @param b
     *            the byte which is added
     */
    public void update(byte b) {
        int k = (int) count++ & mask;
        data[k] = b;
        if (k == mask) {
            transform();
        }
    }

    protected void transform() {
    }

    /**
     * add the given part of the byte array to the digest
     * 
     * @param d
     *            the byte array whichs data is added
     * @param off
     *            offset into the array
     * @param len
     *            count of bytes which are added
     */
    public void update(byte d[], int off, int len) {
        int k = (int) count & mask;
        count += len;
        if (k + len < data.length) //pa???t noch rein?
        {
            System.arraycopy(d, off, data, k, len);
            return;
        }
        int n = data.length - k;
        System.arraycopy(d, off, data, k, n);
        len -= n;
        n += off;
        while (true) {
            transform();
            if (len < data.length)
                break;
            System.arraycopy(d, n, data, 0, data.length);
            len -= data.length;
            n += data.length;
        }
        System.arraycopy(d, n, data, 0, len);
    }

    public void update(byte d[]) {
        update(d, 0, d.length);
    }

    /**
     * <b>SPI</b>: Resets the digest for further use.
     */
    public void reset() {
    }

    /**
     * rotate the given value to left side
     * 
     * @param value
     *            - value to by rotateted left
     * @param bits
     *            - count of bits to rotate left
     * @return
     */
    final static protected int rol(int value, int bits) {
        return (value << bits) | (value >>> (32 - bits));
    }

    /**
     * create some hash bytes<BR> formula: MD(k ^ 0x5C, MD(k ^ 0x36, t1+t2+t3+t4+t5))<BR> tested on MD5-test-vectors
     * found in RFC 2104
     * 
     * @param k
     *            key-parameter(see formula)
     * @param tt
     *            ..text parameters(see formula)
     * @return a new allocated byte array containing the hash bytes
     */
    public final byte[] hmac(byte k[], byte[]...tt) {
        reset();
        if (k.length > data.length) {
            update(k);
            k = digest();
        }

        int i;
        for (i = 0; i < k.length; ++i)
            update((byte) (k[i] ^ 0x36));
        for (; i < data.length; ++i)
            update((byte) 0x36);

        for (byte[]t : tt) {
            update(t);
        }
        
        byte text[] = digest();
//        Misc.dump("hmac final", System.out, text);
        for (i = 0; i < k.length; ++i)
            update((byte) (k[i] ^ 0x5c));
        for (; i < data.length; ++i)
            update((byte) 0x5C);
        update(text);
        return digest();
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    abstract public MessageDigest clone();

    public static MessageDigest get(final String algo) {
        if (algo.equals("SHA256"))
            return new SHA256();
        if (algo.equals("SHA") || algo.equals("SHA1"))
            return new SHA();
        if (algo.equals("MD5"))
            return new MD5();
        if (algo.equals("RMD160"))
            return new RMD160();
        return null;
    }

    /**
	 * See TLS1.3.
     * @param salt
     * @param sid
     * @param data
     * @param len
     * @return
     */
	public byte[] expandLabel(byte[] salt, String sid, byte[] data, int len) {
		byte[] id = sid.getBytes();
		byte[] all = new byte[4 + id.length + data.length];
		
		all[0] = (byte)(len >> 8);
		all[1] = (byte)len;
		all[2] = (byte)id.length;
		System.arraycopy(id, 0, all, 3, id.length);
		all[id.length + 3] = (byte)data.length;
		System.arraycopy(data, 0, all, 4 + id.length, data.length);
		
		return expand(salt, all, len);
	}

	/**
	 * See TLS1.3.
	 * @param salt
	 * @param data
	 * @param len
	 * @return
	 */
	public byte[] expand(byte[] salt, byte[] data, int len) {
		byte[] r = new byte[len];
		byte[] d = new byte[0];
		byte[] c = new byte[1];
		for (int off = 0; off < len; off += d.length) {
			++c[0];
			d = hmac(salt, d, data, c);
			int toCopy = len - off;
			if (toCopy > d.length)
				toCopy = d.length;
			System.arraycopy(d, 0, r, off, toCopy);
		}
		return r;
	}
	
	public byte[] mgf1(byte[] seed, int len) {
		byte r[] = new byte[len];
		byte c[] = new byte[4];
		for (int off = 0;;) {
			update(seed);
			byte d[] = digest(c);
			if (off + d.length > len) {
				System.arraycopy(d, 0, r, off, len - off);
			} else {
				System.arraycopy(d, 0, r, off, d.length);
			}
			off += d.length;
			if (off >= len)
				return r;
			
			for (int i = 3; i >= 0; --i)
				if (++c[i] != 0)
					break;
		}
	}
	
	static byte Z8[] = new byte[8];
	
	public byte[] emsaPssEncode(byte m[], int emBits, int saltLen) {
		int emLen = (emBits + 7) / 8;
		byte[] mHash = digest(m);
//		mHash = Misc.hex2Bytes("FF5F7DC1  09539769  5520B514  5F471B2D"
//					+ "9FC63197  00EE1A22  83AD24A3  81B1899E");
		
		if (saltLen == -1)
			saltLen = mHash.length;
		
		byte[] salt = new byte[saltLen];
		SecureRandom.getInstance().nextBytes(salt);
//		salt = Misc.hex2Bytes("98F3ACD5  DF7257FF  1B3CC464  6BEF6345"
//				+ "E8437EB2  9021F524  BF9F71ED  75598049");
		
		// 5.
		update(Z8);
		update(mHash);
		
		// 6.
		byte[] h = digest(salt);
		
		// 7. + 8.
		// 9.
		byte[] dbMask = mgf1(h, emLen - mHash.length - 1);
		
		// 10.
		int j = dbMask.length - saltLen - 1;
		dbMask[j++] ^= 1;
		for (int i = 0; i < saltLen; ++i, ++j)
			dbMask[j] ^= salt[i];
		
		// 11. clear the bits to match m
		if (emBits != 0)
			dbMask[0] &= (1 << (emBits & 7)) - 1;
		
		// 12.
		byte[] r = new byte[emLen];
		System.arraycopy(dbMask, 0, r, 0, dbMask.length);
		System.arraycopy(h, 0, r, dbMask.length, h.length);
		r[r.length - 1] = (byte)0xbc;

		return r;
	}
	
	public boolean emsPssVerify(byte m[], int emBits, int saltLen, byte data[]) {
		int emLen = (emBits + 7) / 8;
		byte[] mHash = digest(m);
		if (saltLen == -1)
			saltLen = mHash.length;
		
		byte[] h = new byte[mHash.length];
		System.arraycopy(data, data.length - 1 - mHash.length, h, 0, mHash.length);

		byte[] dbMask = mgf1(h, emLen - mHash.length - 1);
		for (int i = 0; i < dbMask.length; ++i)
			data[i] ^= dbMask[i];

		int j = dbMask.length - saltLen - 1;
		if (data[j++] != 1)
			return false;
		
		update(Z8);
		update(mHash);
		update(data, j, saltLen);
		byte[] h2 = digest();
		byte[] h3 = digest(mHash); 
		
		return Misc.equals(h, h2);
	}
}
