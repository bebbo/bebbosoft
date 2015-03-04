/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/SingleMap.java,v $
 * $Revision: 1.6 $
 * $Date: 2014/06/23 20:08:01 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * A sorted map based in the MapBase which allows now duplicate keys. 
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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

import java.util.*;

/**
 * This class is used to maintain key value pairs, sorted by key. All keys must
 * <ul>
 * <li>either use classes which are comparable vice versa be their compareTo() function</li>
 * <li>or which can be compared by the specified Comparator object.</li>
 * </ul>
 */
public class SingleMap<K, V> extends MapBase<K, V> {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3761129357720107316L;

    /**
     * Create a new SingleMap and use the specified Comparator object.
     * 
     * @param comp_
     *            the Comparator used to compare all keys vice versa.
     */
    public SingleMap(Comparator<K> comp_) {
        super(comp_);
    }

    /**
     * Create a new SingleMap and use the object's compareTo() function.
     */
    public SingleMap() {
    }

    /**
     * Insert a given object into the tree using the specified key.
     * 
     * @param key
     *            the key
     * @param value
     *            the inserted value
     * @return the old replaced value or null.
     */
    @Override
    public V put(K key, V value) {
        // handle empty tree: initialize root and count
        if (root == null) {
            root = new Leaf<K, V>(key, value);
            count = 1;
            return null;
        }

        // insert into tree
        Leaf<K, V> neu, i = root;
        if (comp == null) {
            @SuppressWarnings("unchecked")
            Comparable<K> cc = (Comparable<K>) key;
            for (;;) {
                int c = cc.compareTo(i.key);
                if (c == 0) {
                    V o = i.value;
                    i.value = value;
                    return o;
                }
                if (c < 0) {
                    if (i.l == null) {
                        neu = i.l = new Leaf<K, V>(key, value);
                        break;
                    }
                    i = i.l;
                } else {
                    if (i.r == null) {
                        neu = i.r = new Leaf<K, V>(key, value);
                        break;
                    }
                    i = i.r;
                }
            }
        } else {
            for (;;) {
                int c = comp.compare(key, i.key);
                if (c == 0) {
                    V o = i.value;
                    i.value = value;
                    return o;
                }
                if (c < 0) {
                    if (i.l == null) {
                        neu = i.l = new Leaf<K, V>(key, value);
                        break;
                    }
                    i = i.l;
                } else {
                    if (i.r == null) {
                        neu = i.r = new Leaf<K, V>(key, value);
                        break;
                    }
                    i = i.r;
                }
            }
        }
        ++count;
        neu.top = i;
        fixAdd(neu);
        return null;
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
    public boolean remove(Object key, Object value) {
    	final K kkey = (K) key;
        // search the key
        Leaf<K, V> p = find(kkey);
        if (p == null)
            return false;
        if (p.value != value)
            return false;
        return unlink(p) != null;
    }
}

/******************************************************************************
 * $Log: SingleMap.java,v $
 * Revision 1.6  2014/06/23 20:08:01  bebbo
 * @R changes caused by JDK 1.8
 *
 * Revision 1.5  2011/04/27 11:08:15  bebbo
 * @R the SingleMap and MultiMap are using template types now
 * Revision 1.4 2011/01/01 11:39:48 bebbo
 * 
 * @O improved speed Revision 1.3 2008/03/15 18:01:05 bebbo
 * 
 * @R Changed the license: From now on GPL 3 applies.
 * 
 *    Revision 1.2 2005/02/13 15:16:32 bebbo
 * @I nothing important: beautified imports, added SerialUID
 * 
 *    Revision 1.1 2001/09/15 08:57:37 bebbo
 * @N new
 * 
 *    Revision 1.2 2000/12/29 17:47:24 bebbo
 * @? dunno
 * 
 *    Revision 1.1 2000/11/09 15:06:17 bebbo
 * @R repackaged
 * 
 *****************************************************************************/
