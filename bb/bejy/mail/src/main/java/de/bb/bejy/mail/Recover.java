/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
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