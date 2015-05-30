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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import de.bb.util.Pair;
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

    private final static int E_SUCCESS = 0, E_RETRY = 1, E_PERMANENT = 1000, E_INVALID_DOMAIN = 101, E_UNKNOWN_USER = 102;

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
        MailDBI dbi = obtainDbi();
        
        fixSpoolerTable(dbi);
        
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
     * ensure that there are no hanging spooled mails, due to the aborted old process.
     * @param dbi
     */
    private void fixSpoolerTable(MailDBI dbi) {
        // restart all pending entries
        try {
            dbi.resetSpoolEntries();
        } catch (SQLException e) {
        }
    }

    private MailDBI obtainDbi() {
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
        return dbi;
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

        private final static ByteRef R220 = new ByteRef("220"), R250 = new ByteRef("250"), R354 = new ByteRef("354"), EOT = new ByteRef(".\r\n"),
                M_GIVEUP = new ByteRef("599 maximum retrycount reached - destination still not reachable\r\n"), M_INTERN = new ByteRef(
                        "598 general network error"), M_CONNECT_REJECT = new ByteRef("access denied: ");

        // private byte data[] = new byte[8192];

        private MailCfg cfg;

        private LogFile logFile;

        private Spooler spooler;

        private MailDBI dbi;

        private SpoolEntry se;

        private ByteRef line;

        private int error;

        private ByteRef reason;

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

                line = new ByteRef();
                reason = M_GIVEUP;
                error = E_INVALID_DOMAIN;
                
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
                        for (int pass = 0; error != 0 && pass < 2; ++pass) {
                            boolean retryWithoutTLS = pass == 1;

                            for (Iterator<String> i = mailServer.iterator(); error != 0 && i.hasNext();) {

                                // try the next mail server
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
                                                        // start TLS unless it's
                                                        // a retry
                                                        if (startTls && !retryWithoutTLS) {
                                                            Pair<InputStream, OutputStream> p = doStartTls(br, is, os, mailServerDomain, ehlo);
                                                            is = p.getFirst();
                                                            os = p.getSecond();

                                                            // send a new EHLO
                                                            os.write(ehlo.getBytes());
                                                            line = readResponse(br, is);
                                                            cmd = line.substring(0, 3).toUpperCase();
                                                        }
                                                    }
                                                    // second compare - in case
                                                    // of STARTTLS
                                                    if (cmd.equals(R250)) {
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
                                                                    // finally send the file -
                                                                    copyMail(fis, os);

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
                                                // send an error message to
                                                // sender
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
                            // while (error != 0 && mailAddr != null);

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
                            logFile.writeDate("SUCCESS sent mail from " + se.fromName + "@" + se.fromDomain + " to " + se.toName + "@" + se.toDomain
                                    + " using server " + mailServerDomain);

                            if (se.retry > 0)
                                sendMailBack(se, "Delivered your mail to: " + se.toName + '@' + se.toDomain, "notice: attempt #" + se.retry
                                        + " was finally successfull\r\n", R220);
                        } else
                            logFile.writeDate("FAILURE sending mail from " + se.fromName + "@" + se.fromDomain + " to " + se.toName + "@" + se.toDomain
                                    + " using server " + mailServerDomain + ": " + reason);
                    }
                } catch (Throwable e) {
                    reason = M_INTERN;
                    logFile.writeDate("FAILURE sending mail from " + se.fromName + "@" + se.fromDomain + " to " + se.toName + "@" + se.toDomain
                            + " using server " + mailServerDomain + ": " + e.getMessage());
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
                    sendMailBack(se, "Could not deliver your mail to: " + se.toName + '@' + se.toDomain, "error: undeliverable mail", reason);
                } else
                // temporary error
                if (error > 0) {
                    if (se.retry == 0) {
                        if (VERBOSE)
                            logFile.writeDate("spooler: send notice for: " + se.toName + '@' + se.toDomain);
                        sendMailBack(se, "Can not yet deliver your mail to: " + se.toName + '@' + se.toDomain + "\r\n\t doing more retries",
                                "notice: first attempt to send your mail failed, trying to resend", reason);
                    }

                    if (se.retry == spooler.INTERVAL_SWITCH) {
                        if (VERBOSE)
                            logFile.writeDate("spooler: send notice for: " + se.toName + '@' + se.toDomain);
                        sendMailBack(se, "Can not yet deliver your mail to: " + se.toName + '@' + se.toDomain + "\r\n\t doing more retries",
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

        private void copyMail(InputStream fis, OutputStream os) throws IOException {
            // escape leading dots with ..
            final BufferedOutputStream bos = new BufferedOutputStream(os);
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
        }

        private Pair<InputStream, OutputStream> doStartTls(ByteRef br, InputStream is, OutputStream os, String mailServerDomain, String ehlo) throws IOException {
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
            // System.out.println(Asn1.dump(0,
            // root, 0,
            // root.length,
            // false));

            byte[] issuer = Pkcs6.getCertificateIssuer(root);
            // System.out.println(Asn1.dump(0,
            // issuer, 0,
            // issuer.length,
            // false));

            byte[] sig = Pkcs6.getCertificateSignature(certificates);
            if (sig != null) {
                // TODO: check
                // trust of root
                // cert
            }

            byte[] ownerBlock = Pkcs6.getCertificateOwner(certificates.get(0));
            // System.out.println(Asn1.dump(0,
            // ownerBlock, 0,
            // ownerBlock.length,
            // false));

            byte[] domainOid = Asn1.makeASN1(Asn1.string2Oid("2.5.4.3"), Asn1.OBJECT_IDENTIFIER);
            int domainSequenceOffset = Asn1.searchSequence(ownerBlock, domainOid);

            int stringOffset = domainSequenceOffset + Asn1.getHeaderLen(ownerBlock, domainSequenceOffset) + domainOid.length;

            String domain = new String(Asn1.getData(ownerBlock, stringOffset));
            // System.out.println(domain);

            // if we reach this
            // point, STARTTLS
            // succeeded
            logFile.writeDate("STARTTLS to " + mailServerDomain + " == " + domain + " using " + client.getCipherSuite());

            // TODO match server
            // <-> domain

            is = client.getInputStream();
            os = client.getOutputStream();

            return Pair.makePair(is, os);
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

                String msg = "From: postmaster@" + se.fromDomain + "\r\nTo: " + se.fromName + '@' + se.fromDomain + "\r\nSubject: " + theSubject + "\r\nDate: "
                        + DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(System.currentTimeMillis())
                        + "\r\nMIME-Version: 1.0\r\nContent-Type: multipart/mixed;\r\n        boundary=\"" + delimiter
                        + "\"\r\nImportance: High\r\n\r\nThis is a multi-part message in MIME format.\r\n\r\n--" + delimiter
                        + "\r\nContent-Type: text/plain;\r\n        charset=\"iso-8859-1\"\r\nContent-Transfer-Encoding: 7bit\r\n\r\n" + theMsg
                        + "\r\nReason: " + reason + "\r\nOriginal message attached\r\n--" + delimiter
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
