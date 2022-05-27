package de.bb.tools.bnm.eclipse.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class CcpInitializer extends ClasspathContainerInitializer {

    public CcpInitializer() {
    }

    
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        IClasspathContainer container = new CcpContainer(project);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project}, new IClasspathContainer[] {container}, null);
    }

    
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project,
            IClasspathContainer containerSuggestion) throws CoreException {
        // TODO Auto-generated method stub
        super.requestClasspathContainerUpdate(containerPath, project, containerSuggestion);
    }

    
    public String getDescription(IPath containerPath, IJavaProject project) {
        return "BNM compile classpath initializer";
    }

    
    public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
        // TODO Auto-generated method stub
        return super.getSourceAttachmentStatus(containerPath, project);
    }

    
    public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {
        // TODO Auto-generated method stub
        return super.getAttributeStatus(containerPath, project, attributeKey);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getComparisonID(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
     */
    public Object getComparisonID(IPath containerPath, IJavaProject project) {
        if (containerPath == null || project == null)
            return null;

        return containerPath.segment(0) + "/" + project.getPath().segment(0); //$NON-NLS-1$
    }

}
