/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.versioning.dumb;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;

public class DumbXmlSearcher extends DumbSearcher {

  public DumbXmlSearcher(String content) {
    super(content);
  }

  public DumbXmlSearcher(File f) throws IOException {
    super(f);
  }

  public Pos search(String path, String value, int offset) {

    int slash = path.lastIndexOf('/');
    String key = "<" + path.substring(slash + 1);
    String part = path.substring(0, slash < 1 ? slash + 1 : slash);

    for (; offset >= 0; ++offset) {
      offset = content.indexOf(key, offset);
      if (offset < 0)
        break;
      String tag = new StringTokenizer(content.substring(offset), " \r\n\t>")
          .nextToken();
      if (!key.equals(tag))
        continue;

      Pos pos = getValuePos(key.substring(1), offset, value);
      if (pos == null)
        continue;

      if (!hasPartialPath(offset, part))
        continue;

      return pos;
    }
    return null;
  }

  private boolean hasPartialPath(int offset, String part) {
    Stack<String> stack = new Stack<String>();
    while (part.length() > 0) {
      int slash = part.lastIndexOf('/');
      String key = part.substring(slash + 1);
      part = part.substring(0, slash < 1 ? slash + 1 : slash);

      // search the key before current position.
      // if de.bb.eclipse.moject.versioning.dumb closing element occurs, push the key and make the previous
      // current
      // do this until key is empty
      for (;;) {
        int ket = content.lastIndexOf('>', offset);
        if (ket < 0)
          return part.equals("/");
        // skip comments
        if (ket > 1 && "--".equals(content.substring(ket - 2, ket))) {
          int bra = content.lastIndexOf("<!--", ket);
          if (bra < 0)
            return false;
          offset = bra;
          continue;
        }
        // skip CDATA
        if (ket > 1 && "]]".equals(content.substring(ket - 2, ket))) {
          int bra = content.lastIndexOf("<![CDATA[", ket);
          if (bra < 0)
            return false;
          offset = bra;
          continue;
        }
        int bra = content.lastIndexOf('<', ket);
        if (bra < 0)
          return false;

        // endTag? push current key and search the matching start Tag
        if (content.charAt(bra + 1) == '/') {
          stack.push(key);
          key = content.substring(bra + 2, ket).trim();
          offset = bra;
          continue;
        }

        String tag = new StringTokenizer(content.substring(bra + 1, ket),
            " \r\n\t").nextToken();

        if (tag.startsWith("?")) {
          offset = bra;
          continue;
        }

        if (!tag.equals(key))
          return false;
        offset = bra;
        if (stack.size() == 0)
          break;
        key = stack.pop();
      }
    }
    return true;
  }

  private Pos getValuePos(String key, int offset, String value) {
    int ket = content.indexOf('>', offset);
    if (ket < 0)
      return null;

    int start = ket + 1;
    Stack<String> stack = new Stack<String>();
    for (;;) {
      int bra = content.indexOf("<", ket);
      if (bra < 0)
        return null;

      // ensure that the closing tag belongs to the key
      // skip comments
      if (content.substring(bra).startsWith("<!--")) {
        ket = content.indexOf("-->", bra + 4);
        if (ket < 0)
          return null;
        continue;
      }

      // skip CDATA
      if (content.substring(bra).startsWith("<![CDATA[")) {
        ket = content.indexOf("]]>", bra);
        if (ket < 0)
          return null;
        continue;
      }

      // check if de.bb.eclipse.moject.versioning.dumb new tag starts
      String rest = content.substring(bra + 1);
      String tag = new StringTokenizer(rest, " \r\n\t>").nextToken();

      if (!tag.startsWith("/")) {
        ket = content.indexOf('>', bra);
        stack.push(key);
        key = tag;
        continue;
      }

      tag = tag.substring(1);
      if (!tag.equals(key))
        return null;

      if (stack.size() > 0) {
        ket = content.indexOf('>', bra);
        key = stack.pop();
        continue;
      }

      if ("*".equals(value)) {
        return new Pos(start, bra - start);
      }

      int pos = content.indexOf(value, ket);
      if (pos > bra)
        return null;
      return new Pos(pos, value.length());
    }
  }

  public int indexOf(String string) {
    return content.indexOf(string);
  }
}
