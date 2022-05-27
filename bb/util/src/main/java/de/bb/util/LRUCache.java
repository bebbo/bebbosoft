/******************************************************************************
 * An efficient LRU cache implementation  
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/

package de.bb.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A 2-level LRU cache implementation. - Both levels are implemented as global LRU lists. All cache instances are
 * sharing the global LRU lists. - The specified percentage defines the count of entries in level 2 to total count of
 * entries. - Each new entry is placed into the level 1. - If a lookup succeeds and the entry is in level 1, it gets
 * promoted to level 2. - If level 2 exceeds the allowed count it gets downgraded to level 1. - All entries in level 1
 * are soft references, and can be cleared by the gc. Since this reduces the total and further usages will establish the
 * specified percentage. - All entries in level 2 are hard references, so the often used entries are rarely discarded. -
 * There is no configuration but the ratio for level 2.
 * 
 * So the contract is: If you put something into the cache, it may be there if you look after it - or not. This is a
 * cache and not a HashMap!
 * 
 * @author Bebbo
 * @version $Revision: 1.19 $
 */
public class LRUCache<K, V> implements Map<K, V> {
    static ReferenceQueue<? extends Object> RQ = new ReferenceQueue<Object>();
    private static Root HARD = new Root();
    private static Root SOFT = new Root();
    private static int RATIO = 100;
    private static boolean SHUTDOWN;

    private HashMap<K, Node<K, V>> hashMap = new HashMap<K, Node<K, V>>();
    // statistic
    private int hit;
    private int get;

    /**
     * Create a new LRUCache with default ratio of 10% fixed elements (level 2). Use a dynamic size.
     */
    public LRUCache() {
    }

    /**
     * Creates a new LRUCache with the specified ratio in percent (level 2). Use a dynamic size.
     * 
     * @param ratio
     *            a percent value 0.01 &lt;= ratio &lt;= 0.99
     */
    @Deprecated
    public LRUCache(double ratio) {
    }

    /**
     * Return the current count of hard and soft references.
     * 
     * @return the current count of hard and soft references.
     */
    public int size() {
        return hashMap.size();
    }

    private final static boolean hardRefsFull() {
        // check whether to convert hard refs to soft refs
        return HARD.count * 1000 > SOFT.count * RATIO;
    }

    /**
     * Put an object into this cache. There is no warranty how long an object is held in this cache!
     * 
     * @param key
     *            a key to identify the object
     * @param o
     *            the stored object
     */
    public synchronized V put(K key, V o) {
        Node<K, V> old = hashMap.get(key);
        Node<K, V> node = new Node<K, V>(key, o, this);

        if (old != null) {
            old.remove();
        }
        if (hardRefsFull()) {
            // check whether to convert hard refs to soft refs
            hard2Soft();
        }

        // enqueue rq thread into gc on first put
        if (HARD.count + SOFT.count == 0)
            new F();

        // store new object
        SOFT.addHead(node);
        hashMap.put(key, node);

        if (old == null)
            return null;
        return old.get();
    }

    private static void hard2Soft() {
        Node<?, ?> node;
        synchronized (HARD) {
            if (HARD.count == 0)
                return;
            node = HARD.prev;
            node.remove();
            node.hardValueRef = null;
        }
        SOFT.addHead(node);
    }

    /**
     * retrieves the object by key, if it is still in the cache.
     * 
     * @param key
     *            the key to get the object for
     * @return the object if it is still in cache, null either.
     */
    public synchronized V get(Object key) {
        ++get;
        // check whether object is in cache
        Node<K, V> n = hashMap.get(key);
        if (n == null)
            return null;

        // check whether SoftReference still knows the object
        V o = n.get();
        if (o == null) {
            // remove this key from hashtable
            hashMap.remove(key);
            n.remove();
            return null;
        }

        ++hit;
        // retain hard references
        if (n.parent == HARD) {
            return o;
        }

        if (hardRefsFull()) {
            return o;
        }

        // modify counter on transition from soft to hard
        // and store the hardValueRef
        n.hardValueRef = o;
        n.remove();
        HARD.addHead(n);
        return o;
    }

