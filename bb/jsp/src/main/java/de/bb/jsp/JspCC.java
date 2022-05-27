/*
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/JspCC.java,v $
 * $Revision: 1.99 $
 * $Date: 2014/09/22 09:18:17 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) by Stefan Bebbo Franke 1999-2003 all rights reserved
 * all rights reserved
 *
 * Compiler for JSP pages
 *
 */

package de.bb.jsp;

import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import de.bb.util.SingleMap;
import de.bb.util.XmlFile;
import de.bb.util.ZipClassLoader;

/**
 * convert Java Server Pages into a normal Servlet
 */
public class JspCC {
    private ArrayList errors;

    private final static Class[] NOPARAMS = {};

    private boolean noLink;

    // inited only once
    final static byte[] spaces = "                                                                ".getBytes();

    // / cmd line message
    private final static String sysMessage = "de.bb.jsp.JspCC $Revision: 1.99 $\r\n"
            + "usage: Jsp [-b<basePath>] [-o<javaName>] [-d<outDir>] [-p<baseClass>] <inFile>\r\n"
            + "  basePath: root directory for absolute includes/forwards\r\n"
            + "  javaName: name of the generated java class (without extension)\r\n"
            + "  outDir:   destination directory for java source file\r\n"
            + "  inFile:   name of the JSP file (without extension)\r\n";

    // / valid scope values for sessions
    private final static String scopes[] = {"page", "session", "request", "application"};

    // / valid values for true / false
    private final static String trueFalse[] = {"true", "false"};

    // / valid languages
    private final static String languages[] = {"java"};

    /** map URI to XmlFile. */
    private HashMap uriMap;

    private HashMap uri2tli = new HashMap();

    private HashMap xmlMap = new HashMap();

    // =============================================================================
    private HashSet importSet;

    private ArrayList importOrder;

    // / a Hashtable for TaglibInfo lookup
    private HashMap taglibs;

    // / a stack to keep an object of the actual tag
    private Stack tagStack = new Stack();

    // / a classloader, used to load stuff via getResource
    private ZipClassLoader classLoader;

    // / temporary streams
    private CodeStream header;// import stuff

    private CodeStream declaration; // static code

    private CodeStream scriptlet; // the servlet code

    // private CodeStream tempCS; // for <%= ... %>

    private ByteArrayOutputStream tempBO;

    // / map a bean name to its type
    HashMap name2Type;

    // / track included files.
    HashMap incFiles;

    // / track for recursive includes
    HashMap recursiveInc;

    // current line no
    int line;

    int codeEndLine;

    // base path for absolute includes
    private String basePath;

    // current path
    private String path;

    // current action or user tag
    private String curTag;

    // / name of base class, if any
    private String baseClass;

    // / set to true, when threadsafe
    private boolean threadSafe;

    // / set when a session is used
    private String needsSession;

    // / the used buffer size
    private int bufferSize;

    // / use auto flush
    private String autoFlush;

    // / some servlet info
    private String info;

    // / is this an error page?
    private boolean isErrorPage;

    // / name of an error page
    private String errorPage;

    // / store the content type - can be overridden
    private String contentType;

    // / store the page encoding - can be overridden
    private String pageEncoding;

    // / the jspPage
    private String jspPage;

    // / a flag whether to flush output
    private boolean flush;

    // / flags to verify JSP page
    private boolean bBaseClass;

    private boolean bSession;

    private boolean bBuffer;

    private boolean bIsThreadSafe;

    private boolean bInfo;

    private boolean bErrorPage;

    private boolean bIsErrorPage;

    private boolean bContentType;

    private boolean bPageEncoding;

    private boolean bLanguage;

    private boolean bAutoFlush;

    // / used to suppress multiple print.(" ");
    private int lastPos;

    private String packName;

    private int declStart;

    private int codeStart;

    private int headerStart;

    private int resVarCount;

    String currentFile;

    private String outDir;

    private String inFile;

    private String className;

    private HashMap requestAttributes;

    private boolean addEncode;

    /**
     * ct
     * 
     * @param cl
     */
    public JspCC(ZipClassLoader cl) {
        classLoader = cl;
    }

    /**
     * invoked per compile, to reset compile time variables.
     * 
     */
    private void init() {
        importSet = new HashSet();
        importSet.add("javax.servlet.*");
        importSet.add("javax.servlet.http.*");
        importSet.add("javax.servlet.jsp.*");
        importOrder = new ArrayList();
        importOrder.add("javax.servlet.*");
        importOrder.add("javax.servlet.http.*");
        importOrder.add("javax.servlet.jsp.*");

        addEncode = false;

        taglibs = new HashMap();
        tagStack.clear();

        header = new CodeStream(this); // import stuff
        declaration = new CodeStream(this, 2); // static code
        scriptlet = new CodeStream(this, 6); // the servlet code
        // tempCS = new CodeStream(this); // for <%= ... %>
        tempBO = new ByteArrayOutputStream();

        name2Type = new HashMap();
        incFiles = new HashMap();
        recursiveInc = new HashMap();

        line = 1;
        codeEndLine = 1;

        path = "";
        curTag = null;

        baseClass = "de.bb.jsp.JspServletImpl";
        threadSafe = false;
        needsSession = "true";
        bufferSize = 8192;
        autoFlush = "true";
        info = null;
        isErrorPage = false;
        errorPage = null;
        contentType = "text/html";
        pageEncoding = "ISO-8859-1";
        jspPage = null;
        flush = false;

        // / flags to verify JSP page
        bBaseClass = false;
        bSession = false;
        bBuffer = false;
        bIsThreadSafe = false;
        bInfo = false;
        bErrorPage = false;
        bIsErrorPage = false;
        bContentType = false;
        bPageEncoding = false;
        bLanguage = false;
        bAutoFlush = false;

        // / used to suppress multiple print.(" ");
        lastPos = 0;

        packName = null;

        declStart = 0;
        codeStart = 0;
        headerStart = 0;
        resVarCount = 0;

        currentFile = null;
        outDir = null;
        inFile = null;
        className = null;
        noLink = false;

        errors = new ArrayList();
    }

    /**
     * Get the source file name by Java line number.
     * 
     * @param jl
     * @return the file name or null.
     */
    public String getSrcFileByLine(Integer jl) {
        int n = jl.intValue() - 1;

        if (n < headerStart) {
            return null;
        }
        if (n - headerStart < header.file.size()) {
            return header.searchJspFileName(headerStart, n);
        }

        if (n < declStart) {
            return null;
        }
        if (n - declStart < declaration.file.size()) {
            return declaration.searchJspFileName(declStart, n);
        }

        if (n < codeStart) {
            return null;
        }
        if (n - codeStart < scriptlet.file.size()) {
            return scriptlet.searchJspFileName(codeStart, n);
        }
        return null;
    }

    /**
     * Maps a line number from the generated Java code back to the JSP line number.
     * 
     * @param jl
     *            the Java line number
     * @return the JSP line number
     */
    public Integer mapJavaLine(Integer jl) {
        int n = jl.intValue() - 1;

        if (n < headerStart) {
            return new Integer(1);
        }
        if (n - headerStart < header.lines.size()) {
            Integer r = header.searchJspLine(headerStart, n);
            if (r != null)
                return r;
        }

        if (n < declStart) {
            return new Integer(1);
        }
        if (n - declStart < declaration.lines.size()) {
            Integer r = declaration.searchJspLine(declStart, n);
            if (r != null)
                return r;
        }

        if (n < codeStart) {
            return new Integer(1);
        }
        if (n - codeStart < scriptlet.lines.size()) {
            Integer r = scriptlet.searchJspLine(codeStart, n);
            if (r != null)
                return r;
        }
        return new Integer(line);
    }

    /**
     * Maps a line number from the JSP to the generated Java code.
     * 
     * @param jl
     *            the JSP line number
     * @return the java line number
     */
    public Integer mapJspLine(Integer jl) {
        int ln = jl.intValue();
        Integer l = header.searchJavaLine(headerStart, ln);
        if (l != null)
            return l;
        l = declaration.searchJavaLine(declStart, ln);
        if (l != null)
            return l;
        l = scriptlet.searchJavaLine(codeStart, ln);
        if (l != null)
            return l;
        return new Integer(codeStart + codeEndLine - 1);
    }

