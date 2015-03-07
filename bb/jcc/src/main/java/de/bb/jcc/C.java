package de.bb.jcc;

/**
 * Constants.
 * @author sfranke
 *
 */
public interface C {
  static final int TRY = 256;
  static final int CATCH = 257;
  static final int LABEL = 258;
  
  final static int CONSTANT_Dummy = -1;
  final static int CONSTANT_Utf8 = 1;
  final static int CONSTANT_Integer = 3;
  final static int CONSTANT_Float = 4;
  final static int CONSTANT_Long = 5;
  final static int CONSTANT_Double = 6;
  final static int CONSTANT_Class = 7;
  final static int CONSTANT_String = 8;
  final static int CONSTANT_Fieldref = 9;
  final static int CONSTANT_Methodref = 10;
  final static int CONSTANT_InterfaceMethodref = 11;
  final static int CONSTANT_NameAndType = 12;

  final static int CONSTANT_MethodHandle = 15;
  final static int CONSTANT_MethodType = 16;
  final static int CONSTANT_InvokeDynamic = 18;
  
  //Declared public; may be accessed from outside its package. 
  final static int ACC_PUBLIC = 0x0001;

  // Declared private; accessible only within the defining class.  
  final static int ACC_PRIVATE = 0x0002;

  // Declared protected; may be accessed within subclasses.  
  final static int ACC_PROTECTED = 0x0004;

  final static int ACC_STATIC = 0x0008;
  
  final static int ACC_FINAL = 0x0010;

  //Treat superclass methods specially when invoked by the invokespecial instruction. 
  final static int ACC_SYNCHRONIZED = 0x0020;

  final static int ACC_VOLATILE = 0x0040;
  
  final static int ACC_TRANSIENT = 0x0080;

  final static int ACC_NATIVE = 0x0100;
  
  //Is an interface, not a class. 
  final static int ACC_INTERFACE = 0x0200;

  //Declared abstract; may not be instantiated. 
  final static int ACC_ABSTRACT = 0x0400;

}
