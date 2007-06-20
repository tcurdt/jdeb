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

public class SigningUtils {

    private static PGPSecretKey getSecretKey( InputStream is, String keyId ) throws IOException, PGPException {

        final PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(is));

    	final Iterator rIt = pgpSec.getKeyRings();

		while (rIt.hasNext()) {
			final PGPSecretKeyRing kRing = (PGPSecretKeyRing) rIt.next();
			final Iterator kIt = kRing.getSecretKeys();

			while (kIt.hasNext()) {
				final PGPSecretKey k = (PGPSecretKey) kIt.next();

				if (k.isSigningKey() && Long.toHexString(k.getKeyID() & 0xFFFFFFFFL).equals(keyId.toLowerCase())) {
					return k;
				}
			}
		}

		return null;
	}

	
	public static void clearSign( InputStream input, InputStream keyring, String key, String passphrase, OutputStream output ) throws IOException, PGPException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {
		
		Security.addProvider( new BouncyCastleProvider() );
        
        final PGPSecretKey secretKey = getSecretKey(keyring, key);
        final PGPPrivateKey privateKey = secretKey.extractPrivateKey(passphrase.toCharArray(), "BC");
                
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
	
        final ArmoredOutputStream armoredOutput = new ArmoredOutputStream(output);
        
        armoredOutput.beginClearText(digest);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        final byte[] linefeed = "\r\n".getBytes("UTF-8");
        
        while(true) {
        	final String line = reader.readLine();
        	
        	if (line == null) {
        		break;
        	}
        	        	
        	final byte[] data = line.getBytes("UTF-8");
        	
        	armoredOutput.write(data);
        	signatureGenerator.update(data);

        	armoredOutput.write(linefeed);
        	signatureGenerator.update(linefeed);
        	
        }
        
        armoredOutput.endClearText();
        
        final BCPGOutputStream pgpOutput = new BCPGOutputStream(armoredOutput);
        
        signatureGenerator.generate().encode(pgpOutput);

        armoredOutput.close();
		
	}
}
