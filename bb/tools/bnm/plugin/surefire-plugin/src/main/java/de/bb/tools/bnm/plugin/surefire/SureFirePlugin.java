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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.CpHelper;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.util.FileBrowser;
import de.bb.util.ZipClassLoader;

public class SureFirePlugin extends AbstractPlugin {

    private CpHelper cpHelper = new CpHelper();

    private final class LogOutputStream extends OutputStream {
        private Log log;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean hasCR;

        public LogOutputStream(Log log) {
            this.log = log;
        }

        @Override
        public void write(int b) throws IOException {
            if (b == 0x0d) {
                hasCR = true;
                return;
            }
            if (b == 0x0a) {
                flush();
                return;
            }
            if (hasCR) {
                flush();
            }
            bos.write(b);
        }

        @Override
        public void flush() throws IOException {
            hasCR = false;
            log.info(bos.toString());
            bos.reset();
        }
    }

    private final static Object LOCK = new Object();

    @Property("${project.build.testOutputDirectory}")
    private File testOutputDirectory;

    @Property("${project.build.outputDirectory}")
    private File classesOutputDirectory;

    @Property("${project.build.directory}")
    private File directory;

    @Override
    public void execute() throws Exception {
        Log log = getLog();
        final ArrayList<String> files = new ArrayList<String>();
        FileBrowser fb = new FileBrowser() {

            @Override
            protected void handleFile(String path, String file) {
                try {
                    if (path.length() > 0)
                        file = (path.substring(1) + "." + file);
                    file = file.replace('/', '.');
                    file = file.substring(0, file.length() - 6);
                    files.add(file);
                } catch (RuntimeException re) {
                    throw new RuntimeException("\"" + path + "\" : \"" + file + "\"", re);
                }
            }
        };

        fb.addInclude("**/Test*.class");
        fb.addInclude("**/*Test.class");
        fb.addExclude("**/*$*");

        fb.scan(testOutputDirectory.getAbsolutePath(), true);

        if (files.size() == 0) {
            log.info("no tests found.");
            return;
        }

        List<String> classpath = cpHelper.getClasspathElements(project,
                testOutputDirectory, classesOutputDirectory, true);
        URL urls[] = new URL[classpath.size() + 2];
        urls[0] = new URL("file:///" + testOutputDirectory + "/");
        int index = 1;
        for (String cp : classpath) {
            if (new File(cp).isDirectory())
                cp += "/";
            URL url = new URL("file:///" + cp);
            urls[index++] = url;
        }
        URL url = getClass().getClassLoader().getResource(
                "de/bb/tools/bnm/plugin/surefire/TR.class");
        String surl = url.toString();
        int exsl = surl.lastIndexOf("!/");
        if (exsl > 0) {
            surl = surl.substring(4, exsl);
        } else {
            exsl = surl.indexOf("/de/bb");
            surl = surl.substring(0, exsl);
        }
        urls[index] = new URL(surl);
        
        ZipClassLoader zcl = new ZipClassLoader();
        for (URL u : urls) {
            zcl.addURL(u);
        }
        log.info("using class path: " + zcl.getClassPath());

        
        ClassLoader cl = new URLClassLoader(urls);
        // determine which JUNIT is used

        Object tr4 = null;
        try {
            tr4 = cl.loadClass("de.bb.tools.bnm.plugin.surefire.TR4")
                    .newInstance();
        } catch (Throwable ex) {
        }
        Method m4 = null;
        if (tr4 != null)
            m4 = tr4.getClass().getMethod("runTests", Class.class, File.class,
                    files.getClass());

        Object tr3 = cl.loadClass("de.bb.tools.bnm.plugin.surefire.TR3x")
                .newInstance();
        Method m3 = tr3.getClass().getMethod("runTests", Class.class,
                File.class, files.getClass());

        Thread thread = Thread.currentThread();
        ClassLoader ocl = thread.getContextClassLoader();
        // we need to synch here to redirect the output stream
        PrintStream old = System.out;
        synchronized (LOCK) {
            System.setOut(wrap(getLog()));
            try {
                thread.setContextClassLoader(cl);
                boolean ok = true;
                if (m4 != null && tr4 != null) {
                    ok &= (Boolean) m4.invoke(tr4, null, directory, files);
                } else {
                    for (String tc : files) {
                        Class<?> clazz = cl.loadClass(tc);
                        try {
                            ok &= (Boolean) m3.invoke(tr3, clazz, directory,
                                    files);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw (Exception) e.getCause();
                        }
                    }
                }
                if (!ok) {
                    throw new Exception("at least one test failed!");
                }
            } finally {
                System.setOut(old);
                thread.setContextClassLoader(ocl);
            }
        }
    }

    private PrintStream wrap(Log log) {
        return new PrintStream(new LogOutputStream(log));
    }
}