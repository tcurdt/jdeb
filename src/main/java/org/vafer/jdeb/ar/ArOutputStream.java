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
package org.vafer.jdeb.ar;

import java.io.IOException;
import java.io.OutputStream;

/**
 * To be replace by commons compress once released
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public class ArOutputStream extends OutputStream implements ArConstants {

	private final OutputStream out;
	private long archiveOffset = 0;
	private long entryOffset = 0;
	private ArEntry prevEntry;

	public ArOutputStream( final OutputStream pOut ) {
		out = pOut;
	}

	private long writeArchiveHeader() throws IOException {		
		out.write(HEADER);
		return HEADER.length;
	}

	private void closeEntry() throws IOException {
		if ((entryOffset % 2) != 0) {
        	write('\n');
        	archiveOffset++;
        }		
	}
	
	public void putNextEntry( final ArEntry pEntry ) throws IOException {
		
		if (prevEntry == null) {
			archiveOffset += writeArchiveHeader();			
		} else {
			if (prevEntry.getLength() != entryOffset) {
				throw new IOException("length does not match entry (" + prevEntry.getLength() + " != " + entryOffset);
			}
			
			closeEntry();
		}
		
		prevEntry = pEntry;
		
		archiveOffset += writeEntryHeader(pEntry);

		entryOffset = 0;
	}

	/**
	 * Write the data to the stream and pad the output
	 * with white spaces up to the specified size.
	 *
	 * @param data	  the value to be written
	 * @param size	  the total size of the output
	 * @param fieldname the name of the field
	 */
	private void write(String data, int size, String fieldname) throws IOException {
		if (data.length() > size) {
			throw new IOException(fieldname + " too long");
		}

		long length = size - write(data);
		for (int i = 0; i < length; i++) {
			write(' ');
		}
	}

	private long write( final String data ) throws IOException {
		final byte[] bytes = data.getBytes("ascii");
		write(bytes);
		return bytes.length;
	}
	
	private long writeEntryHeader( final ArEntry entry ) throws IOException {

		String n = entry.getName();
		write(n, FIELD_SIZE_NAME, "filename");

		String m = "" + (entry.getLastModified() / 1000);
		write(m, FIELD_SIZE_LASTMODIFIED, "lastmodified");

		String u = "" + entry.getUserId();
		write(u, FIELD_SIZE_UID, "userid");

		String g = "" + entry.getGroupId();
		write(g, FIELD_SIZE_GID, "groupid");

		String fm = Integer.toString(entry.getMode(), 8);
		write(fm, FIELD_SIZE_MODE, "filemode");

		String s = "" + entry.getLength();
		write(s, FIELD_SIZE_LENGTH, "size");

		write(ENTRY_TERMINATOR);
		
		return HEADER_SIZE;
	}		
	
	public void write(int b) throws IOException {
		out.write(b);
		entryOffset++;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		entryOffset += len;
	}

	public void write(byte[] b) throws IOException {
		out.write(b);
		entryOffset += b.length;
	}

	public void close() throws IOException {
		closeEntry();
		out.close();
		prevEntry = null;
	}
	
}
