/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.clean-plugin.
 *
 *   de.bb.tools.bnm.plugin.clean-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.clean-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.clean-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.clean;

import java.util.ArrayList;

public class FileSet {
  /** 
   * Describe the directory where the resources are stored.
   * The path is relative to the POM.
   */
  public String directory;
  /** 
   * A list of patterns to include, e.g. <code>**&#47;*.xml</code>.
   */
  public ArrayList<String> includes = new ArrayList<String>();
  /** 
   * A list of patterns to exclude, e.g. <code>**&#47;*.xml</code>
   */
  public ArrayList<String> excludes = new ArrayList<String>();

  /**
   * ...
   */
  public boolean followSymlinks;
  
  public String toString() {
    return directory + " +" + includes + " -" + excludes;
  }
}
