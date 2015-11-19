package de.bb.jcc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @author bebbo
 */
public class Code
{
  private ByteArrayOutputStream bos = new ByteArrayOutputStream();
  private ConstantPool cp;
  private ArrayList ex = new ArrayList();
  private int maxLocal = 1;
  private int maxStack = 0, curStack = 0;

  Code(ConstantPool cp)
  {
    this.cp = cp;
  }
  /**
   * Method c_aaload.
   */
  public void c_aaload()
  {
    --curStack;
    bos.write(0x32);
  }
  /**
   * Method c_aastore.
   */
  public void c_aastore()
  {
    curStack -= 3;
    bos.write(0x53);
  }
  /**
   * Method c_aconst.
   * @param i
   */
  public void c_aconst_null()
  {
    inc();
    bos.write(0x1);
  }
  
  public void c_aload(int n)
  {
    if (n > maxLocal)
      maxLocal = n;
    
    inc();
    switch (n)
    {
      case 0 :
      case 1 :
      case 2 :
      case 3 :
        bos.write(0x2a + n);
        break;
      default :
        if (n > 255)
          bos.write(0xc4); // wide
        bos.write(0x19); // aload
        if (n > 255)
          bos.write(n >>> 8); //high byte
        bos.write(n);
    }
  }
  /**
   * Method c_anewarray.
   * @param cName
   */
  public void c_anewarray(String cName)
  {
    --curStack;
    int n = cp.addClass(cName);
    bos.write(0xbd);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  /**
   * Method c_areturn.
   */
  public void c_areturn()
  {
    --curStack;
    bos.write(0xb0);
  }

  /**
   * Method c_astore.
   * @param i
   */
  public void c_astore(int i)
  {
    if (i > maxLocal)
      maxLocal = i;
    --curStack;
    if (i >= 0 && i <= 3)
    {
      bos.write(i + 0x75);
    } else    
    if (i < 256) {
      bos.write(0x3a);
      bos.write(i);
    } else {
      bos.write(0xc4); // wide
      bos.write(0x3a);
      bos.write(i>>>8);
      bos.write(i);
    }
  }
  /**
   * Method c_athrow.
   */
  public void c_athrow()
  {
    --curStack;
    bos.write(0xbf);
  }
  /**
   * Method i_const.
   * @param i
   */
  public void c_bipush(int i)
  {
    inc();
    if (i >= -1 && i <= 5)
    {
      bos.write(0x03 + i);
    } else
    {
      bos.write(0x10);
      bos.write(i);
    }
  }
  /**
   * Method c_checkcast.
   * @param string
   */
  public void c_checkcast(String cName)
  {
    int n = cp.addClass(cName);
    bos.write(0xc0);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  /**
   * Method c_dload.
   * @param pno
   */
  public void c_dload(int pno)
  {
    ++curStack;
    inc();
    if (pno <= 3)
      bos.write(0x26 + pno);
    else
    {
      if (pno > 255)
        bos.write(0xc4); // wide        
      bos.write(0x18);
      if (pno > 255)
        bos.write(pno>>>8);
      bos.write(pno);
    }
  }
  /**
   * Method dup.
   */
  public void c_dup()
  {
    inc();
    bos.write(0x59);
  }
  /**
   * Method c_dup_x1.
   */
  public void c_dup_x1()
  {
    inc();
    bos.write(0x5a);
  }
  /**
   * Method dup2.
   */
  public void c_dup2()
  {
    ++curStack;
    inc();
    bos.write(0x5c);
  }
  /**
   * Method c_fload.
   * @param i
   */
  public void c_fload(int i)
  {
    inc();
    if (i <= 3)
      bos.write(0x34 + i);
    else
    {
      if (i > 255)
        bos.write(0xc4); // wide        
      bos.write(0x17);
      if (i > 255)
        bos.write(i>>>8);        
      bos.write(i);
    }
  }
  /**
   * Method c_getField.
   * @param string
   * @param string1
   */
  public void c_getField(String cName, String name, String type)
  {
    inc();
    
    type = ConstantPool.dot2Slash(type);
    int l = type.indexOf('L');
    if (l >= 0) {
      int k = type.indexOf(';');
      cp.addClass(type.substring(l+1, k));
    }
    
    int n = cp.addField(cName, name, type);
    bos.write(0xb4);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  /**
   * Method c_iload.
   * @param i
   */
  public void c_iload(int i)
  {
    inc();
    if (i <= 3)
      bos.write(0x1a + i);
    else
    {
      if (i > 255)
        bos.write(0xc4); // wide        
      bos.write(0x15);
      if (i > 255)
        bos.write(i>>>8);        
      bos.write(i);
    }
  }
  /**
   * Method c_invokeinterface.
   * @param rName
   * @param string
   * @param string1
   */
  public void c_invokeinterface(String cName, String name, String type)
  {
    int count = countParams(type) + 1;
    curStack -= count;
    if (!type.endsWith("V"))
      inc();
    if (type.endsWith("J"))
      inc();
    else
    if (type.endsWith("D"))
      inc();

    int n = cp.addIfaceMethod(cName, name, type);
    bos.write(0xb9);
    bos.write(n >>> 8); //high byte
    bos.write(n);
    bos.write(count);
    bos.write(0);
  }
  /**
   * Method c_invokespecial
   * @param string
   * @param string1
   * @param string2
   */
  public void c_invokespecial(String cName, String name, String type)
  {
    curStack -= countParams(type) + 1;
    if (!type.endsWith("V"))
      inc();
    if (type.endsWith("J"))
      inc();
    else
    if (type.endsWith("D"))
      inc();

    int n = cp.addMethod(cName, name, type);
    bos.write(0xb7);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }

  /**
   * Method c_invokespecial
   * @param string
   * @param string1
   * @param string2
   */
  public void c_invokestatic(String cName, String name, String type)
  {
    curStack -= countParams(type);
    if (!type.endsWith("V"))
      inc();
    if (type.endsWith("J"))
      inc();
    else
    if (type.endsWith("D"))
      inc();

    int n = cp.addMethod(cName, name, type);
    bos.write(0xb8);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  
  /**
   * Method c_invokevirtual.
   * @param string
   * @param string1
   * @param string2
   */
  public void c_invokevirtual(String cName, String name, String type)
  {
    curStack -= countParams(type) + 1;
    if (!type.endsWith("V"))
      inc();
    if (type.endsWith("J"))
      inc();
    else
    if (type.endsWith("D"))
      inc();
      
    int n = cp.addMethod(cName, name, type);
    bos.write(0xb6);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  /**
   * Method c_ireturn.
   */
  public void c_ireturn()
  {
    --curStack;
    bos.write(0xac);
  }
  /**
   * Method c_ldc.
   * @param className
   */
  public void c_ldc(String name)
  {
    inc();
    int n = cp.addString(name);
    bos.write(0x12);
    bos.write(n);
  }
  /**
   * Method c_lload.
   * @param pno
   */
  public void c_lload(int pno)
  {
    ++curStack;
    inc();
    if (pno <= 3)
      bos.write(0x1e + pno);
    else
    {
      if (pno > 255)
        bos.write(0xc4); // wide        
      bos.write(0x16);
      if (pno > 255)
        bos.write(pno>>>8);
      bos.write(pno);
    }
  }
  /**
   * Method c_lreturn.
   */
  public void c_lreturn()
  {
    curStack -= 2;
    bos.write(0xad);
  }
  /**
   * Method c_lreturn.
   */
  public void c_dreturn()
  {
    curStack -= 2;
    bos.write(0xaf);
  }
  /**
   * Method c_new.
   * @param pName
   */
  public void c_new(String cName)
  {
    inc();
    int n = cp.addClass(cName);
    bos.write(0xbb);
    bos.write(n >>> 8); //high byte
    bos.write(n);
  }
  /**
   * Method c_pop.
   */
  public void c_pop()
  {
    --curStack;
    bos.write(0x57);
  }

  public void c_return()
  {
    bos.write(0xb1);
  }
  /**
   * Method c_swap.
   */
  public void c_swap()
  {
    bos.write(0x5f);
  }
  /**
   * Method c_tableswitch.
   * @param i
   * @param cases
   * @param object
   */
  public void c_tableswitch(int n, ArrayList cases, Code defCode) throws IOException
  {
    --curStack;
    bos.write(0xaa);
    
    // pad bytes
    int pad = 4 - (bos.size() & 3) & 3;
    for (int i = 0; i < pad; ++i)
      bos.write(0);
    
    // where first tableswitch starts
    int start = pad + 12 + 4 * cases.size() + 1;
      
    // size of code
    int sz = start;
    for (Iterator e = cases.iterator(); e.hasNext();)
    {
      Code c = (Code)e.next();
      sz += c.bos.size();  
    }
    
    bos.write(sz >>> 24);
    bos.write(sz >>> 16);
    bos.write(sz >>> 8);
    bos.write(sz);

    bos.write(n >>> 24);
    bos.write(n >>> 16);
    bos.write(n >>> 8);
    bos.write(n);
    
    n += cases.size() - 1;

    bos.write(n >>> 24);
    bos.write(n >>> 16);
    bos.write(n >>> 8);
    bos.write(n);

    // write offsets
    for (Iterator e = cases.iterator(); e.hasNext();)
    {
      Code c = (Code)e.next();

      bos.write(start >>> 24);
      bos.write(start >>> 16);
      bos.write(start >>> 8);
      bos.write(start);

      start += c.bos.size();  
    }
    // write code
    for (Iterator e = cases.iterator(); e.hasNext();)
    {
      Code c = (Code)e.next();
      bos.write(c.bos.toByteArray());
      if (c.maxStack + curStack > maxStack)
        maxStack = c.maxStack + curStack;
    }
    
    if (defCode != null)
    {
      bos.write(defCode.bos.toByteArray());
    }    
  }

  int countParams(String s)
  {
    int n = 0;
    int idx = s.indexOf('(');
    if (idx < 0)
      return n;
    s = s.substring(idx + 1);
    while (s.length() > 0)
    {
      int ch = s.charAt(0);
      s = s.substring(1);
      if (ch == ')')
        return n;
      if (ch == '[')
        continue;
      ++n;
      if ("SBCFIZ".indexOf(ch) >= 0)
        continue;
      if ("DJ".indexOf(ch) >= 0)
      {
        ++n;
        continue;
      }
      idx = s.indexOf(';');
      s = s.substring(idx + 1);
    }
    return n;
  }

  void inc()
  {
    ++curStack;
    if (curStack > maxStack)
      maxStack = curStack;
  }
  /**
   * Method setParams.
   * @param type
   */
  void setParams(String type)
  {
    maxLocal += countParams(type);
  }
  /**
   * Method writeTo.
   * @param dos
   */
  void writeTo(DataOutputStream dos) throws IOException
  {
    byte code[] = bos.toByteArray();
    dos.writeShort(cp.addUTF8("Code"));
    dos.writeInt(8 + code.length + 2 + 8 * ex.size() + 2);
    dos.writeShort(maxStack);
    dos.writeShort(maxLocal);
    dos.writeInt(code.length);
    dos.write(code);
    dos.writeShort(ex.size());
    for (Iterator e = ex.iterator(); e.hasNext();)
    {
      Ex exc = (Ex) e.next();
      exc.writeTo(dos);
    }
    dos.writeShort(0); // no attributes
  }
}
