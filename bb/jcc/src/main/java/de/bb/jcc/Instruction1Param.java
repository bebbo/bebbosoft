package de.bb.jcc;

class Instruction1Param extends Instruction {

    private int p1;

    Instruction1Param(int opcode, int val1) {
        super(opcode);
        this.p1 = val1;
    }

    public String toString() {
        return super.toString() + " " + p1;
    }

    public int getP1() {
        return p1;
    }

}
