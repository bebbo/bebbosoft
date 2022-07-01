/******************************************************************************
 * This file is part of de.bb.tools.bnm.eclipse.
 *
 *   de.bb.tools.bnm.eclipse is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.eclipse is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.eclipse.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009-2011
 */
package de.bb.tools.bnm.eclipse.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaModelManager;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.eclipse.Plugin;

public class BnmProject {

	private final static String INSTALL[] = { "install" };
	private final static String COMPILE[] = { "compile" };
	private final static String CLEAN[] = { "clean" };
	private final static String PACK_INSTALL[] = {"org.apache.maven.plugins:maven-jar-plugin:jar", "org.apache.maven.plugins:maven-install-plugin:install"};

	private HashMap<String, IProject> slaves = new HashMap<String, IProject>();
	private IProject project;
	private Bnm bnm;
	private HashMap<String, String> jar2Name;
	private HashMap<String, String> name2Jar;
	private HashMap<String, String> modPath2modName;
	private HashMap<String, String> modName2modPath;

	// needed during build
	private HashSet<String> slaves2build = new HashSet<String>();
	private ArrayList<Pom> projects;

	// indicate to build everything
	private boolean buildTail;

	public BnmProject(IProject p) throws Exception {
		System.err.println("add master project: " + p);
		this.project = p;
	}

	private void init() throws Exception {
		if (bnm != null)
			return;
		this.bnm = new Bnm(Plugin.newLoader(), false);
		bnm.setSkipUnchanged(true);
		File path = project.getLocation().toFile();
		bnm.loadRecursive(path);
		projects = bnm.getProjectsInOrder();
		jar2Name = new HashMap<String, String>();
		name2Jar = new HashMap<String, String>();
		modName2modPath = new HashMap<String, String>();
		modPath2modName = new HashMap<String, String>();

		// init the lookup tables
		for (Pom pom : projects) {
			String name = pom.getName();
			Loader loader = pom.getLoader();
			try {
				String pomDir = pom.getPath().getAbsolutePath();
				if (pomDir.length() > 1 && pomDir.charAt(1) == ':') {
					pomDir = pomDir.substring(0, 1).toUpperCase() + pomDir.substring(1);
				}
				pomDir = pomDir.replace('\\', '/');
				File jar = loader.findFile(pom.getEffectivePom(), null, "jar", false);
				String jarName = jar.toString().replace('\\', '/');
				if (jarName.charAt(1) == ':')
					jarName = jarName.substring(0, 1).toLowerCase() + jarName.substring(1);
				jar2Name.put(jarName, name);
				name2Jar.put(name, jarName);
				modName2modPath.put(name, pomDir);
				modPath2modName.put(pomDir, name);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		slaves2build.clear();
		// output build order once
		bnm.getSorted();
	}

	public boolean addSlave(IProject p) {
		String name = p.getName();
		if (slaves.containsKey(name))
			return false;
		System.err.println("add slave project: " + p);
		slaves.put(name, p);
		return true;
	}

	public boolean removeSlave(IProject p) {
		String name = p.getName();
		if (!slaves.containsKey(name))
			return false;
		System.err.println("remove slave project: " + p);
		slaves.remove(name);
		return true;
	}

	public void invalidate() {
		bnm = null;
	}

	public String getNameForJar(String path) {
		try {
			init();
			if (jar2Name != null)
				return jar2Name.get(path);
		} catch (Exception e) {
		}
		return null;
	}

	public String getJarForName(String name) {
		try {
			init();
			return name2Jar.get(name);
		} catch (Exception e) {
		}
		return null;
	}

	public boolean hasBnm() {
		return this.bnm != null;
	}

	public void reInit() throws Exception {
		bnm = null;
		init();
	}

	/**
	 * Build all projects from start and remove this project from the slaves2build.
	 * 
	 * @param slave
	 */
	void buildUntil(IProject slave) {
		try {
			init();

			String name = slave.getName();
			slaves2build.add(name);
			Stack<ArrayList<Pom>> order = bnm.getBuildOrder();
			Pom pom = bnm.getPomByGA(name);
			while (!order.peek().contains(pom)) {
				order.pop();
			}
			order.peek().clear();
			order.peek().add(pom);

			bnm.process(INSTALL, order, slaves2build);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.getLog().info("passing to Eclipse for: " + slave);
		Log.getLog().close();
	}

	/**
	 * Called after each project build.
	 * 
	 * @param slave
	 */
	void buildAfter(IProject slave) {
		try {
			init();
			String name = slave.getName();
			slaves2build.remove(name);

			Log.getLog().info("post build for " + name);
			Pom pom = bnm.getPomByGA(name);
			Stack<ArrayList<Pom>> order = new Stack<>();
			ArrayList<Pom> al = new ArrayList<>();
			al.add(pom);
			order.push(al);
			bnm.process(PACK_INSTALL, order, slaves2build);
			Log.getLog().close();

			// this was the last project to build?
			if (slaves2build.isEmpty()) {
				if (buildTail) {
					Log.getLog().info("building the remaining tree");
					bnm.process(INSTALL, bnm.getBuildOrder(), slaves2build);
					Log.getLog().close();

					buildTail = false;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Mark all slaves to be built.
	 */
	public void clean() {
		try {
			bnm = null;
			init();
			bnm.setSkipUnchanged(false);
			bnm.process(CLEAN);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bnm.setSkipUnchanged(true);
		slaves2build.addAll(slaves.keySet());
		buildTail = true;
	}

	/**
	 * Mark the current project to be built.
	 */
	public void cleanSlave(IProject project) {
		if (bnm == null)
			return;
		String name = project.getName();
		slaves2build.add(name);
		Pom pom = bnm.getPomByGA(name);
		if (pom != null)
			pom.markModified();
	}

	public void preBuild(IProject current) {
		// check if all artifacts are present
		IJavaProject jp = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(current);
		try {
			String name = project.getName();
			Pom pom = bnm.getPomByGA(name);
			for (IClasspathEntry ice : jp.getRawClasspath()) {
				if (ice.getPath().toString().equals(CcpContainer.ID)) {
					CcpContainer ccp = new CcpContainer(jp);
					for (IClasspathEntry artifact : ccp.getClasspathEntries()) {
						if (!artifact.getPath().toFile().exists()) {
							Stack<ArrayList<Pom>> dis = new Stack<>();
							ArrayList<Pom> al = new ArrayList<>();
							al.add(pom);
							dis.add(al);
							bnm.process(COMPILE, dis, new HashSet<>());
							break;
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean containsSlave(IProject p) {
		return slaves.values().contains(p);
	}

	public IProject getProject() {
		return project;
	}

}
