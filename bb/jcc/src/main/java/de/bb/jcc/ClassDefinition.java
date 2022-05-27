package de.bb.jcc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import de.bb.jcc.ConstantPool.Entry;

/**
 * Used to create and modify a Java class. A Java class maintains its constant pool, where all constant data of all
 * methods, fields etc. is held. There is also a package local ct to support shared constant pools (idea: the MUG file
 * format: all classes of a JAR with one shared constant pool)
 * 
 * @author bebbo
 */
public class ClassDefinition {
    ConstantPool cp;

    /** @see interface C for constants. */
    private int access;

    /** constant id for this class name. */
    private int thisClass;

    /** constant id for parent class name. */
    private int superClass;

    /** constant id for signature. */
    private int singatureIndex;

    /** constant id for source file name. */
    private int sourceFileIndex;

    private ArrayList<Integer> interfaces = new ArrayList<Integer>();

    private ArrayList<Method> methods = new ArrayList<Method>();

    private ArrayList<Field> fields = new ArrayList<Field>();

    boolean addStackMap;

    ClassDefinition(ConstantPool cp) {
        this.cp = cp;
    }

    /**
     * Create class definition with java.lang.Object as parent.
     * 
     * @param className
     *            the class name, e.g. foo.bar.Sample
     */
    public ClassDefinition(String modifier, String className) {
        this(modifier, className, "java.lang.Object");
    }

    /**
     * Create class definition with given parent class.
     * 
     * @param className
     *            the class name, e.g. foo.bar.Sample
     * @param superClassName
     *            the class name, e.g. foo.bar.SampleBase
     */
    public ClassDefinition(String modifier, String className, String superClassName) {
        cp = new ConstantPool();
        setAccess(modifier);
        setClassName(className);
        setSuperClassname(superClassName);
    }

    /**
     * Change the access modifier.
     * 
     * @param modifier
     *            the modifiers. @see C for constants.
     */
    public void setAccess(String modifier) {
        access = Util.modifier2Access(modifier);
    }

    /**
     * Change the access modifier.
     * 
     * @param access
     *            the binary modifiers. @see C for constants.
     */
    public void setAccess(int access) {
        this.access = access;
    }

    void setClassName(String className) {
        className = Util.dot2Slash(className);
        thisClass = cp.addClass(className);
    }

    void setSuperClassname(String superClassName) {
        superClassName = Util.dot2Slash(superClassName);
        superClass = cp.addClass(superClassName);
    }

    /**
     * @param os
     * @throws IOException
     */
    public void write(OutputStream os) throws IOException {
        cp.addUTF8("Code");
        if (addStackMap)
            cp.addUTF8("StackMap");

        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(0xCAFEBABE); // magic
        dos.writeShort(0x03); // version hi
        dos.writeShort(0x2d); // version lo

        cp.writeTo(dos); // write the constant pool

        dos.writeShort(access);
        dos.writeShort(thisClass);
        dos.writeShort(superClass);

        dos.writeShort(interfaces.size());
        for (Integer ii : interfaces) {
            dos.writeShort(ii.intValue());
        }

        dos.writeShort(fields.size());
        for (Field f : fields) {
            f.writeTo(dos);
        }

        dos.writeShort(methods.size());
        for (Method m : methods) {
            m.writeTo(dos);
        }

        // TODO: support inner classes
        // if (innerClasses == null)
        {
            dos.writeShort(0);
        }
    }

    /**
     * Enable StackMap generation.
     */
    public void addStackMap() {
        addStackMap = true;
    }

    /**
     * Method addInterface.
     * 
     * @param interfaceName
     */
    public void addInterface(String interfaceName) {
        interfaces.add(new Integer(cp.addClass(interfaceName)));
    }

    int xaddMethodRef(String cName, String name, String type) {
        return cp.addMethod(cName, name, type);
    }

    /**
     * Define a method.
     * 
     * @param modifiers
     *            the modifiers, like public, protected ...
     * @param name
     *            the name of the method
     * @param signature
     *            the type
     * @param code
     *            the code for the method.
     */
    public Method defineMethod(String modifiers, String name, String signature, Code code) {
        return defineMethod(modifiers, name, signature, code, null);
    }

