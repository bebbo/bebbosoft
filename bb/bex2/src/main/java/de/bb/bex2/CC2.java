/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bex2/src/main/java/de/bb/bex2/CC2.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/01 13:08:09 $
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

  Created on 02.12.2003

 *****************************************************************************/
package de.bb.bex2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Map.Entry;
import java.util.Stack;

import de.bb.bex2.Bex;
import de.bb.bex2.Bex.Rule;

/**
 * @author bebbo
 */
public class CC2 {

    private static String path = ".";

    /**
     * parse the command line for options and return other parameters.
     */
    private static String[] doOptions(String args[]) throws Exception {
        int j = 0;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) != '-') // no argument
            {
                args[j++] = args[i];
                continue;
            }
            String o = args[i++];
            // an argument
            if (o.equals("-?")) {
                throw new Exception(""); // show usage only!
            }
            try {
                if (o.equals("-d")) {
                    path = args[i];
                    continue;
                }
            } catch (Exception e) {
                throw new Exception("Invalid parameter for '" + args[i - 2] + "'");
            }
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }

    public static void main(String[] args) {
        // String fn = "calc.bnf";
        // String fn = "dtd.bnf";
        // String fn = "htmldtd.bnf";
        // String fn = "externalID.bnf";
        // String fn = "space.bnf";
        String fn = null;

        try {
            args = doOptions(args);

            if (args.length > 0)
                fn = args[0];

            if (fn == null) {
                System.out.println("usage: de.bb.bex2.CC [-d outpath] fileName");
                return;
            }

            FileReader fr = new FileReader(fn);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer grammar = new StringBuffer();

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                grammar.append(line);
                grammar.append('\n');
            }
            fr.close();

            Bex bex = new de.bb.bex2.Bex(grammar.toString());

            
            Stack<String> todo = new Stack<String>();
            todo.addAll(bex.exports.values());

            for (final Entry<String, Rule> e: bex.rules.entrySet()) {
                System.out.println(e.getKey() + " -> " + e.getValue());
            }

            RuleExpander re = new RuleExpander(todo, bex.rules);
            re.expandRules();
            re.calcFirst();
            re.calcFollow();
            
            //re.createClosures();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/******************************************************************************
 * Log: $Log: CC2.java,v $
 * Log: Revision 1.1  2011/01/01 13:08:09  bebbo
 * Log: @N added to new CVS repo
 * Log: Log: Revision 1.1 2005/11/18 14:51:35 bebbo Log: @R many updates - somehow stable version Log:
 * Log: Revision 1.1 2004/05/06 11:02:24 bebbo Log: @N first checkin Log:
 ******************************************************************************/
