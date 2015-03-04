/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspFactoryImpl.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/08/11 16:56:39 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * a JSPFactoryImpl
 *
 */

package de.bb.bejy.http.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

class JspFactoryImpl extends JspFactory {
    private static JspEngineInfoImpl jeii = new JspEngineInfoImpl();

    JspFactoryImpl() {
        setDefaultFactory(this);
    }

    public PageContext getPageContext(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse,
            String errorPageUrl, boolean needsSession, int buffer, boolean autoFl) {
        PageContext pc = new PageContextImpl();
        try {
            pc.initialize(servlet, servletRequest, servletResponse, errorPageUrl, needsSession, buffer, autoFl);
            return pc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JspEngineInfo getEngineInfo() {
        return jeii;
    }

    public void releasePageContext(PageContext pageContext) {
        if (pageContext != null)
            pageContext.release();
    }
}

/*
 * $Log: JspFactoryImpl.java,v $
 * Revision 1.2  2012/08/11 16:56:39  bebbo
 * @D added a stacktrace
 *
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.4  2004/03/24 09:41:22  bebbo
 * @B catched possible NPE in releasePageContext()
 *
 * Revision 1.3  2004/03/23 19:02:28  bebbo
 * @B added call to pageContext.release()
 *
 * Revision 1.2  2002/11/06 09:41:41  bebbo
 * @I reorganized imports
 * @I removed unused variables
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
