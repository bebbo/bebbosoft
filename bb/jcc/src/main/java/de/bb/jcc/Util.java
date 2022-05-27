package de.bb.jcc;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * static helper methods
 * 
 * @author sfranke
 * 
 */
public class Util {
    /**
     * Convert the binary access value into a printable String.
     * 
     * @param access
     *            the binary access value.
     * @return a String with modifiers representing the binary value.
     */
    static String access2Modifier(int access) {
        StringBuffer sb = new StringBuffer();
        if ((access & C.ACC_PUBLIC) != 0)
            sb.append("public ");
        if ((access & C.ACC_PRIVATE) != 0)
            sb.append("private ");
        if ((access & C.ACC_PROTECTED) != 0)
            sb.append("protected ");
        if ((access & C.ACC_STATIC) != 0)
            sb.append("static ");
        if ((access & C.ACC_FINAL) != 0)
            sb.append("final ");
        //    if ( (access & C.ACC_SYNCHRONIZED) != 0) sb.append("synchronized ");
        if ((access & C.ACC_VOLATILE) != 0)
            sb.append("volatile ");
        if ((access & C.ACC_TRANSIENT) != 0)
            sb.append("transient ");
        if ((access & C.ACC_NATIVE) != 0)
            sb.append("native ");
        if ((access & C.ACC_ABSTRACT) != 0)
            sb.append("abstract ");
        return sb.toString();
    }

    /**
     * Convert modifiers int the binary access value.
     * 
     * @param modifiers
     *            a String of modifiers.
     * @return the binary access code.
     */
    static int modifier2Access(String modifiers) {
        int access = 0;
        if (modifiers.indexOf("public") >= 0)
            access |= C.ACC_PUBLIC;
        if (modifiers.indexOf("private") >= 0)
            access |= C.ACC_PRIVATE;
        if (modifiers.indexOf("protected") >= 0)
            access |= C.ACC_PROTECTED;
        if (modifiers.indexOf("static") >= 0)
            access |= C.ACC_STATIC;
        if (modifiers.indexOf("final") >= 0)
            access |= C.ACC_FINAL;
        if (modifiers.indexOf("synchronized") >= 0)
            access |= C.ACC_SYNCHRONIZED;
        if (modifiers.indexOf("volatile") >= 0)
            access |= C.ACC_VOLATILE;
        if (modifiers.indexOf("transient") >= 0)
            access |= C.ACC_TRANSIENT;
        if (modifiers.indexOf("native") >= 0)
            access |= C.ACC_NATIVE;
        if (modifiers.indexOf("abstract") >= 0)
            access |= C.ACC_ABSTRACT;
        return access;
    }

    /**
     * Replace all '.' with '/'. This is a separate method to add backward compatibility if needed. String.replace is
     * not available everywhere.
     * 
     * @param cname
     *            a String with a type in Java syntax. E.g. "java.lang.String"
     * @return a String with the signature syntax. E.g. "java./lang/String"
     */
    static String dot2Slash(String cname) {
        return cname.replace('.', '/');
    }

    /**
     * Replace all '/' with '.' This is a separate method to add backward compatibility if needed. String.replace is not
     * available everywhere.
     * 
     * @param cname
     *            a String with a signature syntax. E.g. "java/lang/String"
     * @return a String with Java syntax. E.g. "java.lang.String"
     */
    static String slash2Dot(String cname) {
        return cname.replace('/', '.');
    }

    static String class2Type(String className) {
        if (className.startsWith("["))
            return signature2Type(className);
        return slash2Dot(className);
    }

    /**
     * Convert an signature into a Java type.
     * 
     * @param signature
     *            An intern type, e.g. "[Ljava/lang/String;"
     * @return the Java type, "java.lang.String []"
     */
    static String signature2Type(String signature) {
        StringBuffer sb = new StringBuffer();
        int pos = 0;

        if (signature.charAt(0) == '(') {
            sb.append(parameterList(signature, null));
            signature = signature.substring(signature.indexOf(')') + 1);
        }

        while (signature.charAt(pos) == '[') {
            ++pos;
        }
        switch (signature.charAt(pos)) {
        case 'L':
            sb.append(slash2Dot(signature.substring(pos + 1, signature.length() - 1)));
            break;
        case 'I':
            sb.append("int");
            break;
        case 'F':
            sb.append("float");
            break;
        case 'J':
            sb.append("long");
            break;
        case 'D':
            sb.append("double");
            break;
        case 'B':
            sb.append("byte");
            break;
        case 'Z':
            sb.append("boolean");
            break;
        case 'S':
            sb.append("short");
            break;
        case 'C':
            sb.append("char");
            break;
        case 'V':
            sb.append("void");
        }
        while (pos-- > 0)
            sb.append("[]");
        return sb.toString();
    }

    /**
     * Extract the return type from signature.
     * 
     * @param signature
     *            a method signature, e.g. "([Ljava/lang/String;)V"
     * @return the Java type, e.g. "void"
     */
    static String returnType(String signature) {
        int ket = signature.indexOf(')');
        return signature2Type(signature.substring(ket + 1));
    }

    /**
     * Extract the parameter list from a method signature.
     * 
     * @param signature
     *            a method signature, e.g. "([Ljava/lang/String;)V"
     * @return the parameter list, e.g. "(java.lang.String [] p0)"
     */

