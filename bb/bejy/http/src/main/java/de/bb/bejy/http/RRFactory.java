/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/RRFactory.java,v $
 * $Revision: 1.19 $
 * $Date: 2013/05/17 10:50:53 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * Robin Round Forwarder
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
        String s = "$Revision: 1.19 $";
        no = "1.0." + s.substring(11, s.length() - 1);
        version =
                "bejy round robin V" + no + " (c) 2000-2012 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    public static String getVersion() {
        return no;
    }

    public static String getFull() {
        return version;
    }

    private HashMap<String, ArrayList<RREntry>> forwarders = new HashMap<String, ArrayList<RREntry>>();

    private SessionManager<String, String> sMan = new SessionManager<String, String>(1000 * 60 * 60L);

    String proxyGroup;

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
            String host = hostCfg.getProperty("host");
            if (host != null) {
                ArrayList<RREntry> rrEntryList = new ArrayList<RREntry>();
                for (Iterator<?> j = hostCfg.children(); j.hasNext();) {
                    VPathCfg pathCfg = (VPathCfg) j.next();
                    String path = pathCfg.getProperty("path");
                    if (path != null) {
                        RREntry rr = new RREntry(path);
                        rr.group = pathCfg.getProperty("group", "");
                        rr.userHeader = pathCfg.getProperty("userHeader", "");
                        for (Iterator<?> k = pathCfg.children(); k.hasNext();) {
                            DestinationCfg destCfg = (DestinationCfg) k.next();
                            String dest = destCfg.getProperty("uri");
                            if (dest != null) {
                                rr.ll.addLast(dest);
                            }
                        }
                        rrEntryList.add(rr);
                    }
                }
                // add each forwarder using the rrEntryList
                for (Enumeration<?> e = new StringTokenizer(host, " ,\r\n\f\t"); e.hasMoreElements();) {
                    String hn = (String) e.nextElement();
                    Object old = forwarders.put(hn, rrEntryList);
                    if (old != null)
                        System.out.println("WARNING: redirection for " + old + " is configured twice");
                }

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

/******************************************************************************
 * $Log: RRFactory.java,v $
 * Revision 1.19  2013/05/17 10:50:53  bebbo
 * @I more DEBUG logging
 * Revision 1.18 2012/12/15 19:38:50 bebbo
 * 
 * @I refactoring Revision 1.17 2012/11/14 15:08:00 bebbo
 * 
 * @I better var names Revision 1.16 2012/11/08 12:14:19 bebbo
 * 
 * @B fixed proxy with HTTP chunked mode
 * @N added SOCKS5 proxy support
 * @N added a fallback option for XML data -> XMPP server Revision 1.15 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.14 2010/07/08 18:16:25 bebbo
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 * 
 *    Revision 1.13 2004/04/16 13:47:24 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.12 2003/08/04 08:35:48 bebbo
 * @R modified redirector (removed uri substitution) and rolled back to last working version
 * 
 *    Revision 1.11 2003/07/09 18:29:49 bebbo
 * @N added default values.
 * 
 *    Revision 1.10 2003/06/18 08:36:52 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.9 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.8 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.7 2001/10/09 08:01:20 bebbo
 * @N added mulit host config
 * 
 *    Revision 1.6 2001/09/15 08:47:49 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.5 2001/04/16 20:04:13 bebbo
 * @B B-params are now send at begin of header
 * 
 *    Revision 1.4 2001/04/16 16:23:18 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.3 2001/04/16 13:43:55 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.2 2001/04/06 05:53:39 bebbo
 * @N working round robin dispatcher
 * 
 *    Revision 1.1 2001/04/02 16:14:35 bebbo
 * @N new
 * 
 *****************************************************************************/
