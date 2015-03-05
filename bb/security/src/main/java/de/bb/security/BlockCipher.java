/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/BlockCipher.java,v $
 * $Revision: 1.4 $
 * $Date: 2007/04/21 11:33:56 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Base class for all block ciphers
 *
 *****************************************************************************/

package de.bb.security;

/**
 * Base class for all block ciphers. 
 * @author bebbo
 */
public abstract class BlockCipher
{
  /** How big is a block?. */
  protected int blockSize;

  /**
   * Creates a new BlockCipher object. Derived Objects must pass its block size.
   * @param _blockSize defines the size of the smalles encryptable block
   */
  public BlockCipher (int _blockSize)
  {
    blockSize = _blockSize;
  }

  /**
   * Query the used block size.
   * @return the cipers blocksize
   */
  final public int blockSize ()
  {
    return blockSize;
  }
  /**
   * Set a givem key.
   * @param keyData the bytes which are used for the key
   */
  public abstract void setKey (byte[] keyData);

  /**
   * Encrypt one block of [block size] bytes.
   * You may use one byte array as input data and output data, to encrypt in place.
   * @param clearText input data which is encrypted
   * @param clearOff offset into input data
   * @param cipherText output data which is encrypted.
   * @param cipherOff offset into output data
   */
  public abstract void encrypt (byte[] clearText, int clearOff, byte[] cipherText, int cipherOff);

  /**
   * Decrypt one block of [block size] bytes.
   * You may use one byte array as input data and output data, to decrypt in place.
   * @param cipherText output data which is encrypted.
   * @param cipherOff offset into output data
   * @param clearText input data which is encrypted
   * @param clearOff offset into input data
   */
  public abstract void decrypt (byte[] cipherText, int cipherOff, byte[] clearText, int clearOff);

  /**
   * Encrypt a complete byte array without padding in ECB mode.
   * The size must be a multiple of block size.
   * You may use one byte array as input data and output data, to encrypt in place.
   * @param clearText input data which is encrypted
   * @param cipherText output data which is encrypted.
   */
  public void encryptECB (byte[] clearText, byte[] cipherText)
  {
    for (int i = 0; i < clearText.length; i += blockSize)
    {
      encrypt (clearText, i, cipherText, i);
    }
  }

  /**
   * Decrypt a complete byte array without padding in ECB mode.
   * The size must be a multiple of block size.
   * You may use one byte array as input data and output data, to decrypt in place.
   * @param cipherText output data which is encrypted.
   * @param clearText input data which is encrypted
   */
  public void decryptECB (byte[] cipherText, byte[] clearText)
  {
    for (int i = 0; i < cipherText.length; i += blockSize)
    {
      decrypt (cipherText, i, clearText, i);
    }
  }

  /**
   * Encrypt a complete byte array without padding in CBC mode.
   * The size must be a multiple of block size.
   * You may use one byte array as input data and output data, to encrypt in place.
   * The initialization vector is updated by this function!
   * @param iv the initialization vector.
   * @param clearText input data which is encrypted
   * @param cipherText output data which is encrypted.
   */
  final public void encryptCBC (byte[] iv, byte[] clearText, byte[] cipherText)
  {
    for (int i = 0; i < clearText.length; i += blockSize)
    {
      for (int j = 0; j < blockSize; j ++)
      {
        iv[j] ^= clearText[i + j];
      }
      encrypt (iv, 0, cipherText, i);
      System.arraycopy (cipherText, i, iv, 0, blockSize);
    }
  }

  /**
   * Decrypt a complete byte array wihthout padding in CBC mode.
   * The size must be a multiple of block size.
   * You may use one byte array as input data and output data, to decrypt in place.
   * The initialization vector is updated by this function!
   * @param iv the initialization vector.
   * @param cipherText output data which is encrypted.
   * @param clearText input data which is encrypted
   */
  final public void decryptCBC (byte[] iv, byte[] cipherText, byte[] clearText)
  {
    if (cipherText == clearText) {
      byte t[] = new byte[cipherText.length];
      System.arraycopy (cipherText, 0, t, 0, cipherText.length);
      cipherText = t;
    }
    for (int i = 0; i < cipherText.length; i += blockSize)
    {
      decrypt (cipherText, i, clearText, i);
    }

    for (int i = 0; i < blockSize; i ++)
    {
      clearText[i] ^= iv[i];
    }

    for (int i = blockSize; i < clearText.length; i ++)
    {
      clearText[i] ^= cipherText[i - blockSize];
    }
    System.arraycopy (cipherText, cipherText.length - blockSize, iv, 0, blockSize);
  }
  
