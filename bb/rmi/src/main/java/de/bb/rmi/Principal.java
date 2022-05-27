/* 
 * Created on 13.11.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.rmi;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Properties;


/**
 * @author sfranke
 */
public class Principal implements java.security.Principal, Serializable
{
  private static long count;

  private String name;
  private String pass;
  private Object id;
  
  public Principal(String name, String pass)
  {
    this.name = name;
    this.pass = pass;
    this.id = new Long(unique());
  }
  /** (non-Javadoc)
   * @see java.security.Principal#getName()
   */
  public String getName()
  {
    return name;
  }

  private static synchronized long unique() {
    return ++count;
  }
  
  /**
   * @Return the unique id for this Principal.
   * @return the unique id for this Principal.
   */
  public Object getId()
  {
    return id;
  }
}
