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

package de.bb.util;

/**
 * An implementation of a character based Trie.
 */
public final class Trie {
    // member of each node
    private Trie next, down, up;

    private char character;

    private int id;

    private Object object;

    private int length;

    /**
     * create a public root Trie.
     */
    public Trie() {
    }

    /**
     * create a private Trie element - for internal use only.
     * 
     * @param c
     *            the digit stored in this tree.
     * @param n
     *            the id for this digit or zero when not terminating.
     */
    private Trie(char c) {
        character = c;
    }

    /**
     * Insert a word into current Trie.
     * 
     * @param s
     *            the string which is added.
     * @param newId
     *            the ID
     * @return a Trie object if it was inserted the first time.
     */
    public Trie insert(String s, int newId) {
        char chars[] = new char[s.length()];
        s.getChars(0, chars.length, chars, 0);
        return insert(chars, newId);
    }

    /**
     * Insert a word into current Trie.
     * 
     * @param b
     *            the chars of the string which is added.
     * @param newId
     *            the ID
     * @return a Trie object, if it was inserted the first time.
     */
    public Trie insert(char b[], int newId) {
        if (b.length == 0)
            return this;
        Trie e = doinsert(b, 0); // returns node of the last digit
        if (e.id != 0) // Wort war noch nicht vorhanden: neuen Index vergeben
            return null;
        e.id = newId;
        return e;
    }

    /**
     * Insert a word into current Trie.
     * 
     * @param data
     *            the bytes of the string which is added.
     * @param idx
     *            index to the current digit in the byte array.
     * @return the node for the last digit.
     */
    private Trie doinsert(char data[], int idx) {
        char c = data[idx++];
        Trie e = new Trie(c);

        Trie a = null;
        Trie b = down;
        while (b != null) {
            if (c < b.character) // insert before another element
            {
                if (a == null) // initialize the e.next correctly
                    e.next = down;
                else
                    e.next = a.next;
                break;
            }
            if (c == b.character) // use existing element
            {
                e = b; // no new element, use existing
                break;
            }
            a = b;
            b = b.next;
        }
        if (a == null)
            // insert new element - if it is an existing element, nothin changes
            down = e;
        else
            a.next = e;

        e.length = this.length + 1;
        e.up = this;

        if (idx < data.length) // insert following characters
            return e.doinsert(data, idx);

        return e;
    }

    /**
     * Search a word in current Trie.
     * 
     * @param s
     *            the string which is searched.
     * @return the ID for the given string, or 0 if not found, but longer words exist or -1 if not found, but no longer
     *         words exist
     */
    public Trie search(String s) {
        char chars[] = new char[s.length()];
        s.getChars(0, chars.length, chars, 0);
        return search(chars);
    }

    /**
     * Search a word in current Trie.
     * 
     * @param b
     *            the characters of the string which is searched.
     * @return the ID for the given string, or 0 if not found, but longer words exist or -1 if not found, but no longer
     *         words exist
     */
    public Trie search(char b[]) {
        if (b.length == 0)
            return this;
        return search(b, 0);
    }

    /**
     * Search a word in current Trie.
     * 
     * @param data
     *            the bytes of the string which is searched.
     * @param idx
     *            index to the current digit in the byte array.
     * @return the ID for the given string, or 0 if not found, but longer words exist or -1 if not found, but no longer
     *         words exist
     */
    private Trie search(char data[], int idx) {
        char c = data[idx++];
        Trie b = down;
        while (b != null) {
            if (c < b.character) // c not included
                return null;
            if (c == b.character) // found c
            {
                if (data.length == idx)
                    return b; // zero or id
                return b.search(data, idx);
            }
            b = b.next;
        }
        return null;
    }

    /**
     * Search the last node in the path where the id fits an id in a node.
     * 
     * @param path
     *            the path
     * @param id
     *            the id to search
     * @return the Trie if found.
     */
    public Trie searchLast(String path, int id) {
        int len = path.length();
        if (len == 0)
            return null;
        char chars[] = new char[len];
        path.getChars(0, chars.length, chars, 0);

        int index = 0;
        Trie b = down;
        Trie last = null;
        char c = chars[index++];
        while (b != null) {
            if (c < b.character) // c not included
                break;
            if (c == b.character) // found c
            {
                if (b.id == id)
                    last = b;
                if (index == len)
                    break;
                c = chars[index++];
                b = b.down;
                continue;
            }
            b = b.next;
        }
        return last;
    }

