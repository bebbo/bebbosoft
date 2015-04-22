package de.bb.tools.bnm.plugin.flex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Log;
import de.bb.tools.bnm.annotiation.Property;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;
import de.bb.tools.bnm.model.Project;
import de.bb.util.FileBrowser;
import de.bb.util.XmlFile;
import de.bb.util.ZipClassLoader;

abstract class AbstractFlexPlugin extends AbstractPlugin {

    private static final String CP[] = { "com.adobe.flex.compiler:mxmlc",
            "com.adobe.flex.compiler:asc",
            "com.adobe.flex.compiler:xmlParserAPIs",
            "com.adobe.flex.compiler:afe", "com.adobe.flex.compiler:aglj32",
            "com.adobe.flex.compiler:rideau",
            "com.adobe.flex.compiler:batik-all-flex",
            "com.adobe.flex.compiler:mm-velocity-1.4",
            "com.adobe.flex.compiler:commons-collections",
            "com.adobe.flex.compiler:commons-discovery",
            "com.adobe.flex.compiler:commons-logging",
            "com.adobe.flex.compiler:license",
            "com.adobe.flex.compiler:swfutils",
            "com.adobe.flex.compiler:flex-fontkit",
            "com.adobe.flex.compiler:flex-messaging-common",
            "com.adobe.flex.compiler:mxmlc_ja",
            "com.adobe.flex.compiler:xalan", };

    private static final String LP[] = { "com.adobe.flex.framework:automation",
            "com.adobe.flex.framework:datavisualization",
            "com.adobe.flex.framework:flex",
            "com.adobe.flex.framework:framework",
            "com.adobe.flex.framework:rpc",
            "com.adobe.flex.framework:utilities", };

    @Property("${project.build.directory}")
    protected File directory;

    protected Log log;

