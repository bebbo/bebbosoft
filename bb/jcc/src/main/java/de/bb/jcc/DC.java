package de.bb.jcc;

import java.util.ArrayList;

import de.bb.jcc.ConstantPool.Entry;
import de.bb.util.Pair;
import de.bb.util.SingleMap;

/**
 * Helper class to DeCompile the jasm code.
 * 
 * @author stefan "bebbo" franke
 * 
 */
class DC {

    private static final String[] PRIMITIVES = { "boolean", "char", "float", "double", "byte", "short", "int", "long" };
    private static final String SPACES = "                                                                                ";
    private ArrayList<String> completed = new ArrayList<String>();
    private ArrayList<String> stack = new ArrayList<String>();
    private SingleMap<Integer, Pair<String, String>> localsMap;
    private ConstantPool cp;
    private String className;
    private int tempCount;

    // the label and the condition
    private ArrayList<Pair<String, String>> flow = new ArrayList<Pair<String, String>>();
    private ArrayList<DecompileInstruction> dcIns;
    private ArrayList<DecompileStackEntry> dcStack;

    DC(ConstantPool cp, SingleMap<Integer, Pair<String, String>> localsMap, String className) {
        this.cp = cp;
        this.localsMap = localsMap;
        this.className = className;
    }

    /**
     * Main method to decompile the instructions.
     * 
     * @param instructions
     *            the array with all instructions.
     */
    void decompile(ArrayList<Instruction> instructions) {
        // First convert the instructions into something with more information.
        // While doing this the stack is tracked to know
        // - the types on the stack.
        // - the stack size.
        dcIns = new ArrayList<DecompileInstruction>();
        dcStack = new ArrayList<DecompileStackEntry>();
        for (Instruction ins : instructions) {
            // clone the current stack - we track per instruction the stack before and after
            ArrayList<DecompileStackEntry> stackBefore = (ArrayList<DecompileStackEntry>) dcStack.clone();
            // modify the stack
            DecompileInstruction di = new DecompileInstruction(ins);
            switch (ins.opcode) {
            case 0x00: // nop
                break;
            case 0x01: // aconst_null
                pushDC(di, "?Ljava/lang/Object;");
                break;
            case 0x02: // iconst_m1
            case 0x05: // iconst_2
            case 0x06: // iconst_3
            case 0x07: // iconst_4
            case 0x08: // iconst_5
                pushDC(di, "I");
                break;
            case 0x03: // iconst_0
            case 0x04: // iconst_1
                pushDC(di, "?I");
                break;
            case 0x09: // lconst_0
            case 0x0a: // lconst_1
                pushDC(di, "J");
                break;
            case 0x0b: // fconst_0
            case 0x0c: // fconst_1
            case 0x0d: // fconst_2
                pushDC(di, "F");
                break;
            case 0x0e: // dconst_0
            case 0x0f: // dconst_1
                pushDC(di, "D");
                break;
            case 0x10: // bipush
                pushDC(di, "B");
                break;
            case 0x11: // sipush
                pushDC(di, "S");
                break;
            case 0x12: // ldc
            case 0x13: // ldc_w
                switch (cp.getEntry(((ConstantInstruction)ins).getIndex()).kind) {
                case C.CONSTANT_String:
                    pushDC(di, "Ljava/lang/String;");
                    break;
                case C.CONSTANT_Integer:
                    pushDC(di, "I");
                    break;
                case C.CONSTANT_Float:
                    pushDC(di, "F");
                    break;
                    default:
                        throw new RuntimeException();
                }
                break;
            case 0x14: // ldc2_w
                switch (cp.getEntry(((ConstantInstruction)ins).getIndex()).kind) {
                case C.CONSTANT_Long:
                    pushDC(di, "J");
                    break;
                case C.CONSTANT_Double:
                    pushDC(di, "D");
                    break;
                    default:
                        throw new RuntimeException();
                }
                break;
            case 0x15: // iload
                pushDC(di, "?I");
                break;
            case 0x16: // lload
                pushDC(di, "J");
                break;
            case 0x17: // fload
                pushDC(di, "F");
                break;
            case 0x18: // dload
                pushDC(di, "D");
                break;
            case 0x19: // aload
                pushDC(di, "?Ljava/lang/Object;");
                break;
            case 0x1a: // iload_0
            case 0x1b: // iload_1
            case 0x1c: // iload_2
            case 0x1d: // iload_3
                pushDC(di, "?I");
                break;
            case 0x1e: // lload_0
            case 0x1f: // lload_1
            case 0x20: // lload_2
            case 0x21: // lload_3
                pushDC(di, "J");
                break;
            case 0x22: // fload_0
            case 0x23: // fload_1
            case 0x24: // fload_2
            case 0x25: // fload_3
                pushDC(di, "F");
                break;
            case 0x26: // dload_0
            case 0x27: // dload_1
            case 0x28: // dload_2
            case 0x29: // dload_3
                pushDC(di, "D");
                break;
            case 0x2a: // aload_0
            case 0x2b: // aload_1
            case 0x2c: // aload_2
            case 0x2d: // aload_3
                pushDC(di, "?Ljava/lang/Object;");
                break;
            case 0x2e: // iaload
                popDC(di, "I");
                popDC(di, "[I");
                pushDC(di, "I");
                break;
            case 0x2f: // laload
                popDC(di, "I");
                popDC(di, "[J");
                pushDC(di, "J");
                break;
            case 0x30: // faload
                popDC(di, "I");
                popDC(di, "[F");
                pushDC(di, "F");
                break;
            case 0x31: // daload
                popDC(di, "I");
                popDC(di, "[J");
                pushDC(di, "J");
                break;
            case 0x32: // aaload
                popDC(di, "I");
                popDC(di, "[?Ljava/lang/Object;");
                pushDC(di, "Ljava/lang/Object;");
                break;
            case 0x33: // baload
                popDC(di, "I");
                popDC(di, "[?B");
                pushDC(di, "?B");
                break;
            case 0x34: // caload
                popDC(di, "I");
                popDC(di, "[C");
                pushDC(di, "C");
                break;
            case 0x35: // saload
                popDC(di, "I");
                popDC(di, "[S");
                pushDC(di, "S");
                break;
            case 0x36: // istore
                popDC(di, "?I");
                break;
            case 0x37: // lstore
                popDC(di, "J");
                break;
            case 0x38: // fstore
                popDC(di, "F");
                break;
            case 0x39: // dstore
                popDC(di, "D");
                break;
            case 0x3a: // astore
                popDC(di, "?Ljava/lang/Object;");
                break;
            case 0x3b: // istore_0
            case 0x3c: // istore_1
            case 0x3d: // istore_2
            case 0x3e: // istore_3
                popDC(di, "?I");
                break;
            case 0x3f: // lstore_0
            case 0x40: // lstore_1
            case 0x41: // lstore_2
            case 0x42: // lstore_3
                popDC(di, "J");
                break;
            case 0x43: // fstore_0
            case 0x44: // fstore_1
            case 0x45: // fstore_2
            case 0x46: // fstore_3
                popDC(di, "F");
                break;
            case 0x47: // dstore_0
            case 0x48: // dstore_1
            case 0x49: // dstore_2
            case 0x4a: // dstore_3
                popDC(di, "D");
                break;
            case 0x4b: // astore_0
            case 0x4c: // astore_1
            case 0x4d: // astore_2
            case 0x4e: // astore_3
                popDC(di, "?Ljava/lang/Object;");
                break;
            case 0x4f: // iastore
                popDC(di, "I");
                popDC(di, "I");
                popDC(di, "[I");
                break;
            case 0x50: // lastore
                popDC(di, "J");
                popDC(di, "I");
                popDC(di, "[J");
                break;
            case 0x51: // fastore
                popDC(di, "F");
                popDC(di, "I");
                popDC(di, "[F");
                break;
            case 0x52: // dastore
                popDC(di, "D");
                popDC(di, "I");
                popDC(di, "[D");
                break;
            case 0x53: // aastore
                popDC(di, "?Ljava/lang/Object;");
                popDC(di, "I");
                popDC(di, "[?Ljava/lang/Object;");
                break;
            case 0x54: // bastore
                popDC(di, "?B");
                popDC(di, "I");
                popDC(di, "[?B");
                break;
            case 0x55: // castore
                popDC(di, "C");
                popDC(di, "I");
                popDC(di, "[C");
                break;
            case 0x56: // sastore
                popDC(di, "S");
                popDC(di, "I");
                popDC(di, "[S");
                break;
            case 0x57: // pop
                popDC(di, "?");
                break;
            case 0x58: // pop2
                popDC(di, "?");
                break;
            case 0x59: // dup
                dupDC(di, 0);
                break;
            case 0x5a: // dup_x1
                dupDC(di, 1);
                break;
            case 0x5b: // dup_x2
                dupDC(di, 2);
                break;
            case 0x5c: // dup2
                dupDC(di, 0);
                break;
            case 0x5d: // dup2_x1
                dupDC(di, 1);
                break;
            case 0x5e: // dup2_x2
                dupDC(di, 0);
                break;
            case 0x5f: // swap
                dupDC(di, 1);
                popDC(di, "?");
                break;
            case 0x60: // iadd
            case 0x64: // isub
            case 0x68: // imul
            case 0x6c: // idiv
            case 0x70: // irem
            case 0x74: //.......ineg
            case 0x78: // ishl
            case 0x7a: // ishr
            case 0x7c: // iushr
            case 0x7e: // iand
            case 0x80: // ior
            case 0x82: // ixor
                popDC(di, "I");
                popDC(di, "I");
                pushDC(di, "I");
                break;
            case 0x61: // ladd
            case 0x65: // lsub
            case 0x69: // lmul
            case 0x6d: // ldiv
            case 0x71: // lrem
            case 0x75: // lneg
            case 0x79: // lshl
            case 0x7b: // lshr
            case 0x7d: // lushr
            case 0x7f: // land
            case 0x81: // lor
            case 0x83: // lxor
                popDC(di, "J");
                popDC(di, "J");
                pushDC(di, "J");
                break;
            case 0x62: // fadd
            case 0x66: // fsub
            case 0x6a: // fmul
            case 0x6e: // fdiv
            case 0x72: // frem
            case 0x76: // fneg
                popDC(di, "F");
                popDC(di, "F");
                pushDC(di, "F");
                break;
            case 0x63: // dadd
            case 0x67: // dsub
            case 0x6b: // dmul
            case 0x6f: // ddiv
            case 0x73: // drem
            case 0x77: // dneg
                popDC(di, "D");
                popDC(di, "D");
                pushDC(di, "D");
                break;
            case 0x84: // iinc
                break;
            case 0x85: // i2l
                break;
            case 0x86: // i2f
                break;
            case 0x87: // i2d
                break;
            case 0x88: // l2i
                break;
            case 0x89: // l2f
                break;
            case 0x8a: // l2d
                break;
            case 0x8b: // f2i
                break;
            case 0x8c: // f2l
                break;
            case 0x8d: // f2d
                break;
            case 0x8e: // d2i
                break;
            case 0x8f: // d2l
                break;
            case 0x90: // d2f
                break;
            case 0x91: // i2b
                break;
            case 0x92: // i2c
                break;
            case 0x93: // i2s
                break;
            case 0x94: // lcmp
                break;
            case 0x95: // fcmpl
                break;
            case 0x96: // fcmpg
                break;
            case 0x97: // dcmpl
                break;
            case 0x98: // dcmpg
                break;
            case 0x99: // ifeq
                break;
            case 0x9a: // ifne
                break;
            case 0x9b: // iflt
                break;
            case 0x9c: // ifge
                break;
            case 0x9d: // ifgt
                break;
            case 0x9e: // ifle
                break;
            case 0x9f: // if_icmpeq
                break;
            case 0xa0: // if_icmpne
                break;
            case 0xa1: // if_icmplt
                break;
            case 0xa2: // if_icmpge
                break;
            case 0xa3: // if_icmpgt
                break;
            case 0xa4: // if_icmple
                break;
            case 0xa5: // if_acmpeq
                break;
            case 0xa6: // if_acmpne
                break;
            case 0xa7: // goto 
                break;
            case 0xa8: // jsr
                break;
            case 0xa9: // ret
                break;
            case 0xaa: // tableswitch
                break;
            case 0xab: // lookupswitch
                break;
            case 0xac: // ireturn
                break;
            case 0xad: // lreturn
                break;
            case 0xae: // freturn
                break;
            case 0xaf: // dreturn
                break;
            case 0xb0: // areturn
                break;
            case 0xb1: // return
                break;
            case 0xb2: // getstatic
                break;
            case 0xb3: // putstatic
                break;
            case 0xb4: // getfield
                break;
            case 0xb5: // putfield
                break;
            case 0xb6: // invokevirtual
                break;
            case 0xb7: // invokespecial
                break;
            case 0xb8: // invokestatic
                break;
            case 0xb9: // invokeinterface
                break;
            case 0xba: // xxxunusedxxx1
                break;
            case 0xbb: // new
                break;
            case 0xbc: // newarray
                break;
            case 0xbd: // anewarray
                break;
            case 0xbe: // arraylength
                break;
            case 0xbf: // athrow
                break;
            case 0xc0: // checkcast
                break;
            case 0xc1: // instanceof
                break;
            case 0xc2: // monitorenter
                break;
            case 0xc3: // monitorexit
                break;
            case 0xc4: // wide
                break;
            case 0xc5: // multianewarray
                break;
            case 0xc6: // ifnull
                break;
            case 0xc7: // ifnonnull
                break;
            case 0xc8: // goto_w
                break;
            case 0xc9: // jsr_w
                break;
            }
        }

    }

