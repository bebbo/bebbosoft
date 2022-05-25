/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/RC2.java,v $
 * $Revision: 1.1 $
 * $Date: 2000/09/25 12:20:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: stable $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * RC2 implementation
 *
 *****************************************************************************/

package de.bb.security;

public class RC2 extends BlockCipher
{
  	// 256-entry permutation table, probably derived somehow from pi 
	final static private short permute [] = {
	    217,120,249,196, 25,221,181,237, 40,233,253,121, 74,160,216,157,
	    198,126, 55,131, 43,118, 83,142, 98, 76,100,136, 68,139,251,162,
	     23,154, 89,245,135,179, 79, 19, 97, 69,109,141,  9,129,125, 50,
	    189,143, 64,235,134,183,123, 11,240,149, 33, 34, 92,107, 78,130,
	     84,214,101,147,206, 96,178, 28,115, 86,192, 20,167,140,241,220,
	     18,117,202, 31, 59,190,228,209, 66, 61,212, 48,163, 60,182, 38,
	    111,191, 14,218, 70,105,  7, 87, 39,242, 29,155,188,148, 67,  3,
	    248, 17,199,246,144,239, 62,231,  6,195,213, 47,200,102, 30,215,
	      8,232,234,222,128, 82,238,247,132,170,114,172, 53, 77,106, 42,
	    150, 26,210,113, 90, 21, 73,116, 75,159,208, 94,  4, 24,164,236,
	    194,224, 65,110, 15, 81,203,204, 36,145,175, 80,161,244,112, 57,
	    153,124, 58,133, 35,184,180,122,252,  2, 54, 91, 37, 85,151, 49,
	     45, 93,250,152,227,138,146,174,  5,223, 41, 16,103,108,186,201,
	    211,  0,230,207,225,158,168, 44, 99, 22,  1, 63, 88,226,137,169,
	     13, 56, 52, 27,171, 51,255,176,187, 72, 12, 95,185,177,205, 46,
	    197,243,219, 71,229,165,156,119, 10,166, 32,104,254,127,193,173
	};

  
  private short xkey[];
  
  public RC2()
  {
    super(8);
  }

/**
 * Expand a variable-length user key (between 1 and 128 bytes) to a     
 * 64-short working rc2 key, of at most 1024 effective key bits.      
 * @param key - key data to generate the key
 */
  public void setKey(byte []key)
	{
	  setKey(key, 1024);
	}
/**
 * Expand a variable-length user key (between 1 and 128 bytes) to a     
 * 64-short working rc2 key, of at most "bits" effective key bits.      
 * The effective key bits parameter looks like an export control hack.  
 * For normal use, it should always be set to 1024.  For convenience,   
 * zero is accepted as an alias for 1024.                               
 * @param key - key data to generate the key
 * @param bits - reduce the key length to this bit count
 */
  public void setKey(byte []key, int bits)
	{    
	  xkey = new short[64];
	  if (bits == 0) 
	    bits = 128;
	// Phase 1: Expand input key to 128 bytes 
	  if (key.length < 128) {
      byte ok[] = key;
	    key = new byte[128];
	    System.arraycopy(ok, 0, key, 0, ok.length);
	    int l = ok.length;
		  int i = 0;
		  byte x = key[l-1];
		  do {
			  x = (byte)permute[(x + key[i++]) & 255];
			  key[l++] = x;
		  } while (l < 128);
	  }	  
	  
	// Phase 2 - reduce effective key size to "bits"   	
	  int len = (bits+7)>>3;
	  int i = 128 - len;
	  int x = permute[key[i] & (0xff >> (7 & -bits))];
   	key[i] = (byte)x;
	  while (i-- > 0) 
	  {
	    x = permute[ x ^ (key[i+len]&0xff) ];		
	    key[i] = (byte)x;
	  }   	

	// Phase 3 - copy to xkey in little-endian order 
	  for (i = 63; i >= 0; --i)
	  {
		  xkey[i] = (short)((key[2*i]&0xff) +
		                    ((key[2*i+1]&0xff) << 8));
	  } 
	}

