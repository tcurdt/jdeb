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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.maven.Data;
import org.vafer.jdeb.maven.Mapper;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerLink;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.MapVariableResolver;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DebMakerTestCase extends Assert {

    private static final long EXPECTED_MODIFIED_TIME = 1609455600000L;

    @Test
    public void testCreation() throws Exception {
        DataProducer[] data = prepareData();
        File deb = File.createTempFile("jdeb", ".deb");

        File conffile = new File(getClass().getResource("deb/data.tgz").toURI());

        Data conffile1 = new Data();
        conffile1.setType("file");
        conffile1.setSrc(conffile);
        conffile1.setDst("/absolute/path/to/configuration");
        conffile1.setConffile(true);
        Data conffile2 = new Data();
        conffile2.setType("file");
        conffile2.setSrc(conffile);
        conffile2.setConffile(true);

        Mapper mapper = new Mapper();
        FieldUtils.writeField(mapper, "type", "perm", true);
        FieldUtils.writeField(mapper, "prefix", "/absolute/prefix", true);
        FieldUtils.writeField(conffile2, "mapper", mapper, true);

        DebMaker maker =
            new DebMaker(new NullConsole(), Arrays.asList(data), Arrays.<DataProducer>asList(conffile1, conffile2));
        maker.setControl(new File(getClass().getResource("deb/control").toURI()));
        maker.setDeb(deb);

        BinaryPackageControlFile packageControlFile = maker.createDeb(Compression.GZIP);

        assertTrue(packageControlFile.isValid());

        final Map<String, TarArchiveEntry> filesInDeb = new HashMap<>();

        final Set<String> actualConffileContent = new HashSet<>();

        ArchiveWalker.walkControl(deb, new ArchiveVisitor<TarArchiveEntry>() {
            @Override
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                if (entry.getName().equals("./conffiles")) {
                    actualConffileContent.addAll(org.apache.commons.io.IOUtils
                        .readLines(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
                }
            }
        });

        assertEquals("the conttent of the conffiles is wrong",
            new HashSet<>(Arrays.asList("/absolute/path/to/configuration", "/absolute/prefix/data.tgz")),
            actualConffileContent);

        ArchiveWalker.walkData(deb, new ArchiveVisitor<TarArchiveEntry>() {
            @Override
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                filesInDeb.put(entry.getName(), entry);
            }
        }, Compression.GZIP);

        assertTrue("testfile wasn't found in the package", filesInDeb.containsKey("./test/testfile"));
        assertTrue("testfile2 wasn't found in the package", filesInDeb.containsKey("./test/testfile2"));
        assertTrue("testfile3 wasn't found in the package", filesInDeb.containsKey("./test/testfile3"));
        assertTrue("testfile4 wasn't found in the package", filesInDeb.containsKey("./test/testfile4"));
        assertTrue("/link/path-element.ext wasn't found in the package", filesInDeb.containsKey("./link/path-element.ext"));
        assertEquals("/link/path-element.ext has wrong link target", "/link/target-element.ext", filesInDeb.get("./link/path-element.ext").getLinkName());

        assertTrue("Cannot delete the file " + deb, deb.delete());
    }

    @Test
    public void testControlFilesPermissions() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }

        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        Collection<DataProducer> conffileProducers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        DebMaker maker = new DebMaker(new NullConsole(), producers, conffileProducers);
        maker.setDeb(deb);
        maker.setControl(new File("target/test-classes/org/vafer/jdeb/deb/control"));

        maker.createDeb(Compression.NONE);

        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());

        boolean found = ArchiveWalker.walkControl(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                assertFalse("directory found in the control archive", entry.isDirectory());
                assertTrue("prefix", entry.getName().startsWith("./"));

                InformationInputStream infoStream = new InformationInputStream(new ByteArrayInputStream(content));
                IOUtils.copy(infoStream, NullOutputStream.INSTANCE);

                if (infoStream.isShell()) {
                    assertTrue("Permissions on " + entry.getName() + " should be 755", entry.getMode() == 0755);
                } else {
                    assertTrue("Permissions on " + entry.getName() + " should be 644", entry.getMode() == 0644);
                }

                assertTrue(entry.getName() + " doesn't have Unix line endings", infoStream.hasUnixLineEndings());

                assertEquals("user", "root", entry.getUserName());
                assertEquals("group", "root", entry.getGroupName());
            }
        });

        assertTrue("Control files not found in the package", found);
    }

    @Test
    public void testControlFilesVariables() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("name", "jdeb");
        variables.put("version", "1.0");

        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        Collection<DataProducer> conffileProducers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        DebMaker maker = new DebMaker(new NullConsole(), producers, conffileProducers);
        maker.setDeb(deb);
        maker.setControl(new File("target/test-classes/org/vafer/jdeb/deb/control"));
        maker.setResolver(new MapVariableResolver(variables));

        maker.createDeb(Compression.NONE);

        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());

        boolean found = ArchiveWalker.walkControl(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                if (entry.getName().contains("postinst") || entry.getName().contains("prerm")) {
                    String body = new String(content, UTF_8);
                    assertFalse("Variables not replaced in the control file " + entry.getName(), body.contains("[[name]] [[version]]"));
                    assertTrue("Expected variables not found in the control file " + entry.getName(), body.contains("jdeb 1.0"));
                }
                if (entry.getName().contains("control")) {
                    String control = new String(content, UTF_8);
                    assertTrue("Depends missing" + entry.getName(), control.contains("Depends: some-package"));
                }
            }
        });

        assertTrue("Control files not found in the package", found);
    }

    @Test
    public void testDependsIsOmittedWhenEmpty() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }

        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        Collection<DataProducer> conffileProducers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        DebMaker maker = new DebMaker(new NullConsole(), producers, conffileProducers);
        maker.setDeb(deb);
        maker.setControl(new File("target/test-classes/org/vafer/jdeb/deb/controlwithoutdepends"));

        maker.createDeb(Compression.NONE);

        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());

        boolean found = ArchiveWalker.walkControl(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                if (entry.getName().contains("control")) {
                    String control = new String(content, ISO_8859_1);
                    assertFalse("Depends should be omitted in the control file " + entry.getName(), control.contains("Depends:"));
                }
            }
        });

        assertTrue("Control files not found in the package", found);
    }

    @Test
    public void testDependsIsIncludedIfSet() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }
        // Reuse the no-depends-set controlwithoutdepends file then programatically add a depends

        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        Collection<DataProducer> conffileProducers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        DebMaker maker = new DebMaker(new NullConsole(), producers, conffileProducers);
        maker.setDeb(deb);
        maker.setControl(new File("target/test-classes/org/vafer/jdeb/deb/controlwithoutdepends"));

        final String dependsString = "important-dependency ( > 5)";
        maker.setDepends(dependsString);

        maker.createDeb(Compression.NONE);

        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());

        boolean found = ArchiveWalker.walkControl(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                if (entry.getName().contains("control")) {
                    String control = new String(content, ISO_8859_1);

                    assertTrue("Depends should be present in the control file " + entry.getName(), control.contains("Depends:"));
                    assertTrue("The control file " + entry.getName() + " should specify intended dependency '" + dependsString + "'",
                        control.contains("Depends: " + dependsString));
                }
            }
        });

        assertTrue("Control files not found in the package", found);
    }

    @Test
    public void testConstantModifiedTime() throws Exception {
        DataProducer[] data = prepareData();
        File deb = File.createTempFile("jdeb", ".deb");

        Collection<DataProducer> confFileProducers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        DebMaker maker = new DebMaker(new NullConsole(), Arrays.asList(data), confFileProducers);
        maker.setControl(new File(getClass().getResource("deb/control").toURI()));
        maker.setDeb(deb);
        maker.setOutputTimestampMs(EXPECTED_MODIFIED_TIME);

        BinaryPackageControlFile packageControlFile = maker.createDeb(Compression.GZIP);

        assertTrue(packageControlFile.isValid());
        ArchiveWalker.walkArchive(deb, new ArchiveModifiedTimeAssert());
        ModifiedTimeAssert modifiedTimeAssert = new ModifiedTimeAssert();
        ArchiveWalker.walkData(deb, modifiedTimeAssert, Compression.GZIP);
        ArchiveWalker.walkControl(deb, modifiedTimeAssert);
        assertTrue("Cannot delete the file " + deb, deb.delete());
    }

    private DataProducer[] prepareData() throws URISyntaxException {
        File archive1 = new File(getClass().getResource("deb/data.tgz").toURI());
        File archive2 = new File(getClass().getResource("deb/data.tar.bz2").toURI());
        File archive3 = new File(getClass().getResource("deb/data.zip").toURI());
        File directory = new File(getClass().getResource("deb/data").toURI());

        return new DataProducer[] {
                new DataProducerArchive(archive1, null, null, null),
                new DataProducerArchive(archive2, null, null, null),
                new DataProducerArchive(archive3, null, null, null),
                new DataProducerDirectory(directory, null, new String[] { "**/.svn/**" }, null),
                new DataProducerLink("/link/path-element.ext", "/link/target-element.ext", true, null, null, null)
        };
    }

    private static class ModifiedTimeAssert implements ArchiveVisitor<TarArchiveEntry> {
        public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
            assertEquals("Modified time does not match the expected value for " + entry.getName(), entry.getModTime().getTime(), EXPECTED_MODIFIED_TIME);
        }
    }

    private static class ArchiveModifiedTimeAssert implements ArchiveVisitor<ArArchiveEntry> {
        public void visit(ArArchiveEntry entry, byte[] content) throws IOException {
            assertEquals("Modified time does not match the expected value for " + entry.getName(), entry.getLastModified(), EXPECTED_MODIFIED_TIME / 1000);
        }
    }
}