    /**
     * Create the configuration folder and setup all paths
     * 
     * @param loader
     *            the loader
     * @param version
     *            the flex framework version
     * @param targetVersion
     * @param swcs
     * @return the folder containing the configuration
     * @throws Exception
     */
    protected File loadConfigs(Loader loader, String version,
            String targetPlayer, String lang, ArrayList<File> swcs)
            throws Exception {

        String targetVersion = "10";
        final int dot = targetPlayer.indexOf('.');
        if (dot > 0)
            targetVersion = targetPlayer.substring(0, dot);

        // check if there is a new version
        final File configs = loader.findFile(new Id(
                "com.adobe.flex.framework:framework:" + version), "configs",
                "zip");
        final File flexDir = new File(directory, "flex");
        final File localeDir = new File(flexDir, "locale/" + lang);
        if (!localeDir.exists())
            localeDir.mkdirs();

        // unpack the configuration files
        final ZipFile zf = new ZipFile(configs);
        try {
            for (final Enumeration<? extends ZipEntry> e = zf.entries(); e
                    .hasMoreElements();) {
                final ZipEntry ze = e.nextElement();
                final File out = new File(flexDir, ze.getName());
                copyFile(zf.getInputStream(ze), out);
            }
        } finally {
            zf.close();
        }

        // patch the files
        final File flexConfig = new File(flexDir, "flex-config.xml");
        final XmlFile xml = new XmlFile();
        xml.readFile(flexConfig.getAbsolutePath());

        xml.setString("/flex-config/compiler/source-path", null, "true");
        String sourceDirectory = new File(project.getPath(),
                getSourceDirectory()).getAbsolutePath();
        xml.setContent("/flex-config/compiler/source-path/path-element",
                sourceDirectory);

        // patch manifest
        xml.setContent("/flex-config/target-player/", targetPlayer);

        String resourceBundlePath = configuration.get("resourceBundlePath");
        if (resourceBundlePath != null) {
            final String section = xml
                    .createSection("/flex-config/compiler/source-path/path-element");
            xml.setContent(section, resourceBundlePath);
        }

        String scompiledLocales = configuration.get("compiledLocales");
        if (scompiledLocales != null) {
            XmlFile xcompiledLocales = new XmlFile();
            xcompiledLocales.readString("<x>" + scompiledLocales + "</x>");
            xml.setString("/flex-config/compiler/locale", null, "true");
            for (final String localeSec : xcompiledLocales.getSections("/x/")) {
                final String locale = xcompiledLocales.getContent(localeSec);
                final String sec = xml
                        .createSection("/flex-config/compiler/locale/locale-element");
                xml.setContent(sec, locale);
            }
        }

        xml.setString("/flex-config/compiler/namespaces", null, "true");
        final File manifest = new File(flexDir, "mxml-manifest.xml");
        xml.setContent("/flex-config/compiler/namespaces/namespace/uri",
                "http://www.adobe.com/2006/mxml");
        xml.setContent("/flex-config/compiler/namespaces/namespace/manifest",
                manifest.getAbsolutePath());

        // fill the external library path
        xml.setString("/flex-config/compiler/external-library-path", null,
                "true");
        final File playerglobal = loader.findFile(new Id(
                "com.adobe.flex.framework:playerglobal:" + version),
                targetVersion, "swc");
        xml.setContent(
                "/flex-config/compiler/external-library-path/path-element",
                playerglobal.getAbsolutePath());

        // fill the library path
        xml.setString("/flex-config/compiler/library-path", null, "true");
        for (final String s : LP) {
            final String section = xml
                    .createSection("/flex-config/compiler/library-path/path-element");
            final File lib = loader.findFile(new Id(s + ":" + version), "swc");
            xml.setContent(section, lib.getAbsolutePath());

            final File pomFile = loader.findFile(new Id(s + ":" + version),
                    "pom");
            Project p = project.getBnm().loadProject(pomFile);
            if ("rb.swc".equals(p.packaging)) {
                final File rbFile2 = loader.findFile(new Id(s + ":" + version),
                        lang, "rb.swc");
                final FileInputStream fis2 = new FileInputStream(rbFile2);
                copyFile(fis2, new File(localeDir, rbFile2.getName()));
                fis2.close();
            }

        }
        for (final File swc : swcs) {
            final String section = xml
                    .createSection("/flex-config/compiler/library-path/path-element");
            xml.setContent(section, swc.getAbsolutePath());
        }

        final String section = xml
                .createSection("/flex-config/compiler/library-path/path-element");
        final File locale = new File(flexDir, "locale/" + lang);
        xml.setContent(section, locale.getAbsolutePath());

        final OutputStream fos = new FileOutputStream(flexConfig);
        xml.write(fos);
        fos.close();
        return flexDir;
    }

    protected ArrayList<URL> makeCp(Loader loader, String version)
            throws Exception {
        final ArrayList<URL> urls = new ArrayList<URL>();

        for (final String sid : CP) {
            final Id id = new Id(sid + ":" + version);
            final File jar = loader.findFile(id, "jar");
            urls.add(jar.toURI().toURL());
        }

        return urls;
    }

    protected ArrayList<String> scanFiles(final String source)
            throws IOException {
        final ArrayList<String> files = new ArrayList<String>();
        final FileBrowser fb = new FileBrowser() {
            protected void handleFile(String path, String name) {
                String f = getBaseDir() + path + "/" + name;
                files.add(f);
            }
        };

        final File path = project.getPath();
        fb.scan(new File(path, source).getAbsolutePath(), true);

        return files;
    }

    public static void copyFile(InputStream is, File to) throws IOException {
        File path = to.getParentFile();
        if (!path.exists())
            path.mkdirs();
        FileOutputStream fos = new FileOutputStream(to);
        byte buf[] = new byte[0x10000];
        for (;;) {
            int len = is.read(buf, 0, buf.length);
            if (len <= 0)
                break;
            fos.write(buf, 0, len);
        }
        is.close();
        fos.close();
    }

