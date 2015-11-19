/* 
 * Created on 12.11.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author sfranke
 */
class Marshal
{

  /**
   * Method readObject.
   * @param is
   * @return Object
   * @throws Exception
   */
  static Object readObject(ObjectInputStream is)
    throws Exception
  {
    byte pt = is.readByte();
    switch (pt)
    {
      case 0 :
        return null;
      case 1 :
        return new Boolean(is.readBoolean());
      case 2 :
        return new Byte(is.readByte());
      case 3 :
        return new Character(is.readChar());
      case 4 :
        return new Short(is.readShort());
      case 5 :
        return new Integer(is.readInt());
      case 6 :
        return new Long(is.readLong());
      case 7 :
        return new Float(is.readFloat());
      case 8 :
        return new Double(is.readDouble());
      case 9 :
        return is.readUTF();
      default :
        return is.readObject();
    }
  }

  /**
   * Method readObject.
   * @param o
   * @param os
   * @throws IOException
  */
  static void writeObject(Object o, ObjectOutputStream os)
    throws IOException
  {
    if (o == null)
    {
      os.write(0);
      return;
    }
  
    Class c = o.getClass();
    if (c == Boolean.class)
    {
      os.write(1);
      os.writeBoolean(((Boolean) o).booleanValue());
      return;
    }
    if (c == Byte.class)
    {
      os.write(2);
      os.writeByte(((Byte) o).byteValue());
      return;
    }
    if (c == Character.class)
    {
      os.write(3);
      os.writeChar(((Character) o).charValue());
      return;
    }
    if (c == Short.class)
    {
      os.write(4);
      os.writeShort(((Short) o).shortValue());
      return;
    }
    if (c == Integer.class)
    {
      os.write(5);
      os.writeInt(((Integer) o).intValue());
      return;
    }
    if (c == Long.class)
    {
      os.write(6);
      os.writeLong(((Long) o).longValue());
      return;
    }
    if (c == Float.class)
    {
      os.write(7);
      os.writeFloat(((Float) o).floatValue());
      return;
    }
    if (c == Double.class)
    {
      os.write(8);
      os.writeDouble(((Double) o).doubleValue());
      return;
    }
    if (c == String.class)
    {
      os.write(9);
      os.writeUTF((String) o);
      return;
    }
  
    os.writeByte(10);
    os.writeObject(o);
  }

}
