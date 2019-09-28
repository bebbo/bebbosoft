/******************************************************************************
 * config handling of BEJY
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2016.
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
package de.bb.bejy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.bb.security.DES3;
import de.bb.security.Pkcs5;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;

/**
 * Base class for all configurable objects.
 * 
 * @author bebbo
 */
public abstract class Configurable {
    private final static byte[] BEJY_KEY = "bejy_key12345678this_odd".getBytes();
    private Configurator configurator;
    private String name;
    private Configurable parent;

	private Map<String, Object> propertyNames = new SingleMap<String, Object>();
    private Map<String, Object> propertyTypes = new SingleMap<String, Object>();
    private Map<String, Object> defaultValues = new SingleMap<String, Object>();
    private HashMap<String, String> properties = new HashMap<String, String>();

    private MultiMap<String, Configurable> children = new MultiMap<String, Configurable>();

    /**
     * Used to initialize the name and the properties.
     * 
     * @param name
     *            the own name
     * @param props
     *            property information: name, description
     */
    protected void init(String name, Object props[][]) {
        this.name = name;

        //    propertyNames.clear();
        if (props == null)
            return;

        for (int i = 0; i < props.length; ++i) {
            Object o[] = props[i];
            propertyNames.put((String) o[0], o[1]);
            if (o.length > 2) {
                Object oo = o[2];
                if (oo instanceof Class)
                    propertyTypes.put((String) o[0], oo);
                else if (oo instanceof String)
                    defaultValues.put((String) o[0], oo);
            }
        }
    }

    /**
     * Load this configurable from xml file.
     * 
     * @param cfgt
     *            the Configurator
     * @param xml
     *            the xml file
     * @param section
     *            path to own information
     */
    void load(Configurator cfgt, XmlFile xml, String section) {
        setConfigurator(cfgt);
        name = xml.getString(section, "name", name);

        for (Iterator<String> i = propertyNames(); i.hasNext();) {
            String pn = i.next();
            String v = xml.getString(section, pn, null);
            try {
                if (v != null && v.length() > 0 && pn.toLowerCase().indexOf("password") >= 0) {
                    if (!v.startsWith("{")) {
                        DES3 des = new DES3();
                        des.setKey(BEJY_KEY);
                        byte b[] = v.getBytes();
                        b = Mime.decode(b, 0, b.length);
                        b = des.decryptCBCAndPadd(new byte[8], b);
                        v = new String(b);
                    }
                }
            } catch (Throwable t) {
            }
            properties.put(pn, v);
        }
    }

    /**
     * Get the display name.
     * 
     * @return the display name.
     * @see #setName
     */
    public String getName() {
        return getProperty("name", name);
    }

    /**
     * Set the display name.
     * 
     * @param name
     *            the name
     * @see #getName
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the names of available properties.
     * 
     * @return the names of available properties.
     */
    public Iterator<String> propertyNames() {
        return propertyNames.keySet().iterator();
    }

    /**
     * Get a property descritption by name.
     * 
     * @param name
     *            the property name
     * @return a property by name.
     */
    public String getPropertyInfo(String name) {
        return (String) propertyNames.get(name);
    }

    /**
     * Returns the class names of matching classes in class path.
     * 
     * @param name
     *            specifies the property which contains the class/interface name to match.
     * @return a list containing the class names of matching classes in class path.
     */
    public List<?> getPropertyClassNames(String name) {
        Class<?> c = (Class<?>) propertyTypes.get(name);
        if (c == null)
            return null;

        return Config.getImplementors(c, "*.class");
    }

    /**
     * Get a property by name.
     * 
     * @param name
     *            the property name
     * @return a property by name.
     * @see #setProperty
     */
    public String getProperty(String name) {
        Object r = properties.get(name);
        if (r == null)
            r = defaultValues.get(name);
        return (String) r;
    }

    /**
     * Get a property by name with default value.
     * 
     * @param name
     *            the property name
     * @param def
     *            the default value
     * @return a property by name.
     * @see #setProperty
     */
    public String getProperty(String name, String def) {
        String ret = getProperty(name);
        if (ret != null)
            return ret;
        return def;
    }

    /**
     * Get a property by name as int value.
     * 
     * @param name
     *            the property name
     * @param def
     *            default value.
     * @return a int property by name.
     */
    public int getIntProperty(String name, int def) {
        try {
            return Integer.parseInt(getProperty(name));
        } catch (Throwable t) {
        }
        return def;
    }

    /**
     * Get a property by name as boolean value.
     * 
     * @param name
     *            the property name
     * @param def
     *            default value.
     * @return a boolean property by name.
     */
    public boolean getBooleanProperty(String name, boolean def) {
        String s = getProperty(name);
        if (s == null)
            return def;
        return "true".equalsIgnoreCase(s);
    }

