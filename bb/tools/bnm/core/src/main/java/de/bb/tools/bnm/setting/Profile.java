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

import java.util.ArrayList;
import java.util.Map;

import de.bb.tools.bnm.model.Activation;
import de.bb.tools.bnm.model.Repository;

public class Profile {
  /**
   * The conditional logic which will automatically
   * trigger the inclusion of this profile.
   */
  public Activation activation;
  /**
   * Extended configuration specific to this profile goes here.
   * Contents take the form of
   * <property.name>property.value</property.name>
   */
  public Map<String,String> properties;
  /**
   * The lists of the remote repositories.
   */
  public ArrayList<Repository> repositories;
  /**
   * The lists of the remote repositories for discovering plugins.
   */
  public ArrayList<Repository> pluginRepositories;
  public String id;
}
