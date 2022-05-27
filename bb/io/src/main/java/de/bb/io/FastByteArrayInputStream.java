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

public class FastByteArrayInputStream extends InputStream {

    private byte[] data;
    private int pos;

    public FastByteArrayInputStream(byte[] partsData) {
        this.data = partsData;
    }

    public int read() throws IOException {
        if (pos == data.length)
            return -1;

        return data[pos++] & 0xff;
    }

    public int read(byte b[], int s, int l) throws IOException {
        if (l == 0)
            return 0;
        if (pos == data.length)
            return -1;
        if (l + pos > data.length)
            l = data.length - pos;

        System.arraycopy(data, pos, b, s, l);
        pos += l;
        return l;
    }

    /**
     * Returns the count of available bytes, limited by the defined limit.
     */
    public int available() throws IOException {
        final int a = data.length - pos;
        return a;
    }

    public long skip(long n) throws IOException {
        if (n + pos > data.length) {
            long r = data.length - pos;
            pos = data.length;
            return r;
        }
        
        pos += (int)n;
        return n;
    }

}
