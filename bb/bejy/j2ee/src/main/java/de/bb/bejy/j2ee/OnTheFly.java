package de.bb.bejy.j2ee;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.bb.jcc.ClassDefinition;
import de.bb.jcc.Code;

/**
 * @author bebbo
 */
class OnTheFly {
    private final static boolean DEBUG = false;

    /**
     * Method mkstub. Creates the Ying
     * 
     * @param className
     */
    static byte[] mkstub(String className) throws Exception {
        ClassDefinition cd = null;
        Class<?> clazz;
        clazz = Class.forName(className);
        /*
         String ocName = className;
         int dot = className.lastIndexOf('.');
         if (dot >= 0)
         {
         ocName = className.substring(dot + 1);
         }
         */
//        Class<?> parent = clazz.getSuperclass();

        // parent class is ALWAYS the RemoteRef!!!
        String parentClassName = "de.bb.rmi.RemoteRef";
        /*
         String parentClassName = null;
         for (; parent != null; parent = parent.getSuperclass())
         {
         String pClassName = parent.getName();
         if (pClassName.endsWith("Bean"))
         {
         parentClassName = className;
         break;
         }
         }
         if (parent == null)
         {
         parentClassName = "de.bb.rmi.RemoteRef";
         }
         */
        // create class
        cd = new ClassDefinition(className + "_Ying", parentClassName);

        Class<?> ifaces[] = clazz.getInterfaces();
        for (int i = 0; i < ifaces.length; ++i) {
            Class<?> iface = ifaces[i];
            if (className.startsWith(iface.getName())) {
                cd.addInterface(iface.getName());
                break;
            }
        }

        // add constructor
        Code ct = cd.createCode();
        ct.c_aload(0);
        ct.c_invokespecial(parentClassName, "<init>", "()V");
        ct.c_return();
        cd.defineMethod(0x0001, "<init>", "()V", ct);

        HashMap<String, Method> hm = new HashMap<String, Method>();
        collectRemoteMethods(hm, clazz);

        int fxNo = 0;

        for (Iterator<Method> i = hm.values().iterator(); i.hasNext();) {
            Method m = i.next();
            addYingMethod(className, cd, m, ++fxNo);
        }

        // create functions
        /*
            Method mts[] = clazz.getDeclaredMethods();
            for (int i = 0; i < mts.length; ++i) {
              Method m = mts[i];
              Class exs[] = m.getExceptionTypes();
              if (exs.length != 1)
                continue;
              if (!"java.rmi.RemoteException".equals(exs[0].getName()))
                continue;

              addYingMethod(className, cd, m, ++fxNo);
            }
        */
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        cd.write(bos);
        bos.close();
        byte b[] = bos.toByteArray();

        if (DEBUG) {
            FileOutputStream fos = new FileOutputStream(className + "_Ying.class");
            fos.write(b);
            fos.close();
        }
        return b;
    }

