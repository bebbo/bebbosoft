/******************************************************************************
 * Hold entries with a timeout and support refresh, validation and discarding of entries.  
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * A SessionManager is used to manage sessions. Sessions are objects which live a distinct time. Each time a session is
 * used or touched, the lifetime of that session is renewed. If the life time of a session is over, it is removed from
 * the SessionManager. A session object may implement the <code>Session.Callback</code> interface, and if it does, its
 * callback function is called before removal.
 */
public class SessionManager<K, V> {
    private final static boolean DEBUG = false;

    private int maxCount;
    // max count of sessions herein, or -1 for unlimited count
    private long timeout; // default timeout for all
    private HashMap<K, V> ht; // key -> value map
    private HashMap<K, Long> rev; // reverse: key -> timeout

    private static Random rand = new Random();
    // add some random to all sessionIds

    /** the thread who watches all sessions */
    private static Killer killer;

    /** timeout -> key map */
    static MultiMap<Long, Object[]> map = new MultiMap<Long, Object[]>();

    static {
        // try to use a secure random
        try {
            Class<Random> clazz = (Class<Random>) Class.forName("de.bb.security.SecureRandom");
            rand = clazz.newInstance();
        } catch (Exception ex) {
        }
    }

    /**
     * Set a new random generator for all SessionManagers.
     * 
     * @param rnd
     *            the new random generator
     */
    public static void setRandom(Random rnd) {
        if (rnd != null) {
            rand = rnd;
        }
    }

    /**
     * This interface is used by the SessionManager, which maintains objects which live a distinct time. To support the
     * <i>soft</i> removeButAsk() function, an object might implement this interface. The callback function dontRemove
     * will ve called before <i>soft</i> removal.
     */
    public static interface Callback {
        /**
         * Is called be the removeButAsk() function. If the implementation returns true, the object is not removed. The
         * implementation may touch the object, to renew the sessions life time. If the session remains untouched,
         * touch() is called by the SessionManager with its default life time.
         * 
         * @param key
         *            the key which binds the session to the SessionManager.
         * @return true to renew the session life time, false, to allow deletion of the session.
         */
        public boolean dontRemove(Object key);
    }

    /**
     * Waits until the time is come to kill the next session
     */
    private static class Killer extends Thread {
        /**
         * create a Killer object which knows it's SessionManager
         */
        Killer() {
            super("bb_util: SessionManager thread");
            setDaemon(true);
            setContextClassLoader(null);
        }

        /**
         * run function, to handle sleep / awake stuff properly.
         */
        @Override
        public void run() {
            try {
                for (;;) {
                    try {
                        long diff;
                        do {
                            Object[] o = null;
                            synchronized (SessionManager.map) {
                                if (map.isEmpty()) {
                                    return;
                                }

                                Long l = map.firstKey();
                                if (l == null) {
                                    diff = 500000;
                                    break;
                                }
                                diff = l.longValue() - System.currentTimeMillis();
                                if (diff > 0) {
                                    break;
                                }
                                o = map.get(l);
                            }
                            if (DEBUG) {
                                System.out.println("diff = " + diff);
                            }
                            SessionManager<Object, ?> sm = (SessionManager<Object, ?>) o[0];
                            sm.removeButAsk(o[1]);
                        } while (diff <= 0);

                        if (diff > 0) {
                            synchronized (this) {
                                wait(diff);
                            }
                        }
                    } catch (InterruptedException ie) {
                        if (DEBUG) {
                            System.out.println("Killer - wake up");
                        }
                    }
                }
            } finally {
                SessionManager.killer = null;
            }
        }
    }

    /**
     * This class is used to maintain sessions, that are objects which live a distinct time. Each time a session is used
     * or touched, the lifetime of that session is renewed. Constructs a SessionManager object.
     * 
     * @param timeoutMilli
     *            an int specifying the timeout in ms
     * @param maxCountOfSessions
     *            limit the count of used sessions.
     */
    public SessionManager(long timeoutMilli, int maxCountOfSessions) {
        maxCount = maxCountOfSessions;
        timeout = timeoutMilli;
        ht = new HashMap<K, V>();
        rev = new HashMap<K, Long>();
    }

