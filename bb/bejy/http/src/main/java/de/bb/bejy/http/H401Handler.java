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
