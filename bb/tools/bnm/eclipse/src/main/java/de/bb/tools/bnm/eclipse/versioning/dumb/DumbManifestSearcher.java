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
import java.io.IOException;

public class DumbManifestSearcher extends DumbSearcher {
	public DumbManifestSearcher(String content) {
		super(content);
	}

	public DumbManifestSearcher(File f) throws IOException {
		super(f);
	}

	public Pos search(String key) {
		if (!key.endsWith(":"))
			key += ":";
		for (int offset = content.indexOf(key); offset >= 0; offset = content
				.indexOf(key, offset + 1)) {
			// check for CRLF
			if (offset > 0) {
				char ch = content.charAt(offset - 1);
				if (ch != 0xd && ch != 0xa)
					continue;
			}
			offset += key.length();
			if (offset == content.length())
				return null;
			if (content.charAt(offset) != ' ')
				continue;

			// check next lines
			int end = offset;
			for (;;) {
			  int r1 = content.indexOf('\r', end + 1);
			  int r2 = content.indexOf('\n', end + 1);
				end = r1 < 0 ? r2 : r2 < 0 ? r1 : r1 < r2 ? r2 : r1;
				if (end < 0)
					return null;
				end += 1;
				if (end < content.length() && content.charAt(end) == ' ') {
					continue;
				}
				return new Pos(offset, end - offset);
			}
		}
		return null;
	}
	
}
