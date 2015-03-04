/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/BeanHelper.java,v $
 * $Revision: 1.1 $
 * $Date: 2004/04/16 13:46:09 $
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

  Created on 29.10.2003

 *****************************************************************************/

package de.bb.bejy.http.jsp;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author bebbo
 */
public class BeanHelper
{
  private final static String NAMES [][] = 
  {
    {"byte", "java.lang.Byte"},
    {"short", "java.lang.Short"},
    {"int", "java.lang.Integer"},
    {"long", "java.lang.Long"},
    {"float", "java.lang.Float"},
    {"double", "java.lang.Double"},
    {"char", "java.lang.Character"},
    {"boolean", "java.lang.Boolean"},
  };
  
  /**
   * Apply all parameters from request to bean
   */
  public static void _jsp_copyParameters(HttpServletRequest req, Object bean) throws Exception
  {
    Class clazz = bean.getClass();
    java.lang.reflect.Method [] mts = clazz.getMethods();
    for (int i = 0; i < mts.length; ++i)
    {
      java.lang.reflect.Method m = mts[i];
      if (!m.getName().startsWith("set"))
        continue;
      Class params[] = m.getParameterTypes();
      if (params.length != 1)
        continue;
      Class p = params[0];
      String n = m.getName().substring(3).toLowerCase();
      Object v = req.getParameter(n);
      if (v == null)
        continue;
      // got method m and parameter p to be set from request with String v
      try {
        Class vc = v.getClass();
        if (!vc.getName().equals(p.getName()))
        {
          // do conversion
          if (p.isPrimitive())
          {
            String cn = null, nm = p.getName();
            for (int j = 0; j < NAMES.length; ++j)
            {
              if (nm.equals(NAMES[j][0])) {
                cn = NAMES[j][1];
                break;
              }
            }
            Class pc = Class.forName(cn);
            java.lang.reflect.Method vo = pc.getMethod("valueOf", new Class []{vc});
            v = vo.invoke(null, new Object[] { v });            
          } else {
            java.lang.reflect.Constructor ct = p.getConstructor(new Class []{vc});
            v = ct.newInstance(new Object[]{v});
          }
        }
        m.invoke(bean, new Object[] { v });
      } catch (Exception e)
      {
        System.out.println(e.getMessage());
        System.out.println("" + v.getClass());
        System.out.println("" + p);
      }
    }    
  }
}

/******************************************************************************
 * $Log: BeanHelper.java,v $
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.1  2003/11/16 09:30:29  bebbo
 * @B various changes
 *
 *****************************************************************************/