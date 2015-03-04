/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/SpoolEntry.java,v $
 * $Revision: 1.5 $
 * $Date: 2012/05/17 07:22:26 $
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

class SpoolEntry {
    String msgId;

    String mailId;

    String toName;

    String toDomain;

    int retry;

    String fromName;

    String fromDomain;

    SpoolEntry(String _msgId, String _mailId, String _name, String _domain, int _retry, String _s_name, String _s_domain) {
        msgId = _msgId;
        mailId = _mailId;
        toName = _name;
        toDomain = _domain;
        retry = _retry;
        fromName = _s_name;
        fromDomain = _s_domain;
    }
}

/******************************************************************************
 * $Log: SpoolEntry.java,v $
 * Revision 1.5  2012/05/17 07:22:26  bebbo
 * @I replaced Vector with ArrayList
 * @I more typed collections
 * @R MimeFile is now in de.bb.util
 * Revision 1.4 2005/11/30 07:10:42 bebbo
 * 
 * @F reformatted
 * 
 *    Revision 1.3 2002/04/03 15:41:44 franke
 * @R no longer public
 * 
 *    Revision 1.2 2001/02/27 16:25:35 bebbo
 * @R not all methods were public - fixed
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *****************************************************************************/
