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

public class NameVersion {

	private String name;
	private Pos namePos;
	private String version;
	private Pos versionPos;

	public NameVersion(String name, Pos namePos, String version, Pos versionPos) {
		this.name = name;
		this.namePos = namePos;
		this.version = version;
		this.versionPos = versionPos;
	}

	public String getName() {
		return name;
	}

	public Pos getNamePos() {
		return namePos;
	}

	public String getVersion() {
		return version;
	}

	public Pos getVersionPos() {
		return versionPos;
	}
	
}
