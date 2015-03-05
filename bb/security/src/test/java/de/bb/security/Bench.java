package de.bb.security;

import java.security.NoSuchAlgorithmException;

import de.bb.util.Misc;


public class Bench {

    static byte[] data = new byte[100 * 1000 * 1000];
    
    public static void main(String[] args) throws NoSuchAlgorithmException {
        benchmark(java.security.MessageDigest.getInstance("SHA"));
        benchmark(java.security.MessageDigest.getInstance("SHA-256"));
        
        benchmark(new SHA());
        benchmark(new SHA256());
    }

    private static void benchmark(java.security.MessageDigest md) {
        long start = System.currentTimeMillis();
        byte[] b = md.digest(data);
        long stop = System.currentTimeMillis();
        System.out.println(md.getClass() + " took " + (stop - start) + "ms " + Misc.bytes2Hex(b));
    }

    private static void benchmark(MessageDigest md) {
        long start = System.currentTimeMillis();
        byte[] b = md.digest(data);
        long stop = System.currentTimeMillis();
        System.out.println(md.getClass() + " took " + (stop - start) + "ms " + Misc.bytes2Hex(b));
    }

}
