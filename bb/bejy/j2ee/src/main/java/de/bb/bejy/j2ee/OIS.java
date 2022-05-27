package de.bb.bejy.j2ee;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * @author bebbo
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
class OIS extends ObjectInputStream
{
  OIS(InputStream is) throws IOException
  {
    super(is);
  }
  
  protected Class resolveClass(ObjectStreamClass desc)
    throws IOException, ClassNotFoundException
  {
    try
    {
      String name = desc.getName();
      return CL.cl.loadClass(name);
    } catch (ClassNotFoundException ex)
    {
    }
    return super.resolveClass(desc);
  }
}
