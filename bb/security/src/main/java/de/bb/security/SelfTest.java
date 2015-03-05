/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SelfTest.java,v $
 * $Revision: 1.2 $
 * $Date: 2007/04/01 15:54:43 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * self test for this package
 *
 *****************************************************************************/

package de.bb.security;


/**
 * Provide self test function for all classes in this package
 * <B>! NOT YET FINISHED !<B>
 * @version $Revision: 1.2 $
 * @since $Date: 2007/04/01 15:54:43 $
 * @author $Author: bebbo $
 */
public class SelfTest
{
  public static void main(String args[])
  {
    System.out.println("self test for de.bb.security started");
    if (!selftest())
      System.out.println("failed!!!");
  }
  /**
   * Run all defined tests for this package.
   * @return true on success.
   */
  public static boolean selftest()
  {

    if (!testMD5())
      return false;
    System.out.println("OK");

    if (!testSHA())
      return false;
    System.out.println("OK");
    
    if (!testMDC2())
      return false;
    System.out.println("OK");

    if (!testRMD160())
      return false;
    System.out.println("OK");
    
    if (!testDES())
      return false;
    System.out.println("OK");
    
    if (!testDES2())
      return false;
    System.out.println("OK");

    if (!testDES3())
      return false;
    System.out.println("OK");
    
    if (!testIso9796())
      return false;
    System.out.println("OK");

    if (!testRC2())
      return false;
    System.out.println("OK");
    
    System.out.println("self test for de.bb.security complete!");
    return true;
  }

  public static byte[] toHex(String s)
  {
    int len = s.length();
    byte b[] = new byte[len/2];
    int j = 0;
    for (int i = 0; i < len; i+=2, ++j)
    {      
      while (i < len && s.charAt(i) <= 32)
        ++i;
      if (i < len-1) 
        b[j] = (byte)Integer.parseInt(s.substring(i, i+2), 16);
    }
    byte bb[] = new byte[j];
    System.arraycopy(b, 0, bb, 0, j);
    return bb;
  }
  public static String toString(byte b[])
  {
    String s = "";
    for (int i = 0; i < b.length; ++i)
    {
      s += Integer.toHexString((b[i]&0xf0)>>4) + Integer.toHexString(b[i]&0xf);
    }
    return s;
  }

