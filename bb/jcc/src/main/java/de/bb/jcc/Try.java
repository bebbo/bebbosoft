package de.bb.jcc;

class Try extends Label {

    private Catch iCatch;

    Try(Catch iCatch, int no) {
        super(C.TRY, no);
        this.iCatch = iCatch;
    }

    public String toString() {
        return super.toString() + "\t__try";
    }

}
