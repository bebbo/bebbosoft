package de.bb.bejy.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import de.bb.bejy.Configurable;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.io.IOUtils;
import de.bb.log.Logger;
import de.bb.util.ByteUtil;
import de.bb.util.LogFile;
import de.bb.util.Misc;
import de.bb.util.Pool;

public class FastCGIHost extends Configurable {

	private final static Logger LOG = Logger.getLogger(FastCGIHost.class);

	// 0001 -> responder 01 -> keep connection
	static final byte[] BEGIN_REQUEST_BODY = Misc.hex2Bytes("0001010000000000");

	static final byte[] NULL_BYTES = new byte[0];

	private static final byte[] PAD = Misc.hex2Bytes("0000000000000000");

	private final static String PROPERTIES[][] = { { "command", "the fast-CGI command line" },
			{ "env", "additional environment vars: name1=value1|name2=value2|...", "" },
			{ "port", "the port to connect to the fast-CGI", "" },
			{ "name", "name to lookup this fast-CGI host", "" } };

	private final class FastCgiPoolFactory implements Pool.Factory {
		private int port;

		public FastCgiPoolFactory(int port) {
			this.port = port;
		}

		public boolean validateKey(Object key) {
			return true;
		}

		public boolean validate(Object object) {
			final Socket s = (Socket) object;
			return s.isConnected();
		}

		public void destroy(Object o) {
			final Socket s = (Socket) o;
			try {
				s.close();
			} catch (IOException e) {
			}
		}

		public Object create() throws Exception {
			try {
				Socket s = new Socket("127.0.0.1", port);
				s.setKeepAlive(true);
				return s;
			} catch (IOException ioe) {
				startFastCgi();
				Socket s = new Socket("127.0.0.1", port);
				s.setKeepAlive(true);
				return s;
			}
		}
	}

	private Pool pool;

	private int port;

	private Process proc;

	private ArrayList<String> myEnv = new ArrayList<String>();

	private static ArrayList<String> globalEnv;

	public FastCGIHost() {
		init("fastcgi", PROPERTIES);
	}

	public void activate(LogFile logFile) throws Exception {
		super.activate(logFile);

		this.port = getIntProperty("port", -1);
		pool = new Pool(new FastCgiPoolFactory(port));

		final String senv = getProperty("env", "");
		for (final StringTokenizer st = new StringTokenizer(senv, "|"); st.hasMoreElements();) {
			final String t = st.nextToken();
			myEnv.add(t);
		}

		startFastCgi();
	}

	public Socket obtain() throws Exception {
		final Thread key = Thread.currentThread();
		final Socket s = (Socket) pool.obtain(key);
		LOG.debug("obtaining socket: " + s);
		return s;
	}

	public int getMaxCount() {
		return pool.getMaxCount();
	}

	public void release(Socket socket) {
		LOG.debug("releasing socket: " + socket);
		final Thread key = Thread.currentThread();
		pool.release(key, socket);
	}

	public void destroy(Socket socket)  {
		final Thread key = Thread.currentThread();
		try {
			pool.renew(key);
		} catch (Exception e) {
		}
		pool.release(key, socket);
	}

	private void startFastCgi() throws Exception {
		final String sCommand = getProperty("command", "").trim();
		if (proc != null) {
			proc.destroy();
			proc = null;
		}

		if (pool != null) {
			pool.setMaxCount(0);
			pool.setKeepAlive(60000);
		}

		if (sCommand.length() > 0) {
			LOG.debug("checking for existing server: " + sCommand);
			boolean exists = false;
			try {
				Socket socket = new Socket("127.0.0.1", port);
				exists = true;
				socket.close();
			} catch (Exception ex) {
			}

			if (!exists) {
				LOG.debug("starting command: " + sCommand);
				proc = Runtime.getRuntime().exec(sCommand, myEnv.toArray(new String[myEnv.size()]));
				Thread.sleep(1000);
			}
		}
		pool = new Pool(new FastCgiPoolFactory(port));

		// query the max count
		Socket socket = (Socket) pool.obtain(this);
		FastByteArrayOutputStream params = new FastByteArrayOutputStream(64);
		addParam(params, "FCGI_MAX_CONNS", "");
		addParam(params, "FCGI_MAX_REQS", "");

		FastByteArrayOutputStream getValues = new FastByteArrayOutputStream(64);
		addHeader(getValues, 9, params.toByteArray());

		OutputStream fcgios = socket.getOutputStream();
		fcgios.write(getValues.toByteArray());
		fcgios.flush();

		InputStream fcgiis = socket.getInputStream();
		byte buffer[] = new byte[64];
		try {
			socket.setSoTimeout(1000 * 5);
			IOUtils.readFully(fcgiis, buffer, 0, 8);

			final int type = buffer[1];
			if (type != 10)
				throw new IOException("fcgi get values failed");

			final int skipLen = buffer[6] & 0xff;
			int cl = ((buffer[4] & 0xff) << 8) | (buffer[5] & 0xff);
			if (cl > buffer.length)
				buffer = new byte[cl];

			IOUtils.readFully(fcgiis, buffer, 0, cl);
			int skipped = 0;
			while (skipped < skipLen) {
				skipped += fcgiis.skip(skipLen - skipped);
			}

			if (cl > 0) {
				int l1 = buffer[0];
				int l2 = buffer[1];
				String name = new String(buffer, 0, 2, l1);
				String sval1 = new String(buffer, 0, 2 + l1, l2);
				LOG.debug("fcgi got value: " + name + "=" + sval1);
				int val1 = Integer.parseInt(sval1);

				int pos = 2 + l1 + l2;
				if (cl > pos) {
					l1 = buffer[pos];
					l2 = buffer[pos + 1];
					name = new String(buffer, 0, pos + 2, l1);
					String sval2 = new String(buffer, 0, pos + 2 + l1, l2);
					LOG.debug("fcgi got value: " + name + "=" + sval2);
					int val2 = Integer.parseInt(sval2);
					pool.setMaxCount(val1 < val2 ? val1 : val2);
				} else {
					pool.setMaxCount(val1);
				}
			} else {
				pool.setMaxCount(1);
			}
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			pool.setMaxCount(1);
		}
		socket = (Socket) pool.renew(this);
		pool.release(this, socket);
	}

	public static void addParam(OutputStream header, String name, String value) throws IOException {
		if (name == null || value == null)
			return;
		final int nLen = name.length();
		if (nLen <= 0x7f) {
			header.write(nLen);
		} else {
			header.write(0x80 | (nLen >>> 24));
			header.write(nLen >>> 16);
			header.write(nLen >>> 8);
			header.write(nLen);
		}
		final int vLen = value.length();
		if (vLen <= 0x7f) {
			header.write(vLen);
		} else {
			header.write(0x80 | (vLen >>> 24));
			header.write(vLen >>> 16);
			header.write(vLen >>> 8);
			header.write(vLen);
		}
		ByteUtil.writeString(name, header);
		ByteUtil.writeString(value, header);
	}

	public static void addHeader(final OutputStream fbos, final int type, final byte[] content) throws IOException {
		addFcgi(fbos, type, content, content.length);
	}

	public static void addFcgi(final OutputStream fbos, final int type, final byte[] content, int len)
			throws IOException {
		fbos.write(1); // version
		fbos.write(type);

		// ++requestId;
		// fbos.write(requestId >>> 8);
		// fbos.write(requestId); //

		fbos.write(0);
		fbos.write(1);// type == 9 ? 0 : 1); //

		fbos.write(len >>> 8);
		fbos.write(len);
		int pad = (8 - (len & 7)) & 7;
		fbos.write(pad); // pad
		fbos.write(0); // reserved
		fbos.write(content, 0, len);
		if (pad > 0)
			fbos.write(PAD, 0, pad);
	}

}
