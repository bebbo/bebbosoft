/*
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Pkcs1.java,v $
 * $Revision: 1.5 $
 * $Date: 2012/08/11 19:57:04 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyrights (c) by Stefan Bebbo Franke 1999-2001
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 */

package de.bb.security;

import java.math.BigInteger;

/**
 * This class contains functions from Pkcs 1
 */
public class Pkcs1 {
    final static byte digestAlgorithmMd5[] = Asn1.string2Oid("1.2.840.113549.2.5");
    final static byte digestAlgorithmSHA[] = Asn1.string2Oid("1.2.840.113549.2.26");
    final static byte digestAlgorithmSHA256[] = Asn1.string2Oid("2.16.840.1.101.3.4.2.1");
    final static byte digestAlgorithmSHA384[] = Asn1.string2Oid("2.16.840.1.101.3.4.2.2");
    final static byte digestAlgorithmSHA512[] = Asn1.string2Oid("2.16.840.1.101.3.4.2.3");

    final static byte newSeq[] = {(byte) 0x30, (byte) 0x80};

    /**
     * Create a new sequence containing a RSA key pair
     * 
     * @param p
     *            first prime number.
     * @param q
     *            second prime number.
     * @return a new allocated ASN.1 sequence containing the key pair.
     * @see PKCS-1
     */
    public static byte[] createKeyPair(BigInteger p, BigInteger q) throws Exception {
        try {
            // calculate all other values
            BigInteger e = new BigInteger("65537");
            BigInteger one = new BigInteger("1");
            BigInteger p1 = p.subtract(one);
            BigInteger q1 = q.subtract(one);
            BigInteger n = p1.multiply(q1);
            BigInteger d = e.modInverse(n);
            n = p.multiply(q);
            BigInteger dp1 = d.mod(p1);
            BigInteger dq1 = d.mod(q1);
            BigInteger qp = q.modInverse(p);

            byte seq[] = newSeq;
            seq = Asn1.addTo(seq, Asn1.makeASN1(0, 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(n.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(e.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(d.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(p.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(q.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(dp1.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(dq1.toByteArray(), 2));
            seq = Asn1.addTo(seq, Asn1.makeASN1(qp.toByteArray(), 2));
            return seq;
        } catch (Exception e) {
            throw new Exception(e.toString());
        }
    }

    private static int pan[] = {0x90, 2, 0x82};
    private static int pae[] = {0x90, 2, 2, 0x82};
    private static int pad[] = {0x90, 2, 2, 2, 0x82};
    private final static int PAP[] = {0x90, 2, 2, 2, 2, 0x82};
    private final static int PAQ[] = {0x90, 2, 2, 2, 2, 2, 0x82};
    private final static int padp1[] = {0x90, 2, 2, 2, 2, 2, 2, 0x82};
    private final static int padq1[] = {0x90, 2, 2, 2, 2, 2, 2, 2, 0x82};
    private final static int paiqmp[] = {0x90, 2, 2, 2, 2, 2, 2, 2, 2, 0x82};

    /**
     * return the public modulo or null
     * 
     * @param key
     *            - the key information
     * @return the public modulo or null on error
     */
    public static byte[] getN(byte key[]) {
        return Asn1.getSeq(key, pan, 0);
    }

    /**
     * return the public exponent
     * 
     * @param key
     *            - the key information
     * @return the public exponent or null on error
     */
    public static byte[] getE(byte key[]) {
        return Asn1.getSeq(key, pae, 0);
    }

    /**
     * return the private exponent
     * 
     * @param key
     *            - the key information
     * @return the private exponent or null on error
     */
    public static byte[] getD(byte key[]) {
        return Asn1.getSeq(key, pad, 0);
    }

    /**
     * Return the full private key set.
     * 
     * @param b
     *            a byte array with the key
     * @return a byte array arrays with the private key data [n, e, d, p, q, dp1, dq1, iqmp]
     */
    public static byte[][] getPrivateKey(byte b[]) {
        byte[][] pkData = new byte[8][];
        pkData[0] = Asn1.getSeq(b, pan, 0);
        pkData[1] = Asn1.getSeq(b, pae, 0);
        pkData[2] = Asn1.getSeq(b, pad, 0);
        pkData[3] = Asn1.getSeq(b, PAP, 0);
        pkData[4] = Asn1.getSeq(b, PAQ, 0);
        pkData[5] = Asn1.getSeq(b, padp1, 0);
        pkData[6] = Asn1.getSeq(b, padq1, 0);
        pkData[7] = Asn1.getSeq(b, paiqmp, 0);
        return pkData;
    }
}

/*
 * $Log: Pkcs1.java,v $
 * Revision 1.5  2012/08/11 19:57:04  bebbo
 * @I working stage
 *
 * Revision 1.4  2008/03/13 20:48:39  bebbo
 * @R added support to store a private key
 *
 * Revision 1.3  2002/11/06 09:46:12  bebbo
 * @I cleanup for imports
 *
 * Revision 1.2  2001/03/29 18:25:04  bebbo
 * @C comments
 *
 * Revision 1.1  2001/03/05 17:47:44  bebbo
 * @N new or changed comment
 *
 * Revision 1.2  2000/07/07 10:14:52  bebbo
 * @N new functions to get exponents / modulos
 *
 * Revision 1.1  2000/06/18 17:05:34  bebbo
 * @R splitted into separate files from Pkcs
 *
 * Revision 1.8  2000/06/18 16:20:49  bebbo
 * @N added email id, added email to CertificateInfo
 *
 * Revision 1.7  2000/06/16 19:00:51  bebbo
 * @N added modified pbkdf1a (key material generator)
 *
 * Revision 1.6  2000/05/22 09:30:09  hagen
 * removed cvs conflicts
 * static initializer fx replaced by direct assignment (doesnt compile under JDK 1.2 otherwise)
 *
 * Revision 1.5  2000/05/20 19:47:38  bebbo
 * @R Asn1.getSeq changed. All related changes are done. Read docu!
 *
 * Revision 1.4  2000/02/09 10:37:02  bebbo
 * @R doRSA is now here (was in Ssl3)
 *
 * Revision 1.3  2000/02/02 17:41:39  bebbo
 * @N added getOrganizationName to retrieve the Name from Pkcs-info block
 *
 * Revision 1.2  2000/02/02 13:53:16  bebbo
 * @B fixed TAB handling in INI files
 * @B Certificate request now conforms to PKCS-10
 *
 * Revision 1.1  1999/11/03 11:43:59  bebbo
 * @N added PKCS-5 encrypt and decrypt functions

 * @B fixed PKCS key streaming
 *
 */