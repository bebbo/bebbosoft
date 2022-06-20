/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/MDC2.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/04/01 15:52:52 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * MDC2 implementation
 *
 *****************************************************************************/

package de.bb.security;

/**
 * Implements the MDC2 algorithm. 
 * @author bebbo
 */
public class MDC2 extends MessageDigest
{
  private DES des;

  private byte s1[];

  private byte s2[];

  private byte x1[];

  private byte x2[];

  /**
   * clone implementation.
   * @return a copy of the current object
   */
  public MDC2 clone()
  {
    MDC2 s = new MDC2();
    System.arraycopy(s1, 0, s.s1, 0, s1.length);
    System.arraycopy(s2, 0, s.s2, 0, s2.length);
    System.arraycopy(data, 0, s.data, 0, data.length);
    s.count = count;
    return s;
  }

  /**
   * create a new MDC2 object.
   */
  public MDC2()
  {
    super("MDC2", 8);
    des = new DES();
    s1 = new byte[8];
    s2 = new byte[8];
    x1 = new byte[8];
    x2 = new byte[8];
    reset();
  }

  /**
   * Initialize the context.
   */
  public void reset()
  {
    count = 0;

    for (int i = 0; i < 8; ++i)
      s1[i] = 0x52;
    for (int i = 0; i < 8; ++i)
      s2[i] = 0x25;

    for (int i = 0; i < data.length; ++i)
      data[i] = 0;
  }

  /**
   * Complete processing on the message digest.
   * @return the byte array containing the engineDigest
   */
 public byte[] digest()
  {
    int i = (int)count & 7;
    if (i > 0) {
      for (; i < 8; ++i)
        data[i] = 0;
      transform();
    }

    /* Store state in digest */
    byte buf[] = new byte[16];
    System.arraycopy(s1, 0, buf, 0, 8);
    System.arraycopy(s2, 0, buf, 8, 8);

    reset();
    return buf;
  }

  /**
   * Hash a single block. This is the core of the algorithm.
   *
   * Note that working with arrays is very inefficent in Java as it
   * does a class cast check each time you store into the array.
   *
   */
  protected final void transform()
  {
    s1[0] = (byte) ((s1[0] & 0x9f) | 0x40);
    s2[0] = (byte) ((s2[0] & 0x9f) | 0x20);

    des.setKey(s1);
    des.encryptECB(data, x1);

    des.setKey(s2);
    des.encryptECB(data, x2);

    s2[0] = (byte) (data[0] ^ x2[0]);
    s2[1] = (byte) (data[1] ^ x2[1]);
    s2[2] = (byte) (data[2] ^ x2[2]);
    s2[3] = (byte) (data[3] ^ x2[3]);
    s1[4] = (byte) (data[4] ^ x2[4]);
    s1[5] = (byte) (data[5] ^ x2[5]);
    s1[6] = (byte) (data[6] ^ x2[6]);
    s1[7] = (byte) (data[7] ^ x2[7]);

    s1[0] = (byte) (data[0] ^ x1[0]);
    s1[1] = (byte) (data[1] ^ x1[1]);
    s1[2] = (byte) (data[2] ^ x1[2]);
    s1[3] = (byte) (data[3] ^ x1[3]);
    s2[4] = (byte) (data[4] ^ x1[4]);
    s2[5] = (byte) (data[5] ^ x1[5]);
    s2[6] = (byte) (data[6] ^ x1[6]);
    s2[7] = (byte) (data[7] ^ x1[7]);
  }
}

/**
 * $Log: MDC2.java,v $
 * Revision 1.3  2007/04/01 15:52:52  bebbo
 * @I cleanup
 *
 * Revision 1.2  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */