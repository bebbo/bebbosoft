package de.bb.bejy.j2ee;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.bejy.http.Host;
import de.bb.bejy.http.WebAppContext;
import de.bb.bejy.http.WebAppsCfg;
import de.bb.util.LogFile;
import de.bb.util.MultiMap;
import de.bb.util.XmlFile;

public class Ear extends Configurable {
    private static final String[][] PROPERTIES = { { "display-name", "the display name" } };

    long loadTime;

    private String path;

    private EarClassLoader earClassLoader;

    private ArrayList<EjbClassLoader> ejbClassLoaders = new ArrayList<EjbClassLoader>();

    private ArrayList<WebAppContext> warContexts = new ArrayList<WebAppContext>();

    public Ear() {
        init("ear", PROPERTIES);
    }

    public void activate(LogFile logFile) throws Exception {
        logFile.writeDate("loading EAR:" + getProperty("name"));

        path = getProperty("path").trim();
        // if EAR archive, unpack it
        if (path.toUpperCase().endsWith(".EAR")) {
            File file = new File(path);
            File dir = new File(getProperty("folder"));
            logFile.writeDate("unpacking " + file.getPath());
            WebAppContext.unpack(file, dir);
            path = dir.toString();
        }

        // read the application XML file
        XmlFile application = new XmlFile();
        application.readFile(path + "/META-INF/application.xml");

        setProperty("description", application.getContent("/application/description"));
        setProperty("display-name", application.getContent("/application/display-name"));

        // load the ejb modules
        earClassLoader = new EarClassLoader(path);

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(earClassLoader);
        try {

            // load the ejb modules
            for (Iterator<String> i = application.sections("/application/module"); i.hasNext();) {
                String module = i.next();
                loadEjbModule(application, module);
                loadWarModule(application, module);
            }

            // initialize the ejb modules
            final Thread t = Thread.currentThread();
            final ClassLoader cl = t.getContextClassLoader();
            try {
                for (EjbClassLoader ecl : ejbClassLoaders) {
                    t.setContextClassLoader(ecl);
                    ecl.removeSharedJars();
                    MultiMap<String, String> ejbStuff = EarClassLoader.earscan(ecl,
                            earClassLoader.list("*.class", ecl.jars));
                    earClassLoader.initializeBeans(ecl, ejbStuff, logFile);
                }
            } finally {
                t.setContextClassLoader(cl);
            }
            // collect all instances to patch
            HashSet<Object> beans = new HashSet<Object>();
            Set<EjbIH> ejbs = earClassLoader.getBeans();
            for (EjbIH eih : ejbs) {
                beans.add(eih.ejb);
            }

            earClassLoader.injectBeans(beans, logFile);

            // activate each war and create the web services.
            for (WebAppContext wac : warContexts) {
                addChild("war", wac);
                wac.activate(logFile);

                earClassLoader.initializeWS(wac, logFile);

                ((Host) getParent().getParent()).addContext(wac);
            }

            earClassLoader.check(logFile);

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private void loadEjbModule(XmlFile application, String module) throws Exception {
        String jar = application.getContent(module + "ejb");
        if (jar == null)
            return;

        // create an ejb module
        EjbClassLoader ejbCl = new EjbClassLoader(jar, earClassLoader);
        ejbClassLoaders.add(ejbCl);
        return;
    }

    private void loadWarModule(XmlFile application, String module) throws Exception {
        String jar = application.getContent(module + "web/web-uri");
        if (jar == null)
            return;

        // this is just an anchor - do not add the WAR itself to the class path
        EjbClassLoader ejbCl = new EjbClassLoader(jar, earClassLoader);
        ejbClassLoaders.add(ejbCl);

        String root = application.getContent(module + "web/context-root");
        // add a WebAppContext
        WebAppContext wac = new WebAppContext(ejbCl);

        String[] s3 = WebAppsCfg.resolvePath(new File(path, jar));
        // s3[0] = name -> not used
        wac.setProperty("path", s3[1]);
        wac.setProperty("folder", s3[2]);
        wac.setProperty("alias", root);
        warContexts.add(wac);
    }

    public void deactivate(LogFile logFile) throws Exception {
        logFile.writeDate("unloading EAR:" + getProperty("name"));
    }

    public Configurator getConfigurator() {
        return new EarCfg();
    }

    public Object lookup(Principal principal, String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
