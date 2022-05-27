package de.bb.jcc;

/**
 * Helper class for gotos which are resolved at writing.
 * 
 * @author sfranke
 * 
 */
class Goto {
    boolean isGoto;

    boolean isUnconditional;

    String label;

    int offset;

    int stack;

    boolean touched;

    /**
     * @param isGoto
     * @param isUnconditional
     * @param label
     * @param offset
     * @param stack
     */
    Goto(boolean isGoto, boolean isUnconditional, String label, int offset, int stack) {
        this.isGoto = isGoto;
        this.isUnconditional = isUnconditional;
        this.label = label;
        this.offset = offset;
        this.stack = stack;
    }

    public String toString() {
        if (isGoto)
            return "\tgoto " + label;
        return label + ":";
    }
}