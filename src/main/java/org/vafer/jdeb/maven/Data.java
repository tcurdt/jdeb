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

package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugins.annotations.Parameter;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerFile;
import org.vafer.jdeb.producers.DataProducerFiles;
import org.vafer.jdeb.producers.DataProducerLink;
import org.vafer.jdeb.producers.DataProducerPathTemplate;

import static org.vafer.jdeb.maven.MissingSourceBehavior.*;

/**
 * Maven "data" element acting as a factory for DataProducers. So far Archive and
 * Directory producers are supported. Both support the usual ant pattern set
 * matching.
 *
 * @author Bryan Sant
 */
public final class Data implements DataProducer {

    @Parameter
    private File src;

    public void setSrc( File src ) {
        this.src = src;
    }

    @Parameter
    private String dst;

    public void setDst( String dst ) {
        this.dst = dst;
    }

    @Parameter
    private String type;

    public void setType( String type ) {
        this.type = type;
    }

    @Parameter
    private MissingSourceBehavior missingSrc = FAIL;

    public void setMissingSrc( String missingSrc ) {
        MissingSourceBehavior value = MissingSourceBehavior.valueOf(missingSrc.trim().toUpperCase());
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + MissingSourceBehavior.class.getSimpleName() + ": " + missingSrc);
        }
        this.missingSrc = value;
    }

    @Parameter
    private String linkName;

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    @Parameter
    private String linkTarget;

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    @Parameter
    private boolean symlink = true;

    public void setSymlink(boolean symlink) {
        this.symlink = symlink;
    }
    
    private boolean conffile = false;

    /**
     * @parameter expression="${conffile}"
     */
    public void setConffile(boolean conffile) {
        this.conffile = conffile;
    }
    
    public boolean getConffile() {
        return this.conffile;
    }

    @Parameter(alias = "includes")
    private String[] includePatterns;

    public void setIncludes( String includes ) {
        includePatterns = splitPatterns(includes);
    }

    @Parameter(alias = "excludes")
    private String[] excludePatterns;

    public void setExcludes( String excludes ) {
        excludePatterns = splitPatterns(excludes);
    }

    @Parameter
    private Mapper mapper;

    @Parameter
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
        org.vafer.jdeb.mapping.Mapper[] mappers = null;
        if (mapper != null) {
            mappers = new org.vafer.jdeb.mapping.Mapper[] { mapper.createMapper() };
        }

        // link type

        if (typeIs("link")) {
            if (linkName == null) {
                throw new RuntimeException("linkName is not set");
            }
            if (linkTarget == null) {
                throw new RuntimeException("linkTarget is not set");
            }

            new DataProducerLink(linkName, linkTarget, symlink, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        // template type

        if (typeIs("template")) {
            checkPaths();
            new DataProducerPathTemplate(paths, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if (typeIs("files")) {
            checkPaths();
            new DataProducerFiles(paths, dst, mappers).produce(pReceiver);
            return;
        }

        // Types that require src to exist

        if (src == null || !src.exists()) {
            if (missingSrc == IGNORE) {
                return;
            } else {
                throw new FileNotFoundException("Data source not found : " + src);
            }
        }

        if (typeIs("file")) {
            new DataProducerFile(src, dst, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if (typeIs("archive")) {
            new DataProducerArchive(src, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        if (typeIs("directory")) {
            new DataProducerDirectory(src, includePatterns, excludePatterns, mappers).produce(pReceiver);
            return;
        }

        throw new IOException("Unknown type '" + type + "' (file|directory|archive|template|link) for " + src);
    }

    private boolean typeIs( final String type ) {
        return type.equalsIgnoreCase(this.type);
    }

    private void checkPaths() {
        if (paths == null || paths.length == 0) {
            throw new RuntimeException("paths parameter is not set");
        }
    }
}
