/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
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
