package de.bb.tools.dbgen;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public abstract class CodeGen {

  /**
   * create an PrintWriter for the class using the extension.
   * @param path the used path
   * @param pack the used package
   * @param ext an extension
   * @return a PrintWriter
   * @throws Exception on error
   */
  protected PrintWriter createOs(String classname, String path, String pack, String ext) throws Exception {
    return DbGen.createPw(path, pack, classname + ext);
  }

  /**
   * checks whether file does not yet exist.
   * @param path the used path
   * @param pack the used package
   * @param ext an extension
   * @return true when file does not exist
   * @throws Exception on error
   */
  protected boolean checkOs(String classname, String path, String pack, String ext) throws Exception {
    String p2 = DbGen.package2Path(pack);
    File d = new File(path, p2);
    File f = new File(d, classname + ext);
    return !f.exists();
  }

  /**
   * Must be implemented to create specific code.
   * @param tables
   * @param path
   * @param params
   * @param verbose
   * @throws Exception
   */
  public abstract void processTables(HashMap<String, Table> tables, String path, Map<String, String> params, boolean verbose)
      throws Exception;
}
