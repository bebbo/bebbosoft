/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SslOutputStream.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/04/21 11:33:56 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * OutputStream for SSL
 *
 *****************************************************************************/

package de.bb.security;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for writing to a Ssl connection.
 */
final class SslOutputStream extends OutputStream
{
  /** the used SSL instance. */
  private Ssl3 ssl;
  /** buffer to write a single byte. */
  private byte one[] = { 0 };

  /**
   * Creates a new object for writing to a Ssl2 connection.
   * @param ssl the used SSL connection to write to
   */
  SslOutputStream(Ssl3 ssl)
  {
    this.ssl = ssl;
  }

  /**
   * Writes the given byte to the output stream.
   * @param b the byte to write
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void write(int b) throws IOException
  {
    one[0] = (byte) b;
    ssl.write(one);
  }

  /**
   * Writes the given byte array to the output stream.
   * @param x the buffer which is written. 
   * @param off the offset into the array
   * @param len the count of bytes to write
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void write(byte x[], int off, int len) throws IOException
  {
    byte b[] = new byte[len];
    System.arraycopy(x, off, b, 0, len);
    ssl.write(b);
  }

  /**
   * Writes the given byte array to the output stream.
   * @param b the buffer which is written. 
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void write(byte b[]) throws IOException
  {
    ssl.write(b);
  }

  /**
   * Closes this output stream and releases any system resources associated with the stream.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void close() throws IOException
  {
    ssl.close();
  }
  /**
   * Flushes this output stream.
   * @throws java.io.IOException throws an IOException if an I/O Error occurs.
   */
  public void flush() throws IOException
  {
    ssl.flush();
  }
}

/*
 * $Log: SslOutputStream.java,v $
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
 * Revision 1.3  2000/11/15 09:02:58  bebbo
 * @I changed close_notify alert handling:
 *     no longer raises as an exception
 *     is now always sent on close()
 *
 * Revision 1.2  2000/09/25 12:49:18  bebbo
 * @C fixed comments
 *
 * Revision 1.1  2000/09/25 12:21:10  bebbo
 * @N repackaged
 *
 */
