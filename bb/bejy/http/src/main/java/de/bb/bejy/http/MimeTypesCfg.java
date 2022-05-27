/*****************************************************************************
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
package de.bb.bejy.http;

import java.util.HashMap;
import java.util.Iterator;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class MimeTypesCfg extends Configurable implements Configurator {
	private final static String PROPERTIES[][] = {};

	public MimeTypesCfg() {
		init("mime types", PROPERTIES);
		Config.addGlobalUnique(getPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getName()
	 */
	public String getName() {
		return "mime types";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getDescription()
	 */
	public String getDescription() {
		return "this is a set of mime type definitions";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getPath()
	 */
	public String getPath() {
		return "mime-types";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getId()
	 */
	public String getId() {
		return "de.bb.bejy.http.mime-types";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getExtensionId()
	 */
	public String getExtensionId() {
		return "de.bb.bejy";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#getRequired()
	 */
	public String getRequired() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#create()
	 */
	public de.bb.bejy.Configurable create() {
		return new MimeTypesCfg();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurator#loadClass()
	 */
	public boolean loadClass() {
		return false;
	}

	private static HashMap<String, String> mimes = new HashMap<String, String>();

	/**
	 * @param extension
	 * @return
	 */
	public static String getMimeType(String extension) {
		if (extension == null)
			return null;
		if (mimes.isEmpty()) {
			Configurable c = Config.getInstance().getChild("mime-types");
			if (c != null) {
				for (final Iterator<Configurable> i = c.children(); i.hasNext();) {
					final Configurable m = i.next();
					if (extension.equals(m.getProperty("extension"))) {
						final String mt = m.getProperty("type");
						if (mt != null) {
							mimes.put(extension, mt);
						}
						return mt;
					}
				}
			}
		}
		final String mt = mimes.get(extension);
		return mt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
	 */
	public void update(LogFile logFile) throws Exception {
		Configurable parent = getParent();
		if (parent != Config.getInstance())
			parent.update(logFile);
		else
			mimes.clear();
	}

}
