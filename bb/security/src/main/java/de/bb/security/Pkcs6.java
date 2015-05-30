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
import java.math.BigInteger;
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
    final public static byte rsaEncryption[] = Asn1.string2Oid("1.2.840.113549.1.1.1");
//        { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x01 };
    final static byte md2withRSAEncryption[] = Asn1.string2Oid("1.2.840.113549.1.1.2");
    //{ (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x02 };
    final static byte md5withRSAEncryption[] = Asn1.string2Oid("1.2.840.113549.1.1.4");
//    { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x04 };
    final static byte sha1withRSAEncryption[] = Asn1.string2Oid("1.2.840.113549.1.1.5");
//    { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x05 };
    final static byte sha256WithRSAEncryption[] = Asn1.string2Oid("1.2.840.113549.1.1.11");
//    { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 11 };

    final static byte id_at_aliasedEntryName[] = Asn1.string2Oid("2.5.4.1");
    final static byte id_at_knowldgeinformation[] = Asn1.string2Oid("2.5.4.2");
    final static byte id_at_commonName[] = Asn1.string2Oid("2.5.4.3");
    final static byte id_at_surname[] = Asn1.string2Oid("2.5.4.4");
    final static byte id_at_serialNumber[] = Asn1.string2Oid("2.5.4.5");
    final static byte id_at_countryName[] = Asn1.string2Oid("2.5.4.6");
    final static byte id_at_localityName[] = Asn1.string2Oid("2.5.4.7");
    final static byte id_at_stateOrProvinceName[] = Asn1.string2Oid("2.5.4.8");
    final static byte id_at_streetAddress[] = Asn1.string2Oid("2.5.4.9");
    final static byte id_at_organizationName[] = Asn1.string2Oid("2.5.4.10");
    final static byte id_at_organizationalUnitName[] = Asn1.string2Oid("2.5.4.11");
    final static byte id_at_email[] = Asn1.string2Oid("1.2.840.113549.1.9.1");

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
     * Create a certifcate without siganture. You have to sign this later by
     * calling sign().
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
     * Create a certificate request without signature. You have to sign this
     * later by calling sign().
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
     *      CertificationRequestInfo ::= SEQUENCE { version Version, subject
     *      Name, subjectPublicKeyInfo SubjectPublicKeyInfo, attributes [0]
     *      IMPLICIT Attributes }
     */
    public static byte[] createCertificateRequest(byte owner[], byte pubN[], byte pubE[], byte privF[]) {
        byte ver[] = Asn1.makeASN1(0, Asn1.INTEGER);

        // public Key codieren
        byte t[];
        t = Asn1.addTo(newSeq, Asn1.makeASN1(pubN, 2));
        t = Asn1.addTo(t, Asn1.makeASN1(pubE, 2));
        byte pub[] = Asn1.makeASN1(t, 3);

        // byte sig[] = Pkcs6.sign(pub, pubN, privF);

        // Kennung f??r RSA
        t = Asn1.addTo(newSeq, Asn1.makeASN1(rsaEncryption, 6));
        t = Asn1.addTo(t, nul);
        t = Asn1.addTo(newSeq, t);
        pub = Asn1.addTo(t, pub);
        // pub = Asn1.addTo(t, sig);

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
     * Sign a given sequence with a RSA private key. This is e.g. used for
     * signing certificates or certificate requests. An enhanced version which
     * uses the Chinese remainder theorem is planned <g>.
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

        return padSignedContent(signedContent, keyLength, 1);
    }

    public static byte[] padSignedContent(byte[] signedContent, int keyLength, int type) {
        byte[] t = new byte[keyLength];
        t[0] = 0;
        t[1] = (byte) type;

        for (int i = 2; i < t.length - signedContent.length; ++i) {
            t[i] = (byte) 0xff;
        }
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

    private final static int CERTPK_PATH[] = { 0x90, 0x90, 0x10, 0x10, 0x10, 0x10, 0x90, 0x83 };
    private final static int CSRPK_PATH[] = { 0x90, 0x90, 0x10, 0x90, 0x83 };

    /**
     * Get the public modulo from a X.509 certificate
     * 
     * @param cert
     *            - a X.509 certificate
     * @return the modulo, or null on error
     */
    public static byte[] getX509Modulo(byte cert[]) {
        // get the public modulo from certificate
        byte b[] = Asn1.getSeq(cert, CERTPK_PATH, 0);
        if (b == null)
            b = Asn1.getSeq(cert, CSRPK_PATH, 0);
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
        byte b[] = Asn1.getSeq(cert, CERTPK_PATH, 0);
        if (b == null)
            b = Asn1.getSeq(cert, CSRPK_PATH, 0);

        int p2[] = { 0x90, 0x02, 0x82 };
        return Asn1.getSeq(b, p2, b[0] == 0 ? 1 : 0);
    }

    private static int CERTSIGPATH_PATH[] = { 0x90, 0x10, 0x10, 0x83 };
    private static int CERTCONTENT_PATH[] = { 0x90, 0x10 };
    private static int SIGNATUREHASH_PATH[] = { 0x90, 0x84 };

    public static byte[] getCertificateSignature(byte[] cert) {
        byte signedData[] = Asn1.getSeq(cert, CERTSIGPATH_PATH, 0);
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

    private final static int ENCRYPTION_PATH[] = { 0x90, 0x90, 6 };
    private final static int ENCRYPTION_PATH_CSR[] = {0x90, 0x10, 0x90, 6 };

    /**
     * Returns the signature if the chain was validated successfully.
     * 
     * @param certs
     *            a vector of certificates
     * @return the signature if the chain was validated successfully, null
     *         otherwise.
     */
    public static byte[] getCertificateSignature(Vector<byte[]> certs) {
        if (certs.isEmpty())
            return null;

        byte[] cert = certs.get(certs.size() - 1);
        byte[] n = getX509Modulo(cert);
        byte[] e = getX509Exponent(cert);

        byte[] hash = null;
        for (int i = certs.size() - 1; i >= 0; --i) {
            cert = certs.get(i);
            byte signature[] = Asn1.getSeq(cert, CERTSIGPATH_PATH, 0);
            if (signature == null || n == null || e == null)
                return null;

            byte[] decodedSignature = doRSA(signature, n, e);
            byte[] encodedHash = decodeRSA(decodedSignature);
            if (encodedHash == null)
                return null;

            hash = Asn1.getSeq(encodedHash, SIGNATUREHASH_PATH, 0);
            if (hash == null)
                return null;

            byte signedData[] = Asn1.getSeq(cert, CERTCONTENT_PATH, 0);
            if (signedData == null)
                return null;

            byte oid[] = Asn1.getSeq(signedData, ENCRYPTION_PATH, 0);
            if (oid == null)
                oid = Asn1.getSeq(cert, ENCRYPTION_PATH_CSR, 0);
            if (oid == null)
                return null;

            byte oidData[] = Asn1.getData(oid);

            MessageDigest md = null;
            if (Misc.equals(oidData, sha1withRSAEncryption))
                md = new SHA();
            else if (Misc.equals(oidData, sha256WithRSAEncryption))
                md = new SHA256();
            else if (Misc.equals(oidData, md5withRSAEncryption))
                md = new MD5();

            if (md == null)
                return null;

            byte calcedHash[] = md.digest(signedData);

            if (!Misc.equals(hash, calcedHash))
                return null;

            n = getX509Modulo(cert);
            e = getX509Exponent(cert);
        }

        return hash;
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
        // BigInteger bn = new BigInteger(1, n);
        // BigInteger bf = new BigInteger(1, e);
        // BigInteger zz = new BigInteger(1, z);
        // zz = zz.modPow(bf, bn);

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
        BigInteger bz = new BigInteger(1, z);

        BigInteger p = new BigInteger(1, pkData[3]);
        BigInteger q = new BigInteger(1, pkData[4]);
        BigInteger dp1 = new BigInteger(1, pkData[5]);
        BigInteger dq1 = new BigInteger(1, pkData[6]);
        BigInteger iqmp = new BigInteger(1, pkData[7]);

        BigInteger cDp = bz.modPow(dp1, p);
        BigInteger cDq = bz.modPow(dq1, q);
        if (cDp.compareTo(cDq) < 0)
            cDp = cDp.add(p);
        BigInteger u = ((cDp.subtract(cDq)).multiply(iqmp)).remainder(p);
        BigInteger r = cDq.add(u.multiply(q));

        byte[] b = r.toByteArray();
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

        // System.out.println("RSA took: " + (System.currentTimeMillis() -
        // start) + "ms");
        return b2;
    }
    
    
    public static BigInteger generatePrime(int bitsP) {

        int nb = 1;
        for (int i = bitsP; i > 0; ++nb) {
            i >>>= 1;
        }

        nb = (bitsP - nb) >> 1;

        for (;;) {
            BigInteger r = primeForStrongPrime(nb);
            BigInteger s = new BigInteger(nb, SecureRandom.getInstance());
            BigInteger rs = r.multiply(s);
            BigInteger rr = s.modPow(r.subtract(BigInteger.ONE), rs);
            BigInteger ss = r.modPow(s.subtract(BigInteger.ONE), rs);
            if (rr.compareTo(ss) < 0)
                rr = ss.subtract(rr);
            else
                rr = rr.subtract(ss);

            BigInteger z = primeInArithmeticProgression(bitsP, rr, rs);
            if (z != null)
                return z;
        }
    }

    static private BigInteger primeForStrongPrime(int bitsP) {
        int nb = 1;
        for (int i = bitsP; i > 0; ++nb)
            i >>>= 1;

        nb = bitsP - nb;

        for (;;) {
            BigInteger z = primeInArithmeticProgression(bitsP, BigInteger.ONE,
                    new BigInteger(nb, SecureRandom.getInstance()));
            if (z != null)
                return z;
        }
    }

    static private BigInteger primeInArithmeticProgression(int bitsP, BigInteger z0, BigInteger d) {
        BigInteger z = BigInteger.ONE.shiftLeft(bitsP - 1);
        z = z.subtract(z.mod(d)).add(z0.mod(d));
        // gerade?
        if (!z.testBit(0))
            z = z.add(d);
        d = d.add(d);

        // sicherstellen, daß z groß genug ist
        while (z.bitLength() < bitsP)
            z = z.add(d);

        // suchen, bis der Bereich abgegrast ist
        while (z.bitLength() == bitsP) {
            if (!z.testBit(1) && z.isProbablePrime(33))
                return z; // yepp!
            z = z.add(d);
        }
        // keine gefunden
        return null;
    }

}
