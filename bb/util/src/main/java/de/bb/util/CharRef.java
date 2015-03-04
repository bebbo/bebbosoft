/******************************************************************************
 * $Source: /export/CVS/java/bb_util/src/main/java/de/bb/util/CharRef.java,v $
 * $Revision: 1.2 $
 * $Date: 2008/03/15 18:01:05 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * A String like class for direct char manipulation.
 * Since java.nio this might look outdated, but there are still devices
 * where java.nio does not exist
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
 * <p>A String like class to manipulate byte arrays.
 * The <b>most important</b> differences to Strings:
 * <ul><li><code>CharRef</code> <b>share their buffer</b></li>
 * <li>the current object <b>can be modified</b> by its functions</li></ul>
 * So the user of <code>CharRef</code> <b>must</b> handle them correctly. The result is a big speed gain in data parsing and handling.
 * </p><p>
 * For not yet or bad documented functions search the comment in the String class (or guess).
 * </p>
 *
 * @author Stefan Franke
 */
public class CharRef implements Cloneable
{
  private final static char [] nul = new char[0];
  private char [] data;
  private int begin;
  private int end;

  /**
   * Initializes a newly created <code>CharRef</code> object so that it
   * represents an empty byte sequence.
   */
  public CharRef()
  {
    data = nul;
    begin = end = 0;
  }
  /**
   * Initializes a newly created <code>CharRef</code> object so that it
   * represents the same sequence of bytes as the argument; in other
   * words, the newly created byte array is a copy of the argument string.getChars().
   *
   * @param   s   a <code>String</code>.
   */
  public CharRef(String s)
  {
    if (s != null)
    {
      data = new char[s.length()];
      s.getChars(0, s.length(), data, 0);
      begin = 0;
      end = data.length;
    } else {
      data = nul;
      begin = end = 0;
    }
  }
  /**
   * Initializes a newly created <code>CharRef</code> object so that it
   * represents the same sequence of bytes as the argument; in other
   * words, the newly created byte array is a copy of the argument.
   *
   * @param   b   a <code>char array</code>.
   */
  public CharRef(char [] b)
  {
    this(b, 0, b.length);
  }
  /**
   * Initializes a newly created <code>CharRef</code> object so that it
   * represents the same sequence of chars as the argument; in other
   * words, the newly created char array is a copy of the specified part of the argument.
   *
   * @param   b   a <code>char array</code>.
   * @param _begin begin in the char array
   * @param _end   end in the char array
   */
  public CharRef(char [] b, int _begin, int _end)
  {
    assign(b, _begin, _end);
  }
  /**
   * Assigns the parameters physical char array to this object.
   * @param o a <code>CharRef</code>
   */
  public void assign(CharRef o)
  {
    data = o.data;
    begin = o.begin;
    end = o.end;
  }
  /**
   * Assigns the parameters physical char array to this object.
   * @param b a <code>char array</code>
   * @param _begin begin in the char array
   * @param _end   end in the char array
   */
  public void assign(char [] b, int _begin, int _end)
  {
    if (_begin < 0)
      _begin = 0;
    else
    if (_begin > b.length)
      _begin = b.length;
      
    if (_end > b.length)
      _end = b.length;
      
    data = b;
    begin = _begin;
    end = _end > _begin ? _end : _begin;
  }
  /**
   * Extracts the next available line from this object and returns that line as a new <code>BytRef</code> object.
   * Unix and DOS EOLs are recognized correctly. MAC EOLs will fail at end of buffer!
   * @return a new allocated <code>CharRef</code> object with the next line, or <code>null</code> if no EOL was found.
   */
  public CharRef nextLine()
  {
    for (int i = begin; i < end; ++i)
    {
      // DOS mode
      if (data[i] == 0xd)
      {
        // cannot determine the real end
        if (i+1 == end)
          return null;

        CharRef r = new CharRef(data, begin, i);
        ++i;
        if (i < end && data[i] == 0xa) // MAC does it without 0xa
          ++i;
        begin = i;
        return r;
      }
      // unix mode
      if (data[i] == 0xa)
      {
        CharRef r = new CharRef(data, begin, i);
        begin = i+1;
        return r;
      }
    }
    return null;
  }
  /**
   * Extracts the next available word from this object and returns that word as a new <code>BytRef</code> object.
   * All chars < 0x20 are treated as white spaces.
   * @return a new allocated <code>CharRef</code> object with the next word, or <code>null</code> at end of buffer.
   */
  public CharRef nextWord()
  {
    if (begin == end)
      return null;
    int i = begin;
    for (; i < end; ++i)
    {
      if (data[i] <= 32)
        break;
    }
    CharRef r = new CharRef(data, begin, i);
    while (i < end && data[i] <= 32)
      ++i;
    begin = i;
    return r;
  }

