package de.bb.tools.bnm.eclipse;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.bb.tools.bnm.eclipse.builder.BeforeJavaBuilder;
import de.bb.tools.bnm.eclipse.builder.BnmBuilder;

public abstract class PomAction implements IObjectActionDelegate {

    protected Shell shell;
    protected IProject currentProject;
    protected IFile currentFile;
    protected StructuredSelection currentSelection;
    private boolean masterOnly;
    private boolean slaveOnly;

    public PomAction() {
    }

    public PomAction(boolean masterOnly, boolean slaveOnly) {
        this.masterOnly = masterOnly;
        this.slaveOnly = slaveOnly;
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        currentProject = null;
        currentFile = null;
        currentSelection = null;
        if (selection instanceof StructuredSelection) {
            StructuredSelection ss = (StructuredSelection) selection;
            currentSelection = ss;
            if (ss.size() == 1) {
                Object o = ss.getFirstElement();
                if (o instanceof IFile) {
                    currentFile = (IFile) o;
                    if (currentFile.getFullPath().lastSegment().equals("pom.xml")) {
                        currentProject = currentFile.getProject();
                    } else {
                        currentFile = null;
                    }
                } else if (o instanceof IProject) {
                    IProject ipo = (IProject) o;
                    IFile pomFile = ipo.getFile("pom.xml");
                    if (pomFile.exists()) {
                        currentFile = pomFile;
                        currentProject = ipo;
                    }
                } else if (o instanceof IFolder) {
                    IFolder ipo = (IFolder) o;
                    IFile pomFile = ipo.getFile("pom.xml");
                    if (pomFile.exists()) {
                        currentFile = pomFile;
                    }
                }
            }
        }
        boolean ok = !(masterOnly || slaveOnly);
        if (currentProject != null && !ok) {
            ICommand[] cmds;
            try {
                cmds = currentProject.getDescription().getBuildSpec();
                for (ICommand cmd : cmds) {
                    if (masterOnly && BnmBuilder.BUILDER_ID.equals(cmd.getBuilderName())) {
                        ok = true;
                        break;
                    }
                    if (slaveOnly && BeforeJavaBuilder.ID.equals(cmd.getBuilderName())) {
                        ok = true;
                        break;
                    }
                }
            } catch (CoreException e) {
            }
            if (!ok) {
                currentFile = null;
            }
        }
        action.setEnabled(currentFile != null);
    }
}
