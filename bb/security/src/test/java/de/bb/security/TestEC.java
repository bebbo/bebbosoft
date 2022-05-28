package de.bb.security;

import org.junit.Test;

import de.bb.security.ec.EC;
import de.bb.security.ec.P;
import de.bb.security.ec.W;
import junit.framework.Assert;

public class TestEC {

	private static final byte[] GX = { 15 };
	private static final byte[] GY = { 13 };
	private static final byte[] N = { 18 };

	private static final byte[] B = { 7 };
	private static final byte[] A = { 0 };
	private static final byte[] P = { 17 };

	public void test1() {
		// "p1707" => y^2 = x^3 + 0x + 7 (mod 17
		W w = new W("p1707", P, A, B, N, GX, GY);
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
}
