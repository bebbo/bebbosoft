package de.bb.jcc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Used to create a class table
 * @author bebbo
 */
public class ClassDefinition
{
  ConstantPool cp = new ConstantPool();
  private int access = 0x0001; // PUBLIC
  private int thisClass;
  private int superClass;
  private Vector interfaces = new Vector();
  private Vector methods = new Vector();
  private Vector fields = new Vector();
  private String className;

  public ClassDefinition(String className)
  {
    this(className, "java.lang.Object");
  }

  public ClassDefinition(String className, String parentClassName)
  {
    className = ConstantPool.dot2Slash(className);
    this.className = className;
    thisClass = cp.addClass(className);
    superClass = cp.addClass(parentClassName);
  }

  public void write(OutputStream os) throws IOException
  {
    cp.addUTF8("Code");
    
    DataOutputStream dos = new DataOutputStream(os);
    dos.writeInt(0xCAFEBABE);
    dos.writeShort(0x03);
    dos.writeShort(0x2d);

    cp.writeTo(dos);

    dos.writeShort(access);
    dos.writeShort(thisClass);
    dos.writeShort(superClass);

    dos.writeShort(interfaces.size());
    for (Enumeration e = interfaces.elements(); e.hasMoreElements();)
    {
      Integer ii = (Integer) e.nextElement();
      dos.writeShort(ii.intValue());
    }

    dos.writeShort(fields.size());
    for (Enumeration e = fields.elements(); e.hasMoreElements();)
    {
      Field f = (Field) e.nextElement();
      f.writeTo(dos);
    }

    dos.writeShort(methods.size());
    for (Enumeration e = methods.elements(); e.hasMoreElements();)
    {
      Method m = (Method) e.nextElement();
      m.writeTo(dos);
    }

    //if (innerClasses == null)
    {
      dos.writeShort(0);
    }

  }

  /**
   * Method addInterface.
   * @param string
   */
  public void addInterface(String name)
  {
    interfaces.add(new Integer(cp.addClass(name)));
  }

  public int addMethodRef(String cName, String name, String type)
  {
    return cp.addMethod(cName, name, type);
  }

  public void defineMethod(int acc, String name, String type, Code code)
  {
    type = ConstantPool.dot2Slash(type);
    int i = cp.addUTF8(name);
    int j = cp.addUTF8(type);
    code.setParams(type);
    methods.add(new Method(acc, i, j, code));
  }

  public Method defineMethod(int acc, String name, String type, Code code, String [] exs)
  {
    type = ConstantPool.dot2Slash(type);
    int i = cp.addUTF8(name);
    int j = cp.addUTF8(type);
    code.setParams(type);
    Method m = new Method(acc, i, j, code);
    m.setExceptions(exs);
    methods.add(m);
    return m;
  }

  static class Field
  {
    /**
     * Method writeTo.
     * @param dos
     */
    public void writeTo(DataOutputStream dos)
    {
    }
  }

  class Method
  {
    private int maccess;
    private int iName;
    private int iSig;
    private Code code;
    private String[] exs;
    /**
     * Constructor Method.
     * @param i
     * @param code
     */
    public Method(int m, int i, int j, Code code)
    {
      maccess = m;
      iName = i;
      iSig = j;
      this.code = code;
    }
    /**
     * Method setExceptions.
     * @param exs
     */
    public void setExceptions(String[] exs)
    {
      this.exs = exs;
      cp.addUTF8("Exceptions");
      for (int i = 0; i < exs.length; ++i)
      {
        cp.addClass(exs[i]);  
      }
    }
    /**
     * Method writeTo.
     * @param dos
     */
    void writeTo(DataOutputStream dos) throws IOException
    {
      dos.writeShort(maccess); // ACC_PUBLIC
      dos.writeShort(iName);
      dos.writeShort(iSig);
      
      int noAttr = exs == null ? 1 : 2;
      dos.writeShort(noAttr); // 1 attribute
      code.writeTo(dos);
      if (exs != null) {
        dos.writeShort(cp.addUTF8("Exceptions"));
        dos.writeInt(2 + 2 * exs.length);
        dos.writeShort(exs.length);
        for (int i = 0; i < exs.length; ++i)
        {
          dos.writeShort(cp.addClass(exs[i]));  
        }
      }
    }
    public void setMaccess(int maccess)
    {
      this.maccess = maccess;
    }
  }
  /**
   * Method createCode.
   * @return Code
   */
  public Code createCode()
  {
    return new Code(cp);
  }
}
