package de.bb.bejy.j2ee;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import de.bb.bejy.ServerThread;
import de.bb.bejy.http.HttpHandler;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;
import de.bb.ws.M;
import de.bb.ws.O;
import de.bb.ws.P;
import de.bb.ws.SD;

/**
 * Servlet implementation. Unmarshals the XML, invokes the handler object and marshals the return values.
 * 
 * @author stefan franke
 */
class WebServiceHandler extends HttpHandler {

    private static final HashMap<String, String> PRIMITIVES = new HashMap<String, String>();
    private static final HashMap<String, String> BUILTIN = new HashMap<String, String>();

    Object wsImpl;
    String serviceName;
    private String namespace;
    private String portName;
    private HashMap<String, Method> methods;
    private String portType;
    private SD sd;

    /**
     * Initialize all data to handle requests.
     * 
     * @param wsClass
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IntrospectionException
     */
    WebServiceHandler(Class<?> wsClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            UnsupportedEncodingException, IntrospectionException {
        wsImpl = wsClass.newInstance();

        WebService wsAnnotation = wsClass.getAnnotation(WebService.class);

        // calculate a service name
        String baseName = wsClass.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0)
            baseName = baseName.substring(dot + 1);
        String wsName = baseName + "Service";
        serviceName = wsAnnotation.serviceName().length() > 0 ? wsAnnotation.serviceName() : wsName;

        // calculate a name space
        namespace = "";
        for (StringTokenizer st = new StringTokenizer(wsClass.getName().substring(0, dot), "."); st.hasMoreElements();) {
            if (namespace.length() > 0) {
                namespace = st.nextToken() + "." + namespace;
            } else {
                namespace = st.nextToken();
            }
        }
        namespace = wsAnnotation.targetNamespace() != null && wsAnnotation.targetNamespace().length() > 0 ? wsAnnotation
                .targetNamespace() : "http://" + namespace + "/";

        // calculate a port name
        portName = wsAnnotation.portName() != null ? wsAnnotation.portName() : serviceName + "Port";

        // the port type
        portType = wsAnnotation.name();

        // collect the methods having a ws annotation
        methods = new HashMap<String, Method>();
        String iface = wsAnnotation.endpointInterface();
        if (iface != null && iface.length() > 0) {
            if (portType == null || portType.length() == 0) {
                dot = iface.lastIndexOf('.');
                portType = iface.substring(dot + 1);
            }
            final Class<?> ifaceClass = wsClass.getClassLoader().loadClass(iface);
            for (final Method m : ifaceClass.getDeclaredMethods()) {
                final WebMethod wm = m.getAnnotation(WebMethod.class);
                final String name = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();
                methods.put(name, m);
            }
        } else {
            if (portType == null || portType.length() == 0) {
                portType = baseName;
            }
            for (Class<?> c = wsClass; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    final WebMethod wm = m.getAnnotation(WebMethod.class);
                    final String name = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m
                            .getName();
                    methods.put(name, m);
                }
                for (Class<?> i : c.getInterfaces()) {
                    for (Method m : i.getDeclaredMethods()) {
                        final WebMethod wm = m.getAnnotation(WebMethod.class);
                        final String name = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m
                                .getName();
                        methods.put(name, m);
                    }
                }
            }
        }

