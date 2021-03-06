/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/xml/CombiningChar.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/01 13:17:45 $
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
public class CombiningChar extends UnicodeCharacterRange {
    final static String init = "[#x0300-#x0345]|[#x0360-#x0361]|[#x0483-#x0486]"
            + "|[#x0591-#x05A1]|[#x05A3-#x05B9]|[#x05BB-#x05BD]|#x05BF"
            + "|[#x05C1-#x05C2]|#x05C4|[#x064B-#x0652]|#x0670|[#x06D6-#x06DC]"
            + "|[#x06DD-#x06DF]|[#x06E0-#x06E4]|[#x06E7-#x06E8]|[#x06EA-#x06ED]"
            + "|[#x0901-#x0903]|#x093C|[#x093E-#x094C]|#x094D|[#x0951-#x0954]"
            + "|[#x0962-#x0963]|[#x0981-#x0983]|#x09BC|#x09BE|#x09BF"
            + "|[#x09C0-#x09C4]|[#x09C7-#x09C8]|[#x09CB-#x09CD]|#x09D7"
            + "|[#x09E2-#x09E3]|#x0A02|#x0A3C|#x0A3E|#x0A3F|[#x0A40-#x0A42]"
            + "|[#x0A47-#x0A48]|[#x0A4B-#x0A4D]|[#x0A70-#x0A71]|[#x0A81-#x0A83]"
            + "|#x0ABC|[#x0ABE-#x0AC5]|[#x0AC7-#x0AC9]|[#x0ACB-#x0ACD]"
            + "|[#x0B01-#x0B03]|#x0B3C|[#x0B3E-#x0B43]|[#x0B47-#x0B48]"
            + "|[#x0B4B-#x0B4D]|[#x0B56-#x0B57]|[#x0B82-#x0B83]|[#x0BBE-#x0BC2]"
            + "|[#x0BC6-#x0BC8]|[#x0BCA-#x0BCD]|#x0BD7|[#x0C01-#x0C03]"
            + "|[#x0C3E-#x0C44]|[#x0C46-#x0C48]|[#x0C4A-#x0C4D]|[#x0C55-#x0C56]"
            + "|[#x0C82-#x0C83]|[#x0CBE-#x0CC4]|[#x0CC6-#x0CC8]|[#x0CCA-#x0CCD]"
            + "|[#x0CD5-#x0CD6]|[#x0D02-#x0D03]|[#x0D3E-#x0D43]|[#x0D46-#x0D48]"
            + "|[#x0D4A-#x0D4D]|#x0D57|#x0E31|[#x0E34-#x0E3A]|[#x0E47-#x0E4E]"
            + "|#x0EB1|[#x0EB4-#x0EB9]|[#x0EBB-#x0EBC]|[#x0EC8-#x0ECD]"
            + "|[#x0F18-#x0F19]|#x0F35|#x0F37|#x0F39|#x0F3E|#x0F3F"
            + "|[#x0F71-#x0F84]|[#x0F86-#x0F8B]|[#x0F90-#x0F95]|#x0F97"
            + "|[#x0F99-#x0FAD]|[#x0FB1-#x0FB7]|#x0FB9|[#x20D0-#x20DC]|#x20E1" + "|[#x302A-#x302F]|#x3099|#x309A";

    private final static CombiningChar instance = new CombiningChar();

    private CombiningChar() {
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
 * Log: $Log: CombiningChar.java,v $
 * Log: Revision 1.1  2011/01/01 13:17:45  bebbo
 * Log: @N added to new CVS repo
 * Log: Log: Revision 1.1 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