    private void dupDC(DecompileInstruction di, int offset) {
        dcStack.add(dcStack.get(dcStack.size() - 1 - offset));
    }

    private void popDC(DecompileInstruction di, String asType) {
        DecompileStackEntry popped = dcStack.remove(dcStack.size() - 1);
        if (popped.type.startsWith("?")) {
            debug("fixing " + popped.type + " to " + asType);
            popped.type = asType;
        }
    }


    private void pushDC(DecompileInstruction di, String type) {
        DecompileStackEntry dse = new DecompileStackEntry(di, type);
        dcStack.add(dse);
    }

    void decompile0(ArrayList<Instruction> instructions) {
        for (int j = 0; j < instructions.size(); ++j) {
            final Instruction i = instructions.get(j);
            switch (i.opcode) {

            case 0x01:
                stack.add("null");
                break;

            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x07:
            case 0x08:
                stack.add(Integer.toString(i.opcode - 3));
                break;

            case 0x10:
            case 0x11:
                stack.add(Integer.toString(((Instruction1Param) i).getP1()));
                break;

            case 0x12:
            case 0x13:
            case 0x14:
                pushConstant((ConstantInstruction) i);
                break;

            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
                pushLocal(((Instruction1Param) i).getP1());
                break;
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
                pushLocal(i.opcode - 0x1a);
                break;
            case 0x1e:
            case 0x1f:
            case 0x20:
            case 0x21:
                pushLocal(i.opcode - 0x1e);
                break;
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25:
                pushLocal(i.opcode - 0x22);
                break;
            case 0x26:
            case 0x27:
            case 0x28:
            case 0x29:
                pushLocal(i.opcode - 0x26);
                break;
            case 0x2a:
            case 0x2b:
            case 0x2c:
            case 0x2d:
                pushLocal(i.opcode - 0x2a);
                break;

            case 0x32:
                aaload();
                break;

            // store
            case 0x36:
            case 0x37:
            case 0x38:
            case 0x39:
            case 0x3a:
                assignLocal(((Instruction1Param) i).getP1());
                break;

            case 0x3b:
            case 0x3c:
            case 0x3d:
            case 0x3e:
                assignLocal(i.opcode - 0x3b);
                break;

            case 0x3f:
            case 0x40:
            case 0x41:
            case 0x42:
                assignLocal(i.opcode - 0x3f);
                break;

            case 0x43:
            case 0x44:
            case 0x45:
            case 0x46:
                assignLocal(i.opcode - 0x43);
                break;

            case 0x47:
            case 0x48:
            case 0x49:
            case 0x4a:
                assignLocal(i.opcode - 0x47);
                break;

            case 0x4b:
            case 0x4c:
            case 0x4d:
            case 0x4e:
                assignLocal(i.opcode - 0x4b);
                break;

            case 0x4f:
                assignLArray();
                break;

            case 0x53:
                aastore();
                break;

            case 0x59: // dup
                dup();
                break;

            case 0x60:
                calc("+");
                break;
            case 0x64:
                calc("-");
                break;

            case 0x99: // ifeq
                doIf("!=", ((Jump) i).getLabel());
                break;
            case 0x9a: // ifne
                doIf("==", ((Jump) i).getLabel());
                break;
            case 0x9b: // iflt
                doIf(">=", ((Jump) i).getLabel());
                break;
            case 0x9c: // ifge
                doIf("<", ((Jump) i).getLabel());
                break;
            case 0x9d: // ifgt
                doIf("<=", ((Jump) i).getLabel());
                break;
            case 0x9e: // ifle
                doIf(">", ((Jump) i).getLabel());
                break;

            case 0xb1:
                stack.add("return");
                break;

            case 0xac:
            case 0xad:
            case 0xae:
            case 0xaf:
            case 0xb0:
                xReturn();
                break;

            case 0xb2: // getstatic
                getField((ConstantInstruction) i, true);
                break;
            case 0xb3: // putstatic
                putField((ConstantInstruction) i, true);
                break;
            case 0xb4: // getfield
                getField((ConstantInstruction) i, false);
                break;
            case 0xb5: // putfield
                putField((ConstantInstruction) i, false);
                break;

            case 0xb6: // invokevirtual
            case 0xb7: // invokespecial
            case 0xb9: // invokevirtual
                invokeFunction((ConstantInstruction) i, false);
                break;

            case 0xb8: // invokestatic
                invokeFunction((ConstantInstruction) i, true);
                break;

            case 0xbb: // new
                stack.add(i.toString().substring(1));
                break;

            case 0xbc:
                newArray((NewArray) i);
                break;

            case 0xbd:
                newObjectArray((ConstantInstruction) i);
                break;

            case 0xc0:
                checkCast((ConstantInstruction) i);
                break;

            case 0x102: // label
                doLabel((Label) i);
                break;

            default:
                System.out.println("TODO: " + i);
            }
        }
    }

