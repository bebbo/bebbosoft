/******************************************************************************
 * A String like class for direct byte manipulation.
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

import java.io.*;

/**
 * <p>
 * A String like class to manipulate byte arrays. The <b>most important</b> differences to Strings:
 * <ul>
 * <li><code>ByteRef</code> <b>share their buffer</b></li>
 * <li>the current object <b>can be modified</b> by its functions</li>
 * </ul>
 * So the user of <code>ByteRef</code> <b>must</b> handle them correctly. The result is a big speed gain in data parsing
 * and handling.
 * </p>
 * <p>
 * For not yet or bad documented functions search the comment in the String class (or guess).
 * </p>
 * 
 * @author Stefan Franke
 */
public class ByteRef implements Cloneable, Comparable<ByteRef> {
    private final static byte[] nul = new byte[0];
    private byte[] data;
    private int begin;
    private int end;

    /**
     * Initializes a newly created <code>ByteRef</code> object so that it represents an empty byte sequence.
     */
    public ByteRef() {
        data = nul;
        begin = end = 0;
    }

    /**
     * Initializes a newly created <code>ByteRef</code> object so that it represents the same sequence of bytes as the
     * argument; in other words, the newly created byte array is a copy of the argument string.getBytes().
     * 
     * @param s
     *            a <code>String</code>.
     */
    public ByteRef(String s) {
        if (s != null) {
            int len = s.length();
            char ch[] = new char[len];
            s.getChars(0, len, ch, 0);
            byte b[] = data = new byte[len];
            for (int i = 0; i < len; ++i) {
                b[i] = (byte) ch[i];
            }
            begin = 0;
            end = len;
        } else {
            data = nul;
            begin = end = 0;
        }
    }

    /**
     * Initializes a newly created <code>ByteRef</code> object so that it represents the same sequence of bytes as the
     * argument; in other words, the newly created byte array is a copy of the argument.
     * 
     * @param b
     *            a <code>byte array</code>.
     */
    public ByteRef(byte[] b) {
        this(b, 0, b.length);
    }

    /**
     * Initializes a newly created <code>ByteRef</code> object so that it represents the same sequence of bytes as the
     * argument; in other words, the newly created byte array is a copy of the specified part of the argument.
     * 
     * @param b
     *            a <code>byte array</code>.
     * @param _begin
     *            begin in the byte array
     * @param _end
     *            end in the byte array
     */
    public ByteRef(byte[] b, int _begin, int _end) {
        if (_begin < 0)
            _begin = 0;
        else if (_begin > b.length)
            _begin = b.length;

        if (_end > b.length)
            _end = b.length;

        data = b;
        begin = _begin;
        end = _end > _begin ? _end : _begin;
    }

    /**
     * Assigns the parameters physical byte array to this object.
     * 
     * @param o
     *            a <code>ByteRef</code>
     */
    public void assign(ByteRef o) {
        data = o.data;
        begin = o.begin;
        end = o.end;
    }

    /**
     * Assigns the parameters physical byte array to this object.
     * 
     * @param b
     *            a <code>byte array</code>
     * @param _begin
     *            begin in the byte array
     * @param _end
     *            end in the byte array
     */
    public final void assign(byte[] b, int _begin, int _end) {
        if (_begin < 0)
            _begin = 0;
        else if (_begin > b.length)
            _begin = b.length;

        if (_end > b.length)
            _end = b.length;

        data = b;
        begin = _begin;
        end = _end > _begin ? _end : _begin;
    }

    /**
     * Extracts the next available line from this object and returns that line as a new <code>BytRef</code> object. Unix
     * and DOS EOLs are recognized correctly.
     * 
     * @return a new allocated <code>ByteRef</code> object with the next line, or <code>null</code> if no EOL was found.
     */
    public final ByteRef nextLine() {
        byte data[] = this.data;
        for (int i = begin; i < end; ++i) {
            if (data[i] == 0xa) {
                ByteRef r;
                if (i > begin && data[i - 1] == 0xd) {
                    // DOS mode
                    r = new ByteRef(data, begin, i - 1);
                } else {
                    // unix mode
                    r = new ByteRef(data, begin, i);
                }
                begin = i + 1;
                return r;
            }
        }
        return null;
    }

