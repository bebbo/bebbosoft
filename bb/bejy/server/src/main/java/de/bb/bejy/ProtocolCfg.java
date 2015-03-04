/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/ProtocolCfg.java,v $
 * $Revision: 1.3 $
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
 * the configuration class for all protocols.
 * @author bebbo
 */
public class ProtocolCfg implements Configurator
{

  /**
   * return the name.
   * @return the name
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "tcp/ip protocol";
  }

  /**
   * return the description.
   * @return the description.
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "a protocol implementation";
  }

  /**
   * return the path.
   * @return the path.
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "protocol";
  }

  /**
   * return null, since there are no dependencies - only the extended class.
   * @return null, since there are no dependencies.
   * @see de.bb.bejy.Configurator#getRequired()
   */
  public String getRequired()
  {
    return null;
  }
  
  /**
   * return the extension id.
   * @return the extension id 
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.server";
  }

  /**
   * return the own id. 
   * @return the own id.
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.protocol";
  }
  /**
   * return null, since protocols are loaded dynamically.
   * @return null, since protocols are loaded dynamically.
   * @see de.bb.bejy.Configurator#create()
   */
  public Configurable create()
  {
    return null;
  }
  /**
   * return true, since protocols are loaded dynamically.
   * @return true, since protocols are loaded dynamically.
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return true;
  }
}


/******************************************************************************
 * $Log: ProtocolCfg.java,v $
 * Revision 1.3  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.2  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 */