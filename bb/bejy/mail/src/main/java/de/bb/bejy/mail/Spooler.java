/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/mail/src/main/java/de/bb/bejy/mail/Spooler.java,v $
 * $Revision: 1.50 $
 * $Date: 2014/10/04 19:11:56 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * smtp mail spooler
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import de.bb.bejy.Configurable;
import de.bb.security.Asn1;
import de.bb.security.Pkcs6;
import de.bb.security.Ssl3Client;
import de.bb.util.ByteRef;
import de.bb.util.DateFormat;
import de.bb.util.LogFile;
import de.bb.util.SessionManager;

/**
 * waits until the time is come to kill the next session
 */
class Spooler extends Thread {
    private boolean terminate;

    private static boolean DEBUG = false;

    /** the time intervall used between retries after intervall switch. */
    long LONG_INTERVAL;

    /** the time intervall used between retries before intervall switch. */
    long SHORT_INTERVAL;

    /** the count of retries until intervall switches and a 2nd info is sent. */
    int INTERVAL_SWITCH;

    /** the max count of retries. */
    int MAX_RETRIES;

    /** the max sleep time for the spooler */
    private final static long MAXSLEEP = 1000L * 60 * 5;

    private final static int E_SUCCESS = 0, E_RETRY = 1, E_PERMANENT = 1000, E_INVALID_DOMAIN = 101,
            E_UNKNOWN_USER = 102;

    final static byte[] CRLF = { (byte) '\r', (byte) '\n' };

    final static byte[] EMPTY = {};

    LogFile logFile;

    // flag to prevent double wake up
    volatile boolean sleeping;

    MailCfg cfg;

    int sendCount;

    /**
     * create a Spooler object which is similar to SessionManager
     * 
     * @param logFile
     * @param cfg
     */
    Spooler(LogFile logFile, MailCfg cfg) {
        super("SMTP spooler");
        this.logFile = logFile;
        this.cfg = cfg;
        sleeping = false;
        DEBUG = cfg.getBooleanProperty("debug", false);
    }

