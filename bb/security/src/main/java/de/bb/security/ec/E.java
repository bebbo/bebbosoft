package de.bb.security.ec;

import java.math.BigInteger;

class E extends EC {
	BigInteger l, d;
	P b;

	E(String id, byte[] p, byte[] l, byte[] d, byte[] bx, byte[] by) {
		super(id, p, l);
		this.l = a;
		this.d = new BigInteger(d);
		b = new P(bx, by);
	}

	boolean verify(P aa) {
		if (aa.infinity)
			return true;
		/* Verify y^2 == x^2 + d * x^2 * y^2 + 1 */
		BigInteger lhs = square(aa.y);
		BigInteger x2 = square(aa.x);
		BigInteger rhs = x2.add(d.multiply(x2).mod(p).multiply(lhs)).add(EC.ONE).mod(p);
		return lhs.equals(rhs);
	}

	boolean _normalize(P aa) {
		return true;
	}

	P _add(P aa, P bb, boolean aminus3) {
		/*
		 * outx = (a->x * b->y + b->x * a->y) / (1 + a->curve->e.d * a->x * b->x
		 * * a->y * b->y)
		 */
		BigInteger axby = aa.x.multiply(bb.y).mod(p);
		BigInteger bxay = bb.x.multiply(aa.y).mod(p);
		BigInteger n = add(axby, bxay);

		BigInteger axbxayby = axby.multiply(bxay).mod(p);
		BigInteger z = add(ONE, d.multiply(axbxayby).mod(p)).modInverse(p);
		BigInteger outx = n.multiply(z).mod(p);

		/*
		 * outy = (a->y * b->y + a->x * b->x) / (1 - a->curve->e.d * a->x * b->x
		 * * a->y * b->y)
		 */
		BigInteger ayby = aa.y.multiply(bb.y).mod(p);
		BigInteger axbx = aa.x.multiply(bb.x).mod(p);
		n = add(ayby, axbx);
		z = sub(ONE, d.multiply(axbxayby).mod(p)).modInverse(p);
		BigInteger outy = n.multiply(z).mod(p);

		return new P(outx, outy);
	}

	P _mul2(P aa, boolean aminus3) {
		return add(aa, aa, aminus3);
	}

}
