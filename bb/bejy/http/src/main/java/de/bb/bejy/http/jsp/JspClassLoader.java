/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspClassLoader.java,v $
 * $Revision: 1.2 $
 * $Date: 2004/12/13 15:38:10 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Hagen Raab / Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * JspClassLoader - load and resolve a class, that files stay deleteable
 *
 */

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
/*
 * $Log: JspClassLoader.java,v $
 * Revision 1.2  2004/12/13 15:38:10  bebbo
 * @B fixed class loading for JDK1.4.2_05 and newer
 *
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.6  2004/03/11 18:15:16  bebbo
 * @B getResourceAsStream no also uses the hooked loader
 * @B changed the loading order in loadClass
 *
 * Revision 1.5  2003/07/14 08:13:58  bebbo
 * @B fixed class loader usage
 *
 * Revision 1.4  2003/07/10 21:28:08  bebbo
 * @R modified generation of the class file name
 *
 * Revision 1.3  2002/11/06 09:41:41  bebbo
 * @I reorganized imports
 * @I removed unused variables
 *
 * Revision 1.2  2002/03/21 14:34:45  franke
 * @N added support for JSP files as Servlet
 * @N added support for classes loaded via ClassLoader
 * @N added support for lib/*.jar in web applications
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
