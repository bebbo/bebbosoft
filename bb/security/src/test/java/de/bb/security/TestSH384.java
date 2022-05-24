package de.bb.security;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Misc;

public class TestSH384 {

    @Test
    public void testabc() {
        MessageDigest sha384 = new SHA384();
        sha384.update("abc".getBytes());
        byte[] d = sha384.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("cb00753f45a35e8b b5a03d699ac65007 272c32ab0eded163 1a8b605a43ff5bed 8086072ba1e7cc23 58baeca134c825a7"), d);
    }

    @Test
    public void test448bits() {
        MessageDigest sha384 = new SHA384();
        sha384.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes());
        byte[] d = sha384.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("3391fdddfc8dc739 3707a65b1b470939 7cf8b1d162af05ab fe8f450de5f36bc6 b0455a8520bc4e6f 5fe95b1fe3c8452b"), d);
    }

    @Test
    public void test896bits() {
        MessageDigest sha384 = new SHA384();
        sha384.update("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu".getBytes());
        byte[] d = sha384.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("09330c33f71147e8 3d192fc782cd1b47 53111b173b3b05d2 2fa08086e3b0f712 fcc7c71a557e2db9 66c3e9fa91746039"), d);
    }
    
    @Test
    public void test1MioA() {
        MessageDigest sha384 = new SHA384();
        for (int i = 0; i < 1000000; ++i)
        	sha384.update((byte)'a');
        byte[] d = sha384.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("9d0e1809716474cb 086e834e310a4a1c ed149e9c00f24852 7972cec5704c2a5b 07b8b3dc38ecc4eb ae97ddd87f3d8985"), d);
    }
    
}
