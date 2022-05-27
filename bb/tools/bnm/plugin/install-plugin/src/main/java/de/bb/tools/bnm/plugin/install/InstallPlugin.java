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

package de.bb.tools.bnm.plugin.install;

import java.io.File;

import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Id;
import de.bb.util.T2;

/**
 * Installs project's main artifact in the local repository.
 */
public class InstallPlugin extends AbstractInstallPlugin {

    @Property("${project.packaging}")
    protected String packaging;

    /**
     * Whether to update the metadata to make the artifact as release.
     */
    @Config("updateReleaseInfo")
    private boolean updateReleaseInfo;

    public void execute() throws Exception {
        // TODO: push into transformation
        boolean isPomArtifact = "pom".equals(packaging);

        //    ArtifactMetadata metadata = null;
        //
        //    if (updateReleaseInfo) {
        //      artifact.setRelease(true);
        //    }

        File pomFile = new File(project.getPath(), "pom.xml");
        Id pom = project.getProject();

        installerinstall(pomFile, pom, null, "pom");
        if (isPomArtifact)
            return;

        for (T2<File, String> t2 : project.getAttachedFiles()) {
            File file = t2.a;
            String classifier = t2.b;
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            String ext = name.substring(dot + 1);
            if (file.isFile()) {
                installerinstall(file, pom, classifier, ext);
            } else {
                throw new Exception("artifact not found: " + file);
            }
        }
    }

    private void installerinstall(File file, Id id, String classifier, String ext) throws Exception {
        Log log = getLog();
        log.info("installing " + file + " for " + id.getId());
        Loader loader = project.getLoader();
        File f = loader.makeRepoPath(id, classifier, ext);
        if (file.lastModified() < f.lastModified()) {
            log.info("file is up to date: " + f);
            return;
        }
        copyFile(getLog(), file, f);
        installChecksums(f);
    }

}
