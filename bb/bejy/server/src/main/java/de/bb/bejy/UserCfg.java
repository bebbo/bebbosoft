/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/UserCfg.java,v $
 * $Revision: 1.7 $
 * $Date: 2014/09/22 09:23:18 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.bb.util.LogFile;
/**
 * The configuration vlass for user objects, which are used in groups. 
 * @author bebbo
 */
public class UserCfg extends Configurable implements Configurator
{
  private final static String PROPERTIES [][] =
  {
    { "name", "name of the user"},
    { "password", "the password"},
    { "roles", "the user's roles"}
  };
  
  /**
   * Create a new UserCfg - which is both, Configurator and Configurable.
   */
  public UserCfg()
  {
    init("user", PROPERTIES);
  }
    
  /**
   * return a new UserCfg.
   * @return a new UserCfg.
   * @see de.bb.bejy.Configurator#create()
   */
  public Configurable create()
  {
    return new UserCfg();
  }

  /**
   * return the description.
   * @return the description.
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "with some password";
  }

  /**
   * return the extension id.
   * @return the extension id.
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.inigroup";
  }

  /**
   * return the own id.
   * @return the own id.
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.group.user";
  }

  /**
   * return the name.
   * @return the name.
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    String n = getProperty("name");
    if (n != null)
      return n;
    return "user";
  }

  /**
   * return the path.
   * @return the path.
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "user";
  }

  /**
   * return null, since no other modules are required.
   * @return null, since no other modules are required. 
   * @see de.bb.bejy.Configurator#getRequired()
   */
  public String getRequired()
  {
    return null;
  }
  /**
   * return false, since dynamic loading is not used.
   * @return false, since dynamic loading is not used.
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
  /**
   * hook into update mechanism, and pass it to the group.
   * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
   */
  public void update(LogFile logFile) throws Exception
  {
    getParent().update(logFile);
  }

  public Collection<String> getRoles() {
      final String roles = getProperty("roles", "DEFAULT");
      return new ArrayList<String>(Arrays.asList(roles.split(",")));
  }
}


/******************************************************************************
 * $Log: UserCfg.java,v $
 * Revision 1.7  2014/09/22 09:23:18  bebbo
 * @N added support for user roles
 *
 * Revision 1.6  2003/10/01 12:01:51  bebbo
 * @C fixed all javadoc errors.
 *
 * Revision 1.5  2003/06/20 09:09:32  bebbo
 * @N onine configuration seems to be complete for bejy and http
 *
 * Revision 1.4  2003/06/17 15:13:36  bebbo
 * @R more changes to enable on the fly config updates
 *
 * Revision 1.3  2003/06/17 14:42:28  bebbo
 * @R insert / remove is working now - TBD: enable the changes
 *
 * Revision 1.2  2003/06/17 12:10:03  bebbo
 * @R added a generalization for Configurables loaded by class
 *
 * Revision 1.1  2003/06/17 10:18:10  bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 *
 */