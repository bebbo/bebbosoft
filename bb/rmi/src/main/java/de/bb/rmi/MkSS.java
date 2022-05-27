package de.bb.rmi;

import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.bb.jcc.ClassDefinition;
import de.bb.jcc.Code;

/**
 * @author bebbo
 */
public class MkSS
{
  /**
   * @param args
   */
  public static void main(String[] args)
  {
    for (int i = 0; i < args.length; ++i)
    {
      mkstub(args[i]);
      mkskel(args[i]);
    }
  }

  /**
   * Method mkstub.
   * @param className
   */
  static void mkstub(String className)
  {
    ClassDefinition cd = null;
    Class clazz;
    try
    {
      clazz = Class.forName(className);

      String ocName = className;
      int dot = className.lastIndexOf('.');
      if (dot >= 0)
      {
        emit("package " + className.substring(0, dot) + ";\r\n");
        ocName = className.substring(dot + 1);
      }

      // create class body      
      emit("public class " + ocName + "_Stub extends ");

      Class parent = clazz.getSuperclass();
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

      emit(parentClassName);

      // create class
      cd = new ClassDefinition(className + "_Stub", parentClassName);

      Class ifaces[] = clazz.getInterfaces();
      for (int i = 0; i < ifaces.length; ++i)
      {
        Class iface = ifaces[i];
        if (className.startsWith(iface.getName()))
        {
          emit(" implements " + iface.getName());
          cd.addInterface(iface.getName());
          break;
        }
      }
      emit("\r\n");
      emit("{\r\n");

      // add constructor
      Code ct = cd.createCode();
      ct.c_aload(0);
      ct.c_invokespecial(parentClassName, "<init>", "()V");
      ct.c_return();
      cd.defineMethod(0x0001, "<init>", "()V", ct);

      // create functions
      Method mts[] = clazz.getDeclaredMethods();
      for (int i = 0; i < mts.length; ++i)
      {
        Method m = mts[i];
        Class exs[] = m.getExceptionTypes();
        if (exs.length != 1)
          continue;
        if (!"java.rmi.RemoteException".equals(exs[0].getName()))
          continue;

        Class rType = m.getReturnType();
        String rName = rType.getName();
        String mName = m.getName();
        Class params[] = m.getParameterTypes();

        // add a function
        Code fx = cd.createCode();
        fx.c_aload(0);
        fx.c_getField("de/bb/rmi/RemoteRef", "client", "Lde/bb/rmi/Client;");
        fx.c_ldc(className);
        fx.c_aload(0);
        fx.c_getField("de/bb/rmi/RemoteRef", "oid", "J");

        if (rType.isArray())
          rName = cvtArray(rName);
        emit("  public " + rName + " " + mName + "(");
        String fxParam = "";
        for (int j = 0; j < params.length; ++j)
        {
          if (j > 0)
            emit(", ");
          Class param = params[j];
          String pName = param.getName();

          fxParam += type2Raw(pName);

          if (param.isArray())
            pName = cvtArray(pName);
          emit(pName + " p" + j);
        }

        emit(") throws java.rmi.RemoteException\r\n");
        emit("  {\r\n    ");

        if (!rName.equals("void"))
        {
          emit("return ");
          if (rType.isPrimitive())
          {
            rName = cvtType(rName);
            emit("(");
          }
          emit("(" + rName + ")");
        }

        emit("client.invoke(\"" + className + "\", oid, " + (i + 1));

        fx.c_bipush(i + 1);

        int pno = 1;
        if (params.length > 0)
        {
          fx.c_bipush(params.length);
          fx.c_anewarray("java/lang/Object");

          emit(", new Object[]{");
          if (params.length > 0)
            fx.c_dup();
          for (int j = 1; j < params.length; j += 2)
          {
            fx.c_dup2();
          }
          if (params.length % 2 == 0)
            fx.c_dup();

          for (int j = 0; j < params.length; ++j)
          {
            fx.c_bipush(j);

            if (j > 0)
              emit(", ");
            Class param = params[j];
            String pName = param.getName();
            if (param.isPrimitive())
            {
              fx.c_new(cvtType(pName));
              fx.c_dup();

              if (pName.equals("long"))
              {
                fx.c_lload(pno);
                pno += 2;
              } else
              if (pName.equals("double"))
              {
                fx.c_dload(pno);
                pno += 2;
              } else
              if (pName.equals("float"))
              {
                fx.c_fload(pno++);
              } else
              {
                fx.c_iload(pno++);
              }

              pName = cvtType(pName);
              emit("new " + pName + "(p" + j + ")");

              fx.c_invokespecial(
                pName,
                "<init>",
                "(" + type2Raw(param.getName()) + ")V");

            } else
            {
              if (param.isArray())
                pName = cvtArray(pName);
              emit("p" + j);
              fx.c_aload(pno++);
            }

            fx.c_aastore();
          }
          emit("}");

          fx.c_invokevirtual(
            "de/bb/rmi/Client",
            "invoke",
            "(Ljava/lang/String;JI[Ljava/lang/Object;)Ljava/lang/Object;");
        } else
        {
          fx.c_invokevirtual(
            "de/bb/rmi/Client",
            "invoke",
            "(Ljava/lang/String;JI)Ljava/lang/Object;");
        }

        emit(")");

        String rawName = type2Raw(rType.getName());

        if (rType.isPrimitive())
        {
          if (!rName.equals("void"))
          {
            emit(")." + rType.getName() + "Value()");

            fx.c_checkcast(rName);
            fx.c_invokevirtual(
              rName,
              rType.getName() + "Value",
              "()" + rawName);
            if ("BZSCI".indexOf(rawName) >= 0)
              fx.c_ireturn();
            else
              fx.c_lreturn();
          } else
          {
            fx.c_pop();
            fx.c_return();
          }
        } else
        {
          if (rType.isArray())
            fx.c_checkcast(rawName);
          else
            fx.c_checkcast(rName);
          fx.c_areturn();
        }
        emit(";\r\n");
        emit("  }\r\n");

        cd.defineMethod( 0x0001,
          mName,
          "(" + fxParam + ")" + rawName,
          fx,
          new String[] { "java/rmi/RemoteException" });
      }

      emit("}\r\n");

      FileOutputStream fos = new FileOutputStream(ocName + "_Stub.class");
      cd.write(fos);
      fos.close();

    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  /**
   * Method type2Raw.
   * @param pName
   * @return String
   */
  private static String type2Raw(String pName)
  {
    if (pName.charAt(0) == '[')
    {
      return pName;
    }
    if ("int".equals(pName))
    {
      return "I";
    }
    if ("long".equals(pName))
    {
      return "J";
    }
    if ("float".equals(pName))
    {
      return "F";
    }
    if ("double".equals(pName))
    {
      return "D";
    }
    if ("byte".equals(pName))
    {
      return "B";
    }
    if ("char".equals(pName))
    {
      return "C";
    }
    if ("short".equals(pName))
    {
      return "S";
    }
    if ("boolean".equals(pName))
    {
      return "Z";
    }
    if ("void".equals(pName))
      return "V";
    return "L" + pName + ";";
  }
  /**
   * Method mkskel.
   * @param className
   */
  static void mkskel(String className)
  {
    ClassDefinition cd = null;
    Class clazz;
    try
    {
      clazz = Class.forName(className);

      String ocName = className;
      int dot = className.lastIndexOf('.');
      if (dot >= 0)
      {
        emit("package " + className.substring(0, dot) + ";\r\n");
        ocName = className.substring(dot + 1);
      }

      String ocBaseName = className.substring(0, className.length() - 4);

      // create class
      cd = new ClassDefinition(className + "_Skel", "java/lang/Object");
      cd.addInterface("de/bb/rmi/ISkeleton");

      // add constructor
      Code ct = cd.createCode();
      ct.c_aload(0);
      ct.c_invokespecial("java/lang/Object", "<init>", "()V");
      ct.c_return();
      cd.defineMethod(0x0001, "<init>", "()V", ct);

      emit(
        "public class " + ocName + "_Skel implements de.bb.rmi.ISkeleton\r\n");
      emit("{\r\n");
      emit("  public Object invoke(Object ref, int fxId, Object[] params) throws Exception\r\n");
      emit("  {\r\n");
      emit("    " + ocBaseName + " o = (" + ocBaseName + ")ref;\r\n");
      emit("    switch (fxId) {\r\n");

      Code fx = cd.createCode();
      fx.c_aload(1);
      fx.c_checkcast(ocBaseName);
      fx.c_astore(4);

      ArrayList cases = new ArrayList();

      Method mts[] = clazz.getDeclaredMethods();
      int fxNo = 0;
      for (int i = 0; i < mts.length; ++i)
      {
        Method m = mts[i];
        Class exs[] = m.getExceptionTypes();
        if (exs.length != 1)
          continue;
        if (!"java.rmi.RemoteException".equals(exs[0].getName()))
          continue;

        Code sx = cd.createCode();
        cases.add(sx);
        String pSig = "";


        Class rType = m.getReturnType();
        String mName = m.getName();
        Class params[] = m.getParameterTypes();

        ++fxNo;
        emit("      case " + fxNo + ":\r\n        ");

        String rName = rType.getName();
        String rawRName = rName;
        if (rType.isPrimitive())
        {
          if (!rName.equals("void"))
          {
            rName = cvtType(rName);
            emit("return new " + rName + "(");
          }
        } else
        {
          if (rType.isArray())
            rName = cvtArray(rName);
          emit("return ");
        }

        String rawName = type2Raw(rType.getName());
        if (rType.isPrimitive() && !rName.equals("void"))
        {
          sx.c_new(rName);
          sx.c_dup();
        }

        sx.c_aload(4);

        emit("o." + mName + "(");
        for (int j = 0; j < params.length; ++j)
        {
          if (j > 0)
            emit(", ");
          Class param = params[j];
          String pName = param.getName();

          sx.c_aload(3);
          sx.c_bipush(j);
          sx.c_aaload();

          if (param.isPrimitive())
          {
            pName = cvtType(pName);
            emit("(");
          }
          sx.c_checkcast(pName);
          if (param.isArray())
          {
            pName = cvtArray(pName);
          }

          emit("(" + pName + ")params[" + j + "]");

          if (param.isPrimitive())
          {
            String rpn = type2Raw(param.getName());
            emit(")." + param.getName() + "Value()");
            sx.c_invokevirtual(pName, param.getName() + "Value", "()" + rpn);
            pSig += rpn;
          } else if (param.isArray())
          {
            pSig += param.getName();
          } else
          {
            pSig += "L" + pName + ";";
          }
        }

        String type = "(" + pSig + ")" + type2Raw(rawRName);
        sx.c_invokeinterface(ocBaseName, mName, type);

        if (rType.isPrimitive() && !rName.equals("void"))
        {
          emit(")");
          sx.c_invokespecial(rName, "<init>", "(" + rawName + ")V");
        }

        emit(");\r\n");

        if (rName.equals("void"))
        {
          emit("        return null;\r\n");
          sx.c_aconst_null();
        }

        sx.c_areturn();
      }

      fx.c_iload(2);
      fx.c_tableswitch(1, cases, null);

      emit("    }\r\n");
      emit("    throw new Exception(\"invalid function index\");\r\n");
      emit("  }\r\n");
      emit("}\r\n");

      fx.c_new("java/lang/Exception");
      fx.c_dup();
      fx.c_ldc("invalid function index");
      fx.c_invokespecial(
        "java/lang/Exception",
        "<init>",
        "(Ljava/lang/String;)V");
      fx.c_athrow();

      cd.defineMethod(0x0001,
        "invoke",
        "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
        fx,
        new String[]{"java/lang/Exception"});

      FileOutputStream fos = new FileOutputStream(ocName + "_Skel.class");
      cd.write(fos);
      fos.close();

    } catch (Exception e)
    {
    }
  }

  /**
   * Method cvtType.
   * @param pName
   * @return String
   */
  private static String cvtType(String pName)
  {
    if (pName.equals("int"))
      return "java.lang.Integer";
    if (pName.equals("char"))
      return "java.lang.Character";
    return "java.lang."
      + pName.substring(0, 1).toUpperCase()
      + pName.substring(1);
  }
  /**
   * Method cvtArray.
   * @param tName
   * @return String
   */
  private static String cvtArray(String tName)
  {
    String r = "";
    while (tName.charAt(0) == '[')
    {
      tName = tName.substring(1);
      r += "[]";
    }
    switch (tName.charAt(0))
    {
      case 'L' :
        r = tName.substring(1, tName.length() - 1) + r;
        break;
      case 'B' :
        r = "byte" + r;
        break;
      case 'C' :
        r = "char" + r;
        break;
      case 'S' :
        r = "short" + r;
        break;
      case 'I' :
        r = "int" + r;
        break;
      case 'J' :
        r = "long" + r;
        break;
      case 'F' :
        r = "float" + r;
        break;
      case 'D' :
        r = "double" + r;
        break;
      case 'Z' :
        r = "boolean" + r;
        break;
    }

    return r;
  }

  /**
   * Method emit.
   * @param string
   */
  private static void emit(String string)
  {
    System.out.print(string);
  }
}
