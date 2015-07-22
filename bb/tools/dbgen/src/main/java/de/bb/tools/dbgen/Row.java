package de.bb.tools.dbgen;

import java.io.PrintWriter;

import de.bb.util.ByteRef;

/**
 * Helper class to star a rows data
 */
class Row {
  /** our table */
  Table table;

  /** the rows attributes */
  ByteRef name, type;

  /** the used rowname */
  String rowName;

  /** the resulting Java type */
  String javaType;

  /** used in get/set functions */
  String getType;

  /** if a referenced object exists, this is the type of it. */
  String refType;

  /** if a referenced ob object exists, this is the used name. */
  String refName;

  int size1;

  int size2;

  String iName;

  /**
   * Create a new row object.
   * @param tb
   * @param n the row name
   * @param t the row type
   * @param s1 the rows size or null
   * @param s2 the rows 2nd size or null
   */
  Row(Table tb, ByteRef n, ByteRef t, ByteRef sz1, ByteRef sz2)
  {
    table = tb;
    name = n.toLowerCase();
    type = t.toUpperCase();

    if (sz1 != null) {
      size1 = sz1.toInteger();
      if (sz2 != null)
        size2 = sz2.toInteger();
    }
    
    rowName = name.toString();
    refName = name.substring(0, 1).toString().toUpperCase() + name.substring(1).toString();
    if ((type.equals("INT") || type.equals("NUMBER")) && size1 == 1 && size2 == 0)
    {
      javaType = DbGen.getJavaType(new ByteRef("BOOL"));
    } else {
      javaType = DbGen.getJavaType(type);
    }
    
    if (name.equals("id"))
    {
      javaType = DbGen.idType;
    } else if (name.startsWith("id_") || name.endsWith("_id"))
    {
      javaType = DbGen.idType;
      if (name.startsWith("id_"))
      {
        rowName = rowName.substring(3, 4).toUpperCase() + rowName.substring(4);
      } else
      {
        rowName = rowName.substring(0, 1).toUpperCase() + rowName.substring(1, rowName.length() - 3);
      }
      refType = rowName;
      refName = rowName;

      DbGen.references.put(refName, table.classname);
      
      int underscore = rowName.indexOf('_');
      if (name.startsWith("id_") && underscore > 0)
      {
        DbGen.references.put(refName, table.classname);
        refType = rowName.substring(0, underscore);
        refName = rowName.substring(underscore + 1) + refType;
        refName = refName.substring(0, 1).toUpperCase() + refName.substring(1);
      }

      //        System.out.println(table.classname + " references <" + refType + ">");
    }
    
    refName = DbGen.nice(refName);
    refType = DbGen.nice(refType);

    getType = javaType;
    int idx = getType.lastIndexOf('.');
    if (idx > 0)
      getType = getType.substring(idx + 1);
    getType = getType.substring(0, 1).toUpperCase() + getType.substring(1);
    
    iName = DbGen.convert(refType);

    
    table.refNames.add(refName);
  }


  void writeVarDecl(PrintWriter pw, String ipack) throws Exception
  {
    try
    {
      // dont init id, refs and simple types
      if (name.equals("id") || name.startsWith("id_"))
        throw new Exception();

      // check default CT
      Class clazz = Class.forName(javaType);
      clazz.newInstance();
      pw.println("  private " + javaType + " " + name + " = new " + javaType + "();");

    } catch (Exception ex)
    {
      // or define only
      pw.println("  private " + javaType + " " + name + ";");
    }
    if (refType != null)
      pw.println("  private " + ipack + "." + refType + " t$" + refName + ";");
  }

