package de.bb.security;

/**
 * Implementation of the GCM encryption mode.
 * 
 * @see http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-spec.pdf
 * @see http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-revised-spec.pdf
 * @see http://csrc.nist.gov/publications/nistpubs/800-38D/SP-800-38D.pdf
 * 
 * @author Stefan Bebbo Franke
 *
 */
public final class GCM extends BlockCipher{

    /** 16 zero bytes, referenced to save memory. */
    private static final byte[] ZERO = new byte[16];
    /** The lookup table E1 for 0xe1, 0xe1², ... Used to calculate R. */
    private static final byte[][] E1;
    /** The lookup table R to calculate the table m. */
    private static final byte[][] R;

    /**
     * Initialize E1 and R.
     */
    static {
        byte e1[][] = new byte[8][2];
        e1[7][0] = (byte) 0xe1;
        for (int i = 7; i > 0; --i) {
            final byte a[] = e1[i];
            final byte b[] = e1[i - 1];
            final int c = a[0] & 0xff;
            b[1] = (byte) (((a[1] & 0xff) >>> 1) | (c << 7));
            b[0] = (byte) (c >>> 1);
        }
        E1 = e1;

        byte r[][] = new byte[256][2];
        for (int i = 0; i < 256; ++i) {
            final byte a[] = r[i];
            for (int j = 7; j >= 0; --j) {
                if ((i & (1 << j)) != 0) {
                    final byte b[] = E1[j];
                    a[0] ^= b[0];
                    a[1] ^= b[1];
                }
            }
        }
        R = r;
    }

    /** The used <code>BlockCipher</code>. */
    private BlockCipher bc;
    /** The 12 bytes of nonce plus 4 bytes counter. */
    private byte nonceCounter[] = new byte[16];
    /** The encrypted nonceCounter at counter == 1. */
    private byte cryptedNonceCounter1[] = new byte[16];
    /** A temporary buffer. */
    private byte tmp[] = new byte[16];
    /** The hash value. */
    private byte hash[] = new byte[16];
    /** The lookup table M to speedup multiplications. */
    private byte[][][] m;
    /** The length of the data. */
    private long dataLen;
    /** The length of "Additional Authenticated Data". */
    private long aadLen;

    /**
     * The constructor - needs a <code>BlockCiper</code> instance with block size 16.
     * 
     * @param bc
     *            the used block cipher.
     */
    public GCM(BlockCipher bc) {
    	super(4);
    	
        this.bc = bc;
        if (bc.blockSize != 16)
            throw new IllegalArgumentException("cipher's block size must be 16 - " + bc.getClass().getName() + " has "
                    + bc.blockSize);

        if (bc.hasKey())
        	init();
    }

	private void init() {
		byte h[] = new byte[16];
        bc.encrypt(h, 0, h, 0);

        // create the lookup table
        final byte m[][][] = new byte[32][16][];
        byte b[];

        m[1][8] = h;

        for (int i = 8; i > 1;) {
            b = m[1][i].clone();
            i >>>= 1;
            if (shiftRight1Inplace(b))
                b[0] ^= 0xe1;
            m[1][i] = b;
        }

        b = m[1][1].clone();
        if (shiftRight1Inplace(b))
            b[0] ^= 0xe1;
        m[0][8] = b;

        for (int i = 8; i > 1;) {
            b = m[0][i].clone();
            i >>>= 1;
            if (shiftRight1Inplace(b))
                b[0] ^= 0xe1;
            m[0][i] = b;
        }

        for (int i = 0; i < 32; ++i) {
            m[i][0] = ZERO;
        }

        for (int i = 0;;) {
            for (int j = 2; j < 16; j += j) {
                for (int k = 1; k < j; ++k) {
                    b = m[i][j].clone();
                    xorInplace(b, m[i][k]);
                    m[i][j + k] = b;
                }
            }
            if (++i == 32)
                break;
            if (i > 1) {
                for (int j = 8; j > 0; j >>= 1) {
                    b = m[i - 2][j];
                    int c = b[15] & 0xff;
                    b = shiftRight8(b);
                    b[0] ^= R[c][0];
                    b[1] ^= R[c][1];
                    m[i][j] = b;
                }
            }
        }

        this.m = m;
	}

    /**
     * Shift the byte array v right by 1 bit.
     * 
     * @param v
     *            a byte array of length 16.
     * @return true if the lowest bit was set.
     */
    final private static boolean shiftRight1Inplace(final byte[] v) {
        int bit = 0;
        for (int i = 0; i < 16; ++i) {
            final int b = v[i] & 0xff;
            v[i] = (byte) ((b >>> 1) | bit);
            bit = (b & 1) << 7;
        }
        return bit != 0;
    }

