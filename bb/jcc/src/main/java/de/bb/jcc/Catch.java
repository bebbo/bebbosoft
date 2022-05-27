package de.bb.jcc;

class Catch extends Label {

  private ConstantPool cp;
  private int exceptionType;

  Catch(int exceptionType, int no, ConstantPool cp) {
    super(C.CATCH, no);
    this.exceptionType = exceptionType;
    this.cp = cp;
  }

  public String toString() {
    String constant = cp.getConstant(exceptionType);
    return super.toString() + "\t__catch " + constant;
  }

  
}
