/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Asn1.java,v $
 * $Revision: 1.13 $
 * $Date: 2012/12/19 12:24:31 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyrights (c) by Stefan Bebbo Franke 1999-2001
 * All rights reserved
 *
 * ASN1 handling
 *
 * Based on http://home.netscape.com/eng/ssl3/draft302.txt 
 *****************************************************************************/

package de.bb.security;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.util.ByteRef;
import de.bb.util.Misc;

/**
 * Some helpful static functions to handle ASN.1 with BER/DER.
 * 
 * This class can also be used as an iterator over ASN.1 structures.
 */
public class Asn1 implements Iterator<Asn1> {
    /** OID for INTEGER. */
    final public static byte INTEGER = 2;

    /** OID for BIT_STRING. */
    final public static byte BIT_STRING = 3;

    /** OID for OCTET_STRING. */
    final public static byte OCTET_STRING = 4;

    /** OID for NULL. */
    final public static byte NULL = 5;

    /** OID for OBJECT_IDENTIFIER. */
    final public static byte OBJECT_IDENTIFIER = 6;

    public static final int UTF8String = 12;

    public static final int NumericString = 18;

    /** OID for SEQUENCE. */
    final public static byte SEQUENCE = 16;

    /** OID for SET. */
    final public static byte SET = 17;

    /** OID for PrintableString. */
    final public static byte PrintableString = 19;

    /** OID for T61String. */
    final public static byte T61String = 20;

    /** OID for IA5String. */
    final public static byte IA5String = 22;

    /** OID for UTCTime. */
    final public static byte UTCTime = 23;

    /** ODI for wide char String. */
    final public static byte UTF16String = 30;

    /** byte header to start an empty SEQUENCE. */
    final public static byte newSeq[] = {(byte) 0x30, (byte) 0x0};

    /** byte header to start an empty SET. */
    final public static byte newSet[] = {(byte) 0x31, (byte) 0x0};

    private byte[] data;

    private int offset;

    private int length;

    public final static String SPACES = "                                                                ";

    /** byte null pointer. */
    // final private static byte nul[];
    // final private static byte nullBytes[] = {};

    /**
     * Create an ASN.1 iterator.
     * 
     * @param the
     *            byte array to use
     */
    public Asn1(byte data[]) {
        this(data, 0);
    }

    /**
     * Create an ASN.1 iterator.
     * 
     * @param data
     * @param offset
     */
    public Asn1(byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
        this.length = getLen(data, offset);
    }

