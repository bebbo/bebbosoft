package de.bb.bejy.j2ee;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class LimitedInputStream extends InputStream {

    private ServletInputStream is;
    private int limit;

    public LimitedInputStream(ServletInputStream inputStream, int contentLength) {
        this.is = inputStream;
        this.limit = contentLength;
    }

    
    public int read() throws IOException {
        if (limit > 0) {
            int x = is.read();
            if (x >= 0)
                --limit;
            return x;
        }
        return -1;
    }

    
    public int read(byte[] b) throws IOException {
        int length = b.length < limit ? b.length : limit;
        int read = super.read(b, 0, length);
        limit -= read;
        return read;
    }

    
    public int read(byte[] b, int off, int len) throws IOException {
        int length = len < limit ? len : limit;
        int read = super.read(b, 0, length);
        limit -= read;
        return read;
    }

    
    public int available() throws IOException {
        int av = super.available();
        if (av < limit)
            return av;
        return limit;
    }

}
