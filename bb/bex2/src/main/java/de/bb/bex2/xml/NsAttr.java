/* 
 * Created on 25.07.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.bex2.xml;

/**
 * @author sfranke
 */
public abstract class NsAttr
{

  /**
   * @param value
   * @return
   */
  public abstract String validate(String value);

  /**
   * @return
   */
  public abstract boolean isRequired();
}
