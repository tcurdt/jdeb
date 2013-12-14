/*
 * Copyright 2013 The jdeb developers.
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.vafer.jdeb.Compression;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.utils.Utils;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * DataProducer representing a man page entry.
 *
 * Ensures that man page is compressed with appropriate compression level
 * and is placed to correct location.
 *
 * @author Roman Kashitsyn <roman.kashitsyn@gmail.com>
 */
public class DataProducerManPage extends AbstractDataProducer {

    private final static int DEFAULT_CATEGORY = 1;
    private final static int BAD_CATEGORY = -1;
    private final static Compression COMPRESSOR = Compression.GZIP;
    private final static String MAN_PAGE_PREFIX = "/usr/share/man/man";

    final File file;

    final String destination;

    public DataProducerManPage( final File pFile,
                                String pDestinationName,
                                String[] pIncludes,
                                String[] pExcludes,
                                Mapper[] pMapper ) {
        super(pIncludes, pExcludes, pMapper);
        file = pFile;
        destination = makeDestination(pDestinationName, pFile);
    }

    @Override
    public void produce( final DataConsumer receiver ) throws IOException {

        TarArchiveEntry entry = Producers.defaultFileEntryWithName(destination);

        entry = map(entry);

        if (isCompressedFile(FilenameUtils.getExtension(file.getName()))) {
            entry.setSize(file.length());
            Producers.produceInputStreamWithEntry(receiver, new FileInputStream(file), entry);
        } else {
            produceCompressedPage(receiver, entry);
        }
    }

    private static boolean isCompressedFile( final String extension ) {

        return Compression.toEnum(extension) != null;
    }

    private void produceCompressedPage( final DataConsumer receiver,
                                        final TarArchiveEntry entry ) throws IOException {
        try {
            final byte[] pageBytes = getCompressedPageBytes();
            entry.setSize(pageBytes.length);
            Producers.produceInputStreamWithEntry(receiver, new ByteArrayInputStream(pageBytes), entry);
        } catch (CompressorException e) {
            throw new IOException(e);
        }
    }

    private byte[] getCompressedPageBytes() throws IOException, CompressorException {
        InputStream inputStream = null;
        final ByteArrayOutputStream inMemoryOut = new ByteArrayOutputStream();
        final OutputStream compressedOut = new GZIPOutputStream(inMemoryOut) {
            {
                def.setLevel(Deflater.BEST_COMPRESSION);
            }
        };

        try  {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            Utils.copy(inputStream, compressedOut);
        } finally {
            IOUtils.closeQuietly(compressedOut);
            IOUtils.closeQuietly(inputStream);
        }

        // Updating compression level to avoid the lintian
        // `manpage-not-compressed-with-max-compression` error
        return setBestCompressionFlag(inMemoryOut.toByteArray());
    }

    /**
     * Sets XFLAG header field to BEST COMPRESSION.
     * See http://www.gzip.org/zlib/rfc-gzip.html for details.
     *
     * @param bytes compressed file bytes, must be a valid GZIP file
     * @return augmented file bytes
     */
    private static byte[] setBestCompressionFlag( final byte[] bytes ) {

        final int XFLAG_HEADER_INDEX = 8;
        final byte BEST_COMPRESSION_FLAG = 2;

        bytes[XFLAG_HEADER_INDEX] = BEST_COMPRESSION_FLAG;
        return bytes;
    }

    static String makeDestination( final String dest, final File file ) {
        if (dest != null && dest.length() > 0) {
            return dest;
        }

        String fileName = file.getName();
        final String extension = FilenameUtils.getExtension(fileName);

        if (isCompressedFile(extension)) {
            final String fileNameWithoutSuffix = FilenameUtils.removeExtension(fileName);
            final int category = extractCategory(fileNameWithoutSuffix);
            fileName = addCategory(fileNameWithoutSuffix, extension, category);
        } else {
            final int category = extractCategory(fileName);
            final String newExtension = COMPRESSOR.getExtension().substring(1);
            fileName = addCategory(fileName, newExtension, category);
        }

        return fileName;
    }

    private static String addCategory( final String base,
                                       final String extension,
                                       final int category ) {
        if (category == BAD_CATEGORY) {
            return MAN_PAGE_PREFIX + DEFAULT_CATEGORY + "/" +
                    base + "." + DEFAULT_CATEGORY + "." + extension;
        }
        return MAN_PAGE_PREFIX + category + "/" + base + "." + extension;
    }

    private static int extractCategory( final String fileName ) {
        final String suffix = FilenameUtils.getExtension(fileName);
        if (suffix.length() != 1 || !Character.isDigit(suffix.charAt(0))) {
            return BAD_CATEGORY;
        }
        return suffix.charAt(0) - '0';
    }
}
