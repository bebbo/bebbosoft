/******************************************************************************
 * Map implementation which allows duplicate keys.
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
	public MultiMap(final Comparator<K> comp_) {
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
	@Override
    public boolean remove(final Object key, final Object value) {
    	@SuppressWarnings("unchecked")
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
	@Override
    Leaf<K, V> find(final K key) {
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
						p = p.__l;
						lc = c;
					}
					if (p == null)
						break;
					p = p.__r;
				}
				if (f == null || lc != 0) {
					return p;
				}

				// handle special case: left < p, but a right child from p might
				// be equal!
				p = f.__l;
				while (p != null && cc.compareTo(p.key) > 0)
					p = p.__r;
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
					p = p.__l;
					lc = c;
				}
				if (p == null)
					break;
				p = p.__r;
			}
			if (f == null || lc != 0) {
				return p;
			}

			// handle special case: left < p, but a right child from p might be
			// equal!
			p = f.__l;
			while (p != null && comp.compare(key, p.key) > 0)
				p = p.__r;
			if (p == null)
				return f;
			f = null;
		}
	}

	@Override
    MapBase.Leaf<K, V> findNext(final K key) {
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
	@Override
    public V put(final K key, final V value) {
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
					if (i.__l == null) {
						neu = i.__l = new Leaf<K, V>(key, value);
						break;
					}
					i = i.__l;
				} else {
					if (i.__r == null) {
						neu = i.__r = new Leaf<K, V>(key, value);
						break;
					}
					i = i.__r;
				}
			}
		} else {
			for (;;) {
				if (comp.compare(key, i.key) < 0) {
					if (i.__l == null) {
						neu = i.__l = new Leaf<K, V>(key, value);
						break;
					}
					i = i.__l;
				} else {
					if (i.__r == null) {
						neu = i.__r = new Leaf<K, V>(key, value);
						break;
					}
					i = i.__r;
				}
			}
		}
		++count;
		neu.__top = i;
		fixAdd(neu);
		return null;
	}
}
