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
package de.bb.tools.bnm.plugin.jar;

import java.io.File;

import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Project;

/**
 * 
 */
public class TestJarPlugin extends AbstractJarPlugin {

    @Property("${project.build.testOutputDirectory}")
    private File classesDirectory;

    @Config("classesDirectory")
    private File cfgClassesDirectory;
    
    /**
     * @return type of the generated artifact
     */
    protected String getType() {
        return "jar";
    }

    /**
     * Return the main classes directory, so it's used as the root of the jar.
     */
    protected File getContentDirectory() {
        if (cfgClassesDirectory != null)
            return cfgClassesDirectory;
        return classesDirectory;
    }

    protected void makeName() throws Exception {
        if (finalName == null) {
            classifier = "tests";
            Project pom = project.getEffectivePom();
            finalName = pom.build.finalName + "-tests." + getType();
        }
    }
    
    protected void attachFile(File jarFile) {
        project.attachFile(jarFile, "tests");
    }

}
