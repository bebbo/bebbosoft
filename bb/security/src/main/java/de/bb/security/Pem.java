/**
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/Pem.java,v $
 * $Revision: 1.5 $
 * $Date: 2002/11/06 09:46:12 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Still public domain.
 *
 * Copyright (c) by Stefan Bebbo Franke 1999.
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 *
 * Pem utils implementation
 */

package de.bb.security;

import java.util.Random;

import de.bb.util.Mime;
import de.bb.util.Misc;

/**
 * helper functions to maintain PEM files
 */
 
public class Pem
{
  /**
   * detects whether the given byte block needs an password for decryption
   * @param b - input data
   * @param off - offset into data
   * @return true, when a password is required, either false.
   */
  public static boolean needsPwd(byte b[], int off)
  {
    String procType = Mime.getParam(b, off, "Proc-Type");
    return procType != null && procType.equalsIgnoreCase("4,ENCRYPTED");
  }
  
  /**
   * decodes the given byte block, needs an password for decryption
   * @param b - input data
   * @param off - offset into data
   * @param pwd - supplied password, sometimes needed
   * @return a new allocated byte array, when decoding was successful, either null.
   * @see needsPwd
   */
  public static byte [] decode(byte b[], int off, String pwd)
  {   
    byte [] d = Mime.searchDecode(b, off);
    if (d == null)
      return null;

    String procType = Mime.getParam(b, off, "Proc-Type");
    String dekInfo  = Mime.getParam(b, off, "DEK-Info");
    
    // not encrypted?
    if (procType == null || !procType.equalsIgnoreCase("4,ENCRYPTED"))
      return d;
    
    // no password ?
    if (pwd == null)
      return null;
      
    byte salt[] = new byte[0];
    String cipher = "DES-EDE3-CBC";
    if (dekInfo != null) {
      int lk = dekInfo.lastIndexOf(',');
      if (lk > 0)
      {
        cipher = dekInfo.substring(0,lk++);
        salt = Misc.hex2Bytes(dekInfo.substring(lk));
      }
    }
    byte km [] = Pkcs5.pbkdf1a(pwd.getBytes(), salt, 1, new MD5(), 24);
          
    BlockCipher bc = null;
    if (cipher.equalsIgnoreCase("DES-EDE3-CBC"))
    {
      bc = new DES3();
    }
    
    // unsupported cipher
    if (bc == null)
      return null;
    
    // decrypt
    bc.setKey(km);
    d = bc.decryptCBCAndPadd(salt, d);
    return d;
  }
  
  // create some random bits
  static Random rgen = new Random();
  
  /**
   * encodes a given byte block, needs password for encryption
   * @param b - input data
   * @param name - additional name for the encoded block: -----BEGIN <name>
   * @param off - offset into data
   * @param pwd - supplied password, needed for encryption
   * @param salt - a supplied salt, when null some salt is generated
   * @return a new allocated byte array, when encoding was successful, either null.
   * @see decode
   */
  public static byte [] encode(byte b[], String name, int off, String pwd, byte salt[])
  {   
    String header = "";
    if (pwd != null)
    {
      if (salt == null) {
        salt = new byte[8];
        rgen.nextBytes(salt);
      }
      header = "Proc-Type: 4,ENCRYPTED\nDEK-Info: DES-EDE3-CBC,";
      header += Misc.bytes2Hex(salt);
      header += "\r\n\r\n";
      
      byte km [] = Pkcs5.pbkdf1a(pwd.getBytes(), salt, 1, new MD5(), 24);
          
      BlockCipher bc = new DES3();
    
      // encrypt
      bc.setKey(km);
      b = bc.encryptCBCAndPadd(salt, b);
    }
    
    b = Mime.encode(b, 48);
    
    // result =
    // -----BEGIN <name>
    // <header>
    // <b>
    // -----END <name>
    try {
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      bos.write("-----BEGIN ".getBytes());
      bos.write(name.getBytes());
      bos.write("-----\r\n".getBytes());
      bos.write(header.getBytes());
      bos.write(b);
      bos.write(0xd);
      bos.write(0xa);
      bos.write("-----END ".getBytes());
      bos.write(name.getBytes());
      bos.write("-----\r\n\r\n".getBytes());
      return bos.toByteArray();
    }  catch (Exception e)
    {}
    return null;
  }
}

/**
 * $Log: Pem.java,v $
 * Revision 1.5  2002/11/06 09:46:12  bebbo
 * @I cleanup for imports
 *
 * Revision 1.4  2001/03/29 18:25:04  bebbo
 * @C comments
 *
 * Revision 1.3  2001/03/11 16:26:38  bebbo
 * @M merge
 *
 * Revision 1.2  2001/03/09 19:49:24  bebbo
 * @R my ByteArrayOutputStream moved to de.bb.io
 *
 * Revision 1.1  2001/03/05 17:47:44  bebbo
 * @N new or changed comment
 *
 * Revision 1.5  2000/06/19 10:33:01  bebbo
 * @N adding more lf to encoded data
 *
 * Revision 1.4  2000/06/18 17:06:02  bebbo
 * @R changes caused, by splitting Pkcs into separate files
 *
 * Revision 1.3  2000/06/16 19:41:14  bebbo
 * @B fixed null ptr access in decode
 *
 * Revision 1.2  2000/06/16 19:36:19  bebbo
 * @N added encode function
 *
 * Revision 1.1  2000/06/16 18:59:49  bebbo
 * @N added decode function
 * @N added needsPwd function
 *
 */
