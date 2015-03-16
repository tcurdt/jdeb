/*
 * Copyright 2015 The jdeb developers.
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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

/**
 * Providing data from an archive keeping permissions and ownerships.
 */
public final class DataProducerArchive extends AbstractDataProducer implements DataProducer {

    private final File archive;

    public DataProducerArchive( final File pArchive, final String[] pIncludes, final String[] pExcludes, final Mapper[] pMappers ) {
        super(pIncludes, pExcludes, pMappers);
        archive = pArchive;
    }

    @Override
    public void produce( final DataConsumer pReceiver ) throws IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(archive));

        CompressorInputStream compressorInputStream = null;

        try {
            compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(is);
        } catch (CompressorException e) {
            // expected if the input file is a zip archive
        }

        if (compressorInputStream != null) {
            is = new BufferedInputStream(compressorInputStream);
        }

        ArchiveInputStream archiveInputStream = null;

        try {
            archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(is);
        } catch (ArchiveException e) {
            throw new IOException("Unsupported archive format: " + archive, e);
        }

        EntryConverter converter = null;

        if (archiveInputStream instanceof TarArchiveInputStream) {

            converter = new EntryConverter() {
                public TarArchiveEntry convert( ArchiveEntry entry ) {
                    return (TarArchiveEntry) entry;
                }
            };

        } else if (archiveInputStream instanceof ZipArchiveInputStream) {

            converter = new EntryConverter() {
                public TarArchiveEntry convert( ArchiveEntry entry ) {
                    ZipArchiveEntry src = (ZipArchiveEntry) entry;
                    final TarArchiveEntry dst = new TarArchiveEntry(src.getName(), true);
                    //TODO: if (src.isUnixSymlink()) {
                    //}

                    dst.setSize(src.getSize());
                    dst.setMode(src.getUnixMode());
                    dst.setModTime(src.getTime());

                    return dst;
                }
            };

        } else {
            throw new IOException("Unsupported archive format: " + archive);
        }


        try {
            while (true) {

                ArchiveEntry archiveEntry = archiveInputStream.getNextEntry();

                if (archiveEntry == null) {
                    break;
                }

                if (!isIncluded(archiveEntry.getName())) {
                    continue;
                }

                TarArchiveEntry entry = converter.convert(archiveEntry);

                entry = map(entry);

                if (entry.isSymbolicLink()) {
                    pReceiver.onEachLink(entry);
                    continue;
                }

                if (entry.isDirectory()) {
                    pReceiver.onEachDir(entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
                    continue;
                }

                pReceiver.onEachFile(archiveInputStream, entry);
            }

        } finally {
            if (archiveInputStream != null) {
                archiveInputStream.close();
            }
        }
    }

    private interface EntryConverter {
        public TarArchiveEntry convert( ArchiveEntry entry );
    }
}