package de.bb.tools.dbgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bb.util.XmlFile;

public class TemplateGen extends CodeGen {

  private Map<String, String> params;
  private Map<String, Map<String, String>> kindMap = new HashMap<String, Map<String, String>>();
  private HashMap<File, PrintWriter> f2pw = new HashMap<File, PrintWriter>();
  private String template;
  private String path;

  TemplateGen(String template) throws IOException {
    this.template = template;
  }

  private String convert(String text) {
    StringBuffer sb = new StringBuffer();
    while (true) {
      int pos = text.indexOf("${");
      if (pos < 0) {
        sb.append(text);
        break;
      }
      sb.append(text.substring(0, pos));
      text = text.substring(pos + 2);
      pos = text.indexOf('}');
      if (pos < 0) {
        sb.append("${");
        continue;
      }
      String key = text.substring(0, pos);
      text = text.substring(pos + 1);

      String regex = null;
      String repl = null;
      int slash = key.indexOf('/');
      if (slash > 0) {
        regex = key.substring(slash + 1);
        key = key.substring(0, slash);
        slash = regex.indexOf('/');
        repl = regex.substring(slash + 1);
        if (slash > 0)
          regex = regex.substring(0, slash);
      }
      
      
      Map<String, String> localParams = params;
      int colon = key.indexOf(':');
      String kind = null;
      if (colon > 0) {
        kind = key.substring(colon + 1);
        key = key.substring(0, colon);
      }
      String o = localParams != null ? localParams.get(key) : null;
      if (o == null) {
        sb.append("[[missing value for: " + key + "]]");
        continue;
      }
      if (kind != null) {
        Map<String, String> kp = kindMap.get(kind);
        if (kp == null) {
          sb.append("[[missing kind map for: " + kind + "]]");
          continue;
        }
        String o2 = kp.get(o);
        if (o2 == null) {
          sb.append("[[missing mapping for: " + o + " in " + kind + "]]");
          continue;
        }
        o = o2;
      }
      
      if (regex != null) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(o.subSequence(0, o.length()));
        String r = m.replaceAll(repl);
        System.out.println(r);
      }
      
      sb.append(o);
    }
    return sb.toString();
  }

  @Override
  public void processTables(HashMap<String, Table> tables, String path, Map<String, String> params, boolean verbose)
      throws Exception {
    this.params = params;
    this.path = path;

    if (verbose)
      System.out.println("using template " + template);

    XmlFile xml = new XmlFile();
    xml.readFile(template);

    for (Iterator<String> i = stringIterator(xml.sections("/dbgen/mapping")); i.hasNext();) {
      String key = i.next();
      String name = xml.getString(key, "name", null);
      String value = xml.getString(key, "value", null);
      String kind = xml.getString(key, "kind", null);
      if (name == null || value == null)
        continue;

      if (verbose)
        System.out.println(name + "=" + value);

      if (kind == null) {
        params.put(name, value);
        continue;
      }
      Map<String, String> kp = kindMap.get(kind);
      if (kp == null) {
        kp = new HashMap<String, String>();
        kindMap.put(kind, kp);
      }
      kp.put(name, value);
    }

    for (Iterator<String> i = stringIterator(xml.sections("/dbgen/file/")); i.hasNext();) {
      String itemKey = i.next();
      String fileName = xml.getString(itemKey, "fileName", "undefined");

      // global
      if (itemKey.indexOf("/global") > 0) {
        String content = convert(xml.getContent(itemKey));
        PrintWriter pw = getPw(fileName);
        pw.print(content);
        continue;
      }
      // table based
      if (itemKey.indexOf("/table") > 0) {
        for (Table t : tables.values()) {
          String tableName = t.tableName.toString();
          params.put("tableName", tableName);
          params.put("TABLENAME", tableName.toUpperCase());
          String content = convert(xml.getContent(itemKey));
          PrintWriter pw = getPw(fileName);
          pw.print(content);
        }
        continue;
      }
      // index based
      if (itemKey.indexOf("/index") > 0) {
        Map<String, String> ctypes = kindMap.get("ctype");
        for (Table t : tables.values()) {
          String tableName = t.tableName.toString();
          params.put("tableName", tableName);
          for (Index idx : t.indexes.values()) {
            params.put("indexName", idx.name);
            params.put("IndexName", firstUp(idx.name));
            StringBuilder sbFields = new StringBuilder();
            StringBuilder sbFieldsU = new StringBuilder();
            StringBuilder sbCParamFields = new StringBuilder();
            for (String f : idx.fields) {
              if (sbFields.length() > 0) {
                sbFields.append(", ");
                sbCParamFields.append(", ");
              }
              Row r = t.rows.get(f);
              String fl = firstLow(r.refName);
              sbFields.append(r.refName);
              sbFieldsU.append(r.refName.toUpperCase());
              sbCParamFields.append(ctypes.get(r.type.toString())).append(" const & ").append(fl);
            }
            params.put("indexFields", sbFields.toString());
            params.put("INDEXFIELDS", sbFieldsU.toString());
            params.put("indexCParams", sbCParamFields.toString());
            String content = convert(xml.getContent(itemKey));
            PrintWriter pw = getPw(fileName);
            pw.print(content);
          }
        }
        continue;
      }
      // field based
      if (itemKey.indexOf("/field") > 0) {
        for (Table t : tables.values()) {
          String tableName = t.tableName.toString();
          params.put("tableName", tableName);
          params.put("TABLENAME", tableName.toUpperCase());
          boolean first = true;
          for (Row row : t.rows.values()) {
            params.put("CTDELI", first ? ":" : ",");
            first = false;
            params.put("rowName", row.rowName);
            params.put("refName", firstLow(row.refName));
            params.put("ROWNAME", row.rowName.toUpperCase());
            params.put("REFNAME10", row.getRefName(10).toUpperCase());

            String rowType = row.type.toString();
            params.put("rowType", rowType);

            String rowLen = Integer.toString(row.size1);
            if (rowLen.equals("0")) {
              Map<String, String> tp = kindMap.get("defaultLen");
              String v = tp.get(rowType);
              if (v != null)
                rowLen = v;
            }
            params.put("rowLen", rowLen);
            params.put("rowLen2", Integer.toString(row.size2));
            String content = convert(xml.getContent(itemKey));
            PrintWriter pw = getPw(fileName);
            pw.print(content);
          }
        }
        continue;
      }
      
      // index based
      if (itemKey.indexOf("/index") > 0) {
        for (Table t : tables.values()) {
          String tableName = t.tableName.toString();
          params.put("tableName", tableName);
          params.put("TABLENAME", tableName.toUpperCase());
          for (Index idx : t.indexes.values()) {
            StringBuilder sb = new StringBuilder();
            for (String f : idx.fields) {
              sb.append(firstUp(f));
            }
            params.put("indexFields", sb.toString());
            String content = convert(xml.getContent(itemKey));
            PrintWriter pw = getPw(fileName);
            pw.print(content);
          }
        }
        continue;
      }
      
    }

    // close the files
    for (PrintWriter pw : f2pw.values()) {
      pw.close();
    }
  }

  private PrintWriter getPw(String fileName) throws FileNotFoundException {
    File d = new File(path);
    fileName = convert(fileName);
    File f = new File(d, fileName);
    PrintWriter pw = f2pw.get(f);
    if (pw == null) {
      try {
        f.getParentFile().mkdirs();
        pw = new PrintWriter(new FileOutputStream(f));
      } catch (FileNotFoundException e) {
        throw e;
      }
      f2pw.put(f, pw);
    }
    return pw;
  }

  private static String firstUp(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  private static String firstLow(String name) {
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  @SuppressWarnings("unchecked")
  private Iterator<String> stringIterator(Iterator sections) {
    return sections;
  }

}
