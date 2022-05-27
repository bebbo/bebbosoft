import de.bb.security.DES3;
import de.bb.util.Mime;


public class A {

    private final static byte[] BEJY_KEY = "bejy_key12345678this_odd".getBytes();
    /**
     * @param args
     */
    public static void main(String[] args) {
        String v = "kVjQEyVCvrGULVlcvMzUUT8gx8k75JQxviIfPuX528Y=";
        DES3 des = new DES3();
        des.setKey(BEJY_KEY);
        byte b[] = v.getBytes();
        b = Mime.decode(b, 0, b.length);
        b = des.decryptCBCAndPadd(new byte[8], b);
        v = new String(b);
        System.out.println(v);
    }

}
