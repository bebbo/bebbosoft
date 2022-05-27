/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
 */

package de.bb.web.user.mejb;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.EJBHome;

import de.bb.mejb.Simple;

public class PersonHomeBean extends PersonHomeBaseBean implements PersonHome
{
  public PersonHomeBean(EJBHome home) throws java.rmi.RemoteException
  {
    super(home);
  }

  public Collection findByPermission(String permission) throws RemoteException
  {
    return queryCollection(
        "SELECT DISTINCT Person.* FROM Person, Person2Permission, Permission "
            + "WHERE Person.id=Person2Permission.id_person "
            + "AND Permission.id=Person2Permission.id_permission "
            + "AND Permission.name=?", new Object[] { permission });
  }

  public Person findByNamePassword(String username, String password)
      throws RemoteException
  {
    Collection c = queryCollection(
        "SELECT * FROM Person WHERE name=? AND password=?", new Object[] {
            username, password });
    if (c.size() != 1)
      return null;
    return (Person) c.iterator().next();
  }

  /* (non-Javadoc)
   * @see jspboard.ejb.PersonHome#findByName(java.lang.String)
   */
  public Person findByName(String username) throws RemoteException
  {
    Collection c = queryCollection("SELECT * FROM Person WHERE name=?",
        new Object[] { username });
    if (c.size() != 1)
      return null;
    return (Person) c.iterator().next();
  }

  /** (non-Javadoc)
   * @see de.bb.web.user.mejb.PersonHome#findByEmail(java.lang.String)
   */
  public Person findByEmail(String email) throws RemoteException
  {
    Collection c = queryCollection("SELECT * FROM Person WHERE email=?",
        new Object[] { email });
    if (c.size() != 1)
      return null;
    return (Person) c.iterator().next();
  }

  public Simple internCreate() throws RemoteException {
    return new PersonBean(this);
  }

}