package de.bb.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Mime;
import de.bb.util.Misc;

/**
 * Tests for PKCS5.
 * 
 * @author Stefan Bebbo Franke
 *
 */
public class TestPkcs5 {

    @Test
    public void testPbkdf2() {
        String[][] tests = {
                { "password", "salt", "1", "32",
                        "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b" },
                { "password", "salt", "2", "32",
                        "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43" },
                { "password", "salt", "4096", "32",
                        "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a" },

                {
                        "passwordPASSWORDpassword",
                        "saltSALTsaltSALTsaltSALTsaltSALTsalt",
                        "4096",
                        "40",
                        "348c89dbcbd32b2f32d814b8116e84cf2b17347ebc1800181c4e2a1fb8dd53e1c635518c7dac47e9" }, };

        final MessageDigest md = new SHA256();

        for (final String[] test : tests) {
            final String pwd = test[0];
            final String salt = test[1];
            final int passes = Integer.parseInt(test[2]);
            final int length = Integer.parseInt(test[3]);

            final byte[] pbkdf2 = Pkcs5.pbkdf2(pwd.getBytes(), salt.getBytes(),
                    passes, md, length);

            final String result = Misc.bytes2Hex(pbkdf2);
            Assert.assertEquals(test[4], result);

        }
    }

    @Test
    public void testPbkdf2x() throws IOException {

        final String pwd = "test1234";
        final String salt = "\001\002\003\004\005\006\007\010\011\012\013\014\015\016\017\020";
        int e = 10;
        int d = 7;
        int count = (1 << e) + d;

        final MessageDigest md = new SHA256();
        final byte[] pbkdf2 = Pkcs5.pbkdf2(pwd.getBytes(), salt.getBytes(),
                count, md, 32);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(e);
        bos.write(d);
        bos.write(salt.getBytes());
        bos.write(pbkdf2);

        final byte[] encoded = Mime.encode(bos.toByteArray(), 120);

        final String b64 = "CgcBAgMEBQYHCAkKCwwNDg8QWlfO+luPj3zipKGms+rRDBHTNsMvQ0oI5E7XZAPPAac=";
        final String sencoded = new String(encoded);
        Assert.assertEquals(b64, sencoded);
        
        final String ep = "{PKCS5SHA256}" + new String(encoded);
        
        final boolean r = Pkcs5.verifyPbkdf2(ep, pwd);
        Assert.assertTrue(r);
    }
    
    @Test
    public void testPbkdf2y() {
        final String password = "test1234";
        final String encodedPassword = Pkcs5.encodePbkdf2("SHA256", password, 14);
        final boolean match = Pkcs5.verifyPbkdf2(encodedPassword, password);
        Assert.assertTrue(match);
    }
}
