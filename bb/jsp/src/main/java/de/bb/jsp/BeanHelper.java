/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/

package de.bb.jsp;

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
