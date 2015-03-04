/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MailCfg.java,v $
 * $Revision: 1.23 $
 * $Date: 2013/05/17 11:00:28 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy.mail;

import java.sql.Driver;
import java.sql.SQLException;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.bejy.Dns;
import de.bb.util.LogFile;
import de.bb.util.Pool;

/**
 * @author bebbo
 */
public class MailCfg extends Configurable implements Configurator {
    private final static Object PROPERTIES[][] =
            {
                    {"mailFolder", "path to the mail folder", "mail"},
                    {"jdbcUrl", "the JDBC url to establish the database connection"},
                    {"jdbcDriver", "class name of the JDBC driver implementation", Driver.class},
                    {"mailDbi", "class name of the user mail database implementation", MailDBI.class},
                    {"mainDomain", "domain used in HELO of this mail server"},
                    {"sendThreads", "the max count of sending Threads", "3"},
                    {"dbiMaxCount", "the max count of database connections", "25"},
                    {"dbiThreshold", "the max count of unused database connections", "2"},
                    {"shortIntervall",
                            "the time intervall in minutes used between delivery retries before intervall switch", "5"},
                    {"longIntervall", "the time intervall used between delivery retries after intervall switch", "30"},
                    {
                            "intervallSwitch",
                            "the count of delivery retries until intervall switches and a 2nd mail notification is sent",
                            "12"}, {"maxRetries", "the max count of delivery retries", "60"},
                    {"logFile", "name of an own log file"},};

    static Dns dns;

    private Spooler spooler;

    private Cleanup cleanup;

    String mailFolder;

    String jdbcUrl;

    Class clazz;

    private String mainDomain;

    LogFile logFile;

    private Pool pool;

