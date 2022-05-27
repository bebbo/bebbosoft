/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Configurator.java,v $
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
 * This interface defines everything to instatiate and maintain a Configurable.
 * @author bebbo
 */
public interface Configurator
{
  /**
   * Returns the name of the Configurable object. 
   * @return the name of the Configurable object.
   */
  public String getName();
  
  /**
   * Returns the description of the Configurable object.
   * @return the description of the Configurable object.
   */
  public String getDescription();
  
  /**
   * Return the path where the settings are held.
   * This is a relative path applied to the current path.
   * @return the path where the settings are held.
   */
  public String getPath();
  
  /**
   * Return the own id or null.
   * @return the own id or null.
   */
  public String getId();
  
  /**
   * Return the extension point where this element wants to contribute to - or null.
   * @return the extension point where this element wants to contribute to, or null.
   */
  public String getExtensionId();
  
  /**
   * Return a komma seperated list of required element ids.
   * @return a komma seperated list of required element ids.
   */
  public String getRequired();

  /**
   * Create a Configurable instance.
   * @return a Configurable instance
   */
  public Configurable create();
  
  /**
   * Returns true, if Configurable is loaded using a "class" value. 
   * @return true, if Configurable is loaded using a "class" value.
   */
  public boolean loadClass();
}


/******************************************************************************
 * $Log: Configurator.java,v $
 * Revision 1.4  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.3  2003/06/24 19:47:34  bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 *
 * Revision 1.2  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 */