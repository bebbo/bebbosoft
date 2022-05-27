package de.bb.jcc;

class Label extends Instruction {
    int no;
    boolean isLoop;
    Label afterLoop;
    boolean isIf;
    boolean isElse;
    public int ifCount;

    Label(int no) {
        super(C.LABEL);
        this.no = no;
    }

    Label(int opcode, int no) {
        super(opcode);
        this.no = no;
    }

    public String toString() {
        if (no < 0)
            return "";
        String r = getName() + ":";
        if (isLoop) {
            r += " while(true) {";
        } else if (isElse) {
            r += " } else";
        } else if (ifCount > 0) {
            for (int i = 0; i < ifCount; ++i) {
                r += " }";
            }
        } else {
            //r += " {";
        }
        return r;
    }

    public String getName() {
        return "L" + no;
    }
}
