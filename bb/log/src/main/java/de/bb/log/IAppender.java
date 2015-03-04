package de.bb.log;

import java.util.Map;

public interface IAppender {
    /**
     * Write the log message.
     * 
     * @param now
     *            current time, e.g. used to rotate files.
     * @param message
     *            the formatted log message
     */
    public void write(final long now, final String message);

    /**
     * Initialize the appender with its attributes read from xml configuration.
     * 
     * @param attributes
     *            the attribute map - name->value
     */
    public void setup(Map<String, String> attributes);
}
