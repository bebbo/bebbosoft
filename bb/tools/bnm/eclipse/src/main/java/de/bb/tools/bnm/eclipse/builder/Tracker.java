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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;

import de.bb.tools.bnm.eclipse.Plugin;

public class Tracker implements IResourceChangeListener {

    private static final HashMap<String, BnmProject> PROJECTS = new HashMap<String, BnmProject>();

    public Tracker() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        for (IProject p : projects) {
            // init open projects only
            if (!p.isOpen())
                continue;
            addProject(p);
        }
    }

    private boolean addProject(IProject p) {
        // only BNM nature
        try {
            if (p.getNature(BnmNature.NATURE_ID) == null)
                return false;
            IProjectDescription pd = p.getDescription();
            ICommand[] bs = pd.getBuildSpec();
            for (ICommand c : bs) {
                if (BnmBuilder.BUILDER_ID.equals(c.getBuilderName())) {
                    addBnmProject(p);
                    break;
                }
                if (BeforeJavaBuilder.ID.equals(c.getBuilderName())) {
                    return addSlaveProject(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addBnmProject(IProject p) throws Exception {
        String name = p.getName();
        BnmProject bp = PROJECTS.get(name);
        if (bp != null)
            return false;

        bp = new BnmProject(p);
        PROJECTS.put(name, bp);
        return true;
    }

    private boolean addSlaveProject(IProject p) throws CoreException {
        IProjectDescription d = p.getDescription();
        ICommand[] bs = d.getBuildSpec();
        for (ICommand c : bs) {
            Map<String, String> m = uncheckedCast(c.getArguments());
            if (m == null)
                continue;
            String name = m.get("master");
            if (name != null) {
                BnmProject bp = PROJECTS.get(name);
                if (bp != null) {
                    return bp.addSlave(p);
                }
            }
        }
        return false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, String> uncheckedCast(Map map) {
        return map;
    }

    private static boolean removeSlaveProject(IProject p) throws CoreException {
        for (BnmProject bp : PROJECTS.values()) {
            if (bp.removeSlave(p))
                return true;
        }
        return false;
    }

    public void resourceChanged(IResourceChangeEvent event) {
        // System.err.println(event.getSource() + ": " + event.getDelta() + ", "
        // + event.getBuildKind() + ", " + event.getType());
        boolean modified = false;
        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;
        IResourceDelta[] ch = delta.getAffectedChildren();
        for (IResourceDelta rd : ch) {
            if (!(rd.getResource() instanceof IProject)) {
                // track which projects are dirty
                continue;
            }
            try {
                IProject p = (IProject) rd.getResource();
                int kind = rd.getKind();
                if (kind == IResourceDelta.ADDED) {
                    modified |= addProject(p);
                } else if (kind == IResourceDelta.REMOVED) {
                    String name = p.getName();
                    if (PROJECTS.containsKey(name)) {
                        PROJECTS.remove(name);
                    } else {
                        modified |= removeSlaveProject(p);
                    }
                } else if (kind == IResourceDelta.CHANGED) {
                    IResource r = rd.getResource();
                    if (r.isAccessible()) {
                        modified |= addSlaveProject(p);
                    } else {
                        modified |= removeSlaveProject(p);
                    }
                }
            } catch (Exception ex) {
            }
        }

        // update project/jar build paths
        if (modified) {
            Job job = new Job("update BNM projects") {

                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        updateAllClassPaths(monitor);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Status.CANCEL_STATUS;
                    }
                    return Status.OK_STATUS;
                }

            };
            job.schedule();
        }
    }

    public static IClasspathEntry[] updateClasspath(IClasspathEntry[] cpes) throws CoreException {
        IWorkspaceRoot wproot = ResourcesPlugin.getWorkspace().getRoot();
        boolean changed = false;
        for (int i = 0; i < cpes.length; ++i) {
            IClasspathEntry cpe = cpes[i];
            int kind = cpe.getEntryKind();
            if (kind == IClasspathEntry.CPE_LIBRARY) {
                IPath path = cpe.getPath();
                for (BnmProject bp : PROJECTS.values()) {
                    String pname = path.toString();
                    String dev = path.getDevice();
                    if (dev != null) {
                        pname = pname.substring(0, dev.length()).toLowerCase() + pname.substring(dev.length());
                    }
                    String name = bp.getNameForJar(pname);
                    if (name != null) {
                        IProject localProject = wproot.getProject(name);
                        if (localProject != null && localProject.isAccessible()) {
                            cpe = JavaCore.newProjectEntry(localProject.getFullPath());
                            cpes[i] = cpe;
                            changed = true;
                            break;
                        }
                    }
                }
            } else if (kind == IClasspathEntry.CPE_PROJECT) {
                String name = cpe.getPath().toString();
                if (name.startsWith("/"))
                    name = name.substring(1);
                IProject localProject = wproot.getProject(name);
                if (localProject == null || !localProject.isAccessible()) {
                    for (BnmProject bp : PROJECTS.values()) {
                        String jar = bp.getJarForName(name);
                        if (jar != null) {
                            cpe = JavaCore.newLibraryEntry(new Path(jar), null, null);
                            cpes[i] = cpe;
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!changed) {
            return null;
        }
        return cpes;
    }

    public static void invalidate(IProject project) {
        BnmProject bp = PROJECTS.get(project.getName());
        if (bp != null)
            bp.invalidate();
    }

    /**
     * ensure the bnm instances are up to date.
     * 
     * @throws Exception
     */
    public static void validateAll() throws Exception {
        boolean reset = false;
        for (BnmProject bp : PROJECTS.values()) {
            if (bp.hasBnm())
                continue;
            reset = true;
            break;
        }
        if (!reset)
            return;

        Plugin.newLoader();
        for (BnmProject bp : PROJECTS.values()) {
            bp.reInit();
        }
    }

    /**
     * Uses the bnm builder to build all dirty modules and its dependent modules until this module is built. Then it
     * stops to pass the building to Eclipse.
     * 
     * @param project
     * @param master
     */
    public void buildUntil(IProject project, String master) {
        BnmProject bp = PROJECTS.get(master);
        if (bp == null)
            return;
        bp.buildUntil(project);
    }

    /**
     * Uses the bnm builder to perform the all steps in "install" after the compile:compile step.
     * 
     * @param project
     */
    public void buildAfter(IProject project, String master) {
        BnmProject bp = PROJECTS.get(master);
        if (bp == null)
            return;
        bp.buildAfter(project);
    }

    /**
     * This method only marks the modules with changes. TODO currently everything is marked to build The changes are
     * used in buildUntilProject.
     * 
     * @param project
     * @param delta
     */
    public void buildBnmProject(IProject project, IResourceDelta delta) {
        BnmProject bp = PROJECTS.get(project.getName());
        if (bp == null)
            return;

        final ArrayList<IFile> files = new ArrayList<IFile>();
        collect(delta, files);
        if (files.isEmpty())
            return;

        bp.markDirtySlaves(delta);
    }

    private void collect(final IResourceDelta delta, final ArrayList<IFile> files) {
        if (delta == null)
            return;
        final IResource r = delta.getResource();
        if (r instanceof IFile) {
            files.add((IFile) r);
        } else if (r instanceof IContainer) {
            if (r.getName().equals("target"))
                return;
            for (final IResourceDelta rd : delta.getAffectedChildren()) {
                collect(rd, files);
            }
        }
    }

    public void clean(IProject project) {
        BnmProject bp = PROJECTS.get(project.getName());
        if (bp == null)
            return;
        bp.clean();
    }

    public void cleanSlave(IProject project, String master) {
        BnmProject bp = PROJECTS.get(master);
        if (bp == null)
            return;
        bp.cleanSlave(project);
    }

    public static void updateAllClassPaths(IProgressMonitor monitor) throws JavaModelException, CoreException {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        monitor.beginTask("updating project", projects.length);
        for (IProject p : projects) {
            if (isSlaveProject(p)) {
                // it's a BNM managed project.
                IJavaProject jp = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(p);
                IClasspathEntry[] cpes = jp.getRawClasspath();

                for (IClasspathEntry cpe : cpes) {
                    if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER
                            && CcpContainer.ID.equals(cpe.getPath().toString())) {
                        // TODO check whether the project is affected

                        jp.setRawClasspath(cpes, new NullProgressMonitor());
                        break;
                    }
                }
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    public static boolean isSlaveProject(IProject p) {
        try {
            ICommand[] cmds = p.getDescription().getBuildSpec();
            for (ICommand cmd : cmds) {
                if (cmd.getBuilderName().equals(BeforeJavaBuilder.ID)) {
                    return true;
                }
            }
        } catch (CoreException e) {
        }
        return false;
    }
}
