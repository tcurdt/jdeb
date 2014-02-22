/*
 * Copyright 2014 The jdeb developers.
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

/**
 * DataProducer representing a single file
 * For cross-platform permissions and ownerships you probably want to use a Mapper, too.
 *
 * @author Torsten Curdt
 */
public final class DataProducerFile extends AbstractDataProducer implements DataProducer {

    private final File file;

    private final String destinationName;

    public DataProducerFile( final File pFile, String pDestinationName, String[] pIncludes, String[] pExcludes, Mapper[] pMapper ) {
        super(pIncludes, pExcludes, pMapper);
        file = pFile;
        destinationName = pDestinationName;
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {
        String fileName;
        if (destinationName != null && destinationName.trim().length() > 0) {
            fileName = destinationName.trim();
        } else {
            fileName = file.getName();
        }

        TarArchiveEntry entry = Producers.defaultFileEntryWithName(fileName);

        entry = map(entry);

        entry.setSize(file.length());

        Producers.produceInputStreamWithEntry(pReceiver, new FileInputStream(file), entry);
    }

}
