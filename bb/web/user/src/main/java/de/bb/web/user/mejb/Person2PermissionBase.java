/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

public interface Person2PermissionBase extends de.bb.mejb.Simple
{
  public String getId() throws java.rmi.RemoteException;
  public String getPersonId() throws java.rmi.RemoteException;
  public Person getPerson() throws java.rmi.RemoteException;
  public void setPerson(Person a$Person) throws java.rmi.RemoteException;
  public String getPermissionId() throws java.rmi.RemoteException;
  public Permission getPermission() throws java.rmi.RemoteException;
  public void setPermission(Permission a$Permission) throws java.rmi.RemoteException;
  public StringBuffer toLog() throws java.rmi.RemoteException;
  public void store() throws java.rmi.RemoteException;
}