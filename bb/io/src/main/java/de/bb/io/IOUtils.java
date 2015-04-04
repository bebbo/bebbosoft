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
import java.io.OutputStream;

/**
 * Some useful methods to read or copy date via streams.
 * 
 * @author bebbo
 *
 */
public class IOUtils {

    /**
     * Copy the given length of data from the given <code>InputStream</code> to
     * the given <code>OutputStream</code>.
     * 
     * @param is
     *            the <code>InputStream</code>
     * @param os
     *            the <code>OutputStream</code>
     * @param length
     *            the length of data to copy.
     * @throws IOException
     *             on error, e.g. if the given length of data was not reached.
     */
    public static void copy(InputStream is, OutputStream os, long length)
            throws IOException {
        byte b[] = new byte[8192];
        while (length > 0) {
            int b0 = is.read();
            if (b0 < 0)
                throw new IOException("EOS");
            os.write(b0);
            --length;
            if (length == 0)
                return;
            int read = length > b.length ? b.length : (int) length;
            read = is.read(b, 0, read);
            if (read > 0) {
                os.write(b, 0, read);
                length -= read;
            }
        }
    }

    /**
     * Read the given length of data from the given <code>InputStream</code> to
     * the given <code>byte []</code> buffer.
     * 
     * @param is
     *            the <code>InputStream</code>
     * @param buffer
     *            the <code>byte []</code> buffer
     * @param position
     *            the offset into the buffer to read to
     * @param length
     *            the length of data to copy.
     * @throws IOException
     *             on error, e.g. if the given length of data was not reached.
     */
    public static void readFully(InputStream is, byte[] buffer, int position,
            int length) throws IOException {
        while (position < length) {
            int b0 = is.read();
            if (b0 < 0)
                throw new IOException("EOS");
            buffer[position++] = (byte) b0;
            position += is.read(buffer, position, length - position);
        }
    }

    /**
     * Read the data from the given <code>InputStream</code> to the given
     * <code>byte []</code> buffer and fill starting at 0.
     * 
     * @param is
     *            the <code>InputStream</code>
     * @param buffer
     *            the <code>byte []</code> buffer
     * @throws IOException
     *             on error, e.g. if the given length of data was not reached.
     */
    public static void readFully(InputStream is, byte[] buffer) throws IOException {
        readFully(is, buffer, 0, buffer.length);
    }
}
