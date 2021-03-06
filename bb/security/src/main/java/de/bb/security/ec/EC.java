package de.bb.security.ec;

import java.math.BigInteger;

import de.bb.security.SecureRandom;
import de.bb.util.Misc;

/**
 * Base class of elliptic Curves.
 * 
 * @author stefan bebbo franke
 *
 */
public abstract class EC {
	static final BigInteger ZERO = BigInteger.ZERO;
	static final BigInteger ONE = BigInteger.ONE;
	static final BigInteger xTWO = new BigInteger("2");
	static final BigInteger THREEx = new BigInteger("3");

	public static P INFINITY = new P();

	public String name;
	BigInteger p;
	BigInteger a;
	P g;

	EC(String n, byte[] p, byte[] a, byte[] gx, byte[] gy) {
		this.name = n;
		this.p = new BigInteger(1, p, 0, p.length);
		this.a = new BigInteger(1, a, 0, a.length);
		g = new P(gx, gy);
	}

	public int getByteLength() {
		return (p.bitLength() + 7) / 8;
	}

	
	public P getG() {
		return g;
	}

	public abstract P mul(P aa, BigInteger m);
	
	/**
	 * assume that a and b are normalized.
	 * 
	 * @param a value a.
	 * @param b value b.
	 * @return a + b mod p.
	 */
	BigInteger add(BigInteger a, BigInteger b) {
		BigInteger r = a.add(b);
		if (r.compareTo(p) < 0)
			return r;
		return r.subtract(p);
	}

	/**
	 * assume that a and b are normalized.
	 * 
	 * @param a value a.
	 * @param b value b.
	 * @return a - b mod p.
	 */
	BigInteger sub(BigInteger a, BigInteger b) {
		BigInteger ret = a.subtract(b);
		if (ret.signum() >= 0)
			return ret;
		return ret.add(p);
	}

	BigInteger square(BigInteger a) {
		return a.multiply(a).mod(p);
	}

	public abstract boolean verify(P aa);

	abstract P _add(P aa, P bb);

	public final P add(P aa, P bb) {
		if (aa.infinity())
			return bb;
		if (bb.infinity())
			return aa;
		return _add(aa, bb);
	}

	public BigInteger genPrivateKey() {
		byte b[] = new byte[(p.bitLength() + 7) / 8]; 
		SecureRandom.getInstance().nextBytes(b, 1, b.length - 1);
		BigInteger r = new BigInteger(1, b, 0, b.length);
		return _genPrivateKey(r);
	}

	BigInteger _genPrivateKey(BigInteger r) {
		return r.mod(p);
	}
	
	protected BigInteger mult(BigInteger a, BigInteger b) {
		return a.multiply(b).mod(p);
	}

	/*
	 * static EC C25519 = new M("Curve25519", Misc.hex2Bytes(
	 * "7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed"),
	 * Misc.hex2Bytes(
	 * "0000000000000000000000000000000000000000000000000000000000076d06"),
	 * Misc.hex2Bytes(
	 * "0000000000000000000000000000000000000000000000000000000000000001"),
	 * Misc.hex2Bytes(
	 * "0000000000000000000000000000000000000000000000000000000000000009"));
	 */
	public final static EC ED25519 = new E("Ed25519",
			Misc.hex2Bytes("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed"),
			Misc.hex2Bytes("1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed"),
			Misc.hex2Bytes("52036cee2b6ffe738cc740797779e89800700a4d4141d8ab75eb4dca135978a3"),
			Misc.hex2Bytes("216936d3cd6e53fec0a4e231fdd6dc5c692cc7609525a7b2c9562d608f25d51a"),
			Misc.hex2Bytes("6666666666666666666666666666666666666666666666666666666666666658"));

	public final static W SECP192R1 = new W("secp192r1",
			// p
			new BigInteger("6277101735386680763835789423207666416083908700390324961279").toByteArray(),
			// a
			new BigInteger("6277101735386680763835789423207666416083908700390324961276").toByteArray(),
			// b
			new BigInteger("2455155546008943817740293915197451784769108058161191238065").toByteArray(),
			// n
			null,
			// Gx
			new BigInteger("602046282375688656758213480587526111916698976636884684818").toByteArray(),
			// Gy
			new BigInteger("174050332293622031404857552280219410364023488927386650641").toByteArray());

	public final static W SECP256R1 = new W("secp256r1",
			// p
			Misc.hex2Bytes("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff"),
			// a
			Misc.hex2Bytes("ffffffff00000001000000000000000000000000fffffffffffffffffffffffc"),
			// b
			Misc.hex2Bytes("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b"),
			// n
			Misc.hex2Bytes("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551"),
			// G
			Misc.hex2Bytes("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296"),
			Misc.hex2Bytes("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5"));

	public final static W SECP384R1 = new W("secp384r1",
			// p
			Misc.hex2Bytes(
					"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff"),
			// a
			Misc.hex2Bytes(
					"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000fffffffc"),
			// b
			Misc.hex2Bytes(
					"b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef"),
			// n
			Misc.hex2Bytes(
					"ffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973"),
			// G
			Misc.hex2Bytes(
					"aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760ab7"),
			Misc.hex2Bytes(
					"3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5f"));

	public final static W SECP521R1 = new W("secp521r1",
			// p
			Misc.hex2Bytes(
					"01ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"),
			// a
			Misc.hex2Bytes(
					"01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc"),
			// b
			Misc.hex2Bytes(
					"0051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00"),
			// n
			Misc.hex2Bytes(
					"01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409"),
			// G
			Misc.hex2Bytes(
					"00c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66"),
			Misc.hex2Bytes(
					"011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650"));

}
