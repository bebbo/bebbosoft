/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jcc/src/main/java/de/bb/jcc/app/DisAsm.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 17:07:21 $
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

package de.bb.jcc.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import de.bb.jcc.ClassDefinition;
import de.bb.jcc.ClassReader;
import de.bb.jcc.ConstantPool;

/**
 * Simple application to dissassemble Java byte code.
 * 
 * @author sfranke
 * 
 */
public class DisAsm {

    /** code size table. */
    private final static int CODESIZETABLE[] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3, 3, 2, 2, 2,
            2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 2, 0, 0, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 1, 3, 2, 3, 1, 1, 3, 3, 1, 1, 1, 4, 3, 3, 5, 5, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1};

    /** opcode name table. */
    private final static String NAME[] = {"nop", "aconst_null", "iconst -1", "iconst 0", "iconst 1", "iconst 2",
            "iconst 3", "iconst 4", "iconst 5", "lconst 0", "lconst 1", "fconst 0", "fconst 1", "fconst 2", "dconst 0",
            "dconst 1", "ldc", "ldc", "ldc", "ldc", "ldc", "iload", "lload", "fload", "dload", "aload", "iload 0",
            "iload 1", "iload 2", "iload 3", "lload 0", "lload 1", "lload 2", "lload 3", "fload 0", "fload 1",
            "fload 2", "fload 3", "dload 0", "dload 1", "dload 2", "dload 3", "aload 0", "aload 1", "aload 2",
            "aload 3", "iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload", "istore",
            "lstore", "fstore", "dstore", "astore", "istore 0", "istore 1", "istore 2", "istore 3", "lstore 0",
            "lstore 1", "lstore 2", "lstore 3", "fstore 0", "fstore 1", "fstore 2", "fstore 3", "dstore 0", "dstore 1",
            "dstore 2", "dstore 3", "astore 0", "astore 1", "astore 2", "astore 3", "iastore", "lastore", "fastore",
            "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2",
            "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul",
            "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg",
            "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land",

            "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i",
            "d2l", "d2f", "i2b", "idc", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt",
            "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt", "if_icmple",
            "if_acmpeq", "if_acmpne", "goto", "jsr", "ret", "tableswitch", "lookupswitch", "ireturn", "lreturn",
            "freturn", "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield", "putfield",
            "invokevirtual", "invokespecial", "invokestatic", "invokeinterface", null, "new", "newarray", "anewarray",
            "arraylength", "athrow", "checkcast", "instanceof", "monitorenter", "monitorexit", "wide",
            "multianewarray", "ifnull", "ifnonnull", "goto_w", "jsr_w", null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,};

    /** arraytypes. */
    private static final String ARRAYTYPES[] = {"Z", "C", "F", "D", "B", "S", "I", "J"};

    private static final String USAGE = "java de.bb.jcc.app.DisAsm <classfile>+ [-?]\r\n"
            + "where classfile is either a valid file name or a valid URL\r\n"
            + "- you can omitt the extension '.class'\r\n" + "- dots are converted to slashes if no file was found\r\n"
            + "- you can reference files in jars without unpacking the jar\r\n" + "examples:\r\n"
            + "  jar:file:///c:/java/jdk1.7.0/jre/lib/rt.jar!/java.util.ArrayList\r\n" + "  SomeClass.class\r\n"
            + "  bin/de/bb/util/MultiMap";

    private static boolean USESCONSTANT[] = {false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, true, true, true, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false,

            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, true, true, true, true, true, true, true, true, false, true, false,
            true, false, false, true, true, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,};

    /**
     * Disassemble the provided code and return the method as String.
     * 
     * @param code
     * @return
     */
    public static String disassemble(byte[] code, ConstantPool cp) {

        HashSet labels = new HashSet();

        // pass one: find all jump Labels
        for (int i = 0; i < code.length;) {
            int opcode = 0xff & code[i];
            int size = CODESIZETABLE[opcode];
            // handle wide
            if (opcode == 196) {
                opcode = 0xff & code[i + 1];
                size = CODESIZETABLE[opcode];
                size += (size - 1) * 2;
            }

            switch (opcode) {
                // variants of cmp
                case 0x99:
                case 0x9a:
                case 0x9b:
                case 0x9c:
                case 0x9d:
                case 0x9e:
                case 0x9f:
                case 0xa0:
                case 0xa1:
                case 0xa2:
                case 0xa3:
                case 0xa4:
                case 0xa5:
                case 0xa6:
                    // goto
                case 0xa7:
                    // jsr
                case 0xa8:
                    // cmp null / not null
                case 0xc6:
                case 0xc7: {
                    int offset;
                    if (size == 3)
                        offset = toInt2(code, i + 1);
                    else
                        offset = toInt4(code, i + 2);
                    Integer l = new Integer(i + offset);
                    labels.add(l);
                }
                    break;

                // handle tableswitch (170)
                // tableswitch Ldefault, lobyte, hibyte, L1, ...
                case 170: {
                    int j = (i + 4) & ~3;
                    int def = toInt4(code, j) + i;
                    Integer l = new Integer(def);
                    labels.add(l);
                    int lo = toInt4(code, j += 4);
                    int hi = toInt4(code, j += 4);
                    int count = hi - lo + 1;
                    while (count-- > 0) {
                        int pos = toInt4(code, j += 4) + i;
                        l = new Integer(pos);
                        labels.add(l);
                    }
                    size = j + 4 - i;
                }
                    break;
                // and lookupswitch (171)
                // lookupswitch Ldefault, (value, L1), ...
                case 171: {
                    int j = (i + 4) & ~3;
                    int def = toInt4(code, j) + i;
                    Integer l = new Integer(def);
                    labels.add(l);
                    int count = toInt4(code, j += 4);
                    while (count-- > 0) {
                        // int val =
                        toInt4(code, j += 4);
                        int pos = toInt4(code, j += 4) + i;
                        l = new Integer(pos);
                        labels.add(l);
                    }
                    size = j + 4 - i;
                }
                    break;
            }

            i += size;
        }

        StringBuffer sb = new StringBuffer();
        // now create the dump
        for (int i = 0; i < code.length;) {
            Integer l = new Integer(i);
            if (labels.remove(l)) {
                sb.append("L" + i + ":\r\n");
            }
            int opcode = 0xff & code[i];
            int size = CODESIZETABLE[opcode];
            if (size == 0 && opcode != 170)
                System.err.println("sz == 0");
            // handle wide
            boolean isWide = opcode == 196;
            if (isWide) {
                opcode = 0xff & code[i + 1];
                size = CODESIZETABLE[opcode];
                size += (size - 1) * 2;
            }

            switch (opcode) {
                // variants of cmp
                case 0x99:
                case 0x9a:
                case 0x9b:
                case 0x9c:
                case 0x9d:
                case 0x9e:
                case 0x9f:
                case 0xa0:
                case 0xa1:
                case 0xa2:
                case 0xa3:
                case 0xa4:
                case 0xa5:
                case 0xa6:
                    // goto
                case 0xa7:
                    // jsr
                case 0xa8:
                    // cmp null / not null
                case 0xc6:
                case 0xc7: {
                    int offset;
                    if (size == 3)
                        offset = toInt2(code, i + 1);
                    else
                        offset = toInt4(code, i + 2);
                    l = new Integer(i + offset);
                    sb.append("    " + NAME[opcode] + " L" + l + "\r\n");
                }
                    break;

                // handle tableswitch (170)
                // tableswitch Ldefault, offset, L1, ...
                case 170: {
                    int j = (i + 4) & ~3;
                    int def = toInt4(code, j) + i;
                    sb.append("    tableswitch L" + def);
                    int lo = toInt4(code, j += 4);
                    int hi = toInt4(code, j += 4);
                    sb.append(", (").append(lo).append(", ").append(hi).append(")");
                    int count = hi - lo + 1;
                    while (count-- > 0) {
                        int pos = toInt4(code, j += 4) + i;
                        sb.append(", L" + pos);
                    }
                    sb.append("\r\n");
                    size = j + 4 - i;
                }
                    break;
                // and lookupswitch (171)
                // lookupswitch Ldefault, (value, L1), ...
                case 171: {
                    int j = (i + 4) & ~3;
                    int def = toInt4(code, j) + i;
                    sb.append("    lookupswitch L" + def);
                    int count = toInt4(code, j += 4);
                    while (count-- > 0) {
                        int val = toInt4(code, j += 4);
                        int pos = toInt4(code, j += 4) + i;
                        sb.append(", (" + val + ", L" + pos + ")");
                    }
                    sb.append("\r\n");
                    size = j + 4 - i;
                }
                    break;
                default:
                    String name = NAME[opcode];
                    if (name == null)
                        sb.append("    opcode " + Integer.toHexString(opcode) + " ");
                    else
                        sb.append("    " + name + " ");
                    if (USESCONSTANT[opcode]) {
                        int index;
                        switch (opcode) {
                            case 0x12: // ldc
                                index = 0xff & code[i + 1];
                                break;
                            default:
                                index = toInt2(code, i + 1) & 0xffff;
                        }
                        String c = cp.getConstant(index);
                        sb.append(c);

                        // invoke interface
                        if (opcode == 0xb9) {
                            // ignore extra bytes... not needed to assemble
                        }
                    } else if (opcode == 0xbc) { // newarray
                        sb.append(ARRAYTYPES[code[i + 1] - 4]);
                    } else {
                        if (isWide)
                            sb.append(raw(code, i + 2, size - 2));
                        else
                            sb.append(raw(code, i + 1, size - 1));
                    }
                    sb.append("\r\n");
            }

            i += size;
        }

        return sb.toString();
    }

    private static StringBuffer raw(byte[] code, int i, int j) {
        StringBuffer sb = new StringBuffer();
        if (j > 0)
            sb.append("0x");
        while (j-- > 0) {
            String s = Integer.toHexString(0xff & code[i++]);
            if (s.length() == 1)
                sb.append("0");
            sb.append(s);
        }
        return sb;
    }

    private static int toInt2(byte[] code, int i) {
        return (code[i] << 8) | (0xff & code[i + 1]);
    }

    private static int toInt4(byte[] code, int i) {
        return ((0xff & code[i]) << 24) | ((0xff & code[i + 1]) << 16) | ((0xff & code[i + 2]) << 8)
                | (0xff & code[i + 3]);
    }

    /**
     * Entry point to start the disassembler.
     * 
     * @param args
     *            supported options are -? help other arguments are class names.
     */
    public static void main(String[] args) {
        try {
            // get options and remove from args
            args = doOptions(args);

            // iterate over file names
            for (int i = 0; i < args.length; ++i) {
                String fn = args[i];
                fn = fn.replace('\\', '/');

                // try some variants to get the file
                InputStream is = null;
                try {
                    is = new URL(fn).openConnection().getInputStream();
                } catch (MalformedURLException mue) {
                    File f = new File(fn);
                    if (!f.exists())
                        f = new File(fn + ".class");
                    if (!f.exists())
                        f = new File(fn.replace('.', '/') + ".class");
                    is = new FileInputStream(f);
                } catch (IOException ioe1) {
                    try {
                        is = new URL(fn + ".class").openConnection().getInputStream();
                    } catch (IOException ioe2) {
                        try {
                            is = new URL(fn.replace('.', '/') + ".class").openConnection().getInputStream();
                        } catch (IOException ioe3) {
                            System.err.println("cant read: " + fn);
                            continue;
                        }
                    }
                }

                // read the file and get the class definition
                ClassReader cr = new ClassReader(is);
                is.close();

                ClassDefinition cd = cr.getClassDefinition();
                cd.decompile();
                System.out.println(cd.toString());

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            //            System.out.println(ex.getMessage());
        }
    }

    /**
     * parse the command line for options and return other parameters.
     * 
     * @param args
     *            all command line arguments
     * @return the command line arguments without the options
     * @throws Exception
     */
    private static String[] doOptions(String args[]) throws Exception {
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
             * if (o.equals("-s")) { addStackMap = true; continue; }
             */
            throw new Exception("Invalid option '" + args[i - 1] + "'");
        }

        if (j == 0)
            throw new Exception(USAGE); // show usage only!

        String res[] = new String[j];
        System.arraycopy(args, 0, res, 0, j);
        return res;
    }
}

/*
 * Log: $Log: DisAsm.java,v $
 * Log: Revision 1.2  2012/08/11 17:07:21  bebbo
 * Log: @I working stage
 * Log:
 * Log: Revision 1.1  2011/01/01 13:26:05  bebbo
 * Log: @N added to new CVS repo
 * Log:
 */
