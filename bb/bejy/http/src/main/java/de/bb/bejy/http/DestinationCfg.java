/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/DestinationCfg.java,v $
 * $Revision: 1.6 $
 * $Date: 2012/12/15 19:36:16 $
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

/**
 * @author bebbo
 */
public class DestinationCfg extends Configurable implements Configurator {
    private final static String PROPERTIES[][] =
            {{"uri",
                    "the destination, must be a domain:port combination, e.g. www.aaa.bx:80. Context remapping is partially supported."},};

    public DestinationCfg() {
        init("destination", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new DestinationCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "where the request is forwarded to. Multiple destinations are possible.";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy.http.redir.vpath";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.http.redir.destination";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "dest " + getProperty("uri");
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "destination";
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

    /*
     * (non-Javadoc)
     * @see de.bb.bejy.Configurable#useParentActivate()
     */
    public boolean useParentActivate() {
        return true;
    }

}

/******************************************************************************
 * $Log: DestinationCfg.java,v $
 * Revision 1.6  2012/12/15 19:36:16  bebbo
 * @R better name in admin UI
 * @F fromatted
 * Revision 1.5 2003/08/04 08:34:30 bebbo
 * 
 * @B fixed description for destinations
 * 
 *    Revision 1.4 2003/07/09 18:29:49 bebbo
 * @N added default values.
 * 
 *    Revision 1.3 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.2 2003/06/17 12:09:56 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.1 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 */
