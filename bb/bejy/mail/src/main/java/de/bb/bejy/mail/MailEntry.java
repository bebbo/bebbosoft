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
 
package de.bb.bejy.mail;
 
/**
 * References one mail entity.
 * @author sfranke
 */
public class MailEntry
{
  String mailId, size, idd;
  boolean dele;
  MailEntry(String mailId, String size, String idd)
  {
    this.mailId = mailId;
    this.size = size;
    this.idd = idd;
    dele = false;
  }
  /**
   * Returns the mailId.
   * @return String
   */
  public String getMailId()
  {
    return mailId;
  }

  public String getIdd() {
    return idd;
  }
}

