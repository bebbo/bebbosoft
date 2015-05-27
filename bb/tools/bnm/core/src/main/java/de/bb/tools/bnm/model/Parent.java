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

import de.bb.tools.bnm.model.Id;

public class Parent extends Id {
  /** 
   * The relative path of the parent <code>pom.xml</code> file within the check out.
   * The default value is <code>../pom.xml</code>.
   * Maven looks for the parent pom first in the reactor of currently building projects, then in this location on
   * the filesystem, then the local repository, and lastly in the remote repo.
   * <code>relativePath</code> allows you to select a different location,
   * for example when your structure is flat, or deeper without an intermediate parent pom.
   * However, the group ID, artifact ID and version are still required,
   * and must match the file in the location given or it will revert to the repository for the POM.
   * This feature is only for enhancing the development in a local checkout of that project.
   */
  public String relativePath;
}