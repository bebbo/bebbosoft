package de.bb.ws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Executor;

import jakarta.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.spi.ServiceDelegate;

import de.bb.net.HttpURLConnection;
import de.bb.util.XmlFile;

public class SD extends ServiceDelegate {

	private final static HashMap<String, String> TYPEMAP = new HashMap<String, String>();

	private String serviceName;

	private HashMap<QName, P> portMap;
	String targetNamespace;
	private HashMap<String, String> key2Prefix = new HashMap<String, String>();
	final Map<String, EL> elements = new HashMap<String, EL>();
	final Map<String, List<EL>> types = new HashMap<String, List<EL>>();

	private int unnamedCount;

	private URL wsdlDocumentLocation;

	SD(URL wsdlDocumentLocation, Class<? extends jakarta.xml.ws.Service> serviceClass) {
		final String query = wsdlDocumentLocation.getQuery();
		if (query != null && query.length() > 0) {
			final String surl = wsdlDocumentLocation.toString();
			try {
				this.wsdlDocumentLocation = new URL(surl.substring(0, surl.length() - query.length() - 1));
			} catch (MalformedURLException e) {
			}
		} else {
			this.wsdlDocumentLocation = wsdlDocumentLocation;
		}
		final XmlFile wsdl = loadXmlFile(wsdlDocumentLocation);
		parseWsdl(wsdl);
	}

	/**
	 * Used to create interface and classes.
	 * 
	 * @param wsdlFileName
	 *                     the wsdl file name
	 */
	public SD() {
	}

	SD(InputStream is) {
		XmlFile wsdl = new XmlFile();
		wsdl.read(is);
		parseWsdl(wsdl);
	}