    private void doLabel(Label label) {
        //        if (flow.isEmpty())
        //            return;
        //
        //        Pair<String, String> topFlow = flow.get(flow.size() - 1);
        //        String lastIf = topFlow.getSecond();
        //        if (completed.get(completed.size() - 1).equals(lastIf))
        //            return;
        //        
        //        String l = label.getName();
        //        if (l.equals(topFlow.getFirst())) {
        //            flow.remove(flow.size() - 1);
        //            complete("}");
        //            return;
        //        }
        if (label.ifCount > 0) {
            for (int i = 0; i < label.ifCount; ++i) {
                complete("}");
            }
        }
        if (label.isElse) {
            complete("} else {");
        }
        if (!label.isElse && label.ifCount == 0) {
            complete("{");
        }
    }

    private void doIf(String cmp, String label) {
        String condition = pop() + " " + cmp + " 0";
        if (stack.isEmpty()) {
            condition = "if (" + condition + ") ";
            complete(condition);
            flow.add(Pair.makePair(label, condition));
        } else {
            stack.add("(" + condition + ") ? ");
        }
    }

    private void xReturn() {
        String top = pop();
        complete("return " + top + ";");
    }

    /**
     * Duplicate the top line. Create temporary variables
     */
    private void dup() {
        String top = pop();

        if (top.startsWith("new ")) {

            int bra = top.indexOf('[');
            if (bra > 0) {
                String type = top.substring(4, bra);
                String var = "t" + tempCount;
                ++tempCount;
                complete(type + " " + var + "[] = " + top + ";");
                stack.add(var);
                stack.add(var);
                return;
            }
        }

        stack.add(top);
        stack.add(top);
    }

