/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/MultiMap.java,v $
 * $Revision: 1.12 $
 * $Date: 2014/06/23 20:08:01 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Map implementation which allows duplicate keys. 
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

import java.util.Comparator;

/**
 * This class is used to maintain key value pairs, sorted by key. All keys must
 * be usable by the specified Comparator object, or if no Comparator is used, by
 * the Objects compareTo function. <br>
 * <b>This implementation allows duplicate keys.</b>
 * <ul>
 * <li>If a key is inserted twice or more often, insert places the new entry
 * behind (rightmost) the existing entries.</li>
 * <li>If a key is removed, the first (leftmost) entry is removed.</li>
 * <li>To delete an explicit key, value pair, a remove(k,p) function exists.</li>
 * </ul>
 * In other words: <br>
 * If only one constant key is used, to insert, get and remove values, it acts
 * like a FIFO.
 */
public class MultiMap<K, V> extends MapBase<K, V> {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3545511807420020020L;

	/**
	 * Creates a MultiMap object, using the specified Comparator.
	 * 
	 * @param comp_
	 *            a Comparator object.
	 */
	public MultiMap(Comparator<K> comp_) {
		super(comp_);
	}

	/**
	 * Creates a MultiMap object without a Comparator.
	 */
	public MultiMap() {
	}

	/**
	 * Removes the key holding the specified value from this Map. Useful in
	 * MultiMap to remove a distinct key / value entry. This method does nothing
	 * if the key is not in the Map. If value is null, the first matching key is
	 * removed.
	 * 
	 * @param key
	 *            the key that needs to be removed.
	 * @param value
	 *            the value at the key that needs to be removed.
	 * @return the value to which the key had been mapped in this MultiMap, or
	 *         null if the key did not have a mapping.
	 */
	public boolean remove(Object key, Object value) {
    	final K kkey = (K) key;
    	if (comp == null) {
			@SuppressWarnings("unchecked")
			Comparable<K> cc = (Comparable<K>) key;
			for (Leaf<K, V> i = find(kkey); i != null; i = i.next()) {
				if (cc.compareTo(i.key) != 0)
					return false;
				if (i.value.equals(value))
					return unlink(i) != null;
			}
			return false;
		}
		for (Leaf<K, V> i = find(kkey); i != null; i = i.next()) {
			if (comp.compare(kkey, i.key) != 0)
				return false;
			if (i.value.equals(value))
				return unlink(i) != null;
		}
		return false;
	}

	/**
	 * Really finds the first entry with the possible duplicate key.
	 * 
	 * @param key
	 *            a key
	 * @return the really first Leaf with that key, or null
	 */
	Leaf<K, V> find(K key) {
		Leaf<K, V> p = root;
		Leaf<K, V> f = null;
		if (comp == null) {
			@SuppressWarnings("unchecked")
			Comparable<K> cc = (Comparable<K>) key;
			for (;;) {
				int lc = 1;
				while (p != null) {
					int c;
					while (p != null && (c = cc.compareTo(p.key)) <= 0) {
						f = p;
						p = p.l;
						lc = c;
					}
					if (p == null)
						break;
					p = p.r;
				}
				if (f == null || lc != 0) {
					return p;
				}

				// handle special case: left < p, but a right child from p might
				// be equal!
				p = f.l;
				while (p != null && cc.compareTo(p.key) > 0)
					p = p.r;
				if (p == null)
					return f;
				f = null;
			}
		}
		for (;;) {
			int lc = 1;
			while (p != null) {
				int c;
				while (p != null && (c = comp.compare(key, p.key)) <= 0) {
					f = p;
					p = p.l;
					lc = c;
				}
				if (p == null)
					break;
				p = p.r;
			}
			if (f == null || lc != 0) {
				return p;
			}

			// handle special case: left < p, but a right child from p might be
			// equal!
			p = f.l;
			while (p != null && comp.compare(key, p.key) > 0)
				p = p.r;
			if (p == null)
				return f;
			f = null;
		}
	}

	MapBase.Leaf<K, V> findNext(K key) {
		MapBase.Leaf<K, V> l = find(key);
		if (l != null)
			return l;
		return super.findNext(key);
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
				if (cc.compareTo(i.key) < 0) {
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
				if (comp.compare(key, i.key) < 0) {
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
}

/******************************************************************************
 * $Log: MultiMap.java,v $
 * Revision 1.12  2014/06/23 20:08:01  bebbo
 * @R changes caused by JDK 1.8
 * Revision 1.11 2011/04/27 11:08:12 bebbo
 * 
 * @R the SingleMap and MultiMap are using template types now Revision 1.10
 *    2011/01/01 11:39:47 bebbo
 * 
 * @O improved speed Revision 1.9 2008/03/15 18:01:05 bebbo
 * 
 * @R Changed the license: From now on GPL 3 applies.
 * 
 *    Revision 1.8 2005/02/13 15:16:32 bebbo
 * @I nothing important: beautified imports, added SerialUID
 * 
 *    Revision 1.7 2002/09/09 09:40:36 bebbo
 * @B fixed findNext in MultiMap
 * 
 *    Revision 1.6 2002/03/30 15:40:13 franke
 * @B fix in find() method. If key was not an element of the map, sometimes a
 *    wrong value was returned instead of null
 * 
 *    Revision 1.5 2001/12/10 16:22:50 bebbo
 * @C completed comments!
 * 
 *    Revision 1.4 2001/11/04 18:36:52 franke
 * @B value is now compared using equals()
 * 
 *    Revision 1.3 2001/09/15 08:57:52 bebbo
 * @R complete new design, based on MapBase
 * 
 *    Revision 1.2 2000/12/29 17:47:24 bebbo
 * @? dunno
 * 
 *    Revision 1.1 2000/11/09 15:06:17 bebbo
 * @R repackaged
 * 
 *****************************************************************************/
