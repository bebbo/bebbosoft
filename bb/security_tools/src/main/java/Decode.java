/*
 * $Source: /export/CVS/java/de/bb/security_tools/src/main/java/Decode.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/08/11 19:56:34 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 * Written by Stefan Bebbo Franke
 * All rights reserved.
 *
 * Copyright (c) by Stefan Bebbo Franke 1999/2000.
 * All rights reserved.
 *
 * This version by Stefan Franke (s.franke@bebbosoft.de) and
 * still public domain.
 *
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import de.bb.security.Pem;
import de.bb.util.Mime;

public class Decode {
    public static void main(String args[]) {
        writeln("Decode $Revision: 1.1 $");
        if (args.length == 0 || args.length > 2) {
            writeln("usage: Encode <infile> [<outfile>]");
            return;
            // String p[] = {"ca\\test.cer.b64"};
            // args = p;
        }
        String ifn = args[0];
        String ofn = null;
        if (args.length == 2)
            ofn = args[1];

        try {
            FileInputStream fis = new FileInputStream(ifn);

            // Daten lesen
            int len = fis.available();
            byte b[] = new byte[len];
            fis.read(b);
            fis.close();

            if (ofn == null) {
                byte head[] = "Content-Type: application/octet-stream; name=\"".getBytes();
                int stop = -1, off = Mime.strstr(b, 0, head);
                if (off > 0) {
                    off += head.length;
                    stop = Mime.strstr(b, off, "\"".getBytes());
                }
                if (stop < 0)
                    ofn = "decode.out";
                else
                    ofn = new String(b, off, stop - off);
            }

            writeln("decoding: " + ifn + " -> " + ofn);

            String pwd = null;
            if (Pem.needsPwd(b, 0)) {
                // read password
                write("password: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                pwd = br.readLine();
            }

            b = Pem.decode(b, 0, pwd);

            if (b == null) {
                writeln("error decoding " + args[0]);
                return;
            }

            FileOutputStream fos = new FileOutputStream(ofn);
            fos.write(b);
            fos.close();

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
 * $Log: Decode.java,v $
 * Revision 1.1  2012/08/11 19:56:34  bebbo
 * @I working stage
 *
 * Revision 1.1  2010/12/17 17:20:34  bebbo
 * @INIT
 * Revision 1.5 2000/06/19 10:34:27 bebbo
 * 
 * @N adapted to changes in Pem: search .???.b64 too
 * 
 * Revision 1.4 2000/06/16 18:58:27 bebbo
 * 
 * @R now using Pem.xxxxx this version is now compatible to openSsl, Apache, etc.
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

