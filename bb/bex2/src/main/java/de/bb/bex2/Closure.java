package de.bb.bex2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeSet;

class Closure implements Comparable<Closure> {

	TreeSet<LRE> lres = new TreeSet<LRE>();

	public void addLRE(LRE lre) {
		lres.add(lre);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (LRE lre : lres) {
			if (sb.length() > 1)
				sb.append("\r\n");
			sb.append(lre);
		}
		sb.append("]");
		return sb.toString();
	}

	public void complete(SortedMap<String, LRE> expanded) {
		HashSet<String> done = new HashSet<String>();
		Stack<LRE> todo = new Stack<LRE>();
		todo.addAll(lres);
		while (todo.size() > 0) {
			LRE cl = todo.pop();
			String next = cl.peekFirst();
			if (next == null)
				continue;
			// another rule?
			if (next.charAt(0) == '\'')
				continue;
			if (done.contains(next))
				continue;

			done.add(next);
			Collection<LRE> repls = expanded.subMap(next, next + "\0").values();
			lres.addAll(repls);
			todo.addAll(repls);
		}

	}

	public HashMap<String, Closure> follow() {
		HashMap<String, Closure> ret = new HashMap<String, Closure>();
		for (LRE lre : lres) {
			LRE next = lre.next();
			if (next == null)
				continue;
			String key = lre.peekFirst();
			Closure cl = ret.get(key);
			if (cl == null) {
				cl = new Closure();
				ret.put(key, cl);
			}
			cl.addLRE(next);
		}
		return ret;
	}

	
	public int compareTo(Closure o) {
		int diff = lres.size() - o.lres.size();
		if (diff != 0)
			return diff;
		
		Iterator<LRE> i = lres.iterator();
		Iterator<LRE> j = o.lres.iterator();
		while (i.hasNext()) {
			diff = i.next().compareTo(j.next());
			if (diff != 0)
				return diff;
		}		
		return 0;
	}
	
	public int hashCode() {
		return 33 + lres.hashCode();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Closure))
			return false;
		return compareTo((Closure)o) == 0;
	}
}
