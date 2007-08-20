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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class for ArEntries
 * 
 * @author tcurdt
 */
public abstract class AbstractArEntry {

	private final int userId;
	private final int groupId;
	private final int mode;
	
	public AbstractArEntry( final int pUserId, final int pGroupId, final int pMode ) {
		userId = pUserId;
		groupId = pGroupId;
		mode = pMode;
	}

	public abstract String getName();
	public abstract long getLength();
	public abstract long getLastModified();
	public abstract InputStream getData() throws IOException;
	
	
	long write( final OutputStream pOut ) throws IOException {
		return writeHeader(pOut) + writeData(pOut);
	}
	
	
	private long write( final String pData, final OutputStream pOut ) throws IOException {
		final byte[] data = pData.getBytes("ascii");
		pOut.write(data);
		return data.length;		
	}
	
	private long fill( final long pOffset, final long pNewOffset, final char pFill, final OutputStream pOut) throws IOException { 
		final long diff = pNewOffset - pOffset;
	
		if (diff > 0) {
			for (int i = 0; i < diff; i++) {
				pOut.write(pFill);
			}
		}

		return pNewOffset;
	}
	
	private long writeHeader( final OutputStream pOut ) throws IOException {
		
		long offset = 0;
		
		final String n = getName();
		if (n.length() > 16) {
			throw new IOException("filename too long");
		}		
		offset += write(n, pOut);
		
		offset = fill(offset, 16, ' ', pOut);
		final String m = "" + (getLastModified() / 1000);
		if (m.length() > 12) {
			throw new IOException("modified too long");
		}		
		offset += write(m, pOut);		

		offset = fill(offset, 28, ' ', pOut);
		final String u = "" + userId;
		if (u.length() > 6) {
			throw new IOException("userid too long");
		}		
		offset += write(u, pOut);

		offset = fill(offset, 34, ' ', pOut);
		final String g = "" + groupId;
		if (g.length() > 6) {
			throw new IOException("groupid too long");
		}		
		offset += write(g, pOut);

		offset = fill(offset, 40, ' ', pOut);
		final String fm = "" + Integer.toString(mode, 8);
		if (fm.length() > 8) {
			throw new IOException("filemode too long");
		}		
		offset += write(fm, pOut);

		offset = fill(offset, 48, ' ', pOut);
		final String s = "" + getLength();
		if (s.length() > 10) {
			throw new IOException("size too long");
		}		
		offset += write(s, pOut);

		offset = fill(offset, 58, ' ', pOut);

		offset += write("`\012", pOut);
		
		return offset;
	}
	
	private long writeData( final OutputStream pOut ) throws IOException {
		
        final byte[] buffer = new byte[2048];
        final InputStream input = getData();
        long count = 0;
        int n = 0;
        
        while (-1 != (n = input.read(buffer))) {
            pOut.write(buffer, 0, n);
            count += n;
        }
        
        if ((count % 2) != 0) {
        	pOut.write('\n');
        	count++;
        }
        
        input.close();
        
        return count;
	}
}
