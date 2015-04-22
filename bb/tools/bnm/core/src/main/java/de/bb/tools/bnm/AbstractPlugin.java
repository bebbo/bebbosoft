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

package de.bb.tools.bnm;

import java.io.File;
import java.util.ArrayList;

import de.bb.tools.bnm.model.Dependency;
import de.bb.util.MultiMap;

public abstract class AbstractPlugin {
    // the current project.
    protected Pom project;
    
    // the current plugin dependencies.
    protected ArrayList<Dependency> dependencies;

    // the current directory
    protected File currentDir;
    
    protected MultiMap<String, String> configuration = new MultiMap<String, String>();
    
    public abstract void execute() throws Exception;

    protected Log getLog() {
        return Log.getLog();
    }
    
    protected boolean forceBuild;
}
