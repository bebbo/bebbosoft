package de.bb.jcc.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import de.bb.jcc.ClassDefinition;
import de.bb.jcc.Code;
import de.bb.jcc.Util;

/* 
 * Created on 04.04.2005
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */

/**
 * Converts a Java asm file into a class file.
 * @author sfranke
 */
public class Jasm
{
  private final static Class NOCLASS[] = {};

  private static boolean addStackMap;

  private static final String USAGE = "USAGE: jasm <infile> [<outfile>]";

  private static HashMap varMap = new HashMap();
  private static int paramIndex = 0; 
  
  /**
   * @param args
   */
  public static void main(String[] args)
  {
    try {
      args = doOptions(args);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
      return;
    }
    String jasmFileName = args[0];
    System.out.println("reading " + jasmFileName);
    int lDot = jasmFileName.lastIndexOf('.');
    String classFileName = jasmFileName.substring(0, lDot + 1) + "class";
    if (args.length > 1)
      classFileName = args[1];
    System.out.println("writing " + classFileName);
    int lineNo = 0;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(
          new FileInputStream(jasmFileName)));

      int level = 0;
      ClassDefinition cd = null;
      Code code = null;

      String methodModifier = null, methodName = null, methodSignature = null;
      ArrayList ex = new ArrayList();

