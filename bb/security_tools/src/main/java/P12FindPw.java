import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import de.bb.security.Asn1;
import de.bb.security.Pkcs12;

public class P12FindPw {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream(args[0]);
        int len = fis.available();
        byte b[] = new byte[len];
        fis.read(b);
        fis.close();

        // certificate file
        Asn1 p12 = new Asn1(b);

        // search sequence with 1.2.840.113549.1.7.1
        Outer: for (final Iterator<Asn1> i = p12.children(); i.hasNext();) {
            final Asn1 outMostSequence = i.next();
            if (outMostSequence.getType() == 0x30) {
                for (final Iterator<Asn1> j = outMostSequence.children(); j.hasNext();) {
                    final Asn1 oid = j.next();
                    if (oid.getType() == Asn1.OBJECT_IDENTIFIER
                            && "1.2.840.113549.1.7.1".equals(Asn1.oid2String(oid.asByteRef().toByteArray()))) {
                        final Asn1 innerSeq = j.next().children().next().children().next().children().next();
                        for (final Iterator<Asn1> k = innerSeq.children(); k.hasNext();) {
                            final Asn1 oid2 = k.next();
                            if (oid2.getType() == Asn1.OBJECT_IDENTIFIER
                                    && "1.2.840.113549.1.7.6".equals(Asn1.oid2String(oid2.asByteRef().toByteArray()))) {
                                final Asn1 s1 = k.next();
                                final Iterator<Asn1> s2 = s1.children().next().children();
                                s2.next();
                                final Asn1 keySeq = s2.next();
                                final Iterator<Asn1> s3 = keySeq.children();
                                s3.next(); //oid

                                final Asn1 infoSeq = s3.next();
                                final Iterator<Asn1> s4 = infoSeq.children();
                                s4.next();
                                final Asn1 info = s4.next();
                                
                                final Asn1 data = s3.next();

                                final byte binfo[] = infoSeq.toByteArray();
                                final byte bdata[] = data.asByteRef().toByteArray();

                                final String pwd = "xxx";

                                byte[] r = Pkcs12.pkcs12decrypt(pwd, bdata, binfo);

                                System.out.println(new Asn1(r));

                                break Outer;
                            }
                        }
                    }
                }
            }
        }

        int p1[] = { 0x90, 0x90, 0x80, 0x84 };
        // outer blob
        byte a0[] = Asn1.getSeq(b, p1, 0);

        //inner blob
        byte[] ax = Asn1.getSeq(a0, p1, 0);

        if (ax == null) {
            int p2[] = { 0x90, 0x10, 0x90, 0x80, 0x84 };
            ax = Asn1.getSeq(a0, p2, 0);
        }

        Asn1 a1 = new Asn1(ax);

        for (Iterator<Asn1> i = a1.children(); i.hasNext();) {
            Asn1 seq = i.next();

            Iterator<Asn1> children = seq.children();
            Asn1 firstChild = children.next();
            if (firstChild.getType() == Asn1.OBJECT_IDENTIFIER
                    && firstChild.toString().indexOf("1.2.840.113549.1.12.10.1.2") > 0) {
                Asn1 s1 = children.next(); // a0
                Asn1 bag = s1.children().next();

                byte bagData[] = bag.asByteRef().toByteArray();

                int[] pinfo = { 0x10 };
                byte binfo[] = Asn1.getSeq(bagData, pinfo, 0);

                int[] pdata = { 0x84 };
                byte bdata[] = Asn1.getSeq(bagData, pdata, 0);

                for (int n = 0; n < 1000000; ++n) {
                    String pwd = Integer.toString(n);
                    while (pwd.length() < 6) {
                        pwd = "0" + pwd;
                    }
                    try {
                        byte[] r = Pkcs12.pkcs12decrypt(pwd, bdata, binfo);
                        if (r != null) {
                            Asn1 result = new Asn1(r);
                            if (r[0] == 0x30 && result.getLength() == r.length) {
                                System.out.println("pw=" + pwd);
                                System.out.println(result);
                            }
                        }
                    } catch (Exception ex) {

                    }
                }
            }
        }

    }

}
