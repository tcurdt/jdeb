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
 * Simple ArArchive implementation
 * 
 * @author tcurdt
 */
public final class ArArchive {
	
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
