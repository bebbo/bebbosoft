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
		b[0] |= 8;
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
		
//		Misc.dump("m " , System.out, FastMath32.int2Byte(pki, null));

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
			
//			if (index > 252) {			
//				Misc.dump("bx " + index, System.out, FastMath32.int2Byte(bx, null));
//				Misc.dump("by " + index, System.out, FastMath32.int2Byte(by, null));
//			}
			
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
			
//			Misc.dump("temp ", System.out, FastMath32.int2Byte(t0, null));
			
			// BigInteger temp3 = add(add(temp, temp), temp); ->
			if (FastMath32.add(t3, t0, modLen, t0, modLen))
				FastMath32.sub(t3, t3, mod, modLen);
			
			if (FastMath32.add(t3, t3, modLen, t0, modLen)) 
				FastMath32.sub(t3, t3, mod, modLen);

//			Misc.dump("temp3 ", System.out, FastMath32.int2Byte(t3, null));

			
			// add(temp3, a)
			if (FastMath32.add(t3, ia, modLen, t3, modLen)) 
				FastMath32.sub(t3, t3, mod, modLen);

//			Misc.dump("t3a ", System.out, FastMath32.int2Byte(t3, null));

			// add(aa.y, aa.y)
			if (FastMath32.add(t4, ay, modLen, ay, modLen))
				FastMath32.sub(t4, t4, mod, modLen);

//			Misc.dump("aay ", System.out, FastMath32.int2Byte(t4, null));

			
			// add(aa.y, aa.y).modInverse(p)
			int r[] = FastMath32.invertP2(t4, t1, t2, rx, ry, p_2, mod);	

//			Misc.dump("mdinvers ", System.out, FastMath32.int2Byte(r, null));

			// mult(t3a, add(aa.y, aa.y).modInverse(p));
			FastMath32.mul(t0, t3, r, modLen);
			FastMath32.mod(t0, mod, t1, t2, len, modLen);
		} else {
			
			// sub(aa.y, bb.y)
			if (FastMath32.sub(t3, ay, by, modLen))				
				FastMath32.add(t3, t3, modLen, mod, modLen);

//			Misc.dump("ayby", System.out, FastMath32.int2Byte(t3, null));
			
			// sub(aa.x, bb.x)
			if (FastMath32.sub(t4, ax, bx, modLen))
				FastMath32.add(t4, t4, modLen, mod, modLen);

//			Misc.dump("axbx", System.out, FastMath32.int2Byte(t4, null));

			// sub(aa.x, bb.x).modInverse(p)
			int r[] = FastMath32.invertP2(t4, t1, t2, rx, ry, p_2, mod);
//			Misc.dump("mdinvers ", System.out, FastMath32.int2Byte(r, null));
			
			// mult(sub(aa.y, bb.y), sub(aa.x, bb.x).modInverse(p))
			FastMath32.mul(t0, t3, r, modLen);
			FastMath32.mod(t0, mod, t1, t2, len, modLen);
		}
		
		rx[8] = 0;
		ry[8] = 0;
		
//		Misc.dump("m=", System.out, FastMath32.int2Byte(t0, null));
		
		// BigInteger bx = sub(sub(square(m), aa.x), bb.x);
		FastMath32.square(t3, t0, modLen);
		FastMath32.mod(t3, mod, t1, t2, len, modLen);
		
		if (FastMath32.sub(t3, t3, ax, modLen))
			FastMath32.add(t3, t3, modLen, mod, modLen);
		
		if (FastMath32.sub(rx, t3, bx, modLen))
			FastMath32.add(rx, rx, modLen, mod, modLen);

//		Misc.dump("rx=", System.out, FastMath32.int2Byte(rx, null));

		
		// BigInteger by = sub(p, add(aa.y, mult(m, sub(bx, aa.x))));
		// sub(bx, aa.x)
		if (FastMath32.sub(t4, rx, ax, modLen))
			FastMath32.add(t4, t4, modLen, mod, modLen);

//		Misc.dump("xraax=", System.out, FastMath32.int2Byte(t4, null));

		
		// mult(m, sub(bx, aa.x))
		FastMath32.mul(ry, t0, t4, modLen);
		FastMath32.mod(ry, mod, t1, t2, len, modLen);

//		Misc.dump("mxraax=", System.out, FastMath32.int2Byte(ry, null));

		// add(aa.y, mult(m, sub(bx, aa.x))) 
		if (FastMath32.add(ry, ay, modLen, ry, modLen))
			FastMath32.sub(ry, ry, mod, modLen);

//		Misc.dump("aaymxraax=", System.out, FastMath32.int2Byte(ry, null));

		
		// sub(p, add(aa.y, mult(m, sub(bx, aa.x))))
		if (FastMath32.sub(ry, mod, ry, modLen))
			FastMath32.add(ry, ry, modLen, mod, modLen);
		
//		Misc.dump("ry=", System.out, FastMath32.int2Byte(ry, null));
		
		
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
		if (FastMath32.add(x3, x3, modLen, ax, modLen))
			FastMath32.sub(x3, x3, mod, modLen);

		if (FastMath32.add(x3, x3, modLen, b, modLen))
			FastMath32.sub(x3, x3, mod, modLen);

		return Misc.equals(y2, x3);
	}

}
