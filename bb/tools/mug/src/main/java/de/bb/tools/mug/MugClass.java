/******************************************************************************
 * $Source: /export/CVS/java/de/bb/tools/mug/src/main/java/de/bb/tools/mug/MugClass.java,v $
 * $Revision: 1.2 $
 * $Date: 2011/08/31 06:30:10 $
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

package de.bb.tools.mug;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author bebbo
 */
class MugClass {
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
    /**
     * code size table. * / private final static int csz[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2,
     * 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2,
     * 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
     * 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3,
     * 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0,
     * 0, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 1, 3, 2, 3, 1, 1, 3, 3, 1, 1, 1, 4, 3, 3, 5, 5, 1, 0, 0, 0, 0, 0, 0,
     * 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
     * 0, 0, 0, 0, 0, 0, 0, 1, 1 };
     * 
     * /
     **/

    private short minor;

    private short major;

    /**
     * These 3 arrays hold the constant pool. cTags = tag cRefs = 1 or 2 refs into constant pool cData = raw data for
     * Integer, Long, Float, Double or UTF8
     */
    private int cTags[];

    private short cRefs[][];

    private byte cData[][];

    private short access;

    private short cClass;

    private short cSuper;

    private short[] cIfaces;

    /**
     * Field information held in short[4]. 0: access 1: name 2: typee 3: 0 or index to constant all other attributes are
     * discarded
     */
    private short[][] fields;

    /**
     * Method information held in short[3]. 0: access 1: name 2: typee
     * 
     * And code and exception attribute
     */
    private short[][] methods;

    private byte[][] codes;

    private byte[][] exceptions;

    private short cConstant;

    private HashMap<String, Integer> utf8Map = new HashMap<String, Integer>();

    private HashMap<Integer, Integer> infoMap = new HashMap<Integer, Integer>();

    private short[][] innerClasses;

    private Mug mug;

    private short[][] oldFields;

    private String oldSuperClass;

    /**
     * Reads the class file into internal structures. Also for each UTF8 the references are stored, to detect when an
     * UTF8 String must be copied.
     * 
     * @param mug
     * @param fis
     *            fileInputStream
     * @throws Exception
     */
    MugClass(Mug mug, InputStream fis) throws Exception {
        this.mug = mug;

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
        //		writeln("Class: " + getClassName(cClass));
        cSuper = dis.readShort();
        //		writeln("Superclass: " + getClassName(cSuper));

        short iCount = dis.readShort();
        cIfaces = new short[iCount];
        for (int i = 0; i < iCount; ++i) {
            cIfaces[i] = dis.readShort();
            //  	writeln("Interface: " + getClassName(cIfaces[i]));
        }

        short fCount = dis.readShort();
        fields = new short[fCount][4];
        for (int i = 0; i < fCount; ++i) {
            readField(i, dis);
            // dumpField(i);
        }

        short mCount = dis.readShort();
        methods = new short[mCount][3];
        codes = new byte[mCount][];
        exceptions = new byte[mCount][];
        for (int i = 0; i < mCount; ++i) {
            readMethod(i, dis);
            // dumpMethod(i);
        }

        short aCount = dis.readShort();
        for (int i = 0; i < aCount; ++i) {
            readAttribute(i, dis);
        }

        oldSuperClass = getClassName(cSuper);
    }

