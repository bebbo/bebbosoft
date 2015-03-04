package de.bb.io;

import java.util.Arrays;

public class FastUtf8 {
    /**
     * Convert a String into utf-8 bytes.
     * 
     * @param s
     *            the String
     * @return a byte array.
     */
    public static byte[] encode(final String s) {
        final int slen = s.length();
        //        final char chars[] = new char[slen];
        //        s.getChars(0, slen, chars, 0);
        int blen = slen * 2 + 7;
        byte bytes[] = new byte[blen];

        int j = 0;
        for (int i = 0; i < slen; ++i) {
            int ch = s.charAt(i);
            //            int ch = chars[i];
            // simply ascii
            if (ch < 0x80) {
                bytes[j++] = (byte) ch;
                if (j == blen) {
                    blen += blen >> 1;
                    bytes = Arrays.copyOf(bytes, blen);
                }
                continue;
            }
            if (j + 7 >= blen) {
                blen += blen >> 1;
                bytes = Arrays.copyOf(bytes, blen);
            }
            // multi byte encode
            if (ch < 0x800) {
                bytes[j + 1] = (byte) (0x80 | (ch & 0x3f));
                bytes[j] = (byte) (0xc0 | (ch >> 6));
                j += 2;
                continue;
            }

            if (ch < 0x10000) {
                bytes[j + 2] = (byte) (0x80 | (ch & 0x3f));
                bytes[j + 1] = (byte) (0x80 | ((ch >> 6) & 0x3f));
                bytes[j] = (byte) (0xe0 | (ch >> 12));
                j += 3;
                continue;
            }
            if (ch < 0x200000) {
                bytes[j + 3] = (byte) (0x80 | (ch & 0x3f));
                bytes[j + 2] = (byte) (0x80 | ((ch >> 6) & 0x3f));
                bytes[j + 1] = (byte) (0x80 | ((ch >> 12) & 0x3f));
                bytes[j] = (byte) (0xf0 | (ch >> 18));
                j += 4;
                continue;
            }
            if (ch < 0x4000000) {
                bytes[j + 4] = (byte) (0x80 | (ch & 0x3f));
                bytes[j + 3] = (byte) (0x80 | ((ch >> 6) & 0x3f));
                bytes[j + 2] = (byte) (0x80 | ((ch >> 12) & 0x3f));
                bytes[j + 1] = (byte) (0x80 | ((ch >> 18) & 0x3f));
                bytes[j] = (byte) (0xf8 | (ch >> 24));
                j += 5;
                continue;
            }
            bytes[j + 5] = (byte) (0x80 | (ch & 0x3f));
            bytes[j + 4] = (byte) (0x80 | ((ch >> 6) & 0x3f));
            bytes[j + 3] = (byte) (0x80 | ((ch >> 12) & 0x3f));
            bytes[j + 2] = (byte) (0x80 | ((ch >> 18) & 0x3f));
            bytes[j + 1] = (byte) (0x80 | ((ch >> 24) & 0x3f));
            bytes[j] = (byte) (0xfc | (ch >> 30));
            j += 6;
        }
        if (j == blen)
            return bytes;
        return Arrays.copyOf(bytes, j);
    }

    public static String decode(final byte[] bytes) {
        final int blen = bytes.length;
        final char chars[] = new char[blen];
        int j = 0;
        for (int i = 0; i < blen;) {
            int b = bytes[i];
            if (b >= 0) {
                chars[j++] = (char) b;
                ++i;
                continue;
            }
            if (b < ~0x1f) {
                chars[j++] = (char) (((b << 6) & 0x3f) | (bytes[i + 1] & 0x3f));
                i += 2;
                continue;
            }
            if (b < ~0x0f) {
                chars[j++] = (char) (((b << 12) & 0x1f) | ((bytes[i + 1] & 0x3f) << 6) | (bytes[i + 2] & 0x3f));
                i += 3;
                continue;
            }
            if (b < ~0x07) {
                chars[j++] = (char) (((b << 18) & 0x0f) | ((bytes[i + 1] & 0x3f) << 12) | ((bytes[i + 2] & 0x3f) << 6) | (bytes[i + 3] & 0x3f));
                i += 4;
                continue;
            }
            if (b < ~0x03) {
                chars[j++] = (char) (((b << 24) & 0x07) | ((bytes[i + 1] & 0x3f) << 18) | ((bytes[i + 2] & 0x3f) << 12) | ((bytes[i + 3] & 0x3f) << 6) | (bytes[i + 4] & 0x3f));
                i += 5;
                continue;
            }
            chars[j++] = (char) (((b << 30) & 0x03) | ((bytes[i + 1] & 0x3f) << 24) | ((bytes[i + 2] & 0x3f) << 18) | ((bytes[i + 3] & 0x3f) << 12) | ((bytes[i + 4] & 0x3f) << 6) | (bytes[i + 5] & 0x3f));
            i += 6;
        }
        return new String(chars, 0, j);
    }
}
