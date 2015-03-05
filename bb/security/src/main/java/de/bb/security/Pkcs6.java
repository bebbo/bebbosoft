/*
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Pkcs6.java,v $
 * $Revision: 1.18 $
 * $Date: 2014/10/04 19:13:10 $
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import de.bb.util.Misc;

/**
 * This class contains functions from Pkcs 6
 */
public class Pkcs6 {
    // ===========================================================================
    // some oid's
    // ===========================================================================
    final public static byte rsaEncryption[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x01 };
    final static byte md2withRSAEncryption[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x02 };
    final static byte md5withRSAEncryption[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x04 };
    final static byte sha1withRSAEncryption[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x05 };
    final static byte sha256WithRSAEncryption[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
        (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 11 };

    final static byte id_at_aliasedEntryName[] = { 2 * 40 + 5, 4, 1 };
    final static byte id_at_knowldgeinformation[] = { 2 * 40 + 5, 4, 2 };
    final static byte id_at_commonName[] = { 2 * 40 + 5, 4, 3 };
    final static byte id_at_surname[] = { 2 * 40 + 5, 4, 4 };
    final static byte id_at_serialNumber[] = { 2 * 40 + 5, 4, 5 };
    final static byte id_at_countryName[] = { 2 * 40 + 5, 4, 6 };
    final static byte id_at_localityName[] = { 2 * 40 + 5, 4, 7 };
    final static byte id_at_stateOrProvinceName[] = { 2 * 40 + 5, 4, 8 };
    final static byte id_at_streetAddress[] = { 2 * 40 + 5, 4, 9 };
    final static byte id_at_organizationName[] = { 2 * 40 + 5, 4, 10 };
    final static byte id_at_organizationalUnitName[] = { 2 * 40 + 5, 4, 11 };
    final static byte id_at_email[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D,
            (byte) 0x01, (byte) 0x09, 1 };

    final static byte newSeq[] = { (byte) 0x30, (byte) 0x80 };
    final static byte newSet[] = { (byte) 0x31, (byte) 0x80 };
    final static byte nullBytes[] = {};
    final static byte nul[] = Asn1.makeASN1(nullBytes, 5);
    

    public static final byte[] objectAlgorithmSHA = Asn1.string2Oid("1.3.14.3.2.26");
    public static final byte[] objectAlgorithmSHA256 = Asn1.string2Oid("2.16.840.1.101.3.4.2.1");
    public static final byte[] objectAlgorithmSHA384 = Asn1.string2Oid("2.16.840.1.101.3.4.2.2");
    public static final byte[] objectAlgorithmSHA512 = Asn1.string2Oid("2.16.840.1.101.3.4.2.3");

    static {
        // a null element
        // nul = Asn1.makeASN1(nullBytes, 5);
    }

    /**
     * Create a certifcate without siganture. You have to sign this later by calling sign().
     * 
     * @param issuer
     *            The issuer of the certificate.
     * @param date
     *            An time intervall when the certificate is valid.
     * @param owner
     *            The owner ot the certificate.
     * @param pubN
     *            owners modulo.
     * @param pubE
     *            owners public exponent.
     * @return A new allocated byte array containing the certificate body.
     * @see PKCS-6
     */
    public static byte[] createCertificate(byte issuer[], byte date[], byte owner[], byte pubN[], byte pubE[]) {
        // Kennung f???r RSA_with_SHA1
        byte t[] = Asn1.addTo(newSeq, Asn1.makeASN1(sha256WithRSAEncryption, 6)); // OID
        byte mdx[] = Asn1.addTo(t, nul); // null

        // macht daraus ein Zertifikat "Version 3"
        byte ver[] = Asn1.addTo(newSeq, Asn1.makeASN1(2, 2));
        ver[0] = (byte) 0xa0;

        // Zufallszahl
        t = new byte[16];
        Random sr = SecureRandom.getInstance();
        sr.nextBytes(t);
        byte nr[] = Asn1.makeASN1(t, 2);

        // public Key codieren
        t = Asn1.addTo(newSeq, Asn1.makeASN1(pubN, 2));
        t = Asn1.addTo(t, Asn1.makeASN1(pubE, 2));
        byte pub[] = Asn1.makeASN1(t, 3);
        // Kennung f??r RSA
        t = Asn1.addTo(newSeq, Asn1.makeASN1(rsaEncryption, 6)); // OID
        t = Asn1.addTo(t, nul);
        t = Asn1.addTo(newSeq, t);
        pub = Asn1.addTo(t, pub);

        // alles zusammen setzen
        t = newSeq;
        t = Asn1.addTo(t, ver);
        t = Asn1.addTo(t, nr);
        t = Asn1.addTo(t, mdx);
        t = Asn1.addTo(t, issuer);
        t = Asn1.addTo(t, date);
        t = Asn1.addTo(t, owner);
        t = Asn1.addTo(t, pub);

        return t;
    }

    /**
     * Create a certificate request without signature. You have to sign this later by calling sign().
     * 
     * @param owner
     *            The owner ot the certificate.
     * @param pubN
     *            owners modulo.
     * @param pubE
     *            owners public exponent.
     * @return A new allocated byte array containing the certificate body.
     * @see PKCS-10
     * 
     *      CertificationRequestInfo ::= SEQUENCE { version Version, subject Name, subjectPublicKeyInfo
     *      SubjectPublicKeyInfo, attributes [0] IMPLICIT Attributes }
     */
    public static byte[] createCertificateRequest(byte owner[], byte pubN[], byte pubE[], byte privF[]) {
        byte ver[] = Asn1.makeASN1(0, Asn1.INTEGER);

        // public Key codieren
        byte t[];
        t = Asn1.addTo(newSeq, Asn1.makeASN1(pubN, 2));
        t = Asn1.addTo(t, Asn1.makeASN1(pubE, 2));
        byte pub[] = Asn1.makeASN1(t, 3);

        //        byte sig[] = Pkcs6.sign(pub, pubN, privF);

        // Kennung f??r RSA
        t = Asn1.addTo(newSeq, Asn1.makeASN1(rsaEncryption, 6));
        t = Asn1.addTo(t, nul);
        t = Asn1.addTo(newSeq, t);
        pub = Asn1.addTo(t, pub);
        //        pub = Asn1.addTo(t, sig);

        // alles zusammen setzen
        t = newSeq;
        t = Asn1.addTo(t, ver);
        t = Asn1.addTo(t, owner);
        t = Asn1.addTo(t, pub);

        byte a0[] = { (byte) 160, 0 };
        t = Asn1.addTo(t, a0);

        t = Pkcs6.sign(t, pubN, privF);

        return t;
    }

    /**
     * Sign a given sequence with a RSA private key. This is e.g. used for signing certificates or certificate requests.
     * An enhanced version which uses the Chinese remainder theorem is planned <g>.
     * 
     * @param sign
     *            The data which is signed.
     * @param privN
     *            The public modulo.
     * @param privF
     *            The private exponent.
     * @see PKCS-6
     */
    public static byte[] sign(final byte sign[], final byte privN[], final byte privF[]) {
        byte[] t = prepareSignedContent(sign, "SHA256", privN.length - (privN[0] == 0 ? 1 : 0));

        // rsa ausrechnen
        final byte[] signedData = doRSA(t, privN, privF);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(sha256WithRSAEncryption, 6)); // OID
        byte[] mdx = Asn1.addTo(t, nul); // null

        t = Asn1.addTo(newSeq, sign);
        t = Asn1.addTo(t, mdx);
        t = Asn1.addTo(t, Asn1.makeASN1(signedData, 3));
        return t;
    }

