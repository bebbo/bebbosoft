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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.tools.bnm.annotiation.EPom;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.component.Component;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Exclusion;
import de.bb.tools.bnm.model.Execution;
import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Plugin;
import de.bb.tools.bnm.model.Profile;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.plugin.Mojo;
import de.bb.tools.bnm.plugin.PluginInfo;
import de.bb.tools.bnm.setting.Settings;
import de.bb.util.MultiMap;
import de.bb.util.Process;
import de.bb.util.T2;
import de.bb.util.XmlFile;

/**
 * Pom instance. Acting as an overlay.
 *
 * @author sfranke
 *
 */
public class Pom {

	private static long loadTime;

	private Bnm bnm;

	public Bnm getBnm() {
		return bnm;
	}

	/**
	 * parent Pom.
	 */
	Pom parent;

	/**
	 * own properties.
	 */
	HashMap<String, String> properties = new HashMap<String, String>();

	/**
	 * current folder to work in.
	 */
	private File currentDir;

	/**
	 * the own pom.xml loaded into the Project type
	 */
	Project pom;

	/**
	 * the effective pom - once it is calculated
	 */
	protected Project effectivePom;

	private HashMap<String, Execution> pluginMap;

	private HashMap<String, HashMap<String, MultiMap<String, Object>>> builds;

	private HashMap<String, Object> pluginManagement;

	private HashMap<String, String> pluginVersions;

	private HashMap<String, String> resolvedVars;

	private MultiMap<String, String> phaseMap;

	private HashMap<String, String> replacementMap = new HashMap<String, String>();

	private ArrayList<T2<File, String>> attachedFiles = new ArrayList<T2<File, String>>();

	private HashMap<String, Map<String, String>> confMap = new HashMap<String, Map<String, String>>();

	// set if something has changed
	private boolean modified;

	// timestamp for last build
	private long lastBuild;

	private Loader loader;

	/**
	 * pom with currentDir.
	 *
	 * @param project
	 *
	 * @param parent
	 * @param module
	 * @throws Exception
	 */
	Pom(Bnm bnm, File currentDir, Project project) throws Exception {
		this.bnm = bnm;
		this.loader = bnm.rootLoader;
		if (currentDir != null) {
			while (".".equals(currentDir.getName())) {
				File t = currentDir.getParentFile();
				if (t == null)
					break;
				currentDir = t;
			}
		}
		this.currentDir = currentDir;
		this.pom = project;
		createEffectivePom();
	}

	/**
	 * pom without currentDir
	 */
	Pom(Bnm bnm, Project pom) {
		this.bnm = bnm;
		this.pom = pom;
		this.loader = bnm.rootLoader;
	}

	/**
	 * Add a property to this instance. null gets replaced by "".
	 *
	 * @param name  the name
	 * @param value the value
	 */
	public void addProperty(String name, String value) {
		if (value == null)
			value = "";
		properties.put(name, value);
	}

	/**
	 * Add a set of properties with the specified prefix.
	 *
	 * @param prefix     a prefix String
	 * @param properties a Hastable of String,String
	 */
	void addProperties(String prefix, Map<String, String> props) {
		prefix = prefix + ".";
		for (Entry<String, String> e : props.entrySet()) {
			Object val = e.getValue();
			if (val instanceof String)
				addProperty(prefix + e.getKey(), (String) val);
		}
	}

	/**
	 * Get a property. Searches all parents recursively.
	 */
	public String getProperty(String name) {
		String value = properties.get(name);
		if (value != null)
			return value;
		if (parent != null)
			return parent.getProperty(name);
		return "";
	}

	private void expandLifeCycle(String lastPhase, ArrayList<String> commands) throws Exception {
		int at = lastPhase.indexOf('@');
		String startPhase = null;
		if (at > 0) {
			startPhase = lastPhase.substring(at + 1);
			lastPhase = lastPhase.substring(0, at);
		}
		String lifecycleId = LifecycleManager.getLifecycleId(lastPhase);
		if (lifecycleId == null) {
			throw new Exception("unknown phase: " + lastPhase);
		}
		MultiMap<String, String> m = (MultiMap<String, String>) LifecycleManager.getPhases(builds,
				effectivePom.packaging, lifecycleId);
		if (m == null)
			return;
		for (String currentPhase : LifecycleManager.getGlobalPhases(lifecycleId)) {
			if (startPhase != null && !currentPhase.equals(startPhase))
				continue;
			startPhase = null;
			commands.addAll(m.subMap(currentPhase, currentPhase + "\0").values());

			// add the pom defined executions
			int plugIndex = 0;
			for (Plugin plug : effectivePom.build.plugins) {
				int execIndex = 0;
				for (Execution exec : plug.executions) {
					if (currentPhase.equals(exec.phase)) {
						for (String goal : exec.goals) {
							commands.add(plug.getGA() + ":" + goal + "#" + plugIndex + "#" + execIndex);
						}
					}
					++execIndex;
				}
				++plugIndex;
			}

			if (currentPhase.equals(lastPhase))
				break;
		}
	}

	/**
	 * Creates the effective pom by using the parents effective pom and merging it
	 * with our own pom. Merging with own pom overrides existing settings
	 *
	 * @throws Exception
	 */
	private void createEffectivePom() throws Exception {
		// try to load the parent pom
		if (parent == null) {
			if (currentDir != null) {
				File parentFile;
				// check for parent relative path
				if (pom.parent != null && pom.parent.relativePath != null)
					parentFile = new File(currentDir, pom.parent.relativePath);
				else
					parentFile = new File(currentDir.getParentFile(), "pom.xml");
				parent = bnm.loadPom(loader, parentFile, pom.parent);
			} else {
				parent = bnm.loadPom(loader, null, pom.parent);
			}
		}

		if (pom.groupId == null && !(parent instanceof RootPom)) {
			// bnm.log.warn("[" + pom.getId() + "] missing groupId, using: " +
			// parent.pom.groupId);
			pom.groupId = parent.pom.groupId;
		}

		if (parent != null && parent.pom.version == null && pom.parent.version != null) {
			String old = parent.getId();
			parent.pom.version = pom.parent.version;
			Log.getLog().warn("patching parent version from:" + old + " to " + parent.getId());
		}
		if (parent != null && parent.pom.version != null && pom.version == null) {
			String old = parent.getId();
			pom.version = parent.pom.version;
			Log.getLog().warn("patching version from parent:" + old + " to " + parent.getId());
		}
		if (parent != null && pom.parent != null && !parent.pom.getId().equals(pom.parent.getId())
				&& !(parent instanceof RootPom))
			throw new Exception("parent artifact does not match for: " + pom.getId() + "\r\n" + "pom.parent = " + pom.parent.getId() + "\r\n"
					+ "parent = " + parent.pom.getId());

		// build a list of this pom and all parent poms
		LinkedList<Project> pomsInOrder = new LinkedList<Project>();
		for (Pom b = this; b != null; b = b.parent) {
			Project np = (Project) Bind.dup(b.pom);
			pomsInOrder.addFirst(np);
		}

		resolvedVars = resolveVars(pomsInOrder);

		// apply all variables
		for (Project m : pomsInOrder) {
			Bind.applyVariables(m, m, resolvedVars);
		}

		// now merge the poms
		effectivePom = pomsInOrder.removeFirst();
		for (Project m : pomsInOrder) {
			Bind.merge(effectivePom, m);
		}

		// fill variables with elements from pom
		if (effectivePom.properties != null)
			resolvedVars.putAll(effectivePom.properties);

		Bind.extendVariables("project.", effectivePom, resolvedVars);

		applyPluginManagement();

		applyDependencyManagement();

		Bind.applyVariables(effectivePom, effectivePom, resolvedVars);

		this.builds = parent == null || parent.builds == null ? LifecycleManager.copyBuilds() : parent.builds;

		// repositories are read -> update loader
		boolean updateRootLoader = loader == bnm.rootLoader;
		this.loader = new Loader(effectivePom.repositories, null, loader != null ? loader : bnm.rootLoader);
		if (updateRootLoader)
			bnm.rootLoader = loader;

		createPluginMapping();

		// System.out.println(effectivePom);
	}

