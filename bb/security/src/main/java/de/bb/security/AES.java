package de.bb.security;

/**
 * Fast implementation of the AES algorithm.
 * @author sfranke
 */
public class AES extends BlockCipher {
  private final static String _SBOX = "\u0063\u007c\u0077\u007b\u00f2\u006b\u006f\u00c5\u0030\u0001\u0067"
      + "\u002b\u00fe\u00d7\u00ab\u0076\u00ca\u0082\u00c9\u007d\u00fa\u0059\u0047\u00f0\u00ad\u00d4\u00a2"
      + "\u00af\u009c\u00a4\u0072\u00c0\u00b7\u00fd\u0093\u0026\u0036\u003f\u00f7\u00cc\u0034\u00a5\u00e5"
      + "\u00f1\u0071\u00d8\u0031\u0015\u0004\u00c7\u0023\u00c3\u0018\u0096\u0005\u009a\u0007\u0012\u0080"
      + "\u00e2\u00eb\u0027\u00b2\u0075\u0009\u0083\u002c\u001a\u001b\u006e\u005a\u00a0\u0052\u003b\u00d6"
      + "\u00b3\u0029\u00e3\u002f\u0084\u0053\u00d1\u0000\u00ed\u0020\u00fc\u00b1\u005b\u006a\u00cb\u00be"
      + "\u0039\u004a\u004c\u0058\u00cf\u00d0\u00ef\u00aa\u00fb\u0043\u004d\u0033\u0085\u0045\u00f9\u0002"
      + "\u007f\u0050\u003c\u009f\u00a8\u0051\u00a3\u0040\u008f\u0092\u009d\u0038\u00f5\u00bc\u00b6\u00da"
      + "\u0021\u0010\u00ff\u00f3\u00d2\u00cd\u000c\u0013\u00ec\u005f\u0097\u0044\u0017\u00c4\u00a7\u007e"
      + "\u003d\u0064\u005d\u0019\u0073\u0060\u0081\u004f\u00dc\"\u002a\u0090\u0088\u0046\u00ee\u00b8"
      + "\u0014\u00de\u005e\u000b\u00db\u00e0\u0032\u003a\n\u0049\u0006\u0024\\\u00c2\u00d3\u00ac"
      + "\u0062\u0091\u0095\u00e4\u0079\u00e7\u00c8\u0037\u006d\u008d\u00d5\u004e\u00a9\u006c\u0056\u00f4"
      + "\u00ea\u0065\u007a\u00ae\u0008\u00ba\u0078\u0025\u002e\u001c\u00a6\u00b4\u00c6\u00e8\u00dd\u0074"
      + "\u001f\u004b\u00bd\u008b\u008a\u0070\u003e\u00b5\u0066\u0048\u0003\u00f6\u000e\u0061\u0035\u0057"
      + "\u00b9\u0086\u00c1\u001d\u009e\u00e1\u00f8\u0098\u0011\u0069\u00d9\u008e\u0094\u009b\u001e\u0087"
      + "\u00e9\u00ce\u0055\u0028\u00df\u008c\u00a1\u0089\r\u00bf\u00e6\u0042\u0068\u0041\u0099\u002d"
      + "\u000f\u00b0\u0054\u00bb\u0016";

