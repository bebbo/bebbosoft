/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/WarCfg.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/06/20 09:09:38 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy.http;

import de.bb.bejy.Configurator;

/**
 * @author bebbo
 */
public class WarCfg implements Configurator
{

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "war";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "allows to load a single WAR file and map it to a URL context";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "war";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.war";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.http.host";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getRequired()
   */
  public String getRequired()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#create()
   */
  public de.bb.bejy.Configurable create()
  {
    return new WebAppContext();
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
}


/******************************************************************************
 * $Log: WarCfg.java,v $
 * Revision 1.3  2003/06/20 09:09:38  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.2  2003/06/17 12:09:56  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:41:46  bebbo
 * @N added config classes for future runtime configuration support
 *
 */