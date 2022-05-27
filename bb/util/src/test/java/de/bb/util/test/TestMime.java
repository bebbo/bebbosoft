package de.bb.util.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Test;

import de.bb.util.MimeFile;
import de.bb.util.MimeFile.Info;
import junit.framework.TestCase;

public class TestMime extends TestCase {
	@Test
	public void testNoMime() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("/06856624ac85deaa289d02c")) {
			final ArrayList<Info> mimeFileInfos = MimeFile.parseMime(is);
			assertEquals(2, mimeFileInfos.size());
			for (final MimeFile.Info info : mimeFileInfos) {
				info.toString();
			}
		}
	}

	@Test
	public void testUploadMime() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("/upload")) {
			final ArrayList<Info> mimeFileInfos = MimeFile.parseMime(is, "---------------------------104721970024346");
			assertEquals(1, mimeFileInfos.size());
			for (final MimeFile.Info info : mimeFileInfos) {
				info.toString();
			}
		}
	}

	@Test
	public void testParseMime() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("/wiki_4411094263298514547temp")) {
			ArrayList<MimeFile.Info> r = MimeFile.parseMime(is,
					"---------------------------3786578803113637692238906484");
			assertEquals(1, r.size());
			System.out.println(r);
		}
	}
}