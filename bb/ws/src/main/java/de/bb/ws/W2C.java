package de.bb.ws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import de.bb.util.XmlFile;

public class W2C {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
//        String wsdlFileName = args[0];
        String wsdlFileName = "http://127.0.0.1/TAXOR/NavigationManagerService?WSDL";
        
        SD sd;
        if (wsdlFileName.startsWith("http:")) {
            URL url = new URL(wsdlFileName);
            Object content = url.getContent();
            sd = new SD((InputStream)content);
        } else {
            sd = new SD();
            XmlFile xml = new XmlFile();
            xml.readFile(wsdlFileName);
            sd.parseWsdl(xml);
        }
        String pack = "foo2";
        File dir = new File("src/main/java/", pack);
        dir.mkdirs();
        sd.createCode(dir, pack);
    }

}
