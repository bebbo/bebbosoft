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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.eclipse.versioning.dumb.PomInfo;

public class SnapshotVersionRefactoring extends PromoteVersionRefactoring {

  private IFile currentFile;

  public SnapshotVersionRefactoring(IFile currentFile) {
    super(currentFile.getProject());
    this.currentFile = currentFile;
    promoteToSnapshot = true;
  }

  
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {

    try {
      PomInfo pi = new PomInfo(currentFile.getLocation().toFile());
      // find the change
      if (pi.getVersion().endsWith("SNAPSHOT"))
        return OK;

      String id = pi.getId();
      RefactoringStatus r = super.checkInitialConditions(pm);
      for (VI vi : data) {
        if (id.equals(vi.id)) {
          if (!vi.version.endsWith("-SNAPSHOT"))
            vi.version = Util.nextSnapshots(vi.version).get(0);

          if (vi.bundleVersion != null )
            vi.bundleVersion = Util.toOsgiVersion(vi.version);
        }
      }
      return r;
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
          e.getMessage()));
    }
  }
}
