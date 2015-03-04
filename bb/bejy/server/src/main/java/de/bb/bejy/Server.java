/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Server.java,v $
 * $Revision: 1.66 $
 * $Date: 2014/09/22 09:24:39 $
 * $Author: bebbo $
 * $Locker:  $#
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * generell Server class
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

package de.bb.bejy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.bb.security.Ssl3Server;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.ThreadManager;

/**
 * The generic TCP/IP server.
 * 
 * @author bebbo
 */
public final class Server extends Configurable implements de.bb.util.ThreadManager.Factory {
    final static boolean DEBUG = false;

    // String logFileName;

    // private LogFile logFile;

    /**
     * Create a new server thread and starts it.
     * 
     * @param tm
     *            the ThreadManager instance.
     */
    public void create(ThreadManager tm) {
        // create the thread
        ServerThread t = new ServerThread(tm, this);
        // and start the thread
        t.start();
    }

    private final static String PROPERTIES[][] = { { "name", "a verbose name for this server" },
            { "port", "the port number to listen on" },
            { "timeout", "(ms) limits the maximal waiting time with blocking methods (e.g. read())", "120000" },
            { "bindAddr", "a specific address to bind to (e.g. 127.0.0.1), empty = 0.0.0.0" },
            { "maxThreads", "limits the maximal thread count handling this servers requests", "999" },
            { "maxWait", "limits the maximal inactive threads (recommended: count of CPUs)", "1" },
            { "fallback", "an optional fallback port, if SSL is used and the data does not match", "0" },
            { "startTLS", "the provided sslConfig is used by the protocol and a STARTTLS command", ""}};

    int port;
    int timeout;

    // the Factory
    Factory factory;
    // manager of own threads
    ThreadManager tMan;
    // own socket
    ServerSocket ssocket;

    // private int backLog;

    // private InetAddress bindAddress;

    Selector selector; // only used with nio
    ServerThread currentSelectThread;
    AtomicInteger acceptCount = new AtomicInteger();

    private SslRefCfg sslRef;

    ArrayList<Protocol> waitQueue = new ArrayList<Protocol>();
    ArrayList<Protocol> readyQueue = new ArrayList<Protocol>();

    MultiMap<Long, SelectionKey> expireMap = new MultiMap<Long, SelectionKey>();

