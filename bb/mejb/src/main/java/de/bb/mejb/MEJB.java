package de.bb.mejb;

import de.bb.util.ByteRef;
import de.bb.util.MultiMap;
import java.io.*;
import java.util.*;

public class MEJB {
    static class d {

        void Code(PrintWriter printwriter, String s1) throws Exception {
            try {
                if (a.equals("id") || a.startsWith("id_"))
                    throw new Exception();
                Class class1 = Class.forName(f);
                class1.newInstance();
                printwriter.println("  protected " + f + " " + a + " = new " + f + "();");
            } catch (Exception _ex) {
                printwriter.println("  protected " + f + " " + a + ";");
            }
            if (h != null)
                printwriter.println("  protected " + s1 + "." + h + " t$" + i + ";");
        }

        void a(PrintWriter printwriter) throws Exception {
            if (h == null) {
                if (f.equals("boolean")) {
                    MEJB.n("  is" + i + "()");
                    printwriter.println("  public " + f + " is" + i + "() throws java.rmi.RemoteException;");
                }
                MEJB.n("  get" + i + "()");
                printwriter.println("  public " + f + " get" + i + "() throws java.rmi.RemoteException;");
                if (!"id".equalsIgnoreCase(e)) {
                    MEJB.n("  set" + i + "(" + f + ")");
                    printwriter.println("  public void set" + i + "(" + f + " a$" + i
                            + ") throws java.rmi.RemoteException;");
                }
            } else {
                MEJB.n("  get" + i + "Id()");
                printwriter.println("  public String get" + i + "Id() throws java.rmi.RemoteException;");
                MEJB.n("  get" + i + "()");
                printwriter.println("  public " + h + " get" + i + "() throws java.rmi.RemoteException;");
                MEJB.n("  set" + i + "(" + h + ")");
                printwriter.println("  public void set" + i + "(" + h + " a$" + i
                        + ") throws java.rmi.RemoteException;");
            }
        }

        void b(PrintWriter printwriter) throws Exception {
            if (h != null) {
                MEJB.n("  findBy" + i + "(Object)");
                printwriter.println("  public java.util.Collection findBy" + i
                        + "(Object id) throws java.rmi.RemoteException;");
            }
        }

        void c(PrintWriter printwriter, String s1) throws Exception {
            if (h == null) {
                if (f.equals("boolean")) {
                    MEJB.n("  is" + i + "()");
                    printwriter.println("  public " + f + " is" + i + "() throws java.rmi.RemoteException");
                    printwriter.println("  {");
                    printwriter.println("    return " + a + ";");
                    printwriter.println("  }");
                }
                MEJB.n("  get" + i + "()");
                printwriter.println("  public " + f + " get" + i + "() throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    return " + a + ";");
                printwriter.println("  }");
                if ("id".equalsIgnoreCase(e))
                    printwriter.print("  protected");
                else
                    printwriter.print("  public");
                MEJB.n("  set" + i + "(" + f + ")");
                printwriter.println(" void set" + i + "(" + f + " a$" + i + ") throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    " + a + " = a$" + i + ";");
                printwriter.println("  }");
            } else {
                if (MEJB.r.get((new ByteRef(h)).toLowerCase()) == null
                        && MEJB.r.get(a.substring(3).toLowerCase()) == null)
                    MEJB.o("table for " + a + " not found");
                MEJB.n("  get" + i + "Id()");
                printwriter.println("  public String get" + i + "Id() throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    return " + a + ";");
                printwriter.println("  }");
                MEJB.n("  get" + i + "()");
                printwriter.println("  public " + s1 + "." + h + " get" + i + "() throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    if ( t$" + i + " == null)");
                printwriter.println("      t$" + i + " = new " + h + "HomeBean(getEJBHome()).findByPrimaryKey(" + a
                        + ");");
                printwriter.println("    return t$" + i + ";");
                printwriter.println("  }");
                MEJB.n("  set" + i + "(" + s1 + "." + h + ")");
                printwriter.print("  public void set" + i + "(" + s1 + "." + h + " a$" + i
                        + ") throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    t$" + i + " = a$" + i + ";");
                printwriter.println("    " + a + " = t$" + i + " == null ? null : t$" + i + ".getId();");
                printwriter.println("  }");
            }
        }

        void d(PrintWriter printwriter) throws Exception {
            if (h != null) {
                MEJB.n("  findBy" + i + "(Object)");
                printwriter.println("  public java.util.Collection findBy" + i
                        + "(Object id) throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    return this.queryCollection( \"SELECT * FROM " + MEJB.t + Code.Code + MEJB.t
                        + " WHERE id_" + h + "=? ORDER BY ID\", new Object[]{id});");
                printwriter.println("  }");
            }
        }

        void e(PrintWriter printwriter, int i1) throws Exception {
            printwriter.println("    " + a + " = rs.get" + g + "(" + i1 + ");");
        }

        void f(PrintWriter printwriter, int i1) throws Exception {
            printwriter.println("    ps.set" + g + "(" + i1 + ", " + a + ");");
        }

        void g(PrintWriter printwriter) {
            printwriter.println("    sb.append(\"  <attribute name=\\\"" + a + "\\\">\" + encodeXML(" + a
                    + ") + \"</attribute>\\n\");");
        }

        void h(PrintWriter printwriter) {
            printwriter.println("    b." + a + " = " + a + ";");
        }

        b Code;
        ByteRef a;
        ByteRef b;
        ByteRef c;
        ByteRef d;
        String e;
        String f;
        String g;
        String h;
        String i;

