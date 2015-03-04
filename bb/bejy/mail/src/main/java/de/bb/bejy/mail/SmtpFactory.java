/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/SmtpFactory.java,v $
 * $Revision: 1.29 $
 * $Date: 2013/03/07 12:44:45 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
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

package de.bb.bejy.mail;

import de.bb.util.LogFile;
import de.bb.util.SessionManager;

/**
 * @author sfranke
 */
public class SmtpFactory extends MailFactory {
    private final static String PROPERTIES[][] = {
            {"validateServer", "validate the domain name and ip address of the sender server", "false"},
            {"validateSender", "validate the sender email address", "false"},
            {
                    "virusScanner",
                    "a full path to an virusscanner which returns 0 on ok, not 0 on error."
                            + " Parameter is the mail file name.", ""},
            {"virusScanTimeout", "allowed timeout to check for viruses in ms", "60000"},
            {"greyListBlock", "interval to initally reject delivery in minutes", "5"},
            {"greyListAccept", "interval to initally accept redelivery in minutes", "120"},
            {"greyListKeep", "interval to keep grey list permissions in minutes", "10080"}};

    final static SessionManager<String, String> GREYLIST1 = new SessionManager<String, String>(1000L * 60 * 5);
    final static SessionManager<String, String> GREYLIST2 = new SessionManager<String, String>(1000L * 60 * 60 * 2);
    final static SessionManager<String, String> GREYLIST3 = new SessionManager<String, String>(
            1000L * 60 * 60 * 24 * 7, 100000);

    /**
   * 
   */
    public SmtpFactory() {
        init("implements the SMTP protocol", PROPERTIES);
    }

    /**
     * Return the name of this protocol.
     * 
     * @return the name of this protocol.
     */
    public String getName() {
        return "SMTP";
    }

    /**
     * Override the id for further extensions.
     * 
     * @author bebbo
     * @return an ID to override the Configurator ID.
     */
    public String getId() {
        return "de.bb.bejy.mail.smtp.protocol";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Factory#create()
     */
    public de.bb.bejy.Protocol create() throws Exception {
        return new Smtp(this, logFile);
    }

    /**
     * Apply the grey list timeout settings.
     */
    @Override
    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);

        // values in minutes
        final int greyListBlock = getIntProperty("greyListBlock", 5);
        final int greyListAccept = getIntProperty("greyListAccept", 120);
        final int greyListKeep = getIntProperty("greyListKeep", 10080);

        // 1 minute = 60000 milliseconds
        GREYLIST1.setTimeout(60000L * greyListBlock);
        GREYLIST2.setTimeout(60000L * greyListAccept);
        GREYLIST3.setTimeout(60000L * greyListKeep);
    }
}

/******************************************************************************
 * $Log: SmtpFactory.java,v $
 * Revision 1.29  2013/03/07 12:44:45  bebbo
 * @R grey listing uses a longer timeout if the resolved sender name contains any part of its ip address.
 * Revision 1.28 2012/11/08 12:16:30 bebbo
 * 
 * @N added grey listing Revision 1.27 2005/11/30 07:10:09 bebbo
 * 
 * @R guessPermission is now configured via database
 * 
 *    Revision 1.26 2004/12/16 16:02:30 bebbo
 * @R database connections are now shared
 * 
 *    Revision 1.25 2004/12/13 15:40:22 bebbo
 * @N added sender validation - experimental (but not bad)
 * 
 *    Revision 1.24 2003/07/09 18:29:54 bebbo
 * @N added default values.
 * 
 *    Revision 1.23 2003/06/24 09:34:48 bebbo
 * @N added functionality to validate mail sending server and sender
 * 
 *    Revision 1.22 2003/06/23 15:20:30 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.21 2003/06/17 10:20:18 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.20 2003/04/02 09:25:11 bebbo
 * @R removed DNS to avoid spam, since it is not correct (sigh)
 * 
 *    Revision 1.19 2003/03/31 16:35:06 bebbo
 * @N added DNS support to verify mail sender
 * 
 *    Revision 1.18 2002/12/19 14:53:56 bebbo
 * @R compacted different Dbi implementations
 * @B fixed some missing close statements
 * 
 *    Revision 1.17 2002/12/17 14:01:38 bebbo
 * @B fixed a to early ResultSet close
 * @N added a recovery function for lost and still available mails
 * 
 *    Revision 1.16 2002/11/19 18:13:00 bebbo
 * @R removed migration support
 * 
 *    Revision 1.15 2002/11/19 14:35:53 bebbo
 * @R removed migration support from old version to current version
 * @N separated mail storage into separate class.
 * @R MailFile is now using subdirectories since ext2fs is so slow...
 * 
 *    Revision 1.14 2002/05/16 15:21:03 franke
 * @I added delayed cleanup job 5 seconds after startup
 * 
 *    Revision 1.13 2002/02/16 13:57:55 franke
 * @V now reflecting implementions version number (not factory)
 * 
 *    Revision 1.12 2002/01/20 18:21:56 franke
 * @B fixes in migration
 * 
 *    Revision 1.11 2002/01/20 18:11:07 franke
 * @B no immediate start of cleanup during migration
 * 
 *    Revision 1.10 2001/09/15 08:49:10 bebbo
 * @I using XmlFile instead of ConfigFile
 * 
 *    Revision 1.9 2001/04/16 16:23:24 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.8 2001/04/16 13:44:08 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.7 2001/03/30 17:27:38 bebbo
 * @R factory.load got an additional parameter
 * 
 *    Revision 1.6 2001/03/27 19:50:15 bebbo
 * @I now Protocl knows its Factory
 * @I now Factory is known by Protocol
 * 
 *    Revision 1.5 2001/03/05 17:50:06 bebbo
 * @I spooler and cleanup are only created once now!
 * 
 *    Revision 1.4 2001/02/25 17:07:38 bebbo
 * @B Cleanup MailDBI object are now correctly initialized
 * 
 *    Revision 1.3 2001/02/20 19:13:22 bebbo
 * @B fixed Spooler init
 * 
 *    Revision 1.2 2001/02/20 17:39:00 bebbo
 * @B now mailDbi: init function is now invoked
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *    Revision 1.2 2000/12/30 09:08:16 bebbo
 * @I added Cron
 * 
 *    Revision 1.1 2000/12/28 20:54:41 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
