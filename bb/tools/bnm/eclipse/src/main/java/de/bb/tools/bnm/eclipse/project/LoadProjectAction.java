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
package de.bb.tools.bnm.eclipse.project;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.ide.undo.ProjectDescription;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.eclipse.PomAction;
import de.bb.tools.bnm.eclipse.builder.AfterJavaBuilder;
import de.bb.tools.bnm.eclipse.builder.BeforeJavaBuilder;
import de.bb.tools.bnm.eclipse.builder.BnmBuilder;
import de.bb.tools.bnm.eclipse.builder.BnmNature;
import de.bb.tools.bnm.eclipse.builder.CcpContainer;
import de.bb.tools.bnm.eclipse.builder.Tracker;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.model.Resource;

public class LoadProjectAction extends PomAction {

	protected static final IClasspathEntry[] NOENTRIES = {};
	protected static final IPath[] NOPATH = {};

	protected Exception exe;

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (currentFile == null)
			return;

		final IFile pomFile = currentFile;
		WorkspaceModifyOperation wmo = new WorkspaceModifyOperation() {

			protected void execute(IProgressMonitor monitor)
					throws CoreException, InvocationTargetException, InterruptedException {
				try {

					SubMonitor subM = SubMonitor.convert(monitor,
							"updating project for " + pomFile.getParent().getName(), 6);

					Tracker.validateAll();

					IProject theProject = pomFile.getProject();
					IPath loc = pomFile.getLocation().removeLastSegments(1);
					File path = loc.toFile();

					File bp = new File(path, "build.properties");
					if (!bp.exists()) {
						FileOutputStream fos = new FileOutputStream(bp);
						fos.write("custom=true\r\n".getBytes());
						fos.close();
					}

					Bnm bnm = new Bnm(Plugin.getLoader());
					bnm.loadFirst(path);

					ArrayList<Pom> poms = bnm.getProjectsInOrder();
					if (poms.size() == 1) {

//            new File(path, ".classpath").delete();
//            new File(path, ".project").delete();
						try {
							theProject.refreshLocal(IResource.DEPTH_ONE, subM.newChild(1));
						} catch (Exception e0) {
						}

						Pom pom = poms.get(0);
						Project proj = pom.getEffectivePom();
						String name = proj.groupId + "." + proj.artifactId;
						IWorkspace workspace = ResourcesPlugin.getWorkspace();

						final IProjectDescription description = workspace.newProjectDescription(name);
						description.setLocation(loc);
						boolean hasManifest = new File(path, "META-INF/MANIFEST.MF").exists();
						String[] natures = new String[hasManifest ? 3 : 2];
						int i = 0;
						if (hasManifest)
							natures[i++] = PDE.PLUGIN_NATURE;
						natures[i++] = JavaCore.NATURE_ID;
						natures[i] = BnmNature.NATURE_ID;
						description.setNatureIds(natures);

						HashMap<String, String> args = new HashMap<String, String>();
						args.put("master", Tracker.getMaster(theProject).getName());

						ICommand[] buildSpec = new ICommand[hasManifest ? 5 : 4];
						i = 0;
						ICommand c = description.newCommand();
						if (hasManifest) {
							c.setBuilderName(PDE.MANIFEST_BUILDER_ID);
							buildSpec[i++] = c;
							c = description.newCommand();
						}
						c.setArguments(args);
						c.setBuilderName(BeforeJavaBuilder.BUILDER_ID);
						buildSpec[i++] = c;
						c = description.newCommand();
						c.setArguments(args);
						c.setBuilderName(BnmBuilder.BUILDER_ID);
						buildSpec[i++] = c;
						c = description.newCommand();

						c.setBuilderName(JavaCore.BUILDER_ID);
						buildSpec[i++] = c;
						c = description.newCommand();
						c.setArguments(args);
						c.setBuilderName(AfterJavaBuilder.BUILDER_ID);
						buildSpec[i++] = c;
						description.setBuildSpec(buildSpec);

						IResource pd = new ProjectDescription(description).createResource(subM.newChild(1));
						{
							IProject p = (IProject) pd;
							if (!p.isOpen())
								p.open(subM.newChild(1));
						}

						IJavaProject jp = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(pd);

						ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

						IPath ppath = jp.getPath();
						IPath target = ppath.append("target");
						IPath output = target.append("classes");

						HashMap<IPath, SE> resMap = new HashMap<IPath, SE>();
						IPath smjava = ppath.append(proj.build.sourceDirectory);
						SE se = new SE();
						se.excl.add(new Path(".svn/**"));
						se.excl.add(new Path("CVS/**"));
						resMap.put(smjava, new SE());

						for (Resource r : proj.build.resources) {
							IPath p = new Path(r.directory);
							if (p.isAbsolute()) {
								p = ppath.append(p.removeFirstSegments(loc.segmentCount()));
							} else {
								p = ppath.append(r.directory);
							}
							se = resMap.get(p);
							if (se == null) {
								se = new SE();
								resMap.put(p, se);
							}

							se.output = output;

							addFilter(se, r);
						}

						String testOutputDirectory = proj.build.testOutputDirectory;
						if (testOutputDirectory == null)
							testOutputDirectory = "test-classes";
						IPath testcl = target.append(testOutputDirectory);
						IPath stjava = ppath.append(proj.build.testSourceDirectory);
						SE set = new SE();
						set.output = testcl;
						resMap.put(stjava, set);

						for (Resource r : proj.build.testResources) {
							IPath p = new Path(r.directory);
							if (p.isAbsolute()) {
								p = ppath.append(p.removeFirstSegments(loc.segmentCount()));
							} else {
								p = ppath.append(r.directory);
							}
							se = resMap.get(p);
							if (se == null) {
								se = new SE();
								resMap.put(p, se);
							}

							se.output = testcl;

							addFilter(se, r);
						}

						IClasspathEntry ce;
						for (Entry<IPath, SE> e : resMap.entrySet()) {
							IPath p = e.getKey();
							se = e.getValue();
							ce = JavaCore.newSourceEntry(p, se.incl.toArray(NOPATH), se.excl.toArray(NOPATH),
									se.output);
							File d = new File(path, p.removeFirstSegments(1).toString());
							if (!d.exists())
								d.mkdirs();
							entries.add(ce);
						}

						pd.refreshLocal(IResource.DEPTH_INFINITE, subM.newChild(1));

						ce = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));
						entries.add(ce);

						ce = JavaCore.newContainerEntry(new Path(CcpContainer.ID));
						entries.add(ce);

//            ce = JavaCore.newContainerEntry(new Path("de.bb.tools.bnm.eclipse.testClassPath"));
//            entries.add(ce);

						IClasspathEntry[] entriesA = entries.toArray(NOENTRIES);
//            IClasspathEntry[] entriesB = Tracker.updateClasspath(entriesA);
//            if (entriesB != null)
//              entriesA = entriesB;
						jp.setRawClasspath(entriesA, output, monitor);
						jp.save(subM.newChild(1), true);
						theProject.setDescription(description, subM.newChild(1));
					}
				} catch (Exception e) {
					exe = e;
				}
			}

			private void addFilter(SE se, Resource r) {
				for (String inc : r.includes) {
					se.incl.add(new Path(inc));
					if (inc.startsWith("*")) {
						while (inc.startsWith("*")) {
							inc = inc.substring(1);
						}
						se.incl.add(new Path(inc));
					}
				}

				for (String exc : r.getExcludes()) {
					se.excl.add(new Path(exc));
					if (exc.startsWith("*")) {
						while (exc.startsWith("*")) {
							exc = exc.substring(1);
						}
						se.excl.add(new Path(exc));
					}
				}
			}

		};

		try {
			exe = null;
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, wmo);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (exe != null) {
			MessageBox mb = new MessageBox(shell.getShell(), SWT.OK | SWT.ICON_ERROR);
			mb.setText("BNM Import Failed");
			mb.setMessage(exe.toString() + ":" + exe.getMessage());
			mb.open();
		}
		/*
		 * 
		 * String[] newNatures = new String[1];
		 * newNatures[0] = BnmNature.NATURE_ID;
		 * IProjectDescription description2 = project.getDescription();
		 * description2.setNatureIds(newNatures);
		 * project.setDescription(description2, null);
		 */
	}

	static class SE {
		protected IPath output;
		protected HashSet<IPath> incl = new HashSet<IPath>();
		protected HashSet<IPath> excl = new HashSet<IPath>();
	}
}
