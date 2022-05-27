/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Factory.java,v $
 * $Revision: 1.23 $
 * $Date: 2007/04/13 14:24:13 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * base class for all protocol factories
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

import de.bb.util.*;

/**
 * <p>base class for all protocol factories</p>
 * <p>In the configuration there is a protocol assigned to each server. 
 * That means the class of the protocol factory is specified.
 * For each new server object the factory's create() function is invoked to create a new protocol object.
 * The server keeps that protocol object and manages the sockets and input/output streams for that protocol object.
 * Depending on the return value the socket is kept alive or closed.
 * If an Exception is caught, the server and the protocol object are discarded.
 * The mechanism behind (ThreadManager) creates server objects on demand.
 * 
 * @author Stefan Franke
 * @see Protocol
 */
public abstract class Factory extends Configurable implements Loadable
{
  private final static String PROPERTIES[][] = 
  {
    {
      "logFile", "force an own log file"
    },{
      "logFileDateFormat", "the format log file format", "yyyyMMdd"
    },{
      "verbose", "adds verbose debug information"
    }
  };
  
  /**
   * name of the current Factory without package.
   * e.g. Pop3
   */
  protected String name;

  /**
   * a logfile for this protocol.
   */
  protected LogFile logFile;
 
  protected Factory()
  {
    init("factory", PROPERTIES);
  }
 
  /**
   * is invoked to create a new protocol object.
   * @return a Protocol object
   * @throws Exception on error.
   */
  public abstract Protocol create() throws Exception;

  /**
   * Returns true if the logging is set to verbose.
   * @return true if the logging is set to verbose.
   */
  public boolean isVerbose() { return "true".equals(getProperty("verbose")); }

  /**
   * Returns the id of the Configurator.
   * @return the id of the Configurator.
   */
  public String getImplementationId()
  {
    return "de.bb.bejy.protocol";
  }


  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#activate(de.bb.util.LogFile)
   */
  public void activate(LogFile logFile) throws Exception
  {
    if (this.logFile == null)
      this.logFile = logFile;
    String lfn = getProperty("logFile");
    if (lfn != null && lfn.length() > 0) {
      String fmt = getProperty("logFileDateFormat");
      this.logFile = new LogFile(lfn, fmt);
    }
  }

  /**
   * Get the current log file.
   * @return a LogFile instance
   */
  public LogFile getLogFile()
  {
    return logFile;
  }

}
 
/******************************************************************************
 * $Log: Factory.java,v $
 * Revision 1.23  2007/04/13 14:24:13  bebbo
 * @N added logFileDateFormat
 *
 * Revision 1.22  2007/01/18 21:42:41  bebbo
 * @N added getLogFile()
 *
 * Revision 1.21  2006/02/06 09:12:59  bebbo
 * @N added verbose property
 *
 * Revision 1.20  2003/08/07 07:12:18  bebbo
 * @R logFile is only overwritten with default logFile, if it is null
 *
 * Revision 1.19  2003/08/04 08:32:59  bebbo
 * @N added logFile for all protocols
 *
 * Revision 1.18  2003/06/24 19:47:34  bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 *
 * Revision 1.17  2003/06/20 09:09:31  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.16  2003/06/18 08:36:56  bebbo
 * @R modification, dynamic loading, removing - all works now
 *
 * Revision 1.15  2003/06/17 15:13:36  bebbo
 * @R more changes to enable on the fly config updates
 *
 * Revision 1.14  2003/06/17 10:18:10  bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 *
 * Revision 1.13  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 * Revision 1.12  2003/02/05 08:07:57  bebbo
 * @N new configurable attribute "verbose"
 *
 * Revision 1.11  2002/08/21 09:14:43  bebbo
 * @R changes for the admin UI
 *
 * Revision 1.10  2002/02/16 14:01:13  franke
 * @V now reflecting implementions version number (not factory)
 *
 * Revision 1.9  2001/09/15 08:44:50  bebbo
 * @I using XmlFile instead of ConfigFile
 *
 * Revision 1.8  2001/08/24 08:24:16  bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 *
 * Revision 1.7  2001/04/16 13:43:26  bebbo
 * @I changed IniFile to XmlFile
 *
 * Revision 1.6  2001/03/30 17:27:15  bebbo
 * @R factory.load got an additional parameter
 *
 * Revision 1.5  2001/03/27 19:47:05  bebbo
 * @I now is known by Protocol
 *
 * Revision 1.4  2001/02/20 17:38:26  bebbo
 * @B now member ini is initialized properly
 *
 * Revision 1.3  2001/02/19 19:55:43  bebbo
 * @I logFile is taken from server Entry
 *
 * Revision 1.2  2000/12/30 09:02:32  bebbo
 * @? dunno
 *
 * Revision 1.1  2000/12/28 20:53:24  bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 *
 *****************************************************************************/