    /**
     * Extracts the next available word from this object and returns that word as a new <code>BytRef</code> object. All
     * bytes < 0x20 are treated as white spaces.
     * 
     * @return a new allocated <code>ByteRef</code> object with the next word, or <code>null</code> at end of buffer.
     */
    public final ByteRef nextWord() {
        if (begin == end)
            return null;
        int i = begin;
        for (; i < end; ++i) {
            if (data[i] <= 32)
                break;
        }
        ByteRef r = new ByteRef(data, begin, i);
        while (i < end && data[i] <= 32)
            ++i;
        begin = i;
        return r;
    }

    /**
     * Extracts the content from this object up to the delimiter and returns that as a new <code>BytRef</code> object.
     * The current object is modified and contains the remaining content behind the delimiter.
     * 
     * @param delim
     *            the used delimiter
     * @return a new allocated <code>ByteRef</code> object with the next word, or <code>null</code> at end of buffer.
     */
    public final ByteRef nextWord(int delim) {
        if (begin == end)
            return null;
        int i = begin;
        for (; i < end; ++i) {
            if (data[i] == delim)
                break;
        }
        ByteRef r = new ByteRef(data, begin, i);
        while (i < end && data[i] == delim)
            ++i;
        begin = i;
        return r;
    }

    /**
     * Extracts the content from this object up to the delimiter and returns that as a new <code>BytRef</code> object.
     * The current object is modified and contains the remaining content behind the delimiter.
     * 
     * @param delim
     *            the used delimiter
     * @return a new <code>String</code> object with the next word, or <code>null</code> at end of buffer.
     */
    public final String nextWordAsString(int delim) {
        if (begin == end)
            return null;
        int i = begin;
        for (; i < end; ++i) {
            if (data[i] == delim)
                break;
        }
        int len = i - begin;
        char ch[] = new char[len];
        for (int j = 0; j < len; ++j) {
            ch[j] = (char) (0xff & data[begin + j]);
        }
        String r = new String(ch);
        while (i < end && data[i] == delim)
            ++i;
        begin = i;
        return r;
    }

    /**
     * Return the length of this<code>ByteRef</code> object.
     * 
     * @return the length of this<code>ByteRef</code> object
     */
    public final int length() {
        return end - begin;
    }

    /**
     * Copy this data into the specified <code>byte array</code>.
     * 
     * @param b
     *            a <code>byte array</code>
     */
    public final void copy(byte b[]) {
        System.arraycopy(data, begin, b, 0, end - begin);
    }

    /**
     * Copy a part of this data into the specified <code>byte array</code> at specified offset.<br>
     * If there are not enough bytes to copy, only the available bytes are copied.
     * 
     * @param b
     *            a <code>byte array</code>
     * @param off
     *            the offset into b
     * @param l
     *            the copied length
     * @return an int representing the real count of copied bytes
     */
    public final int copy(byte b[], int off, int l) {
        if (l > end - begin)
            l = end - begin;
        System.arraycopy(data, begin, b, off, l);
        return l;
    }

    /**
     * Append the specified <code>ByteRef</code> to this <code>ByteRef</code>. A new <code>ByteRef</code> is allocated
     * and returned, to store the concatenation of both <code>ByteRef</code>s.
     * 
     * @param o
     *            the appended <code>ByteRef</code>
     * @return a new <code>ByteRef</code> object with the concatenation of both <code>ByteRef</code>s.
     */
    public final ByteRef append(ByteRef o) {
        byte b[] = new byte[end - begin + o.end - o.begin];
        System.arraycopy(data, begin, b, 0, end - begin);
        System.arraycopy(o.data, o.begin, b, end - begin, o.end - o.begin);
        return new ByteRef(b);
    }

