/*
 * This file is generated by MEJB.
 * DO NOT MODIFY THIS FILE!
*/


package de.bb.web.user.mejb.mysql;

public class PermissionDbi extends de.bb.mejb.CMPDbi
{
  public PermissionDbi()
  {}

  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException
  {
    java.sql.ResultSet rs = null;
    java.sql.Statement stmt = conn.createStatement();
    try
    {
      String q;
      q = "SELECT id FROM Person2Permission WHERE id_permission=" + id;
      rs = stmt.executeQuery(q);
      if (rs.next()) throw new java.sql.SQLException("cant remove id=" + id + " from permission - FOREIGN KEY exists in Person2Permission");
      rs.close();
      rs = null;
      q = "DELETE FROM Permission WHERE id=" + id;
      stmt.executeUpdate(q);
    } finally
    {
      if (rs != null) try { rs.close(); } catch (Exception ex){}
      stmt.close();
    }
  }

  public java.sql.ResultSet select(java.sql.Connection conn, Object id) throws java.sql.SQLException
  {
    java.sql.Statement stmt = conn.createStatement();
    String q = "SELECT * FROM Permission WHERE id=" + id;
    return stmt.executeQuery(q);
  }

  public java.sql.PreparedStatement insert(java.sql.Connection conn) throws java.sql.SQLException
  {
    return conn.prepareStatement("INSERT INTO Permission (name) VALUES (?)");
  }

  public java.sql.PreparedStatement update(java.sql.Connection conn) throws java.sql.SQLException
  {
    return conn.prepareStatement("UPDATE Permission SET name=? WHERE id=?");
  }

  public String getId(java.sql.Statement stmt) throws java.sql.SQLException
  {
    java.sql.ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
    if (!rs.next())
      throw new java.sql.SQLException("got no from insert;");
    String id = rs.getString(1);
    rs.close();
    return id;
  }
}
