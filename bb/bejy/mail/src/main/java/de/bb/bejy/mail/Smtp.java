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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
        String s = "$Revision: 6.4 $";
        no = "1." + s.substring(11, s.length() - 1);
        version = Version.getShort() + " ESMTP " + no
                + " (c) 2000-2015 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
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

    protected Smtp(SmtpFactory sf, LogFile _logFile) {
        super(sf);
        logFile = _logFile;

        recipients = new ArrayList<String>();
        bServerName = new ByteRef(sf.mailCfg.getMainDomain());

        VERBOSE = DEBUG | sf.isVerbose();
        // logFile.writeDate(t() + "VERBOSE is " + VERBOSE);
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
            lwrite(READY);
            os.flush();
        } catch (Exception e) {
            return false;
        }
        return super.trigger();
    }

    protected boolean doit() throws Exception {
        String userId = null;

        String resolvedDomain = MailCfg.getDNS().getDomainFromIp(remoteAddress);
        logFile.writeDate(tx() + "connect from [" + remoteAddress + "=" + resolvedDomain + "]");
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

            if (line.startsWith(LOGIN)) {
                logFile.writeDate(tin() + "LOGIN ...");
            } else {
                logFile.writeDate(tin() + line.toString());
            }

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
//                logFile.writeDate(t() + "starting TLS");
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
                    logFile.writeDate(tx() + "STARTTLS failed: " + ioe.getMessage());
                    throw ioe;
                }

                logFile.writeDate(tx() + "STARTTLS using: " + s3.getCipherSuite());
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
                        logFile.writeDate(tx() + "NOHELO from " + remoteAddress);
                        error = ENOVALIDSERVER;
                    } else if (resolvedDomain == null) {
                        logFile.writeDate(tx() + "unresolved domain in HELO from " + remoteAddress);
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
                        logFile.writeDate(tx() + "NOHELO from " + remoteAddress);
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
                                logFile.writeDate(tx() + "login for user: " + user);

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
                                logFile.writeDate(tx() + "login FAILURE for " + user + "@" + domain + " from "
                                        + remoteAddress);
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
                                logFile.writeDate(tx() + "UNRESOLVED: " + msg);
                                error = new ByteRef("450 " + msg + "\r\n");
                                break;
                            }

                            if (user.length() == 0 && domain.length() == 0) {
                                fromUser = "postmaster";

                                fromDomain = resolvedDomain;
                                // allow to step out one level
                                if (MailCfg.getDNS().getRealMXfromDomain(fromDomain).size() == 0) {
                                    int dot = fromDomain.indexOf('.');
                                    fromDomain = fromDomain.substring(dot + 1);
                                }

                                logFile.writeDate(tx() + "ANONYMOUS MAIL FROM:<> " + clientHelo + "[" + remoteAddress + "="
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
                                    logFile.writeDate(tx() + "REQAUTH: " + clientHelo + "[" + remoteAddress + "="
                                            + resolvedDomain + "] sender:" + user + "@" + domain);
                                    logFile.writeDate(tx() + "NORELAY to " + user + "@" + domain + " login required");
                                    error = REQAUTH;
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
                                        logFile.writeDate(tin() + m);
                                        error = new ByteRef(m + "\r\n");
                                        break;
                                    }
                                }

                                if (!mDbi.isLocalDomain(fromDomain)) {
                                    String m = ENOTLOCAL + fromDomain;
                                    logFile.writeDate(tin() + m);
                                    error = new ByteRef(m + "\r\n");
                                    break;
                                }

                                lwrite(SENDANY);
                            }

                            logFile.writeDate(tx() + "MAIL FROM:<" + fromUser + "@" + fromDomain + "> " + clientHelo + "["
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
                                        logFile.writeDate(tx() + "NOUSER " + user + "@" + toDomain);
                                        error = EUSERNOTHERE;
                                        break;
                                    }
                                    // don't fail but replace to with *@domain
                                    to = new ByteRef("*@").append(toDomain);
                                }
                            }

                            if (mDbi.isPOBoxFull(user.toString(), sToDomain)) {
                                logFile.writeDate(tx() + "MAILBOX FULL for " + user + "@" + toDomain);
                                error = EMAILBOXFULL;
                                break;
                            }

                            if (localOnly && mDbi.useGuessPermission(sToDomain)) {

                                //                            // check for self hosted domains and deny those!
                                //                            if (null == MailCfg.getDNS().getExtendedSpf(fromDomain) && !validateDns(resolvedDomain)) {
                                //                                logFile.writeDate(t() + "BLOCKED: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
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
                                    
//                                    final int bra = from.indexOf('<');
//                                    final int ket = from.indexOf('>');
//                                    if (bra >= 0 && ket > bra)
//                                        from = from.substring(bra + 1, ket).trim();
//                                    
//                                    // remove trailing comments in braces '(...)'
//                                    from = from.nextWord('(').trim();
//                                    
//                                    final int at2 = from.indexOf('@');
//                                    final String innerFromUser = from.substring(0, at2).toString();
//                                    final String innerFromDomain = from.substring(at2 + 1).toString();
//
//                                    for (final String recipient : recipients) {
//                                        final int at = recipient.indexOf('@');
//                                        final String sToDomain = recipient.substring(at + 1);
//
//                                        if (mDbi.useGuessPermission(sToDomain)) {
//                                            final boolean okToSend = checkSendPermission(logFile,
//                                                    clientHelo.toString(), remoteAddress, resolvedDomain,
//                                                    innerFromUser, innerFromDomain, sToDomain);
//                                            if (!okToSend) {
//                                                error = new ByteRef(
//                                                        "451-sending server is not yet permitted to send mail for senders domain\r\n451 server: "
//                                                                + clientHelo + "[" + remoteAddress + "="
//                                                                + resolvedDomain + "] tried to send for domain: "
//                                                                + innerFromDomain + "\r\n");
//
//                                                line = null;
//
//                                                // skip the mail content
//                                                copyMail(null);
//                                                break;
//                                            }
//                                        }
//                                    }
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
                                        logFile.writeDate(tx() + "VIRUS: " + f + " failed: " + msg);
                                        error = E551;
                                        break;
                                    }
                                }
                                logFile.writeDate(tx() + "STORED mail from " + fromUser + "@" + fromDomain + " --> "
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

                        logFile.writeDate(tx() + "unknown: '" + cmd + "'");

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
                logFile.writeDate(tx() + "ok=" + ok + ", state=" + state);
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
        logFile.writeDate(tout() + msg.toString().trim());
        msg.writeTo(os);
    }

    private void lwrite(String msg) throws IOException {
        logFile.writeDate(tout() + msg.trim());
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
    //            i = MailCfg.getDNS().getNsFromDomain(domain);
    //            if (i.hasNext()) {
    //                int dot = domain.indexOf('.');
    //                if (dot == domain.lastIndexOf('.')) {
    //                    HashSet<String> set = new HashSet<String>();
    //                    while (i.hasNext()) {
    //                        set.add(i.next());
    //                    }
    //                    i = MailCfg.getDNS().getNsFromDomain(domain.substring(dot + 1));
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
    //                    String ip = MailCfg.getDNS().getIpFromDomain(nsName);
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
//    private static boolean guessPermission(String clientHelo, String remoteAddress, String resolvedDomain,
//            String fromDomain) {
//        // ips contains all allowed senders
//        HashSet<String> ips = new HashSet<String>();
//        String ipForDomain = MailCfg.getDNS().getIpFromDomain(fromDomain);
//        if (ipForDomain != null)
//            ips.add(ipForDomain);
//
//        // add all mail servers for fromDomain
//        for (Iterator<String> i = MailCfg.getDNS().getMXfromDomain(fromDomain).iterator(); i.hasNext();) {
//            String mxName = i.next();
//            String ip = MailCfg.getDNS().getIpFromDomain(mxName);
//            if (ip != null)
//                ips.add(ip);
//        }
//
//        // gather the senders IP and some IP related to the sender
//        HashSet<String> sendIps = new HashSet<String>();
//        try {
//            { // add the 255.255.0.0 part to sendIp
//                String ip = remoteAddress;
//                int dot2 = ip.indexOf('.') + 1;
//                dot2 = ip.indexOf('.', dot2) + 1;
//                ip = ip.substring(0, dot2);
//                sendIps.add(ip);
//            }
//            // add all mail servers for the resolvedDomain to senders
//            // System.out.println("add mx domains for: " + resolvedDomain);
//
//            List<String> list = null;
//            if (resolvedDomain != null)
//                for (;;) {
//                    list = MailCfg.getDNS().getMXfromDomain(resolvedDomain);
//                    if (list != null)
//                        break;
//                    int dot = resolvedDomain.indexOf('.');
//                    if (dot < 0)
//                        break;
//                    resolvedDomain = resolvedDomain.substring(dot + 1);
//                }
//            if (list != null)
//                for (Iterator<String> i = list.iterator(); i.hasNext();) {
//                    String ip = i.next();
//                    if (ip != null)
//                        ip = MailCfg.getDNS().getIpFromDomain(ip);
//                    if (ip != null) {
//                        int dot2 = ip.indexOf('.') + 1;
//                        dot2 = ip.indexOf('.', dot2) + 1;
//                        ip = ip.substring(0, dot2);
//                        sendIps.add(ip);
//                        // System.out.println("-->" + ip);
//                    }
//                }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        // find matches
//        for (Iterator<String> j = sendIps.iterator(); j.hasNext();) {
//            String radd = j.next();
//            for (Iterator<String> i = ips.iterator(); i.hasNext();) {
//                String ip = i.next();
//                // System.out.println(radd + "~" + ip);
//                if (ip.startsWith(radd)) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }

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
    //        for (Iterator<String> i = MailCfg.getDNS().getMXfromDomain(fromDomain).iterator(); i.hasNext();) {
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
    //            String soa = MailCfg.getDNS().getSoaFromDomain(mxName);
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
     * Validate domain whether IP exists and remoteAddress whether domain name exists.
     * 
     * @param domain
     * @param remoteAddress
     * @return true if at least one test was succesful.
     */
    private boolean validateServer(ByteRef domain, String remoteAddress) {
        String addr = MailCfg.getDNS().getIpFromDomain(domain.toString());
        String name = MailCfg.getDNS().getDomainFromIp(remoteAddress);
        boolean res = domain.equalsIgnoreCase(name) || remoteAddress.equals(addr);
        logFile.writeDate(tx() + "validate server: " + domain + " [" + remoteAddress + "] == " + name + " [" + addr + "] : "
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
        logFile.writeDate(tx() + "spf: " + (char) ret + " " + fromEmail + ", " + toDomain + ", " + remoteAddress + ", "
                + resolvedDomain + ", " + clientHelo);
        boolean r = false;
        if (ret == '+') {
            r = true;
        } else if (ret == '-' || ret == '~') {
            logFile.writeDate(tx() + "NOSENDPERMISSION: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
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
            //                        logFile.writeDate(t() + "direct subdomain: " + subdomain + " :: " + fromDomain);
            //                }
            //                // check SOA
            //                //                if (!r) {
            //                //                    r = sameSoa(resolvedDomain, fromDomain);
            //                //                    if (r)
            //                //                        logFile.writeDate(t() + "samesoa succeeded: " + resolvedDomain + " =~= " + fromDomain);
            //                //                }
            //
            //                // check MX / DNS IP addresses
            //                if (!r) {
            //                    r = guessPermission(clientHelo, remoteAddress, resolvedDomain, fromDomain);
            //                    if (r)
            //                        logFile.writeDate(t() + "GUESSPERMISSION: " + clientHelo + ", " + remoteAddress + ", "
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
                logFile.writeDate(tx() + "GREYLIST OK: " + clientHelo + ", " + remoteAddress + ", " + resolvedDomain + ", "
                        + fromDomain);
            } else {
                logFile.writeDate(tx() + "GREYLIST PENDING: " + clientHelo + "[" + remoteAddress + "=" + resolvedDomain
                        + "] for domain:" + fromDomain);
            }
            //            }
        }

        logFile.writeDate(tx() + "check: " + r + " " + clientHelo + ", " + remoteAddress + ", " + resolvedDomain + ", "
                + fromUser + ", " + fromDomain + ", " + toDomain);
        checkCache.put(fromDomain + "#" + toDomain, r);
        return r;
    }

    private static String tx() {
        return "[" + Thread.currentThread().getName() + "] | ";
    }

    private static String tin() {
        return "[" + Thread.currentThread().getName() + "] < ";
    }
    private static String tout() {
        return "[" + Thread.currentThread().getName() + "] > ";
    }
}
