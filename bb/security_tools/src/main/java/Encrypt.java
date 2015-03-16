import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.bb.security.AES;
import de.bb.security.Pkcs12;
import de.bb.security.SHA;
import de.bb.security.SecureRandom;

public class Encrypt {

    private final static byte ID[] = {'B', 'E', 'N', 'C', 'R', 'Y', 'P', 'T', 0, 0, 0, 0, 0, 0, 0, 1};

    /**
     * @param args
     */
    public static void main(String[] args) {
        writeln("Encrypt $Revision: 1.1 $");
        if (args.length == 0) {
            writeln("usage: Encrypt [-p  <password>] <infile> [<outfile>=<infile>.enc]");
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
        String ofn = ifn + ".enc";
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

        writeln("encrypting: " + ifn + " -> " + ofn);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File f = new File(ifn);
            fis = new FileInputStream(f);
            ifn = f.getName();
            fos = new FileOutputStream(ofn);

            SecureRandom.addSeed(System.currentTimeMillis());
            SecureRandom r = SecureRandom.getInstance();

            SHA sha = new SHA();
            AES aes = new AES();

            byte seed[] = new byte[32];
            // create some seed
            r.nextBytes(seed);

            // create stuff
            byte key[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 0, 32);
            byte iv[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 1, 32);
            byte mac[] = Pkcs12.pkcs12Gen(password, seed, 32768, sha, 2, 32);

            aes.setKey(key);

            fos.write(ID);
            fos.write(seed);

            aes.encryptCBC(iv, mac, mac);
            fos.write(mac);

            byte buffer[] = new byte[8192];
            for (;;) {
                if (fis.available() < 8192) {
                    buffer = new byte[fis.available()];
                }
                int len = fis.read(buffer);
                if (len < 8192) {
                    buffer = aes.encryptCBCAndPadd(iv, buffer);
                } else {
                    aes.encryptCBC(iv, buffer, buffer);
                }
                fos.write(buffer);
                if (len < 8192)
                    break;
            }

        } catch (Exception e) {
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
