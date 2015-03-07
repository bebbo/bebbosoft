package de.bb.jcc;

import java.io.DataOutputStream;
import java.io.IOException;

class Field {
    int access;
    int nameIndex;
    int typeIndex;
    private int constant;
    private ConstantPool cp;

    Field(int access, int nameIndex, int typeIndex, ConstantPool cp) {
      this.access = access;
      this.nameIndex = nameIndex;
      this.typeIndex = typeIndex;
      this.cp = cp;
    }

    /**
     * Method writeTo.
     * 
     * @param dos
     * @throws IOException
     */
    public void writeTo(DataOutputStream dos) throws IOException {
      dos.writeShort(access);
      dos.writeShort(nameIndex);
      dos.writeShort(typeIndex);
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("  ").append(Util.access2Modifier(access));
      sb.append(Util.signature2Type(cp.getConstant(typeIndex)));
      sb.append(" ");
      sb.append(cp.getConstant(nameIndex));

      if (constant > 0)
        sb.append(" = ").append(cp.getConstant(constant));

      return sb.toString();
    }

    void setConstant(int v) {
      this.constant = v;
    }

    public String getName() {
        return cp.getConstant(nameIndex);
    }
  }