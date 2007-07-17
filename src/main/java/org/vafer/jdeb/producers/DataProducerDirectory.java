package org.vafer.jdeb.producers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Utils;
import org.vafer.jdeb.mapping.Mapper;

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
			
			final String[] files = scanner.getIncludedFiles();
	    	final File baseDir = scanner.getBasedir();
	        for (int i = 0; i < files.length; i++) {
	        	final File file = new File(baseDir, files[i]);
				InputStream inputStream = null;
	
				if (file.isFile()) {
					inputStream = new FileInputStream(file);
				}
	
				final String filename = getFilename(baseDir, file);	

				receiver.onEachFile(inputStream, filename, "", "root", 0, "root", 0, 33188, file.length());
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