package de.bb.tools.dbgen;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class JavaCodeGen extends CodeGen {
  /**
   * create the interface in the specified directory.
   * @param t 
   * @param path the used path
   * @param pack the used package
   * @throws Exception on error
   */
  void createInterface(Table t, String path, String pack) throws Exception {
    DbGen.log("creating base interface " + pack + "." + t.classname + "Base");
    PrintWriter pw = createOs(t.classname, path, pack, "Base.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + pack + ";");
    pw.println();
    pw.println("public interface " + t.classname + "Base extends javax.ejb.EJBObject");
    pw.println("{");

    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeFunctionDecl(pw);
    }

    DbGen.log("  toLog()");
    pw.println("  public StringBuffer toLog() throws java.rmi.RemoteException;");
    DbGen.log("  store()");
    pw.println("  public void store() throws java.rmi.RemoteException;");

    // add functions for referring classes
    //      System.out.println(t.classname + " is referenced by:");
    Map subMap = DbGen.references.subMap(t.classname, t.classname + "\0");
    Collection vals = subMap.values();

    for (Iterator i = vals.iterator(); i.hasNext();) {
      String ref = (String) i.next();
      //        System.out.println(ref);  

      DbGen.log("  get" + ref + "List()");
      pw.println("  public java.util.Collection get" + ref + "List() throws java.rmi.RemoteException;");
    }

    pw.println("}");
    pw.close();

    if (DbGen.init || checkOs(t.classname, path, pack, ".java")) {
      DbGen.log("creating interface " + pack + "." + t.classname);
      pw = createOs(t.classname, path, pack, ".java");
      pw.println(DbGen.HEADER_CREATED);
      pw.println("package " + pack + ";");
      pw.println();
      pw.println("public interface " + t.classname + " extends " + t.classname + "Base");
      pw.println("{");
      pw.println("}");
      pw.close();
    }
  }

  /**
   * create the home interface in the specified directory.
   * @param t 
   * @param path the used path
   * @param pack the used package
   * @throws Exception on error
   */
  void createHomeInterface(Table t, String path, String pack) throws Exception {
    DbGen.log("creating home base interface " + pack + "." + t.classname + "HomeBase");
    PrintWriter pw = createOs(t.classname, path, pack, "HomeBase.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + pack + ";");
    pw.println();
    pw.println("public interface " + t.classname + "HomeBase extends java.rmi.Remote, javax.ejb.EJBHome");
    pw.println("{");
    DbGen.log("  findByPrimaryKey(Object)");
    pw.println("  public " + t.classname + " findByPrimaryKey(Object pk) throws java.rmi.RemoteException;");
    DbGen.log("  create()");
    pw.println("  public " + t.classname + " create() throws java.rmi.RemoteException;");
    DbGen.log("  findAll()");
    pw.println("  public java.util.Collection findAll() throws java.rmi.RemoteException;");

    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeListFunctionDecl(pw);
    }

    pw.println("}");
    pw.close();
    if (DbGen.init || checkOs(t.classname, path, pack, "Home.java")) {
      DbGen.log("creating home interface " + pack + "." + t.classname + "Home");
      pw = createOs(t.classname, path, pack, "Home.java");
      pw.println(DbGen.HEADER_CREATED);
      pw.println("package " + pack + ";");
      pw.println();
      pw.println("public interface " + t.classname + "Home extends " + t.classname + "HomeBase");
      pw.println("{");
      pw.println("}");
      pw.close();
    }

  }

  /**
   * create the class in the specified directory.
   * @param t 
   * @param path the used path
   * @param pack the used package for entity beans
   * @param epack the used package for entity beans
   * @throws Exception on error
   */
  void createClass(Table t, String path, String pack, String epack) throws Exception {
    // DATA
    DbGen.log("creating data class " + epack + ".mejb." + t.classname + "BeanData");
    PrintWriter pw = createOs(t.classname, path, epack + ".mejb", "BeanData.java");
    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ".mejb;");
    pw.println();
    pw.println("public class " + t.classname + "BeanData");
    pw.println("  extends de.bb.mejb.CmpData");
    pw.println("  implements " + pack + "." + t.classname);
    pw.println("{");
    pw.println("  /** attributes */");
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeVarDecl(pw, pack);
    }
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeFunction(pw, pack);
    }

    DbGen.log("  readValues(java.sql.ResultSet)");
    pw.println("  protected void readValues(java.sql.ResultSet rs) throws java.sql.SQLException");
    pw.println("  {");
    int n = 1;
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n) {
      Row r = (Row) f.next();
      r.writeSqlGet(pw, n);
    }
    pw.println("  }");
    DbGen.log("  writeValues(java.sql.PreparedStatement)");
    pw.println("  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException");
    pw.println("  {");
    n = 1;
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n) {
      Row r = (Row) f.next();
      if (n > 1)
        r.writeSqlSet(pw, n - 1);
    }
    pw.println("  }");

    // add the log function
    DbGen.log("  toLog()");
    pw.println("  public StringBuffer toLog() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    StringBuffer sb = new StringBuffer();");
    pw.println("    sb.append(\"id=\\\"\" + id + \"\\\">\\n\");");
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.appendAttribute(pw);
    }
    pw.println("    ");
    pw.println("    return sb;");
    pw.println("  }");

    DbGen.log("  toString()");
    pw.println("  public String toString()");
    pw.println("  {");
    pw.println("    StringBuffer sb = new StringBuffer();");
    n = 0;
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      if (n++ > 0)
        pw.println("    sb.append(';');");
      pw.println("    sb.append(" + r.name + ");");
    }
    pw.println("    return sb.toString();");
    pw.println("  }");

    // add equals method
    DbGen.log("  equals(Object)");
    pw.println("  public boolean equals(Object o)");
    pw.println("  {");
    pw.println("    if (!(o instanceof " + t.classname + "BeanData)) return false;");
    pw.println("    " + t.classname + "BeanData x = (" + t.classname + "BeanData)o;");
    pw.println("    return id != null || id.equals(x.id);");
    pw.println("  }");

    pw.println("  public boolean isIdentical(javax.ejb.EJBObject o) { return false;}");
    pw.println("  public Object getPrimaryKey() { return id;}");
    pw.println("  public void setPrimaryKey(Object id) { this.id = (" + DbGen.idType + ")id;}");

    pw.println("}");
    pw.close();

    // ===========================================================================
    // CMP
    DbGen.log("creating cmp class " + epack + ".mejb." + t.classname + "BeanCMP");
    pw = createOs(t.classname, path, epack + ".mejb", "BeanCMP.java");
    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ".mejb;");
    pw.println();
    pw.println("public class " + t.classname + "BeanCMP");
    pw.println("  extends " + epack + "." + t.classname + "Bean");
    pw.println("  implements de.bb.mejb.ICmpBean");
    pw.println("{");
    pw.println("  private " + t.classname + "BeanData data;");
    pw.println("  private " + t.classname + "HomeBeanCMP home;");
    pw.println();
    pw.println("  public " + t.classname + "BeanCMP(" + t.classname + "HomeBeanCMP home) {");
    pw.println("    this.home = home;");
    pw.println("  }");

    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeFunctionCmp(pw, pack);
    }

    pw.println("  public de.bb.mejb.CmpData getData()");
    pw.println("  {");
    pw.println("    return data;");
    pw.println("  }");
    pw.println("  public void setData(de.bb.mejb.CmpData data)");
    pw.println("  {");
    pw.println("    this.data = (" + t.classname + "BeanData)data;");
    pw.println("  }");

    DbGen.log("  toLog()");
    pw.println("  public StringBuffer toLog() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return data.toLog();");
    pw.println("  }");

    pw.println();
    pw.println("// from javax.ejb.EJBObject");
    DbGen.log("  getEJBHome()");
    pw.println("  public javax.ejb.EJBHome getEJBHome() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return home;");
    pw.println("  }");
    DbGen.log("  getHandle()");
    pw.println("  public javax.ejb.Handle getHandle() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return null;");
    pw.println("  }");
    DbGen.log("  getPrimaryKey()");
    pw.println("  public Object getPrimaryKey() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return data.getPrimaryKey();");
    pw.println("  }");
    DbGen.log("  isIdentical(javax.ejb.EJBObject)");
    pw.println("  public boolean isIdentical(javax.ejb.EJBObject eo) throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return false;");
    pw.println("  }");
    DbGen.log("  remove()");
    pw.println("  public void remove() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    home.remove(this);");
    pw.println("  }");
    DbGen.log("  store()");
    pw.println("  public void store() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    home.store(this);");
    pw.println("  }");

    // add functions for referring classes
    for (Iterator i = DbGen.references.subMap(t.classname, t.classname + "\0").values().iterator(); i.hasNext();) {
      String ref = (String) i.next();
      DbGen.log("  get" + ref + "List()");
      pw.println("  public java.util.Collection get" + ref + "List() throws java.rmi.RemoteException");
      pw.println("  {");
      pw.println("    return new " + ref + "HomeBean(getEJBHome()).findBy" + t.classname + "(id);");
      pw.println("  }");
    }

    pw.println("}");
    pw.close();

    DbGen.log("creating base class " + epack + "." + t.classname + "BaseBean");
    pw = createOs(t.classname, path, epack, "BaseBean.java");
    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ";");
    pw.println();
    pw.println("abstract class " + t.classname + "BaseBean");
    pw.println("  implements javax.ejb.EntityBean");
    pw.println("{");
    pw.println("  javax.ejb.EntityContext context;");
    //      pw.println("  /** internal stuff */");      
    //      pw.println("  " + t.classname + "HomeBaseBean myHome;");
    /*
     pw.println("  " + t.classname + "BaseBean(" + t.classname
     + "HomeBaseBean aHome) throws java.rmi.RemoteException");
     pw.println("  {");
     pw.println("    super(aHome);");
     pw.println("  }");
     */
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeFunctionDef(pw, pack);
    }

    pw.println("// from javax.ejb.EntityBean");
    DbGen.log("  ejbStore()");
    pw.println("  public void ejbStore() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    // do nothing");
    pw.println("  }");
    DbGen.log("  ejbRemove()");
    pw.println("  public void ejbRemove() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    // do nothing");
    pw.println("  }");
    DbGen.log("  ejbLoad()");
    pw.println("  public void ejbLoad() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    // do nothing");
    pw.println("  }");
    DbGen.log("  ejbPassivate()");
    pw.println("  public void ejbPassivate() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    // do nothing");
    pw.println("  }");
    DbGen.log("  ejbActivate()");
    pw.println("  public void ejbActivate() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    // do nothing");
    pw.println("  }");
    DbGen.log("  setEntityContext(javax.ejb.EntityContext)");
    pw.println("  public void setEntityContext(javax.ejb.EntityContext context) throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    this.context = context;");
    pw.println("  }");
    DbGen.log("  unsetEntityContext()");
    pw.println("  public void unsetEntityContext() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    this.context = null;");
    pw.println("  }");
    pw.println("  public abstract void remove() throws java.rmi.RemoteException;");
    pw.println("  public abstract void store() throws java.rmi.RemoteException;");

    pw.println("}");
    pw.close();

    if (DbGen.init || checkOs(t.classname, path, epack, "Bean.java")) {

      DbGen.log("creating class " + epack + "." + t.classname + "Bean");
      pw = createOs(t.classname, path, epack, "Bean.java");
      pw.println(DbGen.HEADER_CREATED);
      pw.println("package " + epack + ";");
      pw.println();
      //        pw.println("import java.util.Collection;");
      //        pw.println();
      pw.println("public abstract class " + t.classname + "Bean");
      pw.println("  extends " + t.classname + "BaseBean");
      pw.println("  implements " + pack + "." + t.classname);
      pw.println("{");
      pw.println("  // enter your methods here");
      pw.println("}");
      pw.close();
    }
  }

  /**
   * create the home class in the specified directory.
   * @param t 
   * @param path the used path
   * @param pack the used package for interfaces
   * @param epack the used package for entity beans
   * @throws Exception on error
   */
  void createHomeClass(Table t, String path, String pack, String epack) throws Exception {
    // pack += ".srv";
    epack += ".mejb";
    DbGen.log("creating home base class " + epack + "." + t.classname + "HomeBeanCMP");
    PrintWriter pw = createOs(t.classname, path, epack, "HomeBeanCMP.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ";");
    pw.println();
    //      pw.println("import java.util.Hashtable;");
    //      pw.println();
    pw.println("public class " + t.classname + "HomeBeanCMP");
    pw.println("  extends " + t.classname + "HomeBean");
    pw.println("{");

    pw.println("  private static de.bb.mejb.CMPDbi dbi = initDbi(\"" + epack + "\", \"" + t.classname + "\");");

    DbGen.log("  getDbi()");
    pw.println("  protected de.bb.mejb.CMPDbi getDbi() { return dbi; }");

    DbGen.log("  createCMP()");
    pw.println("  public de.bb.mejb.ICmpBean createCmp() throws java.rmi.RemoteException {");
    pw.println("    " + t.classname + "BeanCMP bean = new " + t.classname + "BeanCMP(this);");
    pw.println("    bean.setData(new " + t.classname + "BeanData());");
    pw.println("    return bean;");
    pw.println("  }");

    DbGen.log("  createData()");
    pw.println("  public Object createData() {");
    pw.println("    return new " + t.classname + "BeanData();");
    pw.println("  }");

    DbGen.log("  findByPrimaryKey(Object)");
    pw.println("  public " + pack + "." + t.classname + " findByPrimaryKey(Object pk) throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return (" + pack + "." + t.classname + ")readByPrimaryKey(pk, dbi);");
    pw.println("  }");
    DbGen.log("  create()");
    pw.println("  public " + pack + "." + t.classname + " create() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return (" + pack + "." + t.classname + ")createCmp();");
    pw.println("  }");

    DbGen.log("  store(Object)");
    pw.println("  void store(Object o) throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    store((de.bb.mejb.ICmpBean)o, " + t.rows.size() + ", dbi);");
    pw.println("  }");
    DbGen.log("  load(Object)");
    pw.println("  " + t.classname + "BeanCMP load(Object obj) throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    " + t.classname + "BeanCMP o = (" + t.classname + "BeanCMP) obj;");
    pw.println("    load(o, dbi);");
    pw.println("    return o;");
    pw.println("  }");
    DbGen.log("  findAll()");
    pw.println("  public java.util.Collection findAll() throws java.rmi.RemoteException");
    pw.println("  {");
    pw.println("    return this.queryCollection(\"SELECT * FROM " + DbGen.quote + t.tableName + DbGen.quote
        + " ORDER BY ID\", NOPARAM);");
    pw.println("  }");

    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeListFunction(pw);
    }

    pw.println("}");
    pw.close();

    // 
    if (DbGen.init || checkOs(t.classname, path, epack, "HomeBean.java")) {
      DbGen.log("creating home class " + epack + "." + t.classname + "HomeBean");
      pw = createOs(t.classname, path, epack, "HomeBean.java");
      pw.println(DbGen.HEADER_CREATED);
      pw.println("package " + epack + ";");
      pw.println();
      pw.println("import java.util.Collection;");
      pw.println();
      pw.println("public abstract class " + t.classname + "HomeBean");
      pw.println("  extends de.bb.mejb.CMPHomeBean");
      pw.println("  implements " + pack + "." + t.classname + "Home ");
      pw.println("{");
      pw.println("}");
      pw.close();
    }
  }

  public void createUtil(Table t, String path, String pack, String epack) throws Exception {
    DbGen.log("creating util class " + pack + "." + "Util");

    PrintWriter pwfb = createOs(t.classname, path, pack, "Util.java");
    pwfb.println(DbGen.HEADER_NOMODIFY);
    pwfb.println("package " + pack + ";");
    pwfb.println();
    pwfb.println("public class " + t.classname + "Util ");
    pwfb.println("{");
    pwfb.println("  private static " + t.classname + "Home cachedHome;");
    pwfb.println("  /** return the home object.");
    pwfb.println("   * @throws java.rmi.RemoteException");
    pwfb.println("   * @return the home object */");
    pwfb.println("  public static " + t.classname + "Home get" + t.classname
        + "Home() throws javax.naming.NamingException");
    pwfb.println("  {");
    pwfb.println("    if (cachedHome != null) return cachedHome;");
    pwfb.println("    java.util.Hashtable environment = new java.util.Hashtable();");
    pwfb.println("    return cachedHome = (" + t.classname
        + "Home)new javax.naming.InitialContext(environment).lookup(\"" + epack + "." + t.classname + "Home\");");
    pwfb.println("  }");
    pwfb.println("}");
    pwfb.close();
  }

  /**
   * create the home class in the specified directory.
   * @param path the used path
   * @param pack the used package
   * @throws Exception 
   * @throws Exception on error
   */
  public void createCMP(Table t, String path, String pack, String epack) throws Exception {
    //pack += ".srv";      
    DbGen.log("creating MSSQL DBI class " + epack + "." + t.classname + "Dbi");
    PrintWriter pw = createOs(t.classname, path, epack + "/mssql", "Dbi.java");

    pw.println(DbGen.HEADER_NOMODIFY);
    pw.println("package " + epack + ".mssql;");
    pw.println();
    pw.println("public class " + t.classname + "Dbi extends de.bb.mejb.CMPDbi");
    pw.println("{");
    pw.println("  public " + t.classname + "Dbi()");
    pw.println("  {}");
    pw.println();
    pw.println("  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException");
    pw.println("  {");
    pw.println("    java.sql.Statement stmt = conn.createStatement();");
    pw.println("    String q = \"DELETE FROM " + DbGen.quote + t.tableName + DbGen.quote + " WHERE id=\" + id;");
    pw.println("    stmt.executeUpdate(q);");
    pw.println("    stmt.close();");
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
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n) {
      Row r = (Row) f.next();
      if (n > 1) {
        if (n > 2) {
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
    for (Iterator f = t.rows.values().iterator(); f.hasNext(); ++n) {
      Row r = (Row) f.next();
      if (n > 1) {
        if (n > 2) {
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
    pw.println("    java.sql.ResultSet rs = stmt.executeQuery(\"SELECT @@identity FROM " + DbGen.quote + t.tableName
        + DbGen.quote + "\");");
    pw.println("    if (!rs.next())");
    pw.println("      throw new java.sql.SQLException(\"got no from insert;\");");
    pw.println("    String id = rs.getString(1);");
    pw.println("    rs.close();");
    pw.println("    return id;");
    pw.println("  }");
    pw.println("}");
    pw.close();
  }

  public void processTables(HashMap<String, Table> tables, String path, Map<String, String> params, boolean verbose)
      throws Exception {
    
    String globalPackage = params.get("globalPackage");
    String ejbPackage = params.get("ejbPackage");
    
    for (Table t : tables.values()) {
      if (verbose)
        System.out.println("creating classes for table: " + t.tableName);

      createInterface(t, path, globalPackage);
      createHomeInterface(t, path, globalPackage);
      createClass(t, path, globalPackage, ejbPackage);
      createHomeClass(t, path, globalPackage, ejbPackage);
      createUtil(t, path, globalPackage, ejbPackage);
      createCMP(t, path, globalPackage, ejbPackage);
    }
  }
}
