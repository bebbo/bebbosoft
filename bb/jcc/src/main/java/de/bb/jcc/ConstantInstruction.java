package de.bb.jcc;

import de.bb.jcc.ConstantPool.Entry;

class ConstantInstruction extends Instruction {

    int index;
    ConstantPool cp;

    ConstantInstruction(int opcode, int index, ConstantPool cp) {
        super(opcode);
        this.index = index;
        this.cp = cp;
    }

    public String toString() {
        String s = super.toString() + " " + cp.getConstant(index);
        return s;
    }

    int getIndex() {
        return index;
    }

    public String getStackPush(final Code code) {
        final String x = PUSH[opcode];
        if (x == null)
            return null;
        
        final Entry e = cp.getEntry(index);
        final String p = cp.getConstant(index);
        if (e.kind == C.CONSTANT_String)
            return "Ljava/lang/String;";
        if (e.kind == C.CONSTANT_Class) {
            if ("new".equals(x))
                return "L" + p + ";";
            return "Ljava/lang/Class;";
        }
        
        return p;
    }
}
