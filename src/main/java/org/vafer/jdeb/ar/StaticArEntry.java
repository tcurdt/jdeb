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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ArEntry coming from memory
 * 
 * @author tcurdt
 */
public final class StaticArEntry extends AbstractArEntry {

	private final String name;
	private final StringBuffer buffer;
	private byte[] data;
	
	public StaticArEntry( final String pName, final int pUserId, final int pGroupId, final int pMode, final String pData) {
		super(pUserId, pGroupId, pMode);
		name = pName;
		buffer = new StringBuffer(pData);
	}

	public StaticArEntry( final String pName, final int pUserId, final int pGroupId, final int pMode ) {
		super(pUserId, pGroupId, pMode);
		name = pName;
		buffer = new StringBuffer();
	}

	
	private void unmodifable() {
		if (data == null) {
			data = buffer.toString().getBytes();			
		}
	}
	
	public InputStream getData() throws IOException {
		unmodifable();
		return new ByteArrayInputStream(buffer.toString().getBytes());
	}

	public StringBuffer append( final String pData ) {
		if (data != null) {
			throw new RuntimeException("unmodifiable now");
		}
		buffer.append(pData);
		return buffer;
	}
	
	public long getLastModified() {
		return System.currentTimeMillis();
	}

	public long getLength() {
		unmodifable();
		return data.length; 
	}

	public String getName() {
		return name;
	}

}
