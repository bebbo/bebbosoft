/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/MD5.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/04/01 15:52:17 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * MD5 implementation
 *
 *****************************************************************************/

package de.bb.security;


/**
 * This is an implementation of the MD5 algorithm. 
 * @author bebbo
 */
public class MD5 extends MessageDigest
{
  // only used in transform
  private int block[] = new int[16];
  private int t0, t1, t2, t3;
  private int state0, state1, state2, state3;
  
  
  /**
   * constructor
   */
  public MD5()
  {
    super("MD5", 64);
    reset();
  }

  /**
   * clone implementation
   * @return a copy of the current object
   */
  public Object clone() //throws CloneNotSupportedException
  {
    /** /
    MD5 m = (MD5)super.clone();
    m.block = new int[16];
    /**/
    MD5 m = new MD5();
    System.arraycopy(data, 0, m.data, 0, 64);
    m.count = count;
    m.state0 = state0;
    m.state1 = state1;
    m.state2 = state2;
    m.state3 = state3;
    /**/
    return m;
  }

  /**
   * Initialize new context
   */
  public void reset()
  {
    count = 0;
    /** /
    state0 = state1 = state2 = state3 = 0;
    /**/
    state0 = 1732584193;
    state1 = -271733879;
    state2 = -1732584194;
    state3 = 271733878;
    /**/
  }
  
  protected void addBitCount(long bitCount) {
    for (int i = 56; i < 64; ++i)
    {
      data[i] = (byte) bitCount;
      bitCount >>= 8;
    }
  }

  protected byte[] getDigest()
  {
    return new byte[]{
        (byte)(state0),
        (byte)(state0 >> 8),
        (byte)(state0 >> 16),
        (byte)(state0 >> 24),
        (byte)(state1),
        (byte)(state1 >> 8),
        (byte)(state1 >> 16),
        (byte)(state1 >> 24),
        (byte)(state2),
        (byte)(state2 >> 8),
        (byte)(state2 >> 16),
        (byte)(state2 >> 24),
        (byte)(state3),
        (byte)(state3 >> 8),
        (byte)(state3 >> 16),
        (byte)(state3 >> 24)
    };
    
  }
  
