package de.bb.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Connect {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            byte[][] cs = { Ssl3.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, Ssl3.TLS_DHE_RSA_WITH_AES_256_CBC_SHA };
            Ssl3Client client = new Ssl3Client();
            //client.setMaxVersion(2);
            connect(client);
            connect(client);

        } catch (Exception e) {
            System.out.flush();
            e.printStackTrace();
        }
    }

    private static void connect(Ssl3Client client) throws IOException {
        int port = 443;
        String server;
        server = "www.google.de";
        //        server = "www.ibm.com";
        //       server = "login.live.com";
        //      server = "localhost";
                server = "www.mikestoolbox.org";
        //        server = "serveronline.org";
                server = "blog.fefe.de";
    //     server = "ikanobank.de";
        //        port = 993;

//        port = 4433;
        
        Socket s = new Socket(server, port);
        try {

            client.connect(s.getInputStream(), s.getOutputStream(), server);

            System.out.println(client.getCipherSuite() + " - " + client.getVersion());
            
            OutputStream os = client.getOutputStream();
            InputStream is = client.getInputStream();

            os.write(("GET / HTTP/1.0\r\nhost: " + server + "\r\n\r\n").getBytes());
            byte[] data = new byte[1234];
            for (;;) {
                int ch;
                try {
                    ch = is.read();
                } catch (IOException ex) {
                    break;
                }
                if (ch < 0)
                    break;

                data[0] = (byte) ch;
                int read = is.read(data, 1, data.length - 1);
                System.out.print(new String(data, 0, 0, read));
            }
        } finally {
            s.close();
        }
    }
}
