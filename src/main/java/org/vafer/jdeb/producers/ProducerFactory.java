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

import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

import java.io.File;

/**
 * Factory to create data producers at runtime.
 *
 * @author Roman Kashitsyn <roman.kashitsyn@gmail.com>
 */
public class ProducerFactory {

    private ProducerFactory() {
    }

    /**
     * Enumeration of all known producer types.
     */
    public enum KnownType {
        FILE("file", true),
        ARCHIVE("archive", true),
        DIRECTORY("directory", true),
        MAN_PAGE("man-page", true),
        LINK("link", false),
        TEMPLATE("template", false);

        private final String name;
        private final boolean requiresSource;

        private KnownType( final String name,
                           boolean requiresSource ) {
            this.name = name;
            this.requiresSource = requiresSource;
        }

        public boolean requiresSource() {
            return requiresSource;
        }

        public String shortName() {
            return name;
        }

        public static KnownType forString( final String typeName ) {
            for (KnownType type : values()) {
                if (type.shortName().equalsIgnoreCase(typeName)) {
                    return type;
                }
            }
            return null;
        }
    }

    public static abstract class Params {
        public File getSource() {
            return null;
        }

        public String getDestination() {
            return null;
        }

        public String getLink() {
            return null;
        }

        public String getLinkTarget() {
            return null;
        }

        public String[] getIncludePatterns() {
            return null;
        }

        public String[] getExcludePatterns() {
            return null;
        }

        public String[] getTemplatePaths() {
            return null;
        }

        public boolean isSimlink() {
            return false;
        }

        public Mapper[] getMappers() {
            return null;
        }
    }

    /**
     * Creates a data producer in runtime.
     *
     * @param type   producer type
     * @param params parameter provider
     * @return producer or null if null type is given
     * @throws java.lang.IllegalArgumentException if required parameters are missing
     */
    public static DataProducer create( final KnownType type,
                                       final Params params ) {
        switch (type) {
            case FILE:
                return new DataProducerFile(
                        params.getSource(),
                        params.getDestination(),
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );

            case ARCHIVE:
                return new DataProducerArchive(
                        params.getSource(),
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );

            case DIRECTORY:
                return new DataProducerDirectory(
                        params.getSource(),
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );
            case TEMPLATE:
                final String[] paths = params.getTemplatePaths();
                if (paths == null || paths.length == 0) {
                    throw new IllegalArgumentException("paths is not set");
                }

                return new DataProducerPathTemplate(
                        paths,
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );
            case LINK:
                final String linkName = params.getLink();
                final String linkTarget = params.getLinkTarget();

                if (linkName == null) {
                    throw new IllegalArgumentException("linkName is not set");
                }
                if (linkTarget == null) {
                    throw new IllegalArgumentException("linkTarget is not set");
                }

                return new DataProducerLink(
                        linkName,
                        linkTarget,
                        params.isSimlink(),
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );
            case MAN_PAGE:
                return new DataProducerManPage(
                        params.getSource(),
                        params.getDestination(),
                        params.getIncludePatterns(),
                        params.getExcludePatterns(),
                        params.getMappers()
                );
            default:
                return null;
        }
    }
}
