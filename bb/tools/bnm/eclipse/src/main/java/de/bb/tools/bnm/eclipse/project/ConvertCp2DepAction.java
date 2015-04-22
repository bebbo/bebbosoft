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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import de.bb.tools.bnm.eclipse.PomAction;

/**
 * Convert the elements on the classpath into POM dependencies.
 * 
 * @author stefan franke
 * 
 */
public class ConvertCp2DepAction extends PomAction {

    public ConvertCp2DepAction() {
        super(false, true);
    }

    public void run(IAction arg0) {
        if (currentFile == null)
            return;

        try {
            IJavaProject jp = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(currentProject);

            Cp2PemWizard cp2pem = new Cp2PemWizard();

            WizardDialog wd = new WizardDialog(shell, cp2pem);
            cp2pem.init(PlatformUI.getWorkbench(), currentFile, jp);
            wd.create();
            wd.open();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
