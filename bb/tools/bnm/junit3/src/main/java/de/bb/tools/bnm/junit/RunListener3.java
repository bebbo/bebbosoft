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
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class RunListener3 implements TestListener {

  private File directory;

  public RunListener3(File directory) {
    this.directory = directory;
  }

  

  private DataOutputStream dos;
  private TestInfo currentTestInfo;
  private ArrayList<TestInfo> testInfos = new ArrayList<TestInfo>();
  int errorCount;
  private String testName;
  private int runCount;
  int failureCount;
  private long startTime;
  private long endTime;

  public void addError(Test test, Throwable exception) {
    if (!(exception instanceof junit.framework.AssertionFailedError))
      ++errorCount;
    currentTestInfo.failed(exception.getMessage(), exception);
    ++failureCount;
  }

  public void addFailure(Test test, AssertionFailedError failure) {
    Throwable exception = failure.getCause();
    if (!(exception instanceof junit.framework.AssertionFailedError))
      ++errorCount;
    currentTestInfo.failed(failure.getMessage(), exception);
    ++failureCount;
  }

  public void endTest(Test test) {
    currentTestInfo.finished();
    testInfos.add(currentTestInfo);
    endTime = System.currentTimeMillis();
  }

  public void startTest(Test test) {
    currentTestInfo = new TestInfo(test.toString());
    ++runCount;
    startTime = System.currentTimeMillis();
  }

  public void testRunStarted(String testName) throws Exception {
    File folder = new File(directory, "surefire-reports");
    if (!folder.exists())
      folder.mkdirs();
    File xmlFile = new File(folder, "TEST-" + testName + ".xml");
    dos = new DataOutputStream(new FileOutputStream(xmlFile));
    dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");
  }
  
  public void testRunFinished() throws IOException {
    dos.writeBytes("<testsuite" + " failures='" + failureCount + "' tests='" + runCount
        + "' errors='" + errorCount + "' skipped='" + 0 + "' time='"
        + (endTime - startTime) / 1000d + "' name='" + testName + "'>\r\n");
    for (TestInfo testInfo : testInfos) {
      dos.writeBytes(testInfo.getXmlString());
    }
    dos.writeBytes("</testsuite>\r\n");
    dos.close();
  }
}
