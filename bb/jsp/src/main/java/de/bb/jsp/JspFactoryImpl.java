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
