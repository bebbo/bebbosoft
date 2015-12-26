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
package de.bb.tools.bnm.eclipse.versioning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ReleaseVersionRefactoring extends PromoteVersionRefactoring {

  public ReleaseVersionRefactoring(IProject currentProject) {
    super(currentProject);
    promoteToSnapshot = false;
  }

  
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    RefactoringStatus r = super.checkInitialConditions(pm);
    
    // find all changes
    for (VI vi : data) {
      String orgV = vi.origVersion;
      if (orgV != null && orgV.endsWith("-SNAPSHOT"))
        vi.version = orgV.substring(0, orgV.length() - 9);
      
      orgV = vi.origBundleVersion;
      if (orgV != null && orgV.endsWith(".SNAPSHOT"))
        vi.bundleVersion = orgV.substring(0, orgV.length() - 9);      
    }
    
    return r;
  }

  

  
  protected void extendManifestChanges(SortedMap<String, String> maniDeps,
      HashSet<String> modifiedIds, HashMap<String, String> id2newVersion,
      HashSet<String> touched, String snapshot) {
    // nada - all versions are already modified
  }
  
}
