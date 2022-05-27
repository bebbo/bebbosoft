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

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;

class ServletRegistration extends Registration implements javax.servlet.ServletRegistration, ServletConfig {

    String runAsRole;
    HashMap<String, Boolean> mapping2fwx = new HashMap<String, Boolean>();

    ServletRegistration(HttpContext context) {
        super(context);
    }

    public Set<String> addMapping(String... mappings) {
        checkContextnotInitialized();

        final HashSet<String> set = new HashSet<String>();
        for (final String mapping : mappings) {
            Boolean fromWebXml = mapping2fwx.get(mapping);
            if (Boolean.TRUE.equals(fromWebXml)) {
                set.add(mapping);
                continue;
            }
            mapping2fwx.put(mapping, Boolean.FALSE);
        }
        return set;
    }

    public Collection<String> getMappings() {
        return Collections.unmodifiableSet(mapping2fwx.keySet());
    }

    public String getRunAsRole() {
        return runAsRole;
    }

    static class Dynamic extends ServletRegistration implements javax.servlet.ServletRegistration.Dynamic {
        int loadOnStartup = -1;
        MultipartConfigElement multipartConfigElement;
        Servlet servlet;
        
        Dynamic(HttpContext context, String servletName, Servlet servlet) {
            super(context);
            this.name = servletName;
            this.servlet = servlet;
            this.className = servlet.getClass().getName();
        }

        public void setLoadOnStartup(int loadOnStartup) {
            checkContextnotInitialized();
            this.loadOnStartup = loadOnStartup;
        }

        public void setMultipartConfig(MultipartConfigElement multipartConfigElement) {
            if (multipartConfigElement == null)
                throw new IllegalArgumentException("multipartConfigElement must not be null");
            checkContextnotInitialized();
            this.multipartConfigElement = multipartConfigElement;
        }

        public void setRunAsRole(String role) {
            if (role == null)
                throw new IllegalArgumentException("role must not be null");
            checkContextnotInitialized();
            this.runAsRole = role;
        }

        public Set<String> setServletSecurity(ServletSecurityElement servletSecurityElement) {
            if (servletSecurityElement == null)
                throw new IllegalArgumentException("servletSecurityElement must not be null");
            checkContextnotInitialized();

            return context.addSecurity4Mappings(servletSecurityElement, mapping2fwx.keySet());
        }

        public void setAsyncSupported(boolean arg0) {
            checkContextnotInitialized();

        }

    }

    public String getServletName() {
        return name;
    }

    public ServletContext getServletContext() {
        return context;
    }

    public Enumeration<String> getInitParameterNames() {
        return new IterEnum<String>(getInitParameters().values().iterator());
    }
}
