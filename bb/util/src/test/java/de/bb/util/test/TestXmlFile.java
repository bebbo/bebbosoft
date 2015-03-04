package de.bb.util.test;

import java.io.InputStream;
import java.util.Iterator;

import junit.framework.TestCase;

import de.bb.util.XmlFile;

public class TestXmlFile extends TestCase {
    public static void main(String args[]) {
        TestXmlFile t = new TestXmlFile();
        t.testContent1();
//        t.testSearchWithNs();
    }

//    public void testSearchWithNs() {
//        XmlFile xml = new XmlFile();
//        xml.setPreserveWhiteSpaces(true);
//
//        InputStream is = getClass().getClassLoader().getResourceAsStream("testxml.xml");
//        xml.read(is);
//
//        Iterator<String> i = xml.sections("/S:Envelope/S:Body/ns2:subscribeResponse/formFieldDTOs");
//        String key = i.next();
//        String value = xml.getContent(key + "calculatedValue");
//        assertEquals("Übernahme der Vorjahreswerte", value);
//
//    }

    public void testContent1() {
        String content = "<a> le<![CDATA[<>]]>er\r\n<!-- -->space </a>";
        XmlFile xml = new XmlFile();
        xml.setPreserveWhiteSpaces(true);
        xml.readString(content);
        String res = xml.getContent("/a/");
        if (!" le<>er\r\nspace ".equals(res))
            throw new RuntimeException();
        String xs = xml.toString();
        assertEquals(content, xs);
    }
}
