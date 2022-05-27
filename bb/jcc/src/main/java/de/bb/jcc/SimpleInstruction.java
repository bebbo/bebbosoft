package de.bb.jcc;

class SimpleInstruction extends Instruction {

  private byte [] data;

  SimpleInstruction(int opcode, byte[] code, int offset, int length) {
    super(opcode);
    if (length > 0)
      this.data = Util.copy(code, offset, length);
  }

  public String toString() {
    if (data == null)
      return super.toString();
    return super.toString() + " " + Util.dump(data);
  }
  

}
