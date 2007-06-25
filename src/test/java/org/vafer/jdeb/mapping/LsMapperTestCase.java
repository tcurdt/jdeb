package org.vafer.jdeb.mapping;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.apache.tools.tar.TarEntry;

public final class LsMapperTestCase extends TestCase {

	public void testSuccessfulParsing() throws Exception {
		final ByteArrayInputStream is = new ByteArrayInputStream((
			"./trunk/target/test-classes/org/vafer/dependency:\n" +
			"total 176\n" +
			"drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .\n" +
			"drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..\n" +
			"-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class\n" +
			"-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class\n" +
			"drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes\n" +
			"\n" +
			"./trunk/src/test-classes/org/vafer/dependency:\n" +
			"total 76\n" +
			"drwxr-xr-x   23 tcurdt  tcurdt   782 Jun 25 03:48 .\n" +
			"drwxr-xr-x    3 tcurdt  tcurdt   102 Jun 25 03:48 ..\n" +
			"-rw-r--r--    1 tcurdt  tcurdt  2934 Jun 25 03:48 DependenciesTestCase.class\n" +
			"-rw-r--r--    1 tcurdt  tcurdt   786 Jun 25 03:48 JarCombiningTestCase$1.class\n" +
			"drwxr-xr-x    4 tcurdt  tcurdt   136 Jun 25 03:48 classes\n" +
			"\n"
				).getBytes("UTF-8"));
		
		final Mapper mapper = new LsMapper(is);
		
		final TarEntry unknown = new TarEntry("xyz");
		assertSame(unknown, mapper.map(unknown));
		
		final TarEntry known = new TarEntry("trunk/target/test-classes/org/vafer/dependency");
		final TarEntry knownMapped = mapper.map(known);
		
		assertNotSame(known, knownMapped);
		
	}
	
	public void testPrematureEOF() {
		
	}
	
	public void testWrongFormat() {
		
	}
}
