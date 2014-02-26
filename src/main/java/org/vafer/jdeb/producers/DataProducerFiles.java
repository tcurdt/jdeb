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
