/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/Pem2P12.java,v $
 * $Revision: 1.3 $
 * $Date: 2012/11/13 06:31:50 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Written by Stefan Bebbo Franke
 * Copyright (c) by Netlife Internet Consulting and Software GmbH 2000.
 * All rights reserved.
 *
 * Copyright (c) by Stefan Bebbo Franke 1999/2000.
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Vector;

import de.bb.security.Asn1;
import de.bb.security.DES3;
import de.bb.security.Pem;
import de.bb.security.Pkcs12;
import de.bb.security.Pkcs6;
import de.bb.security.RC2;
import de.bb.security.SHA;
import de.bb.util.Mime;
import de.bb.util.Misc;

public class Pem2P12 {
    final static byte nullBytes[] = {};
    static byte nul[];

    final static byte ID_SHA[] = {1 * 40 + 3, 14, 3, 2, 26};
    final static byte PKCS9_CERT_ID[] = Asn1.string2Oid("1.2.840.113549.1.9.21");
    final static byte PKCS9_CERT_NAME[] = Asn1.string2Oid("1.2.840.113549.1.9.20");

    static String getName(byte b[], int off) {
        int end = off;
        while (b[end] != '"')
            ++end;
        return new String(b, off, end - off);
    }

    public static void main(String args[]) {
        writeln("Pem2P12 $Revision: 1.3 $");
        if (args.length == 0) {
            writeln("usage: Pem2P12 <key file> [-n keyName] [<more certificates, requires outfile>] [<outfile>]");
            return;
            // String p[] = {"ca\\test.cer.b64"};
            // args = p;
        }
        File keyFile = new File(args[0]);
        String keyName = keyFile.getName();
        int l = keyName.indexOf('.');
        if (l > 0)
            keyName = keyName.substring(0, l);

        File outFile = null;
        if (args.length == 2) {
            outFile = new File(args[1]);
        } else {
            outFile = new File(keyFile.getParentFile(), keyName + ".p12");
        }
        keyName.replace('\\', '/');
        l = keyName.indexOf('/');
        keyName = keyName.substring(l + 1);

        try {
            FileInputStream fis = new FileInputStream(keyFile);

            // Daten in den Speicher lesen
            int len = fis.available();
            byte b[] = new byte[len];
            fis.read(b);
            fis.close();

            // Zertifikate sammeln, key lesen
            Vector<byte[]> certs = new Vector<byte[]>();

            byte mimeHead[] = "Content-Type: application/octet-stream; name=\"".getBytes();
            byte certHeader[] = "-----BEGIN CERTIFICATE".getBytes();
            byte rsaHeader[] = "-----BEGIN RSA PRIVATE KEY".getBytes();
            byte header[] = "-----BEGIN".getBytes();

            int rb64 = Mime.strstr(b, 0, rsaHeader);
            if (rb64 < 0) {
                writeln("no RSA PRIVATE KEY FOUND");
                return;
            }

            // read password
            write("password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            // String pwd = "bebbo";
            String pwd = br.readLine();

            byte[] rsaKey = Pem.decode(b, rb64, pwd);
            if (rsaKey == null) {
                writeln("wrong password");
                return;
            }

            // get the public modulo - we need it to get the certificate!
            int pan[] = {0x90, 2, 0x82};
            byte pubMod[] = Asn1.getSeq(rsaKey, pan, 0);

            // read certificates
            for (int off = 0; off < b.length;) {
                int cb64 = Mime.strstr(b, off, certHeader); // next certificate
                if (cb64 < 0)
                    break;
                if (cb64 >= 0) {
                    // check that the mime info is the last before the CERTIFICATE
                    for (int b64 = Mime.strstr(b, off, header); b64 != cb64; b64 =
                            Mime.strstr(b, b64 + header.length, header)) {
                        // is it a Mime info ahead the RSA private key?
                        if (b64 == rb64) {
                            int mime = Mime.strstr(b, off, mimeHead);
                            if (mime >= 0) {
                                String name = getName(b, mime + mimeHead.length);
                                writeln("loading key: " + name);
                            }
                        }
                        off = b64 + 1;
                    }

                    int mime = Mime.strstr(b, off, mimeHead);
                    if (mime >= 0) {
                        String name = getName(b, mime + mimeHead.length);
                        writeln("loading certificate:" + name);
                    }

                    // get the cert
                    byte c[] = Pem.decode(b, cb64, null);
                    certs.addElement(c);
                    off = cb64 + 1;
                }
            }

            for (int i = 1; i + 1 < args.length; ++i) {

                if ("-n".equals(args[i])) {
                    keyName = args[++i];
                    continue;
                }

                fis = new FileInputStream(args[i]);

                // Daten in den Speicher lesen
                len = fis.available();
                b = new byte[len];
                fis.read(b);
                fis.close();
                byte[] t = Pem.decode(b, 0, null);
                if (t == null)
                    t = b;
                certs.addElement(t);
            }

            // ok - now create the p12 file.
            Random rnd = new Random();
            SHA sha = new SHA();
            DES3 des3 = new DES3(); // Des3EDECipher();
            RC2 rc2 = new RC2();

            // prepare rsaKey
            byte data[] = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(Pkcs6.rsaEncryption, Asn1.OBJECT_IDENTIFIER));
            byte[] param = Asn1.addTo(data, nul);
            rsaKey = Asn1.makeASN1(rsaKey, Asn1.OCTET_STRING);

            data = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(0, Asn1.INTEGER));
            data = Asn1.addTo(data, param);
            rsaKey = Asn1.addTo(data, rsaKey);

            // encrypt the key ================================
            byte salt[] = new byte[8];
            rnd.nextBytes(salt);
            // salt = toHex("110f26e41833c137");

            int iCount = 2048;

            byte key[] = Pkcs12.pkcs12Gen(pwd, salt, iCount, sha, 1, 24);
            byte iv[] = Pkcs12.pkcs12Gen(pwd, salt, iCount, sha, 2, 8);
            des3.setKey(key);
            byte rsa[] = des3.encryptCBCAndPadd(iv, rsaKey);

            param = Pkcs12.createParam("3DES", salt, iCount);
            data = Asn1.addTo(Asn1.newSeq, param);
            data = Asn1.addTo(data, Asn1.makeASN1(rsa, Asn1.OCTET_STRING));

            // create encrypted data
            byte newX[] = {(byte) 160, (byte) 0x80};
            rsa = Asn1.addTo(newX, data);

            // uid = toHex("da4c7ebcad152137e5618541009169e65d22ddfd");

            // add the key name
            byte[] name = Asn1.makeASN1(keyName, Asn1.UTF16String);
            name = Asn1.addTo(Asn1.newSet, name);
            data = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(PKCS9_CERT_NAME, Asn1.OBJECT_IDENTIFIER));
            data = Asn1.addTo(data, name);
            byte[] info = Asn1.addTo(Asn1.newSet, data);

