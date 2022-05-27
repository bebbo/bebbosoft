package de.bb.tools.ant;
import java.util.HashMap;

import org.apache.tools.ant.taskdefs.Sequential;

/**
 * ANT Task to synchronize build tasks.
 * Use this ant task inside parallel tasks.
 * 
 * &lt;taskdef name="synchronized" classname="de.bb.tools.ant.SyncTask"/&gt;
 * ...
 * &lt;target name="test"&gt;
 *   &lt;synchronized id="junit" /&gt;
 *     &lt;junit haltonfailure="on" printsummary="on" fork="yes" forkmode="once"&gt;
 *       ...
 *     &lt;/junit&gt;
 *   &lt;/synchronized&gt;
 * &lt;/target&gt;
 * 
 * @author stefan franke
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2011.
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
 */
public class SyncTask extends Sequential {
    // serialize the access to the LOCKS HashMap
    private final static Object SERIALLOCK = new Object();
    // keep objects for different locks
    private final static HashMap<String, Object> LOCKS = new HashMap<String, Object>();
    // the default lock
    private final static Object NULLLOCK = new Object();
    // the id
    private String id = null;

    /**
     * Implements the ANT execute method.
     * This method will obtain the lock, then process the inner tasks and finally release the lock.
     */
    public void execute() {
        final Object lock;
        // obtain the lock from the HashMap
        if (id != null) {
            synchronized (SERIALLOCK) {
                Object l = LOCKS.get(id);
                if (l == null) {
                    // create it on first access
                    l = new Object();
                    LOCKS.put(id, l);
                }
                lock = l;
            }
        } else {
        // or use the default lock
            lock = NULLLOCK;
        }
        // here we go synchronized
        synchronized (lock) {
            super.execute();
        }
    }

    /**
     * Supply an ID for a named lock.
     * @param the id for the lock
     */
    public void setId(String id) {
        this.id = id;
    }
}