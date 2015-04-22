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

public class ActivationOS {
  /** 
   * The name of the operating system to be used to activate the profile. This must be an exact match
   * of the <code>${os.name}</code> Java property, such as <code>Windows XP</code>.
   */
  public String name;
  /** 
   * The general family of the OS to be used to activate the profile, such as <code>windows</code> or <code>unix</code>.
   */
  public String family;
  /** 
   * The architecture of the operating system to be used to activate the profile.
   */
  public String arch;
  /** 
   * The version of the operating system to be used to activate the profile.
   */
  public String version;
}
