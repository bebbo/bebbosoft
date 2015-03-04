/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/Mime.java,v $
 * $Revision: 1.6 $
 * $Date: 2014/06/23 19:10:36 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Helper class for MIME encoding and decoding.  
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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

import java.io.*;

/**
 * Some helpfull static functions to handle Mime encoding and decoding.
 */
public class Mime {
    // ===========================================================================
    // needed for encode / decode
    // ===========================================================================
    final static byte _ERR = 127;
    final static byte _EOF = 126;
    final static byte _EOL = 125;

    final static byte decodeTable[];
    final static byte encodeTable[];
    final static byte beginText[] = {(byte) '-', (byte) '-', (byte) '-', (byte) '-', (byte) '-', (byte) 'B',
            (byte) 'E', (byte) 'G', (byte) 'I', (byte) 'N'};
    final static byte endText[] = {(byte) '-', (byte) '-', (byte) '-', (byte) '-', (byte) '-', (byte) 'E', (byte) 'N',
            (byte) 'D'};
    final static byte lf2a[] = {0xa, 0xa};
    final static byte lf2b[] = {0xd, 0xa, 0xd, 0xa};

    /**
     * Do base64 decode of a byte array.
     * 
     * @param x
     *            input data
     * @return a new allocated byte array with decoded data
     */
    public final static byte[] decode(byte x[]) {
        return decode(x, 0, x.length);
    }
    /**
     * Do base64 decode of a byte array.
     * 
     * @param x
     *            input data
     * @param i
     *            an int specifying the start index into byte array
     * @param stop
     *            an int specifying the stop index into byte array
     * @return a new allocated byte array with decoded data
     */
    public final static byte[] decode(byte x[], int i, int stop) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // read up to 4 bytes - write up to 3 bytes
        try {
            while (i < stop) {
                int a, b;
                do {
                    a = decodeTable[x[i++] & 0xff];
                } while (a == _EOL);
                if (a == _EOF)
                    break;

                do {
                    b = decodeTable[x[i++] & 0xff];
                } while (b == _EOL);
                if (b == _EOF)
                    break;
                bos.write((byte) ((a << 2) | (b >>> 4)));

                do {
                    a = decodeTable[x[i++] & 0xff];
                } while (a == _EOL);
                if (a == _EOF)
                    break;
                bos.write((byte) ((b << 4) | (a >>> 2)));

                do {
                    b = decodeTable[x[i++] & 0xff];
                } while (b == _EOL);
                if (b == _EOF)
                    break;
                bos.write((byte) ((a << 6) | b));
            }
        } catch (Exception e) {
        }
        return bos.toByteArray();
    }

    /**
     * Do base64 encode of a byte array.
     * 
     * @param x
     *            input data
     * @return a new allocated byte array with encoded data - WITHOUT line breaks!
     */
    public final static byte[] encode(byte x[]) {
        return encode(x, x.length);
    }

    /**
     * Do base64 encode of a byte array.
     * 
     * @param x
     *            input data
     * @param width
     *            defines after how many input bytes a lf is added
     * @return a new allocated byte array with encoded data
     */
    public final static byte[] encode(byte x[], int width) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // read up to 3 bytes - write up to 4 bytes
        for (int i = 0; i < x.length;) {
            int a, b, c, d;
            a = x[i++] & 0xff;
            if (i < x.length) {
                b = x[i++] & 0xff;
                if (i < x.length) {
                    c = x[i++] & 0xff;
                    d = c & 0x3f;
                } else {
                    c = 0;
                    d = 0x40;
                }
                c = (c >>> 6) | ((b << 2) & 0x3f);
            } else {
                b = 0;
                c = 0x40;
                d = 0x40;
            }
            b = (b >>> 4) | ((a << 4) & 0x3f);
            a >>>= 2;
            bos.write(encodeTable[a]);
            bos.write(encodeTable[b]);
            bos.write(encodeTable[c]);
            bos.write(encodeTable[d]);
            if (i < x.length && (i % width) == 0) {
                bos.write((byte) 0xd);
                bos.write((byte) 0xa);
            }
        }
        return bos.toByteArray();
    }

    /**
     * Search a byte array within another byte array.
     * 
     * @param s
     *            the searched byte array
     * @param off
     *            an offset where the search starts
     * @param x
     *            we are looking for these bytes
     * @return the absolute offset or -1, when not found
     */
    public static int strstr(byte s[], int off, byte x[]) {
        for (; off < s.length - x.length; ++off) {
            int i;
            for (i = 0; i < x.length; ++i)
                if (s[off + i] != x[i])
                    break;
            if (i == x.length)
                return off;
        }
        return -1;
    }

    /**
     * Search an base64 encodede range. Also header information, marked by a double lf, is skipped!
     * 
     * @param x
     *            the bytes to be searched for "-----BEGIN ..." ... "-----END ..."
     * @return a byte array containing the part within "-----BEGIN ..." ... "-----END ..."
     */
    public static byte[] searchDecode(byte x[]) {
        return searchDecode(x, 0);
    }

    /**
     * Search an base64 encodede range. Also header information, marked by a double lf, is skipped!
     * 
     * @param x
     *            the bytes to be searched for "-----BEGIN ..." ... "-----END ..."
     * @param off
     *            an offset into the byte array
     * @return a byte array containing the part within "-----BEGIN ..." ... "-----END ..."
     */
    public static byte[] searchDecode(byte x[], int off) {
        int start = strstr(x, off, beginText);
        if (start < 0)
            return null;
        // next line
        while (start < x.length)
            if (x[start] >= 32)
                ++start;
            else
                break;
        while (start < x.length)
            if (x[start] < 32)
                ++start;
            else
                break;

        int stop = strstr(x, start, endText);
        int lfp = strstr(x, start, lf2a);
        if (lfp < 0)
            lfp = strstr(x, start, lf2b);
        if (lfp > 0 && lfp < stop)
            start = lfp;

        if (stop < 0)
            return null;
        return decode(x, start, stop);
    }

    /**
     * Search a base64 parameter.
     * 
     * @param b
     *            the searched bytes
     * @param off
     *            offset to start the search
     * @param param
     *            name of wanted parameter
     * @return a String containing the parameter, or null when not found
     */
    static public String getParam(byte b[], int off, String param) {
        param += ":";
        int dek = Mime.strstr(b, off, param.getBytes());
        if (dek < 0)
            return null;

        int end = Mime.strstr(b, off, endText);
        if (dek > end)
            return null;

        dek += param.length() + 1; // skip param and ": "
        for (end = dek; end < b.length && b[end] >= 32;)
            ++end;
        return new String(b, dek, end - dek);
    }

    /**
     * Encode with header / footer.
     * 
     * @param x
     *            bytes to encode
     * @param txt
     *            placed behind _----BEGIN and -----END
     * @return an byte array with the encoded data
     * @throws IOException
     *             on error
     */
    public static byte[] encodeFull(byte x[], String txt) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(beginText);
        bos.write((byte) 32);
        bos.write(txt.getBytes());
        bos.write((byte) 0xd);
        bos.write((byte) 0xa);
        bos.write(encode(x));
        bos.write((byte) 0xd);
        bos.write((byte) 0xa);
        bos.write(endText);
        bos.write((byte) 32);
        bos.write(txt.getBytes());
        bos.write((byte) 0xd);
        bos.write((byte) 0xa);
        return bos.toByteArray();
    }

    /**
     * Create a Mime header for base64 encoded data.
     * 
     * @param name
     *            the name of the original file
     * @return a byte array containing the MIME header, or null on error
     */
    public static byte[] createHeader(String name) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write("MIME-Version: 1.0\r\nContent-Type: application/octet-stream; name=\"".getBytes());
            bos.write(name.getBytes());
            bos.write("\"\r\nContent-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\""
                    .getBytes());
            bos.write(name.getBytes());
            bos.write("\"\r\n\r\n".getBytes());
            return bos.toByteArray();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * init of static members
     */
    static {
        // init decode table
        decodeTable = new byte[0x100];
        int i;
        for (i = 0; i < 0x100; ++i)
            decodeTable[i] = _EOF;
        for (i = 'A'; i <= 'Z'; ++i)
            decodeTable[i] = (byte) (i - 'A');
        for (i = 'a'; i <= 'z'; ++i)
            decodeTable[i] = (byte) (i - 'a' + 26);
        for (i = '0'; i <= '9'; ++i)
            decodeTable[i] = (byte) (i - '0' + 52);

        decodeTable['+'] = 62;
        decodeTable['/'] = 63;

        decodeTable['\r'] = _EOL;
        decodeTable['\n'] = _EOL;
        decodeTable[' '] = _EOL;

        // init encode table
        encodeTable = new byte[0x41];
        for (i = 0; i < 26; ++i) {
            encodeTable[i] = (byte) ('A' + i);
            encodeTable[i + 26] = (byte) ('a' + i);
        }
        for (i = 0; i < 10; ++i)
            encodeTable[i + 52] = (byte) ('0' + i);
        encodeTable[62] = (byte) '+';
        encodeTable[63] = (byte) '/';
        encodeTable[64] = (byte) '=';
    }
    /**
     * public static void main(String args[]) { try { BufferedReader fr = new BufferedReader(new
     * FileReader("c:/temp/taxor.ldif")); FileWriter fw = new FileWriter("c:/temp/t.ldif"); for (String line =
     * fr.readLine();line != null; line = fr.readLine()) { int space = line.indexOf(' '); if (space > 0) { if
     * (line.substring(0, space).endsWith("::")) { String val = line.substring(space).trim(); line = line.substring(0,
     * space - 1) + " " + new String(decode(val.getBytes(), 0, val.length())); } } fw.write(line + "\r\n"); }
     * fw.close(); fr.close(); // String encoded = "e1NTSEF9eS9KcWFFdDZUd0kxcU5PTml2TnFUMVhsOGVKNVJkRjNlR3phbXc9PQ==";
     * // byte data[] = decode(encoded.getBytes(), 0, encoded.length()); // Misc.dump(System.out, data); } catch
     * (Exception ex) { } }
     */
}

/******************************************************************************
 * $Log: Mime.java,v $
 * Revision 1.6  2014/06/23 19:10:36  bebbo
 * @N added convenience method to decode a mime byte array
 *
 * Revision 1.5  2013/05/17 10:57:04  bebbo
 * @ reformat
 * Revision 1.4 2008/03/15 18:01:05 bebbo
 * 
 * @R Changed the license: From now on GPL 3 applies.
 * 
 *    Revision 1.3 2001/12/10 16:22:50 bebbo
 * @C completed comments!
 * 
 *    Revision 1.2 2001/09/15 08:56:34 bebbo
 * @C added comments
 * 
 *    Revision 1.1 2001/01/01 16:53:20 bebbo
 * @N new
 * 
 *****************************************************************************/
