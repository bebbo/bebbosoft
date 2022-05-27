/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/ServerCfg.java,v $
 * $Revision: 1.4 $
 * $Date: 2003/10/01 12:01:51 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

/**
 * Configuration class for a server.
 * @author bebbo
 */
public class ServerCfg implements Configurator
{

  /**
   * return the name.
   * @return the name.
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "tcp/ip server";
  }

  /**
   * return the description.
   * @return the description.
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "which uses a configurable protocol implementation";
  }

  /**
   * return the path.
   * @return the path.
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "server";
  }

  /**
   * return null, since there are no further dependencies.
   * @return null, since there are no further dependencies.
   * @see de.bb.bejy.Configurator#getRequired()
   */
  public String getRequired()
  {
    return null;
  }
  
  /**
   * return the extension id.
   * @return the extension id.
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy";
  }

  /**
   * return own id.
   * @return own id.
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.server";
  }
  /**
   * return a new created server instance.
   * @return a new created server instance.
   * @see de.bb.bejy.Configurator#create()
   */
  public Configurable create()
  {
    return new Server();
  }
  /**
   * return false - no dynamic loading.
   * @return false - no dynamic loading.
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
}


/******************************************************************************
 * $Log: ServerCfg.java,v $
 * Revision 1.4  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.3  2003/06/20 09:09:32  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.2  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 */