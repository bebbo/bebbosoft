package de.bb.tools.bnm.eclipse.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

public class NewModuleWizard extends Wizard {

    private NewModuleWizardPage mainPage;

    
    public boolean performFinish() {
        return mainPage.createProject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IFile currentFile) {
        setWindowTitle("BNM New Module Wizard"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new NewModuleWizardPage("Create a new BNM module", currentFile); // NON-NLS-1
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

    
    public boolean canFinish() {
        return super.canFinish() && mainPage.canFlipToNextPage();
    }

}
