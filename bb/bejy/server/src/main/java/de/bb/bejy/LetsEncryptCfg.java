package de.bb.bejy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.bb.io.IOUtils;
import de.bb.security.Asn1;
import de.bb.util.LogFile;
import de.bb.util.Mime;

public class LetsEncryptCfg extends Configurable implements Configurator {

	private final static String PROPERTIES[][] = {
			{ "path", "path to let's encrypt folder", "/etc/letsencrypt/live/" },
			{ "ciphers", "the list of used ciphers" } };

	public LetsEncryptCfg() {
		init("letsencrypt", PROPERTIES);
	}

	@Override
	public String getDescription() {
		return "Create SslCfg entries for all letsencrypt certificates";
	}

	@Override
	public String getExtensionId() {
		return "de.bb.bejy";
	}

	@Override
	public String getRequired() {
		return null;
	}

	@Override
	public Configurable create() {
		return this;
	}

	@Override
	public boolean loadClass() {
		return false;
	}

	@Override
	public String getId() {
		return "de.bb.bejy.letsencrypt";
	}

	@Override
	public String getName() {
		return "letsencrypt";
	}

	@Override
	public String getPath() {
		return "letsencrypt";
	}

	@Override
	public void activate(final LogFile logFile) throws Exception {
		final File dir = new File(getProperty("path"));
		final File[] files = dir.listFiles();
		if (files == null)
			return;
		for (final File folder : files) {
			if (!folder.isDirectory())
				continue;

			logFile.writeDate("letsencrypt: loading key/cert for: " + folder.getName());

			final Config config = Config.getInstance();

			try {

				final File cert = new File(folder, "cert.pem");
				final ArrayList<String> domains = loadDomains(cert);
				for (final String domain : domains) {
					logFile.writeDate("letsencrypt: use " + folder.getName() + " for " + domain);

					final SslCfg sslcfg = new SslCfg() {
						@Override
						public String getPath() {
							return "#";
						}
					};
					sslcfg.setProperty("name", domain);
					sslcfg.setProperty("keyFile", new File(folder, "privkey.pem").getAbsolutePath());
					sslcfg.setProperty("certFile", new File(folder, "fullchain.pem").getAbsolutePath());
					sslcfg.setProperty("ciphers", getProperty("ciphers"));
					config.addChild("ssl", sslcfg);
					sslcfg.activate(logFile);
				}

			} catch (final Exception ex) {
				logFile.writeDate("letsencrypt: " + ex.getMessage());
			}
		}
	}

	private static int FOR_PATH[] = { 0x90, 0x90, 0x83 };

	private static ArrayList<String> loadDomains(final File cert) throws IOException {
		byte[] certData = IOUtils.readFile(cert);
		final byte[] decode = Mime.searchDecode(certData, 0);
		if (decode != null)
			certData = decode;

		final byte[] forBlock = Asn1.getSeq(certData, FOR_PATH, 0);

		final byte[] domainOid = Asn1.makeASN1(Asn1.string2Oid("2.5.29.17"), Asn1.OBJECT_IDENTIFIER);
		final int domainSequenceOffset = Asn1.searchSequence(forBlock, domainOid);

		final int domainOffset = domainSequenceOffset + Asn1.getHeaderLen(forBlock, domainSequenceOffset)
				+ domainOid.length;
		Asn1 a1 = (Asn1) new Asn1(forBlock, domainOffset).children();
		a1 = (Asn1) a1.children();

		final ArrayList<String> r = new ArrayList<String>();
		while (a1.hasNext()) {
			final Asn1 d = a1.next();
			final String domain = d.getContent().toString();
			// System.out.println(domain);
			r.add(domain);
		}
		return r;
	}

	// public static void main(final String[] args) throws IOException {
	// loadDomains(new File("cert.pem"));
	// }
}