    Server() {
        init("tcp/ip server", PROPERTIES);
        try {
            selector = SelectorProvider.provider().openSelector();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Activate the server.
     */
    public void activate(LogFile logFile) throws Exception {
        factory = (Factory) getChild("protocol");
        if (factory == null)
            throw new Exception("no valid protocol configured");

        port = getIntProperty("port", 0);
        timeout = getIntProperty("timeout", 60000);
        if (timeout < 0)
            timeout = Integer.MAX_VALUE;
        int maxCount = getIntProperty("maxThreads", 99);
        int waitCount = getIntProperty("maxWait", 1);

        String bindAddr = getProperty("bindAddr", "").trim();

        sslRef = (SslRefCfg) getChild("sslref");

        ssocket = ((Config) getParent()).getServerSocket(port);

        if (ssocket == null) {
            for (int i = 0; i < 5; ++i) {
                try {
                    if (bindAddr.length() > 0) {
                        System.out.println("get InetAddr for " + bindAddr);
                        InetAddress inetAddr = InetAddress.getByName(bindAddr);
                        ssocket = ServerSocketFactory.getImpl(port, 100, inetAddr);
                    } else {
                        InetAddress inetAddr = InetAddress.getByName("0.0.0.0");
                        ssocket = ServerSocketFactory.getImpl(port, 100, inetAddr);
                    }
                    break;
                } catch (Exception ex) {
                }
                Thread.sleep(1000);
            }
        }
        if (ssocket == null) {
            logFile.writeDate("FAILURE: could not bind on " + bindAddr + ":" + port);
            return;
        }
        logFile.writeDate("bound on " + bindAddr + ":" + port + "=" + ssocket);

        factory.activate(logFile);

        tMan = new ThreadManager(getName(), this);

        tMan.setMaxCount(maxCount);
        tMan.setWaitCount(waitCount);
    }

    public void deactivate(LogFile logFile) throws Exception {
        // disable spawning of new threads
        if (tMan != null) {
            tMan.setMaxCount(0);
            tMan.renew();
            while (tMan.size() > tMan.getRunning()) {
                try {
                    new Socket(getProperty("bindAddr", "127.0.0.1"), getIntProperty("port", 0));
                    // System.out.println("pending shutdown: " + tMan.size());
                    Thread.sleep(50);
                } catch (Exception ex) {
                }
            }
            logFile.writeDate("deactivate : " + getName() + " - " + tMan.size());
        }
        ((Config) getParent()).putServerSocket(port, ssocket);
        if (factory != null)
            factory.deactivate(null);
    }

    /**
     * Return the port number.
     * 
     * @return the port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the name of this server.
     * 
     * @return the name of this server.
     */
    public String getName() {
        return getProperty("name");
    }

    /**
     * Returns true it the server uses SSL.
     * 
     * @return true it the server uses SSL.
     */

    /**
     * Return the configured timeout.
     * 
     * @return the configured timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    public String toString() {
        return getName() + " : " + port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#acceptNewChild(de.bb.bejy.Configurator)
     */
    public boolean acceptNewChild(Configurator ct) {
        String path = ct.getPath();
        if (path.equals("ssl") && getChild("ssl") != null)
            return false;
        if (path.equals("protocol") && getChild("protocol") != null)
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#mayRemove(de.bb.bejy.Configurable)
     */
    public boolean mayRemove(Configurable configurable) {
        if ("de.bb.bejy.server.ssl".equals(configurable.getId()))
            return true;
        return acceptNewChild(configurable.getConfigurator());
    }

    public int getThreadCount() {
        if (tMan == null)
            return 0;
        return tMan.size();
    }

    public boolean supportsTLS() {
        return sslRef != null;
    }
    
    public Ssl3Server getTLSServer() throws IOException {
        if (!supportsTLS())
            return null;
        
        final Ssl3Server s3 = new Ssl3Server(sslRef.getSsl3Config());
        return s3;
    }

    public boolean usesSsl() {
        return supportsTLS() && !"true".equals(getProperty("startTLS"));
    }
}

/******************************************************************************
 * $Log: Server.java,v $
 * Revision 1.66  2014/09/22 09:24:39  bebbo
 * @N added support for SSL host name, to choose certificate and key based on host name
 *
 * Revision 1.65  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 * Revision 1.64  2014/03/23 22:07:32  bebbo
 * @B fixed termination of current waiting socket accept threads
 * Revision 1.63 2013/11/28 12:23:03 bebbo
 * 
 * @N SSL cipher types are configurable
 * @I using nio sockets Revision 1.62 2013/06/18 13:23:51 bebbo
 * 
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.61 2012/08/11 17:03:51 bebbo
 * 
 * @I typed collections Revision 1.60 2010/12/17 23:25:10 bebbo /FIXED: ssl config now supports multiple certificates
 *    Revision 1.59 2010/04/10 12:11:51 bebbo
 * 
 * @D disabled DEBUG output
 * 
 *    Revision 1.58 2009/11/18 08:04:35 bebbo
 * @B fixed a possible loop with a disconnected socket.
 * 
 *    Revision 1.57 2008/03/13 17:21:59 bebbo
 * @I disable TCP nodelay
 * 
 *    Revision 1.56 2007/08/09 16:06:54 bebbo
 * @I integrated new SSL implementation
 * 
 *    Revision 1.55 2007/04/21 19:13:22 bebbo
 * @R improved speed by using the chinese remainder theorem
 * 
 *    Revision 1.54 2007/01/18 21:43:10 bebbo
 * @D added a stacktrace in case of OOME
 * 
 *    Revision 1.53 2006/03/17 20:06:56 bebbo
 * @B fixed possible NPE
 * 
 *    Revision 1.52 2006/03/17 11:30:02 bebbo
 * @I removed double NPE check
 * 
 *    Revision 1.51 2006/02/06 09:13:40 bebbo
 * @I cleanup
 * 
 *    Revision 1.50 2005/11/11 18:52:00 bebbo
 * @N added stuff for verbosity
 * 
 *    Revision 1.49 2004/12/13 15:27:27 bebbo
 * @D added an logfile entry if a SSL configuration (key or cert) cannot be loaded
 * 
 *    Revision 1.48 2004/04/16 13:38:44 bebbo
 * @O removed unused variables
 * 
 *    Revision 1.47 2004/04/07 16:33:06 bebbo
 * @B fixed log message
 * 
 *    Revision 1.46 2004/01/25 12:43:47 bebbo
 * @B fixed handling of empty bind adress
 * 
 *    Revision 1.45 2003/11/26 09:56:15 bebbo
 * @B fixed NPEs
 * 
 *    Revision 1.44 2003/07/09 18:29:45 bebbo
 * @N added default values.
 * 
 *    Revision 1.43 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.42 2003/06/24 09:37:36 bebbo
 * @B timeout setting is now used properly
 * 
 *    Revision 1.41 2003/06/20 09:09:32 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.40 2003/06/18 15:07:04 bebbo
 * @N added ssl configuration
 * 
 *    Revision 1.39 2003/06/18 13:44:18 bebbo
 * @R modified some descriptions
 * 
 *    Revision 1.38 2003/06/18 13:36:08 bebbo
 * @R almost complete on the fly update.
 * 
 *    Revision 1.37 2003/06/17 15:13:36 bebbo
 * @R more changes to enable on the fly config updates
 * 
 *    Revision 1.36 2003/06/17 10:18:10 bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.35 2003/05/13 15:42:07 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.34 2003/01/25 15:07:49 bebbo
 * @N added parameter backLog and bindAddress
 * 
 *    Revision 1.33 2002/12/19 16:32:11 bebbo
 * @B fixed/changed connect behaviour when SSL is used.
 * 
 *    Revision 1.32 2002/12/19 14:51:03 bebbo
 * @I prepared to read further ssl properties
 * 
 *    Revision 1.31 2002/12/17 14:02:30 bebbo
 * @R changed used logFile: if no own is specified the default is used
 * 
 *    Revision 1.30 2002/11/22 21:20:10 bebbo
 * @R added shutdown() method to Protocol and invokin it
 * 
 *    Revision 1.29 2002/11/06 09:41:07 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.28 2002/08/21 14:49:55 bebbo
 * @R further creation of UI
 * 
 *    Revision 1.27 2002/08/21 09:14:43 bebbo
 * @R changes for the admin UI
 * 
 *    Revision 1.26 2002/05/16 15:19:48 franke
 * @C CVS
 * 
 *    Revision 1.25 2002/01/22 08:54:04 franke
 * @I removed the setNoDelay(true) call
 * 
 *    Revision 1.24 2002/01/19 15:48:53 franke
 * @R Protocol.trigger() now returns true on success, false on error
 * @I enabled Nagels Algorithm again
 * 
 *    Revision 1.23 2001/11/20 17:36:17 bebbo
 * @D removed Exception stacktrace
 * 
 *    Revision 1.22 2001/10/09 08:03:52 bebbo
 * @I added setNoDelay(true) before close
 * 
 *    Revision 1.21 2001/10/08 22:05:13 bebbo
 * @L modified logging
 * 
 *    Revision 1.19 2001/09/15 10:38:53 bebbo
 * @D more logging
 * 
 *    Revision 1.18 2001/09/15 08:45:52 bebbo
 * @I using XmlFile instead of ConfigFile
 * @C added comments
 * 
 *    Revision 1.17 2001/05/07 08:59:06 bebbo
 * @B SSL was disabled since the config changed to XML
 * 
 *    Revision 1.16 2001/04/16 16:23:11 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.15 2001/04/16 13:43:26 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.14 2001/03/30 17:27:15 bebbo
 * @R factory.load got an additional parameter
 * 
 *    Revision 1.13 2001/03/27 19:46:35 bebbo
 * @D removed DEBUG out
 * @I added internal loop calling trigger() and work() until socket is closed
 * 
 *    Revision 1.12 2001/03/27 09:51:52 franke
 * @R added function usesSsl()
 * 
 *    Revision 1.11 2001/03/20 18:32:34 bebbo
 * @R reusing connection until false returned
 * 
 *    Revision 1.10 2001/03/09 19:48:46 bebbo
 * @D disabled DEBUG messages
 * 
 *    Revision 1.9 2001/03/05 17:50:53 bebbo
 * @I added SSL server functionality
 * 
 *    Revision 1.8 2001/02/27 18:20:56 bebbo
 * @I disabled DEBUG messages
 * 
 *    Revision 1.7 2001/02/26 17:47:44 bebbo
 * @B local var hides member var
 * 
 *    Revision 1.6 2001/02/20 17:40:46 bebbo
 * @D added debug messages
 * 
 *    Revision 1.5 2001/02/19 19:55:43 bebbo
 * @I logFile is taken from server Entry
 * 
 *    Revision 1.4 2001/01/01 01:01:52 bebbo
 * @R passing logFile to Server CT
 * 
 *    Revision 1.3 2000/12/30 09:01:42 bebbo
 * @R protocols now are throwing Eceptions to indicate that a thread should end
 * 
 *    Revision 1.2 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *    Revision 1.1 2000/11/10 18:13:26 bebbo
 * @N new (uncomplete stuff)
 * 
 *****************************************************************************/