    public static byte[] prepareSignedContent(final byte sign[], final String hashName, final int keyLength) {
        MessageDigest md;
        byte[] hashOid;
        if ("SHA256".equals(hashName)) {
            md = new SHA256();
            hashOid = objectAlgorithmSHA256;
        } else {
            md = new SHA();
            hashOid = objectAlgorithmSHA;
        }
        
        byte t[] = Asn1.addTo(newSeq, Asn1.makeASN1(hashOid, 6)); // OID
        byte mdx[] = Asn1.addTo(t, nul); // null

        // hash berechnen
        final byte[] signature = md.digest(sign);
        // daten f???r RSA padden
        t = Asn1.addTo(newSeq, mdx);
        final byte signedContent[] = Asn1.addTo(t, Asn1.makeASN1(signature, 4));

        return padSignedContent(signedContent, keyLength);
    }
    
    public static byte[] padSignedContent(byte[] signedContent, int keyLength) {
        byte[] t = new byte[keyLength];
        t[0] = 0;
        t[1] = 1;

        for (int i = 2; i < t.length - signedContent.length; ++i)
            t[i] = (byte) 0xff;
        t[t.length - signedContent.length - 1] = 0;
        System.arraycopy(signedContent, 0, t, t.length - signedContent.length, signedContent.length);
        return t;
    }