    /**
     * open file and write outfile
     * 
     * @param args
     */
    public synchronized void run(String args[]) {
        parseParams(args);
        File oDir = new File(outDir);
        oDir.mkdirs();
        // this is the generated OUT file
        File file = new File(outDir, className + ".java");
        compileFile(inFile);
        // post checks
        if (bufferSize == 0 && autoFlush.equals("false")) {
            addError("buffer=\"none\" and autoflush=\"false\" is not allowed");
        }
        FileWriter fw = null;
        LineCountWriter bw;
        try {
            fw = new FileWriter(file);
            bw = new LineCountWriter(fw);
            writeIt(bw);
        } catch (IOException e) {
            addError("writing to " + file + " failed");
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Add an error message.
     * 
     * @param msg
     *            content of the error message.
     */
    private void addError(String msg) {
        errors.add(new String[]{"" + line, currentFile, msg});
    }

    /**
     * Read from Stream, write to Stream.
     * 
     * @param args
     *            commandline arguments
     * @param is
     *            InputStream to read from.
     * @param os
     *            OutputStream to write to.
     */
    public synchronized void streamCompile(String args[], InputStream is, OutputStream os) {
        parseParams(args);
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            PeekInputStream pis = new PeekInputStream(is);
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            parseStream(pis);
        } catch (Exception ex) {
            addError(ex.getMessage());
        } finally {
            LineCountWriter bw = new LineCountWriter(new OutputStreamWriter(os));
            try {
                writeIt(bw);
            } catch (IOException e) {
            }
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    /**
     * Method writeIt.
     * 
     * @param bw
     */
    private void writeIt(LineCountWriter bw) throws IOException {
        // create the real output file
        if (packName != null) {
            bw.write("package " + packName + ";");
            bw.newLine();
        }

        for (Iterator i = importOrder.iterator(); i.hasNext();) {
            bw.write("import " + i.next() + ";");
            bw.newLine();
        }

        // bw.write("import java.io.*;");
        // bw.newLine();
        // bw.write("import java.text.*;");
        // bw.newLine();
        // bw.write("import java.util.*;");
        // bw.newLine();
        // bw.write("import javax.servlet.*;");
        // bw.newLine();
        // bw.write("import javax.servlet.http.*;");
        // bw.newLine();
        // bw.write("import javax.servlet.jsp.*;");
        // bw.newLine(); bw.write("import de.bb.jsp.*;");
        bw.newLine();

        headerStart = bw.lines;
        bw.write(header.toString());
        bw.lines += header.lines.size();
        // System.out.println("header [" + headerStart + "," + bw.lines + "]" );

        bw.newLine();
        bw.write("/** generated code. */");
        bw.newLine();
        bw.write("public class " + className + " extends " + baseClass);
        if (threadSafe) {
            bw.newLine();
            bw.write("  implements javax.servlet.SingleThreadModel");
        }
        bw.newLine();
        bw.write("{");
        bw.newLine();

        if (info != null) {
            bw.write("/** generated code. */");
            bw.newLine();
            bw.write("  public String getServletInfo() { return \"" + info + "\";}");
            bw.newLine();
        }

        declStart = bw.lines;
        bw.write(declaration.toString());
        bw.lines += declaration.lines.size();

        bw.write("/** generated code.");
        bw.newLine();
        bw.write(" * @param request request");
        bw.newLine();
        bw.write(" * @param response response");
        bw.newLine();
        bw.write(" * @throws java.io.IOException");
        bw.newLine();
        bw.write(" * @throws ServletException");
        bw.newLine();
        bw.write(" */");
        bw.newLine();
        bw.write("  public void _jspService(HttpServletRequest request,");
        bw.newLine();
        bw.write("    HttpServletResponse response)");
        bw.newLine();
        bw.write("    throws java.io.IOException, ServletException");
        bw.newLine();
        bw.write("  {");
        bw.newLine();
        bw.write("    JspFactory _jspxfactory = JspFactory.getDefaultFactory();");
        bw.newLine();
        bw.write("    final PageContext pageContext = _jspxfactory.getPageContext(");
        bw.newLine();
        bw.write("      this, request, response, ");
        bw.newLine();
        if (errorPage != null)
            bw.write("      \"" + errorPage + "\",  // errorPageURL");
        else
            bw.write("      null,  // errorPageURL");
        bw.newLine();
        bw.write("      " + needsSession + ", // needsSession");
        bw.newLine();
        bw.write("      " + bufferSize + ", " + autoFlush + " // autoFlush");
        bw.newLine();
        bw.write("    );");
        // initialize implicit variables for scripting env ...
        bw.newLine();
        bw.write("    response.setContentType(\"" + contentType + ";charset=" + pageEncoding + "\");");
        bw.newLine();
        bw.write("    ServletConfig config = getServletConfig(); _jsp_mark_used(config);");
        bw.newLine();
        bw.write("    ServletContext application = config.getServletContext(); _jsp_mark_used(application);");
        bw.newLine();
        bw.write("    JspWriter   out     = pageContext.getOut(); _jsp_mark_used(out);");
        bw.newLine();
        bw.write("    Object      page    = this; _jsp_mark_used(page);");
        if (needsSession.equals("true")) {
            bw.newLine();
            bw.write("    HttpSession session = pageContext.getSession(); _jsp_mark_used(session);");
        }
        if (isErrorPage) {
            bw.newLine();
            bw.write("    Exception exception = (Exception)request.getAttribute(\"javax.servlet.jsp.jspException\");");
        }
        // bw.newLine();
        // bw.write("    javax.servlet.jsp.tagext.Tag _jspTag = null;");
        bw.newLine();
        bw.write("    _jspPage:");
        bw.newLine();
        bw.newLine();
        bw.write("    try {");
        bw.newLine();

        bw.write("    /** start of generated code. */");
        bw.newLine();
        // body of translated JSP here ...

        codeStart = bw.lines;
        bw.write(scriptlet.toString());
        bw.lines += scriptlet.lines.size();

        bw.newLine();
        bw.newLine();
        bw.write("    } catch (Exception ex) {");
        bw.newLine();
        bw.write("      pageContext.handlePageException(ex);");
        bw.newLine();
        bw.write("    } catch (Throwable t) {");
        bw.newLine();
        bw.write("      pageContext.handlePageException(t);");
        bw.newLine();
        /*
         * bw.write("    } catch (Throwable t) {"); bw.newLine();
         * bw.write("      Exception e; \nt.printStackTrace();"); bw.newLine();
         * bw.write("      if (t instanceof Exception) e = (Exception)t;");
         * bw.newLine();
         * bw.write("      else { e = new Exception(t.getMessage());  }");
         * bw.newLine();
         * bw.write("      try {out.clear();} catch (Throwable t$){}"); if
         * (errorPage != null) { bw.newLine(); bw.write(
         * "      request.setAttribute(\"javax.servlet.jsp.jspException\", e);"
         * ); bw.newLine(); bw.write( "      request.getRequestDispatcher(\"" +
         * errorPage + "\").forward(request, response);"); } else {
         * bw.newLine(); bw.write("      pageContext.handlePageException(e);");
         * } bw.newLine();
         */
        bw.write("    } finally {");
        bw.newLine();
        // bw.write("      out.flush();");
        // bw.newLine();
        // bw.write("      out.close();");
        // bw.newLine();
        bw.write("      _jspxfactory.releasePageContext(pageContext);");
        bw.newLine();
        bw.write("    }");
        bw.newLine();
        bw.write("  }");
        bw.newLine();

        bw.write("  private Object _jsp_mark_used(Object o){ return o; }");
        bw.newLine();

        if (addEncode) {
            bw.write("  private static StringBuffer __encode(String val) {");
            bw.newLine();
            bw.write("    StringBuffer sb = new StringBuffer();");
            bw.newLine();
            bw.write("    for (int i = 0; i < val.length(); ++i) {");
            bw.newLine();
            bw.write("      int ch = val.charAt(i);");
            bw.newLine();
            bw.write("      if (ch == 32) sb.append('+');");
            bw.newLine();
            bw.write("      else if ( (ch >='a' && ch <='z') || (ch >='A' && ch <='Z') || (ch >='0' && ch <='9')) sb.append((char)ch);");
            bw.newLine();
            bw.write("      else if (ch < 16) sb.append(\"%0\").append(Integer.toHexString(ch));");
            bw.newLine();
            bw.write("      else sb.append('%').append(Integer.toHexString(ch));");
            bw.newLine();
            bw.write("    }");
            bw.newLine();
            bw.write("    return sb;");
            bw.newLine();
            bw.write("  }");
            bw.newLine();

            bw.write("  private static String __encode(String url, String [] params) {");
            bw.newLine();

            bw.write("    boolean q = url.indexOf('?') >= 0;");
            bw.newLine();
            bw.write("    for (int i = 0; i < params.length;i += 2) {");
            bw.newLine();
            bw.write("      url += (q ? \"&\" : \"?\") + __encode(params[i]) + '=' + __encode(params[i + 1]);");
            bw.newLine();
            bw.write("      q = true; }");
            bw.newLine();

            bw.write("    return url;");
            bw.newLine();
            bw.write("  }");
            bw.newLine();
        }

        bw.write("}");
        bw.newLine();
        bw.close();
    }

    /**
     * Method parseParams.
     * 
     * @param args
     */
    private void parseParams(String[] args) {
        init();
        if (args.length < 1 || args.length > 6) {
            addError(sysMessage);
            return;
        }

        outDir = "";
        inFile = unquote(args[args.length - 1]);
        className = convert2JavaIdent(inFile);

        // parse options
        for (int i = 0; i < args.length - 1; ++i) {
            String a = args[i];
            if (a.length() < 2 || a.charAt(0) != '-' || "bdopn".indexOf(a.charAt(1)) == -1) {
                addError("invalid argument: " + a + "\r\n\r\n");
                continue;
            }
            String aa = a.substring(2);

            switch ("bdopn".indexOf(a.charAt(1))) {
                case 0:
                    basePath = unquote(aa);
                    break;
                case 1:
                    outDir = unquote(aa);
                    break;
                case 2:
                    className = aa;
                    break;
                case 3:
                    baseClass = aa;
                    break;
                case 4:
                    noLink = true;
            }
        }

        // get default path
        // path = file.getParent();
        int idx = inFile.lastIndexOf('/');
        if (idx == -1)
            idx = inFile.lastIndexOf('\\');
        if (idx >= 0)
            path = inFile.substring(0, idx);
        else
            path = "";

        if (basePath == null) {
            basePath = path;
        }

        if (!basePath.endsWith("/"))
            basePath += "/";

        if (!inFile.startsWith(basePath)) {
            addError("JSP file (" + inFile + ") not in base path (" + basePath + ")");
        }

        if (basePath.length() < path.length()) {
            String packageDir = path.substring(basePath.length());
            if (packageDir.length() != 0) {
                packageDir = replaceAll(packageDir, '\\', '/');
                packageDir = convert2JavaIdent(packageDir);
                outDir += '/' + packageDir;
                packName = replaceAll(packageDir, '/', '.');
            }
        }
        // add file section
        currentFile = inFile;
        if (basePath != null)
            currentFile = currentFile.substring(basePath.length());
        incFiles.put(currentFile, new Integer(1));

        if (basePath != null) {
            File bf = new File(basePath);
            String ab = bf.getAbsolutePath();
            basePath = replaceAll(ab, '\\', '/') + '/';
        }
    }

    /**
     * @param aa
     * @return
     */
    private String unquote(String aa) {
        if (aa.startsWith("\""))
            aa = aa.substring(1);
        if (aa.endsWith("\""))
            aa = aa.substring(0, aa.length() - 1);
        return aa;
    }

    /**
     * Converts a jsp file name into a Java identifier.
     * 
     * @param jspFileName
     * @return the converted String
     */
    public static String convert2JavaIdent(String jspFileName) {
        StringBuffer sb = new StringBuffer();
        for (StringTokenizer st = new StringTokenizer(jspFileName, "\\/"); st.hasMoreTokens();) {
            String s = st.nextToken();
            if (!Character.isLetter(s.charAt(0)))
                s = "_" + s;
            int slen = s.length();
            if (slen == 0)
                continue;
            for (int i = 0; i < slen; ++i) {
                int ch = s.charAt(i);
                if (ch == '.')
                    sb.append('_');
                else if (ch == '_')
                    sb.append("__");
                else if (ch > 127
                        || ch == '$'
                        || !((i == 0 && Character.isJavaIdentifierStart((char) ch)) || Character
                                .isJavaIdentifierPart((char) ch))) {
                    String hex = Integer.toHexString(ch);
                    while (hex.length() < 4)
                        hex = "0" + hex;
                    sb.append('$');
                    sb.append(hex);
                } else
                    sb.append((char) ch);
            }
            if (st.hasMoreTokens())
                sb.append("/");
        }
        return sb.toString();
    }

    /**
     * mangle the inFile name to a valid package.class.name.
     * 
     * @param inFile
     *            a file name with path
     * @return the mangled name.
     */
    /*
     * public static String mangleDir(String inFile) { StringBuffer sb = new
     * StringBuffer(); for (StringTokenizer st = new StringTokenizer(inFile,
     * "\\/"); st .hasMoreTokens();) { String s = st.nextToken(); int slen =
     * s.length(); if (slen == 0) continue; for (int i = 0; i < slen; ++i) { int
     * ch = s.charAt(i); if (ch == '.') sb.append('_'); else if (ch == '_')
     * sb.append("__"); else if (ch > 127 || ch == '$' || !( (i == 0 &&
     * Character.isJavaIdentifierStart((char) ch)) ||
     * Character.isJavaIdentifierPart((char)ch))) { String hex =
     * Integer.toHexString(ch); while (hex.length() < 4) hex = "0" + hex;
     * sb.append('$'); sb.append(hex); } else sb.append((char) ch); } if
     * (st.hasMoreTokens()) sb.append("/"); } return sb.toString(); }
     */

    /**
     * Main Loop to compile a complie jsp file int a java source code
     * 
     * @param fileName
     */
    private void compileFile(String fileName) {
        InputStream is = null;
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            try {
                is = new FileInputStream(fileName + ".jsp");
            } catch (FileNotFoundException fnfe) {
                is = new FileInputStream(fileName);
            }

            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }

            PeekInputStream pis = new PeekInputStream(is);
            parseStream(pis);
        } catch (Exception e) {
            // e.printStackTrace();
            String msg = e.getMessage();

            if (msg == null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                PrintWriter pw = new PrintWriter(bos);
                e.printStackTrace(pw);
                pw.flush();
                msg += " - " + bos.toString();
            }

            addError(fileName + " (" + line + "): " + msg);
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
            try {
                if (is != null)
                    is.close();
            } catch (Throwable ex) {
            }
        }
    }

    private void parseStream(PeekInputStream pis) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        boolean empty = true;
        // main loop, collect data until a '<' appears
        for (int c = pis.read(); c >= 0; c = pis.read()) { // count lines
            if (c == '\n')
                ++line; // mark bos as empty
            empty &= c <= 32;
            if (c != '<') {
                bos.write(c);
                continue;
            }

            bos2.reset();
            bos2.write(c);
            // determine the tag type
            c = pis.read();
            // count lines
            if (c == '\n')
                ++line; // recognize quoted template "<\%"
            if (c == '\\') {
                if (pis.peek() != '%')
                    bos2.write('\\');
                bos.write(bos2.toByteArray());
                continue;
            }

            bos2.write(c); // a jsp tag starts
            if (c == '%') {
                if (!empty && bos.size() > 0)
                    emit(bos);
                // mark current line
                scriptlet.addLineInfo();
                // handle the jsp tags
                c = pis.peek();
                // JspComment <%-- ... --%>
                if (c == '-') {
                    c = pis.read();
                    c = pis.read();
                    if (c != '-')
                        expected("<%-- found <%-");
                    rawRead(pis, "--%>", null);
                    continue;
                } // Directive <%@ ... %>
                if (c == '@') {
                    c = pis.read();
                    doDirective(pis);
                    continue;
                } // Expression <%= ... %>
                if (c == '=') {
                    c = pis.read();
                    /**/
                    scriptlet.write("out.print(");
                    boolean r = copyCode(pis, scriptlet);
                    scriptlet.writeln(");");
                    /*
                     * tempCS.reset(); boolean r = copyCode(pis, tempCS);
                     * scriptlet.writeln("out.print(" + tempCS.toString() +
                     * ");");
                     */
                    if (!r)
                        throw new Exception("missing %>");

                    /**
                     * / scriptlet.writeln2("out.print("); copyCode(pis, scriptlet); scriptlet.writeln(");");
                     */
                    continue;
                } // Declaration <%! ... %>
                if (c == '!') {
                    pis.read();
                    boolean r = copyCode(pis, declaration);
                    if (!r)
                        throw new Exception("missing %>");
                    continue;
                } // Scriptlet <% ... %>
                  // if (c > 32)
                  // expected("'<% ' but there is no white space");
                  // a tag starts - update html code into sriptlet
                boolean r = copyCode(pis, scriptlet);
                if (!r)
                    throw new Exception("missing %>");
                continue;
            }

            boolean endTag = c == '/';
            // collect the rest of the tag until ' ', ':' or '>' appears
            for (c = pis.peek(); c >= 0; c = pis.peek()) { // count lines
                if (c == '<')
                    break;
                pis.read();
                if (c == '\n')
                    ++line;
                bos2.write(c);
                if (c <= 32 || c == ':' || c == '>')
                    break;
            } // no user/jsp tag
            if (c != ':') {
                bos.write(bos2.toByteArray());
                continue;
            }

            String tag = bos2.toString();
            tag = tag.substring(1, tag.length() - 1);
            if (endTag)
                tag = tag.substring(1);

            // unknown tag - handle it relaxed!
            if (!"jsp".equals(tag) && taglibs.get(tag) == null) {
                bos.write(bos2.toByteArray());
                continue;
            }

            if (endTag) {
                if (!tag.equals(curTag)) {
                    unexpected("&lt;/" + tag + ":");
                }
                // exit from loop
                break;
            }

            // it is a user/jsp tag
            // a tag starts - update html code into sriptlet
            if (!empty && bos.size() > 0)
                emit(bos);
            // JspTags <jsp:[other] />
            // UserTags <xxx:[other] />
            doAction(tag, pis);
        }
        if (!empty && bos.size() > 0)
            emit(bos);
        // mark last line
        scriptlet.addLineInfo();
    }

