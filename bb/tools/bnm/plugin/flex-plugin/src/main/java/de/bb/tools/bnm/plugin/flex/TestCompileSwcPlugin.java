package de.bb.tools.bnm.plugin.flex;

import java.util.ArrayList;

import de.bb.tools.bnm.annotiation.Property;

public class TestCompileSwcPlugin extends AbstractFlexPlugin {

    @Property("${project.build.testSourceDirectory}")
    protected String testSourceDirectory;

    protected String getSourceDirectory() {
        return testSourceDirectory;
    }

    protected String getFinalName() throws Exception {
        return "test-" + super.getFinalName();
    }

    protected String getMainMethod() {
        return "compc";
    }

    protected String getCompiler() {
        return "flex2.tools.Compc";
    }

    protected void extendArgs(ArrayList<String> largs) {

    }

}
