package de.bb.security;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import de.bb.util.Misc;

public class FastMath32Test {

	@Test
	public void testMod() {
		byte[] r64 = new byte[64];
		byte[] r32 = new byte[32];
		int d[] = new int[] { 1742790185, 5788501, 0, 0, 0, 0, 0, 0, -157675059, -1, -1, -1, -1, -1, -1, 1073741823 };
		int p[] = new int[] { -19, -1, -1, -1, -1, -1, -1, 2147483647 };
		int[] t1 = new int[16];
		int[] t2 = new int[16];

		byte db[] = FastMath32.int2Byte(d, r64).clone();
		byte dp[] = FastMath32.int2Byte(p, r32).clone();
		BigInteger bb = new BigInteger(1, db);
		BigInteger bp = new BigInteger(1, dp);
		System.out.println(bb.divide(bp));
		Misc.dump(System.out, bp.toByteArray());
		BigInteger br = bb.mod(bp);
		byte[] expected = br.toByteArray();

		FastMath32.mod(d, p, t1, t2, 16, 8);

		byte[] result = FastMath32.int2Byte(d, new byte[expected.length]);

		Misc.dump(System.out, result);
		Misc.dump(System.out, expected);
		assertTrue(Misc.equals(result, expected));
	}

}
