/******************************************************************************
 * A slim and fast Logger implementation.
 *
 * Copyright (c) by Stefan Bebbo Franke 2013.
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

package de.bb.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.bb.util.Pair;
import de.bb.util.SessionManager;
import de.bb.util.XmlFile;

/**
 * A slim and fast Logger implementation.
 * 
 * @author bebbo
 */
public class Logger {
	/** log level constants. For the sake of speed use plain integers. */
	public final static int OFF = 0x7fffffff, FATAL = 0x70000000,
			ERROR = 0x60000000, WARN = 0x50000000, INFO = 0x40000000,
			DEBUG = 0x30000000, FINE = 0x20000000, TRACE = 0x10000000, ALL = 0;

	/** lookup name -> log level. */
	private final static HashMap<String, Integer> LEVELS = new HashMap<String, Integer>();
	/** reverse lookup log level -> name. */
	private static final HashMap<Integer, String> RLEVELS = new HashMap<Integer, String>();

	/** all used loggers. */
	private final static HashMap<String, Logger> LOGGERS = new HashMap<String, Logger>();
	/** all appenders,IFormatter pairs. */
	private final static TreeMap<String, Pair<IAppender, IFormatter>> APPENDERS = new TreeMap<String, Pair<IAppender, IFormatter>>();
	/** a IFormatter lookup by format pattern. */
	private final static HashMap<String, IFormatter> FORMATTERS = new HashMap<String, IFormatter>();

	/** for delayed flushing of streams. */
	final static SessionManager<String, IAppender> FLUSHER = new SessionManager<String, IAppender>(
			200L);
	/** use async flush(). */
	static boolean USE_ASYNC;

	/**
	 * Helper class to expand and fill simple message strings.
	 * 
	 * @author bebbo
	 */
	private static class M {
		String[] parts;
		int[] indexes;
	}

	/**
	 * Lookup from message to helper data M.
	 */
	final static Map<String, M> M2M = new ConcurrentHashMap<String, Logger.M>();
	/** dummy, used in toArray(). */
	private static final String[] EMPTYS = {};

	/** the used IFormatters for this Logger instance. */
	private IFormatter[] iFormatters;
	/** the used appenders for this Logger instance. */
	private ArrayList<Pair<IAppender, Integer>> appenders;

	/** new IFormatters for live configuration. */
	private IFormatter[] pendingIFormatters;
	/** new appenders for live configuration. */
	private ArrayList<Pair<IAppender, Integer>> pendingAppenders = new ArrayList<Pair<IAppender, Integer>>();
	/** indicates a new configuration. */
	private boolean apply = true;

	/** the current log level; */
	private int level = INFO;

	/** the path for this logger. */
	private String path;
	/** the configured appenders for this logger. */
	private String appenderConfig;

	/**
	 * Create a new Logger.
	 * 
	 * @param path
	 *            the path to locate the configuration - search towards root.
	 */
	public Logger(final String path) {
		this.path = path;
		iFormatters = new IFormatter[0];
		appenders = new ArrayList<Pair<IAppender, Integer>>();
	}

	/**
	 * Return the path/name of the logger.
	 * 
	 * @return the path/name of the logger.
	 */
	public String getName() {
		return path;
	}

	/**
	 * Log with the message using the provided level String and an optional
	 * Throwable.
	 * 
	 * Usually not invoked directly.
	 * 
	 * @param level
	 *            the level String e.g. "DEBUG"
	 * @param message
	 *            the log message
	 * @param a
	 *            an array with objects
	 */
	public final void log(final String level, String message, final Object[] o) {
		if (apply) {
			iFormatters = pendingIFormatters;
			appenders = pendingAppenders;
			apply = false;
		}

		Throwable t = null;
		if (o.length > 0) {
			if (o[o.length - 1] instanceof Throwable)
				t = (Throwable) o[o.length - 1];
			if (t == null || o.length > 1)
				message = expand(message, o);
		}
		final long now = System.currentTimeMillis();

		// no measurable gain?
		// if (IFormatters.length == 1) {
		// final String formatted = IFormatters[0].format(now, level, path,
		// message, t);
		// for (final Pair<IAppender, Integer> p : appenders) {
		// final IAppender a = p.getFirst();
		// a.write(now, formatted);
		// }
		// } else {
		final ArrayList<String> messages = new ArrayList<String>();
		for (int i = 0; i < iFormatters.length; ++i) {
			messages.add(iFormatters[i].format(now, level, path, message, t));
		}

		for (final Pair<IAppender, Integer> p : appenders) {
			final IAppender a = p.getFirst();
			a.write(now, messages.get(p.getSecond()));
		}
		// }
	}

