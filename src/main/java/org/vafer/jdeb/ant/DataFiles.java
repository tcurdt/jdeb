package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.AbstractFileSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Utils;

public class DataFiles extends AbstractFileSet implements DataProducer {

	public void produce( final DataConsumer receiver ) {

		try {
	        final DirectoryScanner ds = getDirectoryScanner(getProject());
	        final String[] files = ds.getIncludedFiles();
	    	final File baseDir = ds.getBasedir();
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