  private final static String _ISBOX = "\u0052\u0009\u006a\u00d5\u0030\u0036\u00a5\u0038\u00bf\u0040\u00a3"
      + "\u009e\u0081\u00f3\u00d7\u00fb\u007c\u00e3\u0039\u0082\u009b\u002f\u00ff\u0087\u0034\u008e\u0043"
      + "\u0044\u00c4\u00de\u00e9\u00cb\u0054\u007b\u0094\u0032\u00a6\u00c2\u0023\u003d\u00ee\u004c\u0095"
      + "\u000b\u0042\u00fa\u00c3\u004e\u0008\u002e\u00a1\u0066\u0028\u00d9\u0024\u00b2\u0076\u005b\u00a2"
      + "\u0049\u006d\u008b\u00d1\u0025\u0072\u00f8\u00f6\u0064\u0086\u0068\u0098\u0016\u00d4\u00a4\\"
      + "\u00cc\u005d\u0065\u00b6\u0092\u006c\u0070\u0048\u0050\u00fd\u00ed\u00b9\u00da\u005e\u0015\u0046"
      + "\u0057\u00a7\u008d\u009d\u0084\u0090\u00d8\u00ab\u0000\u008c\u00bc\u00d3\n\u00f7\u00e4\u0058"
      + "\u0005\u00b8\u00b3\u0045\u0006\u00d0\u002c\u001e\u008f\u00ca\u003f\u000f\u0002\u00c1\u00af\u00bd"
      + "\u0003\u0001\u0013\u008a\u006b\u003a\u0091\u0011\u0041\u004f\u0067\u00dc\u00ea\u0097\u00f2\u00cf"
      + "\u00ce\u00f0\u00b4\u00e6\u0073\u0096\u00ac\u0074\"\u00e7\u00ad\u0035\u0085\u00e2\u00f9\u0037"
      + "\u00e8\u001c\u0075\u00df\u006e\u0047\u00f1\u001a\u0071\u001d\u0029\u00c5\u0089\u006f\u00b7\u0062"
      + "\u000e\u00aa\u0018\u00be\u001b\u00fc\u0056\u003e\u004b\u00c6\u00d2\u0079\u0020\u009a\u00db\u00c0"
      + "\u00fe\u0078\u00cd\u005a\u00f4\u001f\u00dd\u00a8\u0033\u0088\u0007\u00c7\u0031\u00b1\u0012\u0010"
      + "\u0059\u0027\u0080\u00ec\u005f\u0060\u0051\u007f\u00a9\u0019\u00b5\u004a\r\u002d\u00e5\u007a"
      + "\u009f\u0093\u00c9\u009c\u00ef\u00a0\u00e0\u003b\u004d\u00ae\u002a\u00f5\u00b0\u00c8\u00eb\u00bb"
      + "\u003c\u0083\u0053\u0099\u0061\u0017\u002b\u0004\u007e\u00ba\u0077\u00d6\u0026\u00e1\u0069\u0014"
      + "\u0063\u0055\u0021\u000c\u007d";

  private final static byte SBOX[];

  /*
   = // the S box
   { 0x63, 0x7c, 0x77, 0x7b, (byte) 0xf2, 0x6b, 0x6f, (byte) 0xc5, 0x30, 0x01, 0x67, 0x2b, (byte) 0xfe, (byte) 0xd7,
   (byte) 0xab, 0x76, (byte) 0xca, (byte) 0x82, (byte) 0xc9, 0x7d, (byte) 0xfa, 0x59, 0x47, (byte) 0xf0,
   (byte) 0xad, (byte) 0xd4, (byte) 0xa2, (byte) 0xaf, (byte) 0x9c, (byte) 0xa4, 0x72, (byte) 0xc0, (byte) 0xb7,
   (byte) 0xfd, (byte) 0x93, 0x26, 0x36, 0x3f, (byte) 0xf7, (byte) 0xcc, 0x34, (byte) 0xa5, (byte) 0xe5,
   (byte) 0xf1, 0x71, (byte) 0xd8, 0x31, 0x15, 0x04, (byte) 0xc7, 0x23, (byte) 0xc3, 0x18, (byte) 0x96, 0x05,
   (byte) 0x9a, 0x07, 0x12, (byte) 0x80, (byte) 0xe2, (byte) 0xeb, 0x27, (byte) 0xb2, 0x75, 0x09, (byte) 0x83, 0x2c,
   0x1a, 0x1b, 0x6e, 0x5a, (byte) 0xa0, 0x52, 0x3b, (byte) 0xd6, (byte) 0xb3, 0x29, (byte) 0xe3, 0x2f, (byte) 0x84,
   0x53, (byte) 0xd1, 0x00, (byte) 0xed, 0x20, (byte) 0xfc, (byte) 0xb1, 0x5b, 0x6a, (byte) 0xcb, (byte) 0xbe, 0x39,
   0x4a, 0x4c, 0x58, (byte) 0xcf, (byte) 0xd0, (byte) 0xef, (byte) 0xaa, (byte) 0xfb, 0x43, 0x4d, 0x33, (byte) 0x85,
   0x45, (byte) 0xf9, 0x02, 0x7f, 0x50, 0x3c, (byte) 0x9f, (byte) 0xa8, 0x51, (byte) 0xa3, 0x40, (byte) 0x8f,
   (byte) 0x92, (byte) 0x9d, 0x38, (byte) 0xf5, (byte) 0xbc, (byte) 0xb6, (byte) 0xda, 0x21, 0x10, (byte) 0xff,
   (byte) 0xf3, (byte) 0xd2, (byte) 0xcd, 0x0c, 0x13, (byte) 0xec, 0x5f, (byte) 0x97, 0x44, 0x17, (byte) 0xc4,
   (byte) 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73, 0x60, (byte) 0x81, 0x4f, (byte) 0xdc, 0x22, 0x2a, (byte) 0x90,
   (byte) 0x88, 0x46, (byte) 0xee, (byte) 0xb8, 0x14, (byte) 0xde, 0x5e, 0x0b, (byte) 0xdb, (byte) 0xe0, 0x32, 0x3a,
   0x0a, 0x49, 0x06, 0x24, 0x5c, (byte) 0xc2, (byte) 0xd3, (byte) 0xac, 0x62, (byte) 0x91, (byte) 0x95, (byte) 0xe4,
   0x79, (byte) 0xe7, (byte) 0xc8, 0x37, 0x6d, (byte) 0x8d, (byte) 0xd5, 0x4e, (byte) 0xa9, 0x6c, 0x56, (byte) 0xf4,
   (byte) 0xea, 0x65, 0x7a, (byte) 0xae, 0x08, (byte) 0xba, 0x78, 0x25, 0x2e, 0x1c, (byte) 0xa6, (byte) 0xb4,
   (byte) 0xc6, (byte) 0xe8, (byte) 0xdd, 0x74, 0x1f, 0x4b, (byte) 0xbd, (byte) 0x8b, (byte) 0x8a, 0x70, 0x3e,
   (byte) 0xb5, 0x66, 0x48, 0x03, (byte) 0xf6, 0x0e, 0x61, 0x35, 0x57, (byte) 0xb9, (byte) 0x86, (byte) 0xc1, 0x1d,
   (byte) 0x9e, (byte) 0xe1, (byte) 0xf8, (byte) 0x98, 0x11, 0x69, (byte) 0xd9, (byte) 0x8e, (byte) 0x94,
   (byte) 0x9b, 0x1e, (byte) 0x87, (byte) 0xe9, (byte) 0xce, 0x55, 0x28, (byte) 0xdf, (byte) 0x8c, (byte) 0xa1,
   (byte) 0x89, 0x0d, (byte) 0xbf, (byte) 0xe6, 0x42, 0x68, 0x41, (byte) 0x99, 0x2d, 0x0f, (byte) 0xb0, 0x54,
   (byte) 0xbb, 0x16 };
   */

