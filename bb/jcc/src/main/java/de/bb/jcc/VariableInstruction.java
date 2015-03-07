package de.bb.jcc;

class VariableInstruction extends Instruction {

    private Instruction i;
    private String name;

    public VariableInstruction(Instruction i, String name) {
        super(i.opcode);
        this.i = i;
        this.name = name;
    }

    public String toString() {
        return "\t" + name;
    }
}
