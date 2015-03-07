package de.bb.jcc;

class Instruction2Param extends Instruction {
  private int val1;
  private int val2;

  Instruction2Param(int opcode, int val1, int val2) {
    super(opcode);
    this.val1 = val1;
    this.val2 = val2;
  }

  public String toString() {
    return super.toString() + " " + val1 + ", " + val2;
  }

  
}
