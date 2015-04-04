/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/RMD160.java,v $
 * $Revision: 1.2 $
 * $Date: 2007/04/01 15:54:02 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * RMD160 implementation
 *
 *****************************************************************************/

package de.bb.security;

/**
 * @author sfranke
 */
public final class RMD160 extends MessageDigest
{
 //   private static final int 64 = 64;
 //   private static final int 5 = 5;
    
    /* The digest bits, if any. */
    private byte[] digestBits;
    
    /** 160-bit h0, h1, h2, h3, h4 (interim result) */
    private int[] context = new int[5];
    
    /** 512 bits work buffer = 16 x 32-bit words */
    private int[] X = new int[16];

    /**
     * Constants for the transform method. They're defined as static because
     * they're common to all RIPEMD160 instantiated objects; and final since
     * they're non-modifiable.
     */
    private static final int[]
        // selection of message word
        R  = {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,
                7,  4, 13,  1, 10,  6, 15,  3, 12,  0,  9,  5,  2, 14, 11,  8,
                3, 10, 14,  4,  9, 15,  8,  1,  2,  7,  0,  6, 13, 11,  5, 12,
                1,  9, 11, 10,  0,  8, 12,  4, 13,  3,  7, 15, 14,  5,  6,  2,
                4,  0,  5,  9,  7, 12,  2, 10, 14,  1,  3,  8, 11,  6, 15, 13},
        Rp = {  5, 14,  7,  0,  9,  2, 11,  4, 13,  6, 15,  8,  1, 10,  3, 12,
                6, 11,  3,  7,  0, 13,  5, 10, 14, 15,  8, 12,  4,  9,  1,  2,
               15,  5,  1,  3,  7, 14,  6,  9, 11,  8, 12,  2, 10,  0,  4, 13,
                8,  6,  4,  1,  3, 11, 15,  0,  5, 12,  2, 13,  9,  7, 10, 14,
               12, 15, 10,  4,  1,  5,  8,  7,  6,  2, 13, 14,  0,  3,  9, 11},
    
        // amount for rotate left (rol)    
        S  = { 11, 14, 15, 12,  5,  8,  7,  9, 11, 13, 14, 15,  6,  7,  9,  8,
                7,  6,  8, 13, 11,  9,  7, 15,  7, 12, 15,  9, 11,  7, 13, 12,
               11, 13,  6,  7, 14,  9, 13, 15, 14,  8, 13,  6,  5, 12,  7,  5,
               11, 12, 14, 15, 14, 15,  9,  8,  9, 14,  5,  6,  8,  6,  5, 12,
                9, 15,  5, 11,  6,  8, 13, 12,  5, 12, 13, 14, 11,  8,  5,  6},
        Sp = {  8,  9,  9, 11, 13, 15, 15,  5,  7,  7,  8, 11, 14, 14, 12,  6,
                9, 13, 15,  7, 12,  8,  9, 11,  7,  7, 12,  7,  6, 15, 13, 11,
                9,  7, 15, 11,  8,  6,  6, 14, 12, 13,  5, 14, 13, 13,  7,  5,
               15,  5,  8, 11, 14, 14,  6, 14,  6,  9, 12,  9, 12,  5, 15,  8,
                8,  5, 12,  9, 12,  5, 14,  6,  8, 13,  6,  5, 15, 13, 11, 11};
    
  /**
   * clone implementation
   * @return a copy of the current object
   */
  public Object clone() {
    RMD160 s = new RMD160();
    System.arraycopy(digestBits, 0, s.digestBits, 0, digestBits.length);
    System.arraycopy(context, 0, s.context, 0, context.length);
    System.arraycopy(data, 0, s.data, 0, data.length);
    s.count = count;
    return s;
  }
   
  
  /**
   * constructor
   */    
  public RMD160 () {
    super ("RMD160", 64);
    reset();
  }
  /**
   * Resets this object disregarding any temporary data present at the
   * time of the invocation of this call.
   */
  public void reset () 
  {
    // magic RIPEMD160 initialisation constants
    context[0] = 0x67452301;
    context[1] = 0xEFCDAB89;
    context[2] = 0x98BADCFE;
    context[3] = 0x10325476;
    context[4] = 0xC3D2E1F0;
    count = 0L;
    for (int i = 0; i < 64; i++) 
      data[i] = 0;
  }    

  /**
   * Complete the hash computation by performing final operations such
   * as padding. At the return of this engineDigest, the MD engine is
   * reset.
   *
   * @return    the array of bytes for the resulting hash value.
   */
  public byte[] digest () 
  {
    // pad output (in bytes) to 56 mod 64
    int bufferNdx = (int)(count % 64);
    int padLen = (bufferNdx < 56) ? (56 - bufferNdx) : (120 - bufferNdx);

    // padding is alwas binary 1 followed by binary 0s
    byte[] tail = new byte[padLen + 8];
    tail[0] = (byte)0x80;

    // save number of bits, casting the long to an array of 8 bytes
    for (int i = 0; i < 8; i++)
        tail[padLen + i] = (byte)((count * 8) >>> (8 * i));
    // append length before final transform
    update(tail, 0, tail.length);

    byte[] result = new byte[20];
    // cast this RIPEMD160's context (array of 5 ints) into an array of 20 bytes.
    for (int i = 0; i < 5; i++)
        for (int j = 0; j < 4; j++)
            result[i * 4 + j] = (byte)((context[i] >>> (8 * j)) & 0xFF);

    reset();
    return result;
  }    
    