	private void applyDependencyManagement() {
		if (effectivePom.dependencyManagement == null)
			return;

		if (effectivePom.dependencies.size() == 0)
			return;

		// create a GA - Dependeny map
		HashMap<String, Dependency> ga2dep = new HashMap<String, Dependency>();
		for (Dependency d : effectivePom.dependencyManagement.dependencies) {
			ga2dep.put(d.getGA(), d);
		}

		// apply the predefined settings to all...
		for (Dependency d : effectivePom.dependencies) {
			Dependency managed = ga2dep.get(d.getGA());
			if (managed == null)
				continue;

			// found a match - overwrite empty settings
			if (d.version == null)
				d.version = managed.version;
			if (!d.optional)
				d.optional = managed.optional;
			if (d.type == null)
				d.type = managed.type;
			if (d.scope == null)
				d.scope = managed.scope;
			if (d.exclusions.size() == 0)
				d.exclusions = managed.exclusions;
		}
	}

	private void createPluginMapping() throws Exception {
		// get the plugins from effective pom, create a map
		phaseMap = new MultiMap<String, String>();
		pluginMap = new HashMap<String, Execution>();
		if (effectivePom.build != null) {
			for (Plugin p : effectivePom.build.plugins) {

				if (p.groupId == null) {
					Log.getLog().warn("[" + pom.getId() + "] setting groupId of plugin " + p.getId()
							+ " = org.apache.maven.plugins");
					p.groupId = "org.apache.maven.plugins";
				}
				if (p.version == null) {
					p.version = pluginVersions.get(p.groupId + ":" + p.artifactId);
				} else {
					String pv = pluginVersions.get(p.groupId + ":" + p.artifactId);
					if (pv == null) {
						pluginVersions.put(p.groupId + ":" + p.artifactId, p.version);
					} else {
						if (!pv.equals(p.version))
							Log.getLog().warn("[" + pom.getId() + "] version differs: " + p.version + " <> " + pv);
					}

				}

				String ga = p.getGA();
				if (!"false".equals(p.inherited)) {
					/*
					 * // merge plugin configuration with plugin managament configuration MultiMap c
					 * = (MultiMap) pluginManagement.get(ga); if (c != null) {
					 * Bind.mergeMap(p.configuration, c); }
					 */
					Object pm = pluginManagement.get(ga);
					if (pm != null)
						Bind.merge(p, pm);
				}
				confMap.put(p.getGA(), p.configuration);

				if (p.extensions) {
					try {
						// String repl = replacementMap.get(ga);
						Id pid = p;// repl == null ? p : new Id(repl + ":" + pluginVersions.get(repl));
						InputStream is = loader.findInputStream(pid, "jar", "META-INF/plexus/components.xml");
						ArrayList<Component> cl = LifecycleManager.getComponents(is);
						HashMap<String, HashMap<String, MultiMap<String, Object>>> pbuilds = LifecycleManager
								.getBuild(cl);
						builds.putAll(pbuilds);
					} catch (Exception ex) {
						Log.getLog().warn("can't load extensions for: " + p);
					}
				}

				for (Execution pe : p.executions) {
					// merge plugin execution configuration with plugin
					// managament execution configuration
					Execution ppe = (Execution) pluginManagement.get(ga + ":" + pe.id);
					if (ppe != null) {
						Bind.merge(pe, ppe);
					}
					// merge plugin configuration with plugin execution
					// configuration and assign to plugin execution
					MultiMap<String, String> conf = new MultiMap<String, String>();
					conf.putAll(p.configuration);
					Bind.mergeMap((Map<String, Object>) (MultiMap) conf,
							(Map<String, Object>) (MultiMap) pe.configuration);
					pe.configuration = conf;

					if (pe.phase != null) {
						for (String goal : pe.goals) {
							String gg = ga + ":" + goal;
							phaseMap.put(pe.phase, gg);
							pluginMap.put(gg, pe);
							confMap.put(gg, conf);
						}
					}
				}
			}
		}
	}

