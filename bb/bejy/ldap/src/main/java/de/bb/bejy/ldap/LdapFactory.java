package de.bb.bejy.ldap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.bb.bejy.Factory;
import de.bb.bejy.Protocol;
import de.bb.util.XmlFile;

public class LdapFactory extends Factory {

    private static final File FILE = new File("ldap.xml");
    private XmlFile xml;
    private long lastModified;

    public LdapFactory() {
    }

    @Override
    public Protocol create() throws Exception {
        return new LdapProtocol(this, logFile);
    }

    public XmlFile getXmlFile() {
        long lm = FILE.lastModified();
        if (lm != lastModified || xml == null) {
            lastModified = lm;
            this.xml = new XmlFile();
            xml.readFile(FILE.getAbsolutePath());
        }
        return xml;
    }

    public void saveXmlFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(FILE);
        xml.write(fos);
        fos.close();
    }

    public static String getXmlFileDate() {
        return Long.toHexString(FILE.lastModified());
    }
}
