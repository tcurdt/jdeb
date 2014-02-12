/*
 * Copyright 2014 The jdeb developers.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Iterator;

import org.apache.commons.io.LineIterator;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
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
        
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(privateKey.getPublicKeyPacket().getAlgorithm(), digest));
        signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);
        
        ArmoredOutputStream armoredOutput = new ArmoredOutputStream(output);
        armoredOutput.beginClearText(digest);
        
        LineIterator iterator = new LineIterator(new InputStreamReader(input));
        
        while (iterator.hasNext()) {
            String line = iterator.nextLine();
            
            // trailing spaces must be removed for signature calculation (see http://tools.ietf.org/html/rfc4880#section-7.1)
            byte[] data = trim(line).getBytes("UTF-8");
            
            armoredOutput.write(data);
            armoredOutput.write(EOL);
            
            signatureGenerator.update(data);
            if (iterator.hasNext()) {
                signatureGenerator.update(EOL);
            }
        }

        armoredOutput.endClearText();
        
        PGPSignature signature = signatureGenerator.generate();
        signature.encode(new BCPGOutputStream(armoredOutput));
        
        armoredOutput.close();
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
