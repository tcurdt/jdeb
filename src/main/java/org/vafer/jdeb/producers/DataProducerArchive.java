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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
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

        TarInputStream archiveInputStream = null;
        try {
            archiveInputStream = new TarInputStream(getCompressedInputStream(new FileInputStream(archive)));

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


    /**
     * TODO: replace by commons compress
     * 
     * Guess the compression used by looking at the first bytes of the stream.
     */
    private InputStream getCompressedInputStream(InputStream in) throws IOException {
    	
        PushbackInputStream pin = new PushbackInputStream(in, 2);
        byte[] header = new byte[2];
        if (pin.read(header) != header.length) {
            throw new IOException("Could not read header");
        }

        if (header[0] == (byte) 0x1f && header[1] == (byte) 0x8b) {
            pin.unread(header);
            return new GZIPInputStream(pin);
        } else if (header[0] == 'B' && header[1] == 'Z') {
            return new CBZip2InputStream(pin);
        } else {
            throw new IOException("Unsupported archive format : " + archive);
        }
    }
    
}
