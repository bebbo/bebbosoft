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

