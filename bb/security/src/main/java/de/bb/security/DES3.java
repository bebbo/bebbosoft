/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/DES3.java,v $
 * $Revision: 1.5 $
 * $Date: 2011/06/11 03:08:03 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved.
 *
 * Triple DES 3 key implementation
 *
 *****************************************************************************/

package de.bb.security;

/**
 * The triple-DES encryption method with 3 DES objects.
 * 
 * This is a fairly standard way of increasing the security of DES. You run each block through DES three times, first
 * encrypting with key A, then decrypting with key B, then encrypting with key C.
 */

public class DES3 extends DES {
    private DES desB, desC;

    /**
     * Creates a new TripleDes object.
     * 
     * @see DesCipher
     * @see BlockCipher
     */
    public DES3() {
        super(8);
        desB = new DES();
        desC = new DES();
    }

    /**
     * Set the key for encryption and decryption.
     * 
     * @param key
     *            the given key, an array with 16 bytes
     */
    public void setKey(byte[] key) {
        byte b[] = new byte[8];
        System.arraycopy(key, 0, b, 0, 8);
        super.setKey(b);
        System.arraycopy(key, 8, b, 0, 8);
        desB.setKey(b);
        System.arraycopy(key, 16, b, 0, 8);
        desC.setKey(b);
    }

    /**
     * Set the key for encryption and decryption.
     * 
     * @param key
     *            the given key, an array with 16 bytes
     */
    public void setKeyUnchecked(byte[] key) {
        byte b[] = new byte[8];
        System.arraycopy(key, 0, b, 0, 8);
        super.setKeyUnchecked(b);
        System.arraycopy(key, 8, b, 0, 8);
        desB.setKeyUnchecked(b);
        System.arraycopy(key, 16, b, 0, 8);
        desC.setKeyUnchecked(b);
    }

    /**
     * Encrypt one block of [block size] bytes. You may use one byte array as input data and output data, to encrypt in
     * place.
     * 
     * @param clearText
     *            input data which is encrypted
     * @param clearOff
     *            offset into input data
     * @param cipherText
     *            output data which is encrypted.
     * @param cipherOff
     *            offset into output data
     */
    public void encrypt(byte[] clearText, int clearOff, byte[] cipherText, int cipherOff) {
        super.encrypt(clearText, clearOff, cipherText, cipherOff);
        desB.decrypt(cipherText, cipherOff, cipherText, cipherOff);
        desC.encrypt(cipherText, cipherOff, cipherText, cipherOff);
    }

    /**
     * Decrypt one block of [block size] bytes. You may use one byte array as input data and output data, to decrypt in
     * place.
     * 
     * @param cipherText
     *            output data which is encrypted.
     * @param cipherOff
     *            offset into output data
     * @param clearText
     *            input data which is encrypted
     * @param clearOff
     *            offset into input data
     */
    public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText, int clearOff) {
        desC.decrypt(cipherText, cipherOff, clearText, clearOff);
        desB.encrypt(clearText, clearOff, clearText, clearOff);
        super.decrypt(clearText, clearOff, clearText, clearOff);
    }
}
/*
 * $Log: DES3.java,v $
 * Revision 1.5  2011/06/11 03:08:03  bebbo
 * @C removed double file header
 *
 * Revision 1.4  2007/04/13 18:03:13  bebbo
 * @B fixed the block size
 *
 * Revision 1.3  2003/10/01 12:25:21  bebbo
 * @C enhanced comments
 *
 * Revision 1.2  2001/09/15 08:54:03  bebbo
 * @C comments
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */
