/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Version.java,v $
 * $Revision: 6.2 $
 * $Date: 2014/09/22 09:25:05 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2003.
 * All rights reserved
 *
 * version info for bejy
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

package de.bb.bejy;

import de.bb.bejy.server.V;

/**
 * Version info for BEJY...
 */
public class Version {
    private final static String shortVersion;
    private final static String version;
    static {
        shortVersion = "BEJY V" + V.V;
        version = shortVersion + " (c) 2000-" + V.Y + " by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
    }

    /**
     * Get only the version number.
     * 
     * @return the version number as String.
     */
    public static String getVersion() {
        return V.V;
    }

    /**
     * Get only the short version message.
     * 
     * @return the short version message as String.
     */
    public static String getShort() {
        return shortVersion;
    }

    /**
     * Get only the full version message.
     * 
     * @return the full version message as String.
     */
    public static String getFull() {
        return version;
    }
}

/******************************************************************************
 * $Log: Version.java,v $
 * Revision 6.2  2014/09/22 09:25:05  bebbo
 * @V new version
 *
 * Revision 6.1  2014/09/21 12:52:04  bebbo
 * 6.1
 *
 * Revision 1.69  2013/11/28 12:23:03  bebbo
 * @N SSL cipher types are configurable
 * @I using nio sockets
 *
 * Revision 1.68  2013/06/18 13:23:31  bebbo
 * @I preparations to use nio sockets
 * @V 1.5.1.68
 * Revision 1.67 2012/08/19 15:46:52 bebbo
 * 
 * @V new version 1.5.1.67 Revision 1.66 2012/08/19 15:46:22 bebbo
 * 
 * @V 1.5.1.66 Revision 1.65 2010/04/10 12:12:33 bebbo
 * 
 * @V version Revision 1.65 2010/04/10 12:12:33 bebbo
 * 
 * @V new version
 * 
 *    Revision 1.64 2009/11/25 08:29:09 bebbo
 * @V bumped the version
 * @B fixed forwarding for the welcome files with CGI: query string was lost.
 * 
 *    Revision 1.63 2008/06/04 06:42:38 bebbo
 * @V new version with extendedSpf.xml
 * 
 *    Revision 1.62 2008/03/14 14:18:06 bebbo
 * @V new version
 * 
 *    Revision 1.61 2007/04/13 14:25:52 bebbo
 * @V new version 1.4.1.61
 * 
 *    Revision 1.60 2007/01/18 21:44:14 bebbo
 * @N new version
 * 
 *    Revision 1.59 2006/05/09 08:38:57 bebbo
 * @V new version
 * 
 *    Revision 1.58 2006/03/17 20:07:07 bebbo
 * @V new Release!
 * 
 *    Revision 1.57 2005/11/11 18:52:29 bebbo
 * @V v1.57
 * 
 *    Revision 1.56 2004/05/06 10:42:28 bebbo
 * @V new version
 * 
 *    Revision 1.55 2004/04/07 16:32:39 bebbo
 * @V new version message
 * 
 *    Revision 1.54 2004/03/24 09:54:13 bebbo
 * @V new version information
 * 
 *    Revision 1.53 2004/03/23 11:05:43 bebbo
 * @V new version
 * 
 *    Revision 1.52 2004/01/09 19:37:08 bebbo
 * @V maintanance release
 * 
 *    Revision 1.51 2004/01/03 18:55:55 bebbo
 * @B fixed SMAP for JSP debugging
 * @B fixed fast DateFormat for 01 January in leap years
 * 
 *    Revision 1.50 2003/11/26 12:02:09 bebbo
 * @N version 1.3.1.50
 * 
 *    Revision 1.49 2003/10/01 14:20:52 bebbo
 * @V new version
 * 
 *    Revision 1.48 2003/09/03 14:57:24 bebbo
 * @V new version
 * 
 *    Revision 1.47 2003/08/07 07:53:34 bebbo
 * @V stable version
 * 
 *    Revision 1.46 2003/08/04 08:33:49 bebbo
 * @V 1.2.1.46
 * 
 *    Revision 1.45 2003/07/14 12:44:54 bebbo
 * @V running like a Tiger too
 * 
 *    Revision 1.44 2003/07/09 18:29:45 bebbo
 * @N added default values.
 * 
 *    Revision 1.43 2003/06/17 10:23:45 bebbo
 * @V new configuration system
 * 
 *    Revision 1.42 2003/03/20 12:12:51 bebbo
 * @N new version
 * 
 *    Revision 1.41 2003/02/05 08:08:17 bebbo
 * @N next version
 * 
 *    Revision 1.40 2003/01/25 15:07:49 bebbo
 * @N added parameter backLog and bindAddress
 * 
 *    Revision 1.39 2003/01/08 12:57:49 bebbo
 * @V new version!
 * 
 *    Revision 1.38 2002/07/26 18:36:44 bebbo
 * @V new version
 * 
 *    Revision 1.37 2002/07/23 15:19:16 bebbo
 * @V one important bugfix
 * 
 *    Revision 1.36 2002/07/16 11:06:56 bebbo
 * @V new version
 * 
 *    Revision 1.35 2002/05/16 15:23:24 franke
 * @V version
 * 
 *    Revision 1.34 2002/04/03 15:40:17 franke
 * @N new release
 * 
 *    Revision 1.33 2002/03/10 20:30:50 bebbo
 * @V new version
 * 
 *    Revision 1.32 2002/03/01 12:39:48 franke
 * @V version info
 * 
 *    Revision 1.31 2002/02/16 15:41:36 bebbo
 * @V new version
 * 
 *    Revision 1.30 2002/02/01 09:50:58 franke
 * @V bumped
 * 
 *    Revision 1.29 2002/01/20 15:58:01 franke
 * @V new release
 * 
 *    Revision 1.28 2001/12/28 11:48:41 franke
 * @V bumped version
 * 
 *    Revision 1.28 2001/12/28 11:48:41 franke
 * @V new version
 * 
 *    Revision 1.27 2001/12/23 23:36:21 bebbo
 * @R moved the mail DB implementations back to de.bb.bejy.mail
 * @N added MSSQL support
 * 
 *    Revision 1.26 2001/12/14 18:39:14 bebbo
 * @V new version
 * 
 *    Revision 1.25 2001/11/26 14:07:02 bebbo
 * @V new version
 * 
 *    Revision 1.24 2001/10/09 08:02:09 bebbo
 * @V new release
 * 
 *    Revision 1.23 2001/09/15 08:46:10 bebbo @ comments
 * 
 *    Revision 1.22 2001/08/24 16:02:01 bebbo
 * @V bumped version
 * 
 *    Revision 1.21 2001/08/14 16:12:23 bebbo
 * @V bumped vserion
 * 
 *    Revision 1.20 2001/06/11 06:31:48 bebbo
 * @N enough changes!
 * 
 *    Revision 1.19 2001/04/16 16:23:11 bebbo
 * @R changes for migration to XML configfile
 * 
 *    Revision 1.18 2001/04/11 16:31:18 bebbo
 * @bumped
 * 
 *         Revision 1.17 2001/03/28 10:28:49 bebbo
 * @V stable version
 * 
 *    Revision 1.16 2001/03/11 20:42:56 bebbo
 * @R new copyright
 * 
 *    Revision 1.15 2001/03/09 19:52:06 bebbo @ bumped
 * 
 *    Revision 1.14 2001/03/05 17:50:37 bebbo @ bumped - with SSL now
 * 
 *    Revision 1.13 2001/02/25 17:06:25 bebbo @ bumped
 * 
 *    Revision 1.12 2001/02/19 19:55:52 bebbo @ bumped
 * 
 *    Revision 1.11 2001/02/12 12:53:20 bebbo @ bumped
 * 
 *    Revision 1.10 2001/02/12 12:41:09 bebbo @ bumped
 * 
 *    Revision 1.9 2001/01/15 16:31:43 bebbo @ fixed broken smtp files. no longer added to user account
 * 
 *    Revision 1.8 2001/01/15 16:30:39 bebbo @ bump
 * 
 *    Revision 1.7 2001/01/15 16:30:00 bebbo @ bump
 * 
 *    Revision 1.6 2001/01/01 16:52:35 bebbo @ bumped
 * 
 *    Revision 1.5 2001/01/01 01:02:06 bebbo @ bumped
 * 
 *    Revision 1.4 2000/12/30 09:02:42 bebbo @ bumped
 * 
 *    Revision 1.3 2000/12/29 17:49:54 bebbo @ bumped
 * 
 *    Revision 1.2 2000/12/28 20:53:24 bebbo
 * @I many changes
 * @I first working smtp and pop protocl
 * 
 *    Revision 1.1 2000/11/10 18:13:27 bebbo
 * @N new (uncomplete stuff)
 *****************************************************************************/
