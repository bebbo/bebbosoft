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
package de.bb.tools.bnm.junit;

import java.io.File;
import java.util.ArrayList;

import org.junit.internal.RealSystem;
import org.junit.runner.JUnitCore;

public class TestRunner4 implements TestRunner {
	private static final String EMPTY[] = {};
	private JUnitCore junitCore;
	private RealSystem sys;

	public TestRunner4() throws Exception {
		junitCore = new JUnitCore();
		sys = new RealSystem();
	}

	public boolean runTests(ClassLoader cl, File dir, ArrayList<String> files) throws Exception {
		RunListener listener = new RunListener(dir);
		junitCore.addListener(listener);
		junitCore.runMain(sys, files.toArray(EMPTY));
		return listener.errorCount == 0 && listener.failureCount == 0;
	}
}
