/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import de.bb.util.LRUCache;

public class Log {
    private static PrintStream out = System.out;
    private static LRUCache<Thread, Log> LOGS = new LRUCache<Thread, Log>();
    private static LinkedList<Log> LOCK = new LinkedList<Log>();
    private volatile static Thread thread;
    public static boolean DEBUG;
    public static boolean WARN = false;
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private PrintStream ps = new PrintStream(bos);
    private Thread t;

    private Log() {
        t = Thread.currentThread();
    }

    public void debug(String msg) {
        if (!DEBUG)
            return;

        println("[DEBUG] " + msg);
    }

    public void warn(String msg) {
        if (!WARN)
            return;

        println("[WARN] " + msg);
    }

    public void error(String msg) {

        println("[ERROR] " + msg);
    }

    public void info(String msg) {

        println("[INFO] " + msg);
    }

    private void println(String msg) {
        ps.println(msg);
        synchronized (LOCK) {
            if (hasIt) {
                out.print("\b");
                hasIt = false;
            }
            if (thread == null || thread == t) {
                thread = t;
                flushBos();
            }
        }
    }

    private void flushBos() {
        ps.flush();
        if (bos.size() > 0) {
            try {
                out.write(bos.toByteArray());
            } catch (IOException e) {
            }
            bos.reset();
        }
    }

    public synchronized void flush() {
        synchronized (LOCK) {
            if (thread != null && !thread.isAlive())
                thread = null;
            if (thread == null || thread == t) {
                while (LOCK.size() > 0) {
                    Log log = LOCK.removeFirst();
                    log.flushBos();
                }
                flushBos();
                thread = null;
                return;
            }
            // can't write now --> queue
            LOGS.remove(t);
            t = null;
            LOCK.add(this);
        }
    }

    public void close() {
        flush();
        if (t != null) {
            LOGS.remove(t);
            t = null;
        }
    }

    public static Log getLog() {
        synchronized (LOCK) {
            Thread t = Thread.currentThread();
            Log log = LOGS.get(t);
            if (log == null) {
                log = new Log();
                LOGS.put(t, log);
            }
            return log;
        }
    }

    private static String ALIVE = "|/-\\";
    private static int N = 0;
    private static boolean hasIt;

    public static void toggle() {
        synchronized (LOCK) {
            if (hasIt)
                out.print("\b");
            out.print(ALIVE.charAt(N));
            out.flush();
            hasIt = true;
            N = (N + 1) & 3;
        }
    }

    public static void setPrintStream(PrintStream newOut) {
        out = newOut;
    }

    public static void clear() {
        synchronized (LOGS) {
            for (Iterator<Entry<Thread, Log>> i = LOGS.entrySet().iterator(); i.hasNext();) {
                Entry<Thread, Log> e = i.next();
                e.getValue().close();
                i.remove();
            }
            LOGS.clear();
        }
    }

}