    /**
     * Append the specified <code>byte []</code>array to this <code>ByteRef</code>. A new <code>ByteRef</code> is allocated
     * and returned, to store the concatenation.
     * 
     * @param o
     *            the appended <code>byte []</code> array
     * @return a new <code>ByteRef</code> object with the concatenation of both <code>ByteRef</code>s.
     */
    public ByteRef append(byte[] data) {
        final int l = end - begin;
        byte b[] = new byte[l + data.length];
        System.arraycopy(this.data, begin, b, 0, l);
        System.arraycopy(data, 0, b, l, data.length);
        return new ByteRef(b);
    }

    
    /**
     * Append the bytes of the <code>String</code> to this <code>ByteRef</code>. A new <code>ByteRef</code> is allocated
     * and returned, to store the concatenation.
     * 
     * @param s
     *            the appended <code>String</code>
     * @return a new <code>ByteRef</code> object with the concatenation.
     */
    public final ByteRef append(String s) {
        return append(new ByteRef(s));
    }

    /**
     * Returns the <code>ByteRef</code>s data as a <code>String</code>.
     * 
     * @return the <code>ByteRef</code>s data as a <code>String</code>.
     */
    public final String toString() {
        int len = end - begin;
        char chs[] = new char[len];
        for (int i = 0; i < len; ++i)
            chs[i] = (char) (0xff & data[begin + i]);
        return new String(chs);
    }

    /**
     * Returns the <code>ByteRef</code>s data as a <code>String</code> starting from specified offset.
     * 
     * @param offset
     *            starting from offset
     * @return the <code>ByteRef</code>s data as a <code>String</code>.
     */
    public final String toString(int offset) {
        int len = end - begin - offset;
        if (len <= 0)
            return "";
        char chs[] = new char[len];
        for (int i = 0; i < len; ++i)
            chs[i] = (char) (0xff & data[begin + i + offset]);
        return new String(chs);
    }

    /**
     * Returns the <code>ByteRef</code>s data as a <code>String</code> using the specified charset.
     * 
     * @param charset
     *            the charset
     * @return the <code>ByteRef</code>s data as a <code>String</code>.
     */
    public final String toString(String charset) {
        try {
            if (charset != null)
                return new String(data, begin, end - begin, charset);
        } catch (UnsupportedEncodingException e) {
        }
        return toString();
    }

    /**
     * Returns the <code>ByteRef</code>s data as a new byte array.
     * 
     * @return the <code>ByteRef</code>s data as a new byte array.
     */
    public final byte[] toByteArray() {
        byte b[] = new byte[end - begin];
        System.arraycopy(data, begin, b, 0, b.length);
        return b;
    }

    /**
     * Converts lower case letters into lower case letters. <b>Convertion is done in place!</b>
     * 
     * @return this
     */
    public final ByteRef toUpperCase() {
        for (int i = begin; i < end; ++i) {
            byte b = data[i];
            if (b >= 'a' && b <= 'z')
                data[i] = (byte) (b - 0x20);
        }
        return this;
    }

    /**
     * Converts upper case letters into lower case letters. <b>Convertion is done in place!</b>
     * 
     * @return this
     */
    public final ByteRef toLowerCase() {
        for (int i = begin; i < end; ++i) {
            byte b = data[i];
            if (b >= 'A' && b <= 'Z')
                data[i] = (byte) (b + 0x20);
        }
        return this;
    }

    /**
     * Returns the last index of the specified byte. Search starts et end of data. Is same as br.lastIndexOf(z,
     * br.length()).
     * 
     * @param z
     *            the searched byte
     * @return the last index of the specified byte, or -1 when not found.
     */
    public final int lastIndexOf(int z) {
        return lastIndexOf(z, end - begin);
    }

