/*
 * Copyright 2010 The Apache Software Foundation.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

/**
 * Utils to do the signing with OpenPGP
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */

public final class SigningUtils {

    private static PGPSecretKey getSecretKey( final InputStream pInput, final String pKey ) throws IOException, PGPException {

        final PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(pInput));

        final Iterator rIt = pgpSec.getKeyRings();

        while (rIt.hasNext()) {
            final PGPSecretKeyRing kRing = (PGPSecretKeyRing) rIt.next();
            final Iterator kIt = kRing.getSecretKeys();

            while (kIt.hasNext()) {
                final PGPSecretKey k = (PGPSecretKey) kIt.next();

                if (k.isSigningKey() && Long.toHexString(k.getKeyID() & 0xFFFFFFFFL).equals(pKey.toLowerCase())) {
                    return k;
                }
            }
        }

        return null;
    }

    /**
     * Create a clear sign signature over the input data. (Not detached)
     *
     * @param pInput
     * @param pKeyring
     * @param pKey
     * @param pPassphrase
     * @param pOutput
     * @throws IOException
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    public static void clearSign( final InputStream pInput, final InputStream pKeyring, final String pKey, final String pPassphrase, final OutputStream pOutput ) throws IOException, PGPException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {

        Security.addProvider( new BouncyCastleProvider() );

        final PGPSecretKey secretKey = getSecretKey(pKeyring, pKey);
        final PGPPrivateKey privateKey = secretKey.extractPrivateKey(pPassphrase.toCharArray(), "BC");

        final int digest = PGPUtil.SHA1;

        final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(secretKey.getPublicKey().getAlgorithm(), digest, "BC");
        signatureGenerator.initSign(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);

//        final PGPSignatureSubpacketGenerator subpackageGenerator = new PGPSignatureSubpacketGenerator();
//
//        final Iterator it = secretKey.getPublicKey().getUserIDs();
//        if (it.hasNext()) {
//            subpackageGenerator.setSignerUserID(false, (String)it.next());
//            signatureGenerator.setHashedSubpackets(subpackageGenerator.generate());
//        }

        final ArmoredOutputStream armoredOutput = new ArmoredOutputStream(pOutput);

        armoredOutput.beginClearText(digest);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(pInput));

        final byte[] newline = "\r\n".getBytes("UTF-8");

        processLine(reader.readLine(), armoredOutput, signatureGenerator);

        while(true) {
            final String line = reader.readLine();

            if (line == null) {
                armoredOutput.write(newline);
                break;
            }

            armoredOutput.write(newline);
            signatureGenerator.update(newline);

            processLine(line, armoredOutput, signatureGenerator);
        }

        armoredOutput.endClearText();

        final BCPGOutputStream pgpOutput = new BCPGOutputStream(armoredOutput);

        signatureGenerator.generate().encode(pgpOutput);

        armoredOutput.close();

    }


    private static void processLine( final String pLine, final ArmoredOutputStream pArmoredOutput, final PGPSignatureGenerator pSignatureGenerator ) throws IOException, SignatureException {

        if (pLine == null) {
            return;
        }

        final char[] chars = pLine.toCharArray();
        int len = chars.length;

        while(len > 0) {
            if (!Character.isWhitespace(chars[len-1])) {
                break;
            }
            len--;
        }

        final byte[] data = pLine.substring(0, len).getBytes("UTF-8");

        pArmoredOutput.write(data);
        pSignatureGenerator.update(data);
    }
}
