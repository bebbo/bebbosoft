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
package de.bb.bejy.http;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class VProxyCfg extends Configurable implements Configurator {
    private final static String PROPERTIES[][] = {{"group", "the user group to protect this proxy", ""},
            {"xmpp", "redirect to a xmpp server", ""}, {"socks5", "the user group for socks5 users", ""},};
    private boolean inActivate;

    public VProxyCfg() {
        init("group", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new VProxyCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "a proxy for CONNECT requests";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy.http.redir.protocol";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.http.redir.vproxy";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        String group = getProperty("group");
        if (group == null || group.length() == 0)
            return "proxy";
        return "proxy for group: " + group;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "vproxy";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getRequired()
     */
    public String getRequired() {
        return null;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    /**
     * pass it to the parent configurable.
     * 
     * @param logFile
     *            the current logfile
     */
    public void activate(LogFile logFile) throws Exception {
        if (inActivate)
            return;
        try {
            inActivate = true;
            getParent().activate(logFile);
        } finally {
            inActivate = false;
        }
    }

    public void deactivate(LogFile logFile) throws Exception {
        if (inActivate)
            return;
        try {
            inActivate = true;
            getParent().deactivate(logFile);
        } finally {
            inActivate = false;
        }
    }
}
