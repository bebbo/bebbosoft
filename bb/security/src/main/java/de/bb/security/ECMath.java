package de.bb.security;

import java.math.BigInteger;

import de.bb.security.ec.EC;
import de.bb.security.ec.P;

public class ECMath {

	public static byte[] genPrivateKey(int curve) {
		EC ec = getEC(curve);
		return ec.genPrivateKey().toByteArray();
	}

	private static EC getEC(int curve) {
		EC ec;
		switch (curve) {
		case 0x17:
			ec = EC.SECP256R1;
			break;
		case 0x18:
			ec = EC.SECP384R1;
			break;
		case 0x19:
			ec = EC.SECP521R1;
			break;
		default:
			throw new RuntimeException("invalid curve: " + curve);
		}
		return ec;
	}

	public static byte[][] genPublicKey(int curve, byte[] n) {
		EC ec = getEC(curve);
		return ec.mul(ec.getG(), new BigInteger(n)).toBA();
	}

	public static byte[][] doEC(int curve, byte[] ptx, byte[] pty, byte[] n) {
		EC ec = getEC(curve);
		P p = new P(ptx, pty);
		if (!ec.verify(p))
			throw new RuntimeException("point " + p + " not on curve: " + curve);
		P r = ec.mul(p, new BigInteger(n));
		return r.toBA();
	}

	public static int byteLength(int curve) {
		EC ec = getEC(curve);
		return ec.getByteLength();
	}
}
