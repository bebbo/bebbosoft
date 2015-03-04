/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/PostgreSQLDbi.java,v $
 * $Revision: 1.3 $
 * $Date: 2014/06/23 19:07:46 $
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
public class PostgreSQLDbi extends MailDBI {
    /**
     * ct for a MSSQL implementation of the DBI interface.
     */
    public PostgreSQLDbi() {
        passwdFx1 = "(";
        passwdFx2 = ")";
        concat1 = "(";
        concat2 = " || ";
        concat3 = ")";
        dateFx = "NOW()";
        deleteEnd = ")";
    }

    protected String getLastInsertQuery(String tableName, String idColumnName) {
        return "SELECT CURRVAL(pg_get_serial_sequence('" + tableName + "','" + idColumnName + "'))";
    }

    protected String makeDelete(final String tableName) {
        return "DELETE FROM " + tableName + " WHERE id IN (SELECT " + tableName + ".ID FROM " + tableName + ", ";
    }

}

/******************************************************************************
 * $Log: PostgreSQLDbi.java,v $
 * Revision 1.3  2014/06/23 19:07:46  bebbo
 * @B fixed delete SQL in postgresql
 *
 * Revision 1.2  2014/03/23 21:59:38  bebbo
 * @N password is now stored with salted SHA256
 *
 * Revision 1.1  2013/05/17 11:01:04  bebbo
 * @N added support for PostgreSQL
 * @R changes due to PostgreSQL support
 * Revision 1.26 2005/11/30 06:16:57 bebbo
 * 
 * @R refactoring changed base class to MailDBI
 * 
 *    Revision 1.25 2002/12/19 16:43:33 bebbo
 * @B fixed LAST_INSERT_ID()
 * 
 *    Revision 1.24 2002/12/19 14:53:56 bebbo
 * @R compacted different Dbi implementations
 * @B fixed some missing close statements
 * 
 *    Revision 1.23 2002/12/16 20:10:43 bebbo
 * @B tracked more unclosed Statements / Connections down and fixed them
 * 
 *    Revision 1.22 2002/12/16 19:55:42 bebbo
 * @B tracked some unclosed Statements / Connections down and fixed them
 * 
 *    Revision 1.21 2002/12/02 18:39:37 bebbo
 * @B fixed multiple statement use -> caused errors.
 * 
 *    Revision 1.20 2002/11/19 14:35:53 bebbo
 * @R removed migration support from old version to current version
 * @N separated mail storage into separate class.
 * @R MailFile is now using subdirectories since ext2fs is so slow...
 * 
 *    Revision 1.19 2002/11/19 12:34:09 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.18 2002/05/16 15:21:49 franke
 * @N added support for BODYSTRUCTURE, ENVELOPE in FETCH
 * @N added notifications for incoming/deleted mail
 * 
 *    Revision 1.17 2002/02/16 17:10:31 bebbo
 * @B fixed UIDNEXT - now max(id) + 1
 * 
 *    Revision 1.16 2002/02/16 16:37:58 bebbo
 * @B fixed STATUS (UIDNEXT)
 * 
 *    Revision 1.15 2002/02/16 15:35:19 bebbo
 * @B enhanced RECENT mail detection
 * @B fixed read behaviour
 * 
 *    Revision 1.14 2002/02/16 14:02:40 bebbo
 * @N inserting sent mail into SENT folder (if folder exists)
 * 
 *    Revision 1.13 2002/01/20 20:57:37 franke
 * @I PoBoxes are sorted by name
 * 
 *    Revision 1.12 2002/01/20 18:21:57 franke
 * @B fixes in migration
 * 
 *    Revision 1.11 2002/01/20 18:08:37 franke
 * @D more info in migration
 * 
 *    Revision 1.10 2002/01/20 15:51:07 franke
 * @R implementing the changes interface
 * @B fixes in statements for LIST()
 * @B fixes in migration
 * 
 *    Revision 1.9 2002/01/13 15:20:34 franke
 * @R reflected mDbi changes
 * 
 *    Revision 1.8 2001/12/23 23:33:21 bebbo
 * @R moved the DB implementations
 * @N added MSSQL support
 * 
 *    Revision 1.9 2001/09/15 08:53:40 bebbo
 * @I remove trailing ';' from SQL statements
 * 
 *    Revision 1.8 2001/07/13 13:16:11 bebbo
 * @N added notification instead of forwarding
 * 
 *    Revision 1.7 2001/06/11 06:31:18 bebbo
 * @N added admin functions
 * 
 *    Revision 1.6 2001/05/06 13:12:12 bebbo
 * @B now catching everything to do a reconnect
 * 
 *    Revision 1.5 2001/04/16 20:05:19 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.4 2001/04/11 15:51:25 bebbo
 * @I rewrote method checkconnection
 * 
 *    Revision 1.3 2001/03/28 09:14:29 bebbo
 * @B fixed the 'NOW()' bug...
 * 
 *    Revision 1.2 2001/03/05 17:46:27 bebbo
 * @B fixed sleep time off Spooler
 * 
 *    Revision 1.1 2001/02/27 16:25:11 bebbo
 * @R moved to new package
 * 
 *    Revision 1.6 2001/02/27 16:11:42 bebbo
 * @D added lots of DEBUG messages
 * @B fixed an Exception in spoolerSleep!
 * 
 *    Revision 1.5 2001/02/26 17:48:39 bebbo
 * @I old file are only deleted when older than one day!
 * 
 *    Revision 1.4 2001/02/25 17:08:30 bebbo
 * @R all functions are throwing exceptions now
 * @I implemented the global cleanup function
 * 
 *    Revision 1.3 2001/02/20 19:14:47 bebbo
 * @D added DEBUG messages
 * @D disabled DEBUG and VERBOSE
 * 
 *    Revision 1.2 2001/02/20 17:40:16 bebbo
 * @B reading correct jdbcUrl value
 * @B added missing return values
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *****************************************************************************/
