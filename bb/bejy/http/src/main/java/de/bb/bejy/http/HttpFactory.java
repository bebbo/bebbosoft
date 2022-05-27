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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.bejy.Config;
import de.bb.bejy.Configurable;
import de.bb.util.LogFile;

public class HttpFactory extends de.bb.bejy.Factory {
    Hashtable hosts = new Hashtable();

    //  private static Hashtable mimes = new Hashtable();
    //  private LogThread logThread;

    /**
     * Create a new HTTP protocol instance.
     * 
     * @return a new HTTP protocol instance.
     */
    public de.bb.bejy.Protocol create() throws Exception {
        return new HttpProtocol(this, logFile);
    }

    /**
     * Helper function to retrieve global configured mime-types.
     * 
     * @param extension
     * @return the mime-type or null / public static String getMimeType(String extension) { String mt =(String)
     *         mimes.get(extension); if (mt != null) return mt; return MimeTypesCfg.getMimeType(extension); }
     * 
     *         /** Return the name of this protocol.
     * @return the name of this protocol.
     */
    public String getName() {
        return "HTTP";
    }

    /**
     * Override the id for further extensions.
     * 
     * @author bebbo
     * @return an ID to override the Configurator ID.
     */
    public String getId() {
        return "de.bb.bejy.http.protocol";
    }

    public void activate(LogFile logFile) throws Exception {
        super.activate(logFile);

        Config.addGlobalUnique("mime-types");

        hosts.clear();

        for (Iterator i = children(); i.hasNext();) {
            Configurable ca = (Configurable) i.next();
            String domains = ca.getProperty("name");
            for (Enumeration e = new StringTokenizer(domains, " ,\r\t\f\n"); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                if (hosts.get(name) != null)
                    logFile.writeDate("WARNING: redirection for " + name + " is configured twice");
                else
                    hosts.put(name, ca);
                logFile.writeDate("added:      virtual HTTP server " + name);
            }
            ca.activate(logFile);
        }

        //    logThread = new LogThread();
        //    logThread.start();
    }

    public void deactivate(LogFile logFile) throws Exception {
        //    logThread.requestDie();
        //    logThread.join();
        super.deactivate(logFile);
    }

}
