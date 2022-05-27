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