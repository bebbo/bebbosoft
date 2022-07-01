package de.bb.tools.bnm.plugin.dependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.bb.tools.bnm.model.Dependency;

/**
 * Print as tree.
 *
 * @author stefan franke
 */

public class TreePlugin extends AbstractDepsPlugin {

	@Override
	public void execute() throws Exception {
		try {
			super.execute();

			List<Object> dependencyTree = project.getDependencyTree(includeScope.toUpperCase());

			dependencyTree.remove(0); // remove self

			write(project.getId());
			recurse(dependencyTree, new ArrayList<>());

		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private void normalize(final List<Object> r, final HashSet<String> done) {
		for (Iterator<Object> i = r.iterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof Dependency) {
				Dependency dep = (Dependency) o;
				if (!done.add(dep.getId())) {
					i.remove();
				}
			} else if (o instanceof List) {
				List<Object> al = (List<Object>) o;
				normalize(al, done);
				if (al.isEmpty()) {
					i.remove();
				}
			}
		}

	}

	private void recurse(final List<Object> deps, final List<Boolean> level) throws IOException {
		if (deps.isEmpty())
			return;
		Object last = deps.get(deps.size() - 1);
		for (Object o : deps) {
			if (o instanceof Dependency) {
				Dependency dep = (Dependency) o;

				String scope = dep.scope;
				if (scope == null) {
					scope = "compile";
				}

				StringBuilder msg = new StringBuilder();
				for (Boolean x : level) {
					msg.append(x ? "|  " : "   ");
				}
				msg.append(last != o ? "+- " : "\\- ");
				msg.append(dep.getId()).append(":").append(scope);
				write(msg.toString());
				continue;
			}
			if (o instanceof List) {
				List<Object> al = (List<Object>) o;
				level.add(o != last);
				recurse(al, level);
				level.remove(level.size() - 1);
				continue;
			}

		}
	}
}