    /**
     * Set a property ba name.
     * 
     * @param name
     *            the property name
     * @param value
     *            the property value
     * @see #getProperty
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    /**
     * Activates the current Configurable.
     * 
     * @param logFile
     *            the logFile
     * @throws Exception
     *             on error
     */
    public void activate(LogFile logFile) throws Exception {
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            try {
                c.activate(logFile);
            } catch (Exception exx) {
            }
        }
    }

    /**
     * Deactivate the current Configurable.
     * 
     * @param logFile
     *            the logFile
     * @throws Exception
     *             on error
     */
    public void deactivate(LogFile logFile) throws Exception {
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            try {
                c.deactivate(logFile);
            } catch (Exception exx) {
            }
        }
    }

    /**
     * Update the current Configurable on changes.
     * 
     * @param logFile
     *            the logFile
     * @throws Exception
     *             on error
     */
    public void update(LogFile logFile) throws Exception {
        if (useParentActivate()) {
            parent.update(logFile);
            return;
        }
        deactivate(logFile);
        activate(logFile);
    }

    /**
     * Indicate that the parent instance will handle the activation. Return value is false. Derived classes may return
     * true to delegate activation to the parent instance. The update method will regard this value.
     * 
     * @return false.
     */
    public boolean useParentActivate() {
        return false;
    }

    /**
     * "OK" if ok, otherwise some text indicating the state.
     * 
     * @return "OK" if ok, otherwise some text indicating the state
     */
    public String getStatus() {
        return "OK";
    }

    /**
     * Return the available child objects.
     * 
     * @return the available child objects.
     */
    public Iterator<Configurable> children() {
        return children.values().iterator();
    }

    /**
     * Return the available child names.
     * 
     * @return the available child names.
     */
    public Set<String> childNames() {
        return children.keySet();
    }

    /**
     * Return the available child objects.
     * 
     * @return the available child objects.
     */
    public Iterator<Configurable> children(String path) {
        return children.subMap(path, path + "\0").values().iterator();
    }

    /**
     * Add a child.
     * 
     * @param path
     *            the unique path into configuration file.
     * @param ca
     *            the child
     */
    public void addChild(String path, Configurable ca) {
        ca.parent = this;
        children.put(path, ca);
    }

    /**
     * Retrieve a child by path.
     * 
     * @param path
     *            the path
     * @return a child by path or null.
     */
    public Configurable getChild(String path) {
        return children.get(path);
    }

    /**
     * Override the id for further extensions.
     * 
     * @return an ID to override the Configurator ID.
     */
    public String getId() {
        return null;
    }

    /**
     * Return the parent Configurable.
     * 
     * @return the parent Configurable.
     */
    public Configurable getParent() {
        return parent;
    }

    /**
     * Return the Configurator.
     * 
     * @return the Configurator.
     * @see #setConfigurator
     */
    public Configurator getConfigurator() {
        return configurator;
    }

    /**
     * Returns true if the creation of the given child type is allowed or not.
     * 
     * @param ct
     *            a Configurator.
     * @return true if the creation of the given child type is allowed or not.
     */
    public boolean acceptNewChild(Configurator ct) {
        return true;
    }

    /**
     * Returns true if the specified element may be deleted.
     * 
     * @param configurable
     *            the Configurable object
     * @return true if the specified element may be deleted.
     */
    public boolean mayRemove(Configurable configurable) {
        return true;
    }

    /**
     * Assign the Configurator.
     * 
     * @param configurator
     *            the Configurator.
     * @see #getConfigurator
     */
    public void setConfigurator(Configurator configurator) {
        this.configurator = configurator;
        if (configurator.loadClass()) {
            propertyNames.put("class", "a class implementing this configurable");
        }
    }

    /**
     * Remove the specified child.
     * 
     * @param child
     *            the child
     * @return true if the child was successfull removed. False either.
     * @throws Exception
     *             on error
     */
    public boolean remove(Configurable child) throws Exception {
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            if (c == child) {
                c.deactivate(Config.getLogFile());
                i.remove();
                return true;
            }
        }
        return false;
    }

    public void makeLast(final Configurable child) {
        for (final Entry<String, Configurable> e : children.entrySet()) {
            if (e.getValue() == child) {
                final String key = e.getKey();
                children.remove(key, child);
                children.put(key, child);
                return;
            }
        }
    }

    void store(String path, XmlFile xml) {
    	// do not save synthetic entries.
    	if (getConfigurator().getPath().equals("#"))
    		return;
    	
        if (!path.endsWith("/"))
            path += "/";
        path += getConfigurator().getPath();
        path = xml.createSection(path);
        // store properties
        for (Iterator<String> i = propertyNames(); i.hasNext();) {
            String pn = i.next();
            String pv = getProperty(pn);
            if (pv == null)
                continue;
            
            if (pv.equals(defaultValues.get(pn)))
                continue;

            if (pn.toLowerCase().indexOf("password") >= 0) {
                if (hashPassword()) {
                    if (!pv.startsWith("{P")) // no double encode
                        pv = Pkcs5.encodePbkdf2("SHA256", pv, 13);
                } else {
                    DES3 des = new DES3();
                    des.setKey(BEJY_KEY);
                    byte b[] = pv.getBytes();
                    b = des.encryptCBCAndPadd(new byte[8], b);
                    b = Mime.encode(b);
                    pv = new String(b);
                }
            }

            xml.setString(path, pn, pv);
        }
        // store children
        for (Iterator<Configurable> i = children(); i.hasNext();) {
            Configurable c = i.next();
            c.store(path, xml);
        }
    }

    public void setParent(Configurable configurable) {
        this.parent = configurable;
    }

    public boolean hashPassword() {
        return true;
    }

    /*
    public static void main(String args[]) {
        DES3 des = new DES3();
        des.setKey(BEJY_KEY);
        String v = "AAAAAAAA=";
        byte b[] = v.getBytes();
        b = Mime.decode(b, 0, b.length);
        b = des.decryptCBCAndPadd(new byte[8], b);
        v = new String(b);
        System.out.println(v);
    }
    */
}