    /**
     * throw an exception
     */
    void expected(String s) throws Exception {
        throw new Exception("expected " + s);
    }

    /**
     * throw an exception
     */
    void unexpected(String s) throws Exception {
        throw new Exception("unexpected " + htmlEscape(s, false));
    }

    /**
     * throw an exception
     */
    void required(String s) throws Exception {
        throw new Exception("required attribute missing: " + s);
    }

    /**
     * verify an attributes value
     */
    void verify(String s, String of[]) throws Exception {
        int i;
        // s = s.substring(1, s.length()-1);
        for (i = 0; i < of.length; ++i)
            if (s.compareTo(of[i]) == 0)
                break;
        if (i == of.length) {
            String msg = "ERROR: attribute value '" + s + "' is invalid. Possible values: ";
            for (i = 0; i < of.length; ++i)
                msg += "\"" + of[i] + "\" ";
            throw new Exception(msg);
        }
    }

    /**
     * no scriptlet allowed
     */
    void noScriptlet(String s) throws Exception {
        if (s.charAt(0) != '"')
            throw new Exception("no scriptlet expression allowed: " + s);
    }

    /**
     * emit a out.print(...) statement to the scriptlet for the buffered raw data.
     * 
     * @param bos
     *            the buffer where the data is written to.
     */
    void emit(ByteArrayOutputStream bos) throws IOException {
        try {
            if (bos.size() == 0)
                return;

            byte[] htm = bos.toByteArray();
            int pos = 0;
            bos.reset();
            do {
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                boolean empty = true;

                // preserve white spaces, even when I dont like it
                // int npos = htm.length;

                int npos = pos;
                boolean lf = false;
                // used to suppress multiple linefeeds in HTML
                while (npos < htm.length) {
                    int ch = htm[npos++] & 0xff;
                    if (ch < 32) {
                        if (!lf) {
                            s.write('\\');
                            s.write('n');
                        }
                        lf = true;
                        continue;
                    }
                    lf = false;
                    if (ch == '"' || ch == '\\')
                        s.write('\\');
                    s.write(ch);
                    empty &= ch <= 32;
                } // use a single space when only white spaces are found

                if (empty) {
                    if (s.size() > 0 && lastPos != scriptlet.size()) {
                        scriptlet.writeln3("out.print(\" \");");
                        lastPos = scriptlet.size();
                        return;
                    } // if only white spaces were found, a space is put into
                      // next buffer after reset
                    if (npos == htm.length) {
                        bos.write(32);
                        return;
                    } // or the next line starts with a white space
                    s.reset();
                    s.write(32);
                } else {
                    scriptlet.writeln3("out.print(\"" + s.toString() + "\");");
                }
                pos = npos;
            } while (pos < htm.length);
        } finally {
            codeEndLine = line;
        }
    }

    /**
     * read instream until match String was found
     */
    private void rawRead(InputStream is, String end, OutputStream os) throws IOException {
        int start = line;
        for (int i = 0; i < end.length();) {
            int ch = is.read();
            if (ch == '\n')
                ++line;
            if (ch < 0) {
                line = start;
                throw new IOException("missing " + end);
            }
            if (end.charAt(i) == ch)
                ++i;
            else if (i > 0) {
                int j = i;
                while (i > 0) {
                    --i;
                    if (end.charAt(i) == ch && end.startsWith(end.substring(j - i, j))) {
                        ++i;
                        break;
                    }
                }
            }
        }
    }

    /**
     * read instream until %> and copy content to CodeStream
     */
    private boolean copyCode(PeekInputStream is, CodeStream cs) throws IOException {
        int start = line;
        cs.write(""); // indent
        int ch = 0;
        for (;;) {
            ch = is.read();
            if (ch < 0) {
                codeEndLine = line;
                line = start;
                return false;
            }
            // end tag "%>", but recognize quoted end tag "%\>"
            if (ch == '%') {
                if (is.peek() == '>') {
                    is.read();
                    break;
                } // quoted?
                if (is.peek() == '\\') {
                    is.read();
                    if (is.peek() != '>') {
                        cs.write('%');
                        ch = '\\';
                    } else
                        ch = '%';
                }
            }
            if (ch == '\n') {
                cs.lf();
                ++line;
                cs.write(""); // indent
            } else if (ch != '\r')
                cs.write(ch);
        }
        // cs.lf();
        codeEndLine = line;
        return true;
    }

    private void nextWord(PeekInputStream is, ByteArrayOutputStream os, String end) throws Exception {
        os.reset();
        // get Word
        int ch;
        for (ch = is.read(); ch > 32; ch = is.read()) {
            os.write(ch);
            if (end.indexOf(is.peek()) >= 0)
                break;
        }

        if (ch == '\n')
            ++line;
        skipSpaces(is);
    }

    private void nextAttr(char quote, int ch, PeekInputStream is, ByteArrayOutputStream os) throws Exception {
        os.reset();
        os.write(ch);

        if (ch == '\\' && is.peek() == quote) {
            os.write(quote);
            is.read();
        }

        while (is.peek() != quote) {
            boolean lastLT = ch == '<';
            boolean lastPC = ch == '%';
            ch = is.read();
            if (ch < 0)
                expected("attribute found EOF");
            if (ch == '\n')
                ++line;
            else if (ch == '\\') {
                ch = is.peek();
                if (lastLT && ch == '%') {
                    ch = is.read();
                    os.write(ch);
                    ch = 0;
                    continue;
                }
                if (lastPC && ch == '>') {
                    ch = is.read();
                    os.write(ch);
                    ch = 0;
                    continue;
                }
                if (ch == '\'' || ch == '"' || ch == '\\') {
                    ch = is.read();
                }
            }
            if (ch == '"')
                os.write('\\');
            os.write(ch);
        }
    }

    private void nextRtAttr(char quote, PeekInputStream is, ByteArrayOutputStream os) throws Exception {
        os.reset();
        os.write(' ');
        int ch = 0;
        while (is.peek() != quote) {
            boolean lastLT = ch == '<';
            boolean lastPC = ch == '%';
            ch = is.read();
            if (ch < 0)
                expected("attribute value found EOF");

            if (ch == '\n')
                ++line;
            else if (ch == '\\') {
                ch = is.peek();
                if (lastLT && ch == '%') {
                    ch = is.read();
                    os.write(ch);
                    ch = 0;
                    continue;
                }
                if (lastPC && ch == '>') {
                    ch = is.read();
                    os.write(ch);
                    ch = 0;
                    continue;
                }
                if (ch == '\'' || ch == '"' || ch == '\\') {
                    ch = is.read();
                }
            }
            os.write(ch);
        }
    }

    private void skipSpaces(PeekInputStream is) throws Exception {
        for (int ch = is.peek(); ch <= 32 && ch >= 0; ch = is.peek()) {
            if (is.read() == '\n')
                ++line;
        }
    }

    /**
     * handle <%@ ... %>
     */
    private void doDirective(PeekInputStream is) throws Exception {
        Vector aName = new Vector();
        Vector aValue = new Vector();
        // get directive
        skipSpaces(is);
        nextWord(is, tempBO, "%");
        String d = tempBO.toString().trim();
        // get attributes
        while (is.peek() != -1 && is.peek() != '%') {

            skipSpaces(is);
            if (is.peek() < 0 || "%>".indexOf(is.peek()) >= 0)
                break;
            nextWord(is, tempBO, "=");
            String n = tempBO.toString().trim();
            if (n.length() == 0)
                break;
            int ch = is.read();
            if (ch != '=')
                expected(n + "=");
            skipSpaces(is);
            char quote = (char) is.read();
            if (quote != '"' && quote != '\'')
                expected(n + "=\"");
            ch = is.read();
            if (ch == '\n')
                ++line;
            String v = "";
            if (ch != quote) {
                nextAttr(quote, ch, is, tempBO);
                v = tempBO.toString();
                ch = is.read();
            }
            if (ch != quote)
                expected("" + quote + " at end of attribute");
            aName.addElement(n);
            aValue.addElement(v);
        }

        if (is.read() != '%' || is.read() != '>')
            expected("%>"); // handle directive
        if (d.equals("page"))
            doPage(aName, aValue);
        else if (d.equals("include"))
            doInclude(aName, aValue);
        else if (d.equals("taglib")) {
            doTaglib(aName, aValue);
        } else
            expected("page, include or taglib directive, found: " + d);
    }

