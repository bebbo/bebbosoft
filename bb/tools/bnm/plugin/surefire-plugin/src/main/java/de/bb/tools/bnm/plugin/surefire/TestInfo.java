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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Track information about one single test.
 * @author x_franks
 *
 */
public class TestInfo {

  private String methodName;
  private String className;
  private long startTime;
  private String failureMessage;
  private Throwable failureException;
  private boolean isIgnored;
  private long totalTime;

  /**
   * CT with a <method(classname)>
   * @param displayName
   */
  public TestInfo(String displayName) {
    StringTokenizer st = new StringTokenizer(displayName, "()");
    this.methodName = st.nextToken();
    this.className = st.nextToken();
    this.startTime = System.currentTimeMillis();
  }

  public void failed(String message, Throwable exception) {
    this.failureMessage = message;
    this.failureException = exception;
  }

  public void ignored() {
    this.isIgnored = true;
  }

  public void finished() {
    this.totalTime = System.currentTimeMillis() - startTime;
  }

  public String getXmlString() {
    StringBuilder sb = new StringBuilder();
    sb.append("  <testcase classname='").append(this.className).append("' name='").append(this.methodName).append(
        "' time='").append(this.totalTime / 1000d).append("'>\r\n");
    if (this.failureException != null) {
      boolean isFailure = this.failureException instanceof junit.framework.AssertionFailedError;
      if (isFailure) {
        sb.append("    <failure ");
      } else {
        sb.append("    <error ");
      }
      sb.append("type='").append(this.failureException.getClass()).append("' message='").append(getEscapedMessage())
          .append("'>\r\n").append(getStackTrace());
      if (isFailure) {
        sb.append("    </failure>\r\n");
      } else {
        sb.append("    </error>\r\n");
      }
    }
    sb.append("  </testcase>\r\n");
    return sb.toString();
  }

  private String getStackTrace() {
    CharArrayWriter caw = new CharArrayWriter();
    this.failureException.printStackTrace(new PrintWriter(caw));
    return caw.toString();
  }

  private StringBuilder getEscapedMessage() {
    StringBuilder sb = new StringBuilder();
    String message = this.failureMessage;
    if (message == null)
      message = failureException.getMessage();
    if (message == null)
      return sb;
    char ch[] = message.toCharArray();
    for (int i = 0; i < ch.length; ++i) {
      char c = ch[i];
      if ('\'' == c) {
        sb.append("&apos;");
        continue;
      }
      if ('&' == c) {
        sb.append("&amp;");
        continue;
      }
      if ('<' == c) {
        sb.append("&lt;");
        continue;
      }
      sb.append(c);
    }
    return sb;
  }
}