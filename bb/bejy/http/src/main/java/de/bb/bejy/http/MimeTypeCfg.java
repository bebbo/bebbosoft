/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/MimeTypeCfg.java,v $
 * $Revision: 1.5 $
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
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class MimeTypeCfg extends Configurable implements Configurator
{
  private final static String PROPERTIES[][] =
    {{"extension", "file extension"},
     {"type", "the associated mime-type"}
    }
  ;
  
  public MimeTypeCfg()
  {
    init("mime type", PROPERTIES);
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    String ext = getProperty("extension");
    if (ext == null)
      return "mime type";
    return ext;
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "which assigns the content type to a file extension";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "mime-type";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.mime-type";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.http.mime-types";
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
    return new MimeTypeCfg();
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
   */
  public void update(LogFile logFile) throws Exception
  {
    getParent().update(logFile);
  }

}


/******************************************************************************
 * $Log: MimeTypeCfg.java,v $
 * Revision 1.5  2003/06/20 09:09:38  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.4  2003/06/17 15:13:32  bebbo
 * @R more changes to enable on the fly config updates
 *
 * Revision 1.3  2003/06/17 14:41:51  bebbo
 * @R changed displayed name
 *
 * Revision 1.2  2003/06/17 12:09:56  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:41:46  bebbo
 * @N added config classes for future runtime configuration support
 *
 */