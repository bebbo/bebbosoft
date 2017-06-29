package de.bb.bejy.mail.spf.test;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import de.bb.bejy.Config;
import de.bb.bejy.Dns;
import de.bb.bejy.DnsCfg;
import de.bb.bejy.mail.MailCfg;
import de.bb.bejy.mail.spf.SpfContext;
import de.bb.util.LogFile;

public class SpfTest {

    private static Dns dns;

    @BeforeClass
    public static void init() throws Exception {
		dns = MailCfg.getDNS();
		if (dns == null) {
			Config cfg = Config.getInstance();
			dns = (Dns) new DnsCfg().create();
			cfg.addChild("dns", dns);
	        LogFile log = new LogFile("*");
			dns.activate(log);
		}
        dns.setProperty("defaultSpf", "v=spf1 +a:%{p1r}.%{p2} a:%{p1r}.netcup.net +a +mx -all");
    }

    @Test
    public void testDns1() {
        dns.addStaticEntry(Dns.TYPE_A, "smtp.netcup.net", "46.38.225.170");
        int r = SpfContext.validateSpf("www-data@s4325.netcup.net", "franke.ms", "46.38.225.170", "smtp.netcup.net",
                "s4325.netcup.net");
        Assert.assertEquals('+', r);
    }

    @Test
    public void testDns2() {
        dns.addStaticEntry(Dns.TYPE_TXT, "yahoo.de", "v=spf1 a:%{p2l}.mail.%{p3} a:%{p1r}.mail.%{p3} mx -all");
        dns.addStaticEntry(Dns.TYPE_A, "nm13-vm0.bullet.mail.ukl.yahoo.com", "217.146.183.248");
        int r = SpfContext.validateSpf("s_o_m_e_o_n_e@yahoo.de", "franke.ms", "217.146.183.248",
                "nm13-vm0.bullet.mail.ukl.yahoo.com", "nm13-vm0.bullet.mail.ukl.yahoo.com");
        Assert.assertEquals('+', r);
    }

    @Test
    public void testDns3() {
        dns.addStaticEntry(
                Dns.TYPE_TXT,
                "dawanda.com",
                "v=spf1 include:_spf.google.com include:emsmtp.com include:emarsys.net mx a:cookie1.dawanda.com a:cookie2.dawanda.com a:cookie3.dawanda.com a:cookie4.dawanda.com a:cookie5.dawanda.com a:cesar1.dawanda.com a:cesar2.dawanda.com a:dawanda.biz ~all");
        dns.addStaticEntry(Dns.TYPE_A, "cesar1.dawanda.com", "46.231.176.107");
        int r = SpfContext.validateSpf("dawanda@dawanda.com", "franke.ms", "46.231.176.107", "cesar1.dawanda.com",
                "mailer1.dawanda.com");
        Assert.assertEquals('+', r);
    }

