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

package de.bb.bejy.http.jsp;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

class JspWriterImpl extends JspWriter {
  private Writer osw;
  private HttpServletResponse response;
  private CharArrayWriter caw = new CharArrayWriter();
  private int remaining;
  private boolean flushed = false;
  private boolean closed = false;

  /**
   * @param response
   * @param bufferSize
   * @param autoFlush
   */
  public JspWriterImpl(
    HttpServletResponse response,
    int bufferSize,
    boolean autoFlush) {
    super(bufferSize, autoFlush);
    this.response = response;
    remaining = bufferSize;

  }

  public void write(char[] ch, int off, int len) throws IOException {
    checkClosed();

    // write without page buffer
    if (bufferSize <= 0) {
      checkOsw();
      osw.write(ch, off, len);
      return;
    }

    // no fit into buffer and no autoFlush?
    if (len > remaining && !autoFlush)
      throw new IOException("buffer overflow");

    while (len > 0) {
      // how many bytes fit into buffer
      int btw = len >= remaining ? remaining : len;
      len -= btw;
      remaining -= btw;
      caw.write(ch, off, btw);
      off += btw;

      if (len > 0)
        flush();
    }
  }
  /**
   * 
   */
  private void checkClosed() throws IOException {
    if (closed)
      throw new IOException("stream IS closed");
  }
  private void checkOsw() throws IOException {
    if (osw == null) {
      osw = response.getWriter();
    }
  }

  //        Clear the contents of the buffer.
  public void clear() throws IllegalStateException {
    if (flushed)
      throw new IllegalStateException("buffer was already flushed");
    caw.reset();
    remaining = bufferSize;
    closed = false;
  }
  //        Clears the current contents of the buffer.
  public void clearBuffer() {
    caw.reset();
    remaining = bufferSize;
  }
  //        Close the stream, flushing it first.
  public void close() throws IOException {
    flush();
    closed = true;
    caw.close();
  }
  //        Flush the stream.
  public void flush() throws IOException {
    checkClosed();
    checkOsw();
    flushed = true;
    osw.write(caw.toCharArray());
    caw.reset();
    if (autoFlush) {
      remaining = bufferSize;
    }
  }

  public int getRemaining() {
    return remaining;
  }

  //        Write a line separator.
  public void newLine() throws IOException {
    print("\r\n");
  }
  //        Print a boolean value.
  public void print(boolean b) throws IOException {
    print("" + b);
  }
  //        Print a character.
  public void print(char c) throws IOException {
    print("" + c);
  }
  //        Print an array of characters.
  public void print(char[] s) throws IOException {
    write(s, 0, s.length);
  }
  //        Print a double-precision floating-point number.
  public void print(double d) throws IOException {
    print(Double.toString(d));
  }
  //        Print a floating-point number.
  public void print(float f) throws IOException {
    print(Float.toString(f));
  }
  //        Print an integer.
  public void print(int i) throws IOException {
    print(Integer.toString(i));
  }
  //        Print a long integer.
  public void print(long l) throws IOException {
    print(Long.toString(l));
  }
  //        Print an object.
  public void print(java.lang.Object obj) throws IOException {
    if (obj == null)
      print("null");
    else
      print(obj.toString());
  }
  //        Print a string.
  public void print(java.lang.String s) throws IOException {
    if (s == null)
      print("null");
    else
      write(s);
  }
  //        Terminate the current line by writing the line separator string.
  public void println() throws IOException {
    print("\r\n");
  }
  //        Print a boolean value and then terminate the line.
  public void println(boolean x) throws IOException {
    print(x);
    println();
  }
  //        Print a character and then terminate the line.
  public void println(char x) throws IOException {
    print(x);
    println();
  }
  //        Print an array of characters and then terminate the line.
  public void println(char[] x) throws IOException {
    print(x);
    println();
  }
  //        Print a double-precision floating-point number and then terminate the line.
  public void println(double x) throws IOException {
    print(x);
    println();
  }
  //        Print a floating-point number and then terminate the line.
  public void println(float x) throws IOException {
    print(x);
    println();
  }
  //        Print an integer and then terminate the line.
  public void println(int x) throws IOException {
    print(x);
    println();
  }
  //        Print a long integer and then terminate the line.
  public void println(long x) throws IOException {
    print(x);
    println();
  }
  //        Print an Object and then terminate the line.
  public void println(java.lang.Object x) throws IOException {
    print(x);
    println();
  }
  //        Print a String and then terminate the line.
  public void println(java.lang.String x) throws IOException {
    print(x);
    println();
  }
}

