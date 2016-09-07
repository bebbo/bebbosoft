package de.bb.web.access;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class AccessFilter implements Filter {


    public void destroy() {
        // nada
    }

    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest hsreq = (HttpServletRequest) req;
        final String sp = hsreq.getServletPath();
        final int s2 = sp.indexOf('/', 1);
        if (s2 > 0) {
            final String first = sp.substring(1, s2).toUpperCase();
            if (!hsreq.isUserInRole(first)) {
                final HttpServletResponse hsresp = (HttpServletResponse) resp;
                hsresp.sendError(403, "not in " + first);
                return;
            }
        }
        
        if (sp.endsWith("/")) {
            RequestDispatcher rd = hsreq.getRequestDispatcher("/WEB-INF/jsp/folderlist.jsp");
            req.setAttribute("dir", hsreq.getServletPath());
            rd.forward(req, resp);
            return;
        }
	
        chain.doFilter(req, resp);
    }

    public void init(final FilterConfig fc) throws ServletException {
        // nada
    }
}

