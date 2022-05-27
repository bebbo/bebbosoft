/* 
 * Created on 02.10.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004 - 2011
 */
package de.bb.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import de.bb.util.SessionManager;

/**
 * This class is used as a singleton and manages the wiki functionality.
 * 
 * @author stefan franke
 */
public class WikiManager {

    /** out servlet context. */
    private ServletContext servletContext;

    /** the real path of the servlet's context. */
    private File baseDir;

    /** used to track pages under modification. */
    private SessionManager<String, HashSet<String>> locks;

    /** the environment - passed to the SCM implementations. */
    private String[] env;

    /** lookup table for the configured image endings. */
    private HashSet<String> images = new HashSet<String>();

    /** configurable values to enhance the generated HTML. */
    private String cssName;
    private String head, bodyStart, bodyEnd, javascript;

    /** not yet - used - planned for image previews. */
    private String thumbWidth = "80", thumbHeight = "60";

    /** the SCM implementation. */
    private Scm scm;

    /**
     * Read the configuration.
     * 
     * @param servletContext
     *            the servlet config
     */
    public WikiManager(ServletConfig servletConfig) {
        this.servletContext = servletConfig.getServletContext();
        this.baseDir = new File(servletContext.getRealPath(""));

        // read the init parameters
        String imageString = get(servletConfig, "images");
        if (imageString == null)
            imageString = ".jpg.png.bmp.gif";

        while (imageString.length() > 0) {
            int dot = imageString.indexOf('.', 1);
            if (dot < 0)
                dot = imageString.length();
            images.add(imageString.substring(0, dot).trim());
            imageString = imageString.substring(dot);
        }

        cssName = get(servletConfig, "cssURL");
        head = get(servletConfig, "head");
        javascript = get(servletConfig, "javascript");
        bodyStart = get(servletConfig, "body");
        if (bodyStart == null)
            bodyStart = "<body>";

        bodyEnd = get(servletConfig, "bodyEnd");
        if (bodyEnd == null)
            bodyEnd = "</body>";

        locks = new SessionManager<String, HashSet<String>>(1000L * 60 * 15);

        // init the SCM
        String scmName = get(servletConfig, "scm");
        String scmCmd = get(servletConfig, "scm-cmd");
        String scmUser = get(servletConfig, "scmUser");
        String scmPassword = get(servletConfig, "scmPassword");
        if (scmCmd == null)
            scmCmd = scmName;
        if ("svn".equals(scmName)) {
            scm = new SvnScm(servletContext, baseDir, env, scmCmd, scmUser, scmPassword);
        } else if ("cvs".equals(scmName)) {
            String cvsroot = get(servletConfig, "CVSROOT");
            if (cvsroot == null || cvsroot.length() == 0)
                cvsroot = get(servletConfig, "cvsroot");
            scm = new CvsScm(servletContext, baseDir, env, scmCmd, cvsroot);
        } else {
            scm = new Scm(servletContext, baseDir, env);
        }
    }

    /**
     * Read a servlet config paramter and strip it. Replace empty values with
     * null.
     * 
     * @param servletConfig
     *            the servlet config.
     * @param name
     *            the config parameter
     * @return a String containing the vlaue or null.
     */
    public static String get(ServletConfig servletConfig, String name) {
        String val = servletConfig.getInitParameter(name);
        if (val != null) {
            val = val.trim();
            if (val.length() == 0)
                val = null;
        }
        return val;
    }

    /**
     * Get a Reader for the wiki file. If the file does not yet exist, an empty
     * file is created. Also all directories are created. if the file exists an
     * update is performed.
     * 
     * @param wikiName
     *            the wiki file name
     * @param user
     *            the user creating the file
     * @param message
     *            a checkin message
     * @return a reader for the file.
     * @throws FileNotFoundException
     */
    public BufferedReader getRawReader(String wikiName, String user, String message) throws IOException {
        String fileName = servletContext.getRealPath(wikiName);
        File file = new File(fileName);

        if (!file.exists()) {
            // get the folders to create
            LinkedList<File> folders = new LinkedList<File>();
            for (File parent = file.getParentFile(); !parent.exists(); parent = parent.getParentFile()) {
                folders.addFirst(parent);
            }
            ArrayList<String> folderNames = new ArrayList<String>();
            for (File folder : folders) {
                folder.mkdir();
                folderNames.add(getRelativeFileName(folder));
            }
            file.createNewFile();

            scm.add(getRelativeFileName(file), folderNames, user, message);

        } else {
            scm.update(getRelativeFileName(file));
        }
        return new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
    }

    /**
     * Mark a wiki page locked for editing.
     * 
     * @param wikiName
     *            the wiki's name
     * @param remoteAddr
     *            the remote IP
     * @return the collection of other lockers.
     */
    public Collection<String> lock(String wikiName, String remoteAddr) {
        @SuppressWarnings("unchecked")
        HashSet<String> set = (HashSet<String>) locks.get(wikiName);
        if (set == null) {
            set = new HashSet<String>();
            locks.put(wikiName, set);
        } else {
            locks.touch(wikiName);
        }
        @SuppressWarnings("unchecked")
        Collection<String> r = (Collection<String>) set.clone();
        set.add(remoteAddr);
        return r;
    }

    /**
     * Mark a wiki page unlocked.
     * 
     * @param wikiName
     *            the wiki's name
     * @param remoteAddr
     *            the remote address
     */
    public void unlock(String wikiName, String remoteAddr) {
        HashSet<?> set = (HashSet<?>) locks.get(wikiName);
        if (set != null)
            set.remove(remoteAddr);
    }

