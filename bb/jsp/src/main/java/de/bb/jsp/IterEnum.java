/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
package de.bb.jsp;

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