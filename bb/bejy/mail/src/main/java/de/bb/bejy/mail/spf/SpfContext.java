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

package de.bb.bejy.mail.spf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import java.util.StringTokenizer;

import de.bb.bejy.Config;
import de.bb.bejy.Dns;
import de.bb.bex2.Context;
import de.bb.bex2.ParseException;
import de.bb.bex2.Scanner;
import de.bb.log.Logger;

/**
 * @author sfranke
 */
public class SpfContext extends Context {
	
	private final static Logger LOG = Logger.getLogger(SpfContext.class);
	
    private StringBuffer macro;

    private Scanner scanner;

    private int start;

    private String mask, name, transform, delimiter;

    private boolean reverse, left;

    private int prefix, alpha;

    private String sender, senderName, senderDomain, toDomain, clientIp, clientDomain, clientHelo, currentDomain;

    private int clientIpInt;

    private HashMap<String, String> modifiers = new HashMap<String, String>();

    private Stack<Object[]> stack = new Stack<Object[]>();

    private HashSet<String> recurse = new HashSet<String>();

    private SpfParser parser;

    private String message;

    /**
     * @param fromEmail
     * @param toDomain
     * @param clientIp
     * @param clientDomain
     * @param clientHelo
     * @throws SpfException
     */
    public SpfContext(String fromEmail, String toDomain, String clientIp, String clientDomain, String clientHelo)
            throws SpfException {
        this.toDomain = toDomain;
        if (toDomain == null)
            toDomain = "localhost";

        this.clientIp = clientIp;
        if (clientIp == null)
            throw new SpfException('-');

        if (clientHelo == null)
            throw new SpfException('-');
        this.clientHelo = clientHelo;

        // sender uses <> --> create a postmaster thing from clientDomain
        if (fromEmail == null || fromEmail.length() == 0) {
            if (clientDomain == null)
                throw new SpfException('-');
            fromEmail = "postmaster@" + clientDomain;
        }
        int at = fromEmail.indexOf('@');
        if (at < 0)
            throw new SpfException('-');

        this.sender = fromEmail;
        this.senderName = fromEmail.substring(0, at);
        this.currentDomain = this.senderDomain = fromEmail.substring(at + 1);

        if (clientDomain == null)
            clientDomain = "";
        this.clientDomain = clientDomain;

        clientIpInt = ip2Int(clientIp);

        parser = new SpfParser(null, this);

        reset();
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.bb.bex2.Context#notify(int)
     */
    public boolean notify(int arg0) throws ParseException {
        switch (arg0) {
        case SpfParser.ID_VERSION:
            prefix = '+';
            break;
        case SpfParser.ID_MACRO:
            reverse = false;
            left = false;
            macro = new StringBuffer();
            break;
        case SpfParser.ID_MINUS:
            macro.append("%20");
            break;
        case SpfParser.ID_PERCENT:
            macro.append('%');
            break;
        case SpfParser.ID_UNDERSCORE:
            macro.append(' ');
            break;
        case SpfParser.ID_VCHAR:
            macro.append(scanner.getToScanPos(scanner.getPosition(-1)));
            break;
        case SpfParser.ID_NAME:
        case SpfParser.ID_MASK:
        case SpfParser.ID_DELIMITER:
            start = scanner.getPosition();
            break;
        case SpfParser.ID_MASK_END:
            mask = scanner.getToScanPos(start);
            break;
        case SpfParser.ID_NAME_END:
            name = scanner.getToScanPos(start);
            break;

        case SpfParser.ID_PREFIX:
            prefix = scanner.charAt(scanner.getPosition() - 1);
            //        if (prefix != '+')
            //          prefix = '-';
            break;
        case SpfParser.ID_REVERSE:
            reverse = true;
            break;
        case SpfParser.ID_LEFT:
            left = true;
            break;
        case SpfParser.ID_TRANSFORM:
            start = scanner.getPosition();
            alpha = scanner.charAt(start - 1);
            break;
        case SpfParser.ID_TRANSFORM_VAL:
            transform = scanner.getToScanPos(start);
            break;
        case SpfParser.ID_TRANSFORM_END:
            delimiter = scanner.getToScanPos(start);
            transform();
            break;

        case SpfParser.ID_MECHANISM:
            mechanism();
            reset();
            break;
        case SpfParser.ID_MODIFIER:
            if (modifiers.get(name) != null)
                throw new SpfException('e');
            modifiers.put(name, macro.toString());
            reset();
            break;
        }
        return true;
    }

    /**
   *
   */
    private void reset() {
        mask = null;
        macro = null;
        prefix = '+';
    }

    /**
     * @throws ParseException
     */
    private void mechanism() throws ParseException {
        try {
            if ("all".equalsIgnoreCase(name)) {
                if (prefix == '+') // disallow +all --> convert it into '?'
                    prefix = '?';
                throw new SpfException(prefix);
            }

            if ("include".equalsIgnoreCase(name)) {
                push(macro.toString());
                try {
                    validateSpf();
                } catch (SpfException me) {
                    // ignore fail and pass
                    if (me.getPrefix() == '+')
                        throw me;
                }
                pop();
                return;
            }

            if ("a".equalsIgnoreCase(name)) {
                String targetName = this.senderDomain;
                if (macro != null)
                    targetName = macro.toString();
                int bits = 32;
                if (mask != null) {
                    bits = Integer.parseInt(mask);
                }

                match(getDns().getIpsFromDomain(targetName), bits);

                // additional check: use the resolved client domain name to compare
                if (bits == 32 && targetName.equals(clientDomain))
                    throw new SpfException(prefix);

                return;
            }

            if ("mx".equalsIgnoreCase(name)) {
                String targetName = this.senderDomain;
                if (macro != null)
                    targetName = macro.toString();
                int bits = 32;
                if (mask != null) {
                    bits = Integer.parseInt(mask);
                }

                match(getDns().getMxIps(targetName), bits);
                return;
            }

            if ("ptr".equalsIgnoreCase(name)) {
                ArrayList<String> validated = new ArrayList<String>();
                for (Iterator<String> i = getDns().getDomainsFromIp(clientIp); i.hasNext();) {
                    String n = i.next();
                    for (Iterator<String> j = getDns().getIpsFromDomain(n); j.hasNext();) {
                        if (clientIp.equals(j.next())) {
                            validated.add(n);
                            break;
                        }
                    }
                }

                String domain = senderDomain;
                if (macro != null)
                    domain = macro.toString();

                for (Iterator<String> i = validated.iterator(); i.hasNext();) {
                    String d = i.next();
                    if (d.endsWith(domain))
                        throw new SpfException(prefix);
                }
                return;
            }

            if ("ip4".equalsIgnoreCase(name)) {
                int bits = 32;
                if (mask != null) {
                    bits = Integer.parseInt(mask);
                }

                int ip = ip2Int(macro.toString());
                if (ip == -1)
                	return;
                
                bits = 32 - bits;
                if (ip >>> bits == clientIpInt >>> bits)
                    throw new SpfException(prefix);

                return;
            }

            if ("ip6".equalsIgnoreCase(name)) {
                // ignore silently
                return;
            }

            if ("exists".equalsIgnoreCase(name)) {
                Iterator<String> i = getDns().getIpsFromDomain(macro.toString());
                if (i.hasNext())
                    throw new SpfException(prefix);
            }
        } catch (RuntimeException re) {
            // ignore.
        }
        LOG.debug("mechanism: " + name + " " + macro + " " + mask);
    }

    /**
     * @param ips
     * @param bits
     * @throws SpfException
     */
    private void match(Iterator<String> i, int bits) throws SpfException {
        bits = 32 - bits;
        for (; i.hasNext();) {
            String ip = i.next();
            int iip = ip2Int(ip);
            if (iip < 0) // invalid String
            	continue;
            if (iip >>> bits == clientIpInt >>> bits)
                throw new SpfException(prefix);
        }
    }

    /**
     * Convert an IP address to an int.
     * 
     * @param ip
     * @return
     * @throws SpfException
     */
    private int ip2Int(String ip) throws SpfException {
        try {
            int n = 0;
            int dot = ip.indexOf('.');
            n = Integer.parseInt(ip.substring(0, dot));
            n <<= 8;
            ip = ip.substring(dot + 1);
            dot = ip.indexOf('.');
            n |= Integer.parseInt(ip.substring(0, dot));
            n <<= 8;
            ip = ip.substring(dot + 1);
            dot = ip.indexOf('.');
            n |= Integer.parseInt(ip.substring(0, dot));
            n <<= 8;
            ip = ip.substring(dot + 1);
            n |= Integer.parseInt(ip);

            return n;
        } catch (Exception e) {
            LOG.error("invalid numeric ip address for " + senderDomain + ": " + this.scanner.toString());
            return -1;
        }
    }

    private void transform() {
        /*
         The following macro letters are expanded in directive arguments:

         l = local-part of responsible-sender
         s = responsible-sender
         o = responsible-domain
         d = current-domain
         i = SMTP client IP (nibble format when an IPv6 address)
         p = SMTP client domain name
         v = client IP version string: "in-addr" for ipv4 or "ip6" for ipv6
         h = HELO/EHLO domain
         r = receiving domain

         The following macro letters are expanded only in "exp" text:

         c = SMTP client IP (easily readable format)
         t = current timestamp in UTC epoch seconds notation

         The uppercase versions of all these macros are URL-encoded.

         **/
        String result = "";
        switch (alpha) {
        case 'L':
        case 'l':
            result = senderName;
            break;
        case 's':
        case 'S':
            result = sender;
            break;
        case 'o':
        case 'O':
            result = senderDomain;
            break;
        case 'r':
        case 'R':
            result = toDomain;
            break;
        case 'd':
        case 'D':
            result = currentDomain;
            break;
        case 'c':
        case 'C':
        case 'i':
        case 'I':
            result = clientIp;
            break;
        case 'p':
        case 'P':
            result = clientDomain;
            break;
        case 'v':
        case 'V':
            result = "in-addr";
            break;
        case 'h':
        case 'H':
            result = clientHelo;
            break;
        case 't':
        case 'T':
            result = "" + System.currentTimeMillis();
            break;
        }
        if (delimiter.length() == 0)
            delimiter = ".";
        LinkedList<String> words = new LinkedList<String>();
        for (StringTokenizer st = new StringTokenizer(result, delimiter); st.hasMoreElements();) {
            String word = st.nextToken();
            if (reverse || left) {
                words.addFirst(word);
            } else {
                words.addLast(word);
            }
        }

        reverse = false;

        if (transform.length() > 0) {
            int n = Integer.parseInt(transform);
            while (words.size() > n)
                words.removeFirst();
        }
        while (words.size() > 0) {
            String w = left ? words.removeLast() : words.removeFirst();
            macro.append(w);
            if (words.size() > 0)
                macro.append('.');
        }

        left = false;

        if (alpha >= 'A' && alpha <= 'Z') {
            // TODO urlencode!

        }
    }

    /**
     * @return
     * @throws SpfException
     * 
     */
    public int validate() {
        try {
            reset();
            prefix = '-';
            push(currentDomain);

            validateSpf();
            pop();
        } catch (SpfException me) {
            return me.getPrefix();
        } catch (ParseException e) {
            return 'e';
        }

        return 'u';
    }

    /**
     * @return
     * @throws ParseException
     */
    private int validateSpf() throws ParseException {
        if (parser.is_SPF()) {
            // parsed through end - handle modifiers.

            for (Iterator<String> i = modifiers.keySet().iterator(); i.hasNext();) {
                String key = i.next();
                String val = modifiers.get(key);
                handleModifier(key, val);
            }
            return 'u';
        }
        return 'e';
    }

    /**
     * @param spf
     * @throws SpfException
     */
    private void push(String nextDomain) throws SpfException {
        if (recurse.contains(nextDomain))
            throw new SpfException('e');
        recurse.add(nextDomain);

        stack.push(new Object[] { scanner, modifiers, currentDomain });
        String spf = getDns().getSpfFromDomain(nextDomain);
        scanner = new Scanner(spf);
        modifiers = new HashMap<String, String>();
        // TODO: wirklich die currentDomain ???ndern?
        currentDomain = nextDomain;
        parser.setScanner(scanner);
    }

    /**
   *
   */
    private void pop() {
        recurse.remove(currentDomain);

        Object[] o3 = stack.pop();
        scanner = (Scanner) o3[0];
        modifiers = (HashMap<String, String>) o3[1];
        currentDomain = (String) o3[2];

        parser.setScanner(scanner);
    }

    /**
     * @param key
     * @param val
     * @throws ParseException
     */
    private void handleModifier(String key, String val) throws ParseException {
        if ("redirect".equalsIgnoreCase(key)) {
            push(val);
            validateSpf();
            pop();
            return;
        }

        if ("exp".equalsIgnoreCase(key)) {
            String text = getDns().getSpfFromDomain(macro.toString());
            scanner = new Scanner(text);
            parser.setScanner(scanner);
            parser.is_MacroString();
            message = macro.toString();
            return;
        }

        // TODO Auto-generated method stub
        System.err.println("unknown modifier: " + key + " " + val);

    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * perform the SPF validation for the given input paramters.
     * 
     * @param fromEmail
     * @param toDomain
     * @param clientIp
     * @param clientDomain
     * @param clientHelo
     * @return
     * @throws SpfException
     */
    public static int validateSpf(String fromEmail, String toDomain, String clientIp, String clientDomain,
            String clientHelo) {
        SpfContext ctx;
        try {
            ctx = new SpfContext(fromEmail, toDomain, clientIp, clientDomain, clientHelo);
            return ctx.validate();
        } catch (SpfException me) {
            return me.getPrefix();
        } catch (Exception e) {
            return 'e';
        }
    }

    private static Dns getDns() {
        Dns dns = (Dns) Config.getInstance().getChild("dns");
        return dns;
    }

}