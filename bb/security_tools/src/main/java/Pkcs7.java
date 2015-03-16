import de.bb.security.Asn1;
import de.bb.security.Pkcs6;

public class Pkcs7 {

    private static final byte[] NUL = {};

    /**
     * @param args
     */
    public static void main(String[] args) {

        byte[] ek = Asn1.makeASN1("foo", Asn1.OCTET_STRING);
        byte[] recip = Asn1.newSeq;

        byte[] alg = makeAlgorithmIdentifierRSA();
        byte[] part = makeRecipientInfo(recip, alg, ek);

        StringBuilder s = Asn1.dump(0, part, 0, part.length, false);
        System.out.println(s);
    }

    public static byte[] makeAlgorithmIdentifierRSA() {
        byte[] oid = Asn1.makeASN1(Pkcs6.rsaEncryption, Asn1.OBJECT_IDENTIFIER);
        byte[] nul = Asn1.makeASN1(NUL, Asn1.NULL);
        byte[] seq = Asn1.newSeq;
        seq = Asn1.addTo(seq, oid);
        seq = Asn1.addTo(seq, nul);
        return seq;
    }

    /**
     * 
     * @param recipid
     *            distinguished name + serial
     * @return
     */
    public static byte[] makeRecipientInfo(byte[] recipid, byte[] keyEncryptionAlgorithmIdentifier, byte[] encryptedKey) {
        byte[] seq = Asn1.newSeq;
        byte[] aint = Asn1.makeASN1(0, Asn1.INTEGER);
        seq = Asn1.addTo(seq, aint);
        seq = Asn1.addTo(seq, recipid);
        seq = Asn1.addTo(seq, keyEncryptionAlgorithmIdentifier);
        seq = Asn1.addTo(seq, encryptedKey);

        return seq;
    }

}
