/*
 * Copyright 2005 The Apache Software Foundation.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

public final class SigningTestCase extends TestCase {

    public void testClearSign() throws Exception {
        
        final InputStream ring = getClass().getClassLoader().getResourceAsStream("org/vafer/gpg/secring.gpg");
        
        assertNotNull(ring);
        
        final String inputStr = "TEST1\nTEST2\nTEST3\n"; 
        final byte[] input = inputStr.getBytes("UTF-8");
        
        final String expectedOutputStr = 
            "-----BEGIN PGP SIGNED MESSAGE-----\n" + 
            "Hash: SHA1\n" + 
            "\n" + 
            "TEST1\r\n" + 
            "TEST2\r\n" + 
            "TEST3\r\n" + 
            "-----BEGIN PGP SIGNATURE-----\n" + 
            "Version: BCPG v1.29\n" + 
            "\n" + 
            "iEYEARECABAFAkax1rgJEHM9pIAuB02PAABIJgCghFmoCJCZ0CGiqgVLGGPd/Yh5\n" + 
            "FQQAnRVqvI2ij45JQSHYJBblZ0Vv2meN\n" + 
            "=aAAT\n" + 
            "-----END PGP SIGNATURE-----\n";
        
        final byte[] expectedOutput = expectedOutputStr.getBytes("UTF-8"); 

        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        SigningUtils.clearSign(
                new ByteArrayInputStream(input),
                ring,
                "2E074D8F", "test",
                os);
        
        final byte[] output = os.toByteArray();
        
        final int from = expectedOutputStr.indexOf("iEYEAREC");
        final int until = expectedOutputStr.indexOf("=aAAT") + 5;
        Arrays.fill(output, from, until, (byte)'?');
        Arrays.fill(expectedOutput, from, until, (byte)'?');

        assertEquals(new String(expectedOutput), new String(output));
    }
}
