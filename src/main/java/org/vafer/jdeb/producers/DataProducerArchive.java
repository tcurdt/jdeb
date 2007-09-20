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
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

/**
 * Providing data from an archive keeping permissions and ownerships.
 * 
 * @author tcurdt
 */
public final class DataProducerArchive extends AbstractDataProducer implements DataProducer {

	private final File archive;
	
	public DataProducerArchive( final File pArchive, final String[] pIncludes, final String[] pExcludes, final Mapper[] pMappers ) {
		super(pIncludes, pExcludes, pMappers);
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

				if (!isIncluded(entry.getName())) {
					continue;					
				}				

				entry = map(entry);
				
				if (entry.isDirectory()) {
					receiver.onEachDir(entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
					continue;
				}
				
				receiver.onEachFile(archiveInputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());						
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
