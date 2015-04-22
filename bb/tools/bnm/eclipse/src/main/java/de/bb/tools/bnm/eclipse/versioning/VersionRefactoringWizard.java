package de.bb.tools.bnm.eclipse.versioning;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard; 

public abstract class VersionRefactoringWizard extends RefactoringWizard {

	protected VersionRefactoring versionRefactoring;
	protected IWizardPage pvp;

	public VersionRefactoringWizard(VersionRefactoring refactoring, int flags) {
		super(refactoring, flags);
		this.versionRefactoring = refactoring;
	}

	
	public boolean canFinish() {
		return pvp.canFlipToNextPage();
	}

	
	protected void addUserInputPages() {
		pvp = createSelectVersionPage();
		this.addPage(pvp);
	}

	protected abstract IWizardPage createSelectVersionPage();

}