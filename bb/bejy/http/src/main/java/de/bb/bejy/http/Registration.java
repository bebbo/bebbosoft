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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class Registration implements javax.servlet.Registration {

    HttpContext context;
    String name;
    String className;
    Map<String, String> initParams = new HashMap<String, String>();

    public Registration(HttpContext context) {
        this.context = context;
    }

    public String getClassName() {
        return className;
    }

    public String getInitParameter(final String key) {
        return initParams.get(key);
    }

    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParams);
    }

    public String getName() {
        return name;
    }

    public boolean setInitParameter(String key, String value) {
        checkContextnotInitialized();
        if (key == null)
            throw new IllegalArgumentException("key must not be null");
        if (initParams.containsKey(key))
            return false;
        initParams.put(key, value);
        return true;
    }

    void checkContextnotInitialized() {
        if (context.isInitialized())
            throw new IllegalStateException("context is already initialized");
    }

    public Set<String> setInitParameters(Map<String, String> map) {
        checkContextnotInitialized();
        Set<String> set = new HashSet<String>();
        for (final Entry<String, String> e : map.entrySet()) {
            final String key = e.getKey();
            if (key == null)
                throw new IllegalArgumentException("key must not be null");
            if (initParams.containsKey(key))
                set.add(key);
            else
                initParams.put(key, e.getValue());
        }
        return set;
    }
    static class Dynamic extends Registration implements javax.servlet.Registration.Dynamic {
        Dynamic(HttpContext context) {
            super(context);
        }

        public void setAsyncSupported(boolean on) {
            checkContextnotInitialized();
            // not yet supported
        }
    }
}