    /**
     * 
     page_directive_attr_list ::= { language=???scriptingLanguage??? } { extends=???className??? } {
     * import=???importList??? } { session=???true|false??? } { buffer=???none|sizekb??? } { autoFlush=???true|false???
     * } { isThreadSafe=???true|false??? } { info=???info_text??? } { errorPage=???error_url??? } {
     * isErrorPage=???true|false??? } { contentType=???ctinfo??? } { pageEncoding=???peinfo??? }
     */
    private void doPage(Vector aName, Vector aValue) throws Exception {
        for (int i = 0, N = aValue.size(); i < N; ++i) {
            String n = (String) aName.elementAt(i);
            String v = (String) aValue.elementAt(i);
            if (n.equals("extends")) {
                if (bBaseClass)
                    throw new Exception("extends already defined");
                bBaseClass = true;
                baseClass = v.trim();
            } else if (n.equals("import")) {
                StringTokenizer sz = new StringTokenizer(v, ", \r\n\t\f");
                while (sz.hasMoreTokens()) {
                    String t = sz.nextToken();
                    if (!importSet.contains(t)) {
                        importSet.add(t);
                        importOrder.add(t);
                    }
                }
            } else if (n.equals("session")) {
                if (bSession)
                    throw new Exception("session already defined");
                bSession = true;
                verify(v, trueFalse);
                needsSession = v;
            } else if (n.equals("buffer")) {
                if (bBuffer)
                    throw new Exception("buffer already defined");
                bBuffer = true;
                if (v.equals("none"))
                    bufferSize = -1;
                else {
                    int idx = v.indexOf("kb");
                    if (idx == -1 || idx + 2 != v.length())
                        unexpected("buffer=\"" + v + "\"");
                    bufferSize = Integer.parseInt(v.substring(0, idx)) * 1024;
                }
                if (bufferSize < 0 && autoFlush.equals("false"))
                    throw new Exception("bufferSize > 0 required when autoFlush=false");
            } else if (n.equals("autoFlush")) {
                if (bAutoFlush)
                    throw new Exception("autoFlush already defined");
                bAutoFlush = true;
                verify(v, trueFalse);
                autoFlush = v;
                if (bufferSize < 0 && autoFlush.equals("false"))
                    throw new Exception("bufferSize > 0 required when autoFlush=false");
            } else if (n.equals("isThreadSafe")) {
                if (bIsThreadSafe)
                    throw new Exception("isThreadSafe already defined");
                bIsThreadSafe = true;
                verify(v, trueFalse);
                threadSafe = v.equals("true");
            } else if (n.equals("info")) {
                if (bInfo)
                    throw new Exception("info already defined");
                bInfo = true;
                info = v;
            } else if (n.equals("errorPage")) {
                if (bErrorPage)
                    throw new Exception("errorpage already defined");
                bErrorPage = true;
                errorPage = v;
                checkUrl("errorPage", errorPage);
            } else if (n.equals("isErrorPage")) {
                if (bIsErrorPage)
                    throw new Exception("isErrorPage already defined");
                bIsErrorPage = true;
                verify(v, trueFalse);
                isErrorPage = v.equals("true");
            } else if (n.equals("contentType")) {
                if (bContentType)
                    throw new Exception("contentType already defined");
                bContentType = true;
                int idx = v.indexOf(';');
                if (idx > 0) {
                    String part = v.substring(idx + 1).trim();
                    if (part.substring(0, 7).equalsIgnoreCase("charset")) {
                        v = v.substring(0, idx);
                        idx = part.indexOf('=');
                        pageEncoding = part.substring(idx + 1);
                    }
                }
                contentType = v;
            } else if (n.equals("pageEncoding")) {
                if (bPageEncoding)
                    throw new Exception("pageEncoding already defined");
                bPageEncoding = true;
                pageEncoding = v;
            } else if (n.equals("language")) {
                if (bLanguage)
                    throw new Exception("language already defined");
                bLanguage = true;
                verify(v, languages);
            } else
                unexpected(n + "=\"" + v + "\"");
        }
    }

    /**
     * verifies the url.
     * 
     * @param url
     *            the url to verify
     */
    private void checkUrl(String name, String url) throws Exception {
        label: {
            if (url.length() == 0)
                break label;
            if (url.startsWith("http://"))
                break label;
            if (url.startsWith("https://"))
                break label;
            return;
        }
        throw new Exception("invalid relative URL " + name + "=\"" + url + "\"");
    }

    /**
     * handle the include directive.
     */
    private void doInclude(Vector aName, Vector aValue) throws Exception {
        String n = null;
        if (aName.size() == 1)
            n = (String) aName.elementAt(0);
        if (aName.size() != 1 || !n.equals("file"))
            expected("file=\"...\"");
        String v = (String) aValue.elementAt(0);

        checkUrl("file", v);

        String fn;
        if (v.charAt(0) == '/')
            fn = basePath + v;
        else
            fn = path + "/" + v;
        for (int ppp = fn.indexOf("/../"); ppp > 0; ppp = fn.indexOf("/../")) {
            int ldir = fn.lastIndexOf("/", ppp - 1);
            if (ldir < 0)
                ldir = fn.lastIndexOf("\\", ppp - 1);
            fn = fn.substring(0, ldir) + fn.substring(ppp + 3);
        }

        // keep old stuff
        int oline = line;
        String ocurf = currentFile;
        String opath = path;

        int idx = fn.lastIndexOf('/');
        if (idx == -1)
            idx = fn.lastIndexOf('\\');
        path = fn.substring(0, idx);
        codeEndLine = line = 1;
        currentFile = fn.substring(basePath.length());

        // mark included files.
        if (incFiles.get(currentFile) == null) {
            incFiles.put(currentFile, new Integer(incFiles.size() + 1));
        }

        // check for recursive includes
        if (recursiveInc.get(currentFile) != null)
            throw new Exception("recursive include: " + currentFile);
        recursiveInc.put(currentFile, currentFile);
        try {
            compileFile(fn);
        } catch (Exception e) {
            throw new Exception("in included file " + fn + ": " + e.getMessage());
        } finally {
            recursiveInc.remove(currentFile);
            currentFile = ocurf;
            path = opath;
            codeEndLine = line = oline;
        }
    }

    private void doTaglib(Vector aName, Vector aValue) throws Exception {
        String uri = null;
        String prefix = null;
        for (int i = 0, N = aValue.size(); i < N; ++i) {
            String n = (String) aName.elementAt(i);
            String v = (String) aValue.elementAt(i);
            if (n.equals("uri")) {
                uri = v.trim();
            } else if (n.equals("prefix")) {
                prefix = v.trim();
            } else
                unexpected(n + "=\"" + v + "\"");
        }

        if (uri == null)
            required("uri=...");
        if (prefix == null)
            required("prefix=...");

        String orgUri = uri; // for error message
        // load uriMap
        loadTaglibs();

        // prefix already loaded?
        if (taglibs.get(prefix) != null)
            return;

        // map URI if mapping exist or implicit mapping was used.
        String mappedUri = (String) uriMap.get(uri);
        if (mappedUri == null) {
            if (!uri.startsWith("/"))
                uri = "/WEB-INF/" + uri;

            if (basePath != null) {
                mappedUri = "file:/" + basePath + uri.substring(1);
            }
        }

        // tag already loaded
        TagLibraryInfo tli = (TagLibraryInfo) uri2tli.get(mappedUri);
        if (tli != null) {
            taglibs.put(prefix, tli);
            return;
        }

        XmlFile xf = (XmlFile) xmlMap.get(mappedUri);
        /*
         * if (xf == null) { String m = mappedUri; if (!m.startsWith("/")) m =
         * "/WEB-INF/" + m; if (basePath != null) { m = "file:" + basePath +
         * m.substring(1); } xf = (XmlFile)xmlMap.get(m); if (xf != null)
         * mappedUri = m; }
         */
        /*
         * if (xf == null) { //System.out.println(mappedUri); for (Iterator i =
         * xmlMap.keySet().iterator(); i.hasNext();) { String s =
         * (String)i.next(); //System.out.println(s); if
         * (s.toLowerCase().indexOf(mappedUri.toLowerCase()) >= 0) {
         * //System.out.println(s + " - " + mappedUri); xf =
         * (XmlFile)xmlMap.get(s); xmlMap.put(mappedUri, xf); break; } } }
         */
        if (xf == null)
            throw new Exception("could not load TLD for uri=" + orgUri);

        // and load the TagLibInfo from the specified tlds file
        tli = new TagLibraryInfo(this, prefix, uri, mappedUri);
        tli.init(xf);
        uri2tli.put(mappedUri, tli);
        taglibs.put(prefix, tli);
    }

