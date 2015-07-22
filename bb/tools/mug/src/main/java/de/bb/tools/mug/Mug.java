/******************************************************************************
 * $Source: /export/CVS/java/de/bb/tools/mug/src/main/java/de/bb/tools/mug/Mug.java,v $
 * $Revision: 1.2 $
 * $Date: 2011/08/31 06:30:07 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author bebbo
 */

class Mug {
    HashMap<String, Counter> pkCounter = new HashMap<String, Counter>();
    HashMap<String, Counter2> mCounter = new HashMap<String, Counter2>();
    HashMap<String, Counter2> fCounter = new HashMap<String, Counter2>();

    HashMap<String, String> classMap = new HashMap<String, String>();
    HashMap<String, String> mMap = new HashMap<String, String>(); //maps the original name to the new name  
    HashMap<String, String> mRevMap = new HashMap<String, String>(); // maps back from short names to original names
    HashMap<String, String> fMap = new HashMap<String, String>();
    HashMap<String, String> fRevMap = new HashMap<String, String>(); // maps back
    HashMap<String, MugClass> classes = new HashMap<String, MugClass>();

    PrintStream ps;
    HashMap<String, String> packages;

    public Mug(PrintStream ps, HashMap<String, String> packages) {
        this.ps = ps;
        this.packages = packages;
    }

    /**
     * Gives a class a new mangled name. For InnerClasses their parents are automatically added.
     * 
     * @param cn
     */
    void addClass(String cn, String toPackage) {
        // get the package
        int idx = cn.lastIndexOf("/");
        String opk = cn.substring(0, idx + 1);
        String pk, oldName;
        if (idx < 0) {
            pk = "/";
            oldName = cn;
        } else {
            pk = opk;
            oldName = cn.substring(idx + 1);
        }
        if (toPackage != null)
            pk = toPackage;

        Counter cnt = pkCounter.get(pk);
        if (cnt == null) {
            cnt = new Counter();
            pkCounter.put(pk, cnt);
        }
        String nm = classMap.get("#" + cn.toLowerCase());
        if (nm == null) {
            nm = cnt.next();
            while (classMap.get("#" + pk + nm.toLowerCase()) != null) {
                nm = cnt.next();
            }
        }
        idx = oldName.lastIndexOf('$');
        if (idx > 0) {
            String oldBase = oldName.substring(0, idx);
            String parent = opk + oldBase;
            String nBase = classMap.get(parent);
            if (nBase == null) {
                MugClass mc = classes.get(parent);
                mc.registerClassNames();
                nBase = classMap.get(parent);
                if (nBase == null) {
                    nBase = oldBase;
                }
            }
            idx = nBase.lastIndexOf('/');
            nBase = nBase.substring(idx + 1);
            nm = nBase + "$" + nm;
        }
        classMap.put(cn, pk + nm);
        classMap.put("#" + cn.toLowerCase(), pk + nm);
        ps.println("renaming class: " + cn + " -> " + nm);
    }

    void reserveClass(String cn) {
        classMap.put("#" + cn.toLowerCase(), cn);
        classMap.put(cn, cn);
    }

    /**
     * Rename a class - used to replace a package name
     * 
     * @param cn
     *            old class name
     * @param ncn
     *            new class name
     */
    void renameClass(String cn, String ncn) {
        classMap.put(cn, ncn);
        classMap.put("#" + cn.toLowerCase(), ncn);
        ps.println("renaming class: " + cn + " -> " + ncn);
    }

    void addMethod(String cn, String methodName, String typ, boolean isPrivate) {
        typ = removeReturn(typ);
        String id = cn + "\\" + methodName + "\\" + typ;
        if (mMap.get(id) != null)
            return;

        Counter2 cnt = mCounter.get(cn);
        if (cnt == null) {
            cnt = new Counter2();
            mCounter.put(cn, cnt);
        }

        int ket = typ.lastIndexOf(')');
        String param = typ.substring(0, ket + 1);

        String newMethodName = cnt.next();
        for (;;) {
            boolean isUsed = mMap.get(cn + "\\" + newMethodName + "\\" + typ) != null;

            // search all parent classes for possible conflicts!
            String acn = cn;
            while (!isUsed) {
                MugClass mc = getSuperClass(acn);
                if (mc == null)
                    break;

                acn = mc.getClassName();
                String parentName = mRevMap.get(acn + "\\" + newMethodName + "\\" + param);
                if (parentName != null) {
                    isUsed = true;
                }
            }

            if (!isUsed)
                break;

            ps.println("method used: " + newMethodName);
            newMethodName = cnt.next();
        }

        mMap.put(id, newMethodName);
        mMap.put(cn + "\\" + newMethodName + "\\" + typ, newMethodName);
        if (!isPrivate)
            mRevMap.put(cn + "\\" + newMethodName + "\\" + param, id);

        ps.println("method: " + methodName + " -> " + newMethodName);
    }

