package de.bb.security;


import de.bb.util.Misc;

public class ECMath {

	// 0 = secp256r1, 1 = secp384r1, 2 = secp521r1
	static byte CURVES[][][] = { {
			// p
			Misc.hex2Bytes("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff"),
			// a
			Misc.hex2Bytes("ffffffff00000001000000000000000000000000fffffffffffffffffffffffc"),
			// b
			Misc.hex2Bytes("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b"),
			// p - 2
			Misc.hex2Bytes("ffffffff00000001000000000000000000000000fffffffffffffffffffffffd"),
			// G
			Misc.hex2Bytes("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296"),
			Misc.hex2Bytes("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5") },
			{
					// p
					Misc.hex2Bytes(
							"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff"),
					// a
					Misc.hex2Bytes(
							"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000fffffffc"),
					// b
					Misc.hex2Bytes(
							"b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef"),
					// p - 2
					Misc.hex2Bytes(
							"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000fffffffd"),
					// G
					Misc.hex2Bytes(
							"aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760ab7"),
					Misc.hex2Bytes(
							"3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5f") },
			{
					// p
					Misc.hex2Bytes(
							"01ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"),
					// a
					Misc.hex2Bytes(
							"01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc"),
					// b
					Misc.hex2Bytes(
							"0051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00"),
					// p - 2
					Misc.hex2Bytes(
							"01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd"),
					// G
					Misc.hex2Bytes(
							"00c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66"),
					Misc.hex2Bytes(
							"011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650") } };

	public static byte[] genPrivateKey(int curve) {
		byte b[] = Ssl3.random(byteLength(curve));
		if (curve == 0x19) {
			b[0] = 0;
			b[1] |= 0x80;
		} else {
			b[0] |= 8;
		}
		b[b.length - 1] |= 0x40;
		return b;
	}

	public static int byteLength(int curve) {
		byte curveData[][] = CURVES[curve - 0x17];
		return curveData[0].length;
	}

	public static byte[][] pub(int curve, byte[] scalar) {
		byte curveData[][] = CURVES[curve - 0x17];
		return mult(curve, curveData[4], curveData[5], scalar); // Gx, Gy
	}

	
	public static byte[][] mult(int curve, byte []ptx, byte [] pty, byte[] scalar) {
		byte curveData[][] = CURVES[curve - 0x17];

		byte[] mod = curveData[0]; // p
		byte[] p_2 = curveData[3]; // p - 2
		
		// byte[] ptx, byte[] pty, byte[] mod, byte[] a,
		int modLen = (mod.length + 3) >> 2; 
		int len = modLen + modLen;
		int pki[] = FastMath32.byte2Int(scalar, len + 1);
		int modi[] = FastMath32.byte2Int(mod, modLen);
		int ax[] = FastMath32.byte2Int(ptx, len + 1);
		int ay[] = FastMath32.byte2Int(pty, len + 1);
		int ia[] = FastMath32.byte2Int(curveData[1], modLen);
		int t0[] = new int[len + 1];
		int t1[] = new int[len + 1];
		int t2[] = new int[len + 1];
		int t3[] = new int[len + 1];
		int t4[] = new int[len + 1];
		int t5[] = new int[len + 1];
		int t6[] = new int[len + 1];
		int tt[][] = new int[][]{t0, t1, t2, t3, t4, t5, t6};

		FastMath32.mod(pki, modi, t1, t2, len, modLen);
		
		int bx[] = null;
		int by[] = null;
		
		for (int index = modLen * 32 - 1; ; --index) {
			if ((pki[index >> 5] >> (index & 31) & 1) == 1) {
				if (bx == null) {
					bx = ax.clone();
					by = ay.clone();
				} else { 
					__m(modi, ax, ay, bx, by, ia, tt, p_2);
				}
			}
			if (index == 0)
				break;
			
			if (bx == null)
				continue;
			
			__m(modi, bx, by, bx, by, ia, tt, p_2);
			
		}
		
		byte[] rx = new byte[mod.length];
		byte[] ry = new byte[mod.length];
		FastMath32.int2Byte(bx, rx);
		FastMath32.int2Byte(by, ry);
		return new byte[][] {rx, ry};
	}

