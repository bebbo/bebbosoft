package de.bb.security;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.bb.util.Misc;

public class TestSsl3 {

    @Test
    public void testPRF1() throws IOException {
    	Ssl3 ssl3 = new Ssl3(new byte[][] {Ssl3.TLS_RSA_WITH_AES_256_GCM_SHA384}) {
			@Override
			public void setMinVersion(int v) {
				versionMinor = 3; // TLS 1.2
				this.prfMd = new SHA384();
				
				clientRandom = Misc.hex2Bytes(
						"44 7b f4 f3 bf 0e 3a 5d-05 76 3f 7c 48 33 c1 84\r\n"
						+ "c3 1c d1 e6 66 5e 2d ee-c1 fa 73 31 0c 1c fb 8a\r\n");
				
				serverRandom = Misc.hex2Bytes(
						"f4 d0 23 e9 8d ac dd 1d-04 e0 d4 8f a2 df 07 ae\r\n"
						+ "b4 b9 8d 63 25 0b 82 f7-44 4f 57 4e 47 52 44 01\r\n"
						);
				
				masterSecret = Misc.hex2Bytes(
						"41 f7 03 1c 9a a6 9f 26-34 ea 05 8f f2 b5 09 32\r\n"
						+ "5f 44 76 f3 77 83 e7 76-89 1f 29 d7 d0 85 06 78\r\n"
						+ "9e ca ea 8c d2 52 41 8b-f2 4b b3 b2 7b c4 73 b9\r\n"
						);
				
				byte[] r = PRF(72, masterSecret, "key expansion", serverRandom, clientRandom);
		        Assert.assertArrayEquals(Misc.hex2Bytes(
		        		"dc 05 38 0f 66 89 f8 8c-e7 6a 4a aa 51 b8 66 58\r\n"
		        		+ "8f 42 a0 42 f1 bf fc ae-34 91 9d 58 7e 48 e3 be\r\n"
		        		+ "98 3a b6 6e b7 4f bb 40-da db 6f f3 e7 5a 1e 28\r\n"
		        		+ "ac 4d b2 c6 5f b6 c3 bd-99 ac f7 61 2c 00 40 b1\r\n"
		        		+ "0d bf 77 53 11 7e 62 c4-                       \r\n"
		        		), r);
			}
    	};
    	
    	ssl3.setMinVersion(0);
    }

    @Test
    public void testGcmEncrypt() throws IOException {
    	Ssl3 ssl3 = new Ssl3(new byte[][] {Ssl3.TLS_RSA_WITH_AES_256_GCM_SHA384}) {
			@Override
			public void setMinVersion(int v) {
				versionMinor = 3; // TLS 1.2
				this.prfMd = new SHA384();
				this.cryptWrite = new GCM(new AES());
				cryptWrite.setKey(Misc.hex2Bytes("46 6c 11 a8 5f 9b 07 92-81 9a 5a 9d e1 8d b4 1e 15 f9 44 67 87 24 6c 52-63 12 30 43 2d 8b fb 04"));
				this.writeAad = new byte[13];
				writeAad[9] = 3;
				writeAad[10] = versionMinor;
				
				writeNonce = Misc.hex2Bytes("fd 58 07 37 --- 0e 42 fc 3f ae f9 ac 29");

				byte [] in = Misc.hex2Bytes("14 00 00 0c  3b 77 0f a7  08 32 02 1d  8c 0d 05 dc");
				gcmEncrypt(in, 22, in.length);

				byte [] ex = Misc.hex2Bytes(
						"16 03 03 00 28 0E 42 FC  3F AE F9 AC 29 " +
            	"04 39 78 c6 00 54 42 06 dd 45 73 ef a0 7f 68 f8" +
            	// hash expected
            	"fc 3f 82 13 bb 45 ca 41 df 60 e4 3f cb b1 fb 56");

				Assert.assertArrayEquals(ex, writeBuffer);
			}
    	};
    	
    	ssl3.setMinVersion(0);
    }
    
}
