/******************************************************************************
 * A class loader implementation with some useful features. 
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extends URLClassLoader and provides more functionality.
 * 
 * @author bebbo
 */
public class ZipClassLoader extends ClassLoader {
    private ArrayList<URL> urls = new ArrayList<URL>();

    // private HashMap cache = new HashMap();

    // work with a 5 second timeout to release zip and jar files.
    private static SessionManager<URL, Object> cache = new SessionManager<URL, Object>(1000L * 5);

    /**
     * Default constructor.
     */
    public ZipClassLoader() {
    }

    /**
     * Creates a new ZipClassLoader and provides a class path.
     * 
     * @param classPath
     *            the initial class path
     * @throws MalformedURLException
     */
    public ZipClassLoader(String classPath) throws MalformedURLException {
        addPath(classPath);
    }

    /**
     * Creates a new ZipClassLoader and provides a parent class loader.
     * 
     * @param parent
     *            the parent class loader.
     */
    public ZipClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Creates a new ZipClassLoader and provides an initial class path and a parent class loader.
     * 
     * @param classPath
     *            the initial class path
     * @param parent
     *            the parent class loader
     * @throws MalformedURLException
     */
    public ZipClassLoader(String classPath, ClassLoader parent) throws MalformedURLException {
        super(parent);
        addPath(classPath);
    }

