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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.bb.tools.bnm.eclipse.Plugin;
/**
 * The idea of this builder:
 * - first let bnm build everything required
 * - then pass to the Java Builder and have eclipse build the project itself 
 * @author stefan franke
 */
public class BeforeJavaBuilder extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = "de.bb.tools.bnm.eclipse.BeforeJavaBuilder";

  
  protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
    System.out.println("before " + getProject() + " " + kind);
    Tracker tracker = Plugin.getTracker();
    IProject project = this.getProject();
	String master = (String)args.get("master");
	tracker.buildUntil(project, master);
	tracker.buildCurrent(project, master);
	project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return null;
  }

  
  protected void clean(IProgressMonitor monitor) throws CoreException {
    Tracker tracker = Plugin.getTracker();
    tracker.cleanSlave(getProject(), (String)getCommand().getArguments().get("master"));
  }
}