            // add the key UID - a random number
            byte uid[] = new byte[20];
            rnd.nextBytes(uid);
            uid = Asn1.makeASN1(uid, Asn1.OCTET_STRING);
            uid = Asn1.addTo(Asn1.newSet, uid);
            data = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(PKCS9_CERT_ID, Asn1.OBJECT_IDENTIFIER));
            data = Asn1.addTo(data, uid);
            info = Asn1.addTo(info, data);

            // create shrouded key bag
            data = Pkcs12.createShroudedKeyBag(rsa, info);
            data = Asn1.addTo(Asn1.newSeq, data);
            data = Asn1.makeASN1(data, Asn1.OCTET_STRING);
            data = Asn1.addTo(newX, data);
            rsa = Pkcs12.createDataBlock(data, null); // key is read

            // encrypt the certificates
            byte allCerts[] = Asn1.newSeq;
            for (int i = 0; i < certs.size(); ++i) {
                byte c[] = (byte[]) certs.elementAt(i);

                // check whether this cert matches our public modulo!
                byte n[] = Pkcs6.getX509Modulo(c);
                boolean isRootCert = Misc.equals(n, pubMod);

                c = Asn1.makeASN1(c, Asn1.OCTET_STRING);
                c = Asn1.addTo(newX, c);
                c = Pkcs12.createCertStore(c, null);
                c = Asn1.addTo(newX, c);

                if (isRootCert) {
                    data = info;
                } else {
                    data = Asn1.newSet;
                }
                c = Pkcs12.createCertificateBag(c, null);

                c = Asn1.addTo(c, data);

                allCerts = Asn1.addTo(allCerts, c);
            }

            // Asn1Dump.dumpSeq(allCerts, 0, 0);

            rnd.nextBytes(salt);
            // salt = toHex("f0583b5e14780430");

            key = Pkcs12.pkcs12Gen(pwd, salt, iCount, sha, 1, 5);
            iv = Pkcs12.pkcs12Gen(pwd, salt, iCount, sha, 2, 8);
            rc2.setKey(key, 40);
            data = rc2.encryptCBCAndPadd(iv, allCerts);

            param = Pkcs12.createParam("RC2", salt, iCount);
            allCerts = Pkcs12.createDataBlock(param, data);

            data = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(0, Asn1.INTEGER));
            data = Asn1.addTo(data, allCerts);
            data = Asn1.addTo(newX, data);
            allCerts = Pkcs12.createEncryptionBlock(data, null);

            // now put both together
            byte sig[] = Asn1.addTo(Asn1.newSeq, allCerts);
            sig = Asn1.addTo(sig, rsa);

            data = Asn1.makeASN1(sig, Asn1.OCTET_STRING);
            data = Asn1.addTo(newX, data);
            data = Pkcs12.createDataBlock(data, null);

            // create the signature
            rnd.nextBytes(salt);
            byte hsh[] = Pkcs12.pkcs12Gen(pwd, salt, iCount, sha, 3, 20);
            byte vfy[] = sha.hmac(hsh, sig, null, null, null, null);

            param = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(ID_SHA, Asn1.OBJECT_IDENTIFIER));
            param = Asn1.addTo(param, nul);
            param = Asn1.addTo(Asn1.newSeq, param);
            vfy = Asn1.addTo(param, Asn1.makeASN1(vfy, Asn1.OCTET_STRING));

            param = Asn1.addTo(Asn1.newSeq, vfy);
            param = Asn1.addTo(param, Asn1.makeASN1(salt, Asn1.OCTET_STRING));
            param = Asn1.addTo(param, Asn1.makeASN1(iCount, Asn1.INTEGER));

            // create the final representation
            byte res[] = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(3, Asn1.INTEGER));
            res = Asn1.addTo(res, data);
            // add signature
            res = Asn1.addTo(res, param);

            // Asn1Dump.dumpSeq(res, 0, 0);

            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(res);
            fos.flush();
            fos.close();

        } catch (Exception e) {
            writeln(e.toString());
        }
    }

    static void write(String s) {
        System.out.print(s);
    }

    static void writeln(String s) {
        System.out.println(s);
    }

    // public static byte[] toHex(String s) { byte b[] = new byte[s.length()/2]; for (int i = 0; i < b.length; ++i) {
    // b[i] = (byte)Integer.parseInt(s.substring(2*i, 2*i+2), 16); } return b; }

    static {
        // a null element
        nul = Asn1.makeASN1(nullBytes, 5);
    }

}

