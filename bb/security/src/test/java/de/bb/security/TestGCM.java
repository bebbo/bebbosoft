package de.bb.security;

import org.junit.Test;

import de.bb.util.Misc;

/**
 * Tests for GCM.
 * 
 * @See http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-revised-spec.pdf
 * 
 * @author Stefan Bebbo Franke
 *
 */
public class TestGCM {

    /** array of test data: {K, P, A, IV, C, T}. */
    private final static String DATA[][] = {
            { "00000000000000000000000000000000", "", "", "000000000000000000000000", "",
                    "58e2fccefa7e3061367f1d57a4e7455a", "Test Case 1" },
            { "00000000000000000000000000000000", "00000000000000000000000000000000", "", "000000000000000000000000",
                    "0388dace60b6a392f328c2b971b2fe78", "ab6e47d42cec13bdf53a67b21257bddf", "Test Case 2" },
            {
                    "feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72 "
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b391aafd255",
                    "",
                    "cafebabefacedbaddecaf888",
                    "42831ec2217774244b7221b784d0d49c e3aa212f2c02a4e035c17e2329aca12e "
                            + "21d514b25466931c7d8f6a5aac84aa05 1ba30b396a0aac973d58e091473f5985",
                    "4d5c2af327cd64a62cf35abd2ba6fab4", "Test Case 3" },
            {
                    "feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbaddecaf888",
                    "42831ec2217774244b7221b784d0d49c e3aa212f2c02a4e035c17e2329aca12e"
                            + "21d514b25466931c7d8f6a5aac84aa05 1ba30b396a0aac973d58e091",
                    "5bc94fbc3221a5db94fae95ae7121a47", "Test Case 4" },
            {
                    "feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbad",
                    "61353b4c2806934a777ff51fa22a4755 699b2a714fcdc6f83766e5f97b6c7423"
                            + "73806900e49f24b22b097544d4896b42 4989b5e1ebac0f07c23f4598",
                    "3612d2e79e3b0785561be14aaca2fccb", "Test Case 5" },
            {
                    "feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "9313225df88406e555909c5aff5269aa 6a7a9538534f7da1e4c303d2a318a728"
                            + "c3c0c95156809539fcf0e2429a6b5254 16aedbf5a0de6a57a637b39b",
                    "8ce24998625615b603a033aca13fb894 be9112a5c3a211a8ba262a3cca7e2ca7"
                            + "01e4a9a4fba43c90ccdcb281d48c7c6f d62875d2aca417034c34aee5",
                    "619cc5aefffe0bfa462af43c1699d050", "Test Case 6" },
            { "00000000000000000000000000000000 0000000000000000", "", "", "000000000000000000000000", "",
                    "cd33b28ac773f74ba00ed1f312572435", "Test Case 7" },
            { "00000000000000000000000000000000 0000000000000000", "00000000000000000000000000000000", "",
                    "000000000000000000000000", "98e7247c07f0fe411c267e4384b0f600", "2ff58d80033927ab8ef4d4587514f0fb",
                    "Test Case 8" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b391aafd255",
                    "",
                    "cafebabefacedbaddecaf888",
                    "3980ca0b3c00e841eb06fac4872a2757 859e1ceaa6efd984628593b40ca1e19c"
                            + "7d773d00c144c525ac619d18c84a3f47 18e2448b2fe324d9ccda2710acade256",
                    "9924a7c8587336bfb118024db8674a14", "Test Case 9" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbaddecaf888",
                    "3980ca0b3c00e841eb06fac4872a2757 859e1ceaa6efd984628593b40ca1e19c"
                            + "7d773d00c144c525ac619d18c84a3f47 18e2448b2fe324d9ccda2710",
                    "2519498e80f1478f37ba55bd6d27618c", "Test Case 10" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbad",
                    "0f10f599ae14a154ed24b36e25324db8 c566632ef2bbb34f8347280fc4507057"
                            + "fddc29df9a471f75c66541d4d4dad1c9 e93a19a58e8b473fa0f062f7",
                    "65dcc57fcf623a24094fcca40d3533f8", "Test Case 11" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "9313225df88406e555909c5aff5269aa 6a7a9538534f7da1e4c303d2a318a728"
                            + "c3c0c95156809539fcf0e2429a6b5254 16aedbf5a0de6a57a637b39b",
                    "d27e88681ce3243c4830165a8fdcf9ff 1de9a1d8e6b447ef6ef7b79828666e45"
                            + "81e79012af34ddd9e2f037589b292db3 e67c036745fa22e7e9b7373b",
                    "dcf566ff291c25bbb8568fc3d376a6d9", "Test Case 12" },
            { "00000000000000000000000000000000 00000000000000000000000000000000", "", "", "000000000000000000000000",
                    "", "530f8afbc74536b9a963b4f1c4cb738b", "Test Case 13" },
            { "00000000000000000000000000000000 00000000000000000000000000000000", "00000000000000000000000000000000",
                    "", "000000000000000000000000", "cea7403d4d606b6e074ec5d3baf39d18",
                    "d0d1c8a799996bf0265b98b5d48ab919", "Test Case 14" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b391aafd255",
                    "",
                    "cafebabefacedbaddecaf888",
                    "522dc1f099567d07f47f37a32a84427d 643a8cdcbfe5c0c97598a2bd2555d1aa"
                            + "8cb08e48590dbb3da7b08b1056828838 c5f61e6393ba7a0abcc9f662898015ad",
                    "b094dac5d93471bdec1a502270e3cc6c", "Test Case 15" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbaddecaf888",
                    "522dc1f099567d07f47f37a32a84427d 643a8cdcbfe5c0c97598a2bd2555d1aa"
                            + "8cb08e48590dbb3da7b08b1056828838 c5f61e6393ba7a0abcc9f662",
                    "76fc6ece0f4e1768cddf8853bb2d551b", "Test Case 16" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "cafebabefacedbad",
                    "c3762df1ca787d32ae47c13bf19844cb af1ae14d0b976afac52ff7d79bba9de0"
                            + "feb582d33934a4f0954cc2363bc73f78 62ac430e64abe499f47c9b1f",
                    "3a337dbf46a792c45e454913fe2ea8f2", "Test Case 17" },
            {
                    "feffe9928665731c6d6a8f9467308308 feffe9928665731c6d6a8f9467308308",
                    "d9313225f88406e5a55909c5aff5269a 86a7a9531534f7da2e4c303d8a318a72"
                            + "1c3c0c95956809532fcf0e2449a6b525 b16aedf5aa0de657ba637b39",
                    "feedfacedeadbeeffeedfacedeadbeef abaddad2",
                    "9313225df88406e555909c5aff5269aa 6a7a9538534f7da1e4c303d2a318a728"
                            + "c3c0c95156809539fcf0e2429a6b5254 16aedbf5a0de6a57a637b39b",
                    "5a8def2f0c9e53f1f75d7853659e2a20 eeb2b22aafde6419a058ab4f6f746bf4"
                            + "0fc0c3b780f244452da3ebf1c5d82cde a2418997200ef82e44ae7e3f",
                    "a44a8266ee1c8eb0c8b5d4cf5ae9f19a", "Test Case 18" },
                    
    
			{
                    	// key
                    	" 46 6c 11 a8 5f 9b 07 92-81 9a 5a 9d e1 8d b4 1e 15 f9 44 67 87 24 6c 52-63 12 30 43 2d 8b fb 04",
                    	// in plaintext
                    	"14 00 00 0c  3b 77 0f a7  08 32 02 1d  8c 0d 05 dc  ",
                    	// 82 dc 50 e6 a1 d0 9f 39 55 15 e8 db 93 d1 fa e0",
                    	// aad
                    	"00 00 00 00 00 00 00 00 16 03 03 00 10",
                    	// nonce secret + nonce public (from out)
                    	" fd 58 07 37" + "0e 42 fc 3f ae f9 ac 29 ",
                    	// out expected
                    	// "0e 42 fc 3f ae f9 ac 29 "
                    	"04 39 78 c6 00 54 42 06 dd 45 73 ef a0 7f 68 f8",
                    	// hash expected
                    	"fc 3f 82 13 bb 45 ca 41 df 60 e4 3f cb b1 fb 56",
                    	"TLS 1.2"}                    
    };

