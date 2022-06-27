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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class RunListener extends org.junit.runner.notification.RunListener {
    private File directory;

    public RunListener(File directory) {
        this.directory = directory;
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
    }

    private DataOutputStream dos;
    private TestInfo currentTestInfo;
    private ArrayList<TestInfo> testInfos = new ArrayList<TestInfo>();
    int errorCount;
    int failureCount;
    private String testName;

    @Override
    public void testRunStarted(Description description) throws Exception {
        testName = description.getDisplayName();
        // System.err.println("testRunStarted: " + testName);
        File folder = new File(directory, "surefire-reports");
        if (!folder.exists())
            folder.mkdirs();
        File xmlFile = new File(folder, "TEST-" + description.getDisplayName() + ".xml");
        dos = new DataOutputStream(new FileOutputStream(xmlFile));
        dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");
    }

    @Override
    public void testStarted(Description description) throws Exception {
        // System.err.println("testStarted: " + description.getDisplayName());
        currentTestInfo = new TestInfo(description.getDisplayName());
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        // System.err.println("testFailure: " + failure);
        Throwable exception = failure.getException();
        if (exception instanceof junit.framework.AssertionFailedError) {
            ++failureCount;
        } else {
            ++errorCount;
        }
        currentTestInfo.failed(failure.getMessage(), exception);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        // System.err.println("testIgnored: " + description);
        currentTestInfo.ignored();
    }

    @Override
    public void testFinished(Description description) throws Exception {
        // System.err.println("testFinished: " + description);
        currentTestInfo.finished();
        testInfos.add(currentTestInfo);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        // System.err.println("testRunFinished: " + result);
        // dumb xml writing... sorry but did not want dependencies to other JARs

        dos.writeBytes("<testsuite" + " failures='" + result.getFailureCount() + "' tests='" + result.getRunCount()
                + "' errors='" + errorCount + "' skipped='" + result.getIgnoreCount() + "' time='"
                + (result.getRunTime() / 1000d) + "' name='" + testName + "'>\r\n");
        for (TestInfo testInfo : testInfos) {
            dos.writeBytes(testInfo.getXmlString());
        }
        dos.writeBytes("</testsuite>\r\n");
        dos.close();
    }

}
