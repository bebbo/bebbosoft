/******************************************************************************
 * A class to handle formatted dates in a fast way. 
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

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A collection of high speed date conversion functions. If you ever measured the really low performance of the JDK
 * DateFormat classes, you understand its purpose instantly. The most needed date/time formatting functions plus the
 * String parsing functions are supplied.
 */
public class DateFormat {
    /** constant value for Sunday. */
    public final static int SUNDAY = 1;

    /** constant value for Monday. */
    public final static int MONDAY = 2;

    /** constant value for Tuesday. */
    public final static int TUESDAY = 3;

    /** constant value for Wednesday. */
    public final static int WEDNESDAY = 4;

    /** constant value for Thursday. */
    public final static int THURSDAY = 5;

    /** constant value for Friday. */
    public final static int FRIDAY = 6;

    /** constant value for Saturday. */
    public final static int SATURDAY = 7;

    private final static boolean DEBUG = false;

    private static int offsetSeconds; // tz in seconds

    final private static int[] MONTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    final private static int[] SMONTH = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    final private static char[][] DAYNAMES = {{'T', 'h', 'u'}, {'F', 'r', 'i'}, {'S', 'a', 't'}, {'S', 'u', 'n'},
            {'M', 'o', 'n'}, {'T', 'u', 'e'}, {'W', 'e', 'd'}};

    final private static char[][] MONTHNAMES = {{'J', 'a', 'n'}, {'F', 'e', 'b'}, {'M', 'a', 'r'}, {'A', 'p', 'r'},
            {'M', 'a', 'y'}, {'J', 'u', 'n'}, {'J', 'u', 'l'}, {'A', 'u', 'g'}, {'S', 'e', 'p'}, {'O', 'c', 't'},
            {'N', 'o', 'v'}, {'D', 'e', 'c'}};

    final private static byte[][] BDAYNAMES = {{'T', 'h', 'u'}, {'F', 'r', 'i'}, {'S', 'a', 't'}, {'S', 'u', 'n'},
            {'M', 'o', 'n'}, {'T', 'u', 'e'}, {'W', 'e', 'd'}};

    final private static byte[][] BMONTHNAMES = {{'J', 'a', 'n'}, {'F', 'e', 'b'}, {'M', 'a', 'r'}, {'A', 'p', 'r'},
            {'M', 'a', 'y'}, {'J', 'u', 'n'}, {'J', 'u', 'l'}, {'A', 'u', 'g'}, {'S', 'e', 'p'}, {'O', 'c', 't'},
            {'N', 'o', 'v'}, {'D', 'e', 'c'}};

    /** millis per day. */
    public final static long TIME_PER_DAY = 24 * 60 * 60 * 1000L;

    // an exact full moon
    private final static long fullMoon;

    // duration of a moon month
    private final static long moonMonth = (long) (29.530587 * TIME_PER_DAY);

    private byte[] mask;

    private int myTimeZone;

    private int[] indexes;

    private int[] offsets;

    private int[] lengths;

    /**
     * This constructor deals with various date formats. Supported abbreviations are:
     * <ul>
     * <li>d - day</li>
     * <li>M - month</li>
     * <li>y - year</li>
     * <li>H - hours</li>
     * <li>m - minutes</li>
     * <li>s - seconds</li>
     * <li>S - milliseconds</li>
     * <li>z - time zone</li>
     * </ul>
     * 
     * if 2 or less characters are used, the short form is used (if any exist). Otherwise leading zeros are inserted to
     * reach the defined amount.
     * 
     * Month: - M yields the numeric months suppressing the leading zero - MM yields the numeric months with a leading
     * zero - MMM yields the English short name - MMMM or more ist not yet implemented
     * 
     * Milliseconds: - S yields 1/10 seconds - SS yields 1/100 seconds - SSS yields 1/1000 seconds
     * 
     * @param format
     *            the format string
     */
    public DateFormat(String format) {
        this(format, offsetSeconds);
    }

    /**
     * Constructor DateFormat.
     * 
     * @param format
     *            the format string
     * @param tz
     *            timezone offset in seconds
     * @see #DateFormat(String)
     */
    public DateFormat(String format, int tz) {
        myTimeZone = tz;

        LinkedList fieldInfos = new LinkedList();
        byte[] b = format.getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < b.length; ++i) {
            int c = b[i];
            switch (c) {
                case 'd':
                case 'H':
                case 'm':
                case 'M':
                case 's':
                case 'S':
                case 'y':
                case 'z':
                    int tag = -1;
                    switch (c) {
                        case 'd':
                            tag = 2;
                            break;
                        case 'H':
                            tag = 4;
                            break;
                        case 'm':
                            tag = 5;
                            break;
                        case 'M':
                            tag = 1;
                            break;
                        case 's':
                            tag = 6;
                            break;
                        case 'S':
                            tag = 7;
                            break;
                        case 'y':
                            tag = 0;
                            break;
                    }

                    int ii[] = new int[]{tag, bos.size(), 0};
                    fieldInfos.add(ii);

                    int len = i + 1;
                    while (len < b.length && b[len] == c) {
                        ++len;
                    }
                    len -= i; // repeat count
                    i += len - 1;
                    // special cases:
                    if (len == 1 && c != 'S') {
                        len = 2;
                    }
                    if (c == 'M' && len > 3) {
                        len = 3;
                    } else if (c == 'y' && len == 3) {
                        len = 4;
                    } else if (c == 'z' && len < 3) {
                        len = 3;
                        // } else if (c == 'S' && len < 3) {
                        // len = 3;
                    }
                    ii[2] = len;
                    // pad bos
                    for (int j = 0; j < len; ++j) {
                        bos.write(32);
                    }
                    break;
                default:
                    bos.write(c);
            }
        }

        mask = bos.toByteArray();

        indexes = new int[fieldInfos.size()];
        offsets = new int[fieldInfos.size()];
        lengths = new int[fieldInfos.size()];