        sd = new SD();
        XmlFile xml = new XmlFile();
        xml.readString(createWsdl("http://foo"));
        sd.parseWsdl(xml);
    }

    /**
     * The servicing method. With ?WSDL the WSDL is delivered. With POST the WSDL is invoked.
     */
    public void service(final ServletRequest request, final ServletResponse response) throws ServletException,
            IOException {
        try {
            final HttpServletRequest hr = ((HttpServletRequest) request);
            String httpMethod = hr.getMethod();
            if ("GET".equals(httpMethod)) {
                if ("WSDL".equalsIgnoreCase(hr.getQueryString())) {
                    response.setContentType("text/xml;charset=utf-8");
                    String url = HttpUtils.getRequestURL(hr).toString();
                    byte[] wsdlData = createWsdl(url).getBytes("utf-8");
                    response.setContentLength(wsdlData.length);
                    response.getOutputStream().write(wsdlData);
                    return;
                }
            } else if ("POST".equals(httpMethod)) {
                XmlFile xml = new XmlFile();
                xml.read(new LimitedInputStream(request.getInputStream(), request.getContentLength()));
                Vector<String> root = xml.getSections("/");
                String rootKey = null;
                if (root.size() == 1) {
                    String tmpKey = root.get(0);
                    int colon = tmpKey.indexOf(':');
                    if (colon > 0) {
                        String ns = tmpKey.substring(1, colon);
                        rootKey = tmpKey + ns + ":Body/";
                    }
                }
                if (rootKey == null)
                    throw new WebServiceException("can not parse:\r\n" + xml);

                Vector<String> ret = xml.getSections(rootKey);
                if (ret.size() != 1)
                    throw new WebServiceException("no or to many entries in Body:\r\n" + xml);

                String fx = ret.get(0);
                int dash = fx.lastIndexOf('/');
                int dash2 = fx.lastIndexOf('/', dash - 1);
                int colon = fx.indexOf(':', dash2);
                if (colon > 0)
                    dash2 = colon;
                String fxName = fx.substring(dash2 + 1, dash);
                Method method = methods.get(fxName);

                Type pts[] = method.getGenericParameterTypes();
                Object args[] = new Object[pts.length];

                P p = sd.getPort(new QName(namespace, portName));
                O op = p.getOperation(method);
                // decode parameter values
                for (Entry<String, Integer> e : op.getInParameters()) {
                    int index = e.getValue();
                    args[index] = M.instantiate(sd.getTypes(), op.getInputType(), xml, fx, e.getKey(), pts[index]);
                }
                // add holders
                for (Entry<String, Integer> e : op.getOutParameters()) {
                    int index = e.getValue();
                    if (index >= 0 && args[index] == null)
                        args[index] = new Holder<Object>();
                }

                // invoke the method
                ThreadContext tc = new ThreadContext();
                try {
                    ((ServerThread) Thread.currentThread()).setContext(tc);
                    Object retVal = method.invoke(wsImpl, args);
                    // marshal the return values 
                    StringBuilder out = new StringBuilder();
                    out.append(M.RESPSTART).append(fxName).append(" xmlns:tns=\"").append(namespace).append("\">");
                    for (Entry<String, Integer> e : op.getOutParameters()) {
                        String pName = e.getKey();
                        int index = e.getValue();
                        Object val = index >= 0 ? args[index] : retVal;

                        M.marshal(sd.getTypes(), out, op.getOutputType(), pName, val,
                                index >= 0 ? pts[index] : method.getGenericReturnType());
                    }
                    out.append("</tns:").append(fxName).append(M.RESPEND);

                    response.getWriter().write(out.toString());
                    return;
                } finally {
                    tc.release();
                }
            }
            ((HttpServletResponse) response).sendError(405);
        } catch (IntrospectionException ex) {
            throw new IOException(ex);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private String createWsdl(String url) throws UnsupportedEncodingException, IntrospectionException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?><definitions");

        // xmlns
        sb.append(" xmlns=\"http://schemas.xmlsoap.org/wsdl/\"");
        sb.append(" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"");
        sb.append(" xmlns:wsam=\"http://www.w3.org/2007/05/addressing/metadata\"");
        sb.append(" xmlns:tns=\"").append(namespace).append("\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");

        sb.append(" targetNamespace=\"").append(namespace).append("\"");
        sb.append(" name=\"").append(serviceName).append("\">");

        // types
        sb.append("<types><xs:schema version=\"1.0\" targetNamespace=\"").append(namespace).append("\">");
        insertTypes(sb);
        sb.append("</xs:schema></types>");

        // messages
        for (final Method m : methods.values()) {
            final WebMethod wm = m.getAnnotation(WebMethod.class);
            final String methodName = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();

            sb.append("<message name=\"").append(methodName).append("\">");
            sb.append("<part name=\"parameters\" element=\"tns:").append(methodName).append("\"/>")
                    .append("</message>");
            sb.append("<message name=\"").append(methodName).append("Response\">");
            sb.append("<part name=\"parameters\" element=\"tns:").append(methodName).append("Response\"/>")
                    .append("</message>");
        }

        // port / operations
        sb.append("<portType name=\"").append(portType).append("\">");
        for (Method m : methods.values()) {
            final WebMethod wm = m.getAnnotation(WebMethod.class);
            final String methodName = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();
            sb.append("<operation name=\"").append(methodName).append("\">");
            sb.append("<input wsam:Action=\"").append(namespace).append(portType).append("/").append(methodName);
            sb.append("Request").append("\" message=\"tns:").append(methodName).append("\"/>");
            sb.append("<output wsam:Action=\"").append(namespace).append(portType).append("/").append(methodName);
            sb.append("Response").append("\" message=\"tns:").append(methodName).append("Response\"/>");
            sb.append("</operation>");
        }
        sb.append("</portType>");

        // binding / operation
        sb.append("<binding name=\"").append(portName).append("Binding\" type=\"tns:").append(portType).append("\">");
        sb.append("<soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>");
        for (Method m : methods.values()) {
            final WebMethod wm = m.getAnnotation(WebMethod.class);
            final String methodName = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();
            sb.append("<operation name=\"").append(methodName).append("\">");
            sb.append("<soap:operation soapAction=\"\"/><input><soap:body use=\"literal\"/></input><output><soap:body use=\"literal\"/></output>");
            sb.append("</operation>");
        }
        sb.append("</binding>");

        // service / port
        sb.append("<service name=\"").append(serviceName).append("\">");
        sb.append("<port name=\"").append(portName).append("\" binding=\"tns:").append(portName).append("Binding\">");
        sb.append("<soap:address location=\"").append(url).append("\" /></port></service>");
        sb.append("</definitions>");
        return sb.toString();
    }

    private void insertTypes(StringBuilder sb) throws IntrospectionException {
        HashSet<Class<?>> complexTypes = new HashSet<Class<?>>();
        for (Method m : methods.values()) {
            final WebMethod wm = m.getAnnotation(WebMethod.class);
            final String methodName = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();
            sb.append("<xs:element name=\"").append(methodName).append("\" type=\"tns:").append(firstLower(methodName))
                    .append("\"/>");
            sb.append("<xs:element name=\"").append(methodName).append("Response\" type=\"tns:")
                    .append(firstLower(methodName)).append("Response\"/>");
        }
        for (Method m : methods.values()) {
            final WebMethod wm = m.getAnnotation(WebMethod.class);
            final String methodName = wm != null && wm.operationName().length() > 0 ? wm.operationName() : m.getName();
            sb.append("<xs:complexType name=\"").append(firstLower(methodName)).append("Response\">")
                    .append("<xs:sequence>");

            // return value is an out parameter
            Type type = m.getGenericReturnType();
            String name = "result";
            if (m.isAnnotationPresent(WebResult.class)) {
                WebResult wr = m.getAnnotation(WebResult.class);
                if (wr.name().length() > 0)
                    name = wr.name();
            }
            Class<?> added = addType(sb, name, type, false);
            if (added != null)
                complexTypes.add(added);

            // add the out paramters 
            Annotation[][] pas = m.getParameterAnnotations();
            Type[] pts = m.getGenericParameterTypes();
            for (int i = 0; i < pts.length; ++i) {
                Annotation[] pa = pas[i];
                WebParam wp = null;
                for (Annotation a : pa) {
                    if (a instanceof WebParam) {
                        wp = (WebParam) a;
                        break;
                    }
                }
                if (wp != null && wp.mode() == Mode.INOUT || wp.mode() == Mode.OUT) {
                    name = "argument" + i;
                    if (wp.name().length() > 0)
                        name = wp.name();
                    added = addType(sb, name, pts[i], false);
                    if (added != null)
                        complexTypes.add(added);
                }
            }
            sb.append("</xs:sequence></xs:complexType>");

            sb.append("<xs:complexType name=\"").append(firstLower(methodName)).append("\">").append("<xs:sequence>");
            // add the in paramaters
            for (int i = 0; i < pts.length; ++i) {
                Annotation[] pa = pas[i];
                WebParam wp = null;
                for (Annotation a : pa) {
                    if (a instanceof WebParam) {
                        wp = (WebParam) a;
                        break;
                    }
                }
                if (wp != null && wp.mode() == Mode.INOUT || wp.mode() == Mode.IN) {
                    name = "argument" + i;
                    if (wp.name().length() > 0)
                        name = wp.name();
                    added = addType(sb, name, pts[i], false);
                    if (added != null)
                        complexTypes.add(added);
                }
            }
            sb.append("</xs:sequence></xs:complexType>");
        }

        // now add the pending elements
        ArrayList<Class<?>> todo = new ArrayList<Class<?>>();
        todo.addAll(complexTypes);
        while (todo.size() > 0) {
            Class<?> cls = todo.remove(todo.size() - 1);
            String name = cls.getName();
            int dot = name.lastIndexOf('.');
            name = name.substring(dot + 1);
            sb.append("<xs:complexType name=\"").append(name).append("\"><xs:sequence>");
            SortedMap<String, Field> fields = new SingleMap<String, Field>();
            for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if ((f.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)
                        fields.put(f.getName(), f);
                }
            }
            for (Field f : fields.values()) {
                Class<?> added = addType(sb, f.getName(), f.getGenericType(), false);
                if (added != null && complexTypes.add(added)) {
                    todo.add(added);
                }
            }

            sb.append("</xs:sequence></xs:complexType>");
        }
    }

    private static String firstLower(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private Class<?> addType(StringBuilder sb, String name, Type type, boolean unbounded) {
        if (type instanceof Class) {
            Class<?> cls = (Class<?>) type;

            // handle primitve types
            if (cls.isPrimitive()) {
                if (cls.getName().equals("void"))
                    return null;
                String stype = PRIMITIVES.get(cls.getName());
                if (stype == null)
                    throw new RuntimeException("TODO: " + type);
                sb.append("<xs:element name=\"").append(name).append("\" type=\"").append(stype).append("\"/>");
                return null;
            }

            String stype = BUILTIN.get(cls.getName());
            if (stype != null) {
                sb.append("<xs:element name=\"").append(name).append("\" type=\"").append(stype)
                        .append("\" minOccurs=\"0\"");
                if (unbounded)
                    sb.append(" maxOccurs=\"unbounded\"");
                sb.append("/>");
                return null;
            }
            stype = cls.getName();
            int dot = stype.lastIndexOf('.');
            stype = "tns:" + stype.substring(dot + 1);
            sb.append("<xs:element name=\"").append(name).append("\" type=\"").append(stype)
                    .append("\" minOccurs=\"0\"");
            if (unbounded)
                sb.append(" maxOccurs=\"unbounded\"");
            sb.append("/>");
            return cls;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            Class<?> cls = (Class<?>) ptype.getRawType();
            if (Collection.class.isAssignableFrom(cls)) {
                return addType(sb, name, ptype.getActualTypeArguments()[0], true);
            }
            if (Holder.class.isAssignableFrom(cls)) {
                return addType(sb, name, ptype.getActualTypeArguments()[0], false);
            }
        }
        throw new RuntimeException("TODO: " + type);
    }

    static {
        PRIMITIVES.put("int", "xs:int");
        PRIMITIVES.put("long", "xs:long");
        PRIMITIVES.put("float", "xs:decimal");
        PRIMITIVES.put("double", "xs:decimal");
        PRIMITIVES.put("boolean", "xs:boolean");
        PRIMITIVES.put("short", "xs:short");
        PRIMITIVES.put("char", "xs:short");
        PRIMITIVES.put("byte", "xs:byte");

        BUILTIN.put("java.lang.Boolean", "xs:boolean");
        BUILTIN.put("java.lang.Integer", "xs:int");
        BUILTIN.put("java.lang.Long", "xs:long");
        BUILTIN.put("java.lang.Float", "xs:decimal");
        BUILTIN.put("java.lang.Double", "xs:decimal");
        BUILTIN.put("java.math.BigDecimal", "xs:decimal");
        BUILTIN.put("java.lang.String", "xs:string");
        BUILTIN.put("java.util.Date", "xs:dateTime");
    }
}
