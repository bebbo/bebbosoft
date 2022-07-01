package de.bb.ws;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.Holder;
import jakarta.xml.ws.WebServiceException;

import de.bb.util.DateFormat;
import de.bb.util.Mime;
import de.bb.util.XmlFile;

/**
 * Handle the marshalling / unmarshalling
 * 
 * @author stefan franke
 * 
 */
public class M {

    public final static String REQSTART = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Body>\r\n<tns:";
    public final static String REQEND = ">\r\n</SOAP-ENV:Body></SOAP-ENV:Envelope>";
    public final static String RESPSTART = "<?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body>\r\n<tns:";
    public final static String RESPEND = ">\r\n</S:Body></S:Envelope>";

    private final static DateFormat DATEFORMAT = new DateFormat("yyyy-MM-ddTHH:mm:sszzz:00");

    public static void marshal(Map<String, List<EL>> types, StringBuilder out, String typeName, String pName,
            Object val, Type type) throws Exception {
        List<EL> typ = types.get(typeName);
        if (typ == null)
            throw new Exception(typeName + " is not mapped");

        EL el = null;
        for (EL e : typ) {
            if (e.name.equals(pName)) {
                el = e;
                break;
            }
        }
        if (el == null)
            throw new Exception(pName + " is not mapped for " + typeName);
        if (el.minOccurs == 0 && val == null)
            return;

        Done: {
            String rtype = O.rest(el.type);
            if ("string".equals(rtype) || "int".equals(rtype) || "boolean".equals(rtype) || "long".equals(rtype)
                    || "short".equals(rtype) || "decimal".equals(rtype)) {

                Class<?> vc = val.getClass();
                if (Holder.class.isAssignableFrom(vc)) {
                    marshal(types, out, typeName, pName, ((Holder<?>) val).value,
                            ((ParameterizedType) type).getActualTypeArguments()[0]);
                    return;
                }
                if (vc.isArray()) {
                    int length = Array.getLength(val);
                    for (int index = 0; index < length; ++index) {
                        Object o = Array.get(val, index);
                        out.append("<").append(pName).append(">");
                        encode(out, o.toString());
                        out.append("</").append(pName).append(">");
                    }
                    break Done;
                }
                if (Collection.class.isAssignableFrom(vc)) {
                    Collection<?> c = (Collection<?>) val;
                    for (Object o : c) {
                        out.append("<").append(pName).append(">");
                        encode(out, o.toString());
                        out.append("</").append(pName).append(">");
                    }
                    break Done;
                }

                out.append("<").append(pName).append(">");
                encode(out, val.toString());
                out.append("</").append(pName).append(">");
                break Done;
            }
            if ("dateTime".equals(rtype)) {
                if (val instanceof Date) {
                    Date d = (Date) val;
                    out.append("<").append(pName).append(">");
                    out.append(DATEFORMAT.format(d.getTime()));
                    out.append("</").append(pName).append(">");
                    break Done;
                }
                if (val instanceof Long) {
                    out.append("<").append(pName).append(">");
                    out.append(DATEFORMAT.format((Long) val));
                    out.append("</").append(pName).append(">");
                    break Done;
                }
                throw new WebServiceException("dateTime");
            }
            if ("base64Binary".equals(rtype)) {
                if (type instanceof GenericArrayType) {
                    GenericArrayType gat = (GenericArrayType) type;
                    Type innertype = gat.getGenericComponentType();
                    if (innertype instanceof GenericArrayType) {
                        Object[] os = (Object[]) val;
                        for (Object o : os) {
                            appendArray(out, pName, o);
                        }
                    } else {
                        appendArray(out, pName, val);
                    }
                    break Done;
                }
                Class<?> vc = val.getClass();
                if (vc.getName().startsWith("[[")) {
                    Object[] os = (Object[]) val;
                    for (Object o : os) {
                        appendArray(out, pName, o);
                    }
                } else {
                    appendArray(out, pName, val);
                }
                break Done;
            }

            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;
                if (clazz.isArray()) {
                    Class<?> at = clazz.getComponentType();
                    if (!at.isPrimitive()) {
                        Object os[] = (Object[]) val;
                        String subType = O.rest(el.type);
                        List<EL> hel = types.get(subType);
                        for (Object o : os) {
                            marshalObject(types, out, pName, subType, hel, at, o);
                        }
                        break Done;
                    }
                    throw new WebServiceException("todo1");
                }
                if (clazz.isAssignableFrom(Collection.class)) {
                    throw new WebServiceException("todo2");
                }
                // normal object - get all fields
                String subtype = O.rest(el.type);
                List<EL> hel = types.get(subtype);
                marshalObject(types, out, pName, subtype, hel, clazz, val);
                return;
            }

            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Type tp[] = pt.getActualTypeArguments();
                Type param = tp[0];
                Class<?> raw = (Class<?>) pt.getRawType();
                if (Collection.class.isAssignableFrom(raw)) {
                    Collection<?> c = (Collection<?>) val;
                    for (Iterator<?> i = c.iterator(); i.hasNext();) {
                        Object o = i.next();
                        marshal(types, out, typeName, pName, o, param);
                    }
                }
                return;
            }