  /**
   * Extracts the next available word from this object and returns that word as a new <code>BytRef</code> object.
   * @param delim the used delimiter
   * @return a new allocated <code>CharRef</code> object with the next word, or <code>null</code> at end of buffer.
   */
  public CharRef nextWord(int delim)
  {
    if (begin == end)
      return null;
    int i = begin;
    for (; i < end; ++i)
    {
      if (data[i] == delim)
        break;
    }
    CharRef r = new CharRef(data, begin, i);
    while (i < end && data[i] == delim)
      ++i;
    begin = i;
    return r;
  }

  /**
   * Return the length of this<code>CharRef</code> object.
   * @return the length of this<code>CharRef</code> object
   */
  public int length()
  {
    return end - begin;
  }
  
  /**
   * Copy this data into the specified <code>char array</code>.
   * @param b a <code>char array</code>
   */
  public void copy(byte b[])
  {
    for (int j = 0, i = begin; i < end; ++i)
    {
      if (j >= b.length)
        break;
      char c1 = (char)(0xff & b[j++]);
      if (j >= b.length)
        break;
      char c2 = (char)(0xff & b[j++]);
      data[i] = (char) (c2<<8 | c1);
    }
  }
  
  /**
   * Copy this data into the specified <code>char array</code>.
   * @param b a <code>char array</code>
   */
  public void copy(char b[])
  {
    System.arraycopy(data, begin, b, 0, end - begin);
  }
  /**
   * Copy a part of this data into the specified <code>char array</code> at specified offset.<br>
   * If there are not enough chars to copy, only the available chars are copied.
   * @param b a <code>char array</code>
   * @param off the offset into b
   * @param l the copied length
   * @return an int representing the real count of copied chars
   */
  public int copy(char b[], int off, int l)
  {
    if (l > end - begin)
      l = end - begin;
    System.arraycopy(data, begin, b, off, l);
    return l;
  }
  /**
   * Append the specified <code>CharRef</code> to this <code>CharRef</code>.
   * A new <code>CharRef</code> is allocated and returned, to store the concatenation of both <code>CharRef</code>s.
   * @param o the appended <code>CharRef</code>
   * @return a new <code>CharRef</code> object with the concatenation of both <code>CharRef</code>s.
   */
  public CharRef append(CharRef o)
  {
    char b[] = new char[end - begin + o.end - o.begin];
    System.arraycopy(data, begin, b, 0, end - begin);
    System.arraycopy(o.data, o.begin, b, end - begin, o.end - o.begin);
    return new CharRef(b);
  }
  /**
   * Append the chars of the <code>String</code> to this <code>CharRef</code>.
   * A new <code>CharRef</code> is allocated and returned, to store the concatenation.
   * @param s the appended <code>String</code>
   * @return a new <code>CharRef</code> object with the concatenation.
   */
  public CharRef append(String s)
  {
    return append(new CharRef(s));
  }
  /**
   * Returns the <code>CharRef</code>s data as a <code>String</code>.
   * @return the <code>CharRef</code>s data as a <code>String</code>.
   */
  public String toString()
  {
    return new String(data, begin, end - begin);
  }
  /**
   * Returns the <code>CharRef</code>s data as a new char array.
   * @return the <code>CharRef</code>s data as a new char array.
   */
  public char [] toCharArray()
  {
    char b[] = new char[end-begin];
    System.arraycopy(data, begin, b, 0, b.length);
    return b;
  }
  /**
   * Converts lower case letters into lower case letters.
   * <b>Convertion is done in place!</b>
   * @return this
   */
  public CharRef toUpperCase()
  {
    for (int i = begin, j = 0; i < end; ++i, ++j)
    {
      char b = data[i];
      if (b >= 'a' && b <= 'z')
        data[i] = (char)(b - 0x20);
    }
    return this;
  }
  /**
   * Converts upper case letters into lower case letters.
   * <b>Convertion is done in place!</b>
   * @return this
   */
  public CharRef toLowerCase()
  {
    for (int i = begin, j = 0; i < end; ++i, ++j)
    {
      char b = data[i];
      if (b >= 'A' && b <= 'Z')
        data[i] = (char)(b + 0x20);
    }
    return this;
  }
  /**
   * Returns the last index of the specified char. Search starts et end of data.
   * Is same as br.lastIndexOf(z, br.length()).
   * @param z the searched char
   * @return the last index of the specified char, or -1 when not found.
   */
  public int lastIndexOf(int z)
  {
    return lastIndexOf(z, end - begin);
  }
  /**
   * Returns the last index of the specified char.
   * @param z the searched char
   * @param off the offset (from begin) where search starts
   * @return the last index of the specified char, or -1 when not found.
   */
  public int lastIndexOf(int z, int off)
  {
    char b = (char)z;
    for (int i = begin + off - 1; i >= begin; --i)
      if (data[i] == b)
        return i - begin;
    return -1;
  }
  /**
   * Returns the first index of the specified char. Search starts et begin of data.
   * Is same as br.indexOf(z, 0).
   * @param z the searched char
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(int z)
  {
    return indexOf(z, 0);
  }
  /**
   * Returns the first index of the specified char. Search starts et begin of data.
   * Is same as br.indexOf(z, 0).
   * @param z the searched char
   * @param off the offset (from begin) where search starts
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(int z, int off)
  {
    char b = (char)z;
    for (int i = begin + off; i < end; ++i)
      if (data[i] == b)
        return i - begin;
    return -1;
  }
  /**
   * Returns the first index of the specified String's chars. Search starts et begin of data.
   * Is same as br.indexOf(s, 0).
   * @param s the searched String
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(String s)
  {
    return indexOf(s, 0);
  }
  /**
   * Returns the first index of the specified String's chars. Search starts et begin of data.
   * Is same as br.indexOf(s, 0).
   * @param s the searched String
   * @param off the offset (from begin) where search starts
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(String s, int off)
  {
    char b[] = new char[s.length()];
    s.getChars(0, s.length(), b, 0);
    for (int i = begin + off; i <= end - b.length; ++i)
    {
      int j = 0;
      for (; j < b.length; ++j)
      {
        if (data[i+j] != b[j])
          break;
      }
      if (j == b.length)
        return i - begin;
    }
    return -1;
  }
  /**
   * Returns the first index of the specified CharRef's chars. Search starts et begin of data.
   * @param s the searched CharRef
   * @param off the offset (from begin) where search starts
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(CharRef s, int off)
  {
    char b[] = s.data;
    for (int i = begin + off; i <= end - s.length(); ++i)
    {
      int j = 0;
      for (; j < s.length(); ++j)
      {
        if (data[i+j] != b[j + s.begin])
          break;
      }
      if (j == s.length())
        return i - begin;
    }
    return -1;
  }
  /**
   * Returns the first index of the specified CharRef's chars. Search starts et begin of data.
   * Is same as br.indexOf(s, 0).
   * @param s the searched CharRef
   * @return the first index of the specified char, or -1 when not found.
   */
  public int indexOf(CharRef s) {
    return indexOf(s, 0);
  }

