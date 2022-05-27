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
 * This is a straightforward implementation of SHA-384 as given in NIST.FIPS.180 .
 * 
 * @version $
 */
public final class SHA384 extends MessageDigest {
    private long state0, state1, state2, state3, state4, state5, state6, state7;
    private long block[] = new long[80];

    private static long K[] = {
 0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
 0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
 0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L, 0xc19bf174cf692694L,
 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
 0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
 0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L,
 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
 0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
 0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L, 0x92722c851482353bL,
 0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
 0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L,
 0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
 0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
 0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
 0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L, 0xc67178f2e372532bL,
 0xca273eceea26619cL, 0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
 0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
 0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
 0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L,            
    };

    /**
     * constructor
     */
    public SHA384() {
        super("SHA384", 128);
        reset();
    }

    /**
     * clone implementation
     * 
     * @return a copy of the current object
     */
    public SHA384 clone() {
        SHA384 m = new SHA384();
        System.arraycopy(data, 0, m.data, 0, 128);
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
     * Hash a single 1024-bit block. This is the core of the algorithm.
     */

    final protected void transform() {
        int i = 0;
        final long block[] = this.block;
        // convert to int
        for (int j = 0; i < 16; j += 8) {
            block[i++] =
                    (((long)data[j] & 0xff) << 56) | (((long)data[j + 1] & 0xff) << 48) 
                    | ((long)(data[j + 2] & 0xff) << 40) | (((long)data[j + 3] & 0xff) << 32)
                    | ((long)(data[j + 4] & 0xff) << 24) | (((long)data[j + 5] & 0xff) << 16)
                    | ((long)(data[j + 6] & 0xff) << 8) | (((long)data[j + 7] & 0xff));
        }

        for (; i < 80; ++i) {
            long s0 = roll(block[i - 15], 63) ^ roll(block[i - 15], 56) ^ (block[i - 15] >>> 7);
            long s1 = roll(block[i - 2], 45) ^ roll(block[i - 2], 3) ^ (block[i - 2] >>> 6);
            block[i] = block[i - 16] + s0 + block[i - 7] + s1;
        }

        /* Copy context->state[] to working vars */
        long t0, t1, t2, t3, t4, t5, t6, t7;

        t0 = state0;
        t1 = state1;
        t2 = state2;
        t3 = state3;
        t4 = state4;
        t5 = state5;
        t6 = state6;
        t7 = state7;

        final long k[] = K;
        for (i = 0; i < 80; ++i) {
            final long s0 = roll(t0, 36) ^ roll(t0, 30) ^ roll(t0, 25);
            final long maj = (t0 & t1) ^ (t0 & t2) ^ (t1 & t2);
            final long add2 = s0 + maj;
            final long s1 = roll(t4, 50) ^ roll(t4, 46) ^ roll(t4, 23);
            final long ch = (t4 & t5) ^ ((~t4) & t6);
            final long add1 = t7 + s1 + ch + k[i] + block[i];

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

    final static protected long roll(long value, long bits) {
        return (value << bits) | (value >>> (64 - bits));
    }

    
    /**
     * Initialize new context
     */
    public void reset() {
        state0 = 0xcbbb9d5dc1059ed8L;
        state1 = 0x629a292a367cd507L;
        state2 = 0x9159015a3070dd17L;
        state3 = 0x152fecd8f70e5939L;
        state4 = 0x67332667ffc00b31L;
        state5 = 0x8eb44a8768581511L;
        state6 = 0xdb0c2e0d64f98fa7L;
        state7 = 0x47b5481dbefa4fa4L;
        /**/
        count = 0;
    }

    protected void addBitCount(long bitCount) {
        for (int i = 127; i >= 112; --i) {
            data[i] = (byte) bitCount;
            bitCount >>= 8;
        }
    }

    protected byte[] getDigest() {
        return new byte[]{
        		(byte) (state0 >> 56), (byte) (state0 >> 48), (byte) (state0 >> 40), (byte) (state0 >> 32), 
        		(byte) (state0 >> 24), (byte) (state0 >> 16), (byte) (state0 >> 8), (byte) (state0),
        		(byte) (state1 >> 56), (byte) (state1 >> 48), (byte) (state1 >> 40), (byte) (state1 >> 32), 
        		(byte) (state1 >> 24), (byte) (state1 >> 16), (byte) (state1 >> 8), (byte) (state1),
        		(byte) (state2 >> 56), (byte) (state2 >> 48), (byte) (state2 >> 40), (byte) (state2 >> 32), 
        		(byte) (state2 >> 24), (byte) (state2 >> 16), (byte) (state2 >> 8), (byte) (state2),
        		(byte) (state3 >> 56), (byte) (state3 >> 48), (byte) (state3 >> 40), (byte) (state3 >> 32), 
        		(byte) (state3 >> 24), (byte) (state3 >> 16), (byte) (state3 >> 8), (byte) (state3),
        		(byte) (state4 >> 56), (byte) (state4 >> 48), (byte) (state4 >> 40), (byte) (state4 >> 32), 
        		(byte) (state4 >> 24), (byte) (state4 >> 16), (byte) (state4 >> 8), (byte) (state4),
        		(byte) (state5 >> 56), (byte) (state5 >> 48), (byte) (state5 >> 40), (byte) (state5 >> 32), 
        		(byte) (state5 >> 24), (byte) (state5 >> 16), (byte) (state5 >> 8), (byte) (state5),
                };
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
