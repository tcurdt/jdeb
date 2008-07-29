/*
 * Copyright 2008 The Apache Software Foundation.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.vafer.jdeb.ar.ArEntry;
import org.vafer.jdeb.ar.ArInputStream;

/**
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class DebAntTaskTestCase extends TestCase {

	private Project project;

	protected void setUp() throws Exception {
		project = new Project();
		project.setCoreLoader(getClass().getClassLoader());
		project.init();

		File buildFile = new File("target/test-classes/testbuild.xml");
		project.setBaseDir(buildFile.getParentFile());
		ProjectHelper.configureProject(project, buildFile);

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
	private void redirectOutput(OutputStream out) {
		DefaultLogger logger = new DefaultLogger();
		logger.setOutputPrintStream(new PrintStream(out));
		logger.setMessageOutputLevel(Project.MSG_INFO);
		project.addBuildListener(logger);
	}

	public void testVerboseEnabled() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		redirectOutput(out);

		project.executeTarget("verbose-enabled");

		assertTrue(out.toString().indexOf("Total size") != -1);
	}

	public void testVerboseDisabled() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		redirectOutput(out);

		project.executeTarget("verbose-disabled");

		assertTrue(out.toString().indexOf("Total size") == -1);
	}

	public void testFileSet() {
		project.executeTarget("fileset");

		assertTrue("package not build", new File("target/test-classes/test.deb").exists());
	}

	public void testTarFileSet() throws Exception {
		project.executeTarget("tarfileset");

		File deb = new File("target/test-classes/test.deb");
		assertTrue("package not build", deb.exists());

		ArInputStream in = new ArInputStream(new FileInputStream(deb));
		ArEntry entry;
		while ((entry = in.getNextEntry()) != null) {
			if (entry.getName().equals("data.tar.gz")) {
				TarInputStream tar = new TarInputStream(new GZIPInputStream(in));
				TarEntry tarentry;
				while ((tarentry = tar.getNextEntry()) != null) {
					assertTrue("prefix", tarentry.getName().startsWith("/foo/"));
					if (tarentry.isDirectory()) {
						assertEquals("directory mode (" + tarentry.getName() + ")", 040700, tarentry.getMode());
					} else {
						assertEquals("file mode (" + tarentry.getName() + ")", 0100600, tarentry.getMode());
					}
					assertEquals("user", "ebourg", tarentry.getUserName());
					assertEquals("group", "ebourg", tarentry.getGroupName());
				}
				tar.close();
			} else {
				// skip to the next entry
				long skip = entry.getLength(); 
				while(skip > 0) {
					long skipped = in.skip(skip); 
					if (skipped == -1) {
						throw new IOException("Failed to skip");
					}
					skip -= skipped;
				}
			}
		}
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

		boolean found = false;

		ArInputStream in = new ArInputStream(new FileInputStream(deb));
		ArEntry entry;
		while ((entry = in.getNextEntry()) != null) {
			if (entry.getName().equals("data.tar.bz2")) {
				found = true;

				assertEquals("header 0", (byte) 'B', in.read());
				assertEquals("header 1", (byte) 'Z', in.read());

				TarInputStream tar = new TarInputStream(new CBZip2InputStream(in));
				while ((tar.getNextEntry()) != null);
				tar.close();
				break;
			} else {
				// skip to the next entry
				long skip = entry.getLength(); 
				while(skip > 0) {
					long skipped = in.skip(skip); 
					if (skipped == -1) {
						throw new IOException("Failed to skip");
					}
					skip -= skipped;
				}
			}
		}

		assertTrue("bz2 file not found", found);
	}

	public void testNoCompression() throws Exception {
		project.executeTarget("no-compression");

		File deb = new File("target/test-classes/test.deb");
		assertTrue("package not build", deb.exists());

		boolean found = false;

		ArInputStream in = new ArInputStream(new FileInputStream(deb));
		ArEntry entry;
		while ((entry = in.getNextEntry()) != null) {
			if (entry.getName().equals("data.tar")) {
				found = true;

				TarInputStream tar = new TarInputStream(in);
				while ((tar.getNextEntry()) != null);
				tar.close();
			} else {
				// skip to the next entry
				long skip = entry.getLength(); 
				while(skip > 0) {
					long skipped = in.skip(skip); 
					if (skipped == -1) {
						throw new IOException("Failed to skip");
					}
					skip -= skipped;
				}
			}
		}

		assertTrue("tar file not found", found);
	}
}
