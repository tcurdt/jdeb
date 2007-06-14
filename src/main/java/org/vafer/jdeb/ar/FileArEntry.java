package org.vafer.jdeb.ar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileArEntry extends AbstractArEntry {

	private final File file;
	private final String name;
	
	public FileArEntry( final File pFile, final int pUserId, final int pGroupId, final int pMode ) {
		this(pFile, pFile.getName(), pUserId, pGroupId, pMode);
	}

	public FileArEntry( final File pFile, final String pName, final int pUserId, final int pGroupId, final int pMode ) {
		super(pUserId, pGroupId, pMode);
		file = pFile;
		name = pName;
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
		return name;
	}
	
}
