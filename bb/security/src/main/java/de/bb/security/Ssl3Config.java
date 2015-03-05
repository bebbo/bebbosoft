package de.bb.security;

import java.math.BigInteger;
import java.util.HashMap;

import de.bb.security.Ssl3Server.SID;
import de.bb.util.SessionManager;

public class Ssl3Config {
    // for session reuse
    private static SessionManager<SID, byte[]> sessions = new SessionManager<SID, byte[]>(60 * 60 * 1000L);

    private HashMap<String, byte[][]> host2KeyData = new HashMap<String, byte[][]>();
    private HashMap<String, byte[][]> host2Certificates = new HashMap<String, byte[][]>();

    private byte[][] ciphersuites;

    public Ssl3Config(byte[][] certificates, byte[][] pkData, byte[][] ciphersuites) {
        this.ciphersuites = ciphersuites;

        addHostData("*", certificates, pkData);
    }

    private static byte[][] checkKeyData(byte[][] pkData) {
        if (pkData.length == 10)
            return pkData;
        if (pkData.length != 8)
            throw new RuntimeException("8 or 10 values are required in pkConfig");
        byte[][] t = new byte[10][];
        for (int i = 0; i < 8; ++i) {
            t[i] = pkData[i];
        }
        // create the diffie hellman primes
        SecureRandom secureRnd = SecureRandom.getInstance();
        int nlen = pkData[0].length - (pkData[0][0] == 0 ? 1 : 0);
        byte p[] = new BigInteger(nlen * 8, 100, secureRnd).toByteArray();
        byte y[] = new byte[nlen];
        System.arraycopy(p, p.length - y.length, y, 0, y.length);

        byte g[] = new byte[nlen];
        secureRnd.nextBytes(g);
        g[0] &= 0x7f;
        g[nlen - 1] |= 1;

        t[8] = p;
        t[9] = g;
        return t;
    }

    public void addHostData(String hostName, byte[][] certificates, byte[][] pkData) {
        pkData = checkKeyData(pkData);

        host2Certificates.put(hostName, certificates);
        host2KeyData.put(hostName, pkData);
    }

    /**
     * 10 byte arrays with the private key data: n, e, d, p, q, dp1, dq1, iqmp, dhp, dhg
     * 
     * @param hostname
     * @return
     */
    byte[][] getKeyData(String hostname) {
        byte[][] r = host2KeyData.get(hostname);
        if (r == null)
            r = host2KeyData.get("*");
        return r;
    }

    /**
     * Get the certificates for the hostname.
     * 
     * @param hostname
     * @return
     */
    byte[][] getCertificates(String hostname) {
        byte[][] r = host2Certificates.get(hostname);
        if (r == null)
            r = host2Certificates.get("*");
        return r;
    }

    public byte[][] getCiphersuites() {
        return ciphersuites;
    }

    public byte[] findSessionData(SID clientSessionId) {
        return sessions.get(clientSessionId);
    }

    public static void dropSession(SID clientSessionId) {
        sessions.remove(clientSessionId);
    }

    public static void createSession(SID sid, byte[] masterSecret) {
        sessions.put(sid, masterSecret);
    }

    /**
     * Set the session timeout for SSL reuse.
     * 
     * @param max
     *            the new used timeout in ms.
     */
    public static void setMaxHold(int max) {
        if (max < 0)
            max = 0;
        if (max > 1000 * 60 * 60 * 24)
            max = 1000 * 60 * 60 * 24;
        sessions.setTimeout(max);
    }

    /**
     * Set the max count of SSL sessions.
     * 
     * @param max
     *            the new maximum of used sessions.
     */
    public static void setMaxSessions(int max) {
        if (max < 0)
            max = 0;
        sessions.setMaxCount(max);
    }

}
