package org.vafer.jdeb.ant;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;

public class DataArchive extends AbstractData {

	public DataProducer getDataProducer() {		
		return new DataProducer() {
			public void produce( DataConsumer receiver ) {

				TarInputStream archiveInputStream = null;
				try {
					archiveInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(getSrc())));

					while(true) {
						final TarEntry entry = archiveInputStream.getNextEntry();
						
						if (entry == null) {
							break;
						}

						InputStream inputStream = archiveInputStream;
						
						if (entry.isDirectory()) {
							inputStream = null;							
						}
						
						receiver.onEachFile(inputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());						
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (archiveInputStream != null) {
						try {
							archiveInputStream.close();
						} catch (IOException e) {
						}
					}
				}
				
			}			
		};
	}

}