    private void newObjectArray(ConstantInstruction i) {
        String len = pop();
        String type = cp.getConstant(i.getIndex());
        stack.add("new " + type + "[" + len + "]");
    }

    private void aastore() {
        String val = pop();
        String index = pop();
        String array = pop();
        complete(array + "[" + index + "] = " + val + ";");
    }

    private void aaload() {
        String index = pop();
        String array = pop();
        stack.add(array + "[" + index + "]");
    }

    private void checkCast(ConstantInstruction i) {
        String type = cp.getConstant(i.getIndex());
        stack.add("((" + type + ")" + pop() + ")");
    }

    private void calc(String op) {
        String v2 = pop();
        String v1 = pop();
        stack.add("(" + v1 + " " + op + " " + v2 + ")");
    }

    /**
     * Assign to array
     */
    private void assignLArray() {
        String val = pop();
        String index = pop();
        String var = pop();
        complete(var + "[" + index + "] = " + val + ";");
    }

    private void newArray(NewArray i) {
        String size = pop();
        String type = PRIMITIVES[i.getArrayType() - 4];
        stack.add("new " + type + "[" + size + "]");
    }

    private void assignLocal(int index) {
        Pair<String, String> p = localsMap.get(index);
        String s = p.getFirst() + " = " + pop() + ";";
        complete(s);
    }

