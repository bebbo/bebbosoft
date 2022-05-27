import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class IOLogPipe extends Thread {

    private byte buffer[] = new byte[2048];

    private InputStream is;

    private OutputStream os;

    private PrintWriter ps;

    private String name;

    private IOLogPipe other;

    private static int N = 0;

    IOLogPipe(String name, InputStream inputStream, OutputStream os,
            PrintWriter ps) {
        super(name + ++N);
        this.name = name;
        this.is = inputStream;
        this.os = os;
        this.ps = ps; // new PrintWriter(System.out);
    }

    /**
     * Dump a byte array as hex values.
     * 
     * @param ps
     *            the used PrintWriter.
     * @param s
     *            a headline for each dumped block.
     * @param b
     *            the byte array to display
     * @param len
     *            the count of the dumped bytes.
     */
    public final static synchronized void dump(PrintWriter ps, String s,
            byte bb[], int len) {
        byte b[] = new byte[len];
        if (bb != null)
            System.arraycopy(bb, 0, b, 0, len);
        {
            ps.println(s + ": " + len + " bytes");
            ps.print("0000");
            for (int i = 0; i < len; ++i) {
                ps.print(" ");
                int x = (b[i] & 0xff) >> 4;
                if (x > 9)
                    ps.print("" + (char) (55 + x));
                else
                    ps.print("" + (char) (48 + x));

                x = (b[i] & 0xf);
                if (x > 9)
                    ps.print("" + (char) (55 + x));
                else
                    ps.print("" + (char) (48 + x));

                if (b[i] < 32)
                    b[i] = 32;

                if (((i + 1) & 15) == 0) {
                    ps.println(" " + new String(b, i - 15, 16));
                    ++i;
                    ps.print(Integer.toString((i >>> 12) & 0xf, 16));
                    ps.print(Integer.toString((i >>> 8) & 0xf, 16));
                    ps.print(Integer.toString((i >>> 4) & 0xf, 16));
                    ps.print(Integer.toString((i) & 0xf, 16));
                    --i;
                } else if (((i + 1) & 7) == 0)
                    ps.print(" ");
            }
            if ((len & 15) != 0) {
                for (int j = 0; ((j + len) & 15) != 0; ++j)
                    ps.print("   ");
                if ((len & 15) < 8)
                    ps.print(" ");
                ps.println(" " + new String(b, (len & ~15), len - (len & ~15)));
            }
            ps.println();
            ps.flush();
        }
    }

    /**
     * helper fx to dump to stdout
     * 
     * @param out
     *            - some outputStream
     * @param b
     *            - the bytes to dump
     */
    public static void dump(OutputStream out, byte b[]) {
        PrintWriter pw = new PrintWriter(out);
        dump(pw, "", b, b.length);
    }

    public void run() {
        try {
            for (;;) {
                int b0 = is.read();
                if (b0 < 0)
                    break;

                synchronized (this) {
                    buffer[0] = (byte) b0;
                    // read only what's available
                    int len = 1;
                    int avail = is.available();
                    if (avail > 0) {
                        if (avail > buffer.length - len)
                            avail = buffer.length - len;
                        len += is.read(buffer, len, avail);
                    }

                    if (ps != null)
                        dump(ps, name + " " + this, buffer, len);
                    os.write(buffer, 0, len);
                }

            }
        } catch (Exception ex) {
        } finally {
            other.close();
        }

    }

    private void close() {
        if (ps != null) {
            ps.println("closing " + name + " " + this);
            ps.flush();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e2) {
        }
        synchronized (this) {
            try {
                os.flush();
            } catch (IOException e1) {
            }
            try {
                is.close();
            } catch (IOException e) {
            }
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    public void setOther(IOLogPipe other) {
        this.other = other;
    }

}