    /**
     * // TODO
     * 
     * @throws IOException
     */
    synchronized void checkout() throws IOException {
        // scm.checkout();
    }

    /**
     * Write the changes to the file and invoke the scm.
     * 
     * @param wikiName
     *            the wiki's file name
     * @param content
     *            the content
     * @param user
     *            the wiki user name
     * @param message
     *            the checkin message (remote IP)
     * @throws IOException
     */
    public void writeChanges(String wikiName, String content, String user, String message) throws IOException {
        String fileName = servletContext.getRealPath(wikiName);
        File f = new File(fileName);

        if (!f.exists())
            f.getParentFile().mkdirs();

        // update the content.
        f.delete();
        RandomAccessFile fis = new RandomAccessFile(f, "rw");
        fis.write(content.getBytes("UTF-8"));
        fis.close();

        commit(f, user, message);
    }

    /**
     * Extract the relative file (to the base dir) name and convert it into the
     * os presentation.
     * 
     * @param f
     *            the file
     * @return the relativ filename
     */
    private String getRelativeFileName(File f) {
        String fileName = f.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
        fileName = fileName.replace('\\', File.separatorChar);
        return fileName;
    }

    public void add(File f, String wikiUser, String message) throws IOException {
        String fileName = getRelativeFileName(f);
        scm.add(fileName, null, wikiUser, message);
    }

    /**
     * Commit the file. If an add command is specified, add the file befor
     * committing.
     * 
     * @param f
     *            the file
     * @param user
     *            the user name
     * @param message
     *            the message
     * @throws IOException
     */
    void commit(File f, String user, String message) throws IOException {

        String fileName = getRelativeFileName(f);

        scm.commit(fileName, user, message);
    }

    /**
     * Remove from the file system.
     * 
     * @param f
     * @param user
     * @param message
     * @throws IOException
     */
    void delete(File f, String user, String message) throws IOException {

        String fileName = getRelativeFileName(f);
        scm.delete(fileName, user, message);
    }

    void update(File f, String user) throws IOException {
        String fileName = getRelativeFileName(f);
        scm.update(fileName);
    }

    /**
     * Read the file and convert the WIKI into HTML by loading the file for the
     * wikiName.
     * 
     * @param contextPath
     *            the context path (base path)
     * @param wikiName
     *            the name of the wiki page
     */
    public String printFile(String contextPath, String wikiName) {
        String fileName = servletContext.getRealPath(wikiName);
        byte b[] = {};
        try {
            RandomAccessFile fis = new RandomAccessFile(fileName, "r");
            b = new byte[(int) fis.length()];
            fis.read(b);
            fis.close();
        } catch (Exception ex) {
            // nada
        }

        String wikiText = "";
        try {
            wikiText = new String(b, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            // cannot happen
        }
        String html = printText(contextPath, wikiName, wikiText, false);

        return html;
    }

    /**
     * Read the file and convert the WIKI into HTML by using the WIKI in
     * wikiText.
     * 
     * @param contextPath
     *            the context path (base path)
     * @param wikiName
     *            the name of the wiki page
     * @param wikiText
     */
    public String printText(String path, String wikiName, String wikiText, boolean preview) {
        SimpleWiki simpleWiki = new SimpleWiki(this, cssName, path);
        StringBuilder sb = simpleWiki.convert(wikiText);

        appendRevision(wikiName, sb);

        StringBuffer all = new StringBuffer();
        if (!preview) {
            all.append("<!DOCTYPE HTML>\r\n");
            all.append("<html><head><title>\r\n");
            String title = simpleWiki.getTitle();
            if (title == null)
                title = "untitled wiki";
            all.append(title);
            all.append("</title>\r\n<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");

            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);

            String cssName = simpleWiki.getCssName();
            if (cssName != null) {
                if (cssName.startsWith("/"))
                    cssName = path + cssName;
                all.append("<link href='");
                all.append(cssName);
                all.append("' type='text/css' rel='stylesheet'>\r\n");
            }

            if (javascript != null) {
                String js = javascript;
                if (js.startsWith("/"))
                    js = path + js;
                all.append("<script type='text/javascript' src='");
                all.append(js);
                all.append("'></script>");
            }

            if (head != null)
                all.append(head);
            all.append("</head>");
            all.append(bodyStart);
        }
        all.append(sb);
        all.append(bodyEnd);
        if (!preview)
            all.append("</html>");
        return all.toString();
    }

    /**
     * Append the revision number if any.
     * 
     * @param wikiName
     *            the wiki file name.
     * @param sb
     *            the StringBuilder to append the version information
     */
    public void appendRevision(String wikiName, StringBuilder sb) {
        String absFileName = servletContext.getRealPath(wikiName);
        String fileName = getRelativeFileName(new File(absFileName));
        String version = scm.getVersion(fileName);
        if (version == null)
            return;

        sb.append("\r\n<hr><small>rev: ").append(version);
        sb.append("</small>\r\n");
    }

    /**
     * Get the last modification time for the wikie file.
     * 
     * @param wikiName
     *            the wiki file name
     * @return the modification time
     */
    public Long getFileTime(String wikiName) {
        String fileName = servletContext.getRealPath(wikiName);
        return new Long(new File(fileName).lastModified());
    }

    public String getCssName() {
        return cssName;
    }

    public HashSet<String> getImages() {
        return images;
    }

    public String getThumbWidth() {
        return thumbWidth;
    }

    public String getThumbHeight() {
        return thumbHeight;
    }
}