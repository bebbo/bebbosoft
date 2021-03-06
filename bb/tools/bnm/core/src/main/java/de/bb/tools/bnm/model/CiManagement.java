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

import de.bb.tools.bnm.model.Notifier;

public class CiManagement {
  /** 
   * The name of the continuous integration system, e.g. <code>continuum</code>.
   */
  public String system;
  /** 
   * URL for the continuous integration system used by the project if it has a web interface.
   */
  public String url;
  /** 
   * Configuration for notifying developers/users when a build is
   * unsuccessful, including user information and notification mode.
   */
  public ArrayList<Notifier> notifiers;
    
}
