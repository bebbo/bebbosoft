package de.bb.util.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.junit.Ignore;

import junit.framework.TestCase;
import de.bb.util.MimeFile;
import de.bb.util.MimeFile.Info;

@Ignore
public class TestMime extends TestCase {
    public void testNoMime() throws FileNotFoundException {
        final File file = new File("src/test/resources/06856624ac85deaa289d02c");
        final FileInputStream fis = new FileInputStream(file);
        final ArrayList<Info> mimeFileInfos = MimeFile.parseMime(fis);
        for (final MimeFile.Info info : mimeFileInfos) {
            info.toString();
        }
    }
    public void testUploadMime() throws FileNotFoundException {
        final File file = new File("src/test/resources/upload");
        final FileInputStream fis = new FileInputStream(file);
        final ArrayList<Info> mimeFileInfos = MimeFile.parseMime(fis, "---------------------------104721970024346");
        for (final MimeFile.Info info : mimeFileInfos) {
            info.toString();
        }
    }
}