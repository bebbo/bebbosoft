/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

abstract class Person2PermissionBaseBean
  extends de.bb.mejb.CMPBean
  implements de.bb.web.user.mejb.Person2PermissionBase
{

  /** attributes */
  protected String id;
  protected String id_person;
  protected de.bb.web.user.mejb.Person t$Person;
  protected String id_permission;
  protected de.bb.web.user.mejb.Permission t$Permission;

  Person2PermissionBaseBean(Person2PermissionHomeBaseBean aHome) throws java.rmi.RemoteException
  {
    super(aHome);
  }

  public String getId() throws java.rmi.RemoteException
  {
    return id;
  }
  protected void setId(String a$Id) throws java.rmi.RemoteException
  {
    id = a$Id;
  }
  public String getPersonId() throws java.rmi.RemoteException
  {
    return id_person;
  }
  public de.bb.web.user.mejb.Person getPerson() throws java.rmi.RemoteException
  {
    if ( t$Person == null)
      t$Person = new PersonHomeBean(getEJBHome()).findByPrimaryKey(id_person);
    return t$Person;
  }
  public void setPerson(de.bb.web.user.mejb.Person a$Person) throws java.rmi.RemoteException  {
    t$Person = a$Person;
    id_person = t$Person == null ? null : t$Person.getId();
  }
  public String getPermissionId() throws java.rmi.RemoteException
  {
    return id_permission;
  }
  public de.bb.web.user.mejb.Permission getPermission() throws java.rmi.RemoteException
  {
    if ( t$Permission == null)
      t$Permission = new PermissionHomeBean(getEJBHome()).findByPrimaryKey(id_permission);
    return t$Permission;
  }
  public void setPermission(de.bb.web.user.mejb.Permission a$Permission) throws java.rmi.RemoteException  {
    t$Permission = a$Permission;
    id_permission = t$Permission == null ? null : t$Permission.getId();
  }
// from javax.ejb.EntityBean
  public void ejbStore() throws java.rmi.RemoteException
  {
    ((Person2PermissionHomeBaseBean)myHome).store(this);
  }
  public void ejbRemove() throws java.rmi.RemoteException
  {
    ((Person2PermissionHomeBaseBean)myHome).remove(this);
  }
  public void ejbLoad() throws java.rmi.RemoteException
  {
    ((Person2PermissionHomeBaseBean)myHome).load(this);
  }
  public void ejbPassivate() throws java.rmi.RemoteException
  {
    // do nothing
  }
  public void ejbActivate() throws java.rmi.RemoteException
  {
    // do nothing
  }
  public void setEntityContext(javax.ejb.EntityContext ec) throws java.rmi.RemoteException
  {
    // do nothing
  }
  public void unsetEntityContext() throws java.rmi.RemoteException
  {
    // do nothing
  }
// from javax.ejb.EJBObject
  public javax.ejb.EJBHome getEJBHome() throws java.rmi.RemoteException
  {
    return myHome;
  }
  public javax.ejb.Handle getHandle() throws java.rmi.RemoteException
  {
    return null;
  }
  public Object getPrimaryKey() throws java.rmi.RemoteException
  {
    return id;
  }
  public boolean isIdentical(javax.ejb.EJBObject eo) throws java.rmi.RemoteException
  {
    return false;
  }
  public void remove() throws java.rmi.RemoteException
  {
    ejbRemove();
  }
  public void store() throws java.rmi.RemoteException
  {
    ejbStore();
  }
  protected void readValues(java.sql.ResultSet rs) throws java.sql.SQLException
  {
    id = rs.getString(1);
    id_person = rs.getString(2);
    id_permission = rs.getString(3);
  }
  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException
  {
    ps.setString(1, id_person);
    ps.setString(2, id_permission);
  }
  public StringBuffer toLog() throws java.rmi.RemoteException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("id=\"" + id + "\">\n");
    sb.append("  <attribute name=\"id\">" + encodeXML(id) + "</attribute>\n");
    sb.append("  <attribute name=\"id_person\">" + encodeXML(id_person) + "</attribute>\n");
    sb.append("  <attribute name=\"id_permission\">" + encodeXML(id_permission) + "</attribute>\n");
    
    return sb;
  }
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(id);
    sb.append(';');
    sb.append(id_person);
    sb.append(';');
    sb.append(id_permission);
    return sb.toString();
  }
  public boolean equals(Object o)
  {
    if (!(o instanceof Person2PermissionBaseBean)) return false;
    Person2PermissionBaseBean x = (Person2PermissionBaseBean)o;
    return (id == null && x.id == null) || id.equals(x.id);
  }
  protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException
  {
    Person2PermissionBean b = (Person2PermissionBean)sb;
    b.id = id;
    b.id_person = id_person;
    b.id_permission = id_permission;
  }
}
