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

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.eclipse.versioning.dumb.PomInfo;

public class ManageVersionRefactoring extends VersionRefactoring {

    /**
     * This refactoring is showing all versions (not only module tree local
     * ones). This allows an easy management of all used versions.
     * 
     * @param currentProject
     */
    public ManageVersionRefactoring(IProject currentProject) {
        super("");
        this.currentProject = currentProject;
    }

    
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        try {
            // collect the data
            IResourceVisitor visitor = new IResourceVisitor() {
                public boolean visit(IResource resource) throws CoreException {
                    if (resource instanceof IFolder)
                        return true;
                    IPath loc = resource.getLocation();
                    if ("pom.xml".equals(loc.lastSegment()))
                        pomList.add(loc);
                    return true;
                }
            };

            pm.beginTask("scanning pom.xml and MANIFEST.MF", 2);

            currentProject.accept(visitor);
            pm.worked(1);

            SubProgressMonitor spm = new SubProgressMonitor(pm, 1);
            spm.beginTask("reading content", pomList.size());

            // map ids to the first occured index.
            HashMap<String, Integer> id2Index = new HashMap<String, Integer>();
            HashMap<String, VI> entries = new HashMap<String, VI>();

            for (IPath pomLoc : pomList) {
                PomInfo pInfo;
                try {
                    pInfo = new PomInfo(pomLoc.toFile());
                } catch (IOException ioe) {
                    continue;
                }
                pom2Loc.put(pInfo, pomLoc);
                id2Pom.put(pInfo.getId(), pInfo);
                String bundleId = pInfo.getBundleId();
                if (bundleId != null) {
                    id2Pom.put(bundleId, pInfo);
                }

                for (String id : pInfo.getReferences()) {
                    int colon = id.lastIndexOf(':');
                    String ga = id.substring(0, colon);
                    String ver = id.substring(colon + 1);

                    // collect the data inside the array list
                    VI vi = entries.get(ga);
                    if (vi == null) {
                        vi = new VI();
                        vi.id = PomInfo.toId(id);
                        vi.origVersion = vi.version = ver;
                        if (vi.id.equals(pInfo.getId())) {
                            vi.bundleId = pInfo.getBundleId();
                            vi.origBundleVersion = vi.bundleVersion = pInfo
                                    .getBundleVersion();
                        }
                        entries.put(ga, vi);
                        data.add(vi);
                    }

                    vi.addVersion(ver);
                }
                spm.worked(1);
            }
            spm.done();

            return OK;
        } catch (CoreException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
                    e.getMessage()));
        }
    }

    
    public String getName() {
        return "bnm version refactoring";
    }

}
