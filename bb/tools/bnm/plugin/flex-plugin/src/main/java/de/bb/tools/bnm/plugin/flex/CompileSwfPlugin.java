/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.install-plugin.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.install-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.install-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.flex;

import java.io.File;
import java.util.ArrayList;

import de.bb.tools.bnm.annotiation.Property;

/**
 * Invoke the compc compiler
 */
public class CompileSwfPlugin extends AbstractFlexPlugin {
    @Property("${project.build.directory}")
    protected File directory;
    @Property("${project.build.sourceDirectory}")
    protected String sourceDirectory;

    protected File getDirectory() {
        return directory;
    }

    protected String getSourceDirectory() {
        return sourceDirectory;
    }

    protected String getCompiler() {
        return "flex2.tools.Compiler";
    }

    @Override
    protected void extendArgs(ArrayList<String> largs) {
        largs.add("--");

        String mainClass = (String) configuration.get("sourceFile");
        mainClass = new File(new File(project.getPath(), sourceDirectory), mainClass)
                .getAbsolutePath();
        largs.add(mainClass);
    }

    protected String getMainMethod() {
        return "mxmlc";
    }
}