  /**
   * Run a test for DES
   * @return true on success.
   */
  public static boolean testDES()
  {
    System.out.print("Testing DES... ");
    DES des = new DES();
    {
      String tests[][] =
      { // key                plain               ciphered
        {"0000000000000000", "0000000000000000", "8CA64DE9C1B123A7"},
        {"ffffffffffffffff", "ffffffffffffffff", "7359B2163E4EDC58"},
        {"3000000000000000", "1000000000000001", "958E6E627A05557B"},
        {"0123456789abcdef", "0123456789abcde7", "c95744256a5ed31d"},
        {"0123456789abcdef", "1111111111111111", "17668DFC7292532D"},
        {"0101010101010101", "95f8a5e5dd31d900", "8000000000000000"},
        {"0101010101010101", "dd7f121ca5015619", "4000000000000000"},
        {"0101010101010101", "2e8653104f3834ea", "2000000000000000"},
      };
  // first test with known test data
      for (int i = 0; i < tests.length; ++i)
      {
        byte key[] = toHex(tests[i][0]);
        byte data[]= toHex(tests[i][1]);
        byte soll[]= toHex(tests[i][2]);

        des.setKeyUnchecked(key);
        des.encryptECB(data, data);
        if (!equals(data, soll))
        {
          System.out.println("expected: " + tests[i][2] + " - got: " + toString(data));
          return false;
        }
        des.decryptECB(data, data);
        if (!equals(data, toHex(tests[i][1])))
        {
          return false;
        }
      }
    }
/*
    {
      String tests[][] =
      { // key                plain               ciphered
        {"3000000000000000", "a3c986d0f91ef5f2", "9295b59bb384736e"}
      };
  // first test with known test data
      for (int i = 0; i < tests.length; ++i)
      {
        byte key[] = toHex(tests[i][0]);
        byte data[]= toHex(tests[i][1]);
        byte soll[]= toHex(tests[i][2]);

        des.setKeyUnchecked(key);
        des.encrypt(data, data);
        if (!equals(data, soll))
        {
          System.out.println("expected: " + tests[i][2] + " - got: " + toString(data));
          return false;
        }
        des.decrypt(data, data);
        if (!equals(data, toHex(tests[i][1])))
        {
          return false;
        }
      }
    }
*/
// some other tests...
    byte []key = des.generateKey();
    des.setKey(key);

    byte testData[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    byte tmpData[] = new byte[testData.length];
    byte ivData[] = {1, 2, 1, 2, 1, 2, 1, 2};
    byte ivTemp[] = new byte[ivData.length];

    // encrypt and decrypt first block
    des.encrypt(testData, 0, tmpData, 0);
    des.decrypt(tmpData, 0, tmpData, 0);
    if (!equals(testData, 0, tmpData, 0, 8))
      return false;

    // encrypt and decrypt all with ECB
    des.encryptECB(testData, tmpData);
    des.decryptECB(tmpData, tmpData);
    if (!equals(testData, tmpData))
      return false;

    // encrypt and decrypt all with CBC
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    des.encryptCBC(ivTemp, testData, tmpData);
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    des.decryptCBC(ivTemp, tmpData, tmpData);
    if (!equals(testData, tmpData))
      return false;

    // encrypt and decrypt all with CBC
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    byte b[] = des.encryptCBCAndPadd(ivTemp, testData);
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    tmpData = des.decryptCBCAndPadd(ivTemp, b);
    if (!equals(testData, tmpData))
      return false;


    return true;
  }
  /**
   * Run a test for DES2
   * @return true on success.
   */
   
  public static boolean testDES2()
  {
    System.out.print("Testing DES2... ");
    DES des = new DES2();
    byte key[] = des.generateKey();
    des.setKey(key);

    byte testData[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    byte tmpData[] = new byte[testData.length];
    byte ivData[] = {1, 2, 1, 2, 1, 2, 1, 2};
    byte ivTemp[] = new byte[ivData.length];

    // encrypt and decrypt first block
    des.encrypt(testData, 0, tmpData, 0);
    des.decrypt(tmpData, 0, tmpData, 0);
    if (!equals(testData, 0, tmpData, 0, 8))
      return false;

    // encrypt and decrypt all with ECB
    des.encryptECB(testData, tmpData);
    des.decryptECB(tmpData, tmpData);
    if (!equals(testData, tmpData))
      return false;

    // encrypt and decrypt all with CBC
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    des.encryptCBC(ivTemp, testData, tmpData);
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    des.decryptCBC(ivTemp, tmpData, tmpData);
    if (!equals(testData, tmpData))
      return false;

    // encrypt and decrypt all with CBC
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    byte b[] = des.encryptCBCAndPadd(ivTemp, testData);
    System.arraycopy(ivData, 0, ivTemp, 0, 8);
    tmpData = des.decryptCBCAndPadd(ivTemp, b);
    if (!equals(testData, tmpData))
      return false;

    return true;
  }


  /**
   * Run a test for DES3
   * @return true on success.
   */
   
  public static boolean testDES3()
  {
    System.out.print("Testing DES3... ");
    DES des = new DES3();
    String tests[][] =
    {
      // key                                                plain               ciphered
      {"4452c9b6329708f5719abfeea49385d41cd5fcd4aad54e68", "e00f35e5fb825409", "feb2ba8ddb33823b"},
//      {"0000000000000000ffffffffffffffff3000000000000000", "0000000000000000", "9295b59bb384736e"},
//      {"1ff8b3bc382997350d9a845fbc70fb831e9897881b32a7bf", "8e8d55e461a930cd", "0e6653ee5e7d6c4c"},
    };
// first test with known test data
    for (int i = 0; i < tests.length; ++i)
    {
      byte key[] = toHex(tests[i][0]);
      byte data[]= toHex(tests[i][1]);
      byte soll[]= toHex(tests[i][2]);

      des.setKey(key);
      des.encryptECB(data, data);
      if (!equals(data, soll))
      {
        System.out.println("expected: " + tests[i][2] + " - got: " + toString(data));
        return false;
      }
      des.decryptECB(data, data);
      if (!equals(data, toHex(tests[i][1])))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Run a test for RipeMD160
   * @return
   */   
  public static boolean testRMD160()
  {
    System.out.print("Testing RMD160 ... ");
    try {
      MessageDigest mda = new RMD160();

      String tests[][] =
      {
        // value, hash
        {"", "9c1185a5c5e9fc54612808977ee8f548b2258d31"},
        {"a", "0bdc9d2d256b3ee9daae347be6f4dc835a467ffe"},
        {"abc", "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc"},
        {"message digest", "5d0689ef49d2fae572b881b123a85ffa21595f36"},
        {"12345678901234567890123456789012345678901234567890123456789012345678901234567890", "9b752e45573d4b39f4dbd3323cab82bf63326bfb"},
        
      };
      
      return testMD(mda, tests);
      
            
    } catch (Exception e) {
      System.out.println("test broken: " + e.getMessage());
    }

    return true;
  }

  /**
   * @param mda
   * @param tests
   * @return
   */
  private static boolean testMD(MessageDigest mda, String[][] tests)
  {
//  first test with known test data
    for (int i = 0; i < tests.length; ++i)
    {
      byte v[] = tests[i][0].getBytes();
      byte r[] = toHex(tests[i][1]);
      byte b[] = mda.digest(v);
      if (!equals(r, b))
        return false;
    }
    return true;
  }
  /**
   * Run a test for MD5
   */   
  public static boolean testMD5()
  {
    System.out.print("Testing MD5 ... ");
    try {
      MessageDigest mda = new MD5();
      java.security.MessageDigest mdb = java.security.MessageDigest.getInstance("MD5");

      mda.update((byte)1);
      mdb.update((byte)1);

      if (!equals(mda.digest(), mdb.digest()))
        return false;

      byte b[] = new byte[777];
      mda.update(b);
      mdb.update(b);
      if (!equals(mda.digest(), mdb.digest()))
        return false;
        
      // test from SSL3
      int hlen = 16;
      String secretS = "14 54 d6 28 28 8b 35 df 73 08 14 97 32 2f 1c c9";
      long seqNum = 1;
      int typ = 23;
      String dataS =
"485454502f312e3120323030204f4b0d0a446174653a205475652c203138204a756c20323030302031333a33323a323320474d540d0a5365727665723a204170616368652f312e332e31322028556e697829205048502f332e302e3136206d6f645f73736"+
"c2f322e362e34204f70656e53534c2f302e392e35610d0a4c6173742d4d6f6469666965643a205475652c203131204a756c20323030302031373a31323a303320474d540d0a455461673a202231333032322d3237652d3339366235353633220d0a416363"+
"6570742d52616e6765733a2062797465730d0a436f6e74656e742d4c656e6774683a203633380d0a436f6e6e656374696f6e3a20636c6f73650d0a436f6e74656e742d547970653a206170706c69636174696f6e2f6f637465742d73747265616d0d0a0d0"+
"acafebabe0003002d002307001c070014070021090003000b0a0002000d090003000a0a0001000d090003000c090003000e0c0010001d0c001f001d0c001e001d0c002000220c0016001d0100047468697301000c626e6b5075626c6963456e6301000d43"+
"6f6e7374616e7456616c75650100124c6f63616c5661726961626c655461626c6501000a457863657074696f6e7301001f636f6d2f6e65746c6966652f736d61727463617264732f544b6579496e666f01000f4c696e654e756d6265725461626c6501000"+
"d6368507269766174655369676e01000a536f7572636546696c6501000e4c6f63616c5661726961626c6573010004436f64650100284c636f6d2f6e65746c6966652f736d61727463617264732f544b6579496e666f5061636b6167653b010014544b6579"+
"496e666f5061636b6167652e6a6176610100106a6176612f6c616e672f4f626a6563740100214c636f6d2f6e65746c6966652f736d61727463617264732f544b6579496e666f3b01000c636850726976617465456e6301000d626e6b5075626c696353696"+
"76e0100063c696e69743e010026636f6d2f6e65746c6966652f736d61727463617264732f544b6579496e666f5061636b6167650100032829560021000300010000000400010016001d00000001001e001d00000001001f001d000000010010001d000000"+
"01000100200022000100190000006f00030001000000312ab700072abb000259b70005b500092abb000259b70005b500082abb000259b70005b500042abb000259b70005b50006b10000000200150000001a00060000001000040012000f0013001a00140"+
"02500150030001000120000000c000100000031000f001a00000001001700000002001b"
;      

      byte mh1[] = calcMessageHash(mda, hlen, toHex(secretS), seqNum, typ, toHex(dataS));
      byte mh2[] = calcMessageHash(mdb, hlen, toHex(secretS), seqNum, typ, toHex(dataS));
        
      if (!equals(mh1, mh2))
        return false;
      
        
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * Run a test for SHA
   * @return true on success
   */   
  public static boolean testSHA()
  {
    System.out.print("Testing SHA ... ");
    try {
      MessageDigest mda = new SHA();
      java.security.MessageDigest mdb = java.security.MessageDigest.getInstance("SHA");

      mda.update((byte)1);
      mdb.update((byte)1);

      if (!equals(mda.digest(), mdb.digest()))
        return false;

      byte b[] = new byte[777];
      mda.update(b);
      mdb.update(b);
      if (!equals(mda.digest(), mdb.digest()))
        return false;
                     
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * Run a test for MDC2
   * @return true on success
   */   
  public static boolean testMDC2()
  {
    System.out.print("Testing MDC2 ... ");
    try {
      MessageDigest mda = new MDC2();
      
      String tests[][] =
      {
          {"Now is the time for all ", "42e50cd224baceba760bdd2bd409281a"}
      };
      
      return testMD(mda, tests);
            
    } catch (Exception e) {
      System.out.println("test broken: " + e.getMessage());
    }

    return true;
  }
  
  protected static final synchronized byte [] calcMessageHash(java.security.MessageDigest md, int hlen, byte secret[], long seqNum, int typ, byte b[]) throws Exception
  {
    md.update(secret);
//    System.out.println("A: " + toString(((java.security.MessageDigest)md.clone()).digest()));
    
    for (int i = 0; i < 80-2*hlen; ++i)
      md.update((byte)0x36);
      
//    System.out.println("B: " + toString(((java.security.MessageDigest)md.clone()).digest()));
      
    for (int i = 0; i < 8; i++) {
      md.update((byte)((seqNum >>> (((7 - i) * 8))) & 0xff));
    }
    md.update((byte)typ);
    md.update((byte)(b.length>>>8));
    md.update((byte)(b.length));
    
//    System.out.println("C: " + toString(((java.security.MessageDigest)md.clone()).digest()));
    
    md.update(b);
    byte hs[] = md.digest();
    
//    System.out.println(toString(hs));
    
    md.update(secret);
    for (int i = 0; i < 80-2*hlen; ++i)
      md.update((byte)0x5c); // 5c
    md.update(hs);
    return md.digest();
  }
  protected static final synchronized byte [] calcMessageHash(MessageDigest md, int hlen, byte secret[], long seqNum, int typ, byte b[]) throws Exception
  {
    md.update(secret);
//    System.out.println("A: " + toString(((MessageDigest)md.clone()).digest()));
    
    for (int i = 0; i < 80-2*hlen; ++i)
      md.update((byte)0x36);

//    System.out.println("B: " + toString(((MessageDigest)md.clone()).digest()));
    
    for (int i = 0; i < 8; i++) {
      md.update((byte)((seqNum >>> (((7 - i) * 8))) & 0xff));
    }
    md.update((byte)typ);
    md.update((byte)(b.length>>>8));
    md.update((byte)(b.length));
    
//    System.out.println("C: " + toString(((MessageDigest)md.clone()).digest()));
    
    md.update(b);

    
    byte hs[] = md.digest();
    
//    System.out.println(toString(hs));
    
    md.update(secret);
    for (int i = 0; i < 80-2*hlen; ++i)
      md.update((byte)0x5c); // 5c
    md.update(hs);
    return md.digest();
  }


  /**
   * Run a Test for RC2
   */   
  public static boolean testRC2()
  {
    System.out.print("Testing RC2... ");
    String tests[][] =
    {
      // key                                                plain               ciphered
      {"00000000000000000000000000000000", "0000000000000000", "1C198A838DF028B7"},
      {"00000000000000000000000000000001", "0000000000000000", "21829C78A9F9C074"},
      {"00000000000000000000000000000000", "ffffffffffffffff", "13DB3517D321869E"},
      {"000102030405060708090a0b0c0d0e0f", "0000000000000000", "50DC0162BD757F31"}
    };
    BlockCipher bc = new RC2();
// first test with known test data
    for (int i = 0; i < tests.length; ++i)
    {
      byte key[] = toHex(tests[i][0]);
      byte data[]= toHex(tests[i][1]);
      byte soll[]= toHex(tests[i][2]);

      bc.setKey(key);
      bc.encryptECB(data, data);
      if (!equals(data, soll))
      {
        System.out.println("encrypt - expected: " + tests[i][2] + " - got: " + toString(data));
        return false;
      }
      bc.decryptECB(data, data);
      if (!equals(data, toHex(tests[i][1])))
      {
        System.out.println("decrypt - expected: " + tests[i][1] + " - got: " + toString(data));
        return false;
      }
    }
    return true;
  }


  static boolean testIso9796()
  {
    System.out.print("Testing Iso9796... ");
    String tests[][] =
    {
      // in                                  out
      { "0cbbaa99887766554433221100", "c4559944883355223311ee00e70c66bbbbaadd990088ff77226644559944883355223311ee00e20c66bbbbaadd990088ff77226644559944883355223311ee06" },
      { "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210", "9dfea7dc6bbad098f276495485323e101cfea7dc6bbad098f276495485323e101cfea7dc6bbad098f276495485323e101cfea7dc6bbad098f276495485323e06"},
      { "0112233445566778899aabbccd", "70780d89db9ab6ab67bc7acde301351258238934944542562f67f0780d89db9ab6ab67bc7acde601351258238934944542562f67f0780d89db9ab6ab67bc7ad6" },
      { "DD4E64F15D004609D7140460B738A025871AE4E6", "006054250f873b1ac9e4c2e6aadd9c4e296413f14a5dee009246ed09afd73914e9042e606fb78038bea054250f873b1ac9e4c2e6abdd9c4e296413f14a5dee009246ed09afd73914e9042e606fb78038bea054250f873b1ac9e4c266" }
    };
    int k[][] = {
      { 100, 513 },
      { 256, 513 },
      { 100, 512 },
      { 160, 728 },
    };

// first test with known test data
    for (int i = 0; i < tests.length; ++i)
    {
      byte in[] = toHex(tests[i][0]);
      byte soll[]= toHex(tests[i][1]);

      byte data[] = Iso9796.pad(in, k[i][0], k[i][1]);
      if (!equals(data, soll))
      {
        System.out.println("pad expected:\n" + tests[i][1] + " - got:\n" + toString(data));
        return false;
      }
      data = Iso9796.unpad(data);
      if (!equals(data, toHex(tests[i][0])))
      {
        System.out.println("unpad - expected: " + tests[i][0] + " - got: " + toString(data));
        return false;
      }
    }
    return true;
  }
  
  /**
   * Compares one segment of a byte array with another segment if a byte array
   * @param a first byte array
   * @param aOff offset into first byte array
   * @param b second byte array
   * @param bOff offset into second byte array
   * @param len length of the compared segments
   * @return true if the segments are equal, otherwise false
   */
  public final static boolean equals (byte a[], int aOff, byte b[], int bOff, int len)
  {
    if (a == null || b == null)
      return a==b;
    while (len -- > 0)
      if (a[aOff + len] != b[bOff + len])
        return false;
    return true;
  }

  /**
   * Compares one byte array with another byte array
   * @param a first byte array
   * @param b second byte array
   * @return true if the byte arrays are equal, otherwise false
   */
  public final static boolean equals (byte a[], byte b[])
  {
    if (a.length != b.length)
      return false;
    return equals (a, 0, b, 0, a.length);
  }

}

/*
 * $Log: SelfTest.java,v $
 * Revision 1.2  2007/04/01 15:54:43  bebbo
 * @N added test for SHA
 *
 * Revision 1.1  2000/09/25 12:20:58  bebbo
 * @N repackaged
 *
 */

