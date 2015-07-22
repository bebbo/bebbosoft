/******************************************************************************
 * $Source: /export/CVS/java/de/bb/tools/uses/src/main/java/de/bb/tools/uses/UsesClass.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/08/11 20:00:28 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * windows like ini file, with xml like extensions
 * next step will be xml files
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

  (c) 2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.tools.uses;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * @author bebbo
 */
class UsesClass {
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

    //Declared public; may be accessed from outside its package. 
    private final static int ACC_PUBLIC = 0x0001;
    // Declared private; accessible only within the defining class.  
    private final static int ACC_PRIVATE = 0x0002;
    // Declared protected; may be accessed within subclasses.  
    private final static int ACC_PROTECTED = 0x0004;
    /*   
      //Declared final; no subclasses allowed. 
    	private final static int ACC_FINAL = 0x0010;
      //Treat superclass methods specially when invoked by the invokespecial instruction. 
    	private final static int ACC_SUPER = 0x0020;
      //Is an interface, not a class. 
    	private final static int ACC_INTERFACE = 0x0200;
      //Declared abstract; may not be instantiated. 
    	private final static int ACC_ABSTRACT = 0x0400;
    */
    /*
      private final static int csz [] = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 3, 2, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 
        3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0, 0, 1, 1, 1, 1, 
        1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 1, 3, 2, 3, 1, 1, 
        3, 3, 1, 1, 1, 4, 3, 3, 5, 5, 1, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1  
      };
     */

    private short minor;

    private short major;

    /**
     * These 3 arrays hold the constant pool. cTags = tag cRefs = 1 or 2 refs into constant pool cData = raw data for
     * Integer, Long, Float, Double or UTF8
     */
    private int cTags[];
    private short cRefs[][];
    private byte cData[][];

    private HashMap<String, Integer> utf8Map = new HashMap<String, Integer>();
    private short access;
    private short cClass;
    private short cSuper;
    private short[][] fields;
    private short[][] methods;

    /**
     * Reads the class file into internal structures. Also for each UTF8 the references are stored, to detect when an
     * UTF8 String must be copied.
     * 
     * 
     * 
     * @param fis
     *            fileInputStream
     */
    UsesClass(InputStream fis) throws Exception {
        DataInputStream dis = new DataInputStream(fis);
        int magic = dis.readInt();
        if (magic != 0xCAFEBABE) {
            throw new Exception("no valid class file");
        }

        minor = dis.readShort();
        major = dis.readShort();

        short cCount1 = dis.readShort();

        //    writeln("initial ccount: " + cCount1);

        cTags = new int[cCount1];
        cRefs = new short[cCount1][2];
        cData = new byte[cCount1][];
        for (int i = 1; i < cCount1; ++i) {
            if (readConstant(i, dis)) {
                ++i;
            }
        }

        access = dis.readShort();
        cClass = dis.readShort();
        //System.out.println("Class: " + getClassName(cClass));
        cSuper = dis.readShort();
        //System.out.println("Superclass: " + getClassName(cSuper));

        short iCount = dis.readShort();
        //    cIfaces = new short[iCount];
        for (int i = 0; i < iCount; ++i) {
            //cIfaces[i] = 
            dis.readShort();
            //    writeln("Interface: " + getClassName(cIfaces[i]));
        }

        short fCount = dis.readShort();
        fields = new short[fCount][4];
        for (int i = 0; i < fCount; ++i) {
            try {
                readField(i, dis);
            } catch (IOException ex) {

                throw ex;
            }
            // dumpField(i);
        }

        short mCount = dis.readShort();
        methods = new short[mCount][3];
        //    codes = new byte[mCount][];
        //    exceptions = new byte[mCount][];
        for (int i = 0; i < mCount; ++i) {
            readMethod(i, dis);
            // dumpMethod(i);
        }

    }

    /**
     * Method readMethod.
     * 
     * @param index
     * @param dis
     * @throws IOException
     */
    private void readMethod(int index, DataInputStream dis) throws IOException {
        short m[] = methods[index];
        m[0] = dis.readShort(); // access
        m[1] = dis.readShort(); // name
        m[2] = dis.readShort(); // mangled typee

        // read attributes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        int len = dis.readUnsignedShort();
        for (int i = 0; i < len; ++i) {
            dos.flush();
            bos.reset();
            short idx = dis.readShort();
            int aLen = dis.readInt();

            dos.writeShort(idx);
            dos.writeInt(aLen);

            String aName = getString(idx);
            // read constant values
            if ("Code".equals(aName)) {
                // copy stack / local size
                int cLen = dis.readInt();
                dos.writeInt(cLen);
                // copy code len
                cLen = dis.readInt();
                dos.writeInt(cLen);
                // copy code
                byte b[] = new byte[cLen];
                dis.readFully(b);
                dos.write(b);
                // copy Exceptions
                cLen = dis.readUnsignedShort();
                dos.writeShort(cLen);
                b = new byte[8];
                for (int j = 0; j < cLen; ++j) {
                    dis.readFully(b);
                    dos.write(b);
                }
                //int numAttr = 
                dis.readShort();
                // skip all attributes
                dos.writeShort(0);
                dos.flush();
                b = bos.toByteArray();
                int newLen = b.length - 6;
                dis.skip(aLen - newLen);
                // and poke the new length
                b[2] = (byte) (newLen >>> 24);
                b[3] = (byte) (newLen >>> 16);
                b[4] = (byte) (newLen >>> 8);
                b[5] = (byte) (newLen);

                //        codes[index] = b;

                continue;
            } else if ("Exceptions".equals(aName)) {
                short numEx = dis.readShort();
                dos.writeShort(numEx);
                for (int j = 0; j < numEx; ++j) {
                    short ex = dis.readShort();
                    dos.writeShort(ex);
                }
                dos.flush();
                //        exceptions[index] = bos.toByteArray();
                continue;
            }

            // ignore others
            dis.skip(aLen);
        }
    }

