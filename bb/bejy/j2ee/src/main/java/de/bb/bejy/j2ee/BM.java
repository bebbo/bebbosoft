package de.bb.bejy.j2ee;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.transaction.TransactionScoped;
import javax.transaction.Transactional;

import de.bb.log.Logger;
import de.bb.util.MultiMap;
import de.bb.util.ZipClassLoader;

public class BM implements BeanManager {
	private final static Logger LOG = Logger.getLogger(BM.class);

	private final static String[] X = {};

	private HashMap<String, Object> beanMap = new HashMap<>();

	/** not yet loaded beans. */
	private HashMap<String, String> toLoad = new HashMap<>();

	/** could not load these beans yet... */
	private HashMap<String, String> delayed = new HashMap<>();

	/** fully initialized beans. */
	private HashSet<Object> doneBeans = new HashSet<>();

	File generated;
	private Class<?> javacClass;
	private Method cc;

	private List<String> toCc = new ArrayList<>();

	private List<FieldHook> fieldHooks = new ArrayList<>();

	public static BM instance = new BM();

	private BM() {
	}

	@Override
	public boolean areInterceptorBindingsEquivalent(Annotation arg0, Annotation arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean areQualifiersEquivalent(Annotation arg0, Annotation arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> AnnotatedType<T> createAnnotatedType(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Bean<T> createBean(BeanAttributes<T> arg0, Class<T> arg1, InjectionTargetFactory<T> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, X> Bean<T> createBean(BeanAttributes<T> arg0, Class<X> arg1, ProducerFactory<X> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> CreationalContext<T> createCreationalContext(Contextual<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InjectionPoint createInjectionPoint(AnnotatedField<?> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InjectionPoint createInjectionPoint(AnnotatedParameter<?> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fireEvent(Object arg0, Annotation... arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Bean<?>> getBeans(Type arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context getContext(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ELResolver getELResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Extension> T getExtension(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getInjectableReference(InjectionPoint arg0, CreationalContext<?> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInterceptorBindingHashCode(Annotation arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bean<?> getPassivationCapableBean(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> arg0, Bean<X> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> arg0, Bean<X> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getQualifierHashCode(Annotation arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInterceptorBinding(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNormalScope(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPassivatingScope(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isQualifier(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isScope(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStereotype(Class<? extends Annotation> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Decorator<?>> resolveDecorators(Set<Type> arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Interceptor<?>> resolveInterceptors(InterceptionType arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void validate(InjectionPoint arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public ExpressionFactory wrapExpressionFactory(ExpressionFactory arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Set<Bean<?>> getBeans(String name) {
		Set<Bean<?>> set = new HashSet<>();
		Bean<?> bean = new B(beanMap.get(name));
		set.add(bean);
		return set;
	}


	@Override
	public Object getReference(Bean<?> bean, Type type, CreationalContext<?> ctx) {
		if (bean != null)
			return ((B)bean).getObject();
		return null;
	}

	public void put(String name, Object bean) {
		beanMap.put(name, bean);
	}

	public Object get(String name) {
		return beanMap.get(name);
	}

	
	void compile(final String arg[], final List<String> files) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);
		// com.sun.tools.javac.main.Main m = new Main("javac", pw);
		Class<?> clazz = getJavacClass();
		if (clazz == null) {
			LOG.error("can't locate com.sun.tools.javac.main.Main!");
			LOG.error("ensure that your java is a JDK and contains lib/tool.jar!");
			throw new Exception("no javac found");
		}
		Constructor<?> ct = clazz.getConstructor(String.class, PrintWriter.class);
		Object m = ct.newInstance("javac", pw);

		LOG.info("compiling {} files", files.size());

		ArrayList<String> param = new ArrayList<>();
		for (String s : arg)
			param.add(s);
		for (String s : files)
			param.add(s);

		LOG.debug("javac {}", param);
		cc.invoke(m, (Object) param.toArray(X));
		pw.flush();
		String msg = bos.toString().trim();
		if (msg.length() > 0)
			LOG.info(msg);
	}

	private Class<?> getJavacClass() {
		if (javacClass != null)
			return javacClass;
		synchronized (X) {
			if (javacClass != null)
				return javacClass;

			String boot = System.getProperty("sun.boot.class.path");
			if (boot == null) {
				boot = System.getProperty("sun.boot.library.path");
			}
			String version = System.getProperty("java.runtime.version");
			int slash = version.indexOf('-');
			String vs = version;
			if (slash > 0)
				vs = version.substring(0, slash);
			for (StringTokenizer st = new StringTokenizer(boot, File.pathSeparator); st.hasMoreElements();) {
				File jar = new File(st.nextToken());
				File check = new File(jar.getParentFile(), "lib/tools.jar");
				if (!check.exists()) {
					check = new File(jar.getParentFile().getParentFile(), "lib/tools.jar");
				}
				if (!check.exists()) {
					check = new File(jar.getParentFile().getParentFile(), "jdk" + version + "/lib/tools.jar");
				}
				if (!check.exists()) {
					check = new File(jar.getParentFile().getParentFile(), "jdk" + vs + "/lib/tools.jar");
				}
				if (!check.exists()) {
					check = new File(jar.getParentFile(), "jmods/jdk.compiler.jmod");
				}
				if (check.exists()) {
					try {
						ClassLoader uClassLoader = new ZipClassLoader(check.getAbsolutePath(),
								boot.getClass().getClassLoader());
						javacClass = uClassLoader.loadClass("com.sun.tools.javac.main.Main");

						for (Method m : javacClass.getMethods()) {
							if (!"compile".equals(m.getName()))
								continue;
							Class<?>[] types = m.getParameterTypes();
							if (types.length != 1)
								continue;
							if (!types[0].equals(X.getClass()))
								continue;
							cc = m;
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return javacClass;
		}
	}

	/**
	 * create a subclass for an annotated bean.
	 * 
	 * @throws IOException
	 */
	void subclass(Class<?> clazz) throws IOException {
		String fullClassName = clazz.getName();
		LOG.debug("subclassing {}", fullClassName);
		String className = fullClassName + "_Ympl";
		toLoad.put(fullClassName, className);
		File dir = generated;
		int dot = className.lastIndexOf('.');
		if (dot > 0) {
			dir = new File(dir, className.substring(0, dot).replace('.', File.separatorChar));
			className = className.substring(dot + 1);
			dir.mkdirs();
		}
		String fileName = className + ".java";

		File file = new File(dir, fileName);
		toCc.add(file.getAbsolutePath());

		PrintWriter pw = new PrintWriter(new FileWriter(file));
		boolean withTa = clazz.getAnnotation(Transactional.class) != null;

		// create the sub class
		if (dot > 0)
			pw.println("package " + fullClassName.substring(0, dot) + ";");
		pw.println("public class " + className + " extends " + fullClassName + " {");

		for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if ((m.getModifiers() & (Modifier.PRIVATE | Modifier.STATIC)) != 0)
					continue;
				// not static or private
				if (withTa || m.getAnnotation(Transactional.class) != null) {
					pw.print("  ");
					if (Modifier.isPublic(m.getModifiers()))
						pw.print("public ");

					printType(pw, m.getReturnType());

					pw.print(" " + m.getName() + "(");

					// add parameters
					int i = 0;
					for (Parameter p : m.getParameters()) {
						if (i > 0)
							pw.print(", ");
						printType(pw, p.getType());
						pw.print(" p" + i++);
					}

					pw.print(") ");

					// TODO: add Exceptions

					pw.println(" {");
					pw.println("    de.bb.bejy.j2ee.TM tm = de.bb.bejy.j2ee.TM.getTM();");
					pw.println("    try {");
					pw.println("      tm.begin();");
					pw.println("      try {");
					pw.println("        " + fullClassName + " proxy = (" + fullClassName
							+ ")de.bb.bejy.j2ee.BM.getTaProxy(tm, \"" + fullClassName + "\");");
					pw.print("        ");
					if (!"void".equals(m.getReturnType().getName()))
						pw.print("return ");
					pw.print("proxy." + m.getName() + "(");

					// add parameters
					i = 0;
					for (Parameter p : m.getParameters()) {
						if (i > 0)
							pw.print(", ");
						pw.print("p" + i++);
					}

					pw.println(");");
					pw.print("      } catch (RuntimeException");

					// TODO: add exceptions

					pw.println(" e) {");
					pw.println("        tm.rollback();");
					pw.println("        throw e;");
					pw.println("      } finally {");
					pw.println("        tm.commit();");
					pw.println("      }");
					pw.println(
							"    } catch (javax.transaction.NotSupportedException|javax.transaction.RollbackException|javax.transaction.SystemException|javax.transaction.HeuristicMixedException|javax.transaction.HeuristicRollbackException e) {");
					pw.println("      throw new RuntimeException(e);");
					pw.println("    }");
					pw.println("  }");
				}
			}
		}

		pw.println("}");
		pw.close();
	}

	private void printType(PrintWriter pw, Class<?> returnTypeIn) {
		pw.print(returnTypeIn.getTypeName());
	}

	void initializeBeans(ClassLoader ecl, MultiMap<String, String> found) throws Exception {
		for (String key : EarClassLoader.BEAN_ANNOTATIONS) {
			for (String className : found.subMap(key, key + "\u0000").values()) {
				Class<?> clazz = ecl.loadClass(className);
				if (clazz.getAnnotation(Singleton.class) == null && clazz.getAnnotation(ApplicationScoped.class) == null
						&& clazz.getAnnotation(RequestScoped.class) == null
						&& clazz.getAnnotation(TransactionScoped.class) == null)
					continue;

				subclass(clazz);
			}
		}

		String[] args = of("-sourcepath", generated.getAbsolutePath(), "-d", generated.getAbsolutePath(), "-cp",
				EarClassLoader.instance.getClassPath());

		compile(args, toCc);

		while (!toLoad.isEmpty()) {
			if (delayed.size() == toLoad.size()) {
				LOG.error("can't load the remaining beans: {}", delayed.values());
				throw new Exception("can't load the remaining beans");
			}

			instantiateBeans(ecl);
			injectBeans();
			toLoad.clear();

			HashMap<String, String> x = toLoad;
			toLoad = delayed;
			delayed = x;
		}
	}

	private void instantiateBeans(ClassLoader ecl) throws Exception {
		for (Entry<String, String> e : toLoad.entrySet()) {
			String beanType = e.getKey();
			String className = e.getValue();
			Object bean = instantiateABean(ecl, beanType, className);
			if (bean == null) {
				LOG.debug("delaying instantiation of bean: {}", beanType);
				delayed.put(beanType, className);
			} else {
				beanMap.put(beanType, bean);
				
				Class<?> clz = bean.getClass();
				if (clz.getName().equals(e.getValue()))
					clz = clz.getSuperclass();
				Named named = clz.getAnnotation(Named.class);
				if (named != null) {
					beanMap.put(named.value(), bean);
				}

			}
		}
	}

	private Object instantiateABean(ClassLoader ecl, String beanType, String className) throws Exception {
		Class<?> clazz = ecl.loadClass(className);
		try {
			Constructor<?> ct = clazz.getConstructor();
			LOG.debug("instantiating bean: {}", className);
			return ct.newInstance();
		} catch (NoSuchMethodException nsme) {
		}

		for (Constructor<?> ct : clazz.getConstructors()) {
			ArrayList<Object> params = new ArrayList<>();
			for (Parameter p : ct.getParameters()) {
				Object v = beanMap.get(p.getType().getName());
				if (v == null)
					break;
				params.add(p);
			}
			if (params.size() == ct.getParameterCount()) {
				LOG.debug("instantiating bean: {}", beanType);
				return ct.newInstance(params.toArray());
			}
		}
		return null;
	}

	private void injectBeans() throws IllegalArgumentException, IllegalAccessException {
		LOG.debug("injecting beans...");
		for (Object bean : beanMap.values()) {
			if (doneBeans.contains(bean))
				continue;

			boolean ok = injectABean(bean);
			if (ok)
				doneBeans.add(bean);
		}
	}

	public boolean injectABean(Object bean) throws IllegalAccessException {
		boolean ok = true;
		for (Class<?> c = bean.getClass(); c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.getAnnotation(Inject.class) != null) {
					String type = f.getType().getName();
					Object injected = beanMap.get(type);
					if (injected == null) {
						ok = false;
					} else {
						f.setAccessible(true);
						f.set(bean, injected);
						LOG.debug("bean {}: injecting {} into {}", bean, injected, f);
					}
				}
				for (FieldHook fh : fieldHooks) {
					fh.handle(bean, f);
				}
			}
		}
		return ok;
	}

	public static String[] of(String... strings) {
		return strings;
	}

	public static Object getTaProxy(TM tm, String className) {
		Object bean = tm.getBean(className);
		if (bean == null) {
			try {
				bean = instance.instantiateABean(Thread.currentThread().getContextClassLoader(),
						className, className);
				if (bean == null)
					throw new RuntimeException("can't load bean: " + className);
				instance.injectABean(bean);
				tm.putBean(className, bean);
				
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return bean;
	}

	public void addFieldHook(FieldHook fh) {
		fieldHooks.add(fh);
	}

}
