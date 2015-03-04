/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/MimeTypesCfg.java,v $
 * $Revision: 1.5 $
 * $Date: 2003/06/24 10:11:49 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy.http;

import java.util.HashMap;
import java.util.Iterator;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class MimeTypesCfg extends Configurable implements Configurator
{
  private final static String PROPERTIES[][] =
    {}
  ;
  
  public MimeTypesCfg()
  {
    init("mime types", PROPERTIES);
    Config.addGlobalUnique(getPath());
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "mime types";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "this is a set of mime type definitions";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "mime-types";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.mime-types";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy";
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
    return new MimeTypesCfg();
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
  
  private static HashMap mimes = new HashMap(); 
  
  /**
   * @param extension
   * @return
   */
  public static String getMimeType(String extension)
  {
    if (extension == null)
      return null;
    String mt = (String)mimes.get(extension);
    if (mt != null)
      return mt;
    Configurable c = Config.getInstance().getChild("mime-types");
    if (c == null)
      return null;
    for (Iterator i = c.children(); i.hasNext();)
    {
      Configurable m = (Configurable)i.next();
      if (extension.equals(m.getProperty("extension")))
      {
        mt = m.getProperty("type");
        if (mt != null)
        {
          mimes.put(extension, mt);      
        }
        return mt;
      }
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
   */
  public void update(LogFile logFile) throws Exception
  {
    Configurable parent = getParent();
    if (parent != Config.getInstance())
      parent.update(logFile);
    else
      mimes.clear();
  }

}


/******************************************************************************
 * $Log: MimeTypesCfg.java,v $
 * Revision 1.5  2003/06/24 10:11:49  bebbo
 * @R global mime types are now unique
 *
 * Revision 1.4  2003/06/20 09:09:38  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.3  2003/06/17 15:13:32  bebbo
 * @R more changes to enable on the fly config updates
 *
 * Revision 1.2  2003/06/17 12:09:56  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:41:46  bebbo
 * @N added config classes for future runtime configuration support
 *
 */