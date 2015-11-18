/******************************************************************************
 * $Source: /export/CVS/java/jspboard/src/de/bb/web/user/PersonData.java,v $
 * $Revision: 1.2 $
 * $Date: 2004/11/28 16:58:26 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 Created on 05.03.2004

 *****************************************************************************/
package de.bb.web.user;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import de.bb.web.user.mejb.Finder;
import de.bb.web.user.mejb.Permission;
import de.bb.web.user.mejb.PermissionHome;
import de.bb.web.user.mejb.Person;

/**
 * @author bebbo
 */
public class PersonData
{
  private HashSet permissions = new HashSet();

  private Person person;

  private long lastVisit = Long.MAX_VALUE;
  
  private HashSet readTopics = new HashSet();
  /**
   * 
   */
  public PersonData()
  {
  }

  /**
   * @param finder
   * @param person
   * @throws Exception
   */
  PersonData(Finder finder, Person person) throws Exception
  {
    this.person = person;

    loadPermissions(finder);
  }

  /**
   * @throws RemoteException
   * 
   */
  void loadPermissions(Finder finder) throws RemoteException
  {
    permissions.add("");

    // load the permissions
    PermissionHome pmh = finder.getPermissionHome();
    Collection c = pmh.findByPerson(person);
    if (c != null)
    for (Iterator i = c.iterator(); i.hasNext();) {
      Permission perm = (Permission) i.next();
      permissions.add(perm.getName());
    }
  }

  /**
   * @return
   * @throws Exception
   */
  public String getName() throws Exception
  {
    return person == null ? "anonymous" : person.getName();
  }

  /**
   * Eine Berechtigung ist gegeben, wenn
   * - die Permission == null ist
   * - ein der Berechtigungen in der Menge der eigenen Berechtigungen gefunden wird.
   * @param permission mit ; getrennte Berechtigungen
   * @return true, falls die Berechtigung existiert.
   */
  public boolean hasPermission(String permission)
  {
    if (permission == null)
      permission = "";
    if (permissions.contains(permission))
      return true;
    int pos = 0;
    for(;;)
    {
      int coma = permission.indexOf(';', pos);
      String p = coma < 0 ? permission.substring(pos) : permission.substring(pos, coma);
      if (permissions.contains(p))
        return true;
      if (coma < 0)
        break;
      pos = coma + 1;
    }
    return false;
  }

  /** (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return person == null ? "anonymous" : person.toString();
  }

  /**
   * @return
   */
  Person getPerson()
  {
    return person;
  }

  /**
   * @return
   * @throws RemoteException
   */
  public String getAvatar() throws RemoteException
  {
    return person == null ? "" : person.getAvatar();
  }
  
  /**
   * @return
   * @throws RemoteException
   */
  public int getPostCount() throws RemoteException
  {
    return person == null ? 0 : person.getPostCount();    
  }
  
  synchronized void incrementPostCount() throws RemoteException
  {
    if (person == null) return;
    person.setPostCount(person.getPostCount() + 1);
    person.store();
  }
  
  /**
   * keep the last login date and touch it in database.
   * @throws RemoteException
   * @author sfranke
   */
  public void login() throws RemoteException
  {
    if (person == null)
      return;
    Timestamp ts = person.getLastVisit();
    if (ts != null)
      lastVisit = ts.getTime();
    person.setLastVisit(new Timestamp(System.currentTimeMillis()));
    person.store();
    readTopics = new HashSet();
  }
  /**
   * The time value of last visit.
   * @return time value of last visit.
   */
  public long getLastVisit()
  {
    return lastVisit;
  }

  /**
   * @throws RemoteException
   * 
   */
  public void addPost() throws RemoteException
  {
    if (person == null) return;
    
    person.setPostCount(person.getPostCount() + 1);
    person.store();
  }

  /**
   * @return
   * @throws RemoteException
   */
  public String getId() throws RemoteException
  {
    if (person == null) return "-1";
    return person.getId();
  }

  /**
   * @param topicId
   * @return
   */
  public boolean isRead(String topicId)
  {
    return readTopics.contains(topicId);
  }
  
  /**
   * @param topicId
   */
  public void markTopic(String topicId)
  {
    if (person != null)
      readTopics.add(topicId);
  }
}

/******************************************************************************
 * Log: $Log: PersonData.java,v $
 * Log: Revision 1.2  2004/11/28 16:58:26  bebbo
 * Log: @R adapted changes of bb_mejb and bb_rmi
 * Log:
 * Log: Revision 1.1  2004/11/26 09:58:04  bebbo
 * Log: @N new
 * Log:
 ******************************************************************************/