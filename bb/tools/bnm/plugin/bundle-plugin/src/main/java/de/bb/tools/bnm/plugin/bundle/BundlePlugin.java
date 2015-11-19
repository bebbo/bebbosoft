/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.bundle-plugin.
 *
 *   de.bb.tools.bnm.plugin.bundle-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.bundle-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.bundle-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.bb.tools.bnm.CpHelper;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.plugin.jar.AbstractJarPlugin;

public class BundlePlugin extends AbstractJarPlugin {
    @Property("${project.build.outputDirectory}")
    private File classesDirectory;

    @Config("classesDirectory")
    File cfgClassesDirectory;

    private CpHelper cpHelper = new CpHelper();

    @Config("instructions")
    private Map<String, String> instructions;

    /**
     * @return type of the generated artifact
     */
    protected String getType() {
        return "jar";
    }

    /**
     * Return the main classes directory, so it's used as the root of the jar.
     */
    protected File getClassesDirectory() {
        return classesDirectory;
    }

    @Override
    public void execute() throws Exception {

        getLog().info("THE BUNDLE PLUGIN IS JUST FAKING A JAR!");

        this.collectFiles();

        // do the bundle stuff and extend the META-INF/MANIFEST.MF
        // TODO

        File jarFile = new File(finalName);
        long minDate = jarFile.lastModified();
        long maxDate = 0;

        // check for modifications
        List<String> cp = cpHelper.getClasspathElements(project, classesDirectory, classesDirectory, false);
        for (String fileName : cp) {
            File f = new File(fileName);
            long date = f.lastModified();
            if (date > maxDate)
                maxDate = date;
            if (maxDate > minDate)
                break;
        }
        File meta = new File(classesDirectory, "META-INF");
        if (!meta.exists())
            meta.mkdirs();
        File mani = new File(meta, "MANIFEST.MF");
        FileOutputStream fos = new FileOutputStream(mani);
        fos.close();
        files.put("META-INF/MANIFEST.MF", mani);

        String embed = instructions.get("Embed-Dependency");
        if (embed != null) {
            ArrayList<Id> ar = project.getCompileDependencies();
            for (Id id : ar) {
                File ef = project.getLoader().findFile(id, "jar");
                files.put(ef.getName(), ef);
            }
        }
        // embed
        if (maxDate > minDate) {
            for (String fileName : cp) {
                File f = new File(fileName);
                if (!f.isFile())
                    continue;
            }
        }

        forceCreation = true;
        this.createJar();
    }

    /**
     * Return the main classes directory, so it's used as the root of the jar.
     * 
     * @throws Exception
     */
    protected File getContentDirectory() throws Exception {
        if (cfgClassesDirectory != null)
            return cfgClassesDirectory;
        return classesDirectory;
    }

}
