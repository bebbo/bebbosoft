/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

public class Finder extends de.bb.mejb.Finder
{
  public Finder(java.util.Properties properties) throws javax.naming.NamingException
  {
    super(properties);
  }
  /** return the home object.
   * @throws java.rmi.RemoteException
   * @return the home object */
  public Person2PermissionHome getPerson2PermissionHome() throws java.rmi.RemoteException
  {
    try {
      return (Person2PermissionHome)getHome("de.bb.web.user.mejb.Person2PermissionHome");
    } catch (Exception ex) {
      throw new java.rmi.RemoteException(ex.getMessage(), ex);
    }
  }
  /** return the home object.
   * @throws java.rmi.RemoteException
   * @return the home object */
  public PermissionHome getPermissionHome() throws java.rmi.RemoteException
  {
    try {
      return (PermissionHome)getHome("de.bb.web.user.mejb.PermissionHome");
    } catch (Exception ex) {
      throw new java.rmi.RemoteException(ex.getMessage(), ex);
    }
  }
  /** return the home object.
   * @throws java.rmi.RemoteException
   * @return the home object */
  public PersonHome getPersonHome() throws java.rmi.RemoteException
  {
    try {
      return (PersonHome)getHome("de.bb.web.user.mejb.PersonHome");
    } catch (Exception ex) {
      throw new java.rmi.RemoteException(ex.getMessage(), ex);
    }
  }
}
