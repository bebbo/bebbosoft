/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.util.XmlFile;

public class Xsd2Class {
  private static String spath = "src";
  private static String pack = "de.bb.tools.bnm.setting";
  private static XmlFile xml;
  private static HashSet<String> types = new HashSet<String>();
  private static File path;

  public static void main(String args[]) {

    try {
      // String surl = "http://maven.apache.org/maven-v4_0_0.xsd";
      //String surl = "file:///c:/workspace/bnm/src/maven-v4_0_0.xsd";
      String surl = "settings-1_0_0.xsd";
      if (args.length > 0)
        surl = args[0];

      xml = new XmlFile();

      // read xsd
      URL url = new URL(surl);
      InputStream is = url.openStream();
      xml.read(is);
      is.close();

      
      path = new File(spath, pack.replace('.', '/'));
      for (Iterator i = xml.sections("/xs:schema/xs:complexType"); i.hasNext();) {
        String key = (String) i.next();
        String name = xml.getString(key, "name", null);
        if (name != null && !name.startsWith("xs:")) {
          defineType(key, name);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static void defineType(String key, String otype) throws IOException {
    if (types.contains(otype))
      return;
    types.add(otype);
    System.out.println(key + " -> " + otype);
    FileOutputStream fos = new FileOutputStream(new File(path, otype + ".java"));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos);
    boolean usesHashMap = false;
    boolean usesArrayList = false;

    for (Iterator i = xml.sections(key + "xs:all/xs:element"); i.hasNext();) {
      key = (String) i.next();
      String type = xml.getString(key, "type", null);
      String name = xml.getString(key, "name", null);
      if (name != null && type != null) {
        if (!type.startsWith("xs:")) {
          // defineType(key, type);
        } else {
          if ("xs:string".equals(type)) {
            type = "String";
          } else
          if ("xs:boolean".equals(type)) {
            type = "boolean";
          }
          if ("xs:int".equals(type)) {
            type = "int";
          }
        }
      } else {
        type = "HashMap<String,String>";
        Iterator j = xml.sections(key + "xs:complexType/");
        if (j.hasNext()) {
          String ckey = (String)j.next();
          if (ckey.endsWith("xs:sequence/")) {
            String ctype = xml.getString(ckey + "xs:element", "type", null);
            if (ctype != null) {
              if (ctype.equals("xs:string"))
                ctype = "String";
              type = "ArrayList<" + ctype + ">";
              usesArrayList = true;
            }
          } else {
            System.err.println(ckey);
          }
        }        
      }
      usesHashMap |= type.equals("HashMap<String,String>");
      
      String comment = null;
      for (Iterator n = xml.sections(key + "xs:annotation/xs:documentation");n.hasNext();) {
        String ckey = (String)n.next();
        String a = xml.getString(ckey, "source", null);
        if (a.equals("description")) {
          comment = xml.getContent(ckey);
        }
      }
      if (comment != null) {
        ps.println("  /**");
        for (StringTokenizer st = new StringTokenizer(comment, "\r\n"); st.hasMoreElements();) {
          ps.print("   * ");
          ps.println(st.nextToken().trim());
        }
        ps.println("   */");
      }
      ps.println("  public " + type + " " + name + ";");
    }

    ps.flush();
    ps = new PrintStream(fos);
    ps.println("// generated by Xsd2Class");
    ps.println("package " + pack + ";\r\n");
    if (usesArrayList)
      ps.println("import java.util.ArrayList;");
    if (usesHashMap)
      ps.println("import java.util.HashMap;");
    if (usesArrayList || usesHashMap)
      ps.println();
    ps.println("public class " + otype + " {");
    ps.write(bos.toByteArray());
    ps.println("}");
    ps.close();
  }
}