/******************************************************************************
 * Server thred of BEJY - uses different run loop for new/old IO.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import de.bb.bejy.NioSocket.IS;
import de.bb.log.Logger;
import de.bb.security.Ssl3Server;
import de.bb.util.ThreadManager;

public class ServerThread extends de.bb.util.ThreadManager.Thread {

	private static Logger LOG = Logger.getLogger(ServerThread.class);

	static {
		LOG.setLevel(Logger.WARN);
	}

	Server server;
	ServerSocket serverSocket;
	Protocol protocol;
	private Socket socket;

	// hidden forwarding feature for SSL
	private String fallBackDest;
	private int fallBackPort;

	// a custom value
	private Object context;

	ServerThread(ThreadManager tm, Server server) {
		super(tm);
		this.server = server;
		serverSocket = server.ssocket;

		fallBackDest = "127.0.0.1";
		fallBackPort = 0;
		try {
			String sFallBack = server.getProperty("fallback");
			if (sFallBack != null) {
				final int colon = sFallBack.indexOf(':');
				if (colon > 0) {
					fallBackDest = sFallBack.substring(0, colon);
					sFallBack = sFallBack.substring(colon + 1);
				}
			}
			fallBackPort = Integer.parseInt(sFallBack);
		} catch (Exception ex) {
			// ignore
		}
	}

	// new run loop
	public void runNio() {
		while (protocol != null || !mustDie()) {
			LOG.debug("Start run loop");
			try {
				if (protocol == null) {
					protocol = getNextSignaled();
					LOG.debug("got next protocol: " + protocol);
				}

				if (protocol != null) {
					setBusy();

					socket = protocol.socket;

					int avail = protocol.getIs().available();
					LOG.debug("available for " + socket + " = " + avail);
					if (avail == 0) {
						LOG.debug("fill socket: " + socket);
						IS is = (IS) protocol.socket.getInputStream();
						try {
							is.fill();
							avail = is.available();
						} catch (IOException ioe) {
							LOG.fine(ioe.getMessage(), ioe);
						}
					}
					if (avail > 0) {
						server.tMan.wakeNIdle(1);
						if (protocol.doit()) {
							protocol.os.flush();
							if (enqueue()) {
								socket = null;
								protocol = null;
							}
							continue;
						}
					}

					silentCloseSocket(socket);
					socket = null;
					protocol = null;
					continue;
				}

				if (actAsSelector())
					continue;

				if (actAsSocketAcceptor())
					continue;

				LOG.debug("is idle");
				idle();

			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				silentCloseSocket(socket);
				protocol = null;
			}
		}
		socket = null;
		protocol = null;
		context = null;
	}

	private boolean actAsSocketAcceptor() throws Exception {

		try {
			if (server.acceptCount.incrementAndGet() > server.tMan.getMaxWaitCount()) {
				return false;
			}

			LOG.debug("act as SocketAcceptor");

			setBusy();
			socket = serverSocket.accept();
		} finally {
			server.acceptCount.decrementAndGet();
		}

		LOG.debug("accepted " + socket);
		server.tMan.wakeNIdle(1); // maybe not necessary

		socket.setBlocking(true);
		socket.setTcpNoDelay(true);

		// create a new protocol instance
		protocol = server.factory.create();

		protocol.setServer(server);
		protocol.setStreams(socket.getInputStream(), socket.getOutputStream(),
				socket.getInetAddress().getHostAddress());

		// start TLS if configured
		if (server.usesSsl()) {
			final Ssl3Server s3 = server.getTLSServer();
			try {
				socket.getInputStream().available();
				socket.setSoTimeout(1000);
				protocol.startTLS(s3);
			} catch (IOException ioe) {
				// if fall back is configured, pass the data to the fall back
				// port and pipe the data.
				if (fallBackPort != 0 && s3.getFirstChunk() != null) {
					doFallbackConnect(socket, fallBackDest, fallBackPort, s3, protocol.getIs(), protocol.getOs());
					socket = null;
				}
				throw ioe;
			}
		}

		if (socket != null) {
			socket.setSoTimeout(server.timeout);
			protocol.socket = socket;
			protocol.trigger();
		}
		return true;
	}

	private boolean enqueue() throws IOException {
		protocol.socket.setBlocking(false);
		if (protocol.socket.getInputStream().available() > 0) {
			// data available continue with blocking mode
			protocol.socket.setBlocking(true);
			return false;
		}

		final Selector selector = server.selector;
		synchronized (server) {
			server.waitQueue.add(protocol);
			selector.wakeup();
		}
		return true;
	}

	private boolean actAsSelector() throws IOException {
		final Selector selector = server.selector;
		try {
			// only one select thread
			synchronized (server) {
				if (server.currentSelectThread != null)
					return false;

				server.currentSelectThread = this;
			}

			setBusy();

			Set<SelectionKey> selected;
			for (;;) {
				addPending(selector);

				long now = System.currentTimeMillis();
				Long first = server.expireMap.firstKey();
				while (first != null && first < now) {
					final SelectionKey expired = server.expireMap.get(first);
					final Protocol p = (Protocol) expired.attachment();

					LOG.debug("expired: " + expired.attachment());
					server.expireMap.remove(first, expired);
					expired.cancel();
					selector.selectNow();
					silentCloseSocket(p.socket);

					first = server.expireMap.firstKey();
				}

				LOG.debug("acts as selector");

				long to;
				if (first == null) {
					to = Integer.MAX_VALUE;
				} else {
					to = first - System.currentTimeMillis();
				}

				// if (to > 1000)
				// to = 1000;
				LOG.debug("select " + to + " with " + selector.keys().size());
				selector.select(to);
				selected = selector.selectedKeys();

				LOG.debug("got " + selected.size() + " sockets with data");
				if (selected.size() > 0) {
					break;
				}

				// server.tMan.wakeNIdle(1); // maybe not necessary
			}

			synchronized (server) {
				for (final SelectionKey sk : selected) {
					final Protocol p = (Protocol) sk.attachment();
					sk.cancel();

					server.expireMap.remove(p.expiresAt, sk);
					server.readyQueue.add(p);
					LOG.debug("ready: " + p);
				}
			}

			server.tMan.wakeNIdle(selected.size());
			selected.clear();
			selector.selectNow(); // removes the cancelled keys

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (server.currentSelectThread == this) {
				server.currentSelectThread = null;
			}
		}
		return true;
	}

	private void addPending(Selector selector) {
		synchronized (server) {
			final Long expiresAt = System.currentTimeMillis() + 30000;// server.timeout;
			for (final Protocol p : server.waitQueue) {
				try {

					LOG.debug("add: " + p);
					SelectionKey key = p.socket.register(selector);
					key.attach(p);

					p.expiresAt = expiresAt;
					server.expireMap.put(expiresAt, key);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			server.waitQueue.clear();
		}
	}

	private Protocol getNextSignaled() throws IOException {
		synchronized (server) {
			if (server.readyQueue.isEmpty())
				return null;

			final Protocol p = server.readyQueue.remove(server.readyQueue.size() - 1);
			try {
				p.socket.setBlocking(true);
				p.socket.setSoTimeout(server.timeout);
			} catch (IOException ioe) {
				LOG.debug(ioe.getMessage(), ioe);
			}

			return p;
		}
	}

	public void run() {
		if (server.ssocket.isNIO()) {
			runNio();
		} else {
			runOld();
		}
	}

	public void runOld() {
		// this endless loop is a MUST
		try {
			protocol = server.factory.create();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		protocol.server = server;

		while (!mustDie()) {
			try {
				// wait for trigger
				if (!protocol.goodStream()) {
					if (Server.DEBUG)
						System.out.println("server: enter accept");
					socket = serverSocket.accept();
					if (Server.DEBUG)
						System.out.println("server: accepted on " + socket.getLocalAddress());
					socket.setSoTimeout(server.timeout);
					socket.setTcpNoDelay(true);
					// now start to do something
					setBusy(); // checks also whether threads must be added

					protocol.isSecure = false;
					protocol.setStreams(socket.getInputStream(), socket.getOutputStream(),
							socket.getInetAddress().getHostAddress());
					if (server.usesSsl()) {
						final Ssl3Server s3 = server.getTLSServer();
						try {
							socket.setSoTimeout(8000);
							protocol.startTLS(s3);
						} catch (IOException ioe) {
							// if fall back is configured, pass the data to the
							// fall back port and pipe the data.
							if (fallBackPort != 0 && s3.getFirstChunk() != null) {
								doFallbackConnect(socket, fallBackDest, fallBackPort, s3, protocol.getIs(),
										protocol.getOs());
								socket = null;
								continue;
							}
							throw ioe;
						} finally {
							if (socket != null)
								socket.setSoTimeout(server.timeout);
						}
					}
				}

				// after accept data arrives -> start the protocol
				while (protocol.trigger()) {
					// do the work
					if (!protocol.work())
						break;
				}
			} catch (Throwable t) // you also MUST catch EVERYTHING!
			{
				if (Server.DEBUG) {
					System.out.println("server: exception " + t.getMessage());
					t.printStackTrace();
				} else if (t instanceof OutOfMemoryError) {
					t.printStackTrace();
				}
				// but anyway: we want to get replaced then!
				// a new thread is created
				// - which creates also a new protocol object!
				requestDie();
			} finally {
				// clear the context
				context = null;
				// clear the streams --> listen for a new connection
				protocol.setStreams(null, null, "dead");
				silentCloseSocket(socket);
			}
			if (Server.DEBUG)
				System.out.println("server: end of loop");
		}
		protocol.shutdown();
	}

	private static void silentCloseSocket(Socket socket) {
		if (socket != null) {
			try {
				if (Server.DEBUG)
					System.out.println("closing socket");
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void doFallbackConnect(Socket socket, String fallBackDest, int fallBack, final Ssl3Server s3,
			final InputStream is, final OutputStream os) throws IOException {
		// System.out.println("using fallback port");
		socket.setSoTimeout(0);
		socket.setTcpNoDelay(true);
		byte[] chunk = s3.getFirstChunk();
		if (chunk[0] == 0 && chunk[1] == 0 && chunk[2] == 0 && chunk[3] == 0 && chunk[4] == 0)
			chunk = null;

		final Socket fallBackSocket = ServerSocketFactory.getSocket(fallBackDest, fallBack);
		InputStream fbis = fallBackSocket.getInputStream();
		OutputStream fbos = fallBackSocket.getOutputStream();

		// create the pipe threads and start them
		IOPipeThread client2Server = new IOPipeThread(is, fbos);
		IOPipeThread server2Client = new IOPipeThread(fbis, os);
		// cross connect
		server2Client.setSlave(client2Server);
		client2Server.setSlave(server2Client);

		// start
		server2Client.start();
		if (chunk != null) {
			fbos.write(chunk);
		}
		client2Server.start();
	}

	public Object getContext() {
		return context;
	}

	public void setContext(Object context) {
		this.context = context;
	}

	public Protocol getProtocol() {
		return protocol;
	}
}