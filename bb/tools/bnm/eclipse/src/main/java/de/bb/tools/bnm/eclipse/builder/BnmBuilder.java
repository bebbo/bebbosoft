/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.bb.tools.bnm.eclipse.Plugin;

/**
 * The BnmBuilder the changes of all resources inside a project to
 * - mark dirty modules (if a resource changed)
 * - mark if BNM needs reload (if pom.xml modified)
 * That information is then used while building opened projects.
 * @author sfranke
 */
public class BnmBuilder extends IncrementalProjectBuilder {
  public static final String BUILDER_ID = "de.bb.tools.bnm.eclipse.bnmBuilder";

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    System.out.println("bnm: build" + project);
    Tracker tracker = Plugin.getTracker();
    tracker.buildBnmProject(project, getDelta(project));
    return null;
  }

  
  protected void clean(IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    System.out.println("bnm: clean " + getProject());
    Tracker tracker = Plugin.getTracker();
    tracker.clean(project);
  }

}
