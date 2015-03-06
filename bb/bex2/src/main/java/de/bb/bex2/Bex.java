/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/Bex.java,v $
 * $Revision: 1.3 $
 * $Date: 2013/11/28 12:24:10 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 Created on 02.12.2003

 *****************************************************************************/
package de.bb.bex2;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;

import de.bb.bex2.Bex.Rule;

/**
 * Bebbo's easy Lex.
 * 
 * @author bebbo
 */
public class Bex {
    private ByteArrayOutputStream bos;

    PrintStream out;

    private String mainClass;

    public boolean isRule;

    TreeMap<String, Rule> rules = new TreeMap<String, Rule>();

    // alias -> aliased rule
    private HashMap<String, String> replaced = new HashMap<String, String>();

    private HashMap<String, String> ids = new HashMap<String, String>();

    HashMap<String, String> exports = new HashMap<String, String>();

    private Scanner scanner;

    private int idStart;

    private int tokenNum;

    HashMap<String, Integer> types = new HashMap<String, Integer>();

    //    StringBuffer contextBuffer = new StringBuffer();

    /**
     * Create a Bex instance and load the given grammar.
     * 
     * @param grammar
     */
    public Bex(String grammar) {
        bos = new ByteArrayOutputStream();
        out = new PrintStream(bos);
        scanner = new Scanner(grammar);
        loadGrammar();
        scanner = null;
    }

    public String getClassName() {
        return mainClass;
    }

    public byte[] getCode() {
        return bos.toByteArray();
    }

    /**
     * load the specified grammar.
     */
    private void loadGrammar() {
        skipSpacesComments();
        if (scanner.match("EXPORT")) {
            loadExport();
        } else {
            throw new RuntimeException("missing EXPORT section");
        }
        if (scanner.match("IMPORT")) {
            loadImports();
        }
        if (scanner.match("RULES")) {
            loadRules();
        }
        if (scanner.match("ID")) {
            loadIds();
        }

        // add a default false rule
        rules.put(".", new FRule("."));

        Rule[] rr = rules.values().toArray(new Rule[] {});
        // alias
        for (int i = 0; i < rr.length; ++i) {
            if (rr[i] instanceof ASRule) {
                ((ASRule) rr[i]).alias();
            }
        }

        // create word rules
        for (int i = 0; i < rr.length; ++i) {
            rr[i].mkWordRules();
        }

        // resolve rules
        for (int i = 0; i < rr.length; ++i) {
            rr[i].resolve();
        }

        // optimize rules
        rr = rules.values().toArray(new Rule[] {});
        for (int i = 0; i < rr.length; ++i) {
            rr[i].optimize();
        }

        int dot = mainClass.lastIndexOf('.');
        String classOnly = mainClass;
        if (dot > 0) {
            out.println("package " + mainClass.substring(0, dot) + ";");
            classOnly = mainClass.substring(dot + 1);
        }

        out.println("\r\npublic final class " + classOnly + " extends de.bb.bex2.Parser {");

        // static IDs
        int id = idStart;
        for (Object element : rules.values()) {
            Rule r = (Rule) element;
            if (r instanceof IDRule) {
                out.println("  public final static int ID_" + r.name + " = " + (++id) + ";");
            }
        }

        out.println();
        // main class
        out.println("  public " + classOnly + "(de.bb.bex2.Scanner scanner, de.bb.bex2.Context ctx) {");
        out.println("    super(scanner, ctx);");
        out.println("    current = new de.bb.bex2.ParseEntry(scanner, -1, 0, scanner.getLength());\r\n");
        out.println("  }\r\n");

        // data defs
        for (Rule r : rules.values()) {
            String def = r.definition();
            if (def != null) {
                out.println(def);
            }
        }

        out.println();

        for (Rule r : rules.values()) {
            String code = r.code();
            if (code != null) {
                if (exports.get(r.name) != null) {
                    out.println("  public");
                } else {
                    out.println("  private");
                }

                out.println(code);
            }
        }
        out.println("  public final static int TK_USERDEFINED = " + (++tokenNum) + ";");
        out.println("}\r\n");
        out.flush();

        System.out.println(types);
        //        System.out.println(contextBuffer);
    }

