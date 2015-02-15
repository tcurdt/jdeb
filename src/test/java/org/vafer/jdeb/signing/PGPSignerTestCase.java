/*
 * Copyright 2015 The jdeb developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vafer.jdeb.signing;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

public final class PGPSignerTestCase extends TestCase {

    public void testClearSign() throws Exception {

        final InputStream ring = getClass().getClassLoader().getResourceAsStream("org/vafer/gpg/secring.gpg");

        assertNotNull(ring);

        String input = "TEST1\n-TEST2 \n  \nTEST3\n";

        final String expectedOutputStr =
            "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                "Hash: SHA1\n" +
                "\n" +
                "TEST1\n" +
                "- -TEST2\n" +
                "\n" +
                "TEST3\n" +
                "-----BEGIN PGP SIGNATURE-----\n" +
                "Version: BCPG v1.51\n" +
                "\n" +
                "iEYEARECABAFAkax1rgJEHM9pIAuB02PAABIJgCghFmoCJCZ0CGiqgVLGGPd/Yh5\n" +
                "FQQAnRVqvI2ij45JQSHYJBblZ0Vv2meN\n" +
                "=aAAT\n" +
                "-----END PGP SIGNATURE-----\n";

        final byte[] expectedOutput = expectedOutputStr.getBytes("UTF-8");

        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        PGPSigner signer = new PGPSigner(ring, "2E074D8F", "test");
        signer.clearSign(input, os);

        final byte[] output = fixCRLF(os.toByteArray());
        final int from = expectedOutputStr.indexOf("iEYEAREC");
        final int until = expectedOutputStr.indexOf("=aAAT") + 5;
        Arrays.fill(output, from, until, (byte) '?');
        Arrays.fill(expectedOutput, from, until, (byte) '?');
        
        assertEquals(new String(expectedOutput), new String(output));
    }

    public void testKeyLoading() throws Exception {
        InputStream ring = getClass().getClassLoader().getResourceAsStream("org/vafer/gpg/secring.gpg");
        PGPSigner signer = new PGPSigner(ring, "2E074D8F", "test");
        assertEquals("correct key found", "733da4802e074d8f", String.format("%016x", signer.getSecretKey().getKeyID()));

        ring.reset();
        signer = new PGPSigner(ring, "0C1FF47A", "test");
        assertEquals("key with leading 0 found", "21970bb80c1ff47a", String.format("%016x", signer.getSecretKey().getKeyID()));
    }

    private byte[] fixCRLF(byte[] b) {
        String s = new String(b);
        s = s.replaceAll("\r\n", "\n");
        return s.getBytes();
    }
}
