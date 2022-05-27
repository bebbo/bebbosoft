/******************************************************************************
 * Miscellaneous. 
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

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Provides some useful functions to deal with byte arrays.
 */
public class Misc {
    /**
     * Compares one segment of a byte array with a segment of a 2nd byte array.
     * 
     * @param a
     *            first byte array
     * @param aOff
     *            offset into first byte array
     * @param b
     *            second byte array
     * @param bOff
     *            offset into second byte array
     * @param len
     *            length of the compared segments
     * @return true if the segments are equal, otherwise false
     */
    public final static boolean equals(byte a[], int aOff, byte b[], int bOff, int len) {
        if (a == null || b == null)
            return a == b;
        while (len-- > 0)
            if (a[aOff + len] != b[bOff + len])
                return false;
        return true;
    }

    public final static boolean equals(int a[], int aOff, int b[], int bOff, int len) {
        if (a == null || b == null)
            return a == b;
        while (len-- > 0)
            if (a[aOff + len] != b[bOff + len])
                return false;
        return true;
    }

    /**
     * Compares one byte array with another byte array.
     * 
     * @param a
     *            first byte array
     * @param b
     *            second byte array
     * @return true if the byte arrays are equal, otherwise false
     */
    public final static boolean equals(byte a[], byte b[]) {
        if (a.length != b.length)
            return false;
        return equals(a, 0, b, 0, a.length);
    }

    public final static boolean equals(int a[], int b[]) {
        if (a.length != b.length)
            return false;
        return equals(a, 0, b, 0, a.length);
    }

    /**
     * Interpret the String as hex values and convert it to a byte array.
     * 
     * <pre> "abcd" -> { (byte)0xab, (byte)0xcd } </pre>
     * 
     * @param s
     *            the string
     * @return a new allocated byte array containing the bytes
     */
    public static byte[] hex2Bytes(String s) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
                sb.append(ch);
        }
        s = sb.toString();
        byte b[] = new byte[s.length() / 2];
        for (int i = 0; i < b.length; ++i) {
            b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return b;
    }

    /**
     * Convert a byte array into a hex string.
     * 
     * <pre> { (byte)0xab, (byte)0xcd } -> "abcd" </pre>
     * 
     * @param b
     *            the byte array
     * @return a new allocated string the bytes in hex
     */
    public static String bytes2Hex(byte[] b) {
        String s = "";
        for (int i = 0; i < b.length; ++i) {
            s += Integer.toHexString((b[i] & 0xf0) >> 4) + Integer.toHexString(b[i] & 0xf);
        }
        return s;
    }

    /**
     * Helper method to replace all occurencies of a character to a different character.
     * 
     * @param string
     *            a string - not null
     * @param c
     *            the character to be replaced
     * @param d
     *            the replacement
     * @return the string
     */
    public static String replaceAll(String string, char c, char d) {
        char b[] = new char[string.length()];
        string.getChars(0, b.length, b, 0);
        for (int i = 0; i < b.length; ++i) {
            if (b[i] == c) {
                b[i] = d;
            }
        }
        return new String(b);
    }

    /**
     * Dump a byte array as hex values.
     * 
     * @param ps
     *            the used PrintWriter.
     * @param s
     *            a headline for each dumped block.
     * @param bb
     *            the byte array to display
     * @param offset
     *            offset into that array
     * @param len
     *            the count of the dumped bytes.
     */
    public final static synchronized void dump(PrintWriter ps, String s, byte bb[], int offset, int len) {
        byte b[] = new byte[len];
        if (bb != null)
            System.arraycopy(bb, offset, b, 0, len);
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
                    ps.print(" " + new String(b, i - 15, 16));
                    ++i;
                    if (i < len) {
                        ps.println();
                        ps.print(Integer.toString((i >>> 12) & 0xf, 16));
                        ps.print(Integer.toString((i >>> 8) & 0xf, 16));
                        ps.print(Integer.toString((i >>> 4) & 0xf, 16));
                        ps.print(Integer.toString((i) & 0xf, 16));
                    }
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
     * helper fx to dump to stdout.
     * 
     * @param out
     *            - some outputStream
     * @param b
     *            - the bytes to dump
     */
    public static void dump(OutputStream out, byte b[]) {
        PrintWriter pw = new PrintWriter(out);
        dump(pw, "", b, 0, b.length);
    }

    /**
     * helper fx to dump to stdout.
     * 
     * @param title
     *            title for the dump message
     * @param out
     *            - some outputStream
     * @param b
     *            - the bytes to dump
     */
    public static void dump(String title, OutputStream out, byte b[]) {
        PrintWriter pw = new PrintWriter(out);
        dump(pw, title, b, 0, b.length);
    }

    /**
     * helper fx to dump to stdout.
     * 
     * @param title
     *            title for the dump message
     * @param out
     *            - some outputStream
     * @param b
     *            - the bytes to dump
     * @param len
     *            the length to dump
     */
    public static void dump(String title, OutputStream out, byte b[], int len) {
        PrintWriter pw = new PrintWriter(out);
        dump(pw, title, b, 0, len);
    }

    /**
     * helper fx to dump to stdout.
     * 
     * @param title
     *            title for the dump message
     * @param out
     *            - some outputStream
     * @param b
     *            - the bytes to dump
     * @param offset
     *            offset into the data
     * @param len
     *            the length to dump
     */
    public static void dump(String title, OutputStream out, byte b[], int offset, int len) {
        PrintWriter pw = new PrintWriter(out);
        dump(pw, title, b, offset, len);
    }
}