  /**
   * Returns a new BytRef object using only the specified part of this <code>CharRef</code>.
   * @param a begin of used chars
   * @param z end of used chars
   * @return a new BytRef object using only the specified part of this <code>CharRef</code>.
   */
  public CharRef substring(int a, int z)
  {
    return new CharRef(data, begin + a, begin + z);
  }
  /**
   * Returns a new BytRef object using only the specified part of this <code>CharRef</code>, up to end of this <code>CharRef</code>.
   * @param a begin of used chars
   * @return a new BytRef object using only the specified part of this <code>CharRef</code>.
   */
  public CharRef substring(int a)
  {
    return new CharRef(data, begin + a, end);
  }
  /**
   * Returns the char at the specified position.
   * @param idx index of the char
   * @return  the char at the specified position.
   */
  public int charAt(int idx)
  {
    if (idx < 0 || begin + idx >= end)
      return -1;
    return data[begin + idx] & 0xff;
  }
  /**
   * Compute a hashCode for use in Hashtable.
   * @return a hashcode.
   */
  public int hashCode()
  {
    int hc = end - begin;
    for (int i = begin; i < end; ++i)
      hc ^= (hc<<4)^data[i];
    return hc;
  }
  /**
   * Compares this objects data to another object.
   * @param _o  an Object
   * @return true, when other object is also a BytRef and it contains same data.
   */
  public boolean equals(Object _o)
  {
    if (! (_o instanceof CharRef))
    {
      if (_o instanceof String)
        return equals(new CharRef((String)_o));
      return false;
    }
    CharRef o = (CharRef)_o;
    if (o.end - o.begin != end - begin)
      return false;
    for (int i = begin, j = o.begin; i < end; ++i, ++j)
      if (data[i] != o.data[j])
        return false;
    return true;
  }
  /**
   * Compares this objects data to another object ignoring upper and lower case.
   * @param _o  an Object
   * @return true, when other object is also a BytRef and it contains same data.
   */
  public boolean equalsIgnoreCase(Object _o)
  {
    if (! (_o instanceof CharRef))
    {
      if (_o instanceof String)
        return equalsIgnoreCase(new CharRef((String)_o));
      return false;
    }
    CharRef o = (CharRef)_o;
    if (o.end - o.begin != end - begin)
      return false;
    for (int i = begin, j = o.begin; i < end; ++i, ++j)
    {
      char a = data[i];
      if (a >= 'a' && a <= 'z')
        a = (char)(a - 0x20);
      char b = o.data[j];
      if (b >= 'a' && b <= 'z')
        b = (char)(b - 0x20);

      if (a != b)
        return false;
    }
    return true;
  }
  /**
   * Compares this <code>CharRef</code>'s data to another <code>CharRef</code>.
   * @param o  a CharRef object
   * @return 0, when other <code>CharRef</code> contains same data, -1 when this is less and 1 when this is greater then the other <code>CharRef</code>.
   */
  public int compareTo(CharRef o)
  {
    for (int i = begin, j = o.begin; i < end && j < o.end; ++i, ++j)
    {
      int diff = data[i] - o.data[j];
      if (diff != 0)
        return diff;
    }
    return begin - end - o.begin + o.end;
  }
  /**
   * Tests whether this BytRef ends with the specified <code>CharRef</code>.
   * @param o the other <code>CharRef</code> where the end is tested against
   * @return true if this BytRef ends with the specified <code>CharRef</code>
   */
  public boolean endsWith(CharRef o)
  {
    int i = end + o.begin - o.end, j = o.begin;
    if (i < begin)
      return false;
    for (; i < end; ++i, ++j)
    {
      int diff = data[i] - o.data[j];
      if (diff != 0)
        return false;
    }
    return true;  
  }
  /**
   * Tests whether this BytRef ends with the specified String.
   * @param s the other String where the end is tested against
   * @return true if this BytRef ends with the specified <code>String</code>
   */
  public boolean endsWith(String s)
  {
    return endsWith(new CharRef(s));
  }

