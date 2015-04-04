/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/MiniClass.java,v $
 * $Revision: 1.8 $
 * $Date: 2013/06/18 13:23:47 $
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

package de.bb.bejy;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;

import de.bb.util.MultiMap;
import de.bb.util.ZipClassLoader;

/**
 * @author bebbo
 */
public class MiniClass {
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
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_InvokeDynamic = 18;

    // private short minor;
    // private short major;

    /**
     * These 3 arrays hold the constant pool. cTags = tag cRefs = 1 or 2 refs
     * into constant pool cData = raw data for Integer, Long, Float, Double or
     * UTF8
     */
    private int cTags[];
    private short cRefs[][];
    private byte cData[][];

    // private short access;

    private short cClass;

    private short cSuper;

    private short[] cIfaces;
    private short access;

    /**
     * Reads the class file into internal structures. Also for each UTF8 the
     * references are stored, to detect when an UTF8 String must be copied.
     * 
     * @param fis
     *            fileInputStream
     */
    public MiniClass(InputStream fis) throws Exception {
        if (fis == null)
            throw new Exception("no class file");
        DataInputStream dis = new DataInputStream(fis);
        int magic = dis.readInt();
        if (magic != 0xCAFEBABE) {
            throw new Exception("no valid class file");
        }

        // minor =
        dis.readShort();
        // major =
        dis.readShort();

        short cCount1 = dis.readShort();

        // writeln("initial ccount: " + cCount1);

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
        // writeln("Class: " + getClassName(cClass));
        cSuper = dis.readShort();
        // writeln("Superclass: " + getClassName(cSuper));

        short iCount = dis.readShort();
        cIfaces = new short[iCount];
        for (int i = 0; i < iCount; ++i) {
            cIfaces[i] = dis.readShort();
            // writeln("Interface: " + getClassName(cIfaces[i]));
        }

        dis.close();
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
            // writeln("" + i + ": " + cs);
            break;
        case CONSTANT_Integer:
        case CONSTANT_Float:
            b = new byte[4];
            dis.readFully(b);
            /*
             * int vi = (b[0] & 0xff); vi = (b[1] & 0xff) | (vi << 8); vi =
             * (b[2] & 0xff) | (vi << 8); vi = (b[3] & 0xff) | (vi << 8);
             * writeln("" + i + ": I: " + vi);
             */
            break;
        case CONSTANT_Long:
        case CONSTANT_Double:
            b = new byte[8];
            dis.readFully(b);
            ret = true;
            /*
             * long vl = (b[0] & 0xff); vl = (b[1] & 0xff) | (vl << 8); vl =
             * (b[2] & 0xff) | (vl << 8); vl = (b[3] & 0xff) | (vl << 8); vl =
             * (b[4] & 0xff) | (vl << 8); vl = (b[5] & 0xff) | (vl << 8); vl =
             * (b[6] & 0xff) | (vl << 8); vl = (b[7] & 0xff) | (vl << 8);
             * writeln("" + i + ": L: " + vl);
             */
            break;
        case CONSTANT_Class:
        case CONSTANT_String:
        case CONSTANT_MethodType:
            cRefs[i][0] = dis.readShort();
            // writeln("" + i + ": S " + getString(cRefs[i][0]) + " -> " +
            // cRefs[i][0]);
            break;
        case CONSTANT_Fieldref:
        case CONSTANT_InterfaceMethodref:
        case CONSTANT_Methodref:
        case CONSTANT_NameAndType:
        case CONSTANT_InvokeDynamic:
            cRefs[i][0] = dis.readShort();
            cRefs[i][1] = dis.readShort();
            // writeln("" + i + ": " + tag + " -> " + cRefs[i][0] + "/" +
            // cRefs[i][1]);
            break;
        case CONSTANT_MethodHandle:
            cRefs[i][0] = (short)(dis.readByte() & 0xff);
            cRefs[i][1] = dis.readShort();
            break;
        default:
            /*
             * writeln( "unknown constant: " + tag + " at " + i +
             * " previous tag=" + cTags[i - 1]);
             */
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
     * Method getClassName.
     * 
     * @param cClass
     * @return String
     */
    private String getClassName(int cClass) {
        if (cClass == 0)
            return null;
        short index = cRefs[cClass][0];
        return getString(index);
    }

    /**
     * Method getClassName.
     */
    public String getClassName() {
        return getClassName(cClass);
    }

    String getSuperClassName() {
        return getClassName(cSuper);
    }

    public String[] getInterfaceNames() {
        String r[] = new String[cIfaces.length];
        for (int i = 0; i < cIfaces.length; ++i) {
            r[i] = getClassName(cIfaces[i]);
        }
        return r;
    }

    /**
     * Returns true if the specified class name is referenced from the
     * constants. E.g. useful to check whether some annotation is referenced.
     * 
     * @param className
     *            the class name
     * @return true if the class name is referenced.
     */
    public boolean refersToClass(String className) {
        for (int i = 0; i < cTags.length; ++i) {
            if (cTags[i] == CONSTANT_Class) {
                if (className.equals(getClassName(i)))
                    return true;
            }
        }
        return false;
    }

    public HashSet<String> getStrings() {
        HashSet<String> strings = new HashSet<String>();
        for (int i = 0; i < cTags.length; ++i) {
            if (cTags[i] == CONSTANT_Utf8) {
                strings.add(new String(cData[i], 2, cData[i].length - 2));
            }
        }
        return strings;
    }

    public boolean isInterface() {
        return (access & 0x601) == 0x601;
    }

    public String toString() {
        return "MiniClass: " + getClassName();
    }

    public static MultiMap<String, String> findRelatedClasses(
            final ZipClassLoader zcl, final String... patterns) throws Exception {
        // get all classes
        final HashSet<URL> urls = new HashSet<URL>();
        for (final URL url : zcl.getURLs()) {
            urls.add(url);
        }
        final String[] classes = zcl.list("*.class", urls);

        final MultiMap<String, String> pattern2Class = new MultiMap<String, String>();
        for (final String cnc : classes) {
            final InputStream is = zcl.getResourceAsStream(cnc);
            if (is == null)
                continue;
            final MiniClass mc = new MiniClass(is);
            is.close();
            if (mc.isInterface())
                continue;
            final String cn = cnc.substring(0, cnc.length() - 6);
            final HashSet<String> strings = mc.getStrings();

            for (final String pattern : patterns) {
                if (strings.contains(pattern))
                    pattern2Class.put(pattern, cn);
            }
        }
        return pattern2Class;

    }
}
