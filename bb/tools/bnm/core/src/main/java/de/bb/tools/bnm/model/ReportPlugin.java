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

import java.util.ArrayList;
import java.util.Map;

public class ReportPlugin extends Id {
  /** 
   * Whether the configuration in this plugin should be made available to projects that
   * inherit from this one.
   */
  public String inherited;
  /** 
   * The configuration of the reporting plugin.
   */
  public Map<String, Object> configuration;
  /** 
   * Multiple specifications of a set of reports, each having (possibly) different
   * configuration. This is the reporting parallel to an <code>execution</code> in the build.
   */
  public ArrayList<ReportSet> reportSets;
}