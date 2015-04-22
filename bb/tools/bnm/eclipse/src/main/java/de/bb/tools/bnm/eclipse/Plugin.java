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
package de.bb.tools.bnm.eclipse;

import java.io.PrintStream;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.eclipse.builder.Tracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class Plugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "de.bb.tools.bnm.eclipse";

  // The shared instance
  private static Plugin plugin;

  // The console output
  private static MessageConsole console;

  // the loader
  private static Loader loader = new Loader();

  // the tracker
  private static Tracker tracker = new Tracker();

  /**
   * The constructor
   */
  public Plugin() {
    System.err.println("start plugin");
    Log.setPrintStream(new PrintStream(getConsole().newOutputStream()));

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IResourceChangeListener listener = tracker;
    workspace.addResourceChangeListener(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
   * )
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
   * )
   */
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Plugin getDefault() {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path
   * 
   * @param path
   *            the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++)
      if (name.equals(existing[i].getName()))
        return (MessageConsole) existing[i];
    // no console found, so create de.bb.eclipse.moject.versioning.dumb new one
    MessageConsole myConsole = new MessageConsole(name, null);
    conMan.addConsoles(new IConsole[] { myConsole });
    return myConsole;
  }

  /**
   * Get the BNM message console.
   * @return the BNM MessageConsole
   */
  public static MessageConsole getConsole() {
    if (console != null)
      return console;
    synchronized (PLUGIN_ID) {
      if (console != null)
        return console;
      console = findConsole("BNM Console");
      return console;
    }
  }

  /**
   * Get the BNM Loader which is used from all projects.
   * @return the BNM Loader.
   */
  public static Loader getLoader() {
      if (loader == null)
          newLoader();
    return loader;
  }

  /**
   * Create a new BNM Loader which is used from all projects.
   * @return the BNM Loader.
   */
  public static Loader newLoader() {
    return loader = new Loader();
  }

  /**
   * Get the tracker which manages all project changes.
   * @return the BNM Tracker.
   */
  public static Tracker getTracker() {
    return tracker;
  }
}
