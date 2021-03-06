/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.board.mejb;

abstract class TopicBaseBean
  extends de.bb.mejb.CMPBean
  implements de.bb.web.board.mejb.TopicBase
{

  /** attributes */
  protected String id;
  protected String id_board;
  protected de.bb.web.board.mejb.Board t$Board;
  protected java.lang.String name = new java.lang.String();
  protected int sticky;
  protected java.lang.String author = new java.lang.String();
  protected java.sql.Timestamp modified;
  protected int viewCount;

  TopicBaseBean(TopicHomeBaseBean aHome) throws java.rmi.RemoteException
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
  public String getBoardId() throws java.rmi.RemoteException
  {
    return id_board;
  }
  public de.bb.web.board.mejb.Board getBoard() throws java.rmi.RemoteException
  {
    if ( t$Board == null)
      t$Board = new BoardHomeBean().findByPrimaryKey(id_board);
    return t$Board;
  }
  public void setBoard(de.bb.web.board.mejb.Board a$Board) throws java.rmi.RemoteException  {
    t$Board = a$Board;
    id_board = t$Board == null ? null : t$Board.getId();
  }
  public java.lang.String getName() throws java.rmi.RemoteException
  {
    return name;
  }
  public void setName(java.lang.String a$Name) throws java.rmi.RemoteException
  {
    name = a$Name;
  }
  public int getSticky() throws java.rmi.RemoteException
  {
    return sticky;
  }
  public void setSticky(int a$Sticky) throws java.rmi.RemoteException
  {
    sticky = a$Sticky;
  }
  public java.lang.String getAuthor() throws java.rmi.RemoteException
  {
    return author;
  }
  public void setAuthor(java.lang.String a$Author) throws java.rmi.RemoteException
  {
    author = a$Author;
  }
  public java.sql.Timestamp getModified() throws java.rmi.RemoteException
  {
    return modified;
  }
  public void setModified(java.sql.Timestamp a$Modified) throws java.rmi.RemoteException
  {
    modified = a$Modified;
  }
  public int getViewCount() throws java.rmi.RemoteException
  {
    return viewCount;
  }
  public void setViewCount(int a$ViewCount) throws java.rmi.RemoteException
  {
    viewCount = a$ViewCount;
  }
// from javax.ejb.EntityBean
  public void ejbStore() throws java.rmi.RemoteException
  {
    ((TopicHomeBaseBean)myHome).store(this);
  }
  public void ejbRemove() throws java.rmi.RemoteException
  {
    ((TopicHomeBaseBean)myHome).remove(this);
  }
  public void ejbLoad() throws java.rmi.RemoteException
  {
    ((TopicHomeBaseBean)myHome).load(this);
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
    id_board = rs.getString(2);
    name = rs.getString(3);
    sticky = rs.getByte(4);
    author = rs.getString(5);
    modified = rs.getTimestamp(6);
    viewCount = rs.getInt(7);
  }
  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException
  {
    ps.setString(1, id_board);
    ps.setString(2, name);
    ps.setInt(3, sticky);
    ps.setString(4, author);
    ps.setTimestamp(5, modified);
    ps.setInt(6, viewCount);
  }
  public StringBuffer toLog() throws java.rmi.RemoteException
  {
    StringBuffer sb = new StringBuffer();
    sb.append("id=\"" + id + "\">\n");
    sb.append("  <attribute name=\"id\">" + encodeXML(id) + "</attribute>\n");
    sb.append("  <attribute name=\"id_board\">" + encodeXML(id_board) + "</attribute>\n");
    sb.append("  <attribute name=\"name\">" + encodeXML(name) + "</attribute>\n");
    sb.append("  <attribute name=\"sticky\">" + encodeXML(sticky) + "</attribute>\n");
    sb.append("  <attribute name=\"author\">" + encodeXML(author) + "</attribute>\n");
    sb.append("  <attribute name=\"modified\">" + encodeXML(modified) + "</attribute>\n");
    sb.append("  <attribute name=\"viewCount\">" + encodeXML(viewCount) + "</attribute>\n");
    
    return sb;
  }
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(id);
    sb.append(';');
    sb.append(id_board);
    sb.append(';');
    sb.append(name);
    sb.append(';');
    sb.append(sticky);
    sb.append(';');
    sb.append(author);
    sb.append(';');
    sb.append(modified);
    sb.append(';');
    sb.append(viewCount);
    return sb.toString();
  }
  public boolean equals(Object o)
  {
    if (!(o instanceof TopicBaseBean)) return false;
    TopicBaseBean x = (TopicBaseBean)o;
    return (id == null && x.id == null) || id.equals(x.id);
  }
  protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException
  {
    TopicBean b = (TopicBean)sb;
    b.id = id;
    b.id_board = id_board;
    b.name = name;
    b.sticky = sticky;
    b.author = author;
    b.modified = modified;
    b.viewCount = viewCount;
  }
  public java.util.Collection getArticleList() throws java.rmi.RemoteException
  {
    return new ArticleHomeBean().findByTopic(id);
  }
}
