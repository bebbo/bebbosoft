package de.bb.bejy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOPipeThread extends Thread {

    private byte buffer[] = new byte[0x2000];

    private InputStream is;

    private OutputStream os;

    private IOPipeThread slave;

    private static int N = 0;

    public IOPipeThread(InputStream inputStream, OutputStream os) {
        this("IOPipeThread", inputStream, os);
    }

    public IOPipeThread(String name, InputStream inputStream, OutputStream os) {
        super(name + "-" + ++N);
        this.is = inputStream;
        this.os = os;
    }

    public void run() {
        try {
            for (;;) {
                int b0 = is.read();
                if (b0 < 0)
                    break;

                // read only what's available
                int avail = is.available();
                if (avail == 0) {
                    os.write(b0);
                    os.flush();
                } else {
                    buffer[0] = (byte) b0;
                    if (avail > buffer.length - 1)
                        avail = buffer.length - 1;
                    int len = 1 + is.read(buffer, 1, avail);
                    os.write(buffer, 0, len);
                    if (is.available() == 0)
                        os.flush();
                }
            }
        } catch (Exception ex) {
        } finally {
            if (slave != null)
                slave.close();
        }
    }

    private void close() {
        try {
            is.close();
        } catch (IOException e) {
        }
        try {
            os.close();
        } catch (IOException e) {
        }
        interrupt();
    }

    public void setSlave(IOPipeThread slave) {
        this.slave = slave;
    }
}
