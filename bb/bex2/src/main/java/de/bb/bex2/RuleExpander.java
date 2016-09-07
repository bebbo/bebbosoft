package de.bb.bex2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import de.bb.bex2.Bex.ARule;
import de.bb.bex2.Bex.ASRule;
import de.bb.bex2.Bex.Rule;
import de.bb.bex2.Bex.SRule;
import de.bb.util.MultiMap;

public class RuleExpander {

    private Stack<String> todo;
    private TreeMap<String, Rule> rules;
    private HashSet<String> done;
    private int rep;

    private SortedMap<String, LRE> expanded = new MultiMap<String, LRE>();
    private Stack<String> init;
    private MultiMap<String, String> first;
    private MultiMap<String, String> follow;

    public RuleExpander(Stack<String> todo, TreeMap<String, Rule> rules) {
        this.init = clone(todo);
        this.todo = todo;
        this.rules = rules;
    }

    @SuppressWarnings("unchecked")
    private static Stack<String> clone(Stack<String> todo) {
        return (Stack<String>) todo.clone();
    }

    void createClosures() {
        Closure cl = new Closure();
        for (String start : init) {
            Collection<LRE> lres = expanded.subMap(start, start + "\0").values();
            for (LRE lre : lres) {
                cl.addLRE(lre);
            }
        }

        HashSet<Closure> all = new HashSet<Closure>();
        Stack<Closure> cls = new Stack<Closure>();
        cl.complete(expanded);
        System.out.println(cl);
        cls.push(cl);
        while (cls.size() > 0) {
            cl = cls.pop();

            HashMap<String, Closure> follow = cl.follow();
            if (follow.size() == 0) {
                continue;
            }
            for (Entry<String, Closure> ec : follow.entrySet()) {
                Closure c = ec.getValue();
                c.complete(expanded);
                if (all.add(c)) {
                    System.out.println(ec.getKey() + "  -->  " + c + "\r\n");
                    cls.add(c);
                }
            }
        }
    }

    // helpers to expand
    void expandRules() {
        done = new HashSet<String>();

        HashSet<String> done = new HashSet<String>();
        while (todo.size() > 0) {
            String ruleName = todo.pop();
            if (done.contains(ruleName)) {
                continue;
            }
            done.add(ruleName);
            ArrayList<String> expansion = new ArrayList<String>();

            Rule rule = rules.get(ruleName);
            expandRule(ruleName, expansion, rule);
        }
        System.out.println("EXPANDED RULES = " + expanded);
        //
        //		// calculate closures and transitions
        //		HashMap<String, Stack<LinkedList<String>>> openClosures = new HashMap<String, Stack<LinkedList<String>>>();
        //		for (String start : init) {
        //			Stack<LinkedList<String>> closure = new Stack<LinkedList<String>>();
        //			closure.addAll(expanded.subMap(start, start + "\0").values());
        //			openClosures.put(start, closure);
        //		}
        //
        //		HashMap<String, Stack<LinkedList<String>>> processedClosures = new HashMap<String, Stack<LinkedList<String>>>();
        //		while (openClosures.size() > 0) {
        //
        //			Stack<LinkedList<String>> completedClosure = new Stack<LinkedList<String>>();
        //
        //			// work on next closure
        //			Iterator<Entry<String, Stack<LinkedList<String>>>> i = openClosures
        //					.entrySet().iterator();
        //			Entry<String, Stack<LinkedList<String>>> e = i.next();
        //			String closureName = e.getKey();
        //			Stack<LinkedList<String>> closure = e.getValue();
        //			i.remove();
        //
        //			// processedClosures.put(closureName, closure);
        //
        //			// complete the closures by adding nested rules for current
        //			// positions
        //			while (closure.size() > 0) {
        //				LinkedList<String> cl = closure.pop();
        //				String next = cl.peekFirst();
        //				// another rule?
        //				if (next.charAt(0) != '\'') {
        //					cl.removeFirst();
        //					for (LinkedList<String> repl : expanded.subMap(next,
        //							next + "\0").values()) {
        //						repl = (LinkedList<String>) repl.clone();
        //						repl.addAll(cl);
        //						closure.push(repl);
        //					}
        //					continue;
        //				}
        //				completedClosure.add(cl);
        //			}
        //			// completedClosure now only contains elements which consume a
        //			// character
        //			System.out.println(completedClosure);
        //			// create the table to consume one character and create follow up
        //			// closures
        //			HashMap<String, Stack<LinkedList<String>>> closureGroups = new HashMap<String, Stack<LinkedList<String>>>();
        //			while (completedClosure.size() > 0) {
        //				LinkedList<String> cl = completedClosure.pop();
        //				String next = cl.removeFirst();
        //				if (cl.size() == 0)
        //					continue;
        //				Stack<LinkedList<String>> group = closureGroups.get(next);
        //				if (group == null) {
        //					group = new Stack<LinkedList<String>>();
        //					closureGroups.put(next, group);
        //				}
        //				group.push(cl);
        //			}
        //
        //			// add the groups to the stack to process
        //			for (Entry<String, Stack<LinkedList<String>>> e2 : closureGroups
        //					.entrySet()) {
        //				System.out.println(e2.getKey() + ": " + e2.getValue());
        //				openClosures
        //						.put(closureName + ":" + e2.getKey(), e2.getValue());
        //			}
        //
        //		}
    }

