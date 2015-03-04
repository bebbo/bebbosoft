/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/StaticJspServlet.java,v $
 * $Revision: 1.3 $
 * $Date: 2012/11/08 12:10:21 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Hagen Raab / Stefan Bebbo Franke
 * (c) 1999-2001 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * JSPServlet - compile and invoke JSPs
 *
 */

package de.bb.bejy.http.jsp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StaticJspServlet extends JspServlet {
    /**
   */
    private static final long serialVersionUID = -8850205404491344935L;
    private String jspFileName;

    public void setJspFileName(String n) {
        jspFileName = n;
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        service(request, response, jspFileName);
    }
}
/**
 * $Log: StaticJspServlet.java,v $
 * Revision 1.3  2012/11/08 12:10:21  bebbo
 * @I cache the realPath - less work during each check
 * Revision 1.2 2006/03/17 11:31:15 bebbo
 * 
 * @N added SUID
 * 
 *    Revision 1.1 2004/04/16 13:46:09 bebbo
 * @R runtime moved to de.bb.jsp
 * 
 *    Revision 1.2 2002/11/06 09:41:41 bebbo
 * @I reorganized imports
 * @I removed unused variables
 * 
 *    Revision 1.1 2002/03/21 14:34:45 franke
 * @N added support for JSP files as Servlet
 * @N added support for classes loaded via ClassLoader
 * @N added support for lib/*.jar in web applications
 * 
 */
