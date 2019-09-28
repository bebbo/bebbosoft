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

package de.bb.tools.bnm.model;

import de.bb.tools.bnm.model.Id;

public class Id {
    /**
     * The group ID of the plugin in the repository.
     */
    public String groupId;
    /**
     * The artifact ID of the plugin in the repository.
     */
    public String artifactId;
    /**
     * The version (or valid range of versions) of the plugin to be used.
     */
    public String version;

    public Id() {
    }

    public Id(String plugin) {
        int col1 = plugin.indexOf(':');
        groupId = plugin.substring(0, col1++);
        int col2 = plugin.indexOf(':', col1);
        if (col2 < col1)
        	throw new RuntimeException("invalid plugin: " + plugin);
        artifactId = plugin.substring(col1, col2);
        version = plugin.substring(col2 + 1);
    }

    /**
     * Get the id String.
     * 
     * @return
     */
    public synchronized String getId() {
    	if (version != null && version.startsWith("[")) {
    		int komma = version.indexOf(',');
    		version = version.substring(1, komma);
    	}
        return groupId + ":" + artifactId + ":" + version;
    }

    public String getGA() {
        return groupId + ":" + artifactId;
    }

    public String toString() {
        return getClass().getName() + " " + getId();
    }

    public int hashCode() {
        int hc = groupId != null ? groupId.hashCode() : 0;
        hc ^= artifactId != null ? artifactId.hashCode() : 0;
        hc ^= version != null ? version.hashCode() : 0;
        return hc;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Id))
            return false;
        Id id = (Id) o;
        if (groupId == null) {
            if (id.groupId != null)
                return false;
        } else if (!groupId.equals(id.groupId))
            return false;

        if (artifactId == null) {
            if (id.artifactId != null)
                return false;
        } else if (!artifactId.equals(id.artifactId))
            return false;

        if (version == null) {
            return id.version == null;
        }
        return version.equals(id.version);
    }

    public final static Comparator COMP = new Comparator();

    public static class Comparator implements java.util.Comparator<Id> {
        public int compare(Id o1, Id o2) {
            try {
                int r = o1.artifactId.compareTo(o2.artifactId);
                if (r != 0)
                    return r;
                r = o1.groupId.compareTo(o2.groupId);
                if (r != 0)
                    return r;

                String v1 = o1.version;
                String v2 = o2.version;

                /*
                 * int c1 = v1.indexOf(':'); int c2 = v2.indexOf(':');
                 * 
                 * // compare versions if (c1 < 0 && c2 < 0) { r =
                 * compareVersion(v1, v2); return r; }
                 */
                return compareVersion(v1, v2);
            } catch (Exception e) {
                throw new RuntimeException("invalid id in: " + o1.getId() + " "
                        + o2.getId());
            }
        }

        private int compareVersion(String v1, String v2) {
            int vl[] = new int[3];
            int vr[] = new int[3];

            String ql = parse(v1, vl);
            String qr = parse(v2, vr);

            for (int i = 0; i < 3; ++i) {
                if (vl[i] != vr[i])
                    return vl[i] - vr[i];
            }

            return ql.compareTo(qr);
        }

        private String parse(String v, int[] vv) {
            int dash = v.indexOf('-');
            if (dash < 0)
                dash = v.length();
            for (int i = 0, last = 0, dot = v.indexOf('.'); i < 3; ++i) {
                if (dot < 0) {
                    if (last < dash)
                        vv[i] = Integer.parseInt(v.substring(last, dash));
                    break;
                }
                vv[i] = Integer.parseInt(v.substring(last, dot));
                last = dot + 1;
                dot = v.indexOf('.', last);
            }
            if (dash == v.length())
                return "";
            return v.substring(dash + 1);
        }
    }

    public String toPath() {
        return groupId.replace('.', '/');
    }
}
