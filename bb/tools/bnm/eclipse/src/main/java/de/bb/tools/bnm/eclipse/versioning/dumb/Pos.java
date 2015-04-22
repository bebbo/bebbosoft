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

public class Pos {
	private int offset;
	private int length;

	public Pos(Pos parent, Pos child) {
		this.offset = parent.offset + child.offset;
		this.length = child.length;
	}

	public Pos(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	public String toString() {
		return offset + ":" + length;
	}

	public int getOffset() {
		return offset;
	}

	public int getEnd() {
		return offset + length;
	}

  public int getLength() {
    return length;
  }
}
