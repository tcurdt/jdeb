/*
 * Copyright 2010 The Apache Software Foundation.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

/**
 * Providing data from an archive keeping permissions and ownerships.
 * 
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class DataProducerArchive extends AbstractDataProducer implements DataProducer {

    private final File archive;
    
    public DataProducerArchive( final File pArchive, final String[] pIncludes, final String[] pExcludes, final Mapper[] pMappers ) {
        super(pIncludes, pExcludes, pMappers);
        archive = pArchive;
    }
        
    public void produce( final DataConsumer pReceiver ) throws IOException {

    	InputStream is = new BufferedInputStream(new FileInputStream(archive));
    	
    	CompressorInputStream compressorInputStream = null; 
    		
    	try {
    		// FIXME remove once commons 1.1 is out
    		
    		final String fn = archive.getName();
    		if (fn.endsWith("gz")) {
    			compressorInputStream = new CompressorStreamFactory().createCompressorInputStream("gz", is);
    		} else if (fn.endsWith("bz2")){
    			compressorInputStream = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
    		}

    		// compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(is);    		
    	
    	} catch(CompressorException e) {
    	}
    	
    	if (compressorInputStream != null) {
    		is = new BufferedInputStream(compressorInputStream);
    	}
    	
    	ArchiveInputStream archiveInputStream = null;
    	
    	try {
    		archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(is);
    	} catch(ArchiveException e) {
            throw new IOException("Unsupported archive format : " + archive, e);    		
    	}

    	EntryConverter converter = null;
    	
    	if (archiveInputStream instanceof TarArchiveInputStream) {

    		converter = new EntryConverter() {
    	        public TarEntry convert(ArchiveEntry entry) {
    	        	TarArchiveEntry src = (TarArchiveEntry)entry;
	        		TarEntry dst = new TarEntry(entry.getName());

	        		dst.setSize(src.getSize());
	        		dst.setGroupName(src.getGroupName());
	        		dst.setGroupId(src.getGroupId());
	        		dst.setUserId(src.getUserId());
	        		dst.setMode(src.getMode());
	        		dst.setModTime(src.getModTime());

	        		return dst;
    	        }
            };    	
    	
    	} else {
            throw new IOException("Unsupported archive format : " + archive);    		
    	}
    	
    	
        try {
            while(true) {
                
            	ArchiveEntry archiveEntry = archiveInputStream.getNextEntry();

                if (archiveEntry == null) {
                    break;
                }

                if (!isIncluded(archiveEntry.getName())) {
                    continue;                   
                }               

                TarEntry entry = converter.convert(archiveEntry);
                
                entry = map(entry);
                
                if (entry.isDirectory()) {
                    pReceiver.onEachDir(entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
                    continue;
                }
                pReceiver.onEachFile(archiveInputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());                      
            }

        } finally {
            if (archiveInputStream != null) {
                archiveInputStream.close();
            }
        }       
    }
    
    private interface EntryConverter {
        public TarEntry convert(ArchiveEntry entry);    	
    }    
}
