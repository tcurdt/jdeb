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

/**
 * To be replace by commons compress once released
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public class ArInputStream extends InputStream implements ArConstants {

	private final InputStream input;
	private long offset = 0;
	
	public ArInputStream( final InputStream pInput ) {
		input = pInput;
	}
	
	public ArEntry getNextEntry() throws IOException {
		
		if (offset == 0) {
			final byte[] expected = HEADER;
			final byte[] realized = new byte[expected.length]; 
			final int read = input.read(realized);
			if (read != expected.length) {
				throw new IOException("failed to read header");
			}
			for (int i = 0; i < expected.length; i++) {
				if (expected[i] != realized[i]) {
					throw new IOException("invalid header " + new String(realized));
				}
			}
		}

		if (input.available() == 0) {
			return null;
		}
				
		if (offset % 2 != 0) {
			read();
		}

		final byte[] name = new byte[FIELD_SIZE_NAME];
		final byte[] lastmodified = new byte[FIELD_SIZE_LASTMODIFIED];
		final byte[] userid = new byte[FIELD_SIZE_UID];
		final byte[] groupid = new byte[FIELD_SIZE_GID];
		final byte[] filemode = new byte[FIELD_SIZE_MODE];
		final byte[] length = new byte[FIELD_SIZE_LENGTH];
		
		read(name);
		read(lastmodified);
		read(userid);
		read(groupid);
		read(filemode);
		read(length);

		final byte[] expected = ENTRY_TERMINATOR;
		final byte[] realized = new byte[expected.length];
		final int read = input.read(realized);
		if (read != expected.length) {
			throw new IOException("failed to read entry header");
		}
		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != realized[i]) {
				throw new IOException("invalid entry header. not read the content?");
			}
		}
		
		return new ArEntry(new String(name).trim(), Long.parseLong(new String(length).trim()));
	}
	
	
	public int read() throws IOException {
		final int ret = input.read();
		offset++;
		return ret;
	}

	public void close() throws IOException {
		input.close();
		super.close();
	}

}
