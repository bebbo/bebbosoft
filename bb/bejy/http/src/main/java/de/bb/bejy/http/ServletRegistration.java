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
