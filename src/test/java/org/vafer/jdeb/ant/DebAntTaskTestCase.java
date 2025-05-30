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

package org.vafer.jdeb.ant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.ArchiveVisitor;
import org.vafer.jdeb.ArchiveWalker;
import org.vafer.jdeb.Compression;

public final class DebAntTaskTestCase extends Assert {

    private Project project;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testMissingControl() {
        try {
            project.executeTarget("missing-control");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testInvalidControl() {
        try {
            project.executeTarget("invalid-control");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testMissingDestFile() {
        try {
            project.executeTarget("missing-destfile");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testEmptyPackage() {
        project.executeTarget("empty-package");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    @Test
    public void testPackageWithArchive() {
        project.executeTarget("with-archive");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    @Test
    public void testPackageWithMissingArchive() {
        try {
            project.executeTarget("with-missing-archive");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testPackageWithDirectory() {
        project.executeTarget("with-directory");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    @Test
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

    @Test
    public void testVerboseEnabled() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        redirectOutput(out);

        project.executeTarget("verbose-enabled");

        assertTrue(out.toString().contains("Total size"));
    }

    @Test
    public void testVerboseDisabled() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        redirectOutput(out);

        project.executeTarget("verbose-disabled");

        assertTrue(!out.toString().contains("Total size"));
    }

    @Test
    public void testMissingDataType() {
        try {
            project.executeTarget("missing-data-type");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testUnknownDataType() {
        try {
            project.executeTarget("unknown-data-type");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testFileSet() {
        project.executeTarget("fileset");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }

    @Test
    public void testTarFileSet() throws Exception {
        project.executeTarget("tarfileset");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        ArchiveWalker.walkData(deb, (entry, content) -> {
            assertTrue("prefix: " + entry.getName(), entry.getName().startsWith("./foo/"));
            if (entry.isDirectory()) {
                assertEquals("directory mode (" + entry.getName() + ")", 040700, entry.getMode());
            } else {
                assertEquals("file mode (" + entry.getName() + ")", 0100600, entry.getMode());
            }
            assertEquals("user", "ebourg", entry.getUserName());
            assertEquals("group", "ebourg", entry.getGroupName());
        }, Compression.GZIP);
    }

    @Test
    public void testLink() throws Exception {
        project.executeTarget("link");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        final AtomicBoolean linkFound = new AtomicBoolean(false);

        ArchiveWalker.walkData(deb, (entry, content) -> {
            if (entry.isSymbolicLink()) {
                linkFound.set(true);
                assertEquals("link mode (" + entry.getName() + ")", 0120755, entry.getMode());
            }
            assertEquals("user", "ebourg", entry.getUserName());
            assertEquals("group", "ebourg", entry.getGroupName());
        }, Compression.GZIP);

        assertTrue("Link not found", linkFound.get());
    }

    @Test
    public void testMapper() throws Exception {
        project.executeTarget("perm-mapper");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        ArchiveWalker.walkData(deb, (entry, content) -> {
            if (entry.isFile()) {
                assertEquals("file mode (" + entry.getName() + ")", 0700, entry.getMode());
            }
        }, Compression.GZIP);
    }

    @Test
    public void testUnkownCompression() throws Exception {
        try {
            project.executeTarget("unknown-compression");
            fail("No exception thrown");
        } catch (BuildException e) {
            // expected
        }
    }

    @Test
    public void testBZip2Compression() throws Exception {
        project.executeTarget("bzip2-compression");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        final AtomicBoolean found = new AtomicBoolean(false);

        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(deb));
        ArchiveWalker.walk(in, (ArchiveVisitor<ArArchiveEntry>) (entry, content) -> {
            if (entry.getName().equals("data.tar.bz2")) {
                found.set(true);

                assertEquals("header 0", (byte) 'B', content[0]);
                assertEquals("header 1", (byte) 'Z', content[1]);

                TarInputStream tar = new TarInputStream(new BZip2CompressorInputStream(new ByteArrayInputStream(content)));
                while ((tar.getNextEntry()) != null) ;
                tar.close();
            }
        });

        assertTrue("bz2 file not found", found.get());
    }

    @Test
    public void testXZCompression() throws Exception {
        project.executeTarget("xz-compression");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        final AtomicBoolean found = new AtomicBoolean(false);

        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(deb));
        ArchiveWalker.walk(in, (ArchiveVisitor<ArArchiveEntry>) (entry, content) -> {
            if (entry.getName().equals("data.tar.xz")) {
                found.set(true);

                assertEquals("header 0", (byte) 0xFD, content[0]);
                assertEquals("header 1", (byte) '7', content[1]);
                assertEquals("header 2", (byte) 'z', content[2]);
                assertEquals("header 3", (byte) 'X', content[3]);
                assertEquals("header 4", (byte) 'Z', content[4]);
                assertEquals("header 5", (byte) '\0', content[5]);

                TarInputStream tar = new TarInputStream(new XZCompressorInputStream(new ByteArrayInputStream(content)));
                while ((tar.getNextEntry()) != null) ;
                tar.close();
            }
        });

        assertTrue("xz file not found", found.get());
    }

    @Test
    public void testNoCompression() throws Exception {
        project.executeTarget("no-compression");

        File deb = new File("target/test-classes/test.deb");
        assertTrue("package not build", deb.exists());

        boolean found = ArchiveWalker.walkData(deb, (entry, content) -> {
        }, Compression.NONE);

        assertTrue("tar file not found", found);
    }

    @Test
    public void testPackageConffiles() {
        project.executeTarget("conffiles");

        assertTrue("package not build", new File("target/test-classes/test.deb").exists());
    }
}
