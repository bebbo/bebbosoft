/******************************************************************************
 * Handles Windows like ini file which provides more functionality than property files.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides the functionality to use and maintain Windows(TM)-style *.ini files.
 * This implementation locks the file and keep all data in memory
 * <ul>
 * <li>a hashtable is used to hold the sections</li>
 * <li>for each section a hashtable and a vector is used to hold the keys, its
 * values and their comments</li>
 * </ul>
 */
public class IniFile {
	private SingleMap<String, Serializable> sections; // hold the sections
// changed implementation:
// each section is a vector now. the first element is the hastable for the keys
// all other elements are comments
// a key hashtable:
// first element is the value, all other are comments!

	/// the file name
	private File fn;

	/**
	 * Construct an IniFile object.
	 *
	 * @param _fn name of the used ini file
	 */
	public IniFile(final String _fn) {
		fn = new File(_fn);
// read the file
		read();
	}

	/**
	 * Read the file, parse it, and create an image in memory.
	 */
	public void read() {
		InputStream is = null;
		try {
			is = new FileInputStream(fn);
			int b1 = is.read();
			int b2 = is.read();
			if (b1 == 255 && b2 == 254) {
				readWide();
				return;
			}
		} catch (Exception ex) {
			/**/} finally {
			try {
				if (is != null)
					is.close();
			} catch (Throwable t) {
				//
			}
			is = null;
		}

		sections = new SingleMap<String, Serializable>();

		MultiMap<String, String> currentSection = new MultiMap<String, String>();
		String currentSectionName = "\1";
		String currentKey = "\1";

		try {
			is = new FileInputStream(fn);

			ByteRef buffer = new ByteRef();
			boolean update = null != buffer.update(is);
			while (buffer.length() > 0) {
				int ret = buffer.indexOf('\r');
				if (ret < 0) {
					if (update) {
						update = null != buffer.update(is);
						continue;
					}
					ret = buffer.length();
				}
				// no data
				if (ret == 0) {
					if (buffer.charAt(1) == '\n')
						++ret;
					buffer = buffer.substring(ret + 1);
					continue;
				}

				// ends with '\'
				if (buffer.charAt(ret - 1) == '\\') {
					ByteRef left = buffer.substring(0, ret - 1);
					if (buffer.charAt(ret + 1) == '\n')
						++ret;
					buffer = left.append(buffer.substring(ret + 1));
					continue;
				}

				// normalized line
				ByteRef line = buffer.substring(0, ret);
				if (buffer.charAt(ret + 1) == '\n')
					++ret;
				buffer = buffer.substring(ret + 1);

				line = line.trim();
				if (line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']') {
					sections.put(currentSectionName, currentSection);
					currentSectionName = line.substring(1, line.length() - 1).toString();
					currentSection = new MultiMap<String, String>();
					currentKey = "\1";
					continue;
				}
				int eq = line.indexOf('=');
				if (line.charAt(0) == ';' || eq < 0) {
					currentSection.put(currentKey + "\1", line.toString());
					continue;
				}
				String key = line.substring(0, eq).toString();
				String val = line.substring(eq + 1).toString();
				currentKey = key;
				currentSection.put(key, val);
			}

		} catch (Exception e) {//
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Throwable t) {//

			}
		}
	}

	private void readWide() {
		InputStream is = null;
		sections = new SingleMap<String, Serializable>();

		MultiMap<String, String> currentSection = new MultiMap<String, String>();
		String currentSectionName = "\1";
		String currentKey = "\1";

		try {
			is = new FileInputStream(fn);

			CharRef buffer = new CharRef();
			boolean update = null != buffer.update(is);
			while (buffer.length() > 0) {
				int ret = buffer.indexOf('\r');
				if (ret < 0) {
					if (update) {
						update = null != buffer.update(is);
						continue;
					}
					ret = buffer.length();
				}
				// no data
				if (ret == 0) {
					if (buffer.charAt(1) == '\n')
						++ret;
					buffer = buffer.substring(ret + 1);
					continue;
				}

				// ends with '\'
				if (buffer.charAt(ret - 1) == '\\') {
					CharRef left = buffer.substring(0, ret - 1);
					if (buffer.charAt(ret + 1) == '\n')
						++ret;
					buffer = left.append(buffer.substring(ret + 1));
					continue;
				}

				// normalized line
				CharRef line = buffer.substring(0, ret);
				if (buffer.charAt(ret + 1) == '\n')
					++ret;
				buffer = buffer.substring(ret + 1);

				line = line.trim();
				if (line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']') {
					sections.put(currentSectionName, currentSection);
					currentSectionName = line.substring(1, line.length() - 1).toString();
					currentSection = new MultiMap<String, String>();
					currentKey = "\1";
					continue;
				}
				int eq = line.indexOf('=');
				if (line.charAt(0) == ';' || eq < 0) {
					currentSection.put(currentKey + "\1", line.toString());
					continue;
				}
				String key = line.substring(0, eq).toString();
				String val = line.substring(eq + 1).toString();
				currentKey = key;
				currentSection.put(key, val);
			}

		} catch (Exception e) {//
		} finally {
			try {
				is.close();
			} catch (Throwable t) {
				//
			}
		}

	}

	/**
	 * Write all internal data to the file.
	 */
	public void flush() {
		File out = new File(fn + ".$$$");
		FileWriter fw = null;
		try {
			fw = new FileWriter(out);
			flush(fw);
// delete org rename new
			fn.delete();
			out.renameTo(fn);
		} catch (Exception e) {//
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e1) {//
			}
		}
	}

	/**
	 * Write all internal data to the file.
	 *
	 * @param w a writer to write data to
	 */
	@SuppressWarnings("unchecked")
	private void flush(final Writer w) throws IOException {
		BufferedWriter bw = new BufferedWriter(w, 0x4000);
		try {
			for (Iterator<String> e = sections.keySet().iterator(); e.hasNext();) {
				String section = e.next();
				Vector<?> sec = (Vector<?>) sections.get(section);
				bw.newLine();
				for (int i = 1; i < sec.size(); ++i) {
					bw.write((String) sec.elementAt(i));
					bw.newLine();
				}
				bw.write("[" + section + "]");
				bw.newLine();
				SingleMap<String, ?> ht = (SingleMap<String, ?>) sec.elementAt(0);
				for (Iterator<String> f = ht.keySet().iterator(); f.hasNext();) {
					String key = f.next();
					Vector<?> v = (Vector<?>) ht.get(key);
					for (int i = 1; i < v.size(); ++i) {
						bw.write((String) v.elementAt(i));
						bw.newLine();
					}
					bw.write(key + "=" + (String) v.elementAt(0));
					bw.newLine();
				}
			}
		} finally {
			bw.close();
		}
	}

	/**
	 * Get all sections for the given ini file.
	 *
	 * @return a vector of String with all section names.
	 */
	public Vector<String> getSections() {
		Vector<String> v = new Vector<String>(sections.size());
		for (Iterator<String> e = sections.keySet().iterator(); e.hasNext();) {
			String s = e.next();
			if (s.equals("\1"))
				continue;
			v.addElement(s);
		}
		return v;
	}

	/**
	 * Get all sections for the given ini file, matching the path.
	 *
	 * @param path the match String
	 * @return a vector of String with all section names
	 */
	public Vector<String> getSections(String path) {
		path = path + '/';
		int plen = path.length();
		Vector<String> v = new Vector<String>(sections.size());
		for (Iterator<String> e = sections.keySet().iterator(); e.hasNext();) {
			String s = e.next();
			if (s.length() >= plen && s.startsWith(path) && s.indexOf('/', plen + 1) == -1)
				v.addElement(s);
		}
		return v;
	}

	/**
	 * Get all keys for a section in the given ini file.
	 *
	 * @param section the name of the section
	 * @return a vector of String with all key names
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getKeys(final String section) {
		MultiMap<String, ?> sec = (MultiMap<String, ?>) sections.get(section);
		if (sec == null)
			return new Vector<String>();
		Vector<String> v = new Vector<String>(sections.size());
		for (Iterator<String> e = sec.keySet().iterator(); e.hasNext();) {
			String key = e.next();
			if (key.equals("\1"))
				continue;
			v.addElement(key);
		}
		return v;
	}

	/**
	 * Get a value for a keys in the section for the given ini file.
	 *
	 * @param section the name of the section
	 * @param key     the name of the key
	 * @param def     a default value, if the key was not found
	 * @return a String with the keys value
	 * @see #setString
	 */
	@SuppressWarnings("unchecked")
	public String getString(final String section, final String key, final String def) {
		try {
			MultiMap<String, ?> sec = (MultiMap<String, String>) sections.get(section);
			if (sec == null)
				return def;
			return (String) sec.get(key);
		} catch (Exception e) {
		}
		return def;
	}

	/**
	 * Set a value for a keys in the section for the given ini file. If the section
	 * not exists, it is created. If the key not exists, it is created. Otherwise
	 * the keys value is replaced. If value == null the key is deleted. If key ==
	 * null the section is deleted!
	 *
	 * @param section the name of the section
	 * @param key     the name of the key
	 * @param value   the new key value
	 * @see #getString
	 */
	@SuppressWarnings("unchecked")
	public void setString(final String section, final String key, final String value) {
		if (key == null) {
			sections.remove(section);
			return;
		}
		Vector<SingleMap<String, Object>> sec = (Vector<SingleMap<String, Object>>) sections.get(section);
		if (sec == null) {
			if (value == null || key == null)
				return;
			sec = new Vector<SingleMap<String, Object>>();
			sec.addElement(new SingleMap<String, Object>());
			sections.put(section, sec);
		}
		SingleMap<String, Object> ht = (SingleMap<String, Object>) sec.elementAt(0);
		if (value == null) {
			ht.remove(key);
			return;
		}
		Vector<String> v = (Vector<String>) ht.get(key);
		if (v == null)
			v = new Vector<String>();
		else
			v.removeElementAt(0);

		v.insertElementAt(value, 0);
		ht.put(key, v);
	}
}
