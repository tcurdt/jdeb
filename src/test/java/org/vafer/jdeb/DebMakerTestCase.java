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

package org.vafer.jdeb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.vafer.jdeb.debian.BinaryPackageControlFile;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerLink;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.MapVariableResolver;

public class DebMakerTestCase extends TestCase {

    public void testCreation() throws Exception {

        File control = new File(getClass().getResource("deb/control/control").toURI());
        File archive1 = new File(getClass().getResource("deb/data.tgz").toURI());
        File archive2 = new File(getClass().getResource("deb/data.tar.bz2").toURI());
        File archive3 = new File(getClass().getResource("deb/data.zip").toURI());
        File directory = new File(getClass().getResource("deb/data").toURI());

        DataProducer[] data = new DataProducer[] {
            new DataProducerArchive(archive1, null, null, null),
            new DataProducerArchive(archive2, null, null, null),
            new DataProducerArchive(archive3, null, null, null),
            new DataProducerDirectory(directory, null, new String[] { "**/.svn/**" }, null),
            new DataProducerLink("/link/path-element.ext", "/link/target-element.ext", true, null, null, null)
        };

        File deb = File.createTempFile("jdeb", ".deb");

        DebMaker maker = new DebMaker(new NullConsole(), Arrays.asList(data), null);
        maker.setControl(new File(getClass().getResource("deb/control").toURI()));
        maker.setDeb(deb);
        
        BinaryPackageControlFile packageControlFile = maker.createDeb(Compression.GZIP);
        
        assertTrue(packageControlFile.isValid());

        final Map<String, TarArchiveEntry> filesInDeb = new HashMap<String, TarArchiveEntry>();
        
        ArchiveWalker.walkData(deb, new ArchiveVisitor<TarArchiveEntry>() {
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
                IOUtils.copy(infoStream, NullOutputStream.NULL_OUTPUT_STREAM);
                
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

    public void testControlFilesVariables() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }
        
        Map<String, String> variables = new HashMap<String, String>();
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
                    String body = new String(content, "ISO-8859-1");
                    assertFalse("Variables not replaced in the control file " + entry.getName(), body.contains("[[name]] [[version]]"));
                    assertTrue("Expected variables not found in the control file " + entry.getName(), body.contains("jdeb 1.0"));
                }
            }
        });
        
        assertTrue("Control files not found in the package", found);
    }
}
