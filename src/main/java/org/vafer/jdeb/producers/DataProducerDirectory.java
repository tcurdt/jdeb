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
package org.vafer.jdeb.producers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.utils.Utils;

public final class DataProducerDirectory extends AbstractDataProducer implements DataProducer {

	private final DirectoryScanner scanner = new DirectoryScanner();
	
	public DataProducerDirectory( final File pDir, final String[] pIncludes, final String[] pExcludes, final Mapper pMapper ) {
		super(pIncludes, pExcludes, pMapper);
		scanner.setBasedir(pDir);
		scanner.setIncludes(pIncludes);
		scanner.setExcludes(pExcludes);
		scanner.setCaseSensitive(true);
		scanner.setFollowSymlinks(true);
	}
	
	public void produce( final DataConsumer receiver ) {
		
		try {

			scanner.scan();
			
	    	final File baseDir = scanner.getBasedir();

			final String[] dirs = scanner.getIncludedDirectories();
	        for (int i = 0; i < dirs.length; i++) {
	        	final File file = new File(baseDir, dirs[i]);
				final String dirname = getFilename(baseDir, file);	
				
				if ("".equals(dirname)) {
					continue;
				}
				
				receiver.onEachDir(dirname, "", "root", 0, "root", 0, TarEntry.DEFAULT_DIR_MODE, file.length());
	        }
	    	
			final String[] files = scanner.getIncludedFiles();

			for (int i = 0; i < files.length; i++) {
	        	final File file = new File(baseDir, files[i]);
				final String filename = getFilename(baseDir, file);
				
				receiver.onEachFile(new FileInputStream(file), filename, "", "root", 0, "root", 0, TarEntry.DEFAULT_FILE_MODE, file.length());
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}			

	private String getFilename( File root, File file ) {
		
		final String relativeFilename = file.getAbsolutePath().substring(root.getAbsolutePath().length());		
		
		return Utils.stripLeadingSlash(relativeFilename);
	}

}