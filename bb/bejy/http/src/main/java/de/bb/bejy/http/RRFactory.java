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

package de.bb.bejy.http;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.util.ByteRef;
import de.bb.util.LogFile;
import de.bb.util.SessionManager;

public class RRFactory extends de.bb.bejy.Factory {
    private final static boolean DEBUG = false;

    private final static String no;

    private final static String version;
    static {
        no = "1.0.20";
        version = "bejy round robin V" + no + " (c) 2000-2015 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    public static String getVersion() {
        return no;
    }

    public static String getFull() {
        return version;
    }

    private HashMap<String, ArrayList<RREntry>> forwarders = new HashMap<String, ArrayList<RREntry>>();

    private SessionManager<String, String> sMan = new SessionManager<String, String>(1000 * 60 * 60L);

    String proxyGroup = "not configured";

    String jabberHost;
    int jabberPort;

    String socks5Group;

    public de.bb.bejy.Protocol create() throws Exception {
        return new RRProtocol(this, logFile);
    }

    /**
     * Return the name of this protocol.
     * 
     * @return the name of this protocol.
     */
    public String getName() {
        return "REDIR";
    }

    /**
     * Override the id for further extensions.
     * 
     * @author bebbo
     * @return an ID to override the Configurator ID.
     */
    public String getId() {
        return "de.bb.bejy.http.redir.protocol";
    }

    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);
        for (Iterator<?> i = children(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof VProxyCfg) {
                VProxyCfg proxy = (VProxyCfg) o;
                this.proxyGroup = proxy.getProperty("group");
                String xmpp = proxy.getProperty("xmpp", "");
                if (xmpp != null && xmpp.trim().length() > 0) {
                    int colon = xmpp.indexOf(':');
                    if (colon > 0) {
                        jabberPort = Integer.parseInt(xmpp.substring(colon + 1));
                        jabberHost = xmpp.substring(0, colon);
                    }
                }

                this.socks5Group = proxy.getProperty("socks5", "");
                if (socks5Group != null && socks5Group.trim().length() == 0) {
                    socks5Group = null;
                }
                continue;
            }
            VHostCfg hostCfg = (VHostCfg) o;
            final String hostNames = hostCfg.getProperty("host");
            if (hostNames == null)
                continue;

            final String redirect = hostCfg.getProperty("redirect");

            ArrayList<RREntry> rrEntryList = new ArrayList<RREntry>();
            for (Iterator<?> j = hostCfg.children(); j.hasNext();) {
                VPathCfg pathCfg = (VPathCfg) j.next();
                String path = pathCfg.getProperty("path");
                if (path != null) {
                    RREntry rr = new RREntry(path);
                    rr.redirect = redirect;
                    rr.group = pathCfg.getProperty("group", "");
                    rr.userHeader = pathCfg.getProperty("userHeader", "");
                    rr.setReverseByExt(pathCfg.getProperty("reverseByExt", ""));
                    rr.setReverseByType(pathCfg.getProperty("reverseByType", ""));
                    for (Iterator<?> k = pathCfg.children(); k.hasNext();) {
                        VDestinationCfg destCfg = (VDestinationCfg) k.next();
                        String dest = destCfg.getProperty("uri");
                        if (dest != null) {
                            rr.ll.addLast(dest);
                        }
                    }
                    
                    rrEntryList.add(rr);
                }
            }
            // add each forwarder using the rrEntryList
            for (Enumeration<?> e = new StringTokenizer(hostNames, " ,\r\n\f\t"); e.hasMoreElements();) {
                String hn = (String) e.nextElement();
                Object old = forwarders.put(hn, rrEntryList);
                if (old != null)
                    System.out.println("WARNING: redirection for " + old + " is configured twice");
            }
        }
    }

    /**
     * @param host
     * @param path
     * @return
     */
    public RREntry getRREntry(ByteRef host, ByteRef path) {
        ArrayList<RREntry> v = forwarders.get(host.toString());

        if (v == null)
            v = forwarders.get("*");

        if (DEBUG)
            System.out.println("got forwarder: " + v);

        if (v == null)
            return null;

        RREntry to = null;

        if (DEBUG)
            System.out.println("lookup for PATH=" + path);
        for (Iterator<RREntry> e = v.iterator(); e.hasNext();) {
            RREntry re = e.next();
            if (DEBUG)
                System.out.println("? " + re.path + " = " + path);
            if (re.path.length() <= path.length() && re.path.equals(path.substring(0, re.path.length()))) {
                to = re;
                break;
            }
        }

        if (DEBUG)
            System.out.println("got destination: " + to);

        return to;
    }

    /**
     * Return the forward uri.
     * 
     * @param remoteAddress
     *            the remoteAddress
     * @param to
     *            the RREntry
     * @return the forward uri.
     */
    public String getUri(String remoteAddress, RREntry to) {
        String key = remoteAddress + '-' + to;
        String fwd = (String) sMan.get(key);
        if (fwd != null) {
            sMan.touch(key);
            return fwd;
        }

        fwd = to.nextDestination();
        sMan.put(key, fwd);
        return fwd;
    }

    /* (non-Javadoc)
     * @see de.bb.bejy.Configurable#deactivate(de.bb.util.LogFile)
     */
    public void deactivate(LogFile logFile) throws Exception {
        super.deactivate(logFile);
        forwarders.clear();
    }
}
