/*
 * $Source: /export/CVS/java/de/bb/bejy/http/src/main/java/de/bb/bejy/http/jsp/PageContextImpl.java,v $
 * $Revision: 1.7 $
 * $Date: 2014/06/23 15:38:46 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * written by Stefan Bebbo Franke
 * (c) 1999-2000 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * a PageContextImpl
 *
 */

package de.bb.bejy.http.jsp;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

import de.bb.bejy.http.IterEnum;
import de.bb.bejy.http.WebAppContext;

public class PageContextImpl extends PageContext {
    private static final boolean debug = false;

    ServletConfig config;

    ServletContext application;

    HttpServletRequest request;

    HttpServletResponse response;

    String errorPage;

    HttpSession session;

    int bufferSize;

    boolean autoFlush;

    Exception exc = null;

    JspWriter out = null;

    private HashMap attrs = new HashMap();

    private Stack stack = new Stack();

    public void initialize(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse,
            String string, boolean needSession, int bufSize, boolean autoFl) throws IOException,
            IllegalArgumentException, IllegalStateException {
        config = servlet.getServletConfig();
        application = config.getServletContext();
        request = (HttpServletRequest) servletRequest;
        response = (HttpServletResponse) servletResponse;
        errorPage = string;
        bufferSize = bufSize > 0 ? bufSize : 0;
        autoFlush = autoFl;

        if (needSession) {
            session = request.getSession();
            if (debug)
                System.out.println("t using session: " + session.toString());
        } else
            session = null;

        out = new JspWriterImpl(response, bufferSize, autoFlush);

        setAttribute(APPLICATION, application);
        setAttribute(CONFIG, config);
        setAttribute(OUT, out);
        setAttribute(PAGE, servlet);
        setAttribute(PAGECONTEXT, this);
        setAttribute(REQUEST, servletRequest);
        setAttribute(RESPONSE, servletResponse);
        if (session != null)
            setAttribute(SESSION, session);
    }

    public void release() {
        try {
            out.flush();
        } catch (IOException e) {
        }
    }

    public void setAttribute(String string, Object object) throws NullPointerException {
        attrs.put(string, object);
    }

    public void setAttribute(String string, Object object, int i) throws IllegalArgumentException, NullPointerException {
        switch (i) {
        case PAGE_SCOPE:
            setAttribute(string, object);
            return;
        case REQUEST_SCOPE:
            request.setAttribute(string, object);
            return;
        case SESSION_SCOPE:
            session.setAttribute(string, object);
            return;
        case APPLICATION_SCOPE:
            application.setAttribute(string, object);
            return;
        default:
            throw new IllegalArgumentException("scope=" + i);
        }
    }

    public Object getAttribute(String string) throws IllegalArgumentException, NullPointerException {
        return attrs.get(string);
    }

    public Object getAttribute(String string, int i) throws IllegalArgumentException, NullPointerException {
        switch (i) {
        case PAGE_SCOPE:
            return getAttribute(string);
        case REQUEST_SCOPE:
            return request.getAttribute(string);
        case SESSION_SCOPE:
            return session.getAttribute(string);
        case APPLICATION_SCOPE:
            return application.getAttribute(string);
        default:
            throw new IllegalArgumentException("scope=" + i);
        }
    }

    public Object findAttribute(String string) {
        Object o = getAttribute(string);
        if (o != null)
            return o;
        o = request.getAttribute(string);
        if (o != null)
            return o;
        if (session != null) {
            o = session.getAttribute(string);
            if (o != null)
                return o;
        }
        o = application.getAttribute(string);
        return o;
    }

    public void removeAttribute(String string) {
        attrs.remove(string);
    }

    public void removeAttribute(String string, int i) {
        switch (i) {
        case PAGE_SCOPE:
            removeAttribute(string);
            return;
        case REQUEST_SCOPE:
            request.removeAttribute(string);
            return;
        case SESSION_SCOPE:
            session.removeAttribute(string);
            return;
        case APPLICATION_SCOPE:
            application.removeAttribute(string);
            return;
        default:
            throw new IllegalArgumentException("scope=" + i);
        }
    }

    public int getAttributesScope(String string) {
        if (getAttribute(string) != null)
            return PAGE_SCOPE;
        if (request.getAttribute(string) != null)
            return REQUEST_SCOPE;
        if (session != null) {
            if (session.getAttribute(string) != null)
                return SESSION_SCOPE;
        }
        if (application.getAttribute(string) != null)
            return APPLICATION_SCOPE;
        return 0;
    }

    public Enumeration getAttributeNamesInScope(int i) throws IllegalArgumentException {
        switch (i) {
        case PAGE_SCOPE:
            return new IterEnum(attrs.keySet().iterator());
        case REQUEST_SCOPE:
            return request.getAttributeNames();
        case SESSION_SCOPE:
            return session.getAttributeNames();
        case APPLICATION_SCOPE:
            return application.getAttributeNames();
        default:
            throw new IllegalArgumentException("scope=" + i);
        }
    }

    public JspWriter getOut() {
        return out;
    }

    public HttpSession getSession() {
        return session;
    }

