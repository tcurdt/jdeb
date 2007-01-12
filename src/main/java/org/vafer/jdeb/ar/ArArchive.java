package org.vafer.jdeb.ar;

import java.io.IOException;
import java.io.OutputStream;

public class ArArchive {
	
	private final OutputStream out;
	private long offset = 0;
	
	public ArArchive( final OutputStream pOut ) {
		out = pOut;
	}
	
	private long writeHeader() throws IOException {		
		final String header = "!<arch>\n"; 
		out.write(header.getBytes());
		return header.length();
	}

	public void add( final AbstractArEntry pEntry ) throws IOException {
		if (offset == 0) {
			offset += writeHeader();
		}
		
		offset += pEntry.write(out);
	}
	
	public void close() throws IOException {
		out.close();
	}
	
}
