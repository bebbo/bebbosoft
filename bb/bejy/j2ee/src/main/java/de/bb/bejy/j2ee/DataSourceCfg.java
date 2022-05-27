package de.bb.bejy.j2ee;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;

public class DataSourceCfg extends Configurable implements Configurator {

    public String getName() {
        return "datasource";
    }

    
    public String getDescription() {
        return "a data source";
    }

    
    public String getPath() {
        return "";
    }

    public String getId() {
        return "de.bb.bejy.j2ee.datasource";
    }

    
    public String getExtensionId() {
        return "de.bb.bejy";
    }

    
    public String getRequired() {
        return null;
    }

    
    public Configurable create() {
        return this;
    }

    
    public boolean loadClass() {
        return false;
    }

}
