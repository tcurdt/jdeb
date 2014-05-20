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
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.vafer.jdeb.mapping.PermMapper;
import org.vafer.jdeb.producers.DataProducerArchive;
import org.vafer.jdeb.producers.DataProducerDirectory;
import org.vafer.jdeb.producers.DataProducerLink;
import org.vafer.jdeb.synology.BinaryPackageInfoFile;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.MapVariableResolver;

public class SpkMakerTestCase extends TestCase {

    public void testCreation() throws Exception {
        File spk = new File("target/test-classes/test-creation.spk");
        if (spk.exists() && !spk.delete()) {
            fail("Couldn't delete " + spk);
        }

        File info = new File(getClass().getResource("spk/info").toURI());
        File archive1 = new File(getClass().getResource("spk/data.tgz").toURI());
        File archive2 = new File(getClass().getResource("spk/data.tar.bz2").toURI());
        File archive3 = new File(getClass().getResource("spk/data.zip").toURI());
        File directory = new File(getClass().getResource("spk/data").toURI());

        DataProducer[] data = new DataProducer[] {
            new DataProducerArchive(archive1, null, null, null),
            new DataProducerArchive(archive2, null, null, null),
            new DataProducerArchive(archive3, null, null, null),
            new DataProducerDirectory(directory, null, new String[] { "**/.svn/**" }, null),
            new DataProducerLink("/link/path-element.ext", "/link/target-element.ext", true, null, null, null)
        };

        SpkMaker maker = new SpkMaker(new NullConsole(), Arrays.asList(data));
        maker.setInfo(info);
        maker.setScripts(new File(getClass().getResource("spk/scripts").toURI()));
        maker.setSpk(spk);
        
        BinaryPackageInfoFile packageInfoFile = maker.createSpk(Compression.GZIP);
        
        assertTrue(packageInfoFile.isValid());

        final Map<String, TarArchiveEntry> filesInSpk = new HashMap<String, TarArchiveEntry>();
        
        ArchiveWalker.walkPackage(spk, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                filesInSpk.put(entry.getName(), entry);
            }
        });
        
        assertTrue("testfile wasn't found in the package", filesInSpk.containsKey("test/testfile"));
        assertTrue("testfile2 wasn't found in the package", filesInSpk.containsKey("test/testfile2"));
        assertTrue("testfile3 wasn't found in the package", filesInSpk.containsKey("test/testfile3"));
        assertTrue("testfile4 wasn't found in the package", filesInSpk.containsKey("test/testfile4"));
        assertTrue("/link/path-element.ext wasn't found in the package", filesInSpk.containsKey("link/path-element.ext"));
        //assertEquals("/link/path-element.ext has wrong link target", "/link/target-element.ext", filesInSpk.get("link/path-element.ext").getLinkName());

        assertTrue("Cannot delete the file " + spk, spk.delete());
    }

    public void testScriptFilesPermissions() throws Exception {
        File spk = new File("target/test-classes/test-permissions.spk");
        if (spk.exists() && !spk.delete()) {
            fail("Couldn't delete " + spk);
        }
        
        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        SpkMaker maker = new SpkMaker(new NullConsole(), producers);
        maker.setSpk(spk);
        maker.setInfo(new File("target/test-classes/org/vafer/jdeb/spk/info"));
        maker.setScripts(new File("target/test-classes/org/vafer/jdeb/spk/scripts"));
        
        maker.createSpk(Compression.GZIP);
        
        // now reopen the package and check the control files
        assertTrue("package not build", spk.exists());
        
        boolean found = ArchiveWalker.walkFileTar(spk, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(final TarArchiveEntry entry, final byte[] content) throws IOException {
                assertTrue("prefix", !entry.getName().startsWith("./"));
                
                InformationInputStream infoStream = new InformationInputStream(new ByteArrayInputStream(content));
                IOUtils.copy(infoStream, NullOutputStream.NULL_OUTPUT_STREAM);
                
                if (entry.getName().startsWith("scripts/")) {
	                if (infoStream.isShell()) {
	                    assertTrue("Permissions on " + entry.getName() + " should be 755", entry.getMode() == PermMapper.toMode("755"));
	                } else {
	                    assertTrue("Permissions on " + entry.getName() + " should be 644", entry.getMode() == PermMapper.toMode("644"));
	                }
	                
	                assertTrue(entry.getName() + " doesn't have Unix line endings", infoStream.hasUnixLineEndings());
	                
	                assertEquals("user", 0, entry.getUserId());
	                assertEquals("group", 0, entry.getGroupId());
                } else {
                	assertFalse("directory found in the archive", entry.isDirectory());
                }
            }
        });
        
        assertTrue("Script files not found in the package", found);
    }

    public void testScriptFilesVariables() throws Exception {
        File spk = new File("target/test-classes/test-scripts.spk");
        if (spk.exists() && !spk.delete()) {
            fail("Couldn't delete " + spk);
        }
        
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("name", "jdeb");
        variables.put("version", "1.0");
        
        Collection<DataProducer> producers = Arrays.asList(new DataProducer[] {new EmptyDataProducer()});
        SpkMaker maker = new SpkMaker(new NullConsole(), producers);
        maker.setSpk(spk);
        maker.setInfo(new File("target/test-classes/org/vafer/jdeb/spk/info"));
        maker.setScripts(new File("target/test-classes/org/vafer/jdeb/spk/scripts"));
        maker.setResolver(new MapVariableResolver(variables));
        
        maker.createSpk(Compression.GZIP);
        
        // now reopen the package and check the control files
        assertTrue("package not build", spk.exists());

        boolean found = ArchiveWalker.walkFileTar(spk, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
            	if (entry.getName().contains("postinst") || entry.getName().contains("prerm")) {
                    String body = new String(content, "ISO-8859-1");
                    assertFalse("Variables not replaced in the script file " + entry.getName(), body.contains("[[name]] [[version]]"));
                    assertTrue("Expected variables not found in the script file " + entry.getName(), body.contains("jdeb 1.0"));
                }
            }
        });
        
        assertTrue("Script files not found in the package", found);
    }
}