    /**
     * Returns the last index of the specified byte.
     * 
     * @param z
     *            the searched byte
     * @param off
     *            the offset (from begin) where search starts
     * @return the last index of the specified byte, or -1 when not found.
     */
    public final int lastIndexOf(int z, int off) {
        byte b = (byte) z;
        for (int i = begin + off - 1; i >= begin; --i)
            if (data[i] == b)
                return i - begin;
        return -1;
    }

    /**
     * Returns the first index of the specified byte. Search starts et begin of data. Is same as br.indexOf(z, 0).
     * 
     * @param z
     *            the searched byte
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(int z) {
        return indexOf(z, 0);
    }

    /**
     * Returns the first index of the specified byte. Search starts et begin of data. Is same as br.indexOf(z, 0).
     * 
     * @param z
     *            the searched byte
     * @param off
     *            the offset (from begin) where search starts
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(int z, int off) {
        byte b = (byte) z;
        for (int i = begin + off; i < end; ++i)
            if (data[i] == b)
                return i - begin;
        return -1;
    }

    /**
     * Returns the first index of the specified String's bytes. Search starts et begin of data. Is same as br.indexOf(s,
     * 0).
     * 
     * @param s
     *            the searched String
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(String s) {
        return indexOf(s, 0);
    }

    /**
     * Returns the first index of the specified String's bytes. Search starts et begin of data. Is same as br.indexOf(s,
     * 0).
     * 
     * @param s
     *            the searched String
     * @param off
     *            the offset (from begin) where search starts
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(String s, int off) {
        byte b[] = s.getBytes();
        for (int i = begin + off; i <= end - b.length; ++i) {
            int j = 0;
            for (; j < b.length; ++j) {
                if (data[i + j] != b[j])
                    break;
            }
            if (j == b.length)
                return i - begin;
        }
        return -1;
    }

    /**
     * Returns the first index of the specified ByteRef's bytes. Search starts et begin of data.
     * 
     * @param s
     *            the searched ByteRef
     * @param off
     *            the offset (from begin) where search starts
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(ByteRef s, int off) {
        byte b[] = s.data;
        int sLen = s.length();
        for (int i = begin + off; i <= end - sLen; ++i) {
            int j = 0;
            for (; j < sLen; ++j) {
                if (data[i + j] != b[j + s.begin])
                    break;
            }
            if (j == sLen)
                return i - begin;
        }
        return -1;
    }

    /**
     * Returns the first index of the specified ByteRef's bytes. Search starts et begin of data. Is same as
     * br.indexOf(s, 0).
     * 
     * @param s
     *            the searched ByteRef
     * @return the first index of the specified byte, or -1 when not found.
     */
    public final int indexOf(ByteRef s) {
        return indexOf(s, 0);
    }

    /**
     * Returns a new BytRef object using only the specified part of this <code>ByteRef</code>.
     * 
     * @param a
     *            begin of used bytes
     * @param z
     *            end of used bytes
     * @return a new BytRef object using only the specified part of this <code>ByteRef</code>.
     */
    public final ByteRef substring(int a, int z) {
        return new ByteRef(data, begin + a, begin + z);
    }

    /**
     * Returns a new BytRef object using only the specified part of this <code>ByteRef</code>, up to end of this
     * <code>ByteRef</code>.
     * 
     * @param a
     *            begin of used bytes
     * @return a new BytRef object using only the specified part of this <code>ByteRef</code>.
     */
    public final ByteRef substring(int a) {
        return new ByteRef(data, begin + a, end);
    }

    /**
     * Returns the byte at the specified position.
     * 
     * @param idx
     *            index of the byte
     * @return the byte at the specified position.
     */
    public final int charAt(int idx) {
        if (idx < 0 || begin + idx >= end)
            return -1;
        return data[begin + idx] & 0xff;
    }

