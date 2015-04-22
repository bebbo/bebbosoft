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

import java.util.HashMap;
import java.util.Map;

public class Notifier {
  /** 
   * The mechanism used to deliver notifications.
   */
  public String type;
  /** 
   * Whether to send notifications on error.
   */
  public boolean sendOnError;
  /** 
   * Whether to send notifications on failure.
   */
  public boolean sendOnFailure;
  /** 
   * Whether to send notifications on success.
   */
  public boolean sendOnSuccess;
  /** 
   * Whether to send notifications on warning.
   */
  public boolean sendOnWarning;
  /** 
   * <b>Deprecated</b>. Where to send the notification to - eg email address.
   * /
  public String address;
  /** 
   * Extended configuration specific to this notifier goes here.
   */
  public Map<String, Object> configuration;
}
