/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/Context.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/07/18 09:08:48 $
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

 Created on 04.12.2003

 *****************************************************************************/
package de.bb.bex2;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author bebbo
 */
public abstract class Context {

    protected Scanner scanner;

    public Context() {
        this(null);
    }

    /**
     * @param scanner2
     */
    public Context(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Notify that an ID is reached in the grammar.
     * 
     * @param id
     *            the event id
     */
    public abstract boolean notify(int id) throws ParseException;

    /**
     * @return
     */
    public ArrayList<RangeInfo> getRangeInfos(ParseEntry root) {
        if (scanner == null)
            return new ArrayList<RangeInfo>();
        if (root.getEnd() < scanner.getPosition())
            root.setEnd(scanner.getPosition());
        ArrayList<RangeInfo> ranges = new ArrayList<RangeInfo>();
        int pos = addRanges(ranges, 0, root);
        if (pos < root.getEnd()) {
            ranges.add(new RangeInfo(pos, root.getEnd(), 0));
        }
        return ranges;
    }

    /**
     * @param ranges
     * @param pos
     * @param node
     * @return
     */
    private int addRanges(ArrayList<RangeInfo> ranges, int pos, ParseEntry node) {
        if (node == null)
            return pos;

        ArrayList<ParseEntry> nodes = node.getChildren();
        if (nodes == null)
            return pos;

        for (Iterator<ParseEntry> i = nodes.iterator(); i.hasNext();) {
            ParseEntry pe = i.next();
            if (pe.getScanner() == scanner) {
                if (pe.getStart() > pos) {
                    ranges.add(new RangeInfo(pos, pe.getStart(), node.getType()));
                    // System.out.println(pos + "-" + pe.getStart() + ":" + node.getType());
                    pos = pe.getStart();
                }
                pos = addRanges(ranges, pos, pe);
                if (pe.getEnd() > pos) {
                    ranges.add(new RangeInfo(pos, pe.getEnd(), pe.getType()));
                    // System.out.println(pos + "-" + pe.getEnd() + ":" + pe.getType());
                    pos = pe.getEnd();
                }
            }
        }
        return pos;
    }
}

/******************************************************************************
 * Log: $Log: Context.java,v $
 * Log: Revision 1.2  2012/07/18 09:08:48  bebbo
 * Log: @I typified
 * Log: Log: Revision 1.1 2011/01/01 13:07:43 bebbo Log: @N added to new CVS repo Log: Log:
 * Revision 1.2 2005/11/18 14:51:35 bebbo Log: @R many updates - somehow stable version Log: Log: Revision 1.1
 * 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
