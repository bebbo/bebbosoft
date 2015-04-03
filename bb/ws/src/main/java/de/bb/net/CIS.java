package de.bb.net;

import java.io.IOException;
import java.io.InputStream;

public class CIS extends InputStream {

    private InputStream is;
    private int size;
    private int pos;
    private byte buffer[];
    private HttpURLConnection con;
    private int total;
    private boolean eos;

    public CIS(HttpURLConnection con, InputStream is) {
        this.con = con;
        this.is = is;
    }

    @Override
    public int read() throws IOException {
        if (pos == size)
            update();
        if (eos)
            return -1;
        return buffer[pos++] & 0xff;
    }

    @Override
    public int available() throws IOException {
        if (pos == size)
            update();
        if (eos)
            return 0;
        return size - pos;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (pos == size)
            update();
        if (eos)
            return -1;
        if (len > size - pos)
            len = size - pos;
        System.arraycopy(buffer, pos, b, off, len);
        pos += len;
        return len;
    }

    private void update() throws IOException {
        int chunk = 0;
        for(;;) {
            int n = is.read();
            if (n < 0)
                throw new IOException("EOF");
            if (n == 0xd)
                break;
            if (n >= 'a' && n <='f')
                chunk = chunk * 16 + n - 'a' + 10;
            else
            if (n >= 'A' && n <='F')
                chunk = chunk * 16 + n - 'A' + 10;
            else
                chunk = chunk * 16 + n - '0';
        }
        is.read();
        
        if (chunk == 0) {
            con.readHeader();
            con.endChunked(total);
            eos = true;
            return;
        }
        
        total += chunk;
        
        if (chunk > size) {
            buffer = new byte[chunk];
        }
        size = chunk;
        pos = 0;
        for (int pos = 0; pos < size;) {
            int l = is.read(buffer, pos, size - pos);
            pos += l;
        }
        
        is.read();
        is.read();
    }

}