    /**
     * Method readField.
     * 
     * @param i
     * @param dis
     * @throws IOException
     */
    private void readField(int i, DataInputStream dis) throws IOException {
        short f[] = fields[i];
        f[0] = dis.readShort(); // access
        f[1] = dis.readShort(); // name
        f[2] = dis.readShort(); // mangled type

        // read attributes
        int len = dis.readUnsignedShort();
        for (i = 0; i < len; ++i) {
            short idx = dis.readShort();
            int aLen = dis.readInt();
            String aName = getString(idx);
            // read constant values
            if ("ConstantValue".equals(aName)) {
                //        cConstant = idx;
                f[3] = dis.readShort();
                aLen -= 2;
                //        break;
            }
            // ignore others
            dis.skip(aLen);
        }
    }

    /**
     * Method readConstant.
     * 
     * @param i
     * @param dis
     */
    private boolean readConstant(int i, DataInputStream dis) throws IOException {
        boolean ret = false;
        int len;
        byte b[] = null;
        int tag = dis.readUnsignedByte();
        cTags[i] = tag;
        switch (tag) {
            case CONSTANT_Utf8:
                len = dis.readUnsignedShort();
                b = new byte[len + 2];
                dis.readFully(b, 2, len);
                b[0] = (byte) (len >> 8);
                b[1] = (byte) len;
                String cs = new String(b, 2, len);
                utf8Map.put(cs, new Integer(i));
                //writeln("" + i + ": " + cs);
                break;
            case CONSTANT_Integer:
            case CONSTANT_Float:
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
            case CONSTANT_Long:
            case CONSTANT_Double:
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
            case CONSTANT_Class:
                cRefs[i][0] = dis.readShort();
                //writeln("" + i + ": C " + getString(cRefs[i][0]) + " -> " + cRefs[i][0]);
                break;
            case CONSTANT_String:
                cRefs[i][0] = dis.readShort();
                //writeln("" + i + ": S " + getString(cRefs[i][0]) + " -> " + cRefs[i][0]);
                break;
            case CONSTANT_Fieldref:
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Methodref:
            case CONSTANT_NameAndType:
                cRefs[i][0] = dis.readShort();
                cRefs[i][1] = dis.readShort();
                if (tag == CONSTANT_NameAndType) {
                    //         putInfo(cRefs[i][0], cRefs[i][1], i);
                }
                //writeln("" + i + ": " + tag + " -> " + cRefs[i][0] + "/" + cRefs[i][1]);
                break;
        }
        cData[i] = b;
        return ret;
    }

    /**
     * Method getString.
     * 
     * @param index
     * @return String
     */
    private String getString(int index) {
        byte b[] = cData[index];
        return b == null ? "NULL: " + index : new String(b, 2, b.length - 2);
    }

    /**
     * Method getClasses.
     * 
     * @param ll
     */
    void getClasses(List<String> ll) {
        for (int i = 0; i < cTags.length; ++i) {
            if (cTags[i] == CONSTANT_Class) {
                String cn = getString(cRefs[i][0]);
                ll.add(cn);
            }
            if (cTags[i] == CONSTANT_NameAndType) {
                String cn = getString(cRefs[i][1]);
                addSig(cn, ll);
            }
        }
        for (int i = 0; i < fields.length; ++i) {
            String sig = getString(fields[i][2]);
            addSig(sig, ll);
        }
        for (int i = 0; i < methods.length; ++i) {
            String sig = getString(methods[i][2]);
            addSig(sig, ll);
        }
    }

    private void addSig(String sig, List<String> ll) {
        for (int i = 0; i < sig.length(); ++i) {
            int ch = sig.charAt(i);
            if ("()[IJDFBSCVZ".indexOf(ch) >= 0)
                continue;
            ++i;
            int end = sig.indexOf(';', i);
            String name = sig.substring(i, end);
            ll.add(name);
            i = end;
        }
    }

}

/**
 * Log: $Log: UsesClass.java,v $
 * Log: Revision 1.1  2012/08/11 20:00:28  bebbo
 * Log: @I working stage
 * Log: Log: Revision 1.3 2002/12/19 14:54:58 bebbo Log: @B only private members are crippled
 * now, since analysis of parent classes is missing! Log: Log: Revision 1.2 2002/11/22 21:23:31 bebbo Log: @B fixed the
 * unused entries. Also started method code parsing. Log: Log: Revision 1.1 2002/11/18 12:00:12 bebbo Log: @N first
 * version Log:
 */