    /**
     * This class is used to maintain sessions, that are objects which live a distinct time. Each time a session is used
     * or touched, the lifetime of that session is renewed. Constructs a SessionManager object.
     * 
     * @param timeoutMilli
     *            an int specifying the timeout in ms
     */
    public SessionManager(long timeoutMilli) {
        this(timeoutMilli, -1);
    }

    /**
     * This class is used to maintain sessions, that are objects which live a distinct time. Each time a session is used
     * or touched, the lifetime of that session is renewed. Constructs a SessionManager object.
     * 
     * @param timeoutMilli
     *            an int specifying the timeout in ms
     * @deprecated use the constructor with long instead!
     */
    @Deprecated
    public SessionManager(int timeoutMilli) {
        this(timeoutMilli, -1);
    }

    /**
     * Clears this SessionManager so that it contains no sessions.
     */
    public void clear() {
        synchronized (map) {
            ht.clear();
            rev.clear();
        }
    }

    /**
     * Tests if some key maps into the specified value in this SessionManager. This operation is more expensive than the
     * containsKey method.
     * 
     * @param value
     *            a value to search for
     * @return true if some key maps to the value argument in this SessionManager; false otherwise.
     */
    public boolean contains(V value) {
        return ht.containsValue(value);
    }

    /**
     * Tests whether the specified object is a key in this SessionManager.
     * 
     * @param key
     *            possible key.
     * @return true if the specified object is a key in this SessionManager; false otherwise.
     */
    public boolean containsKey(K key) {
        return ht.containsKey(key);
    }

    /**
     * Returns an enumeration of the values in this SessionManager. Use the Enumeration methods on the returned object
     * to fetch the elements sequentially.
     * 
     * @return an enumeration of the values in this SessionManager.
     */
    public Iterator<V> elements() {
        return ht.values().iterator();
    }

    /**
     * Returns the value to which the specified key is mapped in this SessionManager.
     * 
     * @param key
     *            a key in the SessionManager.
     * @return the value to which the key is mapped in this SessionManager; null if the key is not mapped to any value
     *         in this SessionManager.
     */
    public V get(K key) {
        return key == null ? null : ht.get(key);
        /*
         * if (key == null) return null; // synchronized (ht) { Object o =
         * ht.get(key); if (DEBUG) System.out.println( "get (" + ht.size() +
         * ") " + key.toString() + " = " + (o == null ? "null" : o.toString()));
         * return o; // }
         */
    }

    /**
     * Get current max count of sessions.
     * 
     * @return current max count of sessions
     * @see #setMaxCount
     */
    public int getMaxCount() {
        return maxCount;
    }

    /**
     * Get current default timout value.
     * 
     * @return current default timout value.
     * @see #setTimeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Tests if this SessionManager maps no keys to values.
     * 
     * @return true if this SessionManager maps no keys to values; false otherwise.
     */
    public boolean isEmpty() {
        return ht.isEmpty();
    }

    /**
     * Returns an enumeration of the keys in this SessionManager.
     * 
     * @return an enumeration of the keys in this SessionManager.
     */
    public Iterator<K> keys() {
        return ht.keySet().iterator();
    }

    /**
     * Maps the specified key to the specified value in this SessionManager. If the key is null a new key is generated
     * and returned. The value can NOT be null. The value can be retrieved by calling the get method with a key that is
     * equal to the original key.
     * 
     * @param key
     *            the key or null, then a new key is created.
     * @param value
     *            the value.
     * @return the used key to store the value.
     */
    public V put(K key, V value) {
        return put(key, value, timeout);
    }