    void preserveOverride(String cn, String mn, String typ) {
        typ = removeReturn(typ);
        String id = cn + "\\" + mn + "\\" + typ;
        if (mMap.get(id) != null)
            return;

        // check for parent classes for overrides
        String override = checkOverride(cn, mn, typ);
        if (override != null) {
            mMap.put(id, override);
            mMap.put(cn + "\\" + override + "\\" + typ, override);
            // ps.println("overridden method: " + mn + " -> " + override);
        }
        return;

    }

    /**
     * @param cn
     * @param mn
     * @param typ
     * @return
     */
    private String checkOverride(String cn, String mn, String typ) {
        MugClass mc = classes.get(cn);
        String suName = mc.getSuperClassName();
        if (suName == null)
            return null;
        MugClass parent = classes.get(suName);
        if (parent == null)
            return null;
        String id = suName + "\\" + mn + "\\" + typ;
        String nm = mMap.get(id);
        if (nm != null)
            return nm;
        return checkOverride(suName, mn, typ);
    }

    void addField(String cn, String fn, String typ, boolean isPrivate) {
        String id = cn + "\\" + fn + "\\" + typ;
        if (fMap.get(id) != null)
            return;

        Counter2 cnt = fCounter.get(cn);
        if (cnt == null) {
            cnt = new Counter2();
            fCounter.put(cn, cnt);
        }

        String nm = cnt.next();
        for (;;) {
            boolean isUsed = fMap.get(cn + "\\" + nm + "\\" + typ) != null;

            // search all parent classes for possible conflicts!
            String acn = cn;
            while (!isUsed) {
                MugClass mc = getSuperClass(acn);
                if (mc == null)
                    break;

                acn = mc.getClassName();
                String parentName = fRevMap.get(acn + "\\" + nm);
                if (parentName != null) {
                    isUsed = true;
                }
            }

            if (!isUsed)
                break;

            // ps.println("field used: " + nm);      
            nm = cnt.next();
        }

        fMap.put(id, nm);
        if (!isPrivate)
            fRevMap.put(cn + "\\" + nm, id);

        ps.println("field: " + fn + " -> " + nm);
    }

    /**
     * Method preserveField.
     * 
     * @param cn
     * @param fn
     * @param tn
     */
    public void preserveField(String cn, String fn, String tn) {
        String id = cn + "\\" + fn + "\\" + tn;
        fMap.put(id, fn);
    }

    /**
     * Method preserveMethod.
     * 
     * @param cn
     * @param fn
     * @param tn
     */
    public void preserveMethod(String cn, String mn, String tn) {
        tn = removeReturn(tn);
        String id = cn + "\\" + mn + "\\" + tn;
        mMap.put(id, mn);
    }

    /**
     * @param tn
     * @return
     */
    private String removeReturn(String tn) {
        int ket = tn.lastIndexOf(")");
        return tn.substring(0, ket + 1);
    }

    private static class Counter {
        private int n = 0;

        String next() {
            int i = n++;
            String s = "";
            do {
                int l = i % 26;
                i /= 26;
                char c = l > 25 ? 'A' : 'a';
                c += (char) (l % 26);
                s = c + s;
            } while (i != 0);

            return s;
        }
    }

    private static class Counter2 {
        private int n = -1;

        String next() {
            int i = n++;
            if (i == -1)
                return "Code";
            String s = "";
            do {
                int l = i % 54;
                i /= 54;
                char c;
                if (l == 52)
                    c = '_';
                else if (l == 53) {
                    c = '$';
                } else {
                    c = l > 25 ? 'A' : 'a';
                    c += (char) (l % 26);
                }

                s = c + s;
            } while (i != 0);

            return s;
        }
    }

    /**
     * Method getNewClassName.
     * 
     * @param cn
     * @return String
     */
    String getNewClassName(String cn) {
        return classMap.get(cn);
    }

    /**
     * Method getFieldName.
     * 
     * @param string
     * @return String
     */
    String getNewFieldName(String cn, String fn, String typ) {
        String id = cn + "\\" + fn + "\\" + typ;
        return fMap.get(id);
    }

