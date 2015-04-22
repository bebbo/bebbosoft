/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

import de.bb.tools.bnm.Bind;

public class PluginInfo {  
  public ArrayList<Mojo> mojos = new ArrayList<Mojo>();

  private HashMap<String, Mojo> __mojoMap = new HashMap<String, Mojo>();

  private URLClassLoader __classLoader;

  public void init() {
    for (Mojo m : mojos) {
      __mojoMap.put(m.goal, m);
    }
  }

  public Mojo getMojo(String goal) {
    return __mojoMap.get(goal);
  }

  public void newClassLoader(URL[] urlClasspath) {
    __classLoader = new URLClassLoader(urlClasspath, getClass().getClassLoader());
  }

  public ClassLoader getClassLoader() {
    return __classLoader;
  }

  public String toString() {
    return Bind.append(0, this).toString();
  }
}
