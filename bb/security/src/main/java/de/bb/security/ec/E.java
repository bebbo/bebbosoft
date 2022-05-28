package de.bb.security.ec;

import java.math.BigInteger;

/**
 * An elliptic curve of y^2 == x^2 + d * x^2 * y^2 + 1  
 */
public class E extends EC {
	BigInteger d;

	E(String id, byte[] p, byte[] l, byte[] d, byte[] gx, byte[] gy) {
		super(id, p, l, gx, gy);
		this.d = new BigInteger(1, d, 0, d.length);
	}

	public boolean verify(P aa) {
		if (aa.infinity())
			return true;
		/* Verify y^2 == x^2 + d * x^2 * y^2 + 1 */
		BigInteger lhs = square(aa.y);
		BigInteger x2 = square(aa.x);
		BigInteger rhs = add(add(x2, mult(mult(d, x2), lhs)), EC.ONE);
		return lhs.equals(rhs);
	}

	P _add(P aa, P bb) {
		/*
		 * outx = (a->x * b->y + b->x * a->y) / (1 + a->curve->e.d * a->x * b->x
		 * * a->y * b->y)
		 */
		BigInteger axby = mult(aa.x, bb.y);
		BigInteger bxay = mult(bb.x, aa.y);
		BigInteger n = add(axby, bxay);

		BigInteger axbxayby = mult(axby, bxay);
		BigInteger z = add(ONE, mult(d, axbxayby)).modInverse(p);
		BigInteger outx = mult(n, z);

		/*
		 * outy = (a->y * b->y + a->x * b->x) / (1 - a->curve->e.d * a->x * b->x
		 * * a->y * b->y)
		 */
		BigInteger ayby = mult(aa.y, bb.y);
		BigInteger axbx = mult(aa.x, bb.x);
		n = add(ayby, axbx);
		z = sub(ONE, mult(d, axbxayby)).modInverse(p);
		BigInteger outy = mult(n, z);

		return new P(outx, outy);
	}
	
	@Override
	public P mul(P aa, BigInteger m) {
		// TODO Auto-generated method stub
		return null;
	}

	public String toString() {
		return "p: " + p.toString(16) + "\n"
				+ "a:" + a.toString(16) + "\n"
				+ "d:" + d.toString(16) + "\n"
				+ "Gx:" + g.x.toString(16) + "\n"
				+ "Gy:" + g.y.toString(16) + "\n"
				;
	}
}
