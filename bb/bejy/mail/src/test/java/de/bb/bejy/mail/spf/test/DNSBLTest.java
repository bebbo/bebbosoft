package de.bb.bejy.mail.spf.test;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import de.bb.bejy.Config;
import de.bb.bejy.Dns;
import de.bb.bejy.DnsCfg;
import de.bb.bejy.mail.MailCfg;
import de.bb.bejy.mail.Smtp;
import de.bb.util.LogFile;
import junit.framework.Assert;

public class DNSBLTest {

	private static Dns dns;
	private static LogFile log;

	@BeforeClass
	public static void init() throws Exception {
		log = new LogFile("*");
		dns = MailCfg.getDNS();
		if (dns == null) {
			Config cfg = Config.getInstance();
			dns = (Dns) new DnsCfg().create();
			cfg.addChild("dns", dns);
			dns.activate(log);
		}
		File wl = new File("whitelist.txt");
		if (!wl.exists()) {
			FileOutputStream fos = new FileOutputStream(wl);
			fos.write("mail.*.foo".getBytes());
			fos.write("neelix.permanent.de".getBytes());
			fos.close();
		}
	}

	@Test
	public void testDnsbl1() {
		dns.addStaticEntry(Dns.TYPE_A, "4.3.2.1.dnsbl.test", "127.0.0.2");
		final String result = Smtp.checkDnsbl("1.2.3.4", "testDnsbl1", "dnsbl.test", log);
		Assert.assertNotNull(result);
	}

	@Test
	public void testDnsbl2() {
		dns.addStaticEntry(Dns.TYPE_A, "5.3.2.1.dnsbl1.test", "127.0.0.12");
		dns.addStaticEntry(Dns.TYPE_A, "6.3.2.1.dnsbl1.test", "127.0.0.11");
		dns.addStaticEntry(Dns.TYPE_A, "85.221.154.62.dnsbl1.test", "127.0.0.11");
		final String result5 = Smtp.checkDnsbl("1.2.3.5", "testDnsbl2", "dnsbl.test, dnsbl1.test<12", log);
		Assert.assertNull(result5);

		// ip is blacklisted by dnsbl1.test
		final String result6 = Smtp.checkDnsbl("1.2.3.6", "testDnsbl2", "dnsbl.test, dnsbl1.test<12", log);
		Assert.assertNotNull(result6);

		// ip is still blacklisted but now the resolved domain is white listed
		final String result6b = Smtp.checkDnsbl("1.2.3.6", "mail.12345.foo", "dnsbl.test, dnsbl1.test<12", log);
		Assert.assertNull(result6b);

		final String result7 = Smtp.checkDnsbl("62.154.221.85", "neelix.permanent.de", "dnsbl.test, dnsbl1.test<12",
				log);
		Assert.assertNull(result7);

	}

}