            throw new WebServiceException("todo3");
        }
    }

    public static void marshalObject(Map<String, List<EL>> types, StringBuilder out, String pName, String subType,
            List<EL> hel, Class<?> at, Object o) throws Exception, NoSuchFieldException {
        out.append("<").append(pName).append(">");
        for (EL el : hel) {
            String name = el.name;
            Field f = null;
            for (Class<?> c = at; c != null; c = c.getSuperclass()) {
                try {
                    f = c.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException nsfe) {
                }
            }
            if (f == null)
                throw new WebServiceException("undeclared field: " + name + " in " + at.getName()
                        + " - client/server are out of sync?");
            f.setAccessible(true);
            Object fv = f.get(o);
            marshal(types, out, subType, name, fv, f.getGenericType());
        }
        out.append("</").append(pName).append(">");
    }

    private static void appendArray(StringBuilder out, String pName, Object o) {
        out.append("<").append(pName).append(">");
        out.append(new String(Mime.encode((byte[]) o)));
        out.append("</").append(pName).append(">");
    }

    private static void encode(StringBuilder out, String string) {
        for (int index = 0; index < string.length(); ++index) {
            char c = string.charAt(index);
            if (c == '<') {
                out.append("&lt;");
                continue;
            }
            if (c == '&') {
                out.append("&amp;");
                continue;
            }
            out.append(c);
        }
    }

    /**
     * currently unused.
     * 
     * @param string
     * @return
     */
    private static String decode(String string) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < string.length(); ++index) {
            char c = string.charAt(index);
            if (c == '&') {
                String rest = string.substring(index + 1);
                if (rest.startsWith("amp;")) {
                    sb.append("&");
                    index += 4;
                    continue;
                }
                if (rest.startsWith("lt;")) {
                    sb.append("<");
                    index += 3;
                    continue;
                }
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static Object instantiate(Map<String, List<EL>> types, XmlFile xml, String retKey, String name,
            String typeName, Type type) throws Exception {
        typeName = O.rest(typeName);
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Class<?> raw = (Class<?>) pt.getRawType();
            if (Collection.class.isAssignableFrom(raw)) {
                ArrayList<Object> al = new ArrayList<Object>();
                String path = retKey + name;

                Type tp[] = pt.getActualTypeArguments();
                Type param = tp[0];
                for (String k : xml.getSections(path)) {
                    Object o = instantiate(types, xml, k, "", typeName, param);
                    al.add(o);
                }
                return al;
            }
            throw new UnsupportedOperationException("cannot handle: " + raw);
        }

        if (type instanceof Class<?>) {

            Class<?> clazz = (Class<?>) type;
            if (clazz.isAssignableFrom(Collection.class)) {
                ArrayList<Object> al = new ArrayList<Object>();
                Class<?> ec;
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    ec = (Class<?>) pt.getActualTypeArguments()[0];
                } else {
                    ec = guessClass(typeName);
                }
                for (String nkey : xml.getSections(retKey + name)) {
                    Object o = instantiate2(types, xml, ec, nkey, typeName);
                    al.add(o);
                }
                return al;
            }

            if (clazz.isArray()) {
                ArrayList<Object> al = new ArrayList<Object>();
                Class<?> ec = clazz.getComponentType();
                for (String nkey : xml.getSections(retKey + name + "/")) {
                    Object o = instantiate2(types, xml, ec, nkey, typeName);
                    al.add(o);
                }

                Object[] r = (Object[]) Array.newInstance(ec, al.size());
                r = al.toArray(r);
                return r;
            }

            if (name.length() > 0)
                retKey += name + "/";
            return instantiate2(types, xml, clazz, retKey, typeName);
        }

        throw new UnsupportedOperationException("TODO");
    }

    private static Class<?> guessClass(String typeName) {
        if ("int".equals(typeName))
            return Integer.class;
        return null;
    }

    private static Object instantiate2(Map<String, List<EL>> types, XmlFile xml, Class<?> clazz, String key,
            String typeName) throws Exception, IllegalAccessException {
        if (clazz.isPrimitive() || clazz.isAssignableFrom(Integer.class) || clazz.isAssignableFrom(Long.class)
                || clazz.isAssignableFrom(Short.class) || clazz.isAssignableFrom(Boolean.class)) {
            String val = xml.getContent(key);
            String cn = clazz.getSimpleName();
            if ("boolean".equals(cn) || clazz.isAssignableFrom(Boolean.class))
                return val == null ? false : Boolean.parseBoolean(val);
            if ("short".equals(cn) || clazz.isAssignableFrom(Short.class))
                return val == null ? (short) 0 : Short.parseShort(val);
            if ("int".equals(cn) || clazz.isAssignableFrom(Integer.class))
                return val == null ? 0 : Integer.parseInt(val);
            if ("long".equals(cn) || clazz.isAssignableFrom(Long.class))
                return val == null ? 0L : Long.parseLong(val);
        }

        if (clazz.isAssignableFrom(String.class)) {
            String val = xml.getContent(key);
            return val;
        }
        if ("string".equals(typeName)) {
            Constructor<?> ct = clazz.getConstructor(String.class);
            String val = xml.getContent(key);
            return ct.newInstance(val);
        }
        if ("decimal".equals(typeName)) {
            Constructor<?> ct = clazz.getConstructor(String.class);
            String val = xml.getContent(key);
            if (val == null)
                return null;
            if ("NaN".equals(val))
                return null;
            return ct.newInstance(val);
        }

        if (clazz.isAssignableFrom(Date.class)) {
            String val = xml.getContent(key);
            if (val == null)
                return null;
            try {
                return new Date(Long.parseLong(val));
            } catch (NumberFormatException nfe) {
                return new Date(DateFormat.parse_yyyy_MM_dd_HH_mm_ss_GMT_zz_zz(val));
            }
        }

        Object o = clazz.newInstance();
        List<EL> elMap = types.get(typeName);

        for (EL el : elMap) {
            String attrName = el.name;
            /*
             * TODO PropertyDescriptor[] descriptors =
             * Introspector.getBeanInfo(object
             * .getClass()).getPropertyDescriptors(); for (PropertyDescriptor
             * descriptor : descriptors) { Method readMethod =
             * descriptor.getReadMethod(); if (readMethod != null) { Object
             * value = invoke(readMethod, object);
             * result.put(descriptor.getName(), doMap(value)); } } return
             * result;
             */
            Field f = null;
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                try {

                    f = c.getDeclaredField(attrName);
                    break;
                } catch (NoSuchFieldException nsfe) {
                }
            }
            f.setAccessible(true);
            Object val = instantiate(types, xml, key, attrName, el.type, f.getGenericType());
            f.set(o, val);
        }

        return o;
    }

    public static Object instantiate(Map<String, List<EL>> types, String outputType, XmlFile xml, String retKey,
            String name, Type type) throws Exception {
        List<EL> elmap = types.get(outputType);
        EL el = null;
        for (EL e : elmap) {
            if (e.name.equals(name)) {
                el = e;
                break;
            }
        }
        return instantiate(types, xml, retKey, name, el.type, type);
    }

}
