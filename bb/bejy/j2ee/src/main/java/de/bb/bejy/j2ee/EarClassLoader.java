package de.bb.bejy.j2ee;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jws.WebService;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.transaction.TransactionScoped;
import javax.transaction.Transactional;
import javax.xml.ws.WebServiceContext;

import de.bb.bejy.MiniClass;
import de.bb.bejy.http.WebAppContext;
import de.bb.log.Logger;
import de.bb.sql.DataSource;
import de.bb.sql.JdbcFactory;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.ZipClassLoader;

/**
 * The ear's class loader.
 * 
 * @author stefan franke
 * 
 */
public class EarClassLoader extends ZipClassLoader {

	private final static Logger LOG = Logger.getLogger(EarClassLoader.class);

	public static final String JAVAX_JWS_WEB_SERVICE = "Ljavax/jws/WebService;";
	public static final String JAVAX_EJB_STATEFUL = "Ljavax/ejb/Stateful;";
	public static final String JAVAX_EJB_STATELESS = "Ljavax/ejb/Stateless;";
	public static final String JAVAX_INJECT_INJECT = "Ljavax/inject/Inject;";

	public static final String BEAN_ANNOTATIONS[] = { 
			"Ljavax/inject/Singleton;",
			"Ljavax/enterprise/context/ApplicationScoped;", 
			"Ljavax/enterprise/context/RequestScoped;", 
			"Ljavax/enterprise/context/TransactionScoped;" 
			};


	private static final String KEYS[] = { JAVAX_JWS_WEB_SERVICE, JAVAX_EJB_STATEFUL, JAVAX_EJB_STATELESS, JAVAX_INJECT_INJECT };

	private String path;
	private HashSet<URL> jars = new HashSet<URL>();
	HashSet<URL> beanJars = new HashSet<URL>();
	private HashMap<String, EjbIH> localMap = new HashMap<String, EjbIH>();
	private HashMap<String, EjbIH> remoteMap = new HashMap<String, EjbIH>();
	private HashSet<EjbIH> used = new HashSet<EjbIH>();

	private ThreadContext defaultContext = new ThreadContext();
	private HashMap<WebAppContext, WSC> wac2wsc = new HashMap<WebAppContext, WSC>();

	private static List<String> entities = new ArrayList<>();

	public static EarClassLoader instance;
	
	/**
	 * CT for the ear root class loader.
	 * 
	 * @param path the path to the unpacked ear.
	 */
	public EarClassLoader(String path) {
		this.path = path;
		instance = this;
	}

	/**
	 * Add a jar to the class path
	 * 
	 * @param jar name of a JAR
	 * @throws MalformedURLException
	 */
	public URL addJar(String jar) throws MalformedURLException {
		URL url = path2URL(path.isEmpty() ? jar : path + "/" + jar);
		if (jars.contains(url)) {
			// beans must not live in a multiple used jar
			beanJars.remove(url);
		} else {
			addURL(url);
			beanJars.add(url);
			jars.add(url);
		}
		return url;
	}

	/**
	 * Scan all referred classes and search - javax/ejb/Stateless -
	 * javax/ejb/Stateful - javax/ejb/Local - javax/ejb/Remote
	 * 
	 * @param ecl
	 * 
	 * @param ejbStuff
	 * 
	 * @throws Exception
	 */
	public void initializeEjbs(ClassLoader ecl, MultiMap<String, String> ejbStuff) throws Exception {
		for (String stateless : ejbStuff.subMap(JAVAX_EJB_STATELESS, JAVAX_EJB_STATELESS + "\u0000").values()) {
			try {
				Class<?> statelessClass = ecl.loadClass(stateless);
				Stateless statelessAnnotation = statelessClass.getAnnotation(Stateless.class);
				if (statelessAnnotation == null)
					continue;

				// search the implemented interfaces
				EjbIH h = null;
				for (Class<?> iface : statelessClass.getInterfaces()) {
					Local local = iface.getAnnotation(Local.class);
					// if (local != null)
					// locals.put(statelessClass.getClass().getName(), wrapper);
					Remote remote = iface.getAnnotation(Remote.class);

					if (local != null || remote != null) {
						h = new EjbIH(statelessClass.newInstance());
						Class<?>[] interfaces = new Class<?>[1];
						interfaces[0] = iface;
						Object proxy = Proxy.newProxyInstance(ecl, interfaces, h);
						h.proxy = proxy;
						if (local != null)
							localMap.put(iface.getName(), h);
						if (remote != null)
							remoteMap.put(iface.getName(), h);
						break;
					}
				}
				if (h != null) {
					LOG.debug("instantiated bean: {}", stateless);
				} else {
					LOG.error("instantiated bean: {} no interface found!", stateless);
				}
			} catch (Throwable ex) {
				LOG.error("instantiating bean {}: {} {}", stateless, ex.getClass().getName(), ex.getMessage());
			}
		}
	}

	public static MultiMap<String, String> earscan(ClassLoader cl, String[] classes) throws Exception {
		MultiMap<String, String> ejbMap = new MultiMap<String, String>();
		ejbMap = new MultiMap<String, String>();
		for (String cn : classes) {
			InputStream is = cl.getResourceAsStream(cn);
			if (is == null)
				continue;
			MiniClass mc = new MiniClass(is);
			is.close();
			if (mc.isInterface())
				continue;
			cn = cn.substring(0, cn.length() - 6);
			HashSet<String> strings = mc.getStrings();
			for (String key : KEYS)
				if (strings.contains(key))
					ejbMap.put(key, cn);
			for (String key : BEAN_ANNOTATIONS)
				if (strings.contains(key))
					ejbMap.put(key, cn);
			
			if (strings.contains("Ljavax/persistence/Entity;")) {
				if (!cn.startsWith("org/hibernate") && !cn.startsWith("de/bb")) {
					cn = cn.replace('/', '.');
					entities.add(cn);
				}
			}
		}
		return ejbMap;
	}

	/**
	 * 
	 * @param logFile
	 * @param beans
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	void injectBeans(Collection<Object> beans) throws IllegalArgumentException, IllegalAccessException {
		// now inject stuff into the beans
		for (Object bean : beans) {
			injectInstance(null, bean);
		}
	}

	void injectInstance(WebAppContext wac, Object bean) throws IllegalAccessException {
		ArrayList<Field> fieldList = new ArrayList<Field>();
		for (Class<?> dis = bean.getClass(); dis != null; dis = dis.getSuperclass()) {
			Field[] fields = dis.getDeclaredFields();
			for (Field field : fields) {
				if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
					if ((field.getModifiers() & Modifier.FINAL) != Modifier.FINAL)
						LOG.warn("{} should be final", field);
					continue;
				}
				if (field.getAnnotations().length > 0) {
					field.setAccessible(true);
					fieldList.add(field);
				} else {
					LOG.warn("non static field: " + field);
				}
			}
		}

		for (Field field : fieldList) {
			EJB ejb = field.getAnnotation(EJB.class);
			if (ejb != null) {
				String name = field.getType().getName();
				EjbIH h = localMap.get(name);
				if (h == null)
					h = remoteMap.get(name);
				if (h == null) {
					LOG.error("no bean for {} in {}", field, bean.getClass().getName());
					continue;
				}
				used.add(h);
				field.set(bean, h.proxy);
				continue;
			}

			PersistenceContext pc = field.getAnnotation(PersistenceContext.class);
			if (pc != null) {
				field.set(bean, new EMWrapper(bean.getClass().getClassLoader(), pc));
				continue;
			}

			Resource res = field.getAnnotation(Resource.class);
			if (res != null) {
				if (field.getType() == SessionContext.class) {
					field.set(bean, SCWrapper.INSTANCE);
					continue;
				}

				if (field.getType() == TimerService.class) {
					// TODO use an instance per EAR
					field.set(bean, new TS());
					continue;
				}

				if (field.getType() == WebServiceContext.class) {
					if (wac == null) {
						LOG.error("not in a WAR: {} {}", field, field.getAnnotations()[0]);
						continue;
					}

					WSC wsc = wac2wsc.get(wac);
					if (wsc == null) {
						wsc = new WSC(wac);
						wac2wsc.put(wac, wsc);
					}
					field.set(bean, wsc);
					continue;
				}

				// continue;
			}
			LOG.info("TODO: {} {}", field, field.getAnnotations()[0]);
		}
		LOG.info("injected: {}", bean.getClass().getName());
	}

	Set<EjbIH> getBeans() {
		HashSet<EjbIH> beans = new HashSet<EjbIH>();
		beans.addAll(localMap.values());
		beans.addAll(remoteMap.values());
		return beans;
	}

	public void initializeWS(WebAppContext wac) throws Exception {
		ZipClassLoader zcl = wac.getZcl();
		Thread.currentThread().setContextClassLoader(zcl);
		MultiMap<String, String> warStuff = earscan(zcl, zcl.list("*.class"));
		for (String wsClassName : warStuff.subMap(JAVAX_JWS_WEB_SERVICE, JAVAX_JWS_WEB_SERVICE + "\u0000").values()) {
			if (wsClassName.startsWith("classes/"))
				continue;
			try {
				Class<?> wsClass = zcl.loadClass(wsClassName);
				if (!wsClass.isAnnotationPresent(WebService.class))
					continue;

				WebServiceHandler wss = new WebServiceHandler(wsClass);
				injectInstance(wac, wss.wsImpl);
				wac.addHandler("/" + wss.serviceName, wss);

			} catch (Throwable t) {
				LOG.error(t.getMessage());
			}
		}
	}

	public void check(LogFile logFile) {
		Set<EjbIH> ejbs = getBeans();
		ejbs.removeAll(used);
		for (EjbIH bean : ejbs) {
			logFile.writeDate("WARNING: unused bean: " + bean.ejb.getClass().getName());
		}
	}

	public ThreadContext getDefaultContext() {
		return defaultContext;
	}

	public void init(EjbClassLoader ejcl, Properties props) throws Exception {
		CDI.setCDIProvider(() -> {return new MyCDI(BM.instance);});

		addDefaultJdbc(props);
		
		MultiMap<String, String> found = earscan(this, list("*.class", jars));
		initializeEjbs(this, found);
		BM.instance.initializeBeans(this, found);
		
//		initializeWS(ejcl, found, null);
	}

	private void addDefaultJdbc(Properties props) throws Exception {
		PC pc = new PC();

		pc.props = toProperties(props);
		
		javax.persistence.EntityManager em = new EMWrapper(this, pc);
		BM.instance.put("javax.persistence.EntityManager", em);
		
		String kind = (String) props.get("datasource.db-kind");
		String cname = "";
		if ("mssql".equals(kind))
			cname = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		String url = (String) props.get("datasource.jdbc.url");
		String usr = (String) props.get("datasource.username");
		String pass = (String)props.get("datasource.password");
		
		JdbcFactory jdbcFactory = new JdbcFactory(cname, url, usr, pass);
		
		DataSource ds = new DataSource();
		ds.setJdbcFactory(jdbcFactory);
		BM.instance.put("javax.sql.DataSource", ds);
	}

	private ArrayList<PersistenceProperty> toProperties(Properties props) {
		ArrayList<PersistenceProperty> r = new ArrayList<PersistenceProperty>();
		for (Entry<Object, Object> e : props.entrySet()) {
			String name = (String) e.getKey();
			String value = (String) e.getValue();
			if (name.startsWith("hibernate") || name.startsWith("datasource"))
				r.add(new PP(name, value));
				
		}
		return r;
	}

	public void setGenerated(String string) {
		File generated = new File(new File(path).getParentFile(), "generated");
		BM.instance.generated = generated;
		try {
			addPath(generated.getAbsolutePath());
		} catch (MalformedURLException e) {
		}
	}
	
	public List<String >getEntities() {
		return entities;
	}

}