    /**
     * load the rules. allowed elements:
     * 
     * <pre> rulename - name of another rule, either defined in RULES, or import via IMPORT 'expected string' - the
     * content is expected. \, : and ' must be quoted by \ #x34 - a hexadecimal value [a-zA-Z] - a character range, a
     * character must not occur twice \ and ] mus be escaped ( ) - grouping brackets, must not be used with characters |
     * - an alternative. characters must only alternate with characters. ? - 0 or 1 times * - 0 to N times + - 1 to N
     * times ; - end of rule </pre>
     */
    private void loadRules() {
        scanner.move(6);
        skipSpacesComments();
        expect("{");
        skipSpacesComments();
        while (scanner.peek() != '}') {
            String name = nextWord();
            skipSpacesComments();
            expect("::=");
            skipSpacesComments();

            int position = scanner.getPosition();
            String rule;
            // seek next ::=
            int next = scanner.indexOf("::=");
            if (next < 0) {
                next = scanner.indexOf("ID");
            }
            if (next > 0) {
                next = scanner.lastIndexOf(";", next);
            } else {
                next = scanner.lastIndexOf(";");
            }
            if (next < position) {
                throw new RuntimeException("missing ; " + scanner);
            }
            rule = scanner.get(position, next - position);
            scanner.move(rule.length());
            rule = rule.trim();

            expect(";");
            skipSpacesComments();

            out.println("// rule: " + name + " ::= " + replace(replace(rule, "\n", "\n//     "), "\\", "\\\\") + ";");

            Rule r;
            if (rule.length() == 0 || "#[".indexOf(rule.charAt(0)) >= 0) {
                r = parseCharacterRule(name, rule);
            } else {
                r = parseRule(name, rule);
            }
            rules.put(name, r);

        }
        expect("}");
        skipSpacesComments();
    }

    /**
     * Parse the specified rule.
     * 
     * @param name
     *            rule name
     * @param rule
     *            the rule definition
     * @param level
     *            nesting level
     */
    private Rule parseRule(String name, String rule) {
        String inRule = rule;
        // set to true, if | is used outside of braces
        boolean fixup = false;
        Stack<ASRule> stack = new Stack<ASRule>();
        int n = 0;
        ASRule r = new SRule(name);
        rules.put(name, r);
        while (rule.length() > 0) {
            // start of an alternative
            switch (rule.charAt(0)) {
            case '(': {
                String nm = name + "__" + (++n);
                r.append(nm);
                stack.push(r);
                r = new ARule(nm);
                rules.put(nm, r);

                nm = name + "__" + (++n);
                r.append(nm);
                stack.push(r);
                r = new SRule(nm);
                rules.put(nm, r);

                rule = rule.substring(1).trim();
                continue;
            }

            case ')':
                stack.pop();
                r = stack.pop();
                rule = rule.substring(1).trim();
                continue;

            case '|': {
                if (stack.size() == 0) {
                    if (fixup) {
                        throw new RuntimeException(" syntax error in rule: " + rule);
                    }
                    fixup = true;

                    ARule a = new ARule(r.name);
                    String nm = name + "__" + (++n);
                    r.name = nm;
                    rules.put(nm, r);

                    a.append(r.name);
                    stack.push(a);

                } // else
                {
                    r = stack.peek();
                    String nm = name + "__" + (++n);
                    r.append(nm);
                    r = new SRule(nm);
                    rules.put(nm, r);
                }
                rule = rule.substring(1).trim();
                continue;
            }

            case '\'': {
                int len = 1;
                for (; len < rule.length();) {
                    int ch = rule.charAt(len++);
                    if (ch == '\'') {
                        break;
                    }
                    if (ch == '\\') {
                        ++len;
                    }
                }
                r.append(rule.substring(0, len));
                rule = rule.substring(len).trim();
                continue;
            }
            case '"': {
                int len = 1;
                for (; len < rule.length();) {
                    int ch = rule.charAt(len++);
                    if (ch == '"') {
                        break;
                    }
                    if (ch == '\\') {
                        ++len;
                    }
                }
                r.append(rule.substring(0, len));
                rule = rule.substring(len).trim();
                continue;
            }
            case '?':
            case '*':
            case '+':
                r.setQuali(rule.substring(0, 1));
                rule = rule.substring(1).trim();
                continue;

            }
            int len = 0;
            for (; len < rule.length();) {
                int ch = rule.charAt(len);
                if ("(|)?*+".indexOf(ch) >= 0) {
                    break;
                }
                if (ch <= 32) {
                    break;
                }
                ++len;
            }
            String t = rule.substring(0, len);
            r.append(t);
            rule = rule.substring(len).trim();
        }
        if (fixup) {
            r = stack.pop();
        }

        if (stack.size() > 0) {
            throw new RuntimeException("incomplete rule: " + inRule);
        }
        return r;
    }