	/**
	 * Get the log level.
	 * 
	 * @return the log level.
	 * @see #setLogLevel()
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Set the log level.
	 * 
	 * @param level
	 *            the log level
	 * @see #getLevel()
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * Log a TRACE message plus a Throwable.
	 * 
	 * @param message
	 *            the message.
	 * @param o
	 *            the optional parameters.
	 */
	public void trace(final String message, final Object... o) {
		if (level > DEBUG)
			return;
		log("DEBUG", message, o);
	}

	/**
	 * Return true if at least TRACE is enabled.
	 * 
	 * @return true if at least TRACE is enabled.
	 */
	public boolean isTrace() {
		return level <= TRACE;
	}

	/**
	 * Log a FINE message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void fine(String message, final Object... a) {
		if (level > FINE)
			return;
		log("FINE", message, a);

	}

	/**
	 * Return true if at least FINE is enabled.
	 * 
	 * @return true if at least FINE is enabled.
	 */
	public boolean isFine() {
		return level <= FINE;
	}

	/**
	 * Log a DEBUG message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void debug(String message, final Object... a) {
		if (level > DEBUG)
			return;
		log("DEBUG", message, a);
	}

	/**
	 * Return true if at least DEBUG is enabled.
	 * 
	 * @return true if at least DEBUG is enabled.
	 */
	public boolean isDebug() {
		return level <= DEBUG;
	}

	/**
	 * Log a INFO message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void info(String message, final Object... a) {
		if (level > INFO)
			return;
		log("INFO", message, a);
	}

	/**
	 * Return true if at least INFO is enabled.
	 * 
	 * @return true if at least INFO is enabled.
	 */
	public boolean isInfo() {
		return level <= INFO;
	}

	/**
	 * Log a WARN message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void warn(String message, final Object... a) {
		if (level > WARN)
			return;
		log("WARN", message, a);
	}

	/**
	 * Return true if at least WARN is enabled.
	 * 
	 * @return true if at least WARN is enabled.
	 */
	public boolean isWarn() {
		return level <= WARN;
	}

	/**
	 * Log a ERROR message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void error(String message, final Object... a) {
		if (level > ERROR)
			return;
		log("ERROR", message, a);
	}

	/**
	 * Return true if at least ERROR is enabled.
	 * 
	 * @return true if at least ERROR is enabled.
	 */
	public boolean isError() {
		return level <= ERROR;
	}

	/**
	 * Log a FATAL message.
	 * 
	 * If optional parameters a supplied the message is parsed to indexes like
	 * {0} to expand the message.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            optional parameters
	 */
	public void fatal(String message, final Object... a) {
		if (level > FATAL)
			return;
		log("FATAL", message, a);
	}

	/**
	 * Return true if at least FATAL is enabled.
	 * 
	 * @return true if at least FATAL is enabled.
	 */
	public boolean isFatal() {
		return level <= FATAL;
	}

	/**
	 * Add a appender/formatter pair to the pending configuration.
	 * 
	 * @param a
	 *            a IAppender
	 * @param f
	 *            a IFormatter
	 */
	public final void addAppenderFormatter(final IAppender a, final IFormatter f) {
		if (pendingIFormatters == null)
			pendingIFormatters = new IFormatter[0];
		int index = 0;
		while (index < pendingIFormatters.length) {
			if (pendingIFormatters[index] == f)
				break;
			++index;
		}
		if (index == pendingIFormatters.length) {
			pendingIFormatters = Arrays.copyOf(pendingIFormatters, index + 1);
			pendingIFormatters[index] = f;
		}
		if (pendingAppenders == null)
			pendingAppenders = new ArrayList<Pair<IAppender, Integer>>();
		pendingAppenders.add(Pair.makePair(a, index));
	}