  void writeFunctionDecl(PrintWriter pw) throws Exception
  {
    if (refType == null)
    {
      if (javaType.equals("boolean"))
      {
        DbGen.log("  is" + refName + "()");
        pw.println("  public " + javaType + " is" + refName + "() throws java.rmi.RemoteException;");
      }
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + javaType + " get" + refName + "() throws java.rmi.RemoteException;");

      if (!"id".equalsIgnoreCase(rowName))
      {
        DbGen.log("  set" + refName + "(" + javaType + ")");
        pw.println("  public void set" + refName + "(" + javaType + " " + rowName.toLowerCase()
            + ") throws java.rmi.RemoteException;");
      }
    } else
    {
      DbGen.log("  get" + refName + "Id()");
      pw.println("  public " + DbGen.idType + " get" + refName + "Id() throws java.rmi.RemoteException;");
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + refType + " get" + refName + "() throws java.rmi.RemoteException;");
      DbGen.log("  set" + refName + "(" + refType + ")");
      pw
          .println("  public void set" + refName + "(" + refType + " " + rowName.toLowerCase()
              + ") throws java.rmi.RemoteException;");
    }
  }

  public void writeFunctionCmp(PrintWriter pw, String ipack)
  {
    if (refType == null)
    {
      if (javaType.equals("boolean"))
      {
        DbGen.log("  is" + refName + "()");
        pw.println("  public " + javaType + " is" + refName + "() throws java.rmi.RemoteException");
        pw.println("  {");
        pw.println("    return data.is" + refName + "();");
        pw.println("  }");
      }
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + javaType + " get" + refName + "() throws java.rmi.RemoteException");

      pw.println("  {");
      pw.println("    return data.get" + refName + "();");
      pw.println("  }");
      if ("id".equalsIgnoreCase(rowName))
        pw.print("  protected");
      else
        pw.print("  public");
      DbGen.log("  set" + refName + "(" + javaType + ")");
      pw.println(" void set" + refName + "(" + javaType + " " + rowName.toLowerCase() + ") throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    data.set" + refName + "(" + rowName.toLowerCase() + ");");
      pw.println("  }");
    } else
    {
      if (DbGen.tables.get(refType.toLowerCase()) == null && DbGen.tables.get(name.substring(3).toLowerCase()) == null)
      {
        DbGen.error("table for " + name + " not found");
      }
      DbGen.log("  get" + refName + "Id()");
      pw.println("  public " + DbGen.idType + " get" + refName + "Id() throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    return data.get" + refName + "Id();");
      pw.println("  }");
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + ipack + "." + refType + " get" + refName + "() throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    if ( t$" + refName + " == null)");
      pw.println("      t$" + refName + " = new " + refType + "HomeBeanCMP().findByPrimaryKey(data.get" + refName
          + "());");
      pw.println("    return t$" + refName + ";");
      pw.println("  }");
      DbGen.log("  set" + refName + "(" + ipack + "." + refType + ")");
      pw.print("  public void set" + refName + "(" + ipack + "." + refType + " " + rowName.toLowerCase()
          + ") throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    t$" + refName + " = " + rowName.toLowerCase() + ";");
      pw.println("    data.set" + refName + "(t$" + refName + " == null ? null : t$" + refName + ".getId());");
      pw.println("  }");
    }
  }

  public void writeFunctionEjb(PrintWriter pw, String ipack)
  {
    DbGen.log("  get" + refName + "()");

    String addid = "";
    if (refType != null)
      addid = "Id";
    
    pw.println(" /**");
    pw.println("  * @ejb.persistent-field");
    pw.println("  * @ejb.persistence column-name=\"" + refName + "\" sql-type=\"" + type + "\"");
    if ("id".equalsIgnoreCase(rowName))
      pw.println("  * @ejb.pk-field");
    pw.println("  * @ejb.interface-method view-type = \"local\"");
    pw.println("  * @ejb.transaction type=\"Supports\"");      
    pw.println("  */");

    pw.println("  public abstract " + javaType + " get" + refName + addid + "();");

    if (!"id".equalsIgnoreCase(rowName))
    {
      DbGen.log("  set" + refName + "Id(" + javaType + ")");
      pw.println(" /**");
      pw.println("  * @ejb.transaction  type=\"Required\"");      
      pw.println("  * @ejb.interface-method view-type = \"local\"");
      pw.println("  */");
      pw.println("  public abstract void set" + refName + addid + "(" + javaType + " " + rowName.toLowerCase() + addid + ");");
    }

    if (refType != null)
    {
      if (DbGen.tables.get(refType.toLowerCase()) == null && DbGen.tables.get(name.substring(3).toLowerCase()) == null)
      {
        DbGen.error("table for " + name + " not found");
      }
      DbGen.log("  get" + refName + "()");
      pw.println(" /**");
      pw.println("  * @ejb.interface-method view-type = \"local\"");
      pw.println("  */");
      pw.println("  public " + ipack + "." + iName + " get" + refName + "() throws FinderException, NamingException");
      pw.println("  {");
      pw.println("    return " + ipack + "." + refName + "Util.getLocalHome().findByPrimaryKey(get" + refName + "Id());");      
      pw.println("  }");

      DbGen.log("  set" + refName + "(" + javaType + ")");
      pw.println(" /**");
      pw.println("  * @ejb.interface-method view-type = \"local\"");
      pw.println("  */");
      pw.println("  public void set" + refName + "(" + ipack + "." + iName + " " + rowName.toLowerCase() + ")");
      pw.println("  {");
      pw.println("    set" + refName + "Id((" + javaType + ")" + rowName.toLowerCase() + ".getPrimaryKey());");      
      pw.println("  }");
    }
  }

  public void writeListFunctionEjb(PrintWriter pw, String pack)
  {
    String addid = "Id";
    String unique = (String)table.uniques.get(rowName.toLowerCase() + "_id");
    if (unique == null) {
      unique = (String)table.uniques.get(rowName.toLowerCase());
      addid = "";
    }
    if (unique != null) {
      pw.println("* @ejb.finder");
      pw.println("*    signature=\"" + table.classname +" find" + table.classname + "By" + refName + addid + "(" + javaType+ " " + rowName.toLowerCase() + ")\"");
      pw.println("*    query=\"SELECT object(t) FROM " + table.classname + " t WHERE t." + unique + "=?1\"");
      return;
    }

    addid = "Id";
    unique = (String)table.uniques.get(rowName.toLowerCase() + "_id, deleted");
    if (unique == null) {
      unique = (String)table.uniques.get(rowName.toLowerCase() + ", deleted");
      addid = "";
    }
    if (unique != null) {
      pw.println("* @ejb.finder");
      pw.println("*    signature=\"" + table.classname +" find" + table.classname + "By" + refName + addid + "(" + javaType+ " " + rowName.toLowerCase() + ")\"");
      pw.println("*    query=\"SELECT object(t) FROM " + table.classname + " t WHERE t." + rowName + "=?1\" AND t.Deleted IS NULL");
    }
    
    if (refType == null) {
      return;      
    }
    Table t = (Table)DbGen.tables.get(refType.toLowerCase());
    if (t == null)
      t = (Table)DbGen.tables.get(name.substring(3).toLowerCase());
    if ( t== null)
    {
      DbGen.error("table for " + name + " not found");
      return;
    }
    pw.println("* @ejb.finder");
    pw.println("*    signature=\"java.util.Collection find" + table.classname +"ListBy" + refName + "Id(" + DbGen.idType + " id)\"");
    pw.println("*    query=\"SELECT object(t) FROM " + table.classname + " t WHERE t." + refName + "=?1\"");
/*    
    pw.println("* @ejb.finder");
    pw.println("*    signature=\"java.util.Iterator find" + table.classname +"sBy" + refName + "(" + MEJB2.idType + " key)\"");
    pw.println("*    query=\"SELECT object(p) FROM " + table.classname + " t WHERE t." + refName + "_id=?1\"");
*/            
  }
  
  
  void writeListFunctionDecl(PrintWriter pw) throws Exception
  {
    if (refType != null)
    {
      DbGen.log("  findBy" + refName + "(Object)");
      pw.println("  public java.util.Collection findBy" + refName + "(Object id) throws java.rmi.RemoteException;");
    }
  }

  void writeFunction(PrintWriter pw, String ipack) throws Exception
  {
    if (refType == null)
    {
      if (javaType.equals("boolean"))
      {
        DbGen.log("  is" + refName + "()");
        pw.println("  public " + javaType + " is" + refName + "() throws java.rmi.RemoteException");
        pw.println("  {");
        pw.println("    return " + name + ";");
        pw.println("  }");
      }
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + javaType + " get" + refName + "() throws java.rmi.RemoteException");

      pw.println("  {");
      pw.println("    return " + name + ";");
      pw.println("  }");
      if ("id".equalsIgnoreCase(rowName))
        pw.print("  protected");
      else
        pw.print("  public");
      DbGen.log("  set" + refName + "(" + javaType + ")");
      pw.println(" void set" + refName + "(" + javaType + " " + rowName.toLowerCase() + ") throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    " + name + " = " + rowName.toLowerCase() + ";");
      pw.println("  }");
    } else
    {
      if (DbGen.tables.get(refType.toLowerCase()) == null && DbGen.tables.get(name.substring(3).toLowerCase()) == null)
      {
        DbGen.error("table for " + name + " not found");
      }
      DbGen.log("  get" + refName + "Id()");
      pw.println("  public " + DbGen.idType + " get" + refName + "Id() throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    return " + name + ";");
      pw.println("  }");
      DbGen.log("  get" + refName + "()");
      pw.println("  public " + ipack + "." + refType + " get" + refName + "() throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    if ( t$" + refName + " == null)");
      pw.println("      t$" + refName + " = new " + refType + "HomeBean(getEJBHome()).findByPrimaryKey(" + name + ");");
      pw.println("    return t$" + refName + ";");
      pw.println("  }");
      DbGen.log("  set" + refName + "(" + ipack + "." + refType + ")");
      pw.print("  public void set" + refName + "(" + ipack + "." + refType + " " + rowName.toLowerCase()
          + ") throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    t$" + refName + " = " + rowName.toLowerCase() + ";");
      pw.println("    " + name + " = t$" + refName + " == null ? null : t$" + refName + ".getId();");
      pw.println("  }");
    }
  }

  void writeFunctionDef(PrintWriter pw, String ipack) throws Exception
  {
    if (refType == null)
    {
      if (javaType.equals("boolean"))
      {
        DbGen.log("  is" + refName + "()");
        pw.println("  public abstract " + javaType + " is" + refName + "() throws java.rmi.RemoteException;");
      }
      DbGen.log("  get" + refName + "()");
      pw.println("  public abstract " + javaType + " get" + refName + "() throws java.rmi.RemoteException;");

      if (!"id".equalsIgnoreCase(rowName))
      {
        pw.print("  public abstract ");
        DbGen.log("  set" + refName + "(" + javaType + ")");
        pw.println(" void set" + refName + "(" + javaType + " " + rowName.toLowerCase() + ") throws java.rmi.RemoteException;");
      }
    } else
    {
      if (DbGen.tables.get(refType.toLowerCase()) == null && DbGen.tables.get(name.substring(3).toLowerCase()) == null)
      {
        DbGen.error("table for " + name + " not found");
      }
      DbGen.log("  get" + refName + "Id()");
      pw.println("  public abstract " + DbGen.idType + " get" + refName + "Id() throws java.rmi.RemoteException;");
      DbGen.log("  get" + refName + "()");
      pw.println("  public abstract " + ipack + "." + refType + " get" + refName
          + "() throws java.rmi.RemoteException;");
      DbGen.log("  set" + refName + "(" + ipack + "." + refType + ")");
      pw.print("  public abstract void set" + refName + "(" + ipack + "." + refType + " " + rowName.toLowerCase()
          + ") throws java.rmi.RemoteException;");
    }
  }

  void writeListFunction(PrintWriter pw) throws Exception
  {
    if (refType != null)
    {
      DbGen.log("  findBy" + refName + "(Object)");
      pw.println("  public java.util.Collection findBy" + refName + "(Object id) throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    return this.queryCollection( \"SELECT * FROM " + DbGen.quote + table.tableName + DbGen.quote
          + " WHERE id_" + refType + "=? ORDER BY ID\", new Object[]{id});");
      pw.println("  }");
    }
  }

  void writeSqlGet(PrintWriter pw, int n) throws Exception
  {
    pw.println("    " + name + " = rs.get" + getType + "(" + n + ");");
  }

  void writeSqlSet(PrintWriter pw, int n) throws Exception
  {
    pw.println("    ps.set" + getType + "(" + n + ", " + name + ");");
  }

  /**
   * Method appendAttribute.
   * @param pw
   */
  void appendAttribute(PrintWriter pw)
  {
    pw.println("    sb.append(\"  <attribute name=\\\"" + name + "\\\">\" + de.bb.mejb.Logger.encodeXML(" + name
        + ") + \"</attribute>\\n\");");
  }

  /**
   * Method appendAssignment.
   * @param pw
   */
  void appendAssignment(PrintWriter pw)
  {
    pw.println("    b." + name + " = " + name + ";");
  }

  public String toString() {
    String r = "row: " + name + ", " + type;
    if (size1 != 0) {
      if (size2 != 0)
        r += "(" + size1 + "," + size2 + ")";
      else
        r += "(" + size1 + ")";      
    }
    return r;
  }


  public String getRefName(int len) {
    if (refName.length() <= len)
      return refName;

    String key = refName + ":" + len;
    String r = table.refNameMap.get(key);
    if (r != null)
      return r;
    
    r = refName.substring(0, len);
    if (!table.refNames.contains(r)) {
      table.refNameMap.put(key, r);
      table.refNames.add(r);
      return r;
    }
    for(;;) {
      len = len - 1;
      for (int i = 0; i < 10; ++i) {
        r = refName.substring(0, len) + i;
        if (!table.refNames.contains(r)) {
          table.refNameMap.put(key, r);
          table.refNames.add(r);
          return r;
        }
      }
    }
  }


}