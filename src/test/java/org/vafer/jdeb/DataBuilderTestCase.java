/*
 * Copyright 2007-2024 The jdeb developers.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Test;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerFile;
import org.vafer.jdeb.producers.DataProducerFileSet;
import org.vafer.jdeb.producers.DataProducerLink;

public final class DataBuilderTestCase extends Assert {

    private static final long EXPECTED_MODIFIED_TIME = 1609455600000L;

    /**
     * Checks if the file paths in the md5sums file use only unix file separators
     * (this test can only fail on Windows)
     */
    @Test
    public void testBuildDataWithFileSet() throws Exception {
        DataBuilder builder = new DataBuilder(new NullConsole(), null);

        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        FileSet fileset = new FileSet();
        fileset.setDir(new File(getClass().getResource("deb/data").toURI()));
        fileset.setIncludes("**/*");
        fileset.setProject(project);

        StringBuilder md5s = new StringBuilder();
        builder.buildData(Arrays.asList((DataProducer) new DataProducerFileSet(fileset)), new File("target/data.tar"), md5s, new TarOptions().compression(Compression.GZIP));

        assertTrue("empty md5 file", md5s.length() > 0);
        assertFalse("windows path separator found", md5s.indexOf("\\") != -1);
        assertTrue("two spaces between md5 and file, no leading slash, or dot, on file path", md5s.toString().equals("8bc944dbd052ef51652e70a5104492e3  test/testfile\n"));
    }

    @Test
    public void testCreateParentDirectories() throws Exception {
        File archive = prepareArchive();

        DataBuilder builder = new DataBuilder(new NullConsole(), null);

        DataProducer producer = new DataProducerFile(new File("pom.xml"), "/usr/share/myapp/pom.xml", null, null, null);

        builder.buildData(Arrays.asList(producer), archive, new StringBuilder(), new TarOptions().compression(Compression.NONE));

        int count = 0;
        try (TarArchiveInputStream in = new TarArchiveInputStream(new FileInputStream(archive))) {
            while (in.getNextEntry() != null) {
                count++;
            }
        }

        assertEquals("entries", 4, count);
    }

    @Test
    public void testModifiedTimeIsSet() throws Exception {
        File dir = prepareSubdir();
        DataProducer dirProducer = new DataProducerDirectory(dir, null, null, null);
        DataProducer fileProducer = new DataProducerFile(new File("pom.xml"), "/usr/share/myapp/pom.xml", null, null, null);
        DataProducer linkProducer = new DataProducerLink("pomLink.xml", "/usr/share/myapp/pom.xml", true, null, null, null);
        File archive = prepareArchive();

        DataBuilder builder = new DataBuilder(new NullConsole(), EXPECTED_MODIFIED_TIME);
        builder.buildData(Arrays.asList(fileProducer, linkProducer, dirProducer), archive, new StringBuilder(), new TarOptions().compression(Compression.NONE));

        assertExpectedModTimeInArchive(archive);
    }

    private File prepareArchive() {
        File archive = new File("target/data.tar");
        if (archive.exists()) {
            archive.delete();
        }
        return archive;
    }

    private File prepareSubdir() throws IOException {
        File subDir = new File("target/subDir");
        subDir.mkdir();

        File file = new File(subDir, "file.txt");
        file.createNewFile();

        File nestedDir = new File(subDir, "nested-dir");
        nestedDir.mkdir();

        File file2 = new File(nestedDir, "file2.txt");
        file2.createNewFile();

        return subDir;
    }

    private void assertExpectedModTimeInArchive(File archive) throws IOException {
        try (TarArchiveInputStream in = new TarArchiveInputStream(new FileInputStream(archive))) {
            ArchiveEntry entry = in.getNextEntry();
            while (entry != null) {
                assertEquals(EXPECTED_MODIFIED_TIME, entry.getLastModifiedDate().getTime());

                entry = in.getNextEntry();
            }
        }
    }
}
