/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/Host.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/12/15 19:37:07 $
 * $Author: bebbo $
 * $Locker:  $#
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * generell Server class
 *
 ******************************************************************************
 NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Every product and solution using this software, must be free
 of any charge. If the software is used by a client part, the
 server part must also be free and vice versa.

 2. Each redistribution must retain the copyright notice, and
 this list of conditions and the following disclaimer.

 3. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in
 the documentation and/or other materials provided with the
 distribution.

 4. All advertising materials mentioning features or use of this
 software must display the following acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 5. Redistributions of any form whatsoever must retain the following
 acknowledgment:
 "This product includes software developed by BebboSoft,
 written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
 DISCLAIMER OF WARRANTY

 Software is provided "AS IS," without a warranty of any kind.
 You may use it on your own risk.

 ******************************************************************************
 LIMITATION OF LIABILITY

 I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
 AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
 FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
 SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
 COPYRIGHT

 (c) 1994-2002 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

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

/******************************************************************************
 * $Log: Host.java,v $
 * Revision 1.1  2012/12/15 19:37:07  bebbo
 * @I renamed Host
 * Revision 1.4 2012/07/18 06:44:43 bebbo
 * 
 * @I typified Revision 1.3 2008/06/04 06:44:52 bebbo
 * 
 * @B enhanced reloading of wep applications
 * 
 *    Revision 1.2 2008/03/13 20:43:38 bebbo
 * @O performance optimizations
 * 
 *    Revision 1.1 2008/01/17 17:30:29 bebbo
 * @R separated VHost from HostCfg
 * 
 *    Revision 1.14 2007/05/01 19:05:27 bebbo
 * @I changes due to RequestDispatcher changes
 * 
 *    Revision 1.13 2007/04/13 17:54:21 bebbo
 * @N added logFileDateFormat
 * 
 *    Revision 1.12 2007/02/12 20:37:35 bebbo
 * @B fixed NPE if webapps dir is empty
 * 
 *    Revision 1.11 2007/01/18 21:45:52 bebbo
 * @B fixed a bug in activation, to update the server instance
 * 
 *    Revision 1.10 2006/10/12 05:53:30 bebbo
 * @B adding all webapps to the internal bejy.xml as if they were configured
 * 
 *    Revision 1.9 2004/12/13 15:31:04 bebbo
 * @F reformatted
 * 
 *    Revision 1.8 2004/04/16 13:47:23 bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 * 
 *    Revision 1.7 2004/04/07 16:27:54 bebbo
 * @B fixed log message
 * 
 *    Revision 1.6 2003/07/09 18:29:49 bebbo
 * @N added default values.
 * 
 *    Revision 1.5 2003/06/20 09:09:38 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.4 2003/06/18 08:36:52 bebbo
 * @R modification, dynamic loading, removing - all works now
 * 
 *    Revision 1.3 2003/06/17 12:09:56 bebbo
 * @R added a generalization for Configurables loaded by class
 * 
 *    Revision 1.2 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.1 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.16 2003/02/19 16:10:31 bebbo
 * @B fix in StringTree Iterator!
 * 
 *    Revision 1.15 2003/02/19 13:26:37 bebbo
 * @I changed visibility of methods/fields -> more performance
 * 
 *    Revision 1.14 2002/11/06 09:40:47 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.13 2002/08/21 09:13:01 bebbo
 * @I added destroy() function
 * 
 *    Revision 1.12 2002/04/08 13:23:43 franke
 * @I rewrote context selection algorithm
 * 
 *    Revision 1.11 2002/04/03 15:40:37 franke
 * @N added support for webapps
 * 
 *    Revision 1.10 2002/03/30 15:48:18 franke
 * @B catching exceptions during server load, and log them
 * 
 *    Revision 1.9 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some config function calls. Also added the
 *    use of a special ClassLoader.
 * 
 *    Revision 1.8 2001/10/08 22:05:34 bebbo
 * @L modified logging
 * 
 *    Revision 1.7 2001/09/15 08:47:41 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.6 2001/04/16 16:23:18 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.5 2001/04/16 13:43:55 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.4 2001/03/29 07:10:22 bebbo
 * @I more functions impemented
 * 
 *    Revision 1.3 2001/03/27 19:48:17 bebbo
 * @I lot's of stuff changed
 * 
 *    Revision 1.2 2001/03/20 18:34:07 bebbo
 * @N enhanced functionality
 * @N more functions for Servlet API
 * @B fixes in filehandler
 * @N first working CGI
 * 
 *    Revision 1.1 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *****************************************************************************/
