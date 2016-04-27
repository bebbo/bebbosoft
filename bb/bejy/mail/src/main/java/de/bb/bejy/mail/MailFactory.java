/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2016.
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
