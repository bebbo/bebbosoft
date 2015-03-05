/*
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Pkcs12.java,v $
 * $Revision: 1.3 $
 * $Date: 2012/11/11 18:34:14 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * Copyright (c) by Netlife Internet Consulting and Software GmbH 2000.
 * All rights reserved.
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999/2000.
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 */

package de.bb.security;

import java.math.BigInteger;

import de.bb.util.Misc;

/**
 * This class contains functions from Pkcs 12
 */
public class Pkcs12 {
    final static byte newSeq[] = {(byte) 0x30, (byte) 0x80};

    final static byte pkcs7data[] = Asn1.string2Oid("1.2.840.113549.1.7.1");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x07, (byte) 1};
    final static byte pkcs7encryptedData[] = Asn1.string2Oid("1.2.840.113549.1.7.6");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x07, (byte) 6};
    final static byte pbewithSHAAnd40BitRC2_CBC[] = Asn1.string2Oid("1.2.840.113549.1.12.1.6");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 12, (byte) 1, (byte) 6};
    final static byte pbeWithSHAAnd3_KeyTripleDES_CBC[] = Asn1.string2Oid("1.2.840.113549.1.12.1.3");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 12, (byte) 1, (byte) 3};
    final static byte pkcs8ShroudedKeyBag[] = Asn1.string2Oid("1.2.840.113549.1.12.10.1.2");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 12, (byte) 10, (byte) 1, (byte) 2};
    final static byte pkcs8CertificateBag[] = Asn1.string2Oid("1.2.840.113549.1.12.10.1.3");
    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 12, (byte) 10, (byte) 1, (byte) 3};

    final static byte pkcs9certStore[] = Asn1.string2Oid("1.2.840.113549.1.9.22.1");

    //{(byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 9, (byte) 22, (byte) 1};

    /**
     * Generate key, iv or mac data conforming to PKCS12
     * 
     * @param pwd
     *            - the password as String
     * @param salt
     *            - the salt bytes
     * @param iCount
     *            - count of itterations, defaults to 2048
     * @param md
     *            - the used MessageDigest
     * @param id
     *            - 1 for key-, 2 for iv-, 3 for mac-material
     * @param len
     *            - the required material length
     * @return a new allocated byte array containing the material
     */
    static public byte[] pkcs12Gen(String pwd, byte salt[], int iCount, MessageDigest md, int id, int len) {
        byte b[] = pwd.getBytes();
        byte b2[] = new byte[b.length * 2 + 2];
        for (int i = 0; i < b.length; ++i) {
            b2[2 * i] = 0;
            b2[2 * i + 1] = b[i];
        }
        return pkcs12Gen(b2, salt, iCount, md, id, len);
    }

    /**
     * Generate key, iv or mac data conforming to PKCS12
     * 
     * @param pwd
     *            - the password bytes - UNICODE!!
     * @param salt
     *            - the salt bytes
     * @param iCount
     *            - count of iterations, defaults to 2048
     * @param md
     *            - the used MessageDigest
     * @param id
     *            - 1 for key-, 2 for iv-, 3 for mac-material
     * @param len
     *            - the required material length
     * @return a new allocated byte array containing the material
     */
    static public byte[] pkcs12Gen(byte pwd[], byte salt[], int iCount, MessageDigest md, int id, int len) {
        byte ret[] = new byte[len];
        md.update((byte) 0);
        int hLen = md.digest().length;
        int hBlock = 64;
        byte idb[] = new byte[hBlock];
        byte temp[] = new byte[hBlock];
        for (int i = 0; i < hBlock; ++i)
            idb[i] = (byte) id;
        int slen = hBlock * ((salt.length + hBlock - 1) / hBlock);
        int plen = hBlock * ((pwd.length + hBlock - 1) / hBlock);
        byte data[] = new byte[slen + plen];
        for (int i = 0; i < slen; ++i)
            data[i] = salt[i % salt.length];
        for (int i = 0; i < plen; ++i)
            data[i + slen] = pwd[i % pwd.length];
        // Trace.dump(new PrintWriter(System.out), "idb", idb, idb.length);
        // Trace.dump(new PrintWriter(System.out), "data", data, data.length);
        for (int pos = 0; pos < len;) {
            md.update(idb);
            md.update(data);
            byte b[] = md.digest();
            // Trace.dump(new PrintWriter(System.out), "b", b, b.length);
            for (int i = 1; i < iCount; ++i) {
                md.update(b);
                b = md.digest();
            }
            System.arraycopy(b, 0, ret, pos, pos + hLen > len ? len - pos : hLen);
            pos += hLen;
            if (pos < len) {
                for (int i = 0; i < hBlock; ++i)
                    temp[i] = b[i % hLen];
                // Trace.dump(new PrintWriter(System.out), "B", temp, temp.length);
                BigInteger bi = new BigInteger(1, temp);
                bi = bi.add(BigInteger.ONE);
                for (int i = 0; i < slen + plen; i += hBlock) {
                    int bl = i + hBlock > data.length ? data.length - i : hBlock;
                    byte temp2[] = new byte[bl];
                    System.arraycopy(data, i, temp2, 0, bl);
                    BigInteger bi2 = new BigInteger(1, temp2);
                    // System.out.println(bi.toString());
                    // System.out.println(bi2.toString());
                    bi2 = bi2.add(bi);
                    // System.out.println(bi2.toString());
                    byte r[] = bi2.toByteArray();
                    // Trace.dump(new PrintWriter(System.out), "I+j", r, r.length);
                    System.arraycopy(r, r.length > bl ? 1 : 0, data, i, bl);
                }
            }
        }

        return ret;
    }

    /**
     * Decrypt the given data using the supplied encryption info
     * 
     * @param pwd
     *            - the password to decrypt
     * @param data
     *            - encrypted data
     * @param info
     *            - the encryption info
     * @return a new allocated byte array containing the decrypted data or null
     */
    static public byte[] pkcs12decrypt(String pwd, byte[] data, byte[] info) {
        // Asn1Dump.dumpSeq(info, 0, 0);

        int keyLen;
        MessageDigest md;
        BlockCipher bc;

        // get encryptione method
        int pMethod[] = {0x90, 0x86};
        byte method[] = Asn1.getSeq(info, pMethod, 0);

        // setup md and cipher
        if (Misc.equals(method, pbeWithSHAAnd3_KeyTripleDES_CBC)) {
            md = new SHA();
            bc = new DES3();
            keyLen = 24;
        } else if (Misc.equals(method, pbewithSHAAnd40BitRC2_CBC)) {
            md = new SHA();
            bc = new RC2();
            keyLen = 5;
        } else {
            // writeln("unsupported encryption method");
            // Asn1Dump.dumpSeq(info, 0, 0);
            return null;
        }

        // get salt
        int pSalt[] = {0x90, 0x90, 0x84};
        byte[] salt = Asn1.getSeq(info, pSalt, 0);

        // get iteration count
        int pCount[] = {0x90, 0x90, 0x82};
        int iCount = Asn1.getInt(info, pCount);
        // writeln("iCount = " + iCount);

        // create the key material
        byte key[] = pkcs12Gen(pwd, salt, iCount, md, 1, keyLen);
        byte iv[] = pkcs12Gen(pwd, salt, iCount, md, 2, bc.blockSize());

        // Trace.dump(new PrintWriter(System.out), "key", key, key.length);
        // Trace.dump(new PrintWriter(System.out), "iv", iv, iv.length);
        if (Misc.equals(method, pbewithSHAAnd40BitRC2_CBC)) {
            ((RC2) bc).setKey(key, 40);
        } else
            bc.setKey(key);

        byte[] dd = bc.decryptCBCAndPadd(iv, data);
        return dd;
    }

    /**
     * create encryption parameters
     * 
     * @param name
     *            - name of encryption, supports "3DES" and "RC2"
     * @param salt
     *            - salt to add or null
     * @param iCount
     *            - iCount to add or null;
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createParam(String name, byte salt[], int iCount) {
        byte oid[] = null;
        if (name.equalsIgnoreCase("3DES"))
            oid = pbeWithSHAAnd3_KeyTripleDES_CBC;
        else if (name.equalsIgnoreCase("RC2"))
            oid = pbewithSHAAnd40BitRC2_CBC;

        if (oid == null)
            return null;

        // params
        byte param[] = null;
        if (salt != null || iCount != 0)
            param = newSeq;
        if (salt != null)
            param = Asn1.addTo(param, Asn1.makeASN1(salt, Asn1.OCTET_STRING));
        if (iCount != 0)
            param = Asn1.addTo(param, Asn1.makeASN1(iCount, Asn1.INTEGER));

        byte seq[] = Asn1.addTo(newSeq, Asn1.makeASN1(oid, Asn1.OBJECT_IDENTIFIER));
        if (param != null)
            seq = Asn1.addTo(seq, param);

        return seq;
    }

    /**
     * create data block
     * 
     * @param param
     *            - parameter to add
     * @param data
     *            - dara to add
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createDataBlock(byte param[], byte data[]) {
        byte seq[] = newSeq;

        seq = Asn1.addTo(seq, Asn1.makeASN1(pkcs7data, Asn1.OBJECT_IDENTIFIER));
        seq = Asn1.addTo(seq, param);
        if (data != null) {
            seq = Asn1.addTo(seq, Asn1.makeASN1(data, 128));
        }

        return seq;
    }

    /**
     * create encryption block
     * 
     * @param param
     *            - parameter to add
     * @param data
     *            - dara to add
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createEncryptionBlock(byte param[], byte data[]) {
        byte seq[] = newSeq;

        seq = Asn1.addTo(seq, Asn1.makeASN1(pkcs7encryptedData, Asn1.OBJECT_IDENTIFIER));
        seq = Asn1.addTo(seq, param);
        if (data != null)
            seq = Asn1.addTo(seq, data);

        return seq;
    }

    /**
     * create shrouded key bag
     * 
     * @param data
     *            - parameter to add
     * @param opt
     *            - dara to add, or null
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createShroudedKeyBag(byte data[], byte opt[]) {
        byte seq[] = newSeq;

        seq = Asn1.addTo(seq, Asn1.makeASN1(pkcs8ShroudedKeyBag, Asn1.OBJECT_IDENTIFIER));
        seq = Asn1.addTo(seq, data);
        if (opt != null)
            seq = Asn1.addTo(seq, opt);

        return seq;
    }

    /**
     * create certificate bag
     * 
     * @param data
     *            - parameter to add
     * @param opt
     *            - dara to add, or null
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createCertificateBag(byte data[], byte opt[]) {
        byte seq[] = newSeq;

        seq = Asn1.addTo(seq, Asn1.makeASN1(pkcs8CertificateBag, Asn1.OBJECT_IDENTIFIER));
        seq = Asn1.addTo(seq, data);
        if (opt != null)
            seq = Asn1.addTo(seq, opt);

        return seq;
    }

    /**
     * create cert store
     * 
     * @param data
     *            - parameter to add
     * @param opt
     *            - dara to add, or null
     * @return a new allocated byte array containing the encryption Params, or null
     */
    public static byte[] createCertStore(byte data[], byte opt[]) {
        byte seq[] = newSeq;

        seq = Asn1.addTo(seq, Asn1.makeASN1(pkcs9certStore, Asn1.OBJECT_IDENTIFIER));
        seq = Asn1.addTo(seq, data);
        if (opt != null)
            seq = Asn1.addTo(seq, opt);

        return seq;
    }

}

/*
 * $Log: Pkcs12.java,v $
 * Revision 1.3  2012/11/11 18:34:14  bebbo
 * @I OID constants are using string2OID now
 *
 * Revision 1.2  2010/12/17 17:35:09  bebbo
 * @C fixed a typo
 * Revision 1.1 2003/01/25 15:08:07 bebbo
 * 
 * @N new!
 * 
 * Revision 1.4 2000/06/28 14:16:47 bebbo
 * 
 * @R removed usage of java.security.*
 * 
 * Revision 1.3 2000/06/19 11:15:59 bebbo
 * 
 * @B fixed createDataBlock: type 128 is used now.
 * 
 * Revision 1.2 2000/06/19 10:33:17 bebbo
 * 
 * @N many new helper functions
 * 
 * Revision 1.1 2000/06/18 17:12:35 bebbo
 * 
 * @N created
 */