	private static void __m(int[] mod, int[] ax, int[] ay, int[] bx, int[] by, int ia[], int tt[][], byte[] p_2) {
		int len = ax.length & ~1;
		int modLen = len >> 1;
		int t0[] = tt[0];
		int t1[] = tt[1];
		int t2[] = tt[2];
		int t3[] = tt[3];
		int t4[] = tt[4];
		int rx[] = tt[5]; 
		int ry[] = tt[6]; 
		
		if (Misc.equals(ax, bx)) {
			// BigInteger temp = mult(bb.x, bb.x); -> t0
			FastMath32.mul(t0, bx, bx, modLen);
			FastMath32.mod(t0, mod, t1, t2, len, modLen);
			
			// BigInteger temp3 = add(add(temp, temp), temp); ->
			if (FastMath32.add(t3, t0, modLen, t0, modLen) || FastMath32.isGreater(mod, t3, modLen))
				FastMath32.sub(t3, t3, mod, modLen);
			
			if (FastMath32.add(t3, t3, modLen, t0, modLen) || FastMath32.isGreater(mod, t3, modLen)) 
				FastMath32.sub(t3, t3, mod, modLen);

			// add(temp3, a)
			if (FastMath32.add(t3, ia, modLen, t3, modLen) || FastMath32.isGreater(mod, t3, modLen)) 
				FastMath32.sub(t3, t3, mod, modLen);

			// add(aa.y, aa.y)
			if (FastMath32.add(t4, ay, modLen, ay, modLen) || FastMath32.isGreater(mod, t4, modLen))
				FastMath32.sub(t4, t4, mod, modLen);

			// add(aa.y, aa.y).modInverse(p)
			int r[] = FastMath32.invertP2(t4, t1, t2, rx, ry, p_2, mod);	

			// mult(t3a, add(aa.y, aa.y).modInverse(p));
			FastMath32.mul(t0, t3, r, modLen);
			FastMath32.mod(t0, mod, t1, t2, len, modLen);
		} else {
			
			// sub(aa.y, bb.y)
			if (FastMath32.sub(t3, ay, by, modLen))				
				FastMath32.add(t3, t3, modLen, mod, modLen);

			// sub(aa.x, bb.x)
			if (FastMath32.sub(t4, ax, bx, modLen))
				FastMath32.add(t4, t4, modLen, mod, modLen);

			// sub(aa.x, bb.x).modInverse(p)
			int r[] = FastMath32.invertP2(t4, t1, t2, rx, ry, p_2, mod);
			
			// mult(sub(aa.y, bb.y), sub(aa.x, bb.x).modInverse(p))
			FastMath32.mul(t0, t3, r, modLen);
			FastMath32.mod(t0, mod, t1, t2, len, modLen);
		}
		
		rx[modLen] = 0;
		ry[modLen] = 0;
		
		// BigInteger bx = sub(sub(square(m), aa.x), bb.x);
		FastMath32.square(t3, t0, modLen);
		FastMath32.mod(t3, mod, t1, t2, len, modLen);
		
		if (FastMath32.sub(t3, t3, ax, modLen))
			FastMath32.add(t3, t3, modLen, mod, modLen);
		
		if (FastMath32.sub(rx, t3, bx, modLen))
			FastMath32.add(rx, rx, modLen, mod, modLen);

		// sub(p, add(aa.y, mult(m, sub(bx, aa.x))));
		// sub(bx, aa.x)
		if (FastMath32.sub(t4, rx, ax, modLen))
			FastMath32.add(t4, t4, modLen, mod, modLen);

		// mult(m, sub(bx, aa.x))
		FastMath32.mul(ry, t0, t4, modLen);
		FastMath32.mod(ry, mod, t1, t2, len, modLen);

		// add(aa.y, mult(m, sub(bx, aa.x))) 
		if (FastMath32.add(ry, ay, modLen, ry, modLen) || FastMath32.isGreater(mod, ry, modLen))
			FastMath32.sub(ry, ry, mod, modLen);

		// sub(p, add(aa.y, mult(m, sub(bx, aa.x))))
		if (FastMath32.sub(ry, mod, ry, modLen))
			FastMath32.add(ry, ry, modLen, mod, modLen);
		
		System.arraycopy(rx, 0, bx, 0, len);
		System.arraycopy(ry, 0, by, 0, len);
	}


