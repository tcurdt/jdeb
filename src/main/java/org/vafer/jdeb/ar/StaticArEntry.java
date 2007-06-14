package org.vafer.jdeb.ar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
