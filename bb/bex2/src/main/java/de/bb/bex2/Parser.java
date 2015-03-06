/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/Parser.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/07/18 09:08:46 $
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

  Created on 08.12.2003

 *****************************************************************************/
package de.bb.bex2;

/**
 * @author bebbo
 */
public abstract class Parser {

    protected Context context;

    protected Scanner scanner;

    protected ParseEntry current;

    /**
     * create a new Parser.
     */
    protected Parser() {
    }

    /**
     * create a new Parser.
     * 
     * @param scanner
     *            scanner
     * @param ctx
     *            context
     */
    protected Parser(Scanner scanner, Context ctx) {
        this.scanner = scanner;
        this.context = ctx;
    }

    /**
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return
     */
    public Scanner getScanner() {
        return scanner;
    }

    /**
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * @param scanner
     */
    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    public ParseEntry getCurrent() {
        return current;
    }

    public void setCurrent(ParseEntry current) {
        this.current = current;
    }

    protected void syntaxError(String key) throws ParseException {
        int offset = scanner.getPosition();
        throw new ParseException("syntax error parsing '" + key + "' at line " + scanner.getLineFromOffset(offset)
                + ":\r\n" + scanner.toString());
    }
}

/******************************************************************************
 * Log: $Log: Parser.java,v $
 * Log: Revision 1.2  2012/07/18 09:08:46  bebbo
 * Log: @I typified
 * Log: Log: Revision 1.1 2011/01/01 13:07:32 bebbo Log: @N added to new CVS repo Log: Log:
 * Revision 1.1 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