    private Asn1(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Get an iterator for the contained elements.
     */
    public Iterator<Asn1> children() {
        int header = getHeaderLen(data, offset);
        return new Asn1(data, offset + header, length - header);
    }

    /**
     * Return the type of the current Asn1 element.
     * 
     * @return the type of the current Asn1 element.
     */
    public int getType() {
        return 0xff & data[offset];
    }

    /**
     * Return the length of the current Asn1 element.
     * 
     * @return the length of the current Asn1 element.
     */
    public int getLength() {
        return length;
    }

    /**
     * Return true if this Asn1 contains more children. false otherwise.
     * 
     * @return true if this Asn1 contains more children, false otherwise.
     */
    public boolean hasNext() {
        return length > 0;
    }

    /**
     * Return a new Asn1 object for the next child.
     * 
     * @return a new Asn1 object for the next child.
     */
    public Asn1 next() {
        Asn1 r = new Asn1(data, offset);
        int next = r.offset + r.length;
        length -= next - offset;
        offset = next;
        return r;
    }

    /**
     * not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * return the int value for current data.
     * 
     * @return the int value for current data.
     */
    public int asInt() {
        return getInt(getData(data, offset), null);
    }

    /**
     * Return the byte data as ByteRef.
     * 
     * @return the byte data as ByteRef.
     */
    public ByteRef asByteRef() {
        return new ByteRef(getData(data, offset));
    }

    public byte[] toByteArray() {
        return makeASN1(asByteRef().toByteArray(), this.getType());
    }

    public String toString() {
        return dump(0, data, offset, length, false).toString() + "\r\n" + asByteRef();
    }

    /**
     * Helper method to get a verbose String to a ASN.1 structure.
     * 
     * @return a StringBuilder containing some text.
     */
    public static StringBuilder dump(int indent, byte[] b, int off, int length, boolean dumpData) {
        StringBuilder sb = new StringBuilder();

        while (length > 0) {
            int len = getLen(b, off);
            if (len + off > b.length) {
                sb.append(SPACES.substring(0, indent & 63));
                sb.append("#LENGTH MISMATCH: wanted ").append(len).append(" available ").append(b.length - off)
                        .append("\r\n");
                len = b.length - off;
            }
            if (len <= 0)
                return sb;
            sb.append(SPACES.substring(0, indent & 63));
            byte d[] = getData(b, off);
            final int type = b[off] & 0xff;
            switch (type & 0x1f) {
                case INTEGER:
                    sb.append("INT " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(Misc.bytes2Hex(d)).append("\r\n");
                    break;
                case BIT_STRING: {
                    sb.append("BIT " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(Misc.bytes2Hex(d));
                    ByteRef sub = new ByteRef(d);
                    if (dumpData)
                        sb.append("[" + sub + "]");
                    sb.append("\r\n");
                    if (!probe(sub.toByteArray()))
                    sub = sub.substring(1);
                    if (probe(sub.toByteArray()))
                        sb.append(dump(indent + 2, sub.toByteArray(), 0, len - 1, dumpData));
                    break;
                }
                case OCTET_STRING:
                    sb.append("OCT " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(Misc.bytes2Hex(d));
                    if (dumpData)
                        sb.append("[" + new ByteRef(d) + "]");
                    sb.append("\r\n");
                    if (probe(d))
                        sb.append(dump(indent + 2, d, 0, len, dumpData));
                    break;
                case NULL:
                    sb.append("NUL " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\r\n");
                    break;
                case OBJECT_IDENTIFIER:
                    sb.append("OID " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(oid2String(d)).append("\r\n");
                    break;
                case PrintableString:
                    sb.append("PRT " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(new String(d, 0)).append("\r\n");
                    break;
                case IA5String:
                    sb.append("IA5 " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(new String(d, 0)).append("\r\n");
                    break;
                case T61String:
                    sb.append("T61 " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(new String(d, 0)).append("\r\n");
                    break;
                case UTF8String:
                    sb.append("UTF " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    try {
                        sb.append(new String(d, "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                    }
                    sb.append("\r\n");
                    break;
                case UTCTime:
                    sb.append("UTC " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(new String(d, 0)).append("\r\n");
                    break;
                case 0:
                case SEQUENCE:
                    sb.append("SEQ " + Integer.toHexString(0xff & b[off]) + " (" + len + ")").append("\r\n");
                    if (type != 0x80) {
                        sb.append(dump(indent + 2, d, 0, len, dumpData));
                    } else {
                        sb.append(Misc.bytes2Hex(d));
                    }
                    break;
                case SET:
                    sb.append("SET " + Integer.toHexString(0xff & b[off]) + " (" + len + ")").append("\r\n");
                    sb.append(dump(indent + 2, d, 0, len, dumpData));
                    break;
                case UTF16String:
                    char ch[] = new char[d.length / 2];
                    for (int i = 0, j = 0; i < ch.length; ++i, j += 2) {
                        ch[i] = (char) ((d[j] & 0xff) << 8 | (d[j + 1] & 0xff));
                    }
                    sb.append("UTF16 " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\t");
                    sb.append(new String(ch)).append("\r\n");
                    break;
                default:
                    sb.append("??? " + Integer.toHexString(0xff & b[off]) + " (" + len + ")\r\n");
                    sb.append(Misc.bytes2Hex(d));
            }
            off += len;
            length -= len;
        }

        return sb;
    }

    public static boolean probe(byte[] d) {
        int l = 0;
        while (l + 2 < d.length) {
            int len = getLen(d, l);
            if (len <= 0)
                return false;
            l += len;
        }
        return l == d.length;
    }

    /**
     * Creates a new byte array containing an ASN.1 element.
     * 
     * @param s
     *            the content of the new ASN.1 element.
     * @param typ
     *            the kind of the new ASN.1 element.
     * @return a new allocated byte array containing an ASN.1 element.
     */
    public static byte[] makeASN1(String s, int typ) {
        final String cs = typ == UTF8String ? "utf-8" : typ == UTF16String ? "utf-16be" : "ISO-8859-1";
        try {
            return makeASN1(s.getBytes(cs), typ);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * creates a new byte array containing an ASN.1 element.
     * 
     * @param i
     *            the value of the new ASN.1 element
     * @param typ
     *            the kind of the new ASN.1 element
     * @return a new allocated byte array containing an ASN.1 element
     */
    public static byte[] makeASN1(int i, int typ) {
        byte[] b = encodeInteger(i);
        return makeASN1(b, typ);
    }

    private static byte[] encodeInteger(int i) {
        int j;
        if (i >= 0) {
            j = 1;
            for (int k = i >>> 7; k != 0; k >>>= 8) {
                ++j;
            }
        } else {
            j = 0;
            for (int k = ~i; k != 0; k >>>= 8) {
                ++j;
            }
        }
        if (j == 0)
            j = 1;

        byte b[] = new byte[j];
        for (int k = 0; k < j; ++k)
            b[k] = (byte) (i >>> (j - k - 1) * 8);
        return b;
    }

    private static byte[] encodeOIDInteger(int n) {
        int j = 0;
        for (int k = n; k != 0; k >>>= 7) {
            ++j;
        }
        if (j == 0)
            j = 1;

        byte b[] = new byte[j];
        for (int k = 0; k < j; ++k) {
            b[k] = (byte) ((0x7f & (n >>> (j - k - 1) * 7)) | (k + 1 < j ? 0x80 : 0));
        }
        return b;
    }

    /**
     * Creates a new byte array containing an ASN.1 element.
     * 
     * @param b
     *            the bytes of the new ASN.1 element.
     * @param typ
     *            the kind of the new ASN.1 element.
     * @return a new allocated byte array containing an ASN.1 element.
     */
    public static byte[] makeASN1(byte b[], int typ) {
        int i, n = 2;
        int len = b.length;
        if (typ == 3)
            ++len;
        if (len > 0x7f) {
            for (i = b.length; i > 0; i >>>= 8)
                ++n;
        }
        byte r[] = new byte[n + len];
        r[0] = (byte) typ;
        if (len > 0x7f) {
            r[1] = (byte) (0x80 + n - 2);
            i = n;
            while (i > 2) {
                r[--i] = (byte) len;
                len >>>= 8;
            }
        } else
            r[1] = (byte) len;
        if (typ == 3)
            r[n++] = 0;
        System.arraycopy(b, 0, r, n, b.length);
        return r;
    }

    /**
     * Add an ASN.1 element to a sequence or a set.
     * 
     * @param seqOrSet
     *            the bytes of the sequence or set.
     * @param dataToAdd
     *            the bytes which are added.
     * @return a new allocated byte array containing the merged ASN.1 element.
     */
    public static byte[] addTo(byte seqOrSet[], byte[] dataToAdd) {
        int off = 0;
        int typ = seqOrSet[off++];
        int len = seqOrSet[off++];
        if (len < 0) {
            int n = len & 0x7f;
            len = 0;
            while (n-- > 0) {
                len = (len << 8) | (seqOrSet[off++] & 0xff);
            }
        }
        byte r[] = new byte[len + dataToAdd.length];
        System.arraycopy(seqOrSet, off, r, 0, len);
        System.arraycopy(dataToAdd, 0, r, r.length - dataToAdd.length, dataToAdd.length);
        return makeASN1(r, typ);
    }

    /**
     * Get the total length of an object incl. header
     * 
     * @param b
     *            the bytes of the object.
     * @return the total length of the object incl. header
     */
    public static int getLen(byte b[]) {
        return getLen(b, 0);
    }

    /**
     * Get the total length of an object incl. header
     * 
     * @param b
     *            the bytes of the object.
     * @param off
     *            offset into the byte array
     * @return the total length of the object incl. header
     */
    public static int getLen(byte b[], int off) {
        int start = off;
        // int typ = b[off];
        ++off;
        if (off >= b.length)
            return -1;
        int len = b[off++];
        if (len == -128) {
            len = b.length - off;
        } else if (len < 0) {
            int n = len & 0x7f;
            len = 0;
            while (n-- > 0) {
                if (off >= b.length)
                    return -1;
                len = (len << 8) | (b[off++] & 0xff);
            }
        }
        return off + len - start;
    }

    /**
     * Return the length of the header. = 1 + count(length bytes)
     * 
     * @param b
     *            the bytes of the object.
     * @param off
     *            offset into the byte array
     * @return the length
     */
    public static int getHeaderLen(byte b[], int off) {
        ++off;
        if (off >= b.length)
            return -1;
        int len = b[off++];
        if (len < 0) {
            return (len & 0x7f) + 2;
        }
        return 2;
    }

    /**
     * Get the content of the current object.
     * 
     * @param b
     *            the bytes of the object.
     * @return a new byte array containing the content of the current object
     */
    public static byte[] getData(byte b[]) {
        return getData(b, 0);
    }

    /**
     * Get the content of the current object.
     * 
     * @param b
     *            the bytes of the object.
     * @param off
     *            - offset into the byte array
     * @return a new byte array containing the content of the current object
     */
    public static byte[] getData(byte b[], int off) {
        // int start = off;
        // int typ = b[off++];
        ++off;
        int len = b[off++];
        if (len == -128) {
            len = b.length - off;
        } else if (len < 0) {
            int n = len & 0x7f;
            len = 0;
            while (n-- > 0) {
                len = (len << 8) | (b[off++] & 0xff);
            }
        }
        if (len + off > b.length)
            len = b.length - off;
        byte r[] = new byte[len];
        System.arraycopy(b, off, r, 0, len);
        return r;
    }

    /**
     * Get a copy of the current object.
     * 
     * @param b
     *            the bytes of the object.
     * @return a new byte array containing the copy of the current object
     */
    public static byte[] copy(byte b[]) {
        return getData(b, 0);
    }

    /**
     * Get a copy of the current object.
     * 
     * @param b
     *            the bytes of the object.
     * @param off
     *            - offset into the byte array
     * @return a new byte array containing the copy of the current object
     */
    public static byte[] copy(byte b[], int off) {
        int start = off;
        // int typ = b[off++];
        ++off;
        int len = b[off++];
        if (len < 0) {
            int n = len & 0x7f;
            len = 0;
            while (n-- > 0) {
                len = (len << 8) | (b[off++] & 0xff);
            }
        }
        len += off - start;
        byte r[] = new byte[len];
        System.arraycopy(b, start, r, 0, len);
        return r;
    }

    /**
     * Get some ASN.1 data from a sequence. The path contains the types of the elements When 0x80 is or'd to the type,
     * it means, that the given element is entered some examples path = 0x10: the first sequence is searched and return
     * with header it IS the complete sequence path = 0x90: the first sequence is searched and its content is returned
     * it IS the complete content of the sequence WITHOUT header path = 0x10, 0x90, 0x84: 0x10, search first sequence,
     * do not enter 0x90, search next sequence, ENTER that sequence 0x84, search bit string, return its content!
     * 
     * @param b
     *            the sequence which is searched.
     * @param s
     *            the path to the searched element.
     * @param off
     *            an offset into b.
     * @return a new allocated byte array which represent the searched element, or null when not found.
     */
    // ===========================================================================
    static public final byte[] getSeq(byte b[], int s[], int off) {
        if (b == null)
            return null;
        if (s == null)
            return b;
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
     * Get an Integer from a sequence.
     * 
     * @param b
     *            the sequence which is searched.
     * @param s
     *            the path to the searched element.
     * @return the value or zero when not found.
     */
    static public final int getInt(byte b[], int s[]) {
        b = getSeq(b, s, 0);
        if (b == null)
            return 0;
        int z = 0;
        for (int i = 0; i < b.length; ++i) {
            z <<= 8;
            z |= 0xff & b[i];
        }
        return z;
    }

    /**
     * Get a Long from a sequence.
     * 
     * @param b
     *            the sequence which is searched.
     * @param s
     *            the path to the searched element.
     * @return the value or zero when not found.
     */
    // ===========================================================================
    static public final long getLong(byte b[], int s[]) {
        b = getSeq(b, s, 0);
        if (b == null)
            return 0;
        long z = 0;
        for (int i = 0; i < b.length; ++i) {
            z <<= 8;
            z |= 0xff & b[i];
        }
        return z;
    }

    /**
     * Search the sequence containing the searched element.
     * 
     * @param b
     *            a byte array containing the sequence
     * @param what
     *            the element which is searched
     * @return the offset into the byte array where the searched element was found, or -1.
     */
    public final static int searchSequence(byte b[], byte what[]) {
        int off[] = {0};
        return searchSequence(b, off, what, b.length);
    }

    /**
     * search the sequence containing the searched element.
     * 
     * @param b
     *            a byte array containing the sequence
     * @param off
     *            - an offset into the byte array
     * @param what
     *            the element which is searched
     * @return the offset into the byte array where the searched element was found, or -1.
     */
    public final static int searchSequence(byte b[], int off, byte what[]) {
        int of[] = {off};
        return searchSequence(b, of, what, b.length);
    }

    /**
     * search the sequence containing the searched element.
     * 
     * @param b
     *            - a byte array containing the sequence
     * @param off
     *            - an offest into the byte array
     * @param what
     *            - the element which is searched
     * @param end
     *            - end of range in b to use
     * @return the offset into the byte array where the searched element was found, or -1.
     */
    public final static int searchSequence(byte b[], int off[], byte what[], int end) {
        if (off[0] + what.length > b.length) {
            off[0] = end;
            return -1;
        }

        if (equals(b, off[0], what, 0, what.length))
            return 0;

        int start = off[0];
        int typ = b[off[0]++] & 0x1f;
        int len = b[off[0]++];
        if (len < 0) {
            int n = len & 0x7f;
            if (n + off[0] >= end) {
                off[0] = end;
                return -1;
            }
            len = 0;
            while (n-- > 0) {
                len = (len << 8) | (b[off[0]++] & 0xff);
            }
            if (len == 0)
                len = end - off[0];
            if (len < 0) {
                off[0] = end;
                return -1;
            }
        }

        switch (typ) {
            case 0x00: // dunno ???, but search it
            case OCTET_STRING:
            case SEQUENCE:
            case SET: {
                int z = off[0] + len;
                do {
                    int ret = searchSequence(b, off, what, z);
                    if (ret > 0)
                        return ret;
                    if (ret == 0)
                        return start;
                } while (off[0] < z);
                off[0] = z;
            }
                break;
            default:
                off[0] += len;
        }
        return -1;
    }

    /**
     * Inserts a NULL as first element of SEQUENCE or SET.
     * 
     * @param b
     *            the byte array containing a SEQUENCE or SET
     * @return the new encoded SEQUENCE or SET
     */
    static public byte[] nullEncode(byte b[]) {
        int off = 0;
        byte typ = b[off++];
        int len = b[off++];
        if (len < 0) {
            int n = len & 0x7f;
            len = 0;
            while (n-- > 0) {
                len = (len << 8) | (b[off++] & 0xff);
            }
        }

        byte r[] = new byte[len + 4];
        r[0] = typ;
        r[1] = (byte) 0x80;
        System.arraycopy(b, off, r, 2, len);
        r[len + 2] = 0;
        r[len + 3] = 0;
        return r;
    }

    /*
     * static { // a null element nul = makeASN1(nullBytes, 5); }
     */
    /**
     * compare 2 byte arrays.
     * 
     * @param a
     *            byte array a
     * @param ai
     *            offset into byte array a
     * @param b
     *            byte array b
     * @param bi
     *            offset into byte array b
     * @param len
     *            length to comapare
     * @return true if the compared bytes are equal, false either.
     */
    final static public boolean equals(byte a[], int ai, byte b[], int bi, int len) {
        for (int i = 0; i < len; ++i)
            if (a[i + ai] != b[i + bi])
                return false;
        return true;
    }

    /**
     * Convert the bytes of an OID into a printable String.
     * 
     * @param d
     *            data of an oid.
     * @return a printable String of the OID
     */
    public static String oid2String(byte[] d) {
        StringBuffer sb = new StringBuffer();
        int a = d[0] & 0xff;
        sb.append(a / 40).append(".").append(a % 40);
        long z = 0;
        for (int i = 1; i < d.length; ++i) {
            byte b = d[i];
            if (b < 0) {
                z = (z << 7) | (0x7f & b);
            } else {
                z = (z << 7) | b;
                sb.append(".").append(z);
                z = 0;
            }
        }

        return sb.toString();
    }

    /**
     * Convert an oid String into it's bytes. The oid must have at least 2 elements.
     * 
     * @param s
     *            the oid String
     * @return the oid bytes
     */
    public static byte[] string2Oid(String s) {
        byte b[] = new byte[s.length()];
        int len = 0;
        try {
            StringTokenizer st = new StringTokenizer(s, ".");
            if (!st.hasMoreElements())
                return null;

            int z = Integer.parseInt(st.nextToken()) * 40;
            if (!st.hasMoreElements())
                return null;
            z += Integer.parseInt(st.nextToken());
            b[0] = (byte) z;
            len = 1;
            for (; st.hasMoreElements();) {
                String n = st.nextToken();
                byte[] d = encodeOIDInteger(Integer.parseInt(n));
                System.arraycopy(d, 0, b, len, d.length);
                len += d.length;
            }
        } catch (Exception ex) {
            return null;
        }
        return Arrays.copyOf(b, len);
    }

    public static void main(String args[]) {
        byte[] b = string2Oid("1.2.840.113549.1.9.21");
        String s = oid2String(b);
        System.out.println(s);
    }
}

/*
 * $Log: Asn1.java,v $
 * Revision 1.13  2012/12/19 12:24:31  bebbo
 * @B changed default encoding in makeASN1(String, int) to ISO-8851-1
 *
 * Revision 1.12  2012/11/11 17:42:15  bebbo
 * @N added UTF16String
 * @B fixed string2Oid()
 * @R changed dump to omit ascii output for bitstrings
 *
 * Revision 1.11  2012/11/10 09:35:18  bebbo
 * @C typo
 *
 * Revision 1.10  2012/08/19 15:24:50  bebbo
 * @B fixed string2Oid - wrong calculation of the first byte
 *
 * Revision 1.9  2012/08/11 19:57:07  bebbo
 * @I working stage
 *
 * Revision 1.8  2011/05/20 09:02:38  bebbo
 * @N ASN1 dumps also text for OCTETs
 *
 * Revision 1.7  2010/12/17 23:25:06  bebbo
 * /FIXED: ssl config now supports multiple certificates
 * Revision 1.6 2010/12/17 17:37:31 bebbo
 * 
 * @N Asn1 is now usable as an object / iterator Revision 1.5 2007/04/01 15:51:52 bebbo
 * 
 * @I removed unused stuff
 * 
 * Revision 1.4 2003/10/01 12:25:21 bebbo
 * 
 * @C enhanced comments
 * 
 * Revision 1.3 2002/11/06 09:46:12 bebbo
 * 
 * @I cleanup for imports
 * 
 * Revision 1.2 2001/03/09 19:49:37 bebbo
 * 
 * @C (c) fixed
 * 
 * Revision 1.1 2000/09/25 12:20:58 bebbo
 * 
 * @N repackaged
 */