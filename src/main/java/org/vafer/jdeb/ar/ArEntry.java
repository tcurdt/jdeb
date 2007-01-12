package org.vafer.jdeb.ar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ArEntry extends AbstractArEntry{

	private final File file;
	
	public ArEntry( final File pFile, final int pUserId, final int pGroupId, final int pMode ) {
		super(pUserId, pGroupId, pMode);
		file = pFile;
	}

	public InputStream getData() throws IOException  {
		return new FileInputStream(file);
	}

	public long getLastModified() {
		return file.lastModified();
	}

	public long getLength() {
		return file.length();
	}

	public String getName() {
		return file.getName();
	}
	
}
