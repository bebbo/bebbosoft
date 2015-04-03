package de.bb.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.jws.WebMethod;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;

import de.bb.util.Mime;
import de.bb.util.XmlFile;

class H implements InvocationHandler, Binding {

    private static final StackTraceElement[] EMPTY = {};

    private SD sd;
    private P p;
    private Class<?> sei;
    private HashMap<Method, O> webserviceMethods = new HashMap<Method, O>();
    private HashMap<Method, Object> simpleMethods = new HashMap<Method, Object>();
    private HashMap<String, Object> requestContext = new HashMap<String, Object>();
    private HashMap<String, Object> responseContext = new HashMap<String, Object>();
    private List<Handler> handlerChain = new ArrayList<Handler>();
    private de.bb.net.HttpURLConnection con;

    H(SD sd, P p, Class<?> serviceEndpointInterface) {
        this.sd = sd;
        this.p = p;
        this.sei = serviceEndpointInterface;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // check web services
        O op = webserviceMethods.get(method);
        if (op != null)
            return invokeMethod(op, method, args);

        // check other simple methods
        Object o = simpleMethods.get(method);
        if (o != null)
            return o;

        // handle methods
        String name = method.getName();
        if ("equals".equals(name) && method.getTypeParameters().length == 1) {
            return proxy.equals(args[0]);
        }

        // initialize the method - one time per method
        if ("getRequestContext".equals(name) && method.getTypeParameters().length == 0) {
            Object r = requestContext;
            simpleMethods.put(method, r);
            return r;
        }
        if ("getResponseContext".equals(name) && method.getTypeParameters().length == 0) {
            Object r = responseContext;
            simpleMethods.put(method, r);
            return r;
        }
        if ("getBinding".equals(name) && method.getTypeParameters().length == 0) {
            Binding r = this;
            simpleMethods.put(method, r);
            return r;
        }
        if ("getEndpointReference".equals(name)) {
            EndpointReference r = p;
            simpleMethods.put(method, r);
            return r;
        }
        if ("hashCode".equals(name) && method.getTypeParameters().length == 0) {
            Integer r = hashCode() ^ 0xdeadbeef;
            simpleMethods.put(method, r);
            return r;
        }
        if ("toString".equals(name) && method.getTypeParameters().length == 0) {
            String r = "Proxy for " + p.toString();
            simpleMethods.put(method, r);
            return r;
        }

        // verify method exists
        if (!method.getDeclaringClass().equals(sei))
            throw new UnsupportedOperationException(method.toString());

        // replace web method name
        WebMethod wm = method.getAnnotation(WebMethod.class);
        if (wm != null) {
            String on = wm.operationName();
            if (on.length() > 0)
                name = on;
        }

        // create a webservice method
        op = p.opMap.get(name);
        if (op == null)
            throw new UnsupportedOperationException(name);

        op.init(method);

        webserviceMethods.put(method, op);

        return invokeMethod(op, method, args);
    }

    private Object invokeMethod(O op, Method method, Object[] args) throws Exception {
        // T t = new T(method.toString());
        StringBuilder out = marshalIn(op, method, args);
        // t.diff("marshal");
        String content = performIO(out);
        // t.diff("performIO");
        Object o = unmarshalOut(op, method, args, content);
        // t.diff("unmarshal");
        return o;
    }

    private Object unmarshalOut(O op, Method method, Object[] args, String content) throws Exception {
        // parse the response
        XmlFile xml = new XmlFile();
        xml.setPreserveWhiteSpaces(true);
        xml.setEncoding("UTF-8");
        xml.readString(content);

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
            throw new WebServiceException("can not parse:\r\n" + content);

        Vector<String> ret = xml.getSections(rootKey);
        if (ret.size() != 1)
            throw new WebServiceException("no or to many entries in Body:\r\n" + content);

        String retKey = ret.get(0);

        if (retKey.endsWith(":Fault/")) {
            // it's a fault
            String faultKey = retKey + "detail/";
            Vector<String> faults = xml.getSections(faultKey);
            for (String fault : faults) {
                int colon = fault.lastIndexOf(':');
                String faultName = fault.substring(colon + 1, fault.length() - 1);
                Class<?> exc = op.exMap.get(faultName);
                if (exc != null) {
                    Exception e = (Exception) exc.newInstance();
                    Field f = Throwable.class.getDeclaredField("detailMessage");
                    f.setAccessible(true);
                    f.set(e, xml.getContent(fault));
                    e.setStackTrace(new Exception().getStackTrace());
                    throw e;
                }
            }
            if (faults.size() > 0) {
                String fs = faults.get(0);
                String msg = xml.getContent(fs);
                String f1 = fs.substring(0, fs.length() - 1);
                int slash = f1.lastIndexOf('/');
                f1 = f1.substring(slash + 1);
                int colon = f1.indexOf(':');
                f1 = f1.substring(colon + 1);
                Exception se = null;
                StackTraceElement[] st = EMPTY;
                if ("exception".equals(f1)) {
                    f1 = "java.lang.Exception";
                    for (Iterator<String> i = xml.sections(fs); i.hasNext();) {
                        String kx = i.next();
                        if (kx.endsWith("message/")) {
                            msg = xml.getContent(kx);
                            continue;
                        }
                        if (kx.endsWith("stackTrace/")) {
                            ArrayList<StackTraceElement> stes = new ArrayList<StackTraceElement>();
                            for (Iterator<String> j = xml.sections(kx); j.hasNext();) {
                                String ky = j.next();
                                String line = xml.getString(ky, "line", "-1");
                                int lineNo = -1;
                                try {
                                    lineNo = Integer.parseInt(line);
                                } catch (NumberFormatException nfe) {
                                }
                                StackTraceElement ste = new StackTraceElement(xml.getString(ky, "class", "?"),
                                        xml.getString(ky, "method", "?"), xml.getString(ky, "file", "?"), lineNo);
                                stes.add(ste);
                            }
                            st = stes.toArray(EMPTY);
                        }
                    }
                }
                try {
                    se = (Exception) sei.getClassLoader().loadClass(f1).getConstructor(String.class).newInstance(msg);
                } catch (Exception ex) {
                }
                if (se != null) {
                    se.setStackTrace(st);
                    throw new SOAPException("SOAP-Fault with " + p.url, se);
                }
            }

            throw new SOAPException("cannot decode: " + xml.getContent(retKey));
        }

        Object returnVal = null;

        Type pts[] = method.getGenericParameterTypes();

        // decode return values
        for (Entry<String, Integer> e : op.outParamNames.entrySet()) {
            int index = e.getValue();
            if (index < 0) {
                Type rt = method.getGenericReturnType();
                if (rt instanceof Class<?>) {
                    Class<?> cls = (Class<?>) rt;
                    if (cls.isPrimitive()) {
                        if (cls.getName().equals("void"))
                            continue;
                    }
                }
                returnVal = M.instantiate(sd.types, op.outputType, xml, retKey, e.getKey(), rt);
                continue;
            }

            Holder<Object> holder = (Holder<Object>) args[index];
            ParameterizedType pt = (ParameterizedType) pts[index];
            Type rt = pt.getActualTypeArguments()[0];
            holder.value = M.instantiate(sd.types, op.outputType, xml, retKey, e.getKey(), rt);
        }

        return returnVal;
    }

