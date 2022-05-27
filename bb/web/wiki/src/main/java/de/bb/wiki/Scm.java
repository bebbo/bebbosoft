package de.bb.wiki;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.ServletContext;

/**
 * Base class and dummy implementation.
 * 
 * @author stefan franke
 * 
 */
public class Scm {
    /** needed to access the String[] class. */
    private final static String[] EMPTYSTRING = {};

    protected final ServletContext servletContext;
    protected final File baseDir;
    protected final String[] env;

    public Scm(ServletContext servletContext, File baseDir, String[] env) {
        this.servletContext = servletContext;
        this.baseDir = baseDir;
        this.env = env;
    }

    void add(String fileName, ArrayList<String> folderNames, String wikiUser, String message) throws IOException {
    }

    void update(String fileName) throws IOException {

    }

    void checkout() throws IOException {

    }

    void commit(String fileName, String wikiUser, String message) throws IOException {

    }

    void delete(String fileName, String user, String message) throws IOException {
        File file = new File(baseDir, fileName);
        if (file.isDirectory())
            delDir(file);
        file.delete();
    }

    /**
     * Recursively delete a folder.
     * 
     * @param dir
     */
    private void delDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    delDir(file);
                file.delete();
            }
        }
    }

    String getVersion(String fileName) {
        return null;
    }

    protected String executeCommand(ArrayList<String> cmdList) throws IOException {
        servletContext.log("execute: " + cmdList.toString() + " in " + baseDir);
        try {
            String[] cmd = cmdList.toArray(EMPTYSTRING);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            java.lang.Process proc = Runtime.getRuntime().exec(cmd, env, baseDir);
            InputStream is = proc.getInputStream();
            for (;;) {
                int ch = is.read();
                if (ch < 0)
                    break;
                bos.write(ch);
            }

            String result = bos.toString();
            if (result != null && result.length() > 0)
                servletContext.log("returned: " + result);
            return result;
        } catch (IOException ioe) {
            servletContext.log(ioe, ioe.getMessage());
            throw ioe;
        }
    }

}
