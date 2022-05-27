/******************************************************************************
 * The FileAppender for de.bb.log, a slim and fast Logger implementation.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import de.bb.io.FastBufferedOutputStream;
import de.bb.util.DateFormat;

public class FileAppender extends Appender {
	/** base part of the log file name. */
	private String baseName = "logfile";
	/** the date format string. */
	private String dateFormat;
	/** the log file extension. */
	private String extension = ".log";
	/** current file name. */
	private String currentfileName;
	/** true if date format string is appended after closing. */
	private boolean appendDateAfterClose;

	/** date extension start time. */
	private long start;
	/** date extension increment time. */
	private long increment;
	/** the formatter for the date extension. */
	private DateFormat dateFormatter;
	/** true if the log file is not erased on start. */
	private boolean append;
	/** lookup key for asynchronous I/O. */
	private String key;
	/** current buffer size. */
	private int bufferSize = 0x1000;
	/** own stream handle. */
	private OutputStream fos;

	public FileAppender() {
		next = 0;
		setDateFormat("_yyyyMMdd");
	}

	public void setup(final Map<String, String> attributes) {
		super.setup(attributes);

		append = !"false".equals(attributes.get("append"));

		bufferSize = 0x1000;
		final String sBufferSize = attributes.get("bufferSize");
		if (sBufferSize != null) {
			try {
				bufferSize = Integer.parseInt(sBufferSize);
			} catch (Exception ex) {
				try {
					bufferSize = Integer.parseInt(sBufferSize, 16);
				} catch (Exception ex2) {
				}
			}
		}

		final String baseName = attributes.get("baseName");
		if (baseName != null)
			setBaseName(baseName);

		final String dateFormat = attributes.get("dateFormat");
		if (dateFormat != null) {
			setDateFormat(dateFormat);
		}

		appendDateAfterClose = "false".equalsIgnoreCase(attributes.get("appendDateAfterClose"));
	}

	/**
	 * Set the date format to extend the log file.
	 * 
	 * @param dateFormat2
	 */
	public void setDateFormat(final String dateFormat) {
		this.dateFormat = dateFormat;
		dateFormatter = new DateFormat(dateFormat);
		key = baseName + extension + "#" + dateFormat;

		// calculate the start time
		final String sStart = dateFormatter.format(System.currentTimeMillis());
		start = dateFormatter.parse(sStart);

		// calculate the increment matching the dateFormat
		long l = 1;
		while (dateFormatter.format(start + l).equals(sStart)) {
			l += l;
		}

		// now inc is to large, find the minimum which yields the same result.
		increment = l;
		for (; l > 0; l >>>= 1) {
			if (!dateFormatter.format(start + increment - l).equals(sStart))
				increment -= l;
		}
	}

	/**
	 * Compose the new file name using the current date and open the stream.
	 */
	protected void nextFile() {
		if (os != null) {
			try {
				os.flush();
				os.close();
			} catch (IOException e) {
			}
		}

		if (dateFormatter != null && appendDateAfterClose) {
			final File baseLog = new File(baseName + extension);
			if (baseLog.exists()) {
				final String dt = dateFormatter.format(start);
				String fn = baseName + '_' + dt + extension;
				int i = 0;
				while (new File(fn).exists()) {
					fn = baseName + '_' + dt + "-" + (i++) + extension;
				}
				baseLog.renameTo(new File(fn));
			}
		}

		final long now = System.currentTimeMillis();
		String datePart;
		if (dateFormatter != null) {
			// long time inactivity can result multiple adds of the increment.
			while (start + increment < now) {
				start += increment;
			}
			next = start + increment;
			datePart = dateFormatter.format(start);
		} else {
			datePart = "";
		}
		final int ls = baseName.lastIndexOf('/');
		if (ls > 0) {
			File dir = new File(baseName.substring(0, ls));
			dir.mkdirs();
		}

		if ("*".equals(baseName)) {
			fos = System.out;
			next = Long.MAX_VALUE;
		} else {

			if (appendDateAfterClose) {
				currentfileName = baseName + extension;
			} else {
				currentfileName = baseName + datePart + extension;
			}
			try {
				fos = new FileOutputStream(currentfileName, append);
			} catch (Exception ioe) {
				try {
					fos = new FileOutputStream(currentfileName);
				} catch (Exception ioe2) {
					fos = System.out;
					next = 0;
				}
			}
		}
		if (fos != null)
			os = new FastBufferedOutputStream(fos, bufferSize);
	}

	/**
	 * Identify the logger.
	 * 
	 * @return a key value.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * set the base name.
	 * 
	 * @param baseName the base name.
	 */
	public void setBaseName(final String baseName) {
		next = 0;
		this.baseName = baseName;
		key = baseName + extension + "#" + dateFormat;
	}

	/**
	 * Get the current base name.
	 * 
	 * @return the current base name.
	 */
	public String getBaseName() {
		return baseName;
	}

	/**
	 * Get the append value.
	 * 
	 * @return true if the log file is appended.
	 * @see #setAppend(boolean)
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * Set the log file append mode.
	 * 
	 * @param append true if the log file is appended.
	 * @see #isAppend()
	 */
	public void setAppend(boolean append) {
		this.append = append;
		if (!append && os != null) {
			synchronized (this) {
				flush();
			}
		}
	}

	/**
	 * Get current buffer size.
	 * 
	 * @return the current buffer size.
	 * @see #setBufferSize(int)
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Set the bufferSize.
	 * 
	 * @param bufferSize the new buffer size.
	 * @see #getBufferSize()
	 */
	public synchronized void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
		if (os != null) {
			flush();
			os = new FastBufferedOutputStream(fos, bufferSize);
		}
	}

	/**
	 * Get the current log file extension.
	 * 
	 * @return the current log file extension.
	 * @see #setExtension(String)
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Set th log file extension.
	 * 
	 * @param extension the log file extension
	 * @see #getExtension()
	 */
	public void setExtension(String extension) {
		if (!extension.startsWith("."))
			extension = "." + extension;
		this.extension = extension;
		key = baseName + extension + "#" + dateFormat;
	}

	/**
	 * Get the current date format.
	 * 
	 * @return the current date format or null if none is used.
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * get the current file name.
	 * 
	 * @return the current file name. Can be null if there was no logging yet!
	 */
	public String getCurrentfileName() {
		return currentfileName;
	}

	/**
	 * Return the value of appendDateAfterClose.
	 * 
	 * @return the value of appendDateAfterClose.
	 */
	public boolean isAppendDateAfterClose() {
		return appendDateAfterClose;
	}

	/**
	 * Set the value for appendDateAfterClose.
	 * 
	 * @param appendDateAfterClose If true only the base name is used to log to,
	 *                             plus if the date part changes the current log
	 *                             file is closed and renamed. If false the datePart
	 *                             is appended immediately.
	 */
	public void setAppendDateAfterClose(boolean appendDateAfterClose) {
		this.appendDateAfterClose = appendDateAfterClose;
	}

	/**
	 * Developer friendly toString().
	 */
	public String toString() {
		if (currentfileName == null)
			return "Appender: -> " + key;
		return "Appender: -> " + currentfileName;
	}

}
