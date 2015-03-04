/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/ParameterCfg.java,v $
 * $Revision: 1.4 $
 * $Date: 2003/06/20 09:09:38 $
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
public class ParameterCfg extends Configurable implements Configurator
{
  private final static String PROPERTIES[][] = 
  {
    { "name", "name of the parameter"
    },
    { "value", "value of the parameter"
    }
  };

  public ParameterCfg()
  {
    init("parameter", PROPERTIES);
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return getProperty("name", "parameter");
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "which is an init parameter for the current instance, used on next activation";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "parameter";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.parameter";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.http.handler, de.bb.bejy.http.context";
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
    return new ParameterCfg();
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
 * $Log: ParameterCfg.java,v $
 * Revision 1.4  2003/06/20 09:09:38  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.3  2003/06/17 12:09:56  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.2  2003/06/17 10:18:42  bebbo
 * @R redesign to utilize the new configuration scheme
 *
 * Revision 1.1  2003/05/13 15:41:46  bebbo
 * @N added config classes for future runtime configuration support
 *
 */