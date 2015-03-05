package de.bb.security;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Misc;

public class TestSH256 {

    private static final byte[] EMPTYRESULT = Misc
            .hex2Bytes("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    private static final byte[] BROWNFOX = Misc
            .hex2Bytes("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592");
    private static final byte[] BROWNFOXDOT = Misc
            .hex2Bytes("ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c");

    @Test
    public void emptyTest() {
        MessageDigest sha256 = new SHA256();

        byte[] d = sha256.digest();
        Assert.assertArrayEquals(EMPTYRESULT, d);
    }

    @Test
    public void brownFoxTest() {
        MessageDigest sha256 = new SHA256();
        sha256.update("The quick brown fox jumps over the lazy dog".getBytes());
        byte[] d = sha256.digest();
        Assert.assertArrayEquals(BROWNFOX, d);
    }

    @Test
    public void brownFoxDotTest() {
        MessageDigest sha256 = new SHA256();
        sha256.update("The quick brown fox jumps over the lazy dog.".getBytes());
        byte[] d = sha256.digest();
        Assert.assertArrayEquals(BROWNFOXDOT, d);
    }

    @Test
    public void test1304() {
        MessageDigest sha256 = new SHA256();
        sha256.update(Misc
                .hex2Bytes("451101250ec6f26652249d59dc974b7361d571a8101cdfd36aba3b5854d3ae086b5fdd4597721b66e3c0dc5d8c606d9657d0e323283a5217d1f53f2f284f57b85c8a61ac8924711f895c5ed90ef17745ed2d728abd22a5f7a13479a462d71b56c19a74a40b655c58edfe0a188ad2cf46cbf30524f65d423c837dd1ff2bf462ac4198007345bb44dbb7b1c861298cdf61982a833afc728fae1eda2f87aa2c9480858bec"));
        byte[] d = sha256.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("3c593aa539fdcdae516cdf2f15000f6634185c88f505b39775fb9ab137a10aa2"), d);
    }

    @Test
    public void test320() {
        MessageDigest sha256 = new SHA256();
        sha256.update(Misc
                .hex2Bytes("64cd363ecce05fdfda2486d011a3db95b5206a19d3054046819dd0d36783955d7e5bf8ba18bf738a"));
        byte[] d = sha256.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("32caef024f84e97c30b4a7b9d04b678b3d8a6eb2259dff5b7f7c011f090845f8"), d);
    }

    @Test
    public void test144() {
        MessageDigest sha256 = new SHA256();
        sha256.update(Misc.hex2Bytes("59eb45bbbeb054b0b97334d53580ce03f699"));
        byte[] d = sha256.digest();
        Assert.assertArrayEquals(Misc.hex2Bytes("32c38c54189f2357e96bd77eb00c2b9c341ebebacc2945f97804f59a93238288"), d);
    }

}
