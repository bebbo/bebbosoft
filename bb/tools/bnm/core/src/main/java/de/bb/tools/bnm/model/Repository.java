/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

// generated by Xsd2Class
package de.bb.tools.bnm.model;

import de.bb.tools.bnm.setting.RepositoryPolicy;

public class Repository {
  /**
   * How to handle downloading of releases from this repository
   */
  public RepositoryPolicy releases;
  /**
   * How to handle downloading of snapshots from this repository
   */
  public RepositoryPolicy snapshots;
  /**
   * A unique identifier for a repository.
   */
  public String id;
  /**
   * Human readable name of the repository.
   */
  public String name;
  /**
   * The url of the repository.
   */
  public String url;
  /**
   * The type of layout this repository uses for locating and
   * storing artifacts - can be "legacy" or "default".
   */
  public String layout;
  
  public String toString() {
	  return id + "," + name + " @ " + url;
  }
}
