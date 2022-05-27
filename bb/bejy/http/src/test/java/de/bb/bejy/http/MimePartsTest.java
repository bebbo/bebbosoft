package de.bb.bejy.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.Part;

import org.junit.Test;

import de.bb.io.FastByteArrayInputStream;
import de.bb.io.FastByteArrayOutputStream;
import de.bb.io.IOUtils;
import de.bb.util.MimeFile;

public class MimePartsTest {

    private String partsData = "-----------------------------57601684829797\r\n" + 
            "Content-Disposition: form-data; name=\"formmailer_recipient\"\r\n" + 
            "\r\n" + 
            "gensek#spun.de;mail#henningblunck.de;pl#spun.de;dr#jovanovic.de\r\n" + 
            "-----------------------------57601684829797\r\n" + 
            "Content-Disposition: form-data; name=\"formmailer_subject\"\r\n" + 
            "\r\n" + 
            "Formular von SPUN.de: Seminaranmeldung\r\n" + 
            "-----------------------------57601684829797\r\n" + 
            "Content-Disposition: form-data; name=\"formmailer_name\"\r\n" + 
            "\r\n" + 
            "seminaranmeldung\r\n" + 
            "-----------------------------57601684829797\r\n" + 
            "Content-Disposition: form-data; name=\"name\"\r\n" + 
            "\r\n" + 
            "ali\r\n" + 
            "-----------------------------57601684829797\r\n" + 
            "Content-Disposition: form-data; name=\"vorname\"\r\n" + 
            "\r\n" + 
            "mente\r\n" + 
            "-----------------------------57601684829797--"
            ;
    private String boundary = "---------------------------57601684829797";

    private static String getValue(Part part) throws Exception {
        final InputStream is = part.getInputStream();
        final FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
        IOUtils.copy(is, bos, (int)part.getSize());
        return bos.toString();
    }

    @Test
    public void test1() throws Exception {
        final InputStream is = new FastByteArrayInputStream(
                partsData.getBytes());
        final ArrayList<MimeFile.Info> mimeInfos = MimeFile.parseMime(is, boundary);
        is.close();

        File partsFile = new File("foo");
        String cType = "ct";
        HashMap<String, Part> parts = new HashMap<String, Part>();
        for (final MimeFile.Info mimeInfo : mimeInfos) {
            final Part p = new MimePart(null , partsData.getBytes(), cType, mimeInfo);
            parts.put(p.getName().toLowerCase(), p);
            String v = getValue(p);
            v.toString();
        }
    }
}