  /**
   * Hash a single 512-bit block. This is the core of the algorithm.
   *
   * Note that working with arrays is very inefficent in Java as it
   * does a class cast check each time you store into the array.
   *
   */
  final protected void transform()
  {
    // convert to int
    for(int j = 0, i = 0;i < 16;j += 4) {
      block[i++] =
        ((data[j] & 0xff)        ) |
        ((data[j+1] & 0xff) <<  8) |
        ((data[j+2] & 0xff) << 16) |
        ((data[j+3]       ) << 24);
    }
    
    t0 = state0;
    t1 = state1;
    t2 = state2;
    t3 = state3;

    G0(F00(), block[0], 7, 0xd76aa478);    
    G3(F03(), block[1], 12, 0xe8c7b756);
    G2(F02(), block[2], 17, 0x242070db);
    G1(F01(), block[3], 22, 0xc1bdceee);

//    HashTest.dump(new int[]{t0, t1, t2, t3});
//    HashTest.dump(block);
    
    G0(F00(), block[4], 7, 0xf57c0faf);
    G3(F03(), block[5], 12, 0x4787c62a);
    G2(F02(), block[6], 17, 0xa8304613);
    G1(F01(), block[7], 22, 0xfd469501);
    G0(F00(), block[8], 7, 0x698098d8);
    G3(F03(), block[9], 12, 0x8b44f7af);
    G2(F02(), block[10], 17, 0xffff5bb1);
    G1(F01(), block[11], 22, 0x895cd7be);
    G0(F00(), block[12], 7, 0x6b901122);
    G3(F03(), block[13], 12, 0xfd987193);
    G2(F02(), block[14], 17, 0xa679438e);
    G1(F01(), block[15], 22, 0x49b40821);
    
    G0(F10(), block[1], 5, 0xf61e2562);
    G3(F13(), block[6], 9, 0xc040b340);
    G2(F12(), block[11], 14, 0x265e5a51);

    G1(F11(), block[0], 20, 0xe9b6c7aa);
    G0(F10(), block[5], 5, 0xd62f105d);
    G3(F13(), block[10], 9, 0x2441453);
    G2(F12(), block[15], 14, 0xd8a1e681);

    G1(F11(), block[4], 20, 0xe7d3fbc8);
    G0(F10(), block[9], 5, 0x21e1cde6);
    G3(F13(), block[14], 9, 0xc33707d6);

    G2(F12(), block[3], 14, 0xf4d50d87);
    G1(F11(), block[8], 20, 0x455a14ed);
    G0(F10(), block[13], 5, 0xa9e3e905);

    G3(F13(), block[2], 9, 0xfcefa3f8);
    G2(F12(), block[7], 14, 0x676f02d9);
    G1(F11(), block[12], 20, 0x8d2a4c8a);
 
    
    G0(F20(), block[5], 4, 0xfffa3942);
    G3(F23(), block[8], 11, 0x8771f681);
    G2(F22(), block[11], 16, 0x6d9d6122);
    G1(F21(), block[14], 23, 0xfde5380c);

    G0(F20(), block[1], 4, 0xa4beea44);
    G3(F23(), block[4], 11, 0x4bdecfa9);
    G2(F22(), block[7], 16, 0xf6bb4b60);
    G1(F21(), block[10], 23, 0xbebfbc70);
    G0(F20(), block[13], 4, 0x289b7ec6);
 
    G3(F23(), block[0], 11, 0xeaa127fa);
    G2(F22(), block[3], 16, 0xd4ef3085);
    G1(F21(), block[6], 23, 0x4881d05);
    G0(F20(), block[9], 4, 0xd9d4d039);
    G3(F23(), block[12], 11, 0xe6db99e5);
    G2(F22(), block[15], 16, 0x1fa27cf8);
    
    G1(F21(), block[2], 23, 0xc4ac5665);
    
    
    G0(F30(), block[0], 6, 0xf4292244);
    G3(F33(), block[7], 10, 0x432aff97);
    G2(F32(), block[14], 15, 0xab9423a7);
 
    G1(F31(), block[5], 21, 0xfc93a039);
    G0(F30(), block[12], 6, 0x655b59c3);

    G3(F33(), block[3], 10, 0x8f0ccc92);
    G2(F32(), block[10], 15, 0xffeff47d);

    G1(F31(), block[1], 21, 0x85845dd1);
    G0(F30(), block[8], 6, 0x6fa87e4f);
    G3(F33(), block[15], 10, 0xfe2ce6e0);

    G2(F32(), block[6], 15, 0xa3014314);
    G1(F31(), block[13], 21, 0x4e0811a1);

    G0(F30(), block[4], 6, 0xf7537e82);
    G3(F33(), block[11], 10, 0xbd3af235);
    
    G2(F32(), block[2], 15, 0x2ad7d2bb);
    G1(F31(), block[9], 21, 0xeb86d391);

    state0 += t0;
    state1 += t1;
    state2 += t2;
    state3 += t3;
  }

  final private void G0(int i, int a, int s, int b)
  {
    t0 = t1 + rol(t0 + i + a + b, s);
  }
  final private void G1(int i, int a, int s, int b)
  {
    t1 = t2 + rol(t1 + i + a + b, s);
  }
  final private void G2(int i, int a, int s, int b)
  {
    t2 = t3 + rol(t2 + i + a + b, s);
  }
  final private void G3(int i, int a, int s, int b)
  {
    t3 = t0 + rol(t3 + i + a + b, s);
  }
  
  final private int F00()
  {
    return (t3 ^ (t1 & (t2 ^ t3)));
  }
  final private int F01()
  {
    return (t0 ^ t2 & (t3 ^ t0));
  }
  final private int F02()
  {
    return (t1 ^ t3 & (t0 ^ t1));
  }
  final private int F03()
  {
    return (t2 ^ t0 & (t1 ^ t2));
  }
  
  final private int F10()
  {
    return (t2 ^ t3 & (t1 ^ t2));
  }
  final private int F11()
  {
    return (t3 ^ t0 & (t2 ^ t3));
  }
  final private int F12()
  {
    return (t0 ^ t1 & (t3 ^ t0));
  }
  final private int F13()
  {
    return (t1 ^ t2 & (t0 ^ t1));
  }

  final private int F20()
  {
    return (t1 ^ t2 ^ t3);
  }
  final private int F21()
  {
    return (t2 ^ t3 ^ t0);
  }
  final private int F22()
  {
    return (t3 ^ t0 ^ t1);
  }
  final private int F23()
  {
    return (t0 ^ t1 ^ t2);
  }

  final private int F30()
  {
    return (t2 ^ (t1 | ~t3));
  }
  final private int F31()
  {
    return (t3 ^ (t2 | ~t0));
  }
  final private int F32()
  {
    return (t0 ^ (t3 | ~t1));
  }
  final private int F33()
  {
    return (t1 ^ (t0 | ~t2));
  }
}

/**
 * $Log: MD5.java,v $
 * Revision 1.3  2007/04/01 15:52:17  bebbo
 * @I improved performance
 *
 * Revision 1.2  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */