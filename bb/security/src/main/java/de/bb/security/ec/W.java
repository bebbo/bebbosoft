package de.bb.security.ec;

import java.math.BigInteger;

class W extends EC {
	BigInteger b, n;
	P g;

	W(String id, byte[] p, byte[] a, byte[] b, byte[] n, byte[] gx, byte[] gy) {
		super(id, p, a);
		this.b = new BigInteger(b);
		this.n = new BigInteger(n);
		g = new P(gx, gy);
	}

	boolean aminus3() {
		return a.add(EC.THREE).equals(p);
	}

	boolean verify(P aa) {
		if (aa.infinity)
			return true;
		/* Verify y^2 = x^3 + ax + b */
		BigInteger lhs = square(aa.y);
		BigInteger x3 = aa.x.modPow(EC.THREE, p);
		BigInteger ax = aa.x.multiply(a).mod(p);
		BigInteger rhs = x3.add(ax).mod(p).add(b).mod(p);
		return lhs.equals(rhs);
	}

	boolean _normalize(P aa) {
		if (aa.x == null || aa.y == null)
			return false;

		if (aa.z == null)
			return true;

		BigInteger z2 = square(aa.z);
		BigInteger z2inv = z2.modInverse(p);
		BigInteger tx = aa.x.multiply(z2inv).mod(p);
		BigInteger z3 = z2.multiply(aa.z).mod(p);
		BigInteger z3inv = z3.modInverse(p);
		BigInteger ty = aa.y.multiply(z3inv).mod(p);

		aa.x = tx;
		aa.y = ty;
		aa.z = null;

		return true;
	}

	P _add(P aa, P bb, boolean aminus3) {
		/* U1 = X1*Z2^2 */
		/* S1 = Y1*Z2^3 */
		BigInteger u1, s1;
		if (bb.z != null) {
			BigInteger bz2 = square(bb.z);
			BigInteger bz3 = bz2.multiply(bb.z).mod(p);
			u1 = aa.x.multiply(bz2).mod(p);
			s1 = aa.y.multiply(bz3).mod(p);
		} else {
			u1 = aa.x;
			s1 = aa.y;
		}

		/* U2 = X2*Z1^2 */
		/* S2 = Y2*Z1^3 */
		BigInteger u2, s2;
		if (aa.z != null) {
			BigInteger az2 = square(aa.z);
			BigInteger az3 = az2.multiply(aa.z).mod(p);
			u2 = bb.x.multiply(az2).mod(p);
			s2 = bb.y.multiply(az3).mod(p);
		} else {
			u2 = bb.x;
			s2 = bb.y;
		}

		if (u1.equals(u2)) {
			if (s1.equals(s2))
				return mul2(aa, aminus3);
			return INFINITY;
		}

		/* H = U2 - U1 */
		BigInteger h = sub(u2, u1);

		/* R = S2 - S1 */
		BigInteger r = sub(s2, s1);

		/* X3 = R^2 - H^3 - 2*U1*H^2 */
		BigInteger r2 = square(r);
		BigInteger h2 = square(h);
		BigInteger h3 = h2.multiply(h).mod(p);
		BigInteger x3 = sub(sub(r2, h3), TWO.multiply(u1).mod(p).multiply(h2).mod(p));

		/* Y3 = R*(U1*H^2 - X3) - S1*H^3 */
		BigInteger y3 = sub(r.multiply(sub(u1.multiply(h2).mod(p), x3)).mod(p), s1.multiply(h3).mod(p));

		/* Z3 = H*Z1*Z2 */
		BigInteger z3 = h.multiply(aa.z).mod(p).multiply(bb.z).mod(p);

		return new P(x3, y3, z3);
	}

	P _mul2(P aa, boolean aminus3) {
		if (aa.y.equals(ZERO))
			return INFINITY;

		BigInteger y2 = square(aa.y);
		BigInteger xy2 = aa.x.multiply(y2).mod(p);
		BigInteger twice_xy2 = mul2(xy2);
		BigInteger s = mul2(twice_xy2);
		BigInteger m;

		if (aminus3) {
			BigInteger z2 = aa.z == null ? ONE : square(aa.z);
			BigInteger xpz2 = add(aa.x, z2);
			BigInteger xmz2 = sub(aa.x, z2);
			BigInteger second = xpz2.multiply(xmz2).mod(p);
			m = mul3(second);
		} else {
			BigInteger az4 = aa.z == null ? a : a.multiply(square(square(aa.z))).mod(p);
			BigInteger x2 = square(aa.x);
			BigInteger triplex2 = mul3(x2);
			m = add(triplex2, az4);
		}

		BigInteger m2 = square(m);
		BigInteger twices = mul2(s);
		BigInteger outx = sub(m2, twices);

		BigInteger sx = sub(s, outx);
		BigInteger msx = m.multiply(sx).mod(p);
		BigInteger y4 = square(y2);
		BigInteger eighty4 = y4.multiply(EIGHT).mod(p);
		BigInteger outy = sub(msx, eighty4);

		BigInteger yz = aa.z == null ? aa.y : aa.y.multiply(aa.z).mod(p);
		BigInteger outz = mul2(yz);

		return new P(outx, outy, outz);
	}
}
