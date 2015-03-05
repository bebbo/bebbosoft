/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SslInputStream.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/04/21 11:33:56 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * InputStream for SSL
 *
 *****************************************************************************/

package de.bb.security;

import java.io.IOException;
import java.io.InputStream;

/**
 * A class for reading from a Ssl connection.
 */
final class SslInputStream extends InputStream
{
  /** the used ssl instance. */
  private Ssl3 ssl;

  /**
   * Creates a new object for reading from a SSL connection.
   * @param ssl the used Ssl3 to read from
   */
  protected SslInputStream(Ssl3 ssl)
  {
    this.ssl = ssl;
  }

  /**
   * Returns the number of bytes that can be read from this input stream without blocking.
   * @return the number of bytes that can be read from this input stream without
   * blocking.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public int available() throws IOException
  {
    return ssl.available();
  }

  /**
   * Reads one byte from input stream with blocking.
   * @return the value of the read byte or -1 on EOS
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public int read() throws IOException
  {
    return ssl.read();
  }

  /**
   * Reads into the given byte array from input stream with blocking until some data is read.
   * @param b the buffer into which the data is read.
   * @return the count of read data.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public int read(byte b[]) throws IOException
  {
    return ssl.read(b);
  }

  /**
   * Reads into the given byte array from input stream with blocking until some data is read.
   * @param b the buffer into which the data is read.
   * @param off the offset into the array
   * @param len the count of bytes to read
   * @return the count of read data.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public int read(byte b[], int off, int len) throws IOException
  {
    byte x[] = new byte[len];
    len = ssl.read(x);
    if (len > 0)
      System.arraycopy(x, 0, b, off, len);
    return len;
  }

  /**
   * Closes this input stream and releases any system resources associated with the stream.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void close() throws IOException
  {
    ssl.close();
  }
}

/*
 * $Log: SslInputStream.java,v $
 * Revision 1.3  2007/04/21 11:33:56  bebbo
 * @N added AES and DES to SSLv3
 *
 * Revision 1.2  2007/04/20 14:26:22  bebbo
 * @I more plain writes. further enhancements are otw
 *
 * Revision 1.1  2007/04/18 13:07:10  bebbo
 * @N first checkin
 *
 * Revision 1.5  2003/03/06 15:25:01  bebbo
 * @C completed documentation
 *
 * Revision 1.4  2003/01/04 12:10:37  bebbo
 * @I cleaned up imports and formatted source code
 *
 * Revision 1.3  2000/11/15 09:02:59  bebbo
 * @I changed close_notify alert handling:
 *     no longer raises as an exception
 *     is now always sent on close()
 *
 * Revision 1.2  2000/09/25 12:49:19  bebbo
 * @C fixed comments
 *
 * Revision 1.1  2000/09/25 12:21:10  bebbo
 * @N repackaged
 *
 */
