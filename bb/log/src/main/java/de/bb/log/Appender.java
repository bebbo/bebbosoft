/******************************************************************************
 * The base Appender for de.bb.log, a slim and fast Logger implementation.
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import de.bb.util.ByteUtil;
import de.bb.util.SessionManager.Callback;

/**
 * Base class for all log appenders.
 * 
 * @author bebbo
 * 
 */
public class Appender implements IAppender, Callback {
    /** the stream to write to. */
    protected OutputStream os;
    /** a time stamp used with rotating log files. */
    protected long next = Long.MAX_VALUE;

    /** a time value to reduce the update frequency of the FLUSHER. */
    private long nextFlush;

    private String name = "<root>";

    /**
     * For derived classes. Logs to stdout by default.
     */
    protected Appender() {
    }

    Appender(final PrintStream ps) {
        os = ps;
    }

    
    public void setup(Map<String, String> attributes) {
        final String name = attributes.get("name");
        if (name != null)
            this.name = name;
    }

    /**
     * writes the log message.
     * 
     * @param now
     *            current time, used to rotate files.
     * @param message
     *            the log message
     */
    public void write(final long now, final String message) {
        synchronized (this) {
            if (now > next)
                nextFile();
            try {
                ByteUtil.writeString(message, os);
            } catch (Exception e) {
                System.err.println(e);
            }
            if (Logger.USE_ASYNC && now > nextFlush) {
                Logger.FLUSHER.put(getKey(), this);
                nextFlush = now + Logger.FLUSHER.getTimeout() / 2;
            } else {
                flush();
            }
        }
    }

    /**
     * Flush the stream.
     */
    protected void flush() {
        try {
            os.flush();
        } catch (IOException e) {
        }
    }

    /**
     * Dummy method, used in derived classes to rotate file.
     */
    protected void nextFile() {
    }

    /**
     * return an unique key.
     * 
     * @return "*" for the default implementation
     */
    String getKey() {
        return "*";
    }

    /**
     * Used to flush asynchronous if configured to.
     */
    
    public boolean dontRemove(Object key) {
        flush();
        return false;
    }

    /**
     * Developer friendly toString().
     */
    public String toString() {
        return "Appender " + name + " : STDOUT";
    }

	@Override
	protected void finalize() throws Throwable {
		flush();
	}
    
    
}
