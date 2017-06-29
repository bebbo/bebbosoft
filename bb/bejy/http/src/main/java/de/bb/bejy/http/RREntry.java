/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
package de.bb.bejy.http;

import java.util.*;

import de.bb.util.*;

class RREntry {
    // match criteria
    ByteRef path;
    LinkedList<String> ll;
    String group;
    String userHeader;
	String redirect;
    HashMap<String, String[]> extension2Attrs = new HashMap<String, String[]>();
    HashMap<String, String[]> type2Attrs = new HashMap<String, String[]>();

    RREntry(String p) {
        if (p.endsWith("/"))
            p = p.substring(0, p.length() - 1);
        path = new ByteRef(p);
        ll = new LinkedList<String>();
    }

    /**
     * Return the next available destination.
     * 
     * @return the next available destination
     */
    synchronized String nextDestination() {
        String fwd = ll.removeFirst();
        if (fwd != null)
            ll.addLast(fwd);
        return fwd;
    }

    public String toString() {
        return "RR: " + path + " (" + group + "->" + userHeader + ") " + ll;
    }

    void setReverseByExt(String property) {
        parse(property, extension2Attrs);
    }

    private void parse(String property, HashMap<String, String[]> map) {
        for (final StringTokenizer st = new StringTokenizer(property, "|"); st.hasMoreElements();) {
            final String reverseByExt = st.nextToken().trim();
            final String split[] = reverseByExt.split(":");
            if (split.length != 2)
                continue;
            
            final String extension = split[0];
            final String attrs[] = split[1].split(",");
            map.put(extension, attrs);
        }
    }

    void setReverseByType(String property) {
        parse(property, type2Attrs);
    }
}