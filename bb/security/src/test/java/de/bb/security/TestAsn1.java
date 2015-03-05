package de.bb.security;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Misc;

public class TestAsn1 {

    @Test
    public void testString2Oid() {
        byte d[] = Asn1.string2Oid("1.3.14.3.2.26");
        Assert.assertArrayEquals(Misc.hex2Bytes("2b0e03021a"), d);
    }
}
