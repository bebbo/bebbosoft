/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MailEntry.java,v $
 * $Revision: 1.8 $
 * $Date: 2006/03/17 11:35:10 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 *
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

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
 
/******************************************************************************
 * $Log: MailEntry.java,v $
 * Revision 1.8  2006/03/17 11:35:10  bebbo
 * @N method for ID
 *
 * Revision 1.7  2005/11/30 06:15:20  bebbo
 * @C added class comment
 *
 * Revision 1.6  2003/06/17 10:20:18  bebbo
 * @R redesign to utilize the new configuration scheme
 *
 * Revision 1.5  2003/01/25 15:11:30  bebbo
 * @N added command line tool and also made necessary changes-
 *
 * Revision 1.4  2002/12/17 14:01:38  bebbo
 * @B fixed a to early ResultSet close
 * @N added a recovery function for lost and still available mails
 *
 * Revision 1.3  2002/01/19 15:49:47  franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 *
 * Revision 1.2  2001/02/27 16:25:35  bebbo
 * @R not all methods were public - fixed
 *
 * Revision 1.1  2001/02/19 19:56:16  bebbo
 * @R new or moved from package smtp or pop3
 *
 *****************************************************************************/
