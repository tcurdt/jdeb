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

package org.vafer.jdeb.ant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.ArchiveVisitor;
import org.vafer.jdeb.ArchiveWalker;
import org.vafer.jdeb.Compression;

/**
 * @author Emmanuel Bourg
 */
public final class DebAntTaskTestCase extends TestCase {

    private Project project;

    protected void setUp() throws Exception {
        project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        File buildFile = new File("target/test-classes/testbuild.xml");
        project.setBaseDir(buildFile.getParentFile());

        final ProjectHelper helper = ProjectHelper.getProjectHelper();
        helper.parse(project, buildFile);

        // remove the package previously build
        File deb = new File("target/test.deb");
        if (deb.exists()) {
            assertTrue("Unable to remove the test archive", deb.delete());
        }
    }

    public void testMissingControl() {
        try {
            project.executeTarget("missing-control");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testInvalidControl() {
        try {
            project.executeTarget("invalid-control");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testMissingDestFile() {
        try {
            project.executeTarget("missing-destfile");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testEmptyPackage() {
        try {
            project.executeTarget("empty-package");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testPackageWithArchive() {
        project.executeTarget("with-archive");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    public void testPackageWithMissingArchive() {
        try {
            project.executeTarget("with-missing-archive");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testPackageWithDirectory() {
        project.executeTarget("with-directory");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    public void testPackageWithMissingDirectory() {
        try {
            project.executeTarget("with-missing-directory");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    /**
     * Redirects the Ant output to the specified stream.
     */
    private void redirectOutput( OutputStream out ) {
        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(new PrintStream(out));
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);
    }

    public void testVerboseEnabled() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        redirectOutput(out);

        project.executeTarget("verbose-enabled");

        assertTrue(out.toString().contains("Total size"));
    }

    public void testVerboseDisabled() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        redirectOutput(out);

        project.executeTarget("verbose-disabled");

        assertTrue(!out.toString().contains("Total size"));
    }

    public void testMissingDataType() {
        try {
            project.executeTarget("missing-data-type");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testUnknownDataType() {
        try {
            project.executeTarget("unknown-data-type");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testFileSet() {
        project.executeTarget("fileset");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    public void testTarFileSet() throws Exception {
        project.executeTarget("tarfileset");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        ArchiveWalker.walkData(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                assertTrue("prefix", entry.getName().startsWith("./foo/"));
                if (entry.isDirectory()) {
                    assertEquals("directory mode (" + entry.getName() + ")", 040700, entry.getMode());
                } else {
                    assertEquals("file mode (" + entry.getName() + ")", 0100600, entry.getMode());
                }
                assertEquals("user", "ebourg", entry.getUserName());
                assertEquals("group", "ebourg", entry.getGroupName());
            }
        }, Compression.GZIP);
    }

    public void testUnkownCompression() throws Exception {
        try {
            project.executeTarget("unknown-compression");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    public void testBZip2Compression() throws Exception {
        project.executeTarget("bzip2-compression");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        final AtomicBoolean found = new AtomicBoolean(false); 
        
        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(deb));
        ArchiveWalker.walk(in, new ArchiveVisitor<ArArchiveEntry>() {
            public void visit(ArArchiveEntry entry, byte[] content) throws IOException {
                if (entry.getName().equals("data.tar.bz2")) {
                    found.set(true);

                    assertEquals("header 0", (byte) 'B', content[0]);
                    assertEquals("header 1", (byte) 'Z', content[1]);

                    TarInputStream tar = new TarInputStream(new BZip2CompressorInputStream(new ByteArrayInputStream(content)));
                    while ((tar.getNextEntry()) != null) ;
                    tar.close();
                }
            }
        });
        
        assertTrue("bz2 file not found", found.get());
    }

    public void testNoCompression() throws Exception {
        project.executeTarget("no-compression");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        final AtomicBoolean found = new AtomicBoolean(false); 
        
        ArchiveWalker.walkData(deb, new ArchiveVisitor<TarArchiveEntry>() {
            public void visit(TarArchiveEntry entry, byte[] content) throws IOException {
                found.set(true);
            }
        }, Compression.NONE);

        assertTrue("tar file not found", found.get());
    }
}