    public Object getPage() {
        return null;
    }

    public ServletRequest getRequest() {
        return request;
    }

    public ServletResponse getResponse() {
        return response;
    }

    public Exception getException() {
        return exc;
    }

    public ServletConfig getServletConfig() {
        return config;
    }

    public ServletContext getServletContext() {
        return application;
    }

    public void forward(String string) throws IOException, ServletException, IllegalArgumentException,
            IllegalStateException, SecurityException {
        out.clear();
        response = new SRW(response, out);
        request.getRequestDispatcher(string).forward(request, response);
    }

    public void include(String uri) throws IOException, ServletException, IllegalArgumentException, SecurityException {
        response = new SRW(response, out);
        request.getRequestDispatcher(uri).include(request, response);
    }

    public void handlePageException(Exception e) throws IOException, ServletException {
        try {
            out.clear();
        } catch (Exception ex) {
        }

        if (errorPage != null) {
            request.setAttribute("javax.servlet.jsp.jspException", e);
            RequestDispatcher rd = request.getRequestDispatcher(errorPage);
            if (rd != null) {
                rd.forward(request, response);
                return;
            }
        }
        if (application instanceof WebAppContext)
            errorPage = ((WebAppContext) application).getErrorPage(e);
        if (errorPage != null) {
            request.setAttribute("javax.servlet.jsp.jspException", e);
            RequestDispatcher rd = request.getRequestDispatcher(errorPage);
            if (rd != null) {
                rd.forward(request, response);
                return;
            }
        }

        response.setStatus(500);

        out.write("<html><head><title>JSP runtime Exception</title></head><body>");
        for (StringTokenizer st = new StringTokenizer(e.toString(), "\n"); st.hasMoreTokens();) {
            out.write(st.nextToken());
            out.write("<br>\n");
        }
        out.println("<hr><pre>");
        e.printStackTrace(new PrintWriter(out));
        out.write("</pre></body></html>");
        //    out.flush();
        /*
           if (e instanceof IOException)
             throw (IOException)e;
           if (e instanceof ServletException)
             throw (ServletException) e;
           ServletException e2 = new ServletException(e);
           e2.setStackTrace(e.getStackTrace());
           throw e2;
         */
    }

    public void handlePageException(java.lang.Throwable t) throws IOException, ServletException {
        t.printStackTrace();
        handlePageException(new Exception(t.getMessage()));
    }

    public JspWriter popBody() {
        return out = (JspWriter) stack.pop();
    }

    public javax.servlet.jsp.tagext.BodyContent pushBody() {
        stack.push(out);
        /*    
            try {
              out.flush();
            } catch (Exception e)
            {}
        */
        javax.servlet.jsp.tagext.BodyContent bc = new BC(out);
        out = bc;
        return bc;
    }

    private static class BC extends javax.servlet.jsp.tagext.BodyContent {
        private boolean closed = false;

        private CharArrayWriter caw = new CharArrayWriter();

        BC(JspWriter jwi) {
            super(jwi);
        }

        public void writeOut(java.io.Writer out) throws java.io.IOException {
            out.write(caw.toCharArray());
            caw.reset();
        }

        public java.lang.String getString() {
            return caw.toString();
        }

        public java.io.Reader getReader() {
            Reader r = new CharArrayReader(caw.toCharArray());
            caw.reset();
            return r;
        }

        public void write(char[] ch, int off, int len) throws IOException {
            if (closed)
                throw new IOException("stream IS closed");
            caw.write(ch, off, len);
        }

        public void clear() throws IllegalStateException {
            caw.reset();
        }

        public void clearBuffer() {
            caw.reset();
        }

        public void close() throws IOException {
            flush();
            closed = true;
        }

        public int getRemaining() {
            return 256;
        }

        //        Write a line separator.
        public void newLine() throws IOException {
            print("\r\n");
        }

        //        Print a boolean value.
        public void print(boolean b) throws IOException {
            print("" + b);
        }

        //        Print a character.
        public void print(char c) throws IOException {
            print("" + c);
        }

        //        Print an array of characters.
        public void print(char[] s) throws IOException {
            write(s, 0, s.length);
        }

        //        Print a double-precision floating-point number.
        public void print(double d) throws IOException {
            print(Double.toString(d));
        }

        //        Print a floating-point number.
        public void print(float f) throws IOException {
            print(Float.toString(f));
        }

        //        Print an integer.
        public void print(int i) throws IOException {
            print(Integer.toString(i));
        }

        //        Print a long integer.
        public void print(long l) throws IOException {
            print(Long.toString(l));
        }

        //        Print an object.
        public void print(java.lang.Object obj) throws IOException {
            if (obj == null)
                print("null");
            else
                print(obj.toString());
        }

        //        Print a string.
        public void print(java.lang.String s) throws IOException {
            if (s == null)
                print("null");
            else
                write(s);
        }

        //        Terminate the current line by writing the line separator string.
        public void println() throws IOException {
            print("\r\n");
        }

        //        Print a boolean value and then terminate the line.
        public void println(boolean x) throws IOException {
            print(x);
            println();
        }

