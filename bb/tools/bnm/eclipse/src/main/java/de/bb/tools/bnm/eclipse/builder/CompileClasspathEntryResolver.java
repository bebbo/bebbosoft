package de.bb.tools.bnm.eclipse.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;

public class CompileClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
            ILaunchConfiguration configuration) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, IJavaProject project)
            throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

}
