package de.bb.wiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;

public class CvsScm extends Scm {

    private String cmd;
    private String cvsRoot;

    public CvsScm(ServletContext servletContext, File baseDir, String[] env, String scmCmd, String root) {
        super(servletContext, baseDir, env);
        this.cmd = scmCmd == null ? "cvs" : scmCmd;
        this.cvsRoot = root;
    }

    @Override
    void add(String fileName, ArrayList<String> folderNames, String wikiUser, String message) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("-d");
        cmdList.add(cvsRoot);
        cmdList.add("add");
        cmdList.add("-m");
        cmdList.add(message);
        cmdList.addAll(folderNames);
        cmdList.add(fileName);
        executeCommand(cmdList);
    }

    @Override
    void update(String fileName) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("-d");
        cmdList.add(cvsRoot);
        cmdList.add("update");
        cmdList.add(fileName);
        executeCommand(cmdList);
    }

    @Override
    void checkout() throws IOException {
        // nothing yet
    }

    @Override
    void commit(String fileName, String wikiUser, String message) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("-d");
        cmdList.add(cvsRoot);
        cmdList.add("commit");
        cmdList.add("-m");
        cmdList.add(message);
        cmdList.add(fileName);
        executeCommand(cmdList);
    }

    @Override
    void delete(String fileName, String user, String message) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("-d");
        cmdList.add(cvsRoot);
        cmdList.add("remove");
        cmdList.add("-m");
        cmdList.add(message);
        cmdList.add(fileName);
        executeCommand(cmdList);
    }

    @Override
    String getVersion(String fileName) {
        try {
            ArrayList<String> cmdList = new ArrayList<String>();
            cmdList.add(cmd);
            cmdList.add("-d");
            cmdList.add(cvsRoot);
            cmdList.add("status");
            cmdList.add(fileName);
            String result = executeCommand(cmdList);
            if (result != null) {
                int wr = result.indexOf("Working revision:");
                if (wr > 0) {
                    wr = result.indexOf(':', wr) + 1;
                    result = result.substring(wr).trim();
                    int space = result.indexOf(' ');
                    int tab = result.indexOf('\t');
                    if ((tab > 0 && tab < space) || space < 0)
                        space = tab;
                    if (space > 0) {
                        return result.substring(0, space);
                    }
                }
            }
        } catch (IOException ioe) {

        }
        return null;
    }

}