    @Test
    public void testDns4() {
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com", "mailru-verification: 71ab435de908d6ed");
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com",
                "v=spf1 ip4:199.255.192.0/22 ip4:199.127.232.0/22 ip4:54.240.0.0/18 ~all");
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com",
                "spf2.0/pra ip4:199.255.192.0/22 ip4:199.127.232.0/22 ip4:54.240.0.0/18 ~all");
        int r = SpfContext.validateSpf("000001421a512aca-3d0774f8-ffdf-4329-97fd-c3e4390aee7d-000000@amazonses.com",
                "taxor.de", "54.240.10.168", "a10-168.smtp-out.amazonses.com", "a10-168.smtp-out.amazonses.com");
        Assert.assertEquals('+', r);
    }

    @Test
    public void testDns5() {
        dns.addStaticEntry(Dns.TYPE_TXT, "yelp.com",
                "spf2.0/pra  a:smtplow.yelpcorp.com a:smtphigh.yelpcorp.com a:smtpdeferred.yelpcorp.com a:admin.yelpcorp.com a:bastion.yelpcorp.com a:mailout.yelpcorp.com include:salesforce.com include:_spf.google.com include:amazonses.com -all");
        dns.addStaticEntry(Dns.TYPE_TXT, "yelp.com",
                "v=spf1 a:smtplow.yelpcorp.com a:smtphigh.yelpcorp.com a:smtpdeferred.yelpcorp.com a:admin.yelpcorp.com a:bastion.yelpcorp.com a:mailout.yelpcorp.com include:salesforce.com include:_spf.google.com include:amazonses.com -all");
        int r = SpfContext.validateSpf("no-reply@yelp.com",
                "bejy.net", "199.255.189.142", "smtphigh.yelpcorp.com", "smtphigh.yelpcorp.com");
        Assert.assertEquals('+', r);
    }
    
    
    @Test
    public void testDns6() {
        dns.addStaticEntry(Dns.TYPE_TXT, "snapfish.de",
        "v=spf1 mx ptr a:mta.snapfish.com a:mx1asp.albumprinter.com a:mx2asp.albumprinter.com ip4:74.122.80.0/21 ip4:63.145.232.0/26 ip4:64.147.178.0/23 ip4:64.147.181.0/24 ip4:64.147.182.0/24 ip4:131.124.0.0/20 ip4:145.7.6.1 ip4:80.237.132.201 ~all");
        dns.addStaticEntry(Dns.TYPE_TXT, "snapfish.de",
                "google-site-verification=OU77ywbG2qG5xc1va6fY2taFEIiVLRRyr8TlKB34b2c");
        int r = SpfContext.validateSpf("donotreply@snapfish.de",
                "bejy.net", "131.124.12.20", "snat-131-124-12-20.snapfish.com", "sfsmtp1.op-zone1.aus1");
        Assert.assertEquals('+', r);
    }
    
    @Test
    public void testDns7() throws Exception {
        dns.addStaticEntry(Dns.TYPE_TXT, "riotgames.com", "v=spf1 a mx ptr include:bounce.exacttarget.com include:exacttarget.com include:_spf.google.com include:mailgun.org ip4:198.62.201.0/24 ip4:64.79.159.194/30 ip4:64.79.156.66/30 ip4:12.249.95.222/30 ip4:209.133.52.192/26" 
    + " ip4:209.66.64.162/30 ip4:12.89.226.214/30 ip4:68.188.46.160/29 ip4:75.128.160.80/28 ip4:83.71.194.0/27 ip4:88.255.192.64/28 ip4:112.220.23.8/29 ip4:195.138.206.208/29"  +
                " ip4:208.184.124.144/28 ip4:211.27.52.66/30 ip4:175.45.107.146/30 ip4:179.184.142.240/29 ip4:223.197.70.48/28 ip4:192.64.175.240/28 ip4:69.31.114.115" + 
    "ip4:199.255.192.0/22 ip4:199.127.232.0/22 ip4:54.240.0.0/18  ~all");
        int r = SpfContext.validateSpf("accounts@riotgames.com", "franke.ms", "54.240.10.130", "a10-130.smtp-out.amazonses.com", "a10-130.smtp-out.amazonses.com");
        Assert.assertEquals('+', r);
    }
    
    @Test
    public void testDns8() throws Exception {
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com", "v=spf1 ip4:199.255.192.0/22 ip4:199.127.232.0/22 ip4:54.240.0.0/18 -all"); 
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com", "spf2.0/pra ip4:199.255.192.0/22 ip4:199.127.232.0/22 ip4:54.240.0.0/18 -all"); 
        dns.addStaticEntry(Dns.TYPE_TXT, "amazonses.com", "mailru-verification: 71ab435de908d6ed"); 
        int r = SpfContext.validateSpf("0000014af9bf958d-4dd6fcb8-d1e2-4d10-9514-7dd8110f75dd-000000@amazonses.com", "franke.ms", "54.240.10.186", "a10-186.smtp-out.amazonses.com", "a10-186.smtp-out.amazonses.com");
        Assert.assertEquals('+', r);
    }
}
