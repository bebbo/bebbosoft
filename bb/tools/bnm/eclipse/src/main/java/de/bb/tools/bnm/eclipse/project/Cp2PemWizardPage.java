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

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.eclipse.builder.Tracker;
import de.bb.util.XmlFile;

public class Cp2PemWizardPage extends WizardPage implements SelectionListener, IStructuredContentProvider,
        ISelectionChangedListener {

    // protected FileFieldEditor editor;
    IFile parentPom;
    private Button asCompile;
    private Button asTest;
    private ListViewer moduleList;
    IJavaProject javaProject;
    private TreeMap<String, IClasspathEntry> filtered;

    public Cp2PemWizardPage(String pageName, IFile currentPom, IJavaProject jp) {
        super(pageName);
        this.parentPom = currentPom;
        this.javaProject = jp;
        setTitle(pageName); // NON-NLS-1
        setDescription("Select the entries to convert:"); // NON-NLS-1
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls
     * (org.eclipse.swt.widgets.Composite)
     */
    protected void createAdvancedControls(Composite parent) {
        Composite area = new Composite(parent, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        area.setLayoutData(gridData);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.makeColumnsEqualWidth = false;
        gridLayout.marginWidth = 10;
        gridLayout.marginHeight = 10;
        area.setLayout(gridLayout);

        gridData.horizontalSpan = 2;

        Label label = new Label(area, SWT.LEFT);
        label.setText("The selected projects and libraries are converted into dependencies.\r\nUse CTRL for multi select.");
        label.setLayoutData(gridData);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessVerticalSpace = true;

        moduleList = new ListViewer(area, SWT.BORDER | SWT.MULTI);
        moduleList.addSelectionChangedListener(this);
        moduleList.getControl().setLayoutData(gridData);

        moduleList.setContentProvider(this);
        try {
            IClasspathEntry[] cpes = javaProject.getRawClasspath();
            filtered = new TreeMap<String, IClasspathEntry>();

            String spath = Loader.getRepoPath().toString();
            if (spath.length() > 1 && spath.charAt(1) == ':') {
                spath = spath.substring(0, 1).toUpperCase() + spath.substring(1);
            }
            spath = spath.replace('\\', '/');

            IPath repo = new Path(spath);
            for (IClasspathEntry cpe : cpes) {
                if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    IPath path = cpe.getPath();
                    IPath part = path.removeLastSegments(path.segmentCount() - repo.segmentCount());
                    if (part.equals(repo))
                        filtered.put("LIBRARY: " + path, cpe);
                    continue;
                }
                if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    IPath path = cpe.getPath();
                    if (Tracker.isSlaveProject(javaProject.getProject()))
                        filtered.put("PROJECT: " + path, cpe);
                    continue;
                }
            }
            moduleList.setInput(filtered);
        } catch (Exception e) {
            e.printStackTrace();
        }

        asCompile = new Button(area, SWT.RADIO);
        asCompile.setText("as compile");
        asCompile.addSelectionListener(this);

        asTest = new Button(area, SWT.RADIO);
        asTest.setText("as test");
        asTest.addSelectionListener(this);

        area.moveAbove(null);

    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        // top level group
        Composite topLevel = new Composite(parent, SWT.NONE);
        topLevel.setLayout(new GridLayout());
        topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        topLevel.setFont(parent.getFont());
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(topLevel,
        // IIDEHelpContextIds.NEW_FILE_WIZARD_PAGE);

        // resource and container group
        createAdvancedControls(topLevel);
        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(topLevel);
    }

    public boolean createProject() {
        if (parentPom == null)
            return true;

        
        try {
            final IPath parentPomloc = parentPom.getLocation();
            if (!"pom.xml".equals(parentPomloc.lastSegment()))
                return true;

            // get the selected entries
            IStructuredSelection selection = (IStructuredSelection) moduleList.getSelection();
            final HashSet<IClasspathEntry> selected = new HashSet<IClasspathEntry>();
            for (Iterator<String> i = selection.iterator(); i.hasNext();) {
                IClasspathEntry cpe = filtered.get(i.next());
                selected.add(cpe);
            }
            
            // copy only the unselected entries
            IClasspathEntry[] cpes = javaProject.getRawClasspath();
            final ArrayList<IClasspathEntry> ncpe = new ArrayList<IClasspathEntry>();
            for (IClasspathEntry cpe : cpes) {
                if (!selected.contains(cpe))
                    ncpe.add(cpe);
                
            }
            
            final boolean asTest = this.asTest.getSelection(); 

            WorkspaceModifyOperation wmo = new WorkspaceModifyOperation() {
                
                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                        InterruptedException {
                    try {
                        
                        XmlFile xml = new XmlFile();
                        xml.readFile(parentPomloc.toOSString());
                        
                        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                        for (IClasspathEntry cpe : selected) {
                            IPath pomPath;
                            if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                                pomPath = root.getProject(cpe.getPath().toString().substring(1)).getLocation().append("pom.xml");
                            } else {
                                pomPath = cpe.getPath().removeFileExtension().addFileExtension("pom");
                            }
                            XmlFile modPom = new XmlFile();
                            modPom.readFile(pomPath.toOSString());
                            
                            String key = xml.createSection("/project/dependencies/dependency");
                            String artifactId = xml.createSection(key + "artifactId");
                            xml.setContent(artifactId, modPom.getContent("/project/artifactId"));
                            String groupId = xml.createSection(key + "groupId");
                            xml.setContent(groupId, modPom.getContent("/project/groupId"));
                            String version = xml.createSection(key + "version");
                            xml.setContent(version, modPom.getContent("/project/version"));
                            
                            String scope = xml.createSection(key + "scope");
                            xml.setContent(scope, asTest ? "test" : "compile");
                        }
                        
                        FileOutputStream fos = new FileOutputStream(parentPomloc.toFile());
                        xml.write(fos);
                        fos.close();

                        IClasspathEntry[] ncpes = new IClasspathEntry[ncpe.size()];
                        ncpe.toArray(ncpes);
                        javaProject.setRawClasspath(ncpes, monitor);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, wmo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // close the dialog
        return true;
    }

    
    public boolean canFlipToNextPage() {
        return !moduleList.getSelection().isEmpty();
    }

    
    public void widgetSelected(SelectionEvent e) {
        getWizard().getContainer().updateButtons();
    }

    
    public void widgetDefaultSelected(SelectionEvent e) {
        getWizard().getContainer().updateButtons();
    }

    
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        getWizard().getContainer().updateButtons();
    }

    
    public void selectionChanged(SelectionChangedEvent event) {
        getWizard().getContainer().updateButtons();
    }

    
    public Object[] getElements(Object inputElement) {
        Map<String, IClasspathEntry> data = (Map<String, IClasspathEntry>) inputElement;
        return data.keySet().toArray();
    }

    
    public void dispose() {
        moduleList.getControl().dispose();
        super.dispose();
    }

}