    /**
     * Maps the specified key to the specified value in this SessionManager, using the SessionManager's timeout. If the
     * key is null a new key is generated and returned. The value can NOT be null. The value can be retrieved by calling
     * the get method with a key that is equal to the original key.
     * 
     * @param key
     *            the key or null, then a new key is created.
     * @param value
     *            the value.
     * @param timeoutMilli
     *            a distinct timeout value for the given key
     * @return the used key to store the value.
     * @deprecated
     */
    @Deprecated
    public V put(K key, V value, int timeoutMilli) {
        return put(key, value, (long) timeoutMilli);
    }

    /**
     * Maps the specified key to the specified value in this SessionManager, using the SessionManager's timeout. If the
     * key is null a new key is generated and returned. The value can NOT be null. The value can be retrieved by calling
     * the get method with a key that is equal to the original key.
     * 
     * @param key
     *            the key or null, then a new key is created.
     * @param value
     *            the value.
     * @param timeoutMilli
     *            a distinct timeout value for the given key
     * @return the used key to store the value.
     */
    public V put(K key, V value, long timeoutMilli) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        synchronized (map) {
            // limit count of elements
            if (maxCount >= 0 && maxCount <= ht.size()) {
                // remove the oldest element
                Object o3[] = map.get(map.lastKey());
                remove(o3[1]);
            }

            V oVal = ht.put(key, value);
            if (oVal != null) {
                if (DEBUG) {
                    System.out.println("double put!");
                }
                Object tkey = rev.get(key);
                map.remove(tkey);
            }

            long now = System.currentTimeMillis();

            Object o3[] = new Object[]{this, key, new Long(now)};
            Long tkey = new Long(now + timeoutMilli);

            update(key, tkey, o3);

            if (DEBUG) {
                size();
            }
            if (DEBUG) {
                System.out.println("put k,v:" + key + "," + value);
            }

            if (killer == null) {
                startKiller();
            }

            return oVal;
        }
    }

    private synchronized static void startKiller() {
        if (killer == null) {
            killer = new Killer();
            killer.start();
        }
    }

    /**
     * Method update.
     * 
     * @param key
     * @param tkey
     * @param o3
     */
    private void update(K key, Long tkey, Object[] o3) {
        Long tkeyOrg = tkey;

        while (map.get(tkey) != null) {
            tkey = new Long(tkey.longValue() + 1);
        }

        rev.put(key, tkey);
        map.put(tkey, o3);

        if (tkey == tkeyOrg) {
            // eine neue kleinste Weckzeit?
            if (tkey.equals(map.firstKey())) {
                if (killer == null) {
                    startKiller();
                }
                synchronized (killer) {
                    killer.notify();
                }
            }
        }
    }

    /**
     * Returns the creation time for the given key. This is the system time in milli seconds when the first put with
     * that key occured.
     * 
     * @param key
     *            the key
     * @return creation time or -1 on invalid key.
     */
    public long getCreationMillis(Object key) {
        Object tkey = rev.get(key);
        if (tkey != null) {
            Object[] o3 = map.get(tkey);
            if (o3 != null) {
                Long l = (Long) o3[2];
                return l.longValue();
            }
        }
        return -1;
    }

    /**
     * Returns the death time for the given key. This is the system time in milli seconds when the object gets invalid.
     * If the stored object implements the Callback interface, thie implementation of the Callback decides whether its
     * lifetime gets renewed,
     * 
     * @param key
     *            the key
     * @return death time or -1 on invalid key.
     */
    public long getDeathMillis(Object key) {
        Long tkey = rev.get(key);
        if (tkey != null) {
            return tkey.longValue();
        }
        return -1;
    }

    /**
     * Removes the key (and its corresponding value) from this SessionManager. This method does nothing if the key is
     * not in the SessionManager. If value is an instance of Callback, its dontRemove() method is called, with key as
     * param, then the removal depends on the result of that function.
     * 
     * @param key
     *            the key that needs to be removed.
     * @return the value to which the key had been mapped in this SessionManager, or null if the key did not have a
     *         mapping.
     */
    public Object removeButAsk(K key) {
        Object val, tkey;
        synchronized (map) {
            val = ht.get(key);
            if (val == null) {
                if (DEBUG) {
                    System.out.println("NOT FOUND");
                }
                remove(key);
                return null;
            }
            tkey = rev.get(key);
            if (val instanceof SessionManager.Callback) {
                if (DEBUG) {
                    System.out.println("notify k,v:" + key + "," + val);
                }

                try {
                    if (((SessionManager.Callback) val).dontRemove(key)) {
                        Object tkey2 = rev.get(key);
                        if (tkey == null || tkey2 == null || !tkey.equals(tkey2)) {
                            return null;
                        }
                        if (DEBUG) {
                            System.out.println("K,V,T: " + key + "," + val + "," + tkey + ": not touched -> touch it");
                        }
                        touch(key);
                        return null;
                    }
                } catch (Exception ex) {
                }
            }
        }
        return remove(key);
    }

    /**
     * Removes the key (and its corresponding value) from this SessionManager. This method does nothing if the key is
     * not in the SessionManager.
     * 
     * @param key
     *            the key that needs to be removed.
     * @return the value to which the key had been mapped in this SessionManager, or null if the key did not have a
     *         mapping.
     */
    public Object remove(Object key) {
        synchronized (map) {
            Object val = ht.get(key);
            if (val == null) {
                return null;
            }

            if (DEBUG) {
                System.out.println("remove k,v:" + key + "," + val);
            }
            Object tkey = rev.get(key);

            rev.remove(key);
            map.remove(tkey);
            Object o = ht.remove(key);
            if (DEBUG) {
                size();
            }
            return o;
        }
    }

    /**
     * Set a new maximum size. The new size only affects future invokations of put(...)!
     * 
     * @param max
     *            the new max count of sessions
     * @see #getMaxCount
     */
    public void setMaxCount(int max) {
        maxCount = max;
    }

    /**
     * Set a new default timout value. The new timout only affects future invokations of put(...) or touch(...)!
     * 
     * @param timeoutMilli
     *            the new default timout value
     * @see #getTimeout
     */
    public void setTimeout(long timeoutMilli) {
        timeout = timeoutMilli;
    }

    /**
     * Returns the number of keys in this SessionManager.
     * 
     * @return the number of keys in this SessionManager.
     */
    public int size() {
        if (DEBUG) {
            if (ht.size() != map.size()) {
                System.out.println("size ht,map,rev: " + ht.size() + "," + map.size() + "," + rev.size());
            }
        }
        return ht.size();
    }

    /**
     * Sets the new timeout for the given key with the default timeout.
     * 
     * @param key
     *            the key
     * @return the key or null if the key was not found in the SessionManager.
     */
    public Object touch(K key) {
        return touch(key, timeout);
    }

    /**
     * Sets the new timeout for the given key.
     * 
     * @param key
     *            the key
     * @param timeoutMilli
     *            the new timeout to set
     * @return the key or null if the key was not found in the SessionManager.
     */
    public Object touch(K key, long timeoutMilli) {
        if (key == null) {
            return null;
        }
        synchronized (map) {
            Object val = ht.get(key);
            if (null == val) {
                return null;
            }

            Long tkey = rev.get(key);
            Object[] o3 = map.remove(tkey);

            Long tkeyNew = new Long(System.currentTimeMillis() + timeoutMilli);

            update(key, tkeyNew, o3);

            return key;
        }
    }

    private static String zeros = "0000000000000000";

    /**
     * Create a new session key.
     * 
     * @return a new created String, containing a
     */
    public static String newKey() {
        String lh = Long.toHexString(rand.nextLong());
        return Long.toHexString(System.currentTimeMillis()) + zeros.substring(lh.length()) + lh;
    }

}
