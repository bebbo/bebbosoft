/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/Pop3.java,v $
 * $Revision: 6.2 $
 * $Date: 2014/09/22 09:22:24 $
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

 (c) 2000-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy.mail;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.Enumeration;

import de.bb.bejy.Version;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;
import de.bb.util.SessionManager;

final class Pop3 extends de.bb.bejy.Protocol {
    private final static boolean DEBUG = false;

    private boolean VERBOSE = DEBUG;

    // version stuff
    private final static String no;

    private final static String version;
    static {
        String s = "$Revision: 6.2 $";
        no = "1." + s.substring(11, s.length() - 1);
        version = Version.getShort() + " POP3 " + no
                + " (c) 2000-2014 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    /**
     * Return the version number.
     * 
     * @return
     */
    public String getVersion() {
        return no;
    }

    /**
     * Return the full version String.
     * 
     * @return
     */
    public String getFull() {
        return version;
    }

    private final static int S_IDLE = 0, S_USER = 1,
    //                           S_APOP = 2, 
            S_AUTH = 3;

    private final static ByteRef CRLF = new ByteRef("\r\n");

    private final static ByteRef OK = new ByteRef("+OK accepted\r\n"), ERR = new ByteRef("-ERR\r\n"),
            UPDATE = new ByteRef("+OK entering update state\r\n"), CLOSING = new ByteRef("+OK closing connection\r\b"),
            USER = new ByteRef("USER"), PASS = new ByteRef("PASS"),
            //    APOP = new ByteRef("APOP"),
            QUIT = new ByteRef("QUIT"), STAT = new ByteRef("STAT"), LIST = new ByteRef("LIST"), UIDL = new ByteRef(
                    "UIDL"), TOP = new ByteRef("TOP "), RETR = new ByteRef("RETR"), DELE = new ByteRef("DELE"),
            NOOP = new ByteRef("NOOP"), RSET = new ByteRef("RSET"), CAPA = new ByteRef("CAPA"),
            CAPARESPONSE = new ByteRef("+OK Capability list follows\r\nPIPELINING\r\nTOP\r\nUIDL\r\nUSER\r\n.\r\n"),
            //    NOLOGIN = new ByteRef("-ERR access denied\r\n"),
            EOT = new ByteRef(".\r\n");

    private LogFile logFile;

    private ByteRef bServerName;

    // used implementations
    MailDBI mDbi;

    // id used for authentication init in trigger
    String authId;

    //  InputStream is;

    OutputStream os;

    protected Pop3(Pop3Factory pf, LogFile _logFile) {
        super(pf);
        logFile = _logFile;
        bServerName = new ByteRef(pf.mailCfg.getMainDomain());

        VERBOSE = DEBUG | pf.isVerbose();
    }

    // overwrite the trigger method, since clients expects a message first
    protected boolean trigger() throws Exception {
        try {
            //      is = getIs();
            os = new BufferedOutputStream(getOs(), 1412);

            authId = "<" + SessionManager.newKey() + '@' + bServerName + '>';
            String msg = "+OK " + getFull() + " ready " + authId + CRLF;
            if (DEBUG)
                System.out.println(msg);
            os.write(msg.getBytes());
            os.flush();
        } catch (Exception e) {
            return false;
        }
        return super.trigger();
    }

    protected boolean doit() throws Exception {
        MailAccount mails = null;
        ByteRef user = null, domain = null, passwd = null;
        String userId = null;
        byte data[] = new byte[8192];

        int state = S_IDLE;

        logFile.writeDate("connect from " + remoteAddress);

        ByteRef br = readFirst();
        try {
            for (;;) {
                os.flush();
                ByteRef line = readLine(br);
                if (line == null)
                    break;

                if (DEBUG)
                    System.out.println(line);
                if (VERBOSE)
                    logFile.writeDate(line.toString());

                ByteRef cmd = line.substring(0, 4).toUpperCase();

                if (cmd.equals(QUIT))
                    break;

                if (cmd.equals(NOOP)) {
                    if (VERBOSE)
                        logFile.writeDate(OK.toString());
                    OK.writeTo(os);
                    continue;
                }

                if (cmd.equals(CAPA)) {
                    if (VERBOSE)
                        logFile.writeDate(CAPARESPONSE.toString());
                    CAPARESPONSE.writeTo(os);
                    continue;
                }

                boolean ok = false;

                mDbi = ((MailFactory) factory).getDbi(this);
                try {
                    switch (state) {
                    case S_IDLE:
                        if (cmd.equals(USER)) {
                            user = line.substring(5).toLowerCase();
                            domain = bServerName;
                            int idx = user.indexOf('@');
                            if (idx == -1)
                                idx = user.indexOf('%');
                            if (idx > 0) {
                                domain = user.substring(idx + 1);
                                user = user.substring(0, idx);
                            }
                            ok = user.length() > 0;
                            if (ok) {
                                state = S_USER;
                                logFile.writeDate("USER " + user + " at " + domain);
                            } else {
                                logFile.writeDate("invalid user: " + line);
                            }
                        }
                        break;
                    case S_USER:
                        if (cmd.equals(PASS)) {
                            passwd = line.substring(5);

                            // release the DBI otherwise it's locked while the client is waiting.
                            ((MailFactory) factory).releaseDbi(this, mDbi);
                            mDbi = null;
                            if (!MailDBI.delayLogin(user.toString(), domain.toString(), remoteAddress))
                                break;

                            // verify user/domain/password
                            mDbi = ((MailFactory) factory).getDbi(this);
                            userId = mDbi.verifyUser(user.toString(), domain.toString(), passwd.toString(),
                                    remoteAddress, null);

                            if (userId == null) {
                                MailDBI.handleLoginFailure(user.toString(), domain.toString(), remoteAddress);
                                break;
                            }
                            // collect the mail information
                            mails = getMailList(userId);

                            state = S_AUTH;
                            ok = true;
                        }
                        break;
                    case S_AUTH:
                        if (cmd.equals(STAT)) {
                            String a = "+OK " + mails.size() + " " + mails.getTotalSize() + CRLF;
                            if (VERBOSE)
                                logFile.writeDate(a);
                            os.write(a.getBytes());
                            continue;
                        }

                        if (cmd.equals(LIST)) {
                            ByteRef param = line.substring(5);
                            int msgNo = param.toInteger();

                            if (msgNo > mails.size())
                                break;
                            if (msgNo == 0) {
                                ByteRef x = new ByteRef("+OK " + mails.size() + " message(s) (" + mails.getTotalSize()
                                        + " octets)\r\n");
                                if (VERBOSE)
                                    logFile.writeDate(x.toString());
                                x.writeTo(os);
                                int i = 1;
                                for (Enumeration<MailEntry> e = mails.elements(); e.hasMoreElements(); ++i) {
                                    MailEntry en = e.nextElement();
                                    if (!en.dele) {
                                        x = new ByteRef("" + i + ' ' + en.size + "\r\n");
                                        if (VERBOSE)
                                            logFile.writeDate(x.toString());
                                        x.writeTo(os);
                                    }
                                }
                                if (VERBOSE)
                                    logFile.writeDate(EOT.toString());
                                EOT.writeTo(os);
                            } else {
                                MailEntry en = mails.elementAt(msgNo - 1);
                                ByteRef x = new ByteRef("+OK " + msgNo + ' ' + en.size + "\r\n");
                                if (VERBOSE)
                                    logFile.writeDate(x.toString());
                                x.writeTo(os);
                            }

                            continue;
                        }

                        if (cmd.equals(UIDL)) {
                            ByteRef param = line.substring(5);
                            int msgNo = param.toInteger();

                            if (msgNo > mails.size())
                                break;
                            if (msgNo == 0) {
                                ByteRef x = new ByteRef("+OK " + mails.size() + " message(s) (" + mails.getTotalSize()
                                        + " octets)\r\n");
                                if (VERBOSE)
                                    logFile.writeDate(x.toString());
                                x.writeTo(os);
                                int i = 1;
                                for (Enumeration<MailEntry> e = mails.elements(); e.hasMoreElements(); ++i) {
                                    MailEntry en = e.nextElement();
                                    x = new ByteRef("" + i + ' ' + en.mailId + "\r\n");
                                    if (VERBOSE)
                                        logFile.writeDate(x.toString());
                                    x.writeTo(os);
                                }
                                EOT.writeTo(os);
                            } else {
                                MailEntry en = mails.elementAt(msgNo - 1);
                                ByteRef x = new ByteRef("+OK " + msgNo + ' ' + en.mailId + "\r\n");
                                if (VERBOSE)
                                    logFile.writeDate(x.toString());
                                x.writeTo(os);
                            }

                            continue;
                        }

                        if (cmd.equals(TOP)) {
                            ByteRef param = line.substring(4);
                            int msgNo = param.toInteger();
                            int idx = param.indexOf(' ');

                            // line count behind header
                            int lines = 0;
                            if (idx > 0)
                                lines = param.substring(idx + 1).toInteger();

                            if (msgNo <= 0 || msgNo > mails.size())
                                break;
                            if (VERBOSE)
                                logFile.writeDate(OK.toString());
                            OK.writeTo(os);

                            // open file and send all til first blank line occurs
                            MailEntry en = mails.elementAt(msgNo - 1);
                            InputStream fis = mDbi.getInputStream(en.mailId);
                            try {
                                boolean seek = true;
                                ByteRef b = new ByteRef();
                                for (b = b.update(fis); (seek || lines > 0) && b != null; b = b.update(fis)) {
                                    for (ByteRef l = b.nextLine(); (seek || lines > 0) && l != null; l = b.nextLine()) {
                                        if (VERBOSE)
                                            logFile.writeDate(l.toString());
                                        l.writeTo(os);
                                        CRLF.writeTo(os);
                                        if (seek && l.length() == 0) {
                                            seek = false;
                                            continue;
                                        }
                                        if (!seek)
                                            --lines;
                                    }
                                }
                            } finally {
                                fis.close();
                            }
                            EOT.writeTo(os); // end of transmission
                            if (VERBOSE)
                                logFile.writeDate(EOT.toString());

                            continue;
                        }

                        if (cmd.equals(RETR)) {
                            ByteRef param = line.substring(5);
                            int msgNo = param.toInteger();

                            if (msgNo <= 0 || msgNo > mails.size())
                                break;
                            if (VERBOSE)
                                logFile.writeDate(OK.toString());
                            OK.writeTo(os);

                            // open file and send all til first blank line occurs
                            MailEntry en = mails.elementAt(msgNo - 1);

                            if (en.dele) {
                                break;
                            }

                            InputStream fis = mDbi.getInputStream(en.mailId);
                            try {
                                int read = fis.read(data);
                                byte last = 0xa;
                                for (; read > 0; read = fis.read(data)) {
                                    os.write(data, 0, read);
                                    last = data[read - 1];
                                }
                                if (last != 0xd && last != 0xa) {
                                    os.write(0xd);
                                    os.write(0xa);
                                }
                                EOT.writeTo(os); // end of transmission
                            } finally {
                                fis.close();
                            }
                            continue;
                        }

                        if (cmd.equals(DELE)) {
                            ByteRef param = line.substring(5);
                            int msgNo = param.toInteger();

                            if (msgNo <= 0 || msgNo > mails.size())
                                break;

                            // mark message for deletion
                            MailEntry en = mails.elementAt(msgNo - 1);
                            en.dele = true;

                            ok = true;
                            break;
                        }
                        if (cmd.equals(RSET)) {
                            // remove all delete markers
                            if (mails != null) {
                                for (Enumeration<MailEntry> e = mails.elements(); e.hasMoreElements();) {
                                    MailEntry en = e.nextElement();
                                    en.dele = false;
                                }
                            }

                            ok = true;
                            break;
                        }

                        break;
                    }
                } finally {
                    ((MailFactory) factory).releaseDbi(this, mDbi);
                    mDbi = null;
                }

                if (ok) {
                    if (VERBOSE)
                        logFile.writeDate(OK.toString());
                    OK.writeTo(os);
                } else {
                    if (VERBOSE)
                        logFile.writeDate(ERR.toString());
                    ERR.writeTo(os);
                }
            }
            // got a break command
            if (state >= S_AUTH && mails != null) {
                UPDATE.writeTo(os);

                // do the update
                mails.update();
                mails = null;
            }
        } catch (Exception e) {
            logFile.writeDate(e.getMessage());
        } finally {
            if (mails != null) {
                // ensure that the counter goes down again!
                mails.mails.removeAllElements();
                mails.update();
            }
        }

        CLOSING.writeTo(os);

        return false; // don't reuse socket
    }

    /**
     * contains mail elements of user
     * 
     * @param userId
     *            - id of the user
     * @return a MailAccount object containing its mail information
     * @throws Exception
     */
    MailAccount getMailList(String userId) throws Exception {
        if (DEBUG)
            System.out.println("getMailList");

        MailAccount ma = new MailAccount(userId, ((MailFactory) factory).mailCfg);

        // old :
        // java.sql.ResultSet rs = mDbi.selectFromMail(userId);

        String unitId = mDbi.getOrCreateUnitId(userId);
        String mbId = mDbi.mailboxId(unitId, "INBOX");

        ResultSet rs = mDbi.selectFromImapData(mbId);

        while (rs.next()) {
            MailEntry e = new MailEntry(rs.getString(3), // filename
                    rs.getString(10), // size
                    rs.getString(1) // uid
            );
            ma.addElement(e);
        }

        rs.close();

        return ma;
    }
}

/******************************************************************************
 * $Log: Pop3.java,v $
 * Revision 6.2  2014/09/22 09:22:24  bebbo
 * @V new version
 *
 * Revision 6.1  2014/09/21 18:50:26  bebbo
 * 6.1
 *
 * Revision 1.38  2013/11/01 13:32:33  bebbo
 * @R uses new mechanism for  logins to slow down password guess attacks
 * Revision 1.37 2013/07/23 07:18:29 bebbo
 * 
 * @I exceptions are logged into own log file not to console
 * 
 *    Revision 1.36 2013/05/21 06:16:13 bebbo
 * @F formatted and typified Revision 1.35 2013/03/07 12:41:19 bebbo
 * 
 * @F formatted Revision 1.34 2006/03/17 11:36:07 bebbo
 * 
 * @B fixed unclosed streams
 * 
 *    Revision 1.33 2005/11/30 06:19:23 bebbo
 * @I code dompliance with JDK 1.5
 * 
 *    Revision 1.32 2004/12/16 16:02:30 bebbo
 * @R database connections are now shared
 * 
 *    Revision 1.31 2004/04/07 16:34:09 bebbo
 * @V new version message
 * @B deleted messages are no longer listed
 * 
 *    Revision 1.30 2004/03/24 09:54:10 bebbo
 * @V new version information
 * 
 *    Revision 1.29 2004/03/24 09:50:11 bebbo
 * @V new version information
 * 
 *    Revision 1.28 2004/03/23 12:38:52 bebbo
 * @V the protocols are now using the same version as BEJY
 * 
 *    Revision 1.27 2003/09/30 12:38:35 bebbo
 * @B adding mail delimiter CRLF. if missing to mail files (RETR X)
 * 
 *    Revision 1.26 2003/06/23 15:20:29 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.25 2003/06/17 10:20:17 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.24 2003/03/31 16:35:06 bebbo
 * @N added DNS support to verify mail sender
 * 
 *    Revision 1.23 2003/03/21 10:14:53 bebbo
 * @I set buffer size to 1412
 * @B added required os.flush() statements
 * 
 *    Revision 1.22 2003/02/25 07:01:53 bebbo
 * @R protocols are usinf now BufferedOutputStreams
 * 
 *    Revision 1.21 2003/02/17 14:20:37 bebbo
 * @N added command CAPA
 * 
 *    Revision 1.20 2003/02/05 08:09:46 bebbo
 * @B better address handling
 * @N usage of VERBOSE attribute from config
 * 
 *    Revision 1.19 2003/01/27 19:30:43 bebbo
 * @I updated version information
 * 
 *    Revision 1.18 2002/12/16 19:55:42 bebbo
 * @B tracked some unclosed Statements / Connections down and fixed them
 * 
 *    Revision 1.17 2002/12/16 16:40:53 bebbo
 * @I added sync for db implementation to release resources
 * 
 *    Revision 1.16 2002/11/22 21:14:42 bebbo
 * @I added method shutdown() - closing DBI
 * 
 *    Revision 1.15 2002/02/16 13:57:54 franke
 * @V now reflecting implementions version number (not factory)
 * 
 *    Revision 1.14 2002/02/05 15:57:05 bebbo
 * @V nicer hello message with implementation version number
 * 
 *    Revision 1.13 2002/01/20 15:51:27 franke
 * @R the messages are stored without dot!
 * 
 *    Revision 1.12 2002/01/20 12:30:05 franke
 * @I create mailbox if no mailbox exists
 * 
 *    Revision 1.11 2002/01/19 15:49:47 franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 * 
 *    Revision 1.10 2002/01/13 15:20:25 franke
 * @R reflected mDbi changes
 * 
 *    Revision 1.9 2001/08/24 08:24:41 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.8 2001/03/28 13:28:24 bebbo
 * @D verbose on if debug is on
 * 
 *    Revision 1.7 2001/03/27 19:50:15 bebbo
 * @I now Protocl knows its Factory
 * @I now Factory is known by Protocol
 * 
 *    Revision 1.6 2001/03/20 18:34:32 bebbo
 * @I changed hostAddress into remoteAddress
 * 
 *    Revision 1.5 2001/03/09 19:47:22 bebbo
 * @B on error mail accounts where never closed -> no deletions!
 * 
 *    Revision 1.4 2001/02/25 17:08:01 bebbo
 * @R no longer public classes
 * 
 *    Revision 1.3 2001/02/20 19:14:07 bebbo
 * @D disabled DEBUG and VERBOSE
 * 
 *    Revision 1.2 2001/02/20 17:41:51 bebbo
 * @D added debug messages
 * @B socket is not longer kept alive
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *    Revision 1.6 2001/01/01 01:02:23 bebbo
 * @I improved database reconnect
 * 
 *    Revision 1.5 2000/12/30 10:16:19 bebbo
 * @B fixed wrong compare
 * 
 *    Revision 1.4 2000/12/30 09:04:51 bebbo
 * @R now throws Exceptions to indicate that a thread should end
 * 
 *    Revision 1.3 2000/12/29 17:50:12 bebbo
 * @I added verbose messages
 * 
 *    Revision 1.2 2000/12/28 21:04:31 bebbo
 * @D DEBUG cleanup
 * 
 *    Revision 1.1 2000/12/28 20:54:29 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
