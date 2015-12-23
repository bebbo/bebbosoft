package de.bb.util.test;

import org.junit.Test;

import junit.framework.TestCase;
import de.bb.util.LRUCache;

public class TestLRUCache extends TestCase {
    private static LRUCache<Integer, String> CACHE = new LRUCache<Integer, String>();

    //  private static PersistantTimedLRUCache CACHE = new PersistantTimedLRUCache("cache", 15*60*1000);

    @Test
    public void testCache1() {
        for (int i = 0; i < 100; ++i) {
            CACHE.put(new Integer(i), "cached value " + i);
        }
        for (int i = 0; i < 5; ++i) {
            CACHE.get(new Integer(i));
        }
        for (int i = 0; i < 5; ++i) {
            CACHE.get(new Integer(i));
        }
        System.out.println(CACHE.toString());
        System.gc();
        Object[] o = new Object[1000000];
        for (int i = 0; i < o.length; ++i) {
            o[i] = ("x" + i).toCharArray();
        }

        System.gc();
        o = null;

        System.out.println(CACHE.toString());

        for (int i = 5; i < 10; ++i) {
            CACHE.put(new Integer(i), "something else" + i);
        }
        for (int i = 0; i < 100; ++i) {
            String s = CACHE.get(new Integer(i));
            if (s != null) {
                s.toString();
            }
        }
        System.out.println(CACHE.toString());
        CACHE.remove(new Integer(5));
        System.out.println(CACHE.toString());
        CACHE.remove(new Integer(55));
        System.out.println(CACHE.toString());

        LRUCache.shutDown();
        System.out.println(CACHE.toString());
    }

}