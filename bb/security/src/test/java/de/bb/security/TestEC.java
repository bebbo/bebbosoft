package de.bb.security;

import java.math.BigInteger;

import org.junit.Test;

import de.bb.security.ec.EC;
import de.bb.security.ec.P;
import de.bb.security.ec.W;
import de.bb.util.Misc;
import junit.framework.Assert;

public class TestEC {

	private static final byte[] GX = { 15 };
	private static final byte[] GY = { 13 };
	private static final byte[] N = { 18 };

	private static final byte[] B = { 7 };
	private static final byte[] A = { 0 };
	private static final byte[] PRIM = { 17 };

	public void test1() {
		// "p1707"=> y^2 = x^3 + 0x + 7 (mod 17
		W w = new W("p1707", PRIM, A, B, N, GX, GY);
		P g = w.getG();
		Assert.assertTrue(w.verify(g));

		for (int i = 0; i < 24; ++i) {
			P n = w.mul(g, i);
			System.out.println(i + ": " + n);
		}
	}

	public void test2() {
		W w = EC.SECP192R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));

		for (int i = 0; i < 8; ++i) {
			P n = w.mul(g, i);
			System.out.println(i + ": " + n);
		}
	}

	@Test
	public void testSECP192R1() {
		W w = EC.SECP192R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));
	}

	@Test
	public void testSECP256R1() {
		W w = EC.SECP256R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));

		BigInteger privAlice = w.genPrivateKey();
		P pubAlice = w.mul(w.getG(), privAlice);

		BigInteger privBob = w.genPrivateKey();
		P pubBob = w.mul(w.getG(), privBob);

		P sharedBob = w.mul(pubAlice, privBob);
		P sharedAlice = w.mul(pubBob, privAlice);
		Assert.assertEquals(sharedBob, sharedAlice);
	}

	@Test
	public void testSECP384R1() {
		W w = EC.SECP384R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));
	}

	@Test
	public void testSECP521R1() {
		W w = EC.SECP521R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));
	}

	@Test
	public void testED25519() {
		EC w = EC.ED25519;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));
	}

	//@Test
	public void testX() {
		byte[] clientSecret = Misc.hex2Bytes("fd e4 81 fc e8 70 28 ee ee a8\n"
				+ "b4 36 95 93 07 4b d8 f4 37 b6 d1 a5 81 9e 8b 8b\n" + "23 af 96 e4 90 bf");
		byte[] serverSecret = Misc.hex2Bytes("c5 63 39 61 70 12 d3 de 44 1e\n"
				+ "6b 97 92 0c 62 a2 76 af 3f cf ab 4a 3b 0d 44 4f\n" + "57 4e 47 52 44 01");

		byte[] preMasterSecret = Misc.hex2Bytes(
//				"04 " +
				 "00 0d 48 0d 43 24 87\n" + "			    a7 e2 8f b7 c5 40 84 67 10 56 b6 8c 14 ab 5b 6f\n"
						+ "			    5b a5 b0 be fd 46 8c 2c 52 0d c9 b3 65 d9 2d b4\n"
						+ "			    bb 65 a3 e1 30 22 df df 7b 97 84 59 5e 5b 6d 0c\n"
						+ "			    df eb f3 f0 dc 5c 49 af 6e c4 c4 01 de f4 6e 73\n"
						+ "			    24 75 af ea 0d 0f 59 87 a6 18 3b 8f 22 1a 92 0f\n"
						+ "			    32 d1 04 7f 0d b1 98 3a 38 3a d3 c3 f9 16 b1 6b\n"
						+ "			    42 dc 2f 47 92 71 26 c8 9e ea 28 a2 15 62 7a b5\n"
						+ "			    a3 44 73 5b 43 21 9f 11 68 ab 42 80 ca");
		
		byte [] serverPubx = new byte[preMasterSecret.length / 2];
		byte [] serverPuby = new byte[preMasterSecret.length / 2];
		System.arraycopy(preMasterSecret, 0, serverPubx, 0, serverPubx.length);
		System.arraycopy(preMasterSecret, serverPubx.length, serverPuby, 0, serverPubx.length);

		W w = EC.SECP521R1;
		P g = w.getG();
		Assert.assertTrue(w.verify(g));

		P pub = new P(serverPubx,  serverPuby);
		Assert.assertTrue(w.verify(pub));
		
		byte [] clientPriv = w.genPrivateKey().toByteArray();
		P clientPub = w.mul(w.getG(), new BigInteger(clientPriv));
	
		P shared = w.mul(pub, new BigInteger(clientPriv));
		System.out.println(shared);
	}
}
