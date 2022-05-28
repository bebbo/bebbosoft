package de.bb.security.ec;

import java.math.BigInteger;

public class M extends EC {
	P g;
	BigInteger b;

	public M(String id, byte[] p, byte[] a, byte[] b, byte[] gx) {
		super(id, p, a);
		this.b = new BigInteger(b);
		g = new P(gx);
	}

	@Override
	boolean _normalize(P aa) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	P _add(P aa, P bb, boolean aminus3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	P _mul2(P aa, boolean aminus3) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

