/**
 * 
 */
package de.bb.bejy.http;

import java.util.Iterator;

public class IterEnum<T> implements java.util.Enumeration<T> {
  Iterator<T> iter;

  public IterEnum(Iterator<T> i)
  {
    iter = i;
  }

  public boolean hasMoreElements()
  {
    return iter.hasNext();
  }

  public T nextElement()
  {
    return iter.next();
  }
}