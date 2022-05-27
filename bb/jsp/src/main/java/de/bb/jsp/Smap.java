/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/Smap.java,v $
 * $Revision: 1.3 $
 * $Date: 2004/04/07 16:35:45 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.jsp;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * stolen from my bb_mug tool :).
 * @author bebbo
 */
public class Smap
{
  private final static int CONSTANT_Utf8 = 1;
  private final static int CONSTANT_Integer = 3;
  private final static int CONSTANT_Float = 4;
  private final static int CONSTANT_Long = 5;
  private final static int CONSTANT_Double = 6;
  private final static int CONSTANT_Class = 7;
  private final static int CONSTANT_String = 8;
  private final static int CONSTANT_Fieldref = 9;
  private final static int CONSTANT_Methodref = 10;
  private final static int CONSTANT_InterfaceMethodref = 11;
  private final static int CONSTANT_NameAndType = 12;


  private short minor;
  private short major;

  /** 
   * These 3 arrays hold the constant pool. 
   * cTags = tag
   * cRefs = 1 or 2 refs into constant pool
   * cData = raw data for Integer, Long, Float, Double or UTF8
   */
  private int cTags[];
  private short cRefs[][];
  private byte cData[][];

  private short access;

  private short cClass;

  private short cSuper;

  private short[] cIfaces;

  /** 
   * Field information held in short[4].
   * 0: access
   * 1: name
   * 2: typee
   * 3: 0 or index to constant
   * all other attributes are discarded
   */
  private short[][] fields;
  private byte [][][] fieldAttrs;

  /**
   * Method information held in short[3].
   * 0: access
   * 1: name
   * 2: typee
   * 
   * And code and exception attribute
   */
  private short[][] methods;
  private byte [][][] methodAttrs;


  private byte [][] classAttr;


  /**
   * Reads the class file into internal structures.
   * Also for each UTF8 the references are stored, to detect
   * when an UTF8 String must be copied.
   * 
   * 
   * 
   * @param fis fileInputStream
   */
  public Smap(InputStream fis) throws Exception
  {

    DataInputStream dis = new DataInputStream(fis);
    int magic = dis.readInt();
    if (magic != 0xCAFEBABE)
    {
      throw new Exception("no valid class file");
    }

    minor = dis.readShort();
    major = dis.readShort();

    int cCount1 = dis.readShort();

    // add 1 since we wanna add an UTF constant
    cTags = new int[cCount1 + 1];
    cRefs = new short[cCount1 + 1][2];
    cData = new byte[cCount1 + 1][];
    for (int i = 1; i < cCount1; ++i)
    {
      if (readConstant(i, dis))
      {
        ++i;
      }
    }
    
    access = dis.readShort();
    cClass = dis.readShort();
    //    writeln("Class: " + getClassName(cClass));
    cSuper = dis.readShort();
    //    writeln("Superclass: " + getClassName(cSuper));

    short iCount = dis.readShort();
    cIfaces = new short[iCount];
    for (int i = 0; i < iCount; ++i)
    {
      cIfaces[i] = dis.readShort();
    }

    short fCount = dis.readShort();
    fields = new short[fCount][4];
    fieldAttrs = new byte[fCount][][];
    for (int i = 0; i < fCount; ++i)
    {
      readFM(fields, fieldAttrs, i, dis);
    }

    short mCount = dis.readShort();
    methods = new short[mCount][3];
    methodAttrs = new byte[mCount][][];
    for (int i = 0; i < mCount; ++i)
    {
      readFM(methods, methodAttrs, i, dis);
    }

    int aCount = dis.readShort(); 
    // we add the SMAP attribute
    classAttr = new byte[aCount + 1][];
    for (int i = 0; i < aCount; ++i)
    {
      classAttr[i] = readAttribute(dis);
    }
  }

  /**
   * Method write.
   * @param outputStream is filled with class file data
   */
  public void write(OutputStream outputStream) throws IOException
  {
    DataOutputStream dos = new DataOutputStream(outputStream);
    dos.writeInt(0xCAFEBABE);
    dos.writeShort(minor);
    dos.writeShort(major);

    dos.writeShort(cTags.length);
    for (int i = 1; i < cTags.length; ++i)
    {
      writeConstant(i, dos);
    }

    dos.writeShort(access);
    dos.writeShort(cClass);
    dos.writeShort(cSuper);

    dos.writeShort(cIfaces.length);
    for (int i = 0; i < cIfaces.length; ++i)
    {
      dos.writeShort(cIfaces[i]);
    }

    dos.writeShort(fields.length);
    for (int i = 0; i < fields.length; ++i)
    {
      writeFM(fields, fieldAttrs, i, dos);
    }

    dos.writeShort(methods.length);
    for (int i = 0; i < methods.length; ++i)
    {
      writeFM(methods, methodAttrs, i, dos);
    }

    dos.writeShort(classAttr.length);
    for (int i = 0; i < classAttr.length; ++i)
    {
      dos.write(classAttr[i]);
    }
  }

  /**
   * write a method or a field with attributes.
   * @param fm the field / method array
   * @param fma the attribute array
   * @param index index of field / method
   * @param dos the stream to write to
   */
  private void writeFM(short fm[][], byte fma[][][], int index, DataOutputStream dos) throws IOException
  {
    short m[] = fm[index];
    dos.writeShort(m[0]);
    dos.writeShort(m[1]);
    dos.writeShort(m[2]);

    int numAttr = fma[index].length;
    dos.writeShort(numAttr);
    for (int i = 0; i < numAttr; ++i)
    {
      dos.write(fma[index][i]);
    }
  }

