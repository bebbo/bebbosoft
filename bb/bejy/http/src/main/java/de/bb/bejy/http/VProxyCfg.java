/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/VProxyCfg.java,v $
 * $Revision: 1.3 $
 * $Date: 2012/12/15 19:38:20 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
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

/******************************************************************************
 * $Log: VProxyCfg.java,v $
 * Revision 1.3  2012/12/15 19:38:20  bebbo
 * @N proxy support
 * Revision 1.2 2012/11/08 12:14:12 bebbo
 * 
 * @B fixed proxy with HTTP chunked mode
 * @N added SOCKS5 proxy support
 * @N added a fallback option for XML data -> XMPP server Revision 1.1 2010/07/08 18:16:25 bebbo
 * 
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 */
