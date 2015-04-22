package de.bb.tools.bnm.eclipse.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

public class Cp2PemWizard extends Wizard {
    private Cp2PemWizardPage mainPage;

    
    public boolean performFinish() {
        return mainPage.createProject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IFile currentFile, IJavaProject jp) {
        setWindowTitle("BNM Convert Build Path to Dependencies"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new Cp2PemWizardPage("Convert Build Path to Dependencies", currentFile, jp); // NON-NLS-1
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
