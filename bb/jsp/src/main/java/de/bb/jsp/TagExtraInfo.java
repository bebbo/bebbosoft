/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/TagExtraInfo.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:45:09 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

  Created on 23.10.2003

 *****************************************************************************/

package de.bb.jsp;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import de.bb.util.XmlFile;


class TagExtraInfo
{
  private final static Class NOCLASS [] = {};
  private final static Object NOPARAM [] = {};
  
  private TagInfo ti;

  private Vector sections;

  private XmlFile xf;

  private Object oo;

  /**
   * @param oo
   */
  TagExtraInfo(Object oo)
  {
    this.oo = oo;
  }

  TagExtraInfo(XmlFile xf, Vector sections)
  {
    this.xf = xf;
    this.sections = sections;
  }

  /* (non-Javadoc)
   * @see javax.servlet.jsp.tagext.TagExtraInfo#getVariableInfo(javax.servlet.jsp.tagext.TagData)
   */
  VariableInfo[] getVariableInfo(TagData tagData)
  {
    if (oo != null)
      return fromOO(tagData);
   
    Vector v = new Vector();
    int i = 0;
    for (Enumeration e = sections.elements(); e.hasMoreElements(); ++i)
    {
      String var = (String) e.nextElement();
      String nameGiven = xf.getContent(var + "name-given");
      String nameFromAttr = xf.getContent(var + "name-from-attribute");

      if (nameGiven == null)
      {
        nameGiven = tagData.getAttributeString(nameFromAttr);
      }
      if (nameGiven == null)
        continue;

      String varClazz = xf.getContent(var + "variable-class");
      if (varClazz == null)
        varClazz = "java.lang.String";
      String sDecl = xf.getContent(var + "declare");
      boolean declare = (sDecl == null || sDecl.length() == 0 || "true".equals(sDecl));
      String sScope = xf.getContent(var + "scope");
      int scope = VariableInfo.NESTED;
      if ("AT_BEGIN".equals(sScope))
        scope = VariableInfo.AT_BEGIN;
      else if ("AT_END".equals(sScope))
        scope = VariableInfo.AT_END;
      VariableInfo vi = new VariableInfo(nameGiven, varClazz, declare, scope);
      v.add(vi);
    }
    return (VariableInfo[]) v.toArray(new VariableInfo[0]);
  }

  /**
   * @param tagData
   * @return
   */
  private VariableInfo[] fromOO(TagData tagData)
  {
    try {
      Class clazz = oo.getClass();
      Method mts[] = clazz.getMethods();
      for (int i = 0; i < mts.length; ++i)
      {
        Method m = mts[i];
        if ("getVariableInfo".equals(m.getName()))
        {
          Class pclazzes [] = m.getParameterTypes();
          if (pclazzes.length == 1)
          {
            Constructor ct = pclazzes[0].getConstructor(new Class[]{Hashtable.class});
            Object p = ct.newInstance(new Object[]{tagData.getHashtable()});
            Object r = m.invoke(oo, new Object[]{p});
            int len = r == null ? 0 : Array.getLength(r);
            VariableInfo v[] = new VariableInfo[len];
            for (int j = 0; j < len; ++j)
            {
              Object vo = Array.get(r, j);
              Class vc = vo.getClass();
              String name = (String)vc.getMethod("getVarName", NOCLASS).invoke(vo, NOPARAM);
              String cname = (String)vc.getMethod("getClassName", NOCLASS).invoke(vo, NOPARAM);
              Boolean decl = (Boolean)vc.getMethod("getDeclare", NOCLASS).invoke(vo, NOPARAM);
              Integer scope = (Integer)vc.getMethod("getScope", NOCLASS).invoke(vo, NOPARAM);
              v[j] = new VariableInfo(name, cname, decl.booleanValue(), scope.intValue());
            }
            return v;
          }
        }
      }
    } catch (Exception ex){      
    }
    return new VariableInfo[0];
  }

  /**
   * @param ti
   */
  void setTagInfo(TagInfo ti)
  {
    this.ti = ti;
  }
}
/******************************************************************************
 * $Log: TagExtraInfo.java,v $
 * Revision 1.1  2004/04/16 13:45:09  bebbo
 * @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 *
 * Revision 1.2  2003/12/18 10:44:20  bebbo
 * @B declare defaulted to false - now to true
 *
 * Revision 1.1  2003/10/23 20:35:34  bebbo
 * @R moved JspCC inner classes into separate files
 * @R jspCC is now reusable
 * @N added more caching to enhance reusability
 *
 *****************************************************************************/