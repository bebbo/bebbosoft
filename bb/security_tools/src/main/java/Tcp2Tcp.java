import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import de.bb.security.Asn1;
import de.bb.security.Pkcs6;
import de.bb.security.Ssl3Client;
import de.bb.swing.Manager;
import de.bb.util.Misc;
import de.bb.util.XmlFile;

public class Tcp2Tcp {
    private XmlFile xmlFile;
    private String localPort;
    private String destination;

    /**
     * check the configuration and start the server if everything is ok.
     */
    private void checkConfig() {
        for (;;) {
            xmlFile = new XmlFile();
            xmlFile.readFile("tcp2tcp.xml");

            localPort = xmlFile.getString("/t2t", "localPort", null);
            destination = xmlFile.getString("/t2t", "destination", null);

            if (localPort == null || destination == null) {
                if (!showConfig())
                    return;
            }
            if (!startServer()) {
                if (!showConfig())
                    return;
            }
        }
    }

    /**
     * try to start the server.
     * 
     * @return true if the server started successfully
     */
    private boolean startServer() {
        try {
            tryStartServer();
            return true;
        } catch (Exception e) {
            showError(e.toString() + "\r\n" + e.getMessage());
        }
        return false;
    }

    private static void showError(String message) {
        Tcp2TcpApp app = Manager.loadApplication(Tcp2TcpApp.class);
        app.setVisible(true);
        app.showError(message);
        app.setVisible(false);
    }

    private void tryStartServer() throws Exception {
        startStreams(null);
        final ServerSocket ssock = new ServerSocket(Integer.parseInt(localPort));
        try {
            for (;;) {
                Socket sin = ssock.accept();
                startStreams(sin);
            }
        } finally {
            ssock.close();
        }
    }

    private void startStreams(Socket sin) throws Exception {
        Socket sout = connect();
        sout.setSoTimeout(1000 * 60 * 60);
        InputStream is2 = sout.getInputStream();
        OutputStream os2 = sout.getOutputStream();

        Ssl3Client sslc = new Ssl3Client();
        sslc.connect(is2, os2, null);

        Vector<byte[]> certs = sslc.getCertificates();
        byte[] cert = certs.get(0);
        String certificate = Misc.bytes2Hex(cert);
        String lastCert = xmlFile.getContent("/t2t");
        if (lastCert == null)
            lastCert = "";
        if (!lastCert.trim().equals(certificate)) {
            if (!acceptCertificate(certs))
                throw new Exception("certificate declined");
        }

        if (sin == null) {
            sout.close();
            return;
        }

        sin.setSoTimeout(1000 * 60 * 60);
        InputStream is1 = sin.getInputStream();
        OutputStream os1 = sin.getOutputStream();

        is2 = sslc.getInputStream();
        os2 = sslc.getOutputStream();

        IOLogPipe t1 = new IOLogPipe("in", is1, os2, null);
        IOLogPipe t2 = new IOLogPipe("out", is2, os1, null);
        t1.setOther(t2);
        t2.setOther(t1);
        t2.start();
        t1.start();
    }

    private boolean acceptCertificate(Vector<byte[]> certs) throws Exception {
        byte[] signature = Pkcs6.getCertificateSignature(certs);
        if (signature == null)
            throw new Exception("no or invalid certificate");

        final byte[] cert = certs.get(0);
        
        byte[] owner = Pkcs6.getCertificateOwner(cert);
        byte[] issuer = Pkcs6.getCertificateIssuer(cert);

        Tcp2TcpApp app = Manager.loadApplication(Tcp2TcpApp.class);
        app.setVisible(true);
        HashMap<String, String> dd = new HashMap<String, String>();
        dd.put("signature", Misc.bytes2Hex(signature));
        dd.put("owner", Asn1.dump(0, owner, 0, owner.length, false).toString());
        dd.put("issuer", Asn1.dump(0, issuer, 0, issuer.length, false).toString());

        boolean r = app.acceptCertificate(dd);
        app.setVisible(false);

        if (!r)
            return false;

        final String certificate = Misc.bytes2Hex(cert);
        xmlFile.setContent("/t2t/", certificate);
        writeConfig();
        return true;
    }

    private Socket connect() throws Exception {
        final int colon = destination.indexOf(":");
        if (colon < 0)
            throw new Exception("malformed destination (missing :) " + destination);
        String ip = destination.substring(0, colon);
        int port = Integer.parseInt(destination.substring(colon + 1));
        final Socket s = new Socket(ip, port);
        return s;
    }

    /**
     * show the configuration dialog.
     */
    private boolean showConfig() {
        Tcp2TcpApp app = Manager.loadApplication(Tcp2TcpApp.class);
        for (;;) {
            app.setVisible(true);
            HashMap<String, String> dd = new HashMap<String, String>();
            dd.put("localPort", localPort);
            dd.put("destination", destination);
            dd = app.showConfig(dd);
            app.setVisible(false);
            if (dd == null)
                return false;

            localPort = dd.get("localPort");
            destination = dd.get("destination");
            xmlFile.setString("/t2t", "localPort", localPort);
            xmlFile.setString("/t2t", "destination", destination);

            writeConfig();
            return true;
        }
    }

    private void writeConfig() {
        try {
            FileOutputStream fos = new FileOutputStream("tcp2tcp.xml");
            xmlFile.write(fos);
        } catch (IOException ioe) {
            showError(ioe.getMessage());
        }
    }

    private static void showHelp() {
        System.out.println("USAGE: Tcp2Tcp [-c]\r\n-c opens the configuration dialog\r\n");
    }

    /**
     * Simple tunnel program.
     * 
     * @param args
     */
    public static void main(String[] args) {
        final Tcp2Tcp t2t = new Tcp2Tcp();
        if (args.length == 1) {
            if (!"-c".equals(args[0])) {
                showHelp();
            } else {
                t2t.showConfig();
            }
        } else {
            t2t.checkConfig();
        }
        System.exit(0);
    }

}
