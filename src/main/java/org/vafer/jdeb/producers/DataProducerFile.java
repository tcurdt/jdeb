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

import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

/**
 * DataProducer representing a single file
 * For cross-platform permissions and ownerships you probably want to use a Mapper, too.
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class DataProducerFile extends AbstractDataProducer implements DataProducer {

    private final File file;

    public DataProducerFile(final File pFile, String[] pIncludes, String[] pExcludes, Mapper[] pMapper) {
        super(pIncludes, pExcludes, pMapper);
        file = pFile;
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {

        TarEntry entry = new TarEntry(file.getName());
        entry.setUserId(0);
        entry.setUserName("root");
        entry.setGroupId(0);
        entry.setGroupName("root");
        entry.setMode(TarEntry.DEFAULT_FILE_MODE);

        entry = map(entry);

        entry.setSize(file.length());

        final InputStream inputStream = new FileInputStream(file);
        try {
            pReceiver.onEachFile(inputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
        } finally {
            inputStream.close();
        }

    }

}
