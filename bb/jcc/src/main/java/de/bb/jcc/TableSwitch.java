package de.bb.jcc;

import java.util.ArrayList;

class TableSwitch extends Instruction {

  private int lo;
  private int hi;
  private Label defaultLabel;
  private ArrayList cases = new ArrayList();

  TableSwitch(int lo, int hi, Label label) {
    super(170);
    this.lo = lo;
    this.hi = hi;
    this.defaultLabel = label;
  }

  public void addCase(Label label) {
    cases.add(label);
  }

}
