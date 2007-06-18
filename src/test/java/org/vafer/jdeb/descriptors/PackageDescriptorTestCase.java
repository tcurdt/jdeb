package org.vafer.jdeb.descriptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

public final class PackageDescriptorTestCase extends TestCase {

	public void testParse() throws Exception {
		
		final InputStream is = new ByteArrayInputStream(
				("Key1: Value1\n" +
				 "Key2: Value2\n" +
				 " Value2.1\n" +
				 " Value2.2\n" +
				 "Key3: Value3\n").getBytes());
		
		final PackageDescriptor d = new PackageDescriptor(is);
		assertTrue(d.isValid());
	}
}
