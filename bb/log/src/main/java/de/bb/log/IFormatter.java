/******************************************************************************
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

public interface IFormatter {
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
	 *            a <code>Throwable</code> or null
	 * @return the formatted message.
	 */
	public String format(final long now, final String logLevel,
			final String loggerName, final String message,
			final Throwable throwable);

	/**
	 * Initialize the formatter with its attributes read from xml configuration.
	 * 
	 * @param attributes
	 *            the attribute map - name->value
	 */
	public void setup(Map<String, String> attributes);
}