  /**
   * Method writeConstant.
   * @param i
   * @param dos
   */
  private void writeConstant(int i, DataOutputStream dos) throws IOException
  {
    int tag = cTags[i];
    if (tag == 0)
      return;

    dos.writeByte(tag);
    short[] s = cRefs[i];
    if (s != null)
    {
      short ss = s[0];
      if (ss != 0)
        dos.writeShort(ss);
      ss = s[1];
      if (ss != 0)
        dos.writeShort(ss);
    }
    byte b[] = cData[i];
    if (b != null)
      dos.write(b);
  }

  /**
   * Method readAttribute.
   * @param ds
   */
  private byte[] readAttribute(DataInputStream dis) throws IOException
  {
    short idx = dis.readShort();
    int len = dis.readInt();
    byte b[] = new byte[len];
    dis.read(b);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dbos = new DataOutputStream(bos);
    dbos.writeShort(idx);
    dbos.writeInt(len);
    dbos.write(b);
    dbos.flush();
    return bos.toByteArray();
  }

  /**
   * read a method or a field with attributes.
   * @param fm the field / method array
   * @param fma the attribute array
   * @param index index of field / method
   * @param dis the stream to read from
   */
  private void readFM(short [][] fm, byte fma[][][], int index, DataInputStream dis) throws Exception
  {
    short m[] = fm[index];
    m[0] = dis.readShort(); // access
    m[1] = dis.readShort(); // name
    m[2] = dis.readShort(); // mangled typee

    // read attributes
    int len = dis.readUnsignedShort();
    fma[index] = new byte[len][];
    for (int i = 0; i < len; ++i)
    {
      fma[index][i] = readAttribute(dis);
    }
  }

  /**
   * Method readConstant.
   * @param i
   * @param dis
   */
  private boolean readConstant(int i, DataInputStream dis) throws Exception
  {
    boolean ret = false;
    int len;
    byte b[] = null;
    int tag = dis.readUnsignedByte();
    cTags[i] = tag;
    switch (tag)
    {
      case CONSTANT_Utf8 :
        len = dis.readUnsignedShort();
        b = new byte[len + 2];
        dis.readFully(b, 2, len);
        b[0] = (byte) (len >> 8);
        b[1] = (byte) len;
//        String cs = new String(b, 2, len);
        //writeln("" + i + ": " + cs);
        break;
      case CONSTANT_Integer :
      case CONSTANT_Float :
        b = new byte[4];
        dis.readFully(b);
        /*
        int 
        vi = (b[0] & 0xff);
        vi = (b[1] & 0xff) | (vi << 8);
        vi = (b[2] & 0xff) | (vi << 8);
        vi = (b[3] & 0xff) | (vi << 8);
        writeln("" + i + ": I: " + vi);
        */
        break;
      case CONSTANT_Long :
      case CONSTANT_Double :
        b = new byte[8];
        dis.readFully(b);
        ret = true;
        /*
        long 
        vl = (b[0] & 0xff);
        vl = (b[1] & 0xff) | (vl << 8);
        vl = (b[2] & 0xff) | (vl << 8);
        vl = (b[3] & 0xff) | (vl << 8);
        vl = (b[4] & 0xff) | (vl << 8);
        vl = (b[5] & 0xff) | (vl << 8);
        vl = (b[6] & 0xff) | (vl << 8);
        vl = (b[7] & 0xff) | (vl << 8);
        writeln("" + i + ": L: " + vl);
        */
        break;
      case CONSTANT_Class :
        cRefs[i][0] = dis.readShort();
        //writeln("" + i + ": C " + getString(cRefs[i][0]) + " -> " + cRefs[i][0]);
        break;
      case CONSTANT_String :
        cRefs[i][0] = dis.readShort();
        //writeln("" + i + ": S " + getString(cRefs[i][0]) + " -> " + cRefs[i][0]);
        break;
      case CONSTANT_Fieldref :
      case CONSTANT_InterfaceMethodref :
      case CONSTANT_Methodref :
      case CONSTANT_NameAndType :
        cRefs[i][0] = dis.readShort();
        cRefs[i][1] = dis.readShort();
        //writeln("" + i + ": " + tag + " -> " + cRefs[i][0] + "/" + cRefs[i][1]);
        break;
      default :
        throw new Exception(
          "unknown constant: "
            + tag
            + " at "
            + i
            + " previous tag="
            + cTags[i
            - 1]);
    }
    cData[i] = b;
    return ret;
  }

  /**
   * Add the SMAP information to the class file.
   * @author bebbo
   */
  public void addSmap(String smap) throws IOException
  {
    int cindex = cTags.length - 1;
    addSde(cindex);
    
    byte b[] = smap.getBytes();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dbos = new DataOutputStream(bos);
    dbos.writeShort(cindex);
    dbos.writeInt(b.length);
    dbos.write(b);
    dbos.flush();
    classAttr[classAttr.length - 1] = bos.toByteArray(); 
  }

  /**
   * 
   */
  private void addSde(int cindex)
  {
    // create the missing attribute
    String sde = "SourceDebugExtension";
    int len = sde.length();    
    byte b [] = new byte[len + 2];
    System.arraycopy(sde.getBytes(), 0, b, 2, len);
    b[0] = (byte) (len >> 8);
    b[1] = (byte) len;

    cTags[cindex] = CONSTANT_Utf8;
    cData[cindex] = b;    
  }

/**/
}

/******************************************************************************
 * $Log: Smap.java,v $
 * Revision 1.3  2004/04/07 16:35:45  bebbo
 * @O optimizations - removed unused variables/methods
 *
 * Revision 1.2  2004/01/03 18:53:41  bebbo
 * @R class is now public
 * @R addSmap() is now public
 * @R write() is now public
 *
 * Revision 1.1  2003/07/14 11:29:37  bebbo
 * @N JspServlet now adds SMAP information to generated class files
 *
 */