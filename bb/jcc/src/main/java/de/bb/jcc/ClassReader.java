/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jcc/src/main/java/de/bb/jcc/ClassReader.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 17:07:01 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Disassemble a Java class file.
 *
 * Copyright (c) by Stefan Bebbo Franke 2002-2008.
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
/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jcc/src/main/java/de/bb/jcc/ClassReader.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 17:07:01 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Read a Java class file and create a class definition object
 *
 * Copyright (c) by Stefan Bebbo Franke 2002-2008.
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

package de.bb.jcc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.bb.jcc.ConstantPool.Entry;

/**
 * Read a class file and create a ClassDefinition.
 * 
 * @author bebbo
 */

public class ClassReader {
    /** class version. */
    private int minor, major;
    /** class attributes. */
    private int access;
    /** constant pool index to class and super class. */
    private int classCI, superClassCI;
    /** constant pool indexes to inner classes. */
    private short[][] innerClassesCI;

    /** the new created ClassDefintion. */
    private ClassDefinition classDefinition;

    /** the constant pool object. */
    private ConstantPool constantPool;

    /**
     * Helper CT to create a ClassDefintion from a byte array.
     * 
     * @param byteCode
     *            the binary class representation.
     * @throws IOException
     */
    public ClassReader(byte byteCode[]) throws IOException {
        this(new ByteArrayInputStream(byteCode));
    }

    /**
     * Reads the class file into internal structures. Also for each UTF8 the references are stored, to detect when an
     * UTF8 String must be copied.
     * 
     * @param mug
     * @param is
     *            fileInputStream
     * @throws Exception
     */
    public ClassReader(InputStream is) throws IOException {
        constantPool = new ConstantPool();

        DataInputStream dis = new DataInputStream(is);
        int magic = dis.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("no valid class file");
        }

        this.minor = dis.readShort();
        this.major = dis.readShort();

        // read all constants
        int constantCount = dis.readShort();
        for (int i = 1; i < constantCount; ++i) {
            if (readConstant(i, dis)) {
                ++i;
            }
        }
        // and rebuild the lookup tables.
        constantPool.rebuildTables();

        access = dis.readShort();
        classCI = dis.readShort();
        superClassCI = dis.readShort();

        classDefinition = new ClassDefinition(constantPool);
        classDefinition.setAccess(access);
        classDefinition.setClassName(constantPool.getConstant(classCI));
        classDefinition.setSuperClassname(constantPool.getConstant(superClassCI));

