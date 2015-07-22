package de.bb.security;

import java.io.InputStream;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Misc;

public class TestCRT {

    @Test
    public void testCRT() {
        String sn = "0001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff003021300906052b0e03021a05000414211a6cf87bc025c909cf1db0710456001774c87d";
        String sexp = "4a3e287881bdddf23d93ba52a0a85a89f296280792c9896154b92bcea998138472f9163ce3a87e7c09010cd17f05e9a93dfde1b31e7c5ed3401a23cd4c14c1f9a489b63b840cb3f61bfa4a38517ffa6c59d5f954eef3b3b902a03e572d2cf290fb70390d64cb607af214f6f959f4d7330e4f4860f9163d6d0d14d3a8b39b379911384f7b89316c8b5969dbce240c97ed43e32be71f4a37b4a448d62c9e9d7a71f2405f2445a2fb473db9eeda48223a2a17f3ef24f327646fcabe41eac949599e0d054bda774020989d1357e43d3ad58511328a54c86ff6f00dd668ddfcf69ca8eeb8e33dabd35a0a544836d0242da82c32395c33f2adf725f80d86b55b9f3791";
        String smod = "000000008109d42c6a944991826d4aad3d58d4c2fe41819c5025aae587971fcaeb0506567f63a3bc1804168b304215b3b3b66fddd59dad46128ee4ea5412bccdb63034214fae2b17b07c8a7dcda3f7926baa3d097c7b65093df4a72e8e4bde751c3937b0f5d4c8895822bdfdab1801c7e9fda1b28bab65d5328355657b41c59c194f3eac46438ea589e35fff6380f80cc33f9f101f813b169bfbe47b8246340e4a31644af51788b4685bbde36ca0d386855b52cbddd9be5ac28a2f34dca2ac696ac9c0f3963f448213264880004c66066e20c1929e1032207521d701ca733429ff2d00c39b1fff48bd65f05410e1b19656fb531b00d87ba75359ad600715a0f514145cb1";

        BigInteger bn = new BigInteger(sn, 16);
        BigInteger bexp = new BigInteger(sexp, 16);
        BigInteger bmod = new BigInteger(smod, 16);
        BigInteger bres = bn.modPow(bexp, bmod);
        String sbres = bres.toString(16);

        byte zn[] = Misc.hex2Bytes(sn);
        int[] z = FastMath32.byte2Int(zn, (zn.length + 3)/ 4);
        byte exp[] = Misc.hex2Bytes(sexp);
        byte zmod[] = Misc.hex2Bytes(smod);
        int[] mod = FastMath32.byte2Int(zmod, 1 + (zmod.length + 3)/ 4);
        int[] res = FastMath32.oddModPow(z, exp, mod);
        int l = res.length;
        while (res[l - 1] == 0) {
            --l;
        }
        byte zres [] = new byte[l * 4];
        FastMath32.int2Byte(res, zres);
        String szres = Misc.bytes2Hex(zres);
        Assert.assertEquals(sbres, szres);
    }

    @Test
    public void test1() throws Exception {
        final InputStream fis = getClass().getClassLoader().getResourceAsStream("test.key");
        final int len = fis.available();
        final byte key[] = new byte[len];
        fis.read(key);
        fis.close();
        final byte[][] pkData = Pkcs1.getPrivateKey(key);
        final String sencoded = "0001FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF003021300906052B0E03021A05000414211A6CF87BC025C909CF1DB0710456001774C87D";
        final byte[] encoded = Misc.hex2Bytes(sencoded);
        final byte[] signed = Pkcs6.doRSA(encoded, pkData);
        final byte[] check = Pkcs6.doRSA(signed, pkData[0], pkData[1]);

        Assert.assertArrayEquals(encoded, check);
    }
}