	/**
	 * Verify the point is on the given curve.
	 * @param curve the curve number from TLS: 0x17, 0x18, 0x19. 
	 * @param ptx
	 * @param pty
	 * @return true if the point is on the curve
	 */
	public static boolean verify(int curve, byte[] ptx, byte[] pty) {
		byte curveData[][] = CURVES[curve - 0x17];
		byte[] cmod = curveData[0]; // p
		int modLen = (cmod.length + 3) >> 2; 
		int len = modLen + modLen;

		int mod[] = FastMath32.byte2Int(cmod, modLen);
		int a[] = FastMath32.byte2Int(curveData[1], modLen);
		int b[] = FastMath32.byte2Int(curveData[2], modLen);

		int x[] = FastMath32.byte2Int(ptx, modLen); 
		int y[] = FastMath32.byte2Int(pty, modLen); 
		
		/* Verify y^2 = x^3 + ax + b */
		int t1[] = new int[len + 1];
		int t2[] = new int[len + 1];
		int y2[] = new int[len + 1];
		int ax[] = new int[len + 1];
		int x3[] = new int[len + 1];
		
		// y^2
		FastMath32.mul(y2, y, y, modLen);
		FastMath32.mod(y2, mod, t1, t2, len, modLen);
		
		// x^3
		FastMath32.mul(ax, x, x, modLen);
		FastMath32.mod(ax, mod, t1, t2, len, modLen);
		FastMath32.mul(x3, ax, x, modLen);
		FastMath32.mod(x3, mod, t1, t2, len, modLen);

		// ax
		FastMath32.mul(ax, a, x, modLen);
		FastMath32.mod(ax, mod, t1, t2, len, modLen);

		// x^3 + ax + b
		if (FastMath32.add(x3, x3, modLen, ax, modLen) || FastMath32.isGreater(mod, x3, modLen))
			FastMath32.sub(x3, x3, mod, modLen);

		if (FastMath32.add(x3, x3, modLen, b, modLen)  || FastMath32.isGreater(mod, x3, modLen))
			FastMath32.sub(x3, x3, mod, modLen);

		return Misc.equals(y2, x3);
	}

	/**
	 * X25519
	 */
	static int X25519_A[] = new int[] { 121665, 0, 0, 0, 0, 0, 0, 0, };
	static int X25519_P[] = new int[] { 0xffffffed, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,
			0x7fffffff, };
	static byte X25519_9[] = new byte[32];
	static byte X25519_P_2[] = new byte[32];
	static {
		FastMath32.int2Byte(X25519_P, X25519_P_2);
		X25519_P_2[31] -= 2;
		X25519_9[0] = 9;
	}
	
	public static byte[] x25519Pub(byte[] pk) {
		return ECMath.x25519(pk, X25519_9);
	}