  /**
   * Tests whether this BytRef starts with the specified <code>CharRef</code>.
   * @param o the other <code>CharRef</code> where the end is tested against
   * @return true if this BytRef starts with the specified <code>CharRef</code>
   */
  public boolean startsWith(CharRef o)
  {
    int i = begin, j = o.begin;
    for (; i < end && j < o.end; ++i, ++j)
    {
      int diff = data[i] - o.data[j];
      if (diff != 0)
        return false;
    }
    return j == o.end;  
//    return substring(0, o.length()).compareTo(o) == 0;
  }
  /**
   * Tests whether this BytRef starts with the specified String.
   * @param s the other String where the end is tested against
   * @return true if this BytRef starts with the specified <code>String</code>
   */
  public boolean startsWith(String s)
  {
    return startsWith(new CharRef(s));
  }


  /**
   * Write the chars to the specified output stream.
   * @param os a output stream
   * @throws IOException on io errors
   */
  public void writeTo(OutputStream os) throws IOException
  {
   // os.write(data, begin, end - begin);
    for (int i = begin; i < end; ++i) {
      char c = data[i];
      os.write((byte)c);
      os.write((byte)c>>8);
    }
  }
  /**
   * Remove all occurencies of the specified charater at begin and end.
   * @param ch a character value
   * @return a new <code>CharRef</code> with the trimed data.
   */
  public CharRef trim(int ch)
  {
    int b = begin, e = end;
    while (b < e && (data[b]&0xff) == ch)
      ++b;
    while (b < e && (data[e-1]&0xff) == ch)
      --e;
    return new CharRef(data, b, e);
  }
  /**
   * Remove all white spaces at begin and end.
   * @return a new <code>CharRef</code> with the trimed data.
   */
  public CharRef trim()
  {
    int b = begin, e = end;
    while (b < e && (data[b]&0xff) <= 32)
      ++b;
    while (b < e && (data[e-1]&0xff) <= 32)
      --e;
    return new CharRef(data, b, e);
  }
  /**
   * Interpret the chars as characters and convert them into an int value.
   * @return an int value
   */
  public int toInteger()
  {
    int z = 0;
    for (int i = begin; i < end; ++i)
    {
      int c = data[i]&0xff;
      if (c < '0' || c > '9')
        break;
      z *= 10;
      z += c - '0';
    }
    return z;
  }
    
