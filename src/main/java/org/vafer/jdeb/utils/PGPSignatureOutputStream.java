package org.vafer.jdeb.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;

/**
 * An output stream that calculates the signature of the input data as it
 * is written
 */
public class PGPSignatureOutputStream extends OutputStream {
    private final PGPSignatureGenerator signatureGenerator;

    public PGPSignatureOutputStream( PGPSignatureGenerator signatureGenerator ) {
        super();
        this.signatureGenerator = signatureGenerator;
    }

    @Override
    public void write( int b ) throws IOException {
        signatureGenerator.update(new byte[] { (byte)b });
    }

    @Override
    public void write( byte[] b ) throws IOException {
        signatureGenerator.update(b);
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        signatureGenerator.update(b, off, len);
    }
    
    public PGPSignature generateSignature() throws PGPException {
        return signatureGenerator.generate();
    }
    
    public String generateASCIISignature() throws PGPException {
        try {
            PGPSignature signature = generateSignature();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ArmoredOutputStream armorStream = new ArmoredOutputStream(buffer);
            signature.encode(armorStream);
            armorStream.close();
            return new String(buffer.toByteArray());
        } catch(IOException e) {
            //Should never happen since we are just using a memory buffer
            throw new RuntimeException(e);
        }
    }

} 