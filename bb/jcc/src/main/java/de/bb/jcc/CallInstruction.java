package de.bb.jcc;

import de.bb.jcc.ConstantPool.Entry;

class CallInstruction extends ConstantInstruction {

    CallInstruction(int opcode, int index, ConstantPool cp) {
        super(opcode, index, cp);
    }

    
    public String getStackPush(final Code code) {
        final Entry e1 = cp.getEntry(index);
        final Entry e2 = cp.getEntry(e1.iVal2);
        final String sig = cp.getConstant(e2.iVal2);
        int rbrace = sig.indexOf(')');
        final String p = sig.substring(rbrace + 1);
        if ("V".equals(p))
            return null;
        return p;
    }


    @Override
    public int getStackUseCount() {
        final Entry e1 = cp.getEntry(index);
        final Entry e2 = cp.getEntry(e1.iVal2);
        final String sig = cp.getConstant(e2.iVal2);
        int n = Util.countStackParams(sig);
        if (opcode != 0xb8)
            ++n; // add this parameter
        return n;
    }
    
    
}