/*
 * $Log: Pem2P12.java,v $
 * Revision 1.3  2012/11/13 06:31:50  bebbo
 * @N added support to set a key name
 * @B encrypted certificate also contains the same info block as used for the key
 *
 * Revision 1.2  2012/11/10 09:31:58  bebbo
 * @N supports adding multiple certificate files
 *
 * Revision 1.1  2012/08/11 19:56:52  bebbo
 * @I working stage
 * Revision 1.1 2010/12/17 17:20:17 bebbo
 * 
 * @INIT Revision 1.6 2000/06/20 16:31:29 bebbo
 * 
 * @B removed hard coded password
 * 
 * Revision 1.5 2000/06/20 16:28:05 bebbo
 * 
 * @R now working with Outlook too
 * 
 * Revision 1.4 2000/06/20 11:51:26 bebbo
 * 
 * @R now working with netscape (Outlook doesn't work yet)
 * 
 * Revision 1.3 2000/06/20 11:07:33 bebbo
 * 
 * @R now order of elements is similar to openssl
 * 
 * Revision 1.2 2000/06/19 11:16:40 bebbo
 * 
 * @B some fixes. First wokring version
 * 
 * Revision 1.1 2000/06/19 10:33:58 bebbo
 * 
 * @N new - not correctly working yet
 * 
 * Revision 1.4 2000/06/16 18:58:27 bebbo
 * 
 * @R now using Pem.xxxxx this version is now compatible to openSsl, Apache, etc.
 * 
 * Revision 1.3 1999/10/25 15:09:23 bebbo
 * 
 * @R changed package assignments
 * 
 * Revision 1.2 1999/09/02 19:07:20 Bebbo
 * 
 * @N created.
 * 
 * Revision 1.1 1999/08/27 12:17:53 Bebbo fixed some deprecated warnings.
 */

