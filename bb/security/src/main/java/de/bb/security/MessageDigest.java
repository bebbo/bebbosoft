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

        byte digestBits[] = getDigest();
        reset();

        return digestBits;
    }

    protected void addBitCount(long bitCount) {
    }

    protected byte[] getDigest() {
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
     * @param t1
     *            ..t5 text parameters(see formula)
     * @param t2
     * @param t3
     * @param t4
     * @param t5
     * @return a new allocated byte array containing the hash bytes
     */
    public final byte[] hmac(byte k[], byte t1[], byte t2[], byte t3[], byte t4[], byte t5[]) {
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
        if (t1 != null)
            update(t1);
        if (t2 != null)
            update(t2);
        if (t3 != null)
            update(t3);
        if (t4 != null)
            update(t4);
        if (t5 != null)
            update(t5);
        byte text[] = digest();
        //Misc.dump("hmac final", System.out, text);
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
    abstract public Object clone();

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
}

/*
 * $Log: MessageDigest.java,v $
 * Revision 1.8  2014/06/23 15:51:15  bebbo
 * @N added helper function for easy password encoding and checking using PBKDF2
 *
 * Revision 1.7  2012/08/11 19:57:00  bebbo
 * @I working stage
 *
 * Revision 1.6  2009/02/05 20:06:28  bebbo
 * @N added support for TLS 1.0
 *
 * Revision 1.5  2007/04/01 15:53:31  bebbo
 * @I cleanup
 *
 * Revision 1.4  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.3  2003/01/07 18:32:13  bebbo
 * @W removed some deprecated warnings
 *
 * Revision 1.2  2001/03/05 17:46:49  bebbo
 * @B fixed getInstance()
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */
