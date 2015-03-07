package de.bb.jcc;

class Jump extends Instruction {

    private Label label;

    Jump(int opcode, Label label) {
        super(opcode);
        this.label = label;
    }

    public String toString() {
        return super.toString() + " " + getLabel();
    }

    String getLabel() {
        return "L" + label.no;
    }

    boolean isGoto() {
        return this.opcode == 0xa7;
    }
}
