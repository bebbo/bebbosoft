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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Project;
import de.bb.tools.bnm.plugin.PluginInfo;
import de.bb.util.DateFormat;
import de.bb.util.LRUCache;
import de.bb.util.ThreadManager;
import de.bb.util.ThreadManager.Factory;
import de.bb.util.XmlFile;

/**
 * Main class to process the commands for all pom.xml.
 * 
 * @author stefan franke
 * 
 */
public class Bnm {

    LRUCache<String, PluginInfo> pluginInfos = new LRUCache<String, PluginInfo>();

    private final class ProcessorFactory implements Factory {
        /** list of all used Poms. */
        private final ArrayList<Pom> poms;
        private final String[] args;
        private final ArrayList<Processor> threads;
        int index = 0;

        ProcessorFactory(ArrayList<Pom> poms, String[] args, ArrayList<Processor> thread) {
            this.poms = poms;
            this.args = args;
            this.threads = thread;
        }

        public void create(final ThreadManager tm) {
            if (errorException != null)
                return;
            if (index == poms.size())
                return;
            final Pom pom = poms.get(index++);
            int n = poms.size() - index;
            // limit maxCount
            if (tm.getMaxCount() > n)
                tm.setMaxCount(n);
            Processor t = new Processor(tm, threads, args, tm, pom);
            t.start();
            synchronized (threads) {
                threads.add(t);
                threads.notify();
            }
        }
    }

    private final class Processor extends ThreadManager.Thread {
        private final ArrayList<Processor> threads;
        private final String[] args;
        private final ThreadManager tm;
        final Pom pom;
        private long diff;
        Exception myException;
        private Log myLog;

        Processor(ThreadManager tm, ArrayList<Processor> threads, String[] args, ThreadManager tm2, Pom pom) {
            super(tm);
            this.threads = threads;
            this.args = args;
            this.tm = tm2;
            this.pom = pom;
        }

        public void run() {
            this.setBusy();
            long start = System.currentTimeMillis();
            myLog = Log.getLog();
            myLog.info(LINE);
            myLog.info(pom.getId());
            myLog.info(LINE);
            try {
                for (int j = 0; j < args.length; ++j) {
                    myLog.info("processing: [" + args[j] + "] " + pom.getId());
                    pom.process(args[j]);
                }
                diff = System.currentTimeMillis() - start;
            } catch (Exception e) {
                tm.setMaxCount(0);
                diff = System.currentTimeMillis() - start;
                myException = e;
                errorException = e;
                synchronized (threads) {
                    threads.notifyAll();
                }
            } finally {
                myLog.info(LINE);
                showResult(myLog);
                myLog.close();
                // terminate smooth
                this.requestDie();
                this.mustDie();
            }
        }

        void showResult(Log log) {
            String id = pom.getId();
            log.info(id + " " + (id.length() < 56 ? DOTS.substring(id.length(), 56) : "")
                    + (myException == null ? " SUCCESS" : " ERROR  ") + DF.format(diff));
        }
    }

    final static String LINE = "------------------------------------------------------------------------";
    final static String DOTS = "........................................................";

    final static DateFormat DF = new DateFormat("mm:ss.S");

    private static Log log = Log.getLog();

    private HashMap<String, Pom> id2pom = new HashMap<String, Pom>();
    Loader rootLoader;
    private ArrayList<Pom> projectList = new ArrayList<Pom>();
    private Stack<ArrayList<Pom>> sorted;
    /** lookup map for Pom instances by file. */
    private final HashMap<File, Pom> file2Pom = new HashMap<File, Pom>();

    Exception errorException;

    // options
    int threadCount = Runtime.getRuntime().availableProcessors();
    Setting setting = new Setting();
    private boolean skipUnchanged;
    private boolean verbose;
	private HashSet<String> relevantProjects;

    public Bnm(Loader loader) throws Exception {
        this(loader, true);
    }

    public Bnm(Loader loader, boolean verbose) throws Exception {
        this.rootLoader = loader;
        this.verbose = verbose;
        try {
            loadDefaultPom();
            setting.load(null);
        } finally {
            // Log.getLog().flush();
        }
    }

    public void loadRecursive(File path) throws Exception {
        try {
            // start loading this one and all children
            Pom pom = loadFirst(path);
            for (String module : pom.effectivePom.modules) {
                File mpath = new File(path, module);
                loadRecursive(mpath);
            }
        } finally {
            Log.getLog().flush();
        }
    }

    // private void reset() {
    // id2pom = new HashMap<String, Pom>();
    // projectList = new ArrayList<Pom>();
    // sorted = null;
    // }

    public Pom loadFirst(File path) throws Exception {
        try {
            // load this one only
            if (path.toString().equals("."))
                path = new File("pom.xml");
            else
                path = new File(path, "pom.xml");
            path = path.getAbsoluteFile();
            Pom pom = file2Pom.get(path);
            if (pom == null) {
                Project project = loadProject(path);
                pom = new Pom(this, path.getParentFile(), project);
                rootLoader.markLocal(project.getGA());
                id2pom.put(project.getId(), pom);
                file2Pom.put(path, pom);
            }
            projectList.add(pom);
            return pom;
        } finally {
            Log.getLog().flush();
        }
    }

