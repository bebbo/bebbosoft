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

import de.bb.tools.bnm.model.Exclusion;
import de.bb.tools.bnm.model.Id;

public class Dependency extends Id {
  /** 
   * The type of dependency. This defaults to <code>jar</code>. While it usually represents the extension on
   * the filename of the dependency, that is not always the case. A type can be HashMapped to a different
   * extension and a classifier.
   * The type often corresponds to the packaging used, though this is also not always the case.
   * Some examples are <code>jar</code>, <code>war</code>, <code>ejb-client</code> and <code>test-jar</code>.
   * New types can be defined by plugins that set
   * <code>extensions</code> to <code>true</code>, so this is not a complete list.
   */
  public String type;
  /** 
   * The classifier of the dependency. This allows distinguishing two artifacts that belong to the same POM but
   * were built differently, and is appended to the filename after the version. For example,
   * <code>jdk14</code> and <code>jdk15</code>.
   */
  public String classifier;
  /** 
   * The scope of the dependency - <code>compile</code>, <code>runtime</code>, <code>test</code>,
   * <code>system</code>, and <code>provided</code>. Used to
   * calculate the various classpaths used for compilation, testing, and so on. It also assists in determining
   * which artifacts to include in a distribution of this project. For more information, see
   * <a href="http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">the
   * dependency mechanism</a>.
   */
  public String scope = "compile";
  /** 
   * FOR SYSTEM SCOPE ONLY. Note that use of this property is <b>discouraged</b> and may be replaced in later
   * versions. This specifies the path on the file system for this dependency.
   * Requires an absolute path for the value, not relative.
   * Use a property that gives the machine specific absolute path,
   * e.g. <code>${java.home}</code>.
   */
  public String systemPath;
  /** 
   * Lists a set of artifacts that should be excluded from this dependency's artifact list when it comes to
   * calculating transitive dependencies.
   */
  public ArrayList<Exclusion> exclusions;
  /** 
   * Indicates the dependency is optional for use of this library. While the version of the dependency will be
   * taken into account for dependency calculation of the library is used elsewhere, it will not be passed on
   * transitively.
   */
  public boolean optional;
  
  
  public int hashCode() {
      int hc = super.hashCode();
      if (classifier != null) hc ^= classifier.hashCode();
      return hc;
  }

  public boolean equals(Object oo) {
      if (!(oo instanceof Dependency))
          return false;
      Dependency o = (Dependency) oo;
      if (!super.equals(o))
          return false;
      if (classifier == null) return null == o.classifier;
      return classifier.equals(o.classifier);
  }

}