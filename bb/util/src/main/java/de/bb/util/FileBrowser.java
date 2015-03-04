/******************************************************************************
 * $Source: /export/CVS/java/de/bb/util/src/main/java/de/bb/util/FileBrowser.java,v $
 * $Revision: 1.11 $
 * $Date: 2013/11/23 10:40:39 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Recurse over directories using a mask and invoke callbacks for files and directories.
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/

package de.bb.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * This is a base class to iterate over files and directories, which can be configured with a pattern.
 * 
 * @author bebbo
 * @version $Revision: 1.11 $
 */
public abstract class FileBrowser {
    /** bytes of a defined mask. */
    private String baseDir;
    private boolean recurse;
    private ArrayList<Pattern> incRules = new ArrayList<Pattern>();
    private ArrayList<Pattern> excRules = new ArrayList<Pattern>();

    /**
     * ct.
     * 
     */
    public FileBrowser() {
    }

    /**
     * ct.
     * 
     * @param mask
     *            a filter which is applied to the files.
     */
    public FileBrowser(String mask) {
        addInclude(mask);
    }

    /**
     * Handle a file, matching the specified masks.
     * 
     * @param path
     *            path to file.
     * @param file
     *            the file name without path.
     */
    protected abstract void handleFile(String path, String file);

    /**
     * Compare the file name with the specified patterns. First a match against one of the include patterns is required.
     * Then no match against the exclude patters may occur.
     * 
     * @param name
     *            the checked file name
     * @return true if the file name matches the patterns, false either.
     */
    protected boolean accept(String name) {
        CharSequence cs = name.subSequence(0, name.length());
        for (Iterator<Pattern> i = incRules.iterator(); i.hasNext();) {
            Pattern p = i.next();
            if (p.matcher(cs).matches()) {
                for (i = excRules.iterator(); i.hasNext();) {
                    p = (Pattern) i.next();
                    if (p.matcher(cs).matches())
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Recurse over directories and invoke the file action for each File.
     * 
     * @param path
     *            path relative to baseDir
     */
    private void recurseDirs(String path) {
        enterDir(path);
        File dir = new File(baseDir, path);
        String[] files = dir.list();
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                String s = files[i];
                File f = new File(dir, s);
                if (f.isDirectory()) {
                    if (recurse) {
                        recurseDirs(path + "/" + s);
                    }
                    continue;
                }
                if (accept(path + "/" + s)) {
                    handleFile(path, s);
                }
            }
        }
        leaveDir(path);
    }

    protected void leaveDir(String path) {
    }

    protected void enterDir(String path) {
    }

    /**
     * Recurse over the entries of a JAR file and invoke the file action for each File.
     * 
     * @param path
     *            path relative to baseDir
     */
    private boolean recurseJar(JarFile jarFile) {
        for (Enumeration<JarEntry> i = jarFile.entries(); i.hasMoreElements();) {
            JarEntry je = i.nextElement();
            String full = je.getName();
            int slash = full.lastIndexOf('/');
            String path = full.substring(0, slash);
            String file = full.substring(slash + 1);
            if (!je.isDirectory()) {
                if (accept(path)) {
                    handleFile(path, file);
                }
            }
        }
        return true;
    }

    /**
     * Recurse over directories and invoke the file action for each File.
     * 
     * @param baseDir
     *            the dir to start
     * @param recurse
     *            a flag whether subdirectories are scanned too
     * @throws IOException
     */
    public final synchronized void scan(String baseDirs, boolean recurse) throws IOException {
        if (incRules.isEmpty()) {
            addInclude("**");
        }
        for (String baseDir : baseDirs.split(File.pathSeparator)) {
            File f = new File(baseDir);
            if (!f.exists())
                return;
            if (f.isDirectory()) {
                if (!baseDir.endsWith("/"))
                    baseDir += "/";
                this.baseDir = baseDir;
                this.recurse = recurse;
                recurseDirs("");
            } else {
                JarFile jarFile = new JarFile(f);
                this.recurse = recurse;
                recurseJar(jarFile);
            }
        }
    }

    /**
     * convert a wild star expression to
     * 
     * @param inc
     * @return
     */
    public static Pattern wild2regex(String inc) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inc.length(); ++i) {
            char ch = inc.charAt(i);
            if (ch == '*') {
                if (i + 1 < inc.length() && inc.charAt(i + 1) == '*') {
                    sb.append(".*");
                    ++i;
                } else {
                    sb.append("[^/\\\\]*");
                }
                continue;
            }
            if (ch == '?') {
                sb.append("[^/\\\\]");
                continue;
            }
            if ("([{\\^-$|]})?+.".indexOf(ch) >= 0) {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return Pattern.compile(sb.toString());
    }

    /**
     * Returns the baseDir.
     * 
     * @return String
     */
    public String getBaseDir() {
        return baseDir;
    }

    /**
     * add a wild star include pattern.
     * 
     * @param inc
     */
    public void addInclude(String inc) {
        incRules.add(wild2regex(inc));
    }

    /**
     * add a wild star exclude pattern.
     * 
     * @param exc
     */
    public void addExclude(String exc) {
        excRules.add(wild2regex(exc));
    }
}