    /**
     * Method readAttribute.
     * 
     * @param index
     * @param dis
     * @throws Exception
     */
    private void readAttribute(int index, DataInputStream dis) throws Exception {
        short idx = dis.readShort();
        int len = dis.readInt();

        String aName = getString(idx);
        if ("InnerClasses".equals(aName)) {
            int noi = dis.readUnsignedShort();
            innerClasses = new short[noi][4];

            for (int i = 0; i < noi; ++i) {
                short[] ic = innerClasses[i];
                ic[0] = dis.readShort();
                ic[1] = dis.readShort();
                ic[2] = dis.readShort();
                ic[3] = dis.readShort();
                //       writeln("ic: " + getClassName(ic[0]));
                //        writeln("ic: " + getClassName(ic[1]));
                //        writeln("ic: " + getString(ic[2]));
            }

            return;
        }

        dis.skip(len);
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

                codes[index] = b;

                continue;
            } else if ("Exceptions".equals(aName)) {
                short numEx = dis.readShort();
                dos.writeShort(numEx);
                for (int j = 0; j < numEx; ++j) {
                    short ex = dis.readShort();
                    dos.writeShort(ex);
                }
                dos.flush();
                exceptions[index] = bos.toByteArray();
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
                cConstant = idx;
                f[3] = dis.readShort();
                aLen -= 2;
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
     * @return
     * @throws IOException
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
                    putInfo(cRefs[i][0], cRefs[i][1], i);
                }
                //        if (getString(cRefs[i][0]).equals("mailCfg"))
                //          writeln("" + i + ": " + tag + " -> " + cRefs[i][0] + "/" + cRefs[i][1]);
                break;
            default:
                writeln("unknown constant: " + tag + " at " + i + " previous tag=" + cTags[i - 1]);
        }
        cData[i] = b;
        return ret;
    }

    /**
     * Method write.
     * 
     * @param outputStream
     *            is filled with class file data
     * @throws IOException
     */
    void write(OutputStream outputStream) throws IOException {
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeInt(0xCAFEBABE);
        dos.writeShort(minor);
        dos.writeShort(major);

        dos.writeShort(cTags.length);
        for (int i = 1; i < cTags.length; ++i) {
            writeConstant(i, dos);
        }

        dos.writeShort(access);
        dos.writeShort(cClass);
        dos.writeShort(cSuper);

        dos.writeShort(cIfaces.length);
        for (int i = 0; i < cIfaces.length; ++i) {
            dos.writeShort(cIfaces[i]);
        }

        dos.writeShort(fields.length);
        for (int i = 0; i < fields.length; ++i) {
            writeField(i, dos);
        }

        dos.writeShort(methods.length);
        for (int i = 0; i < methods.length; ++i) {
            writeMethod(i, dos);
        }

        if (innerClasses == null) {
            dos.writeShort(0);
        }
        /**/
        else {
            dos.writeShort(1);
            short iidx = getStringIndex("InnerClasses");
            dos.writeShort(iidx);
            int len = innerClasses.length * 8 + 2;
            dos.writeInt(len);
            dos.writeShort(innerClasses.length);
            for (int i = 0; i < innerClasses.length; ++i) {
                short[] ic = innerClasses[i];
                dos.writeShort(ic[0]);
                dos.writeShort(ic[1]);
                dos.writeShort(ic[2]);
                dos.writeShort(ic[3]);
            }
        }
        /**/
    }

    /**
     * Method writeMethod.
     * 
     * @param index
     * @param dos
     * @throws IOException
     */
    private void writeMethod(int index, DataOutputStream dos) throws IOException {
        short m[] = methods[index];
        dos.writeShort(m[0]);
        dos.writeShort(m[1]);
        dos.writeShort(m[2]);

        int numAttr = 0;
        if (codes[index] != null)
            ++numAttr;

        if (exceptions[index] != null)
            ++numAttr;

        dos.writeShort(numAttr);
        if (codes[index] != null) {
            dos.write(codes[index]);
        }
        if (exceptions[index] != null) {
            dos.write(exceptions[index]);
        }

    }

    /**
     * Method writeField.
     * 
     * @param i
     * @param dos
     * @throws IOException
     */
    private void writeField(int i, DataOutputStream dos) throws IOException {
        short f[] = fields[i];
        dos.writeShort(f[0]);
        dos.writeShort(f[1]);
        dos.writeShort(f[2]);
        short s = f[3];
        if (s != 0) {
            dos.writeShort(1);
            dos.writeShort(cConstant);
            dos.writeInt(2);
            dos.writeShort(s);
        } else {
            dos.writeShort(0);
        }
    }

    /**
     * Method writeConstant.
     * 
     * @param i
     * @param dos
     * @throws IOException
     */
    private void writeConstant(int i, DataOutputStream dos) throws IOException {
        int tag = cTags[i];
        if (tag == 0)
            return;

        dos.writeByte(tag);
        short[] s = cRefs[i];
        if (s != null) {
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
     * Method putInfo.
     * 
     * @param s
     * @param s1
     * @param i
     */
    private void putInfo(short s, short s1, int i) {
        infoMap.put(new Integer((s << 16) | (0xffff & s1)), new Integer(i));
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
     * Evaluates class names and if possible requests a new name for a class.
     */
    void registerClassNames() {
        String cn = getClassName(cClass);

        // check if the class is inside a renamed package
        int dot = cn.lastIndexOf('/');
        String from = dot < 0 ? "/" : cn.substring(0, dot);
        String to = mug.packages.get(from);

        // check class itself
        // if class is not public, its name gets mangled
        if (0 == (access & ACC_PUBLIC)) {
            mug.addClass(cn, to != null ? to + "/" : null);
        } else if (to != null) {
            String ncn = to + cn.substring(dot);
            mug.renameClass(cn, ncn);
        }
    }

    /**
     * Reserves public names.
     */
    void checkPublic() {
        String cn = getClassName(cClass);

        // check class itself
        // if class is not public, its name gets mangled
        if (0 != (access & ACC_PUBLIC)) {
            int dot = cn.lastIndexOf('/');
            String from = dot < 0 ? "/" : cn.substring(0, dot);
            String to = mug.packages.get(from);
            if (to == null)
                mug.reserveClass(cn);
        }

    }

    /**
     * Evaluates the fields of this class and if possible requests a new name
     */
    void registerFieldNames() {
        mug.ps.println("----");
        String cn = getClassName(cClass);
        mug.ps.println(cn + ": fields");

        // check whether this class is an innerclass
        boolean inner = false;
        if (innerClasses != null) {

            String in = getClassName(innerClasses[0][0]);
            inner = in.equals(cn);
        }

        // first pass for fields adds all field names which must be retained
        mug.preserveField(cn, "serialVersionUID", "J");
        // this means fields for inner or public classes.
        if (inner || 0 != (access & ACC_PUBLIC)) {
            for (int i = 0; i < fields.length; ++i) {
                short[] f = fields[i];
                // preserve public and protected fields, since custom access is possible
                if ((f[0] & (ACC_PUBLIC | ACC_PROTECTED)) != 0) {
                    String fn = getString(f[1]);
                    String tn = getString(f[2]);
                    mug.preserveField(cn, fn, tn);
                }
            }
        }

        // add a mangled name for each field
        for (int i = 0; i < fields.length; ++i) {
            short[] f = fields[i];
            String fn = getString(f[1]);
            String tn = getString(f[2]);
            mug.addField(cn, fn, tn, (f[0] & ACC_PRIVATE) == ACC_PRIVATE);
        }
    }

    void registerMethodNames() {
        String cn = getClassName(cClass);
        mug.ps.println(cn + ": methods");

        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            String fn = getString(m[1]);
            String tn = getString(m[2]);
            mug.preserveOverride(cn, fn, tn);
        }

        // check whether this class is an innerclass
        //    boolean inner = false;
        //    if (innerClasses != null) {

        //      String in = getClassName(innerClasses[0][0]);
        //      inner = in.equals(cn);
        //    }

        // first pass for methods adds all method names which must be retained
        // this means public and protected methods for inner or public classes.

        // if (inner || 0 != (access & ACC_PUBLIC))    
        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            if ((m[0] & (ACC_PUBLIC | ACC_PROTECTED)) != 0) {
                String fn = getString(m[1]);
                String tn = getString(m[2]);
                mug.preserveMethod(cn, fn, tn);
            }
        }

        // check methods
        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            String mn = getString(m[1]);
            String tn = getString(m[2]);
            if (!mn.equals("<init>") && !mn.equals("<clinit>")) {
                mug.addMethod(cn, mn, tn, (m[0] & ACC_PRIVATE) == ACC_PRIVATE);
            }
        }
    }

    /**
     * This functions uses the mug instance to lookup crippled names. It determines which elements are to be renamed and
     * creates new elements with the renamed names. Then all unused elements are collected. Now always the last element
     * is moved into an free slot, until the free slot would be behind the end. Also the move information is stored.
     * Finally all references are updated by using the move information.
     */

    void cripple() {
        String cn = getClassName(cClass);

        oldFields = new short[fields.length][];
        for (int i = 0; i < fields.length; ++i)
            oldFields[i] = fields[i].clone();

        // update fields
        for (int i = 0; i < fields.length; ++i) {
            short[] f = fields[i];
            String type = getString(f[2]);
            String newName = mug.getNewFieldName(cn, getString(f[1]), type);
            if (newName != null) {
                f[1] = getStringIndex(newName);
            }
            String newType = mug.getNewType(type);
            if (type != null) {
                f[2] = getStringIndex(newType);
            }
        }

        // update methods
        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            String oldName = getString(m[1]);
            String type = getString(m[2]);
            String newName = mug.getNewMethodName(cn, oldName, type);
            if (newName != null) {
                m[1] = getStringIndex(newName);
            }
            String newType = mug.getNewType(type);
            if (type != null) {
                m[2] = getStringIndex(newType);
            }
        }

        // update Refs and its NameTypeInfos
        for (int i = 1; i < cTags.length; ++i) {
            int tag = cTags[i];
            switch (tag) {
                case CONSTANT_Fieldref:
                case CONSTANT_InterfaceMethodref:
                case CONSTANT_Methodref:
                    String rcn = getClassName(cRefs[i][0]);
                    short tidx = cRefs[i][1];
                    String name = getString(cRefs[tidx][0]);
                    String type = getString(cRefs[tidx][1]);
                    String newName = null;

                    if (tag == CONSTANT_Fieldref) {
                        newName = mug.getNewFieldName(rcn, name, type);
                        if (newName == null) {
                            // problem:
                            // mug.ps.println("? " + rcn + "." + name + ":" + type);
                            // the field or method might live in a parent class
                            // so we have to search the correct class name
                            // rcn might need an update!
                            String checkClassName = rcn;
                            MugClass mc = mug.getMugClass(rcn);
                            while (mc != null
                            //    && !mc.hasField(name, type)
                            ) {
                                // bingo - this is one we have to search!!!
                                // mug.ps.println(cn + " search parent classes for " + rcn + "." + name);                
                                checkClassName = mc.oldSuperClass;

                                // mug.ps.println("\t" + checkClassName);
                                newName = mug.getNewFieldName(checkClassName, name, type);
                                if (newName != null) {
                                    if (!newName.equals(name)) {
                                        mug.ps.println(rcn + "." + name + " --> " + mc.getSuperClassName() + "."
                                                + newName);
                                    }
                                    break;
                                }
                                mc = mug.getMugClass(checkClassName);
                            }
                        }
                    } else {
                        newName = mug.getNewMethodName(rcn, name, type);
                        if (newName == null) {
                            // problem:
                            // mug.ps.println("? " + rcn + "." + name + ":" + type);
                            // the field or method might live in a parent class
                            // so we have to search the correct class name
                            // rcn might need an update!
                            String checkClassName = rcn;
                            MugClass mc = mug.getMugClass(rcn);
                            while (mc != null
                            //    && !mc.hasField(name, type)
                            ) {
                                // bingo - this is one we have to search!!!
                                // mug.ps.println(cn + " search parent classes for " + rcn + "." + name);                
                                checkClassName = mc.oldSuperClass;

                                // mug.ps.println("\t" + checkClassName);
                                newName = mug.getNewMethodName(checkClassName, name, type);
                                if (newName != null) {
                                    if (!newName.equals(name)) {
                                        mug.ps.println(rcn + "." + name + " --> " + checkClassName + "." + newName);
                                    }
                                    break;
                                }
                                mc = mug.getMugClass(checkClassName);
                            }
                        }
                    }
                    String newType = mug.getNewType(type);

                    if (newName != null || newType != null) {
                        if (newName != null) {
                            name = newName;
                        }
                        if (newType != null) {
                            type = newType;
                        }

                        short ntidx = getInfoIndex(name, type);
                        cRefs[i][1] = ntidx;
                    }
                    break;
            }
        }

        // update class Infos
        for (int i = 1; i < cTags.length; ++i) {
            int tag = cTags[i];
            switch (tag) {
                case CONSTANT_Class:
                    String name = getString(cRefs[i][0]);
                    String newName;
                    if (name.startsWith("["))
                        newName = mug.getNewType(name);
                    else
                        newName = mug.getNewClassName(name);
                    if (newName != null) {
                        //          mug.ps.println("class:" + name + "->" + newName);
                        cRefs[i][0] = getStringIndex(newName);
                    }
                    break;
            }
        }

        // update innerClasses
        if (innerClasses != null) {
            for (int i = 0; i < innerClasses.length; ++i) {
                short ic[] = innerClasses[i];
                String in = getClassName(ic[0]);
                String on = getClassName(ic[1]);
                if (ic[2] != 0) {
                    String newName = in.substring(on.length() + 1);
                    ic[2] = getStringIndex(newName);
                }
            }
        }

        //    iterateMethods();

        shrink();
    }

    /**
     * @param name
     * @param type
     * @return / private boolean hasField(String name, String type) { short [][]fds = oldFields; if (fds == null) fds =
     *         fields;
     * 
     *         for (int i = 0; i < fds.length; ++i) { short[] f = fds[i]; String name2 = getString(f[1]); String type2 =
     *         getString(f[2]); if (name.equals(name2) && type.equals(type2)) return true; } return false; }
     * 
     *         /** Removes no longer used elements from the constant pool.
     */
    private void shrink() {
        TreeSet<Integer> unused = new TreeSet<Integer>();

        // put all UTF8 and NameAndType to unused references
        for (int i = 1; i < cTags.length; ++i) {
            int tag = cTags[i];
            switch (tag) {
                // case CONSTANT_Class: 
                case CONSTANT_Utf8:
                case CONSTANT_NameAndType:
                    Integer ii = new Integer(i);
                    unused.add(ii);
                    break;
            }
        }

        // remove well known UTF8 from unused:
        Integer ii = utf8Map.get("Code");
        if (ii != null) {
            unused.remove(ii);
        }
        ii = utf8Map.get("InnerClasses");
        if (ii != null) {
            unused.remove(ii);
        }
        ii = utf8Map.get("ConstantValue");
        if (ii != null) {
            unused.remove(ii);
        }
        ii = utf8Map.get("Exceptions");
        if (ii != null) {
            unused.remove(ii);
        }

        // remove all refs by fields
        for (int i = 0; i < fields.length; ++i) {
            short[] f = fields[i];
            unused.remove(new Integer(f[1]));
            unused.remove(new Integer(f[2]));
            //      writeln("rem:" + getString(f[1]));
            //      writeln("rem:" + getString(f[2]));
        }

        // remove all refs by methods
        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            unused.remove(new Integer(m[1]));
            unused.remove(new Integer(m[2]));
        }

        // remove all references from constant pool
        for (int i = 1; i < cTags.length; ++i) {
            int tag = cTags[i];
            switch (tag) {
                case CONSTANT_Class:
                case CONSTANT_String:
                    unused.remove(new Integer(cRefs[i][0]));
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_InterfaceMethodref:
                case CONSTANT_Methodref:
                    short nt = cRefs[i][1];
                    unused.remove(new Integer(nt));
                    unused.remove(new Integer(cRefs[nt][0]));
                    unused.remove(new Integer(cRefs[nt][1]));
                    break;
            }
        }
        // if an InnerClass is defined remove its used values too
        if (innerClasses != null) {
            unused.remove(new Integer(getStringIndex("InnerClasses")));
            for (int i = 0; i < innerClasses.length; ++i) {
                short ic[] = innerClasses[i];
                unused.remove(new Integer(ic[2]));
            }
        }

        //    writeln("cc: " + cTags.length + " unused: " + unused.size() + ": " + unused.toString());

        // unused contains now all Entries which are not longer used.
        // now we gather move infos for constants which are moved without touching the code!
        // entries which might be touched by the code would require a code analysis and code modification
        HashMap<Integer, Integer> moveInfo = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> moveUtf8 = new HashMap<Integer, Integer>();
        int last = cTags.length;
        for (Iterator<Integer> i = unused.iterator(); i.hasNext();) {
            --last;
            if (unused.contains(new Integer(last)))
                continue;

            ii = i.next();
            i.remove();
            int idx = ii.intValue();
            if (idx > last)
                break;

            int tag = cTags[last];
            /**/
            if (tag == CONSTANT_NameAndType) {
                cTags[idx] = cTags[last];
                cRefs[idx] = cRefs[last];
                cData[idx] = null;
                moveInfo.put(new Integer(last), ii);
                //        writeln("movconst " + last + ": " + ii);
                continue;
            }
            /**/
            if (tag == CONSTANT_Utf8) {
                String name = getString((short) last);
                cTags[idx] = cTags[last];
                cRefs[idx] = null;
                cData[idx] = cData[last];
                moveUtf8.put(new Integer(last), ii);

                utf8Map.remove(new Integer(last));
                utf8Map.put(name, ii);
                //        writeln("movutf8 " + last + ": " + name);
                continue;
            }
            unused.add(ii);
            //      writeln("last tag = " + cTags[last]);
            break;
        }
        // writeln("new cc: " + last);
        ++last;
        // last is the new minimal size without code modification
        // this might be the same

        /**/
        // update all fields
        for (int i = 0; i < fields.length; ++i) {
            short[] f = fields[i];
            ii = moveUtf8.get(new Integer(f[1]));
            if (ii != null)
                f[1] = ii.shortValue();
            ii = moveUtf8.get(new Integer(f[2]));
            if (ii != null)
                f[2] = ii.shortValue();
        }
        /**/
        // update all methods
        for (int i = 0; i < methods.length; ++i) {
            short[] m = methods[i];
            ii = moveUtf8.get(new Integer(m[1]));
            if (ii != null)
                m[1] = ii.shortValue();
            ii = moveUtf8.get(new Integer(m[2]));
            if (ii != null)
                m[2] = ii.shortValue();
        }

        // update constant pool
        for (int i = 1; i < cTags.length; ++i) {
            int tag = cTags[i];
            switch (tag) {
                case CONSTANT_Class:
                case CONSTANT_String:
                    ii = moveUtf8.get(new Integer(cRefs[i][0]));
                    if (ii != null)
                        cRefs[i][0] = ii.shortValue();
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_InterfaceMethodref:
                case CONSTANT_Methodref:
                    short nt = cRefs[i][1];
                    ii = moveInfo.get(new Integer(nt));
                    if (ii != null)
                        cRefs[i][1] = ii.shortValue();

                    ii = moveUtf8.get(new Integer(cRefs[nt][0]));
                    if (ii != null)
                        cRefs[nt][0] = ii.shortValue();
                    ii = moveUtf8.get(new Integer(cRefs[nt][1]));
                    if (ii != null)
                        cRefs[nt][1] = ii.shortValue();

                    break;
            }
        }

        // update InnerClasses
        if (innerClasses != null) {
            //      move("InnerClasses");

            for (int i = 0; i < innerClasses.length; ++i) {
                short ic[] = innerClasses[i];
                ii = moveUtf8.get(new Integer(ic[2]));
                if (ii != null)
                    ic[2] = ii.shortValue();
                //        writeln(getString(ic[2]));
            }
        }

        // resize
        if (last < cTags.length) {
            int t[] = cTags;
            cTags = new int[last];
            System.arraycopy(t, 0, cTags, 0, last);

            short u[][] = cRefs;
            cRefs = new short[last][];
            System.arraycopy(u, 0, cRefs, 0, last);

            byte v[][] = cData;
            cData = new byte[last][];
            System.arraycopy(v, 0, cData, 0, last);
        }

        // convert empty slots into UTF8 ref to code
        short[] dummy = new short[]{cRefs[cClass][0], 0};
        for (Iterator<Integer> i = unused.iterator(); i.hasNext();) {
            ii = i.next();
            int idx = ii.intValue();
            if (idx >= last)
                break;

            if (cTags[idx] == 1) {
                //        writeln("" + ii + ": " + getString(idx));
            }
            cTags[idx] = CONSTANT_Class;
            cRefs[idx] = dummy;
            cData[idx] = null;
        }
        /**/
    }

    /**
     * Method getInfoIndex.
     * 
     * @param name
     * @param type
     * @return short
     */
    private short getInfoIndex(String name, String type) {
        int ni = getStringIndex(name);
        int ti = getStringIndex(type);

        Integer key = new Integer((ni << 16) | (ti & 0xffff));
        Integer idx = infoMap.get(key);
        if (idx != null) {
            return idx.shortValue();
        }

        short i = (short) cTags.length;
        // put an NameTypeInfo to cPool
        growCP();

        cTags[i] = CONSTANT_NameAndType;
        cRefs[i] = new short[]{(short) ni, (short) ti};

        infoMap.put(key, new Integer(i));
        return i;
    }

    /**
     * Method getStringIndex.
     * 
     * @param s
     * @return short
     */
    private short getStringIndex(String s) {
        Integer ii = utf8Map.get(s);
        if (ii != null) {
            return ii.shortValue();
        }

        short i = (short) cTags.length;
        // put an UTF8 to cPool
        growCP();

        int len = s.length();
        byte[] b = new byte[len + 2];
        b[0] = (byte) (len >> 8);
        b[1] = (byte) len;
        s.getBytes(0, len, b, 2);

        cTags[i] = CONSTANT_Utf8;
        cData[i] = b;

        utf8Map.put(s, new Integer(i));
        return i;
    }

    /**
     * Method growCP.
     */
    private void growCP() {
        int len = cTags.length;

        int t[] = cTags;
        cTags = new int[len + 1];
        System.arraycopy(t, 0, cTags, 0, len);

        short u[][] = cRefs;
        cRefs = new short[len + 1][];
        System.arraycopy(u, 0, cRefs, 0, len);

        byte v[][] = cData;
        cData = new byte[len + 1][];
        System.arraycopy(v, 0, cData, 0, len);
    }

    /**
     * Method dumpField.
     * 
     * @param i
     *            / private void dumpField(int i) {
     * 
     *            writeln(getString(fields[i][2]) + " " + getString(fields[i][1]) + " : " + (fields[i][3] != 0));
     * 
     *            }
     * 
     *            /** Dump info of a method. / private void dumpMethod(int i) { if (codes[i] != null)
     *            writeln(getString(methods[i][2]) + " " + getString(methods[i][1]) + " : " + (codes[i].length)); }
     * 
     *            /** Method getClassName.
     * @param cClass
     * @return String
     */
    private String getClassName(short cClass) {
        short index = cRefs[cClass][0];
        return getString(index);
    }

    /**
     * Method writeln.
     * 
     * @param string
     */
    private void writeln(String string) {
        mug.ps.println(string);
    }

    /**
     * Method getClassName.
     * 
     * @return
     */
    String getClassName() {
        return getClassName(cClass);
    }

    /**
     * Method getClassName.
     * 
     * @return
     */
    String getSuperClassName() {
        return getClassName(cSuper);
    }

    /*
      private void iterateMethods()
      {
        for (int i = 0; i < codes.length; ++i) {
          byte b[] = codes[i];
          int j = 0;
          int len = b[j++] & 0xff;
          len <<= 8;
          len |= b[j++] & 0xff;

          if (!"Code".equals(getString(len))) {
            return;
          }

          j += 8;

          len = b[j++] & 0xff;
          len <<= 8;
          len |= b[j++] & 0xff;
          len <<= 8;
          len |= b[j++] & 0xff;
          len <<= 8;
          len |= b[j++] & 0xff;

          len += j;

          if (len > b.length) {
            throw new RuntimeException("ups");
          }

          int start = j;
          while (j < len) {
            int opcode = b[j] & 0xff;
            if (opcode == 170) // tableswitch
            {
              ++j;
              // align
              j = (j - start + 3) & 0xfffffffc;
              j += start;
              j += 4; // default
              int low = b[j++] & 0xff;
              low <<= 8;
              low |= b[j++] & 0xff;
              low <<= 8;
              low |= b[j++] & 0xff;
              low <<= 8;
              low |= b[j++] & 0xff;
              int count = b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;

              count = count - low + 1;
              j += 4 * count;
              //        mug.ps.println("tableswitch:" + count);

              continue;
            }
            if (opcode == 171) // lookupswitch
            {
              ++j;
              // align
              j = (j - start + 3) & 0xfffffffc;
              j += start;
              j += 4; // default
              int count = b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;
              count <<= 8;
              count |= b[j++] & 0xff;

              j += 8 * count;
              //        mug.ps.println("lookupswitch:" + count);
              continue;
            }

            int size = csz[opcode];
            if (opcode == 196) // wide
            {
              opcode = b[++j];
              size = csz[opcode];
              size = (size - 1) * 2 + 1;
            }

            if (size <= 0) {
              size = 1;
              throw new RuntimeException("invalid opcode: " + opcode);
            }
            switch (opcode) {
              case 18: // ldc // 1
                int idx = ((0xff) & b[j + 1]);
                //              mug.ps.println("const1: " + idx);
                //              idx = mp.convert(idx);
                //              b[j + 1] = (byte)(0xff & (idx >> 8));
                //              b[j + 2] = (byte)(0xff & idx);
                break;
              case 192: // checkcast
                idx = ((0xff) & b[j + 1]);
                idx <<= 8;
                idx |= ((0xff) & b[j + 2]);
                mug.ps.println("op: " + opcode + " const: " + idx + "="
                    + cTags[idx] + "=" + getClassName((short) idx));

              case 178: // getstatic
              case 179: // putstatic
              case 180: // getfield
              case 181: // putfield
              case 182: // invokevirtual
              case 183: // invokespecial
              case 184: // invokestatic
              case 185: // invokeinterface
              case 187: // new
              case 189: // anewarray
              case 193: // instanceof
              case 19: // ldc_w
              case 20: // ldc2_w
              case 197: // multinewarray
                idx = ((0xff) & b[j + 1]);
                idx <<= 8;
                idx |= ((0xff) & b[j + 2]);
                if (getString(cTags[idx]).indexOf("[L") >= 0)
                  mug.ps.println("op: " + opcode + " const: " + idx + "="
                      + cTags[idx] + "=" + getString(cTags[idx]));
                //              idx = mp.convert(idx);
                //              b[j + 1] = (byte)(0xff & (idx >> 8));
                //              b[j + 2] = (byte)(0xff & idx);
                break;
            }

            j += size;
          }
        }
      }

      private interface MethodPoker
      {
        public abstract int convert(int index);
      }

      private class Mark implements MethodPoker
      {
        public int convert(int index)
        {
          return index;
        }
      }

      private class Move implements MethodPoker
      {
        public int convert(int index)
        {
          return index;
        }
      }
    */
    @Override
    public String toString() {
        return getClassName() + " <- " + getSuperClassName();
    }
}