    /**
     * Compute a hashCode for use in Hashtable.
     * 
     * @return a hashcode.
     */
    public final int hashCode() {
        int hc = end - begin;
        for (int i = begin; i < end; ++i)
            hc ^= (hc << 4) ^ data[i];
        return hc;
    }

    /**
     * Compares this objects data to another object.
     * 
     * @param _o
     *            an Object
     * @return true, when other object is also a BytRef and it contains same data.
     */
    public final boolean equals(Object _o) {
        if (!(_o instanceof ByteRef)) {
            if (_o instanceof String)
                return equals(new ByteRef((String) _o));
            return false;
        }
        ByteRef o = (ByteRef) _o;
        if (o.end - o.begin != end - begin)
            return false;
        for (int i = begin, j = o.begin; i < end; ++i, ++j)
            if (data[i] != o.data[j])
                return false;
        return true;
    }

    /**
     * Compares this objects data to another object ignoring upper and lower case.
     * 
     * @param _o
     *            an Object
     * @return true, when other object is also a BytRef and it contains same data.
     */
    public final boolean equalsIgnoreCase(Object _o) {
        if (!(_o instanceof ByteRef)) {
            if (_o instanceof String)
                return equalsIgnoreCase(new ByteRef((String) _o));
            return false;
        }
        ByteRef o = (ByteRef) _o;
        if (o.end - o.begin != end - begin)
            return false;
        for (int i = begin, j = o.begin; i < end; ++i, ++j) {
            byte a = data[i];
            if (a >= 'a' && a <= 'z')
                a = (byte) (a - 0x20);
            byte b = o.data[j];
            if (b >= 'a' && b <= 'z')
                b = (byte) (b - 0x20);

            if (a != b)
                return false;
        }
        return true;
    }

    /**
     * Compares this <code>ByteRef</code>'s data to another <code>ByteRef</code>.
     * 
     * @param o
     *            a ByteRef object
     * @return 0, when other <code>ByteRef</code> contains same data, -1 when this is less and 1 when this is greater
     *         then the other <code>ByteRef</code>.
     */
    public final int compareTo(ByteRef o) {
        for (int i = begin, j = o.begin; i < end && j < o.end; ++i, ++j) {
            int diff = data[i] - o.data[j];
            if (diff != 0)
                return diff;
        }
        return begin - end - o.begin + o.end;
    }

    /**
     * Tests whether this BytRef ends with the specified <code>ByteRef</code>.
     * 
     * @param o
     *            the other <code>ByteRef</code> where the end is tested against
     * @return true if this BytRef ends with the specified <code>ByteRef</code>
     */
    public final boolean endsWith(ByteRef o) {
        int i = end + o.begin - o.end, j = o.begin;
        if (i < begin)
            return false;
        for (; i < end; ++i, ++j) {
            int diff = data[i] - o.data[j];
            if (diff != 0)
                return false;
        }
        return true;
    }

    /**
     * Tests whether this BytRef ends with the specified String.
     * 
     * @param s
     *            the other String where the end is tested against
     * @return true if this BytRef ends with the specified <code>String</code>
     */
    public final boolean endsWith(String s) {
        return endsWith(new ByteRef(s));
    }

    /**
     * Tests whether this BytRef starts with the specified <code>ByteRef</code>.
     * 
     * @param o
     *            the other <code>ByteRef</code> where the end is tested against
     * @return true if this BytRef starts with the specified <code>ByteRef</code>
     */
    public final boolean startsWith(ByteRef o) {
        int i = begin, j = o.begin;
        for (; i < end && j < o.end; ++i, ++j) {
            int diff = data[i] - o.data[j];
            if (diff != 0)
                return false;
        }
        return j == o.end;
        // return substring(0, o.length()).compareTo(o) == 0;
    }

    /**
     * Tests whether this BytRef starts with the specified String.
     * 
     * @param s
     *            the other String where the end is tested against
     * @return true if this BytRef starts with the specified <code>String</code>
     */
    public final boolean startsWith(String s) {
        return startsWith(new ByteRef(s));
    }

