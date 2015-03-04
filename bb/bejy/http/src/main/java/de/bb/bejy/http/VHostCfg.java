/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/VHostCfg.java,v $
 * $Revision: 1.5 $
 * $Date: 2012/12/15 19:36:17 $
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
public class VHostCfg extends Configurable implements Configurator {
    private final static String PROPERTIES[][] = {{"host", "the forwarded host"},};

    public VHostCfg() {
        init("forwarded host", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new VHostCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "to define which URLs are forwarded to which destinations";
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
        return "de.bb.bejy.http.redir.vhost";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        String host = getProperty("host");
        if (host == null)
            return "fwd";
        return host;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "vhost";
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
 * $Log: VHostCfg.java,v $
 * Revision 1.5  2012/12/15 19:36:17  bebbo
 * @R better name in admin UI
 * @F fromatted
 * Revision 1.4 2003/07/30 10:09:49 bebbo
 * 
 * @R enahanced information in admin interface
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
