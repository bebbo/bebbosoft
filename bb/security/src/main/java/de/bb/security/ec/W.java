package de.bb.security.ec;

import java.math.BigInteger;

public class W extends EC {
	BigInteger b, n;

	public W(String id, byte[] p, byte[] a, byte[] b, byte[] n, byte[] gx, byte[] gy) {
		super(id, p, a, gx, gy);
		this.b = new BigInteger(1, b, 0, b.length);
		if (n == null)
			this.n = this.p.add(ONE);
		else
			this.n = new BigInteger(1, n, 0, n.length);
	}

	public boolean verify(P aa) {
		if (aa.infinity())
			return true;
		/* Verify y^2 = x^3 + ax + b */
		BigInteger lhs = square(aa.y);
		BigInteger x3 = mult(aa.x, mult(aa.x, aa.x));
		BigInteger ax = mult(aa.x, a);
		BigInteger rhs = add(add(x3, ax), b);
		return lhs.equals(rhs);
	}

	@Override
	P _add(P aa, P bb) {
		BigInteger m = __m(aa, bb);
		BigInteger xr = sub(sub(square(m), aa.x), bb.x);
		BigInteger yr = sub(p, add(aa.y, mult(m, sub(xr, aa.x))));
		return new P(xr, yr);
	}

	private BigInteger __m(P aa, P bb) {
        if (aa.x.equals(bb.x)) {
            BigInteger temp = mult(bb.x, bb.x);
			BigInteger temp3 = add(add(temp, temp), temp);
			return mult(add(temp3, a), add(aa.y, aa.y).modInverse(p));
        }
        return mult(sub(aa.y, bb.y), sub(aa.x, bb.x).modInverse(p));
	}

	public P mul(P aa, long n) {
		return mul(aa, BigInteger.valueOf(n));
	}
	
	public P mul(P aa, BigInteger m) {
		m = m.mod(n);
		if (m.equals(ZERO))
			return new P();
		
		if (m.signum() < 0) {
			m = m.negate();
			aa = new P(aa.x, aa.y.negate());
		}
		P r = new P();
		for (int i = m.bitLength();;) {
			if (m.testBit(--i))
				r = add(aa, r);
			if (i == 0)
				break;
			r = add(r, r);
		}
		if (r.infinity())
			return r;
		
		if (r.y.signum() < 0)
			r.y = r.y.add(p);
		return r;
	}

	BigInteger _genPrivateKey(BigInteger r) {
		while (r.compareTo(n) >= 0)
			r = r.subtract(n);
		return r.shiftRight(1);
	}

	
	public String toString() {
		return "W:" + name + "\n" +
				"p: " + p.toString(16) + "\n"
				+ "a: " + a.toString(16) + "\n"
				+ "b: " + b.toString(16) + "\n"
				+ "n: " + n.toString(16) + "\n"
				+ "Gx:" + g.x.toString(16) + "\n"
				+ "Gy:" + g.y.toString(16) + "\n"
				;
	}
}
