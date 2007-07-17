package org.vafer.jdeb.producers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

public final class DataProducerArchive extends AbstractDataProducer implements DataProducer {

	private final File archive;
	
	public DataProducerArchive( final File pArchive, final String[] pIncludes, final String[] pExcludes, final Mapper pMapper ) {
		super(pIncludes, pExcludes, pMapper);
		archive = pArchive;
	}
		
	public void produce( final DataConsumer receiver ) {

		TarInputStream archiveInputStream = null;
		try {
			archiveInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(archive)));

			while(true) {
				
				TarEntry entry = archiveInputStream.getNextEntry();

				if (entry == null) {
					break;
				}

				entry = map(entry);
				
				final String name = entry.getName();
				
				if (!isIncluded(name)) {
					continue;					
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

	
	
}