    /**
   * 
   */
    public MailCfg() {
        init("mail", PROPERTIES);
        Config.addGlobalUnique(getPath());
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "mail";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "mail configuration";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "mail";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.mail.config";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#getRequired()
     */
    public String getRequired() {
        return "de.bb.bejy.dns";
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#create()
     */
    public de.bb.bejy.Configurable create() {
        return new MailCfg();
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#activate(de.bb.util.LogFile)
     */
    public void activate(LogFile logFile) throws Exception {
        String lfn = getProperty("logFile");
        if (lfn != null && lfn.length() > 0)
            logFile = new LogFile(lfn);

        this.logFile = logFile;

        jdbcUrl = getProperty("jdbcUrl");
        mailFolder = getProperty("mailFolder");
        mainDomain = getProperty("mainDomain");

        String dName = getProperty("jdbcDriver");
        try {
            clazz = Class.forName(dName);
            clazz.newInstance();
        } catch (Exception e) {
            throw new Exception("cannot load jdbcDriver: " + dName);
        }

        String iName = getProperty("mailDbi");
        try {
            clazz = Class.forName(iName);
        } catch (Exception e) {
            throw new Exception("cannot load mailDbi: " + iName);
        }

        dns = (Dns) Config.getInstance().getChild("dns");
        if (dns == null)
            throw new Exception("module de.bb.bejy.dns is not available");

        if (pool != null) {
            pool.setMaxCount(0);
            pool.setThreshold(0);
            pool.validate();
        } else {
            pool = new Pool(new Pool.Factory() {
                public Object create() throws Exception {
                    MailDBI dbi = (MailDBI) clazz.newInstance();
                    dbi.setLogFile(MailCfg.this.logFile);
                    dbi.setJdbcUrl(jdbcUrl);
                    dbi.setMailPath(mailFolder);
                    dbi.checkConnection();

                    if (getBooleanProperty("debug", false))
                        dbi.DEBUG = true;
                    return dbi;
                }

                public void destroy(Object o) {
                    MailDBI dbi = (MailDBI) o;
                    dbi.close();
                }

                public boolean validate(Object o) {
                    MailDBI dbi = (MailDBI) o;
                    try {
                        dbi.checkConnection();
                        return true;
                    } catch (SQLException e) {
                    }
                    return false;
                }

                public boolean isBusy(Object object) {
                    return false;
                }

                public boolean validateKey(Object key) {
                    if (key instanceof Thread) {
                        Thread t = (Thread) key;
                        return t.isAlive();
                    }
                    return true;
                }
            });
        }

        pool.setThreshold(getIntProperty("dbiThreshold", 2));
        pool.setMaxCount(getIntProperty("dbiMaxCount", 25));

        if (spooler == null) {

            // restart all pending entries
            MailDBI dbi = getDbi(this);
            dbi.resetSpoolEntries();
            releaseDbi(this, dbi);

            spooler = new Spooler(logFile, this);

            spooler.init(this);

            spooler.start();

            // run recover if enabled
            if ("true".equals(getProperty("recover", null))) {
                Recover recoverThread = new Recover(logFile, this, false, false);
                Config.getCron().runIn("recover e-mail", recoverThread, 0);
            }

            // run each 24 hours - but add only if it does not yet exist
            if (cleanup == null) {
                cleanup = new Cleanup(logFile, this);
                Config.getCron().runEvery("e-mail garbage collector", cleanup, /*24 * 60 * 60 */1000L,
                        24 * 60 * 60 * 1000L);
            }
        }
    }

    /**
     * Return a database implementation.
     * 
     * @param key
     * @return a database implementation.
     * @throws Exception
     */
    public MailDBI getDbi(Object key) throws Exception {
        try {
            if (pool == null)
                throw new Exception("there is no JDBC connection!");
            return (MailDBI) pool.obtain(key);
        } catch (Exception ex) {
            logFile.writeDate("MailDBI.getDbi " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * @param key
     * @param dbi
     */
    public void releaseDbi(Object key, MailDBI dbi) {
        pool.release(key, dbi);
    }

    /**
     * @return
     */
    String getMainDomain() {
        return mainDomain;
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */
    public void deactivate(LogFile logFile) throws Exception {
        if (pool == null)
            return;
        pool.setThreshold(0);
        pool.setMaxCount(0);

        /*
            if (recoverThread != null)
              recoverThread.close();
            recoverThread = null;
        */
        if (cleanup != null)
            cleanup.close();
        Config.getCron().remove(cleanup);
        cleanup = null;

        if (spooler != null)
            spooler.close();
        spooler = null;
        super.deactivate(logFile);
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
     */
    public void update(LogFile logFile) throws Exception {
        deactivate(logFile);
        activate(logFile);
    }

    /**
     * @return
     */
    public LogFile getLogFile() {
        return logFile;
    }

    public Spooler getSpooler() {
        return spooler;
    }

    public boolean isRecovering() {
        return MailDBI.isRecovering;
    }

    public void recoverMails(boolean createUsers, boolean createDomains) {
        if (isRecovering())
            return;
        Recover recoverThread = new Recover(logFile, this, createUsers, createDomains);
        Config.getCron().runIn("recover e-mail", recoverThread, 0);
    }
}

/******************************************************************************
 * $Log: MailCfg.java,v $
 * Revision 1.23  2013/05/17 11:00:28  bebbo
 * @N new DEBUG property
 * Revision 1.22 2013/03/15 21:45:20 bebbo
 * 
 * @N shutdown might leave emails in the spooler. Now the start resets the state of such emails. Revision 1.21
 *    2012/08/11 19:26:19 bebbo
 * 
 * @B fixed NPE Revision 1.20 2008/03/13 20:46:24 bebbo
 * 
 * @L better log output if no DB connections exists
 * 
 *    Revision 1.19 2006/05/09 08:42:08 bebbo
 * @B fixed creation of multiple cleanup instances on config reloads
 * 
 *    Revision 1.18 2006/03/17 11:34:29 bebbo
 * @R removed old options
 * @B pool is only created once - solves problems on restarts and use of old keys in new pool
 * 
 *    Revision 1.17 2005/12/31 15:45:00 bebbo
 * @I made some members package accessible
 * 
 *    Revision 1.16 2005/12/11 20:25:08 bebbo
 * @B changed return code in Factory for isIdle()
 * 
 *    Revision 1.15 2005/11/30 06:07:38 bebbo
 * @B the DBI pool is now correctly reinstantiated on reactivations or reloads
 * @N added support to start recovering by admin UI
 * @N added support to supervise background threads
 * 
 *    Revision 1.14 2004/12/16 16:02:30 bebbo
 * @R database connections are now shared
 * 
 *    Revision 1.13 2004/04/07 16:33:46 bebbo
 * @V new version message
 * 
 *    Revision 1.12 2003/10/01 14:19:42 bebbo
 * @N added recover switch
 * 
 *    Revision 1.11 2003/09/25 10:24:51 bebbo
 * @R removed PublicMailDbi
 * 
 *    Revision 1.10 2003/09/08 14:54:41 bebbo
 * @N added functions to maintain forwarder, notifications and auto responses
 * @N added sending of autoresponses
 * 
 *    Revision 1.9 2003/08/07 07:38:27 bebbo
 * @B common logFile is now also applied to Spooler and Cleanup threads
 * 
 *    Revision 1.8 2003/08/07 07:16:48 bebbo
 * @N added a logFile to MailCfg which is default for all mail protocols
 * 
 *    Revision 1.7 2003/07/09 18:29:54 bebbo
 * @N added default values.
 * 
 *    Revision 1.6 2003/07/01 12:38:06 bebbo
 * @R makeDbi() is public now
 * 
 *    Revision 1.5 2003/07/01 10:53:37 bebbo
 * @N enabled class browsing for JDBC driver and MailDBIs
 * 
 *    Revision 1.4 2003/06/24 10:11:20 bebbo
 * @R mail config is now unique
 * 
 *    Revision 1.3 2003/06/23 15:20:30 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.2 2003/06/17 12:09:59 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.1 2003/06/17 10:20:18 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.1 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 */
