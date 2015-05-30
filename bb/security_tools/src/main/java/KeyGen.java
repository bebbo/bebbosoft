/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/KeyGen.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/08/11 19:56:40 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * written by Stefan Bebbo Franke
 * All rights reserved.
 */

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Random;

import de.bb.security.SecureRandom;
import de.bb.security.Asn1;
import de.bb.security.Pem;
import de.bb.security.Pkcs1;
import de.bb.security.Pkcs6;
import de.bb.util.Mime;

/**
 * 1. Generate a key pair -> n,e,f,p,q 2. Save it encrypted to a file.
 */
public class KeyGen {
    static Random sr = SecureRandom.getInstance();

    public KeyGen() {
    }

    static public void main(String args[]) {
        writeln("KeyGen $Revision: 1.1 $");
        int klen = 1024;
        if (args.length == 2)
            klen = Integer.parseInt(args[1]);
        if (args.length < 1 || args.length > 2 || klen < 512 || klen > 4096) {
            writeln("usage: KeyGen <filename> [<512 <= bitlen <= 4096>]");
            return;
        }
        try {
            BigInteger p, q, e, one;
            byte ee[] = { 1, 0, 1 };
            e = new BigInteger(ee);
            byte bb[] = { 1 };
            one = new BigInteger(bb);
            write("e");
            // BigInteger a, b;
            do {
                int l1 = (klen + 1) / 2;
                //                    p = new BigInteger(klen / 2, 100, sr);
                write("p");
                p = Pkcs6.generatePrime(l1);
                //                    q = new BigInteger(klen / 2, 100, sr);
                write("q");
                q = Pkcs6.generatePrime(klen - l1);
                
                if (p.multiply(q).bitLength() < klen) {
                    write("P");
                    p = Pkcs6.generatePrime(l1 + 1);
                }
                
            } while (p.compareTo(q) == 0 || one.compareTo(e.gcd(p.subtract(one))) != 0
                    || one.compareTo(e.gcd(q.subtract(one))) != 0 || p.multiply(q).bitLength() != klen);
            writeln(" - ok");

            byte seq[] = Pkcs1.createKeyPair(p, q);

            // read a password
            write("password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String pwd = br.readLine();
            // String pwd = "test";

            FileOutputStream out = new FileOutputStream(args[0] + ".key.b64");
            out.write(Mime.createHeader(args[0] + ".key"));

            byte data[] = Pem.encode(seq, "RSA PRIVATE KEY", 0, pwd, null);
            out.write(data);

            // now create a certification request
            // int pad[] = {0x90, 0x84};
            int pan[] = { 0x90, 2, 0x82 };
            int pae[] = { 0x90, 2, 2, 0x82 };
            int paf[] = { 0x90, 2, 2, 2, 0x82 };
            // seq = Asn1.getSeq(seq, pad, 0);
            byte kn[] = Asn1.getSeq(seq, pan, 0);
            byte ke[] = Asn1.getSeq(seq, pae, 0);
            byte kf[] = Asn1.getSeq(seq, paf, 0);

            byte owner[] = Pkcs6.makeInfo(args[0] + ".ini");
            byte certreq[] = Pkcs6.createCertificateRequest(owner, kn, ke, kf);

            out = new FileOutputStream(args[0] + ".req.b64");
            out.write(Mime.createHeader(args[0] + ".req"));
            certreq = Pem.encode(certreq, "CERTIFICATE REQUEST", 0, null, null);
            out.write(certreq);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void write(String s) {
        System.out.print(s);
        System.out.flush();
    }

    static void writeln(String s) {
        System.out.println(s);
    }

}

/*
 * $Log: KeyGen.java,v $
 * Revision 1.1  2012/08/11 19:56:40  bebbo
 * @I working stage
 *
 * Revision 1.1  2010/12/17 17:20:28  bebbo
 * @INIT
 * Revision 1.8 2000/06/22 16:10:01 bebbo
 * 
 * @I removed unused variables
 * 
 * Revision 1.7 2000/06/19 10:34:27 bebbo
 * 
 * @N adapted to changes in Pem: search .???.b64 too
 * 
 * Revision 1.6 2000/06/18 17:06:40 bebbo
 * 
 * @R changes caused, by splitting Pkcs into separate files
 * 
 * Revision 1.5 2000/06/18 16:21:17 bebbo
 * 
 * @I modified, caused by file format changes
 * 
 * Revision 1.4 2000/05/20 19:47:50 bebbo
 * 
 * @R Asn1.getSeq changed. All related changes are done. Read docu!
 * 
 * Revision 1.3 1999/11/03 11:43:44 bebbo
 * 
 * @R now creating encrypted private key files
 */