    /**
     * @param name
     * @param rule
     */
    private Rule parseCharacterRule(String name, String rule) {
        CRule r = new CRule(name);
        for (; rule.length() > 0;) {
            int len = 0;
            if (rule.charAt(0) == '#') {
                len = r.addChar(rule);
            } else if (rule.charAt(0) == '[') {
                len = r.addRange(rule);
            } else {
                throw new RuntimeException("invalid character rule: " + rule + scanner);
            }
            rule = rule.substring(len).trim();
            if (rule.length() == 0) {
                break;
            }
            if (rule.charAt(0) != '|') {
                throw new RuntimeException("invalid character rule: " + rule + scanner);
            }
            rule = rule.substring(1).trim();
        }
        return r;
    }

    /**
     * use IDs for the specified RULES
     * 
     * <pre> ID { DTD, Attribute } </pre>
     */
    private void loadIds() {
        scanner.move(2);
        skipSpacesComments();
        expect("{");
        skipSpacesComments();
        while (scanner.peek() != '}') {
            String name = nextWord();
            skipSpacesComments();
            expect(":");
            skipSpacesComments();
            String type = nextWord();
            skipSpacesComments();
            expect(";");
            out.println("// ID_" + name + ": " + type + ";");
            if (rules.get(name) == null) {
                System.err.println("unknown rule: " + name);
            } else {
                ids.put(name, type);
            }
            skipSpacesComments();
        }
        expect("}");
        skipSpacesComments();
    }

    /**
     * load predefined classes. e.g.
     * 
     * <pre> CLASSES { S ::= de.bb.bnf.WhiteSpaces QPS ::= de.bb.bnf.QuotedPlainString NAME ::= de.bb.bnf.xml.Name }
     * </pre>
     */
    private void loadImports() {
        scanner.move(6);
        skipSpacesComments();
        expect("{");
        skipSpacesComments();
        while (scanner.peek() != '}') {
            String name = nextWord();
            skipSpacesComments();
            expect("::=");
            skipSpacesComments();
            String className = nextWord();
            skipSpacesComments();
            expect(";");
            skipSpacesComments();

            out.println("// use: " + className + " for " + name);
            rules.put(name, new XRule(name, className));
        }
        expect("}");
        skipSpacesComments();
    }

    /**
     * load the export declaration. e.g.:
     * 
     * <pre> EXPORT { doctypedecl ::= de.bb.bnf.xml.Dtd; } </pre>
     */
    private void loadExport() {
        scanner.move(6);
        skipSpacesComments();
        expect("{");
        skipSpacesComments();

        while (scanner.peek() != '}') {
            String type = nextWord();
            skipSpacesComments();
            expect(":");
            skipSpacesComments();
            String value = nextWord();
            skipSpacesComments();
            expect(";");
            skipSpacesComments();

            if ("class".equals(type)) {
                mainClass = value;
            } else if ("public".equals(type)) {
                exports.put(value, value);
            } else if ("idstart".equals(type)) {
                idStart = Integer.parseInt(value);
            } else {
                System.err.println("ERROR: " + type);
            }

        }
        expect("}");
        skipSpacesComments();

        out.println("// class: " + mainClass);
    }

    /**
     * Read the next word which should be a valid Java identifier.
     * 
     * @return the word
     */
    private String nextWord() {
        int pos = scanner.getPosition();
        if (Character.isJavaIdentifierStart((char) scanner.peek()) || Character.isDigit((char) scanner.peek())) {
            scanner.move(1);
            for (;;) {
                int ch = scanner.peek();
                if (ch != '.' && !Character.isJavaIdentifierPart((char) ch) && !Character.isDigit((char) ch)) {
                    break;
                }
                scanner.move(1);
            }
        }
        return scanner.get(pos, scanner.getPosition() - pos);
    }

    /**
     * expect the given string, if not throw a runtime exception
     * 
     * @param string
     *            the expected String
     */
    private void expect(String string) {
        if (!scanner.match(string)) {
            throw new RuntimeException("expected '" + string + "' at : " + scanner);
        }
        scanner.move(string.length());
    }

    /**
     * skip spaces and comments.
     */
    private void skipSpacesComments() {
        do {
            skipSpaces();
        } while (skipComment());
    }

