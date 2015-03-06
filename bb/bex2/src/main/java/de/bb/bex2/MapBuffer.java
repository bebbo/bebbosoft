/*
 * Created on 05.07.2004
 */
package de.bb.bex2;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A StringBuffer which also maps offsets to a ParseEntry.
 * 
 * @author sfranke
 */
public class MapBuffer {
    private final static String SPACES = "                                                                ";
    private StringBuffer sb = new StringBuffer();
    private ArrayList<PEPosition> sizes = new ArrayList<PEPosition>();
    private PEPosition last = null;
    private int indent = 6;

    /**
     * Appends the specified text and attaches the ParseEntry to the start position. Adds indention and CRLF
     * 
     * @param txt
     *            a text
     * @param pe
     *            a ParseEntry
     */
    public void writeln(String txt, ParseEntry pe) {
        append(SPACES.substring(0, indent & 63) + txt + "\r\n", pe);
    }

    /**
     * Appends the specified text and attaches the ParseEntry to the start position. Adds indention
     * 
     * @param txt
     *            a text
     * @param pe
     *            a ParseEntry
     */
    public void write(String txt) {
        append(SPACES.substring(0, indent & 63) + txt);
    }

    /**
     * Appends the specified text and attaches the ParsEntry to the start position.
     * 
     * @param txt
     *            a text
     * @param pe
     *            a ParseEntry
     */
    public void append(String txt, ParseEntry pe) {
        if (txt.length() == 0)
            return;

        sb.append(txt);

        if (last == null || last.getParseEntry() != pe) {
            last = new PEPosition(pe, sb.length() - txt.length(), txt.length());
            sizes.add(last);
            return;
        }
        // add length to last untyped length
        last.addLength(txt.length());
    }

    /**
     * @param text
     */
    public void append(String text) {
        append(text, null);
    }

    /**
     * Append all data from specifed MapBuffer into our buffer. This means that offsets must be updated.
     * 
     * @param mb
     */
    public void append(MapBuffer mb) {
        for (Iterator<PEPosition> i = mb.sizes.iterator(); i.hasNext();) {
            PEPosition pep = i.next();
            last = new PEPosition(pep.getParseEntry(), last.getPosition() + last.getLength(), pep.getLength());
            sizes.add(last);
        }
        sb.append(mb.sb);
    }

    /**
     * Retrieve the ParseEntry for the specified offset.
     * 
     * @param offset
     *            the offset.
     * @return a ParseEntry or null.
     */
    public PEPosition getParseEntry(int offset) {
        for (Iterator<PEPosition> i = sizes.iterator(); i.hasNext();) {
            PEPosition pep = i.next();
            if (offset < pep.getPosition()) {
                return pep.getParseEntry() != null ? pep : null;
            }
        }
        return null;
    }

    /**
     * Retrieve the last found ParseEntry for the specified offset.
     * 
     * @param offset
     *            the offset.
     * @return a ParseEntry or null.
     */
    public PEPosition getLastParseEntry(int offset) {
        PEPosition last = null;
        for (Iterator<PEPosition> i = sizes.iterator(); i.hasNext();) {
            PEPosition pep = i.next();
            offset -= pep.getLength();
            if (pep.getParseEntry() != null)
                last = pep;
            if (offset < 0)
                return last;
        }
        return null;
    }

    /**
     * Return the complete String.
     * 
     * @return the complete String.
     */
    public String toString() {
        return sb.toString();
    }

    /**
     * Map the position back to the originating data offset
     * 
     * @param position
     * @return
     */
    public int mapPosition(int position) {
        int pos = 0;
        PEPosition pep = null;
        for (Iterator<PEPosition> i = sizes.iterator(); i.hasNext();) {
            pep = i.next();
            if (pos + pep.getLength() >= position)
                break;
            pos += pep.getLength();
        }
        return pep.getParseEntry().getStart() + position - pos;
    }

    /**
     * ???
     * 
     * @param pe
     * @return
     */
    public int mapPosition(int position, ParseEntry pe) {
        PEPosition last = null;
        for (Iterator<PEPosition> i = sizes.iterator(); i.hasNext();) {
            PEPosition pep = i.next();
            ParseEntry pe2 = pep.getParseEntry();
            if (pe == pe2) {
                last = pep;
                break;
            }
        }
        if (last == null)
            return -1;

        int r = position - pe.getStart() + last.getPosition();
        //    System.out.println(sb.substring(r));
        return r;
    }

    public void indent() {
        indent += 2;
    }

    public void unindent() {
        indent -= 2;
    }

    public static class PEPosition {
        private ParseEntry pe;
        private int position;
        private int length;

        /**
         * @param pe2
         * @param length
         */
        PEPosition(ParseEntry pe, int position, int length) {
            this.pe = pe;
            this.length = length;
            this.position = position;
        }

        /**
         * @return
         */
        public int getLength() {
            return length;
        }

        /**
         * @param i
         */
        void addLength(int i) {
            length += i;
        }

        /**
         * @return
         */
        public ParseEntry getParseEntry() {
            return pe;
        }

        /**
         * @return
         */
        public int getPosition() {
            return position;
        }

        public String toString() {
            return "" + position + ":" + length;
        }
    }
}