    /**
     * Load the default pom into the a parent Pom.
     * 
     * @throws Exception
     */
    Project loadDefaultPom() throws Exception {
        // TODO Auto-generated method stub
        InputStream is = getClass().getClassLoader().getResourceAsStream("pom.xml");
        if (is == null)
            throw new Exception("builtin root pom.xml is missing - get a correct JAR");
        Project project = loadProject(is);
        Pom root = new RootPom(this, project);
        id2pom.put(new Id().getId(), root);
        return project;
    }

    /**
     * Load the specific pom into this bnm;
     * 
     * @param is
     *            an InputStream
     * @return
     * @throws IOException
     */
    public Project loadProject(InputStream is) throws Exception {
        XmlFile xml = new XmlFile();
        xml.read(is);
        is.close();
        Project npom = new Project();
        Bind.bind(xml, "/project/", npom);
        return npom;
    }

    /**
     * load a pom from repository, also trigger download if necessary.
     * 
     * @param id
     *            the ID of the pom
     * @param repos
     * @param file
     *            optional file path
     * @return a
     */
    public Project loadProject(Loader loader, Id id) throws Exception {
        if (loader == null)
            loader = rootLoader;
        InputStream is = loader.findInputStream(id, "pom", null);
        return loadProject(is);
    }

    public Project loadProject(File pomFile) throws Exception {
        if (verbose)
            log.info("loading " + pomFile);
        FileInputStream fis = new FileInputStream(pomFile);
        return loadProject(fis);
    }

    public Stack<ArrayList<Pom>> getSorted() {
        if (verbose)
            log.info("build order (grouped):");
        sorted = getBuildOrder();
        for (int i = sorted.size() - 1; i >= 0; --i) {
            ArrayList<Pom> bnml = sorted.get(i);
            for (Pom b : bnml) {
                if (verbose)
                    log.info(b.getId());
            }
            if (i > 0 && verbose)
                log.info("----");
        }
        if (verbose)
            log.info("====");
        return sorted;
    }

    public Stack<ArrayList<Pom>> getBuildOrder() {
        // new algorithm

        // mark all local projects
        HashMap<String, Pom> local = new HashMap<String, Pom>();
        for (Pom pom : projectList) {
            local.put(pom.getId(), pom);
        }

        // create a set with local dependencies for each project
        HashMap<String, HashSet<String>> pomDepMap = new HashMap<String, HashSet<String>>();
        for (Pom pom : projectList) {
            HashSet<String> pomDeps = new HashSet<String>();
            ArrayList<Id> deps = pom.getAllDependencies();
            for (Id id : deps) {
                String sid = id.getId();
                if (local.get(sid) != null)
                    pomDeps.add(sid);
            }
            pomDeps.remove(pom.getId());
            pomDepMap.put(pom.getId(), pomDeps);
        }

        // add all poms without dependencies - and remove it from the remaining
        // deps
        ArrayList<ArrayList<Pom>> result = new ArrayList<ArrayList<Pom>>();
        while (pomDepMap.size() > 0) {
            ArrayList<String> found = new ArrayList<String>();
            for (Entry<String, HashSet<String>> e : pomDepMap.entrySet()) {
                if (e.getValue().size() == 0)
                    found.add(e.getKey());
            }
            if (found.size() == 0)
                throw new RuntimeException("cyclic dependencies in: " + pomDepMap.keySet());

            ArrayList<Pom> pass = new ArrayList<Pom>();
            for (String sid : found) {
                // remove the found sid
                pomDepMap.remove(sid);
                // add it to the current pass
                pass.add(local.get(sid));
            }
            result.add(pass);
            // update remaining entries
            for (HashSet<String> deps : pomDepMap.values()) {
                deps.removeAll(found);
            }
        }

        // reverse the result
        Stack<ArrayList<Pom>> stack = new Stack<ArrayList<Pom>>();
        for (int i = result.size() - 1; i >= 0; --i) {
            stack.push(result.get(i));
        }
        return stack;
        // ArrayList<Pom> toSort = new ArrayList<Pom>();
        // toSort.addAll(projectList);
        // int remainingCount = toSort.size();
        // while (remainingCount > 0) {
        // // build tree map with id->bnm
        // SingleMap check = new SingleMap(Id.COMP);
        // for (Pom pom : toSort) {
        // check.put(pom.effectivePom, pom);
        // }
        // // remove dependent bnm's
        // for (Pom pom : toSort) {
        // ArrayList<Id> deps = pom.getAllDependencies();
        // for (Id id : deps) {
        // // TODO: support version ranges
        // if ("([".indexOf(id.version.charAt(0)) >= 0) {
        //
        // } else {
        // check.remove(id);
        // }
        // }
        // }
        // int currentCount = check.size();
        // if (currentCount == 0)
        // throw new RuntimeException("cyclic dependencies in: " +
        // check.keySet());
        //
        // // sort out bnm's without further dependencies
        // ArrayList<Pom> done = new ArrayList<Pom>();
        // ArrayList<Pom> temp = new ArrayList<Pom>();
        // for (Pom bnm : toSort) {
        // if (check.containsKey(bnm.effectivePom))
        // done.add(bnm);
        // else
        // temp.add(bnm);
        // }
        // // some must be sorted again
        // toSort = temp;
        // // and some are ok and pushed on stack to preserve global order.
        // stack.add(done);
        //
        // remainingCount -= currentCount;
        // }
        // return stack;
    }

    /**
     * default processing: process all modules.
     * 
     * @param args
     *            what to do
     * @throws Exception
     */
    public boolean process(final String[] args) throws Exception {
        getSorted();
        return process(args, sorted);
    }

    /**
     * process only the specified modules.
     * 
     * @param args
     * @param localSorted
     * @throws Exception
     */
    public boolean process(final String[] args, Stack<ArrayList<Pom>> localSorted) throws Exception {
        try {
            errorException = null;
            log.flush();

            // clear the attached files
            for (ArrayList<Pom> l : localSorted) {
            	for (Iterator<Pom> i = l.iterator(); i.hasNext();) {
            		Pom p = i.next();
            		
            		// remove irrelevant projects
            		if (relevantProjects != null && !relevantProjects.contains(p.getName())) {
            			i.remove();
            			continue;
            		}
            		
                    p.reset();
                }
            }

            final ArrayList<Processor> allThreads = new ArrayList<Processor>();
            for (int i = localSorted.size() - 1; i >= 0; --i) {
                final ArrayList<Pom> poms = localSorted.get(i);
                final ArrayList<Processor> threads = new ArrayList<Processor>();
                Factory f = new ProcessorFactory(poms, args, threads);

                ThreadManager tm = new ThreadManager(f);

                tm.setMaxCount(threadCount);
                tm.setWaitCount(threadCount);
                int idx = 0;
                while ((errorException == null && idx < poms.size() && tm.getRunning() > 0 && tm.getMaxCount() > 0)
                        || idx < threads.size()) {
                    if (idx == threads.size()) {
                        synchronized (threads) {
                            threads.wait(1000L);
                        }
                        continue;
                    }
                    Thread t;
                    synchronized (threads) {
                        t = threads.get(idx++);
                    }
                    t.join();
                }
                allThreads.addAll(threads);
            }
            log.info(LINE);

            // dump the times
            for (Processor p : allThreads) {
                p.showResult(log);
            }

            if (errorException != null) {
                for (Processor p : allThreads) {
                    if (p.myException == null)
                        continue;

                    log.error(LINE);
                    log.error(p.pom.getId());
                    log.error(LINE);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(bos);
                    p.myException.printStackTrace(ps);
                    log.error(bos.toString());
                }
                log.info(LINE);
                return false;
            }
        } finally {
            Log.getLog().flush();
            Log.clear();
            pluginInfos.clear();
        }
        return true;
    }

    /**
     * print the effective pom.
     * 
     * @throws Exception
     */
    public void printEpom() throws Exception {
        for (Pom pom : projectList) {
            System.out.println(pom.getEffectivePom().toString());
        }
    }

    public ArrayList<Pom> getProjectsInOrder() {
        try {
            if (sorted == null)
                getSorted();
            ArrayList<Pom> ordered = new ArrayList<Pom>();
            for (int i = sorted.size() - 1; i >= 0; --i) {
                ArrayList<Pom> bnml = sorted.get(i);
                ordered.addAll(bnml);
            }
            return ordered;
        } finally {
            Log.getLog().flush();
        }
    }

    public synchronized Pom loadPom(Loader loader, File file, Id id) throws Exception {
        if (id == null)
            return id2pom.get(new Id().getId());
        Pom cachedPom = id2pom.get(id.getId());
        if (cachedPom != null)
            return cachedPom;
        if (file != null && file.exists()) {
            file = file.getAbsoluteFile();
            Project project = loadProject(file);
            Pom pom = new Pom(this, file.getParentFile(), project);
            id2pom.put(id.getId(), pom);
            return pom;
        }
        Project project = loadProject(loader, id);
        Pom pom = new Pom(this, null, project);
        id2pom.put(id.getId(), pom);
        return pom;
    }

    public void loadSetting(File repoPath) throws Exception {
        setting.load(repoPath);
    }

    public void setSkipUnchanged(boolean su) {
        skipUnchanged = su;
    }

    public boolean hasSkipUnchanged() {
        return skipUnchanged;
    }

    public Pom getPom(String id) {
        return id2pom.get(id);
    }

    public Pom getPomByGA(String name) {
        for (Pom pom : id2pom.values()) {
            if (name.equals(pom.getName()))
                return pom;
        }
        return null;
    }

    public boolean hasError() {
        return errorException != null;
    }

    public String stats() {
        return pluginInfos.toString();
    }

	public void setBuildOnly(HashSet<String> relevantProjects) {
		this.relevantProjects = relevantProjects;
	}

}