    /**
     * Remove a word from current Trie.
     * 
     * @param s
     *            the string which is removed.
     * @return the ID for the given string, or 0 if not found.
     */
    public int remove(String s) {
        char ch[] = new char[s.length()];
        s.getChars(0, ch.length, ch, 0);
        return remove(ch);
    }

    /**
     * Remove a word from current Trie.
     * 
     * @param b
     *            the characters of the string which is removed.
     * @return the ID for the given string, or 0 if not found.
     */
    public int remove(char b[]) {
        if (b.length == 0)
            return 0;
        int l[] = {0};
        remove(b, 0, l);
        return l[0];
    }

    /**
     * Remove a word from current Trie.
     * 
     * @param data
     *            the characters of the string which is removed.
     * @param idx
     *            index to the current digit in the byte array.
     * @param oldId
     *            an array of one int for additional return value.
     * @return the ID for the given string, or 0 if not found.
     */
    private boolean remove(char data[], int idx, int oldId[]) {
        // last byte reached?
        if (data.length == idx) {
            oldId[0] = id;
            id = 0; // remove id
            return down == null; // return true, when this node can be removed
        }

        char c = data[idx++];
        Trie a = null;
        Trie b = down;
        while (b != null) {
            if (c < b.character)
                return false; // not found
            if (c == b.character) // got a matching letter
            {
                boolean f = b.remove(data, idx, oldId); // true if b can be removed
                if (f) // remove b -> check can this be removed?
                {
                    // this has no other children, and is not terminating?
                    if (a == null && b.next == null && id == 0)
                        return true;
                    // either there are other children, or this is terminating
                    // remove b only
                    if (a == null)
                        down = b.next;
                    else
                        a.next = b.next;
                }
                return false;
            }
            a = b;
            b = b.next;
        }
        return false; // not found
    }

    /**
     * count the number of words in the current tree.
     * 
     * @return the number of words
     */
    public int count() {
        int cnt = 0;
        if (id > 0) // Wortende gefunden
            cnt++;
        if (down != null) // es gibt weitere W???rter, die mit 'this' beginnen
            cnt += down.count();
        if (next != null) // durchlaufe alle Belegungen des letzten Buchstabens
            cnt += next.count();
        return cnt;
    }

    /**
     * Step one digit down in current Trie.
     * 
     * @param c
     *            character to step
     * @return the Trie for character, or null if not found.
     */
    public Trie step(int c) {
        Trie b = down;
        while (b != null) {
            if (c < b.character) // c not included
                return null;
            if (c == b.character) // found c
                return b;

            b = b.next;
        }
        return null;
    }

    /**
     * return current id.
     * 
     * @return the id of this Trie element.
     */
    public int getId() {
        return id;
    }

    /**
     * reconstruct the word.
     * 
     * @return a newly allocated byte array containing the word
     */
    public char[] toCharArray() {
        // count the length;
        int len = length();
        char b[] = new char[len];
        for (Trie t = this; t.up != null; t = t.up)
            b[--len] = t.character;
        return b;
    }

    /**
     * Reconstruct the word.
     * 
     * @return a newly allocated String containing the word
     */
    public String getString() {
        return new String(toCharArray());
    }

    /**
     * Get own character.
     * 
     * @return owned character.
     */
    public char getChar() {
        return character;
    }

    /**
     * get the parent Trie.
     * 
     * @return the parent Trie
     */
    public Trie getParent() {
        return up;
    }

    /**
     * Assign an object.
     * 
     * @param object
     *            the assigned object.
     * @see #getObject
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * Get the assigend object.
     * 
     * @return the assigned object.
     * @see #setObject
     */
    public Object getObject() {
        return object;
    }

    /**
     * Return the length of the path for the current Trie.
     * 
     * @return the length of the path for the current Trie.
     */
    public int length() {
        return length;
    }

}