      String line = "";
      String cmd = "";
      for (;;) {
        /*        
         if (level == 2 && cmd.length() > 0) {
         System.out.println(lineNo + ": " + cmd + " ; " + code.getCurrentStackSize());
         cmd = "";
         }
         */
        line = br.readLine();

        if (line == null)
          break;
        
        ++lineNo;
        line = line.trim();
        if ("{".equals(line)) {
          ++level;
          if (level == 1 && cd == null)
            missing("class declaration", lineNo);
          continue;
        }

        if ("}".equals(line)) {
          --level;
          if (level == 0)
            break;
          if (level == 1) {
            if (methodName == null || methodSignature == null || code == null)
              missing("method definition", lineNo);
            else {              
              cd.defineMethod(methodModifier, methodName, methodSignature,
                  code, (String[]) ex.toArray(new String[ex.size()]));
            }
            methodName = null;
            methodSignature = null;
            code = null;
            ex.clear();
          }
          continue;
        }

        if (level == 0) {
          int c = line.indexOf("class");
          if (c >= 0) {
            while (line.indexOf("{") < 0) {
              line += " " + br.readLine().trim();
              ++lineNo;
            }
            if (line.trim().endsWith("{"))
              level = 1;
            line = line.substring(0, line.length() - 1).trim();

            String classModifier = line.substring(0, c).trim();
            line = line.substring(c + 5).trim();

            int i = line.indexOf("implements");
            String imp = "";
            if (i >= 0) {
              imp = line.substring(i + 10).trim();
              line = line.substring(0, i).trim();
            }
            String parent = "java.lang.Object";
            int e = line.indexOf("extends");
            if (e > 0) {
              parent = line.substring(e + 7).trim();
              line = line.substring(0, e).trim();
            }

            cd = new ClassDefinition(classModifier, line, parent);
            if (addStackMap)
              cd.addStackMap();

            for (StringTokenizer st = new StringTokenizer(imp); st
                .hasMoreElements();) {
              String iface = st.nextToken();
              cd.addInterface(iface);
            }
          }
          continue;
        }

        if (level == 1) {
          int bra = line.indexOf('(');
          if (bra > 0) {
            while (line.indexOf("{") < 0) {
              line += " " + br.readLine().trim();
              ++lineNo;
            }
            int space = bra;
            while (space > 0 && line.charAt(space - 1) <= 32)
              --space;
            int end = space;
            while (space > 0 && line.charAt(space - 1) > 32)
              --space;
            int start = space;

            methodName = line.substring(start, end);
            methodSignature = null;

            while (space > 0 && line.charAt(space - 1) <= 32)
              --space;
            end = space;
            while (space > 0 && line.charAt(space - 1) > 32)
              --space;
            start = space;
            if (start < end) {
              String s = line.substring(start, end);
              if (!"public".equals(s) && !"private".equals(s)
                  && !"protected".equals(s) && !"static".equals(s)
                  && !"final".equals(s) && !"synchronized".equals(s)) {
                methodSignature = s;
                while (space > 0 && line.charAt(space - 1) <= 32)
                  --space;
                end = space;
              }
              methodModifier = line.substring(0, end).trim();
            }
            code = cd.createCode();
            int ket = line.indexOf(')', bra);
            if (methodSignature == null) {
              methodName = "<init>";
              methodSignature = "void";
            }
            
            varMap = new HashMap();
            paramIndex = 0;

            methodSignature = params2Signature(line.substring(bra + 1, ket),
                methodSignature);
          }
          if (line.trim().endsWith("{"))
            level = 2;
          continue;
        }

        line = line.trim();
        // handle the mnemonics.
        // [label:] menmonic [params] [ ; comment]
        int semi = line.indexOf(';');
        if (semi == 0)
          continue;
        if (semi > 0) {
          while (semi > 0 && line.charAt(semi - 1) > 32) {
            semi = line.indexOf(';', semi + 1);
          }
        }
        if (semi > 0)
          line = line.substring(0, semi).trim();

        // label?
        int colon = line.indexOf(':');
        if (colon > 0) {
          String label = line.substring(0, colon);
          int i = 0;
          for (; i < label.length(); ++i) {
            if (label.charAt(i) <= 32)
              break;
          }
          if (i == label.length()) {
            if (!code.addLabel(label))
              error("Label " + label + " is already defined", lineNo);
            line = line.substring(colon + 1).trim();
          }
        }
        if (line.length() == 0)
          continue;

        int sot = nextSpaceTabComma(line);

        cmd = line.substring(0, sot);
        line = line.substring(sot).trim();

        if (cmd.startsWith("aload")) {
          code.c_aload(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("iload")) {
          code.c_iload(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("fload")) {
          code.c_fload(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("dload")) {
          code.c_dload(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("lload")) {
          code.c_lload(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("astore")) {
          code.c_astore(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("istore")) {
          code.c_istore(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("fstore")) {
          code.c_fstore(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("lstore")) {
          code.c_lstore(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.startsWith("dstore")) {
          code.c_dstore(getVal(varMap, cmd, line));
          continue;
        }

        if (cmd.equals("return")) {
          code.c_return();
          continue;
        }

        if (cmd.startsWith("iconst")) {
          if ("iconst_m1".equals(cmd))
            code.c_iconst(-1);
          else
            code.c_iconst(getVal(cmd, line));
          continue;
        }

        if (cmd.equals("bipush")) {
          code.c_iconst((byte) getInt(line));
          continue;
        }
        if (cmd.equals("sipush")) {
          code.c_iconst((short) getInt(line));
          continue;
        }

        if (cmd.startsWith("fconst")) {
          code.c_fconst(getFloat(cmd.substring(7)));
          continue;
        }

        if (cmd.equals("ldc") || cmd.equals("ldc_w")) {
          if (line.toLowerCase().endsWith("l")) {
            code.c_lconst(getLong(line));
            continue;
          }
          if (line.toLowerCase().endsWith("f")) {
            code.c_fconst(getFloat(line));
            continue;
          }
          if (line.toLowerCase().endsWith("d")) {
            code.c_dconst(getDouble(line));
            continue;
          }
          if (line.endsWith("\"")) {
            code.c_ldc(line.substring(1, line.length() - 1));
            continue;
          }
          code.c_iconst(getInt(line));
          continue;

        }

        if (cmd.equals("dconst_0")) {
          code.c_dconst(0);
          continue;
        }
        if (cmd.equals("dconst_1")) {
          code.c_dconst(1);
          continue;
        }
        if (cmd.equals("dconst")) {
          code.c_dconst(getDouble(line));
          continue;
        }

        if (cmd.equals("lconst_0")) {
          code.c_lconst(0L);
          continue;
        }

        if (cmd.equals("lconst_1")) {
          code.c_lconst(1L);
          continue;
        }

        if (cmd.equals("lconst") || cmd.equals("ldc2") || cmd.equals("ldc2_w")) {
          if (line.toLowerCase().endsWith("d")) {
            code.c_dconst(getDouble(line));
            continue;
          }
          code.c_lconst(getLong(line));
          continue;
        }

        if (cmd.equals("iinc")) {
          sot = nextSpaceTabComma(line);
          String index = line.substring(0, sot);
          line = line.substring(sot < line.length() ? sot + 1 : sot).trim();
          if (index.endsWith(","))
            index = index.substring(0, index.length() - 1);
          code.c_iinc(getVal(varMap, "", index), getInt(line));
          continue;
        }

        if (cmd.equals("ifeq")) {
          code.c_ifeq(line);
          continue;
        }
        if (cmd.equals("ifne")) {
          code.c_ifne(line);
          continue;
        }
        if (cmd.equals("iflt")) {
          code.c_iflt(line);
          continue;
        }
        if (cmd.equals("ifge")) {
          code.c_ifge(line);
          continue;
        }
        if (cmd.equals("ifgt")) {
          code.c_ifgt(line);
          continue;
        }
        if (cmd.equals("ifle")) {
          code.c_ifle(line);
          continue;
        }

        if (cmd.equals("if_icmpeq")) {
          code.c_if_icmpeq(line);
          continue;
        }
        if (cmd.equals("if_icmpne")) {
          code.c_if_icmpne(line);
          continue;
        }
        if (cmd.equals("if_icmplt")) {
          code.c_if_icmplt(line);
          continue;
        }
        if (cmd.equals("if_icmpge")) {
          code.c_if_icmpge(line);
          continue;
        }
        if (cmd.equals("if_icmpgt")) {
          code.c_if_icmpgt(line);
          continue;
        }
        if (cmd.equals("if_icmple")) {
          code.c_if_icmple(line);
          continue;
        }
        if (cmd.equals("if_acmpeq")) {
          code.c_if_acmpeq(line);
          continue;
        }
        if (cmd.equals("if_acmpne")) {
          code.c_if_acmpne(line);
          continue;
        }

        if (cmd.equals("goto")) {
          code.c_goto(line);
          continue;
        }

        if (cmd.equals("jsr")) {
          code.c_jsr(line);
          continue;
        }
        if (cmd.equals("ret")) {
          code.c_ret(getInt(line));
          continue;
        }
        if (cmd.equals("bytecode")) {
          code.emit(getInt(line));
          continue;
        }

        if (cmd.equals("getstatic")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          code.c_getstatic(line.substring(0, dot),
              line.substring(dot + 1, col), line.substring(col + 1));
          continue;
        }
        if (cmd.equals("putstatic")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          code.c_putstatic(line.substring(0, dot),
              line.substring(dot + 1, col), line.substring(col + 1));
          continue;
        }
        if (cmd.equals("getfield")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          code.c_getfield(line.substring(0, dot), line.substring(dot + 1, col),
              line.substring(col + 1));
          continue;
        }
        if (cmd.equals("putfield")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          code.c_putfield(line.substring(0, dot), line.substring(dot + 1, col),
              line.substring(col + 1));
          continue;
        }
        if (cmd.equals("invokespecial")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          int bra = col + 1;
          if (bra == 0) 
            col = bra = line.indexOf('(');
          code.c_invokespecial(line.substring(0, dot), line.substring(dot + 1,
              col), line.substring(bra));
          continue;
        }
        if (cmd.equals("invokevirtual")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          int bra = col + 1;
          if (bra == 0) 
            col = bra = line.indexOf('(');
          code.c_invokevirtual(line.substring(0, dot), line.substring(dot + 1,
              col), line.substring(bra));
          continue;
        }
        if (cmd.equals("invokeinterface")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          int bra = col + 1;
          if (bra == 0) 
            col = bra = line.indexOf('(');
          code.c_invokeinterface(line.substring(0, dot), line.substring(
              dot + 1, col), line.substring(bra));
          continue;
        }
        if (cmd.equals("invokestatic")) {
          int dot = line.indexOf('.');
          int col = line.indexOf(':');
          int bra = col + 1;
          if (bra == 0) 
            col = bra = line.indexOf('(');
          code.c_invokestatic(line.substring(0, dot), line.substring(dot + 1,
              col), line.substring(bra));
          continue;
        }

        if (cmd.equals("new")) {
          code.c_new(line);
          continue;
        }

        if (cmd.equals("newarray") || cmd.equals("anewarray")) {
          code.c_anewarray(line);
          continue;
        }

        if (cmd.equals("checkcast")) {
          code.c_checkcast(line);
          continue;
        }

        if (cmd.equals("instanceof")) {
          code.c_instanceof(line);
          continue;
        }

        if (cmd.equals("multianewarray")) {
          error("not implemented: " + cmd, lineNo);
          continue;
        }

        if (cmd.equals("ifnull")) {
          code.c_ifnull(line);
          continue;
        }
        if (cmd.equals("ifnonnull")) {
          code.c_ifnonnull(line);
          continue;
        }
        
        if (cmd.equals("var")) {
          int space = line.indexOf(' ');
          if (space > 0)
          {
            String sn = line.substring(space + 1).trim();
            line = line.substring(0, space);
            varMap.put(line, new Integer(getInt(sn)));
            continue;
          }
          varMap.put(line, new Integer(paramIndex++));
          continue;
        }

        Method m = Code.class.getMethod("c_" + cmd, NOCLASS);
        if (m != null) {
          m.invoke(code, null);
        }
      }
      
      FileOutputStream fos = new FileOutputStream(classFileName);
      cd.write(fos);
      fos.close();

      System.out.println(cd);      
      
    } catch (Exception e) {
      System.out.println("FATAL (" + lineNo + "): " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * @param line
   * @return
   */
  private static long getLong(String line)
  {
    if (line.toLowerCase().endsWith("l"))
      line = line.substring(0, line.length() - 1);
    int radix = 10;
    if (line.toLowerCase().endsWith("h")) {
      radix = 16;
      line = line.substring(0, line.length() - 1);
    }
    return Long.parseLong(line, radix);
  }

  /**
   * @param line
   * @return
   */
  private static float getFloat(String line)
  {
    if (line.toLowerCase().endsWith("f"))
      line = line.substring(0, line.length() - 1);

    return Float.parseFloat(line);
  }

  /**
   * @param line
   * @return
   */
  private static double getDouble(String line)
  {
    if (line.toLowerCase().endsWith("d"))
      line = line.substring(0, line.length() - 1);

    return Double.parseDouble(line);
  }

  /**
   * @param line
   * @return
   */
  private static int getInt(String line)
  {
    int radix = 10;
    if (line.toLowerCase().endsWith("h")) {
      radix = 16;
      line = line.substring(0, line.length() - 1);
    }
    return Integer.parseInt(line, radix);
  }

  /**
   * @param line
   * @return
   */
  private static int nextSpaceTabComma(String line)
  {
    int space = line.indexOf(' ');
    int tab = line.indexOf('\t');
    int comma = line.indexOf(',');
    if (space < 0)
      space = line.length();
    if (tab < 0)
      tab = line.length();
    if (comma < 0)
      comma = line.length();
    if (tab < space && tab < comma)
      return tab;
    if (comma < space)
      return comma;
    return space;
  }

  /**
   * @param cmd
   * @param line
   * @return
   */
  private static int getVal(String cmd, String line)
  {
    int u = cmd.indexOf('_');
    if (u > 0)
      return Integer.parseInt(cmd.substring(u + 1));

    int radix = 10;
    if (line.toLowerCase().endsWith("h")) {
      line = line.substring(0, line.length() - 1);
      radix = 16;
    }
    return (int)Long.parseLong(line, radix);
  }

  /**
   * @param map
   * @param cmd
   * @param line
   * @return
   */
  private static int getVal(HashMap map, String cmd, String line)
  {
    int u = cmd.indexOf('_');
    if (u > 0)
      return Integer.parseInt(cmd.substring(u + 1));
    
    if (Character.isJavaIdentifierStart(line.charAt(0)))
    {
      Integer i = (Integer)map.get(line);
      if (i == null)
        throw new RuntimeException("undefined variable: " + line);
      return i.intValue();
    }
    
    int radix = 10;
    if (line.toLowerCase().endsWith("h")) {
      line = line.substring(0, line.length() - 1);
      radix = 16;
    }
    return Integer.parseInt(line, radix);
  }

  /**
   * @param string
   * @param line
   */
  private static void missing(String string, int line)
  {
    System.out.println("ERROR (" + line + "): missing " + string);
  }

  /**
   * @param string
   * @param line
   */
  private static void error(String string, int line)
  {
    System.out.println("ERROR (" + line + "): " + string);
  }

  /**
   * parse the command line for options and return other parameters.
   * @param args
   * @return
   * @throws Exception
   */
  private static String[] doOptions(String args[]) throws Exception
  {
    int j = 0;
    for (int i = 0; i < args.length; ++i) {
      if (args[i].charAt(0) != '-') // no argument
      {
        args[j++] = args[i];
        continue;
      }
      String o = args[i];
      // an argument
      if (o.equals("-?")) {
        throw new Exception(USAGE); // show usage only!
      }
      /*
       if (o.equals("-s")) {
       addStackMap = true;
       continue;
       }
       */
      throw new Exception("Invalid option '" + args[i - 1] + "'");
    }

    if (j == 0)
      throw new Exception(USAGE); // show usage only!

    String res[] = new String[j];
    System.arraycopy(args, 0, res, 0, j);
    return res;
  }

  /**
   * Create the signature from params and return code.
   * @param map
   * @param params parameter list without variable names
   * @param ret return type
   * @return
   */
  static String params2Signature(String params, String ret)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    for (StringTokenizer st = new StringTokenizer(params, ","); st
        .hasMoreElements();) {
      String param = st.nextToken().trim();
      int space = param.indexOf(" ");
      if (space > 0) {
        String rest = param.substring(space + 1);
        int s = 0;
        for (; s < rest.length(); ++s) {
          if ("[] ".indexOf(rest.charAt(s)) < 0)
            break;
        }
        int e = rest.length();
        for (; e > s; --e) {
          if ("[] ".indexOf(rest.charAt(e - 1)) < 0)
            break;
        }
        String var = rest.substring(s, e);
        varMap.put(var, new Integer(paramIndex));
        param = param.substring(0, space + 1 + s)
            + param.substring(space + 1 + e);
      }
      param = param.trim();
      sb.append(Util.type2Signature(param));
      ++paramIndex;
      if ("long".equals(param) || "double".equals(param))
        ++paramIndex;
    }
    sb.append(")");
    sb.append(Util.type2Signature(ret));
    return sb.toString();
  }

}