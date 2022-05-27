/******************************************************************************
 * Base class for map implementations.  
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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * This is the base class for SingleMap and MultiMap and contains the common part of both implementations. The
 * implementation uses an estimate AVL sorted tree.
 */
abstract class MapBase<K, V> extends java.util.AbstractMap<K, V> implements SortedMap<K, V>, Serializable {

    private static final long serialVersionUID = 8150250365008017693L;

    // / object to compare two keys
    protected Comparator<K> comp;

    // / the root of all stored items
    MapBase.Leaf<K, V> root;

    // / count of items in tree
    int count;

    /**
     * helper class for elements
     */
    static class Leaf<K, V> implements java.util.Map.Entry<K, V>, Serializable {
        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 3256999977816371506L;

        // / refs to top, left nd right
        MapBase.Leaf<K, V> top, l, r;

        // / left = -1, equal = 0, right = 1
        byte len;

        // / the maintained object
        K key;
        V value;

        /**
         * creates an empty leaf
         * 
         * @param k
         * @param v
         */
        Leaf(K k, V v) {
            top = l = r = null;
            len = 0;
            key = k;
            value = v;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map.Entry#getKey()
         */

        public K getKey() {
            return key;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map.Entry#getValue()
         */

        public V getValue() {
            return value;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */

        public V setValue(V o) {
            V l = value;
            value = o;
            return l;
        }

        /**
         * get the next - sorting order is defined by tree
         * 
         * @return the next Leaf or null
         */
        MapBase.Leaf<K, V> next() {
            Leaf<K, V> i = this;
            if (i.r != null) {
                for (i = i.r; i.l != null;)
                    i = i.l;
                return i;
            }
            Leaf<K, V> j = i.top;
            for (; j != null && j.r == i;) {
                i = j;
                j = i.top;
            }
            return j;
        }

        /**
         * get the previous - sorting order is defined by tree
         * 
         * @return the next Leaf or null
         */
        Leaf<K, V> prev() {
            Leaf<K, V> i = this;
            if (i.l != null) {
                for (i = i.l; i.r != null;)
                    i = i.r;
                return i;
            }
            Leaf<K, V> j = i.top;
            for (; j != null && j.l == i;) {
                i = j;
                j = i.top;
            }
            return j;
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    private class SubSet extends AbstractSet<Entry<K, V>> implements SortedSet<Entry<K, V>> {

        private Leaf<K, V> fromKey, toKey;

        private Comparator<Entry<K, V>> c;

        SubSet(Entry<K, V> from, Entry<K, V> to) {
            fromKey = (Leaf<K, V>) from;
            toKey = (Leaf<K, V>) to;

            c = new Comparator<Entry<K, V>>() {

                public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                    return MapBase.this.compare(o1.getKey(), o2.getKey());
                }
            };
        }

        private boolean inRange(Entry<K, V> key) {
            if (fromKey != null && c.compare(fromKey, key) > 0)
                return false;
            if (toKey != null && c.compare(toKey, key) <= 0)
                return false;
            return true;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#comparator()
         */

        public Comparator<Entry<K, V>> comparator() {
            return c;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#first()
         */

        public Leaf<K, V> first() {
            if (fromKey == null)
                return MapBase.this.firstLeaf();
            MapBase.Leaf<K, V> l = MapBase.this.findNext(fromKey.key);
            if (l == null || !inRange(l))
                return null;
            return l;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#headSet(java.lang.Object)
         */

        public java.util.SortedSet<Entry<K, V>> headSet(Entry<K, V> to) {
            if (!inRange(to))
                throw new IllegalArgumentException("key out of range");
            return new SubSet(fromKey, to);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#last()
         */

        public MapBase.Leaf<K, V> last() {
            MapBase.Leaf<K, V> l;
            if (toKey == null) {
                l = MapBase.this.lastLeaf();
                if (l == null || !inRange(l))
                    return null;
                return l;
            }

            l = MapBase.this.findNext(toKey.key);
            if (l == null) {
                l = MapBase.this.lastLeaf();
                if (l == null)
                    return null;
                if (!inRange(l))
                    return null;
                return l;
            }
            MapBase.Leaf<K, V> l2 = l.prev();
            if (l2 == null || !inRange(l2))
                return null;
            return l2;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
         */

        public java.util.SortedSet<Entry<K, V>> subSet(Entry<K, V> from, Entry<K, V> to) {
            if (!inRange(to) || !inRange(from))
                throw new IllegalArgumentException("key out of range");
            return new SubSet(from, to);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.SortedSet#tailSet(java.lang.Object)
         */

        public java.util.SortedSet<Entry<K, V>> tailSet(Entry<K, V> from) {
            if (!inRange(from))
                throw new IllegalArgumentException("key out of range");
            return new SubSet(from, toKey);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Set#iterator()
         */

        public Iterator<Entry<K, V>> iterator() {
            return new Iter(first(), last());
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Set#size()
         */

        public int size() {
            if (fromKey == null && toKey == null)
                return MapBase.this.size();
            int sz = 0;
            for (Iterator<Entry<K, V>> i = iterator(); i.hasNext(); i.next())
                ++sz;
            return sz;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Set#isEmpty()
         */

        public boolean isEmpty() {
            return first() == null;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer("{");
            for (Iterator<Entry<K, V>> i = iterator(); i.hasNext();) {
                sb.append(i.next());
                if (i.hasNext())
                    sb.append(", ");
            }
            return sb.append("}").toString();
        }

        private class Iter implements Iterator<Entry<K, V>>, Cloneable {

            MapBase.Leaf<K, V> here, last, end;

            Iter(MapBase.Leaf<K, V> from, MapBase.Leaf<K, V> to) {
                here = from;
                last = null;
                end = to;
            }

            /**
             * (non-Javadoc)
             * 
             * @see java.util.Iterator#hasNext()
             */

            public boolean hasNext() {
                return last != end && here != null;
            }

            /**
             * (non-Javadoc)
             * 
             * @see java.util.Iterator#next()
             */

            public Entry<K, V> next() {
                last = here;
                here = here.next();
                return last;
            }

            /**
             * (non-Javadoc)
             * 
             * @see java.util.Iterator#remove()
             */

            public void remove() {
                MapBase.this.remove(last.key, last.value);
            }

            /**
             * (non-Javadoc)
             * 
             * @see java.lang.Object#clone()
             */

            public Object clone() {
                Iter i = new Iter(here, end);
                i.last = last;
                return i;
            }
        }
    }

    /**
     * creates a Map object
     * 
     * @param comp
     *            the used comparator
     */
    MapBase(Comparator<K> comp) {
        this.comp = comp;
        root = null;
        count = 0;
    }

    /**
     * creates a Map object
     */
    MapBase() {
        this(null);
    }

    // ============================================================================
    // public functions
    // ============================================================================
    /**
     * Clears this Map so that it contains no objects.
     */

    public void clear() {
        root = null;
        count = 0;
        // rest does the garbage collection
    }

    /**
     * Returns the number of components in this Map.
     * 
     * @return the number of components in this Map.
     */

    public int size() {
        return count;
    }

    /**
     * Tests if this Map has no components.
     * 
     * @return true if this Map has no components; false otherwise.
     */

    public final boolean isEmpty() {
        return count == 0;
    }

    /**
     * get the sorted first leaf.
     * 
     * @return the sorted first leaf.
     */
    MapBase.Leaf<K, V> firstLeaf() {
        if (root == null)
            return null;
        MapBase.Leaf<K, V> i = root;
        while (i.l != null)
            i = i.l;
        return i;
    }

    /**
     * get the sorted last leaf.
     * 
     * @return the sorted last leaf.
     */
    MapBase.Leaf<K, V> lastLeaf() {
        if (root == null)
            return null;
        MapBase.Leaf<K, V> i = root;
        while (i.r != null)
            i = i.r;
        return i;
    }

    private final void drr(MapBase.Leaf<K, V> i, MapBase.Leaf<K, V> j) {
        // System.out.println("double rot right");
        MapBase.Leaf<K, V> k = j.top;
        if (k == null)
            root = i.r;
        else if (k.l == j)
            k.l = i.r;
        else
            k.r = i.r;
        i.r.top = k;

        k = i.r;

        i.r = k.l;
        if (i.r != null)
            i.r.top = i;
        i.top = k;
        i.len = k.len <= 0 ? (byte) 0 : -1;

        j.l = k.r;
        if (j.l != null)
            j.l.top = j;
        j.top = k;
        j.len = k.len >= 0 ? (byte) 0 : 1;

        k.l = i;
        k.r = j;
        k.len = 0;
    }

    private final void rr(MapBase.Leaf<K, V> i, MapBase.Leaf<K, V> j) {
        // System.out.println("rot right");
        MapBase.Leaf<K, V> k = j.top;
        if (k == null)
            root = i;
        else if (k.l == j)
            k.l = i;
        else
            k.r = i;

        j.l = i.r;
        if (j.l != null)
            j.l.top = j;
        j.top = i;

        i.r = j;
        i.top = k;
        ++i.len;
        // if (i.len != 0) System.out.println("*");
        j.len = (byte) -i.len;
    }

    private final void drl(MapBase.Leaf<K, V> i, MapBase.Leaf<K, V> j) {
        // System.out.println("double rot left");
        MapBase.Leaf<K, V> k = j.top;
        if (k == null)
            root = i.l;
        else if (k.l == j)
            k.l = i.l;
        else
            k.r = i.l;
        i.l.top = k;
        k = i.l;

        i.l = k.r;
        if (i.l != null)
            i.l.top = i;
        i.top = k;
        i.len = k.len >= 0 ? (byte) 0 : 1;

        j.r = k.l;
        if (j.r != null)
            j.r.top = j;
        j.top = k;
        j.len = k.len <= 0 ? (byte) 0 : -1;

        k.r = i;
        k.l = j;
        k.len = 0;
    }

    private final void rl(MapBase.Leaf<K, V> i, MapBase.Leaf<K, V> j) {
        // System.out.println("rot left");
        MapBase.Leaf<K, V> k = j.top;
        if (k == null)
            root = i;
        else if (k.l == j)
            k.l = i;
        else
            k.r = i;

        j.r = i.l;
        if (j.r != null)
            j.r.top = j;
        j.top = i;

        i.l = j;
        i.top = k;
        --i.len;
        // if (i.len != 0) System.out.println("*");
        j.len = (byte) -i.len;
    }

    /**
     * Rebalance the tree on add.
     * 
     * @param i
     */
    protected final void fixAdd(MapBase.Leaf<K, V> i) {
        for (MapBase.Leaf<K, V> j = i.top; j != null; i = j, j = i.top) {
            if (j.l == i)
                --j.len;
            else
                ++j.len;

            if (j.len == 0) {
                return;
            }
            if (j.len == -2) {
                if (i.len > 0) {
                    drr(i, j);
                    return;
                }
                rr(i, j);
                return;
            }
            if (j.len == 2) {
                if (i.len < 0) {
                    drl(i, j);
                    return;
                }
                rl(i, j);
                return;
            }
        }
    }

    /**
     * Rebalance the tree on remove.
     * 
     * @param i
     *            the Leaf which must be removed after fixage
     */
    final void fixRemove(MapBase.Leaf<K, V> i) {
        for (MapBase.Leaf<K, V> j = i.top; j != null; i = j, j = i.top) {
            if (j.l == i)
                ++j.len;
            else
                --j.len;

            // change in length
            if (j.len == 0)
                continue;

            // no change in length
            if (j.len == 1 || j.len == -1)
                return;

            if (j.len == -2) {
                i = j.l;
                if (i.len > 0) {
                    drr(i, j);
                    j = j.top;
                    continue;
                }
                rr(i, j);
                if (i.len != 0)
                    return;
                j = i;
                continue;
            }
            if (j.len == 2) {
                i = j.r;
                if (i.len < 0) {
                    drl(i, j);
                    j = j.top;
                    continue;
                }
                rl(i, j);
                if (i.len != 0)
                    return;
                j = i;
                continue;
            }
        }
    }

    MapBase.Leaf<K, V> find(K key) {
        if (comp == null) {
            @SuppressWarnings("unchecked")
            Comparable<K> cc = (Comparable<K>) key;
            for (MapBase.Leaf<K, V> p = root; p != null;) {
                int c = cc.compareTo(p.key);
                if (c == 0)
                    return p;
                if (c > 0)
                    p = p.r;
                else
                    p = p.l;
            }
            return null;
        }
        for (MapBase.Leaf<K, V> p = root; p != null;) {
            int c = comp.compare(key, p.key);
            if (c == 0)
                return p;
            if (c > 0)
                p = p.r;
            else
                p = p.l;
        }
        return null;
    }

    /**
     * finds the next Leaf which either fits the key or is the right next.
     * 
     * @param key
     * @return the matching Leaf or null if none was found.
     */
    MapBase.Leaf<K, V> findNext(K key) {
        MapBase.Leaf<K, V> l = null;

        if (comp == null) {
            @SuppressWarnings("unchecked")
            Comparable<K> cc = (Comparable<K>) key;
            for (MapBase.Leaf<K, V> p = root; p != null;) {
                int c = cc.compareTo(p.key);
                if (c == 0)
                    return p;
                if (c > 0) {
                    l = p;
                    p = p.r;
                } else
                    p = p.l;
            }

            if (l == null)
                return firstLeaf();

            for (;;) {
                l = l.next();
                if (l == null)
                    break;
                if (cc.compareTo(l.key) < 0)
                    break;
            }
            return l;
        }
        for (MapBase.Leaf<K, V> p = root; p != null;) {
            int c = comp.compare(key, p.key);
            if (c == 0)
                return p;
            if (c > 0) {
                l = p;
                p = p.r;
            } else
                p = p.l;
        }

        if (l == null)
            return firstLeaf();

        for (;;) {
            l = l.next();
            if (l == null)
                break;
            if (comp.compare(key, l.key) < 0)
                break;
        }
        return l;
    }

    // remove the Leaf from the tree
    final V unlink(MapBase.Leaf<K, V> i) {
        --count;
        if (count == 0) {
            root = null;
            return i.value;
        }
        if (i.l != null && i.r != null) {
            // seek replacement
            MapBase.Leaf<K, V> r = i.len > 0 ? i.next() : i.prev();

            V o = i.value;
            i.key = r.key;
            i.value = r.value;
            i = r;
            i.value = o;
        }

        // do direct unlink
        fixRemove(i);
        MapBase.Leaf<K, V> j = i.l == null ? i.r : i.l;
        MapBase.Leaf<K, V> k = i.top;
        if (k == null)
            root = j;
        else {
            if (k.l == i) {
                k.l = j;
            } else {
                k.r = j;
            }
        }
        if (j != null)
            j.top = k;
        return i.value;
    }

    /**
     * Get the element for the specified value
     * 
     * @param key
     *            the key for the element to search
     * @return the value if found, either null
     */

    public final V get(Object key) {
        @SuppressWarnings("unchecked")
        MapBase.Leaf<K, V> l = find((K) key);
        if (l == null)
            return null;
        return l.value;
    }

    /**
     * Removes the key holding the specified value from this Map. Useful in MultiMap to remove a distinct key / value
     * entry. This method does nothing if the key is not in the Map. If value is null, the first matching key is
     * removed.
     * 
     * @param key
     *            the key that needs to be removed.
     * @return the value to which the key had been mapped in this MultiMap, or null if the key did not have a mapping.
     */

    public V remove(Object key) {
        // search the key
        @SuppressWarnings("unchecked")
        MapBase.Leaf<K, V> p = find((K) key);
        if (p == null)
            return null;
        return unlink(p);
    }

    /**
     * Removes the key holding the specified value from this Map. Useful in MultiMap to remove a distinct key / value
     * entry. This method does nothing if the key is not in the Map. If value is null, the first matching key is
     * removed.
     * 
     * @param key
     *            the key that needs to be removed.
     * @param value
     *            the value at the key that needs to be removed.
     * @return the value to which the key had been mapped in this MultiMap, or null if the key did not have a mapping.
     */
    public abstract boolean remove(Object key, Object value);

    @SuppressWarnings("unchecked")
    protected final int compare(K a, K b) {
        return comp == null ? ((Comparable<K>) a).compareTo(b) : comp.compare(a, b);
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @param key
     *            key whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @throws NullPointerException
     *             if the key is <tt>null</tt> and this map does not not permit <tt>null</tt> keys.
     */

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Return a Set for this Map's entries.
     * 
     * @return a Set for this Map's entries.
     */

    public Set<Entry<K, V>> entrySet() {
        return new SubSet(null, null);
    }

    /**
     * Return the used Comparator object, if any.
     * 
     * @return the used Comparator object, or null.
     */
    public java.util.Comparator<K> comparator() {
        return comp;
    }

    /**
     * Return the key of the sorted first entry in this Map.
     * 
     * @return the key of the sorted first entry in this Map, or null on empty Map.
     */
    public K firstKey() {
        if (root == null)
            return null;
        return firstLeaf().key;
    }

    /**
     * Return the key of the sorted last entry in this Map.
     * 
     * @return the key of the sorted last entry in this Map, or null on empty Map.
     */

    public K lastKey() {
        if (root == null)
            return null;
        return lastLeaf().key;
    }

    /**
     * Create a sub map view of this map. The parameters specify the start and the not included end point. It is also
     * possible to create a SubMap from this SubMap.
     * 
     * @param from
     *            first key of the sub map
     * @param to
     *            first key <b>behind</b> the last key.
     * @return a new allocated object which is a view to the underlying map.
     */

    public SortedMap<K, V> subMap(K from, K to) {
        return new SubMap(from, to);
    }

    /**
     * Create a sub map view of this map. The parameter specifies the start point, and the sub map ends at the same
     * point as this map. It is also possible to create a SubMap from this SubMap.
     * 
     * @param from
     *            first key of the sub map
     * @return a new allocated object which is a view to the underlying map.
     */

    public SortedMap<K, V> tailMap(K from) {
        return subMap(from, null);
    }

    /**
     * Create a sub map view of this map. The sub map starts at the same point as this map, and the parameter specifies
     * the not included end point. It is also possible to create a SubMap from this SubMap.
     * 
     * @param to
     *            first key <b>behind</b> the last key.
     * @return a new allocated object which is a view to the underlying map.
     */

    public SortedMap<K, V> headMap(K to) {
        return subMap(null, to);
    }

    /**
     * displays the members with toString().
     * 
     * @return a String with all members.
     */

    public String toString() {
        StringBuffer sb = new StringBuffer("{");
        Leaf<K, V> l = firstLeaf();
        while (l != null) {
            sb.append(l.key).append('=').append(l.value);
            l = l.next();
            if (l != null)
                sb.append(", ");
        }
        return sb.append("}").toString();
    }

    private class SubMap extends AbstractMap<K, V> implements SortedMap<K, V> {

        private K fromKey, toKey;

        SubMap(K from, K to) {
            fromKey = from;
            toKey = to;
        }

        private boolean inRange(K key) {
            if (fromKey != null && MapBase.this.compare(fromKey, key) > 0)
                return false;
            if (toKey != null && MapBase.this.compare(toKey, key) <= 0)
                return false;
            return true;
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#comparator()
         */

        public java.util.Comparator<K> comparator() {
            return MapBase.this.comparator();
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#firstKey()
         */

        public K firstKey() {
            if (fromKey == null)
                return MapBase.this.firstKey();
            MapBase.Leaf<K, V> l = MapBase.this.findNext(fromKey);
            if (l == null || !inRange(l.key))
                return null;
            return l.key;
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#headMap(java.lang.Object)
         */

        public SortedMap<K, V> headMap(K to) {
            if (!inRange(to))
                throw new IllegalArgumentException("key out of range");
            return new SubMap(fromKey, to);
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#lastKey()
         */

        public K lastKey() {
            if (toKey == null)
                return null;
            MapBase.Leaf<K, V> l = MapBase.this.findNext(toKey);
            if (l == null) {
                l = MapBase.this.lastLeaf();
                if (inRange(l.key))
                    return l.key;
                return null;
            }
            MapBase.Leaf<K, V> l2 = l.prev();
            if (l2 == null || !inRange(l2.key))
                return firstKey();
            return l2.key;
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#subMap(java.lang.Object, java.lang.Object)
         */

        public SortedMap<K, V> subMap(K from, K to) {
            if (!inRange(from) || !inRange(to))
                throw new IllegalArgumentException("key out of range");
            return new SubMap(from, to);
        }

        /**
         * (non-Javadoc)
         * 
         * @see SortedMap#tailMap(java.lang.Object)
         */

        public SortedMap<K, V> tailMap(K from) {
            if (!inRange(from))
                throw new IllegalArgumentException("key out of range");
            return new SubMap(from, toKey);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#entrySet()
         */

        public Set<Entry<K, V>> entrySet() {
            Leaf<K, V> first = null;
            if (fromKey != null) {
                first = findNext(fromKey);
                if (first == null)
                    return Collections.emptySet();
            }
            return new SubSet(first, toKey == null ? null : findNext(toKey));
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#size()
         */

        public int size() {
            return entrySet().size();
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#isEmpty()
         */

        public boolean isEmpty() {
            return firstKey() == null;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#containsKey(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        public boolean containsKey(Object key) {
            return inRange((K) key) && MapBase.this.containsKey(key);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#get(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        public V get(Object key) {
            if (!inRange((K) key))
                return null;
            return MapBase.this.get(key);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#put(java.lang.Object, java.lang.Object)
         */

        public V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return MapBase.this.put(key, value);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.util.Map#remove(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        public V remove(Object key) {
            if (!inRange((K) key))
                throw new IllegalArgumentException("key out of range");
            return MapBase.this.remove(key);
        }
    }
}
