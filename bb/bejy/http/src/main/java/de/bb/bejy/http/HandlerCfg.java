/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HandlerCfg.java,v $
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
public class HandlerCfg implements Configurator
{

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "handler";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "to handle incoming HTTP requests";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "handler";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.handler";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.http.context";
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
    return null;
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return true;
  }
}


/******************************************************************************
 * $Log: HandlerCfg.java,v $
 * Revision 1.3  2003/06/20 09:09:38  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.2  2003/06/17 12:09:56  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:41:45  bebbo
 * @N added config classes for future runtime configuration support
 *
 */