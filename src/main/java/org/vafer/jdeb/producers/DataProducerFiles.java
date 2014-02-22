package org.vafer.jdeb.producers;

import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Data producer that places multiple files into a single
 * destination directory.
 *
 * @author Roman Kashitsyn
 */
public class DataProducerFiles extends AbstractDataProducer {

    private final String[] files;
    private final String destDir;

    public DataProducerFiles( final String[] files,
                              final String destDir,
                              final Mapper[] mappers ) {
        super(null, null, mappers);
        this.files = files;
        this.destDir = destDir;
    }

    @Override
    public void produce( DataConsumer receiver ) throws IOException {
        boolean hasDestDir = !Utils.isNullOrEmpty(destDir);

        for (String fileName : files) {
            File f = new File(fileName);

            if (hasDestDir) {
                fileName = Utils.movePath(fileName, destDir);
            }

            produceFile(receiver, f, fileName);
        }
    }
}
