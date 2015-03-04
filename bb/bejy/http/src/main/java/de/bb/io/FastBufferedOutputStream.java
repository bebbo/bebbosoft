package de.bb.io;

import java.io.IOException;
import java.io.OutputStream;

public class FastBufferedOutputStream extends OutputStream {

  private final int bufferSize;

  private byte[] buffer;

  private int pos;

  private OutputStream os;

  public FastBufferedOutputStream(OutputStream os, int bufferSize)
  {
    this.os = os;
    this.bufferSize = bufferSize;
    buffer = new byte[bufferSize];
  }

  public void write(int b) throws IOException {
    if (pos == bufferSize)
      flush();
    buffer[pos++] = (byte) b;
  }

  public void flush() throws IOException {
    if (pos > 0) {
      os.write(buffer, 0, pos);
      os.flush();
      pos = 0;
    }
  }

  public void close() throws IOException {
    flush();
    os.close();
  }

  public void write(byte[] b, int off, int len) throws IOException {
    while (pos + len > bufferSize)
    {
      if (pos == bufferSize)
        flush();
      int room = bufferSize - pos;
      if (room > len)
        room = len;
      System.arraycopy(b, off, buffer, pos, room);
      pos += room;
      off += room;
      len -= room;
    }
    if (len == 0)
      return;
    System.arraycopy(b, off, buffer, pos, len);
    pos += len;
  }

  public String toString() {
      return new String(buffer, 0, 0, pos);
  }
}
