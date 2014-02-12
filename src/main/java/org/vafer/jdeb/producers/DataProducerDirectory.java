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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.utils.Utils;

/**
 * DataProducer iterating over a directory.
 * For cross-platform permissions and ownerships you probably want to use a Mapper, too.
 *
 * @author Torsten Curdt
 */
public final class DataProducerDirectory extends AbstractDataProducer implements DataProducer {

    private final DirectoryScanner scanner = new DirectoryScanner();

    public DataProducerDirectory( final File pDir, final String[] pIncludes, final String[] pExcludes, final Mapper[] pMappers ) {
        super(pIncludes, pExcludes, pMappers);
        scanner.setBasedir(pDir);
        scanner.setIncludes(pIncludes);
        scanner.setExcludes(pExcludes);
        scanner.setCaseSensitive(true);
        scanner.setFollowSymlinks(true);
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {

        scanner.scan();

        final File baseDir = scanner.getBasedir();
        
        for (String dir : scanner.getIncludedDirectories()) {
            final File file = new File(baseDir, dir);
            String dirname = getFilename(baseDir, file);

            if ("".equals(dirname)) {
                continue;
            }

            if ('/' != File.separatorChar) {
                dirname = dirname.replace(File.separatorChar, '/');
            }

            if (!isIncluded(dirname)) {
                continue;
            }

            if (!dirname.endsWith("/")) {
                dirname += "/";
            }

            TarArchiveEntry entry = new TarArchiveEntry(dirname, true);
            entry.setUserId(0);
            entry.setUserName("root");
            entry.setGroupId(0);
            entry.setGroupName("root");
            entry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);

            entry = map(entry);

            entry.setSize(0);

            pReceiver.onEachDir(entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
        }


        for (String f : scanner.getIncludedFiles()) {
            final File file = new File(baseDir, f);
            String filename = getFilename(baseDir, file);

            if ('/' != File.separatorChar) {
                filename = filename.replace(File.separatorChar, '/');
            }

            if (!isIncluded(filename)) {
                continue;
            }

            TarArchiveEntry entry = new TarArchiveEntry(filename, true);
            entry.setUserId(0);
            entry.setUserName("root");
            entry.setGroupId(0);
            entry.setGroupName("root");
            entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);

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

    private String getFilename( File root, File file ) {

        final String relativeFilename = file.getAbsolutePath().substring(root.getAbsolutePath().length());

        return Utils.stripLeadingSlash(relativeFilename);
    }

}