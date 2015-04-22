package de.bb.tools.bnm.plugin.exec;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.annotiation.Config;
import de.bb.tools.bnm.annotiation.Default;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;
import de.bb.util.XmlFile;
import de.bb.util.ZipClassLoader;

public class JavaPlugin extends AbstractPlugin {

    private final static String ARGS[] = {};
    
    @Config("arguments")
    private String arguments;

    @Config("classpathScope")
    private String classpathScope;

    @Config("executableDependency")
    private String executableDependency;

    @Config("includePluginDependencies")
    @Default("true")
    private boolean includePluginDependencies;

    @Config("includeProjectDependencies")
    @Default("true")
    private boolean includeProjectDependencies;

    @Config("mainClass")
    private String mainClass;

    @Config("systemProperties")
    private String systemProperties;

    @Override
    public void execute() throws Exception {
        if (mainClass == null)
            throw new Exception("there is no main class specified");

        Loader loader = project.getLoader();

        ZipClassLoader zcl = new ZipClassLoader();

        if (includeProjectDependencies)
            addDeps(loader, zcl, project.getEffectivePom().dependencies);

        if (includePluginDependencies && dependencies != null) {
            addDeps(loader, zcl, dependencies);
        }

        ArrayList<String> argsList = new ArrayList<String>();
        XmlFile xml = new XmlFile();
        if (arguments != null)
            xml.readString("<a>" + arguments + "</a>");
        for (Iterator<String> i = xml.sections("/a/argument");i.hasNext();) {
            String key = i.next();
            argsList.add(xml.getContent(key));
        }
        String args[] = argsList.toArray(ARGS);
        
        getLog().info(mainClass + " " + argsList);
        
        Class<?> mainClazz = zcl.loadClass(mainClass);
        Method mainMethod = mainClazz.getMethod("main", ARGS.getClass());
        mainMethod.invoke(null, (Object)args);
    }

    private void addDeps(Loader loader, ZipClassLoader zcl,
            ArrayList<Dependency> dependencies) throws Exception,
            MalformedURLException {
        for (Dependency dep : dependencies) {
            if ("pom".equals(dep.type))
                continue;

            Pom depPom = project.getBnm().loadPom(loader, null, dep);

            for (Id id : depPom.getRuntimeDependencies()) {
                File cpElement = loader.findFile(id, "jar");
                URL url = new URL("file:///" + cpElement);
                zcl.addURL(url);
            }
        }
    }

}
