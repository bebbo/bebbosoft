/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/XmlFile.java,v $
 * $Revision: 1.44 $
 * $Date: 2014/06/23 19:12:08 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * XmlFile handling similar to the good old Windows INI file.  
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Provides the functionality to use and maintain XML files. This implementation reads the file and keeps all data in
 * memory.<br> Each tag corresponds to a section, with the complete path to a tag and the tag's name itself as section
 * name.
 * 
 * <pre> &lt;foo&gt; &lt;bar attr="value" /&gt; &lt;/foo&gt; </pre>
 * 
 * corresponds to the sections <ul> <li>/foo/</li> <li>/foo/bar/</li> </ul> and one readable value.
 * 
 * <pre> getString(&quot;/foo/bar&quot;, &quot;attr&quot;, null); </pre>
 * 
 * returns "value" <br> A speciality is the handling of duplicate tags:
 * 
 * <pre> &lt;foo&gt; &lt;bar attr="value" /&gt; &lt;bar attr="other" /&gt; &lt;/foo&gt; </pre>
 * 
 * Since there seems no way to have access to one of the "/foo/bar" tags, an autonumbering method is used, which adds an
 * unqiue number to all duplicate tags. Better than guessing the assigned numbers, is to use the
 * <code>getSections()</code> method which returns a Vector with all valid paths:
 * 
 * <pre> getSections(&quot;/foo/bar&quot;) </pre>
 * 
 * will return something like
 * 
 * <pre> /foo/bar#000000/ /foo/bar#000001/ </pre>
 * 
 * Using that paths enables the direct access again. <br> Last feature is a mechanism called 'access by name attribute'.
 * 
 * <pre> &lt;foo&gt; &lt;bar name="joe" attr="value" /&gt; &lt;bar name="carl" attr="other" /&gt; &lt;/foo&gt; </pre>
 * 
 * To access a named tag directly just use a special encoding:
 * 
 * <pre> getString(&quot;/foo/\\bar\\carl&quot;, &quot;attr&quot;, null); </pre>
 * 
 * returns "other".<br> The tag name is escaped within in '\\' and the name value is added. This also works for nested
 * named tags! <br> File parts with <ul> <li>"&lt;? ... &gt;"</li> <li>"&lt;! ... &gt;" and</li>
 * <li>"&lt;!-- ... --&gt;"</li> </ul> are ignored but kept for output.
 */
public class XmlFile {
    private final static boolean DEBUG = false;

    final static ByteRef COMMENT_START = new ByteRef("<!--");

    final static ByteRef COMMENT_END = new ByteRef("-->");

    final static ByteRef CDATA_START = new ByteRef("<![CDATA[");

    final static ByteRef CDATA_END = new ByteRef("]]>");

    final static ByteRef SEPARATOR = new ByteRef("/");

    final static byte ENDTAG[] = { (byte) '<', (byte) '/' };

    final static byte CLOSECRLF[] = { (byte) '>', 0xd, 0xa };

    final static byte CRLF[] = { 0xd, 0xa };

    // final static ByteRef BRCRLF = new ByteRef(CRLF);

    private final static byte SPACES[] = new byte[256];

    private static final ByteRef XML = new ByteRef("<?xml");

    private static final ByteRef ENCODING = new ByteRef("encoding");

    static {
        for (int i = 0; i < SPACES.length; ++i)
            SPACES[i] = 32;
    }

    void indent(int n, OutputStream os) throws IOException {
        if (preserveWhiteSpaces)
            return;
        while (n > SPACES.length) {
            os.write(SPACES);
            n -= SPACES.length;
        }
        os.write(SPACES, 0, n);
    }

    /**
     * Every XML file consist from tags. The file itself corresponds to the root tag "/". All tags on that level are
     * held in the root tag, where each tags again holds its own tags. Based on that recursion the complete tree is held
     * in memory.
     * 
     */
    private class Tag {
        // / my parent
        Tag parent;

        // / name if this tag
        String name, key;

        // / its attributes
        Map attributes;

        // / keep all content by order - contains tags AND ByteRef comments
        ArrayList allByOrder = new ArrayList();

        // / quick acces by name, with duplicates
        MultiMap tagsByName = new MultiMap();

        // / body contents
        Vector contents = new Vector();

        Tag(String n, Map attr) {
            key = name = n;
            attributes = attr;
        }

        Tag(String n) {
            this(n, new SingleMap());
        }

        void addContent(ByteRef ct) {
            allByOrder.add(ct);
            contents.add(ct);
        }

        void add(Tag tag) {
            tag.parent = this;
            allByOrder.add(tag);
            // tagsByOrder.add(tag);

            /**/
            Iterator<String> i = tagsByName.subMap(tag.name, tag.name + "#999999").keySet().iterator();

            if (i.hasNext()) {
                String key = (String) i.next();
                if (key.equals(tag.name)) {
                    Tag to = (Tag) tagsByName.remove(key);
                    to.key = to.name + "#" + i2s(tagsByName.size());
                    tagsByName.put(to.key, to);
                }
                tag.key = tag.name + "#" + i2s(tagsByName.size());
                tagsByName.put(tag.key, tag);
            } else
                tagsByName.put(tag.name, tag);
            /**/
            // tagsByName.put(tag.name + "#" + i2s(tagsByName.size()), tag);
            Object n = tag.attributes.get("name");
            if (n != null)
                tagsByName.put("\\" + tag.name + '\\' + n, tag);
        }

