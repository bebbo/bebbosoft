/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.versioning.dumb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PomInfo {

    private String groupId;
    private String artifactId;
    private String version;
    private String id;
    private HashMap<String, ArrayList<Pos>> id2loc = new HashMap<String, ArrayList<Pos>>();
    private ManifestInfo manifestInfo;

    public PomInfo(File pomFile) throws IOException {
        DumbXmlSearcher dumbXml = new DumbXmlSearcher(pomFile);

        // collect all artifactId:groupId:version --> pos

        Pos gPos = dumbXml.search("/project/groupId", "*", 0);
        Pos aPos = dumbXml.search("/project/artifactId", "*", 0);
        Pos vPos = dumbXml.search("/project/version", "*", 0);

        artifactId = dumbXml.getContent(aPos);

        if (gPos == null)
            gPos = dumbXml.search("/project/parent/groupId", "*", 0);
        groupId = dumbXml.getContent(gPos);

        if (vPos == null) {
            vPos = dumbXml.search("/project/parent/version", "*", 0);
            version = dumbXml.getContent(vPos);
            vPos = null;
            int index = dumbXml.indexOf("project>");
            if (index > 0) {
                vPos = new Pos(index + 8, 0);
            }

        } else {
            version = dumbXml.getContent(vPos);
        }
        id = groupId + ":" + artifactId + ":" + version;

        if (gPos == null || aPos == null || vPos == null)
            throw new IOException("one of groupId:artifactId:version is null: "
                    + id);

        ArrayList<Pos> al = new ArrayList<Pos>();
        al.add(vPos);
        id2loc.put(id, al);

        add(dumbXml, "/project/parent", id2loc);
        add(dumbXml, "dependency", id2loc);
        add(dumbXml, "plugin", id2loc);

        File maniFile = new File(pomFile.getParentFile(),
                "META-INF/MANIFEST.MF");
        if (maniFile.exists()) {
            manifestInfo = new ManifestInfo(maniFile);
            if (!getOsgiVersion().equals(manifestInfo.getVersion())) {
                System.err.println("pom<->osgi version mismatch: "
                        + getOsgiVersion() + " <-> "
                        + manifestInfo.getVersion());
            }
        }
    }

    private String getOsgiVersion() {
        if (version.endsWith("-SNAPSHOT"))
            return version.substring(0, version.length() - 9).replace('-', '.');
        return version.replace('-', '.');
    }

    private static void add(DumbXmlSearcher dumb, String tag,
            Map<String, ArrayList<Pos>> id2loc) {
        for (int offset = 0; offset >= 0;) {
            Pos pos = dumb.search(tag, "*", offset);
            if (pos == null)
                break;
            // // System.out.println(pos);
            String val = dumb.getContent(pos);
            // // System.out.println(val);

            DumbXmlSearcher d = new DumbXmlSearcher(val);
            Pos gPos = d.search("/groupId", "*", 0);
            Pos aPos = d.search("/artifactId", "*", 0);
            Pos vPos = d.search("/version", "*", 0);

            if (aPos != null && vPos != null) {
                String g = gPos == null ? "org.apache.maven.plugins" : d
                        .getContent(gPos).trim();
                String a = d.getContent(aPos).trim();
                String v = d.getContent(vPos).trim();
                String id = g + ":" + a + ":" + v;

                ArrayList<Pos> al = id2loc.get(id);
                if (al == null) {
                    al = new ArrayList<Pos>();
                    id2loc.put(id, al);
                }
                Pos p = new Pos(pos, vPos);
                al.add(p);
                // System.out.println(id + " --> " + dumb.getContent(p) + " @ "
                // + p);
            }

            offset = pos.getEnd();
        }

    }

    public String getVersion() {
        return version;
    }

    public String getId() {
        return toId(id);
    }

    public static String toId(String gav) {
        int colon = gav.indexOf(':');
        if (colon < 0)
            return gav;
        String lid = gav.substring(0, colon) + "." + gav.substring(colon + 1);
        colon = lid.indexOf(':');
        if (colon < 0)
            return lid;
        return lid.substring(0, colon);
    }

    public String getBundleId() {
        if (this.manifestInfo != null)
            return manifestInfo.getSymbolicName();
        return null;
    }

    public String getBundleVersion() {
        if (this.manifestInfo != null)
            return manifestInfo.getVersion();
        return null;
    }

    public Set<String> getReferences() {
        return id2loc.keySet();
    }

    public ArrayList<Pos> getPositions(String dep) {
        ArrayList<Pos> r = new ArrayList<Pos>();
        for (Entry<String, ArrayList<Pos>>e : id2loc.entrySet()) {
            if (toId(e.getKey()).equals(dep)) {
                r.addAll(e.getValue());
            }
        }
        return r;
    }

    public Set<String> getBundleReferences() {
        if (this.manifestInfo == null)
            return Collections.emptySet();
        return manifestInfo.getReferences();
    }

    public Pos getBundlePositions(String mod) {
        if (this.manifestInfo == null)
            return null;
        return manifestInfo.getPositions(mod);
    }

}
