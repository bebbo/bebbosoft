/******************************************************************************
 * A class to parse mime files.
 * - retreive the individual sections.
 * - get the header information.
 *
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

import java.io.InputStream;
import java.util.ArrayList;

public class MimeFile {

    private final static ByteRef END = new ByteRef("--");

    public static class Info implements Cloneable {
        public String path;

        public String contentType;

        public int hBegin, bBegin, end;

        public int hLines, bLines;

        public ArrayList<Pair<String, String>> header;

        Info(String p, String ct, int a, int b, int c, int x, int y) {
            path = p;
            contentType = ct;
            hBegin = a;
            bBegin = b;
            end = c;
            hLines = x;
            bLines = y;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "(" + path + ", " + contentType + ", " + hBegin + ", " + bBegin + ", " + end + ", " + hLines + ", "
                    + bLines + ")";
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.lang.Object#clone()
         */
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    private ByteRef buffer = new ByteRef();

    private InputStream is;

    private int lines;

    private int position;

    private int lastPosition;

    ArrayList<Info> v = new ArrayList<Info>();

    private MimeFile(InputStream _is) {
        is = _is;
        lines = 1;
        position = 0;
    }

    private ByteRef nextLine() {
        lastPosition = position;
        if (buffer == null)
            return null;

        // get next line
        ByteRef line = null;
        for (line = buffer.nextLineCRLF(); line == null; line = buffer.nextLineCRLF()) {
            ByteRef b2 = buffer.update(is);
            if (b2 == null) {
                break;
            }
        }
        if (line == null && buffer.length() > 0) {
            line = buffer;
            buffer = null;
            position += line.length();
            ++lines;
        } else if (line != null) {
            position += 2 + line.length();
            ++lines;
        }

        return line;
    }

    private final static ByteRef CONTENTTYPE = new ByteRef("Content-Type"), MULTIPART = new ByteRef("multipart"),
            MESSAGE = new ByteRef("message"), MM = new ByteRef("--");

    private boolean parse(String path, ByteRef outa) {
        if (path.startsWith("."))
            path = path.substring(1);
        ByteRef boundary = null;
        int startLines = lines;
        int startBytes = position;
        // read header
        ArrayList<Pair<String, String>> header = new ArrayList<Pair<String, String>>();
        ByteRef line, last = null, contentType = null;
        for (;;) {
            line = nextLine();
            if (line == null)
                break;

            if (line.charAt(0) <= 32) {
                if (last != null)
                    last = last.append(line);
            }

            if (line.charAt(0) > 32 || line.length() == 0) {
                // this is an unfolded header line
                if (last != null && last.length() > 13) {
                    ByteRef quali = last.nextWord(':').trim();
                    header.add(new Pair<String, String>(quali.toString(), last.toString()));
                    //          System.out.println("[" + quali + "]");
                    if (quali.equalsIgnoreCase(CONTENTTYPE)) {
                        contentType = (ByteRef) last.trim().clone();
                        if (contentType.substring(0, 9).equalsIgnoreCase(MULTIPART)) {
                            last.nextWord(';');
                            while (last.length() > 0) {
                                ByteRef key = last.nextWord('=').trim();
                                last.trim();
                                ByteRef val;
                                if (last.charAt(0) == '"') {
                                    last.nextWord('"');
                                    val = last.nextWord('"');
                                    last.nextWord(';');
                                } else {
                                    val = last.nextWord(';');
                                }
                                last.trim();
                                //                System.out.println(key + "=" + val + "[" + last);

                                if (key.equalsIgnoreCase("TYPE")) {
                                    contentType = val;
                                } else if (key.equalsIgnoreCase("BOUNDARY")) {
                                    boundary = MM.append(val);
                                    break;
                                }
                            }
                        }
                    }
                }
                last = line;
            }
            if (line.length() == 0)
                break;

        }
        //    if (lines-startLines == 1 && bytes-startBytes == 2)
        //      return false;

        int bodyLines, headerLines = lines - startLines;
        int headerBytes = position - startBytes;

        boolean res;
        // special handling for message    
        if (contentType != null && contentType.startsWith(MESSAGE)) {
            res = parse(path + ".1", outa);
        } else {
            // parse body
            for (;;) {
                line = nextLine();
                if (line == null) {
                    if (outa == null) // valid end
                        break;
                    // throw new IOException("corrupt MIME file: missing termination string");
                    // be relaxant
                    break;
                }

                if (outa != null && line.startsWith(outa)
                        && (line.length() == outa.length() || line.substring(outa.length()).equals(END))) {
                    break;
                }

                if (line.equals(boundary)) {
                    int n = 1;
                    // loop while the current boundary is not ended
                    while (parse(path + "." + n, boundary)) {
                        ++n;
                    }
                }
            }
            res = line != null && line.equals(outa);
        }

        bodyLines = lines - startLines - headerLines - 1;
        //    bodyBytes = bytes - startBytes - headerBytes;
        //    System.out.println(path + " HEADER " + contentType + ": " + startBytes + ":" + headerBytes + " Bytes, " + startLines + ":" + headerLines + " Lines");
        //    System.out.println(path + " BODY " + (startBytes+headerBytes) + "-" + bytes + "(" + bodyBytes + ") Bytes, " + (startLines+headerLines) + "-" + lines + "(" + bodyLines + ") Lines");
        Info info =
                new Info(path, contentType != null ? contentType.toString() : "", startBytes, startBytes + headerBytes,
                        lastPosition, headerLines, bodyLines);
        info.header = header;
        v.add(info);

        return res;
    }

    void parse() {
        parse("", null);
        if (v.size() == 1) {
            Info i;
            try {
                i = (Info) v.get(0).clone();
                i.path = "1";
                v.add(i);
            } catch (CloneNotSupportedException e) {
            }
        }
    }

    /**
     * @param is
     * @return
     * @throws Exception
     */
    public static ArrayList<Info> parseMime(InputStream is) {
        MimeFile mf = new MimeFile(is);
        mf.parse();
        return mf.v;
    }

    public static ArrayList<Info> parseMime(InputStream is, String boundary) {
        MimeFile mf = new MimeFile(is);
        int n = 1;
        for (;;) {
            boolean r = mf.parse("" + n++, new ByteRef("--" + boundary));
            if (!r)
                break;
        }
        return mf.v;
    }

}