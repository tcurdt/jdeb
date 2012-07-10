package org.vafer.jdeb.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

/*
 * Admittedly not the nicest way to assert that failOnMissingSrc functions. However, the best that can be done without
 * refactoring, mocking, or extending the scope of the test beyond this unit.
 */
public class DataTestCase extends TestCase {

    private Data data;
    private File missingFile;
    private File file;

    @Override
    protected void setUp() throws Exception {
        data = new Data();
        missingFile = new File("this-file-does-not-exist");
        file = File.createTempFile(getClass().getSimpleName(), "dat");
    }

    @Override
    protected void tearDown() throws Exception {
        if (file != null) {
            file.delete();
        }
    }

    public void testSrcAndPathsNotSetFails() throws IOException {
        RuntimeException expectedException = null;
        try {
            data.setPaths(null);
            data.setSrc(null);
            data.produce(null);
            fail();
        } catch (RuntimeException expected) {
            expectedException = expected;
        }
        assertEquals("src or paths not set", expectedException.getMessage());
    }

    public void testPathsMaySubstituteSrc() throws IOException {
        IOException expectedException = null;
        try {
            data.setPaths(new String[] { "/var/log", "/var/lib" });
            data.setSrc(null);
            data.produce(null);
            fail();
        } catch (IOException expected) {
            expectedException = expected;
        }
        assertTrue(expectedException.getMessage().startsWith("Unknown type "));
    }

    public void testFailOnUnknownValue() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("not a value value");
            data.produce(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testFailOnMissingSrcDefaultFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.produce(null);
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    public void testFailOnMissingSrcIgnoreFileMissing() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc("ignore");
        data.produce(null);
    }

    public void testFailOnMissingSrcIgnoreFileMissingVaryInput() throws IOException {
        data.setSrc(missingFile);
        data.setMissingSrc(" IGNORE ");
        data.produce(null);
    }

    public void testFailOnMissingSrcFailFileMissing() throws IOException {
        try {
            data.setSrc(missingFile);
            data.setMissingSrc("fail");
            data.produce(null);
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    public void testFailOnMissingSrcDefaultFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

    public void testFailOnMissingSrcIgnoreFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("ignore");
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

    public void testFailOnMissingSrcFailFileExists() throws IOException {
        IOException unknownTypeException = null;
        try {
            data.setSrc(file);
            data.setMissingSrc("fail");
            data.produce(null);
        } catch (IOException expected) {
            unknownTypeException = expected;
        }
        assertTrue(unknownTypeException.getMessage().startsWith("Unknown type"));
    }

}
