/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb;

abstract class PersonBaseBean
  extends de.bb.mejb.CMPBean
  implements de.bb.web.user.mejb.PersonBase
{

  /** attributes */
  protected String id;
  protected java.lang.String name = new java.lang.String();
  protected java.lang.String email = new java.lang.String();
  protected java.lang.String password = new java.lang.String();
  protected java.sql.Timestamp lastVisit;
  protected int postCount;
  protected java.lang.String avatar = new java.lang.String();
  protected java.lang.String prename = new java.lang.String();
  protected java.lang.String surname = new java.lang.String();
  protected java.lang.String street = new java.lang.String();
  protected java.lang.String city = new java.lang.String();
  protected java.lang.String plz = new java.lang.String();
  protected java.lang.String phone1 = new java.lang.String();
  protected java.lang.String phone2 = new java.lang.String();

  PersonBaseBean(PersonHomeBaseBean aHome) throws java.rmi.RemoteException
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
  public java.lang.String getEmail() throws java.rmi.RemoteException
  {
    return email;
  }
  public void setEmail(java.lang.String a$Email) throws java.rmi.RemoteException
  {
    email = a$Email;
  }
  public java.lang.String getPassword() throws java.rmi.RemoteException
  {
    return password;
  }
  public void setPassword(java.lang.String a$Password) throws java.rmi.RemoteException
  {
    password = a$Password;
  }
  public java.sql.Timestamp getLastVisit() throws java.rmi.RemoteException
  {
    return lastVisit;
  }
  public void setLastVisit(java.sql.Timestamp a$LastVisit) throws java.rmi.RemoteException
  {
    lastVisit = a$LastVisit;
  }
  public int getPostCount() throws java.rmi.RemoteException
  {
    return postCount;
  }
  public void setPostCount(int a$PostCount) throws java.rmi.RemoteException
  {
    postCount = a$PostCount;
  }
  public java.lang.String getAvatar() throws java.rmi.RemoteException
  {
    return avatar;
  }
  public void setAvatar(java.lang.String a$Avatar) throws java.rmi.RemoteException
  {
    avatar = a$Avatar;
  }
  public java.lang.String getPrename() throws java.rmi.RemoteException
  {
    return prename;
  }
  public void setPrename(java.lang.String a$Prename) throws java.rmi.RemoteException
  {
    prename = a$Prename;
  }
  public java.lang.String getSurname() throws java.rmi.RemoteException
  {
    return surname;
  }
  public void setSurname(java.lang.String a$Surname) throws java.rmi.RemoteException
  {
    surname = a$Surname;
  }
  public java.lang.String getStreet() throws java.rmi.RemoteException
  {
    return street;
  }
  public void setStreet(java.lang.String a$Street) throws java.rmi.RemoteException
  {
    street = a$Street;
  }
  public java.lang.String getCity() throws java.rmi.RemoteException
  {
    return city;
  }
  public void setCity(java.lang.String a$City) throws java.rmi.RemoteException
  {
    city = a$City;
  }
  public java.lang.String getPlz() throws java.rmi.RemoteException
  {
    return plz;
  }
  public void setPlz(java.lang.String a$Plz) throws java.rmi.RemoteException
  {
    plz = a$Plz;
  }
  public java.lang.String getPhone1() throws java.rmi.RemoteException
  {
    return phone1;
  }
  public void setPhone1(java.lang.String a$Phone1) throws java.rmi.RemoteException
  {
    phone1 = a$Phone1;
  }
  public java.lang.String getPhone2() throws java.rmi.RemoteException
  {
    return phone2;
  }
  public void setPhone2(java.lang.String a$Phone2) throws java.rmi.RemoteException
  {
    phone2 = a$Phone2;
  }
// from javax.ejb.EntityBean
  public void ejbStore() throws java.rmi.RemoteException
  {
    ((PersonHomeBaseBean)myHome).store(this);
  }
  public void ejbRemove() throws java.rmi.RemoteException
  {
    ((PersonHomeBaseBean)myHome).remove(this);
  }
  public void ejbLoad() throws java.rmi.RemoteException
  {
    ((PersonHomeBaseBean)myHome).load(this);
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
    email = rs.getString(3);
    password = rs.getString(4);
    lastVisit = rs.getTimestamp(5);
    postCount = rs.getInt(6);
    avatar = rs.getString(7);
    prename = rs.getString(8);
    surname = rs.getString(9);
    street = rs.getString(10);
    city = rs.getString(11);
    plz = rs.getString(12);
    phone1 = rs.getString(13);
    phone2 = rs.getString(14);
  }
  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException
  {
    ps.setString(1, name);
    ps.setString(2, email);
    ps.setString(3, password);
    ps.setTimestamp(4, lastVisit);
    ps.setInt(5, postCount);
    ps.setString(6, avatar);
    ps.setString(7, prename);
    ps.setString(8, surname);
    ps.setString(9, street);
    ps.setString(10, city);
    ps.setString(11, plz);
    ps.setString(12, phone1);
    ps.setString(13, phone2);
  }
  public StringBuffer toLog() throws java.rmi.RemoteException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("id=\"" + id + "\">\n");
    sb.append("  <attribute name=\"id\">" + encodeXML(id) + "</attribute>\n");
    sb.append("  <attribute name=\"name\">" + encodeXML(name) + "</attribute>\n");
    sb.append("  <attribute name=\"email\">" + encodeXML(email) + "</attribute>\n");
    sb.append("  <attribute name=\"password\">" + encodeXML(password) + "</attribute>\n");
    sb.append("  <attribute name=\"lastVisit\">" + encodeXML(lastVisit) + "</attribute>\n");
    sb.append("  <attribute name=\"postCount\">" + encodeXML(postCount) + "</attribute>\n");
    sb.append("  <attribute name=\"avatar\">" + encodeXML(avatar) + "</attribute>\n");
    sb.append("  <attribute name=\"prename\">" + encodeXML(prename) + "</attribute>\n");
    sb.append("  <attribute name=\"surname\">" + encodeXML(surname) + "</attribute>\n");
    sb.append("  <attribute name=\"street\">" + encodeXML(street) + "</attribute>\n");
    sb.append("  <attribute name=\"city\">" + encodeXML(city) + "</attribute>\n");
    sb.append("  <attribute name=\"plz\">" + encodeXML(plz) + "</attribute>\n");
    sb.append("  <attribute name=\"phone1\">" + encodeXML(phone1) + "</attribute>\n");
    sb.append("  <attribute name=\"phone2\">" + encodeXML(phone2) + "</attribute>\n");
    
    return sb;
  }
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(id);
    sb.append(';');
    sb.append(name);
    sb.append(';');
    sb.append(email);
    sb.append(';');
    sb.append(password);
    sb.append(';');
    sb.append(lastVisit);
    sb.append(';');
    sb.append(postCount);
    sb.append(';');
    sb.append(avatar);
    sb.append(';');
    sb.append(prename);
    sb.append(';');
    sb.append(surname);
    sb.append(';');
    sb.append(street);
    sb.append(';');
    sb.append(city);
    sb.append(';');
    sb.append(plz);
    sb.append(';');
    sb.append(phone1);
    sb.append(';');
    sb.append(phone2);
    return sb.toString();
  }
  public boolean equals(Object o)
  {
    if (!(o instanceof PersonBaseBean)) return false;
    PersonBaseBean x = (PersonBaseBean)o;
    return (id == null && x.id == null) || id.equals(x.id);
  }
  protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException
  {
    PersonBean b = (PersonBean)sb;
    b.id = id;
    b.name = name;
    b.email = email;
    b.password = password;
    b.lastVisit = lastVisit;
    b.postCount = postCount;
    b.avatar = avatar;
    b.prename = prename;
    b.surname = surname;
    b.street = street;
    b.city = city;
    b.plz = plz;
    b.phone1 = phone1;
    b.phone2 = phone2;
  }
  public java.util.Collection getPerson2PermissionList() throws java.rmi.RemoteException
  {
    return new Person2PermissionHomeBean(getEJBHome()).findByPerson(id);
  }
}
