/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.versioning.dumb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class DumbSearcher {
	protected String content;

	public DumbSearcher(String content) {
		this.content = content;
	}

	public DumbSearcher(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		int l = fis.available();
		byte d[] = new byte[l];
		fis.read(d);
		fis.close();
		this.content = new String(d, 0, 0, l);
	}

	public String getContent(Pos pos) {
		if (pos == null)
			return null;
		return content.substring(pos.getOffset(), pos.getEnd());
	}
}