    /**
     * parses and applies the path.
     * 
     * @param classPath
     *            the classpath
     * @throws MalformedURLException
     */
    public void addPath(String classPath) throws MalformedURLException {
        for (StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator); st.hasMoreElements();) {
            String pe = st.nextToken();
            addURL(path2URL(pe));
        }
    }

    /**
     * Returns an array with all files matching the search mask.
     * 
     * @param mask
     *            a search mask, may contain one wild star '*'
     * @return an array of matching URLs as String.
     */
    public String[] list(String mask) {
        return list(mask, null);
    }

    public String[] list(String mask, Set<URL> filter) {
        String start, stop;
        start = mask;
        stop = "";

        int sp = mask.indexOf('*');
        if (sp >= 0) {
            start = mask.substring(0, sp);
            stop = mask.substring(sp + 1);
        }

        HashSet<String> set = new HashSet<String>();

        for (URL url : urls) {
            if (filter != null && !filter.contains(url))
                continue;

            String surl = url.toString();
            if (!surl.startsWith("file:"))
                continue;
            surl = surl.substring(5);
            File f = new File(surl);
            if (f.isDirectory()) {
                collectDirectory(f, start, stop, set);
            } else if (f.isFile()) {
                collectJar(f, start, stop, set);
            }
        }

        String[] res = new String[set.size()];
        set.toArray(res);
        return res;
    }

    /**
     * add all matching files to the set.
     * 
     * @param f
     *            the file for the JAR
     * @param start
     *            start mask
     * @param stop
     *            stop mask
     * @param set
     *            result set
     */
    private static void collectJar(File f, String start, String stop, HashSet<String> set) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(f);
            for (@SuppressWarnings("unchecked")
            Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                String cur = ze.getName();
                if (cur.length() >= start.length() + stop.length() && cur.startsWith(start) && cur.endsWith(stop)) {
                    set.add(cur);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    /**
     * add all matching files to the set.
     * 
     * @param f
     *            the file for the folder
     * @param start
     *            start mask
     * @param stop
     *            stop mask
     * @param set
     *            result set
     */
    private static void collectDirectory(File f, String start, String stop, HashSet<String> set) {
        String add = "";
        int sp = start.lastIndexOf("/");
        if (sp > 0) {
            add = start.substring(0, sp + 1);
            start = start.substring(sp + 1);
        }

        Stack<String> stack = new Stack<String>();
        stack.push(add);

        while (stack.size() > 0) {
            add = stack.pop();
            File d = new File(f, add);
            String files[] = d.list();

            for (int j = 0; j < files.length; ++j) {
                String cur = files[j];
                String x = add + cur;
                if (cur.length() >= start.length() + stop.length() && cur.startsWith(start) && cur.endsWith(stop)) {
                    set.add(x);
                } else {
                    if (new File(f, x).isDirectory()) {
                        stack.push(x + "/");
                    }
                }
            }
        }
    }

    /**
     * Returns the classloader's class path as String.
     * 
     * @return the classloader's class path as String.
     */
    public String getClassPath() {
        StringBuffer sb = new StringBuffer();

        URL urls[] = getURLs();
        for (int i = 0; i < urls.length; ++i) {
            String url = urls[i].toString();
            if (!url.startsWith("file:"))
                continue;
            if (sb.length() > 0)
                sb.append(File.pathSeparatorChar);
            url = url.substring(5);
            while (url.startsWith("/") && url.indexOf(':') >= 0)
                url = url.substring(1);

            if (url.indexOf(' ') > 0) {
                sb.append('"');
                sb.append(url);
                sb.append('"');
            } else {
                sb.append(url);
            }
        }
        // System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * add the path to the class path.
     * 
     * @param path
     *            a class lookup path as folder or JAR file
     * @throws MalformedURLException
     */
    public static URL path2URL(String path) throws MalformedURLException {
        if (path.charAt(0) == '"')
            path = path.substring(1, path.length() - 1);

        File f = new File(path);
        path = f.getAbsolutePath();
        for (int i = path.indexOf('\\'); i >= 0; i = path.indexOf('\\', i + 1)) {
            path = path.substring(0, i) + "/" + path.substring(i + 1);
        }
        if (f.isDirectory())
            path += "/";
        URL url = new URL("file", "", path);
        return url;
    }

    /**
     * Return an array containing all URLs in search order.
     * 
     * @return an array containing all URLs.
     */
    public URL[] getURLs() {
        return urls.toArray(new URL[] {});
    }

    /**
     * Add the url to the search path.
     * 
     * @param url
     *            a url - either path or JAR
     */
    public void addURL(URL url) {
        urls.add(url);
    }

    /**
     * Load a specified class and resolve it, if wanted.
     * 
     * @param name
     *            of the class.
     * @param resolve
     *            flag whether class is to resolve.
     * @return the loaded class.
     * @exception ClassNotFoundException
     *                if class was not loaded.
     */
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, null);
    }

    public Class<?> loadClass(String name, boolean resolve, Set<URL> filter) throws ClassNotFoundException {
        String className = Misc.replaceAll(name, '/', '.');
        name = Misc.replaceAll(name, '.', '/');
        Class<?> c = findLoadedClass(className);
        if (c != null)
            return c;

        try {
            c = findSystemClass(className);
            if (c != null)
                return c;
        } catch (Throwable e) {
        }

        String fileName = makeClassFileName(name);
        InputStream in = getResourceAsStream(fileName, filter);

        if (in != null)
            c = loadClass(className, in);

        if (c == null && getParent() != null)
            c = getParent().loadClass(className);

        if (c == null)
            throw new ClassNotFoundException(name);

        if (resolve)
            resolveClass(c);

        return c;
    }

    /**
     * Load a class by classname and InputStream.
     * 
     * @param className
     *            the classname
     * @param in
     *            the input Stream
     * @return the class
     * @throws ClassNotFoundException
     *             if the class cannot be defined.
     */
    public Class<?> loadClass(String className, InputStream in) throws ClassNotFoundException {
        try {
            byte bits[] = loadBytes(in);
            return defineClass(className, bits, 0, bits.length);
        } catch (Throwable e) {
            throw new ClassNotFoundException(className + " (loaded via stream: " + in, e);
        }
    }

    /**
     * Convert a class name into path name.
     */
    public static String makeClassFileName(String className) {
        byte b[] = className.getBytes();
        for (int i = 0; i < b.length; ++i)
            if (b[i] == '.')
                b[i] = '/';
        return new String(b) + ".class";
    }

    /**
     * Load the bytes for a given file name. / private byte[] loadBytes(String fileName) { InputStream in =
     * getResourceAsStream(fileName); if (in == null) return null; try { return loadBytes(in); } catch (IOException e) {
     * } return null; } /
     **/

    /**
     * Load the bytes for a given InputStream.
     * 
     * @param in
     *            the InputStream to read from.
     * @return a new allocated byte array containing the read data.
     * @throws IOException
     *             on error
     */
    public static byte[] loadBytes(InputStream in) throws IOException {
        try {
            // System.out.println(in.available());
            byte[] b = new byte[in.available()];

            int n, pos = 0;
            while (pos < b.length && (n = in.read(b, pos, b.length - pos)) != -1)
                pos += n;
            return b;
        } finally {
            in.close();
        }
    }

    /**
     * Get an InputStream for the specified file.
     * 
     * @param fileName
     *            the file name.
     * @return an InputStream on success or null on error.
     */
    public InputStream getResourceAsStream(String fileName) {
        URL url = getResource(fileName);
        if (url == null)
            return null;
        return getResourceAsStream(url);
    }

    public InputStream getResourceAsStream(String fileName, Set<URL> filter) {
        URL url = getResource(fileName, filter);
        if (url == null)
            return null;
        return getResourceAsStream(url);
    }

    /**
     * Get an InputStream for the specified URL.
     * 
     * @param url
     *            an URL for a file, also file inside a JAR
     * @return an InputStream on success or null on error.
     */
    public InputStream getResourceAsStream(URL url) {
        try {
            String surl = url.toString();
            if (surl.startsWith("jar:")) {
                int excl = surl.indexOf("!");
                if (excl < 0)
                    return null;
                String szip = surl.substring(4, excl);

                Object o = getObject(new URL(szip));
                String file = surl.substring(excl + 2);
                ZipFile zf = (ZipFile) o;
                ZipEntry ze = zf.getEntry(file);
                InputStream is = zf.getInputStream(ze);
                return new MacBugIS(is, (int) ze.getSize());
            }

            return new FileInputStream(url.getFile());
        } catch (Exception e) {
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String fileName) {
        Enumeration<URL> e = collectURLs(fileName, false, null);
        if (e.hasMoreElements())
            return e.nextElement();
        if (getParent() != null)
            return getParent().getResource(fileName);
        return null;
    }

    public URL getResource(String fileName, Set<URL> filter) {
        Enumeration<URL> e = collectURLs(fileName, false, filter);
        if (e.hasMoreElements())
            return e.nextElement();
        return null;
    }

    /**
     * Searches all resources with this file name, and returns them as an Enumeration.
     * 
     * @param fileName
     *            the file name
     * @return an Enumeration containing the URLs for the specified file name.
     */
    public Enumeration<URL> findResources(String fileName) {
        Enumeration<URL> e = collectURLs(fileName, true, null);
        return e;
    }

    /**
     * Searches all resources with this file name, and returns them as an Enumeration. But only searches the filtered
     * JARs
     * 
     * @param fileName
     *            the file name
     * @param filter
     *            the URLs to search
     * @return an Enumeration containing the URLs for the specified file name.
     */
    public Enumeration<URL> findResources(String fileName, Set<URL> filter) {
        Enumeration<URL> e = collectURLs(fileName, true, filter);
        return e;
    }

    /**
     * @param fileName
     * @param filter
     * @return
     */
    private Enumeration<URL> collectURLs(String fileName, boolean loop, Set<URL> filter) {
        // sMan.put(this, this);
        Vector<URL> v = new Vector<URL>();
        for (URL url : urls) {
            if (filter != null && !filter.contains(url))
                continue;

            Object o = getObject(url);
            if (o instanceof ZipFile) {
                try {
                    ZipFile zf = (ZipFile) o;
                    // System.out.println("zip: " + zf + " for " + url);
                    ZipEntry ze = zf.getEntry(fileName);
                    if (ze != null) {
                        v.add(new URL("jar", "", url.toString() + "!/" + ze.getName()));
                        if (loop)
                            continue;
                        break;
                    }
                } catch (MalformedURLException e) {
                }
                continue;
            }

            if (!(o instanceof File))
                continue;

            File dir = (File) o;
            File f = new File(dir, fileName);
            try {
                if (f.exists()) {
                    v.add(new URL("file:///" + f.getAbsolutePath()));
                    if (loop)
                        continue;
                    break;
                }
            } catch (MalformedURLException e) {
            }
        }
        return v.elements();
    }

    /**
     * @param url
     * @return
     */
    private Object getObject(URL url) {
        // sMan.put(this, this);
        Object o = cache.get(url);
        if (o == null) {
            File f = new File(url.getFile());
            if (f.isDirectory()) {
                o = f;
            } else {
                try {
                    o = new ZipFile(f);
                } catch (IOException e) {
                    o = this;
                }
            }
            cache.put(url, o);
        } else {
            cache.touch(url);
        }
        return o;
    }

    private static class MacBugIS extends InputStream {
        private int sz;

        private InputStream is;

        MacBugIS(InputStream is, int sz) {
            this.is = is;
            this.sz = sz;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException {
            if (sz <= 0)
                return -1;
            --sz;
            return is.read();
        }

        public int available() {
            return sz;
        }

        public void close() throws IOException {
            is.close();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (len < 0)
                return 0;
            if (sz <= 0)
                return -1;
            if (len > sz)
                len = sz;
            len = is.read(b, off, len);
            if (len > 0)
                sz -= len;
            return len;
        }
        /**/
    }

    public String toString() {
        return getClass().getName() + ": " + urls;
    }
}
