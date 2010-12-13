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
package org.vafer.jdeb.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.tools.ant.types.PatternSet;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerFile;

/**
 * Ant "data" element acting as a factory for DataProducers.
 * So far Archive and Directory producers are supported.
 * Both support the usual ant pattern set matching.
 *
 * @author Torsten Curdt <tcurdt@vafer.org>
 */
public final class Data extends PatternSet implements DataProducer {

    private final Collection mapperWrapper = new ArrayList();

    private File src;

    private String type;

    public void setSrc( final File pSrc ) {
        src = pSrc;
    }

    public void setType( final String pType ) {
        type = pType;
    }

    public void addMapper( final Mapper pMapper ) {
        mapperWrapper.add(pMapper);
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {

        if (!src.exists()) {
            throw new FileNotFoundException("Data source not found : " + src);
        }

        org.vafer.jdeb.mapping.Mapper[] mappers = new org.vafer.jdeb.mapping.Mapper[mapperWrapper.size()];
        final Iterator it = mapperWrapper.iterator();
        for (int i = 0; i < mappers.length; i++) {
            mappers[i] = ((Mapper)it.next()).createMapper();
        }

        if ("file".equalsIgnoreCase(type)) {
            new DataProducerFile(
                    src,
                    getIncludePatterns(getProject()),
                    getExcludePatterns(getProject()),
                    mappers
                    ).produce(pReceiver);
            return;
        }

        if ("archive".equalsIgnoreCase(type)) {
            new DataProducerArchive(
                    src,
                    getIncludePatterns(getProject()),
                    getExcludePatterns(getProject()),
                    mappers
                    ).produce(pReceiver);
            return;
        }

        if ("directory".equalsIgnoreCase(type)) {
            new DataProducerDirectory(
                    src,
                    getIncludePatterns(getProject()),
                    getExcludePatterns(getProject()),
                    mappers
                    ).produce(pReceiver);
            return;
        }

        // @deprecated

        if (src.isFile()) {
            new DataProducerArchive(
                src,
                getIncludePatterns(getProject()),
                getExcludePatterns(getProject()),
                mappers
                ).produce(pReceiver);
        } else {
            new DataProducerDirectory(
                src,
                getIncludePatterns(getProject()),
                getExcludePatterns(getProject()),
                mappers
                ).produce(pReceiver);
        }
    }
}