	private static XmlFile loadXmlFile(URL wsdlDocumentLocation) {
		try {
			URLConnection con = new HttpURLConnection(wsdlDocumentLocation);
			con.connect();
			InputStream is = con.getInputStream();
			int inputLength = con.getContentLength();
			String sXml = readFully(is, inputLength == 0 ? Integer.MAX_VALUE : inputLength);
			if (sXml.length() == 0)
				throw new WebServiceException("no content for " + wsdlDocumentLocation);
			XmlFile xml = new XmlFile();
			xml.readString(sXml);
			return xml;
		} catch (IOException e) {
			throw new jakarta.xml.ws.WebServiceException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
		P p = portMap.get(portName);
		if (p == null)
			throw new WebServiceException("no such port: " + portName);

		InvocationHandler h = new H(this, p, serviceEndpointInterface);
		T proxy = (T) Proxy.newProxyInstance(serviceEndpointInterface.getClassLoader(),
				new Class[] { serviceEndpointInterface, BindingProvider.class }, h);

		return proxy;
	}

	public synchronized void parseWsdl(XmlFile wsdl) {
		if (portMap != null)
			return;
		if (wsdl == null)
			throw new WebServiceException("no WSDL definition");
		try {
			String root = wsdl.sections("/").next();
			String rootNs = "";
			String ns = "";
			int colon = root.indexOf(':');
			if (colon > 0) {
				rootNs = root.substring(1, colon);
				ns = rootNs + ":";
			}

			targetNamespace = wsdl.getString(root, "targetNamespace", null);

			// read the prefixes
			Map<String, String> attrs = wsdl.getAttributes(root);
			for (Entry<String, String> e : attrs.entrySet()) {
				String name = e.getKey();
				if (!name.startsWith("xmlns"))
					continue;
				if (name.length() > 5) {
					name = name.substring(6);
				} else {
					name = rootNs;
				}
				key2Prefix.put(e.getValue(), name);
			}

			key2Prefix.put(targetNamespace, "tns");

			String types = root + ns + "types/";
			String xsd = key2Prefix.get("http://www.w3.org/2001/XMLSchema");
			if (xsd == null) {
				String schema = wsdl.sections(types).next();
				attrs = wsdl.getAttributes(schema);
				for (Entry<String, String> e : attrs.entrySet()) {
					if (e.getValue().equals("http://www.w3.org/2001/XMLSchema")) {
						xsd = e.getKey();
						if (xsd.startsWith("xmlns:"))
							xsd = xsd.substring(6);
						break;
					}
				}
				if (xsd == null)
					xsd = "xsd";
			}
			parseSchema(wsdl, types + xsd + ":schema/", xsd);

			portMap = new HashMap<QName, P>();

			// there are many ports for the specified service
			for (String portKey : wsdl.getSections(root + ns + "service/")) {
				if (serviceName == null)
					serviceName = wsdl.getString(portKey, "name", "no service name");
				P p = new P(this, wsdl, portKey, root, ns);
				portMap.put(p.name, p);
			}
		} catch (WebServiceException wse) {
			throw wse;
		} catch (Exception ex) {
			throw new WebServiceException("invalid WSDL definition:\r\n" + wsdl.toString());
		}
	}

	private void parseSchema(XmlFile xml, String path, String xsd) {
		int xsdLen = path.length() + xsd.length() + 1;
		// name -> base
		HashMap<String, String> unresolved = new HashMap<String, String>();
		for (String key : xml.getSections(path)) {
			String what = strip(key.substring(xsdLen));

			// ignore - we'll use String
			if ("simpleType".equals(what)) {
				xml.setString(key, "type", "xsd:string");
				EL e = new EL(xml, key, true);
				elements.put(e.name, e);
				continue;
			}

			if ("import".equals(what)) {
				String loc = xml.getString(key, "schemaLocation", null);
				URL url;
				try {
					if (loc == null)
						throw new MalformedURLException("missing schemaLocation for " + key);
					url = new URL(loc);

				} catch (MalformedURLException e) {

					try {
						url = new URL(wsdlDocumentLocation, loc);
					} catch (MalformedURLException e1) {
						throw new WebServiceException(e);
					}
				}
				XmlFile xsdXml = loadXmlFile(url);
				Vector<String> root = xsdXml.getSections("/");
				if (root.size() == 0)
					throw new WebServiceException("referenced schema is empty: " + url);
				String rootkey = root.get(0);
				int colon = rootkey.indexOf(':');
				String prefix = rootkey.substring(1, colon);
				parseSchema(xsdXml, rootkey, prefix);
				continue;
			}

			if ("element".equals(what)) {
				// define the element if there is a type
				if (xml.getString(key, "type", null) != null) {
					EL e = new EL(xml, key);
					elements.put(e.name, e);
					continue;
				}
				// if not, assume an inner complex type
				String unnamed = "unnamed" + ++unnamedCount;
				xml.setString(key, "type", unnamed);
				EL e = new EL(xml, key);
				elements.put(e.name, e);

				// pass the name to the inner complex type
				what = "complexType";
				if (xsd.length() > 0)
					key += xsd + ":";
				key += "complexType/";
				xml.setString(key, "name", unnamed);
			}

			if ("complexType".equals(what)) {
				List<EL> ct = new ArrayList<EL>();
				String name = xml.getString(key, "name", null);
				Iterator<String> i = xml.sections(key);
				if (i.hasNext()) {
					String child = i.next();
					String whot = strip(child.substring(key.length() + xsd.length() + 1));

					if ("complexContent".equals(whot)) {
						// TODO: hack (? was ist hier kein hack?)
						String ext = child + "xs:extension/";
						String base = xml.getString(ext, "base", null);
						if (base == null)
							throw new WebServiceException("not yet implemented: " + ext + " no extension found");

						child = ext + "xs:sequence/";
						whot = "sequence";
						unresolved.put(name, O.rest(base));
					}

					if (!"sequence".equals(whot))
						throw new WebServiceException("not yet implemented: " + key + " -> " + whot);

					for (String elemKey : xml.getSections(child)) {
						EL e = new EL(xml, elemKey);
						ct.add(e);
					}
				}
				types.put(name, ct);

				continue;
			}

			System.out.println("TODO: " + key);
		}

		// handle unresolved stuff
		while (unresolved.size() > 0) {
			Entry<String, String> e = unresolved.entrySet().iterator().next();
			String name = e.getKey();
			String base = e.getValue();
			while (unresolved.containsKey(base)) {
				name = base;
				base = unresolved.get(name);
			}
			unresolved.remove(name);
			List<EL> myType = types.get(name);
			List<EL> baseType = types.get(base);
			// TODO handle name clashes?
			myType.addAll(baseType);
		}
	}

	private static String strip(String key) {
		String what = key;
		int no = what.indexOf('#');
		if (no > 0) {
			what = what.substring(0, no);
		} else {
			what = what.substring(0, what.length() - 1);
		}
		return what;
	}

	@Override
	public void addPort(QName portName, String bindingId, String endpointAddress) {
	}

	@Override
	public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode, WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Dispatch<T> createDispatch(EndpointReference endpointReference, Class<T> type, Mode mode,
			WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode,
			WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dispatch<Object> createDispatch(EndpointReference endpointReference, JAXBContext context, Mode mode,
			WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Executor getExecutor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HandlerResolver getHandlerResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getPort(Class<T> serviceEndpointInterface) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getPort(Class<T> serviceEndpointInterface, WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getPort(QName portName, Class<T> serviceEndpointInterface, WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
			WebServiceFeature... features) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<QName> getPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QName getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getWSDLDocumentLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExecutor(Executor executor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHandlerResolver(HandlerResolver handlerResolver) {
		// TODO Auto-generated method stub

	}

	static String readFully(InputStream is, int ctlen) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte buffer[] = new byte[16738];
		int total = 0;
		for (;;) {
			if (ctlen >= 0 && total == ctlen)
				break;
			int b0 = is.read();
			if (b0 < 0)
				break;
			bos.write(b0);
			++total;
			int len = is.available();
			if (len > 0) {
				if (len > buffer.length)
					len = buffer.length;
				int read = is.read(buffer);
				bos.write(buffer, 0, read);
				total += read;
			}
		}
		return bos.toString("utf-8");
	}

	/**
	 * Create Java Code from WSDL.
	 * 
	 * @throws IOException
	 */
	public void createCode(File dir, String pack) throws IOException {
		HashSet<String> used = new HashSet<String>();
		for (P p : portMap.values()) {
			p.createCode(dir, pack, this, used);
		}

		Stack<String> todo = new Stack<String>();
		todo.addAll(used);

		while (todo.size() > 0) {
			String typeName = todo.pop();
			typeName = O.rest(typeName);
			List<EL> type = types.get(typeName);
			if (type == null)
				continue;

			defineType(dir, pack, typeName, type, todo, used);
		}
	}

	private void defineType(File dir, String pack, String typeName, List<EL> typeList, Stack<String> todo,
			HashSet<String> used) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("// CREATED BY de.bb.ws.SD. EDIT AT DISCRETION.\r\n\r\n");
		if (pack != null)
			sb.append("package " + pack + ";\r\n");
		sb.append("public class " + typeName + " {\r\n");

		for (EL el : typeList) {
			String name = el.name;
			sb.append("  private ").append(mapType(el.type)).append(" ").append(name).append(";\r\n");
			if (!used.contains(el.type)) {
				todo.add(el.type);
				used.add(el.type);
			}
		}
		sb.append("\r\n");
		for (EL el : typeList) {
			String name = el.name;
			sb.append("  public ").append(mapType(el.type)).append(" get");
			sb.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
			sb.append("() {\r\n").append("    return this.").append(name).append(";\r\n  }\r\n");
			sb.append("  public void set");
			sb.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
			sb.append("(").append(mapType(el.type)).append(" ").append(name).append(") {\r\n");
			sb.append("    this.").append(name).append(" = ").append(name).append(";\r\n  }\r\n");
		}

		sb.append("}\r\n");
		File f = new File(dir, typeName + ".java");
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(sb.toString().getBytes());
		fos.close();
	}

	static {
		TYPEMAP.put("string", "String");
		TYPEMAP.put("boolean", "Boolean");
		TYPEMAP.put("int", "Integer");
		TYPEMAP.put("long", "Long");
		TYPEMAP.put("short", "Short");
		TYPEMAP.put("byte", "Byte");
		TYPEMAP.put("ArrayOf_xsd_string", "String []");

	}

	public String mapType(String type) {

		EL el = elements.get(type);
		if (el == null)
			el = elements.get(O.rest(type));

		String rtype;
		if (el != null && el.implicite) {
			rtype = mapType(el.type);
		} else {
			String ret = TYPEMAP.get(O.rest(type));
			if (ret != null) {
				rtype = ret;
			} else {
				rtype = O.rest(type);
			}
		}

		List<EL> tt = types.get(rtype);
		if (tt != null) {
			el = tt.iterator().next();
			if ("unbounded".equals(el.maxOccurs))
				rtype = "Collection<" + rtype + ">";
		}

		return rtype;
	}

	public Map<String, List<EL>> getTypes() {
		return types;
	}

	public P getPort(QName portName) {
		P p = portMap.get(portName);
		if (p == null)
			throw new WebServiceException("no such port: " + portName);

		return p;
	}
}
