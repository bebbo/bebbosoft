/**
 * 
 */
package de.bb.bejy.http;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import de.bb.bejy.http.FilterRegistration.MappingData;

class AFilterChain implements javax.servlet.FilterChain {
    String servletPath;

    private java.util.Iterator<MappingData> iterator;

    HttpHandler last;

    AFilterChain(String sp, Iterator<MappingData> i, HttpHandler handler) {
        servletPath = sp;
        iterator = i;
        last = handler;
    }

    public void doFilter(javax.servlet.ServletRequest request,
            javax.servlet.ServletResponse response) throws java.io.IOException,
            javax.servlet.ServletException {
        String servletPath = this.servletPath;
        if (request instanceof HttpServletRequest) {
            servletPath = ((HttpServletRequest) request).getServletPath();
        }
        while (iterator.hasNext()) {
            MappingData md = iterator.next();
            if (md.urlPattern != null) {
                final String mask = md.urlPattern;
                if ((mask.startsWith("*") && servletPath.endsWith(mask
                        .substring(1)))
                        || (mask.endsWith("*") && servletPath.startsWith(mask
                                .substring(0, mask.length() - 1)))
                        || mask.equals(servletPath)) {
                    md.filter.doFilter(request, response, this);
                    return;
                }
            }
            if (last instanceof ServletHandler) {
                final ServletHandler sh = (ServletHandler) last;
                if (sh.servletRegistration.name.equals(md.servletName)) {
                    md.filter.doFilter(request, response, this);
                    return;
                }
            }
        }
        last.service(request, response);
    }
}