/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

public interface Person2PermissionHomeBase extends java.rmi.Remote, javax.ejb.EJBHome
{
  public Person2Permission findByPrimaryKey(Object pk) throws java.rmi.RemoteException;
  public Person2Permission create() throws java.rmi.RemoteException;
  public java.util.Collection findAll() throws java.rmi.RemoteException;
  public java.util.Collection findByPerson(Object id) throws java.rmi.RemoteException;
  public java.util.Collection findByPermission(Object id) throws java.rmi.RemoteException;
}
