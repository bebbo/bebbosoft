/* 
 * Created on 15.12.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.bejy.mail;

import de.bb.util.LogFile;


class Recover implements Runnable
{
  private LogFile logFile;
  private MailCfg cfg;
  private boolean createUsers;
  private boolean createDomains;
  Recover(LogFile _logFile, MailCfg cfg, boolean createUsers, boolean createDomains)
  {
    logFile = _logFile;
    this.cfg = cfg;
    this.createUsers = createUsers;
    this.createDomains = createDomains;
  }

  /** (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run()
  {
    MailCfg myCfg = cfg;
    if (cfg == null)
      return;
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    logFile.writeDate("recover: start");
    MailDBI dbi = null;
    try
    {
      dbi = myCfg.getDbi(this);
      dbi.recoverFiles(createUsers, createDomains);
    } catch (Exception e)
    {
      logFile.writeDate("recover: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (dbi != null)
        myCfg.releaseDbi(this, dbi);
    }
    logFile.writeDate("recover: stop");
    close();
  }

  /**
   * close and free resources.
   */
  void close()
  {
    cfg = null;
  }
}