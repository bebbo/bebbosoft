package de.bb.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.bb.security.Ssl3Client;

public class HttpURLConnection extends java.net.HttpURLConnection {
    private Ssl3Client sslc;
    private Map<String, String> reqProps;
    private Map<String, String> respProps;
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private Map<String, List<String>> responseHeaders;

    public HttpURLConnection(URL url) {
        super(url);
        reuse();
    }

    public void connect() throws IOException {
        do {
            open();
            receive();
            if (responseCode == 302 && getInstanceFollowRedirects()) {
                socket.close();
                url = new URL(respProps.get("LOCATION"));
                if (!"GET".equals(method) && getDoOutput())
                    throw new IOException((new StringBuilder("can't redirect to ")).append(url).append(" using ")
                            .append(method).toString());
                socket = null;
                is = null;
                os = null;
            } else {
                return;
            }
        } while (true);
    }

    private void receive() throws IOException {
        boolean useSsl = "https".equals(url.getProtocol());
        if (useSsl)
            is = sslc.getInputStream();
        else
            is = socket.getInputStream();
        // is = new BufferedInputStream(is, 0xc000);
        String line = readLine();
        if (line != null)
            try {
                if (!line.startsWith("HTTP"))
                    throw new Exception();
                int space = line.indexOf(' ');
                line = line.substring(space + 1);
                responseCode = Integer.parseInt(line.substring(0, 3));
                responseMessage = line.substring(4);
            } catch (Throwable t) {
                throw new ProtocolException("erraneous server reply");
            }
        readHeader();
    }

    void readHeader() throws IOException {
        String line;
        while ((line = readLine()) != null) {
            int dot = line.indexOf(':');
            String key = line.substring(0, dot).toUpperCase();
            line = line.substring(dot + 1).trim();
            respProps.put(key, line);

            List<String> list = responseHeaders.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                responseHeaders.put(key, list);
            }
            list.add(line);
        }
    }

    private void open() throws IOException {
        responseCode = -1;
        responseMessage = null;
        boolean useSsl = "https".equals(url.getProtocol());
        int port = url.getPort();
        if (port < 0)
            port = useSsl ? 443 : 80;
        socket = new Socket(url.getHost(), port);
        socket.setSoTimeout(2 * 60 * 60 * 1000);
        if (useSsl) {
            if (sslc == null)
                sslc = new Ssl3Client();
            try {
                sslc.connect(socket.getInputStream(), socket.getOutputStream(), url.getHost());
            } catch (Exception ex) {
                // workaround for the old glassfish
                socket = new Socket(url.getHost(), port);
                socket.setSoTimeout(30 * 60 * 1000);
                sslc = new Ssl3Client();
                sslc.setMaxVersion(1); // restrict to TLS_1.0
                sslc.connect(socket.getInputStream(), socket.getOutputStream(), url.getHost());
            }
            os = sslc.getOutputStream();
        } else {
            os = socket.getOutputStream();
        }
        if (getDoOutput())
            setRequestMethod("POST");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeBytes(method);
        dos.write(32);
        dos.writeBytes(url.getPath());
        String q = url.getQuery();
        if (q != null) {
            dos.write(63);
            dos.writeBytes(q);
        }
        dos.writeBytes(" HTTP/1.1\r\nhost: ");
        dos.writeBytes(url.getHost());
        if ((useSsl && port != 443) || (!useSsl && port != 80))
            dos.writeBytes(":" + port);
        dos.writeBytes("\r\nConnection: Keep-alive\r\n");
        for (Entry<String, String> e : reqProps.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            dos.writeBytes(key);
            dos.write(58);
            dos.write(32);
            dos.writeBytes(val);
            dos.write(13);
            dos.write(10);
        }

        dos.write(13);
        dos.write(10);
        dos.flush();

        os.write(bos.toByteArray());
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int ch = is.read(); ch >= 0; ch = is.read()) {
            if (ch == 13) {
                ch = is.read();
                if (ch == 10)
                    break;
                bos.write(13);
            }
            bos.write(ch);
        }

        if (bos.size() == 0)
            return null;

        return bos.toString();
    }

    public void disconnect() {
        // System.out.println("disconnect");
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
            is = null;
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
            }
            os = null;
        }
    }

    public boolean usingProxy() {
        return false;
    }

    public InputStream getInputStream() throws IOException {
        if (socket == null)
            connect();
        else if (is == null)
            receive();
        List<String> te = responseHeaders.get("TRANSFER-ENCODING");
        if (te != null) {
            for (String ste : te) {
                if (ste.equalsIgnoreCase("chunked"))
                    return new CIS(this, is);
            }
        }
        return is;
    }

    public OutputStream getOutputStream() throws IOException {
        if (socket == null)
            open();
        return os;
    }

    public void addRequestProperty(String key, String value) {
        // super.addRequestProperty(key, value);
        reqProps.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public Map getRequestProperties() {
        return Collections.unmodifiableMap(reqProps);
    }

    public String getRequestProperty(String key) {
        return reqProps.get(key);
    }

    public void setRequestProperty(String key, String value) {
        // super.setRequestProperty(key, value);
        reqProps.put(key, value);
    }

    public String getHeaderField(String name) {
        return respProps.get(name.toUpperCase());
    }

    public Map<String, List<String>> getHeaderFields() {
        return responseHeaders;
    }

    protected void finalize() {
        disconnect();
    }

    public void reuse() {
        List<String> connection = responseHeaders != null ? responseHeaders.get("CONNECTION") : null;
        boolean reuse =
                is != null && os != null && connection != null && connection.size() == 1
                        && connection.get(1).equalsIgnoreCase("KEEP-ALIVE");

        doInput = false;
        doOutput = false;
        reqProps = new HashMap<String, String>();
        respProps = new HashMap<String, String>();
        responseHeaders = new HashMap<String, List<String>>();
        if (!reuse) {
            socket = null;
            is = null;
            os = null;
        }
    }

    void endChunked(int total) {
        ArrayList<String> sl = new ArrayList<String>();
        sl.add(Integer.toString(total));
        responseHeaders.put("CONTENT-LENGTH", sl);
        responseHeaders.remove("TRANSFER-ENCODING");
    }
}
