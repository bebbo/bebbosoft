package de.bb.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void copy(InputStream is, OutputStream os, long length) throws IOException {
            byte b[] = new byte[8192];
            while (length > 0) {
                int b0 = is.read();
                if (b0 < 0)
                    throw new IOException("EOS");
                os.write(b0);
                --length;
                if (length == 0)
                    return;
                int read = length > b.length ? b.length : (int)length;
                read = is.read(b, 0, read);
                if (read > 0) {
                    os.write(b, 0, read);
                    length -= read;
                }
            }
        }

    public static int readFully(InputStream is, byte[] buffer, int length) throws IOException {
        int read = 0;
        while (read < length) {
            int b0 = is.read();
            if (b0 < 0)
                throw new IOException("EOS");
            buffer[read++] = (byte) b0;
            read += is.read(buffer, read, length - read);
        }
        return read;
    }
}
