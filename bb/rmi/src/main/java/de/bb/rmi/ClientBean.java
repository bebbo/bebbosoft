/* 
 * Created on 12.11.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.rmi;

import java.util.HashMap;

import de.bb.util.SessionManager;


class ClientBean implements SessionManager.Callback
{
  long cid;
  long last;
  long no; // counter for object id's

  HashMap skels = new HashMap();
  LongHash objects = new LongHash();
  
  Principal principal;
//  Properties properties;
//  String helper;

  ClientBean(long cid, Principal principal)
  {
    this.cid = cid;    
    this.principal = principal;
  }
  /**
   * @see de.bb.util.SessionManager.Callback#dontRemove(java.lang.Object)
   */
  public boolean dontRemove(Object key)
  {
    boolean ret = false;
    ret = System.currentTimeMillis() < last + 1000 * 60;
    if (!ret)
    {
      Server.logFile.writeDate("release client:" + key + " freeing: " + objects.size() + " objects");
      clear();
    }
    return ret;
  }

  /** (non-Javadoc)
   * @see de.bb.rmi.LongHash#clear()
   */
  public void clear()
  {
    objects.clear();
    skels.clear();
  }

  /**
   * @param key
   * @param val
   */
  public void put(String key, Object val)
  {
    skels.put(key, val);
  }

  /**
   * @param key
   * @return
   */
  public Object get(String key)
  {
    return skels.get(key);
  }
  /** (non-Javadoc)
   * @see de.bb.rmi.Caller#getPrincipal()
   */
  public Principal getPrincipal()
  {
    return principal;
  }
  public synchronized long newKey()
  {
    return ++no;
  }

}