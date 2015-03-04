package de.bb.bejy.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

class FilterRegistration extends Registration implements
        javax.servlet.FilterRegistration, FilterConfig {
    Filter filter;

    FilterRegistration(HttpContext context, Filter filter) {
        super(context);
        this.filter = filter;
    }

    public void addMappingForServletNames(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... servletNames) {
        if (servletNames == null)
            throw new IllegalArgumentException("servletNames must not be null");
        checkContextnotInitialized();

        final ArrayList<String> al = new ArrayList<String>(servletNames.length);
        Collections.addAll(al, servletNames);

        if (dispatcherTypes == null) {
            for (final String servletName : al) {
                final MappingData md = new MappingData(filter,
                        DispatcherType.REQUEST, null, servletName);
                if (isMatchAfter)
                    context.afterQueue.add(md);
                else
                    context.frontQueue.add(md);
            }
            return;
        }
        for (DispatcherType dt : dispatcherTypes) {
            for (String servletName : al) {
                final MappingData md = new MappingData(filter, dt, null,
                        servletName);
                if (isMatchAfter)
                    context.afterQueue.add(md);
                else
                    context.frontQueue.add(md);
            }
        }
    }

    public void addMappingForUrlPatterns(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... urlPatterns) {
        if (urlPatterns == null)
            throw new IllegalArgumentException("servletNames must not be null");
        checkContextnotInitialized();

        final ArrayList<String> al = new ArrayList<String>(urlPatterns.length);
        Collections.addAll(al, urlPatterns);

        if (dispatcherTypes == null) {
            for (String urlPattern : al) {
                final MappingData md = new MappingData(filter,
                        DispatcherType.REQUEST, urlPattern, null);
                if (isMatchAfter)
                    context.afterQueue.add(md);
                else
                    context.frontQueue.add(md);
            }
            return;
        }
        for (DispatcherType dt : dispatcherTypes) {
            for (String urlPattern : al) {
                final MappingData md = new MappingData(filter, dt, urlPattern,
                        null);
                if (isMatchAfter)
                    context.afterQueue.add(md);
                else
                    context.frontQueue.add(md);
            }
        }
    }

    public Collection<String> getServletNameMappings() {
        return context.getServletNameMappings(filter);
    }

    public Collection<String> getUrlPatternMappings() {
        return context.getUrlPatternMappings(filter);
    }

    static class MappingData {
        Filter filter;
        DispatcherType dispatcherType;
        String urlPattern;
        String servletName;

        public MappingData(Filter filter, DispatcherType dispatcherType,
                String urlPattern, String servletName) {
            this.filter = filter;
            this.dispatcherType = dispatcherType;
            this.urlPattern = urlPattern;
            this.servletName = servletName;
        }
    }

    static class Dynamic extends FilterRegistration implements
            javax.servlet.FilterRegistration.Dynamic,
            javax.servlet.FilterConfig {

        Dynamic(HttpContext context, String filterName, Filter filter) {
            super(context, filter);
            this.name = filterName;
            this.className = filter.getClass().getName();
        }

        public void setAsyncSupported(boolean on) {
            checkContextnotInitialized();
            // unsupported
        }

        public String getFilterName() {
            return name;
        }

        public Enumeration<String> getInitParameterNames() {
            return new IterEnum<String>(initParams.keySet().iterator());
        }

        public ServletContext getServletContext() {
            return context;
        }
    }

    public String getFilterName() {
        return name;
    }

    public ServletContext getServletContext() {
        return context;
    }

    public Enumeration<String> getInitParameterNames() {
        return new IterEnum<String>(getInitParameters().values().iterator());
    }
}
