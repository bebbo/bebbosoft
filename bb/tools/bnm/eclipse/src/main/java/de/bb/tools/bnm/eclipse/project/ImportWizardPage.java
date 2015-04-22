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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.eclipse.builder.BnmNature;
import de.bb.tools.bnm.model.Project;

public class ImportWizardPage extends WizardPage {

  protected FileFieldEditor editor;
  private IPath pomFile;

  public ImportWizardPage(String pageName, IStructuredSelection selection) {
    super(pageName);
    setTitle(pageName); //NON-NLS-1
    setDescription("Import a pom based project from the local file system into the workspace"); //NON-NLS-1
  }

  void setFile(IPath path) {
    this.pomFile = path;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
   */
  protected void createAdvancedControls(Composite parent) {
    Composite fileSelectionArea = new Composite(parent, SWT.NONE);
    GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    fileSelectionArea.setLayoutData(fileSelectionData);

    GridLayout fileSelectionLayout = new GridLayout();
    fileSelectionLayout.numColumns = 3;
    fileSelectionLayout.makeColumnsEqualWidth = false;
    fileSelectionLayout.marginWidth = 0;
    fileSelectionLayout.marginHeight = 0;
    fileSelectionArea.setLayout(fileSelectionLayout);

    editor = new FileFieldEditor("fileSelect", "Select a Pom File: ", fileSelectionArea); //NON-NLS-1 //NON-NLS-2
    editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        IPath path = new Path(ImportWizardPage.this.editor.getStringValue());
        setFile(path);
      }

    });
    String[] extensions = new String[] { "pom.xml" }; //NON-NLS-1
    editor.setFileExtensions(extensions);
    fileSelectionArea.moveAbove(null);

  }

  /* (non-Javadoc)
  * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
  */
  protected void createLinkTarget() {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getInitialContents()
   */
  protected InputStream getInitialContents() {
    try {
      return new FileInputStream(new File(editor.getStringValue()));
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getNewFileLabel()
   */
  protected String getNewFileLabel() {
    return "New File Name:"; //NON-NLS-1
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
   */
  protected IStatus validateLinkedResource() {
    return new Status(IStatus.OK, "de.bb.tools.bnm.eclipse", IStatus.OK, "", null); //NON-NLS-1 //NON-NLS-2
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);
    // top level group
    Composite topLevel = new Composite(parent, SWT.NONE);
    topLevel.setLayout(new GridLayout());
    topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    topLevel.setFont(parent.getFont());
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(topLevel, IIDEHelpContextIds.NEW_FILE_WIZARD_PAGE);

    // resource and container group
    createAdvancedControls(topLevel);
    // Show description on opening
    setErrorMessage(null);
    setMessage(null);
    setControl(topLevel);
  }

  public boolean createProjectFromPom() {
    if (pomFile == null)
      return false;
    if (!"pom.xml".equals(pomFile.lastSegment()))
      return false;
    try {
      Bnm bnm = new Bnm(Plugin.getLoader());
      IPath loc = pomFile.removeLastSegments(1);
      bnm.loadFirst(loc.toFile());
      ArrayList<Pom> pio = bnm.getProjectsInOrder();
      if (pio.size() != 1)
        return false;
      Pom pom = pio.get(0);
      Project epom = pom.getEffectivePom();
      String name = epom.groupId + "." + epom.artifactId;
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final IProjectDescription description = workspace.newProjectDescription(name);
      description.setLocation(loc);
      // create the new project operation
      IRunnableWithProgress op = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException {
          CreateProjectOperation op = new CreateProjectOperation(description, ResourceMessages.NewProject_windowTitle);
          try {
            // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
            // directly execute the operation so that the undo state is
            // not preserved.  Making this undoable resulted in too many 
            // accidental file deletions.
            op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
          } catch (ExecutionException e) {
            throw new InvocationTargetException(e);
          }
        }
      };

      getContainer().run(true, true, op);

      IProject project = (IProject) workspace.getRoot().findMember(name);
      String[] newNatures = new String[1];
      newNatures[0] = BnmNature.NATURE_ID;
      IProjectDescription description2 = project.getDescription();
      description2.setNatureIds(newNatures);
      project.setDescription(description2, null);

      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
