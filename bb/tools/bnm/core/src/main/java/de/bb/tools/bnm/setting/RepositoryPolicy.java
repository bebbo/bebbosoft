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
package de.bb.tools.bnm.setting;

public class RepositoryPolicy {
  /**
   * Whether to use this repository for downloading this type of
   * artifact.
   */
  public boolean enabled;
  /**
   * The frequency for downloading updates - can be "always",
   * "daily" (default), "interval:XXX" (in minutes) or "never"
   * (only if it doesn?t exist locally).
   */
  public String updatePolicy;
  /**
   * What to do when verification of an artifact checksum fails -
   * warn, fail, etc. Valid values are "fail" or "warn".
   */
  public String checksumPolicy;
}
