package de.bb.util.test;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import de.bb.util.SingleMap;

public class BenchMap {

    static Random r = new Random();

    /**
     * @param args
     */
    public static void main(String[] args) {
        for (int i = 0; i < 10; ++i) {
            bench(new TreeMap<Integer, Integer>());
            bench(new SingleMap<Integer, Integer>());
        }
    }

    private static void bench(Map<Integer, Integer> m) {
        r.setSeed(0);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; ++i) {
            Integer n = r.nextInt();
            m.put(n, n);
        }
        long stop = System.currentTimeMillis();
        System.out.println(m.getClass().getName() + " took " + (stop - start) + "ms");

//        System.out.println(Runtime.getRuntime().totalMemory()
//                - Runtime.getRuntime().freeMemory());
//        m.clear();
//        System.gc();
//        System.out.println(Runtime.getRuntime().totalMemory()
//                - Runtime.getRuntime().freeMemory());
    }
}
