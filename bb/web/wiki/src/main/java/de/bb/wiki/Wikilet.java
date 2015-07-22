/* 
 * Created on 29.09.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.wiki;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.bb.util.LRUCache;
import de.bb.util.MimeFile;
import de.bb.util.MimeFile.Info;
import de.bb.util.Pair;

/**
 * @author sfranke
 */
public class Wikilet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String editURL;

    private String uploadURL;

    private static String userKey;

    private WikiManager wikiManager;

    private LRUCache<String, Pair<Long, String>> cache = new LRUCache<String, Pair<Long, String>>();

    /**
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#getServletInfo()
     */
    public String getServletInfo() {
        return "Wikilet - a wiki servlet (c) by Stefan Bebbo Franke";
    }

    /**
     * (non-Javadoc)
     * 
     * @throws ServletException
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init() throws ServletException {
        ServletConfig sc = getServletConfig();
        editURL = sc.getInitParameter("editURL");
        uploadURL = sc.getInitParameter("uploadURL");
        userKey = sc.getInitParameter("userKey");
        wikiManager = new WikiManager(sc);
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        HttpSession session = request.getSession(true);
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");

        String changeDebug = request.getParameter("debug");
        if ("true".equals(changeDebug)) {
            session.setAttribute("DEBUG", "true");
        } else if ("false".equals(changeDebug)) {
            session.removeAttribute("DEBUG");
        }

        final boolean debugOn = "true".equals(session.getAttribute("DEBUG"));

        String action = request.getParameter("what");
        final String url = request.getRequestURI();
        String pathInfo = request.getPathInfo();
        String contextPath = request.getContextPath();

        String wikiName = pathInfo;
        if (wikiName == null) {
            wikiName = request.getServletPath();
        } else {
            if (action == null)
                action = "edit";
        }

        String message = request.getRemoteHost();
        String user = request.getRemoteUser();
        if (user == null)
            user = "<unknown>";

        if (debugOn) {
            String fileName = request.getRealPath(wikiName);
            System.out.println(fileName);
        }
        boolean canEdit = userKey == null;
        if (!canEdit && userKey != null) {
            Object o = session.getAttribute(userKey);
            canEdit = o != null;
            if (canEdit) {
                user = (String) o;
                message = user + "@" + message;
            }
        }
        if (!canEdit) {
            canEdit = "dirtdonecheap".equals(request.getParameter("dirtydeeds"));
            if (canEdit)
                session.setAttribute(userKey, "AC/DC rocksors");
        }

        if (!canEdit) {
            action = null;
        } else {

            wikiManager.checkout();

            // check content type for uploads
            String cType = request.getContentType();
            if (cType != null && cType.length() >= 20
                    && cType.substring(0, 20).equalsIgnoreCase("multipart/form-data;")) {

                int boundaryOff = cType.toLowerCase().indexOf("boundary=") + 9;
                String boundary = cType.substring(boundaryOff).trim();
                int semi = boundary.indexOf(';');
                if (semi > 0)
                    boundary = boundary.substring(0, semi);

                if (debugOn) {
                    System.out.println("uploading: " + boundary);
                }

                File temp = File.createTempFile("wiki_", "temp");
                FileOutputStream fos = new FileOutputStream(temp);
                copy(request.getInputStream(), fos, request.getContentLength());
                fos.close();

                if (debugOn) {
                    System.out.println("copied " + request.getContentLength() + " to temp file: " + temp);
                }
                FileInputStream fis = new FileInputStream(temp);
                ArrayList<MimeFile.Info> r = MimeFile.parseMime(fis, boundary);
                fis.close();

                if (debugOn) {
                    System.out.println(r);
                }
                String fileName = null;
                int fStart = 0, fLen = 0;
                for (Iterator<Info> i = r.iterator(); i.hasNext();) {
                    Info info = i.next();
                    for (Iterator<Pair<String, String>> j = info.header.iterator(); j.hasNext();) {
                        Pair<String, String> pair = j.next();
                        if ("Content-Disposition".equalsIgnoreCase(pair.getFirst())) {
                            for (StringTokenizer st = new StringTokenizer(pair.getSecond(), "; "); st.hasMoreTokens();) {
                                String token = st.nextToken();
                                if (debugOn) {
                                    System.out.print(token);
                                }
                                int eq = token.indexOf('=');
                                if (eq > 0) {
                                    String key = token.substring(0, eq);
                                    token = token.substring(eq + 1);
                                    if (token.charAt(0) == '"') {
                                        token = token.substring(1, token.length() - 1);
                                    }
                                    if ("filename".equals(key)) {
                                        fileName = token;
                                        fStart = info.bBegin;
                                        fLen = info.end - info.bBegin - 2;
                                    } else if (info.end > info.bBegin) {
                                        if ("name".equals(key) && "what".equals(token)) {
                                            fis = new FileInputStream(temp);
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            fis.skip(info.bBegin);
                                            copy(fis, bos, info.end - info.bBegin);
                                            fis.close();
                                            action = bos.toString().trim();
                                            if (debugOn) {
                                                System.out.println("action:=" + action);
                                            }
                                        } else if ("name".equals(key) && "delme".equals(token)) {
                                            fis = new FileInputStream(temp);
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            fis.skip(info.bBegin);
                                            copy(fis, bos, info.end - info.bBegin);
                                            fis.close();
                                            String s = bos.toString().trim();
                                            if (s.length() > 0)
                                                fileName = s;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                String rp = request.getRealPath(wikiName);
                File dir = new File(rp);
                if (!dir.isDirectory())
                    dir = dir.getParentFile();

                if (debugOn) {
                    System.out.println("<" + action + "> " + fileName);
                }
                if ("upload".equals(action) && fileName != null && fileName.length() > 0) {
                    File outFile = new File(dir, fileName);
                    fis = new FileInputStream(temp);
                    fos = new FileOutputStream(outFile);
                    fis.skip(fStart);
                    copy(fis, fos, fLen);
                    fis.close();
                    fos.close();

                    try {
                        wikiManager.add(outFile, user, message);
                    } catch (Exception ioe) {
                    }
                } else if ("X".equals(action) && fileName != null) {
                    File delFile = new File(dir, fileName);
                    try {
                        wikiManager.delete(delFile, user, message);
                    } catch (Exception ioe) {
                    }
                }

                temp.delete();

                if (!"cancel".equals(action)) {
                    request.setAttribute("wiki_upload", wikiName);
                    RequestDispatcher rd = request.getRequestDispatcher(uploadURL);
                    rd.forward(request, response);
                    return;
                }
            }
        }

        if ("edit".equals(action) || "preview".equals(action)) {

            cache.remove(wikiName);

            request.setAttribute("wiki_url", url);

            Reader r;
            if ("preview".equals(action)) {
                String wikiText = request.getParameter("content");
                r = new BufferedReader(new StringReader(wikiText));
                request.setAttribute("wiki_content", r);
                String html = wikiManager.printText(contextPath, wikiName, wikiText, true);
                request.setAttribute("wiki_html", html);
                request.setAttribute("wikiManager", wikiManager);
            } else {
                request.removeAttribute("wiki_html");
                r = wikiManager.getRawReader(wikiName, user, message);
                request.setAttribute("wiki_content", r);
            }

            Collection<String> c = wikiManager.lock(wikiName, request.getRemoteAddr());
            request.setAttribute("wiki_locks", c);

            RequestDispatcher rd = request.getRequestDispatcher(editURL);
            rd.forward(request, response);
            r.close();
            return;
        }

        if ("upload".equals(action)) {
            request.setAttribute("wiki_upload", wikiName);
            RequestDispatcher rd = request.getRequestDispatcher(uploadURL);
            rd.forward(request, response);
            return;
        }

        if ("save".equals(action)) {
            String content = request.getParameter("content");
            wikiManager.writeChanges(wikiName, content, user, message);
            action = "cancel";
        }

        if ("cancel".equals(action)) {
            wikiManager.unlock(wikiName, request.getRemoteAddr());
        }

        if (pathInfo != null) {
            String redir = request.getRequestURI();
            redir = redir.substring(request.getContextPath().length());
            redir = url.substring(0, url.length() - redir.length() - 1);
            response.sendRedirect(redir + pathInfo + "");
            return;
        }

        PrintWriter w = response.getWriter();
        Long date = wikiManager.getFileTime(wikiName);
        Pair<Long, String> cached = cache.get(wikiName);
        String content;
        if (cached != null && date.equals(cached.getFirst())) {
            content = cached.getSecond();
        } else {
            content = wikiManager.printFile(contextPath, wikiName);
            try {
                cache.put(wikiName, new Pair<Long, String>(date, content));
            } catch (Throwable ex) {
                // ignore
            }
        }

        boolean addStats = (request.getParameter("stats") != null);

        if (addStats || canEdit) {
            int bend = content.lastIndexOf("</body");
            w.write(content.substring(0, bend));
            if (canEdit) {
                w.write("<br><small><a href='?what=edit'>edit</a> <a href='?what=upload'>upload</a></small>");
            }
            if (addStats) {
                w.write("<br><small><pre>" + cache + "</pre></small>");
            }
            w.write(content.substring(bend));
        } else {
            w.write(content);
        }
    }

    private static void copy(InputStream is, OutputStream os, int length) throws IOException {
        byte b[] = new byte[8192];
        while (length > 0) {
            int b0 = is.read();
            if (b0 < 0)
                throw new IOException("EOS");
            os.write(b0);
            --length;
            if (length == 0)
                return;
            int read = length > b.length ? b.length : length;
            read = is.read(b, 0, read);
            if (read > 0) {
                os.write(b, 0, read);
                length -= read;
            }
        }
    }

    public static boolean canEdit(HttpSession session) {
        if (userKey == null)
            return true;
        Object o = session.getAttribute(userKey);
        return o != null;
    }
}