package de.bb.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

import de.bb.util.ByteRef;

public class SmtpTls {

    private static ByteRef buffer = new ByteRef();;

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
//            byte[][] cs = { Ssl3.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, Ssl3.TLS_DHE_RSA_WITH_AES_256_CBC_SHA };
            byte[][] cs = { Ssl3.TLS_DHE_RSA_WITH_AES_256_CBC_SHA };
            Ssl3Client client = new Ssl3Client(cs);
            client.setMaxVersion(3);
            connect(client);

        } catch (Exception e) {
            System.out.flush();
            e.printStackTrace();
        }
    }

    private static void connect(Ssl3Client client) throws IOException {
        int port = 25;
        String server;
        server = "mx17d.antispameurope.com";
//        server = "aspmx.l.google.com";
//        server = "mta5.am0.yahoodns.net";
        server = "serveronline.org";

        Socket s = new Socket(server, port);
        try {

            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();

            ByteRef line;
            do {
                line = ByteRef.readLine(buffer, is);
                System.out.println(line);
            } while (line.charAt(3) != ' ');

            os.write("EHLO mail-client-1\r\n".getBytes());

            boolean startTls = false;
            do {
                line = ByteRef.readLine(buffer, is);
                System.out.println(line);
                
                startTls |= line.substring(4).equals("STARTTLS");
            } while (line.charAt(3) != ' ');

            if (startTls) {
                os.write("STARTTLS\r\n".getBytes());
                do {
                    line = ByteRef.readLine(buffer, is);
                    System.out.println(line);
                } while (line.charAt(3) != ' ');
                
                client.connect(is, os, server);
                System.out.println(client.getCipherSuite());
                
                Vector<byte[]> certificates = client.getCertificates();
                byte[] root = certificates.get(certificates.size() - 1);
                System.out.println(Asn1.dump(0, root, 0, root.length, false));

                
                byte[] issuer = Pkcs6.getCertificateIssuer(root);
                System.out.println(Asn1.dump(0, issuer, 0, issuer.length, false));

                byte[] sig = Pkcs6.getCertificateSignature(certificates);
                if (sig != null) {
                    // TODO: check trust of root cert
                }
                
                
                byte[] ownerBlock = Pkcs6.getCertificateOwner(certificates.get(0));
                System.out.println(Asn1.dump(0, ownerBlock, 0, ownerBlock.length, false));

                byte[] domainOid = Asn1.makeASN1(Asn1.string2Oid("2.5.4.3"), Asn1.OBJECT_IDENTIFIER);
                int domainSequenceOffset = Asn1.searchSequence(ownerBlock, domainOid);
                
                int stringOffset = domainSequenceOffset + Asn1.getHeaderLen(ownerBlock, domainSequenceOffset) + domainOid.length;
                
                String domain = new String(Asn1.getData(ownerBlock, stringOffset));
                System.out.println(domain);
                
                // TODO match server <-> domain
                
                is = client.getInputStream();
                os = client.getOutputStream();
            }
            
            
            os.write("QuIt\r\n".getBytes());
            do {
                line = ByteRef.readLine(buffer, is);
                System.out.println(line);
            } while (line.charAt(3) != ' ');

            
            //
            //            os.write(("GET / HTTP/1.0\r\nhost: " + server + "\r\n\r\n").getBytes());
            //            byte[] data = new byte[1234];
            //            for (;;) {
            //                int ch;
            //                try {
            //                    ch = is.read();
            //                } catch (IOException ex) {
            //                    break;
            //                }
            //                if (ch < 0)
            //                    break;
            //
            //                data[0] = (byte) ch;
            //                int read = is.read(data, 1, data.length - 1);
            //                System.out.print(new String(data, 0, 0, read));
            //            }
        } finally {
            s.close();
        }
    }
}
