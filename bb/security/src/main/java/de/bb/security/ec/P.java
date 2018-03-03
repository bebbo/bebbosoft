package de.bb.security.ec;

import java.math.BigInteger;

public class P {
	BigInteger x, y, z;
	boolean infinity;

	P() {
		infinity = true;
	}
	
	P(byte[] gx) {
		x = new BigInteger(gx);
	}
	
	P(byte[] gx, byte[] gy) {
		this.x = new BigInteger(gx);
		this.y = new BigInteger(gy);
	}

	public P(BigInteger x, BigInteger y, BigInteger z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}