/*
 * Created on 13.06.2004
 */
package de.bb.bex2.xml;


/**
 * @author bebbo
 */
public abstract class NsInfo
{
  private String prefix;
  private String uri;
  
  /**
   * @param prefix
   * @param uri
   */
  public NsInfo(String prefix, String uri)
  {
    this.prefix = prefix;
    this.uri = uri;
  }
  
  /**
   * @param tagNameVal
   * @return
   */
  public abstract boolean contains(String tagNameVal);
  
  /**
   * 
   * @return
   */
  public String getPrefix()
  {
    return prefix;
  }
  
  /**
   * Return the URI which is the public identifier to defines this namespace.
   * @return the URI which is the public identifier to defines this namespace.
   */
  public String getURI()
  {
    return uri;
  }

  /**
   * @param ename
   * @return
   */
  public abstract NsElement getElement(String ename);
}