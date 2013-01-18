/*
 * Copyright 2013 The Apache Software Foundation.
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.ar.NonClosingInputStream;
import org.vafer.jdeb.producers.DataProducerFileSet;
import org.vafer.jdeb.utils.InformationInputStream;
import org.vafer.jdeb.utils.MapVariableResolver;

public class ProcessorTestCase extends TestCase {

    /**
     * Checks if the file paths in the md5sums file use only unix file separators
     * (this test can only fail on Windows)
     */
    public void testBuildDataWithFileSet() throws Exception {
        Processor processor = new Processor(new NullConsole(), null);

        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        FileSet fileset = new FileSet();
        fileset.setDir(new File(getClass().getResource("deb/data").toURI()));
        fileset.setIncludes("**/*");
        fileset.setProject(project);

        StringBuilder md5s = new StringBuilder();
        processor.buildData(new DataProducer[] { new DataProducerFileSet(fileset) }, new File("target/data.tar"), md5s, Compression.GZIP);

        assertTrue("empty md5 file", md5s.length() > 0);
        assertFalse("windows path separator found", md5s.indexOf("\\") != -1);
    }

    public void testControlFilesPermissions() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }
        
        Processor processor = new Processor(new NullConsole(), new MapVariableResolver(Collections.<String, String>emptyMap()));
        
        File controlDir = new File("target/test-classes/org/vafer/jdeb/deb/control");
        DataProducer[] producers = {new EmptyDataProducer()};
        
        processor.createDeb(controlDir.listFiles(), producers, deb, Compression.NONE);
        
        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());
        
        boolean controlFound = false;
        
        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(deb));
        try {
            ArArchiveEntry entry;
            while ((entry = in.getNextArEntry()) != null) {
                if (entry.getName().equals("control.tar.gz")) {
                    TarInputStream tar = new TarInputStream(new GZIPInputStream(new NonClosingInputStream(in)));
                    TarEntry tarentry;
                    while ((tarentry = tar.getNextEntry()) != null) {
                        controlFound = true;
                        
                        assertFalse("directory found in the control archive", tarentry.isDirectory());
                        assertTrue("prefix", tarentry.getName().startsWith("./"));
                        
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        tar.copyEntryContents(bout);
                        
                        InformationInputStream infoStream = new InformationInputStream(new ByteArrayInputStream(bout.toByteArray()));
                        IOUtils.copy(infoStream, NullOutputStream.NULL_OUTPUT_STREAM);
                        
                        if (infoStream.isShell()) {
                            assertTrue("Permissions on " + tarentry.getName() + " should be 755", tarentry.getMode() == 0755);
                        } else {
                            assertTrue("Permissions on " + tarentry.getName() + " should be 644", tarentry.getMode() == 0644);
                        }
                        
                        assertTrue(tarentry.getName() + " doesn't have Unix line endings", infoStream.hasUnixLineEndings());
                        
                        assertEquals("user", "root", tarentry.getUserName());
                        assertEquals("group", "root", tarentry.getGroupName());
                    }
                    tar.close();
                } else {
                    // skip to the next entry
                    long skip = entry.getLength();
                    while (skip > 0) {
                        long skipped = in.skip(skip);
                        if (skipped == -1) {
                            throw new IOException("Failed to skip");
                        }
                        skip -= skipped;
                    }
                }
            }
        } finally {
            in.close();
        }
        
        assertTrue("Control files not found in the package", controlFound);
    }

    public void testControlFilesVariables() throws Exception {
        File deb = new File("target/test-classes/test-control.deb");
        if (deb.exists() && !deb.delete()) {
            fail("Couldn't delete " + deb);
        }
        
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("name", "jdeb");
        variables.put("version", "1.0");
        
        Processor processor = new Processor(new NullConsole(), new MapVariableResolver(variables));
        
        File controlDir = new File("target/test-classes/org/vafer/jdeb/deb/control");
        DataProducer[] producers = {new EmptyDataProducer()};
        
        processor.createDeb(controlDir.listFiles(), producers, deb, Compression.NONE);
        
        // now reopen the package and check the control files
        assertTrue("package not build", deb.exists());
        
        boolean controlFound = false;
        
        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(deb));
        try {
            ArArchiveEntry entry;
            while ((entry = in.getNextArEntry()) != null) {
                if (entry.getName().equals("control.tar.gz")) {
                    TarInputStream tar = new TarInputStream(new GZIPInputStream(new NonClosingInputStream(in)));
                    TarEntry tarentry;
                    while ((tarentry = tar.getNextEntry()) != null) {
                        controlFound = true;
                        
                        if (tarentry.getName().contains("postinst") || tarentry.getName().contains("prerm")) {
                            ByteArrayOutputStream bout = new ByteArrayOutputStream();
                            tar.copyEntryContents(bout);
                            
                            String content = bout.toString("ISO-8859-1");
                            
                            assertFalse("Variables not replaced in the control file " + tarentry.getName(), content.contains("[[name]] [[version]]"));
                            assertTrue("Expected variables not found in the control file " + tarentry.getName(), content.contains("jdeb 1.0"));
                        }
                    }
                    tar.close();
                } else {
                    // skip to the next entry
                    long skip = entry.getLength();
                    while (skip > 0) {
                        long skipped = in.skip(skip);
                        if (skipped == -1) {
                            throw new IOException("Failed to skip");
                        }
                        skip -= skipped;
                    }
                }
            }
        } finally {
            in.close();
        }
        
        assertTrue("Control files not found in the package", controlFound);
    }
}