    /**
   * 
   */
    void wakeUp() {
        synchronized (this) {
            if (!sleeping)
                return;
            sleeping = false;
            interrupt();
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    public void run() {
        MailDBI dbi = null;
        while (!terminate) {
            try {
                dbi = cfg.getDbi(this);
                if (dbi != null)
                    break;
            } catch (Exception e1) {
            }
            logFile.writeDate("ERROR: no DBI retrying in 30 seconds");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
            }
        }
        try {
            boolean maxReached = false;
            while (!terminate) {
                try {
                    // how long to sleep?
                    long diff = spoolerSleep(dbi);
                    if (diff > 0) {
                        if (DEBUG)
                            logFile.writeDate("sleeping for: " + diff);
                        try {
                            synchronized (this) {
                                sleeping = true;
                            }
                            diff = spoolerSleep(dbi);

                            if (maxReached && diff < 15000)
                                diff = 15000;
                            maxReached = false;

                            if (diff > 0 && !Thread.interrupted()) {
                                try {
                                    sleep(diff);
                                } catch (InterruptedException ie) {

                                }
                            }
                            synchronized (this) {
                                sleeping = false;
                            }
                        } catch (Throwable t) {
                        }
                        continue;
                    }

                    synchronized (cfg) {
                        if (sendCount >= cfg.getIntProperty("sendThreads", 3)) {
                            try {
                                cfg.wait();
                            } catch (Exception e) {
                            }
                            continue;
                        }
                    }

                    SpoolEntry se = spoolerNextMail(dbi);

                    if (se == null) {
                        continue;
                    }

                    Sender sender = new Sender(this, se);
                    synchronized (cfg) {
                        ++sendCount;
                    }
                    sender.start();

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            if (dbi != null)
                cfg.releaseDbi(this, dbi);
        }
    }

    /**
     * Determine spooler sleep interval.
     * 
     * @param dbi
     * @return
     * @throws Exception
     */
    private long spoolerSleep(MailDBI dbi) throws Exception {
        ResultSet rs = null;
        try {
            rs = dbi.selectNextFromSpool();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                Timestamp now = rs.getTimestamp(2);
                if (DEBUG)
                    logFile.writeDate("spoolerSleep: " + ts + " > " + now);
                long tNext = MAXSLEEP;
                if (ts != null) {
                    tNext = ts.getTime() - now.getTime();
                }
                if (tNext > MAXSLEEP)
                    tNext = MAXSLEEP;
                if (tNext < 0)
                    tNext = 0;
                return tNext;
            }
        } catch (Exception e) {
            if (DEBUG)
                logFile.writeDate(e.toString());
        } finally {
            if (rs != null)
                rs.close();
        }
        return MAXSLEEP;
    }

    /**
     * get next mail to spool
     * 
     * @param dbi
     * @return
     * @throws Exception
     */
    private SpoolEntry spoolerNextMail(MailDBI dbi) throws Exception {
        if (DEBUG)
            logFile.writeDate("spoolerNextMail");

        ResultSet rs = dbi.selectNextFromSpool();
        if (rs.next()) {
            java.sql.Timestamp next = rs.getTimestamp(1);
            if (DEBUG)
                logFile.writeDate("spoolerNextMail: next=" + next);

            rs.close();

            rs = dbi.selectFromSpool(next);

            try {
                if (rs.next()) {
                    String id = rs.getString(1);
                    if (DEBUG)
                        logFile.writeDate("spoolerNextMail: id=" + id);
                    SpoolEntry se = new SpoolEntry(id, rs.getString(2), rs.getString(3), rs.getString(4),
                    // 5 is next
                            rs.getInt(6), rs.getString(7), rs.getString(8));

                    // mark with retries=-1 -> processing
                    dbi.updateSpool(rs.getTimestamp(5).getTime(), -1, se.msgId);
                    return se;
                }
            } finally {
                Statement stmt = rs.getStatement();
                rs.close();
                stmt.close();
            }
        }
        if (DEBUG)
            logFile.writeDate("spooler did not find next entry!");
        return null;
    }

    /**
     * Read configured values.
     * 
     * @param c
     */
    public void init(Configurable c) {
        this.INTERVAL_SWITCH = c.getIntProperty("intervallSwitch", 10);
        this.MAX_RETRIES = c.getIntProperty("maxRetries", 20);
        this.SHORT_INTERVAL = c.getIntProperty("shortIntervall", 5);
        this.LONG_INTERVAL = c.getIntProperty("longIntervall", 60);
    }

    /**
     * close this thread.
     */
    void close() {
        terminate = true;
        this.interrupt();
    }

    static class Sender extends Thread {
        private static boolean VERBOSE = DEBUG;

        private final static ByteRef R220 = new ByteRef("220"), R250 = new ByteRef("250"), R354 = new ByteRef("354"),
                EOT = new ByteRef(".\r\n"), M_GIVEUP = new ByteRef(
                        "599 maximum retrycount reached - destination still not reachable\r\n"),
                M_INTERN = new ByteRef("598 general network error"), M_CONNECT_REJECT = new ByteRef("access denied: ");

        //        private byte data[] = new byte[8192];

        private MailCfg cfg;

        private LogFile logFile;

        private Spooler spooler;

        private MailDBI dbi;

        private SpoolEntry se;

        Sender(Spooler spooler, SpoolEntry se) {
            super("BEJY mail-sender");
            this.spooler = spooler;
            this.cfg = spooler.cfg;
            this.logFile = spooler.logFile;
            this.se = se;
        }

        /**
     */
        public void run() {
            dbi = null;
            try {
                dbi = cfg.getDbi(this);

                ByteRef line = new ByteRef();
                ByteRef reason = M_GIVEUP;

                int error = E_INVALID_DOMAIN;
                String mailServerDomain = se.toDomain;

                try {
                    if (DEBUG)
                        logFile.writeDate("try to resolve domain: " + se.toDomain);
                    List<String> mailServer = MailCfg.dns.getMXfromDomain(se.toDomain);
                    if (DEBUG)
                        logFile.writeDate("got " + mailServer + " for domain: " + se.toDomain);

                    // can we send?
                    if (mailServer == null) {
                        reason = new ByteRef("499 could not resolve domain: " + se.toDomain);
                    } else {
                        String localIp = MailCfg.dns.getIpFromDomain(se.fromDomain);
                        InetAddress localAddr = InetAddress.getByName(localIp);

                        Socket socket = null;

                        mailServerDomain = null;
                        boolean retryWithoutTLS = false;
                        for (Iterator<String> i = mailServer.iterator(); error != 0 && i.hasNext();) {
                            // try the next mail server
                            if (!retryWithoutTLS)
                                mailServerDomain = i.next();

                            if (mailServerDomain != null) {
                                try {
                                    if (VERBOSE)
                                        logFile.writeDate("open socket to " + mailServerDomain);
                                    try {
                                        socket = new Socket(mailServerDomain, 25, localAddr, 0);
                                    } catch (Exception ex) {
                                        socket = new Socket(mailServerDomain, 25);
                                    }
                                } catch (Exception e) {
                                }
                            }

                            // try to use the web server directly, if no mail server was found
                            if (socket == null) {
                                try {
                                    socket = new Socket(se.toDomain, 25);
                                } catch (Exception e) {
                                    reason = new ByteRef("498 no mail server available for domain: " + se.toDomain
                                            + "\r\n");
                                }
                            }
                            if (socket != null) {
                                if (VERBOSE)
                                    logFile.writeDate("send mail for " + se.toDomain + " to " + mailServerDomain);

                                try {
                                    socket.setSoTimeout(3 * 60 * 1000);
                                    InputStream is = socket.getInputStream();
                                    OutputStream os = socket.getOutputStream();

                                    ByteRef br = new ByteRef();

                                    // read server's message
                                    line = readResponse(br, is);
                                    ByteRef cmd = line.substring(0, 3).toUpperCase();
                                    if (VERBOSE)
                                        logFile.writeDate(mailServerDomain + ": " + line);
                                    if (cmd.equals(R220)) {
                                        try {
                                            if (VERBOSE)
                                                logFile.writeDate(mailServerDomain + ": open mail <" + se.mailId + ">");
                                            InputStream fis = dbi.getInputStream(se.mailId);
                                            try {

                                                final String ehlo = "EHLO " + se.fromDomain + "\r\n";
                                                os.write(ehlo.getBytes());

                                                //line = readResponse(br, is);
                                                
                                                boolean startTls = false;
                                                do {
                                                    line = ByteRef.readLine(br, is);
                                                    if (VERBOSE)
                                                        logFile.writeDate(line.toString());

                                                    startTls |= line.substring(4).equals("STARTTLS");
                                                } while (line.charAt(3) != ' ');
                                                
                                                if (VERBOSE)
                                                    logFile.writeDate(mailServerDomain + "> " + line);

                                                cmd = line.substring(0, 3).toUpperCase();
                                                if (cmd.equals(R250)) {
                                                    
                                                    // start TLS unless it's a retry
                                                    if (startTls && !retryWithoutTLS) {
                                                        retryWithoutTLS = true;
                                                        
                                                        os.write("STARTTLS\r\n".getBytes());
                                                        os.flush();
                                                        
                                                        line = readResponse(br, is);
                                                        if (VERBOSE)
                                                            logFile.writeDate(mailServerDomain + "> " + line);
                                                        
                                                        Ssl3Client client = new Ssl3Client();
                                                        client.connect(is, os, mailServerDomain);
                                                        if (VERBOSE)
                                                            logFile.writeDate(mailServerDomain + "> " + client.getCipherSuite());
                                                        
                                                        Vector<byte[]> certificates = client.getCertificates();
                                                        byte[] root = certificates.get(certificates.size() - 1);
                                                        // System.out.println(Asn1.dump(0, root, 0, root.length, false));

                                                        
                                                        byte[] issuer = Pkcs6.getCertificateIssuer(root);
                                                        // System.out.println(Asn1.dump(0, issuer, 0, issuer.length, false));

                                                        byte[] sig = Pkcs6.getCertificateSignature(certificates);
                                                        if (sig != null) {
                                                            // TODO: check trust of root cert
                                                        }
                                                        
                                                        
                                                        byte[] ownerBlock = Pkcs6.getCertificateOwner(certificates.get(0));
                                                        // System.out.println(Asn1.dump(0, ownerBlock, 0, ownerBlock.length, false));

                                                        byte[] domainOid = Asn1.makeASN1(Asn1.string2Oid("2.5.4.3"), Asn1.OBJECT_IDENTIFIER);
                                                        int domainSequenceOffset = Asn1.searchSequence(ownerBlock, domainOid);
                                                        
                                                        int stringOffset = domainSequenceOffset + Asn1.getHeaderLen(ownerBlock, domainSequenceOffset) + domainOid.length;
                                                        
                                                        String domain = new String(Asn1.getData(ownerBlock, stringOffset));
                                                        //System.out.println(domain);
                                                        
                                                        // TODO match server <-> domain
                                                        
                                                        is = client.getInputStream();
                                                        os = client.getOutputStream();
                                                        
                                                        // if we reach this point, STARTTLS succeeded
                                                        logFile.writeDate("STARTTLS to " + mailServerDomain + " == " + domain + " using " + client.getCipherSuite());
                                                        
                                                        // send a new EHLO
                                                        os.write(ehlo.getBytes());

                                                        line = readResponse(br, is);
                                                        cmd = line.substring(0, 3).toUpperCase();
                                                    }
                                                }
                                                // second compare - in case of STARTTLS
                                                if (cmd.equals(R250)) {
                                                    retryWithoutTLS = false;
                                                    
                                                    final String mailFrom = "MAIL FROM:<" + se.fromName + '@' + se.fromDomain + ">\r\n";
                                                    os.write(mailFrom.getBytes());

                                                    line = readResponse(br, is);
                                                    if (VERBOSE)
                                                        logFile.writeDate(mailServerDomain + "> " + line);

                                                    cmd = line.substring(0, 3).toUpperCase();
                                                    if (cmd.equals(R250)) {
                                                        final String rcptTo = "RCPT TO:<" + se.toName + '@' + se.toDomain + ">\r\n";
                                                        os.write(rcptTo.getBytes());

                                                        line = readResponse(br, is);
                                                        if (VERBOSE)
                                                            logFile.writeDate(mailServerDomain + ": " + line);

                                                        cmd = line.substring(0, 3).toUpperCase();
                                                        if (cmd.equals(R250)) {
                                                            error = E_RETRY;

                                                            os.write("DATA\r\n".getBytes());

                                                            line = readResponse(br, is);
                                                            if (VERBOSE)
                                                                logFile.writeDate(mailServerDomain + ": " + line);

                                                            cmd = line.substring(0, 3).toUpperCase();
                                                            if (cmd.equals(R354)) {
                                                                // finally send the file - escape leading dots with ..
                                                                final BufferedOutputStream bos = new BufferedOutputStream(
                                                                        os);
                                                                final ByteRef dat = new ByteRef();
                                                                dat.update(fis);
                                                                for (;;) {
                                                                    ByteRef lin = dat.nextLineCRLF();
                                                                    while (lin == null) {
                                                                        if (dat.update(fis) == null) {
                                                                            // handle last line without CRLF
                                                                            if (dat.length() > 0) {
                                                                                lin = dat.clone();
                                                                                dat.assign(EMPTY, 0, 0);
                                                                            }
                                                                            break;
                                                                        }
                                                                        lin = dat.nextLineCRLF();
                                                                    }
                                                                    if (lin == null)
                                                                        break;
                                                                    if (lin.charAt(0) == '.')
                                                                        bos.write('.');
                                                                    lin.writeTo(bos);
                                                                    bos.write(CRLF);
                                                                }

                                                                EOT.writeTo(bos);
                                                                bos.flush();

                                                                line = readResponse(br, is);
                                                                if (VERBOSE)
                                                                    logFile.writeDate(mailServerDomain + ": " + line);

                                                                cmd = line.substring(0, 3).toUpperCase();
                                                                if (cmd.equals(R250)) {
                                                                    error = E_SUCCESS;
                                                                } else {
                                                                    reason = line;
                                                                }
                                                                os.write("QUIT\r\n".getBytes());
                                                            } else
                                                                reason = line;
                                                        } else {
                                                            reason = line;
                                                            error = E_UNKNOWN_USER;
                                                        }
                                                    } else
                                                        reason = line;
                                                } else
                                                    reason = line;

                                            } finally {
                                                fis.close();
                                            }
                                        } catch (Throwable e2) {
                                            if (VERBOSE)
                                                logFile.writeDate(mailServerDomain + ": " + e2.getMessage());
                                            error = E_PERMANENT;
                                            // send an error message to sender                      
                                        }
                                    } else {
                                        error = E_RETRY;
                                        reason = line.substring(0, 3).append(" ").append(M_CONNECT_REJECT).append(line);
                                    }
                                } catch (Throwable e3) {
                                    if (VERBOSE)
                                        logFile.writeDate(mailServerDomain + ": " + e3.getMessage());
                                    if (DEBUG)
                                        logFile.writeDate(e3.getMessage());
                                    error = E_RETRY;
                                    if (line != null) {
                                        reason = line.substring(0, 3).append(" ").append(M_CONNECT_REJECT);
                                        reason = reason.append(line);
                                    } else {
                                        reason = new ByteRef("497 ").append(M_CONNECT_REJECT);
                                    }
                                } finally {
                                    socket.close();
                                }
                            }
                        }
                        //            while (error != 0 && mailAddr != null);

                        // remove from MX Cache
                        if (error != 0) {
                            MailCfg.dns.removeMx(se.toDomain);
                        }

                        if (mailServerDomain == null)
                            mailServerDomain = se.toDomain;

                        if (socket != null) {
                            try {
                                mailServerDomain += "(" + socket.getInetAddress().getHostAddress() + ")";
                            } catch (Exception ex) {
                            }
                        }
                    }
                    if (error == 0) {
                        logFile.writeDate("SUCCESS sent mail from " + se.fromName + "@" + se.fromDomain + " to "
                                + se.toName + "@" + se.toDomain + " using server " + mailServerDomain);

                        if (se.retry > 0)
                            sendMailBack(se, "Delivered your mail to: " + se.toName + '@' + se.toDomain,
                                    "notice: attempt #" + se.retry + " was finally successfull\r\n", R220);
                    } else
                        logFile.writeDate("FAILURE sending mail from " + se.fromName + "@" + se.fromDomain + " to "
                                + se.toName + "@" + se.toDomain + " using server " + mailServerDomain + ": " + reason);
                } catch (Throwable e) {
                    reason = M_INTERN;
                    logFile.writeDate("FAILURE sending mail from " + se.fromName + "@" + se.fromDomain + " to "
                            + se.toName + "@" + se.toDomain + " using server " + mailServerDomain + ": " + e.getMessage());
                    error = 99999;
                    e.printStackTrace();
                }

                // treat all 5xx errors as permanent!
                if (error != 0 && reason != null && reason.startsWith("5")) {
                    error = E_PERMANENT;
                }

                // send a reply
                if (error >= E_PERMANENT) {
                    if (VERBOSE)
                        logFile.writeDate("spooler: give up mail for: " + se.toName + '@' + se.toDomain);
                    sendMailBack(se, "Could not deliver your mail to: " + se.toName + '@' + se.toDomain,
                            "error: undeliverable mail", reason);
                } else
                // temporary error
                if (error > 0) {
                    if (se.retry == 0) {
                        if (VERBOSE)
                            logFile.writeDate("spooler: send notice for: " + se.toName + '@' + se.toDomain);
                        sendMailBack(se, "Can not yet deliver your mail to: " + se.toName + '@' + se.toDomain
                                + "\r\n\t doing more retries",
                                "notice: first attempt to send your mail failed, trying to resend", reason);
                    }

                    if (se.retry == spooler.INTERVAL_SWITCH) {
                        if (VERBOSE)
                            logFile.writeDate("spooler: send notice for: " + se.toName + '@' + se.toDomain);
                        sendMailBack(se, "Can not yet deliver your mail to: " + se.toName + '@' + se.toDomain
                                + "\r\n\t doing more retries",
                                "notice: your mail was not yet sent, still trying to resend", reason);
                    }

                    if ((se.retry > spooler.INTERVAL_SWITCH && error > 99) || se.retry > spooler.MAX_RETRIES) {
                        error = E_PERMANENT;
                    }
                    if (error > 0 && error < E_PERMANENT) {
                        spoolerRetry(se);

                        if (error > 99) // prevent deletion
                            error = 99;
                    }
                }

                // remove the spool entry
                if (error == 0 || error > 99) {
                    if (VERBOSE)
                        logFile.writeDate("spooler: erase " + se.mailId);
                    spoolerDeleteEntry(se);
                }
            } catch (Throwable t) {
            } finally {
                if (dbi != null)
                    cfg.releaseDbi(this, dbi);
                synchronized (cfg) {
                    --spooler.sendCount;
                    cfg.notify();
                }
            }
        }

        private ByteRef readResponse(ByteRef br, InputStream is) {
            ByteRef line;
            do {
                line = ByteRef.readLine(br, is);
                if (VERBOSE)
                    logFile.writeDate(line.toString());
            } while (line.charAt(3) != ' ');
            return line;
        }

        private void sendMailBack(SpoolEntry se, String theSubject, String theMsg, ByteRef reason) {
            // no notifications to postmasters
            if (// se.s_domain.equals(se.domain) && 
            "postmaster".equals(se.fromName))
                return;

            // no notifications from custom domains.
            if (!dbi.isLocalDomain(se.fromDomain))
                return;

            OutputStream fos = null;
            try {
                MailEntry me = dbi.createNewMail();
                fos = dbi.getOutputStream(me.mailId);

                String delimiter = "----=_NextPart_" + SessionManager.newKey() + '.' + me.mailId;

                String msg = "From: postmaster@"
                        + se.fromDomain
                        + "\r\nTo: "
                        + se.fromName
                        + '@'
                        + se.fromDomain
                        + "\r\nSubject: "
                        + theSubject
                        + "\r\nDate: "
                        + DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(System.currentTimeMillis())
                        + "\r\nMIME-Version: 1.0\r\nContent-Type: multipart/mixed;\r\n        boundary=\""
                        + delimiter
                        + "\"\r\nImportance: High\r\n\r\nThis is a multi-part message in MIME format.\r\n\r\n--"
                        + delimiter
                        + "\r\nContent-Type: text/plain;\r\n        charset=\"iso-8859-1\"\r\nContent-Transfer-Encoding: 7bit\r\n\r\n"
                        + theMsg
                        + "\r\nReason: "
                        + reason
                        + "\r\nOriginal message attached\r\n--"
                        + delimiter
                        + "\r\nContent-Type: message/rfc822\r\nContent-Transfer-Encoding: 7bit\r\nContent-Disposition: attachment\r\n\r\n";

                fos.write(msg.getBytes());

                // append original message
                InputStream fis = dbi.getInputStream(se.mailId);
                try {
                    byte data[] = new byte[8192];
                    for (int read = fis.read(data, 0, data.length); read > 0; read = fis.read(data, 0, data.length)) {
                        fos.write(data, 0, read);
                    }
                } catch (Exception e) {
                    logFile.writeDate("cant open:" + se.mailId);
                    e.printStackTrace();
                } finally {
                    fis.close();
                }
                String term = "--" + delimiter + "--\r\n\r\n";
                fos.write(term.getBytes());

                fos.flush();

                dbi.storeMail(me); // update internal values

                // no notifications for forwarded mails
                if (dbi.isForwarder(se.fromName, se.fromDomain, se.toName, se.toDomain)) {
                    // TODO: store it in senders folder instead
                    dbi.sendMail(me, "postmaster", se.fromDomain, se.fromName, se.fromDomain);
                    return;
                }

                if (VERBOSE)
                    logFile.writeDate("spooler: sent reply to: " + se.fromName + '@' + se.fromDomain);
                dbi.sendMail(me, "postmaster", se.fromDomain, se.fromName, se.fromDomain);

            } catch (Exception e) {
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
            }
        }

        /**
         * retry given mail
         * 
         * @param se
         * @throws Exception
         */
        private void spoolerRetry(SpoolEntry se) throws Exception {
            if (DEBUG)
                logFile.writeDate("spoolerRetry");

            long t;

            if (se.retry < spooler.INTERVAL_SWITCH) {
                t = 1000L * 60 * spooler.SHORT_INTERVAL;
            } else {
                t = 1000L * 60 * spooler.LONG_INTERVAL;
            }

            ++se.retry;

            dbi.updateSpool(t, se.retry, se.msgId);
        }

        /**
         * remove spooler Entry
         * 
         * @param se
         * @throws Exception
         */
        private void spoolerDeleteEntry(SpoolEntry se) throws Exception {
            if (DEBUG)
                logFile.writeDate("spoolerDeleteEntry");

            dbi.deleteFromSpool(se.msgId);
        }
    }
}

/******************************************************************************
 * $Log: Spooler.java,v $
 * Revision 1.50  2014/10/04 19:11:56  bebbo
 * @N sending mail is using STARTTLS if offered. There are no reasonable checks yet, since this would cause to much headache (thanks google)
 * Revision 1.49 2014/03/23 22:05:58 bebbo
 * 
 * @B fixed dot handling in incoming mails Revision 1.48 2013/11/29 09:35:06 bebbo
 * 
 * @R the spooler now retries infinitely to obtain a database connection.
 * 
 *    Revision 1.47 2013/05/22 12:47:14 bebbo
 * @I removed some superfluous checks Revision 1.46 2013/05/17 10:58:51 bebbo
 * 
 * @D cleanup for DEBUG messages
 * @B stop retrying different servers after 4xx response Revision 1.45 2013/03/15 21:45:57 bebbo
 * 
 * @I cleanup Revision 1.44 2012/08/13 21:09:45 bebbo
 * 
 * @B forwarding from postmasters did not work properly
 * @B only one message is sent, if the first send attempt fails permanent Revision 1.43 2012/07/18 09:11:32 bebbo
 * 
 * @I typified Revision 1.42 2012/05/17 07:23:36 bebbo
 * 
 * @B fixed a forwarding issues - sometimes the wrong sender was used and thus sending was blocked. Revision 1.41
 *    2011/04/06 21:11:30 bebbo
 * 
 * @D disabled DEBUG Revision 1.40 2010/07/02 17:59:33 bebbo
 * 
 * @B successful sending mails also sent a failure message... fixed
 * 
 *    Revision 1.39 2010/07/01 10:08:51 bebbo
 * @R Spooler does no retries after a 5xx message
 * 
 *    Revision 1.38 2010/04/10 12:07:46 bebbo
 * @B fixed a possible 100% load in spooler
 * 
 *    Revision 1.37 2006/05/09 08:52:02 bebbo
 * @B removed dependency from Java time - only DB time is used to detect next sendable mail
 * 
 *    Revision 1.36 2006/03/17 11:36:38 bebbo
 * @B fixed unclosed streams
 * 
 *    Revision 1.35 2005/12/31 15:42:40 bebbo
 * @I moved members into inner class (where those belong to)
 * 
 *    Revision 1.34 2005/11/30 07:11:16 bebbo
 * @B fixed retry message
 * 
 *    Revision 1.33 2004/12/16 16:14:35 bebbo
 * @R database connections are now shared
 * @N added support for multiple send threads
 * 
 *    Revision 1.32 2003/09/05 11:01:13 bebbo
 * @B prevent to send self notifications on error, e.g. if postmaster does not exist
 * 
 *    Revision 1.31 2003/08/04 09:30:41 bebbo
 * @R added fallback to socket creation, if localAddr is not really local, coz sender is no local mail user.
 * 
 *    Revision 1.30 2003/08/04 08:39:27 bebbo
 * @R spooler chooses appropriate IP from sender domain to send with
 * 
 *    Revision 1.29 2003/06/24 09:34:48 bebbo
 * @N added functionality to validate mail sending server and sender
 * 
 *    Revision 1.28 2003/06/23 15:20:29 bebbo
 * @R moved singletons for spooler and cleanup threads to MailCfg
 * 
 *    Revision 1.27 2003/06/17 10:20:17 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.26 2003/02/05 08:09:46 bebbo
 * @B better address handling
 * @N usage of VERBOSE attribute from config
 * 
 *    Revision 1.25 2003/01/07 18:32:27 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.24 2002/12/16 19:55:42 bebbo
 * @B tracked some unclosed Statements / Connections down and fixed them
 * 
 *    Revision 1.23 2002/12/16 16:40:53 bebbo
 * @I added sync for db implementation to release resources
 * 
 *    Revision 1.22 2002/11/19 18:11:29 bebbo
 * @B 5xx errors are treated as permanent errors
 * 
 *    Revision 1.21 2002/11/19 17:54:42 bebbo
 * @D added more debugging infos
 * 
 *    Revision 1.20 2002/11/19 12:34:09 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.19 2002/07/16 11:04:25 bebbo
 * @I on error MX entries are removed from cache
 * 
 *    Revision 1.18 2002/06/04 17:00:43 bebbo
 * @B new version is now tested and works
 * 
 *    Revision 1.17 2002/06/04 12:41:44 bebbo
 * @B fix for: on temporary errors (4xx) now all available mx gateways are tried.
 * 
 *    Revision 1.16 2002/06/04 12:10:32 bebbo
 * @R on temporary errors now all available mx gateways are tried.
 * 
 *    Revision 1.15 2002/01/27 21:31:30 bebbo
 * @L changed logging
 * 
 *    Revision 1.14 2002/01/20 19:10:28 franke
 * @B using Timestamp to query time
 * 
 *    Revision 1.13 2002/01/20 15:51:27 franke
 * @R the messages are stored without dot!
 * 
 *    Revision 1.12 2002/01/20 12:01:45 franke
 * @D dunping stacktrace on exception
 * 
 *    Revision 1.11 2002/01/19 15:49:47 franke
 * @R 2nd working IMAP implementation and many changes in design due to that
 * 
 *    Revision 1.10 2001/12/06 14:50:43 bebbo
 * @B fixed an endless loop, when an entry without DNS MX entry occured. A Socket Exception was not caught
 * 
 *    Revision 1.9 2001/11/26 14:04:49 bebbo
 * @N if there is no mail server in the DNS entry, the domain is directly used
 * 
 *    Revision 1.8 2001/08/24 08:24:41 bebbo
 * @I changes due to renamed functions in ByteRef - same names as in String class
 * 
 *    Revision 1.7 2001/08/14 16:09:36 bebbo
 * @B better wakeup mechanism
 * 
 *    Revision 1.6 2001/07/13 13:14:28 bebbo
 * @B corrected messages
 * @I doing 5 retries to resolve MX
 * 
 *    Revision 1.5 2001/05/14 21:40:00 bebbo
 * @I sending first warning after first error
 * @B caught an exception
 * 
 *    Revision 1.4 2001/04/11 16:31:51 bebbo
 * @R using all mail relays
 * @N send more early some information
 * 
 *    Revision 1.3 2001/02/25 17:07:09 bebbo
 * @B better handling of error message
 * @I original mail is now an attachment
 * 
 *    Revision 1.2 2001/02/20 19:13:09 bebbo
 * @D added DEBUG messages
 * @B appended new file -> fixed
 * 
 *    Revision 1.1 2001/02/19 19:56:15 bebbo
 * @R new or moved from package smtp or pop3
 * 
 *    Revision 1.9 2001/02/12 12:51:41 bebbo
 * @B fatal errors are sending status back (was broken)
 * 
 *    Revision 1.8 2001/01/15 16:38:19 bebbo
 * @B fixed mail header
 * @B fixed storage of broken files - no longer added to user accounts
 * @I changed interval for retries
 * @N append mail to error reply
 * 
 *    Revision 1.7 2001/01/01 17:40:37 bebbo
 * @D added VERBOSE messages
 * @B fixed spooling of mails > 8K
 * 
 *    Revision 1.6 2001/01/01 01:28:32 bebbo
 * @R default user is now postmaster (was mailserver)
 * 
 *    Revision 1.5 2000/12/30 10:17:07 bebbo
 * @I added verbose messages
 * @I restarting when connection to db broke
 * 
 *    Revision 1.4 2000/12/30 09:05:15 bebbo
 * @I now running as a daemon
 * 
 *    Revision 1.3 2000/12/29 17:50:29 bebbo
 * @I added cleanup for unused spool files
 * 
 *    Revision 1.2 2000/12/28 21:04:26 bebbo
 * @D DEBUG cleanup
 * 
 *    Revision 1.1 2000/12/28 20:54:42 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *****************************************************************************/
