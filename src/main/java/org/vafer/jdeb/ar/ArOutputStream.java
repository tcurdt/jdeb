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

public class ArOutputStream extends OutputStream {

	private final OutputStream out;
	private long archiveOffset = 0;
	private long entryOffset = 0;
	private ArEntry prevEntry;

	public ArOutputStream( final OutputStream pOut ) {
		out = pOut;
		
	}

	private long writeArchiveHeader() throws IOException {		
		final String header = "!<arch>\n";
		out.write(header.getBytes());
		return header.length();
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

	private long fill( final long pOffset, final long pNewOffset, final char pFill ) throws IOException { 
		final long diff = pNewOffset - pOffset;
	
		if (diff > 0) {
			for (int i = 0; i < diff; i++) {
				write(pFill);
			}
		}

		return pNewOffset;
	}
	
	private long write( final String data ) throws IOException {
		final byte[] bytes = data.getBytes("ascii");
		write(bytes);
		return bytes.length;
	}
	
	private long writeEntryHeader( final ArEntry pEntry ) throws IOException {
		
		long offset = 0;
		
		final String n = pEntry.getName();
		if (n.length() > 16) {
			throw new IOException("filename too long");
		}		
		offset += write(n);
		
		offset = fill(offset, 16, ' ');
		final String m = "" + (pEntry.getLastModified() / 1000);
		if (m.length() > 12) {
			throw new IOException("modified too long");
		}		
		offset += write(m);		

		offset = fill(offset, 28, ' ');
		final String u = "" + pEntry.getUserId();
		if (u.length() > 6) {
			throw new IOException("userid too long");
		}		
		offset += write(u);

		offset = fill(offset, 34, ' ');
		final String g = "" + pEntry.getGroupId();
		if (g.length() > 6) {
			throw new IOException("groupid too long");
		}		
		offset += write(g);

		offset = fill(offset, 40, ' ');
		final String fm = "" + Integer.toString(pEntry.getMode(), 8);
		if (fm.length() > 8) {
			throw new IOException("filemode too long");
		}		
		offset += write(fm);

		offset = fill(offset, 48, ' ');
		final String s = "" + pEntry.getLength();
		if (s.length() > 10) {
			throw new IOException("size too long");
		}		
		offset += write(s);

		offset = fill(offset, 58, ' ');

		offset += write("`\012");
		
		return offset;
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
