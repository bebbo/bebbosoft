/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/GlobalCfg.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:41:25 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import de.bb.util.LogFile;

/**
 * Represents the global configuration.
 * @author bebbo
 */
public class GlobalCfg extends Configurable implements Configurator
{
  private final static String[][] PROPERTIES =
  {
    {"javac" , "command line of the java compiler executable"},
    {"logFile", "the default log file, use * for stdout"},
  };

  public GlobalCfg()
  {
    init("global", PROPERTIES);
  }

  /** 
   * return the description.
   * @return the description.
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "global settings";
  }

  /**
   * return the name.
   * @return the name. 
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "global";
  }

  /**
   * return the path.
   * @return the path.
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "global";
  }

  /**
   * no modules required.
   * @return always null 
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

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.global";
  }

  /**
   * return this, since Global is a singleton.
   * @return this, since Global is a singleton.
   * @see de.bb.bejy.Configurator#create()
   */
  public Configurable create()
  {
    return this;
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#activate(de.bb.util.LogFile)
   */
  public void activate(LogFile logFile) throws Exception
  {
    String javac = getProperty("javac");
    if (javac != null)
    {
      System.setProperty("javac", javac);
    }
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
   */
  public void update(LogFile logFile) throws Exception
  {
    activate(logFile);
  }

}

/******************************************************************************
 * $Log: GlobalCfg.java,v $
 * Revision 1.1  2004/04/16 13:41:25  bebbo
 * @R changed start behaviour
 *
 * Revision 1.8  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.7  2003/06/24 19:47:34  bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 *
 * Revision 1.6  2003/06/18 13:54:46  bebbo
 * @R modified some descriptions
 * @R removed mainDomain from global
 * @B fixed activate of Dns
 *
 * Revision 1.5  2003/06/18 13:44:18  bebbo
 * @R modified some descriptions
 *
 * Revision 1.4  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.3  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 */