        /**
         * Method getPath.
         * 
         * @return String
         */
        String getPath() {
            return getPath("/");
        }

        /**
         * Method getPath.
         * 
         * @param path
         * @return String
         */
        private String getPath(String path) {
            if (parent == null) {
                return path;
            }

            for (Iterator i = parent.tagsByName.tailMap(name).entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String key = (String) e.getKey();
                if (!key.startsWith(name))
                    break;
                Tag t = (Tag) e.getValue();
                if (t == this) {
                    return parent.getPath("/" + key + path);
                }
            }
            return parent.getPath("/" + name + path);
        }

        private ByteRef getContentBr() {
            ByteRef r = new ByteRef();
            // for (Enumeration e = contents.elements(); e.hasMoreElements();) {
            for (Iterator i = allByOrder.iterator(); i.hasNext();) {
                // if (!this.preserveWhiteSpaces && r.length() > 0)
                // r = r.append(BRCRLF);
                // r = r.append((ByteRef) e.nextElement());
                Object o = i.next();
                if (o instanceof ByteRef) {
                    ByteRef br = (ByteRef) o;
                    if (!br.startsWith(COMMENT_START))
                        r = r.append(br);
                } else {
                    Tag t = (Tag) o;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        t.write(bos, 0, "utf-8");
                    } catch (IOException e) {
                    }
                    r = r.append(new ByteRef(bos.toByteArray()));
                }
            }
            return r;
        }

        byte[] getContent() {
            return getContentBr().toByteArray();
        }

        void setContent(ByteRef r) {
            if (tagsByName.size() > 0)
                throw new IllegalStateException("cant set content, if child tags are present");
            contents.clear();
            allByOrder.clear();
            addContent(r);
        }

        /**
         * remove the content from the section, but do not remove child tags. Result is an empty but still existing
         * section.
         */
        void clearContent() {
            attributes.clear();
            // remove from allByOrder
            for (int i = 0; i < allByOrder.size();) {
                Object o = allByOrder.get(i);
                if (o instanceof ByteRef || o instanceof String) {
                    allByOrder.remove(i);
                    continue;
                }
                ++i;
            }
        }

        /**
         * remove the complete section.
         */
        void removeMe() {
            parent.remove(this);
        }

        void remove(Tag tag) {
            // remove from allByOrder
            for (int i = 0; i < allByOrder.size();) {
                Object o = allByOrder.get(i);
                if (o == tag) {
                    allByOrder.remove(i);
                    break;
                }
                ++i;
            }
            // removefrom tagsByName
            for (Iterator i = tagsByName.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                XmlFile.Tag t = (XmlFile.Tag) entry.getValue();
                if (t == tag) {
                    tagsByName.remove(entry.getKey(), t);
                    break;
                }
            }
        }

        void remove(String name) {
            // remove from allByOrder
            for (int i = 0; i < allByOrder.size();) {
                Object o = allByOrder.get(i);
                if (o instanceof XmlFile.Tag) {
                    XmlFile.Tag t = (XmlFile.Tag) o;
                    if (name.equals(t.name)) {
                        allByOrder.remove(i);
                        continue;
                    }
                }
                ++i;
            }
            // removefrom tagsByName
            for (Iterator i = tagsByName.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                XmlFile.Tag t = (XmlFile.Tag) entry.getValue();
                if (name.equals(t.name))
                    tagsByName.remove(entry.getKey(), t);
            }
        }

        void writeRoot(OutputStream os, String encoding) throws IOException {
            for (Iterator<?> e = allByOrder.iterator(); e.hasNext();) {
                Object o = e.next();
                if (o instanceof ByteRef) {
                    ((ByteRef) o).writeTo(os);
                    if (!preserveWhiteSpaces)
                        os.write(CRLF);
                } else if (o instanceof String) {
                    ByteRef r = encode((String) o, encoding);
                    r.writeTo(os);
                    if (!preserveWhiteSpaces)
                        os.write(CRLF);
                } else if (o instanceof XmlFile.Tag) {
                    ((XmlFile.Tag) o).write(os, 0, encoding);
                }
            }
        }

        void write(OutputStream os, int n, String encoding) throws IOException {
            byte[] nb = name.getBytes();

            indent(n, os);
            os.write('<');
            os.write(nb);

            for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                os.write(' ');
                os.write(((String) e.getKey()).getBytes());
                os.write('=');
                os.write('"');
                ByteRef r = encode((String) e.getValue(), encoding);
                r.writeTo(os);
                os.write('"');
            }

