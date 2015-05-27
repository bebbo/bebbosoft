/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/CreateCsr.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/12/19 12:26:15 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * written by Stefan Bebbo Franke
 * All rights reserved.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Vector;

import de.bb.security.Pem;
import de.bb.security.Pkcs1;
import de.bb.security.Pkcs6;
import de.bb.security.SecureRandom;
import de.bb.util.Mime;

/**
 * 1. Generate a key pair -> n,e,f,p,q 2. Save it encrypted to a file.
 */
public class Verify {
    static Random sr = SecureRandom.getInstance();

    public Verify() {
    }

    static public void main(String args[]) {
        writeln("Verify $Revision: 1.1 $");
        if (args.length != 1) {
            writeln("usage: Verify <filename>");
            return;
        }
        try {

            FileInputStream fi;
            String kfn1 = args[0];
            // read the csr
            try {
                fi = new FileInputStream(kfn1 + ".b64");
            } catch (FileNotFoundException fnf) {
                fi = new FileInputStream(kfn1);
            }
            byte[] b = new byte[fi.available()];
            fi.read(b);
            fi.close();
            // decrypt it
            // b = Pkcs.pb2decrypt(b, pwd);
            byte[] cert = Pem.decode(b, 0, null);
            if (cert == null) cert = b;

            Vector<byte[]> v = new Vector<byte[]>();
            v.add(cert);
            byte[] sig = Pkcs6.getCertificateSignature(v);
            
            if (sig == null)
                System.out.println("wrong signature");
            else
                System.out.println("signature is OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void write(String s) {
        System.out.print(s);
    }

    static void writeln(String s) {
        System.out.println(s);
    }
}

/*
 * $Log: CreateCsr.java,v $
 * Revision 1.1  2012/12/19 12:26:15  bebbo
 * @N create a CSR for an existing key
 *
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