    /**
     * Escape some characters... "<b>über</b>" -->
     * "&lt;b&gt;&uuml;ber&lt;/b&gt;"
     * 
     * @param s
     *            a String
     * @param isEdit
     *            true, if encoding is done for edit page
     * @return the escaped String.
     */
    public static String htmlEscape(String s, boolean isEdit) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch (ch) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case 'ä':
                sb.append("&auml;");
                break;
            case 'ö':
                sb.append("&ouml;");
                break;
            case 'ü':
                sb.append("&uuml;");
                break;
            case 'Ä':
                sb.append("&Auml;");
                break;
            case 'Ö':
                sb.append("&Ouml;");
                break;
            case 'Ü':
                sb.append("&Uuml;");
                break;
            case '&':
                if (isEdit) {
                    sb.append("&amp;");
                    break;
                }
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    
    /**
   * 
   */
    private void loadTaglibs() {
        if (uriMap != null)
            return;

        uriMap = new HashMap();

        // add explicit mappings
        String webxml = this.basePath + "/WEB-INF/web.xml";
        XmlFile xf = new XmlFile(webxml);
        Vector vtl = xf.getSections("/web-app/taglib");
        if (vtl.size() == 0)
            vtl = xf.getSections("/web-app/jsp-config/taglib");
        for (int i = 0; i < vtl.size(); ++i) {
            String tl = (String) vtl.elementAt(i);
            String key = xf.getContent(tl + "taglib-uri");
            String url = xf.getContent(tl + "taglib-location");

            if (!url.startsWith("/"))
                url = "/WEB-INF/" + url;

            if (basePath != null) {
                url = "file:/" + basePath + url.substring(1);
            }

            if (url.endsWith(".jar"))
                url = "jar:" + url + "!/META-INF/taglib.tld";

            uriMap.put(key, url);
            /*
             * not needed: the spec requires tld files to live in/below WEB-INF
             * all those files are recognized via implicit mapping
             */
            try {
                URL u = new URL(url);
                InputStream is = u.openStream();
                XmlFile xm = new XmlFile();
                xm.read(is);
                is.close();

                xmlMap.put(url, xm);
            } catch (Exception ex) {

            }
            /**/
        }

        // add implicit mappings
        try {
            String cp = classLoader.getClassPath();
            // System.out.println(cp);
            ZipClassLoader zcl = new ZipClassLoader(cp, classLoader);
            zcl.addPath(basePath + "WEB-INF");

            String files[] = zcl.list("*.tld");
            for (int i = 0; i < files.length; ++i) {
                for (Enumeration e = zcl.findResources(files[i]); e.hasMoreElements();) {
                    URL url = (URL) e.nextElement();
                    // System.out.println("x:" + url);
                    InputStream is = zcl.getResourceAsStream(url);
                    XmlFile xm = new XmlFile();
                    xm.read(is);
                    is.close();

                    String furl = zcl.getResource(files[i]).toString();
                    String xuri = xm.getContent("/taglib/uri/");
                    // System.out.println(furl + ":" + xuri);
                    if (xuri != null) {
                        uriMap.put(xuri, furl);
                    }

                    xmlMap.put(furl, xm);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * handle <%@ ... %>
     */
    private void doAction(String tag, PeekInputStream is) throws Exception {
        Vector aName = new Vector();
        Vector aValue = new Vector();

        // get directive
        skipSpaces(is);

        nextWord(is, tempBO, "/>");
        String d = tempBO.toString().trim();

        // get attributes
        while (is.peek() != -1 && "/>".indexOf(is.peek()) < 0) {
            skipSpaces(is);
            if (is.peek() < 0 || "/>".indexOf(is.peek()) >= 0)
                break;

            nextWord(is, tempBO, "=");
            String n = tempBO.toString().trim();
            if (n.length() == 0)
                break;

            aName.addElement(n);

            int ch = is.read();
            if (ch != '=')
                expected(n + "=");

            skipSpaces(is);

            char quote = (char) is.read();
            if (quote != '"' && quote != '\'')
                expected(n + "=\"");

            if (is.peek() != quote) {
                ch = is.read();
                if (ch == '\n')
                    ++line;

                // scriptlet expression
                if (ch == '<' && is.peek() == '%') {
                    is.read();
                    ch = is.read();
                    if (ch != '=')
                        expected("<%= found <%" + (char) ch);

                    // scriptlet.addFullInfo();

                    /*
                     * wrong, since rt-attr-values must be unescaped
                     * tempCS.reset(); copyCode(is, tempCS); String v = " " +
                     * tempCS.toString().trim();
                     */
                    try {
                        nextRtAttr(quote, is, tempBO);

                        String v = tempBO.toString();
                        if (!v.endsWith("%>"))
                            expected("%>" + quote);
                        v = v.substring(0, v.length() - 2);

                        aValue.addElement(v);
                    } catch (Exception ex) {
                        // be nice and add this attribute to have support
                        // completion
                        String v = tempBO.toString();
                        aValue.addElement(v);
                        actionBefore(tag, d, aName, aValue);
                        throw ex;
                    }
                } else {
                    nextAttr(quote, ch, is, tempBO);
                    String v = "\"" + tempBO.toString() + "\"";
                    aValue.addElement(v);
                }
            } else {
                aValue.addElement("\"\"");
            }

            ch = is.read();
            if (ch != quote)
                expected("" + quote + " at end of attribute");
        }

        if (is.peek() == '>') {
            is.read();

            actionBefore(tag, d, aName, aValue);

            String oTag = curTag;
            curTag = tag;

            parseStream(is);

            curTag = oTag;

            nextWord(is, tempBO, ">");

            String d2 = tempBO.toString();
            if (!d.equals(d2)) {
                if (is.peek() < 0)
                    unexpected("EOF expected &lt;/" + tag + ":" + d);

                unexpected("&lt;/" + tag + ":" + d2 + " expected &lt;/" + tag + ":" + d);
            }

            if (is.read() != '>')
                expected(">");

            actionAfter(tag, d, aName, aValue);

            return;
        }
        if (is.read() != '/' || is.read() != '>')
            expected("/>");

        actionBefore(tag, d, aName, aValue);
        actionAfter(tag, d, aName, aValue);
    }

    /**
     * handle action before it???s body
     */
    private void actionBefore(String tag, String subtag, Vector aName, Vector aValue) throws Exception {
        // tagList.push(tag + ":" + subtag);
        // handle jsp tag
        if (tag.equals("jsp")) {
            if (subtag.equals("useBean")) {
                String id, scope, clazz, type, bean;
                id = scope = clazz = type = bean = null;
                for (int i = 0, N = aValue.size(); i < N; ++i) {
                    String n = (String) aName.elementAt(i);
                    String s = (String) aValue.elementAt(i);
                    String v = s.length() >= 2 && s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s;
                    if ("id".equals(n)) {
                        noScriptlet(s);
                        id = v;
                    } else if ("scope".equals(n)) {
                        noScriptlet(s);
                        scope = v;
                    } else if ("class".equals(n)) {
                        noScriptlet(s);
                        clazz = v;
                    } else if ("type".equals(n)) {
                        noScriptlet(s);
                        type = v;
                    } else if ("beanName".equals(n)) {
                        bean = s;
                    } else
                        unexpected(n + "=");
                }
                // verify attr
                if (id == null)
                    required("id");
                if (scope == null)
                    scope = "page";
                if (bean != null && clazz != null)
                    throw new Exception("either beanName or class can be used");
                if (clazz == null && bean == null && type == null)
                    required("class, type or bean");
                if (type == null)
                    type = clazz;
                if (type == null)
                    type = bean;

                verify(scope, scopes);

                if (type != null) {
                    String ttt = type;
                    int array = 0;
                    while (ttt.endsWith("[]")) {
                        ttt = ttt.substring(0, ttt.length() - 2);
                        ++array;
                    }
                    Class c = loadClass(ttt);
                    type = c.getName();
                    for (;;) {
                        Class decl = c.getDeclaringClass();
                        if (decl == null)
                            break;
                        int len = decl.getName().length();
                        type = type.substring(0, len) + "." + type.substring(len + 1);
                        c = decl;
                    }
                    while (array-- > 0) {
                        type += "[]";
                    }
                }

                if (scope.equals("page"))
                    scope += "Context";

                // create code
                scriptlet.writeln(type + ' ' + id + ";");
                scriptlet.writeln("synchronized (" + scope + ") {");
                scriptlet.indent();
                scriptlet.writeln(id + " = (" + type + ")" + scope + ".getAttribute(\"" + id + "\");");
                if (clazz == null && bean == null) {
                    scriptlet.writeln("if (" + id + " == null)");
                    scriptlet.indent();
                    scriptlet.writeln("throw new java.lang.InstantiationException(\"" + id + " not found\");");
                    scriptlet.unindent();
                    scriptlet.writeln("{");
                    scriptlet.indent();
                } else {
                    scriptlet.writeln("if (" + id + " == null) {");
                    scriptlet.indent();
                    if (clazz != null) {
                        scriptlet.writeln(id + " = (" + type + ")getClass().getClassLoader().loadClass(\"" + clazz
                                + "\").newInstance();");
                    } else {
                        scriptlet.writeln(id + " = (" + type
                                + ")java.beans.Beans.instantiate(getClass().getClassLoader()," + bean + ");");
                    }
                    scriptlet.writeln(scope + ".setAttribute(\"" + id + "\", " + id + ");");
                }
                name2Type.put(id, type);

                return;
            }

            // ==================================================================
            if (subtag.equals("setProperty")) {
                String name, property, param, value;
                name = property = param = value = null;
                for (int i = 0, N = aValue.size(); i < N; ++i) {
                    String n = (String) aName.elementAt(i);
                    String s = (String) aValue.elementAt(i);
                    String v = s.length() >= 2 && s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s;

                    if ("name".equals(n)) {
                        noScriptlet(s);
                        name = v;
                    } else if ("property".equals(n)) {
                        noScriptlet(s);
                        property = v;
                    } else if ("param".equals(n)) {
                        noScriptlet(s);
                        param = v;
                    } else if ("value".equals(n)) {
                        value = s;
                    }
                }

                // verify all parameters
                if (name == null)
                    required("name");

                if (property == null)
                    required("property");

                if (param != null && value != null)
                    throw new Exception("either param or value can be used");

                if (param == null && value == null && !property.equals("*"))
                    param = property;

                if ((param != null || value != null) && property.equals("*"))
                    throw new Exception("param or value cannot be used with property=\"*\"");

                // try to apply all parameters
                if (property.equals("*")) {
                    if (!noLink)
                        scriptlet.writeln("de.bb.jsp.BeanHelper._jsp_copyParameters(request, " + name + ");");
                    return;
                }

                String cn = (String) name2Type.get(name);
                if (cn == null)
                    throw new Exception("no bean defined for: " + name);

                Class clazz = loadClass(cn);

                if (setByBeanInfo(clazz, name, property, value, param))
                    return;

                property = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);

                Class para = findMethodParamClass(clazz, property);

                // apply this parameter
                if (param != null) {
                    String pType = para.getName();
                    String expression = "_jsp_v";
                    if (para.isPrimitive()) {
                        if (pType.equals("boolean"))
                            expression = "\"\".equals(_jsp_v) ? false : Boolean.valueOf(_jsp_v).booleanValue()";
                        else if (pType.equals("byte"))
                            expression = "\"\".equals(_jsp_v) ? (byte)0 : Byte.valueOf(_jsp_v).byteValue()";
                        else if (pType.equals("char"))
                            expression = "\"\".equals(_jsp_v) ? (char)0 : _jsp_v.charAt(0)";
                        else if (pType.equals("double"))
                            expression = "\"\".equals(_jsp_v) ? (double)0 : Double.valueOf(_jsp_v).doubleValue()";
                        else if (pType.equals("int"))
                            expression = "\"\".equals(_jsp_v) ? (int)0 : Integer.valueOf(_jsp_v).intValue()";
                        else if (pType.equals("float"))
                            expression = "\"\".equals(_jsp_v) ? (float)0 : Float.valueOf(_jsp_v).floatValue()";
                        else if (pType.equals("long"))
                            expression = "\"\".equals(_jsp_v) ? (long)0 : Long.valueOf(_jsp_v).longValue()";
                        else if (pType.equals("short"))
                            expression = "\"\".equals(_jsp_v) ? (short)0 : Short.valueOf(_jsp_v).shortValue()";
                    } else {
                        if (pType.equals("java.lang.Boolean"))
                            expression = "\"\".equals(_jsp_v) ? new Boolean() : new Boolean(_jsp_v)";
                        else if (pType.equals("java.lang.Byte"))
                            expression = "\"\".equals(_jsp_v) ? new Byte() : new Byte(_jsp_v)";
                        else if (pType.equals("java.lang.Character"))
                            expression = "\"\".equals(_jsp_v) ? new Character() : new Character(_jsp_v.charAt(0))";
                        else if (pType.equals("java.lang.Double"))
                            expression = "\"\".equals(_jsp_v) ? new Double() : new Double(_jsp_v)";
                        else if (pType.equals("java.lang.Integer"))
                            expression = "\"\".equals(_jsp_v) ? new Integer() : new Integer(_jsp_v)";
                        else if (pType.equals("java.lang.Float"))
                            expression = "\"\".equals(_jsp_v) ? new Float() : new Float(_jsp_v)";
                        else if (pType.equals("java.lang.Long"))
                            expression = "\"\".equals(_jsp_v) ? new Long() : new Long(_jsp_v)";
                        else if (pType.equals("java.lang.Short"))
                            expression = "\"\".equals(_jsp_v) ? new Short() : new Short(_jsp_v)";
                        else if (!pType.equals("java.lang.String"))
                            expression = "new " + pType + "(_jsp_v)";
                    }

                    scriptlet.writeln("{");
                    scriptlet.writeln("  String _jsp_v = request.getParameter(\"" + param + "\");");
                    scriptlet.writeln("  if (_jsp_v == null ) _jsp_v = \"\";");

                    scriptlet.writeln("  " + name + "." + property + "(" + expression + ");");
                    scriptlet.writeln("}");
                    return;
                }

                // set this value
                String outValue = mapValue(para, value);
                if (outValue != null) {
                    scriptlet.writeln(name + "." + property + "(" + outValue + ");");
                    return;
                }

                String pType = para.getName();
                scriptlet.writeln("{ String _$_ = " + value + ";");
                scriptlet.writeln(name + "." + property + "( _$_ == null ? null : new " + pType + "(_$_));}");
                return;
            }

            // ==================================================================
            if (subtag.equals("getProperty")) {
                String name, property;
                name = property = null;
                for (int i = 0, N = aValue.size(); i < N; ++i) {
                    String n = (String) aName.elementAt(i);
                    String s = (String) aValue.elementAt(i);
                    String v = s.length() >= 2 && s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s;

                    noScriptlet(s);
                    if ("name".equals(n))
                        name = v;
                    else if ("property".equals(n))
                        property = v;
                }

                if (name == null)
                    required("name");
                if (property == null)
                    required("property");

                // if the object is defined via useBean
                if (null != name2Type.get(name)) {
                    property = property.substring(0, 1).toUpperCase() + property.substring(1);
                    String prop = "get" + property;

                    String cn = (String) name2Type.get(name);
                    Class clazz = loadClass(cn);

                    Method meth = clazz.getMethod(prop, JspCC.NOPARAMS);
                    if (meth == null && clazz.getMethod("is" + property, JspCC.NOPARAMS) != null) {
                        prop = "is" + property;
                    }

                    scriptlet.writeln("out.print(" + name + "." + prop + "());");
                    return;
                }

                // TODO: buggy!!!
                // either try to load the variable from context
                scriptlet.writeln("{ Object ao = pageContext.findAttribute(\"" + property + "\");");
                scriptlet.writeln("  if (ao == null) throw new ServletException(\"" + property + " is unavailable\");");
                scriptlet.writeln("  out.print(ao); }");

                return;
            }
            // ==================================================================
            if (subtag.equals("param")) {
                if (jspPage == null)
                    throw new Exception("jsp:param is only valid in jsp:forward, jsp:include or jsp:params");
                String name, value;
                name = value = null;
                for (int i = 0, N = aValue.size(); i < N; ++i) {
                    String n = (String) aName.elementAt(i);
                    String s = (String) aValue.elementAt(i);

                    if ("name".equals(n)) {
                        noScriptlet(s);
                        name = s;
                    } else if ("value".equals(n)) {
                        value = s;
                    }
                }

                if (name == null)
                    required("name");
                if (value == null)
                    required("value");

                // TODO encode the values and apply them correctly!
                requestAttributes.put(name, value);
                // scriptlet.writeln("request.setAttribute(" + name +
                // ", \"\" + " + value + ");");
                return;
            }
            // ==================================================================
            if (subtag.equals("include")) {
                requestAttributes = new HashMap();
                jspPage = null;
                flush = false;
                for (int i = 0, N = aValue.size(); i < N; ++i) {
                    String n = (String) aName.elementAt(i);
                    String s = (String) aValue.elementAt(i);
                    String v = s.length() >= 2 && s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s;
                    if ("page".equals(n)) {
                        if (s.charAt(0) == '"')
                            checkUrl("page", v);

                        jspPage = s;
                    } else if ("flush".equals(n)) {
                        noScriptlet(s);
                        flush = "true".equals(v);
                    } else
                        unexpected(n + "=...");
                }
                if (jspPage == null)
                    required("page");

                return;
            }
            // ==================================================================
            if (subtag.equals("forward")) {
                requestAttributes = new HashMap();
                jspPage = null;
                if (aValue.size() == 1) {
                    String n = (String) aName.elementAt(0);
                    String s = (String) aValue.elementAt(0);
                    String v = s.length() >= 2 && s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s;
                    if ("page".equals(n)) {
                        if (s.charAt(0) == '"')
                            checkUrl("page", v);
                        jspPage = s;
                    }
                }
                if (jspPage == null)
                    required("page");
                return;
            }
            // ==================================================================
            throw new Exception("jsp:" + subtag + " is not yet supported");
        }

        // lookup TagLibInfo
        TagLibraryInfo tli = (TagLibraryInfo) taglibs.get(tag);
        if (tli == null)
            throw new Exception("unknown user tag &lt;" + tag + ":... add <%@ taglib prefix=\"" + tag + "\" ...&gt;");

        // lookup TagInfo
        TagInfo ti = tli.getTagInfo(subtag);
        if (ti == null)
            throw new Exception("unknown user tag &lt;" + tag + ":" + subtag + " ... check " + tli.getURI());

        // write output to create that tag
        String cn = ti.getTagClassName();
        Class tagClass = loadClass(cn);
        int tagCount = tagStack.size();
        String tagVarName = "_jsp_tag_" + replaceAll(cn, '.', '_') + "$" + resVarCount;

        scriptlet.writeln(cn + " " + tagVarName + " = new " + cn + "();");

        scriptlet.writeln(tagVarName + ".setPageContext(pageContext);");
        if (tagCount > 0) {
            Object o3[] = (Object[]) tagStack.peek();
            String lastTagVarName = (String) o3[2];
            scriptlet.writeln(tagVarName + ".setParent(" + lastTagVarName + ");");
        } else
            scriptlet.writeln(tagVarName + ".setParent(null);");

        // scriptlet.writeln("_jspTag = " + tn + ";");

        // verify attributes - put all into a hash
        HashMap htai = new HashMap();
        TagAttributeInfo[] tais = ti.getAttributes();
        for (int i = 0; i < tais.length; ++i)
            htai.put(tais[i].getName(), tais[i]);

        Hashtable tagDatas = new Hashtable();

        // check params and remove them from Hashtable
        for (int i = 0, N = aValue.size(); i < N; ++i) {
            String n = (String) aName.elementAt(i);
            String v = (String) aValue.elementAt(i);

            TagAttributeInfo tai = (TagAttributeInfo) htai.remove(n);
            if (tai == null)
                unexpected(n + "=...");

            if (v.charAt(0) == '"') {
                String s = v.substring(1, v.length() - 1);
                tagDatas.put(n, s);
            }

            // neu.setAttribute("src", "foo");
            Class clazz = loadClass(cn);

            if (setByBeanInfo(clazz, tagVarName, n, v, null))
                continue;

            String method = "set" + n.substring(0, 1).toUpperCase() + n.substring(1);

            Class para = findMethodParamClass(clazz, method);

            String outVal = mapValue(para, v);
            // System.out.println("not using BeanInfo: " + tagVarName + "," + n
            // + "," + v + ":" + outVal);
            scriptlet.writeln(tagVarName + "." + method + "(" + outVal + ");");
        }

        // htai contains all non supplied TagAttributes
        for (Iterator e = htai.values().iterator(); e.hasNext();) {
            TagAttributeInfo tai = (TagAttributeInfo) e.next();
            if (tai.isRequired())
                required(tai.getName() + "=...");
        }

        VariableInfo vis[] = null;
        TagExtraInfo tei = ti.getTagExtraInfo();
        if (tei != null) {
            TagData tagData = new TagData(tagDatas);
            vis = tei.getVariableInfo(tagData);
        }
        tagStack.push(new Object[]{tagClass, vis, tagVarName});

        String resVarName = "_jsp_tag_result$" + (resVarCount++);

        scriptlet.writeln("int " + resVarName + " = " + tagVarName + ".doStartTag();");

        // add variables defined at BEGIN
        defineVars(vis, VariableInfo.AT_BEGIN);

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.TryCatchFinally")) {
            scriptlet.writeln("try {");
            scriptlet.indent();
        }

        scriptlet.writeln("if (" + resVarName + " != javax.servlet.jsp.tagext.Tag.SKIP_BODY) { /** generated. */");
        scriptlet.indent();
        // add variables defined NESTED
        defineVars(vis, VariableInfo.NESTED);

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.BodyTag")) {
            scriptlet.writeln("if (" + resVarName + " != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {");
            scriptlet.indent();
            scriptlet.writeln("out = pageContext.pushBody();");
            // scriptlet.writeln("out = pageContext.pushBody();");
            scriptlet.writeln(tagVarName + ".setBodyContent((javax.servlet.jsp.tagext.BodyContent)out);");
            scriptlet.writeln(tagVarName + ".doInitBody();");
            scriptlet.unindent();
            scriptlet.writeln("}");
        }
        synchVars(vis, VariableInfo.NESTED);

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.IterationTag")) {
            scriptlet.writeln("for(;;) {");
            scriptlet.indent();
        }
    }

