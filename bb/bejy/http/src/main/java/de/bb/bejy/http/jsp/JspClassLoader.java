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

package de.bb.bejy.http.jsp;

import java.io.*;

class JspClassLoader extends ClassLoader {
  private File path;
  private ClassLoader hookLoader;
  JspClassLoader (File path, ClassLoader hookLoader)
  {
    this.path = path;
    this.hookLoader = hookLoader;
    
  }
  protected Class loadClass (String name, boolean resolve) throws ClassNotFoundException
  {
    String cName = replaceAll(name, '/', '.');
    Class c = findLoadedClass (cName);
    if (c != null)
      return c;

    try {
      c = findSystemClass (cName);
    } catch (ClassNotFoundException e) {}

    if (c == null && hookLoader != null)
    {
      try {
        c = hookLoader.loadClass(cName);
      } catch (Throwable t)
      {
      }
    }

    if (c == null)
    {
      byte[] bits = loadBytes (name);
      if (bits != null) 
        c = defineClass (cName, bits, 0, bits.length);
    }
    
    if (c == null)
      throw new ClassNotFoundException (name);

    if (resolve)
      resolveClass (c);

    return c;
  }
  
  public InputStream getResourceAsStream(String rsrcName)
  {
    File file = new File (path, rsrcName);
    if (file.exists ())
    {
      try {
        return new FileInputStream (file);
      } catch (Exception e)
      {}
    }
    return hookLoader.getResourceAsStream(rsrcName);
  }

  private byte[] loadBytes (String className)
  {
    try
    {
      for (int i = className.indexOf('.'); i >= 0; i = className.indexOf('.'))
      {
        className = className.substring(0, i) + "/" + className.substring(i + 1);
      }
      className += ".class";

      InputStream in = getResourceAsStream(className);
      if (in == null)
        return null;
        
      byte[] b = new byte [ in.available()];
      if (b == null)
        return null;
        
      int n, pos = 0;
      while (pos < b.length && (n = in.read (b, pos, b.length - pos)) != - 1)
        pos += n;
      in.close();
      return b;
    } catch (IOException e) {}
    return null;
  }
  private static String replaceAll(String string, char c, char d)
  {
    char b[] = new char[string.length()];
    string.getChars(0, b.length, b, 0);
    for (int i = 0; i < b.length; ++i)
    {
      if (b[i] == c)
      {
        b[i] = d;
      }
    }
    return new String(b);
  }

}