  /**
   * RIPEMD160 basic transformation.
   * <p>
   * Transforms context based on 512 bits from input block starting from
   * the offset'th byte.
   */
  final protected void transform () 
  {
    int offset = 0;
    int A, B, C, D, E, Ap, Bp, Cp, Dp, Ep, T, s, i;
        
    // encode 64 bytes from input block into an array of 16 unsigned
    // integers.
    for (i = 0; i < 16; i++)
        X[i] = (data[offset++] & 0xFF)       |
                (data[offset++] & 0xFF) <<  8 |
                (data[offset++] & 0xFF) << 16 |
                (data[offset++] & 0xFF) << 24;
    A = Ap = context[0];
    B = Bp = context[1];
    C = Cp = context[2];
    D = Dp = context[3];
    E = Ep = context[4];

    // rounds 0...15
    for (i = 0; i < 16; i++) {
        s = S[i];
        T = A + (B ^ C ^ D) + X[i];
        A = E; E = D; D = C << 10 | C >>> 22; C = B;
        B = (T << s | T >>> (32 - s)) + A;

        s = Sp[i];
        T = Ap + (Bp ^ (Cp | ~Dp)) + X[Rp[i]] + 0x50A28BE6;
        Ap = Ep; Ep = Dp; Dp = Cp << 10 | Cp >>> 22; Cp = Bp;
        Bp = (T << s | T >>> (32 - s)) + Ap;
    }
    // rounds 16...31
    for (i = 16; i < 32; i++) {
        s = S[i];
        T = A + ((B & C) | (~B & D)) + X[R[i]] + 0x5A827999;
        A = E; E = D; D = C << 10 | C >>> 22; C = B;
        B = (T << s | T >>> (32 - s)) + A;
            
        s = Sp[i];
        T = Ap + ((Bp & Dp) | (Cp & ~Dp)) + X[Rp[i]] + 0x5C4DD124;
        Ap = Ep; Ep = Dp; Dp = Cp << 10 | Cp >>> 22; Cp = Bp;
        Bp = (T << s | T >>> (32 - s)) + Ap;
    }
    // rounds 32...47
    for (i = 32; i < 48; i++) {
        s = S[i];
        T = A + ((B | ~C) ^ D) + X[R[i]] + 0x6ED9EBA1;
        A = E; E = D; D = C << 10 | C >>> 22; C = B;
        B = (T << s | T >>> (32 - s)) + A;

        s = Sp[i];
        T = Ap + ((Bp | ~Cp) ^ Dp) + X[Rp[i]] + 0x6D703EF3;
        Ap = Ep; Ep = Dp; Dp = Cp << 10 | Cp >>> 22; Cp = Bp;
        Bp = (T << s | T >>> (32 - s)) + Ap;
    }
    // rounds 48...63
    for (i = 48; i < 64; i++) {
        s = S[i];
        T = A + ((B & D) | (C & ~D)) + X[R[i]] + 0x8F1BBCDC;
        A = E; E = D; D = C << 10 | C >>> 22; C = B;
        B = (T << s | T >>> (32 - s)) + A;

        s = Sp[i];
        T = Ap + ((Bp & Cp) | (~Bp & Dp)) + X[Rp[i]] + 0x7A6D76E9;
        Ap = Ep; Ep = Dp; Dp = Cp << 10 | Cp >>> 22; Cp = Bp;
        Bp = (T << s | T >>> (32 - s)) + Ap;
    }
    // rounds 64...79
    for (i = 64; i < 80; i++) {
        s = S[i];
        T = A + (B ^ (C | ~D)) + X[R[i]] + 0xA953FD4E;
        A = E; E = D; D = C << 10 | C >>> 22; C = B;
        B = (T << s | T >>> (32 - s)) + A;
            
        s = Sp[i];
        T = Ap + (Bp ^ Cp ^ Dp) + X[Rp[i]];
        Ap = Ep; Ep = Dp; Dp = Cp << 10 | Cp >>> 22; Cp = Bp;
        Bp = (T << s | T >>> (32 - s)) + Ap;
    }
    T = context[1] + C + Dp;
    context[1] = context[2] + D + Ep;
    context[2] = context[3] + E + Ap;
    context[3] = context[4] + A + Bp;
    context[4] = context[0] + B + Cp;
    context[0] = T;
  }    
}

/*
 * $Log: RMD160.java,v $
 * Revision 1.2  2007/04/01 15:54:02  bebbo
 * @I cleanup
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 *
 */
