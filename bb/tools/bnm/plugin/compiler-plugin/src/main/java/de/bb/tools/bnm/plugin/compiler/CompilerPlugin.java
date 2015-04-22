/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.compiler-plugin.
 *
 *   de.bb.tools.bnm.plugin.compiler-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.compiler-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.compiler-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.compiler;

import java.io.File;
import java.util.List;

import de.bb.tools.bnm.annotiation.Property;

public class CompilerPlugin extends AbstractCompilerPlugin {

    @Property("${project.build.outputDirectory}")
    private File outputDirectory;

    @Property("${project.build.directory}")
    private File dependentDirectory;

    @Property("${project.build.sourceDirectory}")
    private String sourceDirectory;
    
    protected List<String> getClasspathElements() throws Exception {
        return cpHelper.getClasspathElements(project, outputDirectory, outputDirectory, false);
    }

    protected File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    protected String getSourceRoot() {
        return sourceDirectory;
    }

    @Override
    protected File getDependent() {
        return dependentDirectory;
    }
}