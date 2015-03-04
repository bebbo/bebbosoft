/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspServletImpl.java,v $
 * $Revision: 1.2 $
 * $Date: 2006/05/09 12:13:25 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Hagen Raab / Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * JSPServlet - compile and invoke JSPs
 *
 */

package de.bb.bejy.http.jsp;

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
/*
 * $Log: JspServletImpl.java,v $
 * Revision 1.2  2006/05/09 12:13:25  bebbo
 * @R changes to comply to servlet2_4
 *
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.11  2004/01/09 19:38:48  bebbo
 * @R always set the Thread contect class loader
 *
 * Revision 1.10  2003/11/16 09:30:29  bebbo
 * @B various changes
 *
 * Revision 1.9  2002/12/23 16:56:56  bebbo
 * @B fixed warning message
 * @B ServletImpl now public again :)
 *
 * Revision 1.8  2002/12/19 14:51:37  bebbo
 * @R no longer a public class
 *
 * Revision 1.7  2002/11/06 09:41:41  bebbo
 * @I reorganized imports
 * @I removed unused variables
 *
 * Revision 1.6  2002/03/21 14:34:45  franke
 * @N added support for JSP files as Servlet
 * @N added support for classes loaded via ClassLoader
 * @N added support for lib/*.jar in web applications
 *
 * Revision 1.5  2002/03/10 20:04:48  bebbo
 * @N support wildcard param to tag attr copy
 *
 * Revision 1.4  2001/12/28 11:52:47  franke
 * @B add call to jspInit() and jspDestroy()
 *
 * Revision 1.3  2001/11/19 13:25:25  franke
 * @B fixed handling of useBean, getParam and setParam
 *
 */
