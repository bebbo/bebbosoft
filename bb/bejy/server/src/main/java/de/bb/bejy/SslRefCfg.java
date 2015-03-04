/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/SslRefCfg.java,v $
 * $Revision: 1.1 $
 * $Date: 2014/09/22 09:24:39 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.bb.security.Ssl3Config;

/**
 * Configuration class for the SSL support.
 * 
 * @author bebbo
 */
public class SslRefCfg extends Configurable implements Configurator {

    private final static String PROPERTIES[][] = { { "ref", "the id of the ssl config to refer to " },
            { "byName", "true if the hostname is used to locate the ssl data" }, };

    private Ssl3Config config;

    /**
     * Create a new SslCfg.
     */
    public SslRefCfg() {
        init("sslref", PROPERTIES);
    }

    /**
     * return a new SslCfg, since it is Configurator AND Configurable.
     * 
     * @return a new SslCfg, since it is Configurator AND Configurable.
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new SslRefCfg();
    }

    /**
     * return the description.
     * 
     * @return the description.
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "refer to a ssl config";
    }

    /**
     * return the extension id.
     * 
     * @return the extension id.
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy.server";
    }

    /**
     * return the own id.
     * 
     * @return the own id.
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.server.sslref";
    }

    /**
     * return the own name.
     * 
     * @return the own name.
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "sslref";
    }

    /**
     * return the path.
     * 
     * @return the path.
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "sslref";
    }

    /**
     * return null, since no further modules are required.
     * 
     * @return null, since no further modules are required.
     * @see de.bb.bejy.Configurator#getRequired()
     */
    public String getRequired() {
        return "de.bb.bejy.ssl";
    }

    /**
     * return false, since no dynamic loading is used.
     * 
     * @return false, since no dynamic loading is used.
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    public Ssl3Config getSsl3Config() {
        if (config != null)
            return config;

        final HashMap<String, SslCfg> scs = Config.getSslConfigs();

        final String ref = getProperty("ref");
        final SslCfg sc = scs.get(ref);
        if (sc == null || !sc.isValid())
            return null;

        createConfig(sc, scs);

        return config;
    }

    private synchronized void createConfig(SslCfg sc, HashMap<String, SslCfg> scs) {
        if (config != null)
            return;

        Ssl3Config newConfig = new Ssl3Config(sc.getCerts(), sc.getKeyData(), sc.getCiphers());

        if ("true".equals(getProperty("byName"))) {
            for (final Entry<String, SslCfg> e : scs.entrySet()) {
                final SslCfg v = e.getValue();
                if (v.isValid())
                    newConfig.addHostData(e.getKey(), v.getCerts(), v.getKeyData());
            }
        }
        config = newConfig;
    }
}

/******************************************************************************
 * $Log: SslRefCfg.java,v $ Revision 1.1 2014/09/22 09:24:39 bebbo
 * 
 * @N added support for SSL host name, to choose certificate and key based on host name Revision 1.10 2014/06/23
 *    19:02:58 bebbo
 * 
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 *    Revision 1.9 2013/11/28 12:23:03 bebbo
 * @N SSL cipher types are configurable
 * @I using nio sockets Revision 1.8 2010/12/17 23:25:11 bebbo /FIXED: ssl config now supports multiple certificates
 *    Revision 1.7 2007/08/09 16:06:54 bebbo
 * 
 * @I integrated new SSL implementation
 * 
 *    Revision 1.6 2007/04/21 19:13:22 bebbo
 * @R improved speed by using the chinese remainder theorem
 * 
 *    Revision 1.5 2004/12/13 15:28:07 bebbo
 * @D added logfile entries for SSL configuration loading
 * 
 *    Revision 1.4 2004/05/06 10:42:19 bebbo
 * @B fixed possible unclosed streams
 * 
 *    Revision 1.3 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.2 2003/06/20 09:09:32 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.1 2003/06/18 15:07:04 bebbo
 * @N added ssl configuration
 * 
 */
