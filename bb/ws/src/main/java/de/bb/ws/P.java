package de.bb.ws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceException;

import de.bb.util.XmlFile;

public class P extends EndpointReference {
    QName name;
    String binding;
    URL url;
    private String style;
    private String transport;
    HashMap<String, O> opMap;

    P(SD sd, XmlFile wsdl, String portKey, String root, String ns) {

        // a port has one binding
        String portName = wsdl.getString(portKey, "name", null);
        name = new QName(sd.targetNamespace, portName);
        binding = wsdl.getString(portKey, "binding", null);
        String location = wsdl.getString(portKey + "soap:address", "location", null);
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            throw new WebServiceException(e);
        }

        int colon = binding.indexOf(':');
        binding = binding.substring(colon + 1);

        // the port references one binding
        String bkey = root + "\\" + ns + "binding\\" + binding;
        String type = wsdl.getString(bkey, "type", null);
        if (type == null)
            throw new WebServiceException("no type for binding " + binding);
        String bskey = bkey + "/soap:binding/";

        style = wsdl.getString(bskey, "style", null); // "document");
        transport = wsdl.getString(bskey, "transport", null); // "http://schemas.xmlsoap.org/soap/http");

        colon = type.indexOf(':');
        type = type.substring(colon + 1);

        String ptypeKey = root + "\\" + ns + "portType\\" + type + "/\\" + ns + "operation\\";

        // the port has many operations
        opMap = new HashMap<String, O>();
        for (String opKey : wsdl.getSections(bkey + "/" + ns + "operation")) {
            O o = new O(sd, wsdl, opKey, ptypeKey, root, ns);
            opMap.put(o.opName, o);
        }
    }

    public String toString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<EndpointReference xmlns=\"http://www.w3.org/2005/08/addressing\">" + "<Address>" + url
                + "</Address></EndpointReference>";
    }

    @Override
    public void writeTo(Result result) {
        throw new UnsupportedOperationException();
    }

    public void createCode(File dir, String pack, SD sd, HashSet<String> used) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("// CREATED BY de.bb.ws.SD. EDIT AT DISCRETION.\r\n");
        if (pack != null)
            sb.append("package " + pack + ";\r\n");
        sb.append("import jakarta.jws.WebMethod;\r\n" + "import jakarta.jws.WebParam;\r\n"
                + "import jakarta.jws.WebResult;\r\n" + "import jakarta.jws.WebService;\r\n"
                + "import jakarta.jws.WebParam.Mode;\r\n" + "import jakarta.xml.ws.Holder;\r\n"
                + "import java.util.Collection;\r\n\r\n");
        sb.append("@WebService\r\n");
        sb.append("public interface " + this.name.getLocalPart() + " {\r\n");
        for (O o : opMap.values()) {
            o.createCode(sd, sb, used);
        }

        sb.append("}");

        File f = new File(dir, this.name.getLocalPart() + ".java");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(sb.toString().getBytes());
        fos.close();
    }

    public O getOperation(Method method) {
        final String name = method.getName();
//        WebMethod wm = method.getAnnotation(WebMethod.class);
//        if (wm != null && wm.operationName() != null)
//            name = wm.operationName();
        O o = opMap.get(name);
        o.init(method);
        return o;
    }
}