    /**
     * Define a method.
     * 
     * @param access
     *            the binary modifiers.
     * @param name
     *            the name of the method
     * @param signature
     *            the type
     * @param code
     *            the code for the method.
     */
    public Method defineMethod(int access, String name, String signature, Code code) {
        return defineMethod(access, name, signature, code, null);
    }

    /**
     * Define a method.
     * 
     * @param modifiers
     *            the modifiers, like public, protected ...
     * @param name
     *            the name of the method
     * @param signature
     *            the method signature
     * @param code
     *            the code for the method.
     * @param exs
     *            an array with Exception types
     */
    public Method defineMethod(String modifiers, String name, String signature, Code code, String[] exs) {
        int access = Util.modifier2Access(modifiers);
        return defineMethod(access, name, signature, code, exs);
    }

    /**
     * Define a method.
     * 
     * @param access
     *            the binary modifiers.
     * @param methodName
     *            the name of the method
     * @param signature
     *            the method signature
     * @param code
     *            the code for the method.
     * @param exs
     *            an array with Exception types
     */
    public Method defineMethod(int access, String methodName, String signature, Code code, String[] exs) {
        int indexName = cp.addUTF8(methodName);
        int indexSig = cp.addUTF8(signature);

        code.setClassDefinition(this);
        Method m = new Method(this, access, indexName, indexSig, code);
        if (exs != null)
            m.setExceptions(exs);
        methods.add(m);
        return m;
    }

	public Method getMethod(String ctLambdaName, String signature) {
		for (final Method m : methods) {
			if (m.getName().equals(ctLambdaName) && m.getSignature().equals(signature))
				return m;
		}
		return null;
	}

    
    /**
     * Define a field.
     * 
     * @param modifiers
     *            the modifiers, like public, protected ...
     * @param name
     *            the name of the method
     * @param type
     *            the type
     * @return
     */
    public Field defineField(String modifiers, String name, String type) {
        int access = Util.modifier2Access(modifiers);
        return defineField(access, name, type);
    }

    /**
     * Define a field.
     * 
     * @param access
     *            the binary modifiers.
     * @param name
     *            the name of the method
     * @param type
     *            the type
     * @return
     */
    public Field defineField(int access, String name, String type) {
        type = Util.dot2Slash(type);
        int i = cp.addUTF8(name);
        int j = cp.addUTF8(type);
        Field f = new Field(access, i, j, cp);
        fields.add(f);
        return f;
    }

    /**
     * Method createCode.
     * 
     * @return Code
     */
    public Code createCode() {
        return new Code(cp);
    }

    public String getClassName() {
        return Util.slash2Dot(getClassType());
    }
    
    public String getClassType() {
        Entry e = cp.getEntry(thisClass);
        return cp.getConstant(e.iVal1);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String className = getClassName();
        int dot = className.lastIndexOf('.');
        if (dot > 0) {
            sb.append("package ").append(className.substring(0, dot)).append(";\r\n");
            className = className.substring(dot + 1);
        }
        if (singatureIndex != 0) {
            sb.append("// ").append(cp.getConstant(singatureIndex)).append("\r\n");
        }
        sb.append(Util.access2Modifier(access));
        sb.append("class ");
        sb.append(className);
        sb.append(" extends ");
        sb.append(Util.slash2Dot(cp.getConstant(superClass)));

        if (interfaces.size() > 0) {
            sb.append("\r\n  implements ");
            for (Iterator<Integer> i = interfaces.iterator(); i.hasNext();) {
                Integer ii = i.next();
                sb.append(Util.slash2Dot(cp.getConstant(ii.intValue())));
                if (i.hasNext())
                    sb.append(", ");
            }
        }
        sb.append(" {\r\n");

        // fields
        for (Field f : fields) {
            sb.append(f.toString()).append(";\r\n");
        }

        // methods
        for (Method m : methods) {
            sb.append(m.toString());
        }

        sb.append("}\r\n");
        return sb.toString();
    }

    public Iterator<Method> methods() {
        return methods.iterator();
    }

    public ConstantPool getConstantPool() {
        return cp;
    }

    void setSignature(int signatureIndex) {
        this.singatureIndex = signatureIndex;
    }

    void setSourceFile(int sourceFileIndex) {
        this.sourceFileIndex = sourceFileIndex;
    }

    public void decompile() {
        for (Method m : methods) {
            m.decompile();
        }
    }

	public void defineCtLambdaMethod(String ctLambdaName, String signature) {
		// TODO Auto-generated method stub
		
	}
}