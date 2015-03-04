/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/Smtp.java,v $
 * $Revision: 6.3 $
 * $Date: 2014/10/04 19:11:09 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2005.
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import de.bb.bejy.Version;
import de.bb.bejy.mail.spf.SpfContext;
import de.bb.security.Ssl3Server;
import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.DateFormat;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.Process;
import de.bb.util.SessionManager;

final class Smtp extends de.bb.bejy.Protocol {
    private final static boolean DEBUG = false;

    private boolean VERBOSE = DEBUG;

    // version stuff
    private final static String no;

    private final static String version;
    static {
        String s = "$Revision: 6.3 $";
        no = "1." + s.substring(11, s.length() - 1);
        version = Version.getShort() + " ESMTP " + no
                + " (c) 2000-2014 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    /**
     * Returns the version value.
     * 
     * @return returns the version value.
     */
    public static String getVersion() {
        return no;
    }

    /**
     * Returns the full version String.
     * 
     * @return returns the full version String.
     */
    public static String getFull() {
        return version;
    }

    private final static ByteRef CRLF = new ByteRef("\r\n");

    protected LogFile logFile;

    private MailDBI mDbi;

    private ArrayList<String> recipients;

    private ByteRef bServerName;

    private InputStream is;

    OutputStream os;

    protected Smtp(SmtpFactory sf, LogFile _logFile) {
        super(sf);
        logFile = _logFile;

        recipients = new ArrayList<String>();
        bServerName = new ByteRef(sf.mailCfg.getMainDomain());

        VERBOSE = DEBUG | sf.isVerbose();
        // logFile.writeDate("VERBOSE is " + VERBOSE);
    }

    private final static int S_IDLE = 0, S_HELO = 1, S_MAIL = 2;

    private final static ByteRef READY = new ByteRef("220 " + getFull() + " ready\r\n"),
            // POSTMASTER = new ByteRef("postmaster"),
            ESYNTAX = new ByteRef("500 syntax error or command not valid in current state\r\n"),
            EUSERNOTHERE = new ByteRef("550 user does not exist\r\n"),
            EMAILBOX = new ByteRef("553 mailbox name not allowed:"),
            EPOPBEFORE = new ByteRef("503 get mail with POP3 to send to foreign addresses\r\n"),
            ENORECIPIENTS = new ByteRef("451 Requested action aborted: no recipients\r\n"),
            // ENOVALIDSENDER = new ByteRef("451 Requested action aborted: no valid sender\r\n"),
            ENOVALIDSERVER = new ByteRef("451 Requested action aborted: no valid server: "),
            HELO = new ByteRef("HELO"), EHLO = new ByteRef("EHLO"), STARTTLS = new ByteRef("STARTTLS"),
            MAIL = new ByteRef("MAIL"),
            SEND = new ByteRef("SEND"),
            SOML = new ByteRef("SOML"),
            SAML = new ByteRef("SAML"),
            AUTH = new ByteRef("AUTH"),
            LOGIN = new ByteRef("LOGIN"),
            NOOP = new ByteRef("NOOP"),
            // CRAMMD5 = new ByteRef("CRAM-MD5"),
            LOCALONLY = new ByteRef("250 only local recipients accepted\r\n"),
            // VERIFYONLY = new ByteRef("250 but you cannot send any mail!\r\n"),
            SENDANY = new ByteRef("250 local user may send to any address\r\n"), RCPT = new ByteRef("RCPT"),
            RECIPOK = new ByteRef("250 recipient OK\r\n"),
            DATA = new ByteRef("DATA"),
            K354 = new ByteRef("354 enter mail, end with . and crlf\r\n"),
            // MAILEND = new ByteRef("."),
            MAILOK = new ByteRef("250 mail received\r\n"), QUIT = new ByteRef("QUIT"), K221 = new ByteRef(
                    "221 have a nice day\r\n"), RSET = new ByteRef("RSET"), K250 = new ByteRef("250 OK\r\n"),
            CAPSTARTTLS = new ByteRef("250-STARTTLS\r\n"), CAPABILITIES = new ByteRef("250 AUTH LOGIN\r\n"),
            USERNAME = new ByteRef("334 VXNlcm5hbWU6\r\n"), PASSWORD = new ByteRef("334 UGFzc3dvcmQ6\r\n"),
            K235 = new ByteRef("235 Authentication successful\r\n"), E450 = new ByteRef(
                    "450 Requested mail action not taken: mailbox unavailable\r\n"),
            // E450A = new ByteRef("450 Requested mail action not taken: could not verify sender address\r\n"),
            EMIMECHECK = new ByteRef("551 Requested action aborted: strict MIME check failed\r\n"), E551 = new ByteRef(
                    "551 Requested action aborted: virus check failed\r\n"), REQAUTH = new ByteRef(
                    "550 authentication required to send to foreign adresses\r\n"), EMAILBOXFULL = new ByteRef(
                    "551 disk quota exceeded\r\n"), TLSREADY = new ByteRef("220 ready to start TLS\r\n")

            // CRLF = new ByteRef("\r\n")
            ;
    private final static String EALIAS = "559 Not a valid alias to send for: ", ENOTLOCAL = "558 Not a local domain: ";

    private HashMap<String, Boolean> checkCache = new HashMap<String, Boolean>();

    private ByteRef br;

    private static SessionManager<String, Integer> NOSTARTTLS = new SessionManager<String, Integer>(1000L * 60 * 60 * 3);
    
    // overwrite the trigger method, since clients expects a message first
    protected boolean trigger() throws Exception {
        try {
            is = getIs();
            os = new BufferedOutputStream(getOs(), 8192);
            lwrite(READY);
            os.flush();
        } catch (Exception e) {
            return false;
        }
        return super.trigger();
    }

    protected boolean doit() throws Exception {
        String userId = null;

        String resolvedDomain = MailCfg.dns.getDomainFromIp(remoteAddress);
        logFile.writeDate("connect from [" + remoteAddress + "=" + resolvedDomain + "]");
        recipients.clear();

        ByteRef clientHelo = new ByteRef();
        String fromUser, fromDomain;
        fromUser = fromDomain = "";
        boolean localOnly = true;
        boolean reqAuth = false;
        String loginUser = null;
        String loginDomain = null;

        int state = S_IDLE;

        if (DEBUG)
            System.out.println("enter read first");
        br = readFirst();
        for (;;) {
            os.flush();

            ByteRef error = ESYNTAX;

            if (DEBUG)
                System.out.println("do readline");
            ByteRef line = readLine(br);
            if (line == null)
                return false;
            if (DEBUG)
                System.out.println(line);
            if (VERBOSE)
                logFile.writeDate(line.toString());

            ByteRef cmd = line.substring(0, 4).toUpperCase();
            if (cmd.equals(QUIT)) {
                lwrite(K221);
                os.flush();
                return false;
            }

            // handle STARTTLS
            if (line.equalsIgnoreCase(STARTTLS)) {
                if (isSecure) {
                    // not allowed if already secure
                    lwrite(ESYNTAX);
                    continue;
                }
//                logFile.writeDate("starting TLS");
                lwrite(TLSREADY);
                os.flush();
                
                // mark failed/aborted STARTTLS
                Integer n = NOSTARTTLS.get(remoteAddress);
                if (n == null) n = 0;
                NOSTARTTLS.put(remoteAddress, n + 1);

                
                final Ssl3Server s3 = server.getTLSServer();
                try {
                    this.startTLS(s3);
                } catch (IOException ioe) {
                    logFile.writeDate("STARTTLS failed: " + ioe.getMessage());
                    throw ioe;
                }
                is = getIs();
                os = new BufferedOutputStream(getOs());

                logFile.writeDate("STARTTLS using: " + s3.getCipherSuite());
                NOSTARTTLS.remove(remoteAddress);

                recipients.clear();
                fromUser = null;
                if (state != S_IDLE)
                    state = S_HELO;
                continue;
            }

            if (cmd.equals(RSET)) {
                recipients.clear();
                fromUser = null;

                lwrite(K250);
                if (state != S_IDLE)
                    state = S_HELO;
                continue;
            }

            if (cmd.equals(NOOP)) {
                lwrite(K250);
                continue;
            }

            boolean ok = false;
            mDbi = ((MailFactory) factory).getDbi(this);
            try {
                if (cmd.equals(HELO)) {
                    clientHelo = line.substring(5).trim();
                    if (clientHelo.length() == 0) {
                        logFile.writeDate("NOHELO from " + remoteAddress);
                        error = ENOVALIDSERVER;
                    } else if (resolvedDomain == null) {
                        logFile.writeDate("unresolved domain in HELO from " + remoteAddress);
                        error = ENOVALIDSERVER;
                    } else {

                        reqAuth = factory.getBooleanProperty("validateServer", false)
                                && !validateServer(clientHelo, remoteAddress);

                        String msg = "250 hello " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain + "]\r\n";
                        state = S_HELO;
                        ok = true;

                        lwrite(msg);
                    }
                } else if (cmd.equals(EHLO)) {
                    clientHelo = line.substring(5).trim();
                    if (clientHelo.length() == 0) {
                        logFile.writeDate("NOHELO from " + remoteAddress);
                        error = ENOVALIDSERVER;
                    } else {
                        reqAuth = factory.getBooleanProperty("validateServer", false)
                                && !validateServer(clientHelo, remoteAddress);

                        String msg = "250-hello " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain + "]\r\n";
                        state = S_HELO;
                        ok = true;

                        lwrite(msg);
                        if (!isSecure && server.supportsTLS()) {
                            // enable STARTTLS only if there a no broken STARTTLS connects
                            final Integer n = NOSTARTTLS.get(remoteAddress);
                            if (n == null || n < 2)
                                lwrite(CAPSTARTTLS);
                        }
                        lwrite(CAPABILITIES);
                    }
                } else {
                    switch (state) {
                    case S_HELO:
                        if (cmd.equals(AUTH)) {
                            line = line.substring(5).trim(); // eat AUTH
                            final ByteRef authCmd = line.nextWord(' ').toUpperCase();
                            if (authCmd.equals(LOGIN)) {
                                // check for additional parameter
                                if (line.length() == 0) {
                                    lwrite(USERNAME);
                                    os.flush();
                                    line = readLine(br);
                                }
                                byte[] a = line.toByteArray();
                                ByteRef domain, user = new ByteRef(Mime.decode(a, 0, a.length)).toLowerCase();
                                logFile.writeDate("login for user: " + user);

                                domain = bServerName;
                                int idx = user.indexOf('@');
                                if (idx == -1)
                                    idx = user.indexOf('%');
                                if (idx > 0) {
                                    domain = user.substring(idx + 1);
                                    user = user.substring(0, idx);
                                }

                                lwrite(PASSWORD);
                                os.flush();

                                line = readLine(br);
                                a = line.toByteArray();
                                String passwd = new String(Mime.decode(a, 0, a.length));

                                // release the DBI otherwise it's locked while the client is waiting.
                                ((MailFactory) factory).releaseDbi(this, mDbi);
                                mDbi = null;

                                if (MailDBI.delayLogin(user.toString(), domain.toString(), remoteAddress)) {
                                    // verify user/domain/password
                                    mDbi = ((MailFactory) factory).getDbi(this);
                                    userId = mDbi.verifyUser(user.toString(), domain.toString(), passwd, remoteAddress,
                                            null);
                                    if (userId != null) {
                                        lwrite(K235);
                                        loginUser = user.toString();
                                        loginDomain = domain.toString();

                                        localOnly = false;
                                        continue;
                                    }
                                    MailDBI.handleLoginFailure(user.toString(), domain.toString(), remoteAddress);
                                }
                                logFile.writeDate("with invalid password");
                            }
                            break;
                        }

                        // no support for terminals
                        if (cmd.equals(SEND)) {
                            error = E450;
                            break;
                        }

                        // handle all equal
                        if (cmd.equals(MAIL) || cmd.equals(SAML) || cmd.equals(SOML)) {
                            error = EUSERNOTHERE;
                            recipients.clear();
                            fromUser = null;

                            // remove mark since the secured access worked
                            if (isSecure)
                                NOSTARTTLS.remove(remoteAddress);
                            
                            int a = line.lastIndexOf('<');
                            int b = line.lastIndexOf('>');
                            ByteRef user = new ByteRef();
                            if (a > 0 && b > a)
                                user = line.substring(a + 1, b).toLowerCase();

                            // check invalid user names
                            if (user.indexOf('\'') >= 0)
                                break;

                            ByteRef domain = new ByteRef();
                            int idx = user.indexOf('@');
                            if (idx == -1)
                                idx = user.indexOf('%');
                            if (idx > 0) {
                                domain = user.substring(idx + 1);
                                user = user.substring(0, idx);
                            }

                            if (localOnly && resolvedDomain == null) {
                                String msg = "you need a resolved ip to send";
                                logFile.writeDate("UNRESOLVED: " + msg);
                                error = new ByteRef("450 " + msg + "\r\n");
                                break;
                            }

                            if (user.length() == 0 && domain.length() == 0) {
                                fromUser = "postmaster";

                                fromDomain = resolvedDomain;
                                // allow to step out one level
                                if (MailCfg.dns.getRealMXfromDomain(fromDomain).size() == 0) {
                                    int dot = fromDomain.indexOf('.');
                                    fromDomain = fromDomain.substring(dot + 1);
                                }

                                logFile.writeDate("ANONYMOUS MAIL FROM:<> " + clientHelo + "[" + remoteAddress + "="
                                        + resolvedDomain + "] from postmaster@" + fromDomain);

                                localOnly = true;
                                state = S_MAIL;
                                user = new ByteRef(fromUser);
                                domain = new ByteRef(fromDomain);
                            } else {
                                if (domain.length() == 0) {
                                    domain = bServerName;
                                }

                                fromUser = user.toString();
                                fromDomain = domain.toString();

                                if (localOnly) {
                                    localOnly = null == mDbi.allowUser(fromUser, fromDomain, remoteAddress, null);
                                }
                            }
                            //
                            if (localOnly) {
                                if (reqAuth) {
                                    logFile.writeDate("REQAUTH: " + clientHelo + "[" + remoteAddress + "="
                                            + resolvedDomain + "] sender:" + user + "@" + domain);
                                    logFile.writeDate("NORELAY to " + user + "@" + domain + " login required");
                                    error = REQAUTH;
                                    break;
                                }

                                if (factory.getBooleanProperty("validateSender", false)
                                        && !validateSender(fromUser, fromDomain)) {
                                    String msg = "sender:" + user + "@" + domain + " sending from " + clientHelo + "["
                                            + remoteAddress + "=" + resolvedDomain + "]";

                                    logFile.writeDate("NOACCESS: " + msg);
                                    error = new ByteRef("450-sender mail address verification failed\r\n450 " + msg
                                            + "\r\n");
                                    break;
                                }
                                lwrite(LOCALONLY);
                            } else {
                                // disallow all but the login address
                                if (!fromUser.equals(loginUser) || !fromDomain.equals(loginDomain)) {
                                    // allow forwarder to this account
                                    if (!mDbi.isForwarder(fromUser, fromDomain, loginUser, loginDomain)) {
                                        String m = EALIAS + fromUser + "@" + fromDomain + " <> " + loginUser + "@"
                                                + loginDomain;
                                        logFile.writeDate(m);
                                        error = new ByteRef(m + "\r\n");
                                        break;
                                    }
                                }

                                if (!mDbi.isLocalDomain(fromDomain)) {
                                    String m = ENOTLOCAL + fromDomain;
                                    logFile.writeDate(m);
                                    error = new ByteRef(m + "\r\n");
                                    break;
                                }

                                lwrite(SENDANY);
                            }

                            logFile.writeDate("MAIL FROM:<" + fromUser + "@" + fromDomain + "> " + clientHelo + "["
                                    + remoteAddress + "=" + resolvedDomain + "]");

                            state = S_MAIL;
                            ok = true;
                        }
                        break;

                    case S_MAIL:
                        if (cmd.equals(RCPT)) {
                            error = EMAILBOX.append(line).append("\r\n");

                            ByteRef user = null;
                            int a = line.lastIndexOf('<');
                            int b = line.lastIndexOf('>');
                            if (a > 0 && b > a)
                                user = line.substring(a + 1, b).toLowerCase();
                            if (user == null)
                                break;
                            // check invalid user names
                            if (user.indexOf('\'') >= 0)
                                break;

                            ByteRef to = user;
                            ByteRef toDomain = bServerName;
                            int idx = user.indexOf('@');
                            if (idx == -1)
                                idx = user.indexOf('%');
                            if (idx > 0) {
                                toDomain = user.substring(idx + 1);
                                user = user.substring(0, idx);
                            }
                            if (user == null) {
                                break;
                            }

                            final String sToDomain = toDomain.toString();
                            boolean localDomain = mDbi.isLocalDomain(sToDomain);

                            // custom sender may only send to local users
                            if (localOnly && !localDomain) {
                                error = EPOPBEFORE;
                                break;
                            }

                            // if it's to local domain, user must exist
                            if (localDomain) {
                                if (!mDbi.isLocalUser(user.toString(), sToDomain)) {
                                    // if wildcard account exists
                                    if (!mDbi.isLocalUser("*", sToDomain)) {
                                        logFile.writeDate("NOUSER " + user + "@" + toDomain);
                                        error = EUSERNOTHERE;
                                        break;
                                    }
                                    // don't fail but replace to with *@domain
                                    to = new ByteRef("*@").append(toDomain);
                                }
                            }

                            if (mDbi.isPOBoxFull(user.toString(), sToDomain)) {
                                logFile.writeDate("MAILBOX FULL for " + user + "@" + toDomain);
                                error = EMAILBOXFULL;
                                break;
                            }

                            if (localOnly && mDbi.useGuessPermission(sToDomain)) {

                                //                            // check for self hosted domains and deny those!
                                //                            if (null == MailCfg.dns.getExtendedSpf(fromDomain) && !validateDns(resolvedDomain)) {
                                //                                logFile.writeDate("BLOCKED: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
                                //                                        + "] for domain:" + fromDomain);
                                //                                error = new ByteRef("555-sending server is brown listed\r\n550 server: " + clientHelo
                                //                                        + "[" + remoteAddress + "=" + resolvedDomain + "] tried to send for domain: "
                                //                                        + fromDomain + "\r\n");
                                //                                break;
                                //                            }

                                final boolean okToSend = checkSendPermission(logFile, clientHelo.toString(),
                                        remoteAddress, resolvedDomain, fromUser, fromDomain, sToDomain);

                                if (!okToSend) {
                                    error = new ByteRef(
                                            "451-sending server is not yet permitted to send mail for senders domain\r\n451 server: "
                                                    + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
                                                    + "] tried to send for domain: " + fromDomain + "\r\n");
                                    break;
                                }
                            }

                            recipients.add(to.toString());
                            lwrite(RECIPOK);
                            ok = true;
                            break;
                        }

                        if (cmd.equals(DATA)) {
                            state = S_HELO;
                            if (recipients.size() == 0) {
                                error = ENORECIPIENTS;
                                break;
                            }

                            lwrite(K354);
                            os.flush();

                            MailEntry me = mDbi.createNewMail();
                            OutputStream fos = mDbi.getOutputStream(me.mailId);

                            try {
                                long now = System.currentTimeMillis();
                                // insert a header
                                String h = "Received: from " + clientHelo + " (" + resolvedDomain + " ["
                                        + remoteAddress + "])\r\n\tby " + bServerName + " (" + getFull()
                                        + ")\r\n\twith SMTP id " + me.mailId + "\r\n\tfrom " + fromUser + "@"
                                        + fromDomain + "\r\n\tfor " + recipients.get(0) + ";\r\n\t"
                                        + DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(now) + "\r\n";
                                fos.write(h.getBytes());

                                // seek the empty line to reach end of header
                                ByteRef from = null;
                                boolean hasTo = false;
                                boolean hasDate = false;
                                for (line = readLine(br); line != null; line = readLine(br)) {
                                    // end of header
                                    int len = line.length();
                                    if (len == 0)
                                        break;
                                    // write to stream
                                    line.writeTo(fos);
                                    CRLF.writeTo(fos);
                                    if (len >= 5) {
                                        if ("from:".equals(line.substring(0, 5).toString().toLowerCase()))
                                            from = line.substring(5).trim().toLowerCase();
                                        if ("date:".equals(line.substring(0, 5).toString().toLowerCase()))
                                            hasDate = true;
                                    }
                                    if (len >= 3) {
                                        if ("to:".equals(line.substring(0, 3).toString().toLowerCase()))
                                            hasTo = true;
                                    }
                                }
                                if (line == null)
                                    break; // bail out!

                                // end of header - fix it if necessary
                                if (from == null) {
                                    fos.write(("from: <" + fromUser + "@" + fromDomain + ">\r\n").getBytes());
                                } else if (localOnly) {
                                    final int bra = from.indexOf('<');
                                    final int ket = from.indexOf('>');
                                    if (bra >= 0 && ket > bra)
                                        from = from.substring(bra + 1, ket).trim();
                                    
                                    // remove trailing comments in braces '(...)'
                                    from = from.nextWord('(').trim();
                                    
                                    final int at2 = from.indexOf('@');
                                    final String innerFromUser = from.substring(0, at2).toString();
                                    final String innerFromDomain = from.substring(at2 + 1).toString();

                                    for (final String recipient : recipients) {
                                        final int at = recipient.indexOf('@');
                                        final String sToDomain = recipient.substring(at + 1);

                                        if (mDbi.useGuessPermission(sToDomain)) {
                                            final boolean okToSend = checkSendPermission(logFile,
                                                    clientHelo.toString(), remoteAddress, resolvedDomain,
                                                    innerFromUser, innerFromDomain, sToDomain);
                                            if (!okToSend) {
                                                error = new ByteRef(
                                                        "451-sending server is not yet permitted to send mail for senders domain\r\n451 server: "
                                                                + clientHelo + "[" + remoteAddress + "="
                                                                + resolvedDomain + "] tried to send for domain: "
                                                                + innerFromDomain + "\r\n");

                                                line = null;

                                                // skip the mail content
                                                copyMail(null);
                                                break;
                                            }
                                        }
                                    }
                                    // from check failed
                                    if (line == null)
                                        break;
                                }
                                if (!hasTo) {
                                    fos.write(("to: <" + recipients.get(0) + ">\r\n").getBytes());
                                }
                                if (!hasDate) {
                                    fos.write(("date: " + DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(now) + "\r\n")
                                            .getBytes());
                                }
                                CRLF.writeTo(fos);

                                if (!copyMail(fos)) {
                                    line = null;
                                }

                                /*
                                 * // collect the mail for (line = readLine(br); line != null; line = readLine(br)) { //
                                 * not stored in file if (line.equals(MAILEND)) break; // write to stream
                                 * line.writeTo(fos); crlf.writeTo(fos); }
                                 */

                                fos.flush();
                            } finally {
                                fos.close();
                            }
                            if (line == null)
                                break; // bail out!

                            File f = mDbi.storeMail(me);

                            try {
                                String vScan = factory.getProperty("virusScanner", "").trim();
                                if (vScan.length() != 0) {
                                    vScan = vScan + " " + f.toString();
                                    ByteArrayOutputStream msg = new ByteArrayOutputStream();
                                    int r = Process.execute(vScan, (InputStream) null, msg, null,
                                            factory.getIntProperty("virusScanTimeout", 120000));
                                    if (r != 0) {
                                        logFile.writeDate("VIRUS: " + f + " failed: " + msg);
                                        error = E551;
                                        break;
                                    }
                                }
                                logFile.writeDate("STORED mail from " + fromUser + "@" + fromDomain + " --> "
                                        + recipients);
                                mDbi.sendMultiMail(me, fromUser, fromDomain, recipients);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                error = EMIMECHECK;
                                break;
                            }

                            lwrite(MAILOK);

                            ok = true;
                            ((MailFactory) factory).mailCfg.getSpooler().wakeUp();
                            break;
                        }

                        logFile.writeDate("unknown: '" + cmd + "'");

                        break;
                    } // end switch
                }
            } catch (Exception ex) {
                lwrite(E450);
                os.flush();
                //ex.printStackTrace();
                throw ex;
            } finally {
                ((MailFactory) factory).releaseDbi(this, mDbi);
                mDbi = null;
            }
            if (VERBOSE)
                logFile.writeDate("ok=" + ok + ", state=" + state);
            if (!ok) {
                lwrite(error);
            }
            os.flush();
        }
    }

    private boolean copyMail(OutputStream fos) throws IOException {
        for (;;) {
            // read the next line
            ByteRef line = br.nextLineCRLF();
            while (line == null) {
                // read more data 
                if (br.update(is) == null)
                    return false;
                // check again for next line
                line = br.nextLineCRLF();
            }
            // check for end - which is a single dot
            if (line.charAt(0) == '.') {
                if (line.length() == 1)
                    break;
                // if 2nd char is '.' -> reduce to one '.'
                if (line.charAt(1) == '.')
                    line = line.substring(1);
            }
            // fos can be null to skip read data
            if (fos != null) {
                line.writeTo(fos);
                CRLF.writeTo(fos);
            }
        }
        return true;
    }

    private void lwrite(ByteRef msg) throws IOException {
        if (VERBOSE)
            logFile.writeDate(msg.toString().trim());
        msg.writeTo(os);
    }

    private void lwrite(String msg) throws IOException {
        if (VERBOSE)
            logFile.writeDate(msg.trim());
        ByteUtil.writeString(msg, os);
    }

    //
    //    /**
    //     * retrieve the NS entries get IPs for NS entries return false if all IPs are in the 255.255.255.0
    //     * 
    //     * @param domain
    //     *            the domain to resolve
    //     * @return
    //     */
    //    private static boolean validateDns(String domain) {
    //        if (domain == null)
    //            return false;
    //
    //        // System.out.println("NS for " + domain );
    //        Iterator<String> i = null;
    //        for (;;) {
    //            HashMap<String, String> map = new HashMap<String, String>();
    //            i = MailCfg.dns.getNsFromDomain(domain);
    //            if (i.hasNext()) {
    //                int dot = domain.indexOf('.');
    //                if (dot == domain.lastIndexOf('.')) {
    //                    HashSet<String> set = new HashSet<String>();
    //                    while (i.hasNext()) {
    //                        set.add(i.next());
    //                    }
    //                    i = MailCfg.dns.getNsFromDomain(domain.substring(dot + 1));
    //                    while (i.hasNext()) {
    //                        set.remove(i.next());
    //                    }
    //                    i = set.iterator();
    //                }
    //
    //                while (i.hasNext()) {
    //                    String nsName = i.next();
    //                    // System.out.println("NS = " + nsName);
    //                    if (nsName == null)
    //                        continue;
    //                    String ip = MailCfg.dns.getIpFromDomain(nsName);
    //                    // System.out.println("IP = " + ip);
    //                    if (ip == null)
    //                        continue;
    //                    dot = ip.lastIndexOf('.');
    //                    if (dot < 0)
    //                        continue;
    //                    ip = ip.substring(0, dot);
    //                    // System.out.println("masked = " + nsName);
    //                    map.put(ip, nsName);
    //                }
    //                // System.out.println(domain + ": " + map);
    //                if (map.size() >= 2)
    //                    return true;
    //            }
    //            int dot = domain.indexOf('.');
    //            if (dot < 0)
    //                return false;
    //            domain = domain.substring(dot + 1);
    //            dot = domain.indexOf('.');
    //            if (domain.indexOf('.') < 0)
    //                return false;
    //        }
    //    }

    /**
     * Check whether the MXs and the sending server are in the same segment. - retrieve all MX IPs and the domain ip -
     * retrieve all sender IPs - compare the first 3 segments 1.2.3.X
     * 
     * @param clientHelo
     *            the name in HELO
     * @param remoteAddress
     *            the connecting IP address
     * @param resolvedDomain
     *            the resolved domain for the IP address - might be null!!!
     * @param fromDomain
     *            the senders domain taken of from:
     * @return
     */
    private static boolean guessPermission(String clientHelo, String remoteAddress, String resolvedDomain,
            String fromDomain) {
        // ips contains all allowed senders
        HashSet<String> ips = new HashSet<String>();
        String ipForDomain = MailCfg.dns.getIpFromDomain(fromDomain);
        if (ipForDomain != null)
            ips.add(ipForDomain);

        // add all mail servers for fromDomain
        for (Iterator<String> i = MailCfg.dns.getMXfromDomain(fromDomain).iterator(); i.hasNext();) {
            String mxName = i.next();
            String ip = MailCfg.dns.getIpFromDomain(mxName);
            if (ip != null)
                ips.add(ip);
        }

        // gather the senders IP and some IP related to the sender
        HashSet<String> sendIps = new HashSet<String>();
        try {
            { // add the 255.255.0.0 part to sendIp
                String ip = remoteAddress;
                int dot2 = ip.indexOf('.') + 1;
                dot2 = ip.indexOf('.', dot2) + 1;
                ip = ip.substring(0, dot2);
                sendIps.add(ip);
            }
            // add all mail servers for the resolvedDomain to senders
            // System.out.println("add mx domains for: " + resolvedDomain);

            List<String> list = null;
            if (resolvedDomain != null)
                for (;;) {
                    list = MailCfg.dns.getMXfromDomain(resolvedDomain);
                    if (list != null)
                        break;
                    int dot = resolvedDomain.indexOf('.');
                    if (dot < 0)
                        break;
                    resolvedDomain = resolvedDomain.substring(dot + 1);
                }
            if (list != null)
                for (Iterator<String> i = list.iterator(); i.hasNext();) {
                    String ip = i.next();
                    if (ip != null)
                        ip = MailCfg.dns.getIpFromDomain(ip);
                    if (ip != null) {
                        int dot2 = ip.indexOf('.') + 1;
                        dot2 = ip.indexOf('.', dot2) + 1;
                        ip = ip.substring(0, dot2);
                        sendIps.add(ip);
                        // System.out.println("-->" + ip);
                    }
                }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // find matches
        for (Iterator<String> j = sendIps.iterator(); j.hasNext();) {
            String radd = j.next();
            for (Iterator<String> i = ips.iterator(); i.hasNext();) {
                String ip = i.next();
                // System.out.println(radd + "~" + ip);
                if (ip.startsWith(radd)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if at least one ip segments are in the resolvedDomain. e.g. nat-195-91-144-163.inet-comfort.ru and
     * 195.91.144.162 We check decimal and hex version.
     * 
     * @param resolvedDomain
     *            the resolved Domain
     * @param ip
     *            the ip
     * @return true if one segment matches.
     */
    private static boolean containsIp(String resolvedDomain, String ip) {
        int count = 0;
        for (StringTokenizer st = new StringTokenizer(ip, "."); st.hasMoreElements();) {
            String part = st.nextToken();
            if (resolvedDomain.indexOf(part) >= 0) {
                ++count;
                continue;
            }
            try {
                int i = Integer.parseInt(part);
                String hex = Integer.toHexString(i).toLowerCase();
                if (resolvedDomain.toLowerCase().indexOf(hex) >= 0) {
                    ++count;
                    continue;
                }
            } catch (Exception ex) {
            }
        }
        return count >= 1;
    }

    //    /**
    //     * Check whether the SOA entries for fromDomain matches either the resolvedDomain or one of the MX servers.
    //     * 
    //     * @param resolvedDomain
    //     *            the resolved domain (retrieved by the connected IP).
    //     * @param fromDomain
    //     *            thr from domain in the SMTP "MAIL FROM: <...>"
    //     * @return true if one SOA match is found.
    //     */
    //    private static boolean sameSoa(String resolvedDomain, String fromDomain) {
    //        if (resolvedDomain == null || fromDomain == null)
    //            return false;
    //
    //        String resolvedSoa = getSoa(resolvedDomain);
    //        if (resolvedSoa == null)
    //            return false;
    //        // get soa for fromDomain
    //        String fromSoa = getSoa(fromDomain);
    //        if (fromSoa == null)
    //            return false;
    //
    //        if (resolvedSoa.equals(fromSoa))
    //            return true;
    //
    //        // check all SOAs for the MX of the fromDomain
    //        for (Iterator<String> i = MailCfg.dns.getMXfromDomain(fromDomain).iterator(); i.hasNext();) {
    //            String mxName = i.next();
    //
    //            String soa = getSoa(mxName);
    //            if (resolvedSoa.equals(soa))
    //                return true;
    //        }
    //
    //        return false;
    //    }

    //    private static String getSoa(String mxName) {
    //        for (;;) {
    //            String soa = MailCfg.dns.getSoaFromDomain(mxName);
    //            if (soa != null) {
    //                return soa;
    //            }
    //            int dot = mxName.indexOf('.');
    //            if (dot < 0)
    //                break;
    //            mxName = mxName.substring(dot + 1);
    //            if (mxName.indexOf('.') < 0)
    //                break;
    //        }
    //        return null;
    //    }

    /**
     * @param fromUser
     * @param fromDomain
     * @return
     */
    private boolean validateSender(String fromUser, String fromDomain) {
        if (fromUser.length() + fromDomain.length() == 0)
            return true;
        //        Socket socket = null;
        //        InputStream is = null;
        //        OutputStream os = null;
        for (int pass = 0; pass < 5; ++pass) {
            try {
                List<String> mailServer = MailCfg.dns.getRealMXfromDomain(fromDomain);

                if (VERBOSE)
                    System.out.println("mx=" + mailServer);

                if (mailServer.size() > 0) {
                    logFile.writeDate("validate sender: " + fromUser + "@" + fromDomain + " ==> " + mailServer);
                    return true;
                }
                /*
                 * for (Iterator i = mailServer.iterator(); i.hasNext();) { try { String mailAddr = (String) i.next();
                 * if (VERBOSE) System.out.println("using: " + mailAddr);
                 * 
                 * socket = new Socket(mailAddr, 25); socket.setSoTimeout(3 * 60 * 1000); is = socket.getInputStream();
                 * os = socket.getOutputStream(); ByteRef br = new ByteRef(); ByteRef line = null; while ((line == null
                 * || line.charAt(3) == '-') && br != null) { if (line == null) br = br.update(is); line =
                 * br.nextLine(); } if (VERBOSE) System.out.println(line);
                 * 
                 * ByteRef cmd = line.substring(0, 3).toUpperCase(); if (!cmd.equals(N220)) continue;
                 * 
                 * String a = "HELO " + bServerName + "\r\n"; os.write(a.getBytes()); if (VERBOSE)
                 * System.out.println(a);
                 * 
                 * line = br.nextLine(); while ((line == null || line.charAt(3) == '-') && br != null) { if (line ==
                 * null) br = br.update(is); line = br.nextLine(); } if (VERBOSE) System.out.println(line);
                 * 
                 * cmd = line.substring(0, 3).toUpperCase(); if (!cmd.equals(N250)) continue;
                 * 
                 * a = "MAIL FROM:<>\r\n"; os.write(a.getBytes()); if (VERBOSE) System.out.println(a);
                 * 
                 * line = br.nextLine(); while ((line == null || line.charAt(3) == '-') && br != null) { if (line ==
                 * null) br = br.update(is); line = br.nextLine(); } if (VERBOSE) System.out.println(line); cmd =
                 * line.substring(0, 3); if (!cmd.equals(N250)) continue;
                 * 
                 * a = "RCPT TO:<" + fromUser + '@' + fromDomain + ">\r\n"; os.write(a.getBytes()); if (VERBOSE)
                 * System.out.println(a);
                 * 
                 * line = br.nextLine(); while ((line == null || line.charAt(3) == '-') && br != null) { if (line ==
                 * null) br = br.update(is); line = br.nextLine(); } if (VERBOSE) System.out.println(line);
                 * 
                 * cmd = line.substring(0, 3); if (cmd.equals(N250)) return true;
                 * 
                 * } catch (Throwable e) { } finally { try { if (os != null) os.write("quit\r\n".getBytes()); } catch
                 * (Exception e) { // } try { if (socket != null) socket.close(); } catch (Exception e) { // } is =
                 * null; os = null; socket = null; } }
                 */
            } catch (Throwable e) {
                // } finally
                // {
                // try
                // {
                // if (socket != null)
                // socket.close();
                // } catch (Exception e)
                // {
                // //
                // }
            }
        }
        return false;
    }

    /**
     * Validate domain whether IP exists and remoteAddress whether domain name exists.
     * 
     * @param domain
     * @param remoteAddress
     * @return true if at least one test was succesful.
     */
    private boolean validateServer(ByteRef domain, String remoteAddress) {
        String addr = MailCfg.dns.getIpFromDomain(domain.toString());
        String name = MailCfg.dns.getDomainFromIp(remoteAddress);
        boolean res = domain.equalsIgnoreCase(name) || remoteAddress.equals(addr);
        logFile.writeDate("validate server: " + domain + " [" + remoteAddress + "] == " + name + " [" + addr + "] : "
                + res);
        return res;
    }

    private boolean checkSendPermission(LogFile logFile, String clientHelo, String remoteAddress,
            String resolvedDomain, String fromUser, String fromDomain, String toDomain) {

        final Boolean cachedResult = checkCache.get(fromDomain + "#" + toDomain);
        if (cachedResult != null)
            return cachedResult;

        String fromEmail = fromUser + "@" + fromDomain;
        // check SPF
        int ret = SpfContext.validateSpf(fromEmail, toDomain, remoteAddress, resolvedDomain, clientHelo);
        logFile.writeDate("spf: " + (char) ret + " " + fromEmail + ", " + toDomain + ", " + remoteAddress + ", "
                + resolvedDomain + ", " + clientHelo);
        boolean r = false;
        if (ret == '+') {
            r = true;
        } else if (ret == '-' || ret == '~') {
            logFile.writeDate("NOSENDPERMISSION: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
                    + "] for domain:" + fromDomain);
        } else { // ? or error
            //            if (resolvedDomain != null && !containsIp(resolvedDomain, remoteAddress)) {
            //                // accept sub domains with 1 level depths only
            //                r = resolvedDomain.endsWith(fromDomain);
            //                if (r) {
            //                    String subdomain = resolvedDomain.substring(0, resolvedDomain.length() - fromDomain.length());
            //                    if (!subdomain.endsWith(".")) {
            //                        r = false;
            //                    } else {
            //                        subdomain = subdomain.substring(0, subdomain.length() - 1);
            //                        r = subdomain.indexOf('.') < 0;
            //                    }
            //                    if (r)
            //                        logFile.writeDate("direct subdomain: " + subdomain + " :: " + fromDomain);
            //                }
            //                // check SOA
            //                //                if (!r) {
            //                //                    r = sameSoa(resolvedDomain, fromDomain);
            //                //                    if (r)
            //                //                        logFile.writeDate("samesoa succeeded: " + resolvedDomain + " =~= " + fromDomain);
            //                //                }
            //
            //                // check MX / DNS IP addresses
            //                if (!r) {
            //                    r = guessPermission(clientHelo, remoteAddress, resolvedDomain, fromDomain);
            //                    if (r)
            //                        logFile.writeDate("GUESSPERMISSION: " + clientHelo + ", " + remoteAddress + ", "
            //                                + resolvedDomain + ", " + fromDomain);
            //                }
            //            }

            //            if (!r) {
            // simple grey listing
            // 1. put new entries to GREYLIST1 and GREYLIST2
            // 2. accept if GREYLIST1 has expired but GREYLIST2 contains it
            // 3. promote to GREYLIST3 on success
            final String domainKey = fromDomain + "::" + remoteAddress;
            if (SmtpFactory.GREYLIST3.containsKey(domainKey)) {
                r = true;
                SmtpFactory.GREYLIST3.touch(domainKey);
            } else {
                final String senderKey = fromUser + "@" + domainKey + "::" + toDomain;
                if (SmtpFactory.GREYLIST2.containsKey(senderKey)) {
                    if (!SmtpFactory.GREYLIST1.containsKey(senderKey)) {
                        r = true;
                        SmtpFactory.GREYLIST3.put(senderKey, senderKey);
                    }
                } else {
                    SmtpFactory.GREYLIST2.put(senderKey, senderKey);
                    // use a longer wait time
                    if (containsIp(fromDomain, remoteAddress) || !resolvedDomain.equals(clientHelo)) {
                        long maxWait = SmtpFactory.GREYLIST2.getTimeout() / 2;
                        SmtpFactory.GREYLIST1.put(senderKey, senderKey, maxWait);
                    } else {
                        SmtpFactory.GREYLIST1.put(senderKey, senderKey);
                    }
                }
            }
            if (r) {
                logFile.writeDate("GREYLIST OK: " + clientHelo + ", " + remoteAddress + ", " + resolvedDomain + ", "
                        + fromDomain);
            } else {
                logFile.writeDate("GREYLIST PENDING: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
                        + "] for domain:" + fromDomain);
            }
            //            }
        }

        logFile.writeDate("check: " + r + " " + clientHelo + ", " + remoteAddress + ", " + resolvedDomain + ", "
                + fromUser + ", " + fromDomain + ", " + toDomain);
        checkCache.put(fromDomain + "#" + toDomain, r);
        return r;
    }
    //        /**
    //         */ static String[][] tests = {
    //             // {"ext-ch1gw-9.online-age.net", "64.37.194.13", "ext-ch1gw-9.online-age.net", "klaus.riske", "ge.com", "franke.ms"},
    //             // { "katharina.schmitzhaaxy@enet.vn", "203.190.163.25", "user.enet.vn", "katharina.schmitzhaax", "enet.vn", "franke.ms" },
    //             // { "niegacy4768@ne.jp", "220.147.104.12", "eaoska264124.adsl.ppp.infoweb.ne.jp", "niegacy4768", "ne.jp", "franke.ms" },
    //             // { "ecard@ecards4u.de", "80.237.209.112", "mail.ecards4u.de", "ecard", "ecards4u.de", "franke.ms" },
    //             // { "christagronbach@yahoo.de", "217.146.183.113", "nm19-vm0.bullet.mail.ukl.yahoo.com", "christagronbach", "yahoo.de", "bejy.net"},
    //             { "returnto@yelp.com", "199.255.189.140", "smtplow.yelpcorp.com", "returnto", "yelp.com", "bejy.net"},
    //          
    //          };
    //         
    //        /**
    //         * Test the block function.
    //         * 
    //         * @param args
    //         */
    //        public static void main(String args[]) {
    //            LogFile logFile = new LogFile("*");
    //            DnsCfg dnsCfg = new DnsCfg();
    //            Dns dns = MailCfg.dns = (Dns) dnsCfg.create();
    //            dns.setProperty("server", "8.8.8.8, 192.168.0.1");
    ////            dns.setProperty("server", "8.8.8.8");
    //    //        dns.setProperty("server", "212.23.97.2");
    //            Config config = Config.getInstance();
    //            config.addChild("dns", dns);
    //            try {
    //                dns.activate(logFile);
    //                logFile.writeln("bejy.net --> " + MailCfg.dns.getIpFromDomain("bejy.net"));
    //                for (int i = 0; i < tests.length; ++i) {
    //                    logFile.writeln("r = "
    //                            + checkSendPermission(logFile, tests[i][0], tests[i][1], tests[i][2], tests[i][3], tests[i][4],
    //                                    tests[i][5]));
    //                }
    //    
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //        }

}

/******************************************************************************
 * $Log: Smtp.java,v $
 * Revision 6.3  2014/10/04 19:11:09  bebbo
 * @R some sender stop to send after enabling STARTTLS. Those are now tracked and for 3 hours STARTLS is not offered to those
 *
 * Revision 6.2  2014/09/22 09:22:24  bebbo
 * @V new version
 *
 * Revision 6.1  2014/09/21 18:50:17  bebbo
 * 6.1
 *
 * Revision 1.86  2014/09/19 19:53:08  bebbo
 * @B added missing "\r\n" to STARTTLS message
 *
 * Revision 1.85  2014/08/02 17:45:50  bebbo
 * @R HELO and EHLO are now always allowed.
 * Revision 1.84 2014/06/23 19:08:43 bebbo
 * 
 * @N added support for STARTTLS
 *
 *    Revision 1.83 2014/03/23 22:05:58 bebbo
 * @B fixed dot handling in incoming mails Revision 1.82 2013/12/15 10:19:33 bebbo
 * 
 * @R GREYLISTING is now used unless SPF rejects it.
 * @N added a simple spf lookup cache, for mail with multiple receivers. Revision 1.81 2013/11/28 10:43:33 bebbo
 * 
 * @I aded 1 new test
 * 
 *    Revision 1.80 2013/11/01 13:32:48 bebbo
 * @R uses new mechanism for logins to slow down password guess attacks Revision 1.79 2013/05/17 10:58:01 bebbo
 * 
 * @N added support for immediate user name after AUTH Revision 1.78 2013/03/07 12:44:44 bebbo
 * 
 * @R grey listing uses a longer timeout if the resolved sender name contains any part of its ip address. Revision 1.77
 *    2012/11/08 12:16:28 bebbo
 * 
 * @N added grey listing Revision 1.76 2012/10/28 21:55:59 bebbo
 * 
 * @R improved grey listing: sender e-mail must match to trigger grey list allow Revision 1.75 2012/10/08 04:32:50 bebbo
 * 
 * @N added a simple grey listing implementation Revision 1.74 2012/07/18 09:11:34 bebbo
 * 
 * @I typified Revision 1.73 2011/09/16 16:20:49 bebbo
 * 
 * @F formatted Revision 1.72 2011/08/29 10:16:12 bebbo
 * 
 * @V version/year update Revision 1.71 2011/04/06 21:15:47 bebbo
 * 
 * @I removed some dead code Revision 1.70 2011/04/06 20:59:08 bebbo
 * 
 * @D improved VERBOSE mode Revision 1.69 2010/07/08 18:20:06 bebbo
 * 
 * @R changed validate sender to only check the existence of one or more MX entries
 * 
 *    Revision 1.68 2010/07/01 11:56:12 bebbo
 * @R removed some permission checks
 * @R changed default SPF to be more strict
 * @R an 5xx error now stops resending e-mails
 * 
 *    Revision 1.67 2010/07/01 10:08:51 bebbo
 * @R Spooler does no retries after a 5xx message
 * 
 *    Revision 1.66 2010/04/10 12:07:00 bebbo
 * @R sharpened guessPermssion: only 1st level subdomains are allowed now
 * 
 *    Revision 1.65 2009/11/18 08:48:50 bebbo
 * @B allow sending only for configured forwarders
 * 
 *    Revision 1.64 2009/09/06 09:26:46 bebbo
 * @R restricted sender to login user (fixed)
 * 
 *    Revision 1.63 2009/09/06 09:12:35 bebbo
 * @R restricted sender to login user
 * 
 *    Revision 1.62 2008/03/13 20:46:58 bebbo
 * @R modified the SPAM blocker 'guesspermission'
 * 
 *    Revision 1.61 2008/02/20 19:07:43 bebbo
 * @R modified filter behaviour.
 * 
 *    Revision 1.60 2007/09/08 16:31:43 bebbo
 * @B fixed that all subdomains were accepted - problematic for dialups
 * 
 *    Revision 1.59 2007/09/08 06:51:48 bebbo
 * @N added SOA comparison to relax guessPermission
 * 
 *    Revision 1.58 2007/09/06 10:25:55 bebbo
 * @N added 'brown' listing to block self configured servers (all DNS in same segment)
 * 
 *    Revision 1.57 2007/04/25 06:19:44 bebbo
 * @B sending multiple mails works again
 * 
 *    Revision 1.56 2007/04/13 17:59:10 bebbo
 * @I blocking more invalid user names, but this might be not correct
 * @B fixed the buffer handling after each received email
 * 
 *    Revision 1.55 2007/02/12 20:36:28 bebbo
 * @B moved STORED message behind VIRUS message
 * 
 *    Revision 1.54 2007/01/18 21:52:04 bebbo
 * @N added SPF support
 * 
 *    Revision 1.53 2006/10/12 05:59:01 bebbo
 * @B fix loss of presend data after receiving an email
 * 
 *    Revision 1.52 2006/05/09 13:03:47 bebbo removed output for verbose settings
 * 
 *    Revision 1.51 2006/05/09 08:51:24 bebbo
 * @B fixed handling of short mails
 * 
 *    Revision 1.50 2006/03/17 20:05:36 bebbo
 * @I cleanup
 * 
 *    Revision 1.49 2006/03/17 14:54:14 bebbo
 * @B fixed new implementation of binary copy
 * 
 *    Revision 1.48 2006/03/17 14:00:00 bebbo
 * @B corrupt mails are dropped now
 * 
 *    Revision 1.47 2006/03/17 13:34:54 bebbo
 * @B fixed double close of statements - invented in last fix... /bow
 * 
 *    Revision 1.46 2006/03/17 11:36:24 bebbo
 * @B fixed unclosed streams
 * 
 *    Revision 1.45 2005/12/31 15:45:42 bebbo
 * @N added VERBOSE switch
 * @B fixed that you were unable to send to local users
 * 
 *    Revision 1.44 2005/11/30 07:09:38 bebbo
 * @N added *@domain support
 * @N added PO box quota support
 * @R guessPermission is now domain dependant
 * 
 *    Revision 1.43 2004/12/16 16:15:07 bebbo
 * @R database connections are now shared
 * @N added support to hook a virus scanner
 * 
 *    Revision 1.42 2004/12/13 15:40:22 bebbo
 * @N added sender validation - experimental (but not bad)
 * 
 *    Revision 1.41 2004/04/07 16:33:46 bebbo
 * @V new version message
 * 
 *    Revision 1.40 2004/03/24 09:54:10 bebbo
 * @V new version information
 * 
 *    Revision 1.39 2004/03/24 09:50:11 bebbo
 * @V new version information
 * 
 *    Revision 1.38 2004/03/23 12:38:52 bebbo
 * @V the protocols are now using the same version as BEJY
 * 
 *    Revision 1.37 2003/12/18 10:43:26 bebbo
 * @B added flush to output after QUIT
 * 
 *    Revision 1.36 2003/09/25 10:25:51 bebbo
 * @D added VERBOSE output
 * 
 *    Revision 1.35 2003/09/05 11:00:39 bebbo
 * @B fixed NOACCESS error message
 * 
 *    Revision 1.34 2003/06/24 09:34:48 bebbo
 * @N added functionality to validate mail sending server and sender
 * 
 *    Revision 1.33 2003/06/23 15:20:30 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.32 2003/06/17 10:20:18 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.31 2003/04/30 13:53:32 bebbo
 * @I preps for spam protection
 * 
 *    Revision 1.30 2003/04/02 09:25:11 bebbo
 * @R removed DNS to avoid spam, since it is not correct (sigh)
 * 
 *    Revision 1.29 2003/03/31 16:35:06 bebbo
 * @N added DNS support to verify mail sender
 * 
 *    Revision 1.28 2003/03/21 10:14:53 bebbo
 * @I set buffer size to 1412
 * @B added required os.flush() statements
 * 
 *    Revision 1.27 2003/02/25 07:01:53 bebbo
 * @R protocols are usinf now BufferedOutputStreams
 * 
 *    Revision 1.26 2003/02/17 14:22:15 bebbo
 * @B now anonymous sender MAIL FROM:<> is allowed. But only for local recipients
 * 
 *    Revision 1.25 2003/02/05 08:09:46 bebbo
 * @B better address handling
 * @N usage of VERBOSE attribute from config
 * 
 *    Revision 1.24 2003/01/27 19:30:43 bebbo
 * @I updated version information
 * 
 *    Revision 1.23 2002/12/17 14:01:38 bebbo
 * @B fixed a to early ResultSet close
 * @N added a recovery function for lost and still available mails
 * 
 *    Revision 1.22 2002/12/16 19:55:42 bebbo
 * @B tracked some unclosed Statements / Connections down and fixed them
 * 
 *    Revision 1.21 2002/12/16 16:40:53 bebbo
 * @I added sync for db implementation to release resources
 * 
 *    Revision 1.20 2002/12/02 18:39:37 bebbo
 * @B fixed multiple statement use -> caused errors.
 * 
 *    Revision 1.19 2002/11/22 21:14:24 bebbo
 * @I added method shutdown() - closing DBI
 * 
 *    Revision 1.18 2002/11/19 12:34:09 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.17 2002/03/21 14:37:23 franke
 * @B fixed duplicate copyright in hello
 * 
 *    Revision 1.16 2002/02/16 13:57:55 franke
 * @V now reflecting implementions version number (not factory)
 * 
 *    Revision 1.15 2002/02/05 16:47:40 bebbo
 * @V nicer hello message with implementation version number
 * 
 *    Revision 1.14 2002/02/05 15:57:06 bebbo
 * @V nicer hello message with implementation version number
 * 
 *    Revision 1.13 2002/01/20 15:51:27 franke
 * @R the messages are stored without dot!
 * 
 *    Revision 1.12 2002/01/20 12:27:43 franke
 * @D better error message when a mailbox is not allowed
 * 
 *    Revision 1.11 2002/01/19 15:49:47 franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 * 
 *    Revision 1.10 2002/01/13 15:19:45 franke
 * @N added commands SAML, SOML and SEND
 * @R reflected mDbi changes
 * 
 *    Revision 1.9 2001/08/24 08:24:41 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.8 2001/08/14 16:10:00 bebbo
 * @B fixed error codes
 * 
 *    Revision 1.7 2001/03/28 10:25:07 bebbo
 * @D more debug info
 * 
 *    Revision 1.6 2001/03/27 19:50:15 bebbo
 * @I now Protocl knows its Factory
 * @I now Factory is known by Protocol
 * 
 *    Revision 1.5 2001/03/20 18:34:32 bebbo
 * @I changed hostAddress into remoteAddress
 * 
 *    Revision 1.4 2001/02/25 17:08:00 bebbo
 * @R no longer public classes
 * 
 *    Revision 1.3 2001/02/20 19:13:57 bebbo
 * @B now calling wakeUp instead of interrupt
 * @D disabled VERBOSE
 * 
 *    Revision 1.2 2001/02/20 17:39:27 bebbo
 * @B interrupting spooler for instant mail sending
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *    Revision 1.12 2001/02/12 18:02:51 bebbo
 * @N using the keep flag, to keep files even when the account does forwarding
 * 
 *    Revision 1.11 2001/02/12 12:52:07 bebbo
 * @N added support for forwarder
 * 
 *    Revision 1.10 2001/01/15 16:34:33 bebbo
 * @B fixed mail header
 * @B fixed storage of borken files - no longer added to user accounts
 * 
 *    Revision 1.9 2001/01/01 17:06:15 bebbo
 * @B fixed format of date header
 * 
 *    Revision 1.8 2001/01/01 16:53:43 bebbo
 * @N added AUTH LOGIN support (e.g. Outlook Express)
 * 
 *    Revision 1.7 2001/01/01 01:02:34 bebbo
 * @I improved database reconnect
 * 
 *    Revision 1.6 2000/12/30 10:16:39 bebbo
 * @I reformatted header msg
 * 
 *    Revision 1.5 2000/12/30 09:16:42 bebbo
 * @I fixed format of mail header
 * 
 *    Revision 1.4 2000/12/30 09:05:03 bebbo
 * @R now throws Exceptions to indicate that a thread should end
 * 
 *    Revision 1.3 2000/12/29 17:50:57 bebbo
 * @I added verbose messages
 * @I added information header to received mails
 * 
 *    Revision 1.2 2000/12/28 21:04:26 bebbo
 * @D DEBUG cleanup
 * 
 *    Revision 1.1 2000/12/28 20:54:41 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
