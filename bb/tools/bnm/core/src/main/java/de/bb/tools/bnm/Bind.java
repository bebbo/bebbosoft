/******************************************************************************
 * This file is part of de.bb.tools.bnm.core.
 *
 *   de.bb.tools.bnm.core is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.core is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.core.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElement;

import de.bb.tools.bnm.model.Id;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;

/**
 * Simple XmlBind class. Using the XmlFile from bb_util as Xml pull parser.
 * 
 * @author sfranke
 * 
 */
@SuppressWarnings("unchecked")
public class Bind {

    public final static String SPACES = "                                                            ";
    private static final HashMap<Class<?>, ArrayList<Field>> FIELDMAP = new HashMap<Class<?>, ArrayList<Field>>();
    private static ArrayList<String> FIELDNAMES = new ArrayList<String>();

    /**
     * Assign the content from the XmlFile to the object t starting at the given path-
     * 
     * @param xml
     *            the xmlFile
     * @param path
     *            the path
     * @param t
     *            the object to fill
     * @return the filled object
     * @throws Exception
     *             on error
     */
    public static <T> T bind(XmlFile xml, String path, T t) throws Exception {
        Class<?> clazz = t.getClass();
        ArrayList<Field> fields = getFields(clazz);

        for (Field f : fields) {
            String fName = f.getName();
            if (fName.startsWith("__"))
                continue;
            fName = fName.replace('_', '-');
            String key = path + fName;
            Class<?> type = f.getType();
            if (type.isPrimitive() || String.class.isAssignableFrom(type) || type.isAssignableFrom(URL.class)) {
                f.setAccessible(true);
                Object value = getValue(xml, key, type);
                f.set(t, value);
                continue;
            }
            if (ArrayList.class.isAssignableFrom(type)) {
                ArrayList<Object> al = (ArrayList<Object>) type.newInstance();
                String gt = f.getGenericType().toString();
                int bra = gt.indexOf('<');
                int ket = gt.indexOf('>');
                String ct = gt.substring(bra + 1, ket);
                Class<?> gct = Class.forName(ct);
                int dot = ct.lastIndexOf('.');
                String name = ct.substring(dot + 1);
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
                String sname = name;
                if (gct.isPrimitive() || String.class.isAssignableFrom(gct) || java.net.URL.class.isAssignableFrom(gct)) {
                    sname = f.getName();
                    sname = sname.substring(0, sname.length() - 1);
                }
                for (Iterator<String> i = xml.sections(key + "/" + sname); i.hasNext();) {
                    String kp = i.next();
                    if (gct.isPrimitive() || String.class.isAssignableFrom(gct)) {
                        Object value = getValue(xml, kp, gct);
                        al.add(value);
                        continue;
                    }
                    Object o = gct.newInstance();
                    bind(xml, kp, o);
                    al.add(o);
                }
                f.set(t, al);
                continue;
            }
            if (Map.class.isAssignableFrom(type)) {
                Map<String, Object> hm = fillMap(xml, key);
                f.set(t, hm);
                continue;
            }

            key = key + "/";
            Iterator<String> i = xml.sections(key);
            if (i.hasNext()) {
                Object o = type.newInstance();
                bind(xml, key, o);
                f.set(t, o);
            }
        }

        return t;
    }

    private static Map<String, Object> fillMap(XmlFile xml, String key) {
        Map<String, Object> hm = new MultiMap<String, Object>();
        if (!key.endsWith("/"))
            key += "/";
        for (Iterator<String> i = xml.sections(key); i.hasNext();) {

            String kp = i.next();
            String name = kp.substring(key.length(), kp.length() - 1);
            int gat = name.indexOf('#');
            if (gat > 0)
                name = name.substring(0, gat);

            Map<String, String> attrs = xml.getAttributes(kp);
            if (attrs != null && attrs.size() == 0)
                attrs = null;

            // check for sub elements, if any, recurse
            //            Iterator<String> subi = (Iterator<String>) xml.sections(kp);
            //            if (subi.hasNext()) {
            //                // create a map and recurse
            //                Map<String, Object> m = fillMap(xml, kp);
            //                if (attrs != null)
            //                    m.put("\0", attrs);
            //                hm.put(name, m);
            //            } else 
            {
                if (attrs != null) {
                    Map<String, Object> m = new SingleMap<String, Object>();
                    m.put("\0", attrs);
                    String content = xml.getContent(kp).trim();
                    if (content.length() > 0)
                        m.put("\0\0", content);
                    hm.put(name, m);
                } else {
                    // else take the content and insert it as string
                    hm.put(name, xml.getContent(kp).trim());
                }
            }
        }
        return hm;
    }

    private static Object getValue(XmlFile xml, String key, Class<?> type) throws MalformedURLException {
        String value = xml.getContent(key);
        String name = type.getSimpleName();
        if (type.isPrimitive()) {
            if ("boolean".equals(name)) {
                return Boolean.valueOf(value);
            }
            if ("int".equals(name)) {
                if (value == null)
                    return 0;
                return Integer.valueOf(value);
            }
        }
        if (value == null)
            return null;
        if (String.class.isAssignableFrom(type)) {
            return value;
        }
        if (URL.class.isAssignableFrom(type)) {
            return new URL(value);
        }
        throw new RuntimeException("please define: getValue for " + type);

        // return value;
    }

    public static ArrayList<Field> getFields(Class<?> clazz) {

        ArrayList<Field> fields = FIELDMAP.get(clazz);
        if (fields != null)
            return fields;

        HashMap<String, Field> fieldMap = new HashMap<String, Field>();
        while (clazz != Object.class) {
            Field fs[] = clazz.getDeclaredFields();
            for (int i = 0; i < fs.length; ++i) {
                // not (final or static)
                if (0 != (fs[i].getModifiers() & 0x0018))
                    continue;
                fieldMap.put(fs[i].getName(), fs[i]);
            }
            clazz = clazz.getSuperclass();
        }

        fields = new ArrayList<Field>();
        for (String name : FIELDNAMES) {
            Field f = fieldMap.remove(name);
            if (f != null)
                fields.add(f);
        }
        fields.addAll(fieldMap.values());
        FIELDMAP.put(clazz, fields);
        return fields;
    }

    public static StringBuilder append(int indention, Object obj) {
        StringBuilder sb = new StringBuilder();
        for (Field f : getFields(obj.getClass())) {
            String fieldName = f.getName();
            if (fieldName.startsWith("__") || fieldName.equals("COMP"))
                continue;
            Object val;
            try {
                val = f.get(obj);
            } catch (Exception e) {
                continue;
            }
            if (val == null)
                continue;

            Class<?> type = f.getType();
            if (ArrayList.class.isAssignableFrom(type)) {
                ArrayList<?> al = (ArrayList<?>) val;
                if (al.size() > 0) {
                    Class<? extends Object> lType = al.get(0).getClass();
                    String lName = lType.getName();
                    int dot = lName.lastIndexOf('.');
                    lName = lName.substring(dot + 1);
                    lName = lName.substring(0, 1).toLowerCase() + lName.substring(1);

                    indent(sb, indention);
                    sb.append("<").append(fieldName).append(">\r\n");
                    if (String.class.isAssignableFrom(lType)) {
                        // name w/o 's'
                        lName = fieldName.substring(0, fieldName.length() - 1);
                        for (Object s : al) {
                            indent(sb, indention + 2);
                            sb.append("<").append(lName).append(">");
                            sb.append((String) s);
                            sb.append("</").append(lName).append(">\r\n");
                        }
                    } else {
                        for (Object o : al) {
                            // TODO: handle Strings
                            StringBuilder sub = append(indention + 4, o);
                            if (sub.length() > 0) {
                                indent(sb, indention + 2);
                                sb.append("<").append(lName).append(">");
                                sb.append("\r\n");
                                sb.append(sub);
                                indent(sb, indention + 2);
                                sb.append("</").append(lName).append(">\r\n");
                            }
                        }
                    }
                    indent(sb, indention);
                    sb.append("</").append(fieldName).append(">\r\n");
                }
            } else if (Map.class.isAssignableFrom(type)) {
                appendMap(sb, indention, fieldName, (Map<String, ?>) val);
            } else if (type.isPrimitive() || val instanceof String) {
                indent(sb, indention);
                sb.append("<").append(fieldName).append(">");
                if (val instanceof String) {
                    val = XmlFile.encode((String) val);
                }
                sb.append(val);
                sb.append("</").append(fieldName).append(">\r\n");
            } else {
                StringBuilder sub = append(indention + 2, val);
                if (sub.length() > 0) {
                    indent(sb, indention);
                    sb.append("<").append(fieldName).append(">");
                    sb.append("\r\n");
                    sb.append(sub);
                    indent(sb, indention);
                    sb.append("</").append(fieldName).append(">\r\n");
                }
            }
        }
        return sb;
    }

    private static void appendMap(StringBuilder sb, int indention, String fieldName, Map<String, ?> map) {
        if (map.size() > 0) {
            indent(sb, indention);
            sb.append("<").append(fieldName);
            Map<String, String> attrs = (Map<String, String>) map.get("\0");
            if (attrs != null)
                appendAttrs(sb, attrs);
            int len = sb.length();
            String content = (String) map.get("\0\0");
            if (content != null) {
                sb.append(">\r\n");
                indent(sb, indention);
                sb.append(content).append("\r\n");
            } else {
                for (Map.Entry<String, ?> e : map.entrySet()) {
                    String key = e.getKey();
                    // properties are handled inside the tag
                    if (key.charAt(0) == 0)
                        continue;

                    if (sb.length() == len)
                        sb.append(">\r\n");

                    Object o = e.getValue();
                    if (o instanceof String) {
                        indent(sb, indention + 2);
                        sb.append("<").append(key).append(">");
                        sb.append(o);
                        sb.append("</").append(key).append(">\r\n");
                    } else {
                        appendMap(sb, indention + 2, key, (Map<String, ?>) o);
                    }
                }
            }
            if (sb.length() == len) {
                if (sb.length() == len)
                    sb.append("/>\r\n");
            } else {
                indent(sb, indention);
                sb.append("</").append(fieldName).append(">\r\n");
            }
        }
    }

    private static void appendAttrs(StringBuilder sb, Map<String, String> attrs) {
        for (Entry<String, String> e : attrs.entrySet()) {
            sb.append(" ").append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
        }
    }

    private static void indent(StringBuilder sb, int indention) {
        while (indention > SPACES.length()) {
            sb.append(SPACES);
            indention -= SPACES.length();
        }
        sb.append(SPACES.substring(0, indention));
    }

    public static String resolveVars(Id id, String val, Map<String, String> resolvedVars) {
        for (int bra = val.indexOf("${");; bra = val.indexOf("${", bra + 1)) {
            if (bra < 0)
                break;
            int ket = val.indexOf("}", bra);
            String key = val.substring(bra + 2, ket);
            String repl = resolvedVars.get(key);
            if (repl == null) {
                Log.getLog().warn("[" + id.getId() + "] undefined variable: " + key);
                repl = "${" + key + "}";
                // resolvedVars.put(key, repl);
            }
            val = val.substring(0, bra) + repl + val.substring(ket + 1);
        }
        return val;
    }

    /**
     * Replace all occurencies of variables in this object's String and children.
     * 
     * @param obj
     *            current object
     * @param resolvedVars
     *            the variables (name, value)
     * @return this object
     * @throws Exception
     */
    public static void applyVariables(Id id, Object obj, HashMap<String, String> resolvedVars) throws Exception {
        for (Field f : getFields(obj.getClass())) {
            String fieldName = f.getName();
            try {
                if (fieldName.startsWith("__") || fieldName.equals("COMP"))
                    continue;
                Object val = f.get(obj);
                if (val == null)
                    continue;
                if (val instanceof String) {
                    String value = resolveVars(id, (String) val, resolvedVars);
                    if (!value.equals(val))
                        f.set(obj, value);
                    continue;
                }
                if (val instanceof URL) {
                    String value = resolveVars(id, val.toString(), resolvedVars);
                    f.set(obj, new URL(value));
                    continue;
                }
                Class<?> type = val.getClass();
                if (ArrayList.class.isAssignableFrom(type)) {
                    ArrayList<Object> al = (ArrayList<Object>) val;
                    for (int i = al.size() - 1; i >= 0; --i) {
                        Object o = al.get(i);
                        if (o instanceof String) {
                            o = resolveVars(id, (String) o, resolvedVars);
                            al.set(i, o);
                        } else {
                            // System.out.println(fieldName);
                            applyVariables(id, o, resolvedVars);
                            // System.out.println(o);
                        }
                    }
                    continue;
                }
                if (Map.class.isAssignableFrom(type)) {
                    Map<String, Object> r = applyVariablesToMap(id, (Map<String, Object>) val, resolvedVars);
                    f.set(obj, r);
                    continue;
                }
                // System.out.println(fieldName);
                applyVariables(id, val, resolvedVars);
            } catch (Exception e) {
                System.err.println("error accessing: " + obj.getClass().getName() + ": " + fieldName);
                throw e;
            }
        }
    }

    static Map<String, Object> applyVariablesToMap(Id id, Map<String, Object> map, Map<String, String> resolvedVars) {
        MultiMap<String, Object> r = new MultiMap<String, Object>();
        for (Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            Object o = e.getValue();
            // handle attributes and name
            if (key.charAt(0) == 0 && key.length() == 1) {
                Map<String, String> attrs = (Map<String, String>) o;
                MultiMap<String, String> m = new MultiMap<String, String>();
                for (Entry<String, String> f : attrs.entrySet()) {
                    String value = resolveVars(id, f.getValue(), resolvedVars);
                    m.put(f.getKey(), value);
                }
                r.put(key, m);
            }
            if (o instanceof String) {
                o = resolveVars(id, (String) o, resolvedVars);
            } else {
                o = applyVariablesToMap(id, (Map<String, Object>) o, resolvedVars);
            }
            r.put(key, o);
        }
        return r;
    }

    /**
     * What to merge and not to replace:
     * 
     * direct members: - dependencies : Id - ?dependencyManagement.dependencies : Id
     * 
     * properties : replace by name
     * 
     * id = null --> add - developers : id - contributors : id - repositories : id - pluginRepositories : id -
     * build.resources : id - build.testResources : id
     * 
     * - reporting.plugins - build.plugins - build.plugins.executions : with matching id -
     * 
     * build.plugins.executions.configuration -> merge configuration with plugin data:
     * xxx.jar!/META-INF/maven/plugin.xml/plugin/mojos/mojo/ - with goal in build.plugins.executions.goals
     * /plugin/mojos/mojo/configuration implementation="type" -> type = array --> add -> type instanceof Collection -->
     * add -> type instanceof Map --> put(key) (overrides old key) -> put
     * 
     */
    public static void merge(Object to, Object from) throws Exception {

        Class<? extends Object> type = to.getClass();
        String typeName = type.getName();
        int dot = typeName.lastIndexOf('.');
        typeName = typeName.substring(dot + 1);

        ArrayList<Field> fields = Bind.getFields(type);
        // merge auto mergeable
        for (Field f : fields) {
            String fieldName = f.getName();
            if (fieldName.startsWith("__") || fieldName.equals("COMP"))
                continue;

            Class<?> ctype = f.getType();
            // if (ctype.isPrimitive() || String.class.isAssignableFrom(ctype))
            // continue;

            // allow asymmetric merge, and skip fields which are not present in
            // the right object
            Object fromValue;
            try {
                Field fr = from.getClass().getField(fieldName);
                fromValue = fr.get(from);
            } catch (Exception ex) {
                // field not found? ignore!
                continue;
            }
            // transient elements are not merged
            if (0 != (f.getModifiers() & 0x80)) {
                f.set(to, fromValue);
                continue;
            }

            if (fromValue == null)
                continue;

            Object toValue = f.get(to);
            if (toValue == null) {
                f.set(to, fromValue);
                continue;
            }

            // some type
            // merge configured child elements
            fieldName = typeName + ":" + f.getName();
            if (SUBMERGE.contains(fieldName)) {
                merge(toValue, fromValue);
                continue;
            }

            // both are set
            if (ctype.isPrimitive() || toValue instanceof String) {
                f.set(to, fromValue);
                continue;
            }

            if (Map.class.isAssignableFrom(ctype)) {
                mergeMap((Map<String, String>) toValue, (Map<String, String>) fromValue);
                continue;
            }

            if (ArrayList.class.isAssignableFrom(ctype)) {
                ArrayList<Object> a1 = (ArrayList<Object>) toValue;
                ArrayList<Object> a2 = (ArrayList<Object>) fromValue;
                if (a1.size() == 0) {
                    a1.addAll(a2);
                    continue;
                }
                if (a2.size() == 0) {
                    continue;
                }
                a1 = mergeArrayList(a1, a2);
                f.set(to, a1);
                continue;
            }
        }
    }

