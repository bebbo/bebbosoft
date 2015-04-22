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
package de.bb.tools.bnm.eclipse.versioning;

import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IActionDelegate;

import de.bb.tools.bnm.eclipse.PomAction;

public class ManageVersionAction extends PomAction {

    public ManageVersionAction() {
        super(true, true);
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {

        if (currentProject == null)
            return;

        try {
            ManageVersionRefactoring r = new ManageVersionRefactoring(currentProject);

            ManageVersionRefactoringWizard w = new ManageVersionRefactoringWizard(r, 2);

            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(w);
            int result = op.run(shell, w.getWindowTitle());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
