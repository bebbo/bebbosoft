/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/JspServlet.java,v $
 * $Revision: 1.13 $
 * $Date: 2014/06/24 08:43:12 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Hagen Raab / Stefan Bebbo Franke
 * (c) 1999-2001 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * JSPServlet - compile and invoke JSPs
 *
 */

package de.bb.bejy.http.jsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.bb.io.FastByteArrayOutputStream;
import de.bb.jsp.JspCC;
import de.bb.jsp.Smap;
import de.bb.util.Misc;
import de.bb.util.Process;
import de.bb.util.ZipClassLoader;

/**
 * @author sfranke
 */
public class JspServlet extends HttpServlet {

    private final static String[] X = {};
    private static Class<?> javacClass;
    private static Method cc;

    /**
   */
    private static final long serialVersionUID = -3542162000816397305L;

    private static boolean DEBUG = false;

    // String pageBaseProp;
    String compilerCommand;

    String scratchDir;

    Map<String, Object[]> classTable;

    ServletContext servletContext;

    ServletConfig servletConfig;

    static String classPath = System.getProperty("java.class.path");

    static {
        try {
            StringBuilder ncp = new StringBuilder();
            for (StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator); st.hasMoreElements();) {
                String p = st.nextToken();
                if (p.indexOf(' ') >= 0)
                    p = '"' + p + '"';
                if (ncp.length() > 0)
                    ncp.append(File.pathSeparatorChar);
                ncp.append(p);
            }
            classPath = ncp.toString();
        } catch (Throwable t) {
        }
    }

    /**
   * 
   */
    public JspServlet() {
    }

    /** used to load JSP pages via getResourceAsStream() */
    ZipClassLoader classLoader = null;
    protected String jspFileName;

    // private boolean isScratchFromConfig;

    // private String contextPath;

    /**
     * @param zcl
     */
    public void setZipClassLoader(ZipClassLoader zcl) {
        classLoader = zcl;
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (DEBUG)
            System.out.println("t JspServlet: init cfg=" + config);

        servletConfig = config;
        servletContext = config.getServletContext();
        try {
            scratchDir = config.getInitParameter("workDir");

            if (scratchDir == null)
                scratchDir = getServletContext().getInitParameter("workDir");

            // isScratchFromConfig = scratchDir != null;
            if (scratchDir == null) {
                scratchDir = "work/" + servletContext.getServletContextName();
            }
            // scratchDir = new File(scratchDir).getCanonicalPath();

            compilerCommand = config.getInitParameter("javac");
            if (compilerCommand == null)
                compilerCommand = System.getProperty("javac");
            if (compilerCommand == null)
                compilerCommand = "javac -g -nowarn";

            // pageBaseProp = config.getInitParameter ("pagebase");
            String sDebug = config.getInitParameter("DEBUG");
            if (sDebug != null && sDebug.equals("true"))
                DEBUG = true;
            classTable = new ConcurrentHashMap<String, Object[]>();
            // contextPath = servletContext.getRealPath("");
        } catch (Exception eee) {
            throw new ServletException("JspServlet init failed");
        }
    }

    /**
     * This function starts with jspFileName - name for the JSP file, with path starting in current context To create an
     * unique class a className is calculated: contextPath (member Variable) timestamp of JSPfile jspFileName
     * 
     * It determines whether the file must be compiled when the JSP file was not yet compiled when there is a newer JSP
     * file
     * 
     * If necessary the JSP file gets compiled into JAVA and then into a class and the class is loaded.
     * 
     * The generated class's service method is called
     * 
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        // get JSP file name
        String jspFileName = request.getServletPath();
        service(request, response, jspFileName);
    }

    protected void service(HttpServletRequest request, HttpServletResponse response, String jspFileName)
            throws IOException {
        try {
            // get old values
            Object[] o3 = classTable.get(jspFileName);

            File jspFile;
            long jspTime;
            boolean compile = true;
            // check if file was modified
            if (o3 != null) {
                Long lastTime = (Long) o3[0];
                jspFile = (File) o3[2];
                jspTime = jspFile.lastModified();

                compile = lastTime.longValue() != jspTime;
                if (compile) {
                    classTable.remove(jspFileName);
                }
            } else {
                jspFile = new File(servletContext.getRealPath(jspFileName));
                jspTime = jspFile.lastModified();
            }

            // JSP file is new or was modified
            if (compile) {
                // prevent multiple compiles
                synchronized (classTable) {
                    int idx = jspFileName.lastIndexOf("/") + 1;
                    String subDir = JspCC.convert2JavaIdent(jspFileName.substring(0, idx));
                    // directory of context
                    String ctxDir = servletContext.getRealPath("");
                    File dir = new File(ctxDir);
                    String jDir = jspFile.getCanonicalPath();
                    String dDir = dir.getCanonicalPath();
                    String className = JspCC.convert2JavaIdent(jspFileName.substring(idx));

                    {
                        File classFile = new File(scratchDir, subDir + "/" + className + ".class");
                        compile = !classFile.exists() || classFile.lastModified() < jspTime;
                    }

                    if (compile) {
                        o3 = null;
                        String msg = compileJsp(jDir, className, scratchDir, dDir, subDir);
                        if (msg != null)
                            msg = msg.trim();
                        if (msg != null && msg.length() > 0) {
                            throw new Exception(">" + msg + "<");
                        }
                    }

                    if (o3 == null) {
                        Class<Servlet> clazz = (Class<Servlet>) loadClass(className, scratchDir, subDir);
                        Servlet servlet = clazz.newInstance();
                        servlet.init(servletConfig);
                        o3 = new Object[]{new Long(jspTime), servlet, jspFile};
                        classTable.put(jspFileName, o3);
                    }
                }
            }

            String compileOnly = request.getParameter("jsp_precompile");
            if (compileOnly != null && "true".equals(compileOnly))
                return;
            if (compileOnly != null && !"false".equals(compileOnly))
                throw new Exception("invalid parameter: jsp_precompile=" + compileOnly);
            Servlet servlet = (Servlet) o3[1];
            if (servlet instanceof javax.servlet.SingleThreadModel) {
                synchronized (servlet) {
                    servlet.service(request, response);
                }
            } else
                servlet.service(request, response);
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (Exception ex) {
            response.setStatus(500);
            response.setContentType("text/html");
            PrintWriter out = null;
            try {
                out = response.getWriter();
            } catch (Exception e) {
                out = new PrintWriter(response.getOutputStream());
            }
            // out.println(ex.getMessage());

            out.println("<html><head><title>JSP compile time Exception</title></head><body>");
            out.println("<h1>JSP-Compiler-Servlet Exeption occured for " + jspFileName + ":</h1><br>");
            out.println("<hr>\n");
            out.println("<pre>");
            ex.printStackTrace(out);
            out.println("</pre>");
            // out.write(e.toString());
            out.print("</body></html>");

            out.flush();
        }
    }

    /*
     * private static String toHtml(String s) { byte b[] = s.getBytes();
     * ByteArrayOutputStream bos = new ByteArrayOutputStream(); try { for (int i
     * = 0; i < b.length; ++i) { int c = b[i]; switch (c) { case '<' :
     * bos.write("&lt;".getBytes()); break; case '>' :
     * bos.write("&gt;".getBytes()); break; case '&' :
     * bos.write("&amp;".getBytes()); break; default : bos.write(c); } } } catch
     * (Exception e) { return s; } return bos.toString(); }
     */
    /**
     * create a unique directory name from scratch root and input file
     * 
     * @param extDir
     * @return a File object for tzhe full scratchDirPath
     */
    File getScratchDir(String extDir) {
        int ix;
        ix = extDir.lastIndexOf('/');
        if (ix == -1)
            ix = extDir.lastIndexOf('\\');
        if (ix > 0)
            extDir = extDir.substring(0, ix);
        while ((ix = extDir.indexOf(':')) > 0)
            extDir = extDir.substring(0, ix) + '_' + extDir.substring(ix + 1);
        if (extDir.charAt(0) != '/' && extDir.charAt(0) != '\\')
            extDir = File.separator + extDir;
        File dir = new File(scratchDir + extDir);
        if (!dir.isDirectory())
            dir.mkdirs();
        if (DEBUG)
            System.out.println("t scratchdir: " + dir.toString());
        return dir;
    }

    private String compileJsp(String jspFileName, String className, String javaFileDir, String baseDir, String subDir)
            throws Exception {
        try {
            // File compiled = new File(javaFileDir + subDir, className +
            // ".class");
            // if (!compiled.isFile())
            {
                baseDir = JspCC.replaceAll(baseDir, '\\', '/');
                jspFileName = JspCC.replaceAll(jspFileName, '\\', '/');

                // JspCC jsp = new JspCC(servletContext, classLoader);
                JspCC jsp = new JspCC(classLoader);
                // "./here/dort/test", "test00001" -> erzeugt die Datei
                // test00001.java mit class test00001
                String[] runArgs = {"-o" + className, "-d" + javaFileDir, "-b" + baseDir, jspFileName};
                if (DEBUG)
                    System.out.println("JspCC: -o" + className + " -d" + javaFileDir + " -b" + baseDir + " "
                            + jspFileName);
                jsp.run(runArgs);

                Iterator i = jsp.getErrors();
                if (i.hasNext()) {
                    StringBuilder sb = new StringBuilder();
                    for (; i.hasNext();) {
                        String[] msg = (String[]) i.next();
                        sb.append(msg[2]);
                        sb.append("\r\n<br>\r\n");
                    }
                    throw new Exception(sb.toString());
                }

                if (DEBUG)
                    System.out.println("jspCC done");

                // run the javac compiler
                if (subDir.length() > 0 && !subDir.endsWith("/"))
                    subDir += "/";
                String javaFileName = subDir + className + ".java";
                String msg = compileJava(javaFileName, javaFileDir);
                addSmap(javaFileDir + "/" + subDir + className + ".class", jsp.createSmap());
                return msg;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (e instanceof Exception)
                throw (Exception) e;
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Add the SMAP information.
     * 
     * @param cfn
     *            the class file name
     * @param smap
     *            the SMAP text.
     */
    private void addSmap(String cfn, String smap) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(cfn);
            Smap sm = new Smap(fis);

            sm.addSmap(smap);
            fos = new FileOutputStream(cfn);
            sm.write(fos);
        } catch (Exception ex) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ioe) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ioe) {
                }
            }
        }
    }

    private Class<?> loadClass(String className, String javaFileDir, String subDir) throws ClassNotFoundException {
        try {
            String pack = subDir;
            if (subDir.startsWith("/"))
                pack = subDir.substring(1);

            if (pack.length() > 0)
                className = pack + "/" + className;

            // if (classLoader == null)
            {
                String cName = Misc.replaceAll(className, '/', '.');

                ClassLoader loader = new JspClassLoader(new File(javaFileDir), classLoader);
                return loader.loadClass(cName);
            }
            /*
             * FileInputStream fis = new FileInputStream(javaFileDir + "/" +
             * className + ".class"); try { return
             * classLoader.loadClass(className, fis); } finally { fis.close(); }
             */
        } catch (Exception cnfe) {
            throw new ClassNotFoundException(className);
        }
    }

    private String compileJava(String javaFileName, String dir) throws Exception {
        ClassLoader cl = classLoader;
        if (cl == null)
            cl = getClass().getClassLoader();
        String cp = classPath;
        if (classLoader != null)
            cp += File.pathSeparatorChar + classLoader.getClassPath();

        if (DEBUG)
            System.out.println("compileJava " + javaFileName + " - " + dir + " - " + cp);

        try {
            return newCompile(javaFileName, dir, cp);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            return oldCompile(javaFileName, dir, cp, cl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return externCompileJava(javaFileName, dir, cp);
    }

    private String newCompile(String javaFileName, String dir, String cp) throws Exception, IllegalAccessException,
            InvocationTargetException {
        String fd = dir + File.separator + javaFileName;
        String[] arg = {"-g", "-nowarn", "-classpath", cp, fd};
        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        // com.sun.tools.javac.main.Main m = new Main("javac", pw);
        Class<?> clazz = getJavacClass();
        if (clazz == null) {
            throw new Exception("no javac found");
        }
        Constructor<?> ct = clazz.getConstructor(String.class, PrintWriter.class);
        Object m = ct.newInstance("javac", pw);
        Object r = cc.invoke(m, (Object) arg);
        pw.flush();
        String msg = bos.toString().trim();
        while (msg.startsWith("Note:")) {
            int lf = msg.indexOf('\n');
            msg = lf > 0 ? msg.substring(lf).trim() : "";
        }
        return msg;
    }

    private String oldCompile(String javaFileName, String dir, String cp, ClassLoader cl) throws Exception {

        Class<?> clazz = cl.loadClass("sun.tools.javac.Main");
        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
        java.lang.reflect.Constructor ct =
                clazz.getConstructor(new Class[]{java.io.OutputStream.class, java.lang.String.class});
        Object obj = ct.newInstance(new Object[]{bos, "javac"});
        java.lang.reflect.Method method = clazz.getMethod("compile", new Class[]{java.lang.String[].class});
        String fd = dir + File.separator + javaFileName;
        method.invoke(obj, new Object[]{new String[]{"-g", "-nowarn", "-classpath", cp, fd}});
        String res = bos.toString();
        if (res.startsWith("Note: sun.tools.javac.Main has been deprecated.")) {
            res = res.substring(47).trim();
            if (res.equals("1 warning"))
                res = "";
        }
        return res.length() > 0 ? res : null;
    }

    private String externCompileJava(String javaFileName, String dir, String cp) throws Exception {
        while (javaFileName.startsWith("/") || javaFileName.startsWith("\\"))
            javaFileName = javaFileName.substring(1);
        String fd = dir != null ? dir + File.separator + javaFileName : javaFileName;
        if (fd.indexOf(" ") >= 0)
            fd = "\"" + fd + "\"";

        String cmd = compilerCommand;
        if (cp != null)
            cmd += " -classpath " + cp;
        cmd += " " + fd;

        FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
        Process.execute(cmd, null, bos, null);
        /*
         * Runtime runtime = Runtime.getRuntime(); Process process =
         * runtime.exec(cmd); // process.waitFor(); int ret = -1;
         * 
         * InputStream is = process.getErrorStream(); for (int ch = is.read();
         * ch != -1; ch = is.read()) { bos.write(ch); } ret =
         * process.exitValue();
         * 
         * if (ret == -1) process.destroy();
         * 
         * 
         * is = process.getInputStream(); BufferedReader br = new
         * BufferedReader(new InputStreamReader(is)); while (br.ready()) {
         * String line = br.readLine(); if (DEBUG) System.out.println(line); }
         */
        // System.out.println("" + res);
        String res = bos.toString();
        return res.length() > 0 ? res : null;
    }

    private synchronized Class<?> getJavacClass() {
        if (javacClass != null)
            return javacClass;

        String boot = System.getProperty("sun.boot.class.path");
        String version = System.getProperty("java.runtime.version");
        int slash = version.indexOf('-');
        String vs = version;
        if (slash > 0)
            vs = version.substring(0, slash);
        for (StringTokenizer st = new StringTokenizer(boot, File.pathSeparator); st.hasMoreElements();) {
            File jar = new File(st.nextToken());
            File check = new File(jar.getParentFile(), "lib/tools.jar");
            if (!check.exists()) {
                check = new File(jar.getParentFile().getParentFile(), "lib/tools.jar");
            }
            if (!check.exists()) {
                check = new File(jar.getParentFile().getParentFile().getParentFile(), "lib/tools.jar");
            }
            if (!check.exists()) {
                check = new File(jar.getParentFile().getParentFile(), "jdk" + version + "/lib/tools.jar");
            }
            if (!check.exists()) {
                check = new File(jar.getParentFile().getParentFile(), "jdk" + vs + "/lib/tools.jar");
            }
            if (!check.exists()) {
                check =
                        new File(jar.getParentFile().getParentFile().getParentFile(), "jdk" + version
                                + "/lib/tools.jar");
            }
            if (!check.exists()) {
                check = new File(jar.getParentFile().getParentFile().getParentFile(), "jdk" + vs + "/lib/tools.jar");
            }
            if (check.exists()) {
                URL urls[] = new URL[1];
                try {
                    urls[0] = check.toURI().toURL();
                    URLClassLoader uClassLoader = new URLClassLoader(urls, boot.getClass().getClassLoader());
                    javacClass = uClassLoader.loadClass("com.sun.tools.javac.main.Main");

                    for (Method m : javacClass.getMethods()) {
                        if (!"compile".equals(m.getName()))
                            continue;
                        Class<?>[] types = m.getParameterTypes();
                        if (types.length != 1)
                            continue;
                        if (!types[0].equals(X.getClass()))
                            continue;
                        cc = m;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return javacClass;
    }

    /**
     * Method setWorkDir.
     * 
     * @param workDir
     */
    public void setWorkDir(String workDir) {
        scratchDir = workDir; // new File(workDir).getCanonicalPath();
        // isScratchFromConfig = true;
    }

    public void setJspFileName(String n) {
        jspFileName = n;
    }

    static {
        new JspFactoryImpl(); // needed to initialize that class
    }
}
/*
 * $Log: JspServlet.java,v $
 * Revision 1.13  2014/06/24 08:43:12  bebbo
 * @R fix for Java 8
 *
 * Revision 1.12  2012/11/08 12:10:15  bebbo
 * @I cache the realPath - less work during each check
 *
 * Revision 1.11  2012/07/18 06:45:00  bebbo
 * @I typified
 *
 * Revision 1.10  2011/09/16 16:18:04  bebbo
 * @B fixed jsp compile - warnings caused an error to load the page
 *
 * Revision 1.9  2011/06/12 17:47:28  bebbo
 * @N added a new method to locate the JDK to use the java compiler
 * Revision 1.8 2010/08/29 05:08:43 bebbo
 * 
 * @B: forwarding also preserves isSecure
 * 
 * @O: using unsynchronized classes where not needed to gain speed
 * 
 * Revision 1.7 2008/01/17 17:34:10 bebbo
 * 
 * @B fixed double text output on compile errors
 * 
 * Revision 1.6 2007/01/18 21:50:39 bebbo
 * 
 * @I replaceAll moved to de.bb.util.Misc
 * 
 * Revision 1.5 2006/05/09 12:13:25 bebbo
 * 
 * @R changes to comply to servlet2_4
 * 
 * Revision 1.4 2006/03/17 11:30:46 bebbo
 * 
 * @N added SUID
 * 
 * Revision 1.3 2005/11/18 14:49:24 bebbo
 * 
 * @R changed the naming for work dir
 * 
 * Revision 1.2 2004/12/13 15:38:59 bebbo
 * 
 * @I cleanup
 * 
 * @R changed naming for work directory
 * 
 * Revision 1.1 2004/04/16 13:46:09 bebbo
 * 
 * @R runtime moved to de.bb.jsp
 * 
 * Revision 1.36 2004/03/23 15:14:03 bebbo
 * 
 * @R added -nowarn
 * 
 * Revision 1.35 2004/01/09 19:44:41 bebbo
 * 
 * @B allowed reuse of already existing class files
 * 
 * Revision 1.34 2003/09/05 12:44:13 bebbo
 * 
 * @B fixed the possible work dir recursion
 * 
 * Revision 1.33 2003/07/14 12:41:52 bebbo
 * 
 * @R changed again the path and name generation of Java work files
 * 
 * Revision 1.32 2003/07/14 11:29:37 bebbo
 * 
 * @N JspServlet now adds SMAP information to generated class files
 * 
 * Revision 1.31 2003/07/10 21:28:08 bebbo
 * 
 * @R modified generation of the class file name
 * 
 * Revision 1.30 2003/07/01 12:22:51 bebbo
 * 
 * @B fix due to new name mangling
 * 
 * Revision 1.29 2003/06/23 10:17:15 bebbo
 * 
 * @B fix JspServlet due to new mangled names
 * 
 * Revision 1.28 2003/05/13 15:42:41 bebbo
 * 
 * @B JSP compliance fixes
 * 
 * Revision 1.27 2003/04/23 21:49:44 bebbo
 * 
 * @R changed white space handling and quoting of paths
 * 
 * Revision 1.26 2003/02/09 22:02:29 bebbo
 * 
 * @I smoother java file path
 * 
 * Revision 1.25 2003/01/07 15:07:23 bebbo
 * 
 * @B catching Throwable, and convert it into en Exception
 * 
 * Revision 1.24 2002/12/23 16:56:56 bebbo
 * 
 * @B fixed warning message
 * 
 * @B ServletImpl now public again :)
 * 
 * Revision 1.23 2002/12/16 16:42:17 bebbo
 * 
 * @R changed generated name (again)
 * 
 * @B existing class files are used now also after server restart
 * 
 * Revision 1.22 2002/12/02 18:41:19 bebbo
 * 
 * @B fixed warning suppression
 * 
 * Revision 1.21 2002/12/02 10:55:19 bebbo
 * 
 * @I internal changes
 * 
 * Revision 1.20 2002/11/19 12:56:48 bebbo
 * 
 * @I reorganized imports
 * 
 * Revision 1.19 2002/11/10 10:52:04 bebbo
 * 
 * @I now using de.bb.util.Process to invoke command line javac
 * 
 * Revision 1.18 2002/11/07 13:42:02 bebbo
 * 
 * @B now replacing '.' in classname
 * 
 * @B invoking internal javac with "-nowarn"
 * 
 * Revision 1.17 2002/11/06 09:44:11 bebbo
 * 
 * @I reorganized imports
 * 
 * @I removed unused variables
 * 
 * @R rewrote file naming function
 * 
 * @R rewrote JspCC invokation
 * 
 * Revision 1.16 2002/10/24 11:27:27 bebbo
 * 
 * @R changed exception handling, so they are better visible now
 * 
 * Revision 1.15 2002/07/26 18:09:34 bebbo
 * 
 * @R changed naming of generated classes. Now eclipse can handle these names.
 * 
 * Revision 1.14 2002/05/16 15:15:36 franke
 * 
 * @B fixed that JSP files were compiled at 2nd request instead first request
 * 
 * Revision 1.13 2002/04/08 13:31:05 franke
 * 
 * @I using internal compiler, if available
 * 
 * Revision 1.12 2002/04/03 15:49:07 franke
 * 
 * @I using javac configured with System.setProperty()
 * 
 * Revision 1.11 2002/03/21 14:34:45 franke
 * 
 * @N added support for JSP files as Servlet
 * 
 * @N added support for classes loaded via ClassLoader
 * 
 * @N added support for lib/*.jar in web applications
 * 
 * Revision 1.10 2002/03/10 20:04:17 bebbo
 * 
 * @R restructured command line parameters
 * 
 * @N added support for jsp_precompile switch
 * 
 * Revision 1.9 2002/03/01 12:38:06 franke
 * 
 * @B synchronized the compilation fo each JSP page
 * 
 * @B SingleThreadModel is now correctly used for JSP pages
 * 
 * Revision 1.8 2002/02/27 18:17:41 bebbo
 * 
 * @B caught an exception in exception formatting
 * 
 * Revision 1.7 2001/12/28 11:53:13 franke
 * 
 * @I using JspCC with servletContext as param
 * 
 * Revision 1.6 2001/12/04 17:47:17 franke
 * 
 * @M merge
 * 
 * Revision 1.5 2001/12/02 13:49:25 franke
 * 
 * @I now using JspCC instead of old JSP compiler
 * 
 * Revision 1.4 2001/11/19 13:25:25 franke
 * 
 * @B fixed handling of useBean, getParam and setParam
 * 
 * Revision 1.3 2001/09/15 08:51:35 bebbo
 * 
 * @R removed de.bb.util.Mime
 * 
 * Revision 1.2 2001/06/11 06:33:29 bebbo
 * 
 * @D debug stuff
 * 
 * Revision 1.1 2001/03/29 19:55:33 bebbo
 * 
 * @N moved to this location
 */