        { // read the implemented interfaces
            int interfaceCount = dis.readShort();
            for (int i = 0; i < interfaceCount; ++i) {
                int index = dis.readShort();
                classDefinition.addInterface(constantPool.getConstant(index));
            }
        }
        { // read the member fields
            int fieldCount = dis.readShort();
            for (int i = 0; i < fieldCount; ++i) {
                readField(dis);
            }
        }
        { // read the member methods
            int methodCount = dis.readShort();
            for (int i = 0; i < methodCount; ++i) {
                readMethod(dis);
            }
        }
        { // read the attributes
            int attributeCount = dis.readShort();
            for (int i = 0; i < attributeCount; ++i) {
                readAttribute(dis);
            }
        }
    }

    /**
     * Method readAttribute - reads a class attribute and creates a corresponding object if the attribute is known.
     * 
     * @param dis
     *            the DataInputStream to read from
     * @throws IOException
     */
    private void readAttribute(DataInputStream dis) throws IOException {
        int nameIndex = dis.readUnsignedShort();
        int len = dis.readInt();

        String aName = getString(nameIndex);
        if ("InnerClasses".equals(aName)) {
            int innerClassCount = dis.readUnsignedShort();
            innerClassesCI = new short[innerClassCount][4];

            for (int i = 0; i < innerClassCount; ++i) {
                short[] ic = innerClassesCI[i];
                ic[0] = dis.readShort();
                ic[1] = dis.readShort();
                ic[2] = dis.readShort();
                ic[3] = dis.readShort();
            }
            return;
        }

        if ("Signature".equals(aName)) {
            nameIndex = dis.readUnsignedShort();
            classDefinition.setSignature(nameIndex);
            return;
        }

        if ("SourceFile".equals(aName)) {
            nameIndex = dis.readUnsignedShort();
            classDefinition.setSourceFile(nameIndex);
            return;
        }

        byte b[] = new byte[len];
        dis.readFully(b);

        System.err.println("TODO: class attribute: " + aName);
    }

    /**
     * Method readMethod.
     * 
     * @param dis
     *            the DataInputStream to read from
     * @throws IOException
     */
    private void readMethod(DataInputStream dis) throws IOException {
        int acc = dis.readShort(); // access
        int nameIndex = dis.readShort(); // name
        int typeIndex = dis.readShort(); // mangled type

        Code code = classDefinition.createCode();
        Method m =
                classDefinition.defineMethod(acc, constantPool.getConstant(nameIndex),
                        constantPool.getConstant(typeIndex), code);

        int len = dis.readUnsignedShort();
        for (int i = 0; i < len; ++i) {
            int idx = dis.readUnsignedShort();
            int aLen = dis.readInt();
            String aName = getString(idx);
            // read constant values
            if ("Code".equals(aName)) {
                // copy stack / local size
                int cLen = dis.readInt();
                // copy code len
                cLen = dis.readInt();
                // copy code
                byte b[] = new byte[cLen];
                dis.readFully(b);

                // copy Exceptions
                cLen = dis.readUnsignedShort();
                int exx[][] = new int[cLen][4];
                for (int j = 0; j < cLen; ++j) {
                    exx[j][0] = dis.readShort(); // start pc
                    exx[j][1] = dis.readShort(); // end pc
                    exx[j][2] = dis.readShort(); // handler pc
                    exx[j][3] = dis.readShort(); // exception constant index
                }
                // assign code after exceptions are set, since they are needed.
                code.assignCode(b, exx);

                code.buildStack();
                
                int numAttr = dis.readShort();
                while (numAttr > 0) {
                    --numAttr;
                    idx = dis.readShort();
                    aLen = dis.readInt();

                    // known:
                    final String attrName = getString(idx);
                    // - LineNumberTable
                    if ("LineNumberTable".equals(attrName)) {
                        int lntc = dis.readShort();
                        int lnt[][] = new int[lntc][2];
                        for (int j = 0; j < lntc; ++j) {
                            lnt[j][0] = dis.readShort();
                            lnt[j][1] = dis.readShort();
                        }
                        code.setLineNumberTable(lnt);
                        continue;
                    }

                    if ("LocalVariableTable".equals(attrName)) {
                        //        u2 local_variable_table_length;
                        //        {  u2 start_pc;
                        //            u2 length;
                        //            u2 name_index;
                        //            u2 descriptor_index;
                        //            u2 index;
                        //        } local_variable_table[local_variable_table_length];
                        int lvtLen = dis.readShort();
                        int lvt[][] = new int[lvtLen][5];
                        for (int j = 0; j < lvtLen; ++j) {
                            int[] lvte = lvt[j];
                            lvte[0] = dis.readShort();
                            lvte[1] = dis.readShort();
                            lvte[2] = dis.readShort();
                            lvte[3] = dis.readShort();
                            lvte[4] = dis.readShort();
                        }
                        code.setLocalVariableTable(lvt);
                        continue;
                    }

                    // - LocalVariableTable
                    System.err.println("TODO: " + attrName);
                    b = new byte[aLen];
                    dis.readFully(b);
                }

                continue;
            } else if ("Exceptions".equals(aName)) {
                short numEx = dis.readShort();
                String exs[] = new String[numEx];
                for (int j = 0; j < numEx; ++j) {
                    short ex = dis.readShort();
                    exs[j] = constantPool.getConstant(ex);
                }
                m.setExceptions(exs);
                continue;
            }

            if ("Signature".equals(aName)) {
                idx = dis.readUnsignedShort();
                String sig = getString(idx);
                m.setSignature(sig);
                continue;
            }
            System.err.println("TODO: method attribute: " + aName);

            // ignore others
            dis.skip(aLen);
        }
    }

    /**
     * Method readField.
     * 
     * @param dis
     * @throws IOException
     */
    private void readField(DataInputStream dis) throws IOException {
        int a = dis.readShort(); // access
        int n = dis.readShort(); // name
        int t = dis.readShort(); // mangled type

        Field field = classDefinition.defineField(a, constantPool.getConstant(n), constantPool.getConstant(t));

        // read attributes
        int len = dis.readUnsignedShort();
        for (int i = 0; i < len; ++i) {
            short idx = dis.readShort();
            int aLen = dis.readInt();
            String aName = getString(idx);
            // read constant values
            if ("ConstantValue".equals(aName)) {
                int v = dis.readShort();
                field.setConstant(v);
                aLen -= 2;
                continue;
            }

            // ignore others
            System.err.println("TODO: handle field attribute: " + aName);
            dis.skip(aLen);
        }
    }

    /**
     * Method readConstant reads a constant and constructs the corresponding Entry in the ConstantPool.
     * 
     * @param constantIndex
     *            index into constant pool
     * @param dis
     *            the DataInputStream to read from
     * @return
     * @throws IOException
     */
    private boolean readConstant(int constantIndex, DataInputStream dis) throws IOException {
        int tag = dis.readUnsignedByte();
        Entry e;
        switch (tag) {
            case C.CONSTANT_Utf8:
                e = new Entry(dis.readUTF());
                constantPool.addEntry(e);
                return false;

            case C.CONSTANT_Integer:
                e = new Entry(tag, dis.readInt());
                constantPool.addEntry(e);
                return false;

            case C.CONSTANT_Float:
                e = new Entry(dis.readFloat());
                constantPool.addEntry(e);
                return false;

            case C.CONSTANT_Long:
                e = new Entry(dis.readLong());
                constantPool.addEntry(e);
                return true;

            case C.CONSTANT_Double:
                e = new Entry(dis.readDouble());
                constantPool.addEntry(e);
                return true;

            case C.CONSTANT_Class:
            case C.CONSTANT_String:
            case C.CONSTANT_MethodType:
                e = new Entry(tag, dis.readShort(), 0);
                constantPool.addEntry(e);
                return false;

            case C.CONSTANT_NameAndType:
            case C.CONSTANT_Fieldref:
            case C.CONSTANT_InterfaceMethodref:
            case C.CONSTANT_Methodref:
            case C.CONSTANT_InvokeDynamic:
                int n = dis.readShort();
                int t = dis.readShort();
                e = new Entry(tag, n, t);
                constantPool.addEntry(e);
                return false;
            case C.CONSTANT_MethodHandle:
                n = dis.readByte() & 0xff;
                t = dis.readShort();
                e = new Entry(tag, n, t);
                constantPool.addEntry(e);
                return false;

            default:
                throw new IOException("unknown constant: " + tag + " at " + constantIndex);
        }
    }

    /**
     * Method getString.
     * 
     * @param index
     * @return String
     */
    private String getString(int index) {
        return constantPool.getConstant(index);
    }

    public ClassDefinition getClassDefinition() {
        return classDefinition;
    }

    public int getMinor() {
        return minor;
    }

    public int getMajor() {
        return major;
    }
}

/**
 * Log: $Log: ClassReader.java,v $
 * Log: Revision 1.2  2012/08/11 17:07:01  bebbo
 * Log: @I working stage
 * Log: Log: Revision 1.1 2011/01/01 13:26:25 bebbo Log: @N added to new CVS repo Log:
 */
