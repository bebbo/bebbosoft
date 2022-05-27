package de.bb.tools.bnm.eclipse;

import java.lang.reflect.Field;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.ObjectPluginAction;
import org.eclipse.ui.internal.PluginActionContributionItem;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.views.navigator.ResourceNavigator;

public class OpaHandler implements IHandler {

  
  public void addHandlerListener(IHandlerListener handlerListener) {
    // nada - states never change
  }

  
  public void removeHandlerListener(IHandlerListener handlerListener) {
    // nada
  }

  
  public void dispose() {
    // nada
  }

  
  public boolean isEnabled() {
    return true;
  }

  
  public boolean isHandled() {
    return true;
  }

  /**
   * Find the Menu, load it.
   * Search the BNM Menu items for the action definition id and invoke the matching action.
   */
  @SuppressWarnings({ "restriction", "deprecation" })
  
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String id = event.getCommand().getId();
    Workbench wb = Workbench.getInstance();
    IWorkbenchWindow iaww = wb.getActiveWorkbenchWindow();
    IWorkbenchPage iap = iaww.getActivePage();
    IWorkbenchPart iwp = iap.getActivePart();

    try {
      Menu menu;
      // handle 3 views for now.
      if (iwp instanceof ResourceNavigator) {
        menu = ((ResourceNavigator) iwp).getViewer().getTree().getMenu();
      } else {
        Class<? extends Object> clazz = iwp.getClass();
        if (clazz.getName().endsWith("ProjectExplorer")) {
          Field field = clazz.getSuperclass().getDeclaredField("commonViewer");
          field.setAccessible(true);
          Object cv = field.get(iwp);
          clazz = cv.getClass();
          field = clazz.getSuperclass().getDeclaredField("tree");
          field.setAccessible(true);
          Object tree = field.get(cv);
          Control ctl = (Control) tree;
          menu = ctl.getMenu();
        } else {
          // Package Explorer
          Field field = clazz.getDeclaredField("fContextMenu");
          field.setAccessible(true);
          menu = (Menu) field.get(iwp);
        }
      }

      // update the menu
      showMenu(menu);
      MenuItem[] menuItems = menu.getItems();
      MenuItem bmi = findMenuItem(menuItems, "BNM");
      if (bmi == null)
        return null;

      Menu cm = bmi.getMenu();
      showMenu(cm);
      MenuItem[] cis = cm.getItems();

      // search the matching action.
      for (MenuItem ci : cis) {
        Object o = ci.getData();
        if (!(o instanceof PluginActionContributionItem))
          continue;
        PluginActionContributionItem paci = (PluginActionContributionItem) o;
        o = paci.getAction();
        if (!(o instanceof ObjectPluginAction))
          continue;
        ObjectPluginAction opa = (ObjectPluginAction) o;
        if (id.equals(opa.getActionDefinitionId())) {
          // handle the matching action.
          Listener[] ls = ci.getListeners(SWT.Selection);
          Event e = new Event();
          e.type = SWT.Selection;
          e.widget = ci;
          for (Listener l : ls) {
            l.handleEvent(e);
          }
          return null;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  private MenuItem findMenuItem(MenuItem[] menuItems, String string) {
    for (MenuItem mi : menuItems) {
      if (mi.getText().startsWith(string))
        return mi;
    }
    return null;
  }

  private void showMenu(Menu menu) {
    Event e = new Event();
    e.type = SWT.Show;
    e.widget = menu;
    for (Listener l : menu.getListeners(SWT.Show)) {
      l.handleEvent(e);
    }
  }

}
