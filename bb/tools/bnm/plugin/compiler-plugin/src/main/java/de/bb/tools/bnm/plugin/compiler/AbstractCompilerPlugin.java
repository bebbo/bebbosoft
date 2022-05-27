/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.compiler-plugin.
 *
 *   de.bb.tools.bnm.plugin.compiler-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.compiler-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.compiler-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */

package de.bb.tools.bnm.plugin.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.CpHelper;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.util.DateFormat;
import de.bb.util.FileBrowser;
import de.bb.util.ZipClassLoader;

public abstract class AbstractCompilerPlugin extends AbstractPlugin {

	private final static String[] X = {};

	/** the javac compiler. */
	private static Class<?> javacClass;
	private static Method cc;

	protected CpHelper cpHelper = new CpHelper();

	@Config("failOnError")
	@Default("true")
	private boolean failOnError;

	@Config("debug")
	@Default("true")
	private boolean debug;

	@Config("verbose")
	private boolean verbose;

	@Config("showDeprecation")
	private boolean showDeprecation;

	@Config("optimize")
	private boolean optimize;

	@Config("showWarnings")
	private boolean showWarnings;

	@Config("source")
	private String source;

	@Config("target")
	private String target;

	@Config("encoding")
	private String encoding = "UTF-8";

	@Config("lastModGranularityMs")
	private int staleMillis;

	@Config("compilerId")
	@Default("javac")
	private String compilerId;

	@Config("compilerVersion")
	private String compilerVersion;

	@Config("compileSourceRoots")
	private List<String> compileSourceRoots;

	@Config("includes")
	private List<String> includes;

	@Config("excludes")
	private List<String> excludes;

	/*
	 * @Config("fork") private boolean fork;
	 * 
	 * private String meminitial;
	 * 
	 * private String maxmem;
	 * 
	 * private String executable;
	 * 
	 * private Map compilerArguments;
	 * 
	 * private String compilerArgument; private String outputFileName;
	 * 
	 * private File basedir;
	 * 
	 * private File buildDirectory;
	 */

	long maxDate;

	protected abstract List<String> getClasspathElements() throws Exception;

	protected abstract File getOutputDirectory();

	public void execute() throws Exception {

		Log log = getLog();

		if (!"javac".equals(compilerId))
			throw new Exception("unsupported compilerId: " + compilerId);

		StringBuilder cp = new StringBuilder();
		for (String cpe : getClasspathElements()) {
			long date = new File(cpe).lastModified();
			if (date > maxDate)
				maxDate = date;
			cp.append(cpe).append(File.pathSeparator);
		}

		File out = getDependent();
		ArrayList<String> files = scanFiles(out);
		if (files.size() == 0) {
			getLog().info("nothing to compile");
			return;
		}

		project.markModified();

		ArrayList<String> args = new ArrayList<String>();
		if (debug) {
			args.add("-g");
		}
		if (encoding != null) {
			args.add("-encoding");
			args.add(encoding);
		}
		if (optimize) {
		}

		if (showDeprecation) {
			args.add("-deprecation");
		}
		if (!showWarnings) {
			args.add("-nowarn");
		}
		if (source != null) {
			args.add("-source");
			args.add(source);
		}
		if (target != null) {
			args.add("-target");
			args.add(target);
		}
		if (verbose) {
			args.add("-verbose");
		}

		args.add("-d");
		File dest = getOutputDirectory();
		if (!dest.exists())
			dest.mkdirs();
		args.add(dest.getAbsolutePath());

		String srcRoot = getSourceRoot();
		args.add("-sourcepath");
		args.add(new File(project.getPath(), srcRoot).getAbsolutePath());

		args.add("-classpath");
		args.add(cp.toString());
		log.debug("CLASSPATH=" + cp.toString());

		args.addAll(files);
		String[] arg = args.toArray(X);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);
		// com.sun.tools.javac.main.Main m = new Main("javac", pw);
		Class<?> clazz = getJavacClass();
		if (clazz == null) {
			log.error("can't locate com.sun.tools.javac.main.Main!");
			log.error("ensure that your java is a JDK and contains lib/tool.jar!");
			throw new Exception("no javac found");
		}
		Constructor<?> ct = clazz.getConstructor(String.class, PrintWriter.class);
		Object m = ct.newInstance("javac", pw);