        d(b b1, ByteRef byteref, ByteRef byteref1, ByteRef byteref2, ByteRef byteref3) {
            Code = b1;
            a = byteref;
            b = byteref1.toUpperCase();
            c = byteref2;
            d = byteref3;
            i = e = a.substring(0, 1).toString().toUpperCase() + a.substring(1).toString();
            if (b.equals("INT") && c != null && c.equals("1"))
                f = MEJB.a(new ByteRef("BOOL"));
            else
                f = MEJB.a(b);
            if (a.equals("id"))
                f = "String";
            else if (a.startsWith("id_")) {
                f = "String";
                e = e.substring(3, 4).toUpperCase() + e.substring(4);
                h = e;
                i = h;
                int i1 = h.indexOf('_');
                if (i1 > 0) {
                    h = e.substring(0, i1);
                    i = e.substring(i1 + 1) + h;
                    i = i.substring(0, 1).toUpperCase() + i.substring(1);
                }
                MEJB.s.put(i, Code.a);
            }
            g = f;
            int j1 = g.lastIndexOf('.');
            if (j1 > 0)
                g = g.substring(j1 + 1);
            g = g.substring(0, 1).toUpperCase() + g.substring(1);
        }
    }

    static class b {

        void Code(ByteRef byteref, ByteRef byteref1, ByteRef byteref2, ByteRef byteref3) {
            b.addElement(new d(this, byteref, byteref1, byteref2, byteref3));
        }

        PrintWriter a(String s1, String s2, String s3) throws Exception {
            return MEJB.j(s1, s2, a + s3);
        }

        boolean b(String s1, String s2, String s3) throws Exception {
            String s4 = MEJB.Code(s2);
            File file = new File(s1, s4);
            File file1 = new File(file, a + s3);
            return !file1.exists();
        }

        void c(String s1, String s2) throws Exception {
            MEJB.n("creating base interface " + s2 + "." + a + "Base");
            PrintWriter printwriter = a(s1, s2, "Base.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s2 + ";");
            printwriter.println();
            printwriter.println("public interface " + a + "Base extends " + MEJB.p());
            printwriter.println("{");
            d d1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements(); d1.a(printwriter))
                d1 = (d) enumeration.nextElement();

            MEJB.n("  toLog()");
            printwriter.println("  public StringBuffer toLog() throws java.rmi.RemoteException;");
            MEJB.n("  store()");
            printwriter.println("  public void store() throws java.rmi.RemoteException;");
            SortedMap sortedmap = MEJB.s.subMap(a, a + "\0");
            Collection collection = sortedmap.values();
            String s3;
            for (Iterator iterator = collection.iterator(); iterator.hasNext(); printwriter
                    .println("  public java.util.Collection get" + s3 + "List() throws java.rmi.RemoteException;")) {
                s3 = (String) iterator.next();
                MEJB.n("  get" + s3 + "List()");
            }

            printwriter.println("}");
            printwriter.close();
            if (MEJB.q() || b(s1, s2, "Home.java")) {
                MEJB.n("creating home interface " + s2 + "." + a + "Home");
                PrintWriter printwriter1 = a(s1, s2, "Home.java");
                printwriter1
                        .println("/*\r\n * This file is initially generated by MEJB.\r\n * PLACE YOUR MODIFICATIONS HERE!\r\n*/\r\n\r\n");
                printwriter1.println("package " + s2 + ";");
                printwriter1.println();
                printwriter1.println("public interface " + a + "Home extends " + a + "HomeBase");
                printwriter1.println("{");
                printwriter1.println("}");
                printwriter1.close();
            }
        }

        void d(String s1, String s2) throws Exception {
            MEJB.n("creating home base interface " + s2 + "." + a + "HomeBase");
            PrintWriter printwriter = a(s1, s2, "HomeBase.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s2 + ";");
            printwriter.println();
            printwriter.println("public interface " + a + "HomeBase extends java.rmi.Remote, javax.ejb.EJBHome");
            printwriter.println("{");
            MEJB.n("  findByPrimaryKey(Object)");
            printwriter.println("  public " + a + " findByPrimaryKey(Object pk) throws java.rmi.RemoteException;");
            MEJB.n("  create()");
            printwriter.println("  public " + a + " create() throws java.rmi.RemoteException;");
            MEJB.n("  findAll()");
            printwriter.println("  public java.util.Collection findAll() throws java.rmi.RemoteException;");
            d d1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements(); d1.b(printwriter))
                d1 = (d) enumeration.nextElement();

            printwriter.println("}");
            printwriter.close();
            if (MEJB.q() || b(s1, s2, ".java")) {
                MEJB.n("creating interface " + s2 + "." + a);
                PrintWriter printwriter1 = a(s1, s2, ".java");
                printwriter1
                        .println("/*\r\n * This file is initially generated by MEJB.\r\n * PLACE YOUR MODIFICATIONS HERE!\r\n*/\r\n\r\n");
                printwriter1.println("package " + s2 + ";");
                printwriter1.println();
                printwriter1.println("public interface " + a + " extends " + a + "Base");
                printwriter1.println("{");
                printwriter1.println("}");
                printwriter1.close();
            }
        }

