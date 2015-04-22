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
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;

import de.bb.tools.bnm.model.Repository;
import de.bb.tools.bnm.setting.Mirror;
import de.bb.tools.bnm.setting.Profile;
import de.bb.tools.bnm.setting.RepositoryPolicy;
import de.bb.tools.bnm.setting.Settings;
import de.bb.util.MultiMap;
import de.bb.util.XmlFile;

class Setting {

    private HashMap<String, String> variables;
    Settings settings;

    public HashMap<String, String> getVariables() {
        return variables;
    }

    void load(File repoPath) throws Exception {

        File m2 = new File(System.getProperty("user.home"), ".m2");
        String sFile = new File(m2, "settings.xml").getAbsolutePath();
        Log.getLog().debug("loading " + sFile);
        XmlFile settingsXml = new XmlFile();
        settingsXml.readFile(sFile);
        settings = new Settings();
        Bind.bind(settingsXml, "/settings/", settings);

        // path to local repository
        if (repoPath == null) {
            if (settings.localRepository != null)
                repoPath = new File(settings.localRepository);
            if (repoPath == null) {
                String repo = System.getProperty("M2_REPO");
                if (repo == null)
                    repoPath = new File(m2, "repository");
                else
                    repoPath = new File(repo);
            }
        }
        Loader.setRepo(repoPath);

        // repositories --> fill from active Profiles
        ArrayList<Repository> repos = new ArrayList<Repository>();
        // the mirrors
        SortedMap<String, Mirror> mirrors = new MultiMap();
        for (Mirror m : settings.mirrors) {
            if (m.name == null)
                m.name = m.id;
            mirrors.put(m.mirrorOf, m);
            Repository r = new Repository();
            r.id = m.mirrorOf;
            r.url = m.url;
            r.name = m.name;
            r.releases = new RepositoryPolicy();
            r.releases.updatePolicy = "never";
            repos.add(r);
        }
        Loader.setMirrors(mirrors);

        ArrayList<Repository> pluginRepos = new ArrayList<Repository>();
        HashSet<String> active = new HashSet<String>();
        for (String s : settings.activeProfiles) {
            active.add(s);
        }
        // also fill variables
        variables = new HashMap<String, String>();
        for (Profile p : settings.profiles) {
            boolean isActive = active.contains(p.id);
            if (!isActive && p.activation != null)
                isActive = p.activation.isActive();
            if (isActive) {
                repos.addAll(p.repositories);
                pluginRepos.addAll(p.pluginRepositories);
                variables.putAll(p.properties);
            }
        }
        Loader.setRepositories(repos, pluginRepos);
    }

}
