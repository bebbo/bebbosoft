/******************************************************************************
 * Server config of BEJY
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2016.
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
            { "maxWait", "limits the maximal inactive threads (recommended: count of CPUs)", "3" },
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
        int waitCount = getIntProperty("maxWait", 3);
        if (waitCount <= 0) waitCount = 1;

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
