/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/HttpFactory.java,v $
 * $Revision: 1.28 $
 * $Date: 2012/07/18 06:44:38 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
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

  (c) 1994-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

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

/******************************************************************************
 * $Log: HttpFactory.java,v $
 * Revision 1.28  2012/07/18 06:44:38  bebbo
 * @I typified
 * Revision 1.27 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 *     Revision 1.26 2004/12/13 15:31:49 bebbo
 * @B fixed broken mime type lookup
 * 
 *    Revision 1.25 2004/04/07 16:28:17 bebbo
 * @B fixed log message
 * 
 *    Revision 1.24 2003/08/04 08:35:12 bebbo
 * @B added super.activate(logFile) to init the logFile properly
 * 
 *    Revision 1.23 2003/06/17 15:13:32 bebbo
 * @R more changes to enable on the fly config updates
 * 
 *    Revision 1.22 2003/06/17 14:41:51 bebbo
 * @R changed displayed name
 * 
 *    Revision 1.21 2003/06/17 10:18:42 bebbo
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.20 2003/05/13 15:41:46 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.19 2003/01/07 18:32:20 bebbo
 * @W removed some deprecated warnings
 * 
 *    Revision 1.18 2002/12/16 16:33:11 bebbo
 * @I HttpRequest is now a separate object
 * 
 *    Revision 1.17 2002/11/06 09:38:41 bebbo
 * @I reorganized imports
 * 
 *    Revision 1.16 2002/08/21 09:13:01 bebbo
 * @I added destroy() function
 * 
 *    Revision 1.15 2002/03/30 15:47:29 franke
 * @R moved version info to HttpProtocol
 * 
 *    Revision 1.14 2002/03/21 14:39:35 franke
 * @N added support for web-apps. Added to config file based configuration some config function calls. Also added the
 *    use of a special ClassLoader.
 * 
 *    Revision 1.13 2001/10/09 08:01:42 bebbo
 * @N added message for double configured hosts
 * 
 *    Revision 1.12 2001/10/08 22:05:56 bebbo
 * @L modified logging
 * @N added siupport for mulitple virtual host names
 * 
 *    Revision 1.11 2001/09/15 09:25:55 bebbo
 * @D enhanced logging
 * 
 *    Revision 1.10 2001/09/15 08:46:46 bebbo
 * @I using XmlFile instead of ConfigFile
 * @I reflect changes of XmlFile
 * 
 *    Revision 1.9 2001/05/07 16:18:03 bebbo
 * @B fixed behaviour for long file donwload
 * 
 *    Revision 1.8 2001/04/16 16:23:17 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.7 2001/04/16 13:43:54 bebbo
 * @I changed IniFile to XmlFile
 * 
 *    Revision 1.6 2001/04/06 05:54:05 bebbo
 * @B fix for HTTP/1.0
 * @B fix for cookies
 * 
 *    Revision 1.5 2001/04/02 16:14:15 bebbo
 * @I removed obsolete parameter
 * 
 *    Revision 1.4 2001/03/30 17:27:52 bebbo
 * @R factory.load got an additional parameter
 * 
 *    Revision 1.3 2001/03/27 19:49:08 bebbo
 * @I now is known by Protocol
 * 
 *    Revision 1.2 2001/03/11 20:41:37 bebbo
 * @N first working file handling
 * 
 *    Revision 1.1 2001/02/26 17:48:54 bebbo
 * @R new home
 * 
 *****************************************************************************/