    /**
     * Define the variables if the scope matches.
     * 
     * @param vis
     *            the variable infos
     * @param scope
     *            the scope
     * @throws IOException
     */
    private void defineVars(VariableInfo vis[], int scope) throws IOException {
        if (vis == null)
            return;
        for (int i = 0; i < vis.length; ++i) {
            VariableInfo vi = vis[i];
            if (vi.getScope() != scope || !vi.getDeclare())
                continue;
            scriptlet.writeln(vi.getClassName() + " " + vi.getVarName() + " = null;");
            scriptlet.writeln("_jsp_mark_used(" + vi.getVarName() + ");");
        }
    }

    /**
     * Synch the variables if the scope matches.
     * 
     * @param vis
     *            the variable infos
     * @param scope
     *            the scope
     */
    private void synchVars(VariableInfo vis[], int scope) throws IOException {
        if (vis == null)
            return;
        for (int i = 0; i < vis.length; ++i) {
            VariableInfo vi = vis[i];
            int vscope = vi.getScope();
            if (vscope != scope && vscope != VariableInfo.AT_BEGIN)
                continue;
            scriptlet.writeln(vi.getVarName() + " = (" + vi.getClassName() + ")pageContext.findAttribute(\""
                    + vi.getVarName() + "\");");
        }
    }

    /**
     * @param clazz
     * @param property
     * @return
     */
    private Class findMethodParamClass(Class clazz, String property) throws Exception {
        java.lang.reflect.Method[] mts = clazz.getMethods();
        Class para = null;
        for (int i = 0; i < mts.length; ++i) {
            java.lang.reflect.Method m = mts[i];
            if (m.getName().equals(property)) {
                Class params[] = m.getParameterTypes();
                if (params.length != 1) {
                    // throw new Exception(property +
                    // " must have exact 1 parameter");
                    continue;
                }
                para = params[0];
                break;
            }
        }
        if (para == null) {
            throw new Exception("no setter found: " + clazz.getName() + "." + property + "( )");
        }
        return para;
    }

    /**
     * Convert a value to the specified type. But
     * 
     * @param para
     *            the class of the parameter
     * @param value
     *            the value either "something" or an request time expression varname
     * @return
     */
    private String mapValue(Class para, String value) {
        // is an scriptlet expression
        if (value.charAt(0) != '"')
            return value; // .trim();

        String pType = para.getName();
        String content = value.substring(1, value.length() - 1);

        // check for scriptlet expressions and add a fake
        if (content.indexOf("${") >= 0) {
            if (para.isPrimitive()) {
                if (pType.equals("boolean"))
                    return "false";
                if (pType.equals("int"))
                    return "0";
                return "(" + pType + ")" + 0;
            }
            if (pType.equals("java.lang.Character")) {
                return "new java.lang.Character('0')";
            }
            if (pType.equals("java.lang.String")) {
                return "\"\"";
            }
            return "null";
        }

        if (para.isPrimitive()) {
            if (pType.equals("char"))
                return "'" + content + "'";
            if (pType.equals("float") || pType.equals("int") || pType.equals("boolean"))
                return content;
            return "(" + pType + ")" + content;
        }
        if (pType.equals("java.lang.Character")) {
            return "new java.lang.Character('" + content + "')";
        }
        if (pType.equals("java.lang.String") || pType.equals("java.lang.Object")) {
            return value;
        }
        return null;
    }

    /**
     * @param clazz
     * @param property
     * @return
     */
    private boolean setByBeanInfo(Class clazz, String name, String property, String value, String param)
            throws Exception {
        String propName = property;
        property = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);

