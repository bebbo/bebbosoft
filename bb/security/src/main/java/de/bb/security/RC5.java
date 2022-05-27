/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/RC5.java,v $
 * $Revision: 1.1 $
 * $Date: 2000/09/25 12:20:58 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: stable $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * RC5 implementation
 *
 *****************************************************************************/

package de.bb.security;


/**
 * RC5-Cipher<BR><BR>
 * based of RFC-2040<BR>
 * tested on vectors found in this RFC (only Blocksize=8)
 */
public class RC5 extends BlockCipher 
{
  private int rounds = 16;
  private int wordbits;    // Blocksize / 2 in bits
  private int wordbytes;   // Blocksize / 2 in bytes
  private long s[];        // Key-Field
  private long pw;         // Blocksize-dependend constants for keycalculation
  private long qw;         // Blocksize-dependend constants for keycalculation
  private long rotmask;    // AND-Masks for rotations
  private long cutmask;    // AND-Masks for rotations

  
  private long rotl(long  x, long s){ //  (x << (s & rotmask)) | (x >>> (w - (s & rotmask)))
    s &= rotmask;
    x &= cutmask;
    return ((x << s) | (x >>> (wordbits - s)) & cutmask);
  }

  private long rotr(long  x, long s){ //  (x << (w - (s & rotmask))) | (x >>> (s & rotmask))
    s &= rotmask;
    x &= cutmask;
    return (((x << (wordbits - s)) | (x >>> s)) & cutmask);
  }
  /**
   * Create a new instance
   *
   * @param _blocksize Size of block in bytes to en/decrypt in one step (default 8)
   * @exception java.lang.IllegalArgumentException Blocksize can be only 4, 8 or 16.
   */
  public RC5 (int _blocksize) throws IllegalArgumentException
  {
    super(_blocksize);
    switch (_blocksize){
    case 4:
      pw = 0xB7E1l;
      qw = 0x9E37l;
      cutmask = 0xFFFFl;
      break;
    case 8:
      pw = 0xB7E15163l;
      qw = 0x9E3779B9l;
      cutmask = 0xFFFFFFFFl;
      break;
    case 16:
      pw = 0xB7E151628AED2A6Bl;
      qw = 0x9E3779B97F4A7C15l;
      cutmask = 0xFFFFFFFFFFFFFFFFl;
      break;
    default:
      throw (new IllegalArgumentException("only blocksize 4, 8, 16"));
    }
    wordbytes = _blocksize >>> 1;
    wordbits = wordbytes << 3;
    rotmask = (wordbits - 1);
  }

  /**
   * Setting rounds of inner RC5 transformation.
   *
   * @param _rounds min 0, max 255, default 16 (insecure if rounds < 12)
   * @exception java.lang.IllegalArgumentException 0 < _rounds < 256
   */
  public void setRounds (int _rounds) throws IllegalArgumentException{
    if ((0 > _rounds) || (255 < _rounds)) throw (new IllegalArgumentException("0 < rounds < 256"));
    rounds = _rounds;
  }

  /**
   * Getting rounds of inner RC5 transformation.
   */
  public int rounds(){
    return rounds;
  }

  /**
   * Setting the key for en/decryption.
   *
   * @param keyData the key
   * @exception java.lang.IllegalArgumentException 0 < keylength < 256
   */
  public void setKey (byte keyData[]) throws IllegalArgumentException{
    if ((null == keyData) ||  255 < keyData.length) throw (new IllegalArgumentException("0 < keylength < 256"));
// convert key (byte-array) into long-array
    int llen = (keyData.length + wordbytes - 1) / wordbytes;
    long l[] = new long [llen]; // i think "[256 / wordbytes]" is to big...
    for (int i = 0; llen > i; i++) l[i] = 0;
    for (int i = 0; keyData.length > i; i++){
      l[i / wordbytes] += ((keyData[i] & 0xFF)) << ((i % wordbytes) << 3); // 0, 8, 16, 24 ...;
    }
// compute key-independent array
    int slen = ((rounds+1) << 1);
    s =  new long[slen];
    s[0] = pw;
    for (int i = 1; slen > i; i++){
      s[i] = s[i-1] + qw;
    }
// mix both arrays
    long a = 0;
    long b = 0;
    int i = 0;
    int j = 0;
    for (int k = ((llen > slen) ? llen : slen) * 3; 0 < k; k--){
      a = rotl(s[i] + a + b, 3);
      s[i] = a;
      b = rotl(l[j] + a + b, a + b);
      l[j] = b;
      i = (i + 1) % slen;
      j = (j + 1) % llen;
    }
  }

  /**
   * Encrypt one Block of "Blocksize"
   *
   */
  public void encrypt (byte[] clearText, int clearOff, byte[] cipherText, int cipherOff){
// create LongInteger of half-block-length
    long a = 0; // array2long (cipherText, cipherOff);
    for (int i = wordbytes - 1; i >= 0; --i)
      a = (a << 8) + (clearText[clearOff + i] & 0xFF);
    
    long b = 0; //array2long(clearText, clearOff + wordbytes);
    for (int i = wordbytes+wordbytes - 1; i >= wordbytes; --i)
      b = (b << 8) + (clearText[clearOff + i] & 0xFF);
    
// encrypt...
    a += s[0];
    b += s[1];
    for (int i = 1; rounds >= i; i++){
      a = rotl((a ^ b), b) + (s[i << 1]);
      b = rotl((b ^ a), a) + (s[(i << 1) + 1]);
    }
// convert LongInteger to output
    //long2array(a, cipherText, cipherOff);
    for (int i = 0; i < wordbytes ; ++i){
      cipherText[cipherOff++] = (byte)a;
      a >>>= 8;
    }

    //long2array(b, cipherText, cipherOff + wordbytes);
    for (int i = 0; i < wordbytes ; ++i){
      cipherText[cipherOff++] = (byte)b;
      b >>>= 8;
    }    
  }

  /**
   * Decrypt one Block of "Blocksize"
   *
   */
  public void decrypt (byte[] cipherText, int cipherOff, byte[] clearText, int clearOff){
// create LongInteger of half-block-length
    long a = 0; // array2long (cipherText, cipherOff);
    for (int i = wordbytes - 1; i >= 0; --i)
      a = (a << 8) + (clearText[clearOff + i] & 0xFF);
    
    long b = 0; //array2long(clearText, clearOff + wordbytes);
    for (int i = wordbytes+wordbytes - 1; i >= wordbytes; --i)
      b = (b << 8) + (clearText[clearOff + i] & 0xFF);
      
// decrypt...
    for (int i = rounds; 0 < i; i--){
      b = rotr((b - (s[(i << 1) + 1])), a) ^ a;
      a = rotr((a - (s[i << 1])), b) ^ b;
    }
    a = a - s[0];
    b = b - s[1];
// convert LongInteger to output
    //long2array(a, cipherText, cipherOff);
    for (int i = 0; i < wordbytes ; ++i){
      cipherText[cipherOff++] = (byte)a;
      a >>>= 8;
    }

    //long2array(b, cipherText, cipherOff + wordbytes);
    for (int i = 0; i < wordbytes ; ++i){
      cipherText[cipherOff++] = (byte)b;
      b >>>= 8;
    }    
  }

	@Override
	public boolean hasKey() {
		return s != null;
	}
}

/*
 * $Log: RC5.java,v $
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */




