package de.bb.jcc;

class DecompileStackEntry {

    DecompileInstruction di;
    String type;

    public DecompileStackEntry(DecompileInstruction di, String type) {
        this.di = di;
        this.type = type;
    }

}