        //        Print a character and then terminate the line.
        public void println(char x) throws IOException {
            print(x);
            println();
        }

        //        Print an array of characters and then terminate the line.
        public void println(char[] x) throws IOException {
            print(x);
            println();
        }

        //        Print a double-precision floating-point number and then terminate the line.
        public void println(double x) throws IOException {
            print(x);
            println();
        }

        //        Print a floating-point number and then terminate the line.
        public void println(float x) throws IOException {
            print(x);
            println();
        }

        //        Print an integer and then terminate the line.
        public void println(int x) throws IOException {
            print(x);
            println();
        }

        //        Print a long integer and then terminate the line.
        public void println(long x) throws IOException {
            print(x);
            println();
        }

        //        Print an Object and then terminate the line.
        public void println(java.lang.Object x) throws IOException {
            print(x);
            println();
        }

        //        Print a String and then terminate the line.
        public void println(java.lang.String x) throws IOException {
            print(x);
            println();
        }
    }

    static class SRW extends HttpServletResponseWrapper {
        private PrintWriter out, pw;

        private javax.servlet.ServletOutputStream sos;

        /**
         * Wrap the response to return the matching out.
         * 
         * @param response
         *            the wrapped response.
         * @param out
         *            out object use in getWriter()
         */
        public SRW(HttpServletResponse response, JspWriter out) {
            super(response);
            this.out = new PrintWriter(out);
        }

        /* (non-Javadoc)
         * @see javax.servlet.ServletResponse#getWriter()
         */
        public PrintWriter getWriter() throws IOException {
            if (sos != null)
                throw new IllegalStateException();
            return pw = out;
        }

        /**
         * (non-Javadoc)
         * 
         * @see javax.servlet.ServletResponse#getOutputStream()
         */
        public javax.servlet.ServletOutputStream getOutputStream() throws IOException {
            if (pw != null)
                throw new IllegalStateException();

            sos = new WO(out);

            return sos;
        }
    }

    static class WO extends ServletOutputStream {

        private PrintWriter out;

        /**
         * @param out
         */
        WO(PrintWriter out) {
            this.out = out;
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) {
            out.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener arg0) {
            // TODO Auto-generated method stub
        }

    }

    public void include(String relUri, boolean flush) throws ServletException, IOException {
        if (flush)
            out.flush();
        include(relUri);
    }

    public ExpressionEvaluator getExpressionEvaluator() {
        // TODO Auto-generated method stub
        return null;
    }

    public VariableResolver getVariableResolver() {
        // TODO Auto-generated method stub
        return null;
    }
}

/*
 * $Log: PageContextImpl.java,v $
 * Revision 1.7  2014/06/23 15:38:46  bebbo
 * @N implemented form authentication
 * @R reworked authentication handling to support roles
 *
 * Revision 1.6  2010/08/29 05:08:43  bebbo
 * @B: forwarding also preserves isSecure
 * @O: using unsynchronized classes where not needed to gain speed
 *
 * Revision 1.5  2006/05/09 12:13:25  bebbo
 * @R changes to comply to servlet2_4
 *
 * Revision 1.4  2006/03/17 20:06:43  bebbo
 * @B fixed possible CCE
 *
 * Revision 1.3  2006/03/17 11:31:05  bebbo
 * @I cleanup imports
 *
 * Revision 1.2  2004/12/13 15:39:29  bebbo
 * @B fixed error handling
 *
 * Revision 1.1  2004/04/16 13:46:09  bebbo
 * @R runtime moved to de.bb.jsp
 *
 * Revision 1.13  2004/04/07 16:35:21  bebbo
 * @I JspWriter stuff
 *
 * Revision 1.12  2004/03/24 09:40:52  bebbo
 * @B fixed handling of char encoding
 * @B pageContext now flushes out in release()
 *
 * Revision 1.11  2004/03/23 19:03:34  bebbo
 * @B added out.flush() to release()
 *
 * Revision 1.10  2004/03/23 12:41:47  bebbo
 * @B popBody() does NOT automatically emit content
 *
 * Revision 1.9  2004/03/11 18:14:36  bebbo
 * @B popBody no longer writes the content. This must be done in the Tag
 *
 * Revision 1.8  2004/01/09 19:38:16  bebbo
 * @R Java stack trace now uses raw formatting
 *
 * Revision 1.7  2003/04/04 10:32:28  bebbo
 * @I cleanup
 *
 * Revision 1.6  2003/03/31 11:37:52  bebbo
 * @X experimental change of PageContext
 *
 * Revision 1.5  2002/10/24 11:27:27  bebbo
 * @R changed exception handling, so they are better visible now
 *
 * Revision 1.4  2002/03/10 20:05:37  bebbo
 * @N supporting BodyContent in taglibs
 *
 * Revision 1.3  2001/12/04 17:47:17  franke
 * @M merge
 *
 * Revision 1.2  2001/04/06 05:52:57  bebbo
 * @D DEBUG on
 *
 * Revision 1.1  2001/03/29 19:55:33  bebbo
 * @N moved to this location
 *
 */
