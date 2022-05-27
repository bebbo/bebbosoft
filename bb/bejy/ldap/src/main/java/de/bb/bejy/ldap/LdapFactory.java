package de.bb.bejy.ldap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import de.bb.bejy.Config;
import de.bb.bejy.Factory;
import de.bb.bejy.Protocol;
import de.bb.bejy.Version;
import de.bb.util.ByteRef;
import de.bb.util.LRUCache;
import de.bb.util.XmlFile;

public class LdapFactory extends Factory {

	private static final File FILE = new File("ldap.xml");
	private XmlFile xml;
	private long lastModified;
	private Runnable safer;
	static final LRUCache<ByteRef, ArrayList<byte[]>> CACHE = new LRUCache<ByteRef, ArrayList<byte[]>>();

	private final static String version;

	static {
		version = Version.getShort() + " LDAP " + V.V + " (c) 2000-" + V.Y
				+ " by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
	}

	static HashMap<ByteRef, ByteRef> L2C = new HashMap<ByteRef, ByteRef>();

	public static String getVersion() {
		return V.V;
	}

	public String getFullVersion() {
		return version;
	}

	public LdapFactory() {
		XmlFile xml = getXmlFile();

		xml.setString("/ldap/vendorversion", "name", getVersion());

		// get all predefined names for correct upper/lower case
		for (final String key : xml.getSections("/ldap/\\cn\\schema/attributetypes")) {
			String val = xml.getString(key, "name", "");
			int namePos = val.indexOf("NAME");
			if (namePos < 0)
				continue;
			val = val.substring(namePos + 4).trim();
			if (val.startsWith("(")) {
				int ket = val.indexOf(')');
				val = val.substring(1, ket);
			} else {
				int quote = val.indexOf('\'', 1);
				val = val.substring(0, quote + 1);
			}
			for (final StringTokenizer st = new StringTokenizer(val); st.hasMoreTokens();) {
				String name = st.nextToken();
				name = name.substring(1, name.length() - 1);
				L2C.put(new ByteRef(name.toLowerCase()), new ByteRef(name));
			}
		}

	}

	@Override
	public Protocol create() throws Exception {
		return new LdapProtocol(this, logFile);
	}

	public synchronized XmlFile getXmlFile() {
		long lm = FILE.lastModified();
		if (lm != lastModified || xml == null) {
			lastModified = lm;
			this.xml = new XmlFile();
			xml.readFile(FILE.getAbsolutePath());
			xml.setEncoding("utf-8");
			CACHE.clear();
		}
		return xml;
	}

	public synchronized void saveXmlFile() throws IOException {
		synchronized (this) {
			if (safer != null)
				return;

			safer = new Runnable() {
				public void run() {
					synchronized (LdapFactory.this) {
						try {
							FileOutputStream fos = new FileOutputStream(FILE);
							xml.write(fos);
							fos.close();
							lastModified = FILE.lastModified();
						} catch (IOException e) {
						} finally {
							safer = null;
						}
					}
				}
			};
			Config.getCron().runIn("save ldap.xml", safer, 1000);
		}
	}

	public static String getXmlFileDate() {
		return Long.toHexString(FILE.lastModified());
	}
}
