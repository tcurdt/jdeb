/*
 * Copyright 2012 The Apache Software Foundation.
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
package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerFile;
import org.vafer.jdeb.producers.DataProducerPathTemplate;

import static org.vafer.jdeb.maven.MissingSourceBehavior.FAIL;
import static org.vafer.jdeb.maven.MissingSourceBehavior.IGNORE;

/**
 * Maven "data" element acting as a factory for DataProducers. So far Archive and
 * Directory producers are supported. Both support the usual ant pattern set
 * matching.
 *
 * @author Bryan Sant
 */
public final class Data implements DataProducer {

    private File src;

    /**
     * @parameter expression="${src}"
     * @required
     */
    public void setSrc( File src ) {
        this.src = src;
    }

    private String dst;

    /**
     * @parameter expression="${dst}"
     * @required
     */
    public void setDst( String dst ) {
        this.dst = dst;
    }

    private String type;

    /**
     * @parameter expression="${type}"
     */
    public void setType( String type ) {
        this.type = type;
    }

    private MissingSourceBehavior missingSrc = FAIL;

    /**
     * @parameter expression="${missingSrc}"
     */
    public void setMissingSrc( String missingSrc ) {
        MissingSourceBehavior value = MissingSourceBehavior.valueOf(missingSrc.trim().toUpperCase());
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + MissingSourceBehavior.class.getSimpleName() + ": " + missingSrc);
        }
        this.missingSrc = value;
    }

    /**
     * @parameter expression="${includes}" alias="includes"
     */
    public void setIncludes( String includes ) {
        includePatterns = splitPatterns(includes);
    }

    private String[] includePatterns;

    /**
     * @parameter expression="${excludes}" alias="excludes"
     */
    public void setExcludes( String excludes ) {
        excludePatterns = splitPatterns(excludes);
    }

    private String[] excludePatterns;
    /**
     * @parameter expression="${mapper}"
     */
    private Mapper mapper;

    /**
     * @parameter expression="${paths}"
     */
    private String[] paths;

    /* For testing only */
    void setPaths( String[] paths ) {
        this.paths = paths;
    }

    public String[] splitPatterns( String patterns ) {
        String[] result = null;
        if (patterns != null && patterns.length() > 0) {
            List<String> tokens = new ArrayList<String>();
            StringTokenizer tok = new StringTokenizer(patterns, ", ", false);
            while (tok.hasMoreTokens()) {
                tokens.add(tok.nextToken());
            }
            result = tokens.toArray(new String[tokens.size()]);
        }
        return result;
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {
        if (src != null && !src.exists()) {
            if (missingSrc == IGNORE) {
                return;
            } else {
                throw new FileNotFoundException("Data source not found : " + src);
            }
        }

        if (src == null && (paths == null || paths.length == 0)) {
            throw new RuntimeException("src or paths not set");
        }

        org.vafer.jdeb.mapping.Mapper[] mappers = null;
        if (mapper != null) {
            mappers = new org.vafer.jdeb.mapping.Mapper[] { mapper.createMapper() };
        }

        if ("file".equalsIgnoreCase(type)) {
            new DataProducerFile(src, dst, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if ("archive".equalsIgnoreCase(type)) {
            new DataProducerArchive(src, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if ("directory".equalsIgnoreCase(type)) {
            new DataProducerDirectory(src, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if ("template".equalsIgnoreCase(type)) {
            new DataProducerPathTemplate(paths, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        throw new IOException("Unknown type '" + type + "' (file|directory|archive|template) for " + src);
    }

}
