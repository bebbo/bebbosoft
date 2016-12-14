package de.bb.security.ec;

import java.math.BigInteger;

class M extends EC {
	P g;
	BigInteger b;

	M(String id, byte[] p, byte[] a, byte[] b, byte[] gx) {
		super(id, p, a);
		this.b = new BigInteger(b);
		g = new P(gx);
	}
	
	boolean _normalize(P aa) {
		if (aa.x == null)
			return false;
		
		if (aa.z == null)
			return true;
		
		aa.x = aa.z.modInverse(p).multiply(aa.x).mod(p);
		aa.z = null;
		
		return true;
	}

	P _add(P aa, P bb, boolean aminus3) {
		BigInteger az = aa.z == null ? ONE : aa.z;
		BigInteger bz = bb.z == null ? ONE : bb.z;
				
	    /* a-b is maintained at 1 due to Montgomery ladder implementation */
	    /* Xa+b = Za-b * ((Xa - Za)*(Xb + Zb) + (Xa + Za)*(Xb - Zb))^2 */
	    /* Za+b = Xa-b * ((Xa - Za)*(Xb + Zb) - (Xa + Za)*(Xb - Zb))^2 */
		
		BigInteger axpaz = add(aa.x, az);
		BigInteger bxpbz = add(bb.x, bz);
		BigInteger axmaz = sub(aa.x, az);
		BigInteger bxmbz = sub(bb.x, bz);
		
		BigInteger p1 = axmaz.multiply(bxpbz);
		BigInteger p2 = axpaz.multiply(bxmbz);
		
		BigInteger outx = square(add(p1, p2));
		BigInteger outz = square(sub(p1, p2));

		
		return new P(outx, null, outz);
	}
	
	P _mul2(P aa, boolean aminus3) {
		BigInteger z = aa.z == null ? ONE : aa.z;
		
		/* 4xz = (x + z)^2 - (x - z)^2 */
		BigInteger xpz2 = square(add(aa.x, aa.z));
		BigInteger xmz2 = square(sub(aa.x, aa.z));
		BigInteger fourxz = sub(xpz2, xmz2);
		
		/* outx = (x + z)^2 * (x - z)^2 */
		BigInteger outx = xpz2.multiply(xmz2).mod(p);
		
		 /* outz = 4xz * ((x - z)^2 + ((A + 2) / 4)*4xz) */
		BigInteger t1 = fourxz.multiply(xmz2).mod(p);
		BigInteger t2 = add(a, EC.TWO).multiply(fourinverse).mod(p).multiply(fourxz).mod(p);
		BigInteger outz = add(t1, t2);
		
		return new P(outx, null, outz);
	}

}