    private String performIO(StringBuilder out) {
        // send request
        // con = null;
        try {
            String request = out.toString();
            // System.out.println(request);
            byte[] data = request.getBytes("utf-8");

            // for (;;) {
            if (con == null) {
                con = new de.bb.net.HttpURLConnection(p.url);
            } else {
                con.reuse();
            }
            con.setDoOutput(true);
            con.addRequestProperty("Host", p.url.getHost());
            con.addRequestProperty("SOAPAction", "\"\"");
            con.addRequestProperty("Content-Length", Integer.toString(data.length));
            con.addRequestProperty("Content-Type", "text/xml; charset=utf-8");

            String userName = (String) requestContext.get(BindingProvider.USERNAME_PROPERTY);
            if (userName != null) {
                String passWord = (String) requestContext.get(BindingProvider.PASSWORD_PROPERTY);
                String val = "Basic " + new String(Mime.encode((userName + ":" + passWord).getBytes()));
                con.addRequestProperty("Authorization", val);
            }

            Map<String, List<String>> props = (Map<String, List<String>>) requestContext
                    .get(MessageContext.HTTP_REQUEST_HEADERS);
            if (props != null) {
                for (Entry<String, List<String>> e : props.entrySet()) {
                    String key = e.getKey();
                    for (String value : e.getValue()) {
                        con.addRequestProperty(key, value);
                    }
                }
            }

            OutputStream os = con.getOutputStream();
            os.write(data);
            os.flush();
            InputStream is = con.getInputStream();

            if (con.getResponseCode() == 401) {
                throw new WebServiceException("unauthorized: " + p.url);
            }

            // System.out.println(con.getResponseCode());
            int ctlen = con.getHeaderFieldInt("CONTENT-LENGTH", -1);
            if (ctlen == 0) {
                if ("close".equals(con.getHeaderField("CONNECTION")))
                    ctlen = -1;
            }

            Map<String, List<String>> headers = con.getHeaderFields();
            this.responseContext.put(MessageContext.HTTP_RESPONSE_HEADERS, headers);

            String content = SD.readFully(is, ctlen);
            // System.out.println(content);

            return content;
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new WebServiceException("I/O exception with " + p.url, e1);
        }
    }

    private StringBuilder marshalIn(O op, Method method, Object[] args) throws Exception {
        // marshal the params and invoke the webservice
        StringBuilder out = new StringBuilder();
        out.append(M.REQSTART).append(op.opName).append(" xmlns:tns=\"").append(sd.targetNamespace).append("\">");
        Type[] pts = method.getGenericParameterTypes();
        for (Entry<String, Integer> e : op.inParamNames.entrySet()) {
            String pName = e.getKey();
            int index = e.getValue();
            Object val = args[index];

            M.marshal(sd.types, out, op.inputType, pName, val, pts[index]);
        }

        out.append("</tns:").append(op.opName).append(M.REQEND);
        return out;
    }

    public String getBindingID() {
        return "http://schemas.xmlsoap.org/wsdl/soap/http";
    }

    public List<Handler> getHandlerChain() {
        return handlerChain;
    }

    public void setHandlerChain(List<Handler> chain) {
        handlerChain = chain;
    }

    // static class T {
    // long t;
    // private String title;
    //
    // T(String title) {
    // this.title = title;
    // t = System.currentTimeMillis();
    // }
    //
    // public void diff(String msg) {
    // long now = System.currentTimeMillis();
    // System.out.println(title + ":" + msg + " " + (now - t) + "ms");
    // t = now;
    // }
    //
    // }
}
