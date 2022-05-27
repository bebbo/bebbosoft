package de.bb.bejy.j2ee;

import de.bb.bejy.Factory;
import de.bb.bejy.Protocol;

public class RmiProtocol extends Protocol {

    protected RmiProtocol(Factory f) {
        super(f);
    }

    protected boolean doit() throws Exception {
        Handler handler = new Handler();
        handler.setOutputStream(getOs());
        handler.setInputStream(getIs());

        handler.run();

        return false;
    }

    protected boolean trigger() throws Exception {
        // we want untouched streams
        return true;
    }

}
