/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/ImapFactory.java,v $
 * $Revision: 1.7 $
 * $Date: 2004/12/16 16:02:30 $
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
 * @author sfranke
 */
public class ImapFactory extends MailFactory
{
  /**
   * Return the name of this protocol.
   * @return the name of this protocol.
   */
  public String getName()
  {
    return "IMAP";
  }

  /** (non-Javadoc)
   * @see de.bb.bejy.Factory#create()
   */
  public de.bb.bejy.Protocol create() throws Exception
  {
    return new Imap(this, logFile);
  }
}

/******************************************************************************
 * $Log: ImapFactory.java,v $
 * Revision 1.7  2004/12/16 16:02:30  bebbo
 * @R database connections are now shared
 *
 * Revision 1.6  2003/06/23 15:20:30  bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 *
 * Revision 1.5  2003/06/17 10:20:18  bebbo
 * @R redesign to utilize the new configuration scheme
 *
 * Revision 1.4  2002/11/19 12:34:09  bebbo
 * @I reorganized imports
 *
 * Revision 1.3  2002/02/16 13:57:54  franke
 * @V now reflecting implementions version number (not factory)
 *
 * Revision 1.2  2002/01/19 15:49:47  franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 *
 * Revision 1.1  2002/01/13 15:21:27  franke
 * @N new - first rare tested version
 *
 *****************************************************************************/