        int n = 0;
        for (Iterator i = fieldInfos.iterator(); i.hasNext();) {
            int[] ii = (int[]) i.next();
            indexes[n] = ii[0];
            offsets[n] = ii[1];
            lengths[n] = ii[2];
            ++n;
        }

    }

    /**
     * Returns the current fill bytes. Means all date/time digits are replaces by spaces.
     * 
     * @return Returns the current fill bytes. Means all date/time digits are replaces by spaces.
     */
    public String getMask() {
        return new String(mask);
    }

    /**
     * Returns the formatted String using the format String specified at the constructor. Important to know that this is
     * much faster than the java.util.DateFormat, it is still 3 times slower than the explicit (hard coded) static
     * format functions.
     * 
     * @param time
     *            GMT time in milliseconds.
     * @return Returns the formatted String using the format String specified at the constructor.
     */
    public String format(long time) {
        byte b[] = (byte[]) mask.clone();

        int v[] = calc(time, myTimeZone);

        for (int i = 0; i < indexes.length; ++i) {
            int idx = indexes[i];
            int offset = offsets[i];
            int len = lengths[i];

            if (idx == 1 && len == 3) {
                int m = v[idx];
                b[offset++] = (byte) MONTHNAMES[m][0];
                b[offset++] = (byte) MONTHNAMES[m][1];
                b[offset] = (byte) MONTHNAMES[m][2];
            } else if (idx == 7) {
                lfill(b, v[idx], offset, len, 3);
            } else if (idx >= 0) {
                // month or day start at 1
                if (idx == 1 || idx == 2) {
                    ++v[idx];
                }
                fill(b, v[idx], offset, len);
            } else {
                b[offset++] = (byte) (myTimeZone >= 0 ? '+' : '-');
                fill(b, myTimeZone / 3600, offset, 2);
                offset += 2;
                if (len > 5) {
                    b[offset++] = (byte) ':';
                }
                if (len > 4) {
                    int min = (myTimeZone / 60) % 60;
                    fill(b, min, offset, 2);
                }
            }
        }

        return new String(b);
    }

    /**
     * fill into the given byte array
     * 
     * @param b
     *            the byte array
     * @param val
     * @param offset
     *            into the byte array b
     * @param len
     *            length to fill
     * @param defaultLen
     *            default length
     */
    private void lfill(byte[] b, int val, int offset, int len, int defaultLen) {
        // fill leading zeros
        while (len > defaultLen) {
            b[offset++] = '0';
            --len;
        }
        while (defaultLen > 0) {
            int d = '0' + (val % 10);
            if (defaultLen == len) {
                --len;
                b[offset + len] = (byte) d;
            }
            val = val / 10;
            --defaultLen;
        }
    }

    /**
     * Method fill.
     * 
     * @param b
     * @param val
     * @param offset
     * @param len
     */
    private void fill(byte[] b, int val, int offset, int len) {
        for (int i = offset + len - 1; i >= offset; --i) {
            int d = '0' + (val % 10);
            b[i] = (byte) d;
            val = val / 10;
        }
        if (offset == 0) {
            for (int i = 0; i < len - 1; ++i) {
                if (b[i] == '0')
                    b[i] = ' ';
                else
                    break;
            }
        }
    }

    /**
     * Return a byte array for the given date (long) in format <code>dd/MMM/yyyy:HH:mm:ss +zz:zz</code>.
     * 
     * <pre>
     * e.g. 01/Jan/1970:00:00:01 +02:00
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a byte array for the given date (long) in format </code>dd/MMM/yyyy:HH:mm:ss +zz:zz</code>.
     */
    public static byte[] ba_dd_MMM_yyyy_HH_mm_ss_zzzz(long l) {
        int[] v = calc(l);
        byte b[] = new byte[26];

        int y = v[2] + 1; // day
        b[1] = (byte) (y % 10 + 48);
        y /= 10;
        b[0] = (byte) (y % 10 + 48);

        b[2] = '/';

        y = v[1]; // month
        byte[] month = BMONTHNAMES[y];
        b[3] = month[0];
        b[4] = month[1];
        b[5] = month[2];
        b[6] = '/';

        y = v[0]; // year
        b[10] = (byte) (y % 10 + 48);
        y /= 10;
        b[9] = (byte) (y % 10 + 48);
        y /= 10;
        b[8] = (byte) (y % 10 + 48);
        y /= 10;
        b[7] = (byte) (y % 10 + 48);

        b[11] = ':';

        y = v[4]; // hour
        b[13] = (byte) (y % 10 + 48);
        y /= 10;
        b[12] = (byte) (y % 10 + 48);

        b[14] = ':';

        y = v[5]; // minutes
        b[16] = (byte) (y % 10 + 48);
        y /= 10;
        b[15] = (byte) (y % 10 + 48);

        b[17] = ':';

        y = v[6]; // seconds
        b[19] = (byte) (y % 10 + 48);
        y /= 10;
        b[18] = (byte) (y % 10 + 48);

        b[20] = ' ';

        y = offsetSeconds / 60; // timezone in minutes
        if (y < 0)
            b[21] = '-';
        else
            b[21] = '+';

        int m = y % 60;
        y /= 60;
        b[23] = (byte) (y % 10 + 48);
        y /= 10;
        b[22] = (byte) (y % 10 + 48);

        // b[24] = ':';

        b[25] = (byte) (m % 10 + 48);
        m /= 10;
        b[24] = (byte) (m % 10 + 48);

        return b;
    }

    /**
     * Return a String for the given date (long) in format <code>dd/MMM/yyyy:HH:mm:ss +zz:zz</code>.
     * 
     * <pre>
     * e.g. 01/Jan/1970:00:00:01 +02:00
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format </code>dd/MMM/yyyy:HH:mm:ss +zz:zz</code>.
     */
    public static String dd_MMM_yyyy_HH_mm_ss_zzzz(long l) {
        int[] v = calc(l);
        char b[] = new char[26];

        int y = v[2] + 1; // day
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        b[2] = '/';

        y = v[1]; // month
        char[] month = MONTHNAMES[y];
        b[3] = month[0];
        b[4] = month[1];
        b[5] = month[2];
        b[6] = '/';

        y = v[0]; // year
        b[10] = (char) (y % 10 + 48);
        y /= 10;
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);
        y /= 10;
        b[7] = (char) (y % 10 + 48);

        b[11] = ':';

        y = v[4]; // hour
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        b[14] = ':';

        y = v[5]; // minutes
        b[16] = (char) (y % 10 + 48);
        y /= 10;
        b[15] = (char) (y % 10 + 48);

        b[17] = ':';

        y = v[6]; // seconds
        b[19] = (char) (y % 10 + 48);
        y /= 10;
        b[18] = (char) (y % 10 + 48);

        b[20] = ' ';

        y = offsetSeconds / 60; // timezone in minutes
        b[21] = (y < 0) ? '-' : '+';

        int m = y % 60;
        y /= 60;
        b[23] = (char) (y % 10 + 48);
        y /= 10;
        b[22] = (char) (y % 10 + 48);

        // b[24] = ':';

        b[25] = (char) (m % 10 + 48);
        m /= 10;
        b[24] = (char) (m % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format <code>dd/MMM/yyyy:HH:mm:ss GMT+zz:zz</code>.
     * 
     * <pre>
     * e.g. 01/Jan/1970:00:00:01 GMT+02:00
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format <code>dd/MMM/yyyy:HH:mm:ss GMT+zz:zz</code>.
     */
    public static String dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(long l) {
        int[] v = calc(l);
        char b[] = new char[30];

        int y = v[2] + 1; // day
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        b[2] = '/';

        y = v[1]; // month
        char[] month = MONTHNAMES[y];
        b[3] = month[0];
        b[4] = month[1];
        b[5] = month[2];
        b[6] = '/';

        y = v[0]; // year
        b[10] = (char) (y % 10 + 48);
        y /= 10;
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);
        y /= 10;
        b[7] = (char) (y % 10 + 48);

        b[11] = ':';

        y = v[4]; // hour
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        b[14] = ':';

        y = v[5]; // minutes
        b[16] = (char) (y % 10 + 48);
        y /= 10;
        b[15] = (char) (y % 10 + 48);

        b[17] = ':';

        y = v[6]; // seconds
        b[19] = (char) (y % 10 + 48);
        y /= 10;
        b[18] = (char) (y % 10 + 48);

        b[20] = ' ';
        b[21] = 'G';
        b[22] = 'M';
        b[23] = 'T';

        y = offsetSeconds / 60; // timezone in minutes
        b[24] = (y < 0) ? '-' : '+';

        int m = y % 60;
        y /= 60;
        b[26] = (char) (y % 10 + 48);
        y /= 10;
        b[25] = (char) (y % 10 + 48);

        b[27] = ':';

        b[29] = (char) (m % 10 + 48);
        m /= 10;
        b[28] = (char) (m % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss +zzzz</code>.
     * 
     * <pre>
     * e.g. Thu, 01 Jan 1970 00:00:01 +0200
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss +zzzz</code>.
     */
    public static String EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(long l) {
        int[] v = calc(l);
        char b[] = new char[31];

        int y = v[3]; // weekday
        char[] day = DAYNAMES[(y + 2) % 7];
        b[0] = day[0];
        b[1] = day[1];
        b[2] = day[2];
        b[3] = ',';
        b[4] = ' ';

        y = v[2] + 1; // day
        b[6] = (char) (y % 10 + 48);
        y /= 10;
        b[5] = (char) (y % 10 + 48);

        b[7] = ' ';

        y = v[1]; // month
        char[] month = MONTHNAMES[y];
        b[8] = month[0];
        b[9] = month[1];
        b[10] = month[2];
        b[11] = ' ';

        y = v[0]; // year
        b[15] = (char) (y % 10 + 48);
        y /= 10;
        b[14] = (char) (y % 10 + 48);
        y /= 10;
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        b[16] = ' ';

        y = v[4]; // hour
        b[18] = (char) (y % 10 + 48);
        y /= 10;
        b[17] = (char) (y % 10 + 48);

        b[19] = ':';

        y = v[5]; // minutes
        b[21] = (char) (y % 10 + 48);
        y /= 10;
        b[20] = (char) (y % 10 + 48);

        b[22] = ':';

        y = v[6]; // seconds
        b[24] = (char) (y % 10 + 48);
        y /= 10;
        b[23] = (char) (y % 10 + 48);

        b[25] = ' ';

        y = offsetSeconds / 60; // timezone in minutes
        b[26] = (y < 0) ? '-' : '+';

        int m = y % 60;
        y /= 60;
        b[28] = (char) (y % 10 + 48);
        y /= 10;
        b[27] = (char) (y % 10 + 48);
        b[30] = (char) (m % 10 + 48);
        m /= 10;
        b[29] = (char) (m % 10 + 48);

        return new String(b);
    }

    /**
     * Return a byte array for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss GMT</code>.
     * 
     * <pre>
     * e.g. Mon, 01 Jan 1970 00:00:01 GMT
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a byteA array for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss GMT</code>.
     */
    public static byte[] ba_EEE__dd_MMM_yyyy_HH_mm_ss_GMT(long l) {
        int[] v = calc(l, 0);
        byte b[] = new byte[29];

        int y = v[3]; // weekday
        byte[] day = BDAYNAMES[(y + 2) % 7];
        b[0] = day[0];
        b[1] = day[1];
        b[2] = day[2];
        b[3] = ',';
        b[4] = ' ';

        y = v[2] + 1; // day
        b[6] = (byte) (y % 10 + 48);
        y /= 10;
        b[5] = (byte) (y % 10 + 48);

        b[7] = ' ';

        y = v[1]; // month
        byte[] month = BMONTHNAMES[y];
        b[8] = month[0];
        b[9] = month[1];
        b[10] = month[2];
        b[11] = ' ';

        y = v[0]; // year
        b[15] = (byte) (y % 10 + 48);
        y /= 10;
        b[14] = (byte) (y % 10 + 48);
        y /= 10;
        b[13] = (byte) (y % 10 + 48);
        y /= 10;
        b[12] = (byte) (y % 10 + 48);

        b[16] = ' ';

        y = v[4]; // hour
        b[18] = (byte) (y % 10 + 48);
        y /= 10;
        b[17] = (byte) (y % 10 + 48);

        b[19] = ':';

        y = v[5]; // minutes
        b[21] = (byte) (y % 10 + 48);
        y /= 10;
        b[20] = (byte) (y % 10 + 48);

        b[22] = ':';

        y = v[6]; // seconds
        b[24] = (byte) (y % 10 + 48);
        y /= 10;
        b[23] = (byte) (y % 10 + 48);

        b[25] = ' ';
        b[26] = 'G';
        b[27] = 'M';
        b[28] = 'T';
        return b;
    }

    /**
     * Return a String for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss GMT</code>.
     * 
     * <pre>
     * e.g. Mon, 01 Jan 1970 00:00:01 GMT
     * </pre>
     * 
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format <code>EEE, dd MMM yyyy HH:mm:ss GMT</code>.
     */
    public static String EEE__dd_MMM_yyyy_HH_mm_ss_GMT(long l) {
        int[] v = calc(l, 0);
        char b[] = new char[29];

        int y = v[3]; // weekday
        char[] day = DAYNAMES[(y + 2) % 7];
        b[0] = day[0];
        b[1] = day[1];
        b[2] = day[2];
        b[3] = ',';
        b[4] = ' ';

        y = v[2] + 1; // day
        b[6] = (char) (y % 10 + 48);
        y /= 10;
        b[5] = (char) (y % 10 + 48);

        b[7] = ' ';

        y = v[1]; // month
        char[] month = MONTHNAMES[y];
        b[8] = month[0];
        b[9] = month[1];
        b[10] = month[2];
        b[11] = ' ';

        y = v[0]; // year
        b[15] = (char) (y % 10 + 48);
        y /= 10;
        b[14] = (char) (y % 10 + 48);
        y /= 10;
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        b[16] = ' ';

        y = v[4]; // hour
        b[18] = (char) (y % 10 + 48);
        y /= 10;
        b[17] = (char) (y % 10 + 48);

        b[19] = ':';

        y = v[5]; // minutes
        b[21] = (char) (y % 10 + 48);
        y /= 10;
        b[20] = (char) (y % 10 + 48);

        b[22] = ':';

        y = v[6]; // seconds
        b[24] = (char) (y % 10 + 48);
        y /= 10;
        b[23] = (char) (y % 10 + 48);

        b[25] = ' ';
        b[26] = 'G';
        b[27] = 'M';
        b[28] = 'T';

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format yyyyMMddHHmmss.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format yyyyMMddHHmmss.
     */
    public static String yyyyMMddHHmmss(long l) {
        int[] v = calc(l);
        char b[] = new char[14];
        int y = v[0]; // year
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);
        y /= 10;
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1; // month
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        y = v[2] + 1; // day
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);

        y = v[4]; // hour
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);

        y = v[5]; // minute
        b[11] = (char) (y % 10 + 48);
        y /= 10;
        b[10] = (char) (y % 10 + 48);

        y = v[6]; // seconds
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format yyyyMMddHHmmssSSS.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format yyyyMMddHHmmssSSS.
     */
    public static String yyyyMMddHHmmssSSS(long l) {
        int[] v = calc(l);
        char b[] = new char[17];
        int y = v[0]; // year
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);
        y /= 10;
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1; // month
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        y = v[2] + 1; // day
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);

        y = v[4]; // hour
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);

        y = v[5]; // minute
        b[11] = (char) (y % 10 + 48);
        y /= 10;
        b[10] = (char) (y % 10 + 48);

        y = v[6]; // seconds
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        y = v[7]; // milli seconds
        b[16] = (char) (y % 10 + 48);
        y /= 10;
        b[15] = (char) (y % 10 + 48);
        y /= 10;
        b[14] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format ddMMyyyyHHmmss.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format ddMMyyyyHHmmss.
     */
    public static String ddMMyyyyHHmmss(long l) {
        int[] v = calc(l);
        char b[] = new char[14];
        int y;

        y = v[2] + 1; // day
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1; // month
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);

        y = v[0]; // year
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);
        y /= 10;
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        y = v[4]; // hour
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);

        y = v[5]; // minute
        b[11] = (char) (y % 10 + 48);
        y /= 10;
        b[10] = (char) (y % 10 + 48);

        y = v[6]; // seconds
        b[13] = (char) (y % 10 + 48);
        y /= 10;
        b[12] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format yyyyMMddHH.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format yyyyMMddHH.
     */
    public static String yyyyMMddHH(long l) {
        int[] v = calc(l);
        char b[] = new char[10];
        int y = v[0];
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);
        y /= 10;
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1;
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        y = v[2] + 1;
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);

        y = v[4];
        b[9] = (char) (y % 10 + 48);
        y /= 10;
        b[8] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format yyyyMMdd.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format yyyyMMdd.
     */
    public static String yyyyMMdd(long l) {
        int[] v = calc(l);
        char b[] = new char[8];
        int y = v[0];
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);
        y /= 10;
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1;
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        y = v[2] + 1;
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Return a String for the given date (long) in format ddMMyyyy.
     * 
     * @param l
     *            a long representing a date value
     * @return a String for the given date (long) in format ddMMyyyy.
     */
    public static String ddMMyyyy(long l) {
        int[] v = calc(l);
        char b[] = new char[8];
        int y;
        y = v[2] + 1; // day
        b[1] = (char) (y % 10 + 48);
        y /= 10;
        b[0] = (char) (y % 10 + 48);

        y = v[1] + 1; // month
        b[3] = (char) (y % 10 + 48);
        y /= 10;
        b[2] = (char) (y % 10 + 48);

        y = v[0]; // year
        b[7] = (char) (y % 10 + 48);
        y /= 10;
        b[6] = (char) (y % 10 + 48);
        y /= 10;
        b[5] = (char) (y % 10 + 48);
        y /= 10;
        b[4] = (char) (y % 10 + 48);

        return new String(b);
    }

    /**
     * Creates an array of int[8] with all time elements.
     * <ul>
     * <li>i[0] = year (1970-....)</li>
     * <li>i[1] = month (0-11)</li>
     * <li>i[2] = day (0-30)</li>
     * <li>i[3] = weekday (thursday = 0)</li>
     * <li>i[4] = hours (0-23)</li>
     * <li>i[5] = minutes (0-59)</li>
     * <li>i[6] = seconds (0-59)</li>
     * <li>i[7] = milli seconds (0-999)</li>
     * </ul>
     * .
     * 
     * @param l
     *            a long representing a date value
     * @return a new allocated array containing the calculated values
     */
    public static int[] calc(long l) {
        return calc(l, offsetSeconds);
    }

    /**
     * Creates an array of int[8] with all time elements.
     * <ul>
     * <li>i[0] = year (1970-....)</li>
     * <li>i[1] = month (0-11)</li>
     * <li>i[2] = day (0-30)</li>
     * <li>i[3] = weekday (thursday = 0)</li>
     * <li>i[4] = hours (0-23)</li>
     * <li>i[5] = minutes (0-59)</li>
     * <li>i[6] = seconds (0-59)</li>
     * <li>i[7] = milli seconds (0-999)</li>
     * </ul>
     * .
     * 
     * @param l
     *            a long representing a date value
     * @param timeZone
     *            an explicit time zone
     * @return a new allocated array containing the calculated values
     */
    public static int[] calc(long l, int timeZone) {

        int v[] = new int[8];
        v[7] = (int) (l % 1000);
        l /= 1000;

        l += timeZone;

        v[6] = (int) (l % 60); // seconds
        l /= 60;
        v[5] = (int) (l % 60); // minutes
        l /= 60;
        v[4] = (int) (l % 24); // hours

        int t = (int) (l / 24); // total days

        v[3] = (t + 4) % 7 + 1; // work day, 1 = Sun, 1 = Mon, ...

        int y = (t * 4 + 2) / 1461 + 1970; // estimate of year
        // leap days 1970/4 - 1970/100 + 1970/400 = 477

        --y;
        int nsl = y / 4 - y / 100 + y / 400 - 477; // leap days last year
        ++y;
        int ns = y / 4 - y / 100 + y / 400 - 477; // leap days this year

        v[0] = y = (t - ns) / 365 + 1970; // exact year

        int d = t - nsl - (y - 1970) * 365; // remaining days in year

        int M;
        int[] mon = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? SMONTH : MONTH;
        for (M = 0; M < 12; ++M) {
            if (d < mon[M])
                break;
            d -= mon[M];
        }
        if (M == 12) {
            d = M = 0;
            ++v[0];
        }

        v[1] = M;
        v[2] = d;
        return v;
    }

    private static long calcLong(int year, int mm, int day, int hour, int minutes, int seconds, int tzhour, int tzmin,
            int sig) {
        --year;
        int total = year / 4 - year / 100 + year / 400 - 477;
        // leap days last year
        ++year;
        int l = year / 4 - year / 100 + year / 400 - 477; // leap days this year

        if (total < l && mm > 1)
            total = l;

        for (int j = 0; j < mm; ++j)
            total += MONTH[j];

        total += 365 * (year - 1970);
        total += day - 1;

        long res = total * 24 + (hour - tzhour * sig);
        res = res * 60 + (minutes - tzmin * sig);
        res = res * 60 + seconds;

        return res * 1000;
    }

    /**
     * calculates the long value from the int array - retrieved by calc[].
     * 
     * @param n
     *            the int array as calculated by calc.
     * @return the long value.
     */
    public static long toLong(int n[]) {
        int year = n[0];
        // all values are read
        --year;
        int total = year / 4 - year / 100 + year / 400 - 477;
        // leap days last year
        ++year;
        int l = year / 4 - year / 100 + year / 400 - 477; // leap days this year

        if (total < l && n[1] > 1)
            total = l;

        for (int j = 0; j < n[1]; ++j)
            total += MONTH[j];

        total += 365 * (year - 1970);
        total += n[2];

        long res = total * 24 + n[4];
        res = res * 60 + n[5];
        res = res * 60 + n[6];

        return res * 1000 + n[7] - offsetSeconds * 1000;
    }

    /**
     * Return a long for the given date date in format <code>dd MMM yyyy HH mm ss +zz zz</code> also values without
     * timezone or without HH mm ss are parsed!
     * 
     * <pre>
     * e.g. 01/Jan/1970:00:00:01 GMT+02:00
     * e.g. 01/Jan/1970:00:00:01
     * e.g. 01/Jan/1970
     * e.g. 01Jan                    1970   00::::::00::::::01  GMT    + 02 ---- 00
     * </pre>
     * 
     * the value of delimiters is ignored!
     * 
     * @param date
     *            a date value
     * @return a long for the given date date in format <code>dd MMM yyyy HH mm ss +zz zz</code>
     */
    public static long parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(String date) {
        byte[] b = date.getBytes();
        try {
            int val[] = new int[6];

            int i = nextDigit(b, 0);
            i = nextDay(val, b, i);

            // search next uppercase letter
            while (!(b[i] >= 'A' && b[i] <= 'Z') && !(b[i] >= 'a' && b[i] <= 'z')) {
                ++i;
            }

            {
                int mm = 0;
                for (;; ++mm) {
                    if ((b[i] | 0x20) == (MONTHNAMES[mm][0] | 0x20) && (b[i + 1] | 0x20) == (MONTHNAMES[mm][1] | 0x20)
                            && (b[i + 2] | 0x20) == (MONTHNAMES[mm][2] | 0x20))
                        break;
                }
                val[1] = mm;
                i += 3;
            }

            i = nextDigit(b, i);
            i = nextYear(val, b, i);

            int sig = 1;
            int tzhour = offsetSeconds / 3600;
            int tzmin = (offsetSeconds / 60) % 60;

            // search next digit for hour
            i = nextDigit(b, i);
            if (b.length > i) {
                i = nextHour(val, b, i);

                // search next digit for minutes
                i = nextDigit(b, i);
                if (b.length > i) {
                    i = nextMinutes(val, b, i);

                    i = nextDigit(b, i);
                    if (b.length > i) {
                        i = nextSeconds(val, b, i);

                        i = skipSpaces(b, i);
                        if (isGMT(b, i)) {
                            i += 3;
                            tzhour = tzmin = 0;
                        }

                        i = nextDigitOrSign(b, i);
                        if (b.length > i) {
                            if (b[i] == '-') {
                                sig = -1;
                                ++i;
                            } else if (b[i] == '+')
                                ++i;
                            tzhour = b[i++] - '0';
                            tzmin = 0;
                            if (b[i] >= '0' && b[i] <= '9') {
                                tzhour *= 10;
                                tzhour += b[i++] - '0';
                            }

                            // search next digit#
                            i = nextDigit(b, i);
                            tzmin = b[i++] - '0';
                            if (b[i] >= '0' && b[i] <= '9') {
                                tzmin *= 10;
                                tzmin += b[i++] - '0';
                            }
                        }
                    }
                }
            }

            return calcLong(val[0], val[1], val[2], val[3], val[4], val[5], tzhour, tzmin, sig);
        } catch (Exception e) {
            //
        }
        return -1;
    }

    /**
     * @param b
     * @param i
     * @return
     */
    private static int nextDigitOrSign(byte[] b, int i) {
        // search next digit or + or -
        while (b.length > i && (b[i] < '0' || b[i] > '9') && b[i] != '+' && b[i] != '-') {
            ++i;
        }
        return i;
    }

    /**
     * @param b
     * @param i
     * @return
     */
    private final static int skipSpaces(byte[] b, int i) {
        // search next NON space
        while (i < b.length && b[i] <= 32) {
            ++i;
        }
        return i;
    }

    /**
     * Return a long for the given date date in format <code>dd MM yyyy HH mm ss +zz zz</code> also values without
     * timezone or without HH mm ss are parsed!
     * 
     * <pre>
     * e.g. 01/01/1970:00:00:01 GMT+02:00
     * e.g. 01/01/1970:00:00:01
     * e.g. 01/01/1970
     * e.g. 0101                    1970   00::::::00::::::01  GTM    + 02 ---- 00
     * </pre>
     * 
     * the value of delimiters is ignored!
     * 
     * @param date
     *            a date value
     * @return a long for the given date date in format <code>dd MMM yyyy HH mm ss +zz zz</code>
     */
    public static long parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz(String date) {
        byte[] b = date.getBytes();
        try {
            int val[] = new int[6];

            int i = nextDigit(b, 0);
            i = nextDay(val, b, i);

            i = nextDigit(b, i);
            i = nextMonth(val, b, i);

            i = nextDigit(b, i);
            i = nextYear(val, b, i);

            int sig = 1;
            int tzhour = offsetSeconds / 3600;
            int tzmin = (offsetSeconds / 60) % 60;

            // search next digit for hour
            i = nextDigit(b, i);
            if (b.length > i) {
                i = nextHour(val, b, i);

                // search next digit for minutes
                i = nextDigit(b, i);
                if (b.length > i) {
                    i = nextMinutes(val, b, i);

                    i = nextDigit(b, i);
                    if (b.length > i) {
                        i = nextSeconds(val, b, i);

                        i = skipSpaces(b, i);
                        if (isGMT(b, i)) {
                            i += 3;
                            tzhour = tzmin = 0;
                        }

                        i = nextDigitOrSign(b, i);
                        if (b.length > i) {
                            if (b[i] == '-') {
                                sig = -1;
                                ++i;
                            } else if (b[i] == '+')
                                ++i;
                            tzhour = b[i++] - '0';
                            tzmin = 0;
                            if (b[i] >= '0' && b[i] <= '9') {
                                tzhour *= 10;
                                tzhour += b[i++] - '0';
                            }

                            // search next digit
                            i = nextDigit(b, i);
                            tzmin = b[i++] - '0';
                            if (b[i] >= '0' && b[i] <= '9') {
                                tzmin *= 10;
                                tzmin += b[i++] - '0';
                            }
                        }
                    }
                }
            }

            return calcLong(val[0], val[1], val[2], val[3], val[4], val[5], tzhour, tzmin, sig);

        } catch (Exception e) {
            //
        }
        return -1;
    }

    /**
     * Return a long for the given date date in format <code>yyyy MM dd HH mm ss +zz zz</code> also values without
     * timezone or without HH mm ss are parsed!
     * 
     * <pre>
     * e.g. 1970/01/01:00:00:01 GMT+02:00
     * e.g. 1970/01/01:00:00:01
     * e.g. 1970/01/01
     * e.g. 19700101123000
     * e.g. 1970 0101  00::::::00::::::01  GTM    + 02 ---- 00
     * </pre>
     * 
     * the value of delimiters is ignored!
     * 
     * @param date
     *            a date value
     * @return a long for the given date date in format <code>dd MMM yyyy HH mm ss +zz zz</code>
     */
    public static long parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz(String date) {
        byte[] b = date.getBytes();
        try {
            int val[] = new int[6];

            int i = nextDigit(b, 0);
            i = nextYear(val, b, i);

            i = nextDigit(b, i);
            i = nextMonth(val, b, i);

            i = nextDigit(b, i);
            i = nextDay(val, b, i);

            int sig = 1;
            int tzhour = offsetSeconds / 3600;
            int tzmin = (offsetSeconds / 60) % 60;

            // search next digit for hour
            i = nextDigit(b, i);
            if (b.length > i) {
                i = nextHour(val, b, i);

                // search next digit for minutes
                i = nextDigit(b, i);
                if (b.length > i) {
                    i = nextMinutes(val, b, i);

                    i = nextDigit(b, i);
                    if (b.length > i) {
                        i = nextSeconds(val, b, i);

                        i = skipSpaces(b, i);
                        if (isGMT(b, i)) {
                            i += 3;
                            tzhour = tzmin = 0;
                        }

                        i = nextDigitOrSign(b, i);
                        if (b.length > i) {
                            try {
                                if (b[i] == '-') {
                                    sig = -1;
                                    ++i;
                                } else if (b[i] == '+')
                                    ++i;
                                tzhour = b[i++] - '0';
                                tzmin = 0;
                                if (b[i] >= '0' && b[i] <= '9') {
                                    tzhour *= 10;
                                    tzhour += b[i++] - '0';
                                }

                                // search next digit
                                i = nextDigit(b, i);

                                tzmin = b[i++] - '0';
                                if (b[i] >= '0' && b[i] <= '9') {
                                    tzmin *= 10;
                                    tzmin += b[i++] - '0';
                                }
                            } catch (Exception e) {
                                tzhour = tzmin = 0;
                            }
                        }
                    }
                }
            }

            return calcLong(val[0], val[1], val[2], val[3], val[4], val[5], tzhour, tzmin, sig);

        } catch (Exception e) {
            //
        }
        return -1;
    }

    /**
     * @param b
     * @param i
     * @return
     */
    private static boolean isGMT(byte[] b, int i) {
        if (b.length < i + 3)
            return false;
        return (b[i] | 0x20) == 'g' && (b[i + 1] | 0x20) == 'm' && (b[i + 2] | 0x20) == 't';
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextSeconds(int[] val, byte[] b, int i) {
        int seconds = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            seconds *= 10;
            seconds += b[i++] - '0';
        }
        val[5] = seconds;
        return i;
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextMinutes(int[] val, byte[] b, int i) {
        int minutes = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            minutes *= 10;
            minutes += b[i++] - '0';
        }
        val[4] = minutes;
        return i;
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextHour(int[] val, byte[] b, int i) {
        int hour = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            hour *= 10;
            hour += b[i++] - '0';
        }
        val[3] = hour;
        return i;
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextDay(int[] val, byte[] b, int i) {
        int day = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            day *= 10;
            day += b[i++] - '0';
        }
        val[2] = day;
        return i;
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextMonth(int[] val, byte[] b, int i) {
        int mm = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            mm *= 10;
            mm += b[i++] - '0';
        }
        val[1] = mm - 1;
        return i;
    }

    /**
     * @param val
     * @param b
     * @param i
     * @return
     */
    private final static int nextYear(int[] val, byte[] b, int i) {
        int year = b[i++] - '0';
        if (b[i] >= '0' && b[i] <= '9') {
            year *= 10;
            year += b[i++] - '0';
        }
        if (b[i] >= '0' && b[i] <= '9') {
            year *= 10;
            year += b[i++] - '0';
        }
        if (b[i] >= '0' && b[i] <= '9') {
            year *= 10;
            year += b[i++] - '0';
        }
        val[0] = year;
        return i;
    }

    /**
     * @param b
     * @param i
     * @return
     */
    private final static int nextDigit(byte[] b, int i) {
        // search next digit
        while (i < b.length && (b[i] < '0' || b[i] > '9')) {
            ++i;
        }
        return i;
    }

    /**
     * Calculates the easter day for a given year > 1970.
     * 
     * @param year
     *            some year
     * @return a time value for the easter day within that year in milli seconds since 01.01.1970
     */
    public static long eastern(int year) {
        // always 21.03.yyyy
        long firstSpringDay = parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz(Integer.toString(year) + "0321");
        long diff = firstSpringDay - fullMoon;
        long nextFullMoon = firstSpringDay - diff % moonMonth + moonMonth;

        // System.out.println("erster Fr???hlingsvollmond ist " +
        // EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(nextFullMoon));
        int[] v = calc(nextFullMoon); // v[3] = weekday (thursday = 0)
        int days = SUNDAY - v[3]; // 3 is sunday
        if (days <= 0)
            days += 7;
        v[2] += days;
        v[4] = v[5] = v[7] = 0;
        v[6] = 0; // -offsetSeconds;
        return toLong(v);
        /*
         * // add days and subtract hhmmss long easterDay = nextFullMoon + ((((days * 24 - v[4]) * 60 - v[5]) * 60 -
         * v[6] + offsetSeconds) 1000L); // System.out.println("Ostern ist " +
         * EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(easterDay));
         * 
         * return easterDay;
         */
    }

    /*
     * public static void main(String argso[]) { eastern(1999); eastern(2000); eastern(2001); eastern(2002);
     * 
     * long t = parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz("20000228"); System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay * 2)); t =
     * parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz("20001231"); System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay * 2)); t =
     * parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz("20010228"); System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay));
     * System.out.println(EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(t + timePerDay * 2)); }
     */

    static {
        initTimeZone();
        // an UTC time
        fullMoon = parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz("19990102014930");
    }

    /**
     * Used to read the time tone from System properties.
     */
    public static void initTimeZone() {
        // autodetect the timezone
        try {

            String stz = System.getProperty("user.timezone");
            if (DEBUG)
                System.out.println("user.timezone=" + stz);
            if (stz != null && stz.length() > 0) {
                java.util.TimeZone z = java.util.TimeZone.getTimeZone(stz);
                if (DEBUG)
                    System.out.println("" + z);

                if (z != null) {
                    if (DEBUG)
                        System.out.println("z = " + z.getRawOffset());
                    java.util.TimeZone.setDefault(z);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        offsetSeconds = tz.getRawOffset() / 1000;
        /*
         * long l = System.currentTimeMillis(); java.text.DateFormat df = new SimpleDateFormat("yyyyMMddHH"); long t0 =
         * Long.parseLong(df.format(new Date(l))); long t1 = Long.parseLong(yyyyMMddHH(l)); offsetMillis = 3600 * (int)
         * (t0 - t1);
         */
        if (DEBUG)
            System.out.println("tz = " + tz + " = " + offsetSeconds);
    }

    /**
     * / public static void main(String args[]) { long l1 = parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz("01.Jan.1970"); long l2
     * = parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("02.01.2003"); long l3 = parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("03.01.2003");
     * long l4 = parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("04.01.2003"); long l5 =
     * parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("05.01.2003"); long l6 = parse_dd_MM_yyyy_HH_mm_ss_GMT_zz_zz("06.01.2003");
     * 
     * System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l1)); System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l2));
     * System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l3)); System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l4));
     * System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l5)); System.out.println(dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(l6));
     * 
     * 
     * 
     * DateFormat df = new DateFormat("dd.MM.yy HH:mm:ss+SSS", 0);
     * System.out.println(df.format(System.currentTimeMillis())); df = new DateFormat("d.M.y H:m:s+S TZ=z", 0);
     * System.out.println(df.format(System.currentTimeMillis())); df = new DateFormat("d.MMMMMM.yyyy H:m:s+S TZ=z", 0);
     * System.out.println(df.format(System.currentTimeMillis())); df = new DateFormat("yyyyMMddHHmmssSSS", 0);
     * System.out.println(df.format(System.currentTimeMillis()));
     * System.out.println(yyyyMMddHHmmssSSS(System.currentTimeMillis()));
     * 
     * long now = System.currentTimeMillis();
     * 
     * for (int i = 0; i < 100000; ++i) { df.format(now); }
     * 
     * long diff = System.currentTimeMillis() - now; System.out.println("df: " + diff);
     * 
     * now = System.currentTimeMillis();
     * 
     * for (int i = 0; i < 100000; ++i) { yyyyMMddHHmmssSSS(now); }
     * 
     * diff = System.currentTimeMillis() - now; System.out.println("df: " + diff); } /
     **/

    /**
     * get the first day of the corresponding week.
     * 
     * @param time
     *            the time value
     * @return long time value for the first day.
     */
    public static long firstOfWeek(long time) {
        time = firstOfDay(time);

        int days = getWeekDay(time);
        days = days - MONDAY; // MONDAY = first day of week
        if (days < 0)
            days += 7;

        time -= days * TIME_PER_DAY;

        return time;
    }

    /**
     * Returns the value for date plus 1 week.
     * 
     * @param date
     *            a GMT time in milli seconds.
     * @return the value for date plus 1 week.
     */
    public static long nextWeek(long date) {
        return date + 7 * TIME_PER_DAY;
    }

    /**
     * Returns the value for date plus 1 month. If same day does not exist the last possible day is used.
     * 
     * @param date
     *            a GMT time in milli seconds
     * @return the value for date plus 1 month.
     */
    public static long nextMonth(long date) {
        return addMonths(date, 1);
    }

    /**
     * Add the count of months to the time value.
     * 
     * @param t
     *            a time value
     * @param months
     *            the count of months to add
     * @return the new time value.
     */
    public static long addMonths(long t, int months) {
        int n[] = calc(t);
        n[1] += months;
        n[0] += n[1] / 12;
        n[1] = n[1] % 12;
        if (n[1] < 0) {
            n[1] += 12;
            --n[0];
        }
        int y = n[0];
        int[] mon = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? SMONTH : MONTH;
        if (n[2] > mon[n[1]])
            n[2] = mon[n[1]];

        return toLong(n);
    }

    /**
     * Returns the value for the first day in date's month.
     * 
     * @param date
     *            a time in milli seconds
     * @return the value for the first day in date's month.
     */
    public static long firstOfMonth(long date) {
        int n[] = calc(date);
        n[2] = n[4] = n[5] = n[6] = n[7] = 0;

        return toLong(n);
    }

    /**
     * Returns the value for the first day in date's year.
     * 
     * @param date
     *            a time in milli seconds
     * @return the value for the first day in date's year.
     */
    public static long firstOfYear(long date) {
        int n[] = calc(date);
        n[1] = n[2] = n[4] = n[5] = n[6] = n[7] = 0;

        return toLong(n);
    }

    /**
     * Method getWeek returns the week number for the supplied date.
     * 
     * @param time
     *            a time in milli seconds with current timezone
     * @return int the week number of the day
     */
    public static int getWeek(long time) {
        int n[] = calc(time);
        n[1] = 0;
        n[2] = 0;
        long jan1 = toLong(n);
        int c[] = calc(jan1);
        int jan1Day = c[3]; // thursday = 5
        long firstTh = jan1 + (7 + THURSDAY - jan1Day) % 7 * TIME_PER_DAY;
        // System.out.println( dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(firstTh));
        long firstWeekDayOfYear = firstTh - 3 * TIME_PER_DAY;
        // letztes Jahr verwenden
        if (firstWeekDayOfYear > time) {
            --n[0];
            jan1 = toLong(n);
            c = calc(jan1);
            jan1Day = c[3]; // thursday = 0
            firstTh = jan1 + (7 + THURSDAY - jan1Day) % 7 * TIME_PER_DAY;
            // System.out.println( dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(firstTh));
            firstWeekDayOfYear = firstTh - 3 * TIME_PER_DAY;
        }

        long days = (time - firstWeekDayOfYear) / TIME_PER_DAY;

        // long x = (time - firstWeekDayOfYear) % TIME_PER_DAY;;

        return (int) days / 7 + 1;
    }

    /**
     * Method getDayOfMonth.
     * 
     * @param date
     *            as long
     * @return int the day of mont 1..31
     */
    public static int getDayOfMonth(long date) {
        int n[] = calc(date);
        return n[2] + 1;
    }

    /**
     * Returns the weekday, from 1-7 where 1 = Sunday.
     * 
     * @param time
     *            a time value
     * @return the weekday, from 1-7 where 1 = Sunday.
     */
    public static int getWeekDay(long time) {
        int n[] = calc(time);
        return n[3];
    }

    /**
     * Parses the date string with the current formatter.
     * 
     * @param string
     *            a date string
     * @return -1 on error or the parsed time.
     */
    public long parse(String string) {
        int v[] = new int[8];
        if (indexes.length > 0) {
            int idx = indexes[0];
            int len = lengths[0];
            int n = 1;
            int val = 0;
            boolean hit = false;

            for (int i = 0; i < string.length(); ++i) {
                int ch = string.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    val = 10 * val + ch - '0';
                    --len;
                    hit = true;
                }
                if (len == 0 || (hit && (ch < '0' || ch > '9')) || i + 1 == string.length()) {
                    v[idx] = val;
                    hit = false;
                    if (n == indexes.length)
                        break;

                    idx = indexes[n];
                    len = lengths[n++];
                    val = 0;
                }
            }
        }
        if (v[0] < 1970) {
            if (v[0] < 50)
                v[0] += 2000;
            else
                v[0] += 1900;
        }
        if (v[1] > 0)
            --v[1];
        if (v[2] > 0)
            --v[2];
        return toLong(v);
    }

    /**
     * the month the given date.
     * 
     * @param t
     *            a time value
     * @return 1-12 for the given time value.
     */
    public static int getMonth(long t) {
        int[] v = calc(t);
        return v[1] + 1;
    }

    /**
     * the year for the given time value.
     * 
     * @param t
     *            a time value
     * @return 1970-xxxx for the given time value.
     */
    public static int getYear(long t) {
        int[] v = calc(t);
        return v[0];
    }

    /**
     * Returns a time value for the specified day at 00:00:00.000 with current time zone.
     * 
     * @param t
     *            a time value
     * @return a time value for the specified day at 00:00:00.000 with current time zone.
     */
    public static long firstOfDay(long t) {
        int n[] = calc(t);
        n[4] = n[5] = n[6] = n[7] = 0;
        return toLong(n);
    }
}