    static String parameterList(String signature, ArrayList<String> paramNames) {
        int n = 0;
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        int bra = signature.indexOf('(');
        int ket = signature.indexOf(')');
        signature = signature.substring(bra + 1, ket);

        while (signature.length() > 0) {
            int end = 0;
            while (signature.charAt(end) == '[')
                ++end;
            if (signature.charAt(end) == 'L') {
                int semi = signature.indexOf(';');
                if (semi > 0)
                    end = semi + 1;
                else
                    end = signature.length();
            } else
                ++end;

            sb.append(signature2Type(signature.substring(0, end)));
            signature = signature.substring(end);
            if (paramNames != null) {
                if (!paramNames.isEmpty()) {
                    sb.append(" ");
                    sb.append(paramNames.remove(0));
                } else {
                    sb.append(" p");
                    sb.append(n);
                }
            }
            ++n;
            
            if (signature.length() > 0)
                sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Helper method to convert one type into a signature.
     * 
     * @param type
     *            a Java type, e.g. "long".
     * @return the signature, e.g. "J";
     */
    private static String _type2Signature(String type) {
        if ("void".equals(type))
            return "V";
        if ("int".equals(type))
            return "I";
        if ("boolean".equals(type))
            return "Z";
        if ("byte".equals(type))
            return "B";
        if ("short".equals(type))
            return "S";
        if ("char".equals(type))
            return "C";
        if ("float".equals(type))
            return "F";
        if ("double".equals(type))
            return "D";
        if ("long".equals(type))
            return "J";
        return "L" + Util.dot2Slash(type) + ";";
    }

    /**
     * Convert a Java type into a signature.
     * 
     * @param type
     *            the Java type. E.g. "long".
     * @return the signature, e.g. "J";
     */
    public static String type2Signature(String type) {
        int bra = type.indexOf('[');
        if (bra < 0)
            return _type2Signature(type);
        String base = _type2Signature(type.substring(0, bra).trim());
        do {
            base = "[" + base;
            type = type.substring(bra + 1);
            bra = type.indexOf('[');
        } while (bra >= 0);
        return base;
    }

    /**
     * Create the signature from params and return code. This method does not handle variable names! Ther must be none!!
     * 
     * @param params
     *            parameter list without variable names
     * @param ret
     *            return type
     * @return the calculated signature.
     */
    public static String params2Signature(String params, String ret) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (StringTokenizer st = new StringTokenizer(params, ","); st.hasMoreElements();) {
            String p = st.nextToken().trim();
            sb.append(type2Signature(p));
        }
        sb.append(")");
        sb.append(type2Signature(ret));
        return sb.toString();
    }

    /**
     * Get an short value as int at the specified offset.
     * 
     * @param data
     *            the byte array
     * @param offset
     *            the offset
     * @return an int value containing the short value
     */
    public static int toInt2(byte[] data, int offset) {
        byte d[] = data;
        return (d[offset] << 8) | (0xff & d[offset + 1]);
    }

    /**
     * Get an int value as int at the specified offset.
     * 
     * @param data
     *            the byte array
     * @param offset
     *            the offset
     * @return an int value containing the int value
     */
    public static int toInt4(byte[] data, int offset) {
        byte[] d = data;
        return ((0xff & d[offset]) << 24) | ((0xff & d[offset + 1]) << 16) | ((0xff & d[offset + 2]) << 8)
                | (0xff & d[offset + 3]);
    }

    /**
     * Get a slice from a byte array and return it as a separate byte array.
     * 
     * @param data
     *            the byte array
     * @param offset
     *            the offset into the byte array
     * @param length
     *            the length of the slice
     * @return a new byte array containing the data slice.
     */
    public static byte[] copy(byte[] data, int offset, int length) {
        byte b[] = new byte[length];
        System.arraycopy(data, offset, b, 0, length);
        return b;
    }

    public static String dump(byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("0x");
        for (int i = 0; i < data.length;) {
            String s = Integer.toHexString(0xff & data[i++]);
            if (s.length() == 1)
                sb.append("0");
            sb.append(s);
        }
        return sb.toString();
    }

    static int countStackParams(String s) {
        int n = 0;
        int idx = s.indexOf('(');
        if (idx < 0)
            return n;
        s = s.substring(idx + 1);
        while (s.length() > 0) {
            int ch = s.charAt(0);
            s = s.substring(1);
            if (ch == ')')
                return n;
            if (ch == '[')
                continue;
            ++n;
            if ("SBCFIZ".indexOf(ch) >= 0)
                continue;
            if ("DJ".indexOf(ch) >= 0) {
                ++n;
                continue;
            }
            idx = s.indexOf(';');
            s = s.substring(idx + 1);
        }
        return n;
    }

    static int countLogicalParams(String s) {
        int n = 0;
        int idx = s.indexOf('(');
        if (idx < 0)
            return n;
        s = s.substring(idx + 1);
        while (s.length() > 0) {
            int ch = s.charAt(0);
            s = s.substring(1);
            if (ch == ')')
                return n;
            if (ch == '[')
                continue;
            ++n;
            if ("SBCFIZDJ".indexOf(ch) >= 0)
                continue;
            idx = s.indexOf(';');
            s = s.substring(idx + 1);
        }
        return n;
    }

    
    static String unescape(String val) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < val.length(); ++i) {
            char ch = val.charAt(i);
            if (ch == '"') {
                sb.append("\\\"");
                continue;
            }
            if (ch == '\r') {
                sb.append("\\r");
                continue;
            }
            if (ch == '\n') {
                sb.append("\\n");
                continue;
            }
            if (ch == '\t') {
                sb.append("\\t");
                continue;
            }
            if (ch == '\\') {
                sb.append("\\\\");
                continue;
            }
            if (ch == 32) {
                sb.append(" ");
                continue;
            }
            if (ch > 32 && ch < 128) {
                sb.append(ch);
                continue;
            }
            String s = Integer.toHexString(ch);
            if (s.length() < 4)
                s = "0000".substring(s.length()) + s;
            sb.append("\\u").append(s);
        }
        return sb.toString();
    }
}
