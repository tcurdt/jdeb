package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.PatternSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Utils;

public class DataFiles extends PatternSet implements DataProducer {

	private DirectoryScanner scanner = new DirectoryScanner();
	
	public void setDir( File dir ) {
		scanner.setBasedir(dir);
	}
	
	public void produce( final DataConsumer receiver ) {

		try {
			scanner.setIncludes(getIncludePatterns(getProject()));
			scanner.setExcludes(getExcludePatterns(getProject()));
			scanner.setCaseSensitive(true);
			scanner.setFollowSymlinks(true);

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