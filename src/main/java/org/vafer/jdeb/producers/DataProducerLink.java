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

/**
 * DataProducer representing a single file
 * For cross-platform permissions and ownerships you probably want to use a Mapper, too.
 *
 * @author Thomas Mortagne
 */
public final class DataProducerLink extends AbstractDataProducer implements DataProducer {

    private final String path;
    private final String linkName;
    private final boolean symlink;

    public DataProducerLink(final String path, final String linkName, final boolean symlink, String[] pIncludes, String[] pExcludes, Mapper[] pMapper) {
        super(pIncludes, pExcludes, pMapper);
        this.path = path;
        this.symlink = symlink;
        this.linkName = linkName;
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(path, symlink ? TarArchiveEntry.LF_SYMLINK : TarArchiveEntry.LF_LINK);
        entry.setLinkName(linkName);

        entry.setUserId(Producers.ROOT_UID);
        entry.setUserName(Producers.ROOT_NAME);
        entry.setGroupId(Producers.ROOT_UID);
        entry.setGroupName(Producers.ROOT_NAME);
        entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);

        entry = map(entry);

        pReceiver.onEachLink(path, linkName, symlink, entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode());
    }

}
