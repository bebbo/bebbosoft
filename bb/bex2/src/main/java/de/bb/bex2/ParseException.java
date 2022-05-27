/*
 * Created on 29.06.2004
 */
package de.bb.bex2;

/**
 * @author sfranke
 */
public class ParseException extends Exception
{

  /**
	* Comment for <code>serialVersionUID</code>
	*/
	private static final long serialVersionUID = 3546359522345432371L;

/**
   * @param string
   */
  public ParseException(String msg)
  {
    super(msg);
  }

}
