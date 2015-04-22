package de.bb.tools.bnm.eclipse.versioning;

import org.eclipse.jface.wizard.IWizardPage;

public class ManageVersionPage extends VersionPage implements IWizardPage {
	public ManageVersionPage(VI[] data) {
		super(data, "Manage Versions", "edit the version as you need them");
	}
}
