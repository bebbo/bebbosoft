/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/Scanner.java,v $
 * $Revision: 1.3 $
 * $Date: 2013/11/28 12:24:10 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bex2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author bebbo
 */
public class Scanner {
    protected String text;

    private int begin;
    private int end;

    private int mark;
    protected int position;

    // private int lineCount;
    // private int markLineCount;

    private HashMap<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Create a Scanner for Strings.
     * 
     * @param text
     *            the String
     */
    public Scanner(String text) {
        this.text = text;
        end = text.length();
    }

    /**
     * store current position.
     */
    public void mark() {
        mark = position;
        // markLineCount = lineCount;
    }

    /**
     * Checks whether current data matches the pattern.
     * 
     * @param match
     *            the matched string
     * @return true if match matches current data
     */
    public boolean match(String match) {
        return text.substring(position).startsWith(match);
    }

    /**
     * Checks whether current data matches the pattern.
     * 
     * @param match
     *            the matched string
     * @return true if match matches current data
     */
    public boolean isWord(String match) {
        if (!text.substring(position).startsWith(match))
            return false;
        move(match.length());
        return true;
    }

    /**
     * Checks whether current data matches the pattern.
     * 
     * @param match
     *            the matched string
     * @return true if match matches current data
     */
    public boolean isWord(char [] match) {
        if (match.length + position > text.length())
            return false;
        for (int i = 0; i < match.length; ++i) {
            if (text.charAt(position + i) != match[i])
                return false;
        }
        move(match.length);
        return true;
    }

    /**
     * move the current position by the specified value.
     * 
     * @param delta
     *            where to move current position to.
     */
    public void move(int delta) {
        position += delta;
        if (delta > 0) {
            if (position > end)
                position = end;
        } else {
            if (position < begin)
                position = begin;
        }
    }

    /**
     * Get current value without moving the position.
     * 
     * @return the next value.
     */
    public int peek() {
        if (position == end)
            return -1;
        try {
            return text.charAt(position);
        } catch (Throwable e) {
        }
        return -1;
    }

    /**
     * Get (current + n) value without moving the position.
     * 
     * @return the over next value.
     */
    public int peek(int n) {
        int p = position + n;
        if (p >= end || p < begin)
            return -1;
        return text.charAt(p);
    }

    public int read() {
        if (position == end)
            return -1;
        return text.charAt(position++);
        // int ch = text.charAt(position++);
        // if (ch == '\n') ++ lineCount;
        // return ch;
    }

    /**
     * reset to marked position.
     */
    public void reset() {
        position = mark;
        // lineCount = markLineCount;
    }

    /**
     * Unreads the last read character, by moving the current position back.
     */
    public void unread() {
        if (position > begin)
            --position;
    }

    /**
     * 
     * @param offset
     * @param length
     * @return
     */
    public String get(int offset, int length) {
        return text.substring(offset, offset + length);
    }

    /**
     * @param offset
     * @param length
     * @return
     */
    public String getToScanPos(int offset) {
        return text.substring(offset, position);
    }

    /**
     * 
     * @param offset
     * @param length
     * @return
     */
    public String substring(int start, int end) {
        return text.substring(start, end);
    }

    /**
     * Returns a String from specified offset to current position.
     * 
     * @param offset
     *            an offset
     * @return a String from specified offset to current position.
     */
    public String get(int offset) {
        return text.substring(offset, position);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return showRange(position);
    }

    public String showRange(int there) {
        String content = text;
        int from = content.lastIndexOf('\n', there);
        while (from > 0 && content.substring(from, there).trim().length() == 0) {
            from = content.lastIndexOf('\n', from - 1);
        }
        if (from < 0)
            from = 0;
        int to = content.indexOf('\n', there);
        if (to > 0) {
            int to2 = content.indexOf('\n', to + 1);
            if (to2 > to)
                to = to2;
        }
        if (from > there)
            from = there;
        if (to < there)
            to = content.length();

        return text.substring(from, there) + "^" + text.substring(there, to);
    }

    /**
     * @return
     */
    public int getPosition() {
        return position;
    }

    /**
     * @return
     */
    public int getPosition(int offset) {
        return position + offset;
    }

    /**
     * Return the next index of the search string starting at current position.
     * 
     * @param string
     *            the search string.
     * @return the position or -1 if not found.
     */
    public int indexOf(String string) {
        return text.indexOf(string, position);
    }

    /**
     * Return the last index of the search string starting at specified position.
     * 
     * @param string
     *            the search string.
     * @param pos
     *            the position to search from
     * @return the position or -1 if not found.
     */
    public int lastIndexOf(String string, int pos) {
        return text.lastIndexOf(string, pos);
    }

    /**
     * Return the last index of the search string starting at end.
     * 
     * @param string
     *            the search string.
     * @return the position or -1 if not found.
     */
    public int lastIndexOf(String string) {
        return text.lastIndexOf(string);
    }

    /**
     * checks whether current data matches the pattern.
     * 
     * @param chars
     *            tha pattern
     * @return true if pattern matches current data
     */
    public boolean match(char[] chars) {
        if (chars == null)
            return false;

        if (position + chars.length > end)
            return false;

        int p = position;
        for (int i = 0; i < chars.length; ++i, ++p) {
            if (chars[i] != text.charAt(p))
                return false;
        }
        return true;
    }

    /**
     * Store an additional attribute in the scanner.
     * 
     * @param name
     *            name of the attribute.
     * @param value
     *            value of the attribute.
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Retrieve an attribute.
     * 
     * @param name
     *            name of the attribute.
     * @return value of the attribute if an ttribute was set.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Read a file and return full content as String.
     * 
     * @param fileName
     *            the file name
     * @return file content as String
     * @throws IOException
     *             on error
     */
    public static String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        StringBuffer sb = new StringBuffer();
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * returns true if EOF is reached.
     * 
     * @return true if EOF is reached.
     */
    public boolean isEof() {
        return position >= end;
    }

    /**
     * @param position
     * @return
     */
    public int charAt(int position) {
        return text.charAt(position);
    }

    /**
     * Return the line number, starting with 1.
     * 
     * @param offset
     *            offset into text.
     * @return the line number starting with 1
     */
    public int getLineFromOffset(int offset) {
        String part = text.substring(0, offset);
        int lines = 1;
        for (int i = part.indexOf('\n'); i >= 0; i = part.indexOf('\n', i + 1)) {
            ++lines;
        }
        return lines;
    }

    /**
     * @return Returns the lineCount.
     */
    // public int getLineCount()
    // {
    // return lineCount;
    // }

    public int getLength() {
        return end;
    }

}

/******************************************************************************
 *
 */