    /**
     * push a constant.
     * 
     * @param i
     */
    private void pushConstant(ConstantInstruction i) {
        Entry e = cp.getEntry(i.getIndex());
        switch (e.kind) {
        case C.CONSTANT_Utf8:
            stack.add("\"" + Util.unescape(e.sVal) + "\"");
            break;
        case C.CONSTANT_Integer:
            if (e.iVal1 > 1023)
                stack.add("0x" + Integer.toHexString(e.iVal1));
            else
                stack.add(String.valueOf(e.iVal1));
            break;
        case C.CONSTANT_Long:
            if (e.lVal > 1023)
                stack.add("0x" + Long.toHexString(e.lVal) + "L");
            else
                stack.add(String.valueOf(e.lVal) + "L");
            break;
        case C.CONSTANT_Float:
            stack.add(String.valueOf(e.fVal) + "f");
            break;
        case C.CONSTANT_Double:
            stack.add(String.valueOf(e.dVal) + "d");
            break;
        case C.CONSTANT_String:
            e = cp.getEntry(e.iVal1);
            stack.add("\"" + Util.unescape(e.sVal) + "\"");
            break;
        case C.CONSTANT_Class:
            stack.add(Util.class2Type(cp.getConstant(e.iVal1)) + ".class");
            break;
        }
    }

    private void getField(ConstantInstruction i, boolean isStatic) {
        Entry e = cp.getEntry(i.getIndex());
        String t = cp.getConstant(e.iVal2);
        int colon = t.indexOf(':');
        String name = t.substring(0, colon);

        String base = isStatic ? cp.getConstant(e.iVal1) : pop();
        stack.add(base + "." + name);
    }

    private void putField(ConstantInstruction i, boolean isStatic) {
        Entry e = cp.getEntry(i.getIndex());
        String t = cp.getConstant(e.iVal2);
        int colon = t.indexOf(':');
        String name = t.substring(0, colon);
        String val = pop();
        String base = isStatic ? cp.getConstant(e.iVal1) : pop();
        complete(base + "." + name + " = " + val + ";");
    }

    /**
     * Add code for a function call
     * 
     * @param i
     *            the instruction
     * @param isStatic
     *            true if it's a static call
     */
    private void invokeFunction(ConstantInstruction i, boolean isStatic) {
        Entry e0 = cp.getEntry(i.getIndex());
        String base = cp.getConstant(e0.iVal1);
        Entry e = cp.getEntry(e0.iVal2);
        String fx = cp.getConstant(e.iVal1);
        String sig = cp.getConstant(e.iVal2);
        String params = "";
        int n = Util.countLogicalParams(sig);
        while (n-- > 0) {
            if (params.length() > 0) {
                params = pop() + ", " + params;
            } else {
                params = pop();
            }
        }
        String s;
        if (fx.equals("<init>")) {
            String dis = pop();
            if (dis.equals("this") && !base.equals(className))
                dis = "super";

            s = dis + "(" + params + ")";
            if (stack.size() > 0 && stack.get(stack.size() - 1).equals(dis)) {
                pop();
            } else {
                s += ";";
                complete(s);
                return;
            }
        } else if (isStatic) {
            s = base + "." + fx + "(" + params + ")";
            if (sig.endsWith(")V")) {
                complete(s + ";");
                return;
            }
        } else {
            s = pop() + "." + fx + "(" + params + ")";
            if (sig.endsWith(")V")) {
                complete(s + ";");
                return;
            }
        }
        stack.add(s);
    }

    private final void complete(String expression) {
        completed.add(indention() + expression);
    }

    private final String indention() {
        return SPACES.substring(0, 2 * flow.size() % SPACES.length());
    }

    private final String pop() {
        return stack.remove(stack.size() - 1);
    }

    /**
     * Push a local variable to the stack
     * 
     * @param index
     */
    private void pushLocal(int index) {
        Pair<String, String> p = localsMap.get(index);
        String name = p != null ? p.getFirst() : "l" + index;
        stack.add(name);
    }

    /**
     * Analyze the flow to find - if ... else - loops
     */
    private void analyzeFlow(ArrayList<Instruction> instructions) {
        // find all Label and create lookup maps
        final int sz = instructions.size();
        SingleMap<Integer, Label> i2l = new SingleMap<Integer, Label>();
        SingleMap<String, Integer> l2i = new SingleMap<String, Integer>();
        for (int i = 0; i < sz; ++i) {
            Instruction ins = instructions.get(i);
            if (ins instanceof Label) {
                Label l = (Label) ins;
                i2l.put(i, l);
                l2i.put(l.getName(), i);
            }
        }

        // find all Jump instructions. 
        for (int i = 0; i < sz; ++i) {
            Instruction ins = instructions.get(i);
            if (ins instanceof Jump) {
                Jump j = (Jump) ins;
                int lindex = l2i.get(j.getLabel());
                Label l = i2l.get(lindex);

                // if lindex < i --> label == loop
                if (lindex < i) {
                    l.isLoop = true;
                    if (i + 1 < sz) {
                        ins = instructions.get(i + 1);
                        if (ins instanceof Label) {
                            Label l2 = (Label) ins;
                            l2.afterLoop = l;
                        }
                    }
                    continue;
                }

                // conditional
                // if xy goto L2
                // ..
                // ins2 -- if this is a "goto L3" and "L3" is behind "L2" -> it's a else (see below)
                // L2:
                if (!j.isGoto()) {
                    ins = instructions.get(lindex - 1);
                    if (ins instanceof Jump) {
                        Jump j2 = (Jump) ins;
                        int lindex2 = l2i.get(j2.getLabel());
                        if (lindex2 > lindex) {
                            l.isElse = j2.isGoto();
                            l.isIf = !l.isElse;
                            Label l2 = i2l.get(lindex2);
                            ++l2.ifCount;
                        }
                    } else {
                        ++l.ifCount;
                    }
                }
            }
        }
        for (Label l : i2l.values()) {
            System.out.println(l);
        }
        System.out.println();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : completed) {
            sb.append("\t").append(s).append("\r\n");
        }
        return sb.toString();
    }
    
    private void dummy(int x) {
        switch (x) {
        case 0x00: // nop
            break;
        case 0x01: // aconst_null
            break;
        case 0x02: // iconst_m1
            break;
        case 0x03: // iconst_0
            break;
        case 0x04: // iconst_1
            break;
        case 0x05: // iconst_2
            break;
        case 0x06: // iconst_3
            break;
        case 0x07: // iconst_4
            break;
        case 0x08: // iconst_5
            break;
        case 0x09: // lconst_0
            break;
        case 0x0a: // lconst_1
            break;
        case 0x0b: // fconst_0
            break;
        case 0x0c: // fconst_1
            break;
        case 0x0d: // fconst_2
            break;
        case 0x0e: // dconst_0
            break;
        case 0x0f: // dconst_1
            break;
        case 0x10: // bipush
            break;
        case 0x11: // sipush
            break;
        case 0x12: // ldc
            break;
        case 0x13: // ldc_w
            break;
        case 0x14: // ldc2_w
            break;
        case 0x15: // iload
            break;
        case 0x16: // lload
            break;
        case 0x17: // fload
            break;
        case 0x18: // dload
            break;
        case 0x19: // aload
            break;
        case 0x1a: // iload_0
            break;
        case 0x1b: // iload_1
            break;
        case 0x1c: // iload_2
            break;
        case 0x1d: // iload_3
            break;
        case 0x1e: // lload_0
            break;
        case 0x1f: // lload_1
            break;
        case 0x20: // lload_2
            break;
        case 0x21: // lload_3
            break;
        case 0x22: // fload_0
            break;
        case 0x23: // fload_1
            break;
        case 0x24: // fload_2
            break;
        case 0x25: // fload_3
            break;
        case 0x26: // dload_0
            break;
        case 0x27: // dload_1
            break;
        case 0x28: // dload_2
            break;
        case 0x29: // dload_3
            break;
        case 0x2a: // aload_0
            break;
        case 0x2b: // aload_1
            break;
        case 0x2c: // aload_2
            break;
        case 0x2d: // aload_3
            break;
        case 0x2e: // iaload
            break;
        case 0x2f: // laload
            break;
        case 0x30: // faload
            break;
        case 0x31: // daload
            break;
        case 0x32: // aaload
            break;
        case 0x33: // baload
            break;
        case 0x34: // caload
            break;
        case 0x35: // saload
            break;
        case 0x36: // istore
            break;
        case 0x37: // lstore
            break;
        case 0x38: // fstore
            break;
        case 0x39: // dstore
            break;
        case 0x3a: // astore
            break;
        case 0x3b: // istore_0
            break;
        case 0x3c: // istore_1
            break;
        case 0x3d: // istore_2
            break;
        case 0x3e: // istore_3
            break;
        case 0x3f: // lstore_0
            break;
        case 0x40: // lstore_1
            break;
        case 0x41: // lstore_2
            break;
        case 0x42: // lstore_3
            break;
        case 0x43: // fstore_0
            break;
        case 0x44: // fstore_1
            break;
        case 0x45: // fstore_2
            break;
        case 0x46: // fstore_3
            break;
        case 0x47: // dstore_0
            break;
        case 0x48: // dstore_1
            break;
        case 0x49: // dstore_2
            break;
        case 0x4a: // dstore_3
            break;
        case 0x4b: // astore_0
            break;
        case 0x4c: // astore_1
            break;
        case 0x4d: // astore_2
            break;
        case 0x4e: // astore_3
            break;
        case 0x4f: // iastore
            break;
        case 0x50: // lastore
            break;
        case 0x51: // fastore
            break;
        case 0x52: // dastore
            break;
        case 0x53: // aastore
            break;
        case 0x54: // bastore
            break;
        case 0x55: // castore
            break;
        case 0x56: // sastore
            break;
        case 0x57: // pop
            break;
        case 0x58: // pop2
            break;
        case 0x59: // dup
            break;
        case 0x5a: // dup_x1
            break;
        case 0x5b: // dup_x2
            break;
        case 0x5c: // dup2
            break;
        case 0x5d: // dup2_x1
            break;
        case 0x5e: // dup2_x2
            break;
        case 0x5f: // swap
            break;
        case 0x60: // iadd
            break;
        case 0x61: // ladd
            break;
        case 0x62: // fadd
            break;
        case 0x63: // dadd
            break;
        case 0x64: // isub
            break;
        case 0x65: // lsub
            break;
        case 0x66: // fsub
            break;
        case 0x67: // dsub
            break;
        case 0x68: // imul
            break;
        case 0x69: // lmul
            break;
        case 0x6a: // fmul
            break;
        case 0x6b: // dmul
            break;
        case 0x6c: // idiv
            break;
        case 0x6d: // ldiv
            break;
        case 0x6e: // fdiv
            break;
        case 0x6f: // ddiv
            break;
        case 0x70: // irem
            break;
        case 0x71: // lrem
            break;
        case 0x72: // frem
            break;
        case 0x73: // drem
            break;
        case 0x74: //.......ineg
            break;
        case 0x75: // lneg
            break;
        case 0x76: // fneg
            break;
        case 0x77: // dneg
            break;
        case 0x78: // ishl
            break;
        case 0x79: // lshl
            break;
        case 0x7a: // ishr
            break;
        case 0x7b: // lshr
            break;
        case 0x7c: // iushr
            break;
        case 0x7d: // lushr
            break;
        case 0x7e: // iand
            break;
        case 0x7f: // land
            break;
        case 0x80: // ior
            break;
        case 0x81: // lor
            break;
        case 0x82: // ixor
            break;
        case 0x83: // lxor
            break;
        case 0x84: // iinc
            break;
        case 0x85: // i2l
            break;
        case 0x86: // i2f
            break;
        case 0x87: // i2d
            break;
        case 0x88: // l2i
            break;
        case 0x89: // l2f
            break;
        case 0x8a: // l2d
            break;
        case 0x8b: // f2i
            break;
        case 0x8c: // f2l
            break;
        case 0x8d: // f2d
            break;
        case 0x8e: // d2i
            break;
        case 0x8f: // d2l
            break;
        case 0x90: // d2f
            break;
        case 0x91: // i2b
            break;
        case 0x92: // i2c
            break;
        case 0x93: // i2s
            break;
        case 0x94: // lcmp
            break;
        case 0x95: // fcmpl
            break;
        case 0x96: // fcmpg
            break;
        case 0x97: // dcmpl
            break;
        case 0x98: // dcmpg
            break;
        case 0x99: // ifeq
            break;
        case 0x9a: // ifne
            break;
        case 0x9b: // iflt
            break;
        case 0x9c: // ifge
            break;
        case 0x9d: // ifgt
            break;
        case 0x9e: // ifle
            break;
        case 0x9f: // if_icmpeq
            break;
        case 0xa0: // if_icmpne
            break;
        case 0xa1: // if_icmplt
            break;
        case 0xa2: // if_icmpge
            break;
        case 0xa3: // if_icmpgt
            break;
        case 0xa4: // if_icmple
            break;
        case 0xa5: // if_acmpeq
            break;
        case 0xa6: // if_acmpne
            break;
        case 0xa7: // goto 
            break;
        case 0xa8: // jsr
            break;
        case 0xa9: // ret
            break;
        case 0xaa: // tableswitch
            break;
        case 0xab: // lookupswitch
            break;
        case 0xac: // ireturn
            break;
        case 0xad: // lreturn
            break;
        case 0xae: // freturn
            break;
        case 0xaf: // dreturn
            break;
        case 0xb0: // areturn
            break;
        case 0xb1: // return
            break;
        case 0xb2: // getstatic
            break;
        case 0xb3: // putstatic
            break;
        case 0xb4: // getfield
            break;
        case 0xb5: // putfield
            break;
        case 0xb6: // invokevirtual
            break;
        case 0xb7: // invokespecial
            break;
        case 0xb8: // invokestatic
            break;
        case 0xb9: // invokeinterface
            break;
        case 0xba: // xxxunusedxxx1
            break;
        case 0xbb: // new
            break;
        case 0xbc: // newarray
            break;
        case 0xbd: // anewarray
            break;
        case 0xbe: // arraylength
            break;
        case 0xbf: // athrow
            break;
        case 0xc0: // checkcast
            break;
        case 0xc1: // instanceof
            break;
        case 0xc2: // monitorenter
            break;
        case 0xc3: // monitorexit
            break;
        case 0xc4: // wide
            break;
        case 0xc5: // multianewarray
            break;
        case 0xc6: // ifnull
            break;
        case 0xc7: // ifnonnull
            break;
        case 0xc8: // goto_w
            break;
        case 0xc9: // jsr_w
            break;
        }
    }
    
    private static void debug(String msg) {
        System.out.println(msg);
    }

}
