/*
 * Copyright 2007-2026 The jdeb developers.
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
package org.vafer.jdeb;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.producers.DataProducerLink;
import org.vafer.jdeb.producers.DataProducerPathTemplate;

@RunWith(Parameterized.class)
public class DebMakerLongNameTestCase extends Assert {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      { "gnu", 1, 1 },
      { "posix", 1, 1 },
      { "gnu", 5, 30 },
      { "posix", 5, 30 },
      { "gnu", 100, 1 },
      { "posix", 100, 1 },
      { "gnu", 100, 100 },
      { "posix", 100, 100 },
    });
  }

  @Parameter
  public String tarLongFileMode;

  @Parameter(1)
  public int numDirectories;

  @Parameter(2)
  public int nameLength;

  private final String FOLDER_SEPARATOR = "/";

  @Test
  public void testLongLinkName() throws Exception {
    String longPathDirName = createLongPath(numDirectories, nameLength);
    String shortPathDirName = "/var/log/short";
    Mapper mapper = new PermMapper(-1, -1, "root", "root", -1, -1, 0, null);
    String[] directories = { longPathDirName };
    Mapper[] mappers = { mapper };
    DataProducerPathTemplate dataProducer = new DataProducerPathTemplate(
      directories,
      null,
      null,
      mappers
    );

    DataProducer linkProducer = new DataProducerLink(
      shortPathDirName,
      longPathDirName,
      true,
      null,
      null,
      null
    );

    File deb = folder.newFile("file.deb");

    List<DataProducer> producers = Arrays.asList(linkProducer, dataProducer);

    DebMaker maker = new DebMaker(new NullConsole(), producers, null);
    maker.setEncoding(StandardCharsets.UTF_8);
    maker.setControl(new File(getClass().getResource("deb/control").toURI()));
    maker.setTarLongFileMode(tarLongFileMode);
    maker.setDeb(deb);
    maker.setCompression(Compression.GZIP.toString());

    BinaryPackageControlFile packageControlFile = maker.createDeb(
      Compression.GZIP
    );

    assertTrue(packageControlFile.isValid());

    final Map<String, TarArchiveEntry> filesInDeb = new HashMap<>();

    ArchiveWalker.walkData(
      deb,
      (entry, content) -> filesInDeb.put(entry.getName(), entry),
      Compression.GZIP
    );

    assertTrue(
      "longname file wasn't found in the package",
      filesInDeb.containsKey(
        "." + FOLDER_SEPARATOR + longPathDirName + FOLDER_SEPARATOR
      )
    );
    assertTrue(
      "short wasn't found in the package",
      filesInDeb.containsKey("." + shortPathDirName)
    );
    assertTrue("Cannot delete the file " + deb, deb.delete());
  }

  private String createLongPath(int numDirectories, int nameLength) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numDirectories; ++i) {
      builder.append(i);
      builder.append(FOLDER_SEPARATOR);
    }
    for (int i = 0; i < nameLength; ++i) {
      builder.append(i);
    }
    return builder.toString();
  }
}