    /**
     * skip comments.
     */
    private boolean skipComment() {
        int ch = scanner.peek();
        if (ch != '/') {
            return false;
        }
        ch = scanner.peek(1);
        if (ch == '/') {
            skipLineComment();
            return true;
        }
        if (ch == '*') {
            skipMultiComment();
            return true;
        }
        return false;
    }

    /**
     * skip a multiline comment.
     */
    private void skipMultiComment() {
        scanner.move(2);
        for (;;) {
            int ch = scanner.peek();
            if (ch < 0) {
                break;
            }
            if (ch == '*' && scanner.peek(1) == '/') {
                scanner.move(2);
                break;
            }
            scanner.read();
        }
    }

    /**
     * skip a single line comment.
     */
    private void skipLineComment() {
        scanner.move(2);
        for (;;) {
            int ch = scanner.peek();
            if (ch == 0xa || ch < 0) {
                break;
            }
            scanner.read();
        }
    }

    /**
     * skip white spaces.
     */
    private void skipSpaces() {
        for (;;) {
            int ch = scanner.peek();
            if (ch < 0 || ch > 32) {
                break;
            }
            scanner.read();
        }
    }

    static String toClassName(String name) {
        StringBuffer sb = new StringBuffer();
        sb.append("W_");
        for (int i = 1; i < name.length() - 1; ++i) {
            char ch = name.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                sb.append(ch);
            } else {
                sb.append("_x");
                String hex = Integer.toHexString(ch);
                while (hex.length() < 4) {
                    hex = "0" + hex;
                }
                sb.append(hex);
            }
        }
        return sb.toString();
    }

    abstract class Rule {
        protected String name;

        public String toString() {
            return getClass().getName() + ": " + name;
        }

        /**
         * @param name
         */
        Rule(String name) {
            this.name = name;
        }

        /**
         * @param rules
         */
        void mkWordRules() {
            // empty
        }

        /**
         * @return
         */
        String code() {
            return null;
        }

        /**
     * 
     */
        String definition() {
            return null;
        }

        String fx() {
            return "is_" + name + "()";
        }

        /**
         * @param rules
         * @return
         */
        void resolve() {
            // empty
        }

        /**
         * optimize
         * 
         * @author bebbo
         */
        void optimize() {
            // empty
        }

    }

    abstract class ASRule extends Rule {
        protected HashMap<String, Object> uses = new HashMap<String, Object>();

        protected ArrayList<String> ruleList = new ArrayList<String>();

        protected ArrayList<String> quali = new ArrayList<String>();

        String tokenName;

        /**
         * @param name
         */
        ASRule(String name) {
            super(name);
        }

        /**
         * @param rules
         */
        void alias() {
            if (uses.size() == 1 && " ".equals(quali.get(0)) && exports.get(name) == null
                    && !(name.equals(name.toLowerCase()) && name.indexOf("__") < 0)) {
                String repl = (String) uses.values().iterator().next();
                // if ("!'\"".indexOf(repl.charAt(0)) >= 0 )
                if (!repl.startsWith("$")) {
                    // repl = repl.replaceAll("\r", " ");
                    // repl = repl.replaceAll("\n", " ");
                    out.println("// replacing " + name + " with " + repl);
                    rules.put(name, new RRule(name, repl));
                    replaced.put(repl, name);
                }
            }
        }

        /**
         * @param nm
         */
        void append(String nm) {
            uses.put(nm, nm);
            ruleList.add(nm);
            quali.add(" ");
        }

        void setQuali(String q) {
            quali.set(quali.size() - 1, q);
        }

        void resolve() {
            if (uses.size() == 0) {
                System.err.println("ERROR: empty sequence in " + name);
                return;
            }
            for (int i = 0; i < this.ruleList.size(); ++i) {
                String n = this.ruleList.get(i);
                Rule r = rules.get(n);
                if (r == null) {
                    if (n.charAt(0) != '$') {
                        System.err.println("ERROR: missing rule " + n);
                        continue;
                    }
                    r = new IDRule(n);
                    rules.put(n, r);
                }
                if (r instanceof RRule) {
                    int dead = 0;
                    while (r instanceof RRule) {
                        RRule rr = (RRule) r;
                        r = rules.get(rr.replacement);
                        if (++dead > rules.size()) {
                            throw new RuntimeException("FATAL: recursive rule: " + n + " - aborting");
                        }
                    }
                    if (r != null) {
                        uses.remove(n);
                        n = r.name;
                        this.ruleList.set(i, n);
                    }
                }
                uses.put(n, r);
            }
        }

        protected void mkWordRules() {
            for (int i = 0; i < this.ruleList.size(); ++i) {
                String n = this.ruleList.get(i);

                int ch = n.charAt(0);
                switch (ch) {
                case '!': {
                    Rule r = new NRule(n);
                    rules.put(n, r);
                    uses.put(n, r);
                }
                    continue;
                case '\'':
                case '"': {
                    Rule r = new WRule(n);
                    rules.put(n, r);
                    uses.put(n, r);
                }
                }
            }
        }

        protected String getOrgName() {
            String orgName = null;
            // auto generate variables for the text
            if (name.equals(name.toLowerCase())) {
                orgName = name;
                while (replaced.containsKey(orgName) && orgName.indexOf("__") >= 0) {
                    orgName = replaced.get(orgName);
                }
                if (orgName.indexOf("__") >= 0)
                    orgName = null;
            }
            if (orgName != null) {
                tokenName = "TK_" + orgName.toUpperCase();
            }
            return orgName;
        }

        protected void insertParseEntry(StringBuffer sb, String orgName) {
            sb.append("  private de.bb.bex2.ParseEntry " + orgName + ";\r\n  public de.bb.bex2.ParseEntry get"
                    + orgName.substring(0, 1).toUpperCase() + orgName.substring(1) + "() { return " + orgName
                    + "; }\r\n  public final static int " + tokenName + " = " + ++tokenNum + ";\r\n");
        }
    }

    /**
     * Sequence rule.
     * 
     * @author bebbo
     */
    class SRule extends ASRule {
        public boolean useContinue;

        SRule(String name) {
            super(name);
        }

        String code() {
            StringBuffer sb = new StringBuffer();

            sb.append("  boolean is_");
            sb.append(name);
            sb.append("() throws de.bb.bex2.ParseException\r\n");
            sb.append("  {\r\n");

            String orgName = getOrgName();
            if (orgName != null)
                sb.append("    " + orgName + " = current = new de.bb.bex2.ParseEntry(current, scanner, " + tokenName
                        + ", scanner.getPosition());\r\n");

            int startNotify = 0;
            int xx = 0;
            int N = 0;
            for (Iterator<String> i = this.ruleList.iterator(), j = quali.iterator(); i.hasNext();) {
                Object o = uses.get(i.next());
                String q = j.next();
                if (!(o instanceof Rule)) {
                    System.err.println("unresolved: " + o);
                    continue;
                }
                Rule r = (Rule) o;

                if (r instanceof IDRule) {
                    sb.append("    ");
                    for (int k = 0; k < N; ++k) {
                        sb.append("  ");
                    }
                    sb.append("if (context.notify(ID_");
                    sb.append(r.name);
                    sb.append(")) {\r\n");
                    if (startNotify == N) {
                        ++startNotify;
                    }
                    ++N;
                    continue;
                }

                sb.append("    ");
                for (int k = 0; k < N; ++k) {
                    sb.append("  ");
                }

                if (r instanceof FRule) {
                    if (orgName != null) {
                        sb.append("current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();\r\n");
                        sb.append("    ");
                        for (int k = 0; k < N; ++k) {
                            sb.append("  ");
                        }
                    }
                    sb.append("return true;");
                } else {
                    switch (q.charAt(0)) {
                    case '?':
                        if (r instanceof WRule) {
                            sb.append(((WRule) r).call());
                        } else {
                            sb.append(r.fx());
                        }
                        sb.append(";\r\n");
                        break;
                    case ' ':
                    case '+':
                        sb.append("if (");
                        if (r instanceof WRule) {
                            sb.append(((WRule) r).call());
                        } else {
                            sb.append(r.fx());
                        }
                        sb.append(") {\r\n");
                        ++N;
                        if (q.charAt(0) == ' ') {
                            if (xx == 0) {
                                xx = N;
                            }
                            break;
                        }
                        // fall through
                        sb.append("    ");
                        for (int k = 0; k < N; ++k) {
                            sb.append("  ");
                        }
                    case '*':
                        sb.append("while (");
                        if (r instanceof WRule) {
                            sb.append(((WRule) r).call());
                        } else {
                            sb.append(r.fx());
                        }
                        sb.append(") {/**/}\r\n");
                        break;
                    }
                }
            }

            for (int k = 0; k < N; ++k) {
                sb.append("  ");
            }
            if (orgName != null) {
                sb.append("current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();\r\n");
                for (int k = 0; k < N; ++k) {
                    sb.append("  ");
                }
            }
            sb.append("    return true;\r\n");

            for (int i = 0; i < N; ++i) {
                // sb.append("//" + i + "\r\n");
                for (int k = i; k <= N; ++k) {
                    sb.append("  ");
                }
                if (i > startNotify && xx > 0 && xx + i == N) {
                    // sb.append("  throw new de.bb.bex2.ParseException(\"" + name
                    sb.append("  syntaxError(\"" + name + "\");//" + i + "\r\n");
                    for (int k = i; k <= N; ++k) {
                        sb.append("  ");
                    }
                }
                sb.append("}\r\n");
            }

            if (N > 0) {
                if (orgName != null) {
                    sb.append("current = current.getParent();\r\n");
                }
                sb.append("    return false;\r\n");
            }
            sb.append("  }\r\n");

            if (orgName != null)
                insertParseEntry(sb, orgName);

            return sb.toString();
        }
    }

    /**
     * Alternative rule. foo ::= a | b;
     * 
     * @author bebbo
     */
    class ARule extends ASRule {
        private HashMap<String, Rule> loops = new HashMap<String, Rule>();

        ARule(String name) {
            super(name);
        }

        void optimize() {
            solveRecursions();
        }

        void solveRecursions() {
            // search for recursions
            for (int i = 0; i < ruleList.size(); ++i) {
                Object o = uses.get(ruleList.get(i));
                if (!(o instanceof Rule)) {
                    System.err.println("unresolved: " + o);
                    continue;
                }
                Rule r = (Rule) o;
                if (r instanceof SRule && r.name.indexOf("__") > 0) {
                    ASRule sr = (ASRule) r;
                    int li = sr.ruleList.size() - 1;
                    o = sr.ruleList.get(li);
                    if (o.equals(name) && " ".equals(sr.quali.get(li))) {
                        // recursion detected!
                        sr.ruleList.remove(li);
                        loops.put(r.name, r);
                        // and replace single sequences
                        /*
                         * buggy! if (sr.ruleList.size() == 1) { RRule rr = new RRule(sr.name,
                         * (String)sr.ruleList.get(0)); rules.put(rr.name, rr); ruleList.set(i, rr.replacement); }
                         */
                    }
                }
            }
        }

        String code() {
            StringBuffer sb = new StringBuffer();
            sb.append("  boolean is_");
            sb.append(name);
            sb.append("()");
            sb.append(" throws de.bb.bex2.ParseException");
            sb.append("\r\n  {\r\n");

            String orgName = getOrgName();
            if (orgName != null)
                sb.append("    " + orgName + " = current = new de.bb.bex2.ParseEntry(current, scanner, " + tokenName
                        + ", scanner.getPosition());\r\n");

            if (loops.size() > 0) {
                sb.append("   for(;;) {\r\n");
            }

            for (String string : this.ruleList) {
                Object o = uses.get(string);
                if (!(o instanceof Rule)) {
                    System.err.println("unresolved: " + o);
                    continue;
                }
                Rule r = (Rule) o;

                sb.append("    ");

                if (r instanceof FRule) {
                    if (orgName != null)
                        sb.append("{ current = current.getParent(); return false;}\r\n");
                    else
                        sb.append("return false;\r\n");

                    if (loops.size() > 0) {
                        sb.append("   }\r\n");
                    }
                    sb.append("  }\r\n");
                    if (orgName != null)
                        insertParseEntry(sb, orgName);

                    return sb.toString();
                }

                sb.append("if (");
                if (r instanceof WRule) {
                    sb.append(((WRule) r).call());
                } else {
                    sb.append(r.fx());
                }
                sb.append(") {\r\n");
                if (loops.get(r.name) == null) {
                    if (orgName != null) {
                        sb.append("current.setEnd(scanner.getPosition()); current.getParent().append(current); current = current.getParent();\r\n");
                    }

                    sb.append("      return true;\r\n");
                } else {
                    sb.append("      continue;\r\n");
                }
                sb.append("    }\r\n");

            }

            if (loops.size() > 0) {
                sb.append("    break;\r\n");
                sb.append("   }\r\n");
            }

            // sb.append("    throw new de.bb.bex2.ParseException(\"" + name + "\");\r\n");
            if (orgName != null) {
                sb.append("current = current.getParent();\r\n");
            }
            sb.append("    return false;\r\n");
            sb.append("  }\r\n");
            if (orgName != null)
                insertParseEntry(sb, orgName);
            return sb.toString();
        }
    }

    /**
     * Replacement rule.
     * 
     * @author bebbo
     */
    class RRule extends Rule {
        String replacement;

        RRule(String name, String replacement) {
            super(name);
            this.replacement = replacement;
        }

        public String toString() {
            // creates no code
            return "// " + name + " aliases to " + replacement;
        }
    }

    /**
     * Replacement rule.
     * 
     * @author bebbo
     */
    class XRule extends Rule {
        String className;

        XRule(String name, String className) {
            super(name);
            this.className = className;
        }

        String fx() {
            return className + ".isChar(scanner)";
        }

        public String toString() {
            // creates no code
            return "// " + name + " imported using " + className;
        }
    }

    /**
     * match a specified word.
     * 
     * @author bebbo
     */
    class NWRule extends Rule {
        char chars[];

        String className;

        NWRule(String name, String className) {
            super(toClassName(name));
            this.className = className;

            chars = new char[name.length() - 2];
            name.getChars(1, name.length() - 1, chars, 0);
        }

        String definition() {
            StringBuffer sb = new StringBuffer();
            sb.append("  private final static char [] data_");
            sb.append(name);
            sb.append(" = {");
            for (int i = 0; i < chars.length; ++i) {
                sb.append("'");
                if (chars[i] == '\'' || chars[i] == '\\') {
                    sb.append('\\');
                }
                sb.append(chars[i]);
                sb.append("',");
            }
            sb.append("};");
            return sb.toString();
        }
    }

    class WRule extends NWRule {
        WRule(String name) {
            super(name, "WordRule");
        }

        String call() {
            return "scanner.isWord(data_" + name + ")";
        }

        //        
        //        String code() {
        //            StringBuffer sb = new StringBuffer();
        //            sb.append("  boolean is_");
        //            sb.append(name);
        //            sb.append("()\r\n");
        //            sb.append("  {\r\n");
        //
        //            sb.append("    return scanner.isWord(data_");
        //            sb.append(name);
        //            sb.append(");\r\n");
        //            /*
        //             * sb.append("    if (scanner.match(data_"); sb.append(name); sb.append(")) {\r\n");
        //             * sb.append("      scanner.move("); sb.append(chars.length); sb.append(");\r\n");
        //             * 
        //             * sb.append("      return true;\r\n"); sb.append("    }\r\n"); sb.append("    return false;\r\n");
        //             */
        //            sb.append("  }\r\n");
        //            return sb.toString();
        //        }
    }

    class NRule extends NWRule {
        NRule(String name) {
            super(name, "NotWordRule");
        }
    }

    /**
     * match a single character with specified values.
     * 
     * @author bebbo
     */
    class CRule extends Rule {
        private boolean touched;

        boolean invert = false;

        boolean match[] = new boolean[256];

        /**
         * create the character rule.
         * 
         * @param name
         *            the name
         */
        CRule(String name) {
            super(name);
        }

        String code() {
            StringBuffer sb = new StringBuffer();
            sb.append("  boolean is_");
            sb.append(name);
            sb.append("()\r\n");
            sb.append("  {\r\n");
            if (invert) {
                sb.append("    if (scanner.peek() >= 0 && data_");
                sb.append(name);
                sb.append(".indexOf(scanner.peek()) < 0) {\r\n");
            } else {
                sb.append("    if (data_");
                sb.append(name);
                sb.append(".indexOf(scanner.peek()) >= 0) {\r\n");
            }
            sb.append("      scanner.move(1);\r\n");
            sb.append("      return true;\r\n");
            sb.append("    }\r\n");
            sb.append("    return false;\r\n");
            sb.append("  }\r\n");
            return sb.toString();
        }

        String definition() {
            StringBuffer sb = new StringBuffer();
            sb.append("  private final static String data_");
            sb.append(name);
            sb.append(" = \"");
            for (int i = 0; i < 256; ++i) {
                if (match[i]) {
                    if (i == 0xd) {
                        sb.append("\\r");
                        continue;
                    }
                    if (i == 0xa) {
                        sb.append("\\n");
                        continue;
                    }
                    if (i < 32 || i > 127) {
                        sb.append("\\u");
                        String s = Integer.toHexString(i);
                        for (int j = s.length(); j < 4; ++j) {
                            sb.append("0");
                        }
                        sb.append(s);
                        continue;
                    }
                    if (i == '"' || i == '\\') {
                        sb.append('\\');
                    }
                    sb.append((char) i);
                }
            }
            sb.append("\";");
            return sb.toString();
        }

        /**
         * @param rule
         * @return
         */
        int addRange(String rule) {
            if (invert) {
                throw new RuntimeException("ERROR: mixin [^...] is not supported");
            }
            if (rule.charAt(0) != '[') {
                throw new RuntimeException("[ in " + rule);
            }
            int len = 1;
            if (rule.charAt(1) == '^') {
                if (touched) {
                    throw new RuntimeException("ERROR: mixin [^...] is not supported");
                }
                invert = true;
                ++len;
            }
            touched = true;
            for (;;) {
                int ch = rule.charAt(len++);
                if (ch == '\\') {
                    ch = rule.charAt(len++);
                    if (ch == 'x') {
                        int hex1 = rule.charAt(len++);
                        int hex2 = rule.charAt(len++);
                        ch = Integer.parseInt("" + (char) hex1 + (char) hex2, 16);
                    }
                } else {
                    if (ch == ']') {
                        break;
                    }
                }
                // range?
                if (rule.charAt(len) == '-') {
                    ++len;
                    int ch2 = rule.charAt(len++);
                    if (ch2 < ch) {
                        int t = ch;
                        ch = ch2;
                        ch2 = t;
                    }
                    while (ch <= ch2) {
                        match[ch++] = true;
                    }
                } else {
                    match[ch] = true;
                }
            }
            return len;
        }

        /**
         * @param rule
         * @return
         */
        int addChar(String rule) {
            if (invert) {
                throw new RuntimeException("ERROR: mixin [^...] is not supported");
            }
            touched = true;
            if (rule.charAt(0) != '#' || rule.charAt(1) != 'x') {
                throw new RuntimeException("expected #xNN  in " + rule);
            }
            rule = rule.substring(2);
            int len = 0;
            while (len < rule.length()) {
                int ch = rule.charAt(len);
                if (('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'f') || ('A' <= ch && ch <= 'F')) {
                    ++len;
                } else {
                    break;
                }
            }
            int offset = Integer.parseInt(rule.substring(0, len), 16);
            if (offset < 256) {
                match[offset] = true;
            }
            return len + 2;
        }

        /*
         * (non-Javadoc)
         * 
         * @see de.bb.bex2.Bex.Rule#generate(java.util.HashMap)
         */

        public String toString() {
            return name + ": " + definition();
        }
    }

    class IDRule extends Rule {
        IDRule(String name) {
            super(name.substring(1));
        }
    }

    /**
     * False Rule - returns always false
     * 
     * @author stefan franke
     * 
     */
    class FRule extends Rule {

        /**
         * @param name
         */
        FRule(String name) {
            super(name);
            // TODO Auto-generated constructor stub
        }

        String fx() {
            return "false";
        }
    }

    /**
     * 
     * @param text
     * @param pattern
     * @param replacement
     * @return
     */
    private static String replace(String text, String pattern, String replacement) {
        int idx = text.indexOf(pattern);
        if (idx < 0) {
            return text;
        }
        int len = pattern.length();
        StringBuffer sb = new StringBuffer();
        while (idx >= 0) {
            sb.append(text.substring(0, idx));
            sb.append(replacement);
            text = text.substring(idx + len);
            idx = text.indexOf(pattern);
        }
        sb.append(text);
        return sb.toString();
    }
}

/******************************************************************************
 * Log: $Log: Bex.java,v $ Log: Revision 1.3 2013/11/28 12:24:10 bebbo Log: @O using char[] instead of String Log: Log:
 * Revision 1.2 2012/07/18 09:08:52 bebbo Log: @I typified Log: Log: Revision 1.1 2011/01/01 13:07:48 bebbo Log: @N
 * added to new CVS repo Log: Log: Revision 1.3 2006/05/09 12:20:02 bebbo Log: @I cleanup imports Log: Log: Revision 1.2
 * 2005/11/18 14:51:35 bebbo Log: @R many updates - somehow stable version Log: Log: Revision 1.1 2004/05/06 11:02:24
 * bebbo Log: @N first checkin Log:
 ******************************************************************************/
