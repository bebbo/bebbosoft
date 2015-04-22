/******************************************************************************
 * This file is part of de.bb.tools.bnm.plugin.surefire-plugin.
 *
 *   de.bb.tools.bnm.plugin.surefire-plugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   de.bb.tools.bnm.plugin.surefire-plugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with de.bb.tools.bnm.plugin.surefire-plugin.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   (c) by Stefan "Bebbo" Franke 2009
 */
package de.bb.tools.bnm.plugin.surefire;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.runner.JUnitCore;

public class TR4 implements TR {

    private static final String EMPTY[] = {};
    private JUnitCore core;
    private Method m1;
    private Method m2;
    Object sys;

    public TR4() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        Class<?> clazz = cl.loadClass("org.junit.runner.JUnitCore");
        core = (JUnitCore) clazz.newInstance();
        try {
            m1 = clazz.getMethod("runMain", EMPTY.getClass());
        } catch (Exception ex) {
        }
        try {
            Class<?> usys = cl.loadClass("org.junit.internal.JUnitSystem");
            if (usys != null) {
                m2 = clazz.getMethod("runMain", usys, EMPTY.getClass());
                sys = cl.loadClass("org.junit.internal.RealSystem").newInstance();
            }
        } catch (Exception ex) {
        }
    }

    public boolean runTests(Class<?> clazz, File dir, ArrayList<String> files) throws Exception {
        RunListener listener = new RunListener(dir);
        core.addListener(listener);
        String args[] = files.toArray(EMPTY);
        if (m2 != null) {
            m2.invoke(core, sys, args);
        } else if (m1 != null) {
            m1.invoke(core, (Object) args);
        } else {
            throw new Exception("no method found to run tests...");
        }
        return listener.errorCount == 0 && listener.failureCount == 0;
    }

}
