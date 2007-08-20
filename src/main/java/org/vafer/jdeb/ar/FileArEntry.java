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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ArEntry coming from a file
 * 
 * @author tcurdt
 */
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