  /**
   * Encrypt one block of <block Size> bytes.
   * You may use one byte array as input data and output data, to encrypt in place.
   * @param clearText input data which is encrypted
   * @param clearOff offset into input data
   * @param cipherText output data which is encrypted.
   * @param cipherOff offset into output data
   */
  public void encrypt(byte []plain, int pOff, byte []cipher, int cOff )
	{	  
	  short x10 = (short)(plain[pOff++]&0xff);
	  x10 |= (plain[pOff++]&0xff) << 8;
	  short x32 = (short)(plain[pOff++]&0xff);
	  x32 |= (plain[pOff++]&0xff) << 8;
	  short x54 = (short)(plain[pOff++]&0xff);
	  x54 |= (plain[pOff++]&0xff) << 8;
	  short x76 = (short)(plain[pOff++]&0xff);
	  x76 |= (plain[pOff]&0xff) << 8;
	  

	  for (int i = 0; i < 16; i++) {
		  x10 += (x32 & ~x76) + (x54 & x76) + xkey[4*i+0];
		  x10 = (short)((x10 << 1) + (x10 >>> 15 & 1));
  		
		  x32 += (x54 & ~x10) + (x76 & x10) + xkey[4*i+1];
		  x32 = (short)((x32 << 2) + (x32 >>> 14 & 3));

		  x54 += (x76 & ~x32) + (x10 & x32) + xkey[4*i+2];
		  x54 = (short)((x54 << 3) + (x54 >>> 13 & 7));

		  x76 += (x10 & ~x54) + (x32 & x54) + xkey[4*i+3];
		  x76 = (short)((x76 << 5) + (x76 >>> 11 & 31));

		  if (i == 4 || i == 10) {
			  x10 += xkey[x76 & 63];
			  x32 += xkey[x10 & 63];
			  x54 += xkey[x32 & 63];
			  x76 += xkey[x54 & 63];
		  }
	  }

	  cipher[cOff++] = (byte)x10;
	  cipher[cOff++] = (byte)(x10 >>> 8);
	  cipher[cOff++] = (byte)x32;
	  cipher[cOff++] = (byte)(x32 >>> 8);
	  cipher[cOff++] = (byte)x54;
	  cipher[cOff++] = (byte)(x54 >>> 8);
	  cipher[cOff++] = (byte)x76;
	  cipher[cOff  ] = (byte)(x76 >>> 8);
	}

  /**
   * Decrypt one block of <block Size> bytes.
   * You may use one byte array as input data and output data, to decrypt in place.
   * @param cipherText output data which is encrypted.
   * @param cipherOff offset into output data
   * @param clearText input data which is encrypted
   * @param clearOff offset into input data
   */
  public void decrypt(byte []cipher, int cOff, byte []plain, int pOff)
	{
	  short x10 = (short)(cipher[cOff++]&0xff);
	  x10 |= (cipher[cOff++]&0xff) << 8;
	  short x32 = (short)(cipher[cOff++]&0xff);
	  x32 |= (cipher[cOff++]&0xff) << 8;
	  short x54 = (short)(cipher[cOff++]&0xff);
	  x54 |= (cipher[cOff++]&0xff) << 8;
	  short x76 = (short)(cipher[cOff++]&0xff);
	  x76 |= (cipher[cOff]&0xff) << 8;

	  for (int i = 15; i>= 0; --i)
	  {
		  if (i == 4 || i == 10) {
			  x76 -= xkey[x54 & 63];
			  x54 -= xkey[x32 & 63];
			  x32 -= xkey[x10 & 63];
			  x10 -= xkey[x76 & 63];
		  }
		  x76 = (short)((x76 << 11) + (x76 >>> 5 & 0x07ff));
		  x76 -= (x10 & ~x54) + (x32 & x54) + xkey[4*i+3];

		  x54 = (short)((x54 << 13) + (x54 >>> 3 & 0x1fff));
		  x54 -= (x76 & ~x32) + (x10 & x32) + xkey[4*i+2];
  		
		  x32 = (short)((x32 << 14) + (x32 >>> 2 & 0x3fff));
		  x32 -= (x54 & ~x10) + (x76 & x10) + xkey[4*i+1];

		  x10 = (short)((x10 << 15) + (x10 >>> 1 & 0x7fff));
		  x10 -= (x32 & ~x76) + (x54 & x76) + xkey[4*i+0];
	  } 

	  plain[pOff++] = (byte)x10;
	  plain[pOff++] = (byte)(x10 >>> 8);
	  plain[pOff++] = (byte)x32;
	  plain[pOff++] = (byte)(x32 >>> 8);
	  plain[pOff++] = (byte)x54;
	  plain[pOff++] = (byte)(x54 >>> 8);
	  plain[pOff++] = (byte)x76;
	  plain[pOff  ] = (byte)(x76 >>> 8);
	}

	@Override
	public boolean hasKey() {
		return xkey != null;
	}
}

/*
 * $Log: RC2.java,v $
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */
