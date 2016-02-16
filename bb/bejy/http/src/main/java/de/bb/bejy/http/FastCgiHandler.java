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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import de.bb.bejy.Version;
import de.bb.io.FastBufferedInputStream;
import de.bb.io.FastBufferedOutputStream;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.io.IOUtils;
import de.bb.log.Logger;
import de.bb.util.ByteRef;
import de.bb.util.ByteUtil;
import de.bb.util.LogFile;
import de.bb.util.Misc;
import de.bb.util.Pool;

/**
 * CGI Handle to connect to a fast CGI server. See
 * http://www.fastcgi.com/devkit/doc/fcgi-spec.html
 * 
 * @author bebbo
 * 
 */
public class FastCgiHandler extends HttpHandler {

	private static final class FastCgiPool implements Pool.Factory {
		private int port;

		public FastCgiPool(int port) {
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
			Socket s = new Socket("127.0.0.1", port);
			s.setKeepAlive(true);
			return s;
		}
	}

	private final static Logger LOG = Logger.getLogger(FastCgiHandler.class);

	private boolean DEBUG = HttpProtocol.DEBUG;

	private final static String PROPERTIES[][] = { { "command", "the fast-CGI command line" },
			{ "timeout", "maximum timeout im ms to wait for completion", "180000" },
			{ "debug", "enable debug output", "false" }, { "h404", "handler for file not found", "" },
			{ "env", "additional environment vars: name1=value1|name2=value2|...", "" },
			{ "port", "the port to connect to the fast-CGI", "" } };

	private final static ByteRef STATUS = new ByteRef("STATUS");

	private final static ByteRef COOKIE = new ByteRef("SET-COOKIE");

	private final static ByteRef LOCATION = new ByteRef("LOCATION");

	private final static String SERVER_SOFTWARE = "BEJY/" + Version.getVersion() + " http/" + HttpProtocol.getVersion();

	// 0001 -> responder 01 -> keep connection
	private static final byte[] BEGIN_REQUEST_BODY = Misc.hex2Bytes("0001010000000000");

	private static final byte[] NULL_BYTES = new byte[0];

	private static final byte[] PAD = Misc.hex2Bytes("0000000000000000");

	// String sChDir;
	long timeout;

	private String h404;

	// fast cgi stuff
	private Process proc;
	private int port;

	private Pool pool;

	private static ArrayList<String> globalEnv;

	private static int requestId;

	private ArrayList<String> myEnv = new ArrayList<String>();

	public FastCgiHandler() {
		init("Fast CGI Handler", PROPERTIES);
	}

	public void activate(LogFile logFile) throws Exception {
		super.activate(logFile);

		timeout = getIntProperty("timeout", 180000);
		DEBUG |= "true".equals(getProperty("debug", "false"));

		h404 = getProperty("h404", "");

		final String sPort = getProperty("port");
		try {
			port = Integer.parseInt(sPort);
		} catch (Exception e) {
			throw new Exception("fastcgi: invalid port: " + sPort);
		}

		myEnv.addAll(globalEnv);
		final String senv = getProperty("env", "");
		for (final StringTokenizer st = new StringTokenizer(senv, "|"); st.hasMoreElements();) {
			final String t = st.nextToken();
			myEnv.add(t);
		}

		startFastCgi();
	}

