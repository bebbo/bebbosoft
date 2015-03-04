package de.bb.io;

import java.io.IOException;
import java.io.InputStream;

public class FastBufferedInputStream extends InputStream {

  private InputStream is;

  private int pos;

  private int end;

  private byte[] buffer;

  public FastBufferedInputStream(InputStream is, int bufferSize)
  {
    this.is = is;
    bufferSize = bufferSize > 0 ? bufferSize : 4096;
    buffer = new byte[bufferSize];
  }

  public int available() throws IOException {
    if (pos == end)
      fill();
    return end - pos;
  }

  private void fill() throws IOException {
    int b0 = is.read();
    if (b0 < 0)
      return;

    buffer[0] = (byte) b0;
    end = 1 + is.read(buffer, 1, buffer.length - 1);
    pos = 0;
  }

  public void close() throws IOException {
    is.close();
    pos = end;
  }

  public int read() throws IOException {
    if (pos == end)
      fill();
    if (pos == end)
      return -1;
    return 0xff & buffer[pos++];
  }

  public int read(byte[] b, int off, int len) throws IOException {
    if (pos == end)
      fill();
    int avail = end - pos;
    if (len > avail)
      len = avail;
    System.arraycopy(buffer, pos, b, off, len);
    pos += len;
    return len;
  }

  public void reassign(InputStream is) {
    pos = end = 0;
    this.is = is;
  }

  
}
