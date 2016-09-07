/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/Certify.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/19 15:27:33 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Written by Stefan Bebbo Franke
 * All rights reserved.
 * create an certificate
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;

import de.bb.security.Asn1;
import de.bb.security.Pem;
import de.bb.security.Pkcs6;
import de.bb.util.Mime;

public class Certify {

    static int NS_SSL_CLIENT = 0x80;
    static int NS_SSL_SERVER = 0x40;
    static int NS_SMIME = 0x20;
    static int NS_OBJSIGN = 0x10;
    static int NS_SSL_CA = 0x04;
    static int NS_SMIME_CA = 0x02;
    static int NS_OBJSIGN_CA = 0x01;

    public Certify() {

    }

    static String m2s(int n) {
        return "" + (n / 10) % 10 + n % 10;
    }

    static public void main(String args[]) {
        writeln("Certify $Revision: 1.2 $");
        String p[] = new String[2];
        if (args.length < 2 || args.length > 6) {
            writeln("usage: certify [-ns <certType>] <request> <issuer> [<startdate>=today] [<stopdate>=today + 1 year]");
            writeln("certTypes: SSL_CLIENT = 80, SSL_SERVER = 40, SMIME    = 20, OBJSIGN    = 10");
            writeln("                            SSL_CA     = 04, SMIME_CA = 02, OBJSIGN_CA = 01");
            writeln("e.g. for SSL_CLIENT and SMIME use A0");
            /*
             * p[0] = "bebbo.req"; p[1] = "bebbo.req"; args = p;
             */
        }
        int argi = 0;
        int nsCertType = 0; // NS_SMIME; // default
        if (args[0].equalsIgnoreCase("-ns")) {
            nsCertType = Integer.parseInt(args[1], 16);
            argi = 2;
        }

        p[0] = args[argi + 0] + ".req";
        p[1] = args[argi + 1] + ".ini";
        String kfn1 = args[argi + 1] + ".key.b64";
        String fromDate, toDate;
        if (args.length >= argi + 3)
            fromDate = args[argi + 2];
        else {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            fromDate =
                    m2s(cal.get(Calendar.YEAR)) + m2s(cal.get(Calendar.MONTH) + 1)
                            + m2s(cal.get(Calendar.DAY_OF_MONTH)) + m2s(cal.get(Calendar.HOUR))
                            + m2s(cal.get(Calendar.MINUTE)) + "Z";
        }
        if (args.length >= argi + 4)
            toDate = args[argi + 3];
        else {
            toDate = m2s(11 + Integer.parseInt(fromDate.substring(0, 2))) + fromDate.substring(2);
        }

        try {
            // read the certificate request
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(p[0] + ".b64");
            } catch (FileNotFoundException fnf) {
                fi = new FileInputStream(p[0]);
            }
            byte b[] = new byte[fi.available()];
            fi.read(b);

            byte bb[] = Pem.decode(b, 0, null);
            if (bb != null)
                b = bb;

            // get the owner from request
            int pto[] = {0x90, 0x90, 0x90};
            byte[] owner = Asn1.getSeq(b, pto, 0);

            owner = Asn1.makeASN1(owner, 0x30);

            // get the public key
            int ptk[] = {0x90, 0x90, 0x10, 0x90, 0x83};
            int pan[] = {0x90, 0x82};
            int pae[] = {0x90, 2, 0x82};
            bb = Asn1.getSeq(b, ptk, 0); // the bit string

            byte kn[] = Asn1.getSeq(bb, pan, bb[0] == 0 ? 1 : 0);
            byte ke[] = Asn1.getSeq(bb, pae, bb[0] == 0 ? 1 : 0);

            // create the issuer
            byte issuer[] = Pkcs6.makeInfo(p[1]);

            // calc the date
            byte date[] = Pkcs6.createDate(fromDate.getBytes(), toDate.getBytes());

            // create the certificate
            byte cert[] = Pkcs6.createCertificate(issuer, date, owner, kn, ke);

            // add Netscape certificate type, last byte contains properties
            if (nsCertType != 0) {
                writeln("setting certificate type to: " + Integer.toHexString(nsCertType));
                byte nsc[] =
                        {(byte) 0xa3, 0x18, 0x30, 0x16, 0x30, 0x14, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01,
                                (byte) 0x86, (byte) 0xf8, 0x42, 0x01, 0x01, 0x01, 0x01, (byte) 0xff, 0x04, 0x04, 0x03,
                                0x02, 0x00, (byte) nsCertType};
                cert = Asn1.addTo(cert, nsc);
                
                if ((nsCertType & 0x40) == 0x40) {
                	byte[] t = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(Asn1.string2Oid("2.5.29.37"), Asn1.OBJECT_IDENTIFIER));
                	byte[] s = Asn1.newSeq;
                	s = Asn1.addTo(s, Asn1.makeASN1(Asn1.string2Oid("1.3.6.1.5.5.7.3.1"), Asn1.OBJECT_IDENTIFIER));
                	t = Asn1.addTo(t, Asn1.makeASN1(s, Asn1.OCTET_STRING));
                	cert = Asn1.addTo(cert, t);
                }
            }

