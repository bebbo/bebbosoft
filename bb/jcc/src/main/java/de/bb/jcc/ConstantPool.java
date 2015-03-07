/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jcc/src/main/java/de/bb/jcc/ConstantPool.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 17:07:14 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Handle the constant pool of a Java class file.
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Reflects a class constant pool allowing convenient extension.
 * 
 * @author bebbo
 */
public class ConstantPool {
    private static final Object DUMMY = new Entry(C.CONSTANT_Dummy, 0, 0);

    private ArrayList elements = new ArrayList();

    private HashMap utf8;

    private HashMap classes;

    private HashMap nameTypes;

    private HashMap fields;

    private HashMap methods;

    private HashMap ifacemethods;

    private HashMap strings;

    /**
     * Create a new ConstantPool object.
     */
    public ConstantPool() {
        initMaps();
    }

    /**
     * Adds an Integer to constant pool, if not already there.
     * 
     * @param val
     * @return the index into constant pool.
     */
    int addInteger(int val) {
        Integer key = new Integer(val);
        Integer ii = (Integer) utf8.get(key);
        if (ii == null) {
            elements.add(new Entry(C.CONSTANT_Integer, val));
            ii = new Integer(elements.size());
            utf8.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a Float to constant pool, if not already there.
     * 
     * @param val
     * @return the index into constant pool.
     */
    int addFloat(float val) {
        Float key = new Float(val);
        Integer ii = (Integer) utf8.get(key);
        if (ii == null) {
            elements.add(new Entry(val));
            ii = new Integer(elements.size());
            utf8.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a Long to constant pool, if not already there.
     * 
     * @param val
     * @return the index into constant pool.
     */
    int addLong(long val) {
        Long key = new Long(val);
        Integer ii = (Integer) utf8.get(key);
        if (ii == null) {
            elements.add(new Entry(val));
            ii = new Integer(elements.size());
            utf8.put(key, ii);
            elements.add(new Entry(C.CONSTANT_Dummy, 0));
        }
        return ii.intValue();
    }

    /**
     * Adds a Double to constant pool, if not already there.
     * 
     * @param val
     * @return the index into constant pool.
     */
    int addDouble(double val) {
        Double key = new Double(val);
        Integer ii = (Integer) utf8.get(key);
        if (ii == null) {
            elements.add(new Entry(val));
            ii = new Integer(elements.size());
            utf8.put(key, ii);
            elements.add(new Entry());// C.CONSTANT_Dummy, 0));
        }
        return ii.intValue();
    }

    /**
     * Adds an UTF8 string to constant pool, if not already there.
     * 
     * @param name
     * @return the index in constant pool.
     */
    int addUTF8(String name) {
        Integer ii = (Integer) utf8.get(name);
        if (ii == null) {
            elements.add(new Entry(name));
            ii = new Integer(elements.size());
            utf8.put(name, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a class to constant pool, if not already there.
     * 
     * @param name
     * @return the index in constant pool.
     */
    int addClass(String name) {
        if (name.length() == 0)
            throw new RuntimeException("empty class name");
        name = Util.dot2Slash(name);
        Integer ii = (Integer) classes.get(name);
        if (ii == null) {
            int ci = addUTF8(name);
            elements.add(new Entry(C.CONSTANT_Class, ci));
            ii = new Integer(elements.size());
            classes.put(name, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds an Entry to this constant pool. After using this method a call to rebuildTables is required!
     * 
     * @param e
     *            the entry
     */
    void addEntry(Entry e) {
        elements.add(e);
        if (e.kind == C.CONSTANT_Long || e.kind == C.CONSTANT_Double)
            elements.add(DUMMY);
    }

    /**
     * Rebuild all lookup tables.
     */
    void rebuildTables() {
        int n = 1;
        for (Iterator k = elements.iterator(); k.hasNext(); ++n) {
            initMaps();
            Entry e = (Entry) k.next();

            Integer ii = new Integer(n);
            switch (e.kind) {
                case C.CONSTANT_Utf8:
                    if (utf8.get(e.sVal) == null)
                        utf8.put(e.sVal, ii);
                    break;
                case C.CONSTANT_Integer:
                    Integer i = new Integer(e.iVal1);
                    if (utf8.get(i) == null)
                        utf8.put(i, ii);
                    break;
                case C.CONSTANT_Float:
                    Float f = new Float(e.fVal);
                    if (utf8.get(f) == null)
                        utf8.put(f, ii);
                    break;
                case C.CONSTANT_Long:
                    Long l = new Long(e.lVal);
                    if (utf8.get(l) == null)
                        utf8.put(l, ii);
                    break;
                case C.CONSTANT_Double:
                    Double d = new Double(e.dVal);
                    if (utf8.get(d) == null)
                        utf8.put(d, ii);
                    break;
                case C.CONSTANT_String:
                    String s = getConstant(e.iVal1);
                    if (strings.get(s) == null)
                        strings.put(s, ii);
                    break;
                case C.CONSTANT_Class:
                    s = getConstant(n);
                    if (classes.get(s) == null)
                        classes.put(s, ii);
                    break;
                case C.CONSTANT_NameAndType:
                    s = getConstant(n);
                    if (nameTypes.get(s) == null)
                        nameTypes.put(s, ii);
                    break;
                case C.CONSTANT_Fieldref:
                    s = getConstant(n);
                    if (nameTypes.get(s) == null)
                        nameTypes.put(s, ii);
                    break;
                case C.CONSTANT_InterfaceMethodref:
                    s = getConstant(n);
                    if (ifacemethods.get(s) == null)
                        ifacemethods.put(s, ii);
                    break;
                case C.CONSTANT_Methodref:
                    s = getConstant(n);
                    if (methods.get(s) == null)
                        methods.put(s, ii);
                    break;
            }

        }
    }

    /**
     * Used to reset the lookup tables.
     */
    private void initMaps() {
        utf8 = new HashMap();
        classes = new HashMap();
        nameTypes = new HashMap();
        fields = new HashMap();
        methods = new HashMap();
        ifacemethods = new HashMap();
        strings = new HashMap();
    }

    /**
     * Get an Entry for the constant pool index.
     * 
     * @param index
     *            the index
     * @return the Entry or null if nothing found
     */
    Entry getEntry(int index) {
        if (index < 1 || index > elements.size())
            return null;
        Entry e = (Entry) elements.get(index - 1);
        return e;
    }

    /**
     * Adds a String to constant pool, if not already there. A String refers to an UTF8 Entry.
     * 
     * @param value
     * @return the index in constant pool.
     */
    int addString(String value) {
        value.length();
        Integer ii = (Integer) strings.get(value);
        if (ii == null) {
            int ci = addUTF8(value);
            elements.add(new Entry(C.CONSTANT_String, ci));
            ii = new Integer(elements.size());
            strings.put(value, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a name/type to constant pool, if not already there.
     * 
     * @param name
     *            the name - will refer to an UTF8 Entry
     * @param type
     *            the type - will refer to an UTF8 Entry
     * @return the index in constant pool.
     */
    int addNameType(String name, String type) {
        type = Util.dot2Slash(type);
        String key = name + ":" + type;
        Integer ii = (Integer) nameTypes.get(key);
        if (ii == null) {
            int i1 = addUTF8(name);
            int i2 = addUTF8(type);
            elements.add(new Entry(C.CONSTANT_NameAndType, i1, i2));
            ii = new Integer(elements.size());
            nameTypes.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a field to constant pool, if not already there.
     * 
     * @param className
     *            where the field belongs to
     * @param name
     *            the name - will refer to an UTF8 Entry
     * @param type
     *            the type - will refer to an UTF8 Entry
     * @return the index in constant pool.
     */
    int addField(String className, String name, String type) {
        className = Util.dot2Slash(className);
        type = Util.dot2Slash(type);
        String key = className + "." + name + ":" + type;
        Integer ii = (Integer) fields.get(key);
        if (ii == null) {
            int i1 = addClass(className);
            int i2 = addNameType(name, type);
            elements.add(new Entry(C.CONSTANT_Fieldref, i1, i2));
            ii = new Integer(elements.size());
            fields.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds a method to constant pool, if not already there.
     * 
     * @param className
     *            where the field belongs to
     * @param name
     *            the name - will refer to an UTF8 Entry
     * @param type
     *            the type - will refer to an UTF8 Entry
     * @return the index in constant pool.
     */
    int addMethod(String className, String name, String type) {
        className = Util.dot2Slash(className);
        type = Util.dot2Slash(type);
        String key = className + "." + name + type;
        Integer ii = (Integer) methods.get(key);
        if (ii == null) {
            int i1 = addClass(className);
            int i2 = addNameType(name, type);
            elements.add(new Entry(C.CONSTANT_Methodref, i1, i2));
            ii = new Integer(elements.size());
            methods.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * Adds an interface method to constant pool, if not already there.
     * 
     * @param className
     *            where the field belongs to
     * @param name
     *            the name - will refer to an UTF8 Entry
     * @param type
     *            the type - will refer to an UTF8 Entry
     * @return the index in constant pool.
     */
    int addIfaceMethod(String className, String name, String type) {
        className = Util.dot2Slash(className);
        type = Util.dot2Slash(type);
        String key = className + "." + name + type;
        Integer ii = (Integer) ifacemethods.get(key);
        if (ii == null) {
            int i1 = addClass(className);
            int i2 = addNameType(name, type);
            elements.add(new Entry(C.CONSTANT_InterfaceMethodref, i1, i2));
            ii = new Integer(elements.size());
            ifacemethods.put(key, ii);
        }
        return ii.intValue();
    }

    /**
     * interal class to maintain the constant pool entries.
     * 
     * @author sfranke
     * 
     */
    static class Entry {
        /** what kind is it? */
        int kind;
        /** set if UTF8 kind. */
        String sVal;
        /** set if int kind or ref type (nametype...). */
        int iVal1;
        /** set if ref type (nametype...). */
        int iVal2;
        /** set if long kind. */
        long lVal;
        /** set if float kind. */
        float fVal;
        /** set if double kind. */
        double dVal;

        /**
         * Create a dummy Entry (after long or double).
         */
        Entry() {
            this.kind = C.CONSTANT_Dummy;
        }

        /**
         * Create an UTF8 Entry.
         * 
         * @param val
         *            the value
         */
        Entry(String val) {
            this.kind = C.CONSTANT_Utf8;
            sVal = val;
        }

        /**
         * Create an int Entry.
         * 
         * @param kind
         * @param val
         */
        Entry(int kind, int val) {
            this.kind = kind;
            iVal1 = val;
        }

        /**
         * Create a reference Entry
         * 
         * @param kind
         *            the kind
         * @param val1
         *            value 1
         * @param val2
         *            value 2
         */
        Entry(int kind, int val1, int val2) {
            this.kind = kind;
            iVal1 = val1;
            iVal2 = val2;
        }

        /**
         * Create a long Entry.
         * 
         * @param val
         *            the long value.
         */
        Entry(long val) {
            this.kind = C.CONSTANT_Long;
            lVal = val;
        }

        /**
         * Create a float Entry.
         * 
         * @param val
         *            the float value.
         */
        Entry(float val) {
            this.kind = C.CONSTANT_Float;
            fVal = val;
        }

        /**
         * Create a double Entry.
         * 
         * @param val
         *            the double value.
         */
        Entry(double val) {
            this.kind = C.CONSTANT_Double;
            dVal = val;
        }

        /**
         * Provide a human readable form.
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Entry[");
            if (C.CONSTANT_Dummy == kind)
                return sb.append("dummy:").append("]").toString();
            switch (kind) {
                case C.CONSTANT_Utf8:
                    sb.append("UTF8:");
                    sb.append(sVal);
                    break;
                case C.CONSTANT_String:
                    sb.append("String:");
                    sb.append(iVal1);
                    break;
                case C.CONSTANT_Class:
                    sb.append("Class:");
                    sb.append(iVal1);
                    break;
                case C.CONSTANT_Fieldref:
                    sb.append("Fieldref:");
                    sb.append(iVal1).append(",").append(iVal2);
                    break;
                case C.CONSTANT_InterfaceMethodref:
                    sb.append("InterfaceMethod:");
                    sb.append(iVal1).append(",").append(iVal2);
                    break;
                case C.CONSTANT_Methodref:
                    sb.append("Method:");
                    sb.append(iVal1).append(",").append(iVal2);
                    break;
                case C.CONSTANT_NameAndType:
                    sb.append("NameType:");
                    sb.append(iVal1).append(",").append(iVal2);
                    break;
                case C.CONSTANT_Integer:
                    sb.append("int:");
                    sb.append(iVal1);
                    break;
                case C.CONSTANT_Float:
                    sb.append("float:");
                    sb.append(fVal);
                    break;
                case C.CONSTANT_Long:
                    sb.append("long:");
                    sb.append(lVal);
                    break;
                case C.CONSTANT_Double:
                    sb.append("double:");
                    sb.append(dVal);
                    break;
                default:
                    sb.append("???");
            }
            return sb.append("]").toString();
        }

        /**
         * Write the constant pool Entry to a class file.
         * 
         * @param dos
         *            a DataOutputStream
         * @throws IOException
         */
        void writeTo(DataOutputStream dos) throws IOException {
            // print();
            if (C.CONSTANT_Dummy == kind)
                return;

            dos.writeByte(kind);
            switch (kind) {
                case C.CONSTANT_Utf8: {
                    byte b[] = sVal.getBytes();
                    dos.writeShort(b.length);
                    dos.write(b);
                }
                    break;
                case C.CONSTANT_String:
                case C.CONSTANT_Class: {
                    dos.writeShort(iVal1);
                }
                    break;
                case C.CONSTANT_Fieldref:
                case C.CONSTANT_InterfaceMethodref:
                case C.CONSTANT_Methodref:
                case C.CONSTANT_NameAndType: {
                    dos.writeShort(iVal1);
                    dos.writeShort(iVal2);
                }
                    break;
                case C.CONSTANT_Integer:
                    dos.writeInt(iVal1);
                    break;
                case C.CONSTANT_Float:
                    dos.writeFloat(fVal);
                    break;
                case C.CONSTANT_Long:
                    dos.writeLong(lVal);
                    break;
                default:
                    System.out.println("not specified:" + kind);
            }
        }
    }

    /**
     * Method writeTo.
     * 
     * @param dos
     */
    void writeTo(DataOutputStream dos) throws IOException {
        dos.writeShort(elements.size() + 1);
        // int n = 0;
        for (Iterator e = elements.iterator(); e.hasNext();) {
            // System.out.print((++n) + ": ");
            Entry ee = (Entry) e.next();
            ee.writeTo(dos);
        }
    }

    /**
     * Get the text representation for the assembler.
     * 
     * @param index
     *            index into constant pool
     * @return a String used in jasm to create this constant pool entry
     */
    public String getConstant(int index) {
        Entry e = getEntry(index);
        if (e != null)
            switch (e.kind) {
                case C.CONSTANT_Utf8:
                    return e.sVal;
                case C.CONSTANT_Integer:
                    if (e.iVal1 > 1023)
                        return "0x" + Integer.toHexString(e.iVal1);
                    return String.valueOf(e.iVal1);
                case C.CONSTANT_Long:
                    if (e.lVal > 1023)
                        return "0x" + Long.toHexString(e.lVal) + "L";
                    return String.valueOf(e.lVal) + "L";
                case C.CONSTANT_Float:
                    return String.valueOf(e.fVal) + "f";
                case C.CONSTANT_Double:
                    return String.valueOf(e.dVal) + "d";
                case C.CONSTANT_String:
                    e = getEntry(e.iVal1);
                    return "\"" + Util.unescape(e.sVal) + "\"";
                case C.CONSTANT_Fieldref: {
                    String c = getConstant(e.iVal1);
                    String nt = getConstant(e.iVal2);
                    return Util.slash2Dot(c) + "." + nt;
                }
                case C.CONSTANT_Class:
                    return Util.class2Type(getConstant(e.iVal1));
                case C.CONSTANT_NameAndType: {
                    String n = getConstant(e.iVal1);
                    String t = getConstant(e.iVal2);
                    return n + ":" + Util.signature2Type(t);
                }
                case C.CONSTANT_Methodref:
                case C.CONSTANT_InterfaceMethodref: {
                    String c = getConstant(e.iVal1);
                    e = getEntry(e.iVal2);
                    String n = getConstant(e.iVal1);
                    String t = getConstant(e.iVal2);
                    return Util.slash2Dot(c) + "." + n + Util.signature2Type(t);
                }
                case C.CONSTANT_InvokeDynamic:
                	String n = getConstant(e.iVal2);
                	return e.iVal1 + ":" + n;
            }
        return null;
    }

}
