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

public class Scm {
  /** 
   * The source control management system URL
   * that describes the repository and how to connect to the
   * repository. For more information, see the
   * <a href="http://maven.apache.org/scm/scm-url-format.html">URL format</a>
   * and <a href="http://maven.apache.org/scm/scms-overview.html">list of supported SCMs</a>.
   * This connection is read-only.
   */
  public String connection;
  /** 
   * Just like <code>connection</code>, but for developers, i.e. this scm connection
   * will not be read only.
   */
  public String developerConnection;
  /** 
   * The tag of current code. By default, it?s set to HEAD during development.
   */
  public String tag;
  /** 
   * The URL to the project?s browsable SCM repository, such as ViewVC or Fisheye.
   */
  public String url;
}
