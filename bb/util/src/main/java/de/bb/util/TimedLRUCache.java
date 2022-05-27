/******************************************************************************
 * An LRU cache implementation adding a life time to the items.  
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

import java.util.Random;

/**
 * A LRU cache with limited life time to ensure refreshing of content. This implementation utilizes the memory friendly
 * de.bb.util.LRUCache.
 * 
 * after max life time is reached.
 * 
 * @author sfranke
 * 
 */
public class TimedLRUCache<K, V> {

    private LRUCache<K, Valuette<V>> parent;

    private Random random = new Random();
    /** the life time for objects. */
    protected long lifeTime;
    /** since we add randomness the range for deviations is calculated and held. */
    protected long lifeTimeRange;

    /**
     * Create a new TimedLRUCache instance.
     * 
     * @param defaultLifeTime
     *            the default life time in milliseconds.
     */
    public TimedLRUCache(long defaultLifeTime) {
        this(defaultLifeTime, 0.1);
    }

    /**
     * Create a new TimedLRUCache instance. Also specify the ratio (hard references / soft references) for the
     * underlying cache.
     * 
     * @param defaultLifeTime
     *            the default life time in milliseconds.
     * @param ratio
     *            define the ratio for the underlying cache.
     */
    @Deprecated
    public TimedLRUCache(long defaultLifeTime, double ratio) {
        parent = new LRUCache<K, Valuette<V>>();
        setLifeTime(defaultLifeTime);
    }

    /**
     * Set the default time out.
     * 
     * @param defaultLifeTime
     *            set a new default life time.
     */
    public void setLifeTime(long defaultLifeTime) {
        this.lifeTime = defaultLifeTime;
        this.lifeTimeRange = defaultLifeTime / 20;
    }

    /**
     * Put a value into the cache using the specified key. Apply the specified lifeTime instead of the default life
     * time.
     * 
     * @param key
     *            a key object - not null!
     * @param value
     *            a value object
     * @param specificLifeTime
     *            a life time in milliseconds
     */
    public void put(K key, V value, long specificLifeTime) {
        parent.put(key, new Valuette<V>(value, System.currentTimeMillis() + specificLifeTime));
    }

    /**
     * Put a value into the cache using the specified key. The default life time is applied.
     * 
     * @param key
     *            a key object - not null!
     * @param value
     *            a value object
     */
    public V put(K key, V value) {
        long eoLife = System.currentTimeMillis() + lifeTime + random.nextLong() % lifeTimeRange;
        Valuette<V> val = parent.put(key, new Valuette<V>(value, eoLife));
        if (val != null)
            return val.value;
        return null;
    }

    /**
     * Returns the value to the specified key. Returns the value if
     * <ul>
     * <li>the value is still in the underlying cache</li>
     * <li>and the life time has not expired</li>
     * </ul>
     * otherwise return null;
     * 
     * @param key
     *            a key object - not null!
     * @return the value or null;
     */
    public V get(Object key) {
        Valuette<V> valuette = parent.get(key);
        if (valuette == null)
            return null;
        // no remove occurs here
        // we rely on the fact that on a cache miss a new put will follow
        if (valuette.endOfLife < System.currentTimeMillis()) {
            parent.remove(key);
            return null;
        }
        return valuette.value;
    }

    /**
     * Get a Valuette for the key.
     * 
     * @param key
     *            the key
     * @return the Valuette or null.
     */
    protected Valuette<V> __sget(K key) {
        return parent.get(key);
    }

    /**
     * Put a Valuette for the specified key.
     * 
     * @param key
     *            the key
     * @param v
     *            the Valuette
     */
    @SuppressWarnings("unchecked")
	protected V __sput(K key, Valuette<V> v) {
        return (V) parent.put(key, v);
    }

    /**
     * Removes the associated value from the underlying cache.
     * 
     * @param key
     *            a key object - not null!
     * @return the removed object if it exists.
     */
    public V remove(Object key) {
        Valuette<V> valuette = parent.remove(key);
        if (valuette == null)
            return null;
        return valuette.value;
    }

    /**
     * Removes all keys and values from the underlying cache.
     * 
     */
    public void clear() {
        parent.clear();
    }

    /**
     * Returns the size of the underlying cache.
     * 
     * @return the size of the underlying cache.
     */
    public int size() {
        return parent.size();
    }

    /**
     * Some verbose information about the cache.
     * 
     * @return a String containing some verbose information about the cache.
     */
    public String toString() {
        return "TimedLRUCache: " + this.lifeTime + "ms - using\r\n" + super.toString();
    }

    /**
     * Helper class to track endOfLife/Object pairs. This class is not visible and usable to the outer space.
     * 
     * @author sfranke
     */
    static class Valuette<V> {
        V value;
        long endOfLife;

        Valuette(V value, long endOfLife) {
            this.value = value;
            this.endOfLife = endOfLife;
        }
    }
}