  /**
   * Update this <code>CharRef</code> with the availabledata of the specified input stream.
   * <b>current object's data is also updated!</b>.
   * @param is an input stream
   * @return this on success with old and appended new data, or null on failure.
   */
  public CharRef update(InputStream is)
  {
    try 
    {
      // try read a single char
      int fb1 = is.read();
      if (fb1 == -1)
        return null;
      int fb2 = is.read();
      if (fb2 == -1)
        return null;
        
      // how many chars are available?
      int len = is.available();
      // old length + already read char
      int len2 = length() + 1;

      // limit readahaed      

      if (len2 < 0x4000) {
        len = 0x4000 < len ? 0x4000 : len;
      } else {
        if (len > len2*2) {
          len = len2*2;
        }
      }
      
      byte [] r = new byte[2 * (len2 + len)];
      
      // copy existing data to buffer
      copy(r);
      // add single read char
      r[len2*2-2] = (byte)fb1; 
      r[len2*2-1] = (byte)fb2; 
      
      if (len > 0) {
        len = is.read(r, len2*2, len*2) / 2;
      }
      assign(byte2char(r), 0, len2 + len);
      return this;
    } catch (Exception e)
    {//
    }
    return null;
  }
  private char[] byte2char(byte[] r) {
    char [] cr = new char[r.length >> 1];
    for (int j = 0, i = 0; i < r.length;) {
      char c1 = (char)(0xff & r[i++]);
      char c2 = (char)(0xff & r[i++]);
      cr[j++] = (char)(c1 | c2<<8);
    }
    return cr;
  }
  /**
   * Extracts the next line from the specified <code>CharRef</code> br.
   * If there is no complete line in br, the <code>CharRef</code> is updated using the specified input stream.
   * This is done until a complete line is read, or br.update(is) fails.
   * @param br the <code>CharRef</code> used as input buffer
   * @param is a input stream
   * @return a new <code>CharRef</code> containing the next line, or null on EOS.
   */
  public static CharRef readLine(CharRef br, InputStream is)
  {
    // get request line
    CharRef req = null;
    while(br != null && req == null)
    {
      // get the next line
      // if (DEBUG) System.out.println("C: " + br.toString());
      req = br.nextLine();
      if (req == null)
        br = br.update(is);
    }
    return req;
  }

  public Object clone()
  {
    return new CharRef(this.toCharArray());
  }
  /**
   * Returns a CharRef starting from 0 to stop, 
   * and removes this part from the current CharRef.
   * This CharRef contains only the content from stop to end.
   * 
   * This is a self modifying method!!
   * @param stop end position 
   * @return a CharRef as substring(0, stop) but modifies this object.
   */
  public CharRef splitLeft(int stop) {
    return splitLeft(0, stop);
  }

  /**
   * Returns a CharRef starting from start to stop, 
   * and removes this part from the current CharRef.
   * This CharRef contains only the content from stop to end.
   * 
   * This is a self modifying method!!
   * @param start start position for returned CharRef
   * @param stop stop position for returned CharRef
   * @return a CharRef as substring(start, stop) but modifies this object.
   */
  public CharRef splitLeft(int start, int stop) {
    if (stop < 0)
      stop = 0; 
    CharRef r = substring(start, stop);
    begin += stop;
    if (begin > end)
      begin = end;
    return r;
  }
}

/******************************************************************************
 * $Log: CharRef.java,v $
 * Revision 1.2  2008/03/15 18:01:05  bebbo
 * @R Changed the license: From now on GPL 3 applies.
 *
 * Revision 1.1  2006/02/02 07:51:44  bebbo
 * @N same as ByteRef for unicode chars
 *
 *****************************************************************************/