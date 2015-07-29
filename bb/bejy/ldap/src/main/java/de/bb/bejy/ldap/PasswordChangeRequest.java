package de.bb.bejy.ldap;

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;

import de.bb.security.Asn1;

/**
 * Implementation to change password via LDAP
 * 
 * @author stefan franke
 * 
 */
public class PasswordChangeRequest implements ExtendedRequest {
    private static final long serialVersionUID = 1L;
    public final static String PASSWORDCHANGE = "1.3.6.1.4.1.4203.1.11.1";
    private String userName;
    private String oldPass;
    private String newPass;

    public PasswordChangeRequest(String userName, String oldPass, String newPass) {
        this.userName = userName;
        this.oldPass = oldPass;
        this.newPass = newPass;
    }

    public String getID() {
        return PASSWORDCHANGE;
    }

    /**
     * Create a sequence containing userName, old password and new password.
     */
    public byte[] getEncodedValue() {
        final byte[] uid = Asn1.makeASN1(userName, 0x80);
        final byte[] opw = Asn1.makeASN1(oldPass, 0x81);
        final byte[] npw = Asn1.makeASN1(newPass, 0x82);
        byte[] r = Asn1.newSeq;
        r = Asn1.addTo(r, uid);
        r = Asn1.addTo(r, opw);
        r = Asn1.addTo(r, npw);
        return r;
    }

    public ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset, int length)
            throws NamingException {
        return null;
    }

}
