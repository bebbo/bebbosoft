/* 
 * Created on 07.11.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.bejy.mail.spf;

import de.bb.bex2.ParseException;

/**
 * @author sfranke
 */
public class SpfException extends ParseException
{

  private int prefix;

  /**
   * @param prefix
   */
  public SpfException(int prefix)
  {
    super("match");
    this.prefix = prefix;
  }

  /**
   * @return
   */
  public int getPrefix()
  {
    return prefix;
  }
}
