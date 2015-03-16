import java.io.FileInputStream;

import de.bb.security.Asn1;

public class Asn1View {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0 || args.length > 2 || "-?".equals(args[0]) || (args.length ==2 && !"-p".equals(args[1])) ) {
            System.out.println("USAGE: Asn1View <asn1file> [-p]");
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(args[0]);
            int len = fis.available();
            if (len <= 0)
                return;
            byte b[] = new byte[len];
            fis.read(b);
            fis.close();

            StringBuilder sb = Asn1.dump(0, b, 0, b.length, args.length == 2);
            System.out.print(sb);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
