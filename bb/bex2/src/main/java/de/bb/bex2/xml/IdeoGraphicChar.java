/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/xml/IdeoGraphicChar.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/01 13:17:30 $
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
package de.bb.bex2.xml;

import de.bb.bex2.Scanner;
import de.bb.bex2.UnicodeCharacterRange;

/**
 * @author bebbo
 */
public class IdeoGraphicChar extends UnicodeCharacterRange {
    final static String init = "[#x4E00-#x9FA5]|#x3007|[#x3021-#x3029]";
    private final static IdeoGraphicChar instance = new IdeoGraphicChar();

    private IdeoGraphicChar() {
        add(init);
    }

    /**
     * Returns true if next character matches. Moves scanner 1 char, if true.
     * 
     * @param scanner
     *            scanner to read next char.
     * @return true if next character matches.
     */
    public static boolean isChar(Scanner scanner) {
        return instance.test(scanner);
    }
}

/******************************************************************************
 * Log: $Log: IdeoGraphicChar.java,v $
 * Log: Revision 1.1  2011/01/01 13:17:30  bebbo
 * Log: @N added to new CVS repo
 * Log: Log: Revision 1.1 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