/**
 * Log: $Log: MugClass.java,v $
 * Log: Revision 1.2  2011/08/31 06:30:10  bebbo
 * Log: @B fixes #3
 * Log:  * inner classes are now added correctly
 * Log: Log: Revision 1.1 2011/08/30 11:38:29 bebbo Log: @I renamed package to de.bb.tools.mug
 * Log: @N added package renaming capabilities Log: Log: Revision 1.2 2011/08/29 16:55:59 bebbo Log: @I switch to JDK
 * 1.6 Log: Log: Revision 1.1 2011/08/29 16:32:01 bebbo Log: @N added to BNM build Log: Log: Revision 1.6 2005/12/01
 * 11:38:09 bebbo Log: @I commented out unused code (for code modification which is not used now) Log: Log: Revision 1.5
 * 2005/12/01 11:31:09 bebbo Log: @B fixed field and method renaming for references into other classes with crippled
 * superclasses. E.g. this.other.member -> this.other.x where member is in superclass of other, and was crippled to x.
 * Log: Log: Revision 1.4 2003/01/20 08:11:50 bebbo Log: @B existing method and function names are correctly preserved
 * and no longer used twice. Log: Log: Revision 1.3 2002/12/19 14:54:58 bebbo Log: @B only private members are crippled
 * now, since analysis of parent classes is missing! Log: Log: Revision 1.2 2002/11/22 21:23:31 bebbo Log: @B fixed the
 * unused entries. Also started method code parsing. Log: Log: Revision 1.1 2002/11/18 12:00:12 bebbo Log: @N first
 * version Log:
 */