    /**
     * @param cd
     * @param m
     */
    private static void addYingMethod(String className, ClassDefinition cd, Method m, int fxNo) {
        Class<?> rType = m.getReturnType();
        String rName = rType.getName();
        String mName = m.getName();
        Class<?> params[] = m.getParameterTypes();

        // add a function
        Code fx = cd.createCode();
        fx.c_aload(0);
        fx.c_getfield("de/bb/rmi/RemoteRef", "client", "Lde/bb/rmi/RemoteClient;");
        fx.c_ldc(className);
        fx.c_aload(0);
        fx.c_getfield("de/bb/rmi/RemoteRef", "oid", "J");

        if (rType.isArray())
            rName = cvtArray(rName);
        String fxParam = "";
        for (int j = 0; j < params.length; ++j) {
            Class<?> param = params[j];
            String pName = param.getName();

            fxParam += type2Raw(pName);

            if (param.isArray())
                pName = cvtArray(pName);
        }

        if (!rName.equals("void")) {
            if (rType.isPrimitive()) {
                rName = cvtType(rName);
            }
        }

        fx.c_iconst(fxNo);

        int pno = 1;
        if (params.length > 0) {
            fx.c_iconst(params.length);
            fx.c_anewarray("java/lang/Object");

            /* verifier complains about dup2 for no long ...
             if (params.length > 0)
             fx.c_dup();
             for (int j = 1; j + 1 < params.length; j += 2)
             {
             fx.c_dup2();
             }
             if (params.length % 2 == 0)
             fx.c_dup();
             */
            for (int j = 0; j < params.length; ++j) {
                fx.c_dup();
            }

            for (int j = 0; j < params.length; ++j) {
                fx.c_iconst(j);

                Class<?> param = params[j];
                String pName = param.getName();
                if (param.isPrimitive()) {
                    fx.c_new(cvtType(pName));
                    fx.c_dup();

                    if (pName.equals("long")) {
                        fx.c_lload(pno);
                        pno += 2;
                    } else if (pName.equals("double")) {
                        fx.c_dload(pno);
                        pno += 2;
                    } else if (pName.equals("float")) {
                        fx.c_fload(pno++);
                    } else {
                        fx.c_iload(pno++);
                    }

                    pName = cvtType(pName);

                    fx.c_invokespecial(pName, "<init>", "(" + type2Raw(param.getName()) + ")V");

                } else {
                    if (param.isArray())
                        pName = cvtArray(pName);
                    fx.c_aload(pno++);
                }

                fx.c_aastore();
            }

            fx.c_invokevirtual("de/bb/rmi/RemoteClient", "invoke",
                    "(Ljava/lang/String;JI[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
            fx.c_invokevirtual("de/bb/rmi/RemoteClient", "invoke", "(Ljava/lang/String;JI)Ljava/lang/Object;");
        }

        String rawName = type2Raw(rType.getName());

        if (rType.isPrimitive()) {
            if (!rName.equals("void")) {
                fx.c_checkcast(rName);
                fx.c_invokevirtual(rName, rType.getName() + "Value", "()" + rawName);
                if ("BZSCI".indexOf(rawName) >= 0)
                    fx.c_ireturn();
                else if ("L".indexOf(rawName) >= 0)
                    fx.c_lreturn();
                else
                    fx.c_dreturn();
            } else {
                fx.c_pop();
                fx.c_return();
            }
        } else {
            if (rType.isArray())
                fx.c_checkcast(rawName);
            else
                fx.c_checkcast(rName);
            fx.c_areturn();
        }

        cd.defineMethod(0x0001, mName, "(" + fxParam + ")" + rawName, fx, new String[] { "java/rmi/RemoteException" });

    }

    /**
     * Method type2Raw.
     * 
     * @param pName
     * @return String
     */
    private static String type2Raw(String pName) {
        if (pName.charAt(0) == '[') {
            return pName;
        }
        if ("int".equals(pName)) {
            return "I";
        }
        if ("long".equals(pName)) {
            return "J";
        }
        if ("float".equals(pName)) {
            return "F";
        }
        if ("double".equals(pName)) {
            return "D";
        }
        if ("byte".equals(pName)) {
            return "B";
        }
        if ("char".equals(pName)) {
            return "C";
        }
        if ("short".equals(pName)) {
            return "S";
        }
        if ("boolean".equals(pName)) {
            return "Z";
        }
        if ("void".equals(pName))
            return "V";
        return "L" + pName + ";";
    }

    /**
     * Method mkskel. Create the Yang.
     * 
     * @param className
     */
    static byte[] mkskel(String className) throws Exception {
        ClassDefinition cd = null;
        Class<?> clazz;
        clazz = Class.forName(className);

        //    String ocName = className;
        //    int dot = className.lastIndexOf('.');
        //    if (dot >= 0)
        {
            //      ocName = className.substring(dot + 1);
        }

        String ocBaseName = className.substring(0, className.length() - 4);

        // create class
        cd = new ClassDefinition(className + "_Yang", "java/lang/Object");
        cd.addInterface("de/bb/rmi/ISkeleton");

        // add constructor
        Code ct = cd.createCode();
        ct.c_aload(0);
        ct.c_invokespecial("java/lang/Object", "<init>", "()V");
        ct.c_return();
        cd.defineMethod(0x0001, "<init>", "()V", ct);

        Code fx = cd.createCode();
        fx.c_aload(1);
        fx.c_checkcast(ocBaseName);
        fx.c_astore(4);

        HashMap<String, Method> hm = new HashMap<String, Method>();
        collectRemoteMethods(hm, clazz);

        ArrayList<Code> cases = new ArrayList<Code>();
        int fxNo = 0;

        for (Iterator<Method> i = hm.values().iterator(); i.hasNext();) {
            Method m = i.next();
            addYangMethod(ocBaseName, cd, cases, m, ++fxNo);
        }
        /*    
            Method mts[] = clazz.getDeclaredMethods();
            for (int i = 0; i < mts.length; ++i) {
              Method m = mts[i];
              Class exs[] = m.getExceptionTypes();
              if (exs.length != 1)
                continue;
              if (!"java.rmi.RemoteException".equals(exs[0].getName()))
                continue;

              addYangMethod(ocBaseName, cd, cases, m, ++fxNo);
            }
        */
        fx.c_iload(2);
        fx.c_tableswitch(1, cases, null);

        fx.c_new("java/lang/Exception");
        fx.c_dup();
        fx.c_ldc("invalid function index");
        fx.c_invokespecial("java/lang/Exception", "<init>", "(Ljava/lang/String;)V");
        fx.c_athrow();

        cd.defineMethod(0x0001, "invoke", "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;", fx,
                new String[] { "java/lang/Exception" });

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        cd.write(bos);
        bos.close();
        byte b[] = bos.toByteArray();

        if (DEBUG) {
            FileOutputStream fos = new FileOutputStream(className + "_Yang.class");
            fos.write(b);
            fos.close();
        }
        return b;
    }

    /**
     * @param hm
     * @param clazz
     */
    private static void collectRemoteMethods(HashMap<String, Method> hm, Class<?> clazz) {
        Class<?>[] ifaces = clazz.getInterfaces();
        for (int i = 0; i < ifaces.length; ++i) {
            Class<?> iface = ifaces[i];
            collectRemoteMethods(hm, iface);

            Method[] methods = iface.getDeclaredMethods();
            for (int j = 0; j < methods.length; ++j) {
                Method m = methods[j];

                // at least java.rmi.RemoteException must be thrown
                Class<?> exs[] = m.getExceptionTypes();
                int k = 0;
                for (; k < exs.length; ++k) {
                    if ("java.rmi.RemoteException".equals(exs[k].getName()))
                        break;
                }
                if (k == exs.length)
                    continue;

                StringBuffer sb = new StringBuffer();
                sb.append(m.getName());
                sb.append(';');
                Class<?>[] ps = m.getParameterTypes();
                for (k = 0; k < ps.length; ++k) {
                    Class<?> p = ps[k];
                    sb.append(p.getName());
                    sb.append(',');
                }

                String key = sb.toString();
                if (hm.get(key) == null)
                    hm.put(key, m);
            }
        }
    }

    /**
     * @param ocBaseName
     * @param cd
     * @param cases
     * @param m
     * @param fxNo
     */
    private static void addYangMethod(String ocBaseName, ClassDefinition cd, ArrayList<Code> cases, Method m, int fxNo) {
        Code sx = cd.createCode();
        cases.add(sx);
        String pSig = "";

        Class<?> rType = m.getReturnType();
        String mName = m.getName();
        Class<?> params[] = m.getParameterTypes();

        String rName = rType.getName();
        String rawRName = rName;
        if (rType.isPrimitive()) {
            if (!rName.equals("void")) {
                rName = cvtType(rName);
            }
        } else {
            if (rType.isArray())
                rName = cvtArray(rName);
        }

        String rawName = type2Raw(rType.getName());
        if (rType.isPrimitive() && !rName.equals("void")) {
            sx.c_new(rName);
            sx.c_dup();
        }

        sx.c_aload(4);

        for (int j = 0; j < params.length; ++j) {
            Class<?> param = params[j];
            String pName = param.getName();

            sx.c_aload(3);
            sx.c_iconst(j);
            sx.c_aaload();

            if (param.isPrimitive()) {
                pName = cvtType(pName);
            }
            sx.c_checkcast(pName);
            if (param.isArray()) {
                pName = cvtArray(pName);
            }

            if (param.isPrimitive()) {
                String rpn = type2Raw(param.getName());
                sx.c_invokevirtual(pName, param.getName() + "Value", "()" + rpn);
                pSig += rpn;
            } else if (param.isArray()) {
                pSig += param.getName();
            } else {
                pSig += "L" + pName + ";";
            }
        }

        String type = "(" + pSig + ")" + type2Raw(rawRName);
        sx.c_invokeinterface(ocBaseName, mName, type);

        if (rType.isPrimitive() && !rName.equals("void")) {
            sx.c_invokespecial(rName, "<init>", "(" + rawName + ")V");
        }

        if (rName.equals("void")) {
            sx.c_aconst_null();
        }

        sx.c_areturn();
    }

    /**
     * Method cvtType.
     * 
     * @param pName
     * @return String
     */
    private static String cvtType(String pName) {
        if (pName.equals("int"))
            return "java.lang.Integer";
        if (pName.equals("char"))
            return "java.lang.Character";
        return "java.lang." + pName.substring(0, 1).toUpperCase() + pName.substring(1);
    }

    /**
     * Method cvtArray.
     * 
     * @param tName
     * @return String
     */
    private static String cvtArray(String tName) {
        String r = "";
        while (tName.charAt(0) == '[') {
            tName = tName.substring(1);
            r += "[]";
        }
        switch (tName.charAt(0)) {
        case 'L':
            r = tName.substring(1, tName.length() - 1) + r;
            break;
        case 'B':
            r = "byte" + r;
            break;
        case 'C':
            r = "char" + r;
            break;
        case 'S':
            r = "short" + r;
            break;
        case 'I':
            r = "int" + r;
            break;
        case 'J':
            r = "long" + r;
            break;
        case 'F':
            r = "float" + r;
            break;
        case 'D':
            r = "double" + r;
            break;
        case 'Z':
            r = "boolean" + r;
            break;
        }

        return r;
    }
}