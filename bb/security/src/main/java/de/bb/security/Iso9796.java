/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Iso9796.java,v $
 * $Revision: 1.5 $
 * $Date: 2003/10/01 12:25:21 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * pad / unpad data by Iso9796
 *
 *****************************************************************************/

package de.bb.security;

/**
 * Implements padding as defined in ISO 9796. 
 * @author bebbo
 */
public class Iso9796 
{
  private final static byte mutable[] = {
    14, 3, 5, 8, 9, 4, 2, 15, 0, 13, 11, 6, 7, 10, 12, 1
  };
  private final static byte demutable[] = new byte[16];
  static
  {
    for (int i = 0; i < 16; ++i)
      demutable[0xff&mutable[i]] = (byte)i;
  }

  /**
   * pad an input data according to ISO9796 to fit the specified keylength.
   * @param in - input data
   * @param inbits define how many bits are used, to mask unused bits
   * @param ks - the length of the modulo
   * @return a new allocated byte array containing the padded data
   */
  public static byte [] pad(byte [] in, int inbits, int ks)
  {
    // first step is skipped: in data is always byte aligned!
    int r = 9 - inbits%8;
    if (r == 9)
      r = 1;
    int z = in.length;
    int t = (ks-1 + 15)/16;
    byte b[] = new byte[2*t];

    // extend to t bytes
    boolean first = true;
    for (int i = t-1, j = z-1; i >= 0; --i) {
      byte d = in[j--];
      b[2*i + 1] = d;
      b[2*i] = (byte)(mutable[d&0xf] | (mutable[(d>>>4)&0xf]<<4));
      if (j < 0) {
        if (first) {
          first = false;
          b[2*i] ^= r;
        }
        j = z-1;
      }
    }

    // mask out some bits
    --ks;
    ks = ks%16;
    if (ks == 0)
      ks = 15;
    else
      --ks; 
      
    int i = 0;
    if (ks < 8)
      b[i++] = 0;
    else
      ks -= 8;
      
    ks = (1<<ks);    
    b[i] &= ks-1;
    // set hi bit    
    b[i] |= ks;
    
    
    // mark low byte
    b[b.length-1] = (byte)((b[b.length-1]<<4) | 6);

    return b;
  }

  /**
   * unpad an input data according to ISO9796.
   * @param in - input data
   * @return a new allocated byte array containing the unpadded data or null on error
   */
  public static byte [] unpad(byte [] in)
  {
    int i;
    for (i = 2; i < in.length; i+=2)
    {
      byte d = in[i+1];
      d = (byte)(mutable[d&0xf] | (mutable[(d>>>4)&0xf]<<4));
      if (d != in[i]) {
//        r = 0xff &(d ^ in[i]);
        break;        
      }
    }
    
    if (i+2 == in.length) // reached end -> no padding / no extension
      i =0;
    else
    if ((in[i-1]&0xf) != (in[in.length-1]>>4 &0xf))      
      return null;
    
    // in[in.length-1] = in[i-1]; // works only if there is some repetition
      
    { // this works always: translate the byte back
      byte d = in[in.length - 2];
      in[in.length - 1] =  (byte)(demutable[d&0xf] | (demutable[(d>>>4)&0xf]<<4));
    }
    byte b[] = new byte[(in.length-i) / 2];
    for (int j = 0; j < b.length; ++j, i += 2)
    {
      byte d = in[i+1];
      b[j] = d;
      
      // verify correctness of each byte
      if (j > 0 && in[i] != (byte)(mutable[d&0xf] | (mutable[(d>>>4)&0xf]<<4)))
        return null;
    }
    
    // it is not necessary to mask out the pad bits, they are all zero!       
    return b;
  }
}

/**
 * $Log: Iso9796.java,v $
 * Revision 1.5  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.4  2003/01/08 10:02:44  bebbo
 * @I removed an unused variable
 *
 * Revision 1.3  2003/01/07 18:32:13  bebbo
 * @W removed some deprecated warnings
 *
 * Revision 1.2  2002/11/06 09:46:12  bebbo
 * @I cleanup for imports
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */