package de.bb.jcc;

import java.util.ArrayList;

import de.bb.util.Pair;

class Instruction {
    /** code size table. */
    final static int CODESIZETABLE[] = {
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 3, 2, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,

            1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0, 0, 1, 1, 1, 1,
            1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 5, 3, 2, 3, 1, 1,
            3, 3, 1, 1, 1, 4, 3, 3, 5, 5, 1, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 };

    /**
     * stack count table.
     */
    final static int STACKCOUNTTABLE[] = {
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 2, 2,
            2, 2, 2, 2, 2, 2, 1, 1,  1, 1, 1, 1, 1, 1, 1, 1,
            
            1, 1, 1, 1, 1, 1, 1, 1,  1, 1, 1, 1, 1, 1, 1, 1,
            3, 3, 3, 3, 3, 3, 3, 1,  1, 0, 0, 0, 0, 0, 0, 0,
            2, 2, 2, 2, 2, 2, 2, 2,  2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2,  2, 2, 2, 2, 2, 2, 2, 2,
            
            2, 2, 2, 2, 0, 1, 1, 1,  1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 2, 2, 2, 2,  2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 0,  0, 0, 0, 1, 1, 1, 1, 1,
            1, 0, 0, 1, 0, 1, 0, 0,  0, 0, 0, 0, 0, 1, 1, 1,

            1, 1, 1, 1, 0, 0, 1, 1,  0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0
    };

    /** opcode name table. */
    final static String NAME[] = {
            "nop", "aconst_null", "iconst -1", "iconst 0", "iconst 1", "iconst 2", "iconst 3", "iconst 4",
            "iconst 5", "lconst 0", "lconst 1", "fconst 0", "fconst 1", "fconst 2", "dconst 0", "dconst 1",
            "ldc", "ldc", "ldc", "ldc", "ldc", "iload", "lload", "fload",
            "dload", "aload", "iload 0", "iload 1", "iload 2", "iload 3", "lload 0", "lload 1",
            "lload 2", "lload 3", "fload 0", "fload 1", "fload 2", "fload 3", "dload 0", "dload 1",
            "dload 2", "dload 3", "aload 0", "aload 1", "aload 2", "aload 3", "iaload", "laload",
            "faload", "daload", "aaload", "baload", "caload", "saload", "istore", "lstore",
            "fstore", "dstore", "astore", "istore 0", "istore 1", "istore 2", "istore 3", "lstore 0",
            
            "lstore 1", "lstore 2", "lstore 3", "fstore 0", "fstore 1", "fstore 2", "fstore 3", "dstore 0",
            "dstore 1", "dstore 2", "dstore 3", "astore 0", "astore 1", "astore 2", "astore 3", "iastore",           
            "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop",
            "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap",
            "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub",
            "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv",
            "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg",
            "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land",            
            
            "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d",
            "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l",
            "d2f", "i2b", "idc", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl",
            "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq",
            "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto",
            "jsr", "ret", "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn", "dreturn",
            
            "areturn", "return", "getstatic", "putstatic", "getfield", "putfield", "invokevirtual", "invokespecial",
            "invokestatic", "invokeinterface", "invokedynamic", "new", "newarray", "anewarray", "arraylength",
            "athrow",
            
            "checkcast", "instanceof", "monitorenter", "monitorexit", "wide", "multianewarray", "ifnull", "ifnonnull",
            "goto_w", "jsr_w", null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, };

    /**
     * true if that opcode uses a constant.
     */
    static boolean USESCONSTANT[] = {
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, true, true, true, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,

            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, true, true, true, true, true, true,
            true, true, true, true, false, true, false, false,
            true, true, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false
    };


    final static String PUSH[] = {
            null, "null", "I", "I", "I", "I", "I", "I",
            "I", "J", "J", "F", "F", "F", "D", "D",
            "I", "I", "?", "?", "?", "I", "J", "F",
            "D", "aload", "I", "I", "I", "I", "J", "J",
            "J", "J", "F", "F", "F", "F", "D", "D",
            "D", "D", "aload 0", "aload 1", "aload 2", "aload 3", "I", "J",
            "F", "D", "aaload", "B", "C", "S", null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap",
            "I", "J", "F", "D", "I", "J", "F", "D",
            "I", "J", "F", "D", "I", "J", "F", "D",
            "I", "J", "F", "D", "I", "J", "F", "D",
            "I", "J", "I", "J", "I", "J", "I", "J",
            "I", "J", "I", "J", null, "J", "F", "D",
            "I", "F", "D", "I", "J", "D", "I", "J",
            "F", "B", "C", "S", "Z", "Z", "Z", "Z",
            "Z", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, "getstatic", null, "getfield", null, "invokevirtual", "invokespecial",
            "invokestatic", "invokeinterface", "invokedynamic", "new", "newarray", "anewarray", null, null,
            "checkcast", "Z", null, null, null, "multianewarray", null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
    };

    int opcode;

    private ArrayList<String> stack;

    Instruction(int opcode) {
        this.opcode = opcode;
    }

    public String toString() {
        String name = NAME[opcode];
        if (name != null)
            return "\t" + name;

        return "\topcode " + Integer.toHexString(opcode);
    }

    public void setStack(ArrayList<String> stack) {
        this.stack = stack;
    }

    public int getStackUseCount() {
        if (opcode >= 256)
            return 0;
        return STACKCOUNTTABLE[opcode];
    }

    public String getStackPush(final Code code) {
        if (opcode >= 256)
            return null;
        final String p = PUSH[opcode];
        if (p == null)
            return null;
        if (p.length() == 1)
            return p;
        
        if (p.startsWith("aload")) {
            int i = Integer.parseInt(p.substring(6));
            String t = code.localsMap.get(i).getFirst();
            if ("this".equals(t))
                return code.cd.getClassType() ;
            return t;
        }
        if (p.equals("dup"))
            return "dup";
        return null;
    }

    public String getName() {
        if (opcode >= 256)
            return null;
        final String p = NAME[opcode];
        return p;
    }
}
