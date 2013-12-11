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
import org.vafer.jdeb.producers.*;

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
        final org.vafer.jdeb.mapping.Mapper[] mappers =
                mapper == null
                        ? null
                        : new org.vafer.jdeb.mapping.Mapper[] { mapper.createMapper() };

        ProducerFactory.KnownType knownType = ProducerFactory.KnownType.forString(type);

        if (knownType == null) {
            throw new IOException(buildUnknownTypeMessage(type, src));
        }

        if (knownType.requiresSource() && (src == null || !src.exists())) {
            if (IGNORE == missingSrc) {
                return;
            }
            throw new FileNotFoundException("Data source not found : " + src);
        }

        final DataProducer p = ProducerFactory.create(knownType, new ProducerFactory.Params() {
            @Override
            public File getSource() {
                return src;
            }
            @Override
            public String getDestination() {
                return dst;
            }
            @Override
            public String getLink() {
                return linkName;
            }
            @Override
            public String getLinkTarget() {
                return linkTarget;
            }

            @Override
            public boolean isSimlink() {
                return symlink;
            }

            @Override
            public String[] getTemplatePaths() {
                return paths;
            }

            @Override
            public String[] getIncludePatterns() {
                return includePatterns;
            }

            @Override
            public String[] getExcludePatterns() {
                return excludePatterns;
            }

            @Override
            public org.vafer.jdeb.mapping.Mapper[] getMappers() {
                return mappers;
            }
        });

        p.produce(pReceiver);
    }

    private static String buildUnknownTypeMessage( final String type,
                                                   final File src ) {
        final StringBuilder b = new StringBuilder("Unknown type '");
        b.append(type).append("' (");
        for (ProducerFactory.KnownType t : ProducerFactory.KnownType.values()) {
            if (t.ordinal() != 0) {
                b.append("|");
            }
            b.append(t.shortName());
        }
        b.append(") for ").append(src);
        return b.toString();

    }

}
