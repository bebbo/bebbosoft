package de.bb.tools.bnm.eclipse.versioning;

import org.eclipse.jface.wizard.IWizardPage;

public class ManageVersionPage extends VersionPage implements IWizardPage {
	public ManageVersionPage(VI[] data) {
		super(data, "Manage Versions", "edit the version as you need them");
		setNoteText("Only the modified versions are changed\r\n"
            + "Be careful - this can lead to a non working state!!!\r\n");
	}
}