        java.beans.BeanInfo bi;
        try {
            bi = java.beans.Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            return false;
        }
        if (bi != null) {
            java.beans.PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            if (pds != null) {
                Class pe = null;
                Class type = null;
                for (int i = 0; i < pds.length; ++i) {
                    if (pds[i].getName().equals(propName)) {
                        pe = pds[i].getPropertyEditorClass();
                        type = pds[i].getPropertyType();
                        break;
                    }
                }
                if (pe != null) {
                    scriptlet.writeln("{");
                    scriptlet.writeln("  " + pe.getName() + " pe = new " + pe.getName() + "();");
                    if (param != null) {
                        scriptlet.writeln("  String _jsp_v = request.getParameter(\"" + param + "\");");
                        scriptlet.writeln("  pe.setAsText(_jsp_v);");
                    } else {
                        if (value.charAt(0) == '"')
                            scriptlet.writeln("  pe.setAsText(" + value + ");");
                        else
                            scriptlet.writeln("  pe.setAsText(\"" + value + "\");");
                    }

                    scriptlet.writeln("  " + name + "." + property + "((" + type.getName() + ")pe.getValue());");

                    scriptlet.writeln("}");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method loadClass.
     * 
     * @param cn
     * @return Class
     */
    Class loadClass(String cn) throws Exception {
        try {
            // if (classLoader == null)
            // classLoader = getClass().getClassLoader();
            return classLoader.loadClass(cn);
        } catch (Throwable e) {
            // e.printStackTrace();
            throw new Exception("cannot load class: " + cn);
        }
    }

    /**
   *
   */
    private void actionAfter(String tag, String subtag, Vector aName, Vector aValue) throws Exception {
        /*
         * { String ts = tag + ":" + subtag; if (tagList.size() == 0) {
         * unexpected("</" + ts); } String sts = (String)tagList.peek(); if
         * (!ts.equals(sts)) { expected("</" + sts); } }
         */

        // handle jsp tag
        if (tag.equals("jsp")) {
            if (subtag.equals("useBean")) {
                scriptlet.unindent();
                scriptlet.writeln("}");
                scriptlet.unindent();
                scriptlet.writeln("}");
                return;
            }
            if (subtag.equals("setProperty")) {
                return;
            }
            if (subtag.equals("getProperty")) {
                return;
            }
            if (subtag.equals("param")) {
                return;
            }
            if (subtag.equals("include")) {
                if (jspPage == null)
                    throw new Exception("jsp:include was already closed");
                if (flush)
                    scriptlet.writeln("out.flush();");
                addEncode = true;
                scriptlet.writeln("    pageContext.include(__encode(" + jspPage + ", new String[]{");
                for (Iterator i = requestAttributes.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry) i.next();
                    if (i.hasNext())
                        scriptlet.writeln("        " + e.getKey() + ", " + e.getValue() + ",");
                    else
                        scriptlet.writeln("        " + e.getKey() + ", " + e.getValue());
                }
                scriptlet.writeln("      }));");
                jspPage = null;
                return;
            }
            if (subtag.equals("forward")) {
                if (jspPage == null)
                    throw new Exception("jsp:forward was already closed");
                addEncode = true;
                scriptlet.writeln("  if (true) {");
                scriptlet.writeln("    out.clear();");
                scriptlet.writeln("    pageContext.forward(__encode(" + jspPage + ", new String[]{");
                for (Iterator i = requestAttributes.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e = (Map.Entry) i.next();
                    if (i.hasNext())
                        scriptlet.writeln("        " + e.getKey() + ", " + e.getValue() + ",");
                    else
                        scriptlet.writeln("        " + e.getKey() + ", " + e.getValue());
                }
                scriptlet.writeln("      }));");
                scriptlet.writeln("    return;");
                scriptlet.writeln("  }");
                jspPage = null;
                return;
            }
            throw new Exception("jsp:" + subtag + " is not yet supported");
        }

        Object o3[] = (Object[]) tagStack.pop();
        Class tagClass = (Class) o3[0];
        VariableInfo vis[] = (VariableInfo[]) o3[1];
        String tagVarName = (String) o3[2];

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.IterationTag")) {
            scriptlet.writeln("int _jsp_after$Body = " + tagVarName + ".doAfterBody();");
        }
        if (isInstance(tagClass, "javax.servlet.jsp.tagext.BodyTag")
                || isInstance(tagClass, "javax.servlet.jsp.tagext.IterationTag")) {
            synchVars(vis, VariableInfo.NESTED);
        }
        if (isInstance(tagClass, "javax.servlet.jsp.tagext.IterationTag")) {
            scriptlet.writeln("if (_jsp_after$Body != javax.servlet.jsp.tagext.IterationTag.EVAL_BODY_AGAIN)");
            scriptlet.writeln("  break;");
            scriptlet.unindent();
            scriptlet.writeln("}");
        }

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.BodyTag")) {
            int dollar = tagVarName.lastIndexOf('$');
            String resVarName = "_jsp_tag_result" + tagVarName.substring(dollar);
            scriptlet.writeln("if (" + resVarName + " != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {");
            scriptlet.indent();
            scriptlet.writeln("out = pageContext.popBody();");
            scriptlet.unindent();
            scriptlet.writeln("}");
        }

        scriptlet.unindent();
        scriptlet.writeln("}");

        scriptlet.writeln("if (" + tagVarName + ".doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE)");
        scriptlet.writeln("  break _jspPage;");

        if (isInstance(tagClass, "javax.servlet.jsp.tagext.TryCatchFinally")) {
            scriptlet.unindent();
            scriptlet.writeln("} catch (Throwable t) {");
            scriptlet.writeln("  " + tagVarName + ".doCatch(t);");
            scriptlet.writeln("} finally {");
            scriptlet.writeln("  " + tagVarName + ".doFinally();");
            scriptlet.writeln("}");
        }

        defineVars(vis, VariableInfo.AT_END);
        synchVars(vis, VariableInfo.AT_END);

    }

    /**
     * tests whether clazz is derived from parent.
     * 
     * @param class1
     *            the class which could be a subclass of parent
     * @param class2
     *            the parent class or interface
     * @return
     * @throws Exception
     */
    private boolean isInstance(Class clazz, String parent) throws Exception {
        Class pClazz = this.loadClass(parent);
        return pClazz.isAssignableFrom(clazz);
    }

    /**
     * Returns the current taglibs.
     * 
     * @return the current taglibs
     */
    public Map getTaglibInfo() {
        Map info = new HashMap();
        for (Iterator i = taglibs.keySet().iterator(); i.hasNext();) {
            String tliName = (String) i.next();
            TagLibraryInfo tli = (TagLibraryInfo) taglibs.get(tliName);
            Map lib = new HashMap();
            info.put(tliName, lib);
            lib.put("\1", tli.getShortName());
            lib.put("\2", tli.getInfoString());
            lib.put("\4", tli.getURI());

            String icoName = tli.getSmallIcon();
            if (icoName != null) {
                URL url = classLoader.getResource(icoName);
                if (url != null)
                    lib.put("\3", url.toExternalForm());
            }
            TagInfo[] tis = tli.getTags();
            for (int j = 0; j < tis.length; ++j) {
                TagInfo ti = tis[j];
                String tagName = ti.getTagName();
                Map tag = new HashMap();
                lib.put(tagName, tag);
                lib.put('\1' + tagName, ti.getDisplayName());
                lib.put('\2' + tagName, ti.getInfoString());

                icoName = ti.getSmallIcon();
                if (icoName != null) {
                    URL url = classLoader.getResource(icoName);
                    if (url != null)
                        lib.put('\3' + tagName, url.toExternalForm());
                }
                TagAttributeInfo tais[] = ti.getAttributes();
                for (int k = 0; k < tais.length; ++k) {
                    TagAttributeInfo tai = tais[k];
                    String taiName = tai.getName();
                    tag.put(taiName, taiName);
                    tag.put('\1' + taiName, tai.getShortName());
                    tag.put('\2' + taiName, tai.getInfoString());
                }
            }
        }
        return info;
    }

    /**
     * @param string
     * @param c
     * @param d
     * @return
     */
    public static String replaceAll(String string, char c, char d) {
        byte b[] = string.getBytes();
        for (int i = 0; i < b.length; ++i) {
            char ch = (char) (0xff & b[i]);
            if (ch == c) {
                b[i] = (byte) d;
            }
        }
        return new String(b);
    }

    /**
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * Create a JSR-045 SMAP.
     * 
     * @return SMAP as String.
     */
    public String createSmap() {
        StringBuffer sb = new StringBuffer();
        // header
        sb.append("SMAP\r\n");
        sb.append(className + ".java\r\n");
        sb.append("JSP\r\n*S JSP\r\n*F\r\n");

        SingleMap tm = new SingleMap();
        for (Iterator i = incFiles.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object val = incFiles.get(key);
            tm.put(val, key);
        }
        int n = 0;
        for (Iterator i = tm.values().iterator(); i.hasNext();) {
            String fn = (String) i.next();
            int slash = fn.lastIndexOf('/');
            if (slash < 0) {
                sb.append((++n) + " ");
            } else {
                sb.append("+ " + (++n) + " " + fn.substring(slash + 1) + "\r\n");
            }
            sb.append(fn + "\r\n");
        }
        sb.append("*L\r\n");
        declaration.writeSmap(declStart, sb, incFiles);
        scriptlet.writeSmap(codeStart, sb, incFiles);

        sb.append("*V\r\nde.bb.jsp.JspCC (c) Stefan 'Bebbo' Franke BebboSoft 2003\r\n*E\r\n");
        return sb.toString();
    }

    /**
     * return the output directory.
     * 
     * @return the output directory.
     */
    public String getOutDir() {
        return outDir;
    }

    /**
     * @return / public String getClassPath() { if (classLoader == null || !(classLoader instanceof ZipClassLoader))
     *         classLoader = new ZipClassLoader();
     * 
     *         String cp = ((ZipClassLoader)classLoader).getClassPath().trim(); if (cp.length() == 0) return "."; return
     *         cp; }
     * 
     *         /
     **/
    /**
     * Return an Iterator over all error objects. Each element is a String array with "line", "file", "msg"
     * 
     * @return an Iterator over all error objects.
     */
    public Iterator getErrors() {
        return errors.iterator();
    }

