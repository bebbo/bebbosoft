/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SecureRandom.java,v $
 * $Revision: 1.5 $
 * $Date: 2007/04/21 19:12:12 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * a secure random implementation
 *
 *****************************************************************************/

package de.bb.security;


import java.util.Random;

/**
 * This class provides a crytpographically strong pseudo-random number generator
 * based on the SHA-1 hash algorithm.
 *
 * @see java.util.Random
 *
 * @author Stefan Franke
 */

public class SecureRandom extends Random
{
  /** a public availabl instance, for everyone who needs randomness. */
  final static SecureRandom instance = new SecureRandom();
  /** the used instance of a SHA MessageDigest. */
  private MessageDigest sha;
  private int left = 20;
  private byte[] digest;
  private Random rnd = new Random();
  /**
   * ct of SecureRandom.
   */
  private SecureRandom()
  {
    
    try
    {
      sha = new SHA();
      long t0 = System.currentTimeMillis();
      sha.update((byte)t0);
      sha.update((byte)(t0>>8));
      rnd.setSeed(t0);
    } catch (Exception e)
    {
    }    
  }

  final public void nextBytes(byte []b) {
    nextBytes(b, 0, b.length);
  }
  final public void nextBytes(byte []b, int offset, int length) {
    for (int i = 0; i < length;)
    {
      int n = length - i;
      if (n > 4) n = 4;
      int z = next(n * 8);
      while (n-- > 0)
      {
        b[offset + i] = (byte)z;
        z >>>= 8;
        ++i;
      }
    }
  }
  
  /**
   * generate the next random bits.
   * @param bits - count of new generated bits
   * @return an integer containing the generated random bits
   */
  final public synchronized int next(int bits)
  {
    if (left == 20) {
      long t0 = System.currentTimeMillis();
      sha.update((byte)t0);
      sha.update((byte)(t0>>8));
      sha.update((byte)rnd.nextInt());
      digest = sha.digest();
      sha.update(digest);
      left = 0;      
    }
        
    int r = rnd.nextInt();
    int m = digest[left++] & 0xff;
    m |= m << 8;
    m |= m << 16;
    return r ^ m; // ^ r ^ m ^ 0xdeadbeef;    
  }
  /**
   * Returns the instance.
   * @return SecureRandom
   */
  public static SecureRandom getInstance()
  {
    return instance;
  }
  
  /**
   * 
   * @author sfranke
   */
  public static void addSeed(long seed) {
    for (int i = 0; i < 8; ++i)
    {
      instance.sha.update((byte)seed);
      seed >>>= 8;
    }
  }
}


/**
 * $Log: SecureRandom.java,v $
 * Revision 1.5  2007/04/21 19:12:12  bebbo
 * @B again public
 *
 * Revision 1.4  2007/04/21 11:33:56  bebbo
 * @N added AES and DES to SSLv3
 *
 * Revision 1.4  2007/04/20 18:40:42  bebbo
 * @I optimized writes
 *
 * Revision 1.3  2007/04/20 05:12:01  bebbo
 * @R removed SSLv2
 * @R enabled resumed handshakes
 *
 * Revision 1.2  2007/04/19 16:56:56  bebbo
 * @R modified to use SSLv3 directly
 * @R removed SSLv2
 * @N added AES 128,192,256
 *
 * Revision 1.1  2007/04/18 13:07:09  bebbo
 * @N first checkin
 *
 * Revision 1.4  2003/03/06 15:25:00  bebbo
 * @C completed documentation
 *
 * Revision 1.3  2003/01/04 12:10:37  bebbo
 * @I cleaned up imports and formatted source code
 *
 * Revision 1.2  2003/01/04 12:07:15  bebbo
 * @R cleanup
 *
 * Revision 1.1  2000/09/25 12:21:10  bebbo
 * @N repackaged
 *
 */
