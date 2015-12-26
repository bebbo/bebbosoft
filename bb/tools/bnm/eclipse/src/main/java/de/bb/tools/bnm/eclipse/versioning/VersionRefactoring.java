package de.bb.tools.bnm.eclipse.versioning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import de.bb.tools.bnm.eclipse.Plugin;
import de.bb.tools.bnm.eclipse.versioning.dumb.PomInfo;
import de.bb.tools.bnm.eclipse.versioning.dumb.Pos;

public abstract class VersionRefactoring extends Refactoring {

    protected static final VI VI0[] = {};
    protected static final RefactoringStatus OK = new RefactoringStatus();
    protected IProject currentProject;
    protected ArrayList<VI> data = new ArrayList<VI>();
    ArrayList<IPath> pomList = new ArrayList<IPath>();
    protected HashMap<PomInfo, IPath> pom2Loc = new HashMap<PomInfo, IPath>();
    protected HashMap<String, PomInfo> id2Pom = new HashMap<String, PomInfo>();
	protected boolean promoteToSnapshot = true;

    public VersionRefactoring() {
    }

    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        try {
            // collect the data
            IResourceVisitor visitor = new IResourceVisitor() {
                public boolean visit(IResource resource) throws CoreException {
                    if (resource instanceof IFolder)
                        return true;
                    IPath loc = resource.getLocation();
                    if ("pom.xml".equals(loc.lastSegment())) {
                    	IPath loc2 = loc.removeLastSegments(2);
                    	if (!"target".equals(loc2.lastSegment()))
                    		pomList.add(loc);
                    }
                    return true;
                }
            };

            pm.beginTask("scanning pom.xml and MANIFEST.MF", 2);

            currentProject.accept(visitor);
            pm.worked(1);

            SubProgressMonitor spm = new SubProgressMonitor(pm, 1);
            spm.beginTask("reading content", pomList.size());

            // map ids to the first occured index.
            HashMap<String, Integer> id2Index = new HashMap<String, Integer>();
            HashMap<String, VI> entries = new HashMap<String, VI>();

            for (IPath pomLoc : pomList) {
                PomInfo pInfo;
                try {
                    pInfo = new PomInfo(pomLoc.toFile());
                } catch (IOException ioe) {
                    continue;
                }
                pom2Loc.put(pInfo, pomLoc);
                id2Pom.put(pInfo.getId(), pInfo);
                String bundleId = pInfo.getBundleId();
                if (bundleId != null) {
                    id2Pom.put(bundleId, pInfo);
                }

                for (String id : pInfo.getReferences()) {
                    int colon = id.lastIndexOf(':');
                    String ga = id.substring(0, colon);
                    String ver = id.substring(colon + 1);

                    // collect the data inside the array list
                    VI vi = entries.get(ga);
                    if (vi == null) {
                        vi = new VI();
                        vi.id = PomInfo.toId(id);
                        vi.origVersion = vi.version = ver;
                        if (vi.id.equals(pInfo.getId())) {
                            vi.bundleId = pInfo.getBundleId();
                            vi.origBundleVersion = vi.bundleVersion = pInfo
                                    .getBundleVersion();
                        }
                        entries.put(ga, vi);
                        data.add(vi);
                    }

                    vi.addVersion(ver);
                }
                spm.worked(1);
            }
            spm.done();

            return OK;
        } catch (CoreException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
                    e.getMessage()));
        }
    }

    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        // nada
        return new RefactoringStatus();
    }

    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

        if (data == null)
            return new NullChange();
        CompositeChange change = new CompositeChange("version changes");
        pm.beginTask("calculating changes", 1);
        SubProgressMonitor sm = new SubProgressMonitor(pm, 1);
        ArrayList<String> manifests = addPomChanges(change, sm);
        sm.done();
