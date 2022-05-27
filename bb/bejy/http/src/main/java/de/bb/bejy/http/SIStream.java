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

package de.bb.bejy.http;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import de.bb.io.FastBufferedInputStream;
import de.bb.util.ByteRef;

final class SIStream extends ServletInputStream {
    private InputStream is;

    //private ByteRef buffer;
    byte[] data;

    int pos;

    long avail;

    SIStream(ByteRef buffer, long avail, FastBufferedInputStream fis) {
        this.is = fis;
        if (buffer.length() > avail) {
            data = buffer.substring(0, (int)avail).toByteArray();
            buffer.adjustLeft((int)avail);
        } else {
            data = buffer.toByteArray();
            buffer.adjustLeft(buffer.length());
        }
        this.avail = avail;
    }

    public int read() throws IOException {
        if (avail <= 0)
            return -1;
        --avail;

        //    int r = buffer.removeLeft();
        //    if (r >= 0)
        //      return r;
        if (pos < data.length)
            return data[pos++] & 0xff;

        int there = is.available();
        if (there > 0) {
            if (there > avail)
                there = (int)avail + 1;
            data = new byte[there];
            is.read(data);
            pos = 1;
            return data[0] & 0xff;
        }

        return is.read();
    }

    public int read(byte b[], int s, int l) throws IOException {
        if (l == 0)
            return 0;
        if (avail <= 0)
            return -1;
        if (l > avail)
            l = (int)avail;

        int n2 = data.length - pos;
        if (n2 > 0) {
            if (n2 > l)
                n2 = l;
            System.arraycopy(data, pos, b, s, n2);
            pos += n2;
            avail -= n2;
            return n2;
        }

        /*    if (buffer.length() > 0)
            {
              int n = buffer.copy(b, s, l);
              buffer.adjustLeft(n);
              avail -= n;
              return n;
            }
         */
        int nn = is.read(b, s, l);
        if (nn > 0)
            avail -= nn;
        return nn;
    }

    /**
     * Returns the count of available bytes, limited by the defined limit.
     */
    public int available() throws IOException {
        //    int a = is.available() + buffer.length();
        int a = is.available() + data.length - pos;
        return a < avail ? a : (int)avail;
    }

    public long skip(long n) throws IOException {

        if (n <= 0)
            return 0;

        long remaining = n;
        int size = (int) Math.min(0x2000, remaining);
        byte[] skipBuffer = new byte[size];
        while (remaining > 0) {
            int read = read(skipBuffer, 0, (int) Math.min(size, remaining));
            if (read < 0)
                break;

            if (read == 0) {
                int b = read();
                if (b < 0)
                    break;
                read = 1;
            }

            remaining -= read;
        }

        return n - remaining;
    }

    @Override
    public boolean isFinished() {
        return avail == 0;
    }

    @Override
    public boolean isReady() {
        try {
            return available() > 0;
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public void setReadListener(ReadListener arg0) {
        // TODO Auto-generated method stub
    }

}
