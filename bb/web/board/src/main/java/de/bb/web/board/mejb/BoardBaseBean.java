/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/

package de.bb.web.board.mejb;

abstract class BoardBaseBean extends de.bb.mejb.CMPBean implements de.bb.web.board.mejb.BoardBase {

    /** attributes */
    protected String id;
    protected java.lang.String readPermission = new java.lang.String();
    protected java.lang.String writePermission = new java.lang.String();
    protected java.lang.String adminPermission = new java.lang.String();
    protected java.lang.String name = new java.lang.String();
    protected java.lang.String description = new java.lang.String();
    protected int groupNr;
    protected int orderNr;

    BoardBaseBean(BoardHomeBaseBean aHome) throws java.rmi.RemoteException {
        super(aHome);
    }

    public String getId() throws java.rmi.RemoteException {
        return id;
    }

    protected void setId(String a$Id) throws java.rmi.RemoteException {
        id = a$Id;
    }

    public java.lang.String getReadPermission() throws java.rmi.RemoteException {
        return readPermission;
    }

    public void setReadPermission(java.lang.String a$ReadPermission) throws java.rmi.RemoteException {
        readPermission = a$ReadPermission;
    }

    public java.lang.String getWritePermission() throws java.rmi.RemoteException {
        return writePermission;
    }

    public void setWritePermission(java.lang.String a$WritePermission) throws java.rmi.RemoteException {
        writePermission = a$WritePermission;
    }

    public java.lang.String getAdminPermission() throws java.rmi.RemoteException {
        return adminPermission;
    }

    public void setAdminPermission(java.lang.String a$AdminPermission) throws java.rmi.RemoteException {
        adminPermission = a$AdminPermission;
    }

    public java.lang.String getName() throws java.rmi.RemoteException {
        return name;
    }

    public void setName(java.lang.String a$Name) throws java.rmi.RemoteException {
        name = a$Name;
    }

    public java.lang.String getDescription() throws java.rmi.RemoteException {
        return description;
    }

    public void setDescription(java.lang.String a$Description) throws java.rmi.RemoteException {
        description = a$Description;
    }

    public int getGroupNr() throws java.rmi.RemoteException {
        return groupNr;
    }

    public void setGroupNr(int a$GroupNr) throws java.rmi.RemoteException {
        groupNr = a$GroupNr;
    }

    public int getOrderNr() throws java.rmi.RemoteException {
        return orderNr;
    }

    public void setOrderNr(int a$OrderNr) throws java.rmi.RemoteException {
        orderNr = a$OrderNr;
    }

    // from javax.ejb.EntityBean
    public void ejbStore() throws java.rmi.RemoteException {
        ((BoardHomeBaseBean) myHome).store(this);
    }

    public void ejbRemove() throws java.rmi.RemoteException {
        ((BoardHomeBaseBean) myHome).remove(this);
    }

    public void ejbLoad() throws java.rmi.RemoteException {
        ((BoardHomeBaseBean) myHome).load(this);
    }

    public void ejbPassivate() throws java.rmi.RemoteException {
        // do nothing
    }

    public void ejbActivate() throws java.rmi.RemoteException {
        // do nothing
    }

    public void setEntityContext(javax.ejb.EntityContext ec) throws java.rmi.RemoteException {
        // do nothing
    }

    public void unsetEntityContext() throws java.rmi.RemoteException {
        // do nothing
    }

    // from javax.ejb.EJBObject
    public javax.ejb.EJBHome getEJBHome() throws java.rmi.RemoteException {
        return myHome;
    }

    public javax.ejb.Handle getHandle() throws java.rmi.RemoteException {
        return null;
    }

    public Object getPrimaryKey() throws java.rmi.RemoteException {
        return id;
    }

    public boolean isIdentical(javax.ejb.EJBObject eo) throws java.rmi.RemoteException {
        return false;
    }

    public void remove() throws java.rmi.RemoteException {
        ejbRemove();
    }

    public void store() throws java.rmi.RemoteException {
        ejbStore();
    }

    protected void readValues(java.sql.ResultSet rs) throws java.sql.SQLException {
        id = rs.getString(1);
        readPermission = rs.getString(2);
        writePermission = rs.getString(3);
        adminPermission = rs.getString(4);
        name = rs.getString(5);
        description = rs.getString(6);
        groupNr = rs.getInt(7);
        orderNr = rs.getInt(8);
    }

    protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException {
        ps.setString(1, readPermission);
        ps.setString(2, writePermission);
        ps.setString(3, adminPermission);
        ps.setString(4, name);
        ps.setString(5, description);
        ps.setInt(6, groupNr);
        ps.setInt(7, orderNr);
    }

    public StringBuffer toLog() throws java.rmi.RemoteException {
        StringBuffer sb = new StringBuffer();
        sb.append("id=\"" + id + "\">\n");
        sb.append("  <attribute name=\"id\">" + encodeXML(id) + "</attribute>\n");
        sb.append("  <attribute name=\"readPermission\">" + encodeXML(readPermission) + "</attribute>\n");
        sb.append("  <attribute name=\"writePermission\">" + encodeXML(writePermission) + "</attribute>\n");
        sb.append("  <attribute name=\"adminPermission\">" + encodeXML(adminPermission) + "</attribute>\n");
        sb.append("  <attribute name=\"name\">" + encodeXML(name) + "</attribute>\n");
        sb.append("  <attribute name=\"description\">" + encodeXML(description) + "</attribute>\n");
        sb.append("  <attribute name=\"groupNr\">" + encodeXML(groupNr) + "</attribute>\n");
        sb.append("  <attribute name=\"orderNr\">" + encodeXML(orderNr) + "</attribute>\n");

        return sb;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append(';');
        sb.append(readPermission);
        sb.append(';');
        sb.append(writePermission);
        sb.append(';');
        sb.append(adminPermission);
        sb.append(';');
        sb.append(name);
        sb.append(';');
        sb.append(description);
        sb.append(';');
        sb.append(groupNr);
        sb.append(';');
        sb.append(orderNr);
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (!(o instanceof BoardBaseBean))
            return false;
        BoardBaseBean x = (BoardBaseBean) o;
        return (id == null && x.id == null) || id.equals(x.id);
    }

    protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException {
        BoardBean b = (BoardBean) sb;
        b.id = id;
        b.readPermission = readPermission;
        b.writePermission = writePermission;
        b.adminPermission = adminPermission;
        b.name = name;
        b.description = description;
        b.groupNr = groupNr;
        b.orderNr = orderNr;
    }

    public java.util.Collection getTopicList() throws java.rmi.RemoteException {
        return new TopicHomeBean().findByBoard(id);
    }
}