	private void applyPluginManagement() {
		// get the pluginManagement, if any, and create a map to use
		// configuration or PluginExecution below
		pluginManagement = new HashMap<String, Object>();
		pluginVersions = new HashMap<String, String>();
		if (effectivePom.build != null && effectivePom.build.pluginManagement != null) {
			for (Plugin p : effectivePom.build.pluginManagement.plugins) {
				if (p.version == null) {
					p.version = pluginVersions.get(p.groupId + ":" + p.artifactId);
				}
				if (p.groupId == null) {
					Log.getLog().warn("[" + pom.getId() + "] setting groupId of plugin " + p.getId()
							+ " = org.apache.maven.plugins");
					p.groupId = "org.apache.maven.plugins";
					// continue;
				}
				pluginVersions.put(p.groupId + ":" + p.artifactId, p.version);
				String ga = p.getGA();
				if (p.replaces != null) {
					for (String r : p.replaces) {
						replacementMap.put(r, ga);
					}
				}
				/*
				 * pluginManagement.put(ga, p.configuration); for (PluginExecution pe :
				 * p.executions) { pluginManagement.put(ga + ":" + pe.id, pe); }
				 */
				pluginManagement.put(ga, p);
				confMap.put(p.getGA(), p.configuration);
			}
		}
	}

	private HashMap<String, String> resolveVars(LinkedList<Project> pomsInOrder) throws Exception {
		HashMap<String, String> resolvedVars = new HashMap<String, String>();
		// get all variables, also apply profiles
		// profiles are applied per pom overriding existing values.
		HashMap<String, String> unresolvedVars = getBuildVars();
		for (Project m : pomsInOrder) {
			for (Profile p : m.profiles) {
				if (p.activation != null && p.activation.isActive()) {
					Bind.merge(m, p);
				}
			}
			if (m.properties != null)
				unresolvedVars.putAll(m.properties);
		}

		// apply settings profiles
		unresolvedVars.putAll(bnm.setting.getVariables());

		// resolve all variables
		// HashMap<String, String>
		while (unresolvedVars.size() > 0) {
			// find variables not using other variables
			ArrayList<String> no = new ArrayList<String>();
			boolean isEndless = true;
			for (Iterator<Map.Entry<String, String>> i = unresolvedVars.entrySet().iterator(); i.hasNext();) {
				Entry<String, String> e = i.next();
				String val = e.getValue();
				if (val == null)
					val = "null";
				int bra = val.indexOf("${");
				while (bra >= 0) {
					bra += 2;
					int ket = val.indexOf('}', bra);
					if (ket < 0)
						ket = val.length();
					String vname = val.substring(bra, ket);
					if (unresolvedVars.containsKey(vname))
						break;
					bra = val.indexOf("${", ket);
				}
				if (bra >= 0) {
					no.add(e.getKey());
					continue;
				}

				isEndless = false;
				String key = e.getKey();
				resolvedVars.put(key, val);
				i.remove();
			}
			if (isEndless)
				throw new Exception("cyclic variables in:\r\n" + unresolvedVars);

			updateVars(resolvedVars, resolvedVars);
		}

		return resolvedVars;
	}

	private HashMap<String, String> getBuildVars() {
		HashMap<String, String> buildVars = new HashMap<String, String>();
		buildVars.put("artifactId", pom.artifactId);
		buildVars.put("groupId", pom.groupId);
		buildVars.put("version", pom.version);
		String absPath = currentDir == null ? "." : currentDir.getAbsolutePath();
		buildVars.put("basedir", absPath);
		buildVars.put("project.directory", absPath);
		buildVars.put("project.build.directory", absPath + "/target");
		buildVars.put("project.build.outputDirectory", absPath + "/target/classes");
		buildVars.put("project.build.testOutputDirectory", absPath + "/target/test-classes");
		buildVars.put("project.build.sourceEncoding", "utf-8");
		return buildVars;
	}

