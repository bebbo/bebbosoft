package de.bb.bejy.j2ee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import de.bb.bejy.http.Injector;
import de.bb.util.LogFile;
import de.bb.util.ZipClassLoader;

/**
 * Ejb's class loader restricts the access to the MANFIEST.MF Class-Path
 * 
 * @author stefan franke
 * 
 */
public class EjbClassLoader extends ZipClassLoader implements Injector {

    private EarClassLoader earClassLoader;
    HashSet<URL> jars = new HashSet<URL>();
    ArrayList<URL> jarsOrdered = new ArrayList<URL>();

    /**
     * CT.
     * 
     * @param jar
     *            the referenced jar
     * @param earClassLoader
     *            the parent EarClassLoader
     * @throws IOException
     */
    public EjbClassLoader(String jar, EarClassLoader earClassLoader) throws IOException {
        super(earClassLoader);
        this.earClassLoader = earClassLoader;

        if (jar != null) {
            URL url = earClassLoader.addJar(jar);
            jars.add(url);
            addURL(url);
        }

        InputStream is = getResourceAsStream("META-INF/MANIFEST.MF");
        if (is != null) {
            readManifest(is);
        }
    }

    /**
     * parse the MANIFEST.MF
     * 
     * @param is
     * @throws IOException
     */
    private void readManifest(InputStream is) throws IOException {
        // find the class path
        BufferedReader isr = new BufferedReader(new InputStreamReader(is));
        String classPath = "";
        for (String line = isr.readLine(); line != null; line = isr.readLine()) {
            if (line.startsWith("Class-Path:")) {
                StringBuilder sb = new StringBuilder();
                sb.append(line.substring(11));
                for (line = isr.readLine(); line != null && line.length() > 0 && line.charAt(0) <= 32; line = isr
                        .readLine()) {
                    sb.append(line.substring(1));
                }
                classPath = sb.toString().trim();
                break;
            }
        }
        isr.close();

        // add the modules.
        for (StringTokenizer st = new StringTokenizer(classPath); st.hasMoreElements();) {
            final String jar = st.nextToken().trim();
            final URL url = earClassLoader.addJar(jar);
            if (jars.add(url))
                jarsOrdered.add(url);
        }
    }

    public void inject(LogFile log, Object o) throws Exception {
        earClassLoader.injectInstance(null, log, o);
    }

    /**
     * Ensure that shared JARs are only loaded by the ear class loader.
     */
    public void removeSharedJars() {
        final HashSet<URL> removed = new HashSet<URL>();
        removed.addAll(jars);
        removed.removeAll(earClassLoader.beanJars);

        for (final Iterator<URL> i = jarsOrdered.iterator(); i.hasNext();) {
            final URL url = i.next();
            if (removed.contains(url)) {
                jars.remove(url);
                i.remove();
            } else {
                addURL(url);
            }
        }
    }

    /*    
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve, jars);
        }

        
        public URL getResource(String name) {
            return super.getResource(name, jars);
        }

        
        public Enumeration<URL> findResources(String name) {
            return super.findResources(name, jars);
        }

        
        public InputStream getResourceAsStream(String name) {
            return super.getResourceAsStream(name, jars);
        }
    */
}
