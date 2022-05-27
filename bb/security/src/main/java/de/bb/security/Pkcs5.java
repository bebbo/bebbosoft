/*
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Pkcs5.java,v $
 * $Revision: 1.5 $
 * $Date: 2014/06/23 15:51:15 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 *
 * Copyright (c) by Stefan Bebbo Franke 1999/2000.
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 */

package de.bb.security;

import java.util.Random;

import de.bb.util.Mime;
import de.bb.util.Misc;

/**
 * This class contains functions from Pkcs 5
 */
public class Pkcs5 {
    final static byte encryptionAlgorithm3DES[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x03, (byte) 0x07 };
    final static byte pbkdf2[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D,
            (byte) 0x01, (byte) 0x05, 12 };
    final static byte pbes2[] = { (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D,
            (byte) 0x01, (byte) 0x05, 13 };

    final static byte newSeq[] = { (byte) 0x30, (byte) 0x80 };

    final static Random rand = SecureRandom.getInstance();

    /**
     * Generates a key from password, salt and iteration count using the given message digest. The key len is less equal
     * to the hash result len!
     * 
     * @param pwd
     *            the password.
     * @param salt
     *            the salt.
     * @param count
     *            the used iteration count. A big iteration count increases protected against brute force attacks.
     * @param md
     *            the used message digest.
     * @param klen
     *            the len of the key - note: klen <= length(MessageDigest)!
     * @param a
     *            new allocated byte array containing the key.
     * @see PKCS-5
     */
    public static byte[] pbkdf1(byte pwd[], byte salt[], int count, MessageDigest md, int kLen) {
        md.update(pwd);
        md.update(salt);
        for (int i = 1; i < count; ++i)
            md.update(md.digest());
        byte b[] = new byte[kLen];
        System.arraycopy(md.digest(), 0, b, 0, kLen);
        return b;
    }

    /**
     * Generates a key from password, salt and iteration count using the given message digest. like pbkdf1, but for
     * indeoendend key length
     * 
     * @param pwd
     *            the password.
     * @param salt
     *            the salt.
     * @param count
     *            the used iteration count. A big iteration count increases protected against brute force attacks.
     * @param md
     *            the used message digest.
     * @param klen
     *            the len of the key - note: klen <= length(MessageDigest)!
     * @param a
     *            new allocated byte array containing the key.
     * @see PKCS-5
     */
    public static byte[] pbkdf1a(byte pwd[], byte salt[], int count, MessageDigest md, int kLen) {
        byte b[] = new byte[kLen];
        byte m[] = new byte[0];
        for (int j = 0; j < kLen; j += m.length) {
            md.update(m);
            md.update(pwd);
            md.update(salt);
            for (int i = 1; i < count; ++i)
                md.update(md.digest());
            m = md.digest();
            if (j + m.length <= kLen)
                System.arraycopy(m, 0, b, j, m.length);
            else
                System.arraycopy(m, 0, b, j, kLen - j);
        }
        return b;
    }

    /**
     * Generates a key from password, salt and iteration count using the given message digest. It generates keys of any
     * length.
     * 
     * @param pwd
     *            the password.
     * @param salt
     *            the salt.
     * @param count
     *            the used iteration count. A big iteration count increases protected against brute force attacks.
     * @param md
     *            the used message digest.
     * @param klen
     *            the len of the key.
     * @param a
     *            new allocated byte array containing the key.
     * @see PKCS-5
     */
    public static byte[] pbkdf2Broken(byte pwd[], byte salt[], int count, MessageDigest md, int kLen) {
        md.update((byte) 0);
        int hLen = md.digest().length;
        int pass = kLen + hLen - 1;
        pass -= pass % hLen;
        byte res[] = new byte[pass];
        pass /= hLen;
        for (int i = 0; i < pass; ++i) {
            md.update(pwd);
            md.update(salt);
            for (int j = 0; j < 4; ++j)
                md.update((byte) (i >>> (3 - j) * 8));
            byte u[] = md.digest();
            int off = i * hLen;
            System.arraycopy(res, off, u, 0, hLen);
            for (int j = 1; j < count; ++j) {
                md.update(pwd);
                md.update(u);
                u = md.digest();
                for (int k = 0; k < hLen; ++k)
                    res[off + k] ^= u[k];
            }
        }
        byte r[] = new byte[kLen];
        System.arraycopy(res, 0, r, 0, kLen);
        return r;
    }

    /**
     * Generates a key from password, salt and iteration count using the given message digest. It generates keys of any
     * length.
     * 
     * @param pwd
     *            the password.
     * @param salt
     *            the salt.
     * @param count
     *            the used iteration count. A big iteration count increases protected against brute force attacks.
     * @param md
     *            the used message digest.
     * @param klen
     *            the len of the key.
     * @param a
     *            new allocated byte array containing the key.
     * @see PKCS-5
     */
    public static byte[] pbkdf2(byte pwd[], byte salt[], int count, MessageDigest md, int kLen) {

        // get digest length
        md.update((byte) 0);
        int hLen = md.digest().length;

        // calc buffer size + passes
        int pass = kLen + hLen - 1;
        pass -= pass % hLen;
        byte res[] = new byte[pass];
        pass /= hLen;

        byte n[] = new byte[4];

        for (int i = 1; i <= pass; ++i) {
            for (int j = 0; j < 4; ++j)
                n[j] = (byte) (i >>> (3 - j) * 8);

            byte u[] = md.hmac(pwd, salt, n, null, null, null);
            int off = i * hLen - hLen;
            System.arraycopy(u, 0, res, off, hLen);

            for (int j = 1; j < count; ++j) {
                u = md.hmac(pwd, u, null, null, null, null);
                for (int k = 0; k < hLen; ++k)
                    res[off + k] ^= u[k];
            }
        }
        byte r[] = new byte[kLen];
        System.arraycopy(res, 0, r, 0, kLen);
        return r;
    }

    /**
     * This function performs an encryption using the given BlockCipher. Note that there must exist enough keyData to
     * create key and iv!
     * 
     * @param bc
     *            the used BlockCipher.
     * @param msg
     *            the message to pad and encrypt.
     * @keyData data for key and initialization vector.
     * @see PKCS-5
     */
    public static byte[] pbes(BlockCipher bc, byte msg[], byte keyData[]) {
        byte iv[] = new byte[bc.blockSize()];
        byte key[] = new byte[keyData.length - iv.length];
        System.arraycopy(keyData, 0, key, 0, key.length);
        System.arraycopy(keyData, key.length, iv, 0, iv.length);
        bc.setKey(keyData);

        return bc.encryptCBCAndPadd(iv, msg);
    }

    /**
     * This function performs an decryption using the given BlockCipher. Note that there must exist enough keyData to
     * create key and iv!
     * 
     * @param bc
     *            the used BlockCipher.
     * @param msg
     *            the message to decrypt and unpad.
     * @keyData data for key and initialization vector.
     * @see PKCS-5
     */
    public static byte[] pbds(BlockCipher bc, byte msg[], byte keyData[]) {
        byte iv[] = new byte[bc.blockSize()];
        byte key[] = new byte[keyData.length - iv.length];
        System.arraycopy(keyData, 0, key, 0, key.length);
        System.arraycopy(keyData, key.length, iv, 0, iv.length);
        bc.setKey(keyData);

        return bc.decryptCBCAndPadd(iv, msg);
    }

    /**
     * Create a new sequence conatining an PBES2 encoded sequence.
     * 
     * @param in
     *            original data.
     * @param count
     *            the used iteration count.
     * @param algid
     *            a string which identifies the used algorithm.
     * @param keyLen
     *            the len for the used key.
     * @passwd the used password. valid values are "3DES".
     * @return a new allocated ASN.1 sequence containing encoded and encrypted data or null on error.
     * @see PKCS-5
     */
    public static byte[] pb2encrypt(byte in[], int count, String algid, int keyLen, String passwd) {
        try {
            BlockCipher bc = null;
            if (algid.compareTo("3DES") == 0)
                bc = new DES3();
            if (bc == null)
                return null;

            // key generieren
            SHA sha = new SHA();
            byte[] salt = new byte[8];
            rand.nextBytes(salt);
            byte iv[] = new byte[bc.blockSize()];
            byte key[] = pbkdf2(passwd.getBytes(), salt, count, sha, iv.length + keyLen);
            System.arraycopy(key, keyLen, iv, 0, iv.length);
            // verschluesseln
            byte b[] = pbes(bc, in, key);

            // und nun die Struktur aufbauen:
            // pkdf2 params
            byte seq[] = newSeq;
            seq = Asn1.addTo(seq, Asn1.makeASN1(salt, 4)); // 4 = octet string
            seq = Asn1.addTo(seq, Asn1.makeASN1(count, 2)); // 2 = integer
            seq = Asn1.addTo(seq, Asn1.makeASN1(keyLen, 2)); // 2 = integer
            seq = Asn1.addTo(seq, Asn1.makeASN1(Pkcs1.digestAlgorithmSHA, 6)); // OID

            // pbes2 params
            byte seq2[] = Asn1.addTo(newSeq, Asn1.makeASN1(pbkdf2, 6)); // OID
            seq2 = Asn1.addTo(seq2, seq);
            seq2 = Asn1.addTo(seq2, Asn1.makeASN1(encryptionAlgorithm3DES, 6)); // OID
            seq2 = Asn1.addTo(seq2, Asn1.makeASN1(iv, 4)); // 4 = octet string

            // info
            seq = Asn1.addTo(newSeq, Asn1.makeASN1(pbes2, 6)); // OID
            seq = Asn1.addTo(seq, seq2);

            // all
            seq2 = Asn1.addTo(newSeq, seq);
            seq2 = Asn1.addTo(seq2, Asn1.makeASN1(b, 4)); // 4 = octet string

            return seq2;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Restore original data froam a sequence conatining an PBES2 encoded sequence.
     * 
     * @param in
     *            encrypted data.
     * @passwd the used password.
     * @return a new allocated ASN.1 sequence containing encoded and encrypted data.
     * @see PKCS-5
     */
    public static byte[] pb2decrypt(byte in[], String passwd) {
        try {
            int pas[] = { 0x90 }; // path to sequence
            int pad[] = { 0x90, 0x84 }; // path to octets
            int pao[] = { 0x90, 0x86 }; // path to oid
            int pao2[] = { 0x90, 6, 0x86 }; // path to oid
            int pai[] = { 0x90, 0x82 }; // path to integer
            int pai2[] = { 0x90, 2, 0x82 }; // path to 2nd integer

            byte seq[] = Asn1.getSeq(in, pas, 0); // info
            byte data[] = Asn1.getSeq(in, pad, 0); // data

            byte oid[] = Asn1.getSeq(seq, pao, 0); // must be oid PBES2
            if (!Misc.equals(oid, 0, pbes2, 0, pbes2.length))
                return null;

            seq = Asn1.getSeq(seq, pas, 0); // pbes2 params
            oid = Asn1.getSeq(seq, pao, 0); // must be oid PBKDF2
            if (!Misc.equals(oid, 0, pbkdf2, 0, pbkdf2.length))
                return null;

            byte cipher[] = Asn1.getSeq(seq, pao2, 0); // get used cipher
            if (!Misc.equals(cipher, 0, encryptionAlgorithm3DES, 0, encryptionAlgorithm3DES.length))
                return null;

            byte iv[] = Asn1.getSeq(seq, pad, 0); // get used initialization
                                                  // vector

            seq = Asn1.getSeq(seq, pas, 0); // pbkdf2 params
            byte salt[] = Asn1.getSeq(seq, pad, 0); // get used salt
            int count = Asn1.getInt(seq, pai); // get used count
            int keyLen = Asn1.getInt(seq, pai2); // get used keyLen
            byte algId[] = Asn1.getSeq(seq, pao, 0); // digest alg
            if (!Misc.equals(algId, 0, Pkcs1.digestAlgorithmSHA, 0, Pkcs1.digestAlgorithmSHA.length))
                return null;

            BlockCipher bc = new DES3();

            // key generieren
            SHA sha = new SHA();
            byte key[] = pbkdf2(passwd.getBytes(), salt, count, sha, iv.length + keyLen);
            if (!Misc.equals(key, keyLen, iv, 0, iv.length))
                return null;
            // entschluesseln
            data = pbds(bc, data, key);

            return data;

        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Verify a password by encoding the given password and comparing the result to the provided encoded password.
     * 
     * @param encodedPassword
     *            the encoded password
     * @param password
     *            the provided password
     * @return true if the password matches
     */
    public static boolean verifyPbkdf2(final String encodedPassword, final String password) {
        try {

            // must start with {....}
            int ce = encodedPassword.indexOf('}');
            if (ce <= 0)
                return false;

            if (encodedPassword.charAt(0) != '{')
                return false;

            final String palgo = encodedPassword.substring(1, ce);

            boolean old = true;
            String algo = null;
            if (palgo.startsWith("P5"))
                algo = palgo.substring(2);
            else if (palgo.startsWith("PKCS5"))
                algo = palgo.substring(5);
            else if (palgo.startsWith("PBKDF2")) {
                if (palgo.length() == 6) {
                    algo = "SHA";
                } else if (palgo.length() > 7) {
                    algo = palgo.substring(7);
                    old = false;
                }
            }

            if (algo == null)
                return false;

            final MessageDigest md = MessageDigest.get(algo);
            if (md == null)
                return false;

            if (old) {
                // decode data
                byte decoded[] = Mime.decode(encodedPassword.substring(ce + 1).getBytes());
                if (decoded == null || decoded.length != 2 + 16 + 32)
                    return false;

                // 2 bytes --> passes
                int passes = 1 << (0xff & decoded[0]) | (0xff & decoded[1]);

                // 16 bytes --> salt
                byte salt[] = new byte[16];
                System.arraycopy(decoded, 2, salt, 0, 16);

                // 32 bytes --> hash
                byte[] pbkdf2 = null;

                if (palgo.startsWith("P5"))
                    pbkdf2 = Pkcs5.pbkdf2Broken(password.getBytes("utf-8"), salt, passes, md, 32);
                else if (palgo.startsWith("PKCS5"))
                    pbkdf2 = Pkcs5.pbkdf2(password.getBytes("utf-8"), salt, passes, md, 32);

                if (pbkdf2 == null)
                    return false;
                return Misc.equals(pbkdf2, 0, decoded, 18, 32);
            }

            // new {PBKDF2-hash}nnnnn$saltbase64$hashbase64
            ++ce;
            int dollar = encodedPassword.indexOf('$', ce);
            if (dollar <= ce)
                return false;
            int passes = Integer.parseInt(encodedPassword.substring(ce, dollar));

            ce = dollar + 1;
            dollar = encodedPassword.indexOf('$', ce);
            if (dollar <= ce)
                return false;

            byte salt[] = Mime.decode(encodedPassword.substring(ce, dollar).getBytes());
            byte hash[] = Mime.decode(encodedPassword.substring(dollar + 1).getBytes());
            byte[] pbkdf2 = Pkcs5.pbkdf2(password.getBytes("utf-8"), salt, passes, md, 32);

            return Misc.equals(pbkdf2, hash);

        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Encode a password using PBKDF2, the specified hash algorithm and the number of passes.
     * 
     * @param algo
     *            the message digest e.g. "SHA256"
     * @param password
     *            the password to ncode
     * @param n2Passes
     *            the passes - this value is used as (1 << n2Passes).
     * @return a String containing the hashed and encoded password
     */
    public static String encodePbkdf2(String algo, final String password, int n2Passes) {
        int rand = SecureRandom.getInstance().nextInt() & 0xff;
        try {
            final MessageDigest md = MessageDigest.get(algo);
            if (md == null)
                throw new RuntimeException("unsupported Hash: " + algo);

            int passes = 1 << (n2Passes) | rand;

            final byte salt[] = new byte[16];
            SecureRandom.getInstance().nextBytes(salt);

            byte[] pbkdf2 = Pkcs5.pbkdf2(password.getBytes("utf-8"), salt, passes, md, 32);

            if (algo.equals("SHA"))
                algo = "SHA1";

            final String encodedPassword = "{PBKDF2-" + algo + "}" + passes + "$" + new String(Mime.encode(salt)) + "$"
                    + new String(Mime.encode(pbkdf2));
            return encodedPassword;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

/*
 * $Log: Pkcs5.java,v $ Revision 1.5 2014/06/23 15:51:15 bebbo
 * 
 * @N added helper function for easy password encoding and checking using PBKDF2
 * 
 * Revision 1.4 2007/04/21 11:33:56 bebbo
 * 
 * @N added AES and DES to SSLv3
 * 
 * Revision 1.3 2002/11/06 09:46:12 bebbo
 * 
 * @I cleanup for imports
 * 
 * Revision 1.2 2001/03/29 18:25:04 bebbo
 * 
 * @C comments
 * 
 * Revision 1.1 2001/03/05 17:47:44 bebbo
 * 
 * @N new or changed comment
 * 
 * Revision 1.3 2000/09/08 07:46:22 bebbo
 * 
 * @I SecureRandom is a static member now
 * 
 * Revision 1.2 2000/06/22 16:27:30 bebbo
 * 
 * @I changes reflecting the split from Pkcs into Pkcs1, Pkcs5, Pkcs6 and Pkcs12
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
 * Revision 1.6 2000/05/22 09:30:09 hagen removed cvs conflicts static
 * initializer fx replaced by direct assignment (doesnt compile under JDK 1.2
 * otherwise)
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