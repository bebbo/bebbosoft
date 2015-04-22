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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
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
    private final static String CLEAN[] = { "clean" };

    private HashMap<String, IProject> slaves = new HashMap<String, IProject>();
    private IProject project;
    private Bnm bnm;
    private HashMap<String, String> jar2Name;
    private HashMap<String, String> name2Jar;
    private HashMap<String, String> modPath2modName;
    private HashMap<String, String> modName2modPath;

    // needed during build
    private HashSet<String> slaves2build = new HashSet<String>();
    private Stack<ArrayList<Pom>> buildGroups = new Stack<ArrayList<Pom>>();

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
        ArrayList<Pom> projects = bnm.getProjectsInOrder();
        jar2Name = new HashMap<String, String>();
        name2Jar = new HashMap<String, String>();
        modName2modPath = new HashMap<String, String>();
        modPath2modName = new HashMap<String, String>();
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

    void markDirtySlaves(IResourceDelta delta) {
        try {
            // evaluate the delta and check for pom.xml changes.
            if (deltaContainsPom(delta)) {
                invalidate();
            }
            // re-initialize if necessary
            init();
            // determine the dirty slaves
            markDirtyByDelta(delta);
            System.out.println("modules to build: " + slaves2build);

            buildGroups = new Stack<ArrayList<Pom>>();
            if (slaves2build.size() > 0) {
                Stack<ArrayList<Pom>> sorted = bnm.getBuildOrder();
                Collections.reverse(sorted);
                boolean addRest = false;
                for (ArrayList<Pom> group : sorted) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Pom> dup = (ArrayList<Pom>) group.clone();

                    if (!addRest) {
                        for (Pom p : dup) {
                            if (slaves2build.contains(p.getEffectivePom().getGA().replace(':', '.'))) {
                                addRest = true;
                                break;
                            }
                        }
                        if (!addRest)
                            continue;
                    }

                    buildGroups.add(dup);
                }
                Collections.reverse(buildGroups);
            }

            postBuild();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * check recursively for pom.xml changes.
     * 
     * @param delta
     * @return
     */
    private boolean deltaContainsPom(IResourceDelta delta) {
        if (delta == null)
            return false;
        IResource r = delta.getResource();
        if (r.getName().endsWith("pom.xml"))
            return true;
        IResourceDelta[] children = delta.getAffectedChildren();
        for (IResourceDelta child : children) {
            if (deltaContainsPom(child))
                return true;
        }
        return false;
    }

    /**
     * check recursively for pom.xml changes.
     * 
     * @param delta
     * @return
     */
    private boolean markDirtyByDelta(IResourceDelta delta) {
        if (delta == null)
            return false;
        IResourceDelta[] children = delta.getAffectedChildren();
        boolean sourceAffected = false;
        for (IResourceDelta child : children) {
            if (markDirtyByDelta(child)) {
                continue;
            }
            sourceAffected |= !child.getResource().getName().equals("target");
        }
        IResource r = delta.getResource();
        // check whether the folder is a module folder
        String path = r.getLocation().toString();
        String name = modPath2modName.get(path);
        if (name != null) {
            if (sourceAffected) {
                slaves2build.add(name);
                Pom pom = bnm.getPomByGA(name);
                if (pom != null)
                    pom.markModified();
            }
            return true;
        }
        return false;
    }

    /**
     * We have: - an initialized bnm instance (if not fail) - a list of dirty modules - the name of the current module.
     * 
     * @param slave
     */
    void buildUntil(IProject slave) {
        if (bnm == null)
            return;

        String module = slave.getName();

        // create a local build group until our module appears
        Stack<ArrayList<Pom>> local = new Stack<ArrayList<Pom>>();
        Pom slavePom = null;
        while (buildGroups.size() > 0) {
            // check whether our module is contained
            ArrayList<Pom> topGroup = buildGroups.peek();
            for (Pom pom : topGroup) {
                if (module.equals(pom.getName())) {
                    slavePom = pom;
                    topGroup.remove(pom);
                    topGroup = null;
                    break;
                }
            }
            // without current module
            if (topGroup == null)
                break;

            local.insertElementAt(topGroup, 0);

            for (Pom pom : topGroup) {
                slaves2build.remove(pom.getName());
            }
            buildGroups.pop();
        }

        try {
            if (local.size() > 0) {
                bnm.process(INSTALL, local);
                if (bnm.hasError()) {
                    bnm = null;
                }
            }

            // check if the BNM container contains errors
            final IJavaProject javaProject = JavaModelManager.getJavaModelManager().getJavaModel()
                    .getJavaProject(slave);
            boolean ok = true;
            final CcpContainer ccp = new CcpContainer(javaProject);
            for (final IClasspathEntry ccpe : ccp.getClasspathEntries()) {
                ok &= ccpe.getPath().toFile().exists();
                if (!ok)
                    break;
            }

            if (!ok && slavePom != null) {
                final ArrayList<Pom> al = new ArrayList<Pom>();
                al.add(slavePom);
                local.add(al);
            }

        } catch (Exception e) {
            bnm = null;
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
        finishBuild(slave);
        slaves2build.remove(slave.getName());
        postBuild();
    }

    /**
     * Eclipse does only resource and compile. -> finish remaining compile plugins
     * 
     * @param slave
     */
    private void finishBuild(IProject slave) {
        try {
            init();

            // rebuild current target only
            Stack<ArrayList<Pom>> local = new Stack<ArrayList<Pom>>();
            ArrayList<Pom> current = new ArrayList<Pom>();
            Pom pom = bnm.getPomByGA(slave.getName());
            if (pom == null)
                throw new Exception("slave not found: " + slave.getName());
            current.add(pom);
            local.push(current);
            bnm.process(INSTALL, local);
            if (bnm.hasError()) {
                bnm = null;
            }
        } catch (Exception e) {
            bnm = null;
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            // e.printStackTrace();
        }
    }

    /**
     * Perform the build for the remaining targets.
     * 
     * @param slave
     */
    private void postBuild() {
        if (bnm == null)
            return;

        // test whether dirty projects are left
        for (String moduleName : slaves2build) {
            // yup - so there is another BuildBeforeJavaBuilder invocation -->
            // do nothing
            if (slaves.get(moduleName) != null)
                return;
        }

        // all done - build the rest

        if (bnm != null)
            try {
                bnm.process(INSTALL, buildGroups);
                if (bnm.hasError())
                    bnm = null;
            } catch (Exception e) {
                bnm = null;
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        buildGroups.clear();
        slaves2build.clear();
    }

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
    }

    public void cleanSlave(IProject project) {
        if (bnm == null)
            return;
        String name = project.getName();
        slaves2build.add(name);
        Pom pom = bnm.getPomByGA(name);
        if (pom != null)
            pom.markModified();
    }

}