    /**
     * Create a sequence containing a time interval.
     * 
     * @param from
     *            Begin of the tim interval.
     * @param to
     *            End of the time interval.
     * @return A new allocated ASN.1 sequence containing the interval.
     */
    public static byte[] createDate(byte from[], byte to[]) {
        byte b[] = Asn1.addTo(newSeq, Asn1.makeASN1(from, 23));
        return Asn1.addTo(b, Asn1.makeASN1(to, 23));
    }

    /**
     * Create a owner / issuer info sequence from a file.
     * 
     * @param fn
     *            The file name from which the input data is read.
     * @return a New allocated ASN.1 sequence contatining the info.
     * @see PKCS-6 and PKCS-10
     */
    public static byte[] makeInfo(String fn) throws IOException {
        FileInputStream fis = new FileInputStream(fn);
        try {
            byte b[] = new byte[fis.available()];
            fis.read(b);
            byte seq[] = newSeq;
            for (int i = 0; i < b.length;) {
                // Leerstellen am Anfang ???berspringen
                while (i < b.length && b[i] <= 32)
                    ++i;
                if (i == b.length)
                    break;

                // Rest der Zeile suchen
                int start = i;
                while (i < b.length && b[i] != 0xd && b[i] != 0xa)
                    ++i;

                int stop = i, j;
                // Zuweisung suchen
                for (j = start; j < stop; ++j)
                    if (b[j] == '=')
                        break;

                // keine Zuweisung ? weiter
                if (j == stop)
                    continue;
                int k = j + 1;
                // whitespaces rechts abschneiden
                while (j > start && b[j - 1] <= 32)
                    --j;
                String key = new String(b, start, j - start);
                while (k < stop && b[k] <= 32)
                    ++k;
                while (k < stop && b[stop - 1] <= 32)
                    --stop;
                String value = new String(b, k, stop - k);

                // System.out.println("<" + key + "> = '" + value + "'");

                int type = Asn1.PrintableString;
                byte oid[] = null;
                if (key.equalsIgnoreCase("commonName")) {
                    oid = id_at_commonName;
                    type = Asn1.T61String;
                } else if (key.equalsIgnoreCase("surName"))
                    oid = id_at_surname;
                else if (key.equalsIgnoreCase("countryName"))
                    oid = id_at_countryName;
                else if (key.equalsIgnoreCase("localityName"))
                    oid = id_at_localityName;
                else if (key.equalsIgnoreCase("stateOrProvinceName"))
                    oid = id_at_stateOrProvinceName;
                else if (key.equalsIgnoreCase("streetAddress"))
                    oid = id_at_streetAddress;
                else if (key.equalsIgnoreCase("organizationName"))
                    oid = id_at_organizationName;
                else if (key.equalsIgnoreCase("organizationalUnitName"))
                    oid = id_at_organizationalUnitName;
                else if (key.equalsIgnoreCase("email")) {
                    oid = id_at_email;
                    type = Asn1.IA5String;
                }

                if (oid != null) {
                    byte t[] = Asn1.addTo(newSeq, Asn1.makeASN1(oid, 6));
                    t = Asn1.addTo(t, Asn1.makeASN1(value, type));
                    t = Asn1.addTo(newSet, t);
                    seq = Asn1.addTo(seq, t);
                }
            }
            return seq;
        } finally {
            fis.close();
        }
    }