    /**
     * Returns the internal prefix for a taglibrary.
     * 
     * @param tlduri
     *            the uri of a taglibrary.
     * @return the prefix (taglibs short-name) or null, if no matching taglibrary was found. / public String
     *         tldUri2Namespace(String tlduri) { loadTaglibs(); String url = (String) uriMap.get(tlduri); if (url ==
     *         null) return null;
     * 
     *         XmlFile xf = (XmlFile) xmlMap.get(url); if (xf == null) return null; String prefix =
     *         xf.getContent("/taglib/short-name"); return prefix; } /
     **/
}
/*
 * $Log: JspCC.java,v $
 * Revision 1.99  2014/09/22 09:18:17  bebbo
 * @R < in error messages is now escaped &lt; to be visible in HTML
 *
 * Revision 1.98  2011/05/20 08:51:31  bebbo
 * @I re enabled loading of all TLDs
 * Revision 1.97 2008/01/17 17:35:00 bebbo
 * 
 * @B fixed taglib tld mapping and lookup
 * 
 * Revision 1.96 2006/05/09 08:52:39 bebbo
 * 
 * @R mangleDir --> convert2JavaIdent
 * 
 * Revision 1.95 2004/12/13 15:40:58 bebbo
 * 
 * @R added code to encode request parameters directly
 * 
 * Revision 1.94 2004/07/26 12:50:50 bebbo
 * 
 * @B TLD files outside WEB-INF but mapped using web.xml are used again
 * 
 * @B removed some useless casts from generated code
 * 
 * @B added code to touch unused variables in generated code
 * 
 * Revision 1.93 2004/07/16 12:34:03 bebbo
 * 
 * @B modified code to suppress more possible warnings from generated code.
 * 
 * Revision 1.92 2004/07/16 09:30:23 bebbo
 * 
 * @R added synthetic javadoc comments, in case of user who like to check all
 * warnings
 * 
 * @B in case of EL Expressions, those are fulle removed (no 100% solution yet)
 * 
 * Revision 1.91 2004/06/14 13:12:06 bebbo
 * 
 * @B removed final from session
 * 
 * Revision 1.90 2004/05/18 11:54:03 bebbo
 * 
 * @R changed error handling
 * 
 * @R also evaluating web-app/jsp-config to search taglib configs
 * 
 * Revision 1.89 2004/04/20 13:24:21 bebbo
 * 
 * @B fixed generated code for include
 * 
 * Revision 1.88 2004/04/16 13:45:09 bebbo
 * 
 * @R contains only the JspCC - runtime moved to de.bb.bejy.http.jsp
 * 
 * Revision 1.87 2004/04/07 16:35:05 bebbo
 * 
 * @I internal stuff
 * 
 * Revision 1.86 2004/03/24 09:39:10 bebbo
 * 
 * @R generating better code - some code moved to pageContext
 * 
 * Revision 1.85 2004/03/23 14:45:27 bebbo
 * 
 * @B fixed taglib in JAR files (due to changes in ZipClassLoader)
 * 
 * @B fixed setProperty - comparison was negated
 * 
 * Revision 1.84 2004/03/23 12:42:57 bebbo
 * 
 * @I using new ZipClassLoader
 * 
 * @R out.clear() is encapsulated in try block
 * 
 * Revision 1.83 2004/03/14 10:48:41 bebbo
 * 
 * @B getProperty also regards inherited methods, using getMethod instead of
 * getDeclaredMethod
 * 
 * Revision 1.82 2004/02/25 07:09:20 bebbo
 * 
 * @B eliminated copy buffer for expressions to get an enhanced line accuracy
 * 
 * Revision 1.81 2004/02/21 16:45:20 bebbo
 * 
 * @B fixed list of default import statements
 * 
 * Revision 1.80 2004/02/21 15:25:16 bebbo
 * 
 * @B redesigned handling of duplicate import statements
 * 
 * @B cope for expressions is also in case of EOF emitted
 * 
 * @B fixed line number mapping
 * 
 * Revision 1.79 2004/02/20 07:17:25 bebbo
 * 
 * @B fixed upper/lower case problems with taglib urls
 * 
 * @B added support for boolean property methods using isXXX()
 * 
 * Revision 1.78 2004/02/12 14:40:24 bebbo
 * 
 * @B fixed locating the taglib
 * 
 * Revision 1.77 2004/02/04 14:27:08 bebbo
 * 
 * @B added type conversion for parameters in setProperty
 * 
 * Revision 1.76 2004/01/09 19:39:16 bebbo
 * 
 * @R relaxed handling for useBean tags and inner classes
 * 
 * Revision 1.75 2004/01/03 18:53:06 bebbo
 * 
 * @R remove main()
 * 
 * @R replaceAll() is now public static
 * 
 * @@ createSmap() is now public
 * 
 * Revision 1.74 2003/12/15 20:58:20 bebbo
 * 
 * @N addeding enough code on errors in rtattribute values, to support code
 * completion
 * 
 * Revision 1.73 2003/12/15 07:42:22 bebbo
 * 
 * @R remove trim() from all attribute values
 * 
 * Revision 1.72 2003/12/15 07:39:55 bebbo
 * 
 * @B removed trim() from runtim attribute values
 * 
 * Revision 1.71 2003/12/07 18:12:05 bebbo
 * 
 * @B unkown tags are ignored now and are no longer an error
 * 
 * Revision 1.70 2003/11/27 21:10:02 bebbo
 * 
 * @B tagStack was not cleared in init(), so reused jspCC could create buggy
 * code.
 * 
 * Revision 1.69 2003/11/26 10:01:27 bebbo
 * 
 * @B fixed parsing of RtAttributeValues
 * 
 * @N allowing array in type of useBean
 * 
 * @N new NOLINK parameter
 * 
 * Revision 1.68 2003/11/16 09:30:29 bebbo
 * 
 * @B various changes
 * 
 * Revision 1.67 2003/10/23 20:35:34 bebbo
 * 
 * @R moved JspCC inner classes into separate files
 * 
 * @R jspCC is now reusable
 * 
 * @N added more caching to enhance reusability
 * 
 * Revision 1.66 2003/09/18 05:53:56 bebbo
 * 
 * @R removed the URI check for taglib uri=""
 * 
 * Revision 1.65 2003/09/03 15:01:39 bebbo
 * 
 * @R loading taglibs with introspection now
 * 
 * Revision 1.64 2003/08/31 20:45:10 bebbo
 * 
 * @B restoring thread context class loader
 * 
 * Revision 1.63 2003/08/13 22:46:23 bebbo
 * 
 * @B setting the threads context class loader to solve class loading problems
 * 
 * @N added a fake to handle JSP EL stuff
 * 
 * @B fixed quotes within single quotes
 * 
 * Revision 1.62 2003/08/04 10:41:34 bebbo
 * 
 * @R bug fixes in Java->Jsp line number mapping and source file lookup
 * 
 * Revision 1.61 2003/07/30 10:11:54 bebbo
 * 
 * @I capture more exceptions if classload failes
 * 
 * Revision 1.60 2003/07/14 12:41:52 bebbo
 * 
 * @R changed again the path and name generation of Java work files
 * 
 * Revision 1.59 2003/07/14 11:29:37 bebbo
 * 
 * @N JspServlet now adds SMAP information to generated class files
 * 
 * Revision 1.58 2003/07/14 08:13:58 bebbo
 * 
 * @B fixed class loader usage
 * 
 * Revision 1.57 2003/07/10 21:28:34 bebbo
 * 
 * @R modified generation of the class file name
 * 
 * @N added creation and dump of SMAP information
 * 
 * Revision 1.56 2003/07/07 10:46:11 bebbo
 * 
 * @B fixed directive attribute parser for emtpy attributes "" or ''
 * 
 * @N added validation for relative URLs
 * 
 * Revision 1.55 2003/06/23 09:39:26 bebbo
 * 
 * @R rewrote name mangling of path names
 * 
 * Revision 1.54 2003/06/18 08:36:12 bebbo
 * 
 * @B fixed a white space issue!
 * 
 * Revision 1.53 2003/06/17 10:21:39 bebbo
 * 
 * @I cleaned up imports
 * 
 * Revision 1.52 2003/05/22 09:13:05 bebbo
 * 
 * @B rewrote reading of tld files from META-INF
 * 
 * Revision 1.51 2003/05/15 10:31:08 bebbo
 * 
 * @B for dummy build with base class HttpServlet also a dummy function
 * _jsp_copyParameters(Object, Object) is generated.
 * 
 * Revision 1.50 2003/05/13 15:42:41 bebbo
 * 
 * @B JSP compliance fixes
 * 
 * Revision 1.49 2003/05/01 08:04:35 bebbo
 * 
 * @B now closing unclosed IntputStreams
 * 
 * Revision 1.48 2003/04/30 14:00:13 bebbo
 * 
 * @B fixed AT_END script variable definition
 * 
 * Revision 1.47 2003/04/29 09:33:13 bebbo
 * 
 * @N new parameter -p to specify a parent class
 * 
 * Revision 1.46 2003/04/23 21:49:44 bebbo
 * 
 * @R changed white space handling and quoting of paths
 * 
 * Revision 1.45 2003/04/17 11:37:09 bebbo
 * 
 * @R rewrote the getTaglibInfo function
 * 
 * @B added name mangling to replace non JavaKeywordCharacters
 * 
 * @B added support for JSP1.1 tld keywords
 * 
 * Revision 1.44 2003/04/14 15:55:31 bebbo
 * 
 * @I development version
 * 
 * Revision 1.43 2003/04/14 14:17:56 bebbo
 * 
 * @B working code again
 * 
 * @N TagExtraInfo is used
 * 
 * Revision 1.42 2003/04/12 15:59:15 bebbo
 * 
 * @B fixed JSP compiler error for empty but required parameters (value="")
 * 
 * Revision 1.41 2003/04/11 11:42:19 bebbo
 * 
 * @N getClassName() is public available
 * 
 * Revision 1.40 2003/04/09 13:43:17 bebbo
 * 
 * @B fix of the fix...
 * 
 * Revision 1.39 2003/04/09 13:10:47 bebbo
 * 
 * @B fixed JspCC isInstance was wrong
 * 
 * Revision 1.38 2003/04/09 11:22:09 bebbo
 * 
 * @R taglib objects are no longer instantiated
 * 
 * Revision 1.37 2003/04/08 10:04:18 bebbo
 * 
 * @B fixed searchLine in mapJspLine: included content is no longer regarded
 * 
 * Revision 1.36 2003/04/07 10:01:22 bebbo
 * 
 * @B fixed line breaks in <%@ page import="..." %>
 * 
 * Revision 1.35 2003/04/07 08:25:39 bebbo
 * 
 * @N better error line mapping with incomplete statements in scriptlets
 * 
 * Revision 1.34 2003/04/04 14:18:16 bebbo
 * 
 * @B fixed the "method has more than 1 parameter" bug
 * 
 * @R rewrote automatic parameter conversion
 * 
 * @N added automatic parameter conversion to taglib attributes
 * 
 * Revision 1.33 2003/04/04 10:30:19 bebbo
 * 
 * @B fixed "unexpected scriptlet end" on {
 * 
 * Revision 1.32 2003/04/02 18:57:16 bebbo
 * 
 * @R better detection of incomplete statements
 * 
 * Revision 1.31 2003/03/31 08:19:29 bebbo
 * 
 * @R changed the code generation to enhance code completion with eclipse plugin
 * 
 * Revision 1.30 2003/02/19 16:10:57 bebbo
 * 
 * @B setProperty: name is now required
 * 
 * Revision 1.29 2003/02/19 13:27:33 bebbo
 * 
 * @I changed visibility of methods/fields -> more performance
 * 
 * Revision 1.28 2003/01/22 18:08:59 bebbo
 * 
 * @N added support functions for de.bb.bejy.eclipse plugin. Streams can be used
 * for input and output.
 * 
 * Revision 1.27 2003/01/14 18:28:42 bebbo
 * 
 * @B discarded TABs in scriptlet... fixed!
 * 
 * Revision 1.26 2003/01/14 18:13:13 bebbo
 * 
 * @B some fixes
 * 
 * Revision 1.25 2003/01/14 15:50:56 bebbo
 * 
 * @N added retrival for included file names
 * 
 * Revision 1.24 2003/01/14 14:43:15 bebbo
 * 
 * @S ...
 * 
 * Revision 1.23 2003/01/14 14:42:00 bebbo
 * 
 * @B fixed handling of quoted endtag %\>
 * 
 * Revision 1.22 2003/01/05 15:45:46 bebbo
 * 
 * @R changed order of compilation: first .sjp is appended, then it is tried
 * without
 * 
 * Revision 1.21 2002/12/02 10:55:19 bebbo
 * 
 * @I internal changes
 * 
 * Revision 1.20 2002/11/19 12:56:48 bebbo
 * 
 * @I reorganized imports
 * 
 * Revision 1.19 2002/11/10 10:51:06 bebbo
 * 
 * @I changed internal classLoader handling
 * 
 * Revision 1.18 2002/11/06 09:43:20 bebbo
 * 
 * @I reorganized imports
 * 
 * @I removed unused variables
 * 
 * @N added LineNumber lookup Java->JSP
 * 
 * @B fixed some error messages
 * 
 * @B fixes in taglib handling
 * 
 * Revision 1.17 2002/10/24 11:27:27 bebbo
 * 
 * @R changed exception handling, so they are better visible now
 * 
 * Revision 1.16 2002/05/16 15:18:30 franke
 * 
 * @B error message printed ASCII code of last char instead of char itself.
 * 
 * @B in setProperty now a type cast is tried, if no constructor for param type
 * is found.
 * 
 * Revision 1.15 2002/04/08 13:24:49 franke
 * 
 * @O now suppressing multiple out.print(" ") statements
 * 
 * Revision 1.14 2002/03/30 15:44:26 franke
 * 
 * @I added support to use BeanInfo classes and PropertyEditors (setProperty)
 * 
 * @B tries to use pageContext variables too (getProperty)
 * 
 * Revision 1.13 2002/03/21 14:34:44 franke
 * 
 * @N added support for JSP files as Servlet
 * 
 * @N added support for classes loaded via ClassLoader
 * 
 * @N added support for lib/*.jar in web applications
 * 
 * Revision 1.12 2002/03/12 15:01:46 bebbo
 * 
 * @O removed some superfluos whit spaces in output
 * 
 * @O speedup in emit(), replaced String += by ByteArrayOutputStream
 * 
 * Revision 1.11 2002/03/10 20:03:24 bebbo
 * 
 * @I now supports new taglib tagnames (with '-')
 * 
 * @R restructured command line parameters
 * 
 * @B fixed handling of javax.servlet.jsp.tagext.BodyTag.classs
 * 
 * @B fixed handling of scriptlet expressions in taglib attributes
 * 
 * @B attributes accept now "..." and '...'
 * 
 * @N added support for BodyContent
 * 
 * Revision 1.10 2002/02/27 18:17:02 bebbo
 * 
 * @R no longer imports package de.bb.jsp.*;
 * 
 * @B <% is now accepted without some white space
 * 
 * Revision 1.9 2002/01/16 15:00:26 franke
 * 
 * @B fixed null ptr exception on use from cmd line
 * 
 * Revision 1.8 2002/01/16 14:51:29 franke
 * 
 * @C fixed usage message
 * 
 * Revision 1.7 2001/12/28 11:53:49 franke
 * 
 * @N added support for taglibs
 * 
 * Revision 1.6 2001/12/14 18:32:53 bebbo
 * 
 * @S last version did not compile
 * 
 * Revision 1.5 2001/12/14 18:29:18 bebbo
 * 
 * @B flush HTML buffer at end of page
 * 
 * Revision 1.4 2001/12/04 17:48:40 franke
 * 
 * @B fixed taglib unsupported message
 * 
 * Revision 1.3 2001/12/04 17:46:43 bebbo
 * 
 * @I removed dependency from de.bb.bex
 * 
 * Revision 1.2 2001/12/02 13:53:31 franke
 * 
 * @B fixed Exception in errorpages
 * 
 * Revision 1.1 2001/12/02 13:50:28 franke
 * 
 * @N first new version
 */
