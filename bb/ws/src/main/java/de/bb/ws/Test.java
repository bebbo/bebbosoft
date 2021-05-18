package de.bb.ws;

import java.io.InputStream;
import java.net.URL;

import de.bb.net.HttpURLConnection;
import de.bb.util.Mime;

public class Test {

    final static String C2 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:ns0=\"prt:service:com.sapportals.portal.prt.webservice.usermanagement.UMWebService\""
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + " <soapenv:Body>"
            + "  <ns0:searchUsers>"
            + "    <searchUsers_4_param14 soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xsi:type=\"xsd:string\">*</searchUsers_4_param14>"
            + "    <searchUsers_4_param15 soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xsi:type=\"xsd:int\">1</searchUsers_4_param15>"
            + "    <searchUsers_4_param16 soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xsi:type=\"xsd:boolean\">true</searchUsers_4_param16>"
            + "    <searchUsers_4_param17 soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xsi:type=\"xsd:int\">11</searchUsers_4_param17>"
            + "  </ns0:searchUsers>" + " </soapenv:Body>" + "</soapenv:Envelope>";

    final static String C3 = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:tns=\"prt:service:com.sapportals.portal.prt.webservice.usermanagement.UMWebService\" "
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " + "<SOAP-ENV:Body> " + "<tns:searchGroups> "
            + "    <searchGroups_4_param25>*</searchGroups_4_param25>"
            + "    <searchGroups_4_param26>1</searchGroups_4_param26>"
            + "    <searchGroups_4_param27>true</searchGroups_4_param27>"
            + "    <searchGroups_4_param28>11</searchUsers_4_param28>" + "</tns:searchGroups> " + "</SOAP-ENV:Body> "
            + "</SOAP-ENV:Envelope>";

    final static String COK = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:tns=\"prt:service:com.sapportals.portal.prt.webservice.usermanagement.UMWebService\" "
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " + "<SOAP-ENV:Body> " + "<tns:searchUsers> "
            + "    <searchUsers_4_param14>*</searchUsers_4_param14>"
            + "    <searchUsers_4_param15>1</searchUsers_4_param15>"
            + "    <searchUsers_4_param16>true</searchUsers_4_param16>"
            + "    <searchUsers_4_param17>100</searchUsers_4_param17>" + "</tns:searchUsers> " + "</SOAP-ENV:Body> "
            + "</SOAP-ENV:Envelope>";

    final static String C = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
        + " xmlns:tns=\"prt:service:com.sapportals.portal.prt.webservice.usermanagement.UMWebService\" "
        + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " + "<SOAP-ENV:Body> " + "<tns:getRolesOfUser> "
        + "    <getRolesOfUser_2_param21>true</getRolesOfUser_2_param21>"
        + "    <getRolesOfUser_2_param20>duke</getRolesOfUser_2_param20>"
        + "</tns:getRolesOfUser> " + "</SOAP-ENV:Body> "
        + "</SOAP-ENV:Envelope>";

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // URL url = new URL("http://sabatina.ikornet.de:57000/irj/servlet/prt/soap/UMWebService?style=rpc_enc");
            URL url = new URL("http://sabatina.ikornet.de:57000/irj/servlet/prt/soap/UMWebService");

            HttpURLConnection con = new HttpURLConnection(url);
            String val = "Basic " + new String(Mime.encode(("duke:test1234").getBytes()));
            con.addRequestProperty("Authorization", val);
            con.addRequestProperty("SOAPAction", "");
            con.addRequestProperty("Content-Type", "text/xml");

            con.setDoOutput(true);
            byte[] data = C.getBytes();
            con.addRequestProperty("Content-Length", Integer.toString(data.length));
            con.getOutputStream().write(data);
            InputStream is = con.getInputStream();

            int ctlen = con.getHeaderFieldInt("CONTENT-LENGTH", -1);
            if (ctlen == 0) {
                if ("close".equals(con.getHeaderField("CONNECTION")))
                    ctlen = -1;
            }
            String content = SD.readFully(is, ctlen);
            System.out.println(content);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