  private final static byte ISBOX[]; // the inverse S box

  /*  
   { 0x52, 0x09, 0x6a, (byte) 0xd5, 0x30, 0x36, (byte) 0xa5, 0x38, (byte) 0xbf, 0x40, (byte) 0xa3, (byte) 0x9e,
   (byte) 0x81, (byte) 0xf3, (byte) 0xd7, (byte) 0xfb, 0x7c, (byte) 0xe3, 0x39, (byte) 0x82, (byte) 0x9b, 0x2f,
   (byte) 0xff, (byte) 0x87, 0x34, (byte) 0x8e, 0x43, 0x44, (byte) 0xc4, (byte) 0xde, (byte) 0xe9, (byte) 0xcb,
   0x54, 0x7b, (byte) 0x94, 0x32, (byte) 0xa6, (byte) 0xc2, 0x23, 0x3d, (byte) 0xee, 0x4c, (byte) 0x95, 0x0b, 0x42,
   (byte) 0xfa, (byte) 0xc3, 0x4e, 0x08, 0x2e, (byte) 0xa1, 0x66, 0x28, (byte) 0xd9, 0x24, (byte) 0xb2, 0x76, 0x5b,
   (byte) 0xa2, 0x49, 0x6d, (byte) 0x8b, (byte) 0xd1, 0x25, 0x72, (byte) 0xf8, (byte) 0xf6, 0x64, (byte) 0x86, 0x68,
   (byte) 0x98, 0x16, (byte) 0xd4, (byte) 0xa4, 0x5c, (byte) 0xcc, 0x5d, 0x65, (byte) 0xb6, (byte) 0x92, 0x6c, 0x70,
   0x48, 0x50, (byte) 0xfd, (byte) 0xed, (byte) 0xb9, (byte) 0xda, 0x5e, 0x15, 0x46, 0x57, (byte) 0xa7, (byte) 0x8d,
   (byte) 0x9d, (byte) 0x84, (byte) 0x90, (byte) 0xd8, (byte) 0xab, 0x00, (byte) 0x8c, (byte) 0xbc, (byte) 0xd3,
   0x0a, (byte) 0xf7, (byte) 0xe4, 0x58, 0x05, (byte) 0xb8, (byte) 0xb3, 0x45, 0x06, (byte) 0xd0, 0x2c, 0x1e,
   (byte) 0x8f, (byte) 0xca, 0x3f, 0x0f, 0x02, (byte) 0xc1, (byte) 0xaf, (byte) 0xbd, 0x03, 0x01, 0x13, (byte) 0x8a,
   0x6b, 0x3a, (byte) 0x91, 0x11, 0x41, 0x4f, 0x67, (byte) 0xdc, (byte) 0xea, (byte) 0x97, (byte) 0xf2, (byte) 0xcf,
   (byte) 0xce, (byte) 0xf0, (byte) 0xb4, (byte) 0xe6, 0x73, (byte) 0x96, (byte) 0xac, 0x74, 0x22, (byte) 0xe7,
   (byte) 0xad, 0x35, (byte) 0x85, (byte) 0xe2, (byte) 0xf9, 0x37, (byte) 0xe8, 0x1c, 0x75, (byte) 0xdf, 0x6e, 0x47,
   (byte) 0xf1, 0x1a, 0x71, 0x1d, 0x29, (byte) 0xc5, (byte) 0x89, 0x6f, (byte) 0xb7, 0x62, 0x0e, (byte) 0xaa, 0x18,
   (byte) 0xbe, 0x1b, (byte) 0xfc, 0x56, 0x3e, 0x4b, (byte) 0xc6, (byte) 0xd2, 0x79, 0x20, (byte) 0x9a, (byte) 0xdb,
   (byte) 0xc0, (byte) 0xfe, 0x78, (byte) 0xcd, 0x5a, (byte) 0xf4, 0x1f, (byte) 0xdd, (byte) 0xa8, 0x33,
   (byte) 0x88, 0x07, (byte) 0xc7, 0x31, (byte) 0xb1, 0x12, 0x10, 0x59, 0x27, (byte) 0x80, (byte) 0xec, 0x5f, 0x60,
   0x51, 0x7f, (byte) 0xa9, 0x19, (byte) 0xb5, 0x4a, 0x0d, 0x2d, (byte) 0xe5, 0x7a, (byte) 0x9f, (byte) 0x93,
   (byte) 0xc9, (byte) 0x9c, (byte) 0xef, (byte) 0xa0, (byte) 0xe0, 0x3b, 0x4d, (byte) 0xae, 0x2a, (byte) 0xf5,
   (byte) 0xb0, (byte) 0xc8, (byte) 0xeb, (byte) 0xbb, 0x3c, (byte) 0x83, 0x53, (byte) 0x99, 0x61, 0x17, 0x2b, 0x04,
   0x7e, (byte) 0xba, 0x77, (byte) 0xd6, 0x26, (byte) 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d };
   */

