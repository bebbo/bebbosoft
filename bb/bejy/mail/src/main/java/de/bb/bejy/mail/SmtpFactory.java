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
            {"greyListBlock", "interval to initally reject delivery in minutes", "30"},
            {"greyListAccept", "interval to initally accept redelivery in minutes", "300"},
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
