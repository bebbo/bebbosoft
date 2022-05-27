/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/Version.java,v $
 * $Revision: 1.5 $
 * $Date: 2002/03/01 12:38:26 $
 * $Author: franke $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * version info for bb_jsp
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

  (c) 1994-2001 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/


package de.bb.jsp;

public class Version
{
  final static String no;
  final static String version;
  static {
    String s = "$Revision: 1.5 $";
    no = "V 1.1." + s.substring(11, s.length()-1);
    version = "JSP package " + no + " (c) 1999-2002 by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
  }
  public static String getVersion() { return no; }
  public static String getFull()    { return version; }
}

/*
 * $Log: Version.java,v $
 * Revision 1.5  2002/03/01 12:38:26  franke
 * @V version info
 *
 * Revision 1.4  2001/12/28 11:54:21  franke
 * @V bumped version
 *
 * Revision 1.3  2001/09/15 08:52:06  bebbo
 * @C added LEGAL NOTES
 *
 * Revision 1.2  2001/08/14 16:14:59  bebbo
 * @B did lot a fixes.
 * @B fixed comment handling within scriptlet
 * @N implemented better session support (request, page, session) (application is still missing)
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
