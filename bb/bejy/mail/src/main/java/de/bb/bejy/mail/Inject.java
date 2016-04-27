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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.bb.util.ByteRef;
import de.bb.util.DateFormat;
import de.bb.util.LogFile;
import de.bb.util.MimeFile;
import de.bb.util.MimeFile.Info;
import de.bb.util.XmlFile;

/**
 * This class is designed to ...
 * 
 * @author bebbo
 */
public class Inject {
    private static final ByteRef NTO = new ByteRef("TO:");

    private static final ByteRef NCC = new ByteRef("CC:");

    private static final ByteRef NBCC = new ByteRef("BCC");

    private static final ByteRef NFROM = new ByteRef("FROM:");

    private static ByteRef defaultSender;

    private final static String no;

    private final static String version;
    static {
        String s = "$Revision: 1.11 $";
        String sub = s.substring(11, s.length() - 1);
        // int dot = sub.indexOf('.');
        no = "1.1." + sub;
        version = "Inject V" + no + " (c) 2000-2015 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
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

    private static String sysMessage = "USAGE: java de.bb.bejy.mail.Inject [-i] [-C<configfile>] [-F<mailFile>+] [-L<logfile>] [-S<defaultSender>] [-f<from>] [<to>]\r\n"
            + "if -F is not used, the mail content is read from stdin\r\n"
            + "defaultSender is used, if mail contains no from value\r\n"
            + "-i dummy\r\n" + "-f specify the from\r\n";

    private static String configFile = "bejy.xml";

    private static LinkedList<String> files = new LinkedList<String>();

    private static LogFile logFile;

    private static MailDBI mDbi;

    private static byte[] buffer = new byte[8192];

    private static String logFileName = "bejyMail";

    private static boolean escapeDots;

    private static ByteRef definedFrom;

    /**
     * @param args
     */
    public static void main(String[] args) {
        logFile = new LogFile(logFileName);
        try {
            // create a log and a config
            ArrayList<String> tos = parseParams(args);

            XmlFile config = new XmlFile();
            config.readFile(configFile);

            String dName = config.getString("/bejy/mail", "jdbcDriver", null);
            try {
                Class<?> clazz = Class.forName(dName);
                clazz.newInstance();
            } catch (Exception e) {
                throw new Exception("cannot load jdbcDriver: " + dName);
            }
            if (defaultSender == null) {
                String ds = config.getString("/bejy/mail", "mainDomain", null);
                if (ds != null)
                    defaultSender = new ByteRef("postmaster" + '@' + ds);
            }
            String iName = config.getString("/bejy/mail", "mailDbi", null);
            try {
                Class<?> clazz = Class.forName(iName);
                mDbi = (MailDBI) clazz.newInstance();
            } catch (Exception e) {
                throw new Exception("cannot load mailDbi: " + iName);
            }

            // create the required mailDBI
            String jdbcUrl = config.getString("/bejy/mail", "jdbcUrl", null);
            String mailFolder = config.getString("/bejy/mail", "mailFolder", "mail");
            mDbi.setMailPath(mailFolder);
            mDbi.setJdbcUrl(jdbcUrl);
            mDbi.checkConnection();

            if (files.size() > 0) {
                for (Iterator<String> i = files.iterator(); i.hasNext();) {
                    try {
                        sendMail(i.next(), tos);
                    } catch (Exception e) {
                        logFile.writeDate(e.getMessage());
                    }
                }
            } else {
                File f;
                do {
                    f = new File(mailFolder, "bejySend" + System.currentTimeMillis());
                } while (f.exists());
                logFile.writeDate("temp mail file: " + f.getCanonicalPath());
                FileOutputStream fos = new FileOutputStream(f);
                BufferedReader ir = new BufferedReader(new InputStreamReader(System.in));
                try {
                    for (;;) {
                        String line;
                        line = ir.readLine();
                        if (line == null || ".".equals(line)) {
                            break;
                        }
                        fos.write(line.getBytes());
                        fos.write(13);
                        fos.write(10);
                    }
                } catch (Throwable ex) // handle EOS
                {
                    fos.write(".\r\n".getBytes());
                }
                fos.close();
                sendMail(f.getCanonicalPath(), tos);
                f.delete();
            }

        } catch (Exception ex) {
            logFile.writeDate(ex.getMessage());
            logFile.flush();
            ex.printStackTrace();
            System.exit(1);
        } finally {
            logFile.flush();
        }
    }

    /**
     * Method sendMail.
     * 
     * @param inFile
     * @throws Exception
     */
    private static void sendMail(String inFile, ArrayList<String> givenTos) throws Exception {
        logFile.writeDate("scanning file: " + inFile);
        FileInputStream fis = new FileInputStream(inFile);
        ArrayList<Info> segments = null;
        try {
            segments = MimeFile.parseMime(fis);
        } finally {
            fis.close();
        }

        String senderDomain = null;
        String senderName = null;
        MimeFile.Info info = segments.get(0);

        // read the complete header
        fis = new FileInputStream(inFile);
        byte b[] = new byte[info.bBegin];
        try {
            fis.read(b);
        } finally {
            fis.close();
        }

        // analyze header
        ByteRef br = new ByteRef(b);
        ByteRef to = new ByteRef();
        ByteRef from = new ByteRef();
        int collect = 0;
        boolean collectBcc = false;
        ByteRef postBcc = null;
        for (ByteRef line = br.nextLine(); line != null; line = br.nextLine()) {
            if (line.length() == 0)
                continue;
            if (line.charAt(0) <= 32) {
                if (collect == 1) {
                    to = to.append(line);
                } else if (collect == 2) {
                    from = from.append(line);
                }
                continue;
            }
            if (collectBcc) {
                postBcc = line; // (ByteRef)line.clone();
                collectBcc = false;
            }
            if (line.length() < 3)
                continue;

            collect = 0;
            ByteRef start = line.substring(0, 3).toUpperCase();
            if (start.equals(NBCC) || start.equals(NCC) || start.equals(NTO)) {
                collect = 1;
                int idx = 3;
                if (start.equals(NBCC)) {
                    if (line.charAt(3) != ':')
                        continue;
                    if (postBcc != null) {
                        throw new Exception("cannot handle multiple BCC: entries - mail " + inFile + " not sent!");
                    }
                    collectBcc = true;
                    ++idx;
                }
                // System.out.println(line.substring(0, idx).toString());
                to = to.append(line.substring(idx));
                continue;
            }

            start = line.substring(0, 5).toUpperCase();
            if (start.equals(NFROM)) {
                collect = 2;
                from = from.append(line.substring(5));
            }
        }

        boolean addFrom = false;
        // parse from:
        if (from == null || from.trim().length() == 0) {
            from = defaultSender;
            addFrom = true;
        }

        if (definedFrom != null) {
            from = definedFrom;
            addFrom = true;
        }

        if (from != null) {
            ByteRef ud = from;
            int j = from.indexOf('<');
            if (j >= 0) {
                int k = from.indexOf('>', j);
                ud = from.substring(j + 1, k);
            }
            int k = ud.indexOf('@');
            if (k >= 0) {
                senderName = ud.substring(0, k).trim().toString();
                senderDomain = ud.substring(k + 1).trim().toString();
            }
        }

        if (senderName == null) {
            throw new Exception("no 'from:' found, mail " + inFile + " not sent!");
        }

        // if(!mDbi.isLocalUser(senderName, senderDomain))
        {
            // throw new Exception("from: <" + senderName + "@" + senderDomain +
            // "> is no local user, mail " + inFile + " not sent!");
        }

        ArrayList<String> rcpt = new ArrayList<String>();
        List<ByteRef> ll = Imap.parseMailAddresses(to);

        for (String dt : givenTos) {
            ll.add(new ByteRef(dt));
        }

        // parse from:
        for (Iterator<ByteRef> i = ll.iterator(); i.hasNext();) {
            ByteRef ud = i.next();
            if (ud.charAt(ud.length() - 1) == '>') {
                int gt = ud.lastIndexOf('<');
                ud = ud.substring(gt + 1, ud.length() - 1).trim();
            }
            int l = ud.indexOf('@');
            if (l >= 0) {
                rcpt.add(ud.toString());
            }
        }

        if (rcpt.size() == 0)
            throw new Exception("no recipient found, mail " + inFile + " not sent!");

        // create a new MailEntry
        MailEntry me = mDbi.createNewMail();

        // get outputstream and write complete mail file - with header and so on
        OutputStream os = null;
        fis = null;
        try {
            os = mDbi.getOutputStream(me.getMailId());
            fis = new FileInputStream(inFile);

            // add header
            String date = DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(System.currentTimeMillis());
            // insert a header
            String h = "Received: from " + senderName + "@" + senderDomain + " (from command line)\r\n\tby bejy inject (V" + getVersion() + ") with SMTP id "
                    + me.mailId + "\r\n\tfor " + rcpt.get(0) + ";\r\n\t" + date + "\r\n";
            os.write(h.getBytes());

            // remove BCC:
            if (postBcc != null) {
                ByteRef header = new ByteRef(b);
                int offset = header.indexOf("BCC:");
                fis.read(b, 0, offset);
                os.write(b, 0, offset);
                int post = header.indexOf(postBcc, offset);
                fis.skip(post - offset);
            }

            if (ll.size() == givenTos.size()) {
                // add to
                os.write(("To: " + givenTos.iterator().next() + "\r\n").getBytes());
                if (givenTos.size() > 1) {
                    givenTos.remove(0);
                    String cc = givenTos.toString();
                    os.write(("Cc: " + cc.substring(1, cc.length() - 1) + "\r\n").getBytes());
                }
            }
            
            // add from if missing
            if (addFrom)
                os.write(("From: " + senderName + "@" + senderDomain + "\r\n").getBytes());

            if (h.toUpperCase().indexOf("DATE:") < 0) {
                os.write(("Date: " + date + "\r\n").getBytes());
            }

            // copy mail
            for (int len = fis.read(buffer, 0, 8192); len > 0; len = fis.read(buffer, 0, 8192)) {
                os.write(buffer, 0, len);
            }
        } finally {
            if (fis != null)
                fis.close();
            if (os != null)
                os.close();
        }

        // store the mail
        mDbi.storeMail(me);

        logFile.writeDate("sending mail from: " + senderName + "@" + senderDomain + " to: " + rcpt);

        // and send it out
        mDbi.sendMultiMail(me, senderName, senderDomain, rcpt);
        logFile.writeDate("SUCCESS: sent mail from: " + senderName + "@" + senderDomain + " to: " + rcpt);
    }

    /**
     * Method parseParams.
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private static ArrayList<String> parseParams(String[] args) throws Exception {
        // parse options
        ArrayList<String> res = new ArrayList<String>();
        for (int i = 0; i < args.length; ++i) {
            String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2 || "?ifCLFS".indexOf(a.charAt(1)) == -1)
                    throw new Exception("invalid argument: " + a + "\r\n\r\n" + sysMessage);

                int ch = a.charAt(1);
                switch (ch) {
                // options without parameter
                case '?':
                    throw new Exception(sysMessage);
                case 'i':
                    escapeDots = true;
                    continue;
                }

                String aa = a.substring(2);
                if (aa.length() == 0 && i + 1 < args.length) {
                    aa = args[++i];
                }
                switch (ch) {
                case 'C':
                    configFile = aa;
                    break;
                case 'L':
                    logFileName = aa;
                    break;
                case 'F':
                    files.add(aa);
                    break;
                case 'S':
                    defaultSender = new ByteRef(aa);
                    break;
                case 'f':
                    definedFrom = new ByteRef(aa);
                    break;
                }
                continue;
            }

            res.add(a);
        }

        return res;
    }

}