    /**
     * add right values to left
     * 
     * @param to
     * @param from
     */
    static void mergeMap(Map<String, String> to, Map<String, String> from) {
        if (from.size() == 0)
            return;
        if (to.size() == 0) {
            to.putAll(from);
            return;
        }
        MultiMap<String, String> map = new MultiMap<String, String>();
        Iterator<Entry<String, String>> l = to.entrySet().iterator();
        Iterator<Entry<String, String>> r = from.entrySet().iterator();
        Entry<String, String> le = null;
        Entry<String, String> re = null;
        if (l.hasNext())
            le = l.next();
        if (r.hasNext())
            re = r.next();
        if (le != null && re != null)
            for (;;) {
                String lk = le.getKey();
                String rk = re.getKey();
                int diff = lk.compareTo(rk);
                if (diff < 0) {
                    map.put(le.getKey(), le.getValue());
                    if (!l.hasNext()) {
                        le = null;
                        break;
                    }
                    le = l.next();
                    continue;
                }
                if (diff > 0) {
                    map.put(re.getKey(), re.getValue());
                    if (!r.hasNext()) {
                        re = null;
                        break;
                    }
                    re = r.next();
                    continue;
                }
                // lk == rk
                // use right objects with same key
                for (;;) {
                    map.put(rk, re.getValue());
                    if (!r.hasNext()) {
                        re = null;
                        break;
                    }
                    re = r.next();
                    if (!re.getKey().equals(rk))
                        break;
                }
                // skip left ones
                for (;;) {
                    if (!l.hasNext()) {
                        le = null;
                        break;
                    }
                    le = l.next();
                    if (!le.getKey().equals(rk))
                        break;
                }

                if (le == null || re == null)
                    break;
            }

        if (le != null)
            for (;;) {
                map.put(le.getKey(), le.getValue());
                if (!l.hasNext())
                    break;
                le = l.next();
            }
        if (re != null)
            for (;;) {
                map.put(re.getKey(), re.getValue());
                if (!r.hasNext())
                    break;
                re = r.next();
            }
        to.clear();
        to.putAll(map);
    }

    static ArrayList<Object> mergeArrayList(ArrayList<Object> toList, ArrayList<Object> fromList) throws Exception {
        if (toList.size() == 0)
            return fromList;
        Class<?> mtype = toList.get(0).getClass();
        Field id = null;
        if (!Id.class.isAssignableFrom(mtype)) {
            try {
                id = mtype.getField("id");
                // System.out.println(id);
            } catch (Exception ex) {
                toList.addAll(fromList);
                return toList;
            }
        }
        // either id oder ga present
        ArrayList<Object> order = new ArrayList<Object>();
        MultiMap<Object, Object> toValues = new MultiMap<Object, Object>();
        for (Object to : toList) {
            Object key;
            if (id != null)
                key = id.get(to);
            else
                key = ((Id) to).getGA();

            if (key != null) {
                order.add(key);
                toValues.put(key, to);
            }
        }
        // rest is id==null
        ArrayList<Object> rest = new ArrayList<Object>();
        MultiMap<Object, Object> fromValues = new MultiMap<Object, Object>();
        for (Object from : fromList) {
            Object key;
            if (id != null)
                key = id.get(from);
            else
                key = ((Id) from).getGA();
            if (key != null) {
                order.add(key);
                fromValues.put(key, from);
            } else {
                rest.add(from);
            }
        }

        HashMap<Object, Object> doneToValues = new HashMap<Object, Object>();
        ArrayList<Object> merged = new ArrayList<Object>();
        for (Object key : order) {
            Object from = fromValues.remove(key);
            Object to = toValues.remove(key);
            if (from == null) {
                if (to == null)
                    continue;
                merged.add(to);
                continue;
            }
            if (to == null) {
                merged.add(from);
                continue;
            }
            // both are set
            if (id == null) {
                String ga = ((Id) from).getGA();
                Object done = doneToValues.get(ga);
                if (done != null && 0 != Id.COMP.compare((Id) from, (Id) done))
                    throw new Exception("dependency conflict: " + from + " <-> " + done);
            }
            Bind.merge(to, from);
            merged.add(to);
        }
        merged.addAll(rest);
        return merged;
    }

    /**
     * fields which are recursively merged
     */
    private final static HashSet<String> SUBMERGE = new HashSet<String>();

    static {
        SUBMERGE.add("Project:build");
        SUBMERGE.add("Project:dependencyManagement");
        SUBMERGE.add("Project:reporting");
        SUBMERGE.add("Build:pluginManagement");

        FIELDNAMES.add("modelVersion");
        FIELDNAMES.add("parent");
        FIELDNAMES.add("groupId");
        FIELDNAMES.add("artifactId");
        FIELDNAMES.add("version");
        FIELDNAMES.add("packaging");
        FIELDNAMES.add("id");
        FIELDNAMES.add("activation");
        FIELDNAMES.add("scope");
        FIELDNAMES.add("name");
        FIELDNAMES.add("description");
        FIELDNAMES.add("url");
        FIELDNAMES.add("properties");
        FIELDNAMES.add("dependencies");
        FIELDNAMES.add("build");
        FIELDNAMES.add("resources");
        FIELDNAMES.add("dependencyManagement");
        FIELDNAMES.add("pluginManagement");
        FIELDNAMES.add("plugins");
        FIELDNAMES.add("extensions");
        FIELDNAMES.add("executions");
    }

    public static Object dup(Object left) throws Exception {
        Class<?> type = left.getClass();
        Object right = type.newInstance();
        ArrayList<Field> fs = getFields(type);
        for (Field f : fs) {
            f.setAccessible(true);
            Object ol = f.get(left);
            if (ol == null)
                continue;
            String fieldName = f.getName();
            if (fieldName.startsWith("__") || fieldName.equals("COMP"))
                continue;
            Class<?> ftype = f.getType();
            if (ftype.isPrimitive() || String.class.isAssignableFrom(ftype) || URL.class.isAssignableFrom(ftype)
                    || Integer.class.isAssignableFrom(ftype)) {
                f.set(right, ol);
                continue;
            }
            if (ArrayList.class.isAssignableFrom(ftype)) {
                ArrayList<Object> al = (ArrayList<Object>) ol;
                ArrayList<Object> ar = new ArrayList<Object>();
                for (Object o : al) {
                    Class<?> c = o.getClass();
                    if (c.isPrimitive() || c.getName().startsWith("java."))
                        ar.add(o);
                    else
                        ar.add(dup(o));
                }
                f.set(right, ar);
                continue;
            }
            if (Map.class.isAssignableFrom(ftype)) {
                Map<Object, Object> hl = (Map<Object, Object>) ol;
                Map<Object, Object> hr = new MultiMap<Object, Object>();
                hr.putAll(hl);
                f.set(right, hr);
                continue;
            }
            f.set(right, dup(ol));
        }
        return right;
    }

    //    public static Map<String, Object> mergeParamMap(
    //            ArrayList<Parameter> params, Map<String, Object> left,
    //            Map<String, Object> right) {
    //        for (Parameter p : params) {
    //            Object val = right.get(p.name);
    //            if (val == null)
    //                continue;
    //
    //            Object lval = left.get(p.name);
    //            if (lval == null) {
    //                left.put(p.name, val);
    //                continue;
    //            }
    //
    //            // left an right exist
    //            if (ArrayList.class.isAssignableFrom(lval.getClass())) {
    //                ((ArrayList<Object>) lval).add(val);
    //                continue;
    //            }
    //
    //            if (Map.class.isAssignableFrom(lval.getClass())) {
    //
    //            }
    //        }
    //        return left;
    //    }

    public static Field getField(Class<?> clazz, String name) {
        while (clazz != Object.class) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception e) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static void setField(Object o, Field f, Object val) throws Exception {
        if (val == null)
            return;
        f.setAccessible(true);
        Class<?> cls = f.getType();
        if (String.class.isAssignableFrom(cls)) {
            f.set(o, val);
            return;
        }
        if (File.class.isAssignableFrom(cls)) {
            f.set(o, new File((String) val));
            return;
        }
        if (boolean.class.isAssignableFrom(cls)) {
            f.set(o, Boolean.valueOf((String) val));
            return;
        }
        if (List.class.isAssignableFrom(cls)) {
            final Type type = f.getGenericType();
            if (type instanceof ParameterizedType) {
                final ParameterizedType paraType = (ParameterizedType) type;
                final Type[] args = paraType.getActualTypeArguments();
                if (args.length == 1) {
                    final Class<?> argClass = (Class<?>) paraType.getActualTypeArguments()[0];
                    final String sxml = "<x>" + val + "</x>";
                    final XmlFile xml = new XmlFile();
                    xml.readString(sxml);
                    final ArrayList<Object> al = new ArrayList<Object>();
                    for (final String kp : xml.getSections("/x/")) {
                        final Object a = argClass.newInstance();
                        bind(xml, kp, a);
                        al.add(a);
                    }
                    f.set(o, al);
                    return;
                }
            }

        }
        try {
            f.set(o, val);
        } catch (Exception ex) {

            throw new Exception("TODO: " + cls, ex);
        }
    }

    public static void extendVariables(String path, Object o, HashMap<String, String> resolvedVars) throws Exception {
        Class<?> clazz = o.getClass();
        for (Field f : getFields(clazz)) {
            Class<?> type = f.getType();
            if (String.class.isAssignableFrom(type)) {
                Object val = f.get(o);
                if (val != null) {
                    // resolvedVars.put(path + f.getName().toLowerCase(),
                    // (String) val);
                    String n = path + f.getName();
                    resolvedVars.put(n, (String) val);
                }
                continue;
            }
            if (type.getName().startsWith("de.bb.tools.bnm.model")) {
                Object val = f.get(o);
                if (val != null)
                    // extendVariables(path + f.getName().toLowerCase() + ".",
                    // val, resolvedVars);
                    extendVariables(path + f.getName() + ".", val, resolvedVars);
                continue;
            }
        }
    }

    public static Object getValue(Object current, String key) {

        while (key.length() > 0) {
            int dot = key.indexOf('.');
            String rest = dot >= 0 ? key.substring(dot + 1) : "";
            if (dot < 0)
                dot = key.length();
            key = key.substring(0, dot);

            Field f = getField(current.getClass(), key);
            if (f == null)
                return null;

            try {
                current = f.get(current);
                if (current == null)
                    return null;
            } catch (Exception e) {
                return null;
            }
            key = rest;
        }
        return current;
    }

    public static void setDefaultValues(Object o) throws Exception {
        if (o == null)
            return;
        for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (final Field f : clazz.getDeclaredFields()) {
                final XmlElement xe = f.getAnnotation(XmlElement.class);
                if (xe != null && xe.defaultValue() != null) {
                    final Class<?> t = f.getType();
                    if (t.isAssignableFrom(String.class)) {
                        f.setAccessible(true);
                        f.set(o, xe.defaultValue());
                    } else if (t.isAssignableFrom(Integer.class) || clazz.getSimpleName().equals("int")) {
                        f.setAccessible(true);
                        f.set(o, Integer.parseInt(xe.defaultValue()));
                    } else if (t.isAssignableFrom(Long.class) || clazz.getSimpleName().equals("long")) {
                        f.setAccessible(true);
                        f.set(o, Long.parseLong(xe.defaultValue()));
                    }
                    // else {
                    // TODO: add futher conversions
                    // }
                }
            }
        }
    }
}