    public void execute() throws Exception {
        log = Log.getLog();
        final Loader loader = project.getLoader();

        // final String sourceDirectory = new File(project.getPath(),
        // getSourceDirectory()).getAbsolutePath();
        String sourceDirectory = getSourceDirectory();
        final String finalName = getFinalName();
        final File outputFile = new File(directory, finalName);
        final ArrayList<String> files = scanFiles(sourceDirectory);
        if (files.isEmpty()) {
            log.info("no input for: " + outputFile + ", exiting");
            return;
        }
        if (!forceBuild && outputFile.exists()) {
            for (final String f : files) {
                if (new File(f).lastModified() >= outputFile.lastModified()) {
                    forceBuild = true;
                    break;
                }
            }

            if (!forceBuild) {
                log.info(outputFile + " is up to date, exiting");
                // attach the existing SWC
                project.attachFile(outputFile);
                return;
            }
        }
        forceBuild = true;

        sourceDirectory = new File(project.getPath(), sourceDirectory)
                .getAbsolutePath();

        final ArrayList<Id> compileDeps = project.getCompileDependencies();

        // build class path to invoke flex2.tools.Compc
        // find the framework and get the version
        String version = null;
        for (final Id id : compileDeps) {
            if (id.getGA().equals("com.adobe.flex.framework:flex-framework")) {
                version = id.version;
                break;
            }
        }
        if (version == null)
            throw new Exception("can't determine flex-framwork version");

        String targetPlayer = "10.0.0.0";
        if (configuration.get("target-player") instanceof String) {
            targetPlayer = (String) configuration.get("target-player");
        }

        final ArrayList<URL> urls = makeCp(loader, version);
        final ZipClassLoader zClassLoader = new ZipClassLoader(getClass()
                .getClassLoader());

        final ArrayList<File> swcs = new ArrayList<File>();
        for (final Id id : project.getCompileDependencies()) {
            Project p = project.getBnm().loadProject(loader, id);
            if ("jar".equals(p.packaging)) {
                final File jar = loader.findFile(id, "jar");
                urls.add(jar.toURI().toURL());
            }
            if ("swc".equals(p.packaging)) {
                final File swc = loader.findFile(id,
                        ((Dependency) id).classifier, "swc");
                swcs.add(swc);
            }
        }

        final File configDir = loadConfigs(loader, version, targetPlayer,
                "de_DE", swcs);

        final ArrayList<String> largs = new ArrayList<String>();

        largs.add("+flexlib=" + configDir.getAbsolutePath());
        largs.add("-output=" + outputFile.getAbsolutePath());

        extendArgs(largs);

        String[] args = new String[largs.size()];
        largs.toArray(args);

        // INFO Output
        final URLClassLoader uClassLoader = new URLClassLoader(
                urls.toArray(new URL[urls.size()]));
        for (URL url : urls) {
            zClassLoader.addURL(url);
        }
        log.info("using cp: " + zClassLoader.getClassPath());

        String sargs = largs.toString();
        sargs = sargs.substring(1, sargs.length() - 1).replace(',', ' ');
        log.info("using args: " + sargs);

        // compile the sources
        final Thread fred = Thread.currentThread();
        final ClassLoader ocl = fred.getContextClassLoader();
        try {
            fred.setContextClassLoader(uClassLoader);
            String compiler = getCompiler();
            final Class<?> mainClass = uClassLoader.loadClass(compiler);
            final Method main = mainClass.getMethod(getMainMethod(),
                    args.getClass());
            final Integer ret = (Integer) main.invoke(null, (Object) args);
            if (ret != null && ret != 0)
                throw new Exception("compc returned: " + ret);

            project.attachFile(outputFile);

        } finally {
            fred.setContextClassLoader(ocl);
        }

        // attach the created SWC
        project.attachFile(outputFile);
    }

    protected abstract String getMainMethod();

    protected abstract String getCompiler();

    protected abstract void extendArgs(ArrayList<String> largs);

    protected String getFinalName() throws Exception {
        final Project pom = project.getEffectivePom();
        return pom.build.finalName + "." + pom.packaging;
    }

    protected abstract String getSourceDirectory();

}