    @Test
    public void test() {
        BlockCipher aes = new AES();
        for (final String d[] : DATA) {
            byte[] keyData = Misc.hex2Bytes(d[0]);
            byte[] plainText = Misc.hex2Bytes(d[1]);
            byte[] aad = Misc.hex2Bytes(d[2]);
            byte[] nonce = Misc.hex2Bytes(d[3]);
            byte[] cipherExpected = Misc.hex2Bytes(d[4]);
            byte[] hashExpected = Misc.hex2Bytes(d[5]);
            final String name = d[6];

            byte[] cipherText = new byte[plainText.length];
            byte[] hash = new byte[16];
            byte verify[] = new byte[cipherText.length];

            aes.setKey(keyData);
            GCM ge = new GCM(aes);
            ge.init(nonce);
            ge.updateHash(aad, 0, aad.length);
            ge.encrypt(plainText, 0, cipherText, 0, plainText.length);
            ge.calcHash(hash, 0);

            if (!Misc.equals(cipherExpected, cipherText))
                throw new RuntimeException(name + ": encrypt failed. Expected\r\n" + Misc.bytes2Hex(cipherExpected)
                        + "\r\ngot\r\n" + Misc.bytes2Hex(cipherText));

            if (!Misc.equals(hashExpected, hash))
                throw new RuntimeException(name + ": encrypt hash failed. Expected\r\n" + Misc.bytes2Hex(hashExpected)
                        + "\r\ngot\r\n" + Misc.bytes2Hex(hash));

            ge.init(nonce);
            ge.updateHash(aad, 0, aad.length);
            ge.decrypt(cipherText, 0, verify, 0, plainText.length);
            ge.calcHash(hash, 0);

            if (!Misc.equals(verify, plainText))
                throw new RuntimeException(name + ": decrypt failed. Expected\r\n" + Misc.bytes2Hex(plainText)
                        + "\r\ngot\r\n" + Misc.bytes2Hex(verify));

            if (!Misc.equals(hashExpected, hash))
                throw new RuntimeException(name + ": decrypt hash failed. Expected\r\n" + Misc.bytes2Hex(hashExpected)
                        + "\r\ngot\r\n" + Misc.bytes2Hex(hash));
        }
    }
}
