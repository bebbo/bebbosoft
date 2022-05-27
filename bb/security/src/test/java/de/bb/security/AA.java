package de.bb.security;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import de.bb.util.Misc;

public class AA {

    public static void main(String[] args) throws Exception {
        foo2();
    }

    public static void foo1() throws Exception {
        FileInputStream fis = new FileInputStream("my.txt");
        //FileInputStream fis = new FileInputStream("osl.txt");
        int len = fis.available();
        byte data[] = new byte[len];
        fis.read(data);
        fis.close();

        String s = new String(data, 0, 0, len);
        byte[] b = Misc.hex2Bytes(s);
        Misc.dump(System.out, b);

        SHA256 sha256 = new SHA256();
        Misc.dump(System.out, sha256.digest(b));

        MessageDigest sh = MessageDigest.getInstance("SHA-256");
        Misc.dump(System.out, sh.digest(b));
    }

    public static void foo2() throws IOException {

        byte n[] = Misc.hex2Bytes("00 c1 3b 96 0e f5" + "e1 41 97 f7 b4 3b 4a 12-9c da 4f 06 11 16 cc 3a"
                + "62 3b 83 14 0d e2 32 5f-0f 08 06 a4 cd b0 cb b0" + "a1 89 5f a5 99 06 99 74-a0 ea 88 f0 61 f5 be b7"
                + "5e 70 68 14 e8 a8 1c 51-c4 4b 79 b0 92 03 db 1f" + "77 8f 14 e3 50 1b dc 2d-08 32 e0 6b af 2a a5 a6"
                + "1c fc c0 c7 b4 ab b5 81-94 74 ae bc 2c 9c d3 c6" + "5d ee 1f 72 40 aa cc d1-5c b1 b5 6f 43 02 1f b7"
                + "4e bf f0 96 d4 f3 1f 39-8a 29 c6 fc 9f 41 e2 aa" + "25 ad d7 f4 4b 9b 98 bf-74 6c c3 6f b9 a3 a6 4f"
                + "8e 21 9f 4f 1b 00 63 0d-6e 11 34 2a 15 e4 db cb" + "e1 5b a7 b9 46 9c 23 dc-a1 ab b9 24 fb 00 10 f8"
                + "86 78 f9 d2 55 c3 b7 ea-c5 f6 9d 47 6c 61 e6 63" + "55 10 04 e8 d7 5d 13 57-ac 31 14 ad 11 d7 59 48"
                + "29 4f ff 36 b8 e2 db 5c-79 d6 ca 13 75 a3 3c 19" + "7b 6d 1f df b7 e3 0e 0c-93 b3 11 b8 01 76 96 28"
                + "17 17 56 ab e0 ff 7e be-64 19 39");

        byte e[] = Misc.hex2Bytes("01 00 01");

        byte signed[] = Misc.hex2Bytes("b4 c9 78 4e fa 12 e4 82-b0 4c 56 33 e8 6d f2 9d"
                + "91 ad 7c 04 32 95 fe d6-ca e2 01 83 85 f9 71 b6" + "7b 06 60 bf 93 59 2f 27-b7 2d 96 4d b1 17 b8 e0"
                + "1c 80 d1 37 38 ef b7 af-d2 c7 8e 13 b0 63 1f 6c" + "cb 63 f9 80 4a d8 e4 1d-2b 86 d8 6d c3 c5 3e 82"
                + "fb 77 cd 89 24 8b 43 49-0a 05 d0 90 8a 2c 20 da" + "1b 15 2f f4 0e 49 e2 1b-c8 9e ab 6f 5c 4d 63 d6"
                + "ed e6 75 73 91 19 d4 c1-e7 8c 4e d6 2d 8a 10 00" + "58 45 fa 01 67 8a c9 bc-97 1f 2e cf 0b 49 f3 89"
                + "bf 0e 93 31 e2 d6 50 82-bf 60 89 b8 05 3e e9 05" + "70 01 2e eb 7e a1 1f 41-ff ec 96 3c 72 8a 62 7a"
                + "8c 9f 48 2c b3 3b f3 db-39 8a a7 5f 8b 2c fb 94" + "e8 f5 3a 11 12 03 f1 38-89 16 5f 9e 45 19 44 cb"
                + "e7 ef 95 86 a6 79 5d 84-12 d6 87 03 1b 5b d3 46" + "45 6f 69 75 47 a6 fe c1-7a 34 32 08 93 19 13 c3"
                + "2d a5 f3 c6 74 fe 71 13-11 55 7a c7 93 fa 3b aa");

        byte data[] = Pkcs6.doRSA(signed, n, e);
        Misc.dump(System.out, data);

        byte clientRandom[] = Misc.hex2Bytes("54 15 fc 53 9d" + "13 26 1e a9 bd 8c d1 3e-fa 5e 31 1f 6f 17 cf ef"
                + "5b 67 1e 27 4b ed 87 98-06 23 fc");

        byte serverRandom[] = Misc.hex2Bytes("c1 04 78 53-70 84 1e 49 32 7f b8 b3"
                + "de 25 59 90 2e 40 68 33-3d 00 5c 56 06 6d bb 60" + "90 ed 23 5e");

        byte dhParams[] = Misc.hex2Bytes("00 80 81 3a-60 c2 84 ca 94 56 cd 11"
                + "8f 54 a5 ea f9 09 2f 08-5c a5 9e ba ee 05 ff 0c" + "e4 a3 b3 db 84 bd fb 20-bb dc 45 de 50 72 d4 c4"
                + "63 e3 76 41 0a 4e 31 35-f8 d4 7b a0 4b 3d c4 e1" + "c4 96 43 a6 84 2b 85 f8-fe 50 cb c8 3e 34 1b 75"
                + "76 47 f1 b2 77 85 3c 66-82 a5 9e 80 ae e9 7f" + "59 ae 0d 8d ff f0 9c a7-d1 f7 88 c7 36 99 52 3c"
                + "b5 95 ad 46 b4 e9 68 74-08 68 37 59 3c 83 64 6a" + "15 c7 c4 fe 4c c5 db 00-01 02 00 80 1c 2d 3b 6a"
                + "c6 5b d5 a9 a6 24 85 74-c6 16 fb df 64 5d 1d 1a" + "db ae dc d2 a0 22 1b 74-7d ab 7f d7 45 b3 95 92"
                + "91 9f fe af 3c 55 dd 9f-98 12 c2 4c 2f 5e 66 f1" + "23 93 1f 37 e4 c2 8a c9-5c 23 eb 39 b1 44 e0 d9"
                + "19 71 38 28 e4 fe 94 0c-9e 44 66 d8 3e da 4c 0a" + "f4 03 e5 33 e3 25 4b f6-a9 1f 33 8e bb f2 cc 9d"
                + "40 de fc c2 cb e4 2b c2-da 75 c3 5d 06 e3 33 63" + "8d d9 7d d6 19 72 8e 10-d3 b7 33 0e");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(clientRandom);
        bos.write(serverRandom);
        bos.write(dhParams);
        byte sign[] = bos.toByteArray();

        byte[] prepared = Pkcs6.prepareSignedContent(sign, "SHA", n.length - (n[0] == 0 ? 1 : 0));
        Misc.dump(System.out, prepared);
    }
}
