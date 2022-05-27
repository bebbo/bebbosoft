package de.bb.tools.dbgen;

import java.util.HashMap;
import java.util.HashSet;

import de.bb.util.ByteRef;

/**
 * Helper class to store a tables data
 */
class Table {
  /** name of the table */
  ByteRef tableName;

  /** the used classname */
  String classname;

  /** the table rows */
  HashMap<String, Row> rows = new HashMap<String, Row>();

  HashMap<String, String> uniques = new HashMap<String, String>();

  HashMap<String, Index> indexes = new HashMap<String, Index>();

  HashSet<String> refNames = new HashSet<String>();

  HashMap<String, String> refNameMap = new HashMap<String, String>();

  /**
   * Creates a new table object.
   * @param n the table name
   */
  Table(ByteRef n) {
    tableName = n;
    classname = tableName.toString();
    if (classname.startsWith(DbGen.prefix))
      classname = classname.substring(DbGen.prefix.length());

    classname = DbGen.nice(classname);
  }

  /**
   * Adds a row to the table.
   * @param n the row name
   * @param t the row type
   * @param s1 the rows size or null
   * @param s2 the rows 2nd size or null
   */
  void addRow(ByteRef n, ByteRef t, ByteRef s1, ByteRef s2) {
    rows.put(n.toString(), new Row(this, n, t, s1, s2));
  }

  void markAsUnique(ByteRef line) {
    String l = line.toLowerCase().toString();
    uniques.put(l, l);
  }

  /**
   * verbose presentation for the table.
   */
  public String toString() {
    return this.tableName.toString();
  }

  /**
   * Add an index for this table
   * @param indexName
   * @param indexFields
   */
  public void addIndex(ByteRef indexName, ByteRef indexFields, boolean isUnique) {
    Index index = new Index(indexName, indexFields, isUnique);
    indexes.put(indexName.toString(), index);
  }
}