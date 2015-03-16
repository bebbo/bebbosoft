/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/Tcp2TcpTrace.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/11/13 06:32:35 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) by Netlife Internet Consulting and Software GmbH 1999
 * all rights reserved
 *
 * listens TCP - writes TCP and Logs
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import de.bb.security.Asn1;
import de.bb.security.Pem;
import de.bb.security.Pkcs1;
import de.bb.security.Ssl3Client;
import de.bb.security.Ssl3Server;
import de.bb.util.ByteRef;

public class Tcp2TcpTrace {

    private static byte[][] certs;

    private static byte[][] pkData;

    private static boolean useHttps;

    public Tcp2TcpTrace() {
    }

    static public void main(String args[]) {
        writeln("Tcp2TcpTrace $Revision: 1.2 $");
        int myPort;
        String ip;
        int otherPort;
        String log = null;

        if (args.length >= 1) {
            if ("https".equals(args[0])) {
                useHttps = true;
                String arg2[] = new String[args.length - 1];
                System.arraycopy(args, 1, arg2, 0, arg2.length);
                args = arg2;
            }
        }
        if (args.length < 3) {
            System.out
                    .println("USAGE Tcp2TcpTrace [https] <localport> <destip> <destport> [<logfile> [<certfile> <password]]\r\n");
            System.out.println("    if https is specifed SSL is used to connect to remote server");
            System.out.println("    if logfile is specifed hex dump is written to the log");
            System.out.println("    if certfile and password is specifed the tunnel listens with SSL");
            System.out.println("");
            System.out.println("    e.g. this command tunnels local http to remote https");
            System.out.println("    Tcp2TcpTrace https 1444 188.40.175.103 443");
        }

        myPort = Integer.parseInt(args[0]);
        ip = args[1];
        otherPort = Integer.parseInt(args[2]);

        if (args.length >= 4)
            log = args[3];

        try {
            boolean useSSL = false;
            if (args.length >= 6) {
                useSSL = true;
                loadStuff(args[4], args[5]);
            }

            writeln("running on port " + myPort + ", connecting to " + ip + ":" + otherPort);
            writeln("logging to: " + log);

            PrintWriter ps = null;
            if (log != null)
                ps = new PrintWriter(new FileOutputStream(log));
            ServerSocket ssock = new ServerSocket(myPort);
            for (;;) {
                try {
                    Socket sin = ssock.accept();
                    sin.setSoTimeout(1000 * 60 * 60);
                    writeln("new connect from " + sin);

                    InputStream is1 = sin.getInputStream();
                    OutputStream os1 = sin.getOutputStream();

                    Socket sout = new Socket(ip, otherPort);
                    sout.setSoTimeout(1000 * 60 * 60);
                    InputStream is2 = sout.getInputStream();
                    OutputStream os2 = sout.getOutputStream();

                    if (useSSL) {

                        Ssl3Server ssl3 = new Ssl3Server(certs, pkData);
                        writeln("ssl server handshake");
                        ssl3.listen(is1, os1);
                        writeln("ciphertype = " + ssl3.getCipherType());

                        is1 = ssl3.getInputStream();
                        os1 = ssl3.getOutputStream();
                    }
                    if (useHttps) {
                        writeln("ssl client connect");
                        Ssl3Client sslc = new Ssl3Client();
                        sslc.connect(is2, os2, null);
                        writeln("ciphertype = " + sslc.getCipherType());
                        is2 = sslc.getInputStream();
                        os2 = sslc.getOutputStream();
                    }

                    IOLogPipe t1 = new IOLogPipe("out", is1, os2, ps);
                    IOLogPipe t2 = new IOLogPipe("in", is2, os1, ps);
                    t1.setOther(t2);
                    t2.setOther(t1);
                    t2.start();
                    t1.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    writeln(e.toString());
                }
            }
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

    static void writeln() {
        System.out.println("");
    }

    public static void loadStuff(String fileBase, String password) throws Exception {
        System.out.println("activating SSL config");
        // load certificate
        byte[] certContent = null;
        FileInputStream fis = null;
        String certFile = fileBase + ".cer.b64";
        if (new File(certFile).exists())
            try {
                fis = new FileInputStream(certFile);
                certContent = new byte[fis.available()];
                fis.read(certContent);
            } finally {
                if (fis != null)
                    fis.close();
            }

        byte[] dd = null;
        if (certContent != null)
            dd = Pem.decode(certContent, 0, null);
        if (dd != null)
            certContent = dd;

        if (certContent == null)
            System.out.println("could not load certificate file: " + certFile);

        // split the certs file into an array of certificates.
        ArrayList certList = new ArrayList();
        ByteRef br = new ByteRef(certContent);
        while (br.length() > 0) {
            int len = Asn1.getLen(br.toByteArray());
            if (len > br.length()) {
                System.out.println("certificate lengths mismatch in: " + certFile);
                break;
            }
            certList.add(br.substring(0, len).toByteArray());
            br = br.substring(len);
        }
        certs = new byte[certList.size()][];
        certList.toArray(certs);

        // get the signers private key
        String keyFile = fileBase + ".key.b64";
        byte b[] = null;
        fis = null;
        if (new File(keyFile).exists())
            try {
                fis = new FileInputStream(keyFile);
                b = new byte[fis.available()];
                fis.read(b);
            } finally {
                if (fis != null)
                    fis.close();
            }

        // decrypt it
        if (b != null)
            dd = Pem.decode(b, 0, password);
        if (dd != null)
            b = dd;

        // check pwd
        if (b == null)
            System.out.println("could not load keyfile: " + keyFile);

        pkData = Pkcs1.getPrivateKey(b);

        /* verify the pkData */
        BigInteger p = new BigInteger(1, pkData[3]);
        BigInteger q = new BigInteger(1, pkData[4]);
        BigInteger e = new BigInteger(1, pkData[1]);
        BigInteger one = BigInteger.ONE;
        BigInteger p1 = p.subtract(one);
        BigInteger q1 = q.subtract(one);
        BigInteger n = p1.multiply(q1);
        BigInteger d = e.modInverse(n);
        n = p.multiply(q);
        BigInteger dp1 = d.mod(p1);
        if (!dp1.equals(new BigInteger(1, pkData[5]))) {
            System.out.println("bogus key file, fixing dp1");
            pkData[5] = dp1.toByteArray();
        }
        BigInteger dq1 = d.mod(q1);
        if (!dq1.equals(new BigInteger(1, pkData[6]))) {
            System.out.println("bogus key file, fixing dq1");
            pkData[6] = dq1.toByteArray();
        }
        BigInteger iqmp = q.modInverse(p);
        if (!iqmp.equals(new BigInteger(1, pkData[7]))) {
            System.out.println("bogus key file, fixing iqmp");
            pkData[7] = iqmp.toByteArray();
        }

    }

}

/*
 * $Log: Tcp2TcpTrace.java,v $
 * Revision 1.2  2012/11/13 06:32:35  bebbo
 * @R extended timeout
 *
 * Revision 1.1  2012/08/11 19:56:56  bebbo
 * @I working stage
 *
 * Revision 1.2  2010/12/17 23:24:51  bebbo
 * /FIXED: ssl config now supports multiple certificates
 * Revision 1.1 2010/12/17 17:20:47 bebbo
 * 
 * @INIT Revision 1.5 2000/06/29 17:18:04 bebbo
 * 
 * @B fixed usage of params
 * 
 * Revision 1.4 1999/12/22 16:05:23 hagen taken from bebbo
 * 
 * Revision 1.3 1999/10/25 15:09:24 bebbo
 * 
 * @R changed package assignments
 * 
 * Revision 1.2 1999/09/02 19:07:20 Bebbo
 * 
 * @N created.
 * 
 * Revision 1.1 1999/08/27 12:17:54 Bebbo fixed some deprecated warnings.
 */

