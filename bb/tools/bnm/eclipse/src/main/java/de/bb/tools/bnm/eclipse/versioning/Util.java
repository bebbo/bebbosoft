/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.versioning;

import java.util.LinkedList;
import java.util.List;

import de.bb.util.MultiMap;

public class Util {

  /**
   * Convert a version into an OSGI compatible version format of major.minor.micro.classifier
   * @param version the version string
   * @return an OSGI compatible version String
   */
  public static String toOsgiVersion(String version) {
    if (version == null)
      return null;
    boolean isSnapshot = version.endsWith("-SNAPSHOT");
    if (isSnapshot)
      version = version.substring(0, version.length() - 9);
    int dot1 = version.indexOf('.');
    int dot2 = version.indexOf('.', dot1 + 1);
    int dot3 = version.indexOf('.', dot2 + 1);
    if (dot2 < dot1)
      dot2 = dot1;
    if (dot3 < dot2)
      dot3 = dot2;
    
    int nonDigit = 0;
    for (;nonDigit < dot3;++nonDigit) {
      char ch = version.charAt(nonDigit);
      if (!(ch >= '0' && ch <= '9') && ch != '.' )
        break;
    }
    
    if (nonDigit < dot3) {
      char ch = version.charAt(nonDigit);
      if (ch == '-') {
        version = version.substring(0, nonDigit) + "." + version.substring(nonDigit + 1);
//        if (isSnapshot)
//          return toOsgiVersion(version + "-SNAPSHOT");
        return toOsgiVersion(version);
      }
    }

    // less than 3 dots
    if (dot3 == dot2) {
//      if (isSnapshot)
//        return version + ".SNAPSHOT";
      return version;
    }
    // 3 dots or even more!?
    String rest = version.substring(dot3 + 1).replace('.', '-');
//    if (isSnapshot)
//      rest += "-SNAPSHOT";

    return version.substring(0, dot3 + 1) + rest;
  }

  public static List<String> nextSnapshots(String value) {
      LinkedList<String> list = new LinkedList<String>();
      String unsnapshot = null;
      if (value.endsWith("-SNAPSHOT"))
        unsnapshot = value = value.substring(0, value.length() - 9);
      // find numbers
      char ch [] = value.toCharArray();
      StringBuffer v = new StringBuffer();
      for (int i = 0; i < ch.length; ++i) {
        if (ch[i] >= '0' && ch[i] <= '9') {
          int start = i;
          int end = i + 1;
          for (;end < ch.length;++end) {
            if (ch[end] < '0' || ch[end] > '9')
              break;
          }
          int n = Integer.valueOf(value.substring(start, end));
          StringBuffer sb = new StringBuffer();
          sb.append(v).append(n + 1);
          v.append(n);
          while (end < ch.length && (ch[end] < '0' || ch[end] > '9')) {
            sb.append(ch[end]);
            v.append(ch[end]);
            ++end;          
          }
          i = end - 1;
          // null all other numbers
          while (end < ch.length) {
            
            if (ch[end] >= '0' && ch[end] <= '9') {
              sb.append("0");
              while (end < ch.length && ch[end] >= '0' && ch[end] <= '9') {
                ++end;
              }
              continue;
            }
            sb.append(ch[end]);
            ++end;
          }
          sb.append("-SNAPSHOT");
          list.addFirst(sb.toString());
        }
      }
      if (unsnapshot != null)
    	  list.addFirst(unsnapshot);
      return list;
    }

  @SuppressWarnings("unchecked")
  public static <T> T newTypedMultiMap() {
    return (T) new MultiMap();
  }
}
