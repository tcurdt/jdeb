package org.vafer.jdeb.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SignatureException;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;

/**
 * An output stream that calculates the signature of the input data as it
 * is written
 * 
 * @author mpoindexter
 *
 */
public class PGPSignatureOutputStream extends OutputStream {
    private final PGPSignatureGenerator signatureGenerator;

    public PGPSignatureOutputStream( PGPSignatureGenerator signatureGenerator ) {
        super();
        this.signatureGenerator = signatureGenerator;
    }

    public void write( int b ) throws IOException {
        try {
            signatureGenerator.update(new byte[] { (byte)b });
        } catch(SignatureException e) {
            throw new IOException(e);
        }
    }

    public void write( byte[] b ) throws IOException {
        try {
            signatureGenerator.update(b);
        } catch(SignatureException e) {
            throw new IOException(e);
        }
    }

    public void write( byte[] b, int off, int len ) throws IOException {
        try {
            signatureGenerator.update(b, off, len);
        } catch(SignatureException e) {
            throw new IOException(e);
        }
    }
    
    public PGPSignature generateSignature() throws SignatureException, PGPException {
        return signatureGenerator.generate();
    }
    
    public String generateASCIISignature() throws SignatureException, PGPException {
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