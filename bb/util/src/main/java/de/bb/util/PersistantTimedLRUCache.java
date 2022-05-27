/******************************************************************************
 * An experimental Cache implementation which uses the LRUCache and is able to persist entries.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A LRU cache with limited life time to ensure refreshing of content. This
 * implementation utilizes the memory friendly
 * de.bb.util.LRUCache.
 * 
 * after max life time is reached.
 * 
 * @author sfranke
 * 
 */
public class PersistantTimedLRUCache<K, V> extends TimedLRUCache<K, V> {
	private String folder;

	private int readCount;

	/**
	 * Create a new PersistantTimedLRUCache instance.
	 * 
	 * @param folder
	 *                        the folder to write the files to.
	 * @param defaultLifeTime
	 *                        the default life time in milli seconds.
	 */
	public PersistantTimedLRUCache(String folder, long defaultLifeTime) {
		super(defaultLifeTime);
		this.folder = folder;
	}

	/**
	 * Create a new PresistantTimedLRUCache instance. Also specify the ratio (hard
	 * references / soft references) for the
	 * underlying cache.
	 * 
	 * @param folder
	 *                        the folder to write the files to.
	 * @param defaultLifeTime
	 *                        the default life time in milli seconds.
	 * @param ratio
	 *                        define the ratio for the underlying cache.
	 */
	@Deprecated
	public PersistantTimedLRUCache(String folder, long defaultLifeTime, double ratio) {
		super(defaultLifeTime);
		this.folder = folder;
	}

	/**
	 * get an object from the cache. This implementation tries to load it from disk,
	 * if it is not in the memory.
	 * 
	 * @param key
	 *            the key to perform the lookup
	 * @return the object if found, or null if not found or outdated.
	 */
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		Valuette<V> valuette = __sget((K) key);
		if (valuette != null) {
			// no remove occurs here
			// we rely on the fact that on a cache miss a new put will follow
			if (valuette.endOfLife > System.currentTimeMillis()) {
				return valuette.value;
			}
			super.remove(key);
			return null;
		}

		Valuette<V> v = readFromDisk((K) key, true);
		if (v == null)
			return null;
		__sput((K) key, v);
		return v.value;
	}

	/**
	 * get an object from the cache. This implementation tries to load it from disk,
	 * if it is not in the memory.
	 * 
	 * @param key
	 *            the key to perform the lookup
	 * @return the object if found, or null if not found or outdated.
	 */
	public V remove(Object key) {
		try {
			File fp = makeFilePath(key);
			fp.delete();
		} catch (Throwable t) {
		}
		return super.remove(key);
	}

	/**
	 * Returns the out dated value if there is any. Useful to reuse the outdate
	 * value while calculating the new one in
	 * the background for further gets.
	 * 
	 * @param key
	 *            the key to lookup
	 * @return an entrie if it exists.
	 */

	public Object getOutDated(K key) {
		Valuette<V> valuette = __sget(key);
		if (valuette != null) {
			return valuette.value;
		}
		Valuette<V> v = readFromDisk(key, false);
		if (v == null)
			return null;
		return v.value;
	}

	@SuppressWarnings("unchecked")
	private Valuette<V> readFromDisk(K key, boolean validOnly) {
		File file = makeFilePath(key);
		if (!file.exists())
			return null;

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fis, 2048));
			long eoLife = ois.readLong();
			// outdated?
			if (validOnly && eoLife < System.currentTimeMillis()) {
				return null;
			}
			// still valid --> back into cache
			V value = (V) ois.readObject();
			++readCount;
			return new Valuette<V>(value, eoLife);
		} catch (Exception ex) {
			file.delete();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception ex2) {
				}
			}
		}
		return null;
	}

	public V put(K key, V value) {
		long eoLife = System.currentTimeMillis() + lifeTime;
		if (value instanceof Serializable) {
			// create a new persistant file
			File file = makeFilePath(key);
			try (FileOutputStream fos = new FileOutputStream(file)) {
				try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
					oos.writeLong(eoLife);
					oos.writeObject(value);
				}
			} catch (Exception ex) {
			}
		}
		return __sput(key, new Valuette<V>(value, eoLife));
	}

	private File makeFilePath(Object key) {
		String path2 = unescape(key.toString());
		if (path2.endsWith("/"))
			path2 = path2.substring(0, path2.length() - 1);
		File file = new File(folder, path2);
		file.getParentFile().mkdirs();
		return file;
	}

	private String unescape(String s) {
		StringBuffer sb = new StringBuffer();

		int slen = s.length();

		for (int i = 0; i < slen; ++i) {
			int ch = s.charAt(i);
			if (ch < 32 || ch > 127 || "\\".indexOf(ch) >= 0) {
				sb.append("__");
				String hex = Integer.toHexString(ch);
				while (hex.length() < 4)
					hex = "0" + hex;
				sb.append(hex);
			} else
				sb.append((char) ch);
		}

		return sb.toString();
	}

	public String toString() {
		return "PersistantTimedLRUCache - " + readCount + " reads - using\r\n" + super.toString();
	}
}
