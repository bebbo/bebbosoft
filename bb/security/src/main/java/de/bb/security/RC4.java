/* 
 * Created on 21.04.2005
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.security;

/**
 * @author sfranke
 */
public class RC4 extends BlockCipher
{
  /**
   * @param _blockSize
   */
  public RC4()
  {
    super(1);
  }

  byte[] key;
  
  /** (non-Javadoc)
   * @see de.bb.security.BlockCipher#setKey(byte[])
   */
  public void setKey(byte[] keyData)
  {
	key = new byte[258];
	  
    int i;
    for (i = 0; i < 256; i++)
      key[i] = (byte) i;
    key[256] = 0;
    key[257] = 0;
    int index1 = 0;
    int index2 = 0;
    for (i = 0; i < 256; i++)
    {
      index2 = (keyData[index1] + key[i] + index2) & 0xff;
      byte t = key[i];
      key[i] = key[index2];
      key[index2] = t;
      index1 = (index1 + 1) % keyData.length;
    }
  }

  /** (non-Javadoc)
   * @see de.bb.security.BlockCipher#encrypt(byte[], int, byte[], int)
   */
  public void encrypt(byte[] clearText, int clearOff, byte[] cipherText,
      int cipherOff)
  {
    int x = key[256] & 0xff;
    int y = key[257] & 0xff;

    {
      x = (x + 1) & 0xff;
      y = (key[x] + y) & 0xff;
      byte t = key[x];
      key[x] = key[y];
      key[y] = t;
      cipherText[cipherOff] = (byte)(clearText[clearOff] ^ key[(key[x] + key[y]) & 0xff]);
    }
    key[256] = (byte) x;
    key[257] = (byte) y;
  }

  public void encryptECB (byte[] clearText, byte[] cipherText)
  {
    int x = key[256] & 0xff;
    int y = key[257] & 0xff;
    for (int i = 0; i < clearText.length; i += blockSize)
    {
      x = (x + 1) & 0xff;
      y = (key[x] + y) & 0xff;
      byte t = key[x];
      key[x] = key[y];
      key[y] = t;
      cipherText[i] = (byte)(clearText[i] ^ key[(key[x] + key[y]) & 0xff]);
    }
    key[256] = (byte) x;
    key[257] = (byte) y;
  }

  public void decryptECB (byte[] cipherText, byte[] clearText)
  {
    encryptECB(cipherText, clearText);
  }  
  /** (non-Javadoc)
   * @see de.bb.security.BlockCipher#decrypt(byte[], int, byte[], int)
   */
  public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText,
      int clearOff)
  {
    int x = key[256] & 0xff;
    int y = key[257] & 0xff;

    {
      x = (x + 1) & 0xff;
      y = (key[x] + y) & 0xff;
      byte t = key[x];
      key[x] = key[y];
      key[y] = t;
      clearText[clearOff] = (byte)(cipherText[cipherOff] ^ key[(key[x] + key[y]) & 0xff]);
    }
    key[256] = (byte) x;
    key[257] = (byte) y;
  }
  public void encryptCBC (byte [] iv, byte[] clearText, int clearOffset, byte[] cipherText, int cipherOffset, int length)
  {
    int x = key[256] & 0xff;
    int y = key[257] & 0xff;
    for (int i = 0; i < length; ++i)
    {
      x = (x + 1) & 0xff;
      y = (key[x] + y) & 0xff;
      byte t = key[x];
      key[x] = key[y];
      key[y] = t;
      cipherText[cipherOffset + i] = (byte)(clearText[clearOffset + i] ^ key[(key[x] + key[y]) & 0xff]);
    }
    key[256] = (byte) x;
    key[257] = (byte) y;
  }

  public void decryptCBC (byte [] iv, byte[] cipherText, int cipherOffset, byte[] clearText, int clearOffset, int length)
  {
    encryptCBC(iv, cipherText, cipherOffset, clearText, clearOffset, length);
  }

	@Override
	public boolean hasKey() {
		return key != null;
	}  
}
