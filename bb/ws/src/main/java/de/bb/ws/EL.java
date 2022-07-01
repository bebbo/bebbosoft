package de.bb.ws;

import jakarta.xml.ws.WebServiceException;

import de.bb.util.XmlFile;

class EL {

    String name;
    String type;
    boolean nillable;
    int minOccurs;
    String maxOccurs;
    boolean implicite;

    EL(XmlFile xml, String key, boolean implicite) {
        this(xml, key);
        this.implicite = true;
    }
    
    EL(XmlFile xml, String key) {
        name = xml.getString(key, "name", null);
        type = xml.getString(key, "type", null);
        nillable = "true".equals(xml.getString(key, "nillable", null));
        minOccurs = Integer.parseInt(xml.getString(key, "minOccurs", "1"));
        maxOccurs = xml.getString(key, "maxOccurs", null);
        
        if (name == null || type == null)
            throw new WebServiceException("invalid element: " + key + " name=" + name + " type=" + type);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":").append(type);
        sb.append("[").append(minOccurs);
        if (maxOccurs != null) {
            sb.append("-").append(maxOccurs);
        }
        sb.append("]");
        if (nillable)
            sb.append(" (NUL)");
        
        return sb.toString();
    }
}
