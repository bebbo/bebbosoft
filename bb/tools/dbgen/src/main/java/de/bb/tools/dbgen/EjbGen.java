package de.bb.tools.dbgen;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class EjbGen extends JavaCodeGen {

  void createClass(Table t, String path, String pack, String epack) throws Exception {
    PrintWriter pw = createOs(t.classname, path, epack, "Bean.java");

    pw.println(DbGen.HEADER_CREATED);
    pw.println("package " + epack + ";");
    pw.println();

    pw.println("import javax.ejb.CreateException;");
    pw.println("import javax.ejb.EntityBean;");
    pw.println("import javax.ejb.FinderException;");
    pw.println("import javax.naming.NamingException;");

    pw.println();
    pw.println("/**");
    pw.println("* @ejb.bean name = \"" + t.classname + "\"");
    pw.println("*           type = \"CMP\"");
    pw.println("*           cmp-version = \"2.x\"");
    pw.println("*           display-name = \"" + t.classname + "Bean\"");
    pw.println("*           description = \"" + t.classname + "Bean EJB\"");
    pw.println("*           view-type = \"local\"");
    pw.println("*           jndi-name = \"" + t.classname + "Local\"");
    pw.println("*           primkey-field = \"id\"");
    pw.println("* @ejb:util generate=\"physical\"");
    pw.println("* @ejb.transaction type=\"Supports\"");
    pw.println("* @ejb.persistence table-name=\"" + t.tableName + "\"");
    pw.println("*");
    pw.println("*");
    pw.println("* @ejb.pk class=\"" + DbGen.idType + "\"");
    pw.println("*         generate=\"false\"");
    pw.println("*");
    pw.println("* @weblogic.data-source-name " + DbGen.dataSource);
    pw.println("*");
    pw.println("* @weblogic.automatic-key-generation generator-type=\"ORACLE\"");
    pw.println("*           generator-name=\"SE_" + t.tableName + "\"");
    pw.println("*           key-cache-size=\"1\"");
    pw.println("*");
    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeListFunctionEjb(pw, pack);
    }

    /*
     pw.println("* @ejb.finder");
     pw.println("*    signature=\"" + t.classname + " findByName(java.lang.String name)\"");
     pw.println("*    query=\"SELECT object(p) FROM " + t.tableName + " p WHERE p.name=?1\"");
     */

    pw.println("*/");

    pw.println("public abstract class " + t.classname + "Bean implements EntityBean");
    pw.println("{");

    pw.println(" /**");
    pw.println("  * @ejb.create-method");
    //    pw.println("  * @ejb.transaction type=\"Required\"");
    pw.println("  * @ejb.interface-method view-type = \"both\"");
    pw.println("  */");
    pw.println("  public " + DbGen.idType + " ejbCreate() throws CreateException");
    pw.println("  {");
    pw.println("    return null;");
    pw.println("  }");

    pw.println("");

    for (Iterator f = t.rows.values().iterator(); f.hasNext();) {
      Row r = (Row) f.next();
      r.writeFunctionEjb(pw, pack);
    }

    // add functions for referring classes
    //      System.out.println(t.classname + " is referenced by:");
    Map subMap = DbGen.references.subMap(t.classname, t.classname + "\0");
    Collection vals = subMap.values();

    for (Iterator i = vals.iterator(); i.hasNext();) {
      String ref = (String) i.next();
      String iref = DbGen.convert(ref);

      Table other = (Table) DbGen.tables.get(ref.toLowerCase());
      String addid = "Id";
      String unique = (String) other.uniques.get(t.classname.toLowerCase() + "_id");
      if (unique == null) {
        unique = (String) other.uniques.get(t.classname.toLowerCase());
        addid = "";
      }
      if (unique != null) {
        pw.println(" /**");
        pw.println("  * @ejb.interface-method view-type = \"local\"");
        pw.println("  */");
        pw.println("  public " + epack + "." + iref + " find" + ref + "By" + t.classname + addid
            + "() throws NamingException, FinderException");
        pw.println("  {");
        pw.println("    return " + epack + "." + ref + "Util.getLocalHome().find" + ref + "By" + t.classname + addid
            + "(getId());");
        pw.println("  }");
      } else {
        addid = "Id";
        unique = (String) other.uniques.get(t.classname.toLowerCase() + "_id, deleted");
        if (unique == null) {
          unique = (String) other.uniques.get(t.classname.toLowerCase() + ", deleted");
          addid = "";
        }
        if (unique != null) {
          pw.println(" /**");
          pw.println("  * @ejb.interface-method view-type = \"local\"");
          pw.println("  */");
          pw.println("  public " + epack + "." + iref + " find" + ref + "By" + t.classname + addid
              + "() throws NamingException, FinderException");
          pw.println("  {");
          pw.println("    return " + epack + "." + ref + "Util.getLocalHome().find" + ref + "By" + t.classname + addid
              + "(getId());");
          pw.println("  }");
        }
        DbGen.log("  find" + ref + "List()");
        pw.println(" /**");
        pw.println("  * @ejb.interface-method view-type = \"local\"");
        pw.println("  */");
        pw.println("  public java.util.Collection find" + ref + "ListBy" + t.classname
            + "Id() throws NamingException, FinderException");
        pw.println("  {");
        pw.println("    return " + epack + "." + ref + "Util.getLocalHome().find" + ref + "ListBy" + t.classname
            + "Id(getId());");
        pw.println("  }");
        /*        
         pw.println(" /**");
         pw.println("  * @ejb.interface-method view-type = \"local\"");
         pw.println("  *\/");
         pw.println("  public java.util.Iterator get" + ref + "s()");
         pw.println("  {");
         pw.println("    return " + epack + "." + ref + "Util.getLocalHome().get" + ref + "s(getId());");
         pw.println("  }");
         */

      }
    }

    pw.println("}");
    pw.close();
  }

  public void createCMP(Table t, String path, String pack, String epack) throws Exception {
  }

  void createHomeClass(Table t, String path, String pack, String epack) throws Exception {
  }

  void createHomeInterface(Table t, String path, String pack) throws Exception {
  }

  void createInterface(Table t, String path, String pack) throws Exception {
  }

  public void createUtil(Table t, String path, String pack, String epack) throws Exception {
  }

}