            int sz = allByOrder.size();
            if (sz > 0) {
                boolean lf = !preserveWhiteSpaces && (sz > 1 || !(allByOrder.get(0) instanceof ByteRef));

                if (lf)
                    os.write(CLOSECRLF);
                else
                    os.write('>');

                for (Iterator e = allByOrder.iterator(); e.hasNext();) {
                    Object o = e.next();
                    if (o instanceof CData) {
                        CDATA_START.writeTo(os);
                        ((ByteRef) o).writeTo(os);
                        CDATA_END.writeTo(os);
                    } else if (o instanceof ByteRef) {
                        if (lf)
                            indent(n + 2, os);
                        ((ByteRef) o).writeTo(os);
                        if (lf)
                            os.write(CRLF);
                    } else if (o instanceof String) {
                        ByteRef r = encode((String) o);
                        // indent(n + 2, os);
                        r.writeTo(os);
                        // os.write(CRLF);
                    } else if (o instanceof XmlFile.Tag) {
                        ((XmlFile.Tag) o).write(os, n + 2, encoding);
                    }
                }
                if (lf)
                    indent(n, os);
                os.write(ENDTAG);
                os.write(nb);
            } else {
                os.write('/');
            }
            if (preserveWhiteSpaces)
                os.write('>');
            else
                os.write(CLOSECRLF);
        }

        String getKey() {
            return key;
        }

        void sort(String order) {
            HashSet drop = new HashSet();
            drop.addAll(tagsByName.keySet());
            ArrayList newOrder = new ArrayList();
            for (StringTokenizer st = new StringTokenizer(order, ", ;\r\n\t"); st.hasMoreElements();) {
                String token = st.nextToken();
                for (Iterator i = tagsByName.subMap(token, token + "#\255").entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry) i.next();
                    String key = (String) e.getKey();
                    drop.remove(key);
                    XmlFile.Tag tag = (XmlFile.Tag) e.getValue();
                    newOrder.add(tag);
                }
            }

            for (Iterator i = drop.iterator(); i.hasNext();) {
                String key = (String) i.next();
                tagsByName.remove(key);
            }

            allByOrder = newOrder;
        }

        public String toString() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                write(bos, 0, "utf-8");
            } catch (IOException e) {
            }
            return bos.toString();
        }
    }

    private static class CData extends ByteRef {
        public CData(ByteRef r) {
            this.assign(r);
        }
    }

    private XmlFile.Tag root = null;

    // / the file - if any
    private File file;

    private String encoding;

    private boolean preserveWhiteSpaces;

    /**
     * construct an XmlFile object.
     * 
     * @param fileName
     *            name of the used XML file
     * @deprecated use XmlFile() and readFile()
     */
    public XmlFile(String fileName) {
        root = new XmlFile.Tag("/", new SingleMap());
        setFile(fileName);
        readFile(fileName);
    }

    /**
     * construct an XmlFile object without an file.
     */
    public XmlFile() {
        root = new XmlFile.Tag("/", new SingleMap());
        file = null;
    }

    /**
     * read the specified xml file.
     * 
     * @param fileName
     *            the file name
     */
    public void readFile(String fileName) {
        // read the file
        setFile(fileName);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis, 0x8000);
            read(bis);
        } catch (Exception e) {
        } finally {
            if (bis != null)
                try {
                    bis.close();
                } catch (IOException e1) {
                }

        }
    }

    /**
     * Read the xml content from the given String.
     * 
     * @param content
     *            the xml content.
     */
    public void readString(String content) {
        try {
            byte b[] = content.getBytes("utf-8");
            ByteArrayInputStream bis = new ByteArrayInputStream(b);
            this.encoding = "utf-8";
            this.read(bis);
        } catch (UnsupportedEncodingException e) {
        }
    }

    /**
     * set a new file name.
     * 
     * @param fileName
     *            the new file name
     */
    public void setFile(String fileName) {
        file = new File(fileName);
    }

    /**
     * extract a complete tag from given ByteRef buffer <ul> <li>the content</li> <li>or the tag.</li> </ul> The buffer
     * is modified to hold only the rest of data (without tag). or null, if buffer did not contain a complete tag.
     * 
     * @param br
     *            current buffer to parse
     * @return the parsed tag or null, when buffer contains incomplete data
     */
    private ByteRef parseTag(ByteRef br) {
        int start = br.indexOf('<');
        if (start < 0)
            return null;

        if (start > 0) {
            ByteRef r = br.splitLeft(start);
            if (!preserveWhiteSpaces)
                r = r.trim();
            return r;
        }

        if (br.startsWith(COMMENT_START)) {
            int cend = br.indexOf(COMMENT_END);
            if (cend < 0)
                return null;
            cend += 3;
            // ByteRef r = br.substring(start, cend);
            // br.assign(br.substring(cend));
            ByteRef r = br.splitLeft(start, cend);
            // System.out.println(r.toString());
            return r;
        }

        if (br.startsWith(CDATA_START)) {
            int cend = br.indexOf(CDATA_END);
            if (cend < 0)
                return null;
            cend += 3;
            // ByteRef r = br.substring(start, cend);
            // br.assign(br.substring(cend));
            ByteRef r = br.splitLeft(start, cend);
            // System.out.println(r.toString());
            r = r.substring(9, r.length() - 3);
            CData cd = new CData(r);
            return cd;
        }

        int stop = start;
        for (;;) {
            int ch = br.charAt(++stop);
            if (ch < 0)
                return null;
            if (ch == '>')
                break;
            if (ch == '"' || ch == '\'') {
                for (;;) {
                    int ch2 = br.charAt(++stop);
                    if (ch2 < 0)
                        return null;
                    if (ch2 == ch)
                        break;
                }
            }
        }
        /*
         * int stop, seek = start; for (;;) { stop = br.indexOf('>', seek); if (stop < 0) return null;
         * 
         * int q1Pos = br.indexOf('\'', seek) & 0x7fffffff; int q2Pos = br.indexOf('"', seek) & 0x7fffffff; if (q1Pos ==
         * q2Pos) break;
         * 
         * int ch; if (q1Pos < q2Pos) { seek = q1Pos; ch = '\''; } else { seek = q2Pos; ch = '"'; } if (seek > stop)
         * break;
         * 
         * seek = br.indexOf(ch, seek + 1); if (seek < 0) return null; ++seek; }
         */
        ++stop;
        ByteRef r = br.splitLeft(start, stop);
        // ByteRef r = br.substring(start, stop);
        // br.assign(br.substring(stop));
        // System.out.println(r.toString());
        return r;
    }

    /**
     * Extract all attributes from the specified tag.
     * 
     * @param tag
     *            a ByteRef containing exact one tag.
     * @return A new Map with all attributes.
     */
    private Map getAttrs(ByteRef tag) {
        // System.out.println("in: " + tag);
        Map hrp = new SingleMap();
        for (;;) {
            int pos = tag.indexOf('=');
            if (pos < 0)
                break;

            ByteRef name = tag.substring(0, pos).trim();

            int q1Pos = tag.indexOf('\'', pos) & 0x7fffffff;
            int q2Pos = tag.indexOf('"', pos) & 0x7fffffff;
            if (q1Pos == q2Pos)
                break;

            int ch;
            if (q1Pos < q2Pos) {
                pos = q1Pos;
                ch = '\'';
            } else {
                pos = q2Pos;
                ch = '"';
            }
            ++pos;
            int end = tag.indexOf(ch, pos);
            if (end < 0)
                break;
            ByteRef value = tag.substring(pos, end);
            tag.assign(tag.substring(end + 1).trim());

            // System.out.println("attr: " + name + "='" + value + "'");
            // System.out.println("rest: " + tag);

            hrp.put(name.toString(), decode(value, encoding));
        }

        return hrp;
    }

    /**
     * Remove all content from this instance.
     */
    public void clear() {
        root = new XmlFile.Tag("/", new SingleMap());
    }

    /**
     * Read and parse the specified intput stream. Appends data to current instance.
     * 
     * @param is
     *            n input stream.
     */
    public void read(InputStream is) {
        try {
            // buffers
            ByteRef br = new ByteRef();
            ByteRef l = br.update(is);
            XmlFile.Tag here = root;
            Stack stack = new Stack();

            while (l != null) {
                // find next tag
                ByteRef tag = parseTag(br);
                if (tag == null) {
                    l = br.update(is);
                    continue;
                }
                // no tag?
                if (tag instanceof CData || tag.charAt(0) != '<') {
                    if (tag.length() > 0)
                        here.addContent(tag);
                    continue;
                }

                // comment tag?
                if (tag.charAt(1) == '!') {
                    if (tag.startsWith(COMMENT_START)) {
                        // exact one comment
                        if (tag.endsWith(COMMENT_END)) {
                            here.allByOrder.add(tag);
                            continue;
                        }
                        System.out.println("unterminated comment!");
                        continue;
                    }

                    if (tag.startsWith(CDATA_START)) {
                        // exact one comment
                        if (tag.endsWith(CDATA_END)) {
                            here.addContent(tag.substring(9, tag.length() - 3));
                            continue;
                        }
                        System.out.println("unterminated CDATA!");
                        continue;
                    }
                }
                if (tag.charAt(1) == '!' || tag.charAt(1) == '?') {
                    here.allByOrder.add(tag);
                    if (tag.startsWith(XML)) {
                        setEncoding(tag);
                    }
                    continue;
                }

                tag = tag.substring(1, tag.length() - 1).trim();

                // closing tag?
                if (tag.charAt(0) == '/') {
                    tag = tag.substring(1).trim();

                    if (!tag.equals(here.name))
                        System.out.println("close tag mismatch! expected: " + here.name + " found: " + tag + " near "
                                + here.getPath());
                    here = (XmlFile.Tag) stack.pop();

                    continue;
                }

                // normal tag
                ByteRef tagName = tag.nextWord();
                // if (DEBUG) System.out.println("tagName: " + tagName);

                tag = tag.trim();
                int sp = tagName.indexOf('/');
                if (sp > 0) {
                    // System.out.println("tagName: " + tagName);
                    tag = tag.append(" ").append(tagName.substring(sp)).trim();
                    tagName = tagName.substring(0, sp);
                }

                XmlFile.Tag neu = new XmlFile.Tag(tagName.toString(), getAttrs(tag));
                here.add(neu);
                stack.push(here);
                here = neu;

                if (tag.charAt(0) == '/') {
                    here = (XmlFile.Tag) stack.pop();
                }
            }

            if (stack.size() != 0)
                System.out.println("missing closing tag: " + here.name);

            // fix duplicate names
            // fix(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setEncoding(ByteRef tag) {
        int pos = tag.indexOf(ENCODING);
        if (pos < 0)
            return;

        pos += 8;
        while (pos < tag.length() && tag.charAt(pos) != '=') {
            ++pos;
        }
        if (pos == tag.length())
            return;

        while (pos < tag.length() && tag.charAt(pos) != '"' && tag.charAt(pos) != '\'') {
            ++pos;
        }
        if (pos == tag.length())
            return;
        int quote = tag.charAt(pos++);
        int start = pos;
        while (pos < tag.length() && tag.charAt(pos) != quote) {
            ++pos;
        }
        if (pos == tag.length())
            return;
        encoding = tag.substring(start, pos).toString();
    }

    /**
     * Fast conversion from int to 6 digit String
     * 
     * @param n
     *            an integer
     * @return a String
     */
    static String i2s(int n) {
        char c[] = new char[6];
        for (int i = 5; i >= 0; --i) {
            c[i] = (char) ((n % 10) + '0');
            n /= 10;
        }
        return new String(c);
    }

    /*
     * private void fix(XmlFile.Tag here) { String last = ""; int count = 0; for (Iterator i =
     * here.tagsByName.keySet().iterator(); i.hasNext();) { String key = (String)i.next(); if (!key.equals(last)) { last
     * = key; count = 0; } else { if (count == 0) { XmlFile.Tag o = (XmlFile.Tag)here.tagsByName.get(key);
     * here.tagsByName.remove(key, o); // here.tagsByName.remove(key); here.tagsByName.put(key + '#' + i2s(count++), o);
     * } XmlFile.Tag o = (XmlFile.Tag)here.tagsByName.get(key); // if (!o.name.equals(key)) // throw new
     * RuntimeException("mismatch: " + o.name +"!=" + key); here.tagsByName.remove(key, o); key = key + '#' +
     * i2s(count++); here.tagsByName.put(key, o); } fix((XmlFile.Tag)here.tagsByName.get(key)); } }
     */
    /**
     * Write all internal data to the file.
     */
    public void flush() {
        try {
            File out = new File(file + ".$$$");
            FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream bos = new BufferedOutputStream(fos, 0x8000);

            write(bos);
            bos.flush();
            // close file
            fos.close();

            // delete org rename new
            file.delete();
            out.renameTo(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the current XML file to given OutputStream.
     * 
     * @param os
     *            the OutputStream
     * @throws IOException
     *             on error
     */
    public void write(OutputStream os) throws IOException {
        root.writeRoot(os, encoding);
    }

    private XmlFile.Tag getTag(String path) {
        path = path.substring(1);
        XmlFile.Tag here = root;
        for (int slash = path.indexOf('/'); here != null && path.length() > 0; slash = path.indexOf('/')) {
            String name = slash >= 0 ? path.substring(0, slash) : path;
            path = slash >= 0 ? path.substring(slash + 1) : "";
            Iterator i = here.tagsByName.subMap(name, name + "#999999").values().iterator();
            if (i.hasNext())
                here = (XmlFile.Tag) i.next();
            else
                here = null;
        }
        return here;
    }

    private XmlFile.Tag createTag(String path) {
        if (path.length() == 0)
            return root;
        path = path.substring(1);
        XmlFile.Tag here = root;
        for (int slash = path.indexOf('/'); here != null && path.length() > 0; slash = path.indexOf('/')) {
            String name = slash >= 0 ? path.substring(0, slash) : path;
            path = slash >= 0 ? path.substring(slash + 1) : "";
            XmlFile.Tag neu = (XmlFile.Tag) here.tagsByName.get(name);
            if (neu == null) {
                String at = null;
                if (name.charAt(0) == '\\') {
                    name = name.substring(1);
                    int bs = name.indexOf('\\');
                    at = name.substring(bs + 1);
                    name = name.substring(0, bs);
                }
                neu = new XmlFile.Tag(name);
                if (at != null)
                    neu.attributes.put("name", at);
                here.add(neu);
            }
            here = neu;
        }
        return here;
    }

    /**
     * Get all sections for the given XML file, matching the path String. So "/foo/a" would match "/foo/all" and
     * "/foo/any" but not "/foo/be". A path ending with '/' will return all contained elements.
     * 
     * @param path
     *            the match String
     * @return a vector of String with all matched section names.
     */
    public Vector<String> getSections(String path) {
        if (DEBUG)
            System.out.println("getSections(" + path + ")");
        Vector<String> v = new Vector<String>();
        if (path.length() == 0 || path.charAt(0) != '/')
            return v;
        int sl = 1 + path.lastIndexOf('/');
        String opath = path.substring(0, sl);
        path = path.substring(sl);
        XmlFile.Tag here = getTag(opath);
        if (here == null)
            return v;

        boolean wildStar = path.endsWith("*");
        if (wildStar)
            path = path.substring(path.length() - 1);

        for (Iterator<Object> e = here.allByOrder.iterator(); e.hasNext();) {
            Object o = e.next();
            if (o instanceof XmlFile.Tag) {
                XmlFile.Tag tag = (XmlFile.Tag) o;
                String key = tag.getKey();
                if ((path.length() == 0 && key.charAt(0) != '\\')
                        || (path.length() != 0 && key.startsWith(path) && (wildStar || key.length() == path.length() || key
                                .charAt(path.length()) == '#'))) {
                    v.add(opath + key + '/');
                }
            }
        }
        /*
         * for (Iterator i = here.tagsByName.keySet().iterator(); i.hasNext();) { String key = (String) i.next(); //
         * System.out.println("K: " + key + "[" +(((XmlFile.Tag)here.tagsByName.get(key)).name) + "] *= P: " + path); if
         * ((path.length() == 0 && key.charAt(0) != '\\') || (path.length() != 0 && key.startsWith(path) && (wildStar ||
         * key.length() == path.length() || key.charAt(path.length()) == '#'))) { v.add(opath + key + '/'); } }
         */
        return v;

    }

    /**
     * get all attribute names (keys) for a section in the given XML file.
     * 
     * @param section
     *            the name of the section *
     * @return a vector of String with all attribute names (keys)
     */
    public Vector<String> getKeys(String section) {
        XmlFile.Tag here = getTag(section);
        if (here == null)
            return new Vector<String>();

        Vector<String> v = new Vector<String>(here.attributes.size());
        for (Iterator<String> i = here.attributes.keySet().iterator(); i.hasNext();)
            v.add(i.next());
        return v;
    }

    /**
     * Return a map with all attributes: name-&gt;value.
     * 
     * @param section
     *            the name of the section.
     * @return a map containing the attributes, or null if section does not exist.
     */
    public Map<String, String> getAttributes(String section) {
        XmlFile.Tag here = getTag(section);
        if (here == null)
            return null;
        return Collections.unmodifiableMap(here.attributes);
    }

    /**
     * get a value for an attribute in the section for the given XML file.
     * 
     * @param section
     *            the name of the section
     * @param attribute
     *            the name of the attribute
     * @param def
     *            a default value, if the attribute was not found
     * @return a String with the attributes value
     * @see #setString
     */
    public String getString(String section, String attribute, String def) {
        if (DEBUG)
            System.out.print("getString(" + section + ", " + attribute + ",...)");
        if (!section.endsWith("/"))
            section += '/';
        int sp = attribute.lastIndexOf('/');
        if (sp > 0) {
            section += attribute.substring(0, sp + 1);
            attribute = attribute.substring(sp + 1);
        }

        XmlFile.Tag here = getTag(section);
        if (here != null) {
            String v = (String) here.attributes.get(attribute);
            if (v != null)
                def = v;
        }
        if (DEBUG)
            System.out.println(" = '" + def + "'");
        return def;
    }

    /**
     * Set a value for an attribute in the section for the given XML file. <ul> <li>If an attribute is added to a
     * section which not exists, the section is created.</li> <li>If the attribute not yet exists, the attribute is
     * created, otherwise the attributes value is replaced.</li> <li>If value == null the attribute is deleted.</li>
     * <li>If attribute == null the section is deleted. Depending on value (==null) only the section itself is removed,
     * or (!= null) itself and all children are deleted</li> </ul> .
     * 
     * @param section
     *            the name of the section
     * @param attribute
     *            the name of the attribute
     * @param value
     *            the new key value
     * @see #getString
     */
    public void setString(String section, String attribute, String value) {
        XmlFile.Tag here = getTag(section);
        // tag deletions
        if (attribute == null) {
            if (value == null) {
                if (here == null)
                    return;
                here.clearContent();
                return;
            }
            if (here != null)
                here.removeMe();
            return;
        }

        if (value == null) {
            if (here != null)
                here.attributes.remove(attribute);
            return;
        }
        if (here == null)
            here = createTag(section);
        here.attributes.put(attribute, value);
    }

    /**
     * Clear all content and children from this section.
     * 
     * @param section
     *            a path to a XML tag
     */
    public void clearSection(String section) {
        setString(section, null, null);
    }

    /**
     * Drop the section and remov all content and children from this section.
     * 
     * @param section
     *            a path to a XML tag
     */
    public void dropSection(String section) {
        setString(section, null, "");
    }

    /**
     * Get the complete content of the section as byte array.
     * 
     * @param section
     *            the name of a section.
     * @return a byte array with the nth content or null if section is invalid.
     */
    public byte[] getContentBytes(String section) {
        XmlFile.Tag here = getTag(section);
        if (here == null)
            return null;
        return (byte[]) here.getContent().clone();
    }

    /**
     * Get the specified content element of the section as String. The content is the part between the tags.
     * 
     * <pre> &lt;sometag attr1="a" ...&gt;CONTENT&lt;/sometag&gt; </pre>
     * 
     * @param section
     *            the name of a section.
     * @return a String with the content or null if section is invalid.
     * @see #setContent
     */
    public String getContent(String section) {
        XmlFile.Tag here = getTag(section);
        if (here == null)
            return null;
        return decode(here.getContent(), encoding);
    }

    /**
     * Set the specified content element of the section as String. If the section does not exist, it is created.
     * 
     * @param section
     *            the name of a section.
     * @param comment
     *            the new content.
     */
    public void addComment(String section, String comment) {
        XmlFile.Tag here = createTag(section);
        if (here == null)
            return;
        here.addContent(new ByteRef("<!-- " + encode(comment, encoding) + " -->"));
    }

    /**
     * add a comment to the section as String. This erases all current content! If the section does not exist, it is
     * created.
     * 
     * @param section
     *            the name of a section.
     * @param content
     *            the new content.
     * @see #getContent
     */
    public void setContent(String section, String content) {
        XmlFile.Tag here = createTag(section);
        if (here == null)
            return;
        here.setContent(encode(content, encoding));
    }

    public void addContent(String section, String content) {
        XmlFile.Tag here = createTag(section);
        if (here == null)
            return;
        here.addContent(encode(content, encoding));
    }

    private final static boolean valid[] = new boolean[256];
    static {
        for (int i = 0; i < 256; ++i)
            valid[i] = true;
        for (int i = 32; i < 128; ++i)
            valid[i] = true;
        String s = "&<>\"";
        for (int i = 0; i < s.length(); ++i)
            valid[0xff & s.charAt(i)] = false;
    }

    private static String decode(ByteRef br, String encoding) {
        return decode(br.toByteArray(), encoding);
    }

    private static String decode(byte b[], String encoding) {
        byte x[] = new byte[b.length];
        int j = 0;
        for (int i = 0; i < b.length; ++i, ++j) {
            int c = b[i] & 0xff;
            if (c != '&')
                x[j] = (byte) c;
            else {
                int s = ++i;
                while (i < b.length && b[i] != ';')
                    ++i;
                if (i == b.length)
                    continue;
                try {
                    String r = new String(b, s, i - s);
                    if (b[s] == '#') {
                        x[j] = (byte) Integer.parseInt(r.substring(1));
                    } else if (b[s] == 'x') {
                        x[j] = (byte) Integer.parseInt(r.substring(1), 16);
                    } else if (r.equals("lt")) {
                        x[j] = (byte) '<';
                    } else if (r.equals("gt")) {
                        x[j] = (byte) '>';
                    } else if (r.equals("amp")) {
                        x[j] = (byte) '&';
                    } else if (r.equals("quot")) {
                        x[j] = (byte) '"';
                    }
                    /*
                     * else if (r.equals("apos")) { x[j] = (byte) '\''; } else if (r.equals("szlig")) { x[j] = (byte)
                     * '???'; } else if (r.equals("auml")) { x[j] = (byte) '???'; } else if (r.equals("ouml")) { x[j] =
                     * (byte) '???'; } else if (r.equals("uuml")) { x[j] = (byte) '???'; } else if (r.equals("Auml")) {
                     * x[j] = (byte) '???'; } else if (r.equals("Ouml")) { x[j] = (byte) '???'; } else if
                     * (r.equals("Uuml")) { x[j] = (byte) '???'; }
                     */
                    else
                        x[j] = (byte) '?';

                } catch (Exception e) {
                    x[j] = 32;
                }
            }
        }
        try {
            if (encoding != null) {
                String s1 = new String(x, 0, j, encoding);
                return s1;
            }
        } catch (UnsupportedEncodingException e) {
        }
        return new String(x, 0, j);
    }

    public static ByteRef encode(String s) {
        return encode(s, null);
    }

    public static ByteRef encode(String s, String encoding) {
        byte b[] = null;
        if (encoding != null) {
            try {
                b = s.getBytes(encoding);
            } catch (UnsupportedEncodingException e) {
            }
        }
        if (b == null)
            b = s.getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream((b.length * 17) / 16);
        try {
            for (int i = 0; i < b.length; ++i) {
                byte c = b[i];
                if (valid[0xff & c])
                    bos.write(c);
                else
                    switch (c & 0xff) {
                    case '<':
                        bos.write("&lt;".getBytes());
                        break;
                    case '>':
                        bos.write("&gt;".getBytes());
                        break;
                    case '&':
                        bos.write("&amp;".getBytes());
                        break;
                    case '"':
                        bos.write("&quot;".getBytes());
                        break;
                    case '\'':
                        bos.write("&apos;".getBytes());
                        break;
                    /*
                     * case '???': bos.write("&#xE4;".getBytes()); break; case '???':
                     * bos.write("&#xF6;".getBytes()); break; case '???': bos.write("&#xFC;".getBytes()); break;
                     * case '???': bos.write("&#xC4;".getBytes()); break; case '???':
                     * bos.write("&#xD6;".getBytes()); break; case '???': bos.write("&#xDC;".getBytes()); break;
                     * case '???': bos.write("&#xDF;".getBytes()); break;
                     */
                    default:
                        bos.write('&');
                        bos.write('#');
                        bos.write(Integer.toString(0xff & c).getBytes());
                        bos.write(';');
                    }
            }
        } catch (Exception io) {
        }
        return new ByteRef(bos.toByteArray());
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
        if (root.allByOrder.size() > 0) {
            ByteRef tag = (ByteRef) root.allByOrder.get(0);
            if (tag.startsWith(XML))
                root.allByOrder.remove(0);
        }
        if (encoding != null)
            root.allByOrder.add(0, new ByteRef("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>"));
    }

    /**
     * Creates always a new section and returns its unique path. E.g. if there are duplicate sections
     * createSection("/foo/bar") might return a different path, "/foo/bar#000005/" so your created section is clearly
     * identified.
     * 
     * @param section
     *            the section to be created
     * @return the unique section path to this section, always ending with a "/".
     */
    public String createSection(String section) {
        if (section.endsWith("/"))
            section = section.substring(0, section.length() - 1);
        int idx = section.lastIndexOf('/');
        String path = section.substring(0, idx);
        section = section.substring(idx + 1);

        Tag here = createTag(path);

        XmlFile.Tag nt = new XmlFile.Tag(section);
        here.add(nt);

        return nt.getPath();
    }

    /**
     * Sort the entries by the specified order.
     * 
     * @param section
     *            the name of the section = a path to the xml tag.
     * @param order
     *            the ordered child tag names which is applied to the existing children.
     */
    public void sort(String section, String order) {
        Tag here = getTag(section);
        if (here != null)
            here.sort(order);
    }

    /**
     * Displays the full XML content.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            write(bos);
        } catch (IOException e) {
        }
        return bos.toString();
    }

    /**
     * Get an Iterator for the child sections. The iterator returns a String containing the full path of the child
     * section. so /foo/bar might iterate over /foo/bar/aaaa, foo/bar/bbbb, and so on
     * 
     * @param sectionName
     *            the section name is an XML path like "/foo/bar"
     * @return an Iterator for the child sections.
     */
    public Iterator<String> sections(String sectionName) {
        return getSections(sectionName).iterator();
    }

    public boolean isPreserveWhiteSpaces() {
        return preserveWhiteSpaces;
    }

    public void setPreserveWhiteSpaces(boolean preserveWhiteSpaces) {
        this.preserveWhiteSpaces = preserveWhiteSpaces;
    }
    
    public static String getLastSegment(final String key) {
        int slash = key.lastIndexOf('/', key.length() - 2) + 1;
        int num = key.indexOf('#', slash);
        if (num < slash)
            num = key.length() - 1;
        return key.substring(slash, num);
    }

}

/******************************************************************************
 * $Log: XmlFile.java,v $
 * Revision 1.44  2014/06/23 19:12:08  bebbo
 * @R white spaces are now only preserved if the flag is set
 * Revision 1.43 2012/07/18 06:11:27 bebbo
 * 
 * @R typified Revision 1.42 2011/07/06 12:19:29 bebbo
 * 
 * @N iterator is now typed Revision 1.41 2011/01/01 12:15:42 bebbo
 * 
 * @N added better encoding support Revision 1.39 2009/01/27 18:55:34 bebbo
 * 
 * @R encode method is now public
 * 
 *    Revision 1.38 2008/03/15 18:22:16 bebbo
 * @C added comments.
 * 
 *    Revision 1.37 2008/03/15 18:01:05 bebbo
 * @R Changed the license: From now on GPL 3 applies.
 * 
 *    Revision 1.36 2008/03/13 20:53:02 bebbo
 * @I replaced Vector by ArrayList.
 * 
 *    Revision 1.35 2007/01/18 22:01:08 bebbo
 * @B fixes after last changes - enumerations work again
 * 
 *    Revision 1.34 2006/02/02 07:51:07 bebbo
 * @N new method addComment()
 * 
 *    Revision 1.33 2004/11/18 14:53:45 bebbo
 * @I reformatted source
 * 
 *    Revision 1.32 2004/03/14 18:05:31 bebbo
 * @O optimized for speed
 * 
 *    Revision 1.31 2003/10/02 07:51:23 bebbo
 * @C better comment for setContent()
 * 
 *    Revision 1.30 2003/10/01 12:21:55 bebbo
 * @C comment
 * 
 *    Revision 1.29 2003/10/01 12:18:37 bebbo
 * @C enhanced documentation
 * 
 *    Revision 1.28 2003/09/01 13:58:23 bebbo
 * @B fixed handling of comments
 * @N added handling of CDATA
 * 
 *    Revision 1.27 2003/06/30 12:45:40 bebbo
 * @B written attributes are now properly escaped
 * @B apostrophes are also accepted as attribute delimiters
 * 
 *    Revision 1.26 2003/06/18 12:56:44 bebbo
 * @B fixed insertion of the root element
 * 
 *    Revision 1.25 2003/02/17 14:23:26 bebbo
 * @I changed access rights to remove synthetic accessor methods
 * 
 *    Revision 1.24 2003/01/03 18:19:19 bebbo
 * @R intermediate version
 * 
 *    Revision 1.23 2002/12/19 14:57:01 bebbo
 * @B fixes in getString - when attribute name contained also path
 * 
 *    Revision 1.22 2002/11/19 12:58:24 bebbo
 * @B fixed output of umlauts
 * 
 *    Revision 1.21 2002/11/12 14:29:09 bebbo
 * @I now using buffered IO
 * 
 *    Revision 1.20 2002/11/12 14:06:58 bebbo
 * @R changed formatting of content elements: if size <= 80 chars, all is written in 1 line
 * 
 *    Revision 1.19 2002/11/12 13:31:37 bebbo
 * @C comments...
 * 
 *    Revision 1.18 2002/11/12 13:19:56 bebbo
 * @B fixed ApiDoc errors
 * 
 *    Revision 1.17 2002/11/11 15:33:50 bebbo
 * @B fixed createSection - returned section name now ends on "/"
 * @B fixed setString - if duplicate sections exist only the specified/first is removed
 * 
 *    Revision 1.16 2002/11/08 16:54:49 bebbo
 * @N new function createSection
 * @I internal changes: name generation on duplicate entries
 * @! this version is not fully tested, but seems to work properly
 * 
 *    Revision 1.15 2002/11/08 13:20:03 bebbo
 * @N new method setContent(String section, String value)
 * 
 *    Revision 1.14 2002/10/24 11:29:25 bebbo
 * @R changed update
 * 
 *    Revision 1.13 2002/08/20 15:19:54 bebbo
 * @B not yet existing files can be opened/created now
 * 
 *    Revision 1.12 2002/03/30 15:41:05 franke
 * @R path is now used as an exact path. Added wildstar to search with pattern.
 * 
 *    Revision 1.11 2002/01/16 09:59:01 franke
 * @C fixed comments
 * 
 *    Revision 1.10 2001/12/10 16:22:51 bebbo
 * @C completed comments!
 * 
 *    Revision 1.9 2001/12/10 11:47:37 bebbo
 * @B added type casts to array initializers
 * 
 *    Revision 1.8 2001/12/10 11:46:23 bebbo
 * @B added type casts to array initializers
 * 
 *    Revision 1.7 2001/11/27 13:22:05 bebbo
 * @B getSections enumrated elements twice when the subpath was 0 and a name exists. Then the enumerated and the named
 *    version was in the result. Now only the enumerated sections are returned.
 * 
 *    Revision 1.6 2001/11/04 18:37:36 franke
 * @I explicit type naming for inner classes
 * 
 *    Revision 1.5 2001/09/15 08:57:26 bebbo
 * @C added comments
 * @R complete new design!
 * 
 *    Revision 1.4 2001/08/24 15:59:55 bebbo
 * @N added <!-- comment --> filter
 * @R now ibeying order in file
 * @R added handling of unnamed tags (using autonames)
 * 
 *    Revision 1.3 2001/08/03 13:49:46 bebbo
 * @B fixed quoted colons parsing and some unremoved white spaces
 * 
 *    Revision 1.2 2001/04/16 20:04:59 bebbo
 * @R getString adds path part from key to section
 * 
 *    Revision 1.1 2001/04/16 13:36:51 bebbo
 * @N new
 * 
 *****************************************************************************/
