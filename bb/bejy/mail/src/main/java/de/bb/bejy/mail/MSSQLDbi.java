/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/MSSQLDbi.java,v $
 * $Revision: 1.20 $
 * $Date: 2014/03/23 21:59:38 $
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

  (c) 1994-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.mail;

/**
 * An implementation of the DB interface to use a MSSQL server.
 */
public class MSSQLDbi extends MailDBI {
    /**
     * ct for a MSSQL implementation of the DBI interface.
     */
    public MSSQLDbi() {
        passwdFx1 = "";
        passwdFx2 = "";
        concat1 = "(";
        concat2 = " + ";
        concat3 = ")";
        dateFx = "getdate()";
    }

    @Override
    protected String getLastInsertQuery(String tableName, String idColumnName) {
        return "SELECT @@identity";
    }
}

/******************************************************************************
 * $Log: MSSQLDbi.java,v $
 * Revision 1.20  2014/03/23 21:59:38  bebbo
 * @N password is now stored with salted SHA256
 *
 * Revision 1.19  2013/05/17 11:01:15  bebbo
 * @N added support for PostgreSQL
 * @R changes due to PostgreSQL support
 * Revision 1.18 2005/11/30 06:16:45 bebbo
 * 
 * @R refactoring changed base class to MailDBI
 * 
 *    Revision 1.17 2002/12/19 14:53:56 bebbo
 * @R compacted different Dbi implementations
 * @B fixed some missing close statements
 * 
 *    Revision 1.16 2002/11/19 14:35:53 bebbo
 * @R removed migration support from old version to current version
 * @N separated mail storage into separate class.
 * @R MailFile is now using subdirectories since ext2fs is so slow...
 * 
 *    Revision 1.15 2002/11/19 12:34:09 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.14 2002/05/16 15:21:49 franke
 * @N added support for BODYSTRUCTURE, ENVELOPE in FETCH
 * @N added notifications for incoming/deleted mail
 * 
 *    Revision 1.13 2002/04/09 15:48:57 bebbo
 * @B used ResultSet after next statement.
 * 
 *    Revision 1.12 2002/02/16 17:10:31 bebbo
 * @B fixed UIDNEXT - now max(id) + 1
 * 
 *    Revision 1.11 2002/02/16 15:35:19 bebbo
 * @B enhanced RECENT mail detection
 * @B fixed read behaviour
 * 
 *    Revision 1.10 2002/02/16 14:02:40 bebbo
 * @N inserting sent mail into SENT folder (if folder exists)
 * 
 *    Revision 1.9 2002/01/20 20:57:37 franke
 * @I PoBoxes are sorted by name
 * 
 *    Revision 1.8 2002/01/20 18:21:57 franke
 * @B fixes in migration
 * 
 *    Revision 1.7 2002/01/20 15:50:48 franke
 * @B fixes in statements for LIST()
 * @B fixes in migration
 * 
 *    Revision 1.6 2002/01/20 12:48:34 franke
 * @added migration function
 * 
 *        Revision 1.5 2002/01/20 12:31:05 franke
 * @B wrong statement to query imap_mime infos
 * 
 *    Revision 1.4 2002/01/20 12:02:56 franke
 * @R mail table is obsolete. Spooler is now using imap_mime to keep mails. Did all necessary changes due to that
 * 
 *    Revision 1.3 2002/01/19 15:49:47 franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 * 
 *    Revision 1.2 2002/01/13 15:21:39 franke
 * @N added IMAP functionality
 * 
 *    Revision 1.1 2001/12/23 23:33:21 bebbo
 * @R moved the DB implementations
 * @N added MSSQL support
 * 
 *****************************************************************************/
