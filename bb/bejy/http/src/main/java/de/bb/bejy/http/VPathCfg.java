/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/VPathCfg.java,v $
 * $Revision: 1.5 $
 * $Date: 2012/12/15 19:36:19 $
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
public class VPathCfg extends Configurable implements Configurator {
    private final static String PROPERTIES[][] = {{"path", "the forwarded base path"},
            {"group", "a group to add access protection."},
            {"userHeader", "the header to set with the user name if authenticated."}};

    public VPathCfg() {
        init("forwarded path", PROPERTIES);
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new VPathCfg();
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "to define the URL path. Normally / is used to forward everything.";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy.http.redir.vhost";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.http.redir.vpath";
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "fwd path: " + getProperty("path");
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "vpath";
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
 * $Log: VPathCfg.java,v $
 * Revision 1.5  2012/12/15 19:36:19  bebbo
 * @R better name in admin UI
 * @F fromatted
 * Revision 1.4 2012/11/14 15:07:32 bebbo
 * 
 * @F format Revision 1.3 2003/06/20 09:09:38 bebbo
 * 
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.2 2003/06/17 12:09:56 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.1 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 */
