/******************************************************************************
 * Home of sort implementarions (maybe obsolete...). 
 *
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


package de.bb.util;
/**
 * Sort implementations.
 * @author sfranke
 *
 */
public class Sort {
  
  private static int partialQuickSort(String[] array, int begin, int end)
  {
    int index = (begin + end) >> 1;
    String pivot = array[index];

    //    swap(array, index, end);        
    String tmp = array[end];
    array[end] = array[index];
    array[index] = tmp;

    for (int i = index = begin; i < end; ++i)
    {
      if (pivot.compareTo(array[i]) > 0)
      {
        //            swap(array, index, i);
        tmp = array[i];
        array[i] = array[index];
        array[index] = tmp;
        ++index;
      }
    }
    //    swap(array, index, end);        
    tmp = array[end];
    array[end] = array[index];
    array[index] = tmp;

    return index;
  }
  /**
   * Dumb recursive implementation of quicksort for a part of an array.
   * <code>quicksort(array, 0, array.length);</code> sorts the full array.
   * @param array an array - not null
   * @param begin valid start offset into the array - points to the start element
   * @param end valid end offset into the array - points behind the last element.
   */
  public static void quicksort(String[] array, int begin, int end)
  {
    if (end > begin)
    {
      int index = partialQuickSort(array, begin, end);
      quicksort(array, begin, index - 1);
      quicksort(array, index + 1, end);
    }
  }

}