    /**
     * Method remove.
     * 
     * @param key
     *            the key for the object to remove
     * @return returns the removed object.
     */
    public synchronized V remove(Object key) {
        // check whether object is in cache
        Node<K, V> n = hashMap.remove(key);
        if (n == null)
            return null;

        if (n.parent == HARD) {
            n.hardValueRef = null;
        }

        // remove node
        n.remove();
        return n.get();
    }

    public synchronized boolean containsKey(Object key) {
        return hashMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return false;
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }

    public boolean isEmpty() {
        return hashMap.size() == 0;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public Collection<V> values() {
        return Collections.emptySet();
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        for (Node<K, V> n : hashMap.values()) {
            n.hardValueRef = null;
            n.remove();
        }
        hashMap = new HashMap<K, Node<K, V>>();
    }

    /**
     * Provides a snapshot of keys used in the cache. The HashSet is snapshot which was valid, but may not be accurate
     * now. There is no warranty that any value is still present in the cache or that nothing else is in the cache.
     * 
     * @return a HashSet containing a snapshot of keys used in the cache.
     */
    public HashSet<K> keySet() {
        HashSet<K> hs = new HashSet<K>();
        hs.addAll(hashMap.keySet());
        return hs;
    }

    /**
     * Return some information about the current LRUCache object.
     */
    public String toString() {
        int div = get == 0 ? 1 : get;
        String r = "" + (100. * hit / div + .005);
        int dot = r.indexOf('.');
        if (dot >= 0 && dot + 2 < r.length())
            r = r.substring(0, dot + 3);
        return "bb_util LRUCache: ratio=" + (RATIO / 1000.) + "; size=" + hashMap.size() + "(hardRefs=" + HARD.count
                + "); gets=" + get + ";hit=" + hit + " => " + r + "%";
    }

    /**
     * Helper class for a simple double linked list
     */
    private static class Node<K, V> extends SoftReference<V> {
        Node<?, ?> next, prev;
        Root parent;
        LRUCache<K, V> cache;
        K key;
        V hardValueRef;

        Node() {
            super(null);
            next = prev = this;
        }

        @SuppressWarnings("unchecked")
        Node(K key, V val, LRUCache<K, V> cache) {
            super(val, (ReferenceQueue<? super V>) RQ);
            this.key = key;
            this.cache = cache;
        }

        /**
         * Remove this node and mark next as zero.
         */
        void remove() {
            if (parent == null) {
                if (prev != null)
                    prev.next = next;
                if (next != null)
                    next.prev = prev;
                next = prev = null;
                return;
            }
            // same code but synchronized
            synchronized (parent) {
                if (prev != null)
                    prev.next = next;
                if (next != null)
                    next.prev = prev;
                next = prev = null;
                --parent.count;
            }
        }

    }

    private static class Root extends Node<Object, Object> {
        int count;

        /**
         * append the node to this root.
         * 
         * @param n
         *            the node.
         * @return the node.
         */
        Node<?, ?> addHead(Node<?, ?> n) {
            // synchronized to avoid interferences with remove
            synchronized (this) {
                n.next = next;
                n.prev = this;
                n.parent = this;
                if (next != null)
                    next.prev = n;
                next = n;
                ++count;
            }
            return n;
        }
    }

    /**/

    /**
     * Poll the Phantom Queue and perform necessary cleanup: remove from Hash table is always called from synchronized
     * methods, so no additional synchronized needed.
     */
    static void clearSoftReferences() {
        Node<?, ?> n;
        // remove invalid soft references from hash table
        while ((n = (Node<?, ?>) RQ.poll()) != null) {
            n.cache.remove(n.key);
        }
        while (hardRefsFull()) {
            // check whether to convert hard refs to soft refs
            hard2Soft();
        }

    }

    /**
     * free some memory
     * 
     * @author sfranke
     */
    static class F {
        public synchronized void finalize() {
            if (SHUTDOWN)
                return;
            try {
                clearSoftReferences();
            } catch (Throwable t) {
            }

            if (HARD.count + SOFT.count > 0) {
                new F();
            }
        }
    }

    public static void shutDown() {
        SHUTDOWN = true;
        HARD = new Root();
        SOFT = new Root();
    }
}