  private final static int FF_HELP[] = new int[16];

  private final static int FF_HELP2[] = new int[256];

  private int end;

  private int[] intkp;

  //forward mix column transformation
  //{02}a ^ {03}b ^ {01}c ^ {01}d = a ^ {a ^ b ^ c ^ d} ^ {02}{a ^ b}
  //{01}a ^ {02}b ^ {03}c ^ {01}d = b ^ {a ^ b ^ c ^ d} ^ {02}{b ^ c}
  //{01}a ^ {01}b ^ {02}c ^ {03}d = c ^ {a ^ b ^ c ^ d} ^ {02}{c ^ d}
  //{03}a ^ {01}b ^ {01}c ^ {02}d = d ^ {a ^ b ^ c ^ d} ^ {02}{d ^ a}

  //inverse mix column transformation
  //{0e}a ^ {0b}b ^ {0d}c ^ {09}d
  //{09}a ^ {0e}b ^ {0b}c ^ {0d}d
  //{0d}a ^ {09}b ^ {0e}c ^ {0b}d
  //{0b}a ^ {0d}b ^ {09}c ^ {0e}d
  //= [forward transformation matrix]
  //{0c}{a^c} ^ {08}{b^d} ^ [{02}a ^ {03}b ^ {01}c ^ {01}d]
  //{08}{a^c} ^ {0c}{b^d} ^ [{01}a ^ {02}b ^ {03}c ^ {01}d]
  //{0c}{a^c} ^ {08}{b^d} ^ [{01}a ^ {01}b ^ {02}c ^ {03}d]
  //{08}{a^c} ^ {0c}{b^d} ^ [{03}a ^ {01}b ^ {01}c ^ {02}d]
  //=
  //{0c}{a^c} ^ {08}{b^d} ^ b ^ c ^ d ^ {02}{a ^ b}
  //{08}{a^c} ^ {0c}{b^d} ^ c ^ d ^ a ^ {02}{b ^ c}
  //{0c}{a^c} ^ {08}{b^d} ^ d ^ a ^ b ^ {02}{c ^ d}
  //{08}{a^c} ^ {0c}{b^d} ^ a ^ b ^ c ^ {02}{d ^ a}
  //=
  //a ^ {09}{a^b^c^d} ^ {04}{a^c} ^ {02}{a ^ b}
  //b ^ {09}{a^b^c^d} ^ {04}{b^d} ^ {02}{b ^ c}
  //c ^ {09}{a^b^c^d} ^ {04}{a^c} ^ {02}{c ^ d}
  //d ^ {09}{a^b^c^d} ^ {04}{b^d} ^ {02}{d ^ a}

