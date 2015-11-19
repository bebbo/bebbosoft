package de.bb.jcc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

/**
 * @author bebbo
 */
class ConstantPool
{
  private final static int CONSTANT_Utf8 = 1;
//  private final static int CONSTANT_Integer = 3;
//  private final static int CONSTANT_Float = 4;
//  private final static int CONSTANT_Long = 5;
//  private final static int CONSTANT_Double = 6;
  private final static int CONSTANT_Class = 7;
  private final static int CONSTANT_String = 8;
  private final static int CONSTANT_Fieldref = 9;
  private final static int CONSTANT_Methodref = 10;
  private final static int CONSTANT_InterfaceMethodref = 11;
  private final static int CONSTANT_NameAndType = 12;

  private Vector elements = new Vector();
  private HashMap utf8 = new HashMap();
  private HashMap classes = new HashMap();
  private HashMap nameTypes = new HashMap();
  private HashMap fields = new HashMap();
  private HashMap methods = new HashMap();
  private HashMap ifacemethods = new HashMap();
  private HashMap strings = new HashMap();
  /**
   * adds an UTF8 string to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addUTF8(String name)
  {
    Integer ii = (Integer) utf8.get(name);
    if (ii == null)
    {
      elements.add(new Entry(CONSTANT_Utf8, name));
      ii = new Integer(elements.size());
      utf8.put(name, ii);
    }
    return ii.intValue();
  }

  /**
   * adds a class to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addClass(String name)
  {
    name = dot2Slash(name);
    Integer ii = (Integer) classes.get(name);
    if (ii == null)
    {
      int ci = addUTF8(name);
      elements.add(new Entry(CONSTANT_Class, ci));
      ii = new Integer(elements.size());
      classes.put(name, ii);
    }
    return ii.intValue();
  }

  /**
   * Method dot2Slash.
   * @param pName
   * @return String
   */
  static String dot2Slash(String pName)
  {
    for (int i = pName.indexOf('.'); i >= 0; i = pName.indexOf('.', i))
    {
      pName = pName.substring(0, i) + "/" + pName.substring(i + 1);
    }
    return pName;
  }

  /**
   * adds a String to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addString(String name)
  {
    Integer ii = (Integer) strings.get(name);
    if (ii == null)
    {
      int ci = addUTF8(name);
      elements.add(new Entry(CONSTANT_String, ci));
      ii = new Integer(elements.size());
      strings.put(name, ii);
    }
    return ii.intValue();
  }

  /**
   * adds a type to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addNameType(String name, String type)
  {
    type = dot2Slash(type);
    String key = name + "\\" + type;
    Integer ii = (Integer) nameTypes.get(key);
    if (ii == null)
    {
      int i1 = addUTF8(name);
      int i2 = addUTF8(type);
      elements.add(new Entry(CONSTANT_NameAndType, i1, i2));
      ii = new Integer(elements.size());
      nameTypes.put(key, ii);
    }
    return ii.intValue();
  }

  /**
   * adds a field to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addField(String cName, String name, String type)
  {
    cName = dot2Slash(cName);
    type = dot2Slash(type);
    String key = cName + "\\" + name + "\\" + type;
    Integer ii = (Integer) fields.get(key);
    if (ii == null)
    {
      int i1 = addClass(cName);
      int i2 = addNameType(name, type);
      elements.add(new Entry(CONSTANT_Fieldref, i1, i2));
      ii = new Integer(elements.size());
      fields.put(key, ii);
    }
    return ii.intValue();
  }

  /**
   * adds a method to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addMethod(String cName, String name, String type)
  {
    cName = dot2Slash(cName);
    type = dot2Slash(type);
    String key = cName + "\\" + name + "\\" + type;
    Integer ii = (Integer) methods.get(key);
    if (ii == null)
    {
      int i1 = addClass(cName);
      int i2 = addNameType(name, type);
      elements.add(new Entry(CONSTANT_Methodref, i1, i2));
      ii = new Integer(elements.size());
      methods.put(key, ii);
    }
    return ii.intValue();
  }

  /**
   * adds a method to constant pool, if not already there.
   * @param name
   * @return the index in constant pool.
   */
  int addIfaceMethod(String cName, String name, String type)
  {
    cName = dot2Slash(cName);
    type = dot2Slash(type);
    String key = cName + "\\" + name + "\\" + type;
    Integer ii = (Integer) ifacemethods.get(key);
    if (ii == null)
    {
      int i1 = addClass(cName);
      int i2 = addNameType(name, type);
      elements.add(new Entry(CONSTANT_InterfaceMethodref, i1, i2));
      ii = new Integer(elements.size());
      ifacemethods.put(key, ii);
    }
    return ii.intValue();
  }

  void print()
  {
    for (Enumeration e = elements.elements(); e.hasMoreElements();)
    {
      Entry ee = (Entry) e.nextElement();
      ee.print();
    }
  }

  static class Entry
  {
    int kind;
    String sVal;
    int iVal1;
    int iVal2;
    long lVal;
    float fVal;
    double dVal;

    Entry(int kind, String val)
    {
      this.kind = kind;
      sVal = val;
    }
    Entry(int kind, int val)
    {
      this.kind = kind;
      iVal1 = val;
    }
    Entry(int kind, int val1, int val2)
    {
      this.kind = kind;
      iVal1 = val1;
      iVal2 = val2;
    }
    Entry(int kind, long val)
    {
      this.kind = kind;
      lVal = val;
    }
    Entry(int kind, float val)
    {
      this.kind = kind;
      fVal = val;
    }
    Entry(int kind, double val)
    {
      this.kind = kind;
      dVal = val;
    }

    void print()
    {
      System.out.print(kind + ": ");
      switch (kind)
      {
        case CONSTANT_Utf8 :
          System.out.println(sVal);
          break;
        case CONSTANT_String :
        case CONSTANT_Class :
          System.out.println(iVal1);
          break;
        case CONSTANT_Fieldref :
        case CONSTANT_InterfaceMethodref :
        case CONSTANT_Methodref :
        case CONSTANT_NameAndType :
          System.out.println(iVal1 + ", " + iVal2);
          break;
        default :
          System.out.println("not specified:" + kind);
      }
    }

    void writeTo(DataOutputStream dos) throws IOException
    {
//      print();
      dos.writeByte(kind);
      switch (kind)
      {
        case CONSTANT_Utf8 :
          {
            byte b[] = sVal.getBytes();
            dos.writeShort(b.length);
            dos.write(b);
          }
          break;
        case CONSTANT_String :
        case CONSTANT_Class :
          {
            dos.writeShort(iVal1);
          }
          break;
        case CONSTANT_Fieldref :
        case CONSTANT_InterfaceMethodref :
        case CONSTANT_Methodref :
        case CONSTANT_NameAndType :
          {
            dos.writeShort(iVal1);
            dos.writeShort(iVal2);
          }
          break;
        default :
          System.out.println("not specified:" + kind);
      }
    }
  }
  /**
   * Method writeTo.
   * @param dos
   */
  void writeTo(DataOutputStream dos) throws IOException
  {
    dos.writeShort(elements.size() + 1);
//    int n = 0;
    for (Enumeration e = elements.elements(); e.hasMoreElements();)
    {
//      System.out.print((++n) + ": ");
      Entry ee = (Entry) e.nextElement();
      ee.writeTo(dos);
    }
  }
}
