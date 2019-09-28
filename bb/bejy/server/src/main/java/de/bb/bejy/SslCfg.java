/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/SslCfg.java,v $
 * $Revision: 1.11 $
 * $Date: 2014/09/22 09:24:39 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Copyright (c) by Stefan Bebbo Franke 2003.
 *
 */
package de.bb.bejy;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.ArrayList;

import de.bb.security.Asn1;
import de.bb.security.Pem;
import de.bb.security.Pkcs1;
import de.bb.security.Ssl3;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;

/**
 * Configuration class for the SSL support.
 * 
 * @author bebbo
 */
public class SslCfg extends Configurable implements Configurator {
    private byte[][] certs;
    private byte[][] pkData;
    private byte[][] ciphers;

    private final static String PROPERTIES[][] = { { "keyFile", "file name of the RSA key (PKCS 5)" },
            { "certFile", "file name of the X.509 certificate file" },
            { "password", "the password for the keyFile (if any)" }, { "ciphers", "the list of used ciphers" },
            { "name", "the name to refer to this ssl config"} };

    /**
     * Create a new SslCfg.
     */
    public SslCfg() {
        init("ssl", PROPERTIES);
    }

    /**
     * return a new SslCfg, since it is Configurator AND Configurable.
     * 
     * @return a new SslCfg, since it is Configurator AND Configurable.
     * @see de.bb.bejy.Configurator#create()
     */
    public Configurable create() {
        return new SslCfg();
    }

    /**
     * return the description.
     * 
     * @return the description.
     * @see de.bb.bejy.Configurator#getDescription()
     */
    public String getDescription() {
        return "to specify the key file, its password and a certificate file to enable SSL";
    }

    /**
     * return the extension id.
     * 
     * @return the extension id.
     * @see de.bb.bejy.Configurator#getExtensionId()
     */
    public String getExtensionId() {
        return "de.bb.bejy";
    }

    /**
     * return the own id.
     * 
     * @return the own id.
     * @see de.bb.bejy.Configurator#getId()
     */
    public String getId() {
        return "de.bb.bejy.ssl";
    }

    /**
     * return the own name.
     * 
     * @return the own name.
     * @see de.bb.bejy.Configurator#getName()
     */
    public String getName() {
        return "ssl";
    }

    /**
     * return the path.
     * 
     * @return the path.
     * @see de.bb.bejy.Configurator#getPath()
     */
    public String getPath() {
        return "ssl";
    }

    /**
     * return null, since no further modules are required.
     * 
     * @return null, since no further modules are required.
     * @see de.bb.bejy.Configurator#getRequired()
     */
    public String getRequired() {
        return null;
    }

    /**
     * return false, since no dynamic loading is used.
     * 
     * @return false, since no dynamic loading is used.
     * @see de.bb.bejy.Configurator#loadClass()
     */
    public boolean loadClass() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.bb.bejy.Configurable#activate(de.bb.util.LogFile)
     */
    public void activate(LogFile logFile) throws Exception {
        if (pkData != null)
            return;
        
        // load certificate
        byte[] certContent = null;
        FileInputStream fis = null;
        String certFile = getProperty("certFile");
        if (new File(certFile).exists())
            try {
                fis = new FileInputStream(certFile);
                certContent = new byte[fis.available()];
                fis.read(certContent);
            } finally {
                if (fis != null)
                    fis.close();
            }

        byte[] dd = null;
        if (certContent != null)
            dd = Pem.decode(certContent, 0, null);
        if (dd != null)
            certContent = dd;

        if (certContent == null)
            logFile.writeDate("could not load certificate file: " + certFile);

        // split the certificates from file into an array of certificates.
        ArrayList<byte[]> certList = new ArrayList<byte[]>();
        ByteRef br = new ByteRef(certContent);
        while (br.length() > 0) {
            int len = Asn1.getLen(br.toByteArray());
            if (len > br.length()) {
                logFile.writeDate("certificate lengths mismatch in: " + certFile);
                break;
            }
            certList.add(br.substring(0, len).toByteArray());
            br = br.substring(len);
        }
        certs = new byte[certList.size()][];
        certList.toArray(certs);

        // get the signers private key
        String keyFile = getProperty("keyFile");
        byte b[] = null;
        fis = null;
        if (new File(keyFile).exists())
            try {
                fis = new FileInputStream(keyFile);
                b = new byte[fis.available()];
                fis.read(b);
            } finally {
                if (fis != null)
                    fis.close();
            }

        // decrypt it
        if (b != null)
            dd = Pem.decode(b, 0, getProperty("password", ""));
        if (dd != null)
            b = dd;

        // check pwd
        if (b == null) {
            logFile.writeDate("could not load keyfile: " + keyFile);
        }

        // support let'sencrypt format.
        byte[] lec = Asn1.getSeq(b, new int[] {0x90, 0x84, 0x10}, 0);
        if (lec != null)
        	b = lec;
        
        pkData = Pkcs1.getPrivateKey(b);

        /* verify the pkData */
        BigInteger p = new BigInteger(1, pkData[3]);
        BigInteger q = new BigInteger(1, pkData[4]);
        BigInteger e = new BigInteger(1, pkData[1]);
        BigInteger one = BigInteger.ONE;
        BigInteger p1 = p.subtract(one);
        BigInteger q1 = q.subtract(one);
        BigInteger n = p1.multiply(q1);
        BigInteger d = e.modInverse(n);
        n = p.multiply(q);
        BigInteger dp1 = d.mod(p1);
        if (!dp1.equals(new BigInteger(1, pkData[5]))) {
            logFile.writeDate("bogus key file, fixing dp1");
            pkData[5] = dp1.toByteArray();
        }
        BigInteger dq1 = d.mod(q1);
        if (!dq1.equals(new BigInteger(1, pkData[6]))) {
            logFile.writeDate("bogus key file, fixing dq1");
            pkData[6] = dq1.toByteArray();
        }
        BigInteger iqmp = q.modInverse(p);
        if (!iqmp.equals(new BigInteger(1, pkData[7]))) {
            logFile.writeDate("bogus key file, fixing iqmp");
            pkData[7] = iqmp.toByteArray();
        }
    }

    
    @Override
    public void deactivate(LogFile logFile) throws Exception {
        super.deactivate(logFile);
        certs = null;
        pkData = null;
    }

