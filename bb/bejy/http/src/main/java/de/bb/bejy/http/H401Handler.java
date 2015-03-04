/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/H401Handler.java,v $
 * $Revision: 1.3 $
 * $Date: 2010/07/08 18:16:25 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * file handler for bejy
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

  (c) 1994-2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/
package de.bb.bejy.http;

/**
 * Helper class to provide user authentication.
 * @author bebbo
 */  
public class H401Handler extends HttpHandler
{
  static boolean DEBUG = HttpProtocol.DEBUG;
  
  private final static String PROPERTIES[][] =
    {}
  ;  
  
  public H401Handler()
  {
    init("DenyHandler", PROPERTIES);
  }
  
  H401Handler(HttpContext hc)
  {
    super(hc);
  }
  
  public void service(javax.servlet.ServletRequest in, javax.servlet.ServletResponse out)
  {
    if (DEBUG) System.out.println("requesting authentication");
    HttpRequestBase hr = (HttpRequestBase)in;
    HttpResponse sr = (HttpResponse)out;
    if (DEBUG) System.out.println("add header WWW-Authenticate:Basic realm=\"" + hr.context.aRealm + "\"");
    sr.addHeader("WWW-Authenticate", "Basic realm=\"" + hr.context.aRealm + "\"");
    sr.setStatus(401);
  }
}
/******************************************************************************
 * $Log: H401Handler.java,v $
 * Revision 1.3  2010/07/08 18:16:25  bebbo
 * @I splitted the HttpRequest to use it inside of redirectors proxy
 * @N redir can now handle proxy connects
 *
 * Revision 1.2  2009/11/18 08:47:41  bebbo
 * @D Debug stuff
 *
 * Revision 1.1  2004/04/16 13:47:24  bebbo
 * @R changes in class scanner requires explicit naming now: Handler, Group, Cfg, Factory
 *
 * Revision 1.1  2003/09/30 12:42:27  bebbo
 * @N added welcome handler
 *
 *****************************************************************************/