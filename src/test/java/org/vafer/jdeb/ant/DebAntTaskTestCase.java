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

import java.io.File;

import junit.framework.TestCase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

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

}
