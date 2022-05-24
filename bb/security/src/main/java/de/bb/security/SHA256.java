/******************************************************************************
 * $Source: /export/CVS/java/de/bb/security/src/main/java/de/bb/security/SHA256.java,v $
 * $Revision: 1.2 $
 * $Date: 2014/04/13 20:10:04 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Base class for all message digests
 *
 *****************************************************************************/

package de.bb.security;

/**
 * This is a straightforward implementation of SHA-256 as given in NIST.FIPS.180 .
 * 
 * @version $
 */
public final class SHA256 extends MessageDigest {
    private int state0, state1, state2, state3, state4, state5, state6, state7;
    private int block[] = new int[64];

    private static int K[] = {0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
            0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152,
            0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138,
            0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b, 0xc24b8b70,
            0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa,
            0xa4506ceb, 0xbef9a3f7, 0xc67178f2};

    /**
     * constructor
     */
    public SHA256() {
        super("SHA256", 64);
        reset();
    }

    /**
     * clone implementation
     * 
     * @return a copy of the current object
     */
    public SHA256 clone() {
        SHA256 m = new SHA256();
        System.arraycopy(data, 0, m.data, 0, 64);
        m.count = count;
        m.state0 = state0;
        m.state1 = state1;
        m.state2 = state2;
        m.state3 = state3;
        m.state4 = state4;
        m.state5 = state5;
        m.state6 = state6;
        m.state7 = state7;

        return m;
    }

    /**
     * Hash a single 512-bit block. This is the core of the algorithm.
     */

    final protected void transform() {
        int i = 0;
        final int block[] = this.block;
        // convert to int
        for (int j = 0; i < 16; j += 4) {
            block[i++] =
                    ((data[j]) << 24) | ((data[j + 1] & 0xff) << 16) | ((data[j + 2] & 0xff) << 8)
                            | ((data[j + 3] & 0xff));
        }

        for (; i < 64; ++i) {
            int s0 = rol(block[i - 15], 25) ^ rol(block[i - 15], 14) ^ (block[i - 15] >>> 3);
            int s1 = rol(block[i - 2], 15) ^ rol(block[i - 2], 13) ^ (block[i - 2] >>> 10);
            block[i] = block[i - 16] + s0 + block[i - 7] + s1;
        }

        /* Copy context->state[] to working vars */
        int t0, t1, t2, t3, t4, t5, t6, t7;

        t0 = state0;
        t1 = state1;
        t2 = state2;
        t3 = state3;
        t4 = state4;
        t5 = state5;
        t6 = state6;
        t7 = state7;

        final int k[] = K;
        for (i = 0; i < 64; ++i) {
            final int s0 = rol(t0, 30) ^ rol(t0, 19) ^ rol(t0, 10);
            final int maj = (t0 & t1) ^ (t0 & t2) ^ (t1 & t2);
            final int add2 = s0 + maj;
            final int s1 = rol(t4, 26) ^ rol(t4, 21) ^ rol(t4, 7);
            final int ch = (t4 & t5) ^ ((~t4) & t6);
            final int add1 = t7 + s1 + ch + k[i] + block[i];

            t7 = t6;
            t6 = t5;
            t5 = t4;
            t4 = t3 + add1;
            t3 = t2;
            t2 = t1;
            t1 = t0;
            t0 = add1 + add2;
        }

        /* Add the working vars back into context.state[] */
        state0 += t0; // a
        state1 += t1; // b
        state2 += t2; // c
        state3 += t3; // d
        state4 += t4; // e
        state5 += t5; // f
        state6 += t6; // g
        state7 += t7; // h
    }

    /**
     * Initialize new context
     */
    public void reset() {
        state0 = 0x6a09e667;
        state1 = 0xbb67ae85;
        state2 = 0x3c6ef372;
        state3 = 0xa54ff53a;
        state4 = 0x510e527f;
        state5 = 0x9b05688c;
        state6 = 0x1f83d9ab;
        state7 = 0x5be0cd19;
        /**/
        count = 0;
    }

    protected void addBitCount(long bitCount) {
        for (int i = 63; i >= 56; --i) {
            data[i] = (byte) bitCount;
            bitCount >>= 8;
        }
    }

    protected byte[] getDigest() {
        return new byte[]{(byte) (state0 >> 24), (byte) (state0 >> 16), (byte) (state0 >> 8), (byte) (state0),
                (byte) (state1 >> 24), (byte) (state1 >> 16), (byte) (state1 >> 8), (byte) (state1),
                (byte) (state2 >> 24), (byte) (state2 >> 16), (byte) (state2 >> 8), (byte) (state2),
                (byte) (state3 >> 24), (byte) (state3 >> 16), (byte) (state3 >> 8), (byte) (state3),
                (byte) (state4 >> 24), (byte) (state4 >> 16), (byte) (state4 >> 8), (byte) (state4),
                (byte) (state5 >> 24), (byte) (state5 >> 16), (byte) (state5 >> 8), (byte) (state5),
                (byte) (state6 >> 24), (byte) (state6 >> 16), (byte) (state6 >> 8), (byte) (state6),
                (byte) (state7 >> 24), (byte) (state7 >> 16), (byte) (state7 >> 8), (byte) (state7),};
    }
}

/**
 * $Log: SHA256.java,v $
 * Revision 1.2  2014/04/13 20:10:04  bebbo
 * @R clone() is typed now
 *
 * Revision 1.1  2012/08/19 15:26:37  bebbo
 * @N added SHA256
 * @R added support for TLS1.2
 *
 */
