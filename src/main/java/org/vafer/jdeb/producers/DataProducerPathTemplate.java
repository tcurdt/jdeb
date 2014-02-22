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

import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;

public class DataProducerPathTemplate extends AbstractDataProducer implements DataProducer {

    private final String[] literalPaths;

    public DataProducerPathTemplate( String[] pLiteralPaths, String[] pIncludes, String[] pExcludes, Mapper[] pMapper ) {
        super(pIncludes, pExcludes, pMapper);
        literalPaths = pLiteralPaths;
    }

    public void produce( DataConsumer pReceiver ) throws IOException {
        for (String literalPath : literalPaths) {
            produceDir(pReceiver, literalPath);
        }
    }

}
