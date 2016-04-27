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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.ArrayList;

import de.bb.util.LogFile;
import de.bb.util.XmlFile;

/**
 * This class is designed to ...
 * 
 * @author bebbo
 */
public class ImportSendmail {
    private static String path;

    private static String domain;

    //  private static String defaultSender;

    private final static String no;

    private final static String version;
    static {
        String s = "$Revision: 1.4 $";
        String sub = s.substring(11, s.length() - 1);
        //    int dot = sub.indexOf('.');
        no = "1.1." + sub;
        version = "importSendmail V" + no + " (c) 2005 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
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

    private static String sysMessage =
            "USAGE: de.bb.bejy.mail.ImportSendmail [-c<configfile>=bejy.xml] [-d<domainname>] [p<path_to_mailfiles>=/var/mail]\r\n";

    private static String configFile = "bejy.xml";

    private static LogFile logFile;

    private static MailDBI mDbi;

    //  private static byte[] buffer = new byte[8192];

    private static String logFileName = "importSendmail";

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            parseParams(args);

            // create a log and a config
            logFile = new LogFile(logFileName);
            XmlFile config = new XmlFile();
            config.readFile(configFile);

            String dName = config.getString("/bejy/mail", "jdbcDriver", null);
            try {
                Class<?> clazz = Class.forName(dName);
                clazz.newInstance();
            } catch (Exception e) {
                throw new Exception("cannot load jdbcDriver: " + dName);
            }
            domain = config.getString("/bejy/mail", "mainDomain", null);
            //      defaultSender = "postmaster" + '@' + domain;

            String iName = config.getString("/bejy/mail", "mailDbi", null);
            try {
                Class<?> clazz = Class.forName(iName);
                mDbi = (MailDBI) clazz.newInstance();
            } catch (Exception e) {
                throw new Exception("cannot load mailDbi: " + iName);
            }

            // create the required mailDBI 
            String jdbcUrl = config.getString("/bejy/mail", "jdbcUrl", null);
            String mailFolder = config.getString("/bejy/mail", "mailFolder", null);
            mDbi.setMailPath(mailFolder);
            mDbi.setJdbcUrl(jdbcUrl);
            mDbi.checkConnection();

            File files[] = new File(path).listFiles();

            for (int i = 0; i < files.length; ++i) {
                try {
                    FileInputStream fis = new FileInputStream(files[i]);
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader br = new BufferedReader(isr);

                    String line0 = br.readLine();
                    if (line0 == null || !line0.startsWith("From ")) {
                        logFile.writeDate("ERROR: " + files[i] + " does not start with 'From '");
                        continue;
                    }

                    ArrayList<String> recipients = new ArrayList<String>();
                    String user = files[i].getName() + "@" + domain;
                    recipients.add(user);

                    int l = user.indexOf('@');
                    String ar = user.substring(0, l);
                    String cpt = user.substring(l + 1);
                    ResultSet rs = mDbi.selectFromMailUser(ar, cpt);
                    boolean exi = rs.next();
                    if (!exi) {
                        rs.close();
                        mDbi.createPoBox("admin", ar, cpt);
                        logFile.writeDate("creating mail user: " + user);
                        rs = mDbi.selectFromMailUser(ar, cpt);
                        exi = rs.next();
                    }
                    rs.close();

                    if (!exi) {
                        logFile.writeDate("ERROR: PO box" + user + " does not exist and can't be created");
                        continue;
                    }

                    for (;;) {
                        MailEntry me = mDbi.createNewMail();
                        OutputStream os = mDbi.getOutputStream(me.mailId);
                        try {
                            String line = null;
                            String fromUser = "postmaster";
                            String fromDomain = domain;
                            boolean needsFrom = true;
                            for (;;) {
                                line = br.readLine();
                                if (line == null || line.equals(line0))
                                    break;
                                os.write(line.getBytes());
                                os.write(0xd);
                                os.write(0xa);

                                if (needsFrom) {
                                    String lower = line.toLowerCase();
                                    if (lower.startsWith("from:")) {
                                        needsFrom = false;
                                        int bra = lower.lastIndexOf('<');
                                        int ket = lower.lastIndexOf('>');
                                        if (ket > bra && bra > 0) {
                                            String from = lower.substring(bra + 1, ket);
                                            int at = from.indexOf('@');
                                            if (at > 0) {
                                                fromUser = from.substring(0, at);
                                                fromDomain = from.substring(at + 1);
                                            }
                                        }
                                    }
                                }

                            }
                            os.flush();
                            mDbi.storeMail(me);
                            mDbi.sendMultiMail(me, fromUser, fromDomain, recipients);
                            if (line == null)
                                break;
                        } finally {
                            os.close();
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method parseParams.
     * 
     * @param args
     * @throws Exception
     */
    private static void parseParams(String[] args) throws Exception {
        // parse options
        for (int i = 0; i < args.length; ++i) {
            String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2 || "?cdp".indexOf(a.charAt(1)) == -1)
                    throw new Exception("invalid argument: " + a + "\r\n\r\n" + sysMessage);

                String aa = a.substring(2);
                int ch = a.charAt(1);
                switch (ch) {
                    case '?':
                        throw new Exception(sysMessage);
                    case 'c':
                        configFile = aa;
                        break;
                    case 'p':
                        path = aa;
                        break;
                    case 'd':
                        domain = aa;
                        break;
                }
                continue;
            }
        }
    }
}
