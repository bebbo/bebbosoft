/******************************************************************************
 * The Formatter for de.bb.log, a slim and fast Logger implementation.
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

import java.util.Map;

import de.bb.util.DateFormat;

/**
 * A message formatter implementation. Formats the date and the message line.
 * 
 * @author bebbo
 * 
 */
public class Formatter implements IFormatter {
    /** used to xml escape strings. */
    private static String[] CHARS;
    /** line feed format. */
    private static String LF;

    /** default date format string. */
    public static final String DEFAULT_DATE = "yyyy-MM-dd HH:mm:ss.SSS";
    /** default log format string. */
    public static final String DEFAULT_LOG = "%d %p [%t] %C - %m";

    /** the used date format string. */
    private String dateFormat;
    /** the used DateFormat instance. The bb.util.DateFormat is reentrant! */
    private DateFormat dateFormatter;
    /** the used log format. */
    private char[] logFormat;

    private volatile long lastDate;
    private volatile String lastFormattedDate;

    private boolean escape;

    /**
     * Internal used constructor for default loggers.
     */
    public Formatter() {
        setDateFormat(DEFAULT_DATE);
        setLogFormat(DEFAULT_LOG);
    }

    public void setup(final Map<String, String> attributes) {
        final String dateFormat = attributes.get("dateFormat");
        if (dateFormat != null)
            setDateFormat(dateFormat);

        final String logFormat = attributes.get("format");
        if (logFormat != null)
            setLogFormat(logFormat);

        escape = "true".equals(attributes.get("escape"));
    }

    /**
     * Format the log line.
     * 
     * @param now
     *            current time.
     * @param logLevel
     *            log level string
     * @param loggerName
     *            the logger's name
     * @param message
     *            the log message
     * @param throwable
     *            a Throwable
     * @return the formatted message.
     */
    public String format(final long now, final String logLevel, final String loggerName, final String message,
            Throwable throwable) {
        final StringBuilder sb = new StringBuilder();

        Thread thread = null;
        StackTraceElement st = null;
        for (int i = 0; i < logFormat.length; ++i) {
            final char ch = logFormat[i];
            if (ch == '%') {
                if (++i == logFormat.length)
                    break;
                switch (logFormat[i]) {
                case '%':
                    sb.append("%");
                    continue;
                case 'd':
                    if (now == lastDate) {
                        sb.append(lastFormattedDate);
                        continue;
                    }
                    final String sd = dateFormatter.format(now);
                    sb.append(sd);
                    lastDate = now;
                    lastFormattedDate = sd;
                    continue;
                case 'p':
                    sb.append(logLevel);
                    continue;
                case 't':
                    if (thread == null)
                        thread = Thread.currentThread();
                    sb.append(thread.getName());
                    continue;
                case 'C':
                    sb.append(loggerName);
                    continue;
                case 'm':
                    sb.append(escape ? escape(message) : message);
                    continue;
                case 'F':
                    if (st == null) {
                        if (thread == null)
                            thread = Thread.currentThread();
                        st = thread.getStackTrace()[5];
                    }
                    sb.append(st.getFileName());
                    continue;
                case 'L':
                    if (st == null) {
                        if (thread == null)
                            thread = Thread.currentThread();
                        st = thread.getStackTrace()[5];
                    }
                    sb.append(Integer.toString(st.getLineNumber()));
                    continue;
                }
            } else {
                sb.append(ch);
            }
        }
        sb.append(LF);
        if (throwable != null) {
            for (;;) {
                sb.append(escape ? escape(throwable.toString()) : throwable.toString()).append(LF);
                final StackTraceElement[] stackTraceElements = throwable.getStackTrace();
                for (final StackTraceElement ste : stackTraceElements) {
                    sb.append("\tat ").append(ste).append(LF);
                }
                final Throwable c = throwable.getCause();
                if (c == null || c == throwable)
                    break;
                throwable = c;
                sb.append("caused by ");
            }
        }
        return sb.toString();
    }

    /**
     * provide a unique ID to avoid double formatting the same message.
     * 
     * @return
     */
    public String toString() {
        return Formatter.class.getName() + "|" + dateFormat + "|" + new String(logFormat);
    }

    /**
     * Set a new date format.
     * 
     * @param dateFormat
     *            the date format.
     * @see #getDateFormat()
     */
    public void setDateFormat(final String dateFormat) {
        this.dateFormat = dateFormat;
        dateFormatter = new DateFormat(dateFormat);
    }

    /**
     * Ge the date format.
     * 
     * @return the dare format.
     * @see #setDateFormat(String)
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Set a new log format.
     * 
     * @param logFormat
     *            the log format.
     * @see #getLogFormat()
     */
    public void setLogFormat(final String logFormat) {
        this.logFormat = new char[logFormat.length()];
        logFormat.getChars(0, this.logFormat.length, this.logFormat, 0);
    }

    /**
     * Get the log format.
     * 
     * @return the log format.
     * @see #setLogFormat(String)
     */
    public String getLogFormat() {
        return new String(logFormat);
    }

    /**
     * Escape the XML characters
     * 
     * @param text
     *            the text to escape
     * @return the escaped text
     */
    public static String escape(final String text) {
    	if (text == null)
    		return "";
        final int l = text.length();
        for (int i = 0; i < l; ++i) {
            char ch = text.charAt(i);
            if (ch < CHARS.length && CHARS[ch] != null) {
                final StringBuilder sb = new StringBuilder();
                sb.append(text.substring(0, i));
                sb.append(CHARS[ch]);
                while (++i < l) {
                    ch = text.charAt(i);
                    if (ch < CHARS.length && CHARS[ch] != null) {
                        sb.append(CHARS[ch]);
                    } else {
                        sb.append(ch);
                    }
                }
                return sb.toString();
            }
        }
        return text;
    }

    static {
        CHARS = new String['>' + 1];
        CHARS['<'] = "&lt;";
        CHARS['>'] = "&gt;";
        CHARS['&'] = "&amp;";
        CHARS['\''] = "&#39;";
        CHARS['"'] = "&quot;";

        LF = System.getProperty("line.separator");
    }
}
