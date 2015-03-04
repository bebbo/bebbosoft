/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/HSQLDbi.java,v $
 * $Revision: 1.4 $
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
public class HSQLDbi extends MailDBI {
    /**
     * ct for a MSSQL implementation of the DBI interface.
     */
    public HSQLDbi() {
        passwdFx1 = "";
        passwdFx2 = "";
        concat1 = "(";
        concat2 = " + ";
        concat3 = ")";
        dateFx = "CURRENT_TIMESTAMP";
    }

    @Override
    protected String getLastInsertQuery(String tableName, String idColumnName) {
        return "SELECT value from SYSTEM_SESSIONINFO where key='IDENTITY'";
    }
}

/******************************************************************************
 * $Log: HSQLDbi.java,v $
 * Revision 1.4  2014/03/23 21:59:38  bebbo
 * @N password is now stored with salted SHA256
 *
 * Revision 1.3  2013/05/17 11:01:07  bebbo
 * @N added support for PostgreSQL
 * @R changes due to PostgreSQL support
 * Revision 1.2 2005/12/31 15:41:46 bebbo
 * 
 * @I cleanup of imports
 * 
 *    Revision 1.1 2005/11/30 05:54:30 bebbo
 * @N support for HSQLDbi
 * 
 *****************************************************************************/
