package de.bb.tools.dbgen;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;

class Db2CodeGen extends JavaCodeGen {

  /**
   * create the home class in the specified directory.
   * @param path the used path
   * @param pack the used package
   * @throws Exception on error
   */
  public void createCMP(Table t, String path, String pack, String epack) throws Exception
  {
    // pack += ".srv";      
    DbGen.log("creating DB2 DBI class " + pack + "." + t.classname + "Dbi");
    PrintWriter pw = createOs(t.classname, path, pack + "/db2", "Dbi.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + pack + ".db2;");
    pw.println();
    pw.println("public class " + t.classname + "Dbi extends de.bb.mejb.CMPDbi");
    pw.println("{");
    pw.println("  public " + t.classname + "Dbi()");
    pw.println("  {}");
    pw.println();
    pw.println("  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException");
    pw.println("  {");
    pw.println("    java.sql.ResultSet rs = null;");
    pw.println("    java.sql.Statement stmt = conn.createStatement();");
    pw.println("    try");
    pw.println("    {");
    pw.println("      String q;");

    // add foreign key checks
    for (Iterator i = DbGen.references.subMap(t.classname, t.classname + "\0").values().iterator(); i.hasNext();)
    {
      String ref = (String) i.next();
      pw.println("      q = \"SELECT id FROM " + ref + " WHERE id_" + t.tableName + "=\" + id;");
      pw.println("      rs = stmt.executeQuery(q);");
      pw.println("      if (rs.next()) throw new java.sql.SQLException(\"cant remove id=\" + id + \" from "
          + t.tableName + " - FOREIGN KEY exists in " + ref + "\");");
      pw.println("      rs.close();");
      pw.println("      rs = null;");
    }

    pw.println("      q = \"DELETE FROM " + t.tableName + " WHERE id=\" + id;");
    pw.println("      stmt.executeUpdate(q);");
    pw.println("    } finally");
    pw.println("    {");
    pw.println("      if (rs != null) try { rs.close(); } catch (Exception ex){}");
    pw.println("      stmt.close();");
    pw.println("    }");
    pw.println("  }");
    pw.println();
    pw.println("  public java.sql.ResultSet select(java.sql.Connection conn, Object id) throws java.sql.SQLException");
    pw.println("  {");
    pw.println("    java.sql.Statement stmt = conn.createStatement();");
    pw.println("    String q = \"SELECT * FROM " + t.tableName + " WHERE id=\" + id;");
    pw.println("    return stmt.executeQuery(q);");
    pw.println("  }");
    pw.println();
    pw.println("  public java.sql.PreparedStatement insert(java.sql.Connection conn) throws java.sql.SQLException");
    pw.println("  {");

    StringBuffer names = new StringBuffer();
    StringBuffer vals = new StringBuffer();
    int n = 1;
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n)
    {
      Row r = (Row) f.next();
      if (n > 1)
      {
        if (n > 2)
        {
          names.append(',');
          vals.append(',');
        }
        // DB2 ist etwas eigen beim Quoten: keywords _muessen_ gequotet werden, 
        // non-keywords _duerfen nicht_ gequotet werden - daher das umstaendliche Handling
        if (r.name.equals("name")) // TODO: Mehr DB2-Keywords beachten?
          names.append("\\\"");
        names.append(r.name);
        if (r.name.equals("name")) // TODO: Mehr DB2-Keywords beachten?
          names.append("\\\"");
        vals.append('?');
      }
    }
    pw.println("    return conn.prepareStatement(\"INSERT INTO " + t.tableName + " (" + names.toString() + ") VALUES ("
        + vals.toString() + ")\");");
    pw.println("  }");
    pw.println();
    pw.println("  public java.sql.PreparedStatement update(java.sql.Connection conn) throws java.sql.SQLException");
    pw.println("  {");
    names = new StringBuffer();
    n = 1;
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n)
    {
      Row r = (Row) f.next();
      if (n > 1)
      {
        if (n > 2)
        {
          names.append(',');
        }
        // DB2 ist etwas eigen beim Quoten: keywords _muessen_ gequotet werden, 
        // non-keywords _duerfen nicht_ gequotet werden - daher das umstaendliche Handling
        if (r.name.equals("name")) // TODO: Mehr DB2-Keywords beachten?
          names.append("\\\"");
        names.append(r.name);
        if (r.name.equals("name")) // TODO: Mehr DB2-Keywords beachten?
          names.append("\\\"");
        names.append("=?");
      }
    }
    pw.println("    return conn.prepareStatement(\"UPDATE " + t.tableName + " SET " + names.toString()
        + " WHERE id=?\");");
    pw.println("  }");
    pw.println();
    pw.println("  public String getId(java.sql.Statement stmt) throws java.sql.SQLException");
    pw.println("  {");
    pw.println("    java.sql.ResultSet rs = stmt.executeQuery(\"VALUES IDENTITY_VAL_LOCAL()\");");
    pw.println("    if (!rs.next())");
    pw.println("      throw new java.sql.SQLException(\"got no from insert;\");");
    pw.println("    String id = rs.getString(1);");
    pw.println("    rs.close();");
    pw.println("    return id;");
    pw.println("  }");
    pw.println("}");
    pw.close();
  }

}
