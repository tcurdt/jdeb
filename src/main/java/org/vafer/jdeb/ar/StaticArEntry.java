package org.vafer.jdeb.ar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StaticArEntry extends AbstractArEntry {

	private final String name;
	private final byte[] data;
	
	public StaticArEntry( final String pName, final int pUserId, final int pGroupId, final int pMode, final String pData) {
		super(pUserId, pGroupId, pMode);
		name = pName;
		data = pData.getBytes();
	}

	public InputStream getData() throws IOException {
		return new ByteArrayInputStream(data);
	}

	public long getLastModified() {
		return System.currentTimeMillis();
	}

	public long getLength() {
		return data.length; 
	}

	public String getName() {
		return name;
	}

}
