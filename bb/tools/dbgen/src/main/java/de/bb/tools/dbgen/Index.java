package de.bb.tools.dbgen;

import java.util.ArrayList;

import de.bb.util.ByteRef;

/**
 * Information for an index.
 * @author stefan franke
 *
 */
class Index {

  ArrayList<String> fields = new ArrayList<String>();
  String name;
  boolean isUnique;
  
  /**
   * CT.
   * @param indexFields
   */
  Index(ByteRef name, ByteRef indexFields, boolean isUnique) {
    if (name.toLowerCase().startsWith("idx_"))
      name = name.substring(4);
    this.name = name.toString();
    
    while (indexFields.length() > 0) {
      ByteRef field = indexFields.nextWord(',');
      field = field.nextWord(' ').trim();
      fields.add(field.toString());
    }
    
    this.isUnique = isUnique;
  }

  
  /**
   * verbosify.
   */
  public String toString() {
    return "index: " + fields;
  }
}
