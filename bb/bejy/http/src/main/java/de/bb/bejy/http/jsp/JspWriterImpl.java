/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspWriterImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:46:09 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * a JSPWriterImpl
 *
 */

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

/*
 * $Log: JspWriterImpl.java,v $
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.7  2004/04/07 16:35:21  bebbo
 * @I JspWriter stuff
 *
 * Revision 1.6  2004/03/24 09:39:54  bebbo
 * @B fixed handling of char encoding
 *
 * Revision 1.5  2002/04/08 13:24:14  franke
 * @B fixed write
 *
 * Revision 1.4  2002/03/30 15:43:20  franke
 * @B now throwin more Exceptions, as specified in doc
 *
 * Revision 1.3  2002/03/21 14:34:45  franke
 * @N added support for JSP files as Servlet
 * @N added support for classes loaded via ClassLoader
 * @N added support for lib/*.jar in web applications
 *
 * Revision 1.2  2002/03/10 20:05:20  bebbo
 * @B fixed handling of primitive types
 * @B fixed handling of page buffer
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
