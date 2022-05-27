package de.bb.jcc;

class NewArray extends Instruction {

    private int arrayType;

    NewArray(int arrayType) {
        super(0xbc);
        this.arrayType = arrayType;
    }

    int getArrayType() {
        return arrayType;
    }

}
