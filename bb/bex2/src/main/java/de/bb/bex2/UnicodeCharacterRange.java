/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/UnicodeCharacterRange.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/07/18 09:08:44 $
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

  Created on 05.12.2003

 *****************************************************************************/
package de.bb.bex2;

import java.util.ArrayList;

/**
 * @author bebbo
 */
public class UnicodeCharacterRange {
    protected boolean data[] = new boolean[256];
    private ArrayList<Range> range = new ArrayList<Range>();

    /**
     * @param init
     */
    protected void add(String init) {
        init = init.trim();
        while (init.length() > 0) {
            if (init.charAt(0) == '[') {
                int end = init.indexOf(']');
                addRange(init.substring(1, end).trim());
            } else {
                addValue(init.substring(0, 6));
            }
            int end = init.indexOf('|');
            if (end < 0)
                break;
            init = init.substring(end + 1).trim();
        }
    }

    /**
     * @param string
     */
    private void addRange(String string) {
        int from = Integer.parseInt(string.substring(2, 6), 16);
        int to = Integer.parseInt(string.substring(string.length() - 4, string.length()), 16);
        if (to < 256) {
            while (from <= to) {
                data[from++] = true;
            }
            return;
        }
        Range r = new Range(from, to);
        range.add(r);
    }

    /**
     * @param string
     */
    private void addValue(String string) {
        int val = Integer.parseInt(string.substring(2), 16);
        if (val < 256) {
            data[val] = true;
            return;
        }
        Range r = new Range(val, val);
        range.add(r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.bb.bex2.Rule#match(de.bb.bex2.Scanner)
     */
    protected boolean test(Scanner scanner) {
        int ch = scanner.peek();
        if (ch < 0)
            return false;
        if (ch < 256) {
            if (data[ch]) {
                scanner.move(1);
                return true;
            }
            return false;
        }

        int add = 1;
        while (add + add < range.size()) {
            add += add;
        }

        Range hit = null;
        int off = 0;
        while (add > 0) {
            if (off + add < range.size()) {
                Range r = range.get(off + add);
                if (r.to >= ch) {
                    hit = r;
                    break;
                }
                if (r.from <= ch) {
                    off += add;
                }
            }
            add >>>= 1;
        }
        if (hit == null)
            return false;

        if (hit.to < ch)
            return false;
        scanner.move(1);
        return true;
    }

    static class Range {
        int from, to;

        Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }
}

/******************************************************************************
 * Log: $Log: UnicodeCharacterRange.java,v $
 * Log: Revision 1.2  2012/07/18 09:08:44  bebbo
 * Log: @I typified
 * Log: Log: Revision 1.1 2011/01/01 13:08:12 bebbo Log: @N added to new CVS repo
 * Log: Log: Revision 1.2 2005/11/18 14:51:35 bebbo Log: @R many updates - somehow stable version Log: Log: Revision 1.1
 * 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
