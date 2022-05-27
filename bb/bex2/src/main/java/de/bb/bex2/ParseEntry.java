/**
 * written by Stefan Bebbo Franke
 * (c) 1999-2004 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * BebboSoft Lexer.
 */
package de.bb.bex2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * holds information for segments of the parsed file.
 * 
 * @author bebbo
 */
public class ParseEntry implements Iterator<ParseEntry> {
    private static final ArrayList<ParseEntry> EMPTY_LIST = new ArrayList<ParseEntry>();
    /**
     * Reference to the used scanner object. is used to retrieve the original text.
     */
    private Scanner scanner;
    /** absolute offsets into the scanner. */
    private int start, end;
    /** the type of the segment. */
    private int type;
    /** parent ParseEntry. */
    private ParseEntry parent;
    /**
     * the related ParseEntry is used to keep the relationship between start and end tags.
     */
    private ParseEntry related;
    /** list of children. Is null, if no children exist. */
    private ArrayList<ParseEntry> children;
    private HashMap<String, Object> attributes;
    private TagEntry tag;
    private ParseEntry next;

    /**
     * @param scanner
     *            - the scanner
     * @param type
     * @param start
     */
    public ParseEntry(Scanner scanner, int type, int start) {
        this.scanner = scanner;
        this.type = type;
        this.start = start;
    }

    /**
     * @param scanner
     *            - the scanner. Needed for getText().
     * @param type
     * @param start
     * @param end
     */
    public ParseEntry(Scanner scanner, int type, int start, int end) {
        this(scanner, type, start);
        this.end = end;
    }

    /**
     * @param paren
     *            the parent entry
     * @param scanner
     *            - the scanner. Needed for getText().
     * @param type
     * @param start
     */
    public ParseEntry(ParseEntry parent, Scanner scanner, int type, int start) {
        this(scanner, type, start);
        this.parent = parent;
    }

    /**
     * Append an entry as last child.
     * 
     * @param entry
     * @return
     */
    public ParseEntry append(ParseEntry entry) {
        if (children == null) {
            children = new ArrayList<ParseEntry>();
        } else {
            ParseEntry last = children.get(children.size() - 1);
            last.next = entry;
        }
        children.add(entry);
        entry.setParent(this);
        return entry;
    }

    /**
     * @param parent
     */
    private void setParent(ParseEntry parent) {
        this.parent = parent;
    }

    /**
     * @return
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return
     */
    public ParseEntry getParent() {
        return parent;
    }

    /**
     * @return
     */
    public int getStart() {
        return start;
    }

    /**
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * @param i
     */
    public void setEnd(int i) {
        end = i;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        sb.append(start);
        sb.append("-");
        sb.append(end);
        sb.append("):");
        sb.append(type);
        sb.append("\r\n");
        try {
            if (scanner != null)
                sb.append(scanner.substring(start, end));
        } catch (RuntimeException rte) {
            //rte.printStackTrace();
            sb.append("");
        }
        if (children != null) {
            sb.append(children);
        }
        return sb.toString();
    }

    /**
     * @return
     */
    public ParseEntry first() {
        if (children == null || children.isEmpty())
            return null;
        return children.get(0);
    }

    /**
     * @return
     */
    public ParseEntry last() {
        if (children == null || children.isEmpty())
            return null;
        return children.get(children.size() - 1);
    }

    /**
     * return the Scanner object.
     * 
     * @return the Scanner object.
     */
    public Scanner getScanner() {
        return scanner;
    }

    /**
     * @return Returns the children.
     */
    public ArrayList<ParseEntry> getChildren() {
        if (children == null)
            return EMPTY_LIST;
        return children;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Set the related ParseEntry.
     * 
     * @param related
     *            the related ParseEntry.
     */
    public void setRelated(ParseEntry related) {
        this.related = related;
    }

    /**
     * Return the related ParseEntry.
     * 
     * @return the relted ParseEntry.
     */
    public ParseEntry getRelated() {
        return related;
    }

    /**
     * @return
     */
    public String getText() {
        if (end < start)
            return "?";
        return scanner.substring(start, end);
    }

    /**
     * @param i
     * @return
     */
    public ParseEntry getChild(int i) {
        if (children == null)
            return null;
        if (i >= children.size())
            return null;
        return children.get(i);
    }

    /**
     * @param attributes
     */
    public void setAttributes(HashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    public HashMap<String, Object> getAttributes() {
        return attributes;
    }

    public Object setAttribute(String name, Object value) {
        if (attributes == null)
            attributes = new HashMap<String, Object>();
        return attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        if (attributes == null)
            return null;
        return attributes.get(name);
    }

    /**
     * @return
     */
    public TagEntry getTag() {
        return tag;
    }

    public void setTag(TagEntry te) {
        tag = te;
    }

    /**
   * 
   */
    public void dropLast() {
        if (children == null || children.isEmpty())
            return;
        children.remove(children.size() - 1);
    }

    /**
   * 
   */
    public void unquote() {
        String txt = getText();
        if (txt.length() > 2 && txt.charAt(0) == txt.charAt(txt.length() - 1)) {
            ++start;
            --end;
        }
    }

    public void setStart(int position) {
        start = position;
    }

    public ParseEntry next() {
        return next;
    }

    
    public boolean hasNext() {
        return next != null;
    }

    
    public void remove() {
        throw new RuntimeException("not supported");
    }
}