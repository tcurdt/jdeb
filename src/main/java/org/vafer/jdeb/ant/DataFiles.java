package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.Utils;

public class DataFiles extends AbstractData {

	private static interface FileVisitor {
		void visit( File file );
	}

	public DataProducer getDataProducer() {		
		return new DataProducer() {
			public void produce( final DataConsumer receiver ) {
				final FileVisitor visitor =  new FileVisitor() {
					public void visit( File file ) {
						try {
							InputStream inputStream = null;

							if (".svn".equals(file.getName())) {
								return;
							}
							
							if (file.isFile()) {
								inputStream = new FileInputStream(file);
							}

							final String filename = getFilename(getSrc(), file);

							receiver.onEachFile(inputStream, Utils.stripPath(getStrip(), filename), "", "root", 0, "root", 0, 33188, file.length());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}					
				};

				iterate(getSrc(), visitor);
			}			
		};
	}


	private void iterate( File dir, FileVisitor visitor) {

		visitor.visit(dir);

		if (dir.isDirectory()) {

			File[] childs = dir.listFiles();
			for (int i = 0; i < childs.length; i++) {
				iterate(childs[i], visitor);
			}
		}
	}

	
	private String getFilename( File root, File file ) {
		final String relativeFilename = file.getAbsolutePath().substring(root.getAbsolutePath().length());		
		
		return Utils.stripLeadingSlash(relativeFilename);
	}



}