	private void updateVars(HashMap<String, String> unresolvedVars, HashMap<String, String> resolvedVars) {
		for (Entry<String, String> e : unresolvedVars.entrySet()) {
			String val = e.getValue();
			val = Bind.resolveVars(pom, val, resolvedVars);
			unresolvedVars.put(e.getKey(), val);
		}
	}

	public String getId() {
		if (effectivePom != null)
			return effectivePom.getId();
		return pom.getId();
	}

	public ArrayList<Id> getAllDependencies() {
		ArrayList<Id> al = new ArrayList<Id>();
		al.addAll(effectivePom.getAllDependencies());
		for (String pluginIdGoal : pluginMap.keySet()) {
			int colon = pluginIdGoal.lastIndexOf(':');
			String pluginId = pluginIdGoal.substring(0, colon);
			String version = pluginVersions.get(pluginId);
			al.add(new Dependency(pluginId + ":" + version));
		}
		return al;
	}

	public String toString() {
		return "BNM: " + pom.getId();
	}

	/**
	 * the module folder containing the pom.xml.
	 *
	 * @return a File object.
	 */
	public File getPath() {
		return currentDir;
	}

	private AbstractPlugin configure(Id id, ClassLoader cl, Mojo mojo, Map<String, String> conf,
			ArrayList<Dependency> dependencies) throws Exception {
		try {
			AbstractPlugin o = (AbstractPlugin) cl.loadClass(mojo.implementation).newInstance();

			o.project = this;
			o.dependencies = dependencies;
			o.currentDir = this.currentDir;
			if (conf != null)
				o.configuration.putAll(conf);

			Class<?> clazz = o.getClass();

			ArrayList<Field> fields = Bind.getFields(clazz);
			for (Field f : fields) {
				Property p = f.getAnnotation(Property.class);
				EPom e = f.getAnnotation(EPom.class);
				Config c = f.getAnnotation(Config.class);
				Object val = null;
				if (p != null) {
					String repl = p.value();
					val = Bind.resolveVars(id, repl, resolvedVars);
					// System.out.println("prop: " + repl + "->" + val);
					Bind.setField(o, f, val);
				} else if (e != null) {
					String key = e.value();
					if (key.startsWith("project")) {
						key = key.substring(7);
						if (key.startsWith("."))
							key = key.substring(1);
						val = Bind.getValue(getEffectivePom(), key);
						Bind.setField(o, f, val);
					}
				} else if (c != null) {
					String key = c.value();
					if (conf != null)
						val = conf.get(key);
					// System.out.println("conf: " + key + "->" + val);
					Bind.setField(o, f, val);
				}

				// use the default value if any
				if (val == null) {
					Default d = f.getAnnotation(Default.class);
					if (d != null) {
						String unresolved = d.value();
						val = Bind.resolveVars(id, unresolved, resolvedVars);
						Bind.setField(o, f, val);
					}
				}
			}
			return o;
		} catch (Exception ex) {
			throw ex;
		}
	}

	public void process(String phaseOrPlugin) throws Exception {

		File depFile = loader.findFile(effectivePom, null, effectivePom.packaging, false);
		if (depFile == null || !depFile.exists()) {
			modified = true;
		}
		if (depFile.lastModified() > lastBuild) {
			lastBuild = 0;
		}

		if (!modified) {
			for (Id dep : getAllDependencies()) {
				Pom depPom = bnm.getPom(dep.getId());
				if (depPom != null) {
					if (depPom.modified) {
						modified = true;
						break;
					}
					continue;
				}
				depFile = loader.findFile(dep, ((Dependency) dep).classifier, "jar", false);
				if (depFile == null || !depFile.exists()) {
					modified = true;
					break;
				}
				if (depFile.lastModified() > lastBuild) {
					lastBuild = 0;
					break;
				}
			}
		}
		if (modified)
			lastBuild = 0;

		if (!modified && bnm.hasSkipUnchanged() && lastBuild > 0) {
			Log.getLog().info(getId() + " is unchanged --> skipping");
			return;
		}

		ArrayList<String> plugins = new ArrayList<String>();
		if (phaseOrPlugin.indexOf(':') > 0) {
			// it's a plugin
			plugins.add(phaseOrPlugin);
		} else {
			expandLifeCycle(phaseOrPlugin, plugins);
		}

		// execute the plugins from lifecycle
		for (String pluginGoal : plugins) {
			int colon = pluginGoal.lastIndexOf(':');
			String ga = pluginGoal.substring(0, colon);
			String goal = pluginGoal.substring(colon + 1);

			execute(ga, goal);
		}

		if (bnm.hasSkipUnchanged()) {
			lastBuild = System.currentTimeMillis();
		} else {
			modified = true;
			lastBuild = 0;
		}
	}

	/**
	 * execute a goal for a plugin
	 *
	 * @param ga           the GA of the plugin
	 * @param goal         the goal
	 * @param dconf        the defined configuration, if any
	 * @param dependencies
	 * @throws Exception
	 */
	private void execute(String ga, String goal) throws Exception {
		Log log = Log.getLog();

		Map<String, String> dconf = null;
		ArrayList<Dependency> dependencies = null;

		int no = goal.indexOf('#');
		if (no > 0) {
			int no2 = goal.indexOf('#', no + 1);
			int pluginIndex = Integer.parseInt(goal.substring(no + 1, no2));
			int execIndex = Integer.parseInt(goal.substring(no2 + 1));
			goal = goal.substring(0, no);

			Plugin plug = effectivePom.build.plugins.get(pluginIndex);
			Execution exec = plug.executions.get(execIndex);
			dconf = exec.configuration;
			if (dconf == null || dconf.isEmpty())
				dconf = plug.configuration;
			dependencies = plug.dependencies;
		}

		String orgGa = ga;
		String replaceGa = replacementMap.get(ga);
		if (replaceGa != null) {
			log.warn("mapping [" + ga + "] to [" + replaceGa + "]");
			ga = replaceGa;
		}

		if (ga.indexOf(':') < 0)
			ga = "de.bb.tools.bnm.plugin:" + ga + "-plugin";

		log.info("[" + ga + ":" + goal + "]");

		int colon = ga.indexOf(':');
		String plugin = ga.substring(colon + 1);

		String version = pluginVersions.get(ga);
		if (version == null) {
			log.error("unkonwn plugin: " + ga + ", not in " + pluginVersions + replacementMap);
			return;
		}
		Id id = new Id(ga + ":" + version);

		PluginInfo pi = obtainPluginInfo(id);

		Mojo mojo = pi.getMojo(goal);
		if (mojo == null)
			throw new Exception("no mojo (yet) for: " + id + " " + goal + "\navailable goals: " + pi.getMojoNames());

		String pluginGoal = ga + ":" + goal;
		Map<String, String> conf = dconf;
		if (conf == null)
			conf = confMap.get(pluginGoal);
		if (conf == null) {
			conf = confMap.get(orgGa);
		}

		if (bnm.hasSkipUnchanged() && mojo.buildIfModified && !isModified()) {
			log.info("skipping: " + ga + " no changes found");
			return;
		}

		AbstractPlugin instance;
		try {
			instance = configure(id, pi.getClassLoader(), mojo, conf, dependencies);
		} catch (ClassCastException cce) {
			// try to run mvn instead
			colon = pluginGoal.indexOf(':');
			String what = pluginGoal.substring(colon + 1);
			if (what.startsWith("maven-")) {
				what = what.substring(6);
				colon = what.indexOf(':');
				if (colon > 0) {
					int dash = what.lastIndexOf('-', colon);
					if (dash > 0) {
						what = what.substring(0, dash) + what.substring(colon);
					}
				}
			}
			String cmd;
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				cmd = "mvn.bat -N " + what;
			} else {
				cmd = "mvn -N " + what;
			}

			log.info("MVN: " + cmd);
			String msg = "";
			try {
				int result = Process.execute(cmd, null, System.out, null, 0x1000000000000000L, this.currentDir);
				if (result == 0)
					return;
			} catch (IOException ioe) {
				msg = "\r\n" + ioe.getMessage();
			}
			Log.getLog().error("no BNM plugin replacement for " + ga + " and invoking mvn failed for " + ga + msg);
			return;
		} catch (Throwable e) {
			log.error("can't configure " + ga + ": " + e.getMessage());
			e.printStackTrace();
			throw new Exception("can't configure " + ga + ": " + e.getMessage());
		}
		instance.execute();
	}

	private PluginInfo obtainPluginInfo(Id id) throws Exception, IOException {
		PluginInfo pi = bnm.pluginInfos.get(id.getId());
		if (pi == null) {
			Log log = Log.getLog();
			synchronized (bnm.pluginInfos) {
				pi = bnm.pluginInfos.get(id.getId());
				if (pi == null) {
					InputStream is = loader.findInputStream(id, "jar", "META-INF/maven/plugin.xml");
					XmlFile xml = new XmlFile();
					xml.read(is);
					is.close();
					pi = new PluginInfo();
					Bind.bind(xml, "/plugin/", pi);
					pi.init();

					// get class path
					Pom pluginPom = bnm.loadPom(loader, null, id);
					ArrayList<Id> classpath = pluginPom.getRuntimeDependencies();
					URL urlClasspath[] = id2url(classpath);
					pi.newClassLoader(urlClasspath);
					bnm.pluginInfos.put(id.getId(), pi);
					log.debug("new plugin: " + pluginPom.getId() + " using ");
					for (URL u : urlClasspath) {
						log.debug(u.toString());
					}
				}
			}
		}
		return pi;
	}

	private URL[] id2url(ArrayList<Id> classpath) throws Exception {
		ArrayList<URL> urls = new ArrayList<URL>();
		for (Id id : classpath) {
			if (id.getGA().equals("de.bb.tools.bnm:core"))
				continue;
			URL jarUrl = loader.findURL(id, "jar");
			urls.add(jarUrl);
		}
		return urls.toArray(new URL[] {});
	}

	public ArrayList<Object> getDependencyTree(String sscope) throws Exception {
		Scope targetScope = Scope.valueOf(sscope.toUpperCase());

		HashSet<String> done = new HashSet<String>();
		ArrayList<Object> result = getDependencyTree(done, targetScope, false, Collections.emptyList());
		result.add(0, new Dependency(effectivePom.getId()));
		return result;
	}

	private ArrayList<Object> getDependencyTree(HashSet<String> done, Scope targetScope, boolean compileOnly,
			List<Exclusion> exclusions) throws Exception {
		final ArrayList<Dependency> deps = new ArrayList<>();
		dependencyTreeLoop(effectivePom.dependencies, done, targetScope, compileOnly, exclusions, (dep, ga) -> {
			if (!done.contains(ga)) {
				done.add(ga);
				deps.add(dep);
			}
		});
		ArrayList<Object> result = new ArrayList<Object>(deps);
		dependencyTreeLoop(deps, done, targetScope, compileOnly, exclusions, (dep, ga) -> {
			try {
				Pom transPom = bnm.loadPom(loader, null, dep);
				List<Exclusion> combined = new ArrayList<>(exclusions);
				combined.addAll(dep.exclusions);
				ArrayList<Object> list = transPom.getDependencyTree(done, Scope.COMPILE, true, combined);
				result.add(result.indexOf(dep) + 1, list);
				
			} catch (Exception e) {
				Log.getLog().error("unresolved: " + dep);
			}
		});
		return result;
	}

	private void dependencyTreeLoop(List<Dependency> deps, HashSet<String> done, Scope targetScope, boolean compileOnly,
			List<Exclusion> exclusions, BiConsumer<Dependency, String> bif) {
		for (Dependency dep : deps) {
			String ga = dep.getGA() + ":" + dep.classifier;
			
			if (isExclude(exclusions, dep))
				continue;

			if (!isUsableArtifact(dep))
				continue;
			
			if (dep.optional)
				continue;
			
			if (dep.scope == null)
				dep.scope = "compile";
			Scope depScope = Scope.valueOf(dep.scope.toUpperCase());

			if (compileOnly && Scope.COMPILE != depScope)
				continue;

			switch (depScope) {
			case TEST:
				if (targetScope != Scope.TEST)
					continue;
				break;
			case IMPORT:
				continue;
			case SYSTEM:
			case PROVIDED:
			case RUNTIME:
			case COMPILE:
				// ok
				break;
			}

			if (dep.version == null) {
				Log.getLog().warn("no version for " + ga + " using own version " + effectivePom.version);
				dep.version = effectivePom.version;
			}

			bif.accept(dep, ga);
		}
	}

	private boolean isUsableArtifact(Dependency dep) {		
		return dep.type == null || "jar".equals(dep.type) || "pom".equals(dep.type);
	}

	private boolean isExclude(List<Exclusion> exs, Id id) {
		for (Exclusion ex : exs) {
			if (("*".equals(ex.artifactId) || id.artifactId.equals(ex.artifactId)) &&
					("*".equals(ex.groupId) || id.groupId.equals(ex.groupId))) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<Id> getCompileDependencies() throws Exception {
		ArrayList<Id> r = new ArrayList<>();
		normalize(r, getDependencyTree("compile"));
		return r;
	}

	private void normalize(ArrayList<Id> r, ArrayList<Object> dependencyTree) {
		for (Object o : dependencyTree) {
			if (o instanceof Id) {
				r.add((Id) o);
			} else {
				normalize(r, (ArrayList<Object>) o);
			}
		}
	}

	public ArrayList<Id> getRuntimeDependencies() throws Exception {
		ArrayList<Id> r = new ArrayList<>();
		normalize(r, getDependencyTree("runtime"));
		return r;
	}

	public ArrayList<Id> getTestDependencies() throws Exception {
		ArrayList<Id> r = new ArrayList<>();
		normalize(r, getDependencyTree("test"));
		return r;
	}

	/**
	 * Get the loader instance.
	 *
	 * @return
	 */
	public Loader getLoader() {
		return loader;
	}

	public Project getProject() {
		return pom;
	}

	public Project getEffectivePom() throws Exception {
		if (effectivePom == null)
			createEffectivePom();
		return effectivePom;
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, String> getVariables() {
		return (HashMap<String, String>) this.resolvedVars.clone();
	}

	public void reset() {
		attachedFiles = new ArrayList<T2<File, String>>();
	}

	public void markModified() {
		modified = true;
	}

	public boolean isModified() {
		return modified;
	}

	public ArrayList<T2<File, String>> getAttachedFiles() {
		return attachedFiles;
	}

	public void attachFile(File file) {
		attachFile(file, null);
	}

	public void attachFile(File file, String classifier) {
		T2<File, String> t2 = new T2<File, String>();
		t2.a = file;
		t2.b = classifier;
		attachedFiles.add(t2);
	}

	static {
		loadTime = System.currentTimeMillis();
	}

	public static long getLoadTime() {
		return loadTime;
	}

	public String getName() {
		if (effectivePom == null)
			return pom.name + pom.artifactId;
		return effectivePom.groupId + "." + effectivePom.artifactId;
	}

	/**
	 * provide access to the current settings.
	 */
	public Settings getSettings() {
		return bnm.setting.settings;
	}

	public Pom getParent() {
		return parent;
	}
	
}