	/**
	 * Load the appenders for this logger. Start from the loggers path and
	 * search towards root for an appender configuration.
	 */
	private void loadAppenders() {
		final Logger rootLogger = LOGGERS.get("");
		pendingAppenders = null;
		pendingIFormatters = null;
		String app = null;
		{
			String p = this.path;
			for (;;) {
				final Logger l = LOGGERS.get(p);
				if (l != null && l.appenderConfig != null) {
					app = l.appenderConfig;
					setLevel(l.level);
					break;
				}
				final int dot = p.lastIndexOf('.');
				if (dot < 0)
					break;
				p = p.substring(0, dot);
			}
		}
		if (app != null) {
			for (final StringTokenizer st = new StringTokenizer(app, " \r\n\t,"); st
					.hasMoreElements();) {
				final String name = st.nextToken();
				final Pair<IAppender, IFormatter> p = APPENDERS.get(name);
				if (p == null) {
					rootLogger.error("no appender named: " + name);
					continue;
				}
				addAppenderFormatter(p.getFirst(), p.getSecond());
			}
		}
		if (pendingAppenders == null) {
			pendingAppenders = rootLogger.pendingAppenders;
			pendingIFormatters = rootLogger.pendingIFormatters;
			setLevel(rootLogger.level);
		}
		apply = true;
	}

	/**
	 * User friendly toString().
	 */
	public String toString() {
		return "Logger: " + path + " " + RLEVELS.get(level) + " -> "
				+ appenders + Arrays.toString(iFormatters);
	}

	/**
	 * Expand a pattern message with the provided parameters.
	 * 
	 * A ConcurrentHashMap is used to keep the information to expand a message
	 * with its parameters.
	 * 
	 * @param message
	 *            the message.
	 * @param a
	 *            the array containing the parameters.
	 * @return the expanded message.
	 */
	private static String expand(final String message, Object[] a) {
		M m = M2M.get(message);

		// create a new message helper object.
		if (m == null) {
			final ArrayList<String> p = new ArrayList<String>();
			final ArrayList<Integer> ii = new ArrayList<Integer>();

			String q = message;
			for (int bra = q.indexOf('{'); bra >= 0; bra = q.indexOf('{', bra)) {
				// check for "{{" -> "{"
				if (bra + 1 < q.length() && q.charAt(bra + 1) == '{') {
					q = q.substring(0, bra) + q.substring(bra + 1);
					++bra;
					continue;
				}
				p.add(q.substring(0, bra));

				++bra;
				int ket = q.indexOf('}', bra);
				if (ket < 0) {
					p.remove(p.size() - 1);
					p.add(q);
					break;
				}
				final String ns = q.substring(bra, ket);
				try {
					int n = Integer.parseInt(ns);
					ii.add(n);
				} catch (Exception e) {
					ii.add(0);
				}
				q = q.substring(ket + 1);
				bra = 0;
			}
			if (q.length() > 0)
				p.add(q);

			// fill the helper object
			m = new M();
			m.parts = p.toArray(EMPTYS);
			m.indexes = new int[ii.size()];
			for (int i = 0; i < ii.size(); ++i) {
				m.indexes[i] = ii.get(i);
			}
			M2M.put(message, m);
		}

		// expand the message
		final StringBuilder sb = new StringBuilder();
		int i = 0;
		for (; i < m.indexes.length; ++i) {
			sb.append(m.parts[i]);
			final int index = m.indexes[i];
			if (index < a.length) {
				sb.append(a[index]);
			} else {
				sb.append("{").append(index).append("}");
			}
		}
		if (i < m.parts.length)
			sb.append(m.parts[i]);
		return sb.toString();
	}