    private void expandRule(String ruleName, ArrayList<String> expansion, Rule rule) {
        if (rule instanceof ARule) {
            ARule ar = (ARule) rule;
            for (int i = 0; i < ar.quali.size(); ++i) {
                String quali = ar.quali.get(i); // MUST BE UNUSED
                if (!" ".equals(quali)) {
                    System.err.println("quali in alternation: " + rule);
                }
                String rn = ar.ruleList.get(i);
                rn = unliteral(rn);
                ArrayList<String> ex2 = (ArrayList<String>) expansion.clone();
                if (rn.charAt(0) == '\'') {
                    ex2.add(rn);
                    expanded.put(ruleName, new LRE(ex2));
                    // System.out.println(ruleName + " -> " + ex2);
                } else {
                    Rule r = rules.get(rn);
                    done.add(rn);
                    expandRule(ruleName, ex2, r);
                }
            }
            return;
        }

        if (rule instanceof SRule) {
            ASRule sr = (ASRule) rule;
            next(ruleName, sr, expansion, 0);
            return;
        }

        expansion.add(ruleName);

    }

    private void next(String ruleName, ASRule sr, ArrayList<String> expansion, int i) {
        if (i == sr.quali.size()) {
            expanded.put(ruleName, new LRE(expansion));
            // System.out.println(ruleName + " -> " + expansion);
            return;
        }
        String quali = sr.quali.get(i);
        String rn = sr.ruleList.get(i);
        rn = unliteral(rn);
        addTodo(rn);

        // optionals
        if ("?".equals(quali) || "*".equals(quali)) {
            ArrayList<String> ex2 = (ArrayList<String>) expansion.clone();
            next(ruleName, sr, ex2, i + 1);
        }

        if ("*".equals(quali) || "+".equals(quali)) {
            String rp = "rep_" + rep++;
            ArrayList<String> ex = new ArrayList<String>();
            ex.add(rn);
            ex.add(rp);
            expanded.put(rp, new LRE(ex));
            // System.out.println(rp + " -> [" + rp + ", " + rn + "]");
            expansion.add(rp);
        } else {
            expansion.add(rn);
        }
        next(ruleName, sr, expansion, i + 1);
    }

    String unliteral(String ruleName) {
        if (ruleName.startsWith("W_")) {
            ruleName = "'" + ruleName.substring(2) + "'";
        }
        return ruleName;
    }

    void addTodo(String ruleName) {
        if (done.contains(ruleName)) {
            return;
        }

        todo.add(ruleName);
    }

    public void calcFirst() {
        first = new MultiMap<String, String>();

        int size = 0;
        for (;;) {
            for (final Entry<String, LRE> e : expanded.entrySet()) {
                final String id = e.getKey();
                final LRE lre = e.getValue();
                final String x = lre.peekFirst();

                // self reference
                if (x.equals(id)) // ignore self
                    continue;

                // terminal
                if (x.charAt(0) == '\'') {
                    first.remove(id, x); // avoid duplicates
                    first.put(id, x);
                    continue;
                }

                // rule reference - add all terminals of that rule
                for (final String t : first.subMap(x, x + "\u0000").values()) {
                    first.remove(id, t); // avoid duplicates
                    first.put(id, t);
                }
            }

            if (first.size() == size)
                break;
            size = first.size();
        }

        System.out.println("FIRST = " + first);
    }

    public void calcFollow() {
        follow = new MultiMap<String, String>();
        follow.put(init.peek(), "$");

        // meta -> terminal
        for (final LRE lre : expanded.values()) {
            String last = "'";
            for (final String x : lre.rule) {
                // is last a meta symbol and x a terminal?
                if (last.charAt(0) != '\'' && x.charAt(0) == '\'') {
                    follow.remove(last, x); // avoid duplicates
                    follow.put(last, x);
                }
                last = x;
            }
        }
        
        int size = follow.size();
        for(;;) {
            for (final Entry<String, LRE> e : expanded.entrySet()) {
                final String id = e.getKey();
                final LRE lre = e.getValue();
                final String last = lre.rule.get(lre.rule.size() - 1);
                if (last.charAt(0) != '\'') {
                    // rule reference - add all terminals of that rule
                    for (final String t : follow.subMap(id, id + "\u0000").values()) {
                        follow.remove(last, t); // avoid duplicates
                        follow.put(last, t);
                    }
                }
            }
            if (size == follow.size())
                break;
            size = follow.size();
        }

        System.out.println("FOLLOW = " + follow);
    }
}
