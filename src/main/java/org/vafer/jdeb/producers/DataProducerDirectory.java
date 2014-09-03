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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.tools.ant.DirectoryScanner;
import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.utils.Utils;

/**
 * DataProducer iterating over a directory. For cross-platform permissions and
 * ownerships you probably want to use a Mapper, too.
 *
 * @author Torsten Curdt
 */
public final class DataProducerDirectory extends AbstractDataProducer implements
      DataProducer {
   public static final Map<String, Integer> ORDINAL_MASK = new HashMap<String, Integer>();
   static {
      ORDINAL_MASK.put("OTHERS_READ", 4);
      ORDINAL_MASK.put("OTHERS_WRITE", 2);
      ORDINAL_MASK.put("OTHERS_EXECUTE", 1);
      ORDINAL_MASK.put("GROUP_READ", 32);
      ORDINAL_MASK.put("GROUP_WRITE", 16);
      ORDINAL_MASK.put("GROUP_EXECUTE", 8);
      ORDINAL_MASK.put("OWNER_READ", 256);
      ORDINAL_MASK.put("OWNER_WRITE", 128);
      ORDINAL_MASK.put("OWNER_EXECUTE", 64);
   }
   private final DirectoryScanner scanner = new DirectoryScanner();

   public DataProducerDirectory(final File pDir, final String[] pIncludes,
         final String[] pExcludes, final Mapper[] pMappers) {
      super(pIncludes, pExcludes, pMappers);
      scanner.setBasedir(pDir);
      scanner.setIncludes(pIncludes);
      scanner.setExcludes(pExcludes);
      scanner.setCaseSensitive(true);
      scanner.setFollowSymlinks(true);
   }

   public void produce(final DataConsumer pReceiver) throws IOException {

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

         produceDir(pReceiver, dirname);
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

         produceFile(pReceiver, file, filename);
      }
   }

   @Override
   public void produceFile(final DataConsumer consumer, final File file,
         final String entryName) throws IOException {
      TarArchiveEntry entry = new TarArchiveEntry(entryName, true);
      entry.setUserId(Producers.ROOT_UID);
      entry.setUserName(Producers.ROOT_NAME);
      entry.setGroupId(Producers.ROOT_UID);
      entry.setGroupName(Producers.ROOT_NAME);
      entry.setMode(getOrdinalPermissions(file));
      entry.setSize(file.length());
      entry = map(entry);
      Producers.produceInputStreamWithEntry(consumer,
            new FileInputStream(file), entry);
   }

   public static final int S_IFREG = 0100000;
   public static final int S_IFDIR = 0040000;

   public int getOrdinalPermissions(File file) {
      try {
         Path p = Paths.get(file.getAbsolutePath());
         Set<PosixFilePermission> perms = Files.getPosixFilePermissions(p);
         int mode = 0;
         if (file.isDirectory()) {
            mode |= S_IFDIR;
         } else {
            mode |= S_IFREG;
         }
         for (PosixFilePermission perm : perms) {
            mode |= ORDINAL_MASK.get(perm.name());
         }
         return mode;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private String getFilename(File root, File file) {

      final String relativeFilename = file.getAbsolutePath().substring(
            root.getAbsolutePath().length());

      return Utils.stripLeadingSlash(relativeFilename);
   }

}
