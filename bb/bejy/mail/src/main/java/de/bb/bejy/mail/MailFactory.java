/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MailFactory.java,v $
 * $Revision: 1.8 $
 * $Date: 2013/11/01 13:32:02 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy.mail;

import de.bb.bejy.Config;
import de.bb.bejy.Factory;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
abstract class MailFactory extends Factory {
    MailCfg mailCfg;

    protected MailDBI getDbi(Object o) throws Exception {
        return mailCfg.getDbi(o);
    }

    protected void releaseDbi(Object o, MailDBI dbi) {
        if (dbi != null)
            mailCfg.releaseDbi(o, dbi);
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Factory#activate(de.bb.util.LogFile)
     */
    public void activate(LogFile logFile) throws Exception {
        mailCfg = (MailCfg) Config.getInstance().getChild("mail");
        if (mailCfg == null)
            throw new Exception("missing mail configuration");
        this.logFile = mailCfg.getLogFile();
        super.activate(logFile);
    }

}

/******************************************************************************
 * $Log: MailFactory.java,v $
 * Revision 1.8  2013/11/01 13:32:02  bebbo
 * @I release mailDbi only if not null
 *
 * Revision 1.7  2012/11/08 12:16:29  bebbo
 * @N added grey listing
 * Revision 1.6 2005/12/11 20:26:32 bebbo
 * 
 * @I made mailCfg package visible instead of protected
 * 
 *    Revision 1.5 2004/12/16 16:02:30 bebbo
 * @R database connections are now shared
 * 
 *    Revision 1.4 2003/08/07 07:16:47 bebbo
 * @N added a logFile to MailCfg which is default for all mail protocols
 * 
 *    Revision 1.3 2003/08/04 08:38:06 bebbo
 * @R added use of default logFile
 * 
 *    Revision 1.2 2003/06/23 15:20:30 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.1 2003/06/17 10:20:18 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 */
