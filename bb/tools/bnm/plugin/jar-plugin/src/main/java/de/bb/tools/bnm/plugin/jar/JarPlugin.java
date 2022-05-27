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

/**
 * 
 */
public class JarPlugin extends AbstractJarPlugin {
    @Property("${project.build.outputDirectory}") File classesDirectory;

    @Config("classesDirectory") File cfgClassesDirectory;
    
    /**
     * Return the main classes directory, so it's used as the root of the jar.
     * @throws Exception 
     */
    protected File getContentDirectory() throws Exception {
        if (cfgClassesDirectory != null)
            return cfgClassesDirectory;
        return classesDirectory;
    }

}