	private void startFastCgi() throws Exception {
		final String sCommand = getProperty("command", "").trim();
		if (proc != null) {
			proc.destroy();
			proc = null;
		}

		if (pool != null) {
			pool.setMaxCount(0);
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
		pool = new Pool(new FastCgiPool(port));

		// query the max count
		Socket socket = (Socket) pool.obtain(this);
		FastByteArrayOutputStream params = new FastByteArrayOutputStream(64);
		addParam(params, "FCGI_MAX_CONNS", "");
		addParam(params, "FCGI_MAX_REQS", "");

		FastByteArrayOutputStream getValues = new FastByteArrayOutputStream(64);
		// addFcgi(getValues, 1, BEGIN_REQUEST_BODY);
		addHeader(getValues, 9, params.toByteArray());
		// addFcgi(getValues, 4, HEADER_END);
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
				pool.setMaxCount(2);
			}
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			pool.setMaxCount(2);
		}
		socket = (Socket) pool.renew(this);
		pool.release(this, socket);
	}

	public void deactivate(LogFile logFile) throws Exception {
		if (proc != null) {
			proc.destroy();
			proc = null;
		}
		pool.setMaxCount(0);
		super.deactivate(logFile);
	}

	public void service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
			throws IOException {
		HttpServletRequest hp = (HttpServletRequest) request;
		HttpRequest hp0 = RequestDispatcher.dereference(request);
		HttpResponse sr = RequestDispatcher.dereference(response);

		if (hp0.method.equals("OPTIONS")) {
			sendOptions(sr);
			return;
		}

		HttpContext context = hp0.context;

		String queryString = hp.getQueryString();
		String servletPath = hp.getServletPath();
		String pathInfo = hp.getPathInfo();

		String redirectUrl = null;
		{
			String path = hp0.context.getRealPath2(servletPath);
			if (DEBUG)
				System.out.println("CGI handler: " + path);

			File f = new File(path);
			if (!f.exists()) {
				// apply configure redirect
				if (h404 != null && h404.length() > 0 && !h404.startsWith(servletPath)) {
					redirectUrl = urlEscape(servletPath);
					servletPath = h404 + redirectUrl;
					int qm = servletPath.indexOf('?');
					if (qm > 0) {
						if (queryString != null) {
							queryString += "&" + servletPath.substring(qm + 1);
						} else {
							queryString = servletPath.substring(qm + 1);
						}
						servletPath = servletPath.substring(0, qm);
					}
					path = hp0.context.getRealPath2(servletPath);
					if (DEBUG)
						System.out.println("CGI redirect: " + path);
					f = new File(path);
				}
			}
			if (!f.exists()) {
				sr.setStatus(404);
				return;
			}
			// send redirect if trailing slash is missing
			if (f.isDirectory()) {
				if (!servletPath.endsWith("/")) {
					new HttpHandler.H302Handler(hContext).service(request, response);
					return;
				}
			}
		}

		if (queryString == null)
			queryString = "";

		InputStream is = hp.getInputStream();
		OutputStream os = response.getOutputStream();

		Thread me = Thread.currentThread();

		Socket socket = null;
		try {
			FastByteArrayOutputStream params = new FastByteArrayOutputStream();
			int cl = hp.getContentLength();
			if (cl >= 0) {
				addParam(params, "CONTENT_LENGTH", Integer.toString(cl));
			} else {
				cl = is.available();
			}

			String s = hp.getContentType();
			if (s != null) {
				addParam(params, "CONTENT_TYPE", s);
				addParam(params, "HTTP_CONTENT_TYPE", s);
			}

			addParam(params, "DOCUMENT_ROOT", context.getRealPath("/"));
			addParam(params, "REDIRECT_STATUS", "200");
			if (redirectUrl != null)
				addParam(params, "REDIRECT_URL", "redirectUrl");

			addParam(params, "GATEWAY_INTERFACE", "CGI/1.1");

			if (pathInfo != null)
				addParam(params, "PATH_INFO", pathInfo);

			addParam(params, "PATH_TRANSLATED", context.getRealPath(servletPath));
			addParam(params, "QUERY_STRING", queryString);
			addParam(params, "REMOTE_ADDR", hp.getRemoteAddr());
			addParam(params, "REMOTE_HOST", hp.getRemoteHost());

			s = hp.getRemoteUser();
			if (s != null)
				addParam(params, "REMOTE_USER", hp.getRemoteUser());
			addParam(params, "REQUEST_METHOD", hp.getMethod());

			s = hp.getContextPath();
			{
				String s2 = servletPath;
				if (s.endsWith("/") && s2.startsWith("/"))
					s += s2.substring(1);
				else
					s += s2;
			}
			addParam(params, "SCRIPT_NAME", s);

			if (queryString.length() > 0)
				addParam(params, "REQUEST_URI", urlEscape(hp0.getRequestURI() + "?" + queryString));
			else
				addParam(params, "REQUEST_URI", urlEscape(hp0.getRequestURI()));
			addParam(params, "SERVER_NAME", hp.getServerName());
			addParam(params, "SERVER_PORT", Integer.toString(hp.getServerPort()));
			addParam(params, "SERVER_PROTOCOL", hp.getProtocol());

			addParam(params, "SERVER_SOFTWARE", SERVER_SOFTWARE);
			Cookie[] cookies = hp.getCookies();
			if (cookies != null && cookies.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < cookies.length; ++i) {
					javax.servlet.http.Cookie c = cookies[i];
					if (sb.length() > 0)
						sb.append("; ");
					sb.append(c.getName());
					sb.append('=');
					sb.append(c.getValue());
				}
				addParam(params, "HTTP_COOKIE", sb.toString());
			}

			for (Enumeration<String> e = hp.getHeaderNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				String val = hp.getHeader(key);
				key = key.toUpperCase();
				int idx;
				while ((idx = key.indexOf('-')) >= 0) {
					key = key.substring(0, idx) + "_" + (key.substring(idx + 1));
				}
				addParam(params, "HTTP_" + key + "", val);
			}
			// addHeader(header, "HTTP_ACCEPT", hp.getHeader("accept"));
			String referer = hp.getHeader("referer");
			if (referer != null)
				addParam(params, "HTTP_REFERER", referer);

			addParam(params, "HTTP_USER_AGENT", hp.getHeader("user-agent"));

			// send request body + header

			InputStream fcgiis;
			OutputStream fcgios;

			byte[] buffer = new byte[0x2000];
			final FastByteArrayOutputStream fbos = new FastByteArrayOutputStream();
			int status = -1;
			try {
				socket = (Socket) pool.obtain(me);
				socket.setSoTimeout((int) timeout);
				fcgiis = new FastBufferedInputStream(socket.getInputStream(), 4096);
				fcgios = new FastBufferedOutputStream(socket.getOutputStream(), 4096);

				addHeader(fcgios, 1, BEGIN_REQUEST_BODY);
				addHeader(fcgios, 4, params.toByteArray());
				addHeader(fcgios, 4, NULL_BYTES);

				// send content if any
				while (cl > 0) {
					int b0 = is.read();
					if (b0 < 0)
						throw new IOException("EOS");
					buffer[0] = (byte) b0;
					int toRead = (cl < buffer.length ? cl : buffer.length) - 1;
					int read = 1;
					if (toRead > 0)
						read += is.read(buffer, 1, toRead);
					addFcgi(fcgios, 5, buffer, read);
					cl -= read;
				}

				addHeader(fcgios, 5, NULL_BYTES);
				fcgios.flush();

				// read response
				for (;;) {
					IOUtils.readFully(fcgiis, buffer, 0, 8);
					final int type = buffer[1];
					final int skipLen = buffer[6] & 0xff;
					cl = ((buffer[4] & 0xff) << 8) | (buffer[5] & 0xff);
					if (cl > buffer.length)
						buffer = new byte[cl];

					IOUtils.readFully(fcgiis, buffer, 0, cl);
					int skipped = 0;
					while (skipped < skipLen) {
						skipped += fcgiis.skip(skipLen - skipped);
					}

					if (type == 6) { // STDOUT
						fbos.write(buffer, 0, cl);
						continue;
					}
					if (type == 7) {
						System.out.println(new String(buffer, 0, cl, "utf-8"));
						continue;
					}
					if (type == 3) {
						status = buffer[4] == 0 ? 200 : 500;
						break;
					}
					throw new IOException("unknown:" + type);
					// ignore the rest
				}
			} catch (Exception ex) {
				if (ex instanceof IOException && ex.getMessage().equals("EOS"))
					throw (IOException) ex;
				try {
					startFastCgi();
				} catch (Exception e) {
				}
			}
			ByteRef br = new ByteRef(fbos.toByteArray());
			String statusMsg = null;
			for (;;) {
				ByteRef l = br.nextLine();
				if (l == null)
					break;
				if (l.length() == 0) {
					if (status < 0)
						status = 200;
					if (status >= 400) {
						if (statusMsg != null)
							sr.sendError(status, statusMsg);
						else
							sr.sendError(status);
					} else {
						sr.setStatus(status);
						sr.setContentLength(br.length());
					}
					break;
				}
				int p = l.indexOf(':');
				if (p > 0) {
					ByteRef key = l.substring(0, p).toUpperCase();
					ByteRef val = l.substring(p + 1).trim();
					if (key.equals(STATUS)) {
						p = val.indexOf(' ');
						try {
							status = Integer.parseInt(val.substring(0, p).toString());
							statusMsg = val.substring(p + 1).toString();
						} catch (Exception e) {
						}
					} else if (key.equals(COOKIE))
						sr.addCookie(HttpResponse.createCookie(val));
					else if (key.equals(LOCATION)) {
						sr.sendRedirect(val.toString());
						status = 302;
					} else
						sr.addHeader(key.toString(), val.toString());
				}
				if (DEBUG)
					System.out.println(l);
			}

			br.writeTo(os);

		} finally {
			if (socket != null)
				pool.release(me, socket);
		}
	}

	private static void addParam(FastByteArrayOutputStream header, String name, String value) throws IOException {
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

	private static void addHeader(final OutputStream fbos, final int type, final byte[] content) throws IOException {
		addFcgi(fbos, type, content, content.length);
	}

	private static void addFcgi(final OutputStream fbos, final int type, final byte[] content, int len)
			throws IOException {
		fbos.write(1); // version
		fbos.write(type);

		// ++requestId;
		// fbos.write(requestId >>> 8);
		// fbos.write(requestId); //

		fbos.write(0);
		fbos.write(type == 9 ? 0 : 1); //

		fbos.write(len >>> 8);
		fbos.write(len);
		int pad = (8 - (len & 7)) & 7;
		fbos.write(pad); // pad
		fbos.write(0); // reserved
		fbos.write(content, 0, len);
		if (pad > 0)
			fbos.write(PAD, 0, pad);
	}

	static {
		globalEnv = new ArrayList<String>();
		// autodetect some settings...
		try {
			String os = System.getProperty("os.name").toUpperCase();
			// if (DEBUG)
			System.out.println(os);

			if (os.length() >= 7 && os.substring(0, 7).equals("WINDOWS")) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				if (os.endsWith("NT") || os.endsWith("2000") || os.endsWith("XP") || os.endsWith("VISTA")
						|| os.startsWith("WINDOWS"))
					de.bb.util.Process.execute("cmd /c set", null, bos, null);
				else
					de.bb.util.Process.execute("command /c set", null, bos, null);

				String br = bos.toString().toUpperCase();
				// System.out.println(br);

				String sroot, windir;
				sroot = windir = null;
				int idx = br.indexOf("SYSTEMROOT=");
				if (idx > 0) {
					int end = br.indexOf('\r', idx);
					sroot = br.substring(idx, end);
				}
				idx = br.indexOf("WINDIR=");
				if (idx > 0) {
					int end = br.indexOf('\r', idx);
					windir = br.substring(idx, end);
				} else {
					if (sroot != null)
						windir = "WINDIR=" + sroot.substring(11);
				}
				// System.out.println(sroot);
				// System.out.println(windir);
				if (sroot != null)
					globalEnv.add(sroot);
				if (windir != null)
					globalEnv.add(windir);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convert illegal characters into % quoted representation. "foo bar" ->
	 * "foo%20bar" No support for UTF yet.
	 * 
	 * @param url
	 *            the url
	 * @return the escaped url
	 */
	public static String urlEscape(String url) {
		url = url.trim();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < url.length(); ++i) {
			char ch = url.charAt(i);
			if (ch <= 32 || ch > 127 || ILLEGAL.indexOf(ch) >= 0) {
				sb.append("%").append(Integer.toHexString(ch >> 4)).append(Integer.toHexString(ch & 0xf));
			} else
				sb.append(ch);
		}
		return sb.toString();
	}

	private final static String ILLEGAL = "<>'\"+&";

}