    /**
     * Write the bytes to the specified output stream.
     * 
     * @param os
     *            a output stream
     * @throws IOException
     *             on io errors
     */
    public final void writeTo(OutputStream os) throws IOException {
        os.write(data, begin, end - begin);
    }

    /**
     * Remove all occurencies of the specified charater at begin and end.
     * 
     * @param ch
     *            a character value
     * @return a new <code>ByteRef</code> with the trimed data.
     */
    public final ByteRef trim(int ch) {
        int b = begin, e = end;
        while (b < e && (data[b] & 0xff) == ch)
            ++b;
        while (b < e && (data[e - 1] & 0xff) == ch)
            --e;
        return new ByteRef(data, b, e);
    }

    /**
     * Remove all white spaces at begin and end.
     * 
     * @return a new <code>ByteRef</code> with the trimed data.
     */
    public final ByteRef trim() {
        int b = begin, e = end;
        while (b < e && (data[b] & 0xff) <= 32)
            ++b;
        while (b < e && (data[e - 1] & 0xff) <= 32)
            --e;
        return new ByteRef(data, b, e);
    }

    /**
     * Interpret the bytes as characters and convert them into an int value.
     * This method stops interpreting if a non digit is hit.
     * 
     * @return an int value
     */
    public final int toInteger() {
        int z = 0;
        for (int i = begin; i < end; ++i) {
            int c = data[i] & 0xff;
            if (c < '0' || c > '9')
                break;
            z *= 10;
            z += c - '0';
        }
        return z;
    }

    /**
     * Interpret the bytes as characters and convert them into an long value.
     * This method stops interpreting if a non digit is hit.
     * 
     * @return a long value
     */
    public long toLong() {
        long z = 0;
        for (int i = begin; i < end; ++i) {
            int c = data[i] & 0xff;
            if (c < '0' || c > '9')
                break;
            z *= 10;
            z += c - '0';
        }
        return z;
    }

    /**
     * Update this <code>ByteRef</code> with the available data of the specified input stream. <b>current object's data
     * is also updated!</b>.
     * 
     * @param is
     *            an input stream
     * @return this on success with old and appended new data, or null on failure.
     */
    public final ByteRef update(InputStream is) {
        try {
            // try read a single byte
            int firstByte = is.read();
            if (firstByte == -1)
                return null;

            // old length + already read byte
            int baseLen = length() + 1;

            // how many bytes are available?
            int avail = is.available();
            // limit read ahead
            if (avail > 0x2000) {
                if (baseLen > 0x2000) {
                    if (avail > baseLen)
                        avail = baseLen;
                } else {
                    avail = 0x2000;
                }
            }

            byte[] r = new byte[baseLen + avail];
            // copy existing data to buffer
            copy(r);
            // add single read byte
            r[baseLen - 1] = (byte) firstByte;

            // how many bytes are really read?
            int count = 0;
            if (avail > 0) {
                count = is.read(r, baseLen, avail);
            }
            data = r;
            begin = 0;
            end = baseLen + count;
            // assign(r, 0, baseLen + count);
            return this;
        } catch (Exception e) {//
        }
        return null;
    }

    /**
     * Update this <code>ByteRef</code> with the availabledata of the specified input stream. <b>current object's data
     * is also updated!</b>.
     * 
     * @param raf
     *            an input stream
     * @return this on success with old and appended new data, or null on failure.
     */
    public final ByteRef update(RandomAccessFile raf) {
        try {
            // old length
            int baseLen = length();

            // how many bytes are available?
            long avail = raf.length() - raf.getFilePointer();
            if (avail == 0)
                return null;

            // limit readahaed
            if (avail > 0x2000) {
                if (baseLen > 0x2000) {
                    if (avail > baseLen)
                        avail = baseLen;
                } else {
                    avail = 0x2000;
                }
            }

            byte[] r = new byte[baseLen + (int) avail];
            // copy existing data to buffer
            copy(r);

            // how many bytes are really read?
            int count = 0;
            if (avail > 0) {
                count = raf.read(r, baseLen, (int) avail);
            }
            data = r;
            begin = 0;
            end = baseLen + count;
            // assign(r, 0, baseLen + count);
            return this;
        } catch (Exception e) {//
        }
        return null;
    }

