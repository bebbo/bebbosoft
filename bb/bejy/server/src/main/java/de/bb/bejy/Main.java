/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Main.java,v $
 * $Revision: 1.12 $
 * $Date: 2006/03/17 11:29:33 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * universal server base implementation
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

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/
 
package de.bb.bejy;

 
/**
 * Main class of BEJY, used to start it.
 */
public class Main
{
  private Main() {}
  // default config name
  private static String cfgName = "bejy.xml";

  private static boolean ui;
  
  /**
   * BEJY entry point.
   * @param args arguments for bejy. args[0] is config file name right now.
   */
  public static void main(String args[])
  {    
    // say hello :)
    System.out.println(Version.getFull());
    
    // check commandline
    try {
      doOptions(args);
    } catch (Exception e)
    {
      String s = e.getMessage();
      if (s.length() > 0)
        System.out.println(s);
      else
        showUsage();
      return;
    }
    
    // add shutdown thread
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          Config.shutdown();
        } catch (Throwable t)
        {}
      }
    });
            
    //load the config
    System.out.println("loading " + cfgName);
    Config.loadConfig(cfgName);        
    
    if (ui) {
      // Config.openUI();
    }
  }
  
  /**
   * parse the command line for options and return other parameters.
   */
  private static String [] doOptions(String args[]) throws Exception
  {
    int j = 0;
    for (int i = 0; i < args.length; ++i)
    {
      if (args[i].charAt(0) != '-') // no argument
      {
        args[j++] = args[i];
        continue;
      }
      String o = args[i++];
      // an argument
      if (o.equals("-?"))
      {
        throw new Exception(""); // show usage only!
      } 
      try {
        if (o.equals("-f"))
        {
          cfgName = args[i];
          continue;
        } 
        if (o.equals("-u"))
        {
          ui = true;
          continue;
        }
      } catch (Exception e)
      {
        throw new Exception("Invalid parameter for '" + args[i-2] + "'");   
      }
      throw new Exception("Invalid option '" + args[i-1] + "'"); 
    }     
    
    if (j > 0) 
    {
      throw new Exception("too many parameters"); 
    }
       
    String res[] = new String[j];
    System.arraycopy(args, 0, res, 0, j);
    return res;
  }
  
  private static void showUsage()
  {    
    System.out.println("\nUsage: java de.bb.bejy.Main [-?] [-f <inifile>]");
    System.out.println("  -?            display this message");
    System.out.println("  -f <xmlfile>  use given inifile for config");
  }  
}
 
/******************************************************************************
 * $Log: Main.java,v $
 * Revision 1.12  2006/03/17 11:29:33  bebbo
 * @B fixed option parsing
 *
 * Revision 1.11  2005/11/11 18:51:25  bebbo
 * @R shutdown now passes Exception
 *
 * Revision 1.10  2004/04/07 16:32:39  bebbo
 * @V new version message
 *
 * Revision 1.9  2003/07/09 18:29:45  bebbo
 * @N added default values.
 *
 * Revision 1.8  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 * Revision 1.7  2002/08/21 14:49:55  bebbo
 * @R further creation of UI
 *
 * Revision 1.6  2002/08/21 09:14:43  bebbo
 * @R changes for the admin UI
 *
 * Revision 1.5  2002/05/16 15:19:48  franke
 * @C CVS
 *
 * Revision 1.4  2001/09/15 08:45:24  bebbo
 * @ comments
 *
 * Revision 1.3  2001/04/16 16:23:11  bebbo
 * @R changes for migration to XML configfile
 *
 * Revision 1.2  2000/12/28 20:53:24  bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 *
 * Revision 1.1  2000/11/10 18:13:26  bebbo
 * @N new (uncomplete stuff)
 *
 *****************************************************************************/