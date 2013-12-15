/*
 * Copyright 2013 The jdeb developers.
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

import java.io.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

/**
 * Signing with OpenPGP.
 * 
 * @author Torsten Curdt
 * @author Emmanuel Bourg
 */
public class PGPSigner {

    private static final byte[] EOL = "\r\n".getBytes(Charset.forName("UTF-8"));

    private PGPSecretKey secretKey;
    private PGPPrivateKey privateKey;

    public PGPSigner(InputStream keyring, String keyId, String passphrase) throws IOException, PGPException {
        secretKey = getSecretKey(keyring, keyId);
        if(secretKey == null)
        {
            throw new PGPException(String.format("Specified key %s does not exist in key ring %s", keyId, keyring));
        }
        privateKey = secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase.toCharArray()));
    }

    /**
     * Creates a clear sign signature over the input data. (Not detached)
     *
     * @param input      the content to be signed
     * @param output     the output destination of the signature
     */
    public void clearSign(String input, OutputStream output) throws IOException, PGPException, GeneralSecurityException {
        clearSign(new ByteArrayInputStream(input.getBytes("UTF-8")), output);
    }

    /**
     * Creates a clear sign signature over the input data. (Not detached)
     *
     * @param input      the content to be signed
     * @param output     the output destination of the signature
     */
    public void clearSign(InputStream input, OutputStream output) throws IOException, PGPException, GeneralSecurityException {
        int digest = PGPUtil.SHA1;
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(privateKey.getPublicKeyPacket().getAlgorithm(), digest));
        sGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);
        input = new BufferedInputStream(input);
        ArmoredOutputStream aOut = new ArmoredOutputStream(output);
        aOut.beginClearText(digest);

        //
        // note the last \n/\r/\r\n in the file is ignored
        //
        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int lookAhead = readInputLine(lineOut, input);

        processLine(aOut, sGen, lineOut.toByteArray());

        if (lookAhead != -1) {
            do{
                lookAhead = readInputLine(lineOut, lookAhead, input);

                sGen.update((byte)'\r');
                sGen.update((byte)'\n');

                processLine(aOut, sGen, lineOut.toByteArray());
            }
            while (lookAhead != -1);
        }
        input.close();
        aOut.endClearText();
        sGen.generate().encode(new BCPGOutputStream(aOut));
        aOut.close();

    }

    private static void processLine(OutputStream aOut, PGPSignatureGenerator sGen, byte[] line) throws SignatureException, IOException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sGen.update(line, 0, length);
        }
        aOut.write(line, 0, line.length);
    }


    private static int getLengthWithoutWhiteSpace(byte[] line) {
        int    end = line.length - 1;
        while (end >= 0 && isWhiteSpace(line[end])) {
            end--;
        }
        return end + 1;
    }

    private static boolean isWhiteSpace(byte b) {
        return b == '\r' || b == '\n' || b == '\t' || b == ' ';
    }

    private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn)
            throws IOException
    {
        bOut.reset();

        int lookAhead = -1;
        int ch;

        while ((ch = fIn.read()) >= 0)
        {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n')
            {
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        }

        return lookAhead;
    }

    private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn) throws IOException {
        bOut.reset();

        int ch = lookAhead;

        do {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n') {
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        }
        while ((ch = fIn.read()) >= 0);

        if (ch < 0) {
            lookAhead = -1;
        }

        return lookAhead;
    }

    private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn) throws IOException {
        int lookAhead = fIn.read();

        if (lastCh == '\r' && lookAhead == '\n') {
            bOut.write(lookAhead);
            lookAhead = fIn.read();
        }

        return lookAhead;
    }


    /**
     * Returns the secret key.
     */
    public PGPSecretKey getSecretKey()
    {
        return secretKey;
    }

    /**
     * Returns the private key.
     */
    public PGPPrivateKey getPrivateKey()
    {
        return privateKey;
    }

    /**
     * Returns the secret key matching the specified identifier.
     * 
     * @param input the input stream containing the keyring collection
     * @param keyId the 4 bytes identifier of the key
     */
    private PGPSecretKey getSecretKey(InputStream input, String keyId) throws IOException, PGPException {
        PGPSecretKeyRingCollection keyrings = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input));
        
        Iterator rIt = keyrings.getKeyRings();

        while (rIt.hasNext()) {
            PGPSecretKeyRing kRing = (PGPSecretKeyRing) rIt.next();
            Iterator kIt = kRing.getSecretKeys();

            while (kIt.hasNext()) {
                PGPSecretKey key = (PGPSecretKey) kIt.next();
                
                if (key.isSigningKey() && Long.toHexString(key.getKeyID() & 0xFFFFFFFFL).equals(keyId.toLowerCase())) {
                    return key;
                }
            }
        }

        return null;
    }

    /**
     * Trim the trailing spaces.
     * 
     * @param line
     */
    private String trim(String line) {
        char[] chars = line.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }
        
        return line.substring(0, len);
    }
}