  public AES()
  {
    super(16); // 4*4
  }

  public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText, int clearOff)
  {
    byte ISBOX[] = AES.ISBOX;
    int FF_HELP[] = AES.FF_HELP;
    int FF_HELP2[] = AES.FF_HELP2;
    int intkp[] = this.intkp;
    // fill state
    int s04812 = ((0xff & cipherText[cipherOff])) | ((0xff & cipherText[cipherOff + 4]) << 8)
        | ((0xff & cipherText[cipherOff + 8]) << 16) | ((0xff & cipherText[cipherOff + 12]) << 24);
    int s15913 = ((0xff & cipherText[cipherOff + 1])) | ((0xff & cipherText[cipherOff + 5]) << 8)
        | ((0xff & cipherText[cipherOff + 9]) << 16) | ((0xff & cipherText[cipherOff + 13]) << 24);
    int s261014 = ((0xff & cipherText[cipherOff + 2])) | ((0xff & cipherText[cipherOff + 6]) << 8)
        | ((0xff & cipherText[cipherOff + 10]) << 16) | ((0xff & cipherText[cipherOff + 14]) << 24);
    int s371115 = ((0xff & cipherText[cipherOff + 3])) | ((0xff & cipherText[cipherOff + 7]) << 8)
        | ((0xff & cipherText[cipherOff + 11]) << 16) | ((0xff & cipherText[cipherOff + 15]) << 24);

    int r = end;
    //    add_round_key(s, kp, r);
    s04812 ^= intkp[r];
    s15913 ^= intkp[r + 1];
    s261014 ^= intkp[r + 2];
    s371115 ^= intkp[r + 3];

    for (r -= 4;; r -= 4)
    {
      //      inv_shift_rows(s);
      s15913 = (s15913 >>> 24) | (s15913 << 8);
      s261014 = (s261014 >>> 16) | (s261014 << 16);
      s371115 = (s371115 >>> 8) | (s371115 << 24);

      //      inv_sub_bytes(s);
      s04812 = (0xff & ISBOX[(s04812) & 0xff]) | ((0xff & ISBOX[(s04812 >> 8) & 0xff]) << 8)
          | ((0xff & ISBOX[(s04812 >> 16) & 0xff]) << 16) | ((0xff & ISBOX[(s04812 >> 24) & 0xff]) << 24);
      s15913 = (0xff & ISBOX[(s15913) & 0xff]) | ((0xff & ISBOX[(s15913 >> 8) & 0xff]) << 8)
          | ((0xff & ISBOX[(s15913 >> 16) & 0xff]) << 16) | ((0xff & ISBOX[(s15913 >> 24) & 0xff]) << 24);
      s261014 = (0xff & ISBOX[(s261014) & 0xff]) | ((0xff & ISBOX[(s261014 >> 8) & 0xff]) << 8)
          | ((0xff & ISBOX[(s261014 >> 16) & 0xff]) << 16) | ((0xff & ISBOX[(s261014 >> 24) & 0xff]) << 24);
      s371115 = (0xff & ISBOX[(s371115) & 0xff]) | ((0xff & ISBOX[(s371115 >> 8) & 0xff]) << 8)
          | ((0xff & ISBOX[(s371115 >> 16) & 0xff]) << 16) | ((0xff & ISBOX[(s371115 >> 24) & 0xff]) << 24);

      //      add_round_key(s, kp, r);
      s04812 ^= intkp[r];
      s15913 ^= intkp[r + 1];
      s261014 ^= intkp[r + 2];
      s371115 ^= intkp[r + 3];

      if (r == 0)
        break;
      //      inv_mix_columns(s);
      {
        int ad = s04812 ^ s371115;
        int bc = s15913 ^ s261014;
        int q = ad ^ bc;
        //      q ^= gf_mul8(q);
        int m4 = (q << 2) & 0xfcfcfcfc;
        int t = q & 0xc0c0c0c0;
        m4 ^= FF_HELP2[0xff & ((t >>> 6) | (t >>> 12) | (t >> 18) | (t >> 24))];
        q ^= (m4 << 1) & 0xfefefefe;
        m4 &= 0x80808080;
        q ^= FF_HELP[0xf & ((m4 >>> 7) | (m4 >>> 14) | (m4 >> 21) | (m4 >> 28))];

        t = s04812 ^ s261014;
        int p = q ^ ((t << 2) & 0xfcfcfcfc);
        t = t & 0xc0c0c0c0;
        p ^= FF_HELP2[0xff & ((t >>> 6) | (t >>> 12) | (t >> 18) | (t >> 24))];

        t = s15913 ^ s371115;
        q ^= ((t << 2) & 0xfcfcfcfc);
        t = t & 0xc0c0c0c0;
        q ^= FF_HELP2[0xff & ((t >>> 6) | (t >>> 12) | (t >> 18) | (t >> 24))];

        t = s04812 ^ s15913;
        s04812 ^= p ^ ((t << 1) & 0xfefefefe);
        t = t & 0x80808080;
        s04812 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];

        s15913 ^= q ^ ((bc << 1) & 0xfefefefe);
        t = bc & 0x80808080;
        s15913 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];

        t = s261014 ^ s371115;
        s261014 ^= p ^ ((t << 1) & 0xfefefefe);
        t = t & 0x80808080;
        s261014 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];

        s371115 ^= q ^ ((ad << 1) & 0xfefefefe);
        t = ad & 0x80808080;
        s371115 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];
      }
    }
    //state_out(s, clearText, clearOff);
    clearText[clearOff] = (byte) (s04812);
    clearText[clearOff + 1] = (byte) (s15913);
    clearText[clearOff + 2] = (byte) (s261014);
    clearText[clearOff + 3] = (byte) (s371115);
    clearText[clearOff + 4] = (byte) (s04812 >> 8);
    clearText[clearOff + 5] = (byte) (s15913 >> 8);
    clearText[clearOff + 6] = (byte) (s261014 >> 8);
    clearText[clearOff + 7] = (byte) (s371115 >> 8);
    clearText[clearOff + 8] = (byte) (s04812 >> 16);
    clearText[clearOff + 9] = (byte) (s15913 >> 16);
    clearText[clearOff + 10] = (byte) (s261014 >> 16);
    clearText[clearOff + 11] = (byte) (s371115 >> 16);
    clearText[clearOff + 12] = (byte) (s04812 >> 24);
    clearText[clearOff + 13] = (byte) (s15913 >> 24);
    clearText[clearOff + 14] = (byte) (s261014 >> 24);
    clearText[clearOff + 15] = (byte) (s371115 >> 24);
  }

  public void encrypt(byte[] clearText, int clearOff, byte[] cipherText, int cipherOff)
  {
    byte SBOX[] = AES.SBOX;
    int FF_HELP[] = AES.FF_HELP;
    int intkp[] = this.intkp;
    // fill state
    int s04812 = ((0xff & clearText[clearOff])) | ((0xff & clearText[clearOff + 4]) << 8)
        | ((0xff & clearText[clearOff + 8]) << 16) | ((0xff & clearText[clearOff + 12]) << 24);
    int s15913 = ((0xff & clearText[clearOff + 1])) | ((0xff & clearText[clearOff + 5]) << 8)
        | ((0xff & clearText[clearOff + 9]) << 16) | ((0xff & clearText[clearOff + 13]) << 24);
    int s261014 = ((0xff & clearText[clearOff + 2])) | ((0xff & clearText[clearOff + 6]) << 8)
        | ((0xff & clearText[clearOff + 10]) << 16) | ((0xff & clearText[clearOff + 14]) << 24);
    int s371115 = ((0xff & clearText[clearOff + 3])) | ((0xff & clearText[clearOff + 7]) << 8)
        | ((0xff & clearText[clearOff + 11]) << 16) | ((0xff & clearText[clearOff + 15]) << 24);

    //add_round_key(s, kp, 0);    
    s04812 ^= intkp[0];
    s15913 ^= intkp[1];
    s261014 ^= intkp[2];
    s371115 ^= intkp[3];

    int r = 4;
    for (;; r += 4)
    {
      //sub_bytes(s);      
      s04812 = (0xff & SBOX[(s04812) & 0xff]) | ((0xff & SBOX[(s04812 >> 8) & 0xff]) << 8)
          | ((0xff & SBOX[(s04812 >> 16) & 0xff]) << 16) | ((0xff & SBOX[(s04812 >> 24) & 0xff]) << 24);
      s15913 = (0xff & SBOX[(s15913) & 0xff]) | ((0xff & SBOX[(s15913 >> 8) & 0xff]) << 8)
          | ((0xff & SBOX[(s15913 >> 16) & 0xff]) << 16) | ((0xff & SBOX[(s15913 >> 24) & 0xff]) << 24);
      s261014 = (0xff & SBOX[(s261014) & 0xff]) | ((0xff & SBOX[(s261014 >> 8) & 0xff]) << 8)
          | ((0xff & SBOX[(s261014 >> 16) & 0xff]) << 16) | ((0xff & SBOX[(s261014 >> 24) & 0xff]) << 24);
      s371115 = (0xff & SBOX[(s371115) & 0xff]) | ((0xff & SBOX[(s371115 >> 8) & 0xff]) << 8)
          | ((0xff & SBOX[(s371115 >> 16) & 0xff]) << 16) | ((0xff & SBOX[(s371115 >> 24) & 0xff]) << 24);

      //      shift_rows(s);
      s15913 = (s15913 >>> 8) | (s15913 << 24);
      s261014 = (s261014 >>> 16) | (s261014 << 16);
      s371115 = (s371115 >>> 24) | (s371115 << 8);

      if (r == end)
        break;

      //      mix_columns(s);
      {
        int ad = s04812 ^ s371115;
        int bc = s15913 ^ s261014;
        int abcd = ad ^ bc;
        int t = s04812 ^ s15913;
        s04812 ^= abcd ^ ((t << 1) & 0xfefefefe);
        t = t & 0x80808080;
        s04812 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];
        s15913 ^= abcd ^ ((bc << 1) & 0xfefefefe);
        t = bc & 0x80808080;
        s15913 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];
        t = s261014 ^ s371115;
        s261014 ^= abcd ^ ((t << 1) & 0xfefefefe);
        t = t & 0x80808080;
        s261014 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];
        s371115 ^= abcd ^ ((ad << 1) & 0xfefefefe);
        t = ad & 0x80808080;
        s371115 ^= FF_HELP[0xf & ((t >>> 7) | (t >>> 14) | (t >> 21) | (t >> 28))];
      }
      //      add_round_key(s, kp, r);
      s04812 ^= intkp[r + 0];
      s15913 ^= intkp[r + 1];
      s261014 ^= intkp[r + 2];
      s371115 ^= intkp[r + 3];
    }

    //    add_round_key(s, kp, end);
    s04812 ^= intkp[r + 0];
    s15913 ^= intkp[r + 1];
    s261014 ^= intkp[r + 2];
    s371115 ^= intkp[r + 3];

    // state_out(s, cipherText, cipherOff);    
    cipherText[cipherOff] = (byte) (s04812);
    cipherText[cipherOff + 1] = (byte) (s15913);
    cipherText[cipherOff + 2] = (byte) (s261014);
    cipherText[cipherOff + 3] = (byte) (s371115);
    cipherText[cipherOff + 4] = (byte) (s04812 >> 8);
    cipherText[cipherOff + 5] = (byte) (s15913 >> 8);
    cipherText[cipherOff + 6] = (byte) (s261014 >> 8);
    cipherText[cipherOff + 7] = (byte) (s371115 >> 8);
    cipherText[cipherOff + 8] = (byte) (s04812 >> 16);
    cipherText[cipherOff + 9] = (byte) (s15913 >> 16);
    cipherText[cipherOff + 10] = (byte) (s261014 >> 16);
    cipherText[cipherOff + 11] = (byte) (s371115 >> 16);
    cipherText[cipherOff + 12] = (byte) (s04812 >> 24);
    cipherText[cipherOff + 13] = (byte) (s15913 >> 24);
    cipherText[cipherOff + 14] = (byte) (s261014 >> 24);
    cipherText[cipherOff + 15] = (byte) (s371115 >> 24);
  }

  public void setKey(byte[] key)
  {
    int keylen = key.length;
    if (keylen != 16 && keylen != 24 && keylen != 32)
      throw new RuntimeException("illegal key size: " + keylen);

    int hi = (keylen + 28) << 2;
    byte[] kp = new byte[hi];

    int i = 0;
    for (; i < keylen; ++i)
    {
      kp[i] = key[i];
    }

    int rc = 1;
    for (; i < hi; i += 4)
    {
      byte temp0 = kp[i - 4];
      byte temp1 = kp[i - 3];
      byte temp2 = kp[i - 2];
      byte temp3 = kp[i - 1];

      if (i % keylen == 0)
      {
        //        rot_column(temp);
        byte t = temp0;
        temp0 = temp1;
        temp1 = temp2;
        temp2 = temp3;
        temp3 = t;

        //        sub_column(temp);
        temp0 = SBOX[temp0 & 0xff];
        temp1 = SBOX[temp1 & 0xff];
        temp2 = SBOX[temp2 & 0xff];
        temp3 = SBOX[temp3 & 0xff];

        temp0 ^= rc;
        if ((byte) rc < 0)
          rc = (rc << 1) ^ 0x1b;
        else
          rc <<= 1;
      } else if (keylen > 24 && (i % keylen == 16))
      {
        //        sub_column(temp);
        temp0 = SBOX[temp0 & 0xff];
        temp1 = SBOX[temp1 & 0xff];
        temp2 = SBOX[temp2 & 0xff];
        temp3 = SBOX[temp3 & 0xff];
      }

      kp[i] = (byte) (kp[i - keylen] ^ temp0);
      kp[i + 1] = (byte) (kp[i - keylen + 1] ^ temp1);
      kp[i + 2] = (byte) (kp[i - keylen + 2] ^ temp2);
      kp[i + 3] = (byte) (kp[i - keylen + 3] ^ temp3);
    }

    this.end = hi - 16;

    intkp = new int[kp.length / 4];
    for (i = 0; i < kp.length; i += 16)
    {
      int j = i / 4;
      intkp[j++] = ((0xff & kp[i])) | ((0xff & kp[i + 4]) << 8) | ((0xff & kp[i + 8]) << 16)
          | ((0xff & kp[i + 12]) << 24);
      intkp[j++] = ((0xff & kp[i + 1])) | ((0xff & kp[i + 5]) << 8) | ((0xff & kp[i + 9]) << 16)
          | ((0xff & kp[i + 13]) << 24);
      intkp[j++] = ((0xff & kp[i + 2])) | ((0xff & kp[i + 6]) << 8) | ((0xff & kp[i + 10]) << 16)
          | ((0xff & kp[i + 14]) << 24);
      intkp[j++] = ((0xff & kp[i + 3])) | ((0xff & kp[i + 7]) << 8) | ((0xff & kp[i + 11]) << 16)
          | ((0xff & kp[i + 15]) << 24);
    }
    
    end >>= 2;
  }

  /**
   * init of BOXes and FF_MUL arrays.
   */
  static
  {
    SBOX = new byte[256];
    ISBOX = new byte[256];
    for (int i = 0; i < 256; ++i)
    {
      SBOX[i] = (byte) _SBOX.charAt(i);
      ISBOX[i] = (byte) _ISBOX.charAt(i);
    }

    for (int i = 0; i < 16; ++i)
    {
      int v = 0;
      for (int j = 0; (i >>> j) > 0; ++j)
      {
        int ij = (i >>> j);
        if ((ij & 1) == 1)
          v |= 0x1b << j * 8;
      }
      FF_HELP[i] = v;
    }
    for (int i = 0; i < 256; ++i)
    {
      int v = 0;
      for (int j = 0; (i >>> j) > 0; j += 2)
      {
        int ij = (i >>> j);
        if ((ij & 3) == 1)
          v |= 0x1b << j * 4;
        else if ((ij & 3) == 2)
          v |= 0x36 << j * 4;
        else if ((ij & 3) == 3)
          v |= 0x2d << j * 4;
      }
      FF_HELP2[i] = v;
    }
  }  
}
