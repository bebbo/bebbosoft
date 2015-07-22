package de.bb.tools.dbgen;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;

class MySQLCodeGen extends JavaCodeGen {

  /**
   * create the home class in the specified directory.
   * @param path the used path
   * @param pack the used package
   * @throws Exception on error
   */
  public void createCMP(Table t, String path, String pack, String epack) throws Exception
  {
    // pack += ".srv";
    DbGen.log("creating MySQL DBI class " + epack + ".mejb." + t.classname + "Dbi");
    PrintWriter pw = createOs(t.classname, path, epack + "/mejb/mysql", "Dbi.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ".mejb.mysql;");
    pw.println();
    pw.println("public class " + t.classname + "Dbi extends de.bb.mejb.CMPDbi");
    pw.println("{");
    pw.println("  public " + t.classname + "Dbi()");
    pw.println("  {}");
    pw.println();
    pw.println("  public void remove(java.sql.Connection conn, Object id) throws java.sql.SQLException");
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
      pw.println("      q = \"SELECT id FROM " + DbGen.quote + ref + DbGen.quote + " WHERE id_" + t.tableName
          + "=\" + id;");
      pw.println("      rs = stmt.executeQuery(q);");
      pw.println("      if (rs.next()) throw new java.sql.SQLException(\"cant remove id=\" + id + \" from "
          + t.tableName + " - FOREIGN KEY exists in " + ref + "\");");
      pw.println("      rs.close();");
      pw.println("      rs = null;");
    }

    pw.println("      q = \"DELETE FROM " + DbGen.quote + t.tableName + DbGen.quote + " WHERE id=\" + id;");
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
    pw.println("    String q = \"SELECT * FROM " + DbGen.quote + t.tableName + DbGen.quote + " WHERE id=\" + id;");
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
        names.append(DbGen.quote);
        names.append(r.name);
        names.append(DbGen.quote);
        vals.append('?');
      }
    }
    pw.println("    return conn.prepareStatement(\"INSERT INTO " + DbGen.quote + t.tableName + DbGen.quote + " ("
        + names.toString() + ") VALUES (" + vals.toString() + ")\");");
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
        names.append(DbGen.quote);
        names.append(r.name);
        names.append(DbGen.quote);
        names.append("=?");
      }
    }
    pw.println("    return conn.prepareStatement(\"UPDATE " + DbGen.quote + t.tableName + DbGen.quote + " SET "
        + names.toString() + " WHERE id=?\");");
    pw.println("  }");
    pw.println();
    pw.println("  public String getId(java.sql.Statement stmt) throws java.sql.SQLException");
    pw.println("  {");
    pw.println("    java.sql.ResultSet rs = stmt.executeQuery(\"SELECT LAST_INSERT_ID()\");");
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
