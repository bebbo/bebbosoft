package de.bb.tools.bnm.eclipse.builder;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import de.bb.tools.bnm.Bnm;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.Pom;
import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.model.Dependency;
import de.bb.tools.bnm.model.Id;

public class CcpContainer implements IClasspathContainer {

    public static final String ID = "de.bb.tools.bnm.eclipse.compileClassPath";
    private IJavaProject project;

    public CcpContainer(IJavaProject project) {
        this.project = project;
    }

    /**
     * Get the class path entries based on the pom test dependencies.
     */
    public IClasspathEntry[] getClasspathEntries() {

        ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        try {

            Bnm bnm = new Bnm(Plugin.getLoader(), false);

            File pomPath = project.getResource().getLocation().toFile();
            bnm.loadFirst(pomPath);
            Pom pom = bnm.getProjectsInOrder().iterator().next();

            ArrayList<Id> deps = pom.getTestDependencies();
            Loader loader = pom.getLoader();
            for (Id id : deps) {
                if (!(id instanceof Dependency))
                    continue;
                try {
                    File jar = loader.findFile(id, null, "jar", false);
                    IPath jpath = new Path(jar.getAbsolutePath());
                    IClasspathEntry ce = JavaCore.newLibraryEntry(jpath, null, null);
                    entries.add(ce);
                } catch (Exception ex) {}
            }

        } catch (Exception exx) {
            exx.printStackTrace();
        }
        IClasspathEntry[] entriesA = entries.toArray(new IClasspathEntry[entries.size()]);
        try {
            IClasspathEntry[] entriesB = Tracker.updateClasspath(entriesA);
            if (entriesB != null) {
                entriesA = entriesB;
            }
        } catch (CoreException e) {
        }
        return entriesA;
    }

    public String getDescription() {
        return "BNM classpath";
    }

    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    public IPath getPath() {
        return new Path(Loader.getRepoPath().toString());
    }

}
