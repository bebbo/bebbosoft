/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/UserGroupCfg.java,v $
 * $Revision: 1.5 $
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
 * The configuration class for a user groups, which contains users.
 * @author bebbo
 */
public class UserGroupCfg implements Configurator 
{

  /**
   * return the name.
   * @return the name.
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    return "user group";
  }

  /**
   * return the description.
   * @return the description.
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "to maintain users and their passwords";
  }

  /**
   * return the path.
   * @return the path.
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "group";
  }


  /**
   * return null, since there are no dependencies.
   * @return null, since there are no dependencies.
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
   * return the own id.
   * @return the own id.
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.group";
  }
  /**
   * return null, since dynamic loading is used.
   * @return null, since dynamic loading is used.
   * @see de.bb.bejy.Configurator#create()
   */
  public Configurable create()
  {
    return null;
  }
  /**
   * return true, since dynamic loading is used.
   * @return true, since dynamic loading is used.
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return true;
  }
}


/******************************************************************************
 * $Log: UserGroupCfg.java,v $
 * Revision 1.5  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.4  2003/06/20 09:09:32  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.3  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.2  2003/06/17 10:18:10  bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 *
 * Revision 1.1  2003/05/13 15:42:07  bebbo
 * @N added config classes for future runtime configuration support
 *
 */