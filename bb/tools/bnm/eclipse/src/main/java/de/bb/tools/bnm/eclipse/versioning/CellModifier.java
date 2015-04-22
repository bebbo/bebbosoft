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

import java.util.List;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

class CellModifier implements ICellModifier,  ISelectionChangedListener, FocusListener {

  private TV tv;
  private CCombo cc;

  CellModifier(TV tv) {
    this.tv = tv;
  }

  public boolean canModify(Object element, String property) {
    return "1".equals(property);// || "3".equals(property);
  }

  public Object getValue(Object element, String property) {
    VI vi = (VI) element;
    return vi.get(Integer.valueOf(property));
  }

  public void modify(Object element, String property, Object value) {
    String sval = cc.getText();
    if (sval == null)
      return;

    TableItem ti = (TableItem) element;
    if ("1".equals(property)) {
      VI vi = (VI)ti.getData();
      if (!sval.endsWith("-SNAPSHOT")) {
        if (vi.bundleVersion != null && !sval.equals(vi.bundleVersion))
          sval += "-SNAPSHOT";
      }
      vi.version = sval;
    }
    
    Shell sh = ti.getParent().getShell();
    Object data = sh.getData();
    if (data instanceof WizardDialog) {
      WizardDialog rwd = (WizardDialog) data;
      rwd.updateButtons();
    }

    tv.update(ti.getData(), new String[] { sval });
  }

  public void selectionChanged(SelectionChangedEvent event) {
  }

  public void focusGained(FocusEvent e) {
    // get currentData
    TableItem tableItem = tv.getCurrentItem();
    VI data = (VI)tableItem.getData();
    
    cc = (CCombo)e.getSource();
    cc.removeAll();
    
    String value = data.version;
    cc.add(value);
    cc.setText(value);
    
    List<String> candidates = Util.nextSnapshots(value);
    for (String v : candidates) {
      cc.add(v);
    }
    
    if (!value.equals(data.origVersion))
      cc.add(data.origVersion);
    
  }

  public void focusLost(FocusEvent e) {
    // nada
  }
}
