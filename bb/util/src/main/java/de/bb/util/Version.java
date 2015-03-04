/******************************************************************************
 * $Source: /export/CVS/java/bb_util/src/main/java/de/bb/util/Version.java,v $
 * $Revision: 1.4 $
 * $Date: 2008/03/15 18:01:05 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Version counter. 
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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
 * Version information.
 *
 */
public class Version
{
  private final static String no;
  private final static String version;
  private final static int hi, lo;
  static {
    String s = "$Revision: 1.4 $";
    String sub = s.substring(11, s.length()-1);
    int dot = sub.indexOf('.');
    no = "1.1." + sub;
    version = "de.bb.util V" + no + " (c) 1997-2002 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    hi = Integer.parseInt(sub.substring(0, dot));
    lo = Integer.parseInt(sub.substring(dot+1));
  }
  /**
   * Returns the version value.
   * @return returns the version value.
   */
  public static String getVersion() { return no; }
  /**
   * Returns the full version String.
   * @return returns the full version String.
   */
  public static String getFull()    { return version; }
  /**
   * Returns the version hi value.
   * @return returns the version hi value.
   */
  public static int getHi()         { return hi; }
  /**
   * Returns the version lo value.
   * @return returns the version lo value.
   */
  public static int getLo()         { return lo; }
}

/******************************************************************************
 * $Log: Version.java,v $
 * Revision 1.4  2008/03/15 18:01:05  bebbo
 * @R Changed the license: From now on GPL 3 applies.
 *
 * Revision 1.3  2002/01/20 12:01:15  franke
 * @V bumped
 *
 * Revision 1.2  2001/12/10 16:22:51  bebbo
 * @C completed comments!
 *
 * Revision 1.1  2001/09/15 08:57:38  bebbo
 * @N new
 *
 *****************************************************************************/