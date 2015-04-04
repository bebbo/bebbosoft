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

package de.bb.jsp;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

public abstract class JspServletImpl extends HttpServlet implements HttpJspPage
{
  protected void service (HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    //Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    _jspService(req, resp);
  }

  public void _jspservice (HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
  }
  
  public void init (ServletConfig config) throws ServletException
  {
	  super.init (config);
	  jspInit();
	}
  
  protected void finalize() throws Throwable
  {
    try {
      jspDestroy();
    } catch (Throwable t) {}
    super.finalize();
  }
  
    
  public void jspDestroy()
  {}
  
  public void jspInit()
  {}
  
}
