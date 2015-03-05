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
}