    /**
     * XOR the byte array from into the byte array to. Both byte arrays must have the length 16.
     * 
     * @param to
     *            the byte array which is XORed.
     * @param from
     *            the byte array to XOR with.
     */
    final private static void xorInplace(final byte[] to, final byte[] from) {
        for (int i = 15; i >= 0; --i) {
            to[i] ^= from[i];
        }
    }

    /**
     * XOR the byte array from into the byte array to. The byte array to must at least have the length len.
     * 
     * @param to
     *            the byte array which is XORed.
     * @param from
     *            the byte array to XOR with.
     * @param fromOffset
     *            the offset into the byte array from.
     * @param len
     *            the length to XOR.
     */
    final private static void xorInplace(final byte[] to, final byte[] from, int fromOffset, int len) {
        for (int i = 0; i < len; ++i, ++fromOffset) {
            to[i] ^= from[fromOffset];
        }
    }

    /**
     * The multiplication with H using the lookup table m;
     * 
     * @param z
     *            the byte array to multiply.
     * @param m
     *            the lookup table.
     */
    /*
     * Algorithm2 Computes Z = X · H using the tables M0 and R.
     * Z ← 0
     * for i = 15 to 0 do
     *   Z ← Z ⊕M0[byte(X, i)]
     *   A ← byte(X, 15)
     *   for j = 15 to1 do 
     *     byte(X, j) ← byte(X, j − 1)
     *   end for
     *   Z ← Z ⊕ R[A]
     * end for
     * return Z
     */
    final private void mulHInplace(final byte z[], final byte m[][][]) {
        // clear tmp
        for (int i = 0; i < 16; ++i)
            tmp[i] = 0;

        for (int i = 15; i >= 0; --i) {
            byte b[] = m[i + i][z[i] & 0x0f];
            xorInplace(tmp, b);
            b = m[i + i + 1][(z[i] & 0xf0) >> 4];
            xorInplace(tmp, b);
        }
        System.arraycopy(tmp, 0, z, 0, 16);
    }

    /**
     * Shift the bytes one slot to the right and put the result into a new allocated byte array.
     * 
     * @param data
     *            the byte array to shift.
     * @return the shifted data.
     */
    private final static byte[] shiftRight8(byte[] data) {
        byte[] b = new byte[16];
        for (int i = 0; i < 15; ++i) {
            b[i + 1] = data[i];
        }
        return b;
    }

    /**
     * Encrypt the given clearText starting at clearOffset into the cipherText buffer at cipherOffset for the given
     * length. This method also updates the hash value. Note that <code>init(byte [])</code> must have been called.
     * 
     * @param clearText
     *            the clear text buffer.
     * @param clearOffset
     *            the clear text offset.
     * @param cipherText
     *            the cipher text buffer.
     * @param cipherOffset
     *            the cipher text offset.
     * @param length
     *            the length to encrypt.
     * @return the length.
     */
    public int encrypt(final byte[] clearText, int clearOffset, final byte[] cipherText, int cipherOffset,
            final int length) {
        for (int i = 0;; cipherOffset += 16, clearOffset += 16) {
            // next counter
            if (++nonceCounter[15] == 0) {
                if (++nonceCounter[14] == 0) {
                    if (++nonceCounter[13] == 0) {
                        ++nonceCounter[12];
                    }
                }
            }
            bc.encrypt(nonceCounter, 0, tmp, 0);

            // encrypt data
            final int t = i + 16;
            if (t >= length) {
                // handle partial data
                int len = length - i;
                xorInplace(tmp, clearText, clearOffset, len);

                // update hash
                xorInplace(hash, tmp, 0, len);

                // copy data
                System.arraycopy(tmp, 0, cipherText, cipherOffset, len);
                
                mulHInplace(hash, m);
                break;
            }
            i = t;
            xorInplace(tmp, clearText, clearOffset, 16);

            // update hash
            xorInplace(hash, tmp);

            // copy data
            System.arraycopy(tmp, 0, cipherText, cipherOffset, 16);

            mulHInplace(hash, m);
        }
        dataLen += length;
        return length;
    }

