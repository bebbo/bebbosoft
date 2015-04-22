/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.resources-plugin.
 *
 *   de.bb.tools.bnm.plugin.resources-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.resources-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.resources-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.resources;


import java.io.File;
import java.util.List;

import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.model.Resource;

/**
 * Copy resources of the configured plugin attribute resources
 */
public class CopyResourcesPlugin
    extends ResourcesPlugin
{
  /**
   * The output directory into which to copy the resources.
   *
   * @parameter 
   * @required
   */
  @Config("outputDirectory")
  private File outputDirectory;

  /**
   * The list of resources we want to transfer.
   *
   * @parameter 
   * @required
   */
  @Config("resources")
  private List<Resource> resources;

  @Config("filters")
  private List<String> filters;

  @Override
  public void execute() throws Exception {
    copyResources(resources, filters, outputDirectory);
  }

  
}
