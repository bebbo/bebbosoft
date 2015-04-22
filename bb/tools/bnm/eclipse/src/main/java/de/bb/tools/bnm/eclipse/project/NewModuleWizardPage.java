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
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.model.Parent;
import de.bb.tools.bnm.model.Project;
import de.bb.util.XmlFile;

public class NewModuleWizardPage extends WizardPage implements ModifyListener, SelectionListener {

    private static final String[] PACKAGINGS = {"pom", "jar"};
    // protected FileFieldEditor editor;
    IFile parentPom;
    private Text moduleName;
    private CCombo packaging;

    public NewModuleWizardPage(String pageName, IFile currentPom) {
        super(pageName);
        this.parentPom = currentPom;
        setTitle(pageName); // NON-NLS-1
        setDescription("Enter the name and select the packaging.\r\nAdjust the version after creation."); // NON-NLS-1
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
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        area.setLayoutData(gridData);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.makeColumnsEqualWidth = false;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        area.setLayout(gridLayout);

        Label label = new Label(area, SWT.NONE);
        label.setText("module name:");
        moduleName = new Text(area, SWT.BORDER);
        moduleName.addModifyListener(this);

        label = new Label(area, SWT.NONE);
        label.setText("packaging:");
        packaging = new CCombo(area, SWT.BORDER);
        packaging.setItems(PACKAGINGS);
        packaging.addSelectionListener(this);

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
            return false;

        final String name = moduleName.getText();
        final String packaging = this.packaging.getText();

        final IPath parentPomloc = parentPom.getLocation();

        if (!"pom.xml".equals(parentPomloc.lastSegment()))
            return true;
        try {
            Bnm bnm = new Bnm(Plugin.getLoader());
            IPath loc = parentPomloc.removeLastSegments(1);
            bnm.loadFirst(loc.toFile());
            ArrayList<Pom> pio = bnm.getProjectsInOrder();
            if (pio.size() != 1)
                throw new Exception("no pom found at: " + loc);

            Pom pom = pio.get(0);
            final Project epom = pom.getEffectivePom();

            IPath newModuleLoc = loc.append(name);
            final File newModuleFile = newModuleLoc.toFile();

            IPath newPomLoc = newModuleLoc.append("pom.xml");
            final File newPomFile = newPomLoc.toFile();
            if (newPomFile.exists()) {
                MessageBox box = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
                box.setMessage("WARNING: " + newPomFile.toString() + " already exists!\r\nOverwrite it?");
                int r = box.open();
                if (r != SWT.OK)
                    return true;
            }

            WorkspaceModifyOperation wmo = new WorkspaceModifyOperation() {
                
                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                        InterruptedException {
                    try {

                        if (!newModuleFile.exists())
                            newModuleFile.mkdir();

                        Project newPom = new Project();
                        newPom.artifactId = name;
                        newPom.groupId = epom.groupId;
                        newPom.version = "0.0.1-SNAPSHOT";
                        newPom.parent = new Parent();
                        newPom.parent.artifactId = epom.artifactId;
                        newPom.parent.groupId = epom.groupId;
                        newPom.parent.version = epom.version;
                        newPom.packaging = packaging;

                        FileOutputStream fos = new FileOutputStream(newPomFile);
                        fos.write(newPom.toString().getBytes());
                        fos.close();

                        XmlFile xml = new XmlFile();
                        xml.readFile(parentPomloc.toOSString());

                        boolean exists = false;
                        for (Iterator<String> i = xml.sections("/project/modules/module"); i.hasNext();) {
                            String key = i.next();
                            if (name.equals(xml.getContent(key).trim())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            String key = xml.createSection("/project/modules/module");
                            xml.setContent(key, name);
                            fos = new FileOutputStream(parentPomloc.toFile());
                            xml.write(fos);
                            fos.close();
                        }

                        parentPom.getProject().refreshLocal(IResource.DEPTH_INFINITE,
                                new SubProgressMonitor(monitor, 42));
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
        return moduleName.getText().length() > 0 && packaging.getSelectionIndex() >= 0;
    }

    
    public void modifyText(ModifyEvent e) {
        getWizard().getContainer().updateButtons();
    }

    
    public void widgetSelected(SelectionEvent e) {
        getWizard().getContainer().updateButtons();
    }

    
    public void widgetDefaultSelected(SelectionEvent e) {
        getWizard().getContainer().updateButtons();
    }

}
