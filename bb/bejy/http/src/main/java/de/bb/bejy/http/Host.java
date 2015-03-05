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

import java.util.HashMap;
import java.util.Iterator;

import de.bb.bejy.Configurable;
import de.bb.util.LogFile;

public class Host extends Configurable {
    private final static String PROPERTIES[][] = {{"name", "a list of domain names, ip addresses or *", "*"},
            {"logFile", "file and path for http log file or * for stdout", "*"},
            {"logFileDateFormat", "the format log file format", "yyyyMMdd"}};

    LogFile httpLog;

    HashMap<String, HttpContext> contexts = new HashMap<String, HttpContext>();

    public Host() {
        init("virtual host", PROPERTIES);
    }

    public void activate(LogFile logFile) throws Exception {
        String sLogName = getProperty("logFile", "*");
        String fmt = getProperty("logFileDateFormat");
        httpLog = new LogFile(sLogName, fmt);

        contexts = new HashMap<String, HttpContext>();

        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable o = i.next();
            if (o instanceof HttpContext) {
                activateContext(logFile, (HttpContext) o);
                continue;
            }
            WebAppsCfg web = (WebAppsCfg) o;
            web.vhost = this;
            web.activate(logFile);
        }
    }

    void activateContext(LogFile logFile, HttpContext hc) throws Exception {
        logFile.writeDate("activating: context " + hc.getName());
        hc.hServer = this;
        hc.activate(logFile);
        contexts.put(hc.sContext, hc);
        logFile.writeDate("activated: context " + hc.getName());
    }

    public void deactivate(LogFile logFile) throws Exception {
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            if (c instanceof WebAppsCfg)
                continue;
            try {
                c.deactivate(logFile);
            } catch (Exception exx) {
            }
        }
    }

    HttpContext getContext(String path) {
        //    System.out.println(path);
        //    System.out.println(contexts);
        while (path.length() > 1) {
            HttpContext hc = contexts.get(path);
            if (hc != null) {
                //        System.out.println(path);
                return hc;
            }
            int slash = path.lastIndexOf('/');
            if (slash < 0)
                break;
            path = path.substring(0, slash);
        }
        final HttpContext hc = contexts.get("");
        return hc;
    }

    /**
     * Override the method destroy to release once allocated resources.
     */
    protected void destroy() {
        for (Iterator<HttpContext> i = contexts.values().iterator(); i.hasNext();) {
            final HttpContext hc = i.next();
            hc.destroy();
        }
    }

    public void addContext(HttpContext wac) {
        contexts.put(wac.sContext, wac);
    }
}
