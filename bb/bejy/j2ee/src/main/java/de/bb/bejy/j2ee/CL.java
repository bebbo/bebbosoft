/*
 * $Source: /export/CVS/java/de/bb/bejy/j2ee/src/main/java/de/bb/bejy/j2ee/CL.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/07/18 09:06:06 $
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

package de.bb.bejy.j2ee;

class CL extends ClassLoader {
    static CL cl = new CL();

    private CL() {
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
        }
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

    Class loadClass(String name, byte[] bits) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c != null)
            return c;

        try {
            c = findSystemClass(name);
        } catch (ClassNotFoundException e) {
        }

        if (c == null) {
            //    System.out.println("define class: " + name);
            c = defineClass(name, bits, 0, bits.length);
        }

        if (c == null)
            throw new ClassNotFoundException(name);

        resolveClass(c);

        return c;
    }
}
/*
 * $Log: CL.java,v $
 * Revision 1.1  2012/07/18 09:06:06  bebbo
 * @N new
 *
 * Revision 1.5  2004/12/01 13:36:16  bebbo
 * @B now the thread context class loader is also used to load classes
 *
 * Revision 1.4  2004/11/19 16:45:07  bebbo
 * @B fixed loadClass()
 *
 * Revision 1.3  2004/11/18 17:02:22  bebbo
 * @B fixed class load behaviour
 *
 * Revision 1.2  2004/11/18 14:50:00  bebbo
 * @N new version
 *
 * Revision 1.1  2003/03/03 10:21:01  bebbo
 * @N added to repository
 *
 */
