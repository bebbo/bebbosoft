package de.bb.wiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;

public class SvnScm extends Scm {

    private String cmd;
    private String username;
    private String password;

    public SvnScm(ServletContext servletContext, File baseDir, String env[], String scmCmd, String user, String pass) {
        super(servletContext, baseDir, env);
        cmd = scmCmd == null ? "svn" : scmCmd;
        username = user;
        password = pass;
    }

    public void add(String fileName, ArrayList<String> folderNames, String wikiUser, String message) throws IOException {

        // add it
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("add");
        if (folderNames != null)
            cmdList.addAll(folderNames);
        cmdList.add(fileName);
        executeCommand(cmdList);

        // commit the changes
        commit(null, wikiUser, message);
    }

    public void commit(String fileName, String wikiUser, String message) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        if (username != null && password != null) {
            cmdList.add("--username");
            cmdList.add(username);
            cmdList.add("--password");
            cmdList.add(password);
        }
        cmdList.add("-m");
        cmdList.add(wikiUser + ": " + message);
        cmdList.add("commit");
        if (fileName != null)
            cmdList.add(fileName);

        executeCommand(cmdList);
    }

    public void update(String fileName) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("update");
        cmdList.add(fileName);

        executeCommand(cmdList);
    }

    public void checkout() {
        // TODO
    }

    public void delete(String fileName, String wikiUser, String message) throws IOException {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("delete");
        cmdList.add(fileName);

        executeCommand(cmdList);

        commit(fileName, wikiUser, message);
    }

    public String getVersion(String fileName) {
        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(cmd);
        cmdList.add("info");
        cmdList.add(fileName);

        try {
            String reply = executeCommand(cmdList);
            int idx = reply.indexOf("Rev:");
            if (idx > 0) {
                idx += 4;
                int end = reply.indexOf('\n', idx);
                if (end < 0)
                    end = reply.length();
                String v = reply.substring(idx, end).trim();
                return v;
            }
        } catch (IOException e) {
            return "N/A";
        }
        return null;
    }

}
