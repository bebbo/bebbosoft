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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import de.bb.tools.bnm.component.Component;
import de.bb.tools.bnm.component.ComponentSet;
import de.bb.tools.bnm.component.Lifecycle;
import de.bb.util.MultiMap;
import de.bb.util.XmlFile;

@SuppressWarnings("unchecked")
public class LifecycleManager {
    private static HashMap<String, HashMap<String, MultiMap<String, Object>>> builds;
    private static HashMap<String, String> phase2cycle;
    private static HashMap<String, Object> lifeCycles;

    static {
        try {
            InputStream is = LifecycleManager.class.getClassLoader().getResourceAsStream("components.xml");

            ArrayList<Component> cl = getComponents(is);
            phase2cycle = getPhase2Cycle(cl);
            lifeCycles = (HashMap<String, Object>) getLifeCycles(cl);
            builds = getBuild(cl);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * convert the read data into a map containing - build : life cycles with a phase ordered name : plugin list
     * 
     * @return
     */
    public static HashMap<String, HashMap<String, MultiMap<String, Object>>> getBuild(ArrayList<Component> components) {

        // build : life cycle : phase
        // phase = multi map {id, [plugins]}
        HashMap<String, HashMap<String, MultiMap<String, Object>>> builds = new HashMap<String, HashMap<String, MultiMap<String, Object>>>();
        // create a lifeCycle with associated plugins for each build type
        for (Component c : components) {
            if (c.role_hint != null && c.configuration != null && c.configuration.lifecycles.size() > 0) {
                // get the life cycles for the build
                String build = c.role_hint;
                HashMap<String, MultiMap<String, Object>> buildLifeCycles = builds.get(build);
                if (buildLifeCycles == null) {
                    buildLifeCycles = new HashMap<String, MultiMap<String, Object>>();
                    builds.put(build, buildLifeCycles);

                    // add defaults
                    for (Iterator<Entry<String, Object>> i = (lifeCycles).entrySet().iterator(); i.hasNext();) {
                        Entry<String, Object> e = i.next();
                        String key = e.getKey();
                        if (key.charAt(0) != 0)
                            continue;
                        key = key.substring(1);
                        MultiMap<String, Object> defaults = (MultiMap<String, Object>) e.getValue();
                        if (defaults != null) {
                            ArrayList<String> phaseNames = (ArrayList<String>) lifeCycles.get(key);
                            MultiMap<String, Object> phases = buildLifeCycles.get(key);
                            if (phases == null) {
                                phases = new MultiMap<String, Object>();
                                buildLifeCycles.put(key, phases);
                            }
                            for (String phaseName : phaseNames) {
                                String plugin = (String) defaults.get(phaseName);
                                if (plugin != null) {
                                    // remove defaults
                                    while (phases.remove(phaseName) != null) {
                                    }
                                    for (StringTokenizer st = new StringTokenizer(plugin, ","); st.hasMoreElements();) {
                                        phases.put(phaseName, st.nextToken().trim());
                                    }
                                }
                            }
                        }
                    }

                }
                for (Lifecycle l : c.configuration.lifecycles) {
                    // get the phases for the build/life cycle
                    MultiMap<String, Object> phases = buildLifeCycles.get(l.id);
                    if (phases == null) {
                        phases = new MultiMap<String, Object>();
                        buildLifeCycles.put(l.id, phases);
                    }
                    // now add all phase definitions
                    ArrayList<String> phaseNames = (ArrayList<String>) lifeCycles.get(l.id);
                    for (String phaseName : phaseNames) {
                        String plugin = l.phases.get(phaseName);
                        if (plugin != null) {
                            for (StringTokenizer st = new StringTokenizer(plugin, ","); st.hasMoreElements();) {
                                phases.put(phaseName, st.nextToken().trim());
                            }
                        }
                    }
                }
            }
        }
        return builds;
    }

    private static HashMap<String, ?> getLifeCycles(ArrayList<Component> components) {
        HashMap<String, Object> lifeCycles = new HashMap<String, Object>();
        for (Component c : components) {
            if (c.role_hint == null && c.configuration != null) {
                for (Lifecycle l : c.configuration.lifecycles) {
                    if (l.id != null && l.phases.containsKey("phase")) {
                        ArrayList<String> phases = new ArrayList<String>();
                        phases.addAll(l.phases.subMap("phase", "phase\0").values());
                        lifeCycles.put(l.id, phases);
                        if (l.default_phases != null)
                            lifeCycles.put("\0" + l.id, l.default_phases);
                    }
                }
            }
        }
        return lifeCycles;
    }

    public static HashMap<String, String> getPhase2Cycle(ArrayList<Component> components) {
        HashMap<String, String> phase2cycle = new HashMap<String, String>();
        for (Component c : components) {
            if (c.role_hint == null && c.configuration != null) {
                for (Lifecycle l : c.configuration.lifecycles) {
                    if (l.id != null && l.phases.containsKey("phase")) {
                        for (String phaseName : l.phases.subMap("phase", "phase\0").values()) {
                            phase2cycle.put(phaseName, l.id);
                        }
                    }
                }
            }
        }
        return phase2cycle;
    }

    /**
     * Get the
     * 
     * @param phaseName
     * @return
     */
    public static String getLifecycleId(String phaseName) {
        return phase2cycle.get(phaseName);
    }

    public static ArrayList<Component> getComponents(InputStream is) throws Exception {
        ComponentSet cs = new ComponentSet();
        XmlFile xml = new XmlFile();
        xml.read(is);
        is.close();
        Bind.bind(xml, "/component-set/", cs);
        for (final Component c : cs.components) {
            // create the default lifecycle
            if (c.configuration != null && c.configuration.lifecycles.isEmpty() && !c.configuration.phases.isEmpty()) {
                final Lifecycle lc = new Lifecycle();
                lc.id = "default";
                lc.phases = c.configuration.phases;
                c.configuration.phases = null;
                c.configuration.lifecycles = new ArrayList<Lifecycle>();
                c.configuration.lifecycles.add(lc);
            }
        }
        return cs.components;
    }

    /**
     * Returns a copy of the phaseName plugin mapping for the given build/lifecycleId. It's a copy to allow extension by
     * pom or plugins.
     * 
     * @param build
     *            the build (pom, jar, ...)
     * @param lifecycleId
     *            the lifecycle (clean, default, ...)
     * @return a copy of the phaseName plugin mapping for the given build/lifecycleId.
     */
    public static MultiMap<String, ?> getPhases(HashMap<String, HashMap<String, MultiMap<String, Object>>> pbuilds,
            String build, String lifecycleId) {
        HashMap<String, MultiMap<String, Object>> lc = pbuilds.get(build);
        if (lc == null)
            return null;
        MultiMap<String, Object> m = lc.get(lifecycleId);
        if (m == null)
            return null;
        MultiMap<String, Object> r = new MultiMap<String, Object>();
        r.putAll(m);
        return r;
    }

    public static HashMap<String, HashMap<String, MultiMap<String, Object>>> copyBuilds() {
        HashMap<String, HashMap<String, MultiMap<String, Object>>> cbuilds = new HashMap<String, HashMap<String, MultiMap<String, Object>>>();
        cbuilds.putAll(builds);
        return cbuilds;
    }

    public static ArrayList<String> getGlobalPhases(String lifecycleId) {
        return (ArrayList<String>) lifeCycles.get(lifecycleId);
    }

}