    String getNewMethodName(String cn, String mn, String typ) {
        typ = removeReturn(typ);
        String id = cn + "\\" + mn + "\\" + typ;
        return mMap.get(id);
    }

    /**
     * Method getTyp.
     * 
     * @param typ
     * @return String
     */
    String getNewType(String typ) {
        String newTyp = "";

        for (int idx = typ.indexOf('L'); idx >= 0; idx = typ.indexOf('L')) {
            newTyp += typ.substring(0, idx + 1);
            typ = typ.substring(idx + 1);
            idx = typ.indexOf(';');
            String cn = typ.substring(0, idx);
            String newCn = getNewClassName(cn);
            if (newCn != null) {
                cn = newCn;
            }
            newTyp += cn;
            typ = typ.substring(idx);
        }
        newTyp += typ;
        return newTyp;
    }

    /**
     * Method write.
     * 
     * @param string
     */
    void writeTo(String path) throws IOException {
        File dir = new File(path);
        dir.mkdirs();
        for (Iterator<MugClass> i = classes.values().iterator(); i.hasNext();) {
            MugClass mc = i.next();
            String cn = mc.getClassName();
            int idx = cn.lastIndexOf('/');
            if (idx >= 0) {
                new File(dir, cn.substring(0, idx)).mkdirs();
            }
            OutputStream os = new FileOutputStream(new File(dir, cn + ".class"));
            os = new BufferedOutputStream(os);
            mc.write(os);
            os.close();
        }
    }

    /**
     * Method cripple.
     */
    void cripple() {
        for (Iterator<MugClass> i = classes.values().iterator(); i.hasNext();) {
            MugClass mc = i.next();
            mc.checkPublic();
        }
        for (Iterator<MugClass> i = classes.values().iterator(); i.hasNext();) {
            MugClass mc = i.next();
            if (classMap.get(mc.getClassName()) == null) {
                mc.registerClassNames();
            }
        }

        HashSet<MugClass> done = new HashSet<MugClass>();
        for (Iterator<MugClass> i = classes.values().iterator(); i.hasNext();) {
            MugClass mc = i.next();
            prepare(done, mc);
        }
        for (Iterator<MugClass> i = classes.values().iterator(); i.hasNext();) {
            MugClass mc = i.next();
            //      String oldName = mc.getClassName();
            mc.cripple();
            //      String newName = mc.getClassName();
        }
    }

    /**
     * @param done
     * @param mc
     */
    private void prepare(HashSet<MugClass> done, MugClass mc) {
        if (done.contains(mc))
            return;

        MugClass pc = classes.get(mc.getSuperClassName());
        if (pc != null && !done.contains(pc))
            prepare(done, pc);

        mc.registerFieldNames();
        mc.registerMethodNames();
        done.add(mc);

    }

    MugClass getMugClass(String cn) {
        return classes.get(cn);
    }

    /**
     * Method addClass.
     * 
     * @param fileInputStream
     */
    void addClass(FileInputStream fileInputStream) throws Exception {
        MugClass mc = new MugClass(this, fileInputStream);
        String cn = mc.getClassName();
        ps.println(cn);
        classes.put(cn, mc);
    }

    MugClass getSuperClass(String cn) {
        MugClass mc = getMugClass(cn);
        if (mc == null)
            return null;
        mc = getMugClass(mc.getSuperClassName());
        return mc;
    }
}
/**
 * Log: $Log: Mug.java,v $
 * Log: Revision 1.2  2011/08/31 06:30:07  bebbo
 * Log: @B fixes #3
 * Log:  * inner classes are now added correctly
 * Log: Log: Revision 1.1 2011/08/30 11:38:31 bebbo Log: @I renamed package to de.bb.tools.mug Log: @N
 * added package renaming capabilities Log: Log: Revision 1.2 2011/08/29 16:56:00 bebbo Log: @I switch to JDK 1.6 Log:
 * Log: Revision 1.1 2011/08/29 16:31:58 bebbo Log: @N added to BNM build Log: Revision 1.4 2005/12/01 11:29:17 bebbo
 * Log: @I switched from Hashtable to HashMap Log: @B fixed crippling of method names Log: @B apply class hierarchie to
 * define crippling order Log: Revision 1.3 2003/01/20 08:11:50 bebbo Log: @B existing method and function names are
 * correctly preserved and no longer used twice. Log: Revision 1.2 2002/11/22 21:23:31 bebbo Log: @B fixed the unused
 * entries. Also started method code parsing. Log: Revision 1.1 2002/11/18 12:00:12 bebbo Log: @N first version
 */
