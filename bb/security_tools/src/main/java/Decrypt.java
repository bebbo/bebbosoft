import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.bb.security.AES;
import de.bb.security.Pkcs12;
import de.bb.security.SHA;
import de.bb.util.Misc;

public class Decrypt {

    private final static byte ID[] = {'B', 'E', 'N', 'C', 'R', 'Y', 'P', 'T', 0, 0, 0, 0, 0, 0, 0, 1};

    /**
     * @param args
     */
    public static void main(String[] args) {
        writeln("Decrypt $Revision: 1.1 $");
        if (args.length == 0) {
            writeln("usage: Encrypt [-p <password>] <infile> [<outfile>=<infile> without .enc]");
            return;
        }
        int argi = 0;
        String password = null;
        while (args[argi].charAt(0) == '-') {
            if (args[argi].equalsIgnoreCase("-p")) {
                password = args[++argi];
            } else {
                writeln("invalid option: " + args[argi]);
                return;
            }
            ++argi;
        }

        String ifn = args[argi++];
        String ofn = ifn;
        if (ofn.endsWith(".enc"))
            ofn = ofn.substring(0, ofn.length() - 4);
        if (args.length > argi)
            ofn = args[argi++];

        if (password == null) {
            // read password
            System.out.print("password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                password = br.readLine();
            } catch (IOException e) {
                writeln("no password");
                return;
            }
        }

        writeln("decrypting: " + ifn + " -> " + ofn);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File f = new File(ifn);
            fis = new FileInputStream(f);
            ifn = f.getName();
            fos = new FileOutputStream(ofn);

            SHA sha = new SHA();
            AES aes = new AES();

            byte id[] = new byte[ID.length];
            fis.read(id);
            if (!Misc.equals(id, ID)) {
                throw new Exception("not a BebboSoft encrypt file!");
            }
            byte seed[] = new byte[32];
            fis.read(seed);

            // create stuff
            byte key[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 0, 32);
            byte iv[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 1, 32);
            byte mac[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 2, 32);

            aes.setKey(key);

            byte mac2[] = new byte[32];
            fis.read(mac2);
            aes.decryptCBC(iv, mac2, mac2);
            if (!Misc.equals(mac, mac2)) {
                throw new Exception("wrong password");
            }

            byte buffer[] = new byte[8192];
            for (;;) {
                if (fis.available() < 8192) {
                    buffer = new byte[fis.available()];
                }
                int len = fis.read(buffer);
                if (len < 8192) {
                    buffer = aes.decryptCBCAndPadd(iv, buffer);
                } else {
                    aes.decryptCBC(iv, buffer, buffer);
                }
                fos.write(buffer);
                if (len < 8192)
                    break;
            }

        } catch (Exception e) {
            writeln(e.getMessage());
        }
        try {
            if (fis != null)
                fis.close();
        } catch (IOException e) {
        }
        try {
            if (fos != null)
                fos.close();
        } catch (IOException e) {
        }
    }

    static void writeln(String s) {
        System.out.println(s);
    }
}
