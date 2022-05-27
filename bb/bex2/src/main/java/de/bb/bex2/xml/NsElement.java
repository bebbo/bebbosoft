/* 
 * Created on 25.07.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.bex2.xml;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class tries to reflect a namespace element, 
 * which then is implemented with a start and an end tag.
 * The namespace and its elements are a static definition of the
 * possible tree.
 * @author sfranke
 */
public abstract class NsElement
{
  protected HashMap attributes = new HashMap();

  /**
   * Return an iterator over available attribute names.
   * @return an Iterator with NsAttr objects for the attribute names.
   */
  public Iterator attributeNames()
  {
    return attributes.keySet().iterator();
  }
  
  /**
   * @param name
   * @return
   */
  public NsAttr getAttribute(String name)
  {
    return (NsAttr)attributes.get(name);
  }
  /**
   * Determines whether the specifed childName is allowed, 
   * and resolves the childName to the corresponding NamespaceElement.
   * The reason for returning the NamespaceElement allows to resolv
   * HTML where start/end tags can be ommitted. 
   * @param childName
   * @return the NamespaceElement or null.
   */
  public abstract NsElement resolveChild(String childName);
  
}