    /**
     * Extracts the next line from the specified <code>ByteRef</code> br. If there is no complete line in br, the
     * <code>ByteRef</code> is updated using the specified input stream. This is done until a complete line is read, or
     * br.update(is) fails.
     * 
     * @param br
     *            the <code>ByteRef</code> used as input buffer
     * @param is
     *            a input stream
     * @return a new <code>ByteRef</code> containing the next line, or null on EOS.
     */
    public final static ByteRef readLine(ByteRef br, InputStream is) {
        // get request line
        while (br != null) {
            // get the next line
            // if (DEBUG) System.out.println("C: " + br.toString());
            ByteRef req = br.nextLine();
            if (req != null)
                return req;
            br = br.update(is);
        }
        return null;
    }

    public final static ByteRef readLine(ByteRef br, RandomAccessFile is) {
        // get request line
        while (br != null) {
            // get the next line
            // if (DEBUG) System.out.println("C: " + br.toString());
            ByteRef req = br.nextLine();
            if (req != null)
                return req;
            br = br.update(is);
        }
        return null;
    }

    public final ByteRef clone() {
        return new ByteRef(this.toByteArray());
    }

    /**
     * Returns a ByteRef starting from 0 to stop, and removes this part from the current ByteRef. This ByteRef contains
     * only the content from stop to end.
     * 
     * This is a self modifying method!!
     * 
     * @param stop
     *            end position
     * @return a ByteRef as substring(0, stop) but modifies this object.
     */
    public final ByteRef splitLeft(int stop) {
        return splitLeft(0, stop);
    }

    /**
     * Returns a ByteRef starting from start to stop, and removes this part from the current ByteRef. This ByteRef
     * contains only the content from stop to end.
     * 
     * This is a self modifying method!!
     * 
     * @param start
     *            start position for returned ByteRef
     * @param stop
     *            stop position for returned ByteRef
     * @return a ByteRef as substring(start, stop) but modifies this object.
     */
    public final ByteRef splitLeft(int start, int stop) {
        if (stop < 0)
            stop = 0;
        ByteRef r = substring(start, stop);
        begin += stop;
        if (begin > end)
            begin = end;
        return r;
    }

    /**
     * Remove the first byte and return it as int >= 0 or return -1; This is a self modification.
     * 
     * @return the first byte and return it as int >= 0 or return -1;
     */
    public final int removeLeft() {
        if (begin == end)
            return -1;
        return 0xff & data[begin++];
    }

    /**
     * Move the left offset by count bytes. This is a self modification.
     * 
     * @param count
     *            the count of bytes to remove from left side.
     */
    public final void adjustLeft(int count) {
        begin += count;
        if (begin > end)
            begin = end;
    }

    /**
     * Extracts the next available line from this object and returns that line as a new <code>BytRef</code> object. Only
     * DOS EOLs are recognized correctly.
     * 
     * @return a new allocated <code>ByteRef</code> object with the next line, or <code>null</code> if no EOL was found.
     */
    public final ByteRef nextLineCRLF() {
        byte data[] = this.data;
        for (int i = begin; i < end; ++i) {
            byte b = data[i];
            // DOS mode
            if (b == 0xd) {
                int lineEnd = i;
                ++i;
                if (i < end && data[i] == 0xa) {
                    ByteRef r = new ByteRef(data, begin, lineEnd);
                    begin = i + 1;
                    return r;
                }
            }
        }
        return null;
    }
}