		log.info("compiling " + files.size() + " files");
		log.debug("javac " + args);
		// int r = m.compile(arg);
		int r;
		Object rr = cc.invoke(m, (Object) arg);
		if (rr instanceof Integer) {
			r = (Integer) rr;
		} else {
			Field ec = rr.getClass().getField("exitCode");
			ec.setAccessible(true);
			r = ec.getInt(rr);
		}
		pw.flush();
		String msg = bos.toString().trim();
		if (msg.length() > 0)
			log.info(msg);

		if (failOnError && r != 0) {
			log.info("classpath=" + cp);
			log.flush();
			throw new Exception("javac returned : " + r + "\r\n" + msg + "\r\n in " + project.getId());
		}
		dest.setLastModified(System.currentTimeMillis());
	}

	private Class<?> getJavacClass() {
		if (javacClass != null)
			return javacClass;
		synchronized (X) {
			if (javacClass != null)
				return javacClass;

			String boot = System.getProperty("sun.boot.class.path");
			if (boot == null)
				boot = System.getProperty("sun.boot.library.path");
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

				try {
					if (check.exists()) {
						ClassLoader uClassLoader = new ZipClassLoader(check.getAbsolutePath(),
								getClass().getClassLoader());
						javacClass = uClassLoader.loadClass("com.sun.tools.javac.main.Main");
					} else {
						javacClass = getClass().getClassLoader().loadClass("com.sun.tools.javac.main.Main");
					}

					for (Method m : javacClass.getMethods()) {
						if (!"compile".equals(m.getName()))
							continue;
						Class<?>[] types = m.getParameterTypes();
						if (types.length == 1 && types[0].equals(X.getClass())) {
							cc = m;
							break;							
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return javacClass;
	}


	protected ArrayList<String> scanFiles(File out) throws IOException {
		final ArrayList<String> files = new ArrayList<String>();

		maxDate = 0;

		FileBrowser fb = new FileBrowser() {

			@Override
			protected void handleFile(String path, String name) {
				String f = getBaseDir() + path + "/" + name;
				files.add(f);
				long mod = new File(f).lastModified();
				if (mod > maxDate)
					maxDate = mod;
			}
		};

		if (includes != null) {
			for (String inc : includes) {
				fb.addInclude(inc);
			}
		} else {
			fb.addInclude("**/*.java");
		}
		if (excludes != null) {
			for (String exc : excludes) {
				fb.addExclude(exc);
			}
		}

		File path = project.getPath();
		String srcPath = new File(path, getSourceRoot()).getAbsolutePath();
		// if (compileSourceRoots == null) {
		fb.scan(srcPath, true);
		// } else {
		// for (String folder : compileSourceRoots) {
		// fb.scan(new File(path, folder).getAbsolutePath(), true);
		// }
		// }

		if (out.exists()) {
			long modi = out.lastModified() + 1000;
			if (modi < Pom.getLoadTime() && modi > maxDate) {
				DateFormat df = new DateFormat("yyyyMMddHHmmssSSS");
				getLog().debug("clearing all existing older files:: loaded: " + df.format(Pom.getLoadTime()) + ", modified: "
						+ df.format(modi));
				for (Iterator<String> i = files.iterator(); i.hasNext();) {
					String s = i.next();
					File src = new File(s);
					File dst = new File(getOutputDirectory(), s.substring(srcPath.length()).replace(".java", ".class"));
					if (dst.exists() && dst.lastModified() > src.lastModified())
						i.remove();
				}
			}
		}
		return files;
	}

	protected abstract String getSourceRoot();

	protected abstract File getDependent();

}
