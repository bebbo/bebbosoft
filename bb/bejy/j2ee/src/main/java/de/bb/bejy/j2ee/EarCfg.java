package de.bb.bejy.j2ee;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;

public class EarCfg implements Configurator {

    public String getName() {
        return "ear";
    }

    
    public String getDescription() {
        return "an enterprise application";
    }

    
    public String getPath() {
        return "ear";
    }

    public String getId() {
        return "de.bb.bejy.http.entapps.ear";
    }

    
    public String getExtensionId() {
        return "de.bb.bejy.http.entapps";
    }

    
    public String getRequired() {
        return null;
    }

    
    public Configurable create() {
        return null;
    }

    
    public boolean loadClass() {
        return false;
    }

}