	public static byte[] x25519(byte scalar[], byte point[]) {
		// clear high bit
		point[31] &= 0x7f;
	
		// clamp the scalar
		scalar[31] &= 0x7f;
		scalar[31] |= 0x40;
		scalar[0] &= 0xf8;
	
		// the point is little endian, byte2int for bigendian => reverse it
		int x1[] = FastMath32.byte2Int(FastMath32.reverse(point), 16);
	
		int x2[] = new int[16];
		x2[0] = 1;
		int z2[] = new int[16];
		int x3[] = x1.clone();
		int z3[] = x2.clone();
	
		int a_e[] = new int[16];
		int b_m[] = new int[16];
		int c_aa[] = new int[16];
		int d_bb[] = new int[16];
		int cb[] = new int[16];
		int da[] = new int[16];
	
		int t3[] = new int[16];
		int t4[] = new int[16];
	
		int swap = 0;
		for (int index = 254; index >= 0; --index) {
			int t[];
			int bit = (1 & scalar[index >> 3] >> (index & 7));
			if (swap != bit) {
				t = x2;
				x2 = x3;
				x3 = t;
	
				t = z2;
				z2 = z3;
				z3 = t;
			}
			swap = bit;
	
			if (FastMath32.add(a_e, x2, 8, z2, 8) || a_e[7] < 0)
				FastMath32.sub(a_e, a_e, X25519_P, 8);
			if (FastMath32.sub(b_m, x2, z2, 8))
				FastMath32.add(b_m, b_m, 8, X25519_P, 8);
	
			if (FastMath32.add(c_aa, x3, 8, z3, 8) || c_aa[7] < 0)
				FastMath32.sub(c_aa, c_aa, X25519_P, 8);
			if (FastMath32.sub(d_bb, x3, z3, 8))
				FastMath32.add(d_bb, d_bb, 8, X25519_P, 8);
	
			FastMath32.mul(cb, c_aa, b_m, 8);
			FastMath32.mod(cb, X25519_P, t3, t4, 16, 8);
	
			FastMath32.mul(da, d_bb, a_e, 8);
			FastMath32.mod(da, X25519_P, t3, t4, 16, 8);
	
			FastMath32.square(c_aa, a_e, 8);
			FastMath32.mod(c_aa, X25519_P, t3, t4, 16, 8);
	
			FastMath32.square(d_bb, b_m, 8);
			FastMath32.mod(d_bb, X25519_P, t3, t4, 16, 8);
	
			if (FastMath32.sub(a_e, c_aa, d_bb, 8))
				FastMath32.add(a_e, a_e, 8, X25519_P, 8);
	
			FastMath32.mul(b_m, X25519_A, a_e, 8); // using 121665
			FastMath32.mod(b_m, X25519_P, t3, t4, 16, 8);
	
			if (FastMath32.add(b_m, c_aa, 8, b_m, 8))
				FastMath32.sub(b_m, b_m, X25519_P, 8);
	
			FastMath32.mul(x2, c_aa, d_bb, 8);
			FastMath32.mod(x2, X25519_P, t3, t4, 16, 8);
	
			FastMath32.mul(z2, a_e, b_m, 8);
			FastMath32.mod(z2, X25519_P, t3, t4, 16, 8);
	
			if (FastMath32.add(b_m, da, 8, cb, 8) || b_m[7] < 0)
				FastMath32.sub(b_m, b_m, X25519_P, 8);
	
			FastMath32.square(x3, b_m, 8);
			FastMath32.mod(x3, X25519_P, t3, t4, 16, 8);
	
			if (FastMath32.sub(b_m, da, cb, 8))
				FastMath32.add(b_m, b_m, 8, X25519_P, 8);
	
			FastMath32.square(a_e, b_m, 8);
			FastMath32.mod(a_e, X25519_P, t3, t4, 16, 8);
	
			FastMath32.mul(z3, x1, a_e, 8);
			FastMath32.mod(z3, X25519_P, t3, t4, 16, 8);
		}
	
		z2 = FastMath32.invertP2(z2, x3, z3, t3, t4, X25519_P_2, X25519_P);
		FastMath32.mul(a_e, z2, x2, 8);
		FastMath32.mod(a_e, X25519_P, t3, t4, 16, 8);
	
		byte[] result = new byte[32];
		return FastMath32.reverse(FastMath32.int2Byte(a_e, result));
	}

}
