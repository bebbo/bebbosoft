/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/Encode.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/08/11 19:56:29 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Written by Stefan Bebbo Franke
 * All rights reserved.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import de.bb.security.Pem;

public class Encode {
    public static void main(String args[]) {
        writeln("Encode $Revision: 1.1 $");
        if (args.length == 0) {
            writeln("usage: Encode [-p] [-n <dataname>] <infile> [<outfile>=<infile>.b64]");
            return;
        }
        int argi = 0;
        boolean usePwd = false;
        String mark = "DATA";

        while (args[argi].charAt(0) == '-') {
            if (args[argi].equalsIgnoreCase("-p")) {
                usePwd = true;
            } else if (args[argi].equalsIgnoreCase("-n")) {
                ++argi;
                mark = args[argi];
            } else {
                writeln("invalid option: " + args[argi]);
                return;
            }
            ++argi;
        }

        String ifn = args[argi++];
        String ofn = ifn + ".b64";
        if (args.length > argi)
            ofn = args[argi++];

        try {

            String pwd = null;
            if (usePwd) {
                // read password
                write("password: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                pwd = br.readLine();
            }

            writeln("encoding: " + ifn + " -> " + ofn);
            File f = new File(ifn);
            FileInputStream fis = new FileInputStream(f);
            ifn = f.getName();
            FileOutputStream fos = new FileOutputStream(ofn);

            // header schreiben
            fos.write("MIME-Version: 1.0\r\nContent-Type: application/octet-stream; name=\"".getBytes());
            fos.write(ifn.getBytes());
            fos.write("\"\r\nContent-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\""
                    .getBytes());
            fos.write(ifn.getBytes());
            fos.write("\"\r\n\r\n".getBytes());

            int len = fis.available();
            byte b[] = new byte[len];
            fis.read(b);
            b = Pem.encode(b, mark, 0, pwd, null);
            fos.write(b);
        } catch (Exception e) {
            writeln(e.toString());
        }
    }

    static void write(String s) {
        System.out.print(s);
    }

    static void writeln(String s) {
        System.out.println(s);
    }
}

/*
 * $Log: Encode.java,v $
 * Revision 1.1  2012/08/11 19:56:29  bebbo
 * @I working stage
 *
 * Revision 1.1  2010/12/17 17:20:31  bebbo
 * @INIT
 * Revision 1.4 2000/06/16 19:36:35 bebbo
 * 
 * @R now using Pem and supports encrpytion!
 * 
 * Revision 1.3 1999/10/25 15:09:23 bebbo
 * 
 * @R changed package assignments
 * 
 * Revision 1.2 1999/09/02 19:07:20 Bebbo
 * 
 * @N created.
 * 
 * Revision 1.1 1999/08/27 12:17:53 Bebbo fixed some deprecated warnings.
 */

