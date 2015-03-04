/******************************************************************************
 * A Helper class to piped typed data into OutputStreams (and vice versa).
 *  
 * Since java.nio this might look outdated, but there are still devices
 * where java.nio does not exist
 *
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

package de.bb.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A Helper class to piped typed data into OutputStreams (and vice versa maybe... TBD).
 *  
 * Since java.nio this might look outdated, but there are still devices
 * where java.nio does not exist
 * 
 * @author sfranke
 *
 */
public final class ByteUtil {
  /**
   * Write the bytes of the int i to the OutputStream in big endian order.
   * @param i an int value
   * @param os an OutputStream
   * @throws IOException on error
   */
  public final static void writeInt(int i, OutputStream os) throws IOException
  {
    byte b[] = new byte[11];
    int p = 0;
    if (i < 0)
    {
      b[p++] = '-';
      i = -i;
    }
    int i1 = i / 10;
    if (i1 > 0)
    {
      int i2 = i1 / 10;
      if (i2 > 0)
      {
        int i3 = i2 / 10;
        if (i3 > 0)
        {
          int i4 = i3 / 10;
          if (i4 > 0)
          {
            int i5 = i4 / 10;
            if (i5 > 0)
            {
              int i6 = i5 / 10;
              if (i6 > 0)
              {
                int i7 = i6 / 10;
                if (i7 > 0)
                {
                  int i8 = i7 / 10;
                  if (i8 > 0)
                  {
                    int i9 = i8 / 10;
                    if (i9 > 0)
                    {
                      b[p++] = (byte)('0' + i9 % 10);
                    }
                    b[p++] = (byte)('0' + i8 % 10);
                  }
                  b[p++] = (byte)('0' + i7 % 10);
                }
                b[p++] = (byte)('0' + i6 % 10);
              }
              b[p++] = (byte)('0' + i5 % 10);
            }
            b[p++] = (byte)('0' + i4 % 10);
          }
          b[p++] = (byte)('0' + i3 % 10);
        }
        b[p++] = (byte)('0' + i2 % 10);
      }
      b[p++] = (byte)('0' + i1 % 10);
    }
    b[p++] = (byte)('0' + i % 10);
    os.write(b, 0, p);
  }

  /**
   * Write the bytes of the long l to the OutputStream in big endian order.
   * @param l a long value
   * @param os an OutputStream
   * @throws IOException on error
   */
  public static void writeLong(long l, OutputStream os) throws IOException
  {
    if (l < 0)
    {
      os.write('-');
      l = -l;
    }
    int i = (int) (l % 1000000000);
    l /= 1000000000;
    if (l > 0)
    {
      long l1 = l / 1000000000;
      if (l1 > 0)
      {
        l %= 1000000000;
        writeInt((int) l1, os);
      }
      writeInt((int) l, os);
    }
    writeInt(i, os);
  }
  
  /**
   * Write the bytes of the String s to the OutputStream.
   * @param s a String
   * @param os an OutputStream
   * @throws IOException on error
   */
  public static void writeString(String s, OutputStream os) throws IOException {
    int len = s.length();
    byte b[] = new byte[len];
    s.getBytes(0, len, b, 0);
    os.write(b);
  }
}
