/*
 * Copyright 2015 The jdeb developers.
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.tar.TarEntry;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.utils.SymlinkUtils;

/**
 * DataProducer providing data from an Ant fileset. TarFileSets are also
 * supported with their permissions.
 */
public final class DataProducerFileSet implements DataProducer {

    private final FileSet fileset;

    public DataProducerFileSet( final FileSet fileset ) {
        this.fileset = fileset;
    }

    @Override
    public void produce( final DataConsumer pReceiver ) throws IOException {
        String user = Producers.ROOT_NAME;
        int uid = Producers.ROOT_UID;
        String group = Producers.ROOT_NAME;
        int gid = Producers.ROOT_UID;
        int filemode = TarEntry.DEFAULT_FILE_MODE;
        int dirmode = TarEntry.DEFAULT_DIR_MODE;
        String prefix = "";

        if (fileset instanceof Tar.TarFileSet) {
            Tar.TarFileSet tarfileset = (Tar.TarFileSet) fileset;
            user = tarfileset.getUserName();
            uid = tarfileset.getUid();
            group = tarfileset.getGroup();
            gid = tarfileset.getGid();
            filemode = tarfileset.getMode();
            dirmode = tarfileset.getDirMode(tarfileset.getProject());
            prefix = tarfileset.getPrefix(tarfileset.getProject());
        }

        final DirectoryScanner scanner = fileset.getDirectoryScanner(fileset.getProject());
        scanner.scan();

        final File basedir = scanner.getBasedir();
        
        for (String directory : scanner.getIncludedDirectories()) {
            String name = directory.replace('\\', '/');

            pReceiver.onEachDir(prefix + "/" + name, null, user, uid, group, gid, dirmode, 0);
        }
        
        for (String filename : scanner.getIncludedFiles()) {
            final String name = filename.replace('\\', '/');
            final File file = new File(basedir, name);

            final InputStream inputStream = new FileInputStream(file);
            try {
                final String entryName = prefix + "/" + name;

                final File entryPath = new File(entryName);

                final boolean symbolicLink = SymlinkUtils.isSymbolicLink(entryPath);
                final TarArchiveEntry e;
                if (symbolicLink) {
                    e = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
                    e.setLinkName(SymlinkUtils.readSymbolicLink(entryPath));
                } else {
                    e = new TarArchiveEntry(entryName, true);
                }

                e.setUserId(uid);
                e.setGroupId(gid);
                e.setUserName(user);
                e.setGroupName(group);
                e.setMode(filemode);
                e.setSize(file.length());

                pReceiver.onEachFile(inputStream, e);
            } finally {
                inputStream.close();
            }
        }
    }
}
