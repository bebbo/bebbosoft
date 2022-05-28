package de.bb.security.ec;

import java.math.BigInteger;

public class W extends EC {
	BigInteger b, n;
	P g;

	public W(String id, byte[] p, byte[] a, byte[] b, byte[] n, byte[] gx, byte[] gy) {
		super(id, p, a);
		this.b = new BigInteger(b);
		if (n == null)
			this.n = this.p.add(ONE);
		else
			this.n = new BigInteger(n);
		g = new P(gx, gy);
	}

	public P getG() {
		return g;
	}

	boolean aminus3() {
		return a.add(EC.THREE).equals(p);
	}

	public boolean verify(P aa) {
		if (aa.infinity)
			return true;
		/* Verify y^2 = x^3 + ax + b */
		BigInteger lhs = square(aa.y);
		BigInteger x3 = aa.x.modPow(EC.THREE, p);
		BigInteger ax = aa.x.multiply(a).mod(p);
		BigInteger rhs = x3.add(ax).mod(p).add(b).mod(p);
		return lhs.equals(rhs);
	}

	@Override
	boolean _normalize(P aa) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	P _add(P aa, P bb, boolean aminus3) {
		if (aa.infinity)
			return bb;
		if (bb.infinity)
			return aa;
		BigInteger m = __m(aa, bb);
		BigInteger xr = m.multiply(m).subtract(aa.x).subtract(bb.x).mod(p);
		BigInteger yr = aa.y.add(m.multiply(xr.subtract(aa.x))).mod(p).negate();
		return new P(xr, yr);
	}

	private BigInteger __m(P aa, P bb) {
        if (aa.x.equals(bb.x))
            return THREE.multiply(bb.x.multiply(bb.x)).add(a).multiply(TWO.multiply(aa.y).modInverse(p));
        return aa.y.subtract(bb.y).multiply(aa.x.subtract(bb.x).modInverse(p));
	}

	/*
 def __mul__(self, other):
        if isinstance(other, int) or isinstance(other, LONG_TYPE):
            if other % self.curve.field.n == 0:
                return Inf(self.curve)
            if other < 0:
                addend = Point(self.curve, self.x, -self.y % self.p)
            else:
                addend = self
            result = Inf(self.curve)
            # Iterate over all bits starting by the LSB
            for bit in reversed([int(i) for i in bin(abs(other))[2:]]):
                if bit == 1:
                    result += addend
                addend += addend
            return result
	 */
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
				r = _add(aa, r, false);
			if (i == 0)
				break;
			r = _add(r, r, false);
		}
		
		if (!r.infinity && r.y.signum() < 0)
			r.y = r.y.add(p);
		return r;
	}

	@Override
	P _mul2(P aa, boolean aminus3) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String toString() {
		return "p: " + p.toString(16) + "\n"
				+ "a:" + a.toString(16) + "\n"
				+ "b:" + b.toString(16) + "\n"
				+ "n:" + n.toString(16) + "\n"
				+ "Gx:" + g.x.toString(16) + "\n"
				+ "Gy:" + g.y.toString(16) + "\n"
				;
	}
}
