package de.bb.jcc;

import java.util.ArrayList;

class LookupSwitch extends Instruction {

    private Label defaultLabel;
    private ArrayList<Integer> values = new ArrayList<Integer>();
    private ArrayList<Label> cases = new ArrayList<Label>();

    LookupSwitch(Label label) {
        super(171);
        this.defaultLabel = label;
    }

    public void addCase(int value, Label label) {
        values.add(new Integer(value));
        cases.add(label);
    }
}
