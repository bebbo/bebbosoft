/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

abstract class PermissionBaseBean
  extends de.bb.mejb.CMPBean
  implements de.bb.web.user.mejb.PermissionBase
{

  /** attributes */
  protected String id;
  protected java.lang.String name = new java.lang.String();

  PermissionBaseBean(PermissionHomeBaseBean aHome) throws java.rmi.RemoteException
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
  public java.lang.String getName() throws java.rmi.RemoteException
  {
    return name;
  }
  public void setName(java.lang.String a$Name) throws java.rmi.RemoteException
  {
    name = a$Name;
  }
// from javax.ejb.EntityBean
  public void ejbStore() throws java.rmi.RemoteException
  {
    ((PermissionHomeBaseBean)myHome).store(this);
  }
  public void ejbRemove() throws java.rmi.RemoteException
  {
    ((PermissionHomeBaseBean)myHome).remove(this);
  }
  public void ejbLoad() throws java.rmi.RemoteException
  {
    ((PermissionHomeBaseBean)myHome).load(this);
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
    name = rs.getString(2);
  }
  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException
  {
    ps.setString(1, name);
  }
  public StringBuffer toLog() throws java.rmi.RemoteException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("id=\"" + id + "\">\n");
    sb.append("  <attribute name=\"id\">" + encodeXML(id) + "</attribute>\n");
    sb.append("  <attribute name=\"name\">" + encodeXML(name) + "</attribute>\n");
    
    return sb;
  }
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(id);
    sb.append(';');
    sb.append(name);
    return sb.toString();
  }
  public boolean equals(Object o)
  {
    if (!(o instanceof PermissionBaseBean)) return false;
    PermissionBaseBean x = (PermissionBaseBean)o;
    return (id == null && x.id == null) || id.equals(x.id);
  }
  protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException
  {
    PermissionBean b = (PermissionBean)sb;
    b.id = id;
    b.name = name;
  }
  public java.util.Collection getPerson2PermissionList() throws java.rmi.RemoteException
  {
    return new Person2PermissionHomeBean(getEJBHome()).findByPermission(id);
  }
}