            // read password
            write("password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            // String pwd = "test";
            String pwd = br.readLine();

            // get the signers private key
            try {
                fi = new FileInputStream(kfn1 + ".b64");
            } catch (FileNotFoundException fnf) {
                fi = new FileInputStream(kfn1);
            }
            b = new byte[fi.available()];
            fi.read(b);
            // decrypt it
            // b = Pkcs.pb2decrypt(b, pwd);
            b = Pem.decode(b, 0, pwd);
            // check pwd
            if (b == null)
                throw new Exception("wrong password");

            // get values
            // int pad[] = {0x90, 0x84};
            int pan2[] = {0x90, 2, 0x82};
            int pae2[] = {0x90, 2, 2, 0x82};
            int paf[] = {0x90, 2, 2, 2, 0x82};

            // b = Asn1.getSeq(b, pad, 0);
            kn = Asn1.getSeq(b, pan2, 0);
            ke = Asn1.getSeq(b, pae2, 0);
            byte kf[] = Asn1.getSeq(b, paf, 0);

            // sign and write
            cert = Pkcs6.sign(cert, kn, kf);

            FileOutputStream fw = new FileOutputStream(args[argi + 0] + ".cer.b64");
            fw.write(Mime.createHeader(args[argi + 0] + ".cer"));
            cert = Pem.encode(cert, "CERTIFICATE", 0, null, null);

            fw.write(cert);
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
}

/*
 * $Log: Certify.java,v $
 * Revision 1.2  2012/08/19 15:27:33  bebbo
 * @R default validity of certificates is now 11 years.
 *
 * Revision 1.1  2012/08/11 19:56:45  bebbo
 * @I working stage
 *
 * Revision 1.1  2010/12/17 17:20:12  bebbo
 * @INIT
 * Revision 1.10 2000/07/18 18:07:48 bebbo
 * 
 * @B now uses compliant certificate request
 * 
 * Revision 1.9 2000/06/29 17:19:28 bebbo
 * 
 * @N added parameters for ns certificate type
 * 
 * Revision 1.8 2000/06/22 16:10:24 bebbo
 * 
 * @B now using timezone GMT
 * 
 * Revision 1.7 2000/06/19 10:34:27 bebbo
 * 
 * @N adapted to changes in Pem: search .???.b64 too
 * 
 * Revision 1.6 2000/06/18 17:06:40 bebbo
 * 
 * @R changes caused, by splitting Pkcs into separate files
 * 
 * Revision 1.5 2000/06/18 16:21:25 bebbo
 * 
 * @I modified, caused by file format changes
 * 
 * Revision 1.4 2000/05/20 19:47:50 bebbo
 * 
 * @R Asn1.getSeq changed. All related changes are done. Read docu!
 * 
 * Revision 1.3 1999/11/03 11:43:47 bebbo
 * 
 * @R now using encrypted private key files
 */