/******************************************************************************
 * $Source: /export/CVS/java/de/bb/tools/mug/src/main/java/de/bb/tools/mug/Main.java,v $
 * $Revision: 1.2 $
 * $Date: 2011/08/31 06:30:16 $
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

  (c) 1999-2011 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.tools.mug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author bebbo
 */
public class Main extends de.bb.util.FileBrowser {
    private static String logName;
    private static HashMap<String, String> packages = new HashMap<String, String>();
    private Mug mug;
    private PrintStream ps;

    private Main() throws FileNotFoundException {
        super("**/*.class");
        if (logName != null) {
            ps = new PrintStream(new FileOutputStream(logName));
        } else {
            ps = System.out;
        }
        mug = new Mug(ps, packages);
    }

    /**
     * Shrinks and obfuscates all classes from &lt;inpath&gt; and puts them into &lt;outpath&gt;. Both arguments must be
     * valid paths. If &lt;outpath&gt; does not exist, it will be created.
     * 
     * Mapping informations are written to stdout or to the specified log file.
     * 
     * @param args
     *            args[0] = &lt;inpath&gt;, args[1] = &lt;outpath&gt;
     */
    public static void main(String[] args) {
        try {
            args = doOptions(args);
            Main m = new Main();
            m.doit(args);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            if (ex.getMessage() == null || ex.getMessage().length() == 0)
                showUsage();
        }
    }

    /**
     * Method doit.
     */
    private void doit(String[] args) throws IOException {
        scan(args[0], true);
        // mug.writeTo(args[1] + "$");
        mug.cripple();
        mug.writeTo(args[1]);
        mug.ps.close();
    }

    /**
     * @see de.bb.util.FileBrowser#handleDir(String, String)
     */
    protected void handleDir(String path, String dir) {
    }

    /**
     * @see de.bb.util.FileBrowser#handleFile(String, String)
     */
    protected void handleFile(String path, String file) {
        try {
            // System.out.println(file);
            if (file.endsWith(".class")) {
                mug.addClass(new FileInputStream(new File(getBaseDir() + path, file)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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
                if (o.equals("-l")) {
                    logName = args[i];
                    continue;
                }
                if (o.equals("-p")) {
                    String p2p = args[i];
                    String[] p2 = p2p.split("=");
                    packages.put(p2[0].replace('.', '/'), p2[1].replace('.', '/'));
                    continue;
                }
            } catch (Exception e) {
                throw new Exception("Invalid parameter for '" + args[i - 2] + "'");
            }
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        if (j != 2) {
            throw new Exception("");
        }

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }

    private static void showUsage() {
        System.out.println("shrinks and obfuscates all classes from <inpath> and puts them into <outpath>");
        System.out.println("USAGE: java -jar bb_mug.jar [-?] [-l <logfile>] [-p <old>=<new>] <inpath> <outpath>");
        System.out.println("  -?              display this message");
        System.out.println("  -l <logfile>    write mapping info into file");
        System.out.println("  -p <old>=<new>  rename package <old> to <new>");
        System.out.println("bb_mug.jar $Revision: 1.2 $ (c) 2002-2011 by Stefan Bebbo Franke");
    }
}
/*
 * Log: $Log: Main.java,v $
 * Log: Revision 1.2  2011/08/31 06:30:16  bebbo
 * Log: @B fixes #3
 * Log:  * inner classes are now added correctly
 * Log:
 * Log: Revision 1.1  2011/08/30 11:38:26  bebbo
 * Log: @I renamed package to de.bb.tools.mug
 * Log: @N added package renaming capabilities
 * Log:
 * Log: Revision 1.2  2011/08/29 16:55:58  bebbo
 * Log: @I switch to JDK 1.6
 * Log:
 * Log: Revision 1.1  2011/08/29 16:31:55  bebbo
 * Log: @N added to BNM build - new version
 * Log: Revision 1.4 2005/12/01 12:53:50 bebbo
 * Log: @C added comments for class and main() 
 * Log: Revision 1.3 2005/12/01 11:25:04 bebbo
 * Log: @V added version message
 * Log: Revision 1.2 2002/11/28 21:20:36 bebbo 
 * Log: @R removed intermediate output 
 * Log: Revision 1.1 2002/11/18 12:00:12 bebbo
 * Log: @N first version Log:
 */
