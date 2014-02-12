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
 * @author Torsten Curdt
 */
public final class Data extends PatternSet implements DataProducer {

    private final Collection<Mapper> mapperWrapper = new ArrayList<Mapper>();

    private File src;

    private String type;

    private Boolean conffile;

    private String destinationName;

    public void setSrc(File src) {
        this.src = src;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConffile(Boolean conffile) {
        this.conffile = conffile;
    }
    
    public Boolean getConffile() {
        return this.conffile;
    }

    public void setDst(String destinationName) {
        this.destinationName = destinationName;
    }

    public void addMapper(Mapper mapper) {
        mapperWrapper.add(mapper);
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {

        if (src == null || !src.exists()) {
            throw new FileNotFoundException("Data source not found : " + src);
        }

        org.vafer.jdeb.mapping.Mapper[] mappers = new org.vafer.jdeb.mapping.Mapper[mapperWrapper.size()];
        final Iterator<Mapper> it = mapperWrapper.iterator();
        for (int i = 0; i < mappers.length; i++) {
            mappers[i] = it.next().createMapper();
        }

        if ("file".equalsIgnoreCase(type)) {
            new DataProducerFile(
                src,
                destinationName,
                getIncludePatterns(getProject()),
                getExcludePatterns(getProject()),
                mappers
            ).produce(pReceiver);

        } else if ("archive".equalsIgnoreCase(type)) {
            new DataProducerArchive(
                src,
                getIncludePatterns(getProject()),
                getExcludePatterns(getProject()),
                mappers
            ).produce(pReceiver);

        } else if ("directory".equalsIgnoreCase(type)) {
            new DataProducerDirectory(
                src,
                getIncludePatterns(getProject()),
                getExcludePatterns(getProject()),
                mappers
            ).produce(pReceiver);
        }
    }
}
