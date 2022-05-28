package de.bb.security.ec;

import java.math.BigInteger;

public class P {
	BigInteger x, y;

	public final static P INF = new P();

	P() {}

	P(byte[] gx) {
		x = new BigInteger(gx);
	}

	public P(byte[] gx, byte[] gy) {
		this.x = new BigInteger(1, gx, 0, gx.length);
		this.y = new BigInteger(1, gy, 0, gy.length);
	}

	public P(BigInteger x, BigInteger y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof P) {
			P p = (P) obj;
			if (x == null)
				return p.x == null;
			
			return x.equals(p.x) && y.equals(p.y);
		}
		return false;
	}

	@Override
	public String toString() {
		if (x == null)
			return "[INF]";
		if (y != null)
			return "[" + x.toString(16) + ", " + y.toString(16) + "]";
		return "[" + x.toString(16) + "]";
	}

	public byte[][] toBA() {
		if (x == null)
			return null;
		return new byte[][] { x.toByteArray(), y.toByteArray() };
	}

	public boolean infinity() {
		return x == null;
	}
}
