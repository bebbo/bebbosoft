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

import java.io.InputStream;
import java.net.SocketException;
import java.sql.ResultSet;
import java.util.Enumeration;

import de.bb.bejy.Version;
import de.bb.log.Logger;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;
import de.bb.util.SessionManager;

final class Pop3 extends de.bb.bejy.Protocol {
	private final static Logger LOG = Logger.getLogger(Pop3.class);
	
    private final static boolean DEBUG = false;

    private boolean VERBOSE = DEBUG;

    // version stuff
    private final static String version;
    static {
        version = Version.getShort() + " POP3 " + V.V
                + " (c) 2000-" + V.Y + " by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    /**
     * Return the version number.
     * 
     * @return
     */
    public String getVersion() {
        return V.V;
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

    protected Pop3(Pop3Factory pf, LogFile _logFile) {
        super(pf);
        logFile = _logFile;
        bServerName = new ByteRef(pf.mailCfg.getMainDomain());

        VERBOSE = DEBUG | pf.isVerbose();
    }

    // overwrite the trigger method, since clients expects a message first
    protected boolean trigger() throws Exception {
        try {
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
                                logFile.writeDate("login FAILURE for " + user + "@" + domain + " from "
                                        + remoteAddress);
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
        } catch (Exception ex) {
			if (!(ex instanceof SocketException))
				LOG.debug(ex.getMessage(), ex);
            throw ex;
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
