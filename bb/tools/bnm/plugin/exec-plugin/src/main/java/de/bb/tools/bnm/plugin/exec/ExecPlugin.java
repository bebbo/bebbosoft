package de.bb.tools.bnm.plugin.exec;

import de.bb.tools.bnm.AbstractPlugin;
import de.bb.tools.bnm.Loader;
import de.bb.tools.bnm.annotiation.Config;

public class ExecPlugin extends AbstractPlugin {

    @Config("target")
    private String target;

    @Override
    public void execute() throws Exception {
        if (target == null)
            throw new Exception("plugin has no target in configuration");

        Loader loader = project.getLoader();
    }

}
