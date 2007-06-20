package org.vafer.jdeb.signing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

public final class SigningTestCase extends TestCase {

	public void testClearSign() throws Exception {
		
		final InputStream input = new FileInputStream("/Users/tcurdt/changes.txt");
		final OutputStream output = new FileOutputStream("/Users/tcurdt/changes.txt.signed");
		final InputStream ring = new FileInputStream("/Users/tcurdt/.gnupg/secring.gpg");

		SigningUtils.clearSign(input, ring, "7C200941", "", output);
	}
}