    /**
     * Decrypt the given cipherText starting at cipherOffset into the clearText buffer at clearOffset for the given
     * length. This method also updates the hash value. Note that <code>init(byte [])</code> must have been called.
     * 
     * @param clearText
     *            the clear text buffer.
     * @param clearOffset
     *            the clear text offset.
     * @param cipherText
     *            the cipher text buffer.
     * @param cipherOffset
     *            the cipher text offset.
     * @param length
     *            the length to decrypt.
     * @return the length.
     */
    public int decrypt(final byte[] cipherText, int cipherOffset, final byte[] clearText, int clearOffset,
            final int length) {
        for (int i = 0;; cipherOffset += 16, clearOffset += 16) {
            // next counter
            if (++nonceCounter[15] == 0) {
                if (++nonceCounter[14] == 0) {
                    if (++nonceCounter[13] == 0) {
                        ++nonceCounter[12];
                    }
                }
            }
            bc.encrypt(nonceCounter, 0, tmp, 0);

            // decrypt data
            final int t = i + 16;
            if (t >= length) {
                // handle partial data
                int len = length - i;
                xorInplace(tmp, cipherText, cipherOffset, len);
                
                // update hash
                xorInplace(hash, cipherText, cipherOffset, len);

                // copy data
                System.arraycopy(tmp, 0, clearText, clearOffset, len);

                mulHInplace(hash, m);
                break;
            }
            i = t;
            xorInplace(tmp, cipherText, cipherOffset, 16);

            // update hash
            xorInplace(hash, cipherText, cipherOffset, 16);
            
            // copy data
            System.arraycopy(tmp, 0, clearText, clearOffset, 16);
            
            mulHInplace(hash, m);
        }
        dataLen += length;
        return length;
    }

    /**
     * Initialize the data with the nonce. This method allows to reuse the GCM object.
     * 
     * @param nonce
     *            a nonce value of 12 bytes.
     */
    public void init(byte[] nonce) {
        for (int i = 0; i < 16; ++i)
            hash[i] = 0;

        dataLen = 0;
        aadLen = 0;

        if (nonce.length == 12) {
            System.arraycopy(nonce, 0, this.nonceCounter, 0, 12);
            this.nonceCounter[12] = 0;
            this.nonceCounter[13] = 0;
            this.nonceCounter[14] = 0;
            this.nonceCounter[15] = 1;
        } else {
            updateHash(nonce, 0, nonce.length);
            dataLen = aadLen;
            aadLen = 0;
            hash();
            System.arraycopy(hash, 0, nonceCounter, 0, 16);
            for (int i = 0; i < 16; ++i)
                hash[i] = 0;
            dataLen = 0;
        }
        bc.encrypt(this.nonceCounter, 0, cryptedNonceCounter1, 0);
    }

    /**
     * This method is used to feed Additional Authenticated Data into the hash.
     * 
     * @param aad
     *            the Additional Authenticated Data
     * @param offset
     *            the offset into aad
     * @param len
     *            the length
     */
    public void updateHash(byte[] aad, int offset, int len) {
        aadLen += len;
        while (len >= 16) {
            len -= 16;
            xorInplace(hash, aad, offset, 16);
            mulHInplace(hash, m);
            offset += 16;
        }
        // add partial
        if (len > 0) {
            xorInplace(hash, aad, offset, len);
            mulHInplace(hash, m);
        }
    }

    /**
     * After encrypt() or decrypt() this method calculates the hash and places it into to at toOffset.
     * 
     * @param to
     *            the destination byte array for the hash
     * @param toOffset
     *            the offset into to
     */
    public void calcHash(byte[] to, int toOffset) {
        hash();
        System.arraycopy(cryptedNonceCounter1, 0, tmp, 0, 16);
        xorInplace(tmp, hash);
        System.arraycopy(tmp, 0, to, toOffset, 16);
    }

    /** internal hash without cryptedNonceCounter1. */
    private void hash() {
        long pos = aadLen * 8;
        int hi = (int) (pos >>> 32);
        tmp[0] = (byte) (hi >>> 24);
        tmp[1] = (byte) (hi >>> 16);
        tmp[2] = (byte) (hi >>> 8);
        tmp[3] = (byte) (hi);
        int lo = (int) (pos);
        tmp[4] = (byte) (lo >>> 24);
        tmp[5] = (byte) (lo >>> 16);
        tmp[6] = (byte) (lo >>> 8);
        tmp[7] = (byte) (lo);

        pos = dataLen * 8;
        hi = (int) (pos >>> 32);
        tmp[8] = (byte) (hi >>> 24);
        tmp[9] = (byte) (hi >>> 16);
        tmp[10] = (byte) (hi >>> 8);
        tmp[11] = (byte) (hi);
        lo = (int) (pos);
        tmp[12] = (byte) (lo >>> 24);
        tmp[13] = (byte) (lo >>> 16);
        tmp[14] = (byte) (lo >>> 8);
        tmp[15] = (byte) (lo);

        xorInplace(hash, tmp);
        mulHInplace(hash, m);
    }

	@Override
	public void setKey(byte[] keyData) {
		bc.setKey(keyData);
		init();
	}

	@Override
	public void encrypt(byte[] clearText, int clearOff, byte[] cipherText, int cipherOff) {
		// dummy
	}

	@Override
	public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText, int clearOff) {
		// dummy
	}

	@Override
	public boolean hasKey() {
		return bc.hasKey();
	}
}