//        sm = new SubProgressMonitor(pm, 1);
//        addManifestChanges(change, sm, manifests);
//        sm.done();

        return change;
    }

    public String getName() {
        return "bnm version refactoring";
    }

    public VI[] getData() {
        return data.toArray(VI0);
    }

    protected ArrayList<String> addPomChanges(CompositeChange change, SubProgressMonitor pm) {
        SortedMap<String, String> pomDeps = Util.newTypedMultiMap();

        pm.beginTask("refactoring pom.xml files", 4);
        // build reference map
        for (PomInfo pi : pom2Loc.keySet()) {
            String id = pi.getId();
            for (String ref : pi.getReferences()) {
                pomDeps.put(PomInfo.toId(ref), PomInfo.toId(id));
            }
        }
        pm.worked(1);

        HashSet<String> modifiedIds = new HashSet<String>();
        HashMap<String, String> id2newVersion = new HashMap<String, String>();
        HashSet<String> touched = new HashSet<String>();
        // find all changes
        for (VI vi : data) {
            String orgV = vi.origVersion;
            String newV = vi.version;
            if (orgV != null && !orgV.equals(newV)) {
                String mod = vi.id;
                modifiedIds.add(mod);
                id2newVersion.put(mod, newV);
                touched.add(mod);
            }
        }
        pm.worked(1);

        extendPomChanges(pomDeps, modifiedIds, id2newVersion, touched);
        pm.worked(1);

        // now all elements to modify are known.
        ArrayList<String> manifests = new ArrayList<String>();
        if (modifiedIds.size() == 0)
            return manifests;

        int removeCount = currentProject.getLocation().segmentCount();
        HashMap<String, TextEdit> pom2TextEdit = new HashMap<String, TextEdit>();
        for (String pomId : touched) {
            PomInfo pi = id2Pom.get(pomId);
            if (pi == null)
                continue;
            TextEdit te = pom2TextEdit.get(pomId);
            if (te == null) {
                if (pi.getBundleId() != null)
                    manifests.add(pi.getBundleId());
                te = new MultiTextEdit();
                pom2TextEdit.put(pomId, te);
                IPath loc = pom2Loc.get(pi).removeFirstSegments(removeCount);
                IFile f = (IFile) this.currentProject.findMember(loc);
                TextFileChange tfc = new TextFileChange("pom.xml : " + pomId, f);
                tfc.setEdit(te);
                change.add(tfc);
            }
            for (String mod : modifiedIds) {
                ArrayList<Pos> positions = pi.getPositions(mod);
                if (positions != null) {
                    String nv = id2newVersion.get(mod);
                    for (Pos pos : positions) {
                        TextEdit re;
                        if (pos.getLength() > 0) {
                            re = new ReplaceEdit(pos.getOffset(), pos.getLength(), nv);
                        } else {
                            re = new ReplaceEdit(pos.getOffset(), pos.getLength(), "\r\n<version>" + nv + "</version>");
                        }
                        try {
                            te.addChild(re);
                        } catch (MalformedTreeException mte) {
                        }
                    }
                }
            }
            
            String bundleId = pi.getBundleId();
            if (bundleId == null)
                continue;

            te = new MultiTextEdit();
            for (String mod : modifiedIds) {
                String nv = id2newVersion.get(mod);
                if (nv.endsWith("-SNAPSHOT")) {
                    nv = nv.substring(0, nv.length() - 9);
                }
                Pos pos = pi.getBundlePositions(mod);
                if (pos != null) {
                    if (pos.getLength() == 0) {
                        te.addChild(new ReplaceEdit(pos.getOffset(), pos.getLength(), ";bundle-version=\"" + nv + "\""));
                    } else {
                        te.addChild(new ReplaceEdit(pos.getOffset(), pos.getLength(), nv));
                    }
                }
            }
            IPath loc = pom2Loc.get(pi).removeFirstSegments(removeCount);
            loc = loc.removeLastSegments(1).append("META-INF").append("MANIFEST.MF");
            IFile f = (IFile) this.currentProject.findMember(loc);
            TextFileChange tfc = new TextFileChange("MANIFEST.MF : " + pomId, f);
            tfc.setEdit(te);
            change.add(tfc);

        }
        pm.worked(1);

        return manifests;
    }


    protected void extendPomChanges(SortedMap<String, String> maniDeps, HashSet<String> modifiedIds,
            HashMap<String, String> id2newVersion, HashSet<String> touched) {
        // walk through all and promote changes
        Stack<String> stack = new Stack<String>();
        stack.addAll(modifiedIds);
        while (stack.size() > 0) {
            String module = stack.pop();
            for (String ref : maniDeps.subMap(module, module + "\0").values()) {
//                if (touched.contains(ref))
//                    continue;
                touched.add(ref);

                PomInfo pi = id2Pom.get(ref);
                String version = id2newVersion.get(ref);
                if (version == null)
                    version = pi.getVersion();
                if (version == null || promoteToSnapshot == version.endsWith("-SNAPSHOT"))
                    continue;

                String newVersion = Util.nextSnapshots(version).get(0);
                id2newVersion.put(ref, newVersion);
                modifiedIds.add(ref);
                stack.push(ref);
            }
        }
    }
}