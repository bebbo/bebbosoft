/**
 * 
 */
package de.bb.jcc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Method {
    /**
   * 
   */
    private ClassDefinition classDefinition;

    int methodAccess;

    int indexName;
    int indexSig;
    ArrayList<Integer> exceptions;
    Code code;

    /**
     * Constructor Method.
     * 
     * @param modifiers
     * @param i
     * @param j
     * @param code
     * @param classDefinition
     *            TODO
     */
    Method(ClassDefinition classDefinition, int access, int indexName, int indexSig, Code code) {
        this.classDefinition = classDefinition;
        this.methodAccess = access;
        this.indexName = indexName;
        this.indexSig = indexSig;
        this.code = code;
        
        String thisType = null;
        if ((access & C.ACC_STATIC) == 0) {
            thisType = classDefinition.getClassName();
        }
        code.setParams(classDefinition.cp.getConstant(indexSig), thisType);
    }

    /**
     * Method setExceptions.
     * 
     * @param exs
     */
    public void setExceptions(String[] exs) {
        if (exs == null || exs.length == 0) {
            exceptions = null;
            return;
        }

        exceptions = new ArrayList<Integer>();
        this.classDefinition.cp.addUTF8("Exceptions");
        for (int i = 0; i < exs.length; ++i) {
            int ei = this.classDefinition.cp.addClass(exs[i]);
            exceptions.add(new Integer(ei));
        }
    }

    /**
     * Method writeTo.
     * 
     * @param dos
     */
    void writeTo(DataOutputStream dos) throws IOException {
        dos.writeShort(methodAccess); // ACC_PUBLIC

        dos.writeShort(indexName);
        dos.writeShort(indexSig);

        int noAttr = 1;

        if (exceptions != null)
            ++noAttr;

        if (this.classDefinition.addStackMap)
            ++noAttr;

        dos.writeShort(noAttr); // attribute
        code.writeCode(dos);
        if (exceptions != null) {
            dos.writeShort(this.classDefinition.cp.addUTF8("Exceptions"));
            int sz = exceptions.size();
            dos.writeInt(2 + 2 * sz);
            dos.writeShort(sz);
            for (Iterator<Integer> i = exceptions.iterator(); i.hasNext();) {
                Integer ei = i.next();
                dos.writeShort(ei.shortValue());
            }
        }
        // add stackMap if necessary
        if (this.classDefinition.addStackMap) {
            code.writeStackMap(dos);
        }
    }

    public String getName() {
        return code.cp.getConstant(indexName);
    }
    
    public String getSignature() {
        return code.cp.getConstant(indexSig);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ConstantPool cp = code.cp;
        String sig = cp.getConstant(indexSig);
        String name = cp.getConstant(indexName);
        sb.append("  ").append(Util.access2Modifier(methodAccess));
        if ("<init>".equals(name))
            sb.append(classDefinition.getClassName());
        else {
            sb.append(Util.returnType(sig)).append(" ");
            sb.append(name);
        }
        ArrayList<String> paramNames = code.getLocals();
        sb.append(Util.parameterList(sig, paramNames)).append("\r\n");
        code.localCount = paramNames.size();

        if (exceptions != null) {
            sb.append("    throws ");
            for (Iterator<Integer> j = exceptions.iterator(); j.hasNext();) {
                Integer ei = j.next();
                sb.append(Util.signature2Type(cp.getConstant(ei.intValue()))).append("\r\n");
                if (j.hasNext())
                    sb.append(", ");
            }
        }
        sb.append("  {\r\n");
        sb.append(code.toString());
        sb.append("  }\r\n");
        return sb.toString();
    }

    public void setSignature(String sig) {

    }

    public void decompile() {
        code.decompile();
    }

	public void downgradeLambda() {
		code.downgradeLambda();
	}

}