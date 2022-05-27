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

package de.bb.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.bb.util.SessionManager.Callback;

/**
 * Provides simple log facilities. These are:
 * <ul>
 * <li>Write a message into the log file.</li>
 * <li>Write a message with timestamp into the log file.</li>
 * <li>Create each day a new log file, using the file name and the encoded date.</li>
 * </ul>
 */
public class LogFile implements Callback {

    private final static long INC = 1000L * (60 * 60 * 24);
    private static final String YYYYMMDD = "yyyyMMdd";

    private final static SessionManager<String, LogFile> FLUSHER = new SessionManager<String, LogFile>(2000L);

    private static final byte[] space3 = {32, 32, 32};
    private static final byte[] crlf = {0xd, 0xa};

    // base part of file
    private String baseName;
    // the used file
    private OutputStream fos;
    // next time to switch file name
    private long next;
    // start time
    private long start;
    private DateFormat dateFormat;
    private long nextFlush;

    /**
     * Create a <code>LogFile</code> using the specified base name. The actual date is always added to the base name,
     * and each day a new log file is created. If * is used as base name, stdout is used for output.
     * 
     * @param bn
     *            the log files base name
     */
    public LogFile(String bn) {
        this(bn, null);
    }

    /**
     * Create a <code>LogFile</code> using the specified base name. The actual date is always added to the base name,
     * and each day a new log file is created. If * is used as base name, stdout is used for output.
     * 
     * @param bn
     *            the log files base name
     * @param fmt
     *            the date format which is appended to the basename
     */
    public LogFile(String bn, String fmt) {
        baseName = bn;
        if (fmt == null)
            fmt = YYYYMMDD;
        dateFormat = new DateFormat(fmt);
        final String dt = dateFormat.format(System.currentTimeMillis());
        start = DateFormat.parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz(dt);
        nextFile();
    }

    private void nextFile() {
        if ("*".equals(baseName)) {
            fos = System.out;
            return;
        }
        if (fos != null) {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
            }
            final String dt = dateFormat.format(start);
            String fn = baseName + '_' + dt + ".log";
            int i = 0;
            while (new File(fn).exists()) {
                fn = baseName + '_' + dt + "-" + (i++) + ".log";
            }
            new File(baseName + ".log").renameTo(new File(fn));
        }

        final long now = System.currentTimeMillis();
        while (start + INC < now) {
            start += INC;
        }
        next = start + INC;
        int ls = baseName.lastIndexOf('/');
        if (ls > 0) {
            File dir = new File(baseName.substring(0, ls));
            dir.mkdirs();
        }
        
        String fn = baseName + ".log";
        try {
            fos = new FileOutputStream(fn, true);
        } catch (Exception ioe) {
            try {
                fos = new FileOutputStream(fn);
            } catch (Exception ioe2) {
                fos = System.out;
                next = 0;
            }
        }
        if (fos != null)
            fos = new BufferedOutputStream(fos, 0x4000);
    }

    /**
     * Append a message to the log file and add a date/time stamp.
     * 
     * @param msg
     *            the message
     */
    public void writeDate(String msg) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            String n = DateFormat.dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(now);
            try {
                if (now > next)
                    nextFile();

                ByteUtil.writeString(n, fos);
                fos.write(space3);
                ByteUtil.writeString(msg, fos);
                fos.write(crlf);

            } catch (Exception e) {
                System.out.println(n + "   " + msg);
            }
        }
        FLUSHER.put(baseName, this);
    }

    /**
     * Append a message to the log file.
     * 
     * @param msg
     *            the message
     */
    public void writeln(String msg) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            try {
                if (now > next)
                    nextFile();

                ByteUtil.writeString(msg, fos);
                fos.write(crlf);

            } catch (Exception e) {
            }
        }
        FLUSHER.put(baseName, this);
    }

    /**
     * Append a message to the log file.
     * 
     * @param msg
     *            the message
     */
    public void writeln(byte[] msg) {
        long now = System.currentTimeMillis();
        synchronized (this) {
            try {
                if (now > next)
                    nextFile();

                fos.write(msg);
                fos.write(crlf);

            } catch (Exception e) {
            }
        }
        if (now > nextFlush) {
            FLUSHER.put(baseName, this);
            nextFlush = now + 1000L;
        }
    }

    /**
     * Append a message to the log file.
     * 
     * @param msg
     *            the message
     */
    public void write(String msg) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            try {
                if (now > next)
                    nextFile();
                ByteUtil.writeString(msg, fos);
            } catch (Exception e) {
            }
        }
        FLUSHER.put(baseName, this);
    }

    public boolean dontRemove(Object key) {
        flush();
        return false;
    }

    public synchronized void flush() {
        try {
            fos.flush();
        } catch (IOException e) {
        }
    }
}
