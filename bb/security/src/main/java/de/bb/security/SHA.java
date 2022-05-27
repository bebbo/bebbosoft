/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SHA.java,v $
 * $Revision: 1.4 $
 * $Date: 2007/04/01 15:54:28 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Base class for all message digests
 *
 *****************************************************************************/

package de.bb.security;



/**
 * This is a simple port of Steve Reid's SHA-1 code into Java.
 * I've run his test vectors through the code and they all pass.
 *
 * @version $Revision: 1.4 $
 */
public final class SHA extends MessageDigest
{
  private int block[] = new int[20];

  private int state0, state1, state2, state3, state4;

  private int t0, t1, t2, t3, t4, add;

  /**
   * constructor
   */
  public SHA()
  {
    super("SHA1", 64);
    reset();
  }

  /**
   * clone implementation
   * @return a copy of the current object
   */
  public Object clone() //throws CloneNotSupportedException
  {
    /**/
    SHA m = new SHA();
    System.arraycopy(data, 0, m.data, 0, 64);
    m.count = count;
    m.state0 = state0;
    m.state1 = state1;
    m.state2 = state2;
    m.state3 = state3;
    m.state4 = state4;
    
    /** /
    SHA m = (SHA) super.clone();
    m.block = new int[20];
    /**/
    return m;
  }

  final private void R00(int a, int b)
  {
    t4 += a + b + add + rol(t0, 5);
    t1 = rol(t1, 30);
  }

  final private void R01(int a, int b)
  {
    t0 += a + b + add + rol(t1, 5);
    t2 = rol(t2, 30);
  }

  final private void R02(int a, int b)
  {
    t1 += a + b + add + rol(t2, 5);
    t3 = rol(t3, 30);
  }

  final private void R03(int a, int b)
  {
    t2 += a + b + add + rol(t3, 5);
    t4 = rol(t4, 30);
  }

  final private void R04(int a, int b)
  {
    t3 += a + b + add + rol(t4, 5);
    t0 = rol(t0, 30);
  }

  final private void blk()
  {
    int i = 0;
    for (; i < 3; ++i)
      block[i] = rol(block[i + 17] ^ block[i + 12] ^ block[i + 6]
          ^ block[i + 4], 1);
    for (; i < 8; ++i)
      block[i] = rol(
          block[i - 3] ^ block[i + 12] ^ block[i + 6] ^ block[i + 4], 1);
    for (; i < 14; ++i)
      block[i] = rol(block[i - 3] ^ block[i - 8] ^ block[i + 6] ^ block[i + 4],
          1);
    for (; i < 16; ++i)
      block[i] = rol(block[i - 3] ^ block[i - 8] ^ block[i - 14]
          ^ block[i + 4], 1);
    for (; i < 20; ++i)
      block[i] = rol(block[i - 3] ^ block[i - 8] ^ block[i - 14]
          ^ block[i - 16], 1);
  }

  /**
   * Hash a single 512-bit block. This is the core of the algorithm.
   */
  
