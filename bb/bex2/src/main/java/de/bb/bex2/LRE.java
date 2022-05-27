package de.bb.bex2;

import java.util.ArrayList;
import java.util.Iterator;

class LRE implements Comparable<LRE> {
    ArrayList<String> rule = new ArrayList<String>();
    int pos;

    LRE(ArrayList<String> inRules) {
        for (final String s : inRules) {
            if (s.charAt(0) == '\'' && s.length() > 3) {
                final int end = s.length() - 1;
                for (int i = 1; i < end; ++i) {
                    rule.add("'" + s.substring(i, i + 1) + "'");
                }
                continue;
            }
            rule.add(s);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String r : rule) {
            if (sb.length() > 0)
                sb.append(" ");
            if (n++ == pos) {
                sb.append(".");
            }
            sb.append(r);
        }
        if (pos == rule.size())
            sb.append(".");
        return sb.toString();
    }

    public int hashCode() {
        int n = pos * 37;
        for (String r : rule) {
            n = n * 17 + r.hashCode();
        }
        return n;
    }

    public boolean equals(LRE o) {
        if (pos != o.pos)
            return false;

        if (rule.size() != o.rule.size())
            return false;

        for (Iterator<String> i = rule.iterator(), j = o.rule.iterator(); i.hasNext();) {
            String a = i.next();
            String b = j.next();
            if (!a.equals(b))
                return false;
        }

        return true;
    }

    public int compareTo(LRE o) {
        int n = 0;
        Iterator<String> i = rule.iterator(), j = o.rule.iterator();
        for (; i.hasNext() && j.hasNext(); ++n) {
            String a = i.next();
            String b = j.next();
            int c = a.compareTo(b);
            if (c != 0)
                return c;

            if (pos != o.pos) {
                if (pos == n)
                    return 1;
                if (o.pos == n)
                    return -1;
            }
        }
        if (i.hasNext())
            return 1;
        if (j.hasNext())
            return -1;

        return 0;
    }

    public String peekFirst() {
        if (pos == rule.size())
            return null;
        return rule.get(pos);
    }

    public LRE next() {
        if (pos == rule.size())
            return null;
        LRE next = new LRE(rule);
        next.pos = pos + 1;
        return next;
    }

}