	/**
	 * Get a Logger using a class name.
	 * 
	 * @param clazz
	 *            a class to provide a name
	 * @return the Logger instance.
	 */
	public static Logger getLogger(final Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	/**
	 * Get a Logger using a path.
	 * 
	 * @param path
	 *            the path for the logger
	 * @return the Logger instance.
	 */
	public static synchronized Logger getLogger(final String path) {
		if (path != null) {
			Logger logger = LOGGERS.get(path);
			if (logger != null)
				return logger;

			logger = new Logger(path);
			LOGGERS.put(path, logger);
			logger.loadAppenders();
			return logger;
		}
		return LOGGERS.get("");
	}

	/**
	 * (re-)load the configuration.
	 * 
	 * @throws IOException
	 */
	public static synchronized void loadConfig() throws IOException {
		if (LOGGERS.isEmpty()) {
			final Logger logger = new Logger("<root>");
			logger.addAppenderFormatter(new Appender(System.out),
					getUniqueFormatter(new Formatter()));
			logger.apply = true;
			LOGGERS.put("", logger);
		}
		final Logger rootLogger = LOGGERS.get("");

		// reset current config
		FORMATTERS.clear();
		APPENDERS.clear();

		final ClassLoader cl = Logger.class.getClassLoader();
		InputStream is = null;
		try {
			is = cl.getResourceAsStream("bblog.xml");

			if (is == null && rootLogger.isWarn())
				rootLogger.warn("no file: 'bblog.xml' found");

			final XmlFile xml = new XmlFile();
			if (is != null)
				xml.read(is);

			final String stimeout = xml.getString("/log", "timeout", "100");
			try {
				final long timeoutMilli = Long.parseLong(stimeout);
				FLUSHER.setTimeout(timeoutMilli);
				USE_ASYNC = timeoutMilli > 0;
			} catch (Exception ex) {
				rootLogger.error("invalid timeout: " + stimeout);
			}

			// read IFormatters
			for (final Iterator<String> i = xml.sections("/log/formatter"); i
					.hasNext();) {
				final String key = i.next();

				final Map<String, String> attributes = xml.getAttributes(key);

				final String name = attributes.get("name");
				if (name == null) {
					rootLogger.error("missing name in formatter: " + key
							+ " using " + attributes);
					continue;
				}

				final String className = attributes.get("class");

				IFormatter f;
				if (className == null) {
					f = new Formatter();
				} else {
					try {
						f = (IFormatter) Class.forName(className).newInstance();
					} catch (Exception ex) {
						rootLogger.error("can't load class : " + className, ex);
						continue;
					}
				}
				f.setup(attributes);
				f = getUniqueFormatter(f);

				FORMATTERS.put(name, f);
			}

			// read appenders
			for (final Iterator<String> i = xml.sections("/log/appender"); i
					.hasNext();) {
				final String key = i.next();

				final Map<String, String> attributes = xml.getAttributes(key);

				final String name = attributes.get("name");
				if (name == null) {
					rootLogger.error("missing name in appender: " + key
							+ " using " + attributes);
					continue;
				}

				final String className = attributes.get("class");

				IAppender a;
				if (className == null) {
					a = new FileAppender();
				} else {
					try {
						a = (IAppender) Class.forName(className).newInstance();
					} catch (Exception ex) {
						rootLogger.error("can't load class : " + className, ex);
						continue;
					}
				}

				a.setup(attributes);

				final String formatterName = attributes.get("formatter");
				IFormatter f = FORMATTERS.get(formatterName);
				if (f == null)
					f = getUniqueFormatter(new Formatter());

				APPENDERS.put(name, Pair.makePair(a, f));
			}

			// reset previous read appenders
			for (final Logger l : LOGGERS.values()) {
				l.appenderConfig = null;
			}

			// read loggers and set level
			for (final Iterator<String> i = xml.sections("/log/logger"); i
					.hasNext();) {
				final String key = i.next();
				final String path = xml.getString(key, "name", "");
				try {
					Logger logger = LOGGERS.get(path);
					if (logger == null) {
						logger = new Logger(path);
						LOGGERS.put(path, logger);
					}

					final String levelString = xml.getString(key, "level",
							"DEBUG").toUpperCase();
					int level = toLevel(levelString);
					logger.setLevel(level);

					final String appender = xml
							.getString(key, "appender", null);
					logger.appenderConfig = appender;
				} catch (Exception ex) {
					if (rootLogger.isDebug())
						rootLogger.debug("initialize logger '" + path
								+ "' failed", ex);
				}
			}
		} catch (Exception ex) {
			rootLogger.warn(ex.getMessage(), ex);
		} finally {
			if (is != null)
				is.close();
		}

		// reset root logger
		rootLogger.pendingAppenders = null;
		rootLogger.pendingIFormatters = null;

		// load root logger into temp
		LOGGERS.put("<root>", rootLogger);
		rootLogger.loadAppenders();
		LOGGERS.remove("<root>");

		if (rootLogger.pendingAppenders == null) {
			rootLogger.addAppenderFormatter(new Appender(System.out),
					getUniqueFormatter(new Formatter()));
			rootLogger.apply = true;
		}

		// // initialize all loggers w/o root
		for (final Entry<String, Logger> e : LOGGERS.entrySet()) {
			String path = e.getKey();
			if (path.length() == 0)
				continue;

			final Logger l = e.getValue();
			l.loadAppenders();
			l.apply = true;
		}
	}

	/**
	 * Convert the level string int the level value.
	 * 
	 * @param levelString
	 *            the level string.
	 * @return the level value. If no match is found INFO is returned.
	 */
	public static int toLevel(final String levelString) {
		final Integer level = LEVELS.get(levelString);
		if (level != null)
			return level;

		return INFO;
	}

	/**
	 * Ensure single instances for identical IFormatters.
	 * 
	 * @param IFormatter
	 * @return
	 */
	private static IFormatter getUniqueFormatter(final IFormatter IFormatter) {
		final IFormatter found = FORMATTERS.get(IFormatter.toString());
		if (found != null)
			return found;

		FORMATTERS.put(IFormatter.toString(), IFormatter);
		return IFormatter;
	}

	/**
	 * Get all current loggers.
	 * 
	 * @return An ArrayList containing all Loggers
	 */
	public static synchronized ArrayList<Logger> getCurrentLoggers() {
		final ArrayList<Logger> al = new ArrayList<Logger>();
		al.addAll(LOGGERS.values());
		return al;
	}

	/**
	 * Get the root logger.
	 * 
	 * @return the root logger.
	 */
	public static synchronized Logger getRootLogger() {
		return LOGGERS.get("");
	}

	/**
	 * Initialize the log level name -> log level value map, then load the
	 * configuaration.
	 */
	static {
		LEVELS.put("OFF", 0x7fffffff);
		LEVELS.put("FATAL", 0x70000000);
		LEVELS.put("ERROR", 0x60000000);
		LEVELS.put("WARN", 0x50000000);
		LEVELS.put("INFO", 0x40000000);
		LEVELS.put("DEBUG", 0x30000000);
		LEVELS.put("FINE", 0x20000000);
		LEVELS.put("TRACE", 0x10000000);
		LEVELS.put("ALL", 0);
		for (final Entry<String, Integer> e : LEVELS.entrySet()) {
			RLEVELS.put(e.getValue(), e.getKey());
		}
		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void trace(final Supplier<String> msgSup) {
		if (level > TRACE)
			return;
		log("TRACE", msgSup.get(), EMPTYS);
	}

	public void fine(final Supplier<String> msgSup) {
		if (level > FINE)
			return;
		log("FINE", msgSup.get(), EMPTYS);
	}

	public void debug(final Supplier<String> msgSup) {
		if (level > DEBUG)
			return;
		log("DEBUG", msgSup.get(), EMPTYS);
	}

	public void info(final Supplier<String> msgSup) {
		if (level > INFO)
			return;
		log("INFO", msgSup.get(), EMPTYS);
	}

	public void warn(final Supplier<String> msgSup) {
		if (level > WARN)
			return;
		log("WARN", msgSup.get(), EMPTYS);
	}

	public void error(final Supplier<String> msgSup) {
		if (level > ERROR)
			return;
		log("ERROR", msgSup.get(), EMPTYS);
	}

	public void fatal(final Supplier<String> msgSup) {
		if (level > FATAL)
			return;
		log("FATAL", msgSup.get(), EMPTYS);
	}

	public void trace(final Supplier<String> msgSup, final Throwable t) {
		if (level > TRACE)
			return;
		log("TRACE", msgSup.get(), new Object[]{t});
	}

	public void fine(final Supplier<String> msgSup, final Throwable t) {
		if (level > FINE)
			return;
		log("FINE", msgSup.get(), new Object[]{t});
	}

	public void debug(final Supplier<String> msgSup, final Throwable t) {
		if (level > DEBUG)
			return;
		log("DEBUG", msgSup.get(), new Object[]{t});
	}

	public void info(final Supplier<String> msgSup, final Throwable t) {
		if (level > INFO)
			return;
		log("INFO", msgSup.get(), new Object[]{t});
	}

	public void warn(final Supplier<String> msgSup, final Throwable t) {
		if (level > WARN)
			return;
		log("WARN", msgSup.get(), new Object[]{t});
	}

	public void error(final Supplier<String> msgSup, final Throwable t) {
		if (level > ERROR)
			return;
		log("ERROR", msgSup.get(), new Object[]{t});
	}

	public void fatal(final Supplier<String> msgSup, final Throwable t) {
		if (level > FATAL)
			return;
		log("FATAL", msgSup.get(), new Object[]{t});
	}
}