        void e(String s1, String s2, String s3) throws Exception {
            MEJB.n("creating base class " + s3 + "." + a + "BaseBean");
            PrintWriter printwriter = a(s1, s3, "BaseBean.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s3 + ";");
            printwriter.println();
            printwriter.println("abstract class " + a + "BaseBean");
            printwriter.println("  extends de.bb.mejb.CMPBean");
            printwriter.println("  implements " + s2 + "." + a + "Base");
            printwriter.println("{");
            printwriter.println();
            printwriter.println("  /** attributes */");
            d d1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements(); d1.Code(printwriter, s2))
                d1 = (d) enumeration.nextElement();

            printwriter.println();
            printwriter.println("  " + a + "BaseBean(" + a + "HomeBaseBean aHome) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    super(aHome);");
            printwriter.println("  }");
            printwriter.println();
            d d2;
            for (Enumeration enumeration1 = b.elements(); enumeration1.hasMoreElements(); d2.c(printwriter, s2))
                d2 = (d) enumeration1.nextElement();

            printwriter.println("// from javax.ejb.EntityBean");
            MEJB.n("  ejbStore()");
            printwriter.println("  public void ejbStore() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    ((" + a + "HomeBaseBean)myHome).store(this);");
            printwriter.println("  }");
            MEJB.n("  ejbRemove()");
            printwriter.println("  public void ejbRemove() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    ((" + a + "HomeBaseBean)myHome).remove(this);");
            printwriter.println("  }");
            MEJB.n("  ejbLoad()");
            printwriter.println("  public void ejbLoad() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    ((" + a + "HomeBaseBean)myHome).load(this);");
            printwriter.println("  }");
            MEJB.n("  ejbPassivate()");
            printwriter.println("  public void ejbPassivate() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    // do nothing");
            printwriter.println("  }");
            MEJB.n("  ejbActivate()");
            printwriter.println("  public void ejbActivate() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    // do nothing");
            printwriter.println("  }");
            MEJB.n("  setEntityContext(javax.ejb.EntityContext)");
            printwriter
                    .println("  public void setEntityContext(javax.ejb.EntityContext ec) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    // do nothing");
            printwriter.println("  }");
            MEJB.n("  unsetEntityContext()");
            printwriter.println("  public void unsetEntityContext() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    // do nothing");
            printwriter.println("  }");
            printwriter.println("// from javax.ejb.EJBObject");
            MEJB.n("  getEJBHome()");
            printwriter.println("  public javax.ejb.EJBHome getEJBHome() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return myHome;");
            printwriter.println("  }");
            MEJB.n("  getHandle()");
            printwriter.println("  public javax.ejb.Handle getHandle() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return null;");
            printwriter.println("  }");
            MEJB.n("  getPrimaryKey()");
            printwriter.println("  public Object getPrimaryKey() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return id;");
            printwriter.println("  }");
            MEJB.n("  isIdentical(javax.ejb.EJBObject)");
            printwriter.println("  public boolean isIdentical(javax.ejb.EJBObject eo) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return false;");
            printwriter.println("  }");
            MEJB.n("  remove()");
            printwriter.println("  public void remove() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    ejbRemove();");
            printwriter.println("  }");
            MEJB.n("  store()");
            printwriter.println("  public void store() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    ejbStore();");
            printwriter.println("  }");
            MEJB.n("  readValues(java.sql.ResultSet)");
            printwriter.println("  protected void readValues(java.sql.ResultSet rs) throws java.sql.SQLException");
            printwriter.println("  {");
            int i1 = 1;
            for (Enumeration enumeration2 = b.elements(); enumeration2.hasMoreElements();) {
                d d3 = (d) enumeration2.nextElement();
                d3.e(printwriter, i1);
                i1++;
            }

            printwriter.println("  }");
            MEJB.n("  writeValues(java.sql.PreparedStatement)");
            printwriter
                    .println("  protected void writeValues(java.sql.PreparedStatement ps) throws java.sql.SQLException");
            printwriter.println("  {");
            i1 = 1;
            for (Enumeration enumeration3 = b.elements(); enumeration3.hasMoreElements();) {
                d d4 = (d) enumeration3.nextElement();
                if (i1 > 1)
                    d4.f(printwriter, i1 - 1);
                i1++;
            }

            printwriter.println("  }");
            MEJB.n("  toLog()");
            printwriter.println("  public StringBuffer toLog() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    StringBuffer sb = new StringBuffer();");
            printwriter.println("    sb.append(\"id=\\\"\" + id + \"\\\">\\n\");");
            d d5;
            for (Enumeration enumeration4 = b.elements(); enumeration4.hasMoreElements(); d5.g(printwriter))
                d5 = (d) enumeration4.nextElement();

            printwriter.println("    ");
            printwriter.println("    return sb;");
            printwriter.println("  }");
            MEJB.n("  toString()");
            printwriter.println("  public String toString()");
            printwriter.println("  {");
            printwriter.println("    StringBuffer sb = new StringBuffer();");
            i1 = 0;
            d d6;
            for (Enumeration enumeration5 = b.elements(); enumeration5.hasMoreElements(); printwriter
                    .println("    sb.append(" + d6.a + ");")) {
                d6 = (d) enumeration5.nextElement();
                if (i1++ > 0)
                    printwriter.println("    sb.append(';');");
            }

            printwriter.println("    return sb.toString();");
            printwriter.println("  }");
            MEJB.n("  equals(Object)");
            printwriter.println("  public boolean equals(Object o)");
            printwriter.println("  {");
            printwriter.println("    if (!(o instanceof " + a + "BaseBean)) return false;");
            printwriter.println("    " + a + "BaseBean x = (" + a + "BaseBean)o;");
            printwriter.println("    return (id == null && x.id == null) || id.equals(x.id);");
            printwriter.println("  }");
            MEJB.n("  assign(de.bb.mejb.CMPBean, de.bb.mejb.CMPHomeBean)");
            printwriter
                    .println("  protected void assign(de.bb.mejb.CMPBean sb, de.bb.mejb.CMPHomeBean home) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    " + a + "Bean b = (" + a + "Bean)sb;");
            d d7;
            for (Enumeration enumeration6 = b.elements(); enumeration6.hasMoreElements(); d7.h(printwriter))
                d7 = (d) enumeration6.nextElement();

            printwriter.println("  }");
            for (Iterator iterator = MEJB.s.subMap(a, a + "\0").values().iterator(); iterator.hasNext(); printwriter
                    .println("  }")) {
                String s4 = (String) iterator.next();
                MEJB.n("  get" + s4 + "List()");
                printwriter
                        .println("  public java.util.Collection get" + s4 + "List() throws java.rmi.RemoteException");
                printwriter.println("  {");
                printwriter.println("    return new " + s4 + "HomeBean(getEJBHome()).findBy" + a + "(id);");
            }

            printwriter.println("}");
            printwriter.close();
            if (MEJB.q() || b(s1, s3, "Bean.java")) {
                MEJB.n("creating class " + s3 + "." + a + "Bean");
                PrintWriter printwriter1 = a(s1, s3, "Bean.java");
                printwriter1
                        .println("/*\r\n * This file is initially generated by MEJB.\r\n * PLACE YOUR MODIFICATIONS HERE!\r\n*/\r\n\r\n");
                printwriter1.println("package " + s3 + ";");
                printwriter1.println();
                printwriter1.println("public class " + a + "Bean");
                printwriter1.println("  extends " + a + "BaseBean");
                printwriter1.println("  implements " + s2 + "." + a);
                printwriter1.println("{");
                printwriter1.println("  public " + a + "Bean(" + a
                        + "HomeBaseBean aHome) throws java.rmi.RemoteException");
                printwriter1.println("  {");
                printwriter1.println("    super(aHome);");
                printwriter1.println("  }");
                printwriter1.println("}");
                printwriter1.close();
            }
        }