    /**
     * Create a owner / issuer info sequence from givenparameters.
     * 
     * @param fn
     *            The file name from which the input data is read.
     * @return A new allocated ASN.1 sequence contatining the info.
     * @see PKCS-6 and PKCS-10
     */
    public static byte[] makeInfo(String name, String orgName, String url, String country, String state, String location) {
        byte s[], set[], t[];
        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_organizationName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(name, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(newSeq, set);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_organizationalUnitName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(orgName, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(s, set);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_commonName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(url, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(s, set);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_countryName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(country, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(s, set);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_stateOrProvinceName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(state, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(s, set);

        t = Asn1.addTo(newSeq, Asn1.makeASN1(id_at_localityName, 6));
        t = Asn1.addTo(t, Asn1.makeASN1(location, 19));
        set = Asn1.addTo(newSet, t);
        s = Asn1.addTo(s, set);

        return s;
    }

    private final static int certPKpath[] = { 0x90, 0x90, 0x10, 0x10, 0x10, 0x10, 0x90, 0x83 };

    /**
     * Get the public modulo from a X.509 certificate
     * 
     * @param cert
     *            - a X.509 certificate
     * @return the modulo, or null on error
     */
    public static byte[] getX509Modulo(byte cert[]) {
        // get the public modulo from certificate
        byte b[] = Asn1.getSeq(cert, certPKpath, 0);
        // dump(b);
        int p1[] = { 0x90, 0x82 };
        return Asn1.getSeq(b, p1, b[0] == 0 ? 1 : 0);
    }

    /**
     * Get the public exponent from a X.509 certificate
     * 
     * @param cert
     *            - a X.509 certificate
     * @return the exponent, or null on error
     */
    public static byte[] getX509Exponent(byte cert[]) {
        // get the public exponent from certificate
        byte b[] = Asn1.getSeq(cert, certPKpath, 0);

        int p2[] = { 0x90, 0x02, 0x82 };
        return Asn1.getSeq(b, p2, b[0] == 0 ? 1 : 0);
    }

    private static int certSigPath[] = { 0x90, 0x10, 0x10, 0x83 };
    private static int certContent[] = { 0x90, 0x10 };
    private static int signatureHash[] = { 0x90, 0x84 };

    public static byte[] getCertificateSignature(byte[] cert) {
        byte signedData[] = Asn1.getSeq(cert, certSigPath, 0);
        byte[] n = getX509Modulo(cert);
        byte[] e = getX509Exponent(cert);

        if (signedData == null || n == null || e == null)
            return null;

        byte[] u = doRSA(signedData, n, e);

        byte[] sig = decodeRSA(u);
        return sig;
    }

    public static byte[] decodeRSA(byte[] u) {
        int i = 0;
        while (i < u.length && u[i] == 0) {
            ++i;
        }
        if (i == u.length || u[i] != 1)
            return null;
        ++i;
        while (i < u.length && u[i] == -1) {
            ++i;
        }
        if (i == u.length || u[i] != 0)
            return null;
        ++i;
        return Arrays.copyOfRange(u, i, u.length);
    }

    /**
     * Returns the signature if the chain was validated successfully.
     * @param certs a vector of certificates
     * @return the signature if the chain was validated successfully, null otherwise.
     */
    public static byte[] getCertificateSignature(Vector<byte[]> certs) {
        if (certs.isEmpty())
            return null;

        byte[] cert = certs.get(certs.size() - 1);
        byte[] n = getX509Modulo(cert);
        byte[] e = getX509Exponent(cert);

        byte[] u = null;
        for (int i = certs.size() - 1; i >= 0; --i) {
            cert = certs.get(i);
            byte signature[] = Asn1.getSeq(cert, certSigPath, 0);
            if (signature == null || n == null || e == null)
                return null;

            u = doRSA(signature, n, e);
            u = decodeRSA(u);
            if (u == null)
                return null;

            u = Asn1.getSeq(u, signatureHash, 0);
            if (u == null)
                return null;

            byte signedData[] = Asn1.getSeq(cert, certContent, 0);
            if (signedData == null)
                return null;

            byte calcedHash[] = new SHA().digest(signedData);

            if (!Misc.equals(u, calcedHash))
                return null;

            n = getX509Modulo(cert);
            e = getX509Exponent(cert);
        }

        return u;
    }

    private static int reqPKpath[] = { 0x90, 0x90, 0x10, 0x90, 0x83 };

    /**
     * Get the public modulo from a X.509 certificate request
     * 
     * @param cert
     *            - a X.509 certificate request
     * @return the modulo, or null on error
     */
    public static byte[] getReqModulo(byte req[]) {
        // get the public modulo from certificate
        byte b[] = Asn1.getSeq(req, reqPKpath, 0);
        // dump(b);
        int p1[] = { 0x90, 0x82 };
        return Asn1.getSeq(b, p1, b[0] == 0 ? 1 : 0);
    }

    /**
     * Get the public exponent from a X.509 certificate request
     * 
     * @param cert
     *            - a X.509 certificate request
     * @return the modulo, or null on error
     */
    public static byte[] getReqExponent(byte req[]) {
        // get the public modulo from certificate
        byte b[] = Asn1.getSeq(req, reqPKpath, 0);
        // dump(b);
        int p2[] = { 0x90, 0x02, 0x82 };
        return Asn1.getSeq(b, p2, b[0] == 0 ? 1 : 0);
    }

    private static int ISSUER_PATH[] = { 0x90, 0x90, 0x10, 0x10 };

    /**
     * Get the bytes containing the issuer of a certificate.
     * 
     * @param cert
     *            the certificate data
     * @return the issuer data.
     */
    public static byte[] getCertificateIssuer(byte cert[]) {
        byte b[] = Asn1.getSeq(cert, ISSUER_PATH, 0);
        return b;
    }

    private static int OWNER_PATH[] = { 0x90, 0x90, 0x10, 0x10, 0x10, 0x10 };

    /**
     * Get the bytes containing the owner of a certificate.
     * 
     * @param cert
     *            the certificate data
     * @return the owner data.
     */
    public static byte[] getCertificateOwner(byte cert[]) {
        byte b[] = Asn1.getSeq(cert, OWNER_PATH, 0);
        return b;
    }

    /**
     * Search owners name in an owner / issuer info sequence
     * 
     * @param owner
     *            the input data, which is searched
     * @return the name or null
     */
    static public String searchOwner(byte owner[]) {
        int offset = Asn1.searchSequence(owner, Asn1.makeASN1(id_at_organizationName, 6));
        if (offset < 0)
            return null;
        int path[] = { 0x90, 0x93 };
        byte txt[] = Asn1.getSeq(owner, path, offset);
        return new String(txt);
    }

    /**
     * perform an RSA encryption.
     * 
     * @param z
     *            the data to encrypt/decrypt
     * @param n
     *            the modulo
     * @param e
     *            the exponent
     * @return a new allocated byte array with the result of (z**e)mod n
     */
    public static byte[] doRSA(byte z[], byte n[], byte e[]) {
//        BigInteger bn = new BigInteger(1, n);
//        BigInteger bf = new BigInteger(1, e);
//        BigInteger zz = new BigInteger(1, z);
//        zz = zz.modPow(bf, bn);
        
        int mlen = (n.length >> 2) + 1;
        int[] iz = FastMath32.byte2Int(z, mlen);
        int[] in = FastMath32.byte2Int(n, mlen);
        
        int[] ir = FastMath32.oddModPow(iz, e, in);
        
        int nlen = n.length;
        if (n[0] == 0)
            --nlen;
        byte[] b = new byte[nlen];
        FastMath32.int2Byte(ir, b);
        return b;
    }

    /**
     * Perform a RSA private key encryption.
     * 
     * @param z
     *            the data to en/decrypt
     * @param pkData
     *            the private key data (n, e, d, p, q, dp1, dq1, iqmp)
     * @return a new allocated byte array containing the en/decrypted data
     */
    public static byte[] doRSA(byte[] z, byte[][] pkData) {
        if (z.length == 0)
            return z;
        /*
         * / BigInteger bz = new BigInteger(1, z);
         * 
         * BigInteger p = new BigInteger(1, pkData[3]); BigInteger q = new BigInteger(1, pkData[4]); BigInteger dp1 =
         * new BigInteger(1, pkData[5]); BigInteger dq1 = new BigInteger(1, pkData[6]); BigInteger iqmp = new
         * BigInteger(1, pkData[7]);
         * 
         * BigInteger cDp = bz.modPow(dp1, p); BigInteger cDq = bz.modPow(dq1, q); if (cDp.compareTo(cDq) < 0) cDp =
         * cDp.add(p); BigInteger u = ((cDp.subtract(cDq)).multiply(iqmp)).remainder(p); BigInteger r =
         * cDq.add(u.multiply(q));
         * 
         * byte []b = r.toByteArray(); /
         */
        // long start = System.currentTimeMillis();
        int mlen = pkData[3].length;
        if (mlen < pkData[4].length)
            mlen = pkData[4].length;
        mlen = (mlen >> 2) + 1;
        int[] ip = FastMath32.byte2Int(pkData[3], mlen);
        int[] iq = FastMath32.byte2Int(pkData[4], mlen);
        int[] iiqmp = FastMath32.byte2Int(pkData[7], mlen);
        int maxLen = mlen + mlen + 1;
        int[] iz = FastMath32.byte2Int(z, mlen);
        int[] icDp = FastMath32.oddModPow(iz, pkData[5], ip);
        int[] icDq = FastMath32.oddModPow(iz, pkData[6], iq);
        int[] it = new int[maxLen];
        if (FastMath32.sub(it, icDp, icDq, mlen)) {
            FastMath32.add(it, it, mlen, ip, mlen);
        }
        int[] iu = new int[maxLen + 1];
        FastMath32.mul(iu, it, iiqmp, mlen);
        FastMath32.mod(iu, ip, it, iu, maxLen, mlen);
        FastMath32.mul(it, iu, iq, mlen);
        FastMath32.add(it, it, maxLen, icDq, mlen);

        byte b2[] = new byte[z.length];
        FastMath32.int2Byte(it, b2);

        // System.out.println("RSA took: " + (System.currentTimeMillis() - start) + "ms");
        return b2;

        /*
         * / sub(t, cDp, cDq, p); // on underflow add p mul(u, t, iqmp); mod(u, p); mul(t, u, q); add(r, t, cDq); /
         * 
         * BigInteger f = new BigInteger(1, pkData[2]); BigInteger n = new BigInteger(1, pkData[0]); long now =
         * System.currentTimeMillis(); bz.modPow(f, n); System.out.println((System.currentTimeMillis() - now)); byte
         * fb[] = FastMath32.oddModPow(z, pkData[2], pkData[0]);
         * 
         * if (new BigInteger(1, b).compareTo(new BigInteger(1, fb)) != 0) { Misc.dump("Z", System.out, z);
         * Misc.dump("N", System.out, pkData[0]); Misc.dump("F", System.out, pkData[2]); Misc.dump("SOLL", System.out,
         * b); Misc.dump("IST", System.out, fb);
         * 
         * IntImpl zi = new IntImpl(z); IntImpl ni = new IntImpl(pkData[0]); IntImpl fi = new IntImpl(pkData[2]);
         * IntImpl res = zi.modPow(fi, ni); System.out.println(res.toHexString()); }
         * 
         * byte t[] = new byte[z.length]; if (b.length > t.length) System.arraycopy(b, b.length-t.length, t, 0,
         * t.length); else System.arraycopy(b, 0, t, t.length-b.length, b.length);
         */

    }

}

/*
 * $Log: Pkcs6.java,v $
 * Revision 1.18  2014/10/04 19:13:10  bebbo
 * @C added comments
 *
 * Revision 1.17  2014/09/18 13:49:19  bebbo
 * @N added support for TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 and TLS_DHE_RSA_WITH_AES_256_CBC_SHA
 *
 * Revision 1.16  2014/04/13 20:18:38  bebbo
 * @B fixed a missing close() call
 *
 * Revision 1.15  2013/11/23 10:43:08  bebbo
 * @N added public static byte[] decodeRSA(byte[])
 * @N added public static byte[] getCertificateSignature(Vector<byte[]>)
 *
 * Revision 1.14  2012/12/19 12:25:41  bebbo
 * @B fixed certificate request structure
 * @R changed encoding type for commonName and email
 *
 * Revision 1.13  2012/11/12 20:47:30  bebbo
 * @N new method to read and decrypt the certificate signature
 *
 * Revision 1.12  2012/08/19 15:25:48  bebbo
 * @R certificates are now signed using objectAlgorithmSHA
 *
 * Revision 1.11  2012/08/11 19:57:01  bebbo
 * @I working stage
 *
 * Revision 1.10  2010/12/17 23:25:05  bebbo
 * /FIXED: ssl config now supports multiple certificates
 * Revision 1.9 2010/12/17 17:41:27 bebbo
 * 
 * @R switched to use SHA instead of MD5 to sign a certificate Revision 1.8 2010/12/17 17:40:28 bebbo
 * 
 * @R switched to use SHA instead of MD5 to sign a certificate Revision 1.7 2010/12/17 17:36:50 bebbo
 * 
 * @D removed some debug output Revision 1.6 2009/02/05 20:06:28 bebbo
 * 
 * @N added support for TLS 1.0
 * 
 * Revision 1.5 2008/03/13 20:49:14 bebbo
 * 
 * @I using FastMath32 instead of BigInteger
 * 
 * Revision 1.4 2007/04/21 19:11:35 bebbo
 * 
 * @N added new method for private key RSA operation
 * 
 * Revision 1.3 2002/11/06 09:46:12 bebbo
 * 
 * @I cleanup for imports
 * 
 * Revision 1.2 2001/03/29 18:25:05 bebbo
 * 
 * @C comments
 * 
 * Revision 1.1 2001/03/05 17:47:44 bebbo
 * 
 * @N new or changed comment
 * 
 * Revision 1.7 2000/07/19 15:16:14 bebbo
 * 
 * @D removed dump
 * 
 * Revision 1.6 2000/07/18 18:08:35 bebbo
 * 
 * @B fixed path to public key
 * 
 * Revision 1.5 2000/07/18 11:10:58 bebbo
 * 
 * @B fixed certificate request structure
 * 
 * Revision 1.4 2000/07/07 10:14:52 bebbo
 * 
 * @N new functions to get exponents / modulos
 * 
 * Revision 1.3 2000/06/20 16:28:37 bebbo
 * 
 * @N added getX509Exponent and getX509Modulo functions
 * 
 * Revision 1.2 2000/06/19 11:16:13 bebbo
 * 
 * @R rsaEncryption is public now
 * 
 * Revision 1.1 2000/06/18 17:05:34 bebbo
 * 
 * @R splitted into separate files from Pkcs
 * 
 * Revision 1.8 2000/06/18 16:20:49 bebbo
 * 
 * @N added email id, added email to CertificateInfo
 * 
 * Revision 1.7 2000/06/16 19:00:51 bebbo
 * 
 * @N added modified pbkdf1a (key material generator)
 * 
 * Revision 1.6 2000/05/22 09:30:09 hagen removed cvs conflicts static initializer fx replaced by direct assignment
 * (doesnt compile under JDK 1.2 otherwise)
 * 
 * Revision 1.5 2000/05/20 19:47:38 bebbo
 * 
 * @R Asn1.getSeq changed. All related changes are done. Read docu!
 * 
 * Revision 1.4 2000/02/09 10:37:02 bebbo
 * 
 * @R doRSA is now here (was in Ssl3)
 * 
 * Revision 1.3 2000/02/02 17:41:39 bebbo
 * 
 * @N added getOrganizationName to retrieve the Name from Pkcs-info block
 * 
 * Revision 1.2 2000/02/02 13:53:16 bebbo
 * 
 * @B fixed TAB handling in INI files
 * 
 * @B Certificate request now conforms to PKCS-10
 * 
 * Revision 1.1 1999/11/03 11:43:59 bebbo
 * 
 * @N added PKCS-5 encrypt and decrypt functions
 * 
 * @B fixed PKCS key streaming
 */