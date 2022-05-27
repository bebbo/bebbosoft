/******************************************************************************
 * $Source: /export/CVS/java/de/bb/tools/uses/src/main/java/de/bb/tools/uses/Main.java,v $
 * $Revision: 1.3 $
 * $Date: 2014/09/22 09:34:57 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * windows like ini file, with xml like extensions
 * next step will be xml files
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

  (c) 2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.tools.uses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import de.bb.util.FileBrowser;
import de.bb.util.ZipClassLoader;

/**
 * Main class for bb_uses
 * 
 * @author bebbo
 */
public class Main extends FileBrowser {

    private String logName;
    private boolean processJava;

    HashMap<String, String> used = new HashMap<String, String>();
    HashMap<String, String> visited = new HashMap<String, String>();
    HashMap<String, Long> exist = new HashMap<String, Long>();
    private LinkedList<String> toProcess = new LinkedList<String>();

    private ZipClassLoader zcl;

    private PrintStream ps;

    private Main() throws FileNotFoundException {
        super("**/*");
        if (logName != null) {
            ps = new PrintStream(new FileOutputStream(logName));
        } else {
            ps = System.out;
        }
    }

    public static void main(String[] args) {
        try {
            Main m = new Main();
            args = m.doOptions(args);
            m.doit(args);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().length() == 0)
                showUsage();
            else
                ex.printStackTrace();
        }
    }

    /**
     * Method doit.
     */
    private void doit(String[] args) throws IOException {
        zcl = new ZipClassLoader(args[0]);

        ps.println("scanning existing classes in: " + args[1]);
        scan(args[1], true);

        // check dependencies
        for (int i = 2; i < args.length; ++i) {
            String className = args[i];
            if (className.startsWith("@")) {
                String s = readFile(className.substring(1));
                for (StringTokenizer st = new StringTokenizer(s); st.hasMoreElements();) {
                    String a = st.nextToken();
                    toProcess.add(a);
                }
            } else {
                toProcess.add(className);
            }
        }

        ps.println("processing " + toProcess.size() + " classes");
        while (toProcess.size() > 0) {
            String oName = toProcess.removeFirst();
            String cName = toSlash(oName);
            if (cName.endsWith("*")) {
                String[] all = zcl.list(cName);
                for (String c : all) {
                    if (c.endsWith(".class"))
                        toProcess.add(c.substring(0, c.length() - 6));
                }
                continue;
            }

            if (visited.get(cName) == null) {
                visited.put(cName, oName);
                if (!processJava && cName.startsWith("java/")) {
                    continue;
                }
                try {
                    final String fileName = cName + ".class";
                    InputStream is = zcl.getResourceAsStream(fileName);
                    if (is == null) {
                        continue;
                    }
                    UsesClass uc = new UsesClass(is);
                    uc.getClasses(toProcess);
                    is.close();
                    
                    Long lastModified = exist.get(cName);
                    if (lastModified != null) {
                        final URL fileUrl = zcl.getResource(fileName);
                        File file = new File(fileUrl.getFile());
                        if (fileUrl.getProtocol().equals("jar")) {
                            final String s = file.getPath();
                            final int ex = s.indexOf('!');
                            file = new File(new URL(s.substring(0, ex)).getFile());
                        }
                        if (file.lastModified() > lastModified)
                            lastModified = null;
                    }
                    if (lastModified == null) {
                        ps.println("add: " + cName);
                        used.put(cName, oName);
                    }

                } catch (Exception ex) {
                    //          ex.printStackTrace();
                }
            }
        }

        ps.println("copying: " + used.size() + " classes");
        // now copy all used files
        for (Iterator<String> e = used.keySet().iterator(); e.hasNext();) {
            String key = e.next();
            //      String fName = (String)used.get(key);
            int idx = key.lastIndexOf('/');
            String p = idx < 0 ? "" : key.substring(0, idx);
            File dd = new File(args[1] + '/' + p);
            File dst = new File(dd, key.substring(idx + 1) + ".class");
            dd.mkdirs();
            InputStream fis = zcl.getResourceAsStream(key + ".class");
            FileOutputStream fos = new FileOutputStream(dst);
            byte b[] = new byte[fis.available()];
            int len = b.length;
            while (len > 0) {
                int n = fis.read(b);
                fos.write(b, 0, n);
                len -= n;
            }
            fis.close();
            fos.close();
        }

    }

    private static String readFile(String string) throws IOException {
        FileInputStream fis = new FileInputStream(string);
        int len = fis.available();
        byte b[] = new byte[len];
        fis.read(b);
        return new String(b, 0);
    }

    /**
     * Method toSlash.
     * 
     * @param cName
     * @return String
     */
    private static String toSlash(String cName) {
        for (int idx = cName.indexOf('.'); idx >= 0; idx = cName.indexOf('.')) {
            cName = cName.substring(0, idx) + '/' + cName.substring(idx + 1);
        }
        return cName;
    }

    protected void handleDir(String path, String dir) {
        // nada    
    }

    @Override
    protected void handleFile(String path, String fileName) {
        if (fileName.endsWith(".class")) {
            File file = new File(this.getBaseDir() + path, fileName);
            String pf = path.substring(1) + "/" + fileName.substring(0, fileName.length() - 6);
            toProcess.add(pf);
            exist.put(pf, file.lastModified());
        }
    }

    /**
     * parse the command line for options and return other parameters.
     */
    private String[] doOptions(String args[]) throws Exception {
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
                if (o.equals("-j")) {
                    processJava = true;
                    --i;
                    continue;
                }
            } catch (Exception e) {
                throw new Exception("Invalid parameter for '" + args[i - 2] + "'");
            }
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        if (j < 3) {
            throw new Exception("");
        }

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }

    private static void showUsage() {
        System.out.println("collects classes from <inpath> and puts them into <outpath> if the class is referenced");
        System.out.println("USAGE: java -jar bb_uses.jar [-?] [-l <logfile>] <inpath> <outpath> [<entrypoints>]");
        System.out.println("  -?            display this message");
        System.out.println("  -l <logfile>  write output info into file");
        System.out.println("  -j            also process the java package");
        System.out.println("bb_uses.jar $Revision: 1.3 $ (c) 2002-2012 by Stefan Bebbo Franke");
    }
}
/**
 * Log: $Log: Main.java,v $
 * Log: Revision 1.3  2014/09/22 09:34:57  bebbo
 * Log: @B use the jar file date if file inside of jar
 * Log:
 * Log: Revision 1.2  2013/12/16 07:22:32  bebbo
 * Log:  @N added switch -j -> also java classes are processed and copied
 * Log: Log: Revision 1.1 2012/08/11 20:00:25 bebbo Log: @I working stage Log: Log: Revision 1.2
 * 2002/11/28 21:20:36 bebbo Log: @R removed intermediate output Log: Revision 1.1 2002/11/18 12:00:12 bebbo Log: @N
 * first version
 */
