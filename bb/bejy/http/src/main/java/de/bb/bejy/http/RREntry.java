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