        void f(String s1, String s2, String s3) throws Exception {
            MEJB.n("creating home base class " + s3 + "." + a + "HomeBaseBean");
            PrintWriter printwriter = a(s1, s3, "HomeBaseBean.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s3 + ";");
            printwriter.println();
            printwriter.println("abstract class " + a + "HomeBaseBean");
            printwriter.println("  extends de.bb.mejb.CMPHomeBean");
            printwriter.println("  implements " + s2 + "." + a + "HomeBase");
            printwriter.println("{");
            printwriter.println("  private static de.bb.mejb.CMPDbi dbi = null;");
            MEJB.n("  getDbi()");
            printwriter.println("  protected de.bb.mejb.CMPDbi getDbi() { return dbi; }");
            MEJB.n("  init()");
            printwriter.println("  private synchronized void init() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    dbi = initDbi(\"" + s3 + "\", \"" + a + "\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter.println("  " + a + "HomeBaseBean(javax.ejb.EJBHome home) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    super(home, \"" + a + "\");");
            printwriter.println("    if (dbi == null)");
            printwriter.println("      init();");
            printwriter.println("  }");
            MEJB.n("  getEJBMetaData()");
            printwriter.println("  public javax.ejb.EJBMetaData getEJBMetaData() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return null;");
            printwriter.println("  }");
            MEJB.n("  getHomeHandle()");
            printwriter.println("  public javax.ejb.HomeHandle getHomeHandle() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return null;");
            printwriter.println("  }");
            MEJB.n("  remove()");
            printwriter.println("  public void remove(Object o) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    remove((de.bb.mejb.CMPBean)o, dbi);");
            printwriter.println("  }");
            MEJB.n("  remove(javax.ejb.Handle)");
            printwriter.println("  public void remove(javax.ejb.Handle handle) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("  }");
            MEJB.n("  findByPrimaryKey(Object)");
            printwriter.println("  public " + s2 + "." + a
                    + " findByPrimaryKey(Object pk) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return (" + s2 + "." + a + ")readByPrimaryKey(pk, dbi);");
            printwriter.println("  }");
            MEJB.n("  create()");
            printwriter.println("  public " + s2 + "." + a + " create() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return new " + a + "Bean(this);");
            printwriter.println("  }");
            printwriter.println("  // not ejb standard functions");
            MEJB.n("  internCreate()");
            printwriter.println("  public de.bb.mejb.Simple internCreate() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return (de.bb.mejb.Simple)create();");
            printwriter.println("  }");
            MEJB.n("  store(Object)");
            printwriter.println("  void store(Object o) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    store((de.bb.mejb.CMPBean)o, " + b.size() + ", dbi);");
            printwriter.println("  }");
            MEJB.n("  load(Object)");
            printwriter.println("  " + a + "Bean load(Object obj) throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    " + a + "Bean o = (" + a + "Bean) obj;");
            printwriter.println("    load(o, dbi);");
            printwriter.println("    return o;");
            printwriter.println("  }");
            MEJB.n("  findAll()");
            printwriter.println("  public java.util.Collection findAll() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    return this.queryCollection(\"SELECT * FROM " + MEJB.t + Code + MEJB.t
                    + " ORDER BY ID\", noParam);");
            printwriter.println("  }");
            d d1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements(); d1.d(printwriter))
                d1 = (d) enumeration.nextElement();

            printwriter.println("}");
            printwriter.close();
            if (MEJB.q() || b(s1, s3, "HomeBean.java")) {
                MEJB.n("creating home class " + s3 + "." + a + "HomeBean");
                PrintWriter printwriter1 = a(s1, s3, "HomeBean.java");
                printwriter1
                        .println("/*\r\n * This file is initially generated by MEJB.\r\n * PLACE YOUR MODIFICATIONS HERE!\r\n*/\r\n\r\n");
                printwriter1.println("package " + s3 + ";");
                printwriter1.println();
                printwriter1.println("import java.util.Collection;");
                printwriter1.println();
                printwriter1.println("public class " + a + "HomeBean");
                printwriter1.println("  extends " + a + "HomeBaseBean");
                printwriter1.println("  implements " + s2 + "." + a + "Home ");
                printwriter1.println("{");
                printwriter1.println("  public " + a
                        + "HomeBean(javax.ejb.EJBHome home) throws java.rmi.RemoteException");
                printwriter1.println("  {");
                printwriter1.println("    super(home);");
                printwriter1.println("  }");
                printwriter1.println("}");
                printwriter1.close();
            }
        }

        void g(String s1, String s2) throws Exception {
            MEJB.n("creating MSSQL DBI class " + s2 + "." + a + "Dbi");
            PrintWriter printwriter = a(s1, s2 + "/mssql", "Dbi.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s2 + ".mssql;");
            printwriter.println();
            printwriter.println("public class " + a + "Dbi extends de.bb.mejb.CMPDbi");
            printwriter.println("{");
            printwriter.println("  public " + a + "Dbi()");
            printwriter.println("  {}");
            printwriter.println();
            printwriter
                    .println("  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    String q = \"DELETE FROM " + MEJB.t + Code + MEJB.t + " WHERE id=\" + id;");
            printwriter.println("    stmt.executeUpdate(q);");
            printwriter.println("    stmt.close();");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.ResultSet select(java.sql.Connection conn, Object id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    String q = \"SELECT * FROM " + MEJB.t + Code + MEJB.t + " WHERE id=\" + id;");
            printwriter.println("    return stmt.executeQuery(q);");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement insert(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            StringBuffer stringbuffer = new StringBuffer();
            StringBuffer stringbuffer1 = new StringBuffer();
            int i1 = 1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements();) {
                d d1 = (d) enumeration.nextElement();
                if (i1 > 1) {
                    if (i1 > 2) {
                        stringbuffer.append(',');
                        stringbuffer1.append(',');
                    }
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append(d1.a);
                    stringbuffer.append(MEJB.t);
                    stringbuffer1.append('?');
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"INSERT INTO " + MEJB.t + Code + MEJB.t + " ("
                    + stringbuffer.toString() + ") VALUES (" + stringbuffer1.toString() + ")\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement update(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            stringbuffer = new StringBuffer();
            i1 = 1;
            for (Enumeration enumeration1 = b.elements(); enumeration1.hasMoreElements();) {
                d d2 = (d) enumeration1.nextElement();
                if (i1 > 1) {
                    if (i1 > 2)
                        stringbuffer.append(',');
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append(d2.a);
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append("=?");
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"UPDATE " + MEJB.t + Code + MEJB.t + " SET "
                    + stringbuffer.toString() + " WHERE id=?\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter.println("  public String getId(java.sql.Statement stmt) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.ResultSet rs = stmt.executeQuery(\"SELECT @@identity FROM " + MEJB.t
                    + Code + MEJB.t + "\");");
            printwriter.println("    if (!rs.next())");
            printwriter.println("      throw new java.sql.SQLException(\"got no from insert;\");");
            printwriter.println("    String id = rs.getString(1);");
            printwriter.println("    rs.close();");
            printwriter.println("    return id;");
            printwriter.println("  }");
            printwriter.println("}");
            printwriter.close();
        }

        void h(String s1, String s2) throws Exception {
            MEJB.n("creating MySQL DBI class " + s2 + "." + a + "Dbi");
            PrintWriter printwriter = a(s1, s2 + "/mysql", "Dbi.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s2 + ".mysql;");
            printwriter.println();
            printwriter.println("public class " + a + "Dbi extends de.bb.mejb.CMPDbi");
            printwriter.println("{");
            printwriter.println("  public " + a + "Dbi()");
            printwriter.println("  {}");
            printwriter.println();
            printwriter
                    .println("  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.ResultSet rs = null;");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    try");
            printwriter.println("    {");
            printwriter.println("      String q;");
            for (Iterator iterator = MEJB.s.subMap(a, a + "\0").values().iterator(); iterator.hasNext(); printwriter
                    .println("      rs = null;")) {
                String s3 = (String) iterator.next();
                printwriter.println("      q = \"SELECT id FROM " + MEJB.t + s3 + MEJB.t + " WHERE id_" + Code
                        + "=\" + id;");
                printwriter.println("      rs = stmt.executeQuery(q);");
                printwriter
                        .println("      if (rs.next()) throw new java.sql.SQLException(\"cant remove id=\" + id + \" from "
                                + Code + " - FOREIGN KEY exists in " + s3 + "\");");
                printwriter.println("      rs.close();");
            }

            printwriter.println("      q = \"DELETE FROM " + MEJB.t + Code + MEJB.t + " WHERE id=\" + id;");
            printwriter.println("      stmt.executeUpdate(q);");
            printwriter.println("    } finally");
            printwriter.println("    {");
            printwriter.println("      if (rs != null) try { rs.close(); } catch (Exception ex){}");
            printwriter.println("      stmt.close();");
            printwriter.println("    }");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.ResultSet select(java.sql.Connection conn, Object id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    String q = \"SELECT * FROM " + MEJB.t + Code + MEJB.t + " WHERE id=\" + id;");
            printwriter.println("    return stmt.executeQuery(q);");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement insert(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            StringBuffer stringbuffer = new StringBuffer();
            StringBuffer stringbuffer1 = new StringBuffer();
            int i1 = 1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements();) {
                d d1 = (d) enumeration.nextElement();
                if (i1 > 1) {
                    if (i1 > 2) {
                        stringbuffer.append(',');
                        stringbuffer1.append(',');
                    }
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append(d1.a);
                    stringbuffer.append(MEJB.t);
                    stringbuffer1.append('?');
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"INSERT INTO " + MEJB.t + Code + MEJB.t + " ("
                    + stringbuffer.toString() + ") VALUES (" + stringbuffer1.toString() + ")\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement update(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            stringbuffer = new StringBuffer();
            i1 = 1;
            for (Enumeration enumeration1 = b.elements(); enumeration1.hasMoreElements();) {
                d d2 = (d) enumeration1.nextElement();
                if (i1 > 1) {
                    if (i1 > 2)
                        stringbuffer.append(',');
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append(d2.a);
                    stringbuffer.append(MEJB.t);
                    stringbuffer.append("=?");
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"UPDATE " + MEJB.t + Code + MEJB.t + " SET "
                    + stringbuffer.toString() + " WHERE id=?\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter.println("  public String getId(java.sql.Statement stmt) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.ResultSet rs = stmt.executeQuery(\"SELECT LAST_INSERT_ID()\");");
            printwriter.println("    if (!rs.next())");
            printwriter.println("      throw new java.sql.SQLException(\"got no from insert;\");");
            printwriter.println("    String id = rs.getString(1);");
            printwriter.println("    rs.close();");
            printwriter.println("    return id;");
            printwriter.println("  }");
            printwriter.println("}");
            printwriter.close();
        }

        void i(String s1, String s2) throws Exception {
            MEJB.n("creating DB2 DBI class " + s2 + "." + a + "Dbi");
            PrintWriter printwriter = a(s1, s2 + "/db2", "Dbi.java");
            printwriter
                    .println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
            printwriter.println("package " + s2 + ".db2;");
            printwriter.println();
            printwriter.println("public class " + a + "Dbi extends de.bb.mejb.CMPDbi");
            printwriter.println("{");
            printwriter.println("  public " + a + "Dbi()");
            printwriter.println("  {}");
            printwriter.println();
            printwriter
                    .println("  public void remove(java.sql.Connection conn, String id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.ResultSet rs = null;");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    try");
            printwriter.println("    {");
            printwriter.println("      String q;");
            for (Iterator iterator = MEJB.s.subMap(a, a + "\0").values().iterator(); iterator.hasNext(); printwriter
                    .println("      rs = null;")) {
                String s3 = (String) iterator.next();
                printwriter.println("      q = \"SELECT id FROM " + s3 + " WHERE id_" + Code + "=\" + id;");
                printwriter.println("      rs = stmt.executeQuery(q);");
                printwriter
                        .println("      if (rs.next()) throw new java.sql.SQLException(\"cant remove id=\" + id + \" from "
                                + Code + " - FOREIGN KEY exists in " + s3 + "\");");
                printwriter.println("      rs.close();");
            }

            printwriter.println("      q = \"DELETE FROM " + Code + " WHERE id=\" + id;");
            printwriter.println("      stmt.executeUpdate(q);");
            printwriter.println("    } finally");
            printwriter.println("    {");
            printwriter.println("      if (rs != null) try { rs.close(); } catch (Exception ex){}");
            printwriter.println("      stmt.close();");
            printwriter.println("    }");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.ResultSet select(java.sql.Connection conn, Object id) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.Statement stmt = conn.createStatement();");
            printwriter.println("    String q = \"SELECT * FROM " + Code + " WHERE id=\" + id;");
            printwriter.println("    return stmt.executeQuery(q);");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement insert(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            StringBuffer stringbuffer = new StringBuffer();
            StringBuffer stringbuffer1 = new StringBuffer();
            int i1 = 1;
            for (Enumeration enumeration = b.elements(); enumeration.hasMoreElements();) {
                d d1 = (d) enumeration.nextElement();
                if (i1 > 1) {
                    if (i1 > 2) {
                        stringbuffer.append(',');
                        stringbuffer1.append(',');
                    }
                    if (d1.a.equals("name"))
                        stringbuffer.append("\\\"");
                    stringbuffer.append(d1.a);
                    if (d1.a.equals("name"))
                        stringbuffer.append("\\\"");
                    stringbuffer1.append('?');
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"INSERT INTO " + Code + " ("
                    + stringbuffer.toString() + ") VALUES (" + stringbuffer1.toString() + ")\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter
                    .println("  public java.sql.PreparedStatement update(java.sql.Connection conn) throws java.sql.SQLException");
            printwriter.println("  {");
            stringbuffer = new StringBuffer();
            i1 = 1;
            for (Enumeration enumeration1 = b.elements(); enumeration1.hasMoreElements();) {
                d d2 = (d) enumeration1.nextElement();
                if (i1 > 1) {
                    if (i1 > 2)
                        stringbuffer.append(',');
                    if (d2.a.equals("name"))
                        stringbuffer.append("\\\"");
                    stringbuffer.append(d2.a);
                    if (d2.a.equals("name"))
                        stringbuffer.append("\\\"");
                    stringbuffer.append("=?");
                }
                i1++;
            }

            printwriter.println("    return conn.prepareStatement(\"UPDATE " + Code + " SET " + stringbuffer.toString()
                    + " WHERE id=?\");");
            printwriter.println("  }");
            printwriter.println();
            printwriter.println("  public String getId(java.sql.Statement stmt) throws java.sql.SQLException");
            printwriter.println("  {");
            printwriter.println("    java.sql.ResultSet rs = stmt.executeQuery(\"VALUES IDENTITY_VAL_LOCAL()\");");
            printwriter.println("    if (!rs.next())");
            printwriter.println("      throw new java.sql.SQLException(\"got no from insert;\");");
            printwriter.println("    String id = rs.getString(1);");
            printwriter.println("    rs.close();");
            printwriter.println("    return id;");
            printwriter.println("  }");
            printwriter.println("}");
            printwriter.close();
        }

        ByteRef Code;
        String a;
        Vector b;

        b(ByteRef byteref) {
            Code = byteref;
            b = new Vector();
            a = Code.toString();
            a = a.substring(0, 1).toUpperCase() + a.substring(1).toString();
        }
    }

    static String Code(String s1) {
        if (s1 == null)
            return null;
        byte abyte0[] = s1.getBytes();
        for (int i1 = 0; i1 < abyte0.length; i1++)
            if (abyte0[i1] == 46)
                abyte0[i1] = 47;

        return new String(abyte0);
    }

    static String a(ByteRef byteref) {
        String s1 = (String) c.get(byteref);
        if (s1 != null)
            return s1;
        else
            return "java.lang.Object";
    }

    MEJB(InputStream inputstream) {
        n = new ByteRef();
        o = null;
        p = null;
        m = inputstream;
    }

    void b() throws Exception {
        if (n != null)
            for (o = n.nextLine(); o == null; o = n.nextLine()) {
                n = n.update(m);
                if (n == null)
                    break;
            }

    }

    void c() throws Exception {
        q = null;
        do {
            if (o == null)
                return;
            q = o.nextWord();
            if (q != null)
                return;
            b();
        } while (true);
    }

    void d(int i1) throws Exception {
        q = null;
        do {
            if (o == null)
                return;
            q = o.nextWord(i1);
            if (q != null)
                return;
            b();
        } while (true);
    }

    void e() throws Exception {
        p = null;
        if (o != null) {
            n = o.append(" ").append(n);
            o = null;
        }
        int i1;
        while ((i1 = n.indexOf(59)) < 0) {
            n = n.update(m);
            if (n == null)
                return;
        }
        p = n.substring(0, i1).trim();
        p = p.trim(40).trim(41).trim();
        n = n.substring(i1 + 1);
    }

    void f() throws Exception {
        o = null;
        if (p == null)
            return;
        int i1 = p.indexOf(44);
        int j1 = p.indexOf(40);
        int k1 = p.indexOf(41);
        if (j1 > 0 && j1 < i1 && (k1 > i1 || k1 < 0))
            if (k1 < 0)
                i1 = p.length();
            else
                i1 = p.indexOf(44, k1);
        if (i1 < 0) {
            o = p;
            p = null;
            return;
        } else {
            o = p.substring(0, i1).trim();
            p = p.substring(i1 + 1).trim();
            return;
        }
    }

    void g() throws Exception {
        b();
        for (; o != null; b()) {
            o = o.trim();
            c();
            if (q == null || !q.equalsIgnoreCase("CREATE"))
                continue;
            c();
            if (q == null || !q.equalsIgnoreCase("TABLE"))
                continue;
            d(40);
            ByteRef byteref = q.trim();
            e();
            if (p == null)
                break;
            byteref = byteref.trim(34);
            if (k)
                System.out.println("parsing table: " + byteref);
            b b1 = new b(byteref);
            f();
            while (o != null) {
                ByteRef byteref1 = o.nextWord();
                if (byteref1 != null && !byteref1.equalsIgnoreCase("CONSTRAINT")
                        && !byteref1.equalsIgnoreCase("FOREIGN") && !byteref1.equalsIgnoreCase("INDEX")
                        && !byteref1.equalsIgnoreCase("PRIMARY") && !byteref1.equalsIgnoreCase("KEY")
                        && !byteref1.equalsIgnoreCase("UNIQUE")) {
                    byteref1 = byteref1.trim(34);
                    ByteRef byteref2 = null;
                    ByteRef byteref3 = null;
                    ByteRef byteref4 = o.nextWord();
                    if (byteref4 != null) {
                        if (byteref4.indexOf(40) > 0) {
                            ByteRef byteref5 = byteref4;
                            byteref4 = byteref5.nextWord(40);
                            o = (new ByteRef("(")).append(byteref5).append(" ").append(o);
                        }
                        if (o.charAt(0) == 40) {
                            ByteRef byteref6 = o.nextWord(41).trim(40).trim();
                            byteref2 = byteref6.nextWord(44);
                            byteref3 = byteref6.nextWord();
                        }
                        if (k)
                            System.out.println("found row: " + byteref1 + ", " + byteref4 + "(" + byteref2 + ","
                                    + byteref3 + ")");
                        b1.Code(byteref1, byteref4, byteref2, byteref3);
                    }
                }
                f();
            }
            r.put(byteref.toLowerCase(), b1);
        }

    }

    void h() {
        for (Enumeration enumeration = r.elements(); enumeration.hasMoreElements();) {
            b b1 = (b) enumeration.nextElement();
            System.out.println("table: " + b1.Code);
            d d1;
            for (Enumeration enumeration1 = b1.b.elements(); enumeration1.hasMoreElements(); System.out.println("row: "
                    + d1.a + ", " + d1.b + "(" + d1.c + "," + d1.d + ")"))
                d1 = (d) enumeration1.nextElement();

        }

    }

    void i(String s1, String s2, String s3) throws Exception {
        if (k) {
            System.out.println("creating package " + s2 + " in directory " + s1);
            System.out.println("using database: " + h);
        }
        PrintWriter printwriter = j(s1, s2, "Finder.java");
        printwriter.println("/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n");
        printwriter.println("package " + s2 + ";");
        printwriter.println();
        printwriter.println("public class Finder extends de.bb.mejb.Finder");
        printwriter.println("{");
        printwriter.println("  public Finder(java.util.Properties properties) throws javax.naming.NamingException");
        printwriter.println("  {");
        printwriter.println("    super(properties);");
        printwriter.println("  }");
        for (Enumeration enumeration = r.elements(); enumeration.hasMoreElements(); printwriter.println("  }")) {
            b b1 = (b) enumeration.nextElement();
            if (k)
                System.out.println("creating classes for table: " + b1.Code);
            b1.c(s1, s2);
            b1.d(s1, s2);
            b1.e(s1, s2, s3);
            b1.f(s1, s2, s3);
            if ("mysql".equals(h))
                b1.h(s1, s3);
            else if ("db2".equals(h))
                b1.i(s1, s3);
            else if ("mssql".equals(h)) {
                t = "\\\"";
                b1.g(s1, s3);
            } else {
                System.err.println("warning: invalid sql type specified: " + h);
            }
            printwriter.println("  /** return the home object.");
            printwriter.println("   * @throws java.rmi.RemoteException");
            printwriter.println("   * @return the home object */");
            printwriter.println("  public " + b1.a + "Home get" + b1.a + "Home() throws java.rmi.RemoteException");
            printwriter.println("  {");
            printwriter.println("    try {");
            printwriter.println("      return (" + b1.a + "Home)getHome(\"" + s3 + "." + b1.a + "Home\");");
            printwriter.println("    } catch (Exception ex) {");
            printwriter.println("      throw new java.rmi.RemoteException(ex.getMessage());");
            printwriter.println("    }");
        }

        printwriter.println("}");
        printwriter.close();
    }

    static PrintWriter j(String s1, String s2, String s3) throws Exception {
        String s4 = Code(s2);
        File file = new File(s1, s4);
        file.mkdirs();
        File file1 = new File(file, s3);
        return new PrintWriter(new FileOutputStream(file1));
    }

    private static void k(String as[]) throws Exception {
        for (int i1 = 0; i1 < as.length; i1++) {
            String s1 = as[i1];
            if (s1.charAt(0) == '-') {
                char c1 = '\0';
                if (s1.length() > 1)
                    c1 = s1.charAt(1);
                switch (c1) {
                case 100: // 'd'
                    if (++i1 != as.length) {
                        d = as[i1];
                        continue;
                    }
                    break;

                case 112: // 'p'
                    if (++i1 != as.length) {
                        e = as[i1];
                        continue;
                    }
                    break;

                case 101: // 'e'
                    if (++i1 != as.length) {
                        f = as[i1];
                        continue;
                    }
                    break;

                case 120: // 'x'
                    i = true;
                    continue;

                case 118: // 'v'
                    k = true;
                    continue;

                case 105: // 'i'
                    j = true;
                    continue;

                case 115: // 's'
                    if (++i1 != as.length) {
                        h = as[i1];
                        continue;
                    }
                    break;

                case 116: // 't'
                    if (++i1 != as.length) {
                        ByteRef byteref = new ByteRef(as[i1]);
                        ByteRef byteref1 = byteref.nextWord(61);
                        c.put(byteref1.toUpperCase(), byteref.toString());
                    }
                    break;

                case 98: // 'b'
                    if (++i1 != as.length)
                        l = as[i1];
                    break;

                default:
                    throw new Exception("invalid switch: " + s1);
                }
                if (i1 == as.length)
                    throw new Exception("missing value for switch: " + s1);
            } else {
                if (g != null)
                    throw new Exception("too many arguments: " + s1);
                g = s1;
            }
        }

        if (g == null)
            throw new Exception(
                    "usage: MEJB [-s <sqltype>=mssql] [-d <dest dir>] [-p <package>] [-e <ejb-package>] [-t <typemapping>] [-v] [-x] [-i] <filename>");
        else
            return;
    }

    private static void l(String s1) {
        if (k)
            System.out.println("INFO  " + s1);
    }

    private static void m(String s1) {
        System.out.println("ERROR " + s1);
    }

    public static void main(String args[]) {
        try {
            k(args);
            if (k) {
                System.out.println("Type mapping:");
                Object obj;
                for (Enumeration enumeration = c.keys(); enumeration.hasMoreElements(); System.out.println(obj + " -> "
                        + c.get(obj)))
                    obj = enumeration.nextElement();

            }
            if (f == null)
                f = e;
            FileInputStream fileinputstream = new FileInputStream(g);
            MEJB mejb = new MEJB(fileinputstream);
            if (!i)
                mejb.g();
            mejb.i(d, e, f);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    static void n(String s1) {
        l(s1);
    }

    static void o(String s1) {
        m(s1);
    }

    static String p() {
        return l;
    }

    static boolean q() {
        return j;
    }

    private static final boolean Code = false;
    private static final String a = "/*\r\n * This file is generated by MEJB.\r\n * DO NOT MODIFY THIS FILE!\r\n*/\r\n\r\n";
    private static final String b = "/*\r\n * This file is initially generated by MEJB.\r\n * PLACE YOUR MODIFICATIONS HERE!\r\n*/\r\n\r\n";
    private static Hashtable c;
    private static String d = ".";
    private static String e = "dummy";
    private static String f = null;
    private static String g = null;
    private static String h = null;
    private static boolean i = false;
    private static boolean j = false;
    private static boolean k;
    private static String l = "de.bb.mejb.Simple";
    InputStream m;
    ByteRef n;
    ByteRef o;
    ByteRef p;
    ByteRef q;
    static Hashtable r = new Hashtable();
    static MultiMap s = new MultiMap();
    static String t = "";

    static {
        c = new Hashtable();
        c.put(new ByteRef("BOOL"), "boolean");
        c.put(new ByteRef("INT"), "int");
        c.put(new ByteRef("INTEGER"), "int");
        c.put(new ByteRef("CHAR"), "java.lang.String");
        c.put(new ByteRef("VARCHAR"), "java.lang.String");
        c.put(new ByteRef("TEXT"), "java.lang.String");
        c.put(new ByteRef("DATE"), "java.sql.Date");
        c.put(new ByteRef("DATETIME"), "java.sql.Timestamp");
        c.put(new ByteRef("TINYINT"), "byte");
        c.put(new ByteRef("SMALLINT"), "short");
        c.put(new ByteRef("BIT"), "byte");
        c.put(new ByteRef("NUMERIC"), "double");
        c.put(new ByteRef("DECIMAL"), "java.math.BigDecimal");
    }
}
