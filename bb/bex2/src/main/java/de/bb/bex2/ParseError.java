/**
 * written by Stefan Bebbo Franke
 * (c) 1999-2004 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * BebboSoft Lexer.
 */
package de.bb.bex2;

/**
 * @author bebbo
 */
public class ParseError
{
  private String fileName, errorMessage;
  private int errorOffset;
  private int errorEnd;
  private int errorLine;

  /**
   * @param fileName
   * @param errorMessage
   * @param errorOffset
   * @param errorLength
   */
  public ParseError(String fileName, String errorMessage, int errorOffset, int errorEnd, int errorLine)
  {
    this.fileName = fileName;
    this.errorMessage = errorMessage;
    this.errorOffset = errorOffset;
    this.errorEnd = errorEnd;
    this.errorLine = errorLine;
  }

  /**
   * @return Returns the errorEnd.
   */
  public int getEnd()
  {
    return errorEnd;
  }
  /**
   * @return Returns the errorMessage.
   */
  public String getMessage()
  {
    return errorMessage;
  }
  /**
   * @return Returns the errorOffset.
   */
  public int getStart()
  {
    return errorOffset;
  }
  /**
   * @return Returns the fileName.
   */
  public String getFileName()
  {
    return fileName;
  }
  
  public String toString()
  {
    return errorMessage + " @ " + fileName + "("+ errorOffset + "-" + errorEnd + ")";
  }
  /**
   * @return Returns the errorLine.
   */
  public int getLine()
  {
    return errorLine;
  }

  /**
   * @param end
   */
  public void setEnd(int end) {
    this.errorEnd = end;
  }
}