    /**
     * @return
     */
    byte[][] getCerts() {
        return certs;
    }

    /**
     * @return
     */
    byte[][] getKeyData() {
        return pkData;
    }

    public byte[][] getCiphers() {
        if (ciphers != null)
            return ciphers;

        final String sCiphers = getProperty("ciphers");
        ciphers = Ssl3.getCipherSuites(sCiphers);
        return ciphers;
    }

    /**
     * @return
     */
    boolean isValid() {
        return pkData != null && certs != null;
    }

    public boolean hashPassword() {
        return false;
    }
    
//    public static void main(String args[]) {
//    	SslCfg sslcfg = new SslCfg();
//    	sslcfg.setProperty("certFile", "d:\\develop\\workspaces\\w1\\bebbosoft\\bb\\security\\fullchain.pem");
//    	sslcfg.setProperty("keyFile", "d:\\develop\\workspaces\\w1\\bebbosoft\\bb\\security\\privkey.pem");
//    	
//    	LogFile logFile = new LogFile("*");
//		try {
//			sslcfg.activate(logFile);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//    }
}

/******************************************************************************
 * $Log: SslCfg.java,v $
 * Revision 1.11  2014/09/22 09:24:39  bebbo
 * @N added support for SSL host name, to choose certificate and key based on host name
 *
 * Revision 1.10  2014/06/23 19:02:58  bebbo
 * @N added support for startTLS: ssl info is not immediately used
 * @R passwords which are not needed in clear text are now stored via PKDBF2 with SHA256
 * @R added support for groups/roles in groups / dbis
 *
 * Revision 1.9  2013/11/28 12:23:03  bebbo
 * @N SSL cipher types are configurable
 * @I using nio sockets
 * Revision 1.8 2010/12/17 23:25:11 bebbo /FIXED: ssl config now supports multiple certificates
 * Revision 1.7 2007/08/09 16:06:54 bebbo
 * 
 * @I integrated new SSL implementation
 * 
 *    Revision 1.6 2007/04/21 19:13:22 bebbo
 * @R improved speed by using the chinese remainder theorem
 * 
 *    Revision 1.5 2004/12/13 15:28:07 bebbo
 * @D added logfile entries for SSL configuration loading
 * 
 *    Revision 1.4 2004/05/06 10:42:19 bebbo
 * @B fixed possible unclosed streams
 * 
 *    Revision 1.3 2003/10/01 12:01:51 bebbo
 * @C fixed all javadoc errors.
 * 
 *    Revision 1.2 2003/06/20 09:09:32 bebbo
 * @N onine configuration seems to be complete for bejy and http
 * 
 *    Revision 1.1 2003/06/18 15:07:04 bebbo
 * @N added ssl configuration
 * 
 */
