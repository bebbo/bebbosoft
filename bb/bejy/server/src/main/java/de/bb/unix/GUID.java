package de.bb.unix;

/**
 * Helper class to change the uid and gid.
 * @author Stefan Bebbo Franke
 */
public class GUID {
  static {
    System.loadLibrary("de-bb-unix-guid");
  }
  public native static boolean setGUID(int uid, int gid);
} 