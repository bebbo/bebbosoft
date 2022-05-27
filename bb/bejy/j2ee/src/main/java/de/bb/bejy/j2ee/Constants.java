package de.bb.bejy.j2ee;

public interface Constants {

  public final static int CMD_CONNECT = 1;

  public final static int CMD_CREATE = 2;

  public final static int CMD_INVOKE = 3;

  public final static int CMD_LOADSTUB = 4;

  public final static int CMD_GC = 0xfe;

  public final static int CMD_TERMINATE = 0xff;

  /**
   * specifies whether local objects should be used without a fastrmi connection.
   * possible values:
   * <li>only - only local objects are used
   * <li>never - only remote objects are used
   * <li>defuault - first local is tried, then remote
   */
  public static final String LOCAL_SETTING = "FASTRMI_LOCAL";

  /**
   * Used to specify a proxy class which accepts the Principal in ct.
   */
  public static final String PRINCIPAL_HOLDER = "de.bb.rmi.PRINCIPAL_HOLDER";
  /*  
  final static byte T_OBJECT = 0;
  final static byte T_BOOLEAN = 1;
  final static byte T_BYTE = 2;
  final static byte T_CHAR = 3;
  final static byte T_SHORT = 4;
  final static byte T_INT = 5;
  final static byte T_LONG = 6;
  final static byte T_FLOAT = 7;
  final static byte T_DOUBLE = 8;
  final static byte T_STRING = 9;
  */
 public final static int RET_FAILURE = -1;

 public final static int RET_OK = 0;

 public final static int OIS_REFERENCE = 1;

 public final static int OIS_SERIALIZED = 2;

 public final static int OIS_STUB = 3;

 public final static int OIS_EXCEPTION = 0xf;

}