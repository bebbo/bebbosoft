/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/Cleanup.java,v $
 * $Revision: 1.10 $
 * $Date: 2013/03/07 12:38:26 $
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

class Cleanup implements Runnable {
    private LogFile logFile;
    private MailCfg cfg;

    Cleanup(LogFile logFile, MailCfg cfg) {
        this.logFile = logFile;
        this.cfg = cfg;
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        MailCfg myCfg = cfg;
        if (cfg == null)
            return;
        if (myCfg.isRecovering())
            return;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        logFile.writeDate("cleanup: start");
        MailDBI dbi = null;
        try {
            dbi = myCfg.getDbi(this);
            dbi.globalCleanup();
        } catch (Exception e) {
            logFile.writeDate("cleanup: " + e.getMessage());
        } finally {
            if (dbi != null)
                myCfg.releaseDbi(this, dbi);
        }
        logFile.writeDate("cleanup: stop");
    }

    /**
     * close and free resources.
     */
    void close() {
        cfg = null;
    }
}

/******************************************************************************
 * $Log: Cleanup.java,v $
 * Revision 1.10  2013/03/07 12:38:26  bebbo
 * @F formatted
 * Revision 1.9 2006/05/09 08:39:53 bebbo
 * 
 * @B cleanup could run only once - removed invalid close() invokation
 * 
 *    Revision 1.8 2005/11/30 05:50:01 bebbo
 * @B prevented double instancing on looong runs
 * 
 *    Revision 1.7 2004/12/16 16:01:17 bebbo
 * @R database connections are now shared
 * 
 *    Revision 1.6 2003/06/23 15:20:29 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.5 2002/11/19 17:25:25 bebbo
 * @I now running with low priority
 * 
 *    Revision 1.4 2002/11/19 12:34:09 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.3 2002/01/20 12:02:57 franke
 * @R mail table is obsolete. Spooler is now using imap_mime to keep mails. Did all necessary changes due to that
 * 
 *    Revision 1.2 2001/02/25 17:09:15 bebbo
 * @I invokes globalCleanup() too
 * 
 *    Revision 1.1 2001/02/19 19:56:16 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *    Revision 1.1 2000/12/30 09:07:54 bebbo
 * @N performs database and mail file cleanup, started by Cron
 * 
 *    Revision 1.1 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
