/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Configurable.java,v $
 * $Revision: 1.31 $
 * $Date: 2014/09/22 09:23:45 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 * config handling of bejy
 *
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 1994-2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

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
                    if (!pv.startsWith("{PKCS5SHA256}")) // no double encode
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
/******************************************************************************
 * $Log: Configurable.java,v $
 * Revision 1.31  2014/09/22 09:23:45  bebbo
 * @R default parameters are no longer written to the config file bejy.xml - looks much nicer now
 *
 * Revision 1.30  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 * Revision 1.29 2014/03/23 22:06:44 bebbo
 * 
 * @B fixed shifting elements in web tree to last
 * 
 *    Revision 1.28 2013/06/18 13:23:24 bebbo
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.27 2012/11/13 06:37:46 bebbo
 * 
 * @R all properties containing "password" are supporting encryption Revision 1.26 2012/08/11 17:03:46 bebbo
 * 
 * @I typed collections Revision 1.25 2007/08/09 16:06:55 bebbo
 * 
 * @I integrated new SSL implementation
 * 
 *    Revision 1.24 2006/10/12 05:51:33 bebbo
 * @R default update = deactivate + activate. parent is no longer updated!
 * 
 *    Revision 1.23 2006/03/17 11:26:15 bebbo
 * @N added method children to get an Iterator for children with same key
 * 
 *    Revision 1.22 2006/02/06 09:12:13 bebbo
 * @I cleanup
 * 
 *    Revision 1.21 2005/11/11 18:49:55 bebbo
 * @R moved code to Config
 * 
 *    Revision 1.20 2004/12/13 15:26:25 bebbo
 * @B fixed class loading for JDK1.4.2_05 and later
 * 
 *    Revision 1.19 2004/04/07 16:33:23 bebbo
 * @I using new ClassLoader
 * 
 *    Revision 1.18 2004/03/23 11:07:52 bebbo
 * @I using new ZipClassLoader
 * @B catched an Exception in getPropertyClassNames()
 * 
 *    Revision 1.17 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.16 2003/07/09 18:29:45 bebbo
 * @N added default values.
 * 
 *    Revision 1.15 2003/07/01 10:56:23 bebbo
 * @N added class browsing
 * 
 *    Revision 1.14 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *****************************************************************************/
