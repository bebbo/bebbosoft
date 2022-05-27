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

import java.util.ArrayList;
import java.util.HashMap;

public class PluginParams {
  public ArrayList<Mojo> mojos;

  public HashMap<String, Mojo> __map;
  public ArrayList<Parameter> __allParams;
  
  public void fillMap() {
    __map = new HashMap<String, Mojo>();
    HashMap<String, Parameter> params = new HashMap<String, Parameter>();
    for (Mojo m : mojos) {
      __map.put(m.goal, m);
      for (Parameter p : m.parameters) {
        params.put(p.name, p);
      }
    }
    __allParams = new ArrayList<Parameter>();
    __allParams.addAll(params.values());
  }

  public ArrayList<Parameter> getAllParameters() {
    return __allParams;
  }
    
  public ArrayList<Parameter> getParameters(String goal) {
    Mojo m = __map.get(goal);
    if (m == null)
      return null;
    return m.parameters;
  }
}