  /**
   * encrypt data whith CBC
   * @param iv an initialization vector, sizeof blocksize.
   * @param clearText clear data, multiple of blocksize.
   * @param clearOffset offset into clearText
   * @param cipherText encrypted data, also multiple of blocksize
   * @param cipherOffset offset into cipherText
   * @param length length to encrypt
   */
  public void encryptCBC (byte[] iv, byte[] clearText, int clearOffset, byte[] cipherText, int cipherOffset, int length)
  {
    for (int i = 0; i < length; i += blockSize, cipherOffset += blockSize)
    {
      for (int j = 0; j < blockSize; ++j, ++clearOffset)
      {
        iv[j] ^= clearText[clearOffset];
      }
      encrypt (iv, 0, cipherText, cipherOffset);
      System.arraycopy (cipherText, cipherOffset, iv, 0, blockSize);
    }
  }  
  /**
   * dencrypt data whith CBC
   * @param iv an initialization vector, sizeof blocksize.
   * @param cipherText encrypted data, also multiple of blocksize
   * @param cipherOffset offset into cipherText
   * @param clearText clear data, multiple of blocksize.
   * @param clearOffset offset into clearText
   * @param length length to encrypt
   */
  public void decryptCBC (byte [] iv, byte[] cipherText, int cipherOffset, byte[] clearText, int clearOffset, int length)
  {
    byte iv1[] = iv;
    byte iv2[] = new byte[iv.length];
    for (int i = 0; i < length; i += blockSize, cipherOffset += blockSize)
    {
      System.arraycopy (cipherText, cipherOffset, iv2, 0, blockSize);
      decrypt (cipherText, cipherOffset, clearText, clearOffset);
      for (int j = 0; j < blockSize; ++j, ++clearOffset)
      {
        clearText[clearOffset] ^= iv1[j];
      }
      byte t[] = iv1;
      iv1 = iv2;
      iv2 = t;
    }
    if (iv1 != iv)
      System.arraycopy (iv1, 0, iv, 0, blockSize);
  }
  

  /**
   * Calculate the buffer length for padding.
   * @param plaintextLength length of input data
   * @return plaintextLength + pad length
   */
  protected int getBufferLength (int plaintextLength)
  {
    return plaintextLength + blockSize - plaintextLength % blockSize;
  }

  /**
   * Encrypt a complete byte array with padding in CBC mode.
   * The initialization vector is updated by this function!
   * @param iv the initialization vector.
   * @param clearText input data which is encrypted
   * @return a new allocated array of bytes containing the encrypted data.
   */
  final public byte[] encryptCBCAndPadd (byte[] iv, byte[] clearText)
  {
    int cipherTextLength = getBufferLength (clearText.length);
    byte[] buffer = new byte[cipherTextLength];
    byte[] cipherText = new byte[cipherTextLength];

    System.arraycopy (clearText, 0, buffer, 0, clearText.length);
    for (int i = clearText.length; i < cipherTextLength; ++ i)
      buffer[i] = (byte) (cipherTextLength - clearText.length);

    encryptCBC (iv, buffer, cipherText);

    return cipherText;
  }

  /**
   * Decrypt a complete byte array with padding in CBC mode.
   * The initialization vector is updated by this function!
   * @param iv the initialization vector.
   * @param cipherText output data which is encrypted.
   * @return a new allocated array of bytes containing the encrypted data.
   */
  final public byte[] decryptCBCAndPadd (byte[] iv, byte[] cipherText)
  {
    byte[] buffer = new byte[cipherText.length];
    decryptCBC (iv, cipherText, buffer);
    int offset = buffer[cipherText.length - 1];
    if (offset < 1 || offset > blockSize) // invalid padding
      return null;
    byte[] plainText = new byte[cipherText.length - offset];
    System.arraycopy (buffer, 0, plainText, 0, plainText.length);

    return plainText;
  }
}

/*
 * $Log: BlockCipher.java,v $
 * Revision 1.4  2007/04/21 11:33:56  bebbo
 * @N added AES and DES to SSLv3
 *
 * Revision 1.3  2007/04/13 18:03:31  bebbo
 * @R removed final from some methods
 *
 * Revision 1.2  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */

