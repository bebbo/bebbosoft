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

import junit.framework.TestResult;
import junit.framework.TestSuite;

public class TestRunner3 implements TestRunner {

	public boolean runTests(ClassLoader cl, File dir, ArrayList<String> files) throws Exception {
		RunListener3 listener = new RunListener3(dir);

		for (String file : files) {
			String classname = file.replace(".class", "").replace('/', '.').replace('\\', '.');
			TestResult tr = new TestResult();
			tr.addListener(listener);
			listener.testRunStarted(classname);
			TestSuite ts = new TestSuite(classname);
			ts.run(tr);
		}

		listener.testRunFinished();

		return listener.errorCount == 0 && listener.failureCount == 0;
	}

}