  final protected void transform()
  {
    int i = 0;

    // convert to int
    for (int j = 0; i < 16; j += 4) {
      block[i++] = ((data[j]) << 24) | ((data[j + 1] & 0xff) << 16)
          | ((data[j + 2] & 0xff) << 8) | ((data[j + 3] & 0xff));
    }

    for (; i < 20; ++i) {
      block[i] = rol(block[i - 3] ^ block[i - 8] ^ block[i - 14]
          ^ block[i - 16], 1);
    }

    /* Copy context->state[] to working vars */
    t0 = state0;
    t1 = state1;
    t2 = state2;
    t3 = state3;
    t4 = state4;
    
    i = 0;
    add = 0x5A827999;
    for (;;) 
    { 
      R00(((t1 & (t2 ^ t3)) ^ t3), block[i++]);            
      R04(((t0 & (t1 ^ t2)) ^ t2), block[i++]);
      R03(((t4 & (t0 ^ t1)) ^ t1), block[i++]);
      R02(((t3 & (t4 ^ t0)) ^ t0), block[i++]);
      R01(((t2 & (t3 ^ t4)) ^ t4), block[i++]);      
      if (i == 20) break;
    }

    
    add = 0x6ED9EBA1;
    blk();
    
    for (i = 0;;) {
      R00((t1 ^ t2 ^ t3), block[i++]);
      R04((t0 ^ t1 ^ t2), block[i++]);
      R03((t4 ^ t0 ^ t1), block[i++]);
      R02((t3 ^ t4 ^ t0), block[i++]);
      R01((t2 ^ t3 ^ t4), block[i++]);
      if (i == 20)
        break;
    }
    /*
     * ((a + b) * c) + (a * b) = (ac + bc + ab)
     *     4 ops					5 ops
     * ac + b(a+c) = (a+b)c + ab = (b+c)a + bc
     */
    add = 0x8F1BBCDC;
    blk();
    
    for (i = 0;;) {
      R00((((t1 | t2) & t3) | (t1 & t2)), block[i++]);
      R04((((t0 | t1) & t2) | (t0 & t1)), block[i++]);
      R03((((t4 | t0) & t1) | (t4 & t0)), block[i++]);
      R02((((t3 | t4) & t0) | (t3 & t4)), block[i++]);
      R01((((t2 | t3) & t4) | (t2 & t3)), block[i++]);
      if (i == 20)
        break;
    }

    add = 0xCA62C1D6;
    blk();
    for (i = 0;;) {
      R00((t1 ^ t2 ^ t3), block[i++]);
      R04((t0 ^ t1 ^ t2), block[i++]);
      R03((t4 ^ t0 ^ t1), block[i++]);
      R02((t3 ^ t4 ^ t0), block[i++]);
      R01((t2 ^ t3 ^ t4), block[i++]);
      if (i == 20)
        break;
    }
    /* Add the working vars back into context.state[] */
    state0 += t0;
    state1 += t1;
    state2 += t2;
    state3 += t3;
    state4 += t4;
  }

  /**
   * Initialize new context
   */
  public void reset()
  {
    /* SHA1 initialization constants */
/*    
    state0 = 0;
    state1 = 0;
    state2 = 0;
    state3 = 0;
    state4 = 0;
/**/
    state0 = 0x67452301;
    state1 = 0xEFCDAB89;
    state2 = 0x98BADCFE;
    state3 = 0x10325476;
    state4 = 0xC3D2E1F0;
/**/    
    count = 0;
  }

  protected void addBitCount(long bitCount)
  {
    for (int i = 63; i >= 56; --i) {
      data[i] = (byte) bitCount;
      bitCount >>= 8;
    }
  }

  protected byte[] getDigest()
  {
    return new byte[] { (byte) (state0 >> 24), (byte) (state0 >> 16),
        (byte) (state0 >> 8), (byte) (state0), (byte) (state1 >> 24),
        (byte) (state1 >> 16), (byte) (state1 >> 8), (byte) (state1),
        (byte) (state2 >> 24), (byte) (state2 >> 16), (byte) (state2 >> 8),
        (byte) (state2), (byte) (state3 >> 24), (byte) (state3 >> 16),
        (byte) (state3 >> 8), (byte) (state3), (byte) (state4 >> 24),
        (byte) (state4 >> 16), (byte) (state4 >> 8), (byte) (state4) };
  }
}

/**
 * $Log: SHA.java,v $
 * Revision 1.4  2007/04/01 15:54:28  bebbo
 * @I improved performance
 *
 * Revision 1.3  2002/11/06 09:46:12  bebbo
 * @I cleanup for imports
 *
 * Revision 1.2  2001/03/29 18:25:05  bebbo
 * @C comments
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 * Revision 1.7  2000/06/22 16:35:41  bebbo
 * @N implemented clone() function
 *
 * Revision 1.6  2000/06/16 08:10:56  bebbo
 * @R now derived from MessageDigest instead of MessageDigestBase
 *
 * Revision 1.5  2000/06/15 09:58:37  bebbo
 * @I moved some code to base class
 *
 * Revision 1.4  1999/11/03 11:44:02  bebbo
 * *** empty log message ***
 *
 * Revision 1.3  1999/09/02 18:16:38  Bebbo
 * @C again...
 *
 * Revision 1.2  1999/09/02 18:16:03  Bebbo
 * @C added version to comment
 *
 * Revision 1.1.1.1  1999/09/02 17:17:46  Bebbo
 * import
 *
 */
