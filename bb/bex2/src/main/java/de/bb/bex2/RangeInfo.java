/**
 * written by Stefan Bebbo Franke
 * (c) 1999-2004 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * BebboSoft Lexer.
 */
package de.bb.bex2;

/**
 * @author sfranke
 */
public class RangeInfo {

  private int start;

  private int end;

  private int type;

  /**
   * @param pos
   * @param start
   * @param type
   */
  public RangeInfo(int start, int end, int type) {
    this.start = start;
    this.end = end;
    this.type = type;
  }

  /**
   * @return Returns the pos.
   */
  public int getEnd() {
    return end;
  }

  /**
   * @return Returns the start.
   */
  public int getStart() {
    return start;
  }

  /**
   * @return Returns the type.
   */
  public int getType() {
    return type;
  }

  public boolean equals(Object o) {
    if (!(o instanceof RangeInfo)) return false;
    RangeInfo r = (RangeInfo) o;
    return start == r.start && end == r.end && type == r.type;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    sb.append(start);
    sb.append("-");
    sb.append(end);
    sb.append(") ");
    sb.append(type);
    sb.append("\r\n");
    return sb.toString();
  }

  /**
   * Return the length of the range = end - start.
   * @return the length of the range = end - start.
   */
  public int getLength() {
    return end - start;
  }

  /**
   * @param len
   */
  public void addLength(int len) {
    end += len;
  }

  /**
   * @param len
   */
  public void move(int len) {
    start += len;